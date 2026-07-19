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

import java.nio.FloatBuffer;
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
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.FF;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatBiPredicate;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatNFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatSupplier;
import com.landawn.abacus.util.function.FloatTernaryOperator;
import com.landawn.abacus.util.function.FloatToDoubleFunction;
import com.landawn.abacus.util.function.FloatToIntFunction;
import com.landawn.abacus.util.function.FloatToLongFunction;
import com.landawn.abacus.util.function.FloatTriPredicate;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.function.ObjFloatConsumer;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * A specialized stream implementation for processing sequences of primitive {@code float} values with functional-style operations.
 * This abstract class extends {@link StreamBase} and provides comprehensive float-specific operations including
 * filtering, mapping, reduction, and collection operations optimized for float data types.
 *
 * <p>FloatStream represents a sequence of float elements supporting sequential and parallel aggregate operations.
 * It provides a more efficient alternative to generic {@link Stream} when working specifically with float values,
 * avoiding boxing/unboxing overhead and offering float-specific utility methods for numerical computations.
 *
 * <p><b>Floating-point special values:</b> Elements may include {@link Float#NaN},
 * {@link Float#POSITIVE_INFINITY}, {@link Float#NEGATIVE_INFINITY}, and negative zero ({@code -0.0f}).
 * Aggregate operations such as {@link #sum()}, {@link #average()}, {@link #min()}, and {@link #max()}
 * follow IEEE 754 semantics: any {@code NaN} element propagates to the result as {@code NaN}.
 * Ordering uses {@link Float#compare(float, float)}, which treats {@code NaN} as greater than any
 * other value (including positive infinity) and considers {@code -0.0f} less than {@code +0.0f}.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed for float operations, preventing ClassCastException</li>
 *   <li><b>Performance:</b> Optimized for float primitives, avoiding boxing overhead</li>
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
 *   <li><b>Graphics Programming:</b> Processing coordinates, color values, transformation matrices</li>
 *   <li><b>Performance Analysis:</b> Processing measurement data, metrics analysis</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic float stream operations
 * FloatStream.of(1.5f, 2.7f, 3.1f, 4.9f, 5.2f)
 *     .filter(f -> f > 3.0f)   // keeps values > 3.0
 *     .map(f -> f * 2)         // maps each value to its double
 *     .sum();                  // returns sum 26.4
 *
 * // Statistical operations
 * FloatSummaryStatistics stats = FloatStream.of(temperatureReadings)
 *     .filter(temp -> temp > 0)   // filters valid temperatures
 *     .summaryStatistics();       // gets min, max, avg, count
 *
 * // Mathematical operations with parallel processing
 * double result = FloatStream.iterate(1.0f, f -> f * 1.1f)
 *     .limit(1000)               // keeps 1000 values
 *     .parallel()                // switches to parallel processing
 *     .filter(f -> f < 100)      // filters values < 100
 *     .mapToDouble(Math::sqrt)   // maps via square root
 *     .average()                 // returns average
 *     .orElse(0.0);              // returns 0.0 if empty
 *
 * // Integration with other stream types
 * IntStream counts = FloatStream.of(coordinates)
 *     .mapToInt(coord -> (int) Math.ceil(coord)) // maps to ceiling integers
 *     .distinct();                               // removes duplicates
 *
 * // Processing a sequence of float values
 * FloatStream.of(1.5f, 2.5f, 3.5f)
 *     .takeWhile(f -> f >= 0)            // keeps values until negative number
 *     .mapToObj(String::valueOf)         // maps to strings
 *     .forEach(System.out::println);     // prints each value
 * }</pre>
 *
 * <p><b>Float-Specific Operations:</b>
 * <ul>
 *   <li>{@code filter(FloatPredicate)} - Filter floats based on conditions</li>
 *   <li>{@code map(FloatUnaryOperator)} - Transform float values</li>
 *   <li>{@code reduce(FloatBinaryOperator)} - Reduce to single float value</li>
 *   <li>{@code sum()} (returns {@code double}), {@code average()} (returns {@code OptionalDouble}),
 *       {@code min()}, {@code max()} - Mathematical aggregations. Note: {@code sum()} and
 *       {@code average()} return {@code double}/{@code OptionalDouble} (not {@code float}) for
 *       precision; use {@link #asDoubleStream()} when a JDK {@code DoubleStream} is needed.</li>
 *   <li>{@code mapToInt()}, {@code mapToLong()}, {@code mapToDouble()} - Convert to other primitive streams</li>
 *   <li>{@code boxed()} - Convert to Stream&lt;Float&gt;</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li>Use FloatStream instead of {@code Stream<Float>} to avoid boxing overhead</li>
 *   <li>Parallel processing benefits large datasets (typically &gt; 10,000 elements)</li>
 *   <li>Sequential processing is more efficient for small datasets and simple operations</li>
 *   <li>Lazy evaluation means intermediate operations are not executed until terminal operations</li>
 * </ul>
 *
 * @see StreamBase
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see Stream
 * @see FloatIterator
 * @see FloatList
 * @see FloatSummaryStatistics
 * @see OptionalFloat
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@LazyEvaluation
public abstract class FloatStream extends StreamBase<Float, float[], FloatPredicate, FloatConsumer, OptionalFloat, IndexedFloat, FloatIterator, FloatStream> {

    static final Random RAND = new SecureRandom();

    FloatStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
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
     * FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
     *       .filter(x -> x > 3.0f)
     *       .toArray();   // returns [4.0f, 5.0f]
     *
     * FloatStream.of(1.0f, Float.NaN, 3.0f, Float.NaN, 5.0f)
     *       .filter(x -> !Float.isNaN(x))
     *       .toArray();   // returns [1.0, 3.0, 5.0]
     *
     * FloatStream.empty().filter(x -> x > 0).toArray();   // returns []
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
    public abstract FloatStream filter(final FloatPredicate predicate);

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
     * FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 2.0f, 1.0f)
     *       .takeWhile(x -> x < 4.0f)
     *       .toArray();   // returns [1.0f, 2.0f, 3.0f]
     *
     * FloatStream.of(Float.NaN, 1.0f, 2.0f)
     *       .takeWhile(x -> !Float.isNaN(x))
     *       .toArray();   // returns [] (NaN fails predicate immediately)
     *
     * FloatStream.empty().takeWhile(x -> true).toArray();   // returns []
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
    public abstract FloatStream takeWhile(final FloatPredicate predicate);

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
     * FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 2.0f, 1.0f)
     *       .dropWhile(x -> x < 4.0f)
     *       .toArray();   // returns [4.0f, 5.0f, 2.0f, 1.0f]
     *
     * FloatStream.of(1.0f, 2.0f, 3.0f)
     *       .dropWhile(x -> x > 10.0f)
     *       .toArray();   // returns [1.0f, 2.0f, 3.0f] (nothing dropped)
     *
     * FloatStream.empty().dropWhile(x -> true).toArray();   // returns []
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
    public abstract FloatStream dropWhile(final FloatPredicate predicate);

    /**
     * Returns a FloatStream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each float element using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f)
     *       .map(x -> x * 2.0f)
     *       .toArray();   // returns [2.0f, 4.0f, 6.0f, 8.0f]
     *
     * FloatStream.of(Float.NaN, 2.0f, 3.0f)
     *       .map(x -> Float.isNaN(x) ? 0.0f : x * 2.0f)
     *       .toArray();   // returns [0.0f, 4.0f, 6.0f]
     *
     * FloatStream.empty().map(x -> x * 2.0f).toArray();   // returns []
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to float
     * @return a new FloatStream consisting of the results of applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream map(FloatUnaryOperator mapper);

    /**
     * Returns an IntStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each float element to an int value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert floats to ints (with truncation)
     * FloatStream.of(1.9f, 2.5f, 3.1f, 4.7f)
     *       .mapToInt(f -> (int) f)
     *       .toArray();   // returns [1, 2, 3, 4]
     *
     * // Round floats to nearest int
     * FloatStream.of(1.5f, 2.5f, 3.5f)
     *       .mapToInt(Math::round)
     *       .toArray();   // returns [2, 3, 4]
     *
     * // Convert NaN to 0
     * FloatStream.of(Float.NaN, 2.5f, 3.0f)
     *       .mapToInt(f -> Float.isNaN(f) ? 0 : (int) f)
     *       .toArray();   // returns [0, 2, 3]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to int
     * @return a new IntStream consisting of the results of applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see #map(FloatUnaryOperator)
     * @see #mapToObj(FloatFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapToInt(FloatToIntFunction mapper);

    /**
     * Returns a LongStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each float element to a long value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert floats to longs (with truncation)
     * FloatStream.of(1.9f, 2.5f, 3.1f, 4.7f)
     *       .mapToLong(f -> (long) f)
     *       .toArray();   // returns [1L, 2L, 3L, 4L]
     *
     * // Scale and convert to long
     * FloatStream.of(1.5f, 2.7f, 3.9f)
     *       .mapToLong(f -> (long) (f * 1000))
     *       .toArray();   // returns [1500L, 2700L, 3900L]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to long
     * @return a new LongStream consisting of the results of applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see #map(FloatUnaryOperator)
     * @see #mapToDouble(FloatToDoubleFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream mapToLong(FloatToLongFunction mapper);

    /**
     * Returns a DoubleStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each float element to a double value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert floats to doubles for higher precision
     * FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f)
     *       .mapToDouble(f -> (double) f)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0]
     *
     * // Apply mathematical functions requiring double precision
     * FloatStream.of(1.0f, 4.0f, 9.0f, 16.0f)
     *       .mapToDouble(Math::sqrt)
     *       .toArray();   // returns [1.0, 2.0, 3.0, 4.0]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to double
     * @return a new DoubleStream consisting of the results of applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see #map(FloatUnaryOperator)
     * @see #mapToLong(FloatToLongFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream mapToDouble(FloatToDoubleFunction mapper);

    /**
     * Returns an object-valued Stream consisting of the results of applying the
     * given function to the elements of this stream. This is an intermediate operation
     * that transforms each float element to an object of type T using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert floats to strings
     * FloatStream.of(1.5f, 2.7f, 3.9f, 4.2f)
     *       .mapToObj(String::valueOf)
     *       .toList();   // returns ["1.5", "2.7", "3.9", "4.2"]
     *
     * // Format floats as currency
     * FloatStream.of(19.99f, 29.99f, 39.99f)
     *       .mapToObj(f -> String.format("$%.2f", f))
     *       .toList();   // returns ["$19.99", "$29.99", "$39.99"]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to T
     * @return a new Stream of objects resulting from applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see #map(FloatUnaryOperator)
     * @see #mapToInt(FloatToIntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> mapToObj(FloatFunction<? extends T> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each non-null mapped stream is
     * closed after its contents are consumed or when the resulting stream is closed.
     * A null mapped stream is treated as empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each number to include its double
     * FloatStream.of(1.0f, 2.0f, 3.0f)
     *       .flatMap(f -> FloatStream.of(f, f * 2))
     *       .toFloatList();   // returns [1.0, 2.0, 2.0, 4.0, 3.0, 6.0]
     *
     * // Duplicate each element
     * FloatStream.of(1.5f, 2.5f, 3.5f)
     *       .flatMap(f -> FloatStream.of(f, f))
     *       .toFloatList();   // returns [1.5, 1.5, 2.5, 2.5, 3.5, 3.5]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to FloatStream
     * @return a new {@link FloatStream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#flatMap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream flatMap(FloatFunction<? extends FloatStream> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped {@link java.util.Collection Collection} produced by applying the provided mapping function to each element.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(FloatFunction) flatMap} (uppercase 'M') - transforms each element into a {@link FloatStream}.</li>
     *   <li>{@link #flatmap(FloatFunction) flatmap} (this method, lowercase 'm') - transforms each element into a {@link java.util.Collection Collection} of {@link Float}.</li>
     *   <li>{@link #flatMapArray(FloatFunction) flatMapArray} - transforms each element into a {@code float[]}.</li>
     * </ul>
     *
     * <p>Each element is transformed into a collection, and all resulting collections are flattened
     * into a single {@code FloatStream}. If the mapper returns {@code null} or an empty collection
     * for a given element, that element contributes no values to the resulting stream.
     *
     * <p><b>Null elements within a collection</b> returned by the mapper are <em>not</em> propagated as
     * exceptions. Each {@code null} {@link Float} entry is replaced with the primitive default value
     * {@code 0f} so that this operation never throws {@link NullPointerException} when unboxing.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream.of(1f, 2f)
     *            .flatmap(x -> Arrays.asList(x, x * 10f))
     *            .toArray();   // [1.0, 10.0, 2.0, 20.0]
     *
     * // null elements in a returned collection become 0f
     * FloatStream.of(1f)
     *            .flatmap(x -> Arrays.asList((Float) null, 7f))
     *            .toArray();   // [0.0, 7.0]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to {@code Collection<Float>}
     * @return a new {@code FloatStream} consisting of the flattened contents of the collections produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(FloatFunction)
     * @see #flatMapArray(FloatFunction)
     * @see Stream#flatmap(java.util.function.Function)
     */
    // @ai-ignore flatmap/flatMap naming - intentional: flatMap maps to FloatStream, flatmap maps to Collection<Float>, flatMapArray maps to float[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream flatmap(FloatFunction<? extends Collection<Float>> mapper); //NOSONAR

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
     * FloatStream.of(2.0f, 3.0f)
     *       .flatMapArray(f -> new float[] {f, f * 10, f * 100})
     *       .toFloatList();   // returns [2.0, 20.0, 200.0, 3.0, 30.0, 300.0]
     *
     * // Generate sequences based on each number
     * FloatStream.of(1.0f, 2.0f, 3.0f)
     *       .flatMapArray(f -> new float[] {f, f + 10})
     *       .toFloatList();   // returns [1.0, 11.0, 2.0, 12.0, 3.0, 13.0]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to float[]
     * @return a new {@code FloatStream} consisting of the flattened contents of the arrays produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(FloatFunction)
     * @see #flatMapToInt(FloatFunction)
     * @see #flatMapToObj(FloatFunction)
     */
    // @ai-ignore flatMapArray/flatMap naming - intentional: flatMap maps to FloatStream, flatMapArray maps to float[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream flatMapArray(FloatFunction<float[]> mapper); //NOSONAR

    /**
     * Returns an IntStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each non-null mapped stream is
     * closed after its contents are consumed or when the resulting stream is closed.
     * A null mapped stream is treated as empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each float to floor and ceiling integers
     * FloatStream.of(1.5f, 2.5f, 3.5f)
     *       .flatMapToInt(f -> IntStream.of((int) f, (int) Math.ceil(f)))
     *       .toIntList();   // returns [1, 2, 2, 3, 3, 4]
     *
     * // Generate integer range based on float value
     * FloatStream.of(2.0f, 3.0f)
     *       .flatMapToInt(f -> IntStream.range(0, (int) f))
     *       .toIntList();   // returns [0, 1, 0, 1, 2]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to IntStream
     * @return a new {@link IntStream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatMapToInt(FloatFunction<? extends IntStream> mapper);

    /**
     * Returns a LongStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each non-null mapped stream is
     * closed after its contents are consumed or when the resulting stream is closed.
     * A null mapped stream is treated as empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each float to floor and ceiling longs
     * FloatStream.of(1.5f, 2.5f, 3.5f)
     *       .flatMapToLong(f -> LongStream.of((long) f, (long) Math.ceil(f)))
     *       .toLongList();   // returns [1, 2, 2, 3, 3, 4]
     *
     * // Scale and generate range
     * FloatStream.of(2.5f, 3.0f)
     *       .flatMapToLong(f -> LongStream.range(0, (long) f))
     *       .toLongList();   // returns [0, 1, 0, 1, 2]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to LongStream
     * @return a new {@link LongStream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract LongStream flatMapToLong(FloatFunction<? extends LongStream> mapper);

    /**
     * Returns a DoubleStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each non-null mapped stream is
     * closed after its contents are consumed or when the resulting stream is closed.
     * A null mapped stream is treated as empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each float to double with transformations
     * FloatStream.of(1.5f, 2.5f, 3.5f)
     *       .flatMapToDouble(f -> DoubleStream.of(f, f * 2.0))
     *       .toDoubleList();   // returns [1.5, 3.0, 2.5, 5.0, 3.5, 7.0]
     *
     * // Generate double range from float
     * FloatStream.of(2.0f, 3.0f)
     *       .flatMapToDouble(f -> DoubleStream.of(f, f + 0.5, f + 1.0))
     *       .toDoubleList();   // returns [2.0, 2.5, 3.0, 3.0, 3.5, 4.0]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to DoubleStream
     * @return a new {@link DoubleStream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract DoubleStream flatMapToDouble(FloatFunction<? extends DoubleStream> mapper);

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element. Each non-null mapped stream is
     * closed after its contents are consumed or when the resulting stream is closed.
     * A null mapped stream is treated as empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each float to multiple string representations
     * FloatStream.of(1.5f, 2.5f, 3.5f)
     *       .flatMapToObj(f -> Stream.of(String.valueOf(f), String.valueOf(f * 2)))
     *       .toList();   // returns ["1.5", "3.0", "2.5", "5.0", "3.5", "7.0"]
     *
     * // Generate objects based on float values
     * FloatStream.of(1.0f, 2.0f)
     *       .flatMapToObj(f -> Stream.of("Value: " + f, "Double: " + (f * 2)))
     *       .toList();   // returns ["Value: 1.0", "Double: 2.0", "Value: 2.0", "Double: 4.0"]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to Stream
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapToObj(FloatFunction<? extends Stream<? extends T>> mapper); //NOSONAR

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped collection produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each float to collection of strings
     * FloatStream.of(1.5f, 2.5f, 3.5f)
     *       .flatmapToObj(f -> Arrays.asList(String.valueOf(f), String.valueOf(f * 2)))
     *       .toList();   // returns ["1.5", "3.0", "2.5", "5.0", "3.5", "7.0"]
     *
     * // Generate multiple objects from each float
     * FloatStream.of(1.0f, 2.0f)
     *       .flatmapToObj(f -> List.of("Val:" + f, "x2:" + (f * 2)))
     *       .toList();   // returns ["Val:1.0", "x2:2.0", "Val:2.0", "x2:4.0"]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to Collection
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the collections produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatmapToObj(FloatFunction<? extends Collection<? extends T>> mapper); //NOSONAR

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped array produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each float to array of strings
     * FloatStream.of(1.5f, 2.5f, 3.5f)
     *       .flatMapArrayToObj(f -> new String[] { String.valueOf(f), String.valueOf(f * 2) })
     *       .toList();   // returns ["1.5", "3.0", "2.5", "5.0", "3.5", "7.0"]
     *
     * // Generate multiple objects from each float
     * FloatStream.of(1.0f, 2.0f)
     *       .flatMapArrayToObj(f -> new String[] {"Value: " + f, "Doubled: " + (f * 2)})
     *       .toList();   // returns ["Value: 1.0", "Doubled: 2.0", "Value: 2.0", "Doubled: 4.0"]
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
     * @param mapper a non-interfering, stateless function that transforms each element from float to T[]
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the arrays produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapArrayToObj(FloatFunction<T[]> mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the elements of this stream, where the mapper function returns an OptionalFloat.
     * Only non-empty optional values are included in the returned stream.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] safeDivisions = FloatStream.of(10f, 0f, 20f, 5f)
     *     .mapPartial(f -> f != 0 ? OptionalFloat.of(100f / f) : OptionalFloat.empty())
     *     .toArray();   // returns [10.0, 5.0, 20.0] (skips division by zero)
     *
     * float[] validRoots = FloatStream.of(4f, -1f, 9f, -4f, 16f)
     *     .mapPartial(f -> f >= 0 ? OptionalFloat.of((float) Math.sqrt(f)) : OptionalFloat.empty())
     *     .toArray();   // returns [2.0, 3.0, 4.0] (only positive numbers)
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
     * @param mapper a function to apply to each element which produces an OptionalFloat
     * @return a new FloatStream with the non-empty mapped elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream mapPartial(FloatFunction<OptionalFloat> mapper);

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
     * float[] ranges = FloatStream.of(1f, 2f, 3f, 10f, 11f, 20f, 21f)
     *     .rangeMap((first, next) -> next - first < 2, (first, last) -> last - first)
     *     .toArray();   // returns [1.0, 0.0, 1.0, 1.0] (range sizes)
     *
     * float[] sums = FloatStream.of(1f, 2f, 5f, 6f, 7f, 15f)
     *     .rangeMap((first, next) -> next - first < 3, (first, last) -> first + last)
     *     .toArray();   // returns [3.0, 12.0, 30.0] (first + last of each range)
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
     * @param sameRange a {@code non-null} predicate that determines if the next element belongs to the same range as the first element of the current range.
     *              The first argument tested by sameRange is the first(not the last) element of the current range, and the second argument is the next element to check.
     *              If {@code true} is returned, the next element belongs to the same range as the first element.
     * @param mapper a {@code non-null} function that maps a range (defined by its first and last element) to an output element
     * @return a new stream consisting of the results of applying the mapper function to each range of elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract FloatStream rangeMap(final FloatBiPredicate sameRange, final FloatBinaryOperator mapper);

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
     * Stream<String> rangeDescriptions = FloatStream.of(1f, 2f, 3f, 10f, 11f, 20f)
     *     .rangeMapToObj((first, next) -> next - first < 2,
     *                    (first, last) -> String.format("[%.1f-%.1f]", first, last));
     *     // returns Stream of ["[1.0-2.0]", "[3.0-3.0]", "[10.0-11.0]", "[20.0-20.0]"]
     *
     * Stream<Pair<Float, Float>> rangePairs = FloatStream.of(1f, 2f, 5f, 6f, 7f)
     *     .rangeMapToObj((first, next) -> next - first < 3,
     *                    (first, last) -> Pair.of(first, last));
     *     // returns Stream of Pair objects
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
     * @param sameRange a {@code non-null} predicate that determines if the next element belongs to the same range as the first element of the current range.
     *              The first argument tested by sameRange is the first(not the last) element of the current range, and the second argument is the next element to check.
     *              If {@code true} is returned, the next element belongs to the same range as the first element.
     * @param mapper a {@code non-null} function that maps a range (defined by its first and last element) to an output object of type T
     * @return a new stream consisting of the results of applying the mapper function to each range of elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract <T> Stream<T> rangeMapToObj(final FloatBiPredicate sameRange, final FloatBiFunction<? extends T> mapper);

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
     * Stream<FloatList> groups = FloatStream.of(1f, 2f, 5f, 6f, 7f, 10f)
     *     .collapse((prev, curr) -> curr - prev < 3);
     *     // returns Stream of [[1.0, 2.0], [5.0, 6.0, 7.0], [10.0]]
     *
     * Stream<FloatList> sameValues = FloatStream.of(1f, 1f, 2f, 2f, 2f, 3f)
     *     .collapse((a, b) -> a == b);
     *     // returns Stream of [[1.0, 1.0], [2.0, 2.0, 2.0], [3.0]]
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
    public abstract Stream<FloatList> collapse(final FloatBiPredicate collapsible);

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
     * float[] sums = FloatStream.of(1f, 2f, 5f, 6f, 7f, 10f)
     *     .collapse((prev, curr) -> curr - prev < 3, (a, b) -> a + b)
     *     .toArray();   // returns [3.0, 18.0, 10.0]
     *
     * float[] maxes = FloatStream.of(1f, 3f, 2f, 8f, 7f, 15f)
     *     .collapse((prev, curr) -> curr - prev < 5, Float::max)
     *     .toArray();   // returns [3.0, 8.0, 15.0]
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
    public abstract FloatStream collapse(final FloatBiPredicate collapsible, final FloatBinaryOperator mergeFunction);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * Elements for which the predicate returns {@code true} when applied with the first and last elements of the group are merged into a single value.
     * The collapsible predicate takes three elements: the first and last elements of the group, and the next element in the stream.
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 2, 5, 6, 7, 10] and the predicate
     * tests whether the difference between the first element of the group and the next element is less than 3,
     * and the merge function sums the elements, the resulting stream will contain [3, 18, 10].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] collapsed = FloatStream.of(1f, 2f, 3f, 10f, 11f, 20f)
     *     .collapse((first, last, next) -> next - first < 5, (a, b) -> a + b)
     *     .toArray();   // returns [6.0, 21.0, 20.0]
     *
     * float[] ranges = FloatStream.of(1f, 2f, 3f, 4f, 10f, 11f)
     *     .collapse((first, last, next) -> next - first <= 5, Float::max)
     *     .toArray();   // returns [4.0, 11.0]
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
    public abstract FloatStream collapse(final FloatTriPredicate collapsible, final FloatBinaryOperator mergeFunction);

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
     * float[] runningSum = FloatStream.of(1f, 2f, 3f, 4f)
     *     .scan((a, b) -> a + b)
     *     .toArray();   // returns [1.0, 3.0, 6.0, 10.0]
     *
     * float[] runningProduct = FloatStream.of(1f, 2f, 3f, 4f)
     *     .scan((a, b) -> a * b)
     *     .toArray();   // returns [1.0, 2.0, 6.0, 24.0]
     *
     * float[] runningMax = FloatStream.of(1f, 5f, 3f, 7f, 2f)
     *     .scan(Float::max)
     *     .toArray();   // returns [1.0, 5.0, 5.0, 7.0, 7.0]
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
     * @param accumulator a {@code FloatBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code FloatStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract FloatStream scan(final FloatBinaryOperator accumulator);

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
     * float[] withInit = FloatStream.of(1f, 2f, 3f, 4f)
     *     .scan(10f, (a, b) -> a + b)
     *     .toArray();   // returns [11.0, 13.0, 16.0, 20.0]
     *
     * float[] cumulativeWithBase = FloatStream.of(1f, 2f, 3f)
     *     .scan(100f, (a, b) -> a * b)
     *     .toArray();   // returns [100.0, 200.0, 600.0]
     *
     * float[] emptyWithInit = FloatStream.empty()
     *     .scan(10f, (a, b) -> a + b)
     *     .toArray();   // returns [] (empty array, init is not included for empty stream)
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
     * @param accumulator a {@code FloatBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code FloatStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(Object, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract FloatStream scan(final float init, final FloatBinaryOperator accumulator);

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
     * float[] withInitIncluded = FloatStream.of(1f, 2f, 3f)
     *     .scan(10f, true, (a, b) -> a + b)
     *     .toArray();   // returns [10.0, 11.0, 13.0, 16.0] (init value included)
     *
     * float[] withInitNotIncluded = FloatStream.of(1f, 2f, 3f)
     *     .scan(10f, false, (a, b) -> a + b)
     *     .toArray();   // returns [11.0, 13.0, 16.0] (init value not included)
     *
     * float[] emptyIncluded = FloatStream.empty()
     *     .scan(10f, true, (a, b) -> a + b)
     *     .toArray();   // returns [10.0] (only init value)
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
     * @param init the initial value used as the starting accumulated value. When {@code initIncluded} is {@code true}, it is also emitted as the first element of the returned stream.
     * @param initIncluded a boolean value that determines if the initial value should be included as the first element in the returned stream.
     * @param accumulator a {@code FloatBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code FloatStream} consisting of the results of the scan operation on the elements of the original stream.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(Object, boolean, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract FloatStream scan(final float init, final boolean initIncluded, final FloatBinaryOperator accumulator);

    /**
     * Returns a stream consisting of the specified elements followed by the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] result = FloatStream.of(3f, 4f, 5f)
     *     .prepend(1f, 2f)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * float[] withHeader = FloatStream.of(10f, 20f)
     *     .prepend(0f)
     *     .toArray();   // returns [0.0, 10.0, 20.0]
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
    public abstract FloatStream prepend(final float... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] result = FloatStream.of(1f, 2f, 3f)
     *     .append(4f, 5f)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0]
     *
     * float[] withTrailer = FloatStream.of(10f, 20f)
     *     .append(99f)
     *     .toArray();   // returns [10.0, 20.0, 99.0]
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
    public abstract FloatStream append(final float... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended if this stream is empty.
     * If this stream is not empty, returns this stream unchanged.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] nonEmpty = FloatStream.of(1f, 2f, 3f)
     *     .appendIfEmpty(99f, 100f)
     *     .toArray();   // returns [1.0, 2.0, 3.0] (original stream unchanged)
     *
     * float[] empty = FloatStream.empty()
     *     .appendIfEmpty(99f, 100f)
     *     .toArray();   // returns [99.0, 100.0]
     *
     * float[] filtered = FloatStream.of(1f, 2f, 3f)
     *     .filter(f -> f > 10)
     *     .appendIfEmpty(0f)
     *     .toArray();   // returns [0.0] (stream was empty after filtering)
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
    public abstract FloatStream appendIfEmpty(final float... a);

    /**
     * Returns a FloatStream consisting of the top n elements of this stream, according to the natural order of the elements.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p>This method only runs sequentially, even in a parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] top3 = FloatStream.of(5f, 2f, 8f, 1f, 9f, 3f)
     *     .top(3)
     *     .toArray();   // returns the 3 largest values: [9.0, 8.0, 5.0] (order not guaranteed)
     *
     * float[] top5 = FloatStream.of(1f, 2f, 3f)
     *     .top(5)
     *     .toArray();   // returns all 3 elements (fewer than n)
     *
     * double sum = FloatStream.of(10f, 20f, 5f, 15f, 30f)
     *     .top(2)
     *     .sum();   // returns 50.0 (sum of top 2: 30 + 20)
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
     * @param n the number of elements to return
     * @return a new FloatStream containing the top n elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract FloatStream top(int n);

    /**
     * Returns a FloatStream consisting of the top n elements of this stream compared by the provided Comparator.
     * If this stream contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <p>This method only runs sequentially, even in a parallel stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] top3Natural = FloatStream.of(5f, 2f, 8f, 1f, 9f, 3f)
     *     .top(3, Comparator.naturalOrder())
     *     .toArray();   // returns the 3 largest values: [9.0, 8.0, 5.0] (order not guaranteed)
     *
     * float[] top2Reversed = FloatStream.of(5f, 2f, 8f, 1f, 9f)
     *     .top(2, Comparator.reverseOrder())
     *     .toArray();   // returns the 2 smallest values (largest under reverse order): [1.0, 2.0]
     *
     * float[] custom = FloatStream.of(-5f, -2f, 8f, -1f, 9f)
     *     .top(3, Comparator.comparingDouble(Math::abs))
     *     .toArray();   // returns [9.0, 8.0, -5.0] (top 3 by absolute value, order not guaranteed)
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
     * @param n the number of elements to return
     * @param comparator a comparator to order the elements
     * @return a new FloatStream containing the top n elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @SequentialOnly
    @IntermediateOp
    public abstract FloatStream top(final int n, Comparator<? super Float> comparator);

    /**
     * Returns a FloatList containing all the elements of this stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatList list = FloatStream.of(1.1f, 2.2f, 3.3f, 4.4f)
     *     .filter(f -> f > 2.0f)
     *     .toFloatList();   // returns FloatList: [2.2, 3.3, 4.4]
     *
     * FloatList emptyList = FloatStream.empty().toFloatList();   // returns empty FloatList
     *
     * FloatList withNaN = FloatStream.of(1.0f, Float.NaN, 3.0f)
     *     .toFloatList();   // returns FloatList: [1.0, NaN, 3.0]
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
     * @return a FloatList containing all stream elements
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract FloatList toFloatList();

    /**
     * Returns a Map where keys are generated by the keyMapper function and values are generated by the valueMapper function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = FloatStream.of(1.5f, 2.7f, 3.9f)
     *     .toMap(f -> "key" + (int)f, f -> (int)(f * 10));
     *     // returns {"key1"=15, "key2"=27, "key3"=39}
     *
     * Map<Integer, String> formatted = FloatStream.of(10.5f, 20.3f, 30.8f)
     *     .toMap(f -> (int)f, f -> String.format("%.2f", f));
     *     // returns {10="10.50", 20="20.30", 30="30.80"}
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
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <E> the type of exception that the keyMapper may throw
     * @param <E2> the type of exception that the valueMapper may throw
     * @param keyMapper a function to produce keys for the map
     * @param valueMapper a function to produce values for the map
     * @return a Map containing the mapped key-value pairs
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the keyMapper throws an exception
     * @throws E2 if the valueMapper throws an exception
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.FloatFunction<? extends K, E> keyMapper,
            Throwables.FloatFunction<? extends V, E2> valueMapper) throws E, E2;

    /**
     * Returns a Map where keys are generated by the keyMapper function and values are generated by the valueMapper function, using the provided map factory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LinkedHashMap<Integer, String> orderedMap = FloatStream.of(3.5f, 1.2f, 2.8f)
     *     .toMap(f -> (int)f, f -> String.valueOf(f), LinkedHashMap::new);
     *     // returns LinkedHashMap preserving insertion order: {3="3.5", 1="1.2", 2="2.8"}
     *
     * TreeMap<Integer, Double> sortedMap = FloatStream.of(5.5f, 2.2f, 8.8f)
     *     .toMap(f -> (int)f, f -> (double)f, TreeMap::new);
     *     // returns TreeMap with sorted keys: {2=2.2, 5=5.5, 8=8.8}
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
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception that the keyMapper may throw
     * @param <E2> the type of exception that the valueMapper may throw
     * @param keyMapper a function to produce keys for the map
     * @param valueMapper a function to produce values for the map
     * @param mapFactory a supplier providing a new Map into which the results will be inserted
     * @return a Map containing the mapped key-value pairs
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the keyMapper throws an exception
     * @throws E2 if the valueMapper throws an exception
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.FloatFunction<? extends K, E> keyMapper,
            Throwables.FloatFunction<? extends V, E2> valueMapper, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Returns a Map where keys are generated by the keyMapper function and values are generated by the valueMapper function, with a merge function to handle duplicate keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum values for duplicate keys
     * Map<Integer, Integer> summed = FloatStream.of(1.5f, 1.7f, 2.3f, 2.8f, 1.2f)
     *     .toMap(f -> (int)f, f -> (int)(f * 10), Integer::sum);
     *     // returns {1=44, 2=51} (15+17+12=44, 23+28=51)
     *
     * // Keep maximum value for duplicate keys
     * Map<Integer, Float> maxValues = FloatStream.of(1.5f, 1.9f, 2.3f, 2.1f)
     *     .toMap(f -> (int)f, f -> f, Float::max);
     *     // returns {1=1.9f, 2=2.3f}
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
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <E> the type of exception that the keyMapper may throw
     * @param <E2> the type of exception that the valueMapper may throw
     * @param keyMapper a function to produce keys for the map
     * @param valueMapper a function to produce values for the map
     * @param mergeFunction a function to resolve collisions between values associated with the same key
     * @return a Map containing the mapped key-value pairs
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the keyMapper throws an exception
     * @throws E2 if the valueMapper throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.FloatFunction<? extends K, E> keyMapper,
            Throwables.FloatFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction) throws E, E2;

    /**
     * Returns a Map where keys are generated by the keyMapper function and values are generated by the valueMapper function, with a merge function to handle duplicate keys, using the provided map factory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum values for duplicate keys in a TreeMap
     * TreeMap<Integer, Integer> sortedSummed = FloatStream.of(3.5f, 1.7f, 2.3f, 3.8f, 1.2f)
     *     .toMap(f -> (int)f, f -> (int)(f * 10), Integer::sum, TreeMap::new);
     *     // returns TreeMap {1=29, 2=23, 3=73}
     *
     * // Keep first occurrence for duplicate keys in LinkedHashMap
     * LinkedHashMap<Integer, Float> firstValues = FloatStream.of(1.5f, 2.3f, 1.9f, 2.1f)
     *     .toMap(f -> (int)f, f -> f, (v1, v2) -> v1, LinkedHashMap::new);
     *     // returns LinkedHashMap {1=1.5f, 2=2.3f}
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
     * @param <K> the type of the map keys
     * @param <V> the type of the map values
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception that the keyMapper may throw
     * @param <E2> the type of exception that the valueMapper may throw
     * @param keyMapper a function to produce keys for the map
     * @param valueMapper a function to produce values for the map
     * @param mergeFunction a function to resolve collisions between values associated with the same key
     * @param mapFactory a supplier providing a new Map into which the results will be inserted
     * @return a Map containing the mapped key-value pairs
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the keyMapper throws an exception
     * @throws E2 if the valueMapper throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.FloatFunction<? extends K, E> keyMapper,
            Throwables.FloatFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Groups the elements of this stream by a classifier function and collects them using the specified downstream collector.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by integer part and count occurrences
     * Map<Integer, Long> counts = FloatStream.of(1.2f, 1.8f, 2.3f, 2.7f, 3.1f)
     *     .groupTo(f -> (int)f, Collectors.counting());
     *     // returns {1=2, 2=2, 3=1}
     *
     * // Group by range and collect to list
     * Map<String, List<Float>> rangeGroups = FloatStream.of(0.5f, 1.5f, 2.5f, 3.5f, 4.5f)
     *     .groupTo(f -> f < 2 ? "low" : f < 4 ? "mid" : "high",
     *              Collectors.mapping(Float::valueOf, Collectors.toList()));
     *     // returns {"low"=[0.5, 1.5], "mid"=[2.5, 3.5], "high"=[4.5]}
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
     * @param <E> the type of exception that the keyMapper may throw
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a Collector implementing the downstream reduction
     * @return a Map containing the results of the group-by operation
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the keyMapper throws an exception
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, E extends Exception> Map<K, D> groupTo(Throwables.FloatFunction<? extends K, E> keyMapper,
            final Collector<? super Float, ?, D> downstream) throws E;

    /**
     * Groups the elements of this stream by a classifier function and collects them using the specified downstream collector and map factory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group by integer part into sorted TreeMap with sum of values
     * TreeMap<Integer, Double> sortedSums = FloatStream.of(1.2f, 2.8f, 1.7f, 2.3f)
     *     .groupTo(f -> (int)f,
     *              Collectors.summingDouble(Float::doubleValue),
     *              TreeMap::new);
     *     // returns TreeMap {1=2.9, 2=5.1}
     *
     * // Group into LinkedHashMap preserving encounter order
     * LinkedHashMap<String, Set<Float>> orderedGroups = FloatStream.of(1.1f, 2.2f, 1.5f, 2.8f)
     *     .groupTo(f -> f < 2 ? "small" : "large",
     *              Collectors.mapping(Float::valueOf, Collectors.toSet()),
     *              LinkedHashMap::new);
     *     // returns LinkedHashMap {"small"=[1.1, 1.5], "large"=[2.2, 2.8]}
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
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception that the keyMapper may throw
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a Collector implementing the downstream reduction
     * @param mapFactory a supplier providing a new Map into which the results will be inserted
     * @return a Map containing the results of the group-by operation
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the keyMapper throws an exception
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, M extends Map<K, D>, E extends Exception> M groupTo(Throwables.FloatFunction<? extends K, E> keyMapper,
            final Collector<? super Float, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E;

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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float sum = FloatStream.of(1.5f, 2.5f, 3.5f)
     *     .reduce(0f, (a, b) -> a + b);   // returns 7.5f
     *
     * // Empty stream returns identity
     * FloatStream.empty().reduce(0f, (a, b) -> a + b);   // returns 0.0f
     *
     * float product = FloatStream.of(2f, 3f, 4f)
     *     .reduce(1f, (a, b) -> a * b);   // returns 24f
     *
     * float max = FloatStream.of(1.1f, 5.5f, 3.3f)
     *     .reduce(Float.NEGATIVE_INFINITY, Float::max);   // returns 5.5f
     *
     * // NaN propagation: any NaN operand produces NaN
     * FloatStream.of(1.0f, Float.NaN, 3.0f).reduce(0f, Float::max);   // returns NaN
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
    public abstract float reduce(float identity, FloatBinaryOperator accumulator);

    /**
     * Performs a reduction on the elements of this stream, using the provided accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current reduced value and the current stream element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalFloat sum = FloatStream.of(1.5f, 2.5f, 3.5f)
     *     .reduce((a, b) -> a + b);   // returns OptionalFloat.of(7.5f)
     *
     * OptionalFloat min = FloatStream.of(5.5f, 2.2f, 8.8f)
     *     .reduce(Float::min);   // returns OptionalFloat.of(2.2f)
     *
     * OptionalFloat empty = FloatStream.empty()
     *     .reduce((a, b) -> a + b);   // returns OptionalFloat.empty()
     *
     * // NaN propagation
     * FloatStream.of(1.0f, Float.NaN, 3.0f).reduce(Float::max);   // returns OptionalFloat[NaN]
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
     * @return an OptionalFloat describing the result of the reduction. If the stream is empty, an empty {@code OptionalFloat} is returned.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#reduce(BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract OptionalFloat reduce(FloatBinaryOperator accumulator);

    /**
     * Performs a mutable reduction operation on the elements of this stream using a supplier, accumulator, and combiner.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Float> list = FloatStream.of(1.1f, 2.2f, 3.3f)
     *     .collect(ArrayList::new, List::add, List::addAll);   // returns [1.1, 2.2, 3.3]
     *
     * FloatList floatList = FloatStream.of(1.5f, 2.5f, 3.5f)
     *     .collect(FloatList::new, FloatList::add, FloatList::addAll);   // returns FloatList [1.5, 2.5, 3.5]
     *
     * StringBuilder sb = FloatStream.of(1f, 2f, 3f)
     *     .collect(StringBuilder::new,
     *              (builder, f) -> builder.append(f).append(" "),
     *              StringBuilder::append);   // returns "1.0 2.0 3.0 "
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
    public abstract <R> R collect(Supplier<R> supplier, ObjFloatConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * Performs a mutable reduction operation on the elements of this stream using a supplier and accumulator.
     *
     * <br />
     * Only call this method when the returned type {@code R} is one of these types: {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * Otherwise, please call {@link #collect(Supplier, ObjFloatConsumer, BiConsumer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Float> list = FloatStream.of(1.1f, 2.2f, 3.3f)
     *     .collect(ArrayList::new, List::add);   // returns [1.1, 2.2, 3.3]
     *
     * FloatList floatList = FloatStream.of(1.5f, 2.5f, 3.5f)
     *     .collect(FloatList::new, FloatList::add);   // returns FloatList [1.5, 2.5, 3.5]
     *
     * Set<Float> set = FloatStream.of(1f, 2f, 1f, 3f)
     *     .collect(HashSet::new, Set::add);   // returns [1.0, 2.0, 3.0]
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
     * @see #collect(Supplier, ObjFloatConsumer, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjFloatConsumer<? super R> accumulator);

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
     * FloatStream.of(1.1f, 2.2f, 3.3f, 4.4f, 5.5f)
     *     .forEach(System.out::println);
     * // prints: 1.1, 2.2, 3.3, 4.4, 5.5 (each on a new line)
     *
     * FloatList collected = new FloatList();
     * FloatStream.of(1.0f, 2.0f, 3.0f).forEach(collected::add);
     * collected.toArray();                                // returns [1.0, 2.0, 3.0]
     *
     * FloatStream.empty().forEach(System.out::println);   // prints nothing
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
     * @param <E> the type of exception that the action may throw
     * @param action a non-interfering action to perform on the elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEach(final Throwables.FloatConsumer<E> action) throws E;

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
     * FloatStream.of(10.5f, 20.5f, 30.5f)
     *     .forEachIndexed((index, value) ->
     *         System.out.println("Element at index " + index + ": " + value));
     * // prints:
     * // Element at index 0: 10.5
     * // Element at index 1: 20.5
     * // Element at index 2: 30.5
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
     * @param <E> the type of exception that the action may throw
     * @param action a non-interfering action to perform on the elements, taking both index and value
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEachIndexed(Throwables.IntFloatConsumer<E> action) throws E;

    /**
     * Returns whether any elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code false} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean hasLarge = FloatStream.of(1.1f, 2.2f, 3.3f, 4.4f)
     *     .anyMatch(f -> f > 3.0f);   // returns true
     *
     * boolean hasNegative = FloatStream.of(1f, 2f, 3f)
     *     .anyMatch(f -> f < 0);   // returns false
     *
     * boolean emptyAny = FloatStream.empty()
     *     .anyMatch(f -> true);   // returns false
     *
     * boolean hasNaN = FloatStream.of(1.0f, Float.NaN, 3.0f)
     *     .anyMatch(Float::isNaN);   // returns true
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
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if any elements of the stream match the provided predicate,
     *         otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean anyMatch(final Throwables.FloatPredicate<E> predicate) throws E;

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code true} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean allPositive = FloatStream.of(1.1f, 2.2f, 3.3f)
     *     .allMatch(f -> f > 0);   // returns true
     *
     * boolean allLarge = FloatStream.of(1f, 2f, 3f)
     *     .allMatch(f -> f > 5f);   // returns false
     *
     * boolean emptyAll = FloatStream.empty()
     *     .allMatch(f -> false);   // returns true (vacuous truth)
     *
     * // NaN is not less than 0; NaN < 0 evaluates to false
     * boolean allNonNegative = FloatStream.of(1.0f, Float.NaN, 3.0f)
     *     .allMatch(f -> f >= 0);   // returns false (NaN >= 0 is false)
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
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if either all elements of the stream match the provided predicate or
     *         the stream is empty, otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean allMatch(final Throwables.FloatPredicate<E> predicate) throws E;

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code true} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean noNegatives = FloatStream.of(1.1f, 2.2f, 3.3f)
     *     .noneMatch(f -> f < 0);   // returns true
     *
     * boolean noLarge = FloatStream.of(1f, 5f, 3f)
     *     .noneMatch(f -> f > 4f);   // returns false (5f matches)
     *
     * boolean emptyNone = FloatStream.empty()
     *     .noneMatch(f -> true);   // returns true (vacuous truth)
     *
     * // NaN comparisons: Float.isNaN must be used; NaN == NaN is false
     * boolean noNaN = FloatStream.of(1.0f, 2.0f, 3.0f)
     *     .noneMatch(Float::isNaN);   // returns true
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
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if either no elements of the stream match the provided predicate or
     *         the stream is empty, otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean noneMatch(final Throwables.FloatPredicate<E> predicate) throws E;

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalFloat}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalFloat first = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
     *                                 .findFirst();   // returns OptionalFloat.of(1.0f)
     *
     * FloatStream.empty().findFirst();   // returns OptionalFloat.empty()
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
     * @return an {@code OptionalFloat} containing the first element of the stream, or an empty {@code OptionalFloat} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #first()
     * @see #findAny()
     * @see #findFirst(Throwables.FloatPredicate)
     * @see #findAny(Throwables.FloatPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalFloat findFirst() {
        assertNotClosed();

        return first();
    }

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalFloat}.
     * This is a terminal operation.
     *
     * <p>This method is a deterministic alias of {@link #first()} and {@link #findFirst()}; despite the
     * JDK-style name it returns the FIRST element, not an arbitrary one, even for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.
     * It always returns the first element regardless of whether the stream is parallel or sequential.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalFloat any = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
     *                              .findAny();   // returns OptionalFloat.of(1.0f)
     *
     * FloatStream.empty().findAny();   // returns OptionalFloat.empty()
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
     * @return an {@code OptionalFloat} containing the first element of the stream, or an empty {@code OptionalFloat} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #first()
     * @see #findFirst()
     * @see #findFirst(Throwables.FloatPredicate)
     * @see #findAny(Throwables.FloatPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalFloat findAny() {
        assertNotClosed();

        return first();
    }

    /**
     * Returns an OptionalFloat describing the first element of this stream that matches the given predicate, or an empty {@code OptionalFloat} if no such element exists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find first value greater than 3
     * OptionalFloat first = FloatStream.of(1.5f, 2.3f, 4.7f, 5.2f)
     *     .findFirst(f -> f > 3);   // returns OptionalFloat.of(4.7f)
     *
     * // Find first negative value
     * OptionalFloat negative = FloatStream.of(1.0f, 2.0f, -3.5f, -4.0f)
     *     .findFirst(f -> f < 0);   // returns OptionalFloat.of(-3.5f)
     *
     * // No matching element
     * OptionalFloat notFound = FloatStream.of(1.0f, 2.0f, 3.0f)
     *     .findFirst(f -> f > 10);   // returns OptionalFloat.empty()
     *
     * // Find first NaN value
     * OptionalFloat nanVal = FloatStream.of(1.0f, Float.NaN, 3.0f)
     *     .findFirst(Float::isNaN);   // returns OptionalFloat.of(NaN)
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
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an OptionalFloat describing the first matching element of this stream, or an empty {@code OptionalFloat} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalFloat findFirst(final Throwables.FloatPredicate<E> predicate) throws E;

    /**
     * Returns an OptionalFloat describing any element of this stream that matches the given predicate, or an empty {@code OptionalFloat} if no such element exists.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find any value greater than 3 (useful in parallel streams)
     * OptionalFloat any = FloatStream.of(1.5f, 2.3f, 4.7f, 5.2f)
     *     .findAny(f -> f > 3);   // returns OptionalFloat.of(4.7f) or OptionalFloat.of(5.2f)
     *
     * // Find any even integer value
     * OptionalFloat evenInt = FloatStream.of(1.5f, 3.0f, 4.0f, 5.5f)
     *     .findAny(f -> f == (int)f && (int)f % 2 == 0);   // returns OptionalFloat.of(4.0f)
     *
     * // No matching element
     * OptionalFloat notFound = FloatStream.of(1.0f, 2.0f, 3.0f)
     *     .findAny(f -> f > 10);   // returns OptionalFloat.empty()
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
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an OptionalFloat describing any matching element of this stream, or an empty {@code OptionalFloat} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalFloat findAny(final Throwables.FloatPredicate<E> predicate) throws E;

    /**
     * Returns an OptionalFloat describing the last element of this stream that matches the given predicate, or an empty {@code OptionalFloat} if no such element exists.
     * Consider using: {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find last value less than 5
     * OptionalFloat last = FloatStream.of(1.5f, 4.3f, 6.7f, 3.2f)
     *     .findLast(f -> f < 5);   // returns OptionalFloat.of(3.2f)
     *
     * // Find last even integer value
     * OptionalFloat lastEven = FloatStream.of(2.0f, 3.5f, 4.0f, 5.5f)
     *     .findLast(f -> f == (int)f && (int)f % 2 == 0);   // returns OptionalFloat.of(4.0f)
     *
     * // No matching element
     * OptionalFloat notFound = FloatStream.of(1.0f, 2.0f, 3.0f)
     *     .findLast(f -> f > 10);   // returns OptionalFloat.empty()
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
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an OptionalFloat describing the last matching element of this stream, or an empty {@code OptionalFloat} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalFloat findLast(final Throwables.FloatPredicate<E> predicate) throws E;

    /**
     * Returns an OptionalFloat describing the minimum element of this stream, or an empty {@code OptionalFloat} if the stream is empty.
     *
     * <p>If any element is {@code NaN}, the result is {@code NaN} wrapped in a present
     * {@code OptionalFloat}, following {@link Math#min(float, float)} semantics which propagate
     * {@code NaN}. An empty stream returns an empty {@code OptionalFloat}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream.of(5.5f, 2.3f, 8.1f, 1.7f, 9.2f).min();                        // returns OptionalFloat[1.7f]
     *
     * FloatStream.empty().min();                                                 // returns OptionalFloat.empty()
     *
     * // NaN propagates: any NaN makes the result NaN
     * FloatStream.of(1.0f, Float.NaN, 3.0f).min();                               // returns OptionalFloat[NaN]
     *
     * // -0.0f is less than +0.0f (per Float.compare semantics)
     * FloatStream.of(-0.0f, 0.0f).min().getAsFloat();                            // returns -0.0f
     *
     * // Safe retrieval with default value
     * float minValue = FloatStream.of(10.5f, 20.3f, 30.8f).min().orElse(0.0f);   // returns 10.5f
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
     * @return an {@code OptionalFloat} containing the minimum element, or an empty {@code OptionalFloat} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalFloat min();

    /**
     * Returns an OptionalFloat describing the maximum element of this stream, or an empty {@code OptionalFloat} if the stream is empty.
     *
     * <p>If any element is {@code NaN}, the result is {@code NaN} wrapped in a present
     * {@code OptionalFloat}, following {@link Math#max(float, float)} semantics which propagate
     * {@code NaN}. An empty stream returns an empty {@code OptionalFloat}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream.of(5.5f, 2.3f, 8.1f, 1.7f, 9.2f).max();                        // returns OptionalFloat[9.2f]
     *
     * FloatStream.empty().max();                                                 // returns OptionalFloat.empty()
     *
     * // NaN propagates: any NaN makes the result NaN
     * FloatStream.of(1.0f, Float.NaN, 3.0f).max();                               // returns OptionalFloat[NaN]
     *
     * // Positive infinity is less than NaN
     * FloatStream.of(1.0f, Float.POSITIVE_INFINITY).max().getAsFloat();          // returns Float.POSITIVE_INFINITY
     *
     * // Safe retrieval with default value
     * float maxValue = FloatStream.of(10.5f, 20.3f, 30.8f).max().orElse(0.0f);   // returns 30.8f
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
     * @return an {@code OptionalFloat} containing the maximum element, or an empty {@code OptionalFloat} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalFloat max();

    /**
     * Returns the <i>k-th</i> largest element in the stream, using natural ordering consistent
     * with {@link Float#compare(float, float)} (which considers {@code NaN} greater than any
     * other value, including positive infinity).
     * If the stream is empty or the count of elements is less than {@code k}, an empty {@code OptionalFloat} is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the 2nd largest element
     * FloatStream.of(10.5f, 30.2f, 20.8f, 50.1f, 40.7f).kthLargest(2);   // returns OptionalFloat[40.7f]
     *
     * // Find the largest element (same as max)
     * FloatStream.of(5.5f, 2.3f, 8.1f, 1.7f).kthLargest(1);   // returns OptionalFloat[8.1f]
     *
     * // When k exceeds stream size
     * FloatStream.of(1.5f, 2.3f, 3.8f).kthLargest(5);   // returns OptionalFloat.empty()
     *
     * // Empty stream
     * FloatStream.empty().kthLargest(1);   // returns OptionalFloat.empty()
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
     * @return an {@code OptionalFloat} containing the k-th largest element, or an empty {@code OptionalFloat} if the stream is empty or contains fewer than {@code k} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalFloat kthLargest(int k);

    /**
     * Returns the sum of all elements in this stream as a double. This is a terminal operation.
     *
     * <p>The sum is computed by promoting each {@code float} element to {@code double} and
     * accumulating with compensated (Kahan) summation, which provides better precision than summing
     * in float. If any element is {@code Float.NaN} (promoted to {@code Double.NaN}), the result is
     * {@code NaN}. Returns {@code 0.0} if the stream is empty.
     *
     * <p><b>Note on precision:</b> Floating-point arithmetic may produce rounding errors. The result is returned
     * as a double to provide better precision than float. For exact decimal calculations, consider converting
     * to {@link java.math.BigDecimal}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double total = FloatStream.of(1.5f, 2.5f, 3.5f, 4.5f).sum();   // returns 12.0
     *
     * // Sum of measurements
     * float[] measurements = {10.5f, 20.3f, 30.7f};
     * double sum = FloatStream.of(measurements).sum();   // returns 61.5
     *
     * // NaN propagation
     * FloatStream.of(1.0f, Float.NaN, 3.0f).sum();   // returns NaN
     *
     * // Empty stream
     * FloatStream.empty().sum();   // returns 0.0
     *
     * // Negative zero and positive zero both sum to 0.0
     * FloatStream.of(-0.0f, 0.0f).sum();   // returns 0.0
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
     * @return the sum of elements in this stream as a double, or {@code 0.0} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #average()
     * @see #reduce(float, FloatBinaryOperator)
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
     * <p>If any element is {@code Float.NaN} (promoted to {@code Double.NaN} during computation),
     * the result is {@code NaN} wrapped in a present {@code OptionalDouble}.
     * An empty stream returns an empty {@code OptionalDouble}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).average();                  // returns OptionalDouble[3.0]
     *
     * FloatStream.of(10.5f, 20.3f, 30.8f).average();                           // returns OptionalDouble[20.533333...]
     *
     * FloatStream.empty().average();                                           // returns OptionalDouble.empty()
     *
     * // NaN propagation
     * FloatStream.of(1.0f, Float.NaN, 3.0f).average();                         // returns OptionalDouble[NaN]
     *
     * // Safe retrieval with default value
     * double avg = FloatStream.of(98.5f, 87.3f).average().orElse(0.0);         // returns 92.9
     *
     * // Infinity is handled
     * FloatStream.of(1.0f, Float.POSITIVE_INFINITY).average().getAsDouble();   // returns Double.POSITIVE_INFINITY
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
     * Returns statistics about the elements of this stream.
     * The statistics include count, sum, min, max, and average.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatSummaryStatistics stats = FloatStream.of(1.5f, 2.3f, 3.7f, 4.2f, 5.8f).summaryStatistics();
     * System.out.println("Count: " + stats.getCount());       // count is 5
     * System.out.println("Sum: " + stats.getSum());           // sum is 17.5
     * System.out.println("Min: " + stats.getMin());           // min is 1.5
     * System.out.println("Max: " + stats.getMax());           // max is 5.8
     * System.out.println("Average: " + stats.getAverage());   // average is 3.5
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
     * @return a FloatSummaryStatistics containing various statistics about the elements
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract FloatSummaryStatistics summaryStatistics();

    /**
     * Returns a pair consisting of FloatSummaryStatistics for the elements of this stream,
     * and an Optional containing a map of percentiles if the stream is not empty.
     * To calculate the percentiles of the elements in the stream, all elements will be loaded into memory and sorted if not already sorted.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result =
     *     FloatStream.of(1.5f, 2.3f, 3.7f, 4.2f, 5.8f, 6.1f, 7.9f, 8.5f, 9.2f, 10.0f)
     *         .summaryStatisticsAndPercentiles();
     *
     * FloatSummaryStatistics stats = result.left();
     * System.out.println("Count: " + stats.getCount());       // count is 10
     * System.out.println("Average: " + stats.getAverage());   // average is 5.92
     *
     * Optional<Map<Percentage, Float>> percentiles = result.right();
     * if (percentiles.isPresent()) {
     *     Map<Percentage, Float> map = percentiles.get();
     *     System.out.println("Median (50th percentile): " + map.get(Percentage._50));
     *     System.out.println("90th percentile: " + map.get(Percentage._90));
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
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a Pair where the first element is FloatSummaryStatistics and the second is an Optional containing a map of percentiles
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> summaryStatisticsAndPercentiles();

    /**
     * Merges this stream with another FloatStream based on the provided selector function.
     * <br />
     * This method only runs sequentially, even in a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] merged = FloatStream.of(1f, 3f, 5f)
     *     .mergeWith(FloatStream.of(2f, 4f, 6f),
     *                (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0] (sorted merge)
     *
     * float[] alternate = FloatStream.of(10f, 20f, 30f)
     *     .mergeWith(FloatStream.of(1f, 2f),
     *                (a, b) -> MergeResult.TAKE_FIRST)
     *     .toArray();   // returns [10.0, 20.0, 30.0, 1.0, 2.0]
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
     * @param b the other FloatStream to merge with
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a new FloatStream containing the merged elements
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract FloatStream mergeWith(final FloatStream b, final FloatBiFunction<MergeResult> nextSelector);

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
     * FloatStream.of(1.0f, 2.0f, 3.0f)
     *            .zipWith(FloatStream.of(10.0f, 20.0f, 30.0f, 40.0f),
     *                     (a, b) -> a + b)
     *            .toArray();   // returns [11.0f, 22.0f, 33.0f]
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
     * @param b the FloatStream to be combined with the current FloatStream. Must be {@code non-null}.
     * @param zipFunction a FloatBinaryOperator that determines the combination of elements in the combined FloatStream. Must be {@code non-null}.
     * @return a new FloatStream that is the result of combining the current FloatStream with the given FloatStream
     * @throws IllegalStateException if the stream is already closed
     * @see #zipWith(FloatStream, float, float, FloatBinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream zipWith(FloatStream b, FloatBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the shortest of the three streams.
     * If any stream is longer, its remaining elements are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream.of(1.0f, 2.0f, 3.0f)
     *            .zipWith(FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f),
     *                     (a, b, c) -> a + b + c)
     *            .toArray();   // returns [111.0f, 222.0f]
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
     * @param b the second FloatStream to be combined with the current FloatStream. Will be closed along with this FloatStream.
     * @param c the third FloatStream to be combined with the current FloatStream. Will be closed along with this FloatStream.
     * @param zipFunction a FloatTernaryOperator that determines the combination of elements in the combined FloatStream. Must be {@code non-null}.
     * @return a new FloatStream that is the result of combining the current FloatStream with the given FloatStreams
     * @throws IllegalStateException if the stream is already closed
     * @see #zipWith(FloatStream, FloatStream, float, float, float, FloatTernaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream zipWith(FloatStream b, FloatStream c, FloatTernaryOperator zipFunction);

    /**
     * Zips this stream with the given stream using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from two streams pairwise.
     *
     * <p>The resulting stream will have the length of the longer of the two streams.
     * Default values are used when one stream runs out of elements before the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream.of(1.0f, 2.0f, 3.0f)
     *            .zipWith(FloatStream.of(10.0f), 0.0f, 0.0f, (a, b) -> a + b)
     *            .toArray();   // returns [11.0f, 2.0f, 3.0f]
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
     * @param b the FloatStream to be combined with the current FloatStream. Will be closed along with this FloatStream.
     * @param valueForNoneA the default value to use for the current FloatStream when it runs out of elements
     * @param valueForNoneB the default value to use for the given FloatStream when it runs out of elements
     * @param zipFunction a FloatBinaryOperator that determines the combination of elements in the combined FloatStream. Must be {@code non-null}.
     * @return a new FloatStream that is the result of combining the current FloatStream with the given FloatStream
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream zipWith(FloatStream b, float valueForNoneA, float valueForNoneB, FloatBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the longest of the three streams.
     * Default values are used when any stream runs out of elements before the others.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream.of(1.0f, 2.0f, 3.0f)
     *            .zipWith(FloatStream.of(10.0f), FloatStream.of(100.0f),
     *                     0.0f, 0.0f, 0.0f,
     *                     (a, b, c) -> a + b + c)
     *            .toArray();   // returns [111.0f, 2.0f, 3.0f]
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
     * @param b the second FloatStream to be combined with the current FloatStream. Will be closed along with this FloatStream.
     * @param c the third FloatStream to be combined with the current FloatStream. Will be closed along with this FloatStream.
     * @param valueForNoneA the default value to use for the current FloatStream when it runs out of elements
     * @param valueForNoneB the default value to use for the second FloatStream when it runs out of elements
     * @param valueForNoneC the default value to use for the third FloatStream when it runs out of elements
     * @param zipFunction a FloatTernaryOperator that determines the combination of elements in the combined FloatStream. Must be {@code non-null}.
     * @return a new FloatStream that is the result of combining the current FloatStream with the given FloatStreams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract FloatStream zipWith(FloatStream b, FloatStream c, float valueForNoneA, float valueForNoneB, float valueForNoneC,
            FloatTernaryOperator zipFunction);

    /**
     * Converts this FloatStream to a DoubleStream.
     * <br />
     * This method only runs sequentially, even in a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleStream doubleStream = FloatStream.of(1.1f, 2.2f, 3.3f)
     *     .asDoubleStream();   // returns DoubleStream of [1.1, 2.2, 3.3] (as doubles)
     *
     * double sum = FloatStream.of(1f, 2f, 3f)
     *     .asDoubleStream()
     *     .sum();   // returns 6.0 (double)
     *
     * double[] array = FloatStream.of(1.5f, 2.5f, 3.5f)
     *     .asDoubleStream()
     *     .toArray();   // returns double[] {1.5, 2.5, 3.5}
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
     * @return a DoubleStream containing the elements of this stream converted to doubles
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract DoubleStream asDoubleStream();

    /**
     * Returns a Stream consisting of the elements of this stream, each boxed to a Float.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream<Float> boxedStream = FloatStream.of(1.1f, 2.2f, 3.3f)
     *     .boxed();   // returns Stream<Float> of [1.1, 2.2, 3.3]
     *
     * List<Float> list = FloatStream.of(1f, 2f, 3f)
     *     .boxed()
     *     .collect(Collectors.toList());   // returns List<Float> [1.0, 2.0, 3.0]
     *
     * Stream<String> strings = FloatStream.of(1.5f, 2.5f, 3.5f)
     *     .boxed()
     *     .map(f -> "Value: " + f);   // returns Stream<String>
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
     * @return a Stream consisting of the elements of this stream, each boxed to a Float
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<Float> boxed();

    abstract FloatIteratorEx iteratorEx();

    // private static final FloatStream EMPTY_STREAM = new ArrayFloatStream(N.EMPTY_FLOAT_ARRAY, true, null);

    /**
     * Returns an empty FloatStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream emptyStream = FloatStream.empty();
     * long count = emptyStream.count();   // returns 0
     *
     * float sum = FloatStream.empty()
     *     .reduce(0f, (a, b) -> a + b);   // returns 0f
     *
     * FloatList list = FloatStream.empty().toFloatList();   // returns empty FloatList
     *
     * OptionalFloat min = FloatStream.empty().min();        // returns OptionalFloat.empty()
     * }</pre>
     *
     * @return an empty FloatStream
     */
    public static FloatStream empty() {
        return new ArrayFloatStream(N.EMPTY_FLOAT_ARRAY, true, null);
    }

    /**
     * Creates a new FloatStream that is supplied by the given supplier.
     * The supplier is memoized and invoked at most once, when the returned stream is first traversed or closed.
     * Closing the returned stream before traversal may still invoke the supplier so the supplied stream can be closed.
     *
     * <p><b>Implementation Note:</b> it's equivalent to {@code Stream.just(supplier).flatMapToFloat(it -> it.get())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Lazy evaluation - supplier is called when the stream is consumed or closed
     * FloatStream deferred = FloatStream.defer(() -> {
     *     System.out.println("Creating stream...");   // prints when stream is consumed or closed
     *     return FloatStream.of(1.0f, 2.0f, 3.0f);
     * });
     *
     * // Deferring expensive computation
     * FloatStream expensive = FloatStream.defer(() ->
     *     FloatStream.of(computeExpensiveFloatArray()));
     *
     * // Supplier is memoized; the underlying stream is only created once
     * double sum = deferred.sum();   // "Creating stream..." printed
     * }</pre>
     *
     * @param supplier the supplier that provides the FloatStream
     * @return a new FloatStream supplied by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#defer(Supplier)
     */
    public static FloatStream defer(final Supplier<FloatStream> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Supplier<FloatStream> s = Fn.memoize(supplier);
        return Stream.just(s).flatMapToFloat(Supplier::get).onClose(newCloseHandler(s));
    }

    /**
     * Returns a FloatStream containing a single element if the specified value is not {@code null}, otherwise returns an empty FloatStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Non-null value
     * FloatStream stream1 = FloatStream.ofNullable(3.14f);
     * stream1.count();   // returns 1
     *
     * // Null value
     * FloatStream stream2 = FloatStream.ofNullable(null);
     * stream2.count();   // returns 0
     *
     * // Useful for optional values
     * Float maybeValue = getMaybeFloat();
     * float result = FloatStream.ofNullable(maybeValue)
     *     .findFirst()
     *     .orElse(0.0f);
     * }</pre>
     *
     * @param e the Float value to create a stream from
     * @return a FloatStream containing the element if not {@code null}, otherwise an empty stream
     */
    public static FloatStream ofNullable(final Float e) {
        return e == null ? empty() : of(e);
    }

    /**
     * Returns a FloatStream containing the specified elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream stream = FloatStream.of(1.1f, 2.2f, 3.3f);
     * stream.forEach(System.out::println);   // prints: 1.1, 2.2, 3.3
     *
     * double sum = FloatStream.of(1f, 2f, 3f, 4f, 5f)
     *     .sum();   // returns 15.0 (as double)
     *
     * FloatStream single = FloatStream.of(42.5f);   // stream has a single element
     *
     * FloatStream fromArray = FloatStream.of(new float[]{1.5f, 2.5f, 3.5f});
     *
     * FloatStream empty = FloatStream.of(new float[]{});   // returns empty stream
     * }</pre>
     *
     * @param a the elements to be contained in the stream
     * @return a FloatStream containing the specified elements
     */
    public static FloatStream of(final float... a) {
        return N.isEmpty(a) ? empty() : new ArrayFloatStream(a);
    }

    /**
     * Returns a FloatStream containing elements from the specified array between the start (inclusive) and end (exclusive) indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] array = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
     * FloatStream stream = FloatStream.of(array, 1, 4);
     * stream.toArray();   // returns [2.0f, 3.0f, 4.0f]
     *
     * // Process a subrange
     * double sum = FloatStream.of(array, 0, 3).sum();   // returns 6.0 (1.0 + 2.0 + 3.0)
     * }</pre>
     *
     * @param a the array from which to create the stream
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a FloatStream containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the array length, or {@code fromIndex} is greater than {@code toIndex}
     */
    public static FloatStream of(final float[] a, final int fromIndex, final int toIndex) {
        return isEmptyRange(N.len(a), fromIndex, toIndex) ? empty() : new ArrayFloatStream(a, fromIndex, toIndex);
    }

    /**
     * Returns a FloatStream containing the unboxed elements from the specified Float array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Float[] boxedArray = {1.5f, 2.5f, 3.5f, null, 4.5f};
     * FloatStream stream = FloatStream.of(boxedArray);
     * // Note: null values are unboxed to 0f
     *
     * Float[] numbers = {10.5f, 20.3f, 30.8f};
     * double sum = FloatStream.of(numbers).sum();   // returns 61.6
     * }</pre>
     *
     * @param a the Float array to create a stream from
     * @return a FloatStream containing the unboxed elements ({@code null} elements are unboxed to {@code 0f})
     */
    public static FloatStream of(final Float[] a) {
        return Stream.of(a).mapToFloat(FF.unbox());
    }

    /**
     * Returns a FloatStream containing the unboxed elements from the specified Float array between the start (inclusive) and end (exclusive) indices.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Float[] boxedArray = {1.5f, 2.5f, 3.5f, 4.5f, 5.5f};
     * FloatStream stream = FloatStream.of(boxedArray, 1, 4);
     * stream.toArray();   // returns [2.5f, 3.5f, 4.5f]
     *
     * // Process a subrange of boxed Floats
     * double avg = FloatStream.of(boxedArray, 0, 3).average().orElse(0.0);   // returns 2.5
     * }</pre>
     *
     * @param a the Float array from which to create the stream
     * @param fromIndex the starting index (inclusive)
     * @param toIndex the ending index (exclusive)
     * @return a FloatStream containing the specified range of unboxed elements
     * @throws IndexOutOfBoundsException if the indices are out of range
     */
    public static FloatStream of(final Float[] a, final int fromIndex, final int toIndex) {
        return Stream.of(a, fromIndex, toIndex).mapToFloat(FF.unbox());
    }

    /**
     * Returns a FloatStream containing the unboxed elements from the specified collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Float> list = Arrays.asList(1.5f, 2.5f, 3.5f, 4.5f);
     * FloatStream stream = FloatStream.of(list);
     * double sum = stream.sum();   // returns 12.0
     *
     * Set<Float> set = new HashSet<>(Arrays.asList(5.5f, 6.5f, 7.5f));
     * float max = FloatStream.of(set).max().orElse(0f);   // returns 7.5f
     * }</pre>
     *
     * @param c the collection of Float values
     * @return a FloatStream containing the unboxed elements from the collection ({@code null} elements are unboxed to {@code 0f})
     */
    public static FloatStream of(final Collection<Float> c) {
        return Stream.of(c).mapToFloat(FF.unbox());
    }

    /**
     * Returns a FloatStream with elements from the specified FloatIterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iterator = FloatIterator.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
     * FloatStream stream = FloatStream.of(iterator);
     * double sum = stream.sum();   // returns 15.0
     *
     * // From a custom iterator
     * FloatIterator custom = new FloatIteratorEx() {
     *     private int count = 0;
     *     public boolean hasNext() { return count < 3; }
     *     public float nextFloat() { return (++count) * 1.5f; }
     * };
     * FloatStream.of(custom).toArray();   // returns [1.5f, 3.0f, 4.5f]
     * }</pre>
     *
     * @param iterator the FloatIterator to create a stream from
     * @return a FloatStream containing elements from the iterator, or an empty stream if iterator is null
     */
    public static FloatStream of(final FloatIterator iterator) {
        return iterator == null ? empty() : new IteratorFloatStream(iterator);
    }

    /**
     * Returns a FloatStream containing elements from the specified {@link FloatBuffer},
     * reading from the buffer's current {@link FloatBuffer#position() position} (inclusive)
     * to its {@link FloatBuffer#limit() limit} (exclusive). Returns an empty stream if
     * {@code buf} is {@code null}.
     *
     * <p>The buffer's position is <b>not</b> advanced by stream consumption — the stream
     * reads via absolute indexed {@code get(int)} access, so the buffer remains usable
     * afterwards.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatBuffer buffer = FloatBuffer.wrap(new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f});
     * FloatStream stream = FloatStream.of(buffer);
     * double sum = stream.sum();   // returns 15.0
     *
     * // With position and limit
     * FloatBuffer subBuffer = FloatBuffer.wrap(new float[]{1.0f, 2.0f, 3.0f, 4.0f, 5.0f});
     * subBuffer.position(1).limit(4);
     * FloatStream.of(subBuffer).toArray();   // returns [2.0f, 3.0f, 4.0f]
     * }</pre>
     *
     * @param buf the FloatBuffer to create a stream from (may be {@code null})
     * @return a FloatStream over elements in {@code buf} from {@code position} (inclusive)
     *         to {@code limit} (exclusive), or an empty stream if {@code buf} is {@code null}
     */
    public static FloatStream of(final FloatBuffer buf) {
        if (buf == null) {
            return empty();
        }

        //noinspection resource
        return IntStream.range(buf.position(), buf.limit()).mapToFloat(buf::get);
    }

    private static final Function<float[], FloatStream> flatMapper = FloatStream::of;

    private static final Function<float[][], FloatStream> flattMapper = FloatStream::flatten;

    /**
     * Returns a FloatStream containing all elements from the two-dimensional array, flattened row by row.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[][] matrix = {{1f, 2f, 3f}, {4f, 5f, 6f}, {7f, 8f, 9f}};
     * float[] flattened = FloatStream.flatten(matrix)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     *
     * float[][] jaggedArray = {{1f, 2f}, {3f, 4f, 5f}, {6f}};
     * double sum = FloatStream.flatten(jaggedArray)
     *     .sum();   // returns 21.0
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @return a FloatStream containing all elements from the array
     */
    public static FloatStream flatten(final float[][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToFloat(flatMapper);
    }

    /**
     * Returns a FloatStream containing all elements from the two-dimensional array, flattened either row by row or column by column.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[][] matrix = {{1f, 2f, 3f}, {4f, 5f, 6f}, {7f, 8f, 9f}};
     * float[] horizontal = FloatStream.flatten(matrix, false)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0] (row by row)
     *
     * float[] vertical = FloatStream.flatten(matrix, true)
     *     .toArray();   // returns [1.0, 4.0, 7.0, 2.0, 5.0, 8.0, 3.0, 6.0, 9.0] (column by column)
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @param vertically if {@code true}, flattens column by column; if {@code false}, flattens row by row
     * @return a FloatStream containing all elements from the array
     */
    public static FloatStream flatten(final float[][] a, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (!vertically) {
            return Stream.of(a).flatMapToFloat(flatMapper);
        }

        long n = 0;

        for (final float[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final FloatIterator iter = new FloatIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public float nextFloat() {
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
     * Returns a FloatStream containing all elements from the two-dimensional array, flattened with alignment.
     * If rows have different lengths, the valueForAlignment is used to pad shorter rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Jagged array flattened horizontally with 0 padding
     * float[][] jagged = {{1f, 2f}, {3f, 4f, 5f}, {6f}};
     * float[] horizontal = FloatStream.flatten(jagged, 0f, false)
     *     .toArray();   // returns [1.0, 2.0, 0.0, 3.0, 4.0, 5.0, 6.0, 0.0, 0.0]
     *
     * // Jagged array flattened vertically with -1 padding
     * float[] vertical = FloatStream.flatten(jagged, -1f, true)
     *     .toArray();   // returns [1.0, 3.0, 6.0, 2.0, 4.0, -1.0, -1.0, 5.0, -1.0]
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @param valueForAlignment the element to append so each row/column has the same number of elements
     * @param vertically if {@code true}, flattens column by column; if {@code false}, flattens row by row
     * @return a FloatStream containing all elements from the array with alignment
     */
    public static FloatStream flatten(final float[][] a, final float valueForAlignment, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (final float[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = (long) rows * cols;
        FloatIterator iter = null;

        if (vertically) {
            iter = new FloatIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public float nextFloat() {
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
            iter = new FloatIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public float nextFloat() {
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
     * Returns a FloatStream containing all elements from the three-dimensional array, flattened.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[][][] cube = {
     *     {{1f, 2f}, {3f, 4f}},
     *     {{5f, 6f}, {7f, 8f}}
     * };
     * float[] flattened = FloatStream.flatten(cube)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Calculate sum of all elements in a 3D array
     * double sum = FloatStream.flatten(cube).sum();   // returns 36.0
     * }</pre>
     *
     * @param a the three-dimensional array to flatten
     * @return a FloatStream containing all elements from the array
     */
    public static FloatStream flatten(final float[][][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToFloat(flattMapper);
    }

    /**
     * Returns a FloatStream consisting of n repetitions of the specified element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] fives = FloatStream.repeat(5f, 3)
     *     .toArray();   // returns [5.0, 5.0, 5.0]
     *
     * double sum = FloatStream.repeat(2.5f, 4)
     *     .sum();   // returns 10.0
     *
     * FloatList zeros = FloatStream.repeat(0f, 10)
     *     .toFloatList();   // returns list of 10 zeros
     * }</pre>
     *
     * @param element the element to repeat
     * @param n the number of times to repeat the element
     * @return a FloatStream containing n repetitions of the element
     * @throws IllegalArgumentException if n is negative
     */
    public static FloatStream repeat(final float element, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return empty();
        } else if (n < 10) {
            return of(Array.repeat(element, (int) n));
        }

        return new IteratorFloatStream(new FloatIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public float nextFloat() {
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
            public float[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final float[] result = new float[(int) cnt];

                Arrays.fill(result, element);

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns an effectively unlimited stream of pseudorandom float values, each between 0.0 (inclusive) and 1.0 (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 5 random floats
     * float[] randomValues = FloatStream.random()
     *     .limit(5)
     *     .toArray();   // returns 5 random floats between 0.0 and 1.0
     *
     * // Generate random floats in a range [min, max)
     * float min = 10.0f, max = 20.0f;
     * float[] rangedRandom = FloatStream.random()
     *     .limit(10)
     *     .map(f -> min + f * (max - min))
     *     .toArray();   // returns 10 random floats between 10.0 and 20.0
     *
     * // Get a single random float
     * float randomFloat = FloatStream.random()
     *     .findFirst()
     *     .orElse(0.0f);   // returns a single random float
     * }</pre>
     *
     * @return a stream of pseudorandom float values
     */
    public static FloatStream random() {
        return generate(RAND::nextFloat);
    }

    /**
     * Creates a stream that iterates using the given <i>hasNext</i> and <i>next</i> suppliers.
     * After the {@code hasNext} supplier first returns {@code false}, the stream remains exhausted
     * and that supplier is not invoked again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * float[] values = FloatStream.iterate(() -> counter.get() < 5,
     *                                       () -> counter.incrementAndGet() * 1.5f)
     *     .toArray();   // returns [1.5, 3.0, 4.5, 6.0, 7.5]
     *
     * AtomicReference<Float> current = new AtomicReference<>(1f);
     * float[] doubling = FloatStream.iterate(() -> current.get() < 100,
     *                                         () -> {
     *                                             float val = current.get();
     *                                             current.set(val * 2);
     *                                             return val;
     *                                         })
     *     .toArray();   // returns [1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0]
     * }</pre>
     *
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param next a FloatSupplier that provides the next float in the iteration
     * @return a FloatStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>next</i> is null
     * @see Stream#iterate(BooleanSupplier, Supplier)
     */
    public static FloatStream iterate(final BooleanSupplier hasNext, final FloatSupplier next) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorFloatStream(new FloatIteratorEx() {
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
            public float nextFloat() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return next.getAsFloat();
            }
        });
    }

    /**
     * Creates a stream that iterates from an initial value, applying a function to generate subsequent values,
     * and continues as long as the {@code hasNext} supplier returns {@code true}.
     * After that supplier first returns {@code false}, the stream remains exhausted and the supplier
     * is not invoked again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] powers = FloatStream.iterate(1f, () -> true, x -> x * 2)
     *     .limit(5)
     *     .toArray();   // returns [1.0, 2.0, 4.0, 8.0, 16.0]
     *
     * AtomicInteger count = new AtomicInteger(0);
     * float[] limited = FloatStream.iterate(0f, () -> count.incrementAndGet() <= 4, x -> x + 0.5f)
     *     .toArray();   // returns [0.0, 0.5, 1.0, 1.5]
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param f a function to apply to the previous element to generate the next element
     * @return a FloatStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>f</i> is null
     * @see Stream#iterate(Object, BooleanSupplier, java.util.function.UnaryOperator)
     */
    public static FloatStream iterate(final float init, final BooleanSupplier hasNext, final FloatUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorFloatStream(new FloatIteratorEx() {
            private float cur = 0;
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
            public float nextFloat() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsFloat(cur);
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
     * float[] lessThan10 = FloatStream.iterate(1f, x -> x < 10, x -> x + 1)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     *
     * float[] geometric = FloatStream.iterate(1f, x -> x < 100, x -> x * 2)
     *     .toArray();   // returns [1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0]
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a predicate that determines if the returned stream has a next element; tested against {@code init} for the first element and {@code f.apply(previous)} for subsequent elements.
     * @param f a function to apply to the previous element to generate the next element
     * @return a FloatStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>f</i> is null
     * @see Stream#iterate(Object, java.util.function.Predicate, java.util.function.UnaryOperator)
     */
    public static FloatStream iterate(final float init, final FloatPredicate hasNext, final FloatUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorFloatStream(new FloatIteratorEx() {
            private float cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsFloat(cur));
                    }

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public float nextFloat() {
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
     * This creates an infinite stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate powers of 2
     * float[] powers = FloatStream.iterate(1f, x -> x * 2)
     *     .limit(5)
     *     .toArray();   // returns [1.0, 2.0, 4.0, 8.0, 16.0]
     *
     * // Generate arithmetic sequence
     * float[] sequence = FloatStream.iterate(0f, x -> x + 0.5f)
     *     .limit(6)
     *     .toArray();   // returns [0.0, 0.5, 1.0, 1.5, 2.0, 2.5]
     *
     * // Calculate fibonacci-like sequence
     * AtomicReference<Float> prev = new AtomicReference<>(0f);
     * float[] fib = FloatStream.iterate(1f, current -> {
     *     float next = current + prev.get();
     *     prev.set(current);
     *     return next;
     * }).limit(7).toArray();   // returns [1.0, 1.0, 2.0, 3.0, 5.0, 8.0, 13.0]
     * }</pre>
     *
     * @param init the initial value
     * @param f a function to apply to the previous element to generate the next element
     * @return an infinite FloatStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>f</i> is null
     * @see Stream#iterate(Object, java.util.function.UnaryOperator)
     */
    public static FloatStream iterate(final float init, final FloatUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(f);

        return new IteratorFloatStream(new FloatIteratorEx() {
            private float cur = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public float nextFloat() {
                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsFloat(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Generates a FloatStream using the provided FloatSupplier.
     * The supplier is used to generate each element of the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] randomValues = FloatStream.generate(() -> (float) Math.random())
     *     .limit(5)
     *     .toArray();   // returns 5 random float values
     *
     * float[] constants = FloatStream.generate(() -> 3.14f)
     *     .limit(3)
     *     .toArray();   // returns [3.14, 3.14, 3.14]
     *
     * AtomicInteger counter = new AtomicInteger(0);
     * float[] sequence = FloatStream.generate(() -> counter.incrementAndGet() * 0.5f)
     *     .limit(4)
     *     .toArray();   // returns [0.5, 1.0, 1.5, 2.0]
     * }</pre>
     *
     * @param s the FloatSupplier that provides the elements of the stream
     * @return a FloatStream generated by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#generate(Supplier)
     */
    public static FloatStream generate(final FloatSupplier s) throws IllegalArgumentException {
        N.checkArgNotNull(s);

        return new IteratorFloatStream(new FloatIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public float nextFloat() {
                return s.getAsFloat();
            }
        });
    }

    /**
     * Concatenates multiple arrays of floats into a single FloatStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] array1 = {1.0f, 2.0f, 3.0f};
     * float[] array2 = {4.0f, 5.0f};
     * float[] array3 = {6.0f, 7.0f, 8.0f};
     * float[] result = FloatStream.concat(array1, array2, array3)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Concatenate and process
     * double sum = FloatStream.concat(array1, array2)
     *     .sum();   // returns 15.0
     * }</pre>
     *
     * @param a the arrays of floats to concatenate
     * @return a FloatStream containing all the floats from the input arrays
     * @see Stream#concat(Object[][])
     */
    public static FloatStream concat(final float[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Arrays.asList(a));
    }

    /**
     * Concatenates multiple FloatIterators into a single FloatStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter1 = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * FloatIterator iter2 = FloatIterator.of(4.0f, 5.0f);
     * FloatIterator iter3 = FloatIterator.of(6.0f, 7.0f, 8.0f);
     * float[] result = FloatStream.concat(iter1, iter2, iter3)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Concatenate and process
     * double sum = FloatStream.concat(iter1, iter2).sum();
     * }</pre>
     *
     * @param a the FloatIterators to concatenate
     * @return a FloatStream containing all the floats from the input FloatIterators in order
     * @see Stream#concat(Iterator[])
     */
    public static FloatStream concat(final FloatIterator... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concatIterators(Array.asList(a));
    }

    /**
     * Concatenates multiple FloatStreams into a single FloatStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream stream1 = FloatStream.of(1.0f, 2.0f, 3.0f);
     * FloatStream stream2 = FloatStream.of(4.0f, 5.0f);
     * FloatStream stream3 = FloatStream.of(6.0f, 7.0f, 8.0f);
     * float[] result = FloatStream.concat(stream1, stream2, stream3)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Concatenate filtered streams
     * FloatStream positive = FloatStream.of(-1.0f, 2.0f, -3.0f).filter(f -> f > 0);
     * FloatStream negatives = FloatStream.of(1.0f, -4.0f, 5.0f).filter(f -> f < 0);
     * FloatStream.concat(positive, negatives).toArray();   // returns [2.0, -4.0]
     * }</pre>
     *
     * @param a the FloatStreams to concatenate
     * @return a FloatStream containing all the floats from the input FloatStreams in order
     * @see Stream#concat(Stream[])
     */
    public static FloatStream concat(final FloatStream... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a list of float arrays into a single FloatStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<float[]> arrays = Arrays.asList(
     *     new float[]{1.0f, 2.0f, 3.0f},
     *     new float[]{4.0f, 5.0f},
     *     new float[]{6.0f, 7.0f, 8.0f}
     * );
     * float[] result = FloatStream.concat(arrays)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Useful for building streams from multiple sources
     * List<float[]> measurements = collectMeasurements();
     * double average = FloatStream.concat(measurements).average().orElse(0.0);
     * }</pre>
     *
     * @param c the list of float arrays to concatenate
     * @return a FloatStream containing all the floats from the input arrays in order
     * @see Stream#concat(Object[][])
     */
    @Beta
    public static FloatStream concat(final List<float[]> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return of(new FloatIteratorEx() {
            private final Iterator<float[]> iter = c.iterator();
            private float[] cur;
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
            public float nextFloat() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        });
    }

    /**
     * Concatenates a collection of FloatStream into a single FloatStream.
     * The collection's membership and encounter order are snapshotted when this method is called.
     * Closing the returned stream closes every snapshotted input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<FloatStream> streams = Arrays.asList(
     *     FloatStream.of(1.0f, 2.0f, 3.0f),
     *     FloatStream.of(4.0f, 5.0f),
     *     FloatStream.of(6.0f, 7.0f, 8.0f)
     * );
     * float[] result = FloatStream.concat(streams)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Concatenate dynamically generated streams
     * Set<FloatStream> streamSet = generateStreams();
     * double sum = FloatStream.concat(streamSet).sum();
     * }</pre>
     *
     * @param streams the collection of FloatStream to concatenate; {@code null} elements are treated as empty streams
     * @return a FloatStream containing all the floats from the input collection of FloatStream
     * @see Stream#concat(Collection)
     */
    public static FloatStream concat(final Collection<? extends FloatStream> streams) {
        if (N.isEmpty(streams)) {
            return empty();
        }

        final List<? extends FloatStream> sources = new ArrayList<>(streams);

        return new IteratorFloatStream(new FloatIteratorEx() { //NOSONAR
            private final Iterator<? extends FloatStream> iterators = sources.iterator();
            private FloatStream cur;
            private FloatIterator iter;

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
            public float nextFloat() {
                if ((iter == null || !iter.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextFloat();
            }
        }).onClose(newCloseHandler(sources));
    }

    /**
     * Concatenates a collection of FloatIterator into a single FloatStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iter1 = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * FloatIterator iter2 = FloatIterator.of(4.0f, 5.0f);
     * FloatIterator iter3 = FloatIterator.of(6.0f, 7.0f, 8.0f);
     *
     * float[] result = FloatStream.concatIterators(Arrays.asList(iter1, iter2, iter3))
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0]
     *
     * // Concatenate and process
     * List<FloatIterator> iterators = Arrays.asList(iter1, iter2);
     * double sum = FloatStream.concatIterators(iterators).sum();
     * }</pre>
     *
     * @param floatIterators the collection of FloatIterator to concatenate
     * @return a FloatStream containing all the floats from the input collection of FloatIterator
     * @see Stream#concatIterators(Collection)
     */
    @Beta
    public static FloatStream concatIterators(final Collection<? extends FloatIterator> floatIterators) {
        if (N.isEmpty(floatIterators)) {
            return empty();
        }

        return new IteratorFloatStream(new FloatIteratorEx() {
            private final Iterator<? extends FloatIterator> iter = floatIterators.iterator();
            private FloatIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public float nextFloat() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextFloat();
            }
        });
    }

    /**
     * Zips two float arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from both arrays into a single stream using the provided
     * zip function. The stream ends when the shorter array runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] a = {1.0f, 2.0f, 3.0f};
     * float[] b = {4.0f, 5.0f, 6.0f};
     * FloatStream.zip(a, b, (x, y) -> x + y)
     *         .toFloatList();   // returns [5.0f, 7.0f, 9.0f]
     * }</pre>
     *
     * @param a the first float array
     * @param b the second float array
     * @param zipFunction the function to combine elements from both arrays. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], BiFunction)
     */
    public static FloatStream zip(final float[] a, final float[] b, final FloatBinaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b)) {
            return empty();
        }

        return new IteratorFloatStream(new FloatIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public float nextFloat() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsFloat(a[cursor], b[cursor++]);
            }
        });
    }

    /**
     * Zips three float arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from all three arrays into a single stream using the provided
     * zip function. The stream ends when the shortest array runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] a = {1.0f, 2.0f, 3.0f};
     * float[] b = {4.0f, 5.0f, 6.0f};
     * float[] c = {7.0f, 8.0f, 9.0f};
     * FloatStream.zip(a, b, c, (x, y, z) -> x + y + z)
     *         .toFloatList();   // returns [12.0f, 15.0f, 18.0f]
     * }</pre>
     *
     * @param a the first float array
     * @param b the second float array
     * @param c the third float array
     * @param zipFunction the function to combine elements from all three arrays. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object[], TriFunction)
     */
    public static FloatStream zip(final float[] a, final float[] b, final float[] c, final FloatTernaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b) || N.isEmpty(c)) {
            return empty();
        }

        return new IteratorFloatStream(new FloatIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public float nextFloat() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsFloat(a[cursor], b[cursor], c[cursor++]);
            }
        });
    }

    /**
     * Zips two float iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from both iterators into a single stream using the provided
     * zip function. The stream ends when the shorter iterator runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator a = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * FloatIterator b = FloatIterator.of(4.0f, 5.0f, 6.0f);
     * FloatStream.zip(a, b, (x, y) -> x + y)
     *         .toFloatList();   // returns [5.0f, 7.0f, 9.0f]
     * }</pre>
     *
     * @param a the first float iterator
     * @param b the second float iterator
     * @param zipFunction the function to combine elements from both iterators. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, BiFunction)
     */
    public static FloatStream zip(final FloatIterator a, final FloatIterator b, final FloatBinaryOperator zipFunction) {
        return new IteratorFloatStream(new FloatIteratorEx() {
            private final FloatIterator iterA = a == null ? FloatIterator.empty() : a;
            private final FloatIterator iterB = b == null ? FloatIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public float nextFloat() {
                return zipFunction.applyAsFloat(iterA.nextFloat(), iterB.nextFloat());
            }
        });
    }

    /**
     * Zips three float iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from all three iterators into a single stream using the provided
     * zip function. The stream ends when the shortest iterator runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator a = FloatIterator.of(1.0f, 2.0f, 3.0f);
     * FloatIterator b = FloatIterator.of(4.0f, 5.0f, 6.0f);
     * FloatIterator c = FloatIterator.of(7.0f, 8.0f, 9.0f);
     * FloatStream.zip(a, b, c, (x, y, z) -> x + y + z)
     *         .toFloatList();   // returns [12.0f, 15.0f, 18.0f]
     * }</pre>
     *
     * @param a the first float iterator
     * @param b the second float iterator
     * @param c the third float iterator
     * @param zipFunction the function to combine elements from all three iterators. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Iterator, TriFunction)
     */
    public static FloatStream zip(final FloatIterator a, final FloatIterator b, final FloatIterator c, final FloatTernaryOperator zipFunction) {
        return new IteratorFloatStream(new FloatIteratorEx() {
            private final FloatIterator iterA = a == null ? FloatIterator.empty() : a;
            private final FloatIterator iterB = b == null ? FloatIterator.empty() : b;
            private final FloatIterator iterC = c == null ? FloatIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public float nextFloat() {
                return zipFunction.applyAsFloat(iterA.nextFloat(), iterB.nextFloat(), iterC.nextFloat());
            }
        });
    }

    /**
     * Zips two float streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from both streams into a single stream using the provided
     * zip function. The stream ends when the shorter stream runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream a = FloatStream.of(1.0f, 2.0f, 3.0f);
     * FloatStream b = FloatStream.of(4.0f, 5.0f, 6.0f);
     * FloatStream.zip(a, b, (x, y) -> x + y)
     *         .toFloatList();   // returns [5.0f, 7.0f, 9.0f]
     * }</pre>
     *
     * @param a the first float stream
     * @param b the second float stream
     * @param zipFunction the function to combine elements from both streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, BiFunction)
     */
    public static FloatStream zip(final FloatStream a, final FloatStream b, final FloatBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three float streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from all three streams into a single stream using the provided
     * zip function. The stream ends when the shortest stream runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream a = FloatStream.of(1.0f, 2.0f, 3.0f);
     * FloatStream b = FloatStream.of(4.0f, 5.0f, 6.0f);
     * FloatStream c = FloatStream.of(7.0f, 8.0f, 9.0f);
     * FloatStream.zip(a, b, c, (x, y, z) -> x + y + z)
     *         .toFloatList();   // returns [12.0f, 15.0f, 18.0f]
     * }</pre>
     *
     * @param a the first float stream
     * @param b the second float stream
     * @param c the third float stream
     * @param zipFunction the function to combine elements from all three streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Stream, TriFunction)
     */
    public static FloatStream zip(final FloatStream a, final FloatStream b, final FloatStream c, final FloatTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), zipFunction).onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple float streams into a single stream until any of them run out of values.
     * This is a static factory method that combines elements from all streams into a single stream using the provided
     * zip function. The stream ends when any of the input streams runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream a = FloatStream.of(1.0f, 2.0f);
     * FloatStream b = FloatStream.of(3.0f, 4.0f);
     * FloatStream c = FloatStream.of(5.0f, 6.0f);
     * FloatStream.zip(Arrays.asList(a, b, c), floats -> floats[0] + floats[1] + floats[2])
     *         .toFloatList();   // returns [9.0f, 12.0f]
     * }</pre>
     *
     * @param streams the collection of float streams to zip; its contents are snapshotted, and {@code null} streams are treated as empty
     * @param zipFunction the function to combine elements from all the streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Collection, Function)
     */
    public static FloatStream zip(final Collection<? extends FloatStream> streams, final FloatNFunction<Float> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, zipFunction).mapToFloat(ToFloatFunction.UNBOX);
    }

    /**
     * Zips two float arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from both arrays into a single stream using the provided
     * zip function. The stream ends when both arrays run out of values. If one array runs out of values before the other,
     * the specified default value is used for the shorter array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] a = {1.0f, 2.0f};
     * float[] b = {4.0f, 5.0f, 6.0f};
     * FloatStream.zip(a, b, 0.0f, 0.0f, (x, y) -> x + y)
     *         .toFloatList();   // returns [5.0f, 7.0f, 6.0f]
     * }</pre>
     *
     * @param a the first float array
     * @param b the second float array
     * @param valueForNoneA the default value to use if the first array is shorter
     * @param valueForNoneB the default value to use if the second array is shorter
     * @param zipFunction the function to combine elements from both arrays. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object, Object, BiFunction)
     */
    public static FloatStream zip(final float[] a, final float[] b, final float valueForNoneA, final float valueForNoneB,
            final FloatBinaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b)) {
            return empty();
        }

        return new IteratorFloatStream(new FloatIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private float ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public float nextFloat() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsFloat(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips three float arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from all three arrays into a single stream using the provided
     * zip function. The stream ends when all arrays run out of values. If one array runs out of values before the others,
     * the specified default value is used for the shorter array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] a = {1.0f, 2.0f};
     * float[] b = {3.0f, 4.0f, 5.0f};
     * float[] c = {6.0f, 7.0f};
     * FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
     *         .toFloatList();   // returns [10.0f, 13.0f, 5.0f]
     * }</pre>
     *
     * @param a the first float array
     * @param b the second float array
     * @param c the third float array
     * @param valueForNoneA the default value to use if the first array is shorter
     * @param valueForNoneB the default value to use if the second array is shorter
     * @param valueForNoneC the default value to use if the third array is shorter
     * @param zipFunction the function to combine elements from all three arrays. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object[], Object, Object, Object, TriFunction)
     */
    public static FloatStream zip(final float[] a, final float[] b, final float[] c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTernaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b) && N.isEmpty(c)) {
            return empty();
        }

        return new IteratorFloatStream(new FloatIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private float ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public float nextFloat() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsFloat(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
                        cursor < cLen ? c[cursor] : valueForNoneC);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips two float iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from both iterators into a single stream using the provided
     * zip function. The stream ends when both iterators run out of values. If one iterator runs out of values before the other,
     * the specified default value is used for the shorter iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator a = FloatIterator.of(1.0f, 2.0f);
     * FloatIterator b = FloatIterator.of(4.0f, 5.0f, 6.0f);
     * FloatStream.zip(a, b, 0.0f, 0.0f, (x, y) -> x + y)
     *         .toFloatList();   // returns [5.0f, 7.0f, 6.0f]
     * }</pre>
     *
     * @param a the first float iterator
     * @param b the second float iterator
     * @param valueForNoneA the default value to use if the first iterator is shorter
     * @param valueForNoneB the default value to use if the second iterator is shorter
     * @param zipFunction the function to combine elements from both iterators. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Object, Object, BiFunction)
     */
    public static FloatStream zip(final FloatIterator a, final FloatIterator b, final float valueForNoneA, final float valueForNoneB,
            final FloatBinaryOperator zipFunction) {
        return new IteratorFloatStream(new FloatIteratorEx() {
            private final FloatIterator iterA = a == null ? FloatIterator.empty() : a;
            private final FloatIterator iterB = b == null ? FloatIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public float nextFloat() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsFloat(iterA.nextFloat(), iterB.hasNext() ? iterB.nextFloat() : valueForNoneB);
                } else {
                    return zipFunction.applyAsFloat(valueForNoneA, iterB.nextFloat());
                }
            }
        });
    }

    /**
     * Zips three float iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from all three iterators into a single stream using the provided
     * zip function. The stream ends when all iterators run out of values. If one iterator runs out of values before the others,
     * the specified default value is used for the shorter iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator a = FloatIterator.of(1.0f, 2.0f);
     * FloatIterator b = FloatIterator.of(3.0f, 4.0f, 5.0f);
     * FloatIterator c = FloatIterator.of(6.0f, 7.0f);
     * FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
     *         .toFloatList();   // returns [10.0f, 13.0f, 5.0f]
     * }</pre>
     *
     * @param a the first float iterator
     * @param b the second float iterator
     * @param c the third float iterator
     * @param valueForNoneA the default value to use if the first iterator is shorter
     * @param valueForNoneB the default value to use if the second iterator is shorter
     * @param valueForNoneC the default value to use if the third iterator is shorter
     * @param zipFunction the function to combine elements from all three iterators. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Iterator, Object, Object, Object, TriFunction)
     */
    public static FloatStream zip(final FloatIterator a, final FloatIterator b, final FloatIterator c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTernaryOperator zipFunction) {
        return new IteratorFloatStream(new FloatIteratorEx() {
            private final FloatIterator iterA = a == null ? FloatIterator.empty() : a;
            private final FloatIterator iterB = b == null ? FloatIterator.empty() : b;
            private final FloatIterator iterC = c == null ? FloatIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public float nextFloat() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsFloat(iterA.nextFloat(), iterB.hasNext() ? iterB.nextFloat() : valueForNoneB,
                            iterC.hasNext() ? iterC.nextFloat() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.applyAsFloat(valueForNoneA, iterB.nextFloat(), iterC.hasNext() ? iterC.nextFloat() : valueForNoneC);
                } else {
                    return zipFunction.applyAsFloat(valueForNoneA, valueForNoneB, iterC.nextFloat());
                }
            }
        });
    }

    /**
     * Zips two float streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from both streams into a single stream using the provided
     * zip function. The stream ends when both streams run out of values. If one stream runs out of values before the other,
     * the specified default value is used for the shorter stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream a = FloatStream.of(1.0f, 2.0f);
     * FloatStream b = FloatStream.of(4.0f, 5.0f, 6.0f);
     * FloatStream.zip(a, b, 0.0f, 0.0f, (x, y) -> x + y)
     *         .toFloatList();   // returns [5.0f, 7.0f, 6.0f]
     * }</pre>
     *
     * @param a the first float stream
     * @param b the second float stream
     * @param valueForNoneA the default value to use if the first stream is shorter
     * @param valueForNoneB the default value to use if the second stream is shorter
     * @param zipFunction the function to combine elements from both streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Object, Object, BiFunction)
     */
    public static FloatStream zip(final FloatStream a, final FloatStream b, final float valueForNoneA, final float valueForNoneB,
            final FloatBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three float streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from all three streams into a single stream using the provided
     * zip function. The stream ends when all streams run out of values. If one stream runs out of values before the others,
     * the specified default value is used for the shorter stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream a = FloatStream.of(1.0f, 2.0f);
     * FloatStream b = FloatStream.of(3.0f, 4.0f, 5.0f);
     * FloatStream c = FloatStream.of(6.0f, 7.0f);
     * FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z)
     *         .toFloatList();   // returns [10.0f, 13.0f, 5.0f]
     * }</pre>
     *
     * @param a the first float stream
     * @param b the second float stream
     * @param c the third float stream
     * @param valueForNoneA the default value to use if the first stream is shorter
     * @param valueForNoneB the default value to use if the second stream is shorter
     * @param valueForNoneC the default value to use if the third stream is shorter
     * @param zipFunction the function to combine elements from all three streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Stream, Object, Object, Object, TriFunction)
     */
    public static FloatStream zip(final FloatStream a, final FloatStream b, final FloatStream c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple float streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from all streams into a single stream using the provided
     * zip function. The stream ends when all input streams run out of values. If one stream runs out of values before the others,
     * the specified default value is used for the shorter stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatStream a = FloatStream.of(1.0f, 2.0f);
     * FloatStream b = FloatStream.of(3.0f, 4.0f, 5.0f);
     * FloatStream c = FloatStream.of(6.0f, 7.0f);
     * float[] defaults = {0.0f, 0.0f, 0.0f};
     * FloatStream.zip(Arrays.asList(a, b, c), defaults, floats -> floats[0] + floats[1] + floats[2])
     *         .toFloatList();   // returns [10.0f, 13.0f, 5.0f]
     * }</pre>
     *
     * @param streams the collection of float streams to zip; its contents are snapshotted, and {@code null} streams are treated as empty
     * @param valuesForNone the default values to use if the corresponding stream is shorter
     * @param zipFunction the function to combine elements from all the streams. Must be non-null
     * @return a stream of combined values
     * @see Stream#zip(Collection, List, Function)
     */
    public static FloatStream zip(final Collection<? extends FloatStream> streams, final float[] valuesForNone, final FloatNFunction<Float> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, valuesForNone, zipFunction).mapToFloat(ToFloatFunction.UNBOX);
    }

    /**
     * Merges two float arrays into a single FloatStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted arrays in ascending order
     * float[] a = {1.0f, 3.0f, 5.0f};
     * float[] b = {2.0f, 4.0f, 6.0f};
     * float[] merged = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Always take from first array first
     * float[] firstPriority = FloatStream.merge(a, b, (x, y) -> MergeResult.TAKE_FIRST)
     *     .toArray();   // returns [1.0, 3.0, 5.0, 2.0, 4.0, 6.0]
     * }</pre>
     *
     * @param a the first float array
     * @param b the second float array
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a FloatStream containing the merged elements from the two input arrays
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Object[], Object[], BiFunction)
     */
    public static FloatStream merge(final float[] a, final float[] b, final FloatBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return new IteratorFloatStream(new FloatIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public float nextFloat() {
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
     * Merges three float arrays into a single FloatStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the three arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted arrays in ascending order
     * float[] a = {1.0f, 4.0f, 7.0f};
     * float[] b = {2.0f, 5.0f, 8.0f};
     * float[] c = {3.0f, 6.0f, 9.0f};
     * float[] merged = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     * }</pre>
     *
     * @param a the first float array
     * @param b the second float array
     * @param c the third float array
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a FloatStream containing the merged elements from the three input arrays
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Object[], Object[], Object[], BiFunction)
     */
    public static FloatStream merge(final float[] a, final float[] b, final float[] c, final FloatBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), FloatStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * Merges two FloatIterators into a single FloatStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two iterators in ascending order
     * FloatIterator a = FloatIterator.of(1.0f, 3.0f, 5.0f);
     * FloatIterator b = FloatIterator.of(2.0f, 4.0f, 6.0f);
     * float[] merged = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Custom merge logic
     * FloatIterator iter1 = FloatIterator.of(10.0f, 20.0f, 30.0f);
     * FloatIterator iter2 = FloatIterator.of(15.0f, 25.0f, 35.0f);
     * float[] custom = FloatStream.merge(iter1, iter2, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [10.0, 15.0, 20.0, 25.0, 30.0, 35.0]
     * }</pre>
     *
     * @param a the first FloatIterator
     * @param b the second FloatIterator
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a FloatStream containing the merged elements from the two input iterators
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Iterator, Iterator, BiFunction)
     */
    public static FloatStream merge(final FloatIterator a, final FloatIterator b, final FloatBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return new IteratorFloatStream(new FloatIteratorEx() {
            private final FloatIterator iterA = a == null ? FloatIterator.empty() : a;
            private final FloatIterator iterB = b == null ? FloatIterator.empty() : b;
            private float nextA = 0;
            private float nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public float nextFloat() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.nextFloat())) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextFloat()), nextB) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextFloat()), (nextB = iterB.nextFloat())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.nextFloat();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.nextFloat();
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     * Merges three FloatIterators into a single FloatStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the three iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three iterators in ascending order
     * FloatIterator a = FloatIterator.of(1.0f, 4.0f, 7.0f);
     * FloatIterator b = FloatIterator.of(2.0f, 5.0f, 8.0f);
     * FloatIterator c = FloatIterator.of(3.0f, 6.0f, 9.0f);
     * float[] merged = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     * }</pre>
     *
     * @param a the first FloatIterator
     * @param b the second FloatIterator
     * @param c the third FloatIterator
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a FloatStream containing the merged elements from the three input iterators
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Iterator, Iterator, Iterator, BiFunction)
     */
    public static FloatStream merge(final FloatIterator a, final FloatIterator b, final FloatIterator c, final FloatBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * Merges two FloatStreams into a single FloatStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted streams in ascending order
     * FloatStream a = FloatStream.of(1.0f, 3.0f, 5.0f);
     * FloatStream b = FloatStream.of(2.0f, 4.0f, 6.0f);
     * float[] merged = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0]
     *
     * // Merge with custom logic (take smaller absolute value)
     * FloatStream s1 = FloatStream.of(-5.0f, -3.0f, -1.0f);
     * FloatStream s2 = FloatStream.of(-4.0f, -2.0f, 0.0f);
     * float[] byAbs = FloatStream.merge(s1, s2, (x, y) -> Math.abs(x) <= Math.abs(y) ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [-4.0, -2.0, 0.0, -5.0, -3.0, -1.0]
     * }</pre>
     *
     * @param a the first FloatStream
     * @param b the second FloatStream
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a FloatStream containing the merged elements from the two input streams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Stream, Stream, BiFunction)
     */
    public static FloatStream merge(final FloatStream a, final FloatStream b, final FloatBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return merge(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(a, b));
    }

    /**
     * Merges three FloatStreams into a single FloatStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the three streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted streams in ascending order
     * FloatStream a = FloatStream.of(1.0f, 4.0f, 7.0f);
     * FloatStream b = FloatStream.of(2.0f, 5.0f, 8.0f);
     * FloatStream c = FloatStream.of(3.0f, 6.0f, 9.0f);
     * float[] merged = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     * }</pre>
     *
     * @param a the first FloatStream
     * @param b the second FloatStream
     * @param c the third FloatStream
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a FloatStream containing the merged elements from the three input streams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Stream, Stream, Stream, BiFunction)
     */
    public static FloatStream merge(final FloatStream a, final FloatStream b, final FloatStream c, final FloatBiFunction<MergeResult> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * Merges a collection of FloatStream into a single FloatStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the multiple streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge multiple sorted streams in ascending order
     * List<FloatStream> streams = Arrays.asList(
     *     FloatStream.of(1.0f, 4.0f, 7.0f),
     *     FloatStream.of(2.0f, 5.0f, 8.0f),
     *     FloatStream.of(3.0f, 6.0f, 9.0f)
     * );
     * float[] merged = FloatStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // returns [1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]
     *
     * // Merge dynamic set of streams
     * Set<FloatStream> streamSet = generateFloatStreams();
     * FloatStream combined = FloatStream.merge(streamSet, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * }</pre>
     *
     * @param streams the collection of FloatStream instances to merge; a {@code null} collection and {@code null} elements are treated as empty
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a FloatStream containing the merged elements from the input FloatStreams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Collection, BiFunction)
     */
    public static FloatStream merge(final Collection<? extends FloatStream> streams, final FloatBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (N.isEmpty(streams)) {
            return empty();
        } else if (streams.size() == 1) {
            final FloatStream stream = streams.iterator().next();
            return stream == null ? empty() : stream;
        } else if (streams.size() == 2) {
            final Iterator<? extends FloatStream> iter = streams.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends FloatStream> iter = streams.iterator();
        FloatStream result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * Extended abstract class for FloatStream implementations.
     */
    public abstract static class FloatStreamEx extends FloatStream {
        /**
         * Constructor for FloatStreamEx.
         *
         * @param sorted whether the stream is sorted
         * @param closeHandlers collection of close handlers
         */
        private FloatStreamEx(final boolean sorted, final Collection<LocalRunnable> closeHandlers) { //NOSONAR
            super(sorted, closeHandlers);
        }
    }
}
