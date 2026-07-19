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

import java.nio.DoubleBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
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
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
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
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.FD;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoubleMapMultiConsumer;
import com.landawn.abacus.util.function.DoubleNFunction;
import com.landawn.abacus.util.function.DoubleTernaryOperator;
import com.landawn.abacus.util.function.DoubleToFloatFunction;
import com.landawn.abacus.util.function.DoubleTriPredicate;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * A specialized stream implementation for processing sequences of primitive {@code double} values with functional-style operations.
 * This abstract class extends {@link StreamBase} and provides comprehensive double-specific operations including
 * filtering, mapping, reduction, and collection operations optimized for double data types.
 *
 * <p>DoubleStream represents a sequence of double elements supporting sequential and parallel aggregate operations.
 * It provides a more efficient alternative to generic {@link Stream} when working specifically with double values,
 * avoiding boxing/unboxing overhead and offering double-specific utility methods for numerical computations.
 *
 * <p><b>Floating-point special values:</b> Elements may include {@link Double#NaN},
 * {@link Double#POSITIVE_INFINITY}, {@link Double#NEGATIVE_INFINITY}, and negative zero ({@code -0.0}).
 * Aggregate operations such as {@link #sum()}, {@link #average()}, {@link #min()}, and {@link #max()}
 * follow IEEE 754 semantics: any {@code NaN} element propagates to the result as {@code NaN}
 * (because {@link #min()}/{@link #max()} use {@link Math#min(double, double)}/{@link Math#max(double, double)}).
 * Ordering operations (such as {@link #sorted()}, {@link #kthLargest(int)}, and {@link #top(int)}) instead use
 * {@link Double#compare(double, double)}, which treats {@code NaN} as greater than any
 * other value (including positive infinity) and considers {@code -0.0} less than {@code +0.0}.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed for double operations, preventing ClassCastException</li>
 *   <li><b>Performance:</b> Optimized for double primitives, avoiding boxing overhead</li>
 *   <li><b>Numerical Operations:</b> Specialized methods for mathematical computations and statistical analysis</li>
 *   <li><b>Parallel Support:</b> Efficient parallel processing capabilities for large numerical datasets</li>
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
 *   <li><b>Data Processing:</b> Processing large datasets of floating-point numbers</li>
 *   <li><b>Scientific Computing:</b> Mathematical simulations, data analysis</li>
 *   <li><b>Financial Calculations:</b> Processing monetary values, financial computations</li>
 *   <li><b>Performance Analysis:</b> Processing measurement data, metrics analysis</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic double stream operations
 * DoubleStream.of(1.5, 2.7, 3.1, 4.9, 5.2)
 *     .filter(d -> d > 3.0)   // keeps values > 3.0
 *     .map(d -> d * 2)        // doubles each value
 *     .sum();                 // sums result: 26.4
 *
 * // Statistical operations
 * DoubleSummaryStatistics stats = DoubleStream.of(temperatureReadings)
 *     .filter(temp -> temp > 0)   // filters valid temperatures
 *     .summaryStatistics();       // gets min, max, avg, count
 *
 * // Mathematical operations with parallel processing
 * double result = DoubleStream.iterate(1.0, d -> d * 1.1)
 *     .limit(1000)            // keeps 1000 values
 *     .parallel()             // uses parallel processing
 *     .filter(d -> d < 100)   // filters values < 100
 *     .map(Math::sqrt)        // maps via square root
 *     .average()              // returns average
 *     .orElse(0.0);           // returns default if empty
 *
 * // Integration with other stream types
 * IntStream counts = DoubleStream.of(prices)
 *     .mapToInt(price -> (int) Math.ceil(price)) // maps to ceiling integers
 *     .distinct();                               // removes duplicates
 *
 * // Processing a sequence of double values
 * DoubleStream.of(1.5, 2.5, 3.5)
 *     .takeWhile(d -> d >= 0)            // processes until negative number
 *     .mapToObj(String::valueOf)         // maps to strings
 *     .forEach(System.out::println);     // prints each value
 * }</pre>
 *
 * <p><b>Double-Specific Operations:</b>
 * <ul>
 *   <li>{@code filter(DoublePredicate)} - Filter doubles based on conditions</li>
 *   <li>{@code map(DoubleUnaryOperator)} - Transform double values</li>
 *   <li>{@code reduce(DoubleBinaryOperator)} - Reduce to single double value</li>
 *   <li>{@code sum()}, {@code average()}, {@code min()}, {@code max()} - Mathematical aggregations</li>
 *   <li>{@code mapToInt()}, {@code mapToLong()} - Convert to other primitive streams</li>
 *   <li>{@code boxed()} - Convert to Stream&lt;Double&gt;</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li>Use DoubleStream instead of {@code Stream<Double>} to avoid boxing overhead</li>
 *   <li>Parallel processing benefits large datasets (typically &gt; 10,000 elements)</li>
 *   <li>Sequential processing is more efficient for small datasets and simple operations</li>
 *   <li>Lazy evaluation means intermediate operations are not executed until terminal operations</li>
 * </ul>
 *
 * <p><b>abacus {@code DoubleStream} vs. {@link java.util.stream.DoubleStream JDK DoubleStream} &mdash; how they differ:</b>
 * This {@code DoubleStream} mirrors the JDK's {@link java.util.stream.DoubleStream} (the same lazy, single-use,
 * primitive-{@code double} pipeline) but adds a much larger operation set and a few behavioral differences,
 * paralleling the relationship between the abacus and JDK object {@link Stream}s:
 *
 * <table border="1">
 *   <caption>abacus {@code DoubleStream} compared with {@code java.util.stream.DoubleStream}</caption>
 *   <tr><th>Aspect</th><th>abacus {@code DoubleStream}</th><th>JDK {@code java.util.stream.DoubleStream}</th></tr>
 *   <tr>
 *     <td>Operation set</td>
 *     <td>Much larger &mdash; e.g. {@code takeWhile}/{@code dropWhile} (independent of the JDK version),
 *         {@code collapse}, {@code scan}, {@code step}, {@code zipWith}/{@code mergeWith}, plus convenient
 *         terminals and {@link DoubleList} conversions.</td>
 *     <td>The standard, smaller set defined by {@code java.util.stream}.</td>
 *   </tr>
 *   <tr>
 *     <td>Companion primitive streams</td>
 *     <td>One of a full family: {@link ByteStream}, {@link CharStream}, {@link ShortStream}, {@link IntStream},
 *         {@link LongStream}, {@link FloatStream}, {@code DoubleStream}.</td>
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
 *   <caption>How selected {@code DoubleStream} operations differ from JDK</caption>
 *   <thead>
 *     <tr><th>Operation</th><th>Behavior difference</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@code min}/{@code max}/{@code reduce}/{@code findFirst}/{@code findAny}/{@code average}</td>
 *       <td><b><i>abacus</i></b>: returns {@code u.OptionalDouble} &middot; &#9888;&#65039; <b><i>JDK</i></b>: returns {@code java.util.OptionalDouble}</td>
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
 *       <td><b><i>abacus</i></b>: returns an extended {@code DoubleIterator} (and is deprecated) that does not auto-close the stream &middot; &#9888;&#65039; <b><i>JDK</i></b>: returns a {@code java.util.PrimitiveIterator.OfDouble}</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p><b>Prefer the abacus {@code DoubleStream} when:</b> you want the expanded operation set, the additional
 * primitive stream types, automatic closing of I/O-backed pipelines, or tight interop with the abacus
 * {@link Stream} / {@link DoubleList} ecosystem.
 *
 * <p><b>Prefer the JDK {@link java.util.stream.DoubleStream} when:</b> you want to avoid an extra dependency,
 * or the surrounding code is standardized on {@code java.util.stream} and must exchange JDK stream types
 * across its API boundaries.
 *
 * <p>The two convert directly: {@link #from(java.util.stream.DoubleStream)} wraps a JDK {@code DoubleStream}
 * and {@link #toJdkStream()} returns one, while {@link #boxed()} bridges to the object stream and
 * {@code mapToInt}/{@code mapToLong} narrow to other primitive streams.
 *
 * @see StreamBase
 * @see IntStream
 * @see LongStream
 * @see FloatStream
 * @see Stream
 * @see DoubleIterator
 * @see DoubleList
 * @see DoubleSummaryStatistics
 * @see java.util.stream.DoubleStream
 * @see OptionalDouble
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@LazyEvaluation
public abstract class DoubleStream
        extends StreamBase<Double, double[], DoublePredicate, DoubleConsumer, OptionalDouble, IndexedDouble, DoubleIterator, DoubleStream> {

    static final Random RAND = new SecureRandom();

    DoubleStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
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
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
     *       .filter(x -> x > 3.0)
     *       .toArray();   // returns [4.0, 5.0]
     *
     * // Filter out NaN values
     * DoubleStream.of(1.0, Double.NaN, 3.0, Double.NaN, 5.0)
     *       .filter(d -> !Double.isNaN(d))
     *       .toArray();   // returns [1.0, 3.0, 5.0]
     *
     * // Filter finite values
     * DoubleStream.of(1.0, Double.POSITIVE_INFINITY, -0.0, Double.NEGATIVE_INFINITY, 5.0)
     *       .filter(d -> Double.isFinite(d))
     *       .toArray();   // returns [1.0, -0.0, 5.0]
     *
     * // Empty stream filter returns empty
     * DoubleStream.empty().filter(x -> true).toArray();   // returns []
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
     * @param predicate a non-interfering, stateless predicate that tests each element to determine if it should be included
     * @return a new stream consisting of the elements that match the given predicate
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#filter(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract DoubleStream filter(final DoublePredicate predicate);

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
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 2.0, 1.0)
     *       .takeWhile(x -> x < 4.0)
     *       .toArray();   // returns [1.0, 2.0, 3.0]
     *
     * // Empty stream returns empty
     * DoubleStream.empty().takeWhile(x -> true).toArray();   // returns []
     *
     * // No elements match predicate from start
     * DoubleStream.of(5.0, 6.0, 7.0).takeWhile(x -> x < 3.0).toArray();   // returns []
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
     * @return a new stream consisting of elements from this stream until an element is encountered that doesn't match the predicate
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#takeWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract DoubleStream takeWhile(final DoublePredicate predicate);

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
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 2.0, 1.0)
     *       .dropWhile(x -> x < 4.0)
     *       .toArray();   // returns [4.0, 5.0, 2.0, 1.0]
     *
     * // Empty stream returns empty
     * DoubleStream.empty().dropWhile(x -> true).toArray();   // returns []
     *
     * // All elements match: all dropped
     * DoubleStream.of(1.0, 2.0, 3.0).dropWhile(x -> x < 10.0).toArray();   // returns []
     *
     * // No elements match predicate: all preserved
     * DoubleStream.of(5.0, 6.0, 7.0).dropWhile(x -> x < 3.0).toArray();   // returns [5.0, 6.0, 7.0]
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
    public abstract DoubleStream dropWhile(final DoublePredicate predicate);

    /**
     * Returns a DoubleStream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each double element using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0)
     *       .map(x -> x * 2.0)
     *       .toArray();   // returns [2.0, 4.0, 6.0, 8.0]
     *
     * // Map with NaN (NaN operations propagate NaN)
     * DoubleStream.of(1.0, Double.NaN, 3.0)
     *       .map(d -> d * 2.0)
     *       .toArray();   // returns [2.0, NaN, 6.0]
     *
     * // Map with square root
     * DoubleStream.of(4.0, 9.0, 16.0)
     *       .map(Math::sqrt)
     *       .toArray();   // returns [2.0, 3.0, 4.0]
     *
     * // Empty stream returns empty
     * DoubleStream.empty().map(x -> x * 2).toArray();   // returns []
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
     * @param mapper a non-interfering, stateless function that transforms each element from double to double
     * @return a new DoubleStream consisting of the results of applying the mapper function to the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream map(DoubleUnaryOperator mapper);

    /**
     * Returns an {@code IntStream} consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that converts each double element to an int using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.5, 2.7, 3.2, 4.9)
     *       .mapToInt(d -> (int) d)
     *       .toArray();   // returns [1, 2, 3, 4]
     *
     * // Converting prices to dollar amounts
     * DoubleStream.of(10.99, 20.49, 15.00)
     *       .mapToInt(price -> (int) Math.round(price))
     *       .sum();   // returns 46
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
     * @param mapper a non-interfering, stateless function that transforms each element from double to int
     * @return a new IntStream consisting of the results of applying the mapper function to the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     * @see #mapToLong(DoubleToLongFunction)
     * @see #mapToFloat(DoubleToFloatFunction)
     * @see #mapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapToInt(DoubleToIntFunction mapper);

    /**
     * Returns a {@code LongStream} consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that converts each double element to a long using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.5, 2.7, 3.2, 4.9)
     *       .mapToLong(d -> (long) d)
     *       .toArray();   // returns [1L, 2L, 3L, 4L]
     *
     * // Converting timestamps with fractional seconds
     * DoubleStream.of(1609459200.5, 1609459260.8, 1609459320.2)
     *       .mapToLong(timestamp -> (long) (timestamp * 1000))
     *       .toArray();   // returns millisecond timestamps
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
     * @param mapper a non-interfering, stateless function that transforms each element from double to long
     * @return a new LongStream consisting of the results of applying the mapper function to the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     * @see #mapToInt(DoubleToIntFunction)
     * @see #mapToFloat(DoubleToFloatFunction)
     * @see #mapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream mapToLong(DoubleToLongFunction mapper);

    /**
     * Returns a {@code FloatStream} consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that converts each double element to a float using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.5, 2.7, 3.2, 4.9)
     *       .mapToFloat(d -> (float) d)
     *       .toArray();   // returns [1.5f, 2.7f, 3.2f, 4.9f]
     *
     * // Downcast to float for memory efficiency
     * DoubleStream.of(10.123456789, 20.987654321, 30.555555555)
     *       .mapToFloat(d -> (float) d)
     *       .forEach(System.out::println);
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
     * @param mapper a non-interfering, stateless function that transforms each element from double to float
     * @return a new FloatStream consisting of the results of applying the mapper function to the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     * @see #mapToInt(DoubleToIntFunction)
     * @see #mapToLong(DoubleToLongFunction)
     * @see #mapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream mapToFloat(DoubleToFloatFunction mapper);

    /**
     * Returns an object-valued {@code Stream} consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that converts each double element to an object using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.5, 2.7, 3.2)
     *       .mapToObj(String::valueOf)
     *       .collect(Collectors.toList());   // returns ["1.5", "2.7", "3.2"]
     *
     * // Convert to custom objects
     * DoubleStream.of(10.99, 20.49, 15.00)
     *       .mapToObj(price -> new Product("Item", price))
     *       .forEach(System.out::println);
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
     * @param mapper a non-interfering, stateless function that transforms each element from double to T
     * @return a new Stream consisting of the results of applying the mapper function to the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     * @see #mapToInt(DoubleToIntFunction)
     * @see #mapToLong(DoubleToLongFunction)
     * @see #mapToFloat(DoubleToFloatFunction)
     * @see #boxed()
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> mapToObj(DoubleFunction<? extends T> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents of
     * a mapped stream produced by applying the provided mapping function to each element.
     * Each non-null mapped stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0)
     *       .flatMap(d -> DoubleStream.of(d, d * 2))
     *       .toArray();   // returns [1.0, 2.0, 2.0, 4.0, 3.0, 6.0]
     *
     * // Generate range for each element
     * DoubleStream.of(2.0, 3.0)
     *       .flatMap(d -> IntStream.range(1, (int) d + 1).asDoubleStream())
     *       .toArray();   // returns [1.0, 2.0, 1.0, 2.0, 3.0]
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
     * @param mapper a non-interfering, stateless function that transforms each element to a DoubleStream
     * @return a new DoubleStream consisting of the flattened contents of all mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapArray(DoubleFunction)
     * @see #flatMapToInt(DoubleFunction)
     * @see #flatMapToLong(DoubleFunction)
     * @see #flatMapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper);

    // public abstract DoubleStream flatmap(DoubleFunction<DoubleIterator> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped {@link java.util.Collection Collection} produced by applying the provided mapping function to each element.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(DoubleFunction) flatMap} (uppercase 'M') - transforms each element into a {@link DoubleStream}.</li>
     *   <li>{@link #flatmap(DoubleFunction) flatmap} (this method, lowercase 'm') - transforms each element into a {@link java.util.Collection Collection} of {@link Double}.</li>
     *   <li>{@link #flatMapArray(DoubleFunction) flatMapArray} - transforms each element into a {@code double[]}.</li>
     * </ul>
     *
     * <p>Each element is transformed into a collection, and all resulting collections are flattened
     * into a single {@code DoubleStream}. If the mapper returns {@code null} or an empty collection
     * for a given element, that element contributes no values to the resulting stream.
     *
     * <p><b>Null elements within a collection</b> returned by the mapper are <em>not</em> propagated as
     * exceptions. Each {@code null} {@link Double} entry is replaced with the primitive default value
     * {@code 0d} so that this operation never throws {@link NullPointerException} when unboxing.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0)
     *             .flatmap(d -> Arrays.asList(d, d * 10))
     *             .toArray();   // [1.0, 10.0, 2.0, 20.0]
     *
     * // null elements in a returned collection become 0d
     * DoubleStream.of(1.0)
     *             .flatmap(d -> Arrays.asList((Double) null, 7.5))
     *             .toArray();   // [0.0, 7.5]
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
     * @param mapper a non-interfering, stateless function that transforms each element from double to {@code Collection<Double>}
     * @return a new {@code DoubleStream} consisting of the flattened contents of the collections produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(DoubleFunction)
     * @see #flatMapArray(DoubleFunction)
     * @see Stream#flatmap(java.util.function.Function)
     */
    // @ai-ignore flatmap/flatMap naming - intentional: flatMap maps to DoubleStream, flatmap maps to Collection<Double>, flatMapArray maps to double[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream flatmap(DoubleFunction<? extends Collection<Double>> mapper); //NOSONAR

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0)
     *       .flatMapArray(d -> new double[]{d, d * 2})
     *       .toArray();   // returns [1.0, 2.0, 2.0, 4.0, 3.0, 6.0]
     *
     * // Generate multiples for each element
     * DoubleStream.of(2.0, 3.0)
     *       .flatMapArray(d -> new double[]{d, d * 10, d * 100})
     *       .toArray();   // returns [2.0, 20.0, 200.0, 3.0, 30.0, 300.0]
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
     * @param mapper a non-interfering, stateless function that transforms each element to a double array
     * @return a new DoubleStream consisting of the flattened contents of all mapped arrays
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(DoubleFunction)
     * @see #flatMapToInt(DoubleFunction)
     * @see #flatMapToLong(DoubleFunction)
     */
    // @ai-ignore flatMapArray/flatMap/flattMap naming - intentional: flatMap maps to DoubleStream, flatMapArray maps to double[], flattMap maps to JDK java.util.stream.DoubleStream. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream flatMapArray(DoubleFunction<double[]> mapper); //NOSONAR

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped JDK DoubleStream produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     * This method is useful when integrating with standard Java Stream API.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0)
     *       .flattMap(d -> java.util.stream.DoubleStream.of(d, d * 2))
     *       .toArray();   // returns [1.0, 2.0, 2.0, 4.0, 3.0, 6.0]
     *
     * // Integration with JDK streams
     * DoubleStream.of(5.0, 10.0)
     *       .flattMap(d -> java.util.stream.DoubleStream.iterate(d, i -> i - 1).limit(3))
     *       .toArray();   // returns [5.0, 4.0, 3.0, 10.0, 9.0, 8.0]
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
     * Each non-null mapped JDK stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a JDK DoubleStream
     * @return a new DoubleStream consisting of the flattened contents of all mapped JDK streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(DoubleFunction)
     * @see #flatMapArray(DoubleFunction)
     */
    // @ai-ignore flatMapArray/flatMap/flattMap naming - intentional: flatMap maps to DoubleStream, flatMapArray maps to double[], flattMap maps to JDK java.util.stream.DoubleStream. Do not suggest renaming.
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream flattMap(DoubleFunction<? extends java.util.stream.DoubleStream> mapper); //NOSONAR

    /**
     * Returns an {@code IntStream} consisting of the results of replacing each element of this stream with the contents
     * of a mapped stream produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(2.5, 3.7)
     *       .flatMapToInt(d -> IntStream.of((int) d, (int) (d * 2)))
     *       .toArray();   // returns [2, 5, 3, 7]
     *
     * // Generate integer range for each double
     * DoubleStream.of(2.0, 3.0)
     *       .flatMapToInt(d -> IntStream.range(0, (int) d))
     *       .toArray();   // returns [0, 1, 0, 1, 2]
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
     * Each non-null mapped stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to an IntStream
     * @return a new IntStream consisting of the flattened contents of all mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapToLong(DoubleFunction)
     * @see #flatMapToFloat(DoubleFunction)
     * @see #flatMapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatMapToInt(DoubleFunction<? extends IntStream> mapper);

    /**
     * Returns a {@code LongStream} consisting of the results of replacing each element of this stream with the contents
     * of a mapped stream produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(2.5, 3.7)
     *       .flatMapToLong(d -> LongStream.of((long) d, (long) (d * 2)))
     *       .toArray();   // returns [2L, 5L, 3L, 7L]
     *
     * // Generate long range for each double
     * DoubleStream.of(2.0, 3.0)
     *       .flatMapToLong(d -> LongStream.range(0, (long) d))
     *       .toArray();   // returns [0L, 1L, 0L, 1L, 2L]
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
     * Each non-null mapped stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a LongStream
     * @return a new LongStream consisting of the flattened contents of all mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapToInt(DoubleFunction)
     * @see #flatMapToFloat(DoubleFunction)
     * @see #flatMapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream flatMapToLong(DoubleFunction<? extends LongStream> mapper);

    /**
     * Returns a {@code FloatStream} consisting of the results of replacing each element of this stream with the contents
     * of a mapped stream produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(2.5, 3.7)
     *       .flatMapToFloat(d -> FloatStream.of((float) d, (float) (d * 2)))
     *       .toArray();   // returns [2.5f, 5.0f, 3.7f, 7.4f]
     *
     * // Generate float values for each double
     * DoubleStream.of(10.0, 20.0)
     *       .flatMapToFloat(d -> FloatStream.of((float) d, (float) d / 2))
     *       .toArray();   // returns [10.0f, 5.0f, 20.0f, 10.0f]
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
     * Each non-null mapped stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a FloatStream
     * @return a new FloatStream consisting of the flattened contents of all mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapToInt(DoubleFunction)
     * @see #flatMapToLong(DoubleFunction)
     * @see #flatMapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream flatMapToFloat(DoubleFunction<? extends FloatStream> mapper);

    /**
     * Returns an object-valued {@code Stream} consisting of the results of replacing each element of this stream
     * with the contents of a mapped stream produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.5, 2.7)
     *       .flatMapToObj(d -> Stream.of(String.valueOf(d), String.valueOf(d * 2)))
     *       .collect(Collectors.toList());   // returns ["1.5", "3.0", "2.7", "5.4"]
     *
     * // Generate multiple objects for each double
     * DoubleStream.of(10.0, 20.0)
     *       .flatMapToObj(price -> Stream.of(
     *           new Product("Item", price),
     *           new Product("Item with tax", price * 1.1)))
     *       .forEach(System.out::println);
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
     * Each non-null mapped stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * @param mapper a non-interfering, stateless function that transforms each element to a Stream
     * @return a new Stream consisting of the flattened contents of all mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapToInt(DoubleFunction)
     * @see #flatMapToLong(DoubleFunction)
     * @see #flatmapToObj(DoubleFunction)
     * @see #mapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapToObj(DoubleFunction<? extends Stream<? extends T>> mapper);

    /**
     * Returns an object-valued {@code Stream} consisting of the results of replacing each element of this stream
     * with the contents of a mapped collection produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.5, 2.7)
     *       .flatmapToObj(d -> Arrays.asList(String.valueOf(d), String.valueOf(d * 2)))
     *       .collect(Collectors.toList());   // returns ["1.5", "3.0", "2.7", "5.4"]
     *
     * // Generate list of objects for each double
     * DoubleStream.of(10.0, 20.0)
     *       .flatmapToObj(price -> List.of(
     *           "Price: $" + price,
     *           "With Tax: $" + (price * 1.1)))
     *       .forEach(System.out::println);
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
     * @param mapper a non-interfering, stateless function that transforms each element to a Collection
     * @return a new Stream consisting of the flattened contents of all mapped collections
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapToObj(DoubleFunction)
     * @see #flatMapArrayToObj(DoubleFunction)
     * @see #mapToObj(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatmapToObj(DoubleFunction<? extends Collection<? extends T>> mapper); //NOSONAR

    /**
     * Returns an object-valued {@code Stream} consisting of the results of replacing each element of this stream
     * with the contents of a mapped array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.5, 2.7)
     *       .flatMapArrayToObj(d -> new String[]{String.valueOf(d), String.valueOf(d * 2)})
     *       .collect(Collectors.toList());   // returns ["1.5", "3.0", "2.7", "5.4"]
     *
     * // Generate array of objects for each double
     * DoubleStream.of(10.0, 20.0)
     *       .flatMapArrayToObj(price -> new String[]{
     *           "Price: $" + price,
     *           "With Tax: $" + (price * 1.1)})
     *       .forEach(System.out::println);
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
     * @param mapper a non-interfering, stateless function that transforms each element to a {@code T[]}
     * @return a new Stream consisting of the flattened contents of all mapped arrays
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapToObj(DoubleFunction)
     * @see #flatmapToObj(DoubleFunction)
     * @see #mapToObj(DoubleFunction)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapArrayToObj(DoubleFunction<T[]> mapper);

    /**
     * Returns a stream consisting of the results of applying the given multi-mapping function to the elements of this stream.
     * The mapper function accepts a DoubleConsumer which can be invoked multiple times to accept multiple values.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0)
     *       .mapMulti((d, consumer) -> {
     *           consumer.accept(d);
     *           consumer.accept(d * 2);
     *       })
     *       .toArray();   // returns [1.0, 2.0, 2.0, 4.0, 3.0, 6.0]
     *
     * // Conditional multi-mapping
     * DoubleStream.of(1.5, 2.5, 3.5)
     *       .mapMulti((d, consumer) -> {
     *           consumer.accept(d);
     *           if (d > 2.0) {
     *               consumer.accept(d * 10);
     *           }
     *       })
     *       .toArray();   // returns [1.5, 2.5, 25.0, 3.5, 35.0]
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
     * @param mapper a non-interfering, stateless function that generates zero or more output values for each input value
     * @return a new DoubleStream consisting of the results of applying the mapper function
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(DoubleFunction)
     * @see #flatMapArray(DoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream mapMulti(DoubleMapMultiConsumer mapper);

    /**
     * Returns a stream consisting of the non-empty results of applying the given function to the elements of this stream.
     * If the mapper function returns an empty {@code OptionalDouble} for an element, that element is not included in the result stream.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.</p>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter and transform in one step - only include positive square roots
     * DoubleStream.of(-4.0, 16.0, -9.0, 25.0)
     *       .mapPartial(d -> d >= 0 ? OptionalDouble.of(Math.sqrt(d)) : OptionalDouble.empty())
     *       .toArray();   // returns [4.0, 5.0]
     *
     * // Safe division - skip division by zero
     * DoubleStream.of(10.0, 0.0, 20.0, 5.0)
     *       .mapPartial(d -> d != 0 ? OptionalDouble.of(100.0 / d) : OptionalDouble.empty())
     *       .toArray();   // returns [10.0, 5.0, 20.0]
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
     * @param mapper a non-interfering, stateless function that transforms each element to an OptionalDouble
     * @return a new stream containing only the values from non-empty OptionalDoubles
     * @throws IllegalStateException if the stream is already closed
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream mapPartial(DoubleFunction<OptionalDouble> mapper);

    /**
     * Returns a stream consisting of the non-empty results of applying the given function to the elements of this stream.
     * If the mapper function returns an empty {@code java.util.OptionalDouble} for an element, that element is not included in the result stream.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.</p>
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter and transform using JDK OptionalDouble
     * DoubleStream.of(-4.0, 16.0, -9.0, 25.0)
     *       .mapPartialJdk(d -> d >= 0 ? java.util.OptionalDouble.of(Math.sqrt(d)) : java.util.OptionalDouble.empty())
     *       .toArray();   // returns [4.0, 5.0]
     *
     * // Integration with JDK stream operations
     * DoubleStream.of(10.0, 0.0, 20.0, 5.0)
     *       .mapPartialJdk(d -> d != 0 ? java.util.OptionalDouble.of(100.0 / d) : java.util.OptionalDouble.empty())
     *       .toArray();   // returns [10.0, 5.0, 20.0]
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
     * @param mapper a non-interfering, stateless function that transforms each element to a JDK {@code java.util.OptionalDouble}
     * @return a new stream containing only the values from non-empty {@code java.util.OptionalDouble}s
     * @throws IllegalStateException if the stream is already closed
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream mapPartialJdk(DoubleFunction<java.util.OptionalDouble> mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the first and last element of the ranges in this stream,
     * where elements in a range are determined by the sameRange predicate.
     * The first argument tested by sameRange is the first element of the current range, and the second argument is the next element to check.
     * If {@code true} is returned, the next element belongs to the same range as the first element.
     *
     * <p>
     * For example, if this stream contains elements [1, 2, 3, 10, 11, 20, 21] and the sameRange predicate returns true
     * when the difference between elements is less than 2, the stream will map ranges [1,2], [3,3], [10,11], [20,21].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map consecutive ranges to their sums
     * DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0, 20.0, 21.0)
     *       .rangeMap((first, next) -> next - first < 2, (first, last) -> first + last)
     *       .toArray();   // returns [3.0, 6.0, 21.0, 41.0] (ranges: [1,2], [3], [10,11], [20,21])
     *
     * // Calculate range differences
     * DoubleStream.of(1.0, 1.5, 2.0, 5.0, 5.5, 10.0)
     *       .rangeMap((first, next) -> next - first <= 1, (first, last) -> last - first)
     *       .toArray();   // returns [1.0, 0.5, 0.0] (range differences)
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
    public abstract DoubleStream rangeMap(final DoubleBiPredicate sameRange, final DoubleBinaryOperator mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the first and last element of the ranges in this stream,
     * where elements in a range are determined by the sameRange predicate.
     * The first argument tested by sameRange is the first element of the current range, and the second argument is the next element to check.
     * If {@code true} is returned, the next element belongs to the same range as the first element.
     *
     * <p>
     * For example, if this stream contains elements [1, 2, 3, 10, 11, 20, 21] and the sameRange predicate returns true
     * when the difference between elements is less than 2, the stream will map ranges [1,2], [3,3], [10,11], [20,21] to objects
     * using the mapper function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map ranges to String descriptions
     * DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0, 20.0)
     *       .rangeMapToObj((first, next) -> next - first < 2,
     *                      (first, last) -> String.format("[%.1f-%.1f]", first, last))
     *       .collect(Collectors.toList());   // returns ["[1.0-2.0]", "[3.0-3.0]", "[10.0-11.0]", "[20.0-20.0]"]
     *
     * // Create custom objects from ranges
     * DoubleStream.of(1.5, 2.5, 5.0, 6.0, 10.0)
     *       .rangeMapToObj((first, next) -> next - first <= 1.5,
     *                      (first, last) -> new Range(first, last))
     *       .forEach(System.out::println);
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
    public abstract <T> Stream<T> rangeMapToObj(final DoubleBiPredicate sameRange, final DoubleBiFunction<? extends T> mapper);

    /**
     * Collapses consecutive elements in the stream into groups based on a predicate.
     * Elements for which the predicate returns {@code true} when applied to adjacent elements are grouped together into lists.
     *
     * <p>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * the resulting stream will contain [[1, 2], [5, 6, 7], [10]].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group consecutive numbers with difference < 3
     * DoubleStream.of(1.0, 2.0, 5.0, 6.0, 7.0, 10.0)
     *       .collapse((last, next) -> next - last < 3)
     *       .forEach(System.out::println);
     * // Output: [1.0, 2.0], [5.0, 6.0, 7.0], [10.0]
     *
     * // Group similar values (within 0.1 tolerance)
     * DoubleStream.of(1.0, 1.05, 1.08, 2.0, 2.02, 5.0)
     *       .collapse((last, next) -> Math.abs(next - last) < 0.1)
     *       .map(DoubleList::average)
     *       .forEach(System.out::println);   // prints average of each group
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
    public abstract Stream<DoubleList> collapse(final DoubleBiPredicate collapsible);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * When adjacent elements satisfy the collapsible predicate, they are merged using the provided merge function.
     *
     * <p>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * and the merge function sums the elements, the resulting stream will contain [3, 18, 10].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum consecutive numbers with difference < 3
     * DoubleStream.of(1.0, 2.0, 5.0, 6.0, 7.0, 10.0)
     *       .collapse((last, next) -> next - last < 3, Double::sum)
     *       .toArray();   // returns [3.0, 18.0, 10.0]
     *
     * // Find max in each group of similar values
     * DoubleStream.of(1.0, 1.5, 2.0, 5.0, 5.2, 5.1, 10.0)
     *       .collapse((last, next) -> Math.abs(next - last) < 1.0, Math::max)
     *       .toArray();   // returns [2.0, 5.2, 10.0]
     *
     * // Calculate average of consecutive groups
     * DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0)
     *       .collapse((last, next) -> next - last < 2, (a, b) -> (a + b) / 2)
     *       .toArray();   // returns [2.25, 10.5]
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
    public abstract DoubleStream collapse(final DoubleBiPredicate collapsible, final DoubleBinaryOperator mergeFunction);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * Elements for which the predicate returns {@code true} when applied with the first and last elements of the current group and the next element are merged together.
     * The collapsible predicate takes three elements: the first and last elements of the group, and the next element in the stream.
     *
     * <p>
     * For example, if this stream contains elements [1.0, 2.0, 3.0, 10.0, 11.0, 20.0] and the predicate
     * tests whether the difference between the first element of the current group and the next element is less than 5,
     * and the merge function sums the elements, the resulting stream will contain [6.0, 21.0, 20.0].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse with range-based condition (first to next difference < 5)
     * DoubleStream.of(1.0, 2.0, 3.0, 10.0, 11.0, 20.0)
     *       .collapse((first, last, next) -> next - first < 5, Double::sum)
     *       .toArray();   // returns [6.0, 21.0, 20.0]
     *
     * // Group values within a total range
     * DoubleStream.of(1.0, 1.5, 2.0, 10.0, 10.5, 20.0)
     *       .collapse((first, last, next) -> next - first < 2.0, Math::max)
     *       .toArray();   // returns maximum values in each range
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
     * @see Stream#collapse(BiPredicate, BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream collapse(final DoubleTriPredicate collapsible, final DoubleBinaryOperator mergeFunction);

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
     * // Calculate running sum (cumulative sum)
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0)
     *       .scan(Double::sum)
     *       .toArray();   // returns [1.0, 3.0, 6.0, 10.0]
     *
     * // Calculate running product
     * DoubleStream.of(2.0, 3.0, 4.0)
     *       .scan((a, b) -> a * b)
     *       .toArray();   // returns [2.0, 6.0, 24.0]
     *
     * // Running maximum
     * DoubleStream.of(3.0, 1.0, 4.0, 1.0, 5.0, 9.0)
     *       .scan(Math::max)
     *       .toArray();   // returns [3.0, 3.0, 4.0, 4.0, 5.0, 9.0]
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
     * @param accumulator a {@code DoubleBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code DoubleStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream scan(final DoubleBinaryOperator accumulator);

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
     * // Calculate running sum starting from 10
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0)
     *       .scan(10.0, Double::sum)
     *       .toArray();   // returns [11.0, 13.0, 16.0, 20.0]
     *
     * // Running product starting from 2
     * DoubleStream.of(3.0, 4.0, 5.0)
     *       .scan(2.0, (a, b) -> a * b)
     *       .toArray();   // returns [6.0, 24.0, 120.0]
     *
     * // Account balance tracking
     * DoubleStream.of(100.0, -50.0, 200.0, -30.0)
     *       .scan(1000.0, Double::sum)
     *       .toArray();   // returns [1100.0, 1050.0, 1250.0, 1220.0]
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
     * @param accumulator a {@code DoubleBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code DoubleStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(Object, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream scan(final double init, final DoubleBinaryOperator accumulator);

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
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0)
     *       .scan(10.0, true, Double::sum)
     *       .toArray();   // returns [10.0, 11.0, 13.0, 16.0, 20.0]
     *
     * // Exclude initial value (same as scan(init, accumulator))
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0)
     *       .scan(10.0, false, Double::sum)
     *       .toArray();   // returns [11.0, 13.0, 16.0, 20.0]
     *
     * // Balance tracking with initial balance shown
     * DoubleStream.of(100.0, -50.0, 200.0)
     *       .scan(1000.0, true, Double::sum)
     *       .toArray();   // returns [1000.0, 1100.0, 1050.0, 1250.0]
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
     * @param accumulator a {@code DoubleBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code DoubleStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(Object, boolean, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream scan(final double init, final boolean initIncluded, final DoubleBinaryOperator accumulator);

    /**
     * Returns a stream consisting of the specified elements followed by the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(3.0, 4.0, 5.0)
     *       .prepend(1.0, 2.0)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * // Add header values
     * DoubleStream.of(10.5, 20.3, 30.7)
     *       .prepend(0.0)
     *       .forEach(System.out::println);
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
    public abstract DoubleStream prepend(final double... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0)
     *       .append(4.0, 5.0)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * // Add footer values
     * DoubleStream.of(10.5, 20.3, 30.7)
     *       .append(0.0)
     *       .forEach(System.out::println);
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
    public abstract DoubleStream append(final double... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended if this stream is empty.
     * If this stream is not empty, returns this stream unchanged.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Non-empty stream remains unchanged
     * DoubleStream.of(1.0, 2.0, 3.0)
     *       .appendIfEmpty(99.0)
     *       .toArray();   // returns [1.0, 2.0, 3.0]
     *
     * // Empty stream gets default values
     * DoubleStream.empty()
     *       .appendIfEmpty(0.0, 0.0)
     *       .toArray();   // returns [0.0, 0.0]
     *
     * // Provide fallback data
     * DoubleStream.of(dataArray)
     *       .filter(d -> d > 0)
     *       .appendIfEmpty(-1.0) // -1.0 if no positive values
     *       .forEach(System.out::println);
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
    public abstract DoubleStream appendIfEmpty(final double... a);

    /**
     * Returns a DoubleStream consisting of the top n elements of this stream, according to the natural order of the elements.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p>This method only runs sequentially, even in parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top 3 largest values
     * DoubleStream.of(5.0, 2.0, 8.0, 1.0, 9.0, 3.0)
     *       .top(3)
     *       .toArray();   // returns [9.0, 8.0, 5.0] (order not guaranteed)
     *
     * // Get top values from measurements
     * DoubleStream.of(temperatureReadings)
     *       .top(5)
     *       .average();   // returns average of top 5 temperatures
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
     * @param n the number of elements to select
     * @return a new stream
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream top(int n);

    /**
     * Returns a DoubleStream consisting of the top n elements of this stream compared by the provided Comparator.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p>This method only runs sequentially, even in parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get top 3 smallest values (reverse order)
     * DoubleStream.of(5.0, 2.0, 8.0, 1.0, 9.0, 3.0)
     *       .top(3, Comparator.reverseOrder())
     *       .toArray();   // returns [1.0, 2.0, 3.0] (order not guaranteed)
     *
     * // Get farthest values from a target
     * double target = 5.0;
     * DoubleStream.of(1.0, 4.5, 7.0, 5.2, 3.0, 6.0)
     *       .top(3, Comparator.comparingDouble(d -> Math.abs(d - target)))
     *       .forEach(System.out::println);   // prints farthest 3 from 5.0 (largest |d - 5.0|)
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
     * @param n the number of elements to select
     * @param comparator a non-interfering, stateless comparator to compare elements of this stream; if {@code null}, natural ordering is used (the {@code n} greatest elements are returned)
     * @return a new stream
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream top(final int n, Comparator<? super Double> comparator);

    /**
     * Returns a {@code DoubleList} containing all the elements of this stream.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleList list = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
     *       .filter(d -> d > 2.0)
     *       .toDoubleList();   // returns DoubleList[3.0, 4.0, 5.0]
     *
     * // Collect transformed results
     * DoubleList squares = IntStream.range(1, 6)
     *       .asDoubleStream()
     *       .map(d -> d * d)
     *       .toDoubleList();
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
     * @return a {@code DoubleList} containing all the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract DoubleList toDoubleList();

    /**
     * Returns a {@code Map} where the keys and values are the results of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * an {@code IllegalStateException} is thrown when the collection operation is performed.
     * If the mapped keys may have duplicates, use {@link #toMap(Throwables.DoubleFunction, Throwables.DoubleFunction, BinaryOperator)} instead.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map index to value
     * Map<Integer, Double> map = DoubleStream.of(1.5, 2.7, 3.2)
     *       .toMap(d -> (int) d, d -> d * 2);
     * // Result: {1=3.0, 2=5.4, 3=6.4}
     *
     * // Create lookup map
     * Map<String, Double> grades = DoubleStream.of(85.5, 92.3, 78.9)
     *       .toMap(d -> "Grade-" + (int) d, d -> d);
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
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a non-interfering, stateless function to apply to each element to derive the key
     * @param valueMapper a non-interfering, stateless function to apply to each element to derive the value
     * @return a {@code Map} whose keys and values are the results of applying the mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.DoubleFunction<? extends K, E> keyMapper,
            Throwables.DoubleFunction<? extends V, E2> valueMapper) throws E, E2;

    /**
     * Returns a {@code Map} where the keys and values are the results of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * an {@code IllegalStateException} is thrown when the collection operation is performed.
     * If the mapped keys may have duplicates, use {@link #toMap(Throwables.DoubleFunction, Throwables.DoubleFunction, BinaryOperator, Supplier)} instead.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create LinkedHashMap to maintain insertion order
     * LinkedHashMap<Integer, String> orderedMap = DoubleStream.of(3.5, 1.2, 2.7)
     *       .toMap(d -> (int) d, d -> "Value: " + d, LinkedHashMap::new);
     * // Result: {3=Value: 3.5, 1=Value: 1.2, 2=Value: 2.7} (insertion order preserved)
     *
     * // Create TreeMap for sorted keys
     * TreeMap<String, Double> sortedMap = DoubleStream.of(85.5, 92.3, 78.9)
     *       .toMap(d -> "Score-" + (int) d, d -> d, TreeMap::new);
     * // Keys sorted alphabetically: Score-78, Score-85, Score-92
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
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a non-interfering, stateless function to apply to each element to derive the key
     * @param valueMapper a non-interfering, stateless function to apply to each element to derive the value
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} whose keys and values are the results of applying the mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.DoubleFunction<? extends K, E> keyMapper,
            Throwables.DoubleFunction<? extends V, E2> valueMapper, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Returns a {@code Map} where the keys and values are the results of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * the values are merged using the provided merging function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by category (rounded value) and sum duplicates
     * Map<Integer, Double> sumByCategory = DoubleStream.of(1.5, 1.7, 2.3, 2.8, 3.1)
     *       .toMap(d -> (int) d, d -> d, Double::sum);
     * // Result: {1=3.2, 2=5.1, 3=3.1}
     *
     * // Keep first value when duplicates occur
     * Map<String, Double> firstValues = DoubleStream.of(85.5, 92.3, 85.7, 78.9)
     *       .toMap(d -> "Range-" + ((int) d / 10) * 10, d -> d, (v1, v2) -> v1);
     * // Result: {Range-80=85.5, Range-90=92.3, Range-70=78.9}
     *
     * // Keep maximum value for duplicates
     * Map<Integer, Double> maxValues = DoubleStream.of(1.5, 1.9, 2.3, 1.2)
     *       .toMap(d -> (int) d, d -> d, Math::max);
     * // Result: {1=1.9, 2=2.3}
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
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a non-interfering, stateless function to apply to each element to derive the key
     * @param valueMapper a non-interfering, stateless function to apply to each element to derive the value
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @return a {@code Map} whose keys and values are the results of applying the mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.DoubleFunction<? extends K, E> keyMapper,
            Throwables.DoubleFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction) throws E, E2;

    /**
     * Returns a {@code Map} where the keys and values are the results of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * the values are merged using the provided merging function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create sorted map with duplicate handling
     * TreeMap<Integer, Double> sortedSums = DoubleStream.of(1.5, 1.7, 2.3, 2.8, 3.1)
     *       .toMap(d -> (int) d, d -> d, Double::sum, TreeMap::new);
     * // Result: TreeMap with sorted keys {1=3.2, 2=5.1, 3=3.1}
     *
     * // Create LinkedHashMap maintaining insertion order with max values
     * LinkedHashMap<String, Double> orderedMax = DoubleStream.of(85.5, 92.3, 85.7, 78.9, 92.8)
     *       .toMap(d -> "Range-" + ((int) d / 10) * 10,
     *              d -> d,
     *              Math::max,
     *              LinkedHashMap::new);
     * // Result: {Range-80=85.7, Range-90=92.8, Range-70=78.9} (insertion order preserved)
     *
     * // Create concurrent map for parallel processing
     * ConcurrentHashMap<Integer, Double> concurrentSums = DoubleStream.of(1.5, 1.7, 2.3, 2.8)
     *       .parallel()
     *       .toMap(d -> (int) d, d -> d, Double::sum, ConcurrentHashMap::new);
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
     * @param <K> the type of keys
     * @param <V> the type of values
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the key mapping function
     * @param <E2> the type of exception thrown by the value mapping function
     * @param keyMapper a non-interfering, stateless function to apply to each element to derive the key
     * @param valueMapper a non-interfering, stateless function to apply to each element to derive the value
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} whose keys and values are the results of applying the mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.DoubleFunction<? extends K, E> keyMapper,
            Throwables.DoubleFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Groups the elements of this stream according to a classification function and collects the results
     * using the specified downstream collector.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by range and count elements in each range
     * Map<String, Long> countsByRange = DoubleStream.of(1.5, 2.7, 3.2, 1.9, 2.1, 3.8)
     *       .groupTo(d -> "Range-" + ((int) d / 10) * 10, Collectors.counting());
     * // Result: {Range-0=6}
     *
     * // Group by integer part and collect to list
     * Map<Integer, List<Double>> grouped = DoubleStream.of(1.5, 2.7, 1.9, 3.2, 2.1)
     *       .groupTo(d -> (int) d, Collectors.toList());
     * // Result: {1=[1.5, 1.9], 2=[2.7, 2.1], 3=[3.2]}
     *
     * // Group and calculate average per group
     * Map<String, Double> averages = DoubleStream.of(85.5, 92.3, 78.9, 88.7, 95.1)
     *       .groupTo(d -> d >= 90 ? "A" : (d >= 80 ? "B" : "C"),
     *                Collectors.averagingDouble(Double::doubleValue));
     * // Result: {A=93.7, B=87.1, C=78.9}
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
     * @param <K> the type of keys
     * @param <D> the result type of the downstream reduction
     * @param <E> the type of exception thrown by the classification function
     * @param keyMapper a non-interfering, stateless function to be used for classifying elements
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @return a {@code Map} containing the results of the group-by operation
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the classification function throws an exception
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, E extends Exception> Map<K, D> groupTo(Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Collector<? super Double, ?, D> downstream) throws E;

    /**
     * Groups the elements of this stream according to a classification function and collects the results
     * using the specified downstream collector into the specified map factory.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group into sorted TreeMap with lists
     * TreeMap<Integer, List<Double>> sortedGroups = DoubleStream.of(1.5, 2.7, 1.9, 3.2, 2.1)
     *       .groupTo(d -> (int) d, Collectors.toList(), TreeMap::new);
     * // Result: TreeMap with sorted keys {1=[1.5, 1.9], 2=[2.7, 2.1], 3=[3.2]}
     *
     * // Group into LinkedHashMap preserving insertion order with sums
     * LinkedHashMap<String, Double> orderedSums = DoubleStream.of(85.5, 92.3, 78.9, 88.7)
     *       .groupTo(d -> d >= 90 ? "Excellent" : (d >= 80 ? "Good" : "Fair"),
     *                Collectors.summingDouble(Double::doubleValue),
     *                LinkedHashMap::new);
     * // Result: {Good=174.2, Excellent=92.3, Fair=78.9} (insertion order preserved)
     *
     * // Group with concurrent map for parallel processing
     * ConcurrentHashMap<Integer, Long> concurrentCounts = DoubleStream.of(1.5, 2.7, 1.9, 2.1)
     *       .parallel()
     *       .groupTo(d -> (int) d, Collectors.counting(), ConcurrentHashMap::new);
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
     * @param <K> the type of keys
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting {@code Map}
     * @param <E> the type of exception thrown by the classification function
     * @param keyMapper a non-interfering, stateless function to be used for classifying elements
     * @param downstream a {@code Collector} implementing the downstream reduction
     * @param mapFactory a supplier providing a new empty {@code Map} into which the results will be inserted
     * @return a {@code Map} containing the results of the group-by operation
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the classification function throws an exception
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, M extends Map<K, D>, E extends Exception> M groupTo(Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Collector<? super Double, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E;

    /**
     * Performs a reduction on the elements of this stream, using the provided identity value and
     * accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current accumulated value (starting from
     * {@code identity}) and the current stream element.
     *
     * <p>If the stream is empty, {@code identity} is returned. The {@code identity} value must be
     * an identity for the accumulator function, i.e., for all {@code t},
     * {@code accumulator.applyAsDouble(identity, t) == t}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum all elements starting from 0
     * double sum = DoubleStream.of(1.0, 2.0, 3.0, 4.0)
     *       .reduce(0.0, Double::sum);   // returns 10.0
     *
     * // Empty stream returns identity
     * double emptySum = DoubleStream.empty().reduce(0.0, Double::sum);   // returns 0.0
     *
     * // Product of all elements
     * double product = DoubleStream.of(2.0, 3.0, 4.0)
     *       .reduce(1.0, (a, b) -> a * b);   // returns 24.0
     *
     * // Find maximum with initial value
     * double max = DoubleStream.of(5.0, 2.0, 8.0, 1.0)
     *       .reduce(Double.NEGATIVE_INFINITY, Math::max);   // returns 8.0
     *
     * // NaN propagation
     * double nanReduce = DoubleStream.of(1.0, Double.NaN, 3.0).reduce(0.0, Double::sum);
     * assertTrue(Double.isNaN(nanReduce));   // result is NaN, propagated through reduction
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
    public abstract double reduce(double identity, DoubleBinaryOperator accumulator);

    /**
     * Performs a reduction on the elements of this stream, using the provided accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current reduced value and the current stream element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum all elements
     * OptionalDouble sum = DoubleStream.of(1.0, 2.0, 3.0, 4.0)
     *       .reduce(Double::sum);   // returns OptionalDouble[10.0]
     *
     * // Product of all elements
     * OptionalDouble product = DoubleStream.of(2.0, 3.0, 4.0)
     *       .reduce((a, b) -> a * b);   // returns OptionalDouble[24.0]
     *
     * // Empty stream returns empty Optional
     * OptionalDouble empty = DoubleStream.empty()
     *       .reduce(Double::sum);   // returns OptionalDouble.empty()
     *
     * // Single element stream returns that element
     * OptionalDouble single = DoubleStream.of(42.0)
     *       .reduce(Double::sum);   // returns OptionalDouble[42.0]
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
     * @return an OptionalDouble describing the result of the reduction. If the stream is empty, an empty {@code OptionalDouble} is returned.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#reduce(BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract OptionalDouble reduce(DoubleBinaryOperator accumulator);

    /**
     * Performs a mutable reduction operation on the elements of this stream using the provided supplier, accumulator, and combiner.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into DoubleList
     * DoubleList list = DoubleStream.of(1.5, 2.7, 3.2, 4.9)
     *       .collect(DoubleList::new, DoubleList::add, DoubleList::addAll);
     * // Result: [1.5, 2.7, 3.2, 4.9]
     *
     * // Collect into ArrayList<Double>
     * ArrayList<Double> arrayList = DoubleStream.of(1.5, 2.7, 3.2)
     *       .collect(ArrayList::new,
     *                ArrayList::add,
     *                ArrayList::addAll);
     *
     * // Collect into custom container
     * StringBuilder sb = DoubleStream.of(1.0, 2.0, 3.0)
     *       .collect(StringBuilder::new,
     *                (builder, d) -> builder.append(d).append(", "),
     *                StringBuilder::append);
     * // Result: "1.0, 2.0, 3.0, "
     *
     * // Parallel collection with combiner
     * Set<Double> set = DoubleStream.of(1.5, 2.7, 1.5, 3.2, 2.7)
     *       .parallel()
     *       .collect(HashSet::new, HashSet::add, HashSet::addAll);
     * // Result: {1.5, 2.7, 3.2}
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
    public abstract <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * Performs a mutable reduction operation on the elements of this stream using the provided supplier and accumulator.
     *
     * <br />
     * Only call this method when the returned type {@code R} is one of these types: {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * Otherwise, please call {@link #collect(Supplier, ObjDoubleConsumer, BiConsumer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into DoubleList (combiner automatically inferred)
     * DoubleList list = DoubleStream.of(1.5, 2.7, 3.2, 4.9)
     *       .collect(DoubleList::new, DoubleList::add);
     * // Result: [1.5, 2.7, 3.2, 4.9]
     *
     * // Collect into ArrayList (combiner automatically inferred)
     * ArrayList<Double> arrayList = DoubleStream.of(1.5, 2.7, 3.2)
     *       .collect(ArrayList::new, ArrayList::add);
     *
     * // Collect into HashSet (combiner automatically inferred)
     * HashSet<Double> set = DoubleStream.of(1.5, 2.7, 1.5, 3.2)
     *       .collect(HashSet::new, HashSet::add);
     * // Result: {1.5, 2.7, 3.2}
     *
     * // Collect into StringBuilder (combiner automatically inferred)
     * StringBuilder sb = DoubleStream.of(1.0, 2.0, 3.0)
     *       .collect(StringBuilder::new,
     *                (builder, d) -> builder.append(d).append(" "));
     * // Result: "1.0 2.0 3.0 "
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
     * @throws IllegalStateException if the stream is already closed
     * @throws RuntimeException if this stream is parallel and the result type {@code R} is not one of: {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}
     *         (the default combiner cannot merge the per-thread containers); sequential streams perform no such check.
     * @return the result of the reduction
     * @see #collect(Supplier, ObjDoubleConsumer, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<? super R> accumulator);

    /**
     * Performs an action for each element of this stream.
     * This is a convenience method that delegates to {@link #forEach(Throwables.DoubleConsumer)}.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
     *     .foreach(System.out::println);
     * // prints: 1.0, 2.0, 3.0, 4.0, 5.0 (each on a new line)
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
     * @param action a non-interfering action to perform on the elements
     * @throws IllegalStateException if the stream is already closed
     * @see #forEach(Throwables.DoubleConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public void foreach(final DoubleConsumer action) { // NOSONAR
        assertNotClosed();

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
     * DoubleStream.of(1.5, 2.5, 3.5, 4.5, 5.5)
     *     .forEach(System.out::println);
     * // prints: 1.5, 2.5, 3.5, 4.5, 5.5 (each on a new line)
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
    public abstract <E extends Exception> void forEach(final Throwables.DoubleConsumer<E> action) throws E;

    /**
     * Performs an action for each element of this stream, providing access to both the element and its index.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.1, 2.2, 3.3)
     *     .forEachIndexed((index, value) ->
     *         System.out.println("Element at index " + index + ": " + value));
     * // prints:
     * // Element at index 0: 1.1
     * // Element at index 1: 2.2
     * // Element at index 2: 3.3
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
     * @param action a non-interfering action to perform on the elements, accepting the index and the element
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEachIndexed(Throwables.IntDoubleConsumer<E> action) throws E;

    /**
     * Returns whether any elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if any element is greater than 5
     * boolean hasLarge = DoubleStream.of(1.5, 2.7, 3.2, 4.9)
     *       .anyMatch(d -> d > 5.0);   // returns false
     *
     * // Check if any element is negative
     * boolean hasNegative = DoubleStream.of(1.5, -2.7, 3.2)
     *       .anyMatch(d -> d < 0);   // returns true (short-circuits at -2.7)
     *
     * // Empty stream always returns false
     * boolean anyInEmpty = DoubleStream.empty()
     *       .anyMatch(d -> true);   // returns false
     *
     * // Check if any grade is passing
     * boolean hasPassing = DoubleStream.of(45.5, 55.3, 68.9)
     *       .anyMatch(grade -> grade >= 60.0);   // returns true
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
    public abstract <E extends Exception> boolean anyMatch(final Throwables.DoublePredicate<E> predicate) throws E;

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if all elements are positive
     * boolean allPositive = DoubleStream.of(1.5, 2.7, 3.2, 4.9)
     *       .allMatch(d -> d > 0);   // returns true
     *
     * // Check if all elements are greater than 5
     * boolean allLarge = DoubleStream.of(1.5, 6.7, 3.2)
     *       .allMatch(d -> d > 5.0);   // returns false (short-circuits at 1.5)
     *
     * // Empty stream always returns true
     * boolean allInEmpty = DoubleStream.empty()
     *       .allMatch(d -> false);   // returns true
     *
     * // Check if all grades are passing
     * boolean allPassing = DoubleStream.of(85.5, 92.3, 78.9, 88.7)
     *       .allMatch(grade -> grade >= 60.0);   // returns true
     *
     * // Validate all values are within range
     * boolean inRange = DoubleStream.of(0.5, 0.7, 0.9)
     *       .allMatch(d -> d >= 0.0 && d <= 1.0);   // returns true
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
    public abstract <E extends Exception> boolean allMatch(final Throwables.DoublePredicate<E> predicate) throws E;

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if no elements are negative
     * boolean noNegatives = DoubleStream.of(1.5, 2.7, 3.2, 4.9)
     *       .noneMatch(d -> d < 0);   // returns true
     *
     * // Check if no elements are greater than 10
     * boolean noneAboveTen = DoubleStream.of(1.5, 2.7, 11.2)
     *       .noneMatch(d -> d > 10.0);   // returns false (short-circuits at 11.2)
     *
     * // Empty stream always returns true
     * boolean noneInEmpty = DoubleStream.empty()
     *       .noneMatch(d -> true);   // returns true
     *
     * // Validate no grades are failing
     * boolean noFailures = DoubleStream.of(85.5, 92.3, 78.9, 88.7)
     *       .noneMatch(grade -> grade < 60.0);   // returns true
     *
     * // Check no NaN values present
     * boolean noNaN = DoubleStream.of(1.5, 2.7, 3.2)
     *       .noneMatch(Double::isNaN);   // returns true
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
    public abstract <E extends Exception> boolean noneMatch(final Throwables.DoublePredicate<E> predicate) throws E;

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalDouble}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalDouble first = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
     *                                 .findFirst();   // returns OptionalDouble[1.0]
     *
     * DoubleStream.empty().findFirst();                                         // returns OptionalDouble.empty()
     *
     * double result = DoubleStream.of(9.5, 8.3, 7.1).findFirst().orElse(0.0);   // returns 9.5
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
     * @return an {@code OptionalDouble} containing the first element of the stream, or an empty {@code OptionalDouble} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #first()
     * @see #findAny()
     * @see #findFirst(Throwables.DoublePredicate)
     * @see #findAny(Throwables.DoublePredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalDouble findFirst() {
        assertNotClosed();

        return first();
    }

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalDouble}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()} and {@link #findFirst()}; despite the
     * JDK-style name it returns the FIRST element, not an arbitrary one, even for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.
     * Unlike the general contract of {@code findAny}, this implementation always returns the first element,
     * even on parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalDouble any = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
     *                              .findAny();   // returns OptionalDouble[1.0]
     *
     * DoubleStream.empty().findAny();   // returns OptionalDouble.empty()
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
     * @return an {@code OptionalDouble} containing the first element of the stream, or an empty {@code OptionalDouble} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #first()
     * @see #findFirst()
     * @see #findFirst(Throwables.DoublePredicate)
     * @see #findAny(Throwables.DoublePredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalDouble findAny() {
        assertNotClosed();

        return first();
    }

    /**
     * Returns an {@code OptionalDouble} describing the first element of this stream that matches the given predicate,
     * or an empty {@code OptionalDouble} if no such element exists.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find first element greater than 5
     * OptionalDouble firstLarge = DoubleStream.of(1.5, 2.7, 6.2, 4.9, 7.1)
     *       .findFirst(d -> d > 5.0);   // returns OptionalDouble[6.2]
     *
     * // Find first negative element
     * OptionalDouble firstNegative = DoubleStream.of(1.5, 2.7, -3.2, -4.9)
     *       .findFirst(d -> d < 0);   // returns OptionalDouble[-3.2]
     *
     * // No matching element returns empty
     * OptionalDouble notFound = DoubleStream.of(1.5, 2.7, 3.2)
     *       .findFirst(d -> d > 10.0);   // returns OptionalDouble.empty()
     *
     * // Find first passing grade
     * OptionalDouble firstPassing = DoubleStream.of(45.5, 55.3, 68.9, 72.1)
     *       .findFirst(grade -> grade >= 60.0);   // returns OptionalDouble[68.9]
     *
     * // Safe retrieval with default value
     * double result = DoubleStream.of(1.5, 2.7, 3.2)
     *       .findFirst(d -> d > 2.0)
     *       .orElse(0.0);   // returns 2.7
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
     * @return an {@code OptionalDouble} describing the first element that matches the predicate, or an empty {@code OptionalDouble} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalDouble findFirst(final Throwables.DoublePredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalDouble} describing any element of this stream that matches the given predicate,
     * or an empty {@code OptionalDouble} if no such element exists.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is free to select any element
     * in the stream that matches the predicate. This is to allow for maximal performance in parallel operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find any element greater than 5 (sequential returns first match)
     * OptionalDouble anyLarge = DoubleStream.of(1.5, 2.7, 6.2, 4.9, 7.1)
     *       .findAny(d -> d > 5.0);   // returns OptionalDouble[6.2] in sequential
     *
     * // Parallel stream may return any matching element
     * OptionalDouble anyEven = DoubleStream.of(1.5, 2.0, 3.5, 4.0, 5.5)
     *       .parallel()
     *       .findAny(d -> d % 2 == 0);   // returns 2.0 or 4.0
     *
     * // No matching element returns empty
     * OptionalDouble notFound = DoubleStream.of(1.5, 2.7, 3.2)
     *       .findAny(d -> d > 10.0);   // returns OptionalDouble.empty()
     *
     * // Find any passing grade
     * OptionalDouble anyPassing = DoubleStream.of(45.5, 55.3, 68.9, 72.1)
     *       .findAny(grade -> grade >= 60.0);   // returns any grade >= 60.0
     *
     * // Use for existence check
     * boolean hasNegative = DoubleStream.of(1.5, -2.7, 3.2)
     *       .findAny(d -> d < 0)
     *       .isPresent();   // returns true
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
     * @return an {@code OptionalDouble} describing any element that matches the predicate, or an empty {@code OptionalDouble} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalDouble findAny(final Throwables.DoublePredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalDouble} describing the last element of this stream that matches the given predicate,
     * or an empty {@code OptionalDouble} if no such element exists.
     *
     * <p>Consider using: {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find last element greater than 5
     * OptionalDouble lastLarge = DoubleStream.of(1.5, 6.7, 3.2, 7.9, 4.1)
     *       .findLast(d -> d > 5.0);   // returns OptionalDouble[7.9]
     *
     * // Find last negative element
     * OptionalDouble lastNegative = DoubleStream.of(1.5, -2.7, 3.2, -4.9, 5.1)
     *       .findLast(d -> d < 0);   // returns OptionalDouble[-4.9]
     *
     * // No matching element returns empty
     * OptionalDouble notFound = DoubleStream.of(1.5, 2.7, 3.2)
     *       .findLast(d -> d > 10.0);   // returns OptionalDouble.empty()
     *
     * // Find last passing grade
     * OptionalDouble lastPassing = DoubleStream.of(45.5, 68.9, 55.3, 72.1, 58.7)
     *       .findLast(grade -> grade >= 60.0);   // returns OptionalDouble[72.1]
     *
     * // Better performance with reversed stream
     * OptionalDouble lastEven = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
     *       .reversed()
     *       .findFirst(d -> d % 2 == 0);   // returns OptionalDouble[4.0]
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
     * @return an {@code OptionalDouble} describing the last element that matches the predicate, or an empty {@code OptionalDouble} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalDouble findLast(final Throwables.DoublePredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalDouble} describing the minimum element of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p>If any element is {@code NaN}, the result is {@code NaN} wrapped in a present
     * {@code OptionalDouble}, because {@link Math#min(double, double)} propagates {@code NaN}.
     * An empty stream returns an empty {@code OptionalDouble}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(5.5, 2.3, 8.1, 1.7, 9.2).min();                          // returns OptionalDouble[1.7]
     *
     * DoubleStream.empty().min();                                              // returns OptionalDouble.empty()
     *
     * // Safe retrieval with default value
     * double minValue = DoubleStream.of(10.5, 20.3, 30.8).min().orElse(0.0);   // returns 10.5
     *
     * // NaN propagation: any NaN element causes the result to be NaN
     * double nanResult = DoubleStream.of(5.0, Double.NaN, 3.0).min().getAsDouble();
     * assertTrue(Double.isNaN(nanResult));
     *
     * // Negative zero is less than positive zero
     * double negZeroResult = DoubleStream.of(-0.0, 0.0).min().getAsDouble();
     * assertTrue(Double.compare(negZeroResult, -0.0) == 0);  // -0.0 is returned
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
     * @return an {@code OptionalDouble} containing the minimum element of this stream, or an empty optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalDouble min();

    /**
     * Returns an {@code OptionalDouble} describing the maximum element of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p>If any element is {@code NaN}, the result is {@code NaN} wrapped in a present
     * {@code OptionalDouble}, because {@link Math#max(double, double)} propagates {@code NaN}.
     * An empty stream returns an empty {@code OptionalDouble}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(5.5, 2.3, 8.1, 1.7, 9.2).max();                          // returns OptionalDouble[9.2]
     *
     * DoubleStream.empty().max();                                              // returns OptionalDouble.empty()
     *
     * // Safe retrieval with default value
     * double maxValue = DoubleStream.of(10.5, 20.3, 30.8).max().orElse(0.0);   // returns 30.8
     *
     * // NaN propagation: any NaN element causes the result to be NaN
     * double nanResult = DoubleStream.of(5.0, Double.NaN, 3.0).max().getAsDouble();
     * assertTrue(Double.isNaN(nanResult));
     *
     * // Positive infinity is larger than any finite value but less than NaN
     * double infResult = DoubleStream.of(1.0, Double.POSITIVE_INFINITY, 3.0).max().getAsDouble();
     * assertTrue(Double.isInfinite(infResult) && infResult > 0);  // returns POSITIVE_INFINITY
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
     * @return an {@code OptionalDouble} containing the maximum element of this stream, or an empty optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalDouble max();

    /**
     * Returns the <i>k-th</i> largest element in the stream, using natural ordering consistent
     * with {@link Double#compare(double, double)} (which considers {@code NaN} greater than any
     * other value, including positive infinity).
     * If the stream is empty or the count of elements is less than {@code k}, an empty {@code OptionalDouble} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the 2nd largest element
     * DoubleStream.of(10.5, 30.2, 20.8, 50.1, 40.7).kthLargest(2);   // returns OptionalDouble[40.7]
     *
     * // Find the largest element (same as max)
     * DoubleStream.of(5.5, 2.3, 8.1, 1.7).kthLargest(1);   // returns OptionalDouble[8.1]
     *
     * // When k exceeds stream size
     * DoubleStream.of(1.5, 2.3, 3.8).kthLargest(5);   // returns OptionalDouble.empty()
     *
     * // Empty stream
     * DoubleStream.empty().kthLargest(1);   // returns OptionalDouble.empty()
     *
     * // NaN is considered larger than any other value (including positive infinity)
     * double nanKth = DoubleStream.of(1.0, Double.NaN, 5.0, 3.0).kthLargest(1).getAsDouble();
     * assertTrue(Double.isNaN(nanKth));
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
     * @return an {@code OptionalDouble} containing the k-th largest element, or an empty {@code OptionalDouble} if the stream is empty or contains fewer than {@code k} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalDouble kthLargest(int k);

    /**
     * Returns the sum of elements in this stream.
     *
     * <p>This is a terminal operation.
     *
     * <p>If any element is {@code NaN}, the result is {@code NaN}. If the stream is empty,
     * {@code 0.0} is returned. The sum is computed using compensated summation for improved
     * numerical accuracy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double total = DoubleStream.of(1.5, 2.3, 3.7, 4.2, 5.8).sum();   // returns 17.5
     *
     * DoubleStream.empty().sum();                                      // returns 0.0
     *
     * // NaN propagation
     * double nanSum = DoubleStream.of(1.0, Double.NaN, 3.0).sum();
     * assertTrue(Double.isNaN(nanSum));   // result is NaN when any element is NaN
     *
     * // Infinity handling
     * double infSum = DoubleStream.of(1.0, Double.POSITIVE_INFINITY, 3.0).sum();
     * assertTrue(Double.isInfinite(infSum) && infSum > 0);  // returns POSITIVE_INFINITY
     *
     * // Single element
     * double single = DoubleStream.of(42.0).sum();   // returns 42.0
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
     * @return the sum of elements in this stream, or {@code 0.0} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract double sum();

    /**
     * Returns an OptionalDouble describing the arithmetic mean of elements of this stream,
     * or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p>If any element is {@code NaN}, the result is {@code NaN} wrapped in a present
     * {@code OptionalDouble}. If the stream is empty, an empty {@code OptionalDouble} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).average();   // returns OptionalDouble[3.0]
     *
     * DoubleStream.of(10.5, 20.3, 30.8).average();          // returns OptionalDouble[20.533333...]
     *
     * DoubleStream.empty().average();                       // returns OptionalDouble.empty()
     *
     * // NaN propagation
     * double nanAvg = DoubleStream.of(1.0, Double.NaN, 3.0).average().getAsDouble();
     * assertTrue(Double.isNaN(nanAvg));
     *
     * // Safe retrieval with default value
     * double avg = DoubleStream.of(98.5, 87.3).average().orElse(0.0);   // returns 92.9
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
     * @return an OptionalDouble containing the average of elements of this stream,
     *         or an empty optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalDouble average();

    /**
     * Returns a {@code DoubleSummaryStatistics} describing various summary data about the elements of this stream.
     * The statistics include count, sum, min, max, and average.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleSummaryStatistics stats = DoubleStream.of(1.5, 2.3, 3.7, 4.2, 5.8).summaryStatistics();
     * System.out.println("Count: " + stats.getCount());       // prints Count: 5
     * System.out.println("Sum: " + stats.getSum());           // prints Sum: 17.5
     * System.out.println("Min: " + stats.getMin());           // prints Min: 1.5
     * System.out.println("Max: " + stats.getMax());           // prints Max: 5.8
     * System.out.println("Average: " + stats.getAverage());   // prints Average: 3.5
     *
     * // Empty stream returns count=0, sum=0
     * DoubleSummaryStatistics emptyStats = DoubleStream.empty().summaryStatistics();
     * System.out.println(emptyStats.getCount());   // 0
     *
     * // NaN propagation in statistics
     * DoubleSummaryStatistics nanStats = DoubleStream.of(1.0, Double.NaN, 3.0).summaryStatistics();
     * assertTrue(Double.isNaN(nanStats.getAverage()));   // average is NaN, propagated
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
     * @return a {@code DoubleSummaryStatistics} describing various summary data about the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract DoubleSummaryStatistics summaryStatistics();

    /**
     * Returns a pair consisting of DoubleSummaryStatistics for the elements of this stream,
     * and an Optional containing a map of percentiles if the stream is not empty.
     * To calculate the percentiles of the elements in the stream, all elements will be loaded into memory and sorted if not yet.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p><b>Caution:</b> because every element is buffered into memory (and sorted) to compute the percentiles, this
     * operation is unsuitable for very large or unbounded streams and may exhaust available memory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> result =
     *       DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
     *             .summaryStatisticsAndPercentiles();
     *
     * DoubleSummaryStatistics stats = result.left();
     * System.out.println("Count: " + stats.getCount());       // 10
     * System.out.println("Average: " + stats.getAverage());   // 5.5
     *
     * result.right().ifPresent(percentiles -> {
     *     // Access percentile data if available
     *     percentiles.forEach((pct, val) ->
     *         System.out.println(pct + ": " + val));
     * });
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
     * @return a {@code Pair} containing the {@code DoubleSummaryStatistics} and an optional map of percentile values
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> summaryStatisticsAndPercentiles();

    /**
     * Merges this stream with another stream according to the provided {@code nextSelector} function.
     * The {@code nextSelector} determines which element to take next from the two streams.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted streams in ascending order
     * DoubleStream.of(1.0, 3.0, 5.0, 7.0)
     *       .mergeWith(DoubleStream.of(2.0, 4.0, 6.0, 8.0),
     *                  (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Merge with custom selection logic
     * DoubleStream.of(10.0, 20.0, 30.0)
     *       .mergeWith(DoubleStream.of(15.0, 25.0, 35.0),
     *                  (a, b) -> Math.abs(a) < Math.abs(b) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *       .forEach(System.out::println);
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
     * @param b the other stream to merge with this stream
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return the new merged stream
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream mergeWith(final DoubleStream b, final DoubleBiFunction<MergeResult> nextSelector);

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
     * DoubleStream.of(1.0, 2.0, 3.0)
     *             .zipWith(DoubleStream.of(10.0, 20.0, 30.0, 40.0),
     *                      (a, b) -> a + b)
     *             .toArray();   // returns [11.0, 22.0, 33.0]
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
     * @param b the DoubleStream to be combined with the current DoubleStream. Must be {@code non-null}.
     * @param zipFunction a DoubleBinaryOperator that determines the combination of elements in the combined DoubleStream. Must be {@code non-null}.
     * @return a new DoubleStream that is the result of combining the current DoubleStream with the given DoubleStream
     * @throws IllegalStateException if the stream is already closed
     * @see #zipWith(DoubleStream, double, double, DoubleBinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream zipWith(DoubleStream b, DoubleBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the shortest of the three streams.
     * If any stream is longer, its remaining elements are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0)
     *             .zipWith(DoubleStream.of(10.0, 20.0), DoubleStream.of(100.0, 200.0),
     *                      (a, b, c) -> a + b + c)
     *             .toArray();   // returns [111.0, 222.0]
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
     * @param b the second DoubleStream to be combined with the current DoubleStream. Will be closed along with this DoubleStream.
     * @param c the third DoubleStream to be combined with the current DoubleStream. Will be closed along with this DoubleStream.
     * @param zipFunction a DoubleTernaryOperator that determines the combination of elements in the combined DoubleStream. Must be {@code non-null}.
     * @return a new DoubleStream that is the result of combining the current DoubleStream with the given DoubleStreams
     * @throws IllegalStateException if the stream is already closed
     * @see #zipWith(DoubleStream, DoubleStream, double, double, double, DoubleTernaryOperator)
     * @see #zipWith(DoubleStream, DoubleBinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream zipWith(DoubleStream b, DoubleStream c, DoubleTernaryOperator zipFunction);

    /**
     * Zips this stream with the given stream using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from two streams pairwise.
     *
     * <p>The resulting stream will have the length of the longer of the two streams.
     * Default values are used when one stream runs out of elements before the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0)
     *             .zipWith(DoubleStream.of(10.0), 0.0, 0.0, (a, b) -> a + b)
     *             .toArray();   // returns [11.0, 2.0, 3.0]
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
     * @param b the DoubleStream to be combined with the current DoubleStream. Will be closed along with this DoubleStream.
     * @param valueForNoneA the default value to use for the current DoubleStream when it runs out of elements
     * @param valueForNoneB the default value to use for the given DoubleStream when it runs out of elements
     * @param zipFunction a DoubleBinaryOperator that determines the combination of elements in the combined DoubleStream. Must be {@code non-null}.
     * @return a new DoubleStream that is the result of combining the current DoubleStream with the given DoubleStream
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream zipWith(DoubleStream b, double valueForNoneA, double valueForNoneB, DoubleBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the longest of the three streams.
     * Default values are used when any stream runs out of elements before the others.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(1.0, 2.0, 3.0)
     *             .zipWith(DoubleStream.of(10.0), DoubleStream.of(100.0),
     *                      0.0, 0.0, 0.0,
     *                      (a, b, c) -> a + b + c)
     *             .toArray();   // returns [111.0, 2.0, 3.0]
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
     * @param b the second DoubleStream to be combined with the current DoubleStream. Will be closed along with this DoubleStream.
     * @param c the third DoubleStream to be combined with the current DoubleStream. Will be closed along with this DoubleStream.
     * @param valueForNoneA the default value to use for the current DoubleStream when it runs out of elements
     * @param valueForNoneB the default value to use for the second DoubleStream when it runs out of elements
     * @param valueForNoneC the default value to use for the third DoubleStream when it runs out of elements
     * @param zipFunction a DoubleTernaryOperator that determines the combination of elements in the combined DoubleStream. Must be {@code non-null}.
     * @return a new DoubleStream that is the result of combining the current DoubleStream with the given DoubleStreams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream zipWith(DoubleStream b, DoubleStream c, double valueForNoneA, double valueForNoneB, double valueForNoneC,
            DoubleTernaryOperator zipFunction);

    /**
     * Returns a Stream consisting of the elements of this stream, each boxed to a Double.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert to Stream<Double> for use with object-based operations
     * Stream<Double> boxed = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
     *       .boxed();
     *
     * // Collect to List<Double>
     * List<Double> list = DoubleStream.of(10.5, 20.3, 30.7)
     *       .boxed()
     *       .collect(Collectors.toList());
     *
     * // Use with operations requiring objects
     * DoubleStream.of(1.5, 2.7, 3.2)
     *       .boxed()
     *       .forEach(d -> System.out.println("Value: " + d));
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
     * @return a Stream consisting of the elements of this stream, each boxed to a Double
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<Double> boxed();

    /**
     * Converts this stream to a JDK {@code DoubleStream}.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert to JDK DoubleStream for compatibility
     * java.util.stream.DoubleStream jdkStream = DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0)
     *       .toJdkStream();
     *
     * // Use JDK stream operations
     * double[] sorted = DoubleStream.of(5.0, 2.0, 8.0, 1.0)
     *       .toJdkStream()
     *       .sorted()
     *       .toArray();
     *
     * // Integrate with JDK stream APIs
     * DoubleSummaryStatistics stats = DoubleStream.of(10.5, 20.3, 30.7)
     *       .toJdkStream()
     *       .summaryStatistics();
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
     * <p>If this stream has close handlers, closing the returned JDK stream closes this stream and
     * invokes those handlers exactly once.</p>
     *
     * @return a JDK {@code DoubleStream} consisting of the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract java.util.stream.DoubleStream toJdkStream();

    /**
     * Transforms this stream using the provided transfer function that operates on the JDK stream representation.
     * This method provides a bridge to use JDK stream operations on this library's DoubleStream.
     * The function receives this stream as a JDK DoubleStream and returns a transformed JDK DoubleStream,
     * which is then converted back to this library's DoubleStream type.
     *
     * <p>This is useful when you need to leverage specific JDK DoubleStream operations that are not
     * directly available in this stream implementation, or when integrating with third-party libraries
     * that work with JDK streams.
     *
     * <p>This is an intermediate operation with immediate transformation (non-deferred).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(5.0, 1.0, 3.0, 4.0, 2.0)
     *     .transformB(jdkStream -> jdkStream.sorted())
     *     .toArray();
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
     * @param transfer the function to transform the JDK stream. Must not be {@code null}.
     * @return the transformed stream as a DoubleStream
     * @throws IllegalStateException if the stream is already closed
     * @see #transformB(Function, boolean)
     * @see #toJdkStream()
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public DoubleStream transformB(final Function<? super java.util.stream.DoubleStream, ? extends java.util.stream.DoubleStream> transfer) {
        assertNotClosed();

        return transformB(transfer, false);
    }

    /**
     * Transforms this stream using the provided transfer function that operates on the JDK stream representation.
     * This method provides a bridge to use JDK stream operations on this library's DoubleStream.
     * The function receives this stream as a JDK DoubleStream and returns a transformed JDK DoubleStream,
     * which is then converted back to this library's DoubleStream type.
     *
     * <p>This is useful when you need to leverage specific JDK DoubleStream operations that are not
     * directly available in this stream implementation.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Deferred vs Immediate Execution:</b>
     * <ul>
     *   <li>If {@code deferred = false}: The transfer function is applied immediately when this method is called</li>
     *   <li>If {@code deferred = true}: The transfer function is applied lazily when a terminal operation is invoked</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream.of(5.0, 1.0, 3.0, 4.0, 2.0)
     *     .transformB(jdkStream -> jdkStream.sorted())
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * // Immediate transformation
     * DoubleStream.of(1.0, 2.0, 3.0)
     *     .transformB(jdkStream -> jdkStream.map(x -> x * 2), false)
     *     .toArray();   // returns [2.0, 4.0, 6.0]
     *
     * // Deferred transformation
     * DoubleStream.of(1.0, 2.0, 3.0)
     *     .transformB(jdkStream -> jdkStream.map(x -> x * 2), true)
     *     .toArray();   // returns [2.0, 4.0, 6.0]
     *
     * // Empty stream
     * DoubleStream.empty()
     *     .transformB(jdkStream -> jdkStream.sorted())
     *     .toArray();   // returns []
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
     * @param transfer the function to transform the JDK stream. Must not be {@code null}.
     * @param deferred if {@code true}, the transformation is deferred until the stream is consumed;
     *                 if {@code false}, the transformation is applied immediately
     * @return the transformed stream as a DoubleStream
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if transfer is null
     * @see #transformB(Function)
     * @see #toJdkStream()
     * @see DoubleStream#defer(Supplier)
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public DoubleStream transformB(final Function<? super java.util.stream.DoubleStream, ? extends java.util.stream.DoubleStream> transfer,
            final boolean deferred) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(transfer, cs.transfer);

        if (deferred) {
            final Supplier<DoubleStream> delayInitializer = () -> DoubleStream.from(transfer.apply(toJdkStream()));
            return DoubleStream.defer(delayInitializer);
        } else {
            return DoubleStream.from(transfer.apply(toJdkStream()));
        }
    }

    abstract DoubleIteratorEx iteratorEx();

    // private static final DoubleStream EMPTY_STREAM = new ArrayDoubleStream(N.EMPTY_DOUBLE_ARRAY, true, null);

    /**
     * Returns an empty DoubleStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream empty = DoubleStream.empty();
     * empty.count();   // returns 0
     *
     * // Use as default when no data available
     * DoubleStream stream = hasData ? DoubleStream.of(data) : DoubleStream.empty();
     * }</pre>
     *
     * @return an empty DoubleStream
     */
    public static DoubleStream empty() {
        return new ArrayDoubleStream(N.EMPTY_DOUBLE_ARRAY, true, null);
    }

    /**
     * Returns a DoubleStream that is lazily populated by an input supplier.
     * This is a static factory method that defers stream creation until the returned stream is first traversed or closed.
     *
     * <p>The supplier is memoized and invoked at most once. Closing the returned stream before traversal may still
     * invoke the supplier so the supplied stream can be closed.
     *
     * <p>It is equivalent to {@code Stream.just(supplier).flatMapToDouble(it -> it.get())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Defer expensive computation until needed
     * DoubleStream deferred = DoubleStream.defer(() -> {
     *     // This code runs when the stream is consumed or closed
     *     double[] expensiveData = loadDataFromDatabase();
     *     return DoubleStream.of(expensiveData);
     * });
     *
     * // Conditional stream creation
     * DoubleStream conditionalStream = DoubleStream.defer(() ->
     *     useCache ? DoubleStream.of(cachedData) : DoubleStream.of(freshData)
     * );
     * }</pre>
     *
     * @param supplier the supplier that provides the DoubleStream
     * @return a new DoubleStream supplied by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#defer(Supplier)
     */
    public static DoubleStream defer(final Supplier<DoubleStream> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Supplier<DoubleStream> s = Fn.memoize(supplier);
        return Stream.just(s).flatMapToDouble(Supplier::get).onClose(newCloseHandler(s));
    }

    /**
     * Creates a DoubleStream from a java.util.stream.DoubleStream.
     * The resulting stream will have the same characteristics (sequential/parallel) as the input stream.
     * The input stream will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert JDK DoubleStream to this library's DoubleStream
     * java.util.stream.DoubleStream jdkStream = java.util.stream.DoubleStream.of(1.0, 2.0, 3.0);
     * DoubleStream stream = DoubleStream.from(jdkStream);
     *
     * // Use with JDK stream operations then convert
     * DoubleStream result = DoubleStream.from(
     *     java.util.stream.DoubleStream.iterate(1.0, d -> d * 2).limit(5)
     * );
     * }</pre>
     *
     * @param stream the java.util.stream.DoubleStream to convert
     * @return a new DoubleStream containing the same elements, or empty stream if input is null
     */
    public static DoubleStream from(final java.util.stream.DoubleStream stream) {
        if (stream == null) {
            return empty();
        }

        return of(new DoubleIteratorEx() {
            private PrimitiveIterator.OfDouble iter = null;
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
            public double nextDouble() {
                if (exhausted) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.nextDouble();
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
            public double[] toArray() {
                if (exhausted) {
                    return new double[0];
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
     * Creates a DoubleStream containing the specified {@code nullable} Double element.
     * If the element is {@code null}, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Non-null value creates single element stream
     * DoubleStream.ofNullable(5.5).count();   // returns 1
     *
     * // Null value creates empty stream
     * DoubleStream.ofNullable(null).count();   // returns 0
     *
     * // Use when value might be null
     * Double value = getValue();   // returns null possibly
     * DoubleStream.ofNullable(value)
     *       .forEach(System.out::println);   // prints only if value is non-null
     * }</pre>
     *
     * @param e the {@code nullable} Double element
     * @return a DoubleStream containing the element if {@code non-null}, otherwise an empty stream
     */
    public static DoubleStream ofNullable(final Double e) {
        return e == null ? empty() : of(e);
    }

    /**
     * Creates a DoubleStream from the specified double array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from varargs
     * DoubleStream.of(1.5, 2.7, 3.2)
     *     .forEach(System.out::println);   // prints: 1.5, 2.7, 3.2
     *
     * // Create stream from array
     * double[] values = {10.5, 20.3, 30.1};
     * DoubleStream.of(values)
     *     .sum();   // returns 60.9
     *
     * // Empty array returns empty stream
     * DoubleStream.of()
     *     .count();   // returns 0
     * }</pre>
     *
     * @param a the double array
     * @return a DoubleStream containing the elements of the array, or empty stream if array is {@code null} or empty
     */
    public static DoubleStream of(final double... a) {
        return N.isEmpty(a) ? empty() : new ArrayDoubleStream(a);
    }

    /**
     * Creates a DoubleStream from a portion of the specified double array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
     *
     * // Stream middle elements (indices 1 to 3)
     * DoubleStream.of(values, 1, 4)
     *     .toArray();   // returns [2.0, 3.0, 4.0]
     *
     * // Stream from beginning
     * DoubleStream.of(values, 0, 3)
     *     .sum();   // returns 6.0 (1.0 + 2.0 + 3.0)
     *
     * // Empty range returns empty stream
     * DoubleStream.of(values, 2, 2)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param a the double array
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a DoubleStream containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the array length, or {@code fromIndex} is greater than {@code toIndex}
     */
    public static DoubleStream of(final double[] a, final int fromIndex, final int toIndex) {
        return isEmptyRange(N.len(a), fromIndex, toIndex) ? empty() : new ArrayDoubleStream(a, fromIndex, toIndex);
    }

    /**
     * Creates a DoubleStream from the specified Double array.
     * Null elements will be converted to 0.0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from Double array
     * Double[] values = {1.5, 2.7, 3.2};
     * DoubleStream.of(values)
     *     .forEach(System.out::println);   // prints: 1.5, 2.7, 3.2
     *
     * // Null elements converted to 0.0
     * Double[] withNulls = {1.5, null, 3.2};
     * DoubleStream.of(withNulls)
     *     .toArray();   // returns [1.5, 0.0, 3.2]
     *
     * // Sum ignoring null values (treated as 0.0)
     * Double[] prices = {10.5, null, 20.3};
     * DoubleStream.of(prices)
     *     .sum();   // returns 30.8
     * }</pre>
     *
     * @param a the Double array
     * @return a DoubleStream containing the unboxed elements of the array, or an empty stream if the array is {@code null} or empty
     */
    public static DoubleStream of(final Double[] a) {
        return Stream.of(a).mapToDouble(FD.unbox());
    }

    /**
     * Creates a DoubleStream from a portion of the specified Double array.
     * Null elements will be converted to 0.0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Double[] values = {1.0, 2.0, 3.0, 4.0, 5.0};
     *
     * // Stream middle elements
     * DoubleStream.of(values, 1, 4)
     *     .toArray();   // returns [2.0, 3.0, 4.0]
     *
     * // Handle null values in range
     * Double[] withNulls = {1.0, null, 3.0, null, 5.0};
     * DoubleStream.of(withNulls, 1, 4)
     *     .toArray();   // returns [0.0, 3.0, 0.0]
     * }</pre>
     *
     * @param a the Double array
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a DoubleStream containing the unboxed elements from the specified range
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    public static DoubleStream of(final Double[] a, final int fromIndex, final int toIndex) {
        return Stream.of(a, fromIndex, toIndex).mapToDouble(FD.unbox());
    }

    /**
     * Creates a DoubleStream from the specified Collection of Double.
     * Null elements will be converted to 0.0.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from List
     * List<Double> values = Arrays.asList(1.5, 2.7, 3.2);
     * DoubleStream.of(values)
     *     .sum();   // returns 7.4
     *
     * // Handle null values in collection
     * List<Double> withNulls = Arrays.asList(1.5, null, 3.2);
     * DoubleStream.of(withNulls)
     *     .average()
     *     .getAsDouble();   // returns 1.566... (4.7 / 3)
     *
     * // Process Set of Double values
     * Set<Double> uniqueValues = new HashSet<>(Arrays.asList(1.0, 2.0, 3.0));
     * DoubleStream.of(uniqueValues)
     *     .max();   // returns OptionalDouble[3.0]
     * }</pre>
     *
     * @param c the Collection of Double
     * @return a DoubleStream containing the unboxed elements of the collection, or an empty stream if the collection is {@code null} or empty
     */
    public static DoubleStream of(final Collection<Double> c) {
        return Stream.of(c).mapToDouble(FD.unbox());
    }

    /**
     * Creates a DoubleStream from the specified DoubleIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from iterator
     * DoubleList list = DoubleList.of(1.5, 2.7, 3.2);
     * DoubleStream.of(list.iterator())
     *     .sum();   // returns 7.4
     *
     * // Custom iterator
     * DoubleIterator customIterator = new DoubleIterator() {
     *     private double value = 1.0;
     *     private int count = 0;
     *     public boolean hasNext() { return count < 5; }
     *     public double nextDouble() { count++; return value *= 2; }
     * };
     * DoubleStream.of(customIterator)
     *     .toArray();   // returns [2.0, 4.0, 8.0, 16.0, 32.0]
     * }</pre>
     *
     * @param iterator the {@link DoubleIterator} to source elements from (may be {@code null})
     * @return a DoubleStream over the elements from {@code iterator}, or an empty stream if {@code iterator} is {@code null}
     */
    public static DoubleStream of(final DoubleIterator iterator) {
        return iterator == null ? empty() : new IteratorDoubleStream(iterator);
    }

    /**
     * Creates a DoubleStream from the specified {@link DoubleBuffer}, reading elements
     * from the buffer's current {@link DoubleBuffer#position() position} (inclusive) to
     * its {@link DoubleBuffer#limit() limit} (exclusive). Returns an empty stream if
     * {@code buf} is {@code null}.
     *
     * <p>The buffer's position is <b>not</b> advanced by stream consumption — the stream
     * reads via absolute indexed {@code get(int)} access, so the buffer remains usable
     * afterwards.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from DoubleBuffer
     * DoubleBuffer buffer = DoubleBuffer.wrap(new double[]{1.5, 2.7, 3.2, 4.9});
     * DoubleStream.of(buffer)
     *     .sum();   // returns 12.3
     *
     * // Stream from positioned buffer
     * DoubleBuffer positioned = DoubleBuffer.wrap(new double[]{1.0, 2.0, 3.0, 4.0, 5.0});
     * positioned.position(2);   // starts from index 2
     * DoubleStream.of(positioned)
     *     .toArray();   // returns [3.0, 4.0, 5.0]
     *
     * // Use with limited buffer
     * DoubleBuffer limited = DoubleBuffer.allocate(10);
     * limited.put(new double[]{1.5, 2.5, 3.5});
     * limited.flip();   // buffer becomes position 0, limit 3
     * DoubleStream.of(limited)
     *     .count();   // returns 3
     * }</pre>
     *
     * @param buf the DoubleBuffer (may be {@code null})
     * @return a DoubleStream over elements in {@code buf} from {@code position} (inclusive)
     *         to {@code limit} (exclusive), or an empty stream if {@code buf} is {@code null}
     */
    public static DoubleStream of(final DoubleBuffer buf) {
        if (buf == null) {
            return empty();
        }

        //noinspection resource
        return IntStream.range(buf.position(), buf.limit()).mapToDouble(buf::get);
    }

    /**
     * Creates a DoubleStream from the specified OptionalDouble.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream from present OptionalDouble
     * OptionalDouble present = OptionalDouble.of(42.5);
     * DoubleStream.of(present)
     *     .sum();   // returns 42.5
     *
     * // Stream from empty OptionalDouble
     * OptionalDouble empty = OptionalDouble.empty();
     * DoubleStream.of(empty)
     *     .count();   // returns 0
     *
     * // Chain with other operations
     * OptionalDouble value = OptionalDouble.of(10.0);
     * DoubleStream.of(value)
     *     .map(d -> d * 2)
     *     .findFirst();   // returns OptionalDouble[20.0]
     * }</pre>
     *
     * @param op the {@link OptionalDouble} (may be {@code null})
     * @return a single-element DoubleStream containing {@code op.get()} if it is present,
     *         or an empty stream if {@code op} is {@code null} or empty
     */
    public static DoubleStream of(final OptionalDouble op) {
        return op == null || op.isEmpty() ? DoubleStream.empty() : DoubleStream.of(op.get());
    }

    /**
     * Creates a DoubleStream from the specified java.util.OptionalDouble.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream from present java.util.OptionalDouble
     * java.util.OptionalDouble present = java.util.OptionalDouble.of(42.5);
     * DoubleStream.of(present)
     *     .sum();   // returns 42.5
     *
     * // Stream from empty java.util.OptionalDouble
     * java.util.OptionalDouble empty = java.util.OptionalDouble.empty();
     * DoubleStream.of(empty)
     *     .count();   // returns 0
     *
     * // Integration with JDK streams
     * java.util.OptionalDouble max = java.util.stream.DoubleStream.of(1.0, 2.0, 3.0).max();
     * DoubleStream.of(max)
     *     .findFirst();   // returns OptionalDouble[3.0]
     * }</pre>
     *
     * @param op the java.util.OptionalDouble
     * @return a DoubleStream containing the value if present, otherwise an empty stream
     */
    public static DoubleStream of(final java.util.OptionalDouble op) {
        return op == null || op.isEmpty() ? DoubleStream.empty() : DoubleStream.of(op.getAsDouble());
    }

    private static final Function<double[], DoubleStream> flatMapper = DoubleStream::of;

    private static final Function<double[][], DoubleStream> flattMapper = DoubleStream::flatten;

    /**
     * Flattens a two-dimensional double array into a single DoubleStream.
     * Elements are read row by row (horizontally).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten 2D array horizontally (row by row)
     * double[][] matrix = {
     *     {1.0, 2.0, 3.0},
     *     {4.0, 5.0, 6.0},
     *     {7.0, 8.0, 9.0}
     * };
     * DoubleStream.flatten(matrix)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     *
     * // Flatten jagged array
     * double[][] jagged = {
     *     {1.0, 2.0},
     *     {3.0, 4.0, 5.0},
     *     {6.0}
     * };
     * DoubleStream.flatten(jagged)
     *     .sum();   // returns 21.0
     * }</pre>
     *
     * @param a the two-dimensional double array
     * @return a DoubleStream containing all elements from the array
     */
    public static DoubleStream flatten(final double[][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToDouble(flatMapper);
    }

    /**
     * Flattens a two-dimensional double array into a single DoubleStream.
     * Elements can be read either horizontally (row by row) or vertically (column by column).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[][] matrix = {
     *     {1.0, 2.0, 3.0},
     *     {4.0, 5.0, 6.0},
     *     {7.0, 8.0, 9.0}
     * };
     *
     * // Flatten horizontally (row by row)
     * DoubleStream.flatten(matrix, false)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     *
     * // Flatten vertically (column by column)
     * DoubleStream.flatten(matrix, true)
     *     .toArray();   // returns [1.0, 4.0, 7.0, 2.0, 5.0, 8.0, 3.0, 6.0, 9.0]
     *
     * // Vertical flattening with jagged array
     * double[][] jagged = {
     *     {1.0, 2.0, 3.0},
     *     {4.0, 5.0},
     *     {6.0}
     * };
     * DoubleStream.flatten(jagged, true)
     *     .toArray();   // returns [1.0, 4.0, 6.0, 2.0, 5.0, 3.0]
     * }</pre>
     *
     * @param a the two-dimensional double array
     * @param vertically if {@code true}, reads elements column by column; if {@code false}, reads row by row
     * @return a DoubleStream containing all elements from the array
     */
    public static DoubleStream flatten(final double[][] a, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (!vertically) {
            return Stream.of(a).flatMapToDouble(flatMapper);
        }

        long n = 0;

        for (final double[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final DoubleIterator iter = new DoubleIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public double nextDouble() {
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
     * Flattens a two-dimensional double array into a single DoubleStream with alignment.
     * When arrays have different lengths, shorter arrays are padded with the specified value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[][] jagged = {
     *     {1.0, 2.0, 3.0},
     *     {4.0, 5.0},
     *     {6.0}
     * };
     *
     * // Flatten vertically with padding value 0.0
     * DoubleStream.flatten(jagged, 0.0, true)
     *     .toArray();   // returns [1.0, 4.0, 6.0, 2.0, 5.0, 0.0, 3.0, 0.0, 0.0]
     *
     * // Flatten horizontally with padding value -1.0
     * DoubleStream.flatten(jagged, -1.0, false)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, -1.0, 6.0, -1.0, -1.0]
     *
     * // Use NaN for missing values
     * DoubleStream.flatten(jagged, Double.NaN, true)
     *     .filter(d -> !Double.isNaN(d))
     *     .sum();   // returns 21.0
     * }</pre>
     *
     * @param a the two-dimensional double array
     * @param valueForAlignment the value to use for padding shorter arrays
     * @param vertically if {@code true}, reads elements column by column; if {@code false}, reads row by row
     * @return a DoubleStream containing all elements from the array with alignment
     */
    public static DoubleStream flatten(final double[][] a, final double valueForAlignment, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (final double[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = (long) rows * cols;
        DoubleIterator iter = null;

        if (vertically) {
            iter = new DoubleIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public double nextDouble() {
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
            iter = new DoubleIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public double nextDouble() {
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
     * Flattens a three-dimensional double array into a single DoubleStream.
     * The array is flattened by first flattening each two-dimensional array, then concatenating the results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten 3D array
     * double[][][] cube = {
     *     {{1.0, 2.0}, {3.0, 4.0}},
     *     {{5.0, 6.0}, {7.0, 8.0}}
     * };
     * DoubleStream.flatten(cube)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Flatten irregular 3D array
     * double[][][] irregular = {
     *     {{1.0}, {2.0, 3.0}},
     *     {{4.0, 5.0, 6.0}}
     * };
     * DoubleStream.flatten(irregular)
     *     .sum();   // returns 21.0
     * }</pre>
     *
     * @param a the three-dimensional double array
     * @return a DoubleStream containing all elements from the array
     */
    public static DoubleStream flatten(final double[][][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToDouble(flattMapper);
    }

    /**
     * Creates a DoubleStream that repeats the specified element for the given number of times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Repeat a value 5 times
     * DoubleStream.repeat(3.14, 5)
     *     .toArray();   // returns [3.14, 3.14, 3.14, 3.14, 3.14]
     *
     * // Create array of zeros
     * DoubleStream.repeat(0.0, 10)
     *     .sum();   // returns 0.0
     *
     * // Use in calculations
     * DoubleStream.repeat(2.5, 100)
     *     .limit(50)
     *     .sum();   // returns 125.0 (2.5 * 50)
     *
     * // Empty stream when n is 0
     * DoubleStream.repeat(1.0, 0)
     *     .count();   // returns 0
     * }</pre>
     *
     * @param element the element to repeat
     * @param n the number of times to repeat
     * @return a DoubleStream containing n copies of the element
     * @throws IllegalArgumentException if n is negative
     */
    public static DoubleStream repeat(final double element, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return empty();
        } else if (n < 10) {
            return of(Array.repeat(element, (int) n));
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public double nextDouble() {
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
            public double[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final double[] result = new double[(int) cnt];

                Arrays.fill(result, element);

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates an infinite DoubleStream of random double values between 0.0 (inclusive) and 1.0 (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 5 random doubles
     * DoubleStream.random()
     *     .limit(5)
     *     .toArray();   // returns array like [0.234, 0.789, 0.456, 0.123, 0.901]
     *
     * // Generate random values in range [10, 20)
     * DoubleStream.random()
     *     .map(d -> d * 10 + 10)
     *     .limit(5)
     *     .forEach(System.out::println);
     *
     * // Monte Carlo simulation - estimate pi
     * long insideCircle = DoubleStream.generate(() -> {
     *         double x = Math.random();
     *         double y = Math.random();
     *         return x * x + y * y;
     *     })
     *     .limit(10000)
     *     .filter(d -> d <= 1.0)
     *     .count();
     *
     * // Random statistics
     * DoubleSummaryStatistics stats = DoubleStream.random()
     *     .limit(1000)
     *     .summaryStatistics();
     * }</pre>
     *
     * @return an infinite DoubleStream of random double values
     */
    public static DoubleStream random() {
        return generate(RAND::nextDouble);
    }

    /**
     * Creates a DoubleStream that generates elements using the provided hasNext and next suppliers.
     * The stream continues as long as hasNext returns {@code true}.
     * After the {@code hasNext} supplier first returns {@code false}, the stream remains exhausted
     * and that supplier is not invoked again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Iterate with external state
     * final double[] state = {1.0};
     * DoubleStream.iterate(() -> state[0] <= 5.0, () -> state[0]++)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * // Generate Fibonacci-like sequence
     * final double[] fib = {0.0, 1.0};
     * DoubleStream.iterate(() -> fib[1] < 100, () -> {
     *     double next = fib[0] + fib[1];
     *     fib[0] = fib[1];
     *     fib[1] = next;
     *     return fib[0];
     * }).toArray();
     *
     * // Read from external source conditionally
     * final Iterator<Double> source = getDataSource();
     * DoubleStream.iterate(source::hasNext, () -> source.next())
     *     .sum();
     * }</pre>
     *
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param next a DoubleSupplier that provides the next double in the iteration
     * @return a DoubleStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or next is null
     * @see Stream#iterate(BooleanSupplier, Supplier)
     */
    public static DoubleStream iterate(final BooleanSupplier hasNext, final DoubleSupplier next) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
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
            public double nextDouble() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return next.getAsDouble();
            }
        });
    }

    /**
     * Creates a DoubleStream that starts with an initial value and generates subsequent values by applying a function.
     * The stream continues as long as hasNext returns {@code true}.
     * After the {@code hasNext} supplier first returns {@code false}, the stream remains exhausted
     * and that supplier is not invoked again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate powers of 2 up to 1000
     * final double[] limit = {1.0};
     * DoubleStream.iterate(2.0, () -> limit[0] <= 1000, d -> {
     *     limit[0] = d * 2;
     *     return d * 2;
     * }).toArray();   // returns [2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0, 1024.0]
     *
     * // Countdown with external condition
     * final boolean[] shouldContinue = {true};
     * DoubleStream.iterate(10.0, () -> shouldContinue[0], d -> d - 1)
     *     .limit(5)
     *     .toArray();   // returns [10.0, 9.0, 8.0, 7.0, 6.0]
     *
     * // Generate sequence with dynamic termination
     * final AtomicInteger count = new AtomicInteger(0);
     * DoubleStream.iterate(1.0, () -> count.get() < 5, d -> {
     *     count.incrementAndGet();
     *     return d * 1.5;
     * }).sum();
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param f a function to apply to the previous element to generate the next element
     * @return a DoubleStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or f is null
     * @see Stream#iterate(Object, BooleanSupplier, java.util.function.UnaryOperator)
     */
    public static DoubleStream iterate(final double init, final BooleanSupplier hasNext, final DoubleUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private double cur = 0;
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
            public double nextDouble() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsDouble(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Creates a DoubleStream that starts with an initial value and generates subsequent values by applying a function.
     * The stream continues as long as the predicate test passes for the current value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate values while less than 100
     * DoubleStream.iterate(1.0, d -> d < 100, d -> d * 2)
     *     .toArray();   // returns [1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0]
     *
     * // Countdown from 10 to 1
     * DoubleStream.iterate(10.0, d -> d > 0, d -> d - 1)
     *     .sum();   // returns 55.0
     *
     * // Generate diminishing values
     * DoubleStream.iterate(1000.0, d -> d > 1.0, d -> d / 2)
     *     .toArray();   // returns [1000.0, 500.0, 250.0, 125.0, 62.5, 31.25, 15.625, 7.8125, 3.90625, 1.953125]
     *
     * // Geometric sequence with condition
     * DoubleStream.iterate(0.5, d -> d < 10.0, d -> d * 1.5)
     *     .forEach(System.out::println);
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext predicate to test the current value; stream continues while this returns true
     * @param f a function to apply to the previous element to generate the next element
     * @return a DoubleStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or f is null
     * @see Stream#iterate(Object, java.util.function.Predicate, java.util.function.UnaryOperator)
     */
    public static DoubleStream iterate(final double init, final DoublePredicate hasNext, final DoubleUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private double cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsDouble(cur));
                    }

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public double nextDouble() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return cur;
            }
        });
    }

    /**
     * Creates an infinite DoubleStream that starts with an initial value and generates subsequent values by applying a function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate powers of 2 (infinite, use limit)
     * DoubleStream.iterate(1.0, d -> d * 2)
     *     .limit(10)
     *     .toArray();   // returns [1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0, 128.0, 256.0, 512.0]
     *
     * // Natural numbers starting from 1
     * DoubleStream.iterate(1.0, d -> d + 1)
     *     .limit(5)
     *     .sum();   // returns 15.0 (1+2+3+4+5)
     *
     * // Geometric sequence
     * DoubleStream.iterate(2.0, d -> d * 1.5)
     *     .limit(6)
     *     .toArray();   // returns [2.0, 3.0, 4.5, 6.75, 10.125, 15.1875]
     *
     * // Fibonacci-like sequence (single state)
     * DoubleStream.iterate(1.0, d -> d * 1.618) // uses golden ratio approximation
     *     .limit(10)
     *     .forEach(System.out::println);
     * }</pre>
     *
     * @param init the initial value
     * @param f a function to apply to the previous element to generate the next element
     * @return an infinite DoubleStream of elements generated by the iteration
     * @throws IllegalArgumentException if f is null
     * @see Stream#iterate(Object, java.util.function.UnaryOperator)
     */
    public static DoubleStream iterate(final double init, final DoubleUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(f);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private double cur = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double nextDouble() {
                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsDouble(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Creates an infinite DoubleStream that generates elements using the provided DoubleSupplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate constant values
     * DoubleStream.generate(() -> 42.0)
     *     .limit(5)
     *     .toArray();   // returns [42.0, 42.0, 42.0, 42.0, 42.0]
     *
     * // Generate random values
     * DoubleStream.generate(Math::random)
     *     .limit(5)
     *     .forEach(System.out::println);
     *
     * // Generate from external state
     * final AtomicInteger counter = new AtomicInteger(0);
     * DoubleStream.generate(() -> counter.incrementAndGet() * 1.5)
     *     .limit(5)
     *     .toArray();   // returns [1.5, 3.0, 4.5, 6.0, 7.5]
     *
     * // Generate timestamps
     * DoubleStream.generate(() -> System.currentTimeMillis() / 1000.0)
     *     .limit(3)
     *     .forEach(System.out::println);
     *
     * // Generate computed values
     * final Random rand = new Random(42);
     * DoubleStream.generate(() -> rand.nextGaussian() * 10 + 100)
     *     .limit(1000)
     *     .average();
     * }</pre>
     *
     * @param s the DoubleSupplier that provides the elements of the stream
     * @return an infinite DoubleStream generated by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#generate(Supplier)
     */
    public static DoubleStream generate(final DoubleSupplier s) throws IllegalArgumentException {
        N.checkArgNotNull(s);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double nextDouble() {
                return s.getAsDouble();
            }
        });
    }

    /**
     * Concatenates multiple double arrays into a single DoubleStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple arrays
     * double[] arr1 = {1.0, 2.0};
     * double[] arr2 = {3.0, 4.0};
     * double[] arr3 = {5.0, 6.0};
     * DoubleStream.concat(arr1, arr2, arr3)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Concatenate with empty arrays
     * DoubleStream.concat(new double[]{1.0}, new double[]{}, new double[]{2.0})
     *     .sum();   // returns 3.0
     *
     * // Concatenate many arrays
     * DoubleStream.concat(
     *     new double[]{1.0, 2.0},
     *     new double[]{3.0, 4.0},
     *     new double[]{5.0, 6.0}
     * ).average().getAsDouble();   // returns 3.5
     * }</pre>
     *
     * @param a the arrays of doubles to concatenate
     * @return a DoubleStream containing all elements from the input arrays in order
     * @see Stream#concat(Object[][])
     */
    public static DoubleStream concat(final double[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Arrays.asList(a));
    }

    /**
     * Concatenates multiple DoubleIterators into a single DoubleStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple iterators
     * DoubleIterator iter1 = DoubleIterator.of(1.0, 2.0);
     * DoubleIterator iter2 = DoubleIterator.of(3.0, 4.0);
     * DoubleIterator iter3 = DoubleIterator.of(5.0, 6.0);
     * DoubleStream.concat(iter1, iter2, iter3)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Concatenate from different sources
     * DoubleList list = DoubleList.of(1.0, 2.0);
     * double[] array = {3.0, 4.0};
     * DoubleStream.concat(list.iterator(), DoubleIterator.of(array))
     *     .sum();   // returns 10.0
     * }</pre>
     *
     * @param a the DoubleIterators to concatenate
     * @return a DoubleStream containing all elements from the input iterators in order
     * @see Stream#concat(Iterator[])
     */
    public static DoubleStream concat(final DoubleIterator... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concatIterators(Array.asList(a));
    }

    /**
     * Concatenates multiple DoubleStreams into a single DoubleStream.
     * The input streams will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple streams
     * DoubleStream s1 = DoubleStream.of(1.0, 2.0);
     * DoubleStream s2 = DoubleStream.of(3.0, 4.0);
     * DoubleStream s3 = DoubleStream.of(5.0, 6.0);
     * DoubleStream.concat(s1, s2, s3)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Combine filtered streams
     * DoubleStream positives = DoubleStream.of(1.0, 2.0, 3.0);
     * DoubleStream negatives = DoubleStream.of(-1.0, -2.0, -3.0);
     * DoubleStream.concat(positives, negatives)
     *     .sorted()
     *     .toArray();   // returns [-3.0, -2.0, -1.0, 1.0, 2.0, 3.0]
     *
     * // Concatenate with stream operations
     * DoubleStream.concat(
     *     DoubleStream.iterate(1.0, d -> d + 1).limit(3),
     *     DoubleStream.repeat(10.0, 3)
     * ).sum();   // returns 36.0
     * }</pre>
     *
     * @param a the DoubleStreams to concatenate
     * @return a DoubleStream containing all elements from the input streams in order
     * @see Stream#concat(Stream[])
     */
    public static DoubleStream concat(final DoubleStream... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a list of double arrays into a single DoubleStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate list of arrays
     * List<double[]> arrays = Arrays.asList(
     *     new double[]{1.0, 2.0},
     *     new double[]{3.0, 4.0},
     *     new double[]{5.0, 6.0}
     * );
     * DoubleStream.concat(arrays)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Dynamic array collection
     * List<double[]> dynamicArrays = new ArrayList<>();
     * dynamicArrays.add(new double[]{1.0, 2.0});
     * dynamicArrays.add(new double[]{3.0});
     * DoubleStream.concat(dynamicArrays)
     *     .sum();   // returns 6.0
     * }</pre>
     *
     * @param c the list of double arrays to concatenate
     * @return a DoubleStream containing all elements from the input arrays in order
     * @see Stream#concat(Object[][])
     */
    @Beta
    public static DoubleStream concat(final List<double[]> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return of(new DoubleIteratorEx() {
            private final Iterator<double[]> iter = c.iterator();
            private double[] cur;
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
            public double nextDouble() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        });
    }

    /**
     * Concatenates a collection of DoubleStreams into a single DoubleStream.
     * The input streams will be closed when the returned stream is closed.
     * The collection's membership and encounter order are snapshotted when this method is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate collection of streams
     * List<DoubleStream> streams = Arrays.asList(
     *     DoubleStream.of(1.0, 2.0),
     *     DoubleStream.of(3.0, 4.0),
     *     DoubleStream.of(5.0, 6.0)
     * );
     * DoubleStream.concat(streams)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Dynamic stream collection
     * List<DoubleStream> dynamicStreams = new ArrayList<>();
     * dynamicStreams.add(DoubleStream.of(1.0, 2.0, 3.0));
     * dynamicStreams.add(DoubleStream.of(10.0, 20.0));
     * DoubleStream.concat(dynamicStreams)
     *     .sum();   // returns 36.0
     * }</pre>
     *
     * @param streams the collection of DoubleStreams to concatenate; {@code null} elements are treated as empty streams
     * @return a DoubleStream containing all elements from the input streams in order
     * @see Stream#concat(Collection)
     */
    public static DoubleStream concat(final Collection<? extends DoubleStream> streams) {
        if (N.isEmpty(streams)) {
            return empty();
        }

        final List<? extends DoubleStream> sources = new ArrayList<>(streams);

        return new IteratorDoubleStream(new DoubleIteratorEx() { //NOSONAR
            private final Iterator<? extends DoubleStream> iterators = sources.iterator();
            private DoubleStream cur;
            private DoubleIterator iter;

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
            public double nextDouble() {
                if ((iter == null || !iter.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextDouble();
            }
        }).onClose(newCloseHandler(sources));
    }

    /**
     * Concatenates a collection of DoubleIterators into a single DoubleStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple iterators from a collection
     * List<DoubleIterator> iterators = Arrays.asList(
     *     DoubleIterator.of(1.0, 2.0),
     *     DoubleIterator.of(3.0, 4.0),
     *     DoubleIterator.of(5.0, 6.0)
     * );
     * DoubleStream.concatIterators(iterators)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Combine iterators from different sources
     * List<DoubleIterator> mixed = new ArrayList<>();
     * mixed.add(DoubleList.of(1.5, 2.5).iterator());
     * mixed.add(DoubleIterator.of(new double[]{3.5, 4.5}));
     * DoubleStream.concatIterators(mixed)
     *     .sum();   // returns 12.0
     *
     * // Empty collection returns empty stream
     * DoubleStream.concatIterators(Collections.emptyList())
     *     .count();   // returns 0
     * }</pre>
     *
     * @param doubleIterators the collection of DoubleIterators to concatenate
     * @return a DoubleStream containing all elements from the input iterators in order
     * @see Stream#concatIterators(Collection)
     * @see #concat(DoubleIterator...)
     */
    @Beta
    public static DoubleStream concatIterators(final Collection<? extends DoubleIterator> doubleIterators) {
        if (N.isEmpty(doubleIterators)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final Iterator<? extends DoubleIterator> iter = doubleIterators.iterator();
            private DoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public double nextDouble() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextDouble();
            }
        });
    }

    /**
     * Zips two double arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two double arrays using a zip function.
     *
     * <p>The operation stops when either array runs out of values. No default values are used
     * for the shorter array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] a = {1.0, 2.0, 3.0};
     * double[] b = {4.0, 5.0, 6.0};
     * DoubleStream.zip(a, b, (x, y) -> x + y)
     *       .toDoubleList();   // returns [5.0, 7.0, 9.0]
     * }</pre>
     *
     * @param a the first double array
     * @param b the second double array
     * @param zipFunction the function to combine pairs of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], BiFunction)
     */
    public static DoubleStream zip(final double[] a, final double[] b, final DoubleBinaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public double nextDouble() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsDouble(a[cursor], b[cursor++]);
            }
        });
    }

    /**
     * Zips three double arrays into a single stream until any of them runs out of values.
     * This is a static factory method that combines elements from three double arrays using a zip function.
     *
     * <p>The operation stops when any array runs out of values. No default values are used
     * for the shorter arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] a = {1.0, 2.0, 3.0};
     * double[] b = {4.0, 5.0, 6.0};
     * double[] c = {7.0, 8.0, 9.0};
     * DoubleStream.zip(a, b, c, (x, y, z) -> x + y + z)
     *       .toDoubleList();   // returns [12.0, 15.0, 18.0]
     * }</pre>
     *
     * @param a the first double array
     * @param b the second double array
     * @param c the third double array
     * @param zipFunction the function to combine triples of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object[], TriFunction)
     */
    public static DoubleStream zip(final double[] a, final double[] b, final double[] c, final DoubleTernaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b) || N.isEmpty(c)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public double nextDouble() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsDouble(a[cursor], b[cursor], c[cursor++]);
            }
        });
    }

    /**
     * Zips two double iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two double iterators using a zip function.
     *
     * <p>The operation stops when either iterator runs out of values. No default values are used
     * for the shorter iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter1 = DoubleIterator.of(1.0, 2.0, 3.0);
     * DoubleIterator iter2 = DoubleIterator.of(4.0, 5.0, 6.0);
     * DoubleStream.zip(iter1, iter2, (x, y) -> x + y)
     *       .toDoubleList();   // returns [5.0, 7.0, 9.0]
     * }</pre>
     *
     * @param a the first double iterator
     * @param b the second double iterator
     * @param zipFunction the function to combine pairs of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, BiFunction)
     */
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleBinaryOperator zipFunction) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final DoubleIterator iterA = a == null ? DoubleIterator.empty() : a;
            private final DoubleIterator iterB = b == null ? DoubleIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public double nextDouble() {
                return zipFunction.applyAsDouble(iterA.nextDouble(), iterB.nextDouble());
            }
        });
    }

    /**
     * Zips three double iterators into a single stream until any of them runs out of values.
     * This is a static factory method that combines elements from three double iterators using a zip function.
     *
     * <p>The operation stops when any iterator runs out of values. No default values are used
     * for the shorter iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter1 = DoubleIterator.of(1.0, 2.0, 3.0);
     * DoubleIterator iter2 = DoubleIterator.of(4.0, 5.0, 6.0);
     * DoubleIterator iter3 = DoubleIterator.of(7.0, 8.0, 9.0);
     * DoubleStream.zip(iter1, iter2, iter3, (x, y, z) -> x + y + z)
     *       .toDoubleList();   // returns [12.0, 15.0, 18.0]
     * }</pre>
     *
     * @param a the first double iterator
     * @param b the second double iterator
     * @param c the third double iterator
     * @param zipFunction the function to combine triples of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Iterator, TriFunction)
     */
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final DoubleTernaryOperator zipFunction) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final DoubleIterator iterA = a == null ? DoubleIterator.empty() : a;
            private final DoubleIterator iterB = b == null ? DoubleIterator.empty() : b;
            private final DoubleIterator iterC = c == null ? DoubleIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public double nextDouble() {
                return zipFunction.applyAsDouble(iterA.nextDouble(), iterB.nextDouble(), iterC.nextDouble());
            }
        });
    }

    /**
     * Zips two double streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two double streams using a zip function.
     *
     * <p>The operation stops when either stream runs out of values. No default values are used
     * for the shorter stream. The input streams will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream stream1 = DoubleStream.of(1.0, 2.0, 3.0);
     * DoubleStream stream2 = DoubleStream.of(4.0, 5.0, 6.0);
     * DoubleStream.zip(stream1, stream2, (x, y) -> x + y)
     *       .toDoubleList();   // returns [5.0, 7.0, 9.0]
     * }</pre>
     *
     * @param a the first double stream
     * @param b the second double stream
     * @param zipFunction the function to combine pairs of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, BiFunction)
     */
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three double streams into a single stream until any of them runs out of values.
     * This is a static factory method that combines elements from three double streams using a zip function.
     *
     * <p>The operation stops when any stream runs out of values. No default values are used
     * for the shorter streams. The input streams will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream stream1 = DoubleStream.of(1.0, 2.0, 3.0);
     * DoubleStream stream2 = DoubleStream.of(4.0, 5.0, 6.0);
     * DoubleStream stream3 = DoubleStream.of(7.0, 8.0, 9.0);
     * DoubleStream.zip(stream1, stream2, stream3, (x, y, z) -> x + y + z)
     *       .toDoubleList();   // returns [12.0, 15.0, 18.0]
     * }</pre>
     *
     * @param a the first double stream
     * @param b the second double stream
     * @param c the third double stream
     * @param zipFunction the function to combine triples of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Stream, TriFunction)
     */
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final DoubleTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), zipFunction).onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple double streams into a single stream until any of them runs out of values.
     * This is a static factory method that combines elements from multiple double streams using a zip function.
     *
     * <p>The operation stops when any stream runs out of values. No default values are used
     * for the shorter streams. The input streams will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream stream1 = DoubleStream.of(1.0, 2.0);
     * DoubleStream stream2 = DoubleStream.of(3.0, 4.0);
     * DoubleStream stream3 = DoubleStream.of(5.0, 6.0);
     * DoubleStream.zip(Arrays.asList(stream1, stream2, stream3),
     *       values -> values[0] + values[1] + values[2])
     *       .toDoubleList();   // returns [9.0, 12.0]
     * }</pre>
     *
     * <p><b>Note:</b> This overload boxes each combined value: {@code zipFunction} returns a boxed
     * {@link Double} which is then unboxed back to {@code double}. For performance-critical paths prefer
     * the typed two-stream / three-stream overloads (which use {@link DoubleBinaryOperator} /
     * {@link DoubleTernaryOperator} and avoid boxing).
     *
     * @param streams the collection of double streams to zip; its contents are snapshotted, and {@code null} streams are treated as empty
     * @param zipFunction the function to combine values from all the streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Collection, Function)
     */
    public static DoubleStream zip(final Collection<? extends DoubleStream> streams, final DoubleNFunction<Double> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Zips two double arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two double arrays using a zip function,
     * with default values for arrays that run out of values early.
     *
     * <p>The operation continues until all arrays are exhausted. When an array runs out of values,
     * the corresponding default value is used for the remaining iterations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] a = {1.0, 2.0};
     * double[] b = {3.0, 4.0, 5.0};
     * DoubleStream.zip(a, b, 0.0, 0.0, (x, y) -> x + y)
     *       .toDoubleList();   // returns [4.0, 6.0, 5.0]
     * }</pre>
     *
     * @param a the first double array
     * @param b the second double array
     * @param valueForNoneA the default value to use when array a runs out of values
     * @param valueForNoneB the default value to use when array b runs out of values
     * @param zipFunction the function to combine pairs of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object, Object, BiFunction)
     */
    public static DoubleStream zip(final double[] a, final double[] b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBinaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private double ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public double nextDouble() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsDouble(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips three double arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three double arrays using a zip function,
     * with default values for arrays that run out of values early.
     *
     * <p>The operation continues until all arrays are exhausted. When an array runs out of values,
     * the corresponding default value is used for the remaining iterations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] a = {1.0, 2.0};
     * double[] b = {3.0, 4.0, 5.0};
     * double[] c = {6.0, 7.0, 8.0, 9.0};
     * DoubleStream.zip(a, b, c, 0.0, 0.0, 0.0, (x, y, z) -> x + y + z)
     *       .toDoubleList();   // returns [10.0, 13.0, 13.0, 9.0]
     * }</pre>
     *
     * @param a the first double array
     * @param b the second double array
     * @param c the third double array
     * @param valueForNoneA the default value to use when array a runs out of values
     * @param valueForNoneB the default value to use when array b runs out of values
     * @param valueForNoneC the default value to use when array c runs out of values
     * @param zipFunction the function to combine triples of values from the arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object[], Object, Object, Object, TriFunction)
     */
    public static DoubleStream zip(final double[] a, final double[] b, final double[] c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTernaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b) && N.isEmpty(c)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private double ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public double nextDouble() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsDouble(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
                        cursor < cLen ? c[cursor] : valueForNoneC);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips two double iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two double iterators using a zip function,
     * with default values for iterators that run out of values early.
     *
     * <p>The operation continues until all iterators are exhausted. When an iterator runs out of values,
     * the corresponding default value is used for the remaining iterations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter1 = DoubleIterator.of(1.0, 2.0);
     * DoubleIterator iter2 = DoubleIterator.of(3.0, 4.0, 5.0);
     * DoubleStream.zip(iter1, iter2, 0.0, 0.0, (x, y) -> x + y)
     *       .toDoubleList();   // returns [4.0, 6.0, 5.0]
     * }</pre>
     *
     * @param a the first double iterator
     * @param b the second double iterator
     * @param valueForNoneA the default value to use when iterator a runs out of values
     * @param valueForNoneB the default value to use when iterator b runs out of values
     * @param zipFunction the function to combine pairs of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Object, Object, BiFunction)
     */
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBinaryOperator zipFunction) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final DoubleIterator iterA = a == null ? DoubleIterator.empty() : a;
            private final DoubleIterator iterB = b == null ? DoubleIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public double nextDouble() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsDouble(iterA.nextDouble(), iterB.hasNext() ? iterB.nextDouble() : valueForNoneB);
                } else {
                    return zipFunction.applyAsDouble(valueForNoneA, iterB.nextDouble());
                }
            }
        });
    }

    /**
     * Zips three double iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three double iterators using a zip function,
     * with default values for iterators that run out of values early.
     *
     * <p>The operation continues until all iterators are exhausted. When an iterator runs out of values,
     * the corresponding default value is used for the remaining iterations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iter1 = DoubleIterator.of(1.0, 2.0);
     * DoubleIterator iter2 = DoubleIterator.of(3.0, 4.0, 5.0);
     * DoubleIterator iter3 = DoubleIterator.of(6.0, 7.0, 8.0, 9.0);
     * DoubleStream.zip(iter1, iter2, iter3, 0.0, 0.0, 0.0, (x, y, z) -> x + y + z)
     *       .toDoubleList();   // returns [10.0, 13.0, 13.0, 9.0]
     * }</pre>
     *
     * @param a the first double iterator
     * @param b the second double iterator
     * @param c the third double iterator
     * @param valueForNoneA the default value to use when iterator a runs out of values
     * @param valueForNoneB the default value to use when iterator b runs out of values
     * @param valueForNoneC the default value to use when iterator c runs out of values
     * @param zipFunction the function to combine triples of values from the iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Iterator, Object, Object, Object, TriFunction)
     */
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final double valueForNoneA,
            final double valueForNoneB, final double valueForNoneC, final DoubleTernaryOperator zipFunction) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final DoubleIterator iterA = a == null ? DoubleIterator.empty() : a;
            private final DoubleIterator iterB = b == null ? DoubleIterator.empty() : b;
            private final DoubleIterator iterC = c == null ? DoubleIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public double nextDouble() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsDouble(iterA.nextDouble(), iterB.hasNext() ? iterB.nextDouble() : valueForNoneB,
                            iterC.hasNext() ? iterC.nextDouble() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.applyAsDouble(valueForNoneA, iterB.nextDouble(), iterC.hasNext() ? iterC.nextDouble() : valueForNoneC);
                } else {
                    return zipFunction.applyAsDouble(valueForNoneA, valueForNoneB, iterC.nextDouble());
                }
            }
        });
    }

    /**
     * Zips two double streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two double streams using a zip function,
     * with default values for streams that run out of values early.
     *
     * <p>The operation continues until all streams are exhausted. When a stream runs out of values,
     * the corresponding default value is used for the remaining iterations. The input streams will be closed
     * when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream stream1 = DoubleStream.of(1.0, 2.0);
     * DoubleStream stream2 = DoubleStream.of(3.0, 4.0, 5.0);
     * DoubleStream.zip(stream1, stream2, 0.0, 0.0, (x, y) -> x + y)
     *       .toDoubleList();   // returns [4.0, 6.0, 5.0]
     * }</pre>
     *
     * @param a the first double stream
     * @param b the second double stream
     * @param valueForNoneA the default value to use when stream a runs out of values
     * @param valueForNoneB the default value to use when stream b runs out of values
     * @param zipFunction the function to combine pairs of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Object, Object, BiFunction)
     */
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three double streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three double streams using a zip function,
     * with default values for streams that run out of values early.
     *
     * <p>The operation continues until all streams are exhausted. When a stream runs out of values,
     * the corresponding default value is used for the remaining iterations. The input streams will be closed
     * when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream stream1 = DoubleStream.of(1.0, 2.0);
     * DoubleStream stream2 = DoubleStream.of(3.0, 4.0, 5.0);
     * DoubleStream stream3 = DoubleStream.of(6.0, 7.0, 8.0, 9.0);
     * DoubleStream.zip(stream1, stream2, stream3, 0.0, 0.0, 0.0, (x, y, z) -> x + y + z)
     *       .toDoubleList();   // returns [10.0, 13.0, 13.0, 9.0]
     * }</pre>
     *
     * @param a the first double stream
     * @param b the second double stream
     * @param c the third double stream
     * @param valueForNoneA the default value to use when stream a runs out of values
     * @param valueForNoneB the default value to use when stream b runs out of values
     * @param valueForNoneC the default value to use when stream c runs out of values
     * @param zipFunction the function to combine triples of values from the streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Stream, Object, Object, Object, TriFunction)
     */
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple double streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from multiple double streams using a zip function,
     * with default values for streams that run out of values early.
     *
     * <p>The operation continues until all streams are exhausted. When a stream runs out of values,
     * the corresponding default value from the valuesForNone array is used for the remaining iterations.
     * The input streams will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream stream1 = DoubleStream.of(1.0, 2.0);
     * DoubleStream stream2 = DoubleStream.of(3.0, 4.0, 5.0);
     * DoubleStream stream3 = DoubleStream.of(6.0, 7.0, 8.0, 9.0);
     * double[] defaults = {0.0, 0.0, 0.0};
     * DoubleStream.zip(Arrays.asList(stream1, stream2, stream3), defaults,
     *       values -> values[0] + values[1] + values[2])
     *       .toDoubleList();   // returns [10.0, 13.0, 13.0, 9.0]
     * }</pre>
     *
     * <p><b>Note:</b> This overload boxes each combined value: {@code zipFunction} returns a boxed
     * {@link Double} which is then unboxed back to {@code double}. For performance-critical paths prefer
     * the typed two-stream / three-stream overloads (which use {@link DoubleBinaryOperator} /
     * {@link DoubleTernaryOperator} and avoid boxing).
     *
     * @param streams the collection of double streams to zip; its contents are snapshotted, and {@code null} streams are treated as empty
     * @param valuesForNone the array of default values to use when streams run out of values
     * @param zipFunction the function to combine values from all the streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Collection, List, Function)
     */
    public static DoubleStream zip(final Collection<? extends DoubleStream> streams, final double[] valuesForNone, final DoubleNFunction<Double> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, valuesForNone, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * Merges two double arrays into a single DoubleStream based on a selector function.
     * The selector function determines which element to select next from the two arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted arrays maintaining order
     * double[] a = {1.0, 3.0, 5.0};
     * double[] b = {2.0, 4.0, 6.0};
     * DoubleStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Alternate between arrays
     * double[] arr1 = {1.0, 3.0, 5.0};
     * double[] arr2 = {2.0, 4.0, 6.0};
     * final boolean[] toggle = {false};
     * DoubleStream.merge(arr1, arr2, (x, y) -> {
     *     toggle[0] = !toggle[0];
     *     return toggle[0] ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
     * }).toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Always take larger value
     * DoubleStream.merge(
     *     new double[]{5.0, 3.0, 1.0},
     *     new double[]{4.0, 2.0, 0.0},
     *     (x, y) -> x > y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND
     * ).toArray();   // returns [5.0, 4.0, 3.0, 2.0, 1.0, 0.0]
     * }</pre>
     *
     * @param a the first double array
     * @param b the second double array
     * @param nextSelector a function that determines which element should be selected next. Must not be {@code null}.
     *                     Returns MergeResult.TAKE_FIRST to select from array a, otherwise selects from array b
     * @return a DoubleStream containing the merged elements
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Object[], Object[], BiFunction)
     */
    public static DoubleStream merge(final double[] a, final double[] b, final DoubleBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public double nextDouble() {
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
     * Merges three double arrays into a single DoubleStream based on a selector function.
     * The selector function determines which element to select next from the arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted arrays
     * double[] a = {1.0, 4.0, 7.0};
     * double[] b = {2.0, 5.0, 8.0};
     * double[] c = {3.0, 6.0, 9.0};
     * DoubleStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     *
     * // Merge with custom selection logic
     * DoubleStream.merge(
     *     new double[]{10.0, 20.0},
     *     new double[]{5.0, 15.0},
     *     new double[]{12.0, 18.0},
     *     (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND
     * ).toArray();
     * }</pre>
     *
     * @param a the first double array
     * @param b the second double array
     * @param c the third double array
     * @param nextSelector a function that determines which element should be selected next. Must not be {@code null}.
     *                     Returns MergeResult.TAKE_FIRST to select the first parameter, otherwise selects the second
     * @return a DoubleStream containing the merged elements
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Object[], Object[], Object[], BiFunction)
     */
    public static DoubleStream merge(final double[] a, final double[] b, final double[] c, final DoubleBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), DoubleStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * Merges two DoubleIterators into a single DoubleStream based on a selector function.
     * The selector function determines which element to select next from the two iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two iterators maintaining sorted order
     * DoubleIterator iter1 = DoubleIterator.of(1.0, 3.0, 5.0);
     * DoubleIterator iter2 = DoubleIterator.of(2.0, 4.0, 6.0);
     * DoubleStream.merge(iter1, iter2, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Merge from different sources
     * DoubleList list = DoubleList.of(1.0, 2.0, 3.0);
     * double[] array = {4.0, 5.0, 6.0};
     * DoubleStream.merge(
     *     list.iterator(),
     *     DoubleIterator.of(array),
     *     (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND
     * ).sum();   // returns 21.0
     * }</pre>
     *
     * @param a the first DoubleIterator
     * @param b the second DoubleIterator
     * @param nextSelector a function that determines which element should be selected next. Must not be {@code null}.
     *                     Returns MergeResult.TAKE_FIRST to select from iterator a, otherwise selects from iterator b
     * @return a DoubleStream containing the merged elements
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Iterator, Iterator, BiFunction)
     */
    public static DoubleStream merge(final DoubleIterator a, final DoubleIterator b, final DoubleBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final DoubleIterator iterA = a == null ? DoubleIterator.empty() : a;
            private final DoubleIterator iterB = b == null ? DoubleIterator.empty() : b;
            private double nextA = 0;
            private double nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public double nextDouble() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.nextDouble())) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextDouble()), nextB) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextDouble()), (nextB = iterB.nextDouble())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.nextDouble();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.nextDouble();
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     * Merges three DoubleIterators into a single DoubleStream based on a selector function.
     * The selector function determines which element to select next from the iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three iterators
     * DoubleIterator iter1 = DoubleIterator.of(1.0, 4.0, 7.0);
     * DoubleIterator iter2 = DoubleIterator.of(2.0, 5.0, 8.0);
     * DoubleIterator iter3 = DoubleIterator.of(3.0, 6.0, 9.0);
     * DoubleStream.merge(iter1, iter2, iter3, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     * }</pre>
     *
     * @param a the first DoubleIterator
     * @param b the second DoubleIterator
     * @param c the third DoubleIterator
     * @param nextSelector a function that determines which element should be selected next. Must not be {@code null}.
     *                     Returns MergeResult.TAKE_FIRST to select the first parameter, otherwise selects the second
     * @return a DoubleStream containing the merged elements
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Iterator, Iterator, Iterator, BiFunction)
     */
    public static DoubleStream merge(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final DoubleBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * Merges two DoubleStreams into a single DoubleStream based on a selector function.
     * The selector function determines which element to select next from the two streams.
     * The input streams will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted streams
     * DoubleStream s1 = DoubleStream.of(1.0, 3.0, 5.0);
     * DoubleStream s2 = DoubleStream.of(2.0, 4.0, 6.0);
     * DoubleStream.merge(s1, s2, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Merge with complex selection logic
     * DoubleStream evens = DoubleStream.iterate(2.0, d -> d + 2).limit(5);
     * DoubleStream odds = DoubleStream.iterate(1.0, d -> d + 2).limit(5);
     * DoubleStream.merge(evens, odds, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0]
     * }</pre>
     *
     * @param a the first DoubleStream
     * @param b the second DoubleStream
     * @param nextSelector a function that determines which element should be selected next. Must not be {@code null}.
     *                     Returns MergeResult.TAKE_FIRST to select from stream a, otherwise selects from stream b
     * @return a DoubleStream containing the merged elements
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Stream, Stream, BiFunction)
     */
    public static DoubleStream merge(final DoubleStream a, final DoubleStream b, final DoubleBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return merge(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(a, b));
    }

    /**
     * Merges three DoubleStreams into a single DoubleStream based on a selector function.
     * The selector function determines which element to select next from the streams.
     * The input streams will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted streams
     * DoubleStream s1 = DoubleStream.of(1.0, 4.0, 7.0);
     * DoubleStream s2 = DoubleStream.of(2.0, 5.0, 8.0);
     * DoubleStream s3 = DoubleStream.of(3.0, 6.0, 9.0);
     * DoubleStream.merge(s1, s2, s3, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     * }</pre>
     *
     * @param a the first DoubleStream
     * @param b the second DoubleStream
     * @param c the third DoubleStream
     * @param nextSelector a function that determines which element should be selected next. Must not be {@code null}.
     *                     Returns MergeResult.TAKE_FIRST to select the first parameter, otherwise selects the second
     * @return a DoubleStream containing the merged elements
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Stream, Stream, Stream, BiFunction)
     */
    public static DoubleStream merge(final DoubleStream a, final DoubleStream b, final DoubleStream c, final DoubleBiFunction<MergeResult> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * Merges a collection of DoubleStreams into a single DoubleStream based on a selector function.
     * The selector function determines which element to select next from the streams.
     * The input streams will be closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge multiple sorted streams
     * List<DoubleStream> streams = Arrays.asList(
     *     DoubleStream.of(1.0, 4.0, 7.0),
     *     DoubleStream.of(2.0, 5.0, 8.0),
     *     DoubleStream.of(3.0, 6.0, 9.0)
     * );
     * DoubleStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     *
     * // Merge dynamically created streams
     * List<DoubleStream> dynamicStreams = new ArrayList<>();
     * for (int i = 0; i < 3; i++) {
     *     final int idx = i;
     *     dynamicStreams.add(DoubleStream.iterate(idx + 1.0, d -> d + 3).limit(3));
     * }
     * DoubleStream.merge(dynamicStreams, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();
     * }</pre>
     *
     * @param streams the collection of DoubleStreams to merge; a {@code null} collection and {@code null} elements are treated as empty
     * @param nextSelector a function that determines which element should be selected next. Must not be {@code null}.
     *                     Returns MergeResult.TAKE_FIRST to select the first parameter, otherwise selects the second
     * @return a DoubleStream containing the merged elements
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Collection, BiFunction)
     */
    public static DoubleStream merge(final Collection<? extends DoubleStream> streams, final DoubleBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (N.isEmpty(streams)) {
            return empty();
        } else if (streams.size() == 1) {
            final DoubleStream stream = streams.iterator().next();
            return stream == null ? empty() : stream;
        } else if (streams.size() == 2) {
            final Iterator<? extends DoubleStream> iter = streams.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends DoubleStream> iter = streams.iterator();
        DoubleStream result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * An extension class for DoubleStream to provide additional functionality.
     */
    public abstract static class DoubleStreamEx extends DoubleStream {
        /**
         * Constructor for DoubleStreamEx.
         *
         * @param sorted whether the stream is sorted
         * @param closeHandlers collection of close handlers
         */
        private DoubleStreamEx(final boolean sorted, final Collection<LocalRunnable> closeHandlers) { //NOSONAR
            super(sorted, closeHandlers);
            // Factory class.
        }
    }
}
