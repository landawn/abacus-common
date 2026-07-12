/*
 * Copyright (C) 2024 HaiYang Li
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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.annotation.TerminalOpTriggered;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Difference;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.Immutable;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Iterables;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * The base interface for all stream types in the abacus-core library, providing a foundation for functional-style
 * operations on sequences of elements. This interface defines the core streaming operations and serves as
 * the parent interface for specialized stream types.
 *
 * <p>BaseStream represents a sequence of elements supporting sequential and parallel aggregate operations.
 * Stream operations are divided into <em>intermediate</em> operations (which return a new stream) and
 * <em>terminal</em> operations (which produce a result or side-effect). Intermediate operations are always
 * lazy; executing an intermediate operation such as {@code filter()} does not actually perform any filtering,
 * but instead creates a new stream that, when traversed, contains the elements of the initial stream that
 * match the given predicate.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Lazy Evaluation:</b> Operations are not executed until a terminal operation is invoked</li>
 *   <li><b>Auto-closeable:</b> Streams are automatically closed after terminal operations complete</li>
 *   <li><b>Parallel Support:</b> Supports both sequential and parallel execution modes</li>
 *   <li><b>Resource Management:</b> Proper resource cleanup through close handlers</li>
 *   <li><b>Type Safety:</b> Strongly typed with generic parameters for different stream aspects</li>
 * </ul>
 *
 * <p><b>Stream Lifecycle:</b>
 * <ol>
 *   <li><b>Creation:</b> Stream is created from a data source (collection, array, generator, etc.)</li>
 *   <li><b>Intermediate Operations:</b> Zero or more transformations are applied (filter, map, etc.)</li>
 *   <li><b>Terminal Operation:</b> A single terminal operation produces a result and closes the stream</li>
 *   <li><b>Closed:</b> Stream can no longer be used; attempting to operate on it throws IllegalStateException</li>
 * </ol>
 *
 * <p><b>Parallel Processing:</b>
 * Streams can be switched between sequential and parallel modes using {@link #sequential()} and
 * {@link #parallel()} methods. Parallel operations may improve performance for CPU-intensive tasks
 * on large datasets, but sequential processing is often more efficient for small datasets or simple operations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic stream pipeline
 * List<String> result = Stream.of("apple", "banana", "cherry")
 *     .filter(s -> s.length() > 5)   // filters as an intermediate operation
 *     .map(String::toUpperCase)      // maps as an intermediate operation
 *     .toList();                     // collects as a terminal operation
 * // Result: ["BANANA", "CHERRY"]
 *
 * // Parallel processing for expensive operations
 * List<ProcessedData> results = Stream.of(largeDataset)
 *     .parallel(8)                      // switches to parallel mode
 *     .map(this::expensiveProcessing)   // uses CPU-intensive operation
 *     .filter(data -> data.isValid())   // filters results
 *     .toList();                        // collects results
 *
 * // Resource management with try-with-resources
 * try (Stream<String> lines = Stream.ofLines(path)) {
 *     lines.filter(line -> !line.isEmpty())
 *          .map(String::trim)
 *          .forEach(System.out::println);
 * } // closes the stream automatically
 * }</pre>
 *
 * <p><b>Implementation Notes:</b>
 * <ul>
 *   <li>Streams are not data structures; they don't store elements but process them on-demand</li>
 *   <li>Streams should not be reused after a terminal operation has been performed</li>
 *   <li>Most stream operations accept functional interfaces as parameters, enabling lambda expressions</li>
 *   <li>Operations should be non-interfering and stateless for predictable behavior</li>
 * </ul>
 *
 * <p><b>How selected operations differ from the JDK ({@code java.util.stream}):</b> beyond the larger operation
 * set, a few <i>same-named</i> operations behave differently from the JDK; each point where the {@code JDK}
 * differs is flagged with {@code &#9888;&#65039;}:</p>
 * <table border="1">
 *   <caption>How selected {@code BaseStream} operations differ from JDK</caption>
 *   <thead>
 *     <tr><th>Operation</th><th>Behavior difference</th></tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>{@code first}/{@code last}/{@code elementAt}/{@code onlyOne} (and the typed streams' {@code min}/{@code max}/{@code reduce}/{@code findFirst})</td>
 *       <td><b><i>abacus</i></b>: return abacus's own {@code u.Optional}/{@code u.OptionalInt}/{@code u.OptionalLong}/{@code u.OptionalDouble} &middot; &#9888;&#65039; <b><i>JDK</i></b>: return {@code java.util.Optional}/{@code OptionalInt}/{@code OptionalLong}/{@code OptionalDouble}</td>
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
 *       <td><b><i>abacus</i></b>: returns an extended iterator ({@code ObjIterator}/{@code IntIterator}/&hellip;, and is deprecated) that does not auto-close the stream &middot; &#9888;&#65039; <b><i>JDK</i></b>: returns a plain {@code java.util.Iterator}</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <br />
 * <br />
 *
 * @param <T> the type of elements in this stream
 * @param <A> the type of array that {@link #toArray()} returns for this stream type
 * @param <P> the type of predicate used in filtering operations (e.g., {@code Predicate<T>} for object streams)
 * @param <C> the type of consumer used in consumption operations (e.g., {@code Consumer<T>} for object streams)
 * @param <OT> the type of Optional returned by operations like {@link #first()}, {@link #last()}
 * @param <IT> the type of indexed element returned by {@link #indexed()} operation
 * @param <ITER> the type of iterator returned by {@link #iterator()} method
 * @param <S> the self-type of the stream implementing this interface (enables method chaining with correct return types)
 *
 * @see Stream
 * @see EntryStream
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see com.landawn.abacus.util.Seq
 * @see Collectors
 * @see com.landawn.abacus.util.Fn
 * @see com.landawn.abacus.util.Comparators
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@com.landawn.abacus.annotation.Immutable
@LazyEvaluation
public interface BaseStream<T, A, P, C, OT, IT, ITER extends Iterator<T>, S extends BaseStream<T, A, P, C, OT, IT, ITER, S>> extends AutoCloseable, Immutable {
    // extends java.util.stream.BaseStream<T, BaseStream<T, A, P, C, PL, OT, IT, ITER, S>>, Immutable {

    /**
     * Returns a new stream consisting of the elements of this stream that match the given predicate.
     * This is an intermediate operation that filters elements based on the specified predicate condition.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter integers greater than 5
     * Stream.of(1, 7, 3, 9, 4, 6)
     *       .filter(n -> n > 5)
     *       .toList();   // returns [7, 9, 6]
     *
     * // Filter non-empty strings
     * Stream.of("apple", "", "banana", null, "cherry")
     *       .filter(Strings::isNotEmpty)
     *       .toList();   // returns ["apple", "banana", "cherry"]
     * }</pre>
     *
     * <p>In parallel streams, filtering operations may be performed concurrently.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine if it should be included
     * @return a new stream consisting of the elements that match the given predicate
     * @see #filter(Object, Object)
     * @see #takeWhile(Object)
     * @see #dropWhile(Object)
     */
    @ParallelSupported
    @IntermediateOp
    S filter(P predicate);

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate.
     * If an element doesn't match the predicate, the provided action {@code onDrop} is applied to that element.
     * This is an intermediate operation.
     *
     * <p>In parallel streams, the action may be performed concurrently for multiple dropped elements.
     * The implementation should ensure the action is thread-safe when used with parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter integers greater than 5 and log discarded values
     * Stream.of(1, 7, 3, 9, 4, 6)
     *       .filter(n -> n > 5, n -> System.out.println("Dropped: " + n))
     *       .toList();   // returns [7, 9, 6], and prints "Dropped: 1", "Dropped: 3", "Dropped: 4"
     *
     * // Count processed items while filtering
     * AtomicInteger dropped = new AtomicInteger();
     * Stream.of("apple", "", "banana", null, "cherry")
     *       .filter(Strings::isNotEmpty, s -> dropped.incrementAndGet())
     *       .toList();   // returns ["apple", "banana", "cherry"], with dropped.get() == 2
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine if it should be included
     * @param onDrop the action to perform on elements that don't match the predicate;
     *        this action is applied to each element that fails the predicate test and is
     *        therefore excluded from the resulting stream
     * @return a new stream consisting of the elements that match the given predicate
     * @see #filter(Object)
     * @see #takeWhile(Object)
     * @see #dropWhile(Object)
     * @see #dropWhile(Object, Object)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    S filter(P predicate, C onDrop);

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
     * // Taking numbers less than 5
     * Stream.of(1, 2, 3, 4, 5, 6)
     *       .takeWhile(n -> n < 5)
     *       .toList();   // returns [1, 2, 3, 4]
     *
     * // With non-matching element in the middle
     * Stream.of(1, 2, 5, 6, 3, 4)
     *       .takeWhile(n -> n < 5)
     *       .toList();   // returns [1, 2] - stops at first non-matching element
     *
     * // With first element not matching
     * Stream.of(5, 6, 1, 2, 3, 4)
     *       .takeWhile(n -> n < 5)
     *       .toList();   // returns [] - stops immediately
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to stop taking elements
     * @return a new stream consisting of elements from this stream until an element
     *         is encountered that doesn't match the predicate
     * @see #dropWhile(Object)
     * @see #filter(Object)
     * @see #skipUntil(Object)
     */
    @ParallelSupported
    @IntermediateOp
    S takeWhile(P predicate);

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
     * // Dropping numbers less than 5
     * Stream.of(1, 2, 3, 4, 5, 6)
     *       .dropWhile(n -> n < 5)
     *       .toList();   // returns [5, 6]
     *
     * // With matching elements in the middle
     * Stream.of(1, 2, 5, 6, 3, 4)
     *       .dropWhile(n -> n < 5)
     *       .toList();   // returns [5, 6, 3, 4] - starts at first non-matching element
     *
     * // With first element not matching predicate
     * Stream.of(5, 6, 1, 2, 3, 4)
     *       .dropWhile(n -> n < 5)
     *       .toList();   // returns [5, 6, 1, 2, 3, 4] - predicate is false for first element
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to stop dropping elements
     * @return a new stream consisting of the remaining elements of this stream after dropping elements
     *         while the given predicate returns {@code true}
     * @see #dropWhile(Object, Object)
     * @see #takeWhile(Object)
     * @see #filter(Object)
     * @see #skipUntil(Object)
     */
    @ParallelSupported
    @IntermediateOp
    S dropWhile(P predicate);

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
     * <p>In parallel streams, the action may be performed concurrently for multiple dropped elements.
     * The implementation should ensure the action is thread-safe when used with parallel streams.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Dropping numbers less than 5 and counting them
     * AtomicInteger count = new AtomicInteger();
     * Stream.of(1, 2, 3, 4, 5, 6)
     *       .dropWhile(n -> n < 5, n -> count.incrementAndGet())
     *       .toList();   // returns [5, 6], and count.get() == 4
     *
     * // With matching elements in the middle and logging dropped elements
     * Stream.of(1, 2, 5, 6, 3, 4)
     *       .dropWhile(n -> n < 5, n -> System.out.println("Dropped: " + n))
     *       .toList();   // returns [5, 6, 3, 4], and prints "Dropped: 1", "Dropped: 2"
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to stop dropping elements
     * @param onDrop the action to perform on elements that are dropped;
     *        this action is applied to each element that is discarded from the start
     *        of the stream while the predicate returns {@code true}
     * @return a new stream consisting of the remaining elements of this stream after dropping elements
     *         while the given predicate returns {@code true}
     * @see #dropWhile(Object)
     * @see #filter(Object, Object)
     * @see #takeWhile(Object)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    S dropWhile(P predicate, C onDrop);

    /**
     * Returns a stream consisting of the remaining elements of this stream after
     * discarding elements until the given predicate evaluates to {@code true}. Once an
     * element satisfies the predicate, that element and all subsequent elements are
     * included without further predicate checks. This is an intermediate operation.
     *
     * <p><b>Notes on parallel streams:</b><br>
     * ⚠️ In a parallel stream, elements after the first matched element (the first element for which
     * the predicate returns {@code true}) may still be processed and skipped if they do not satisfy
     * the predicate.<br>
     * For sequential or ordered streams the operation is deterministic — elements are skipped until the
     * first element for which the predicate returns {@code true}, and the rest are passed through
     * unchanged; in unordered parallel streams the result may appear unintuitive.
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
     * // Skip numbers until finding one greater than or equal to 5
     * Stream.of(1, 2, 3, 4, 5, 6)
     *       .skipUntil(n -> n >= 5)
     *       .toList();   // returns [5, 6]
     *
     * // With matching element in the middle
     * Stream.of(1, 2, 5, 6, 3, 4)
     *       .skipUntil(n -> n >= 5)
     *       .toList();   // returns [5, 6, 3, 4] - starts at first matching element
     *
     * // With no matching elements
     * Stream.of(1, 2, 3, 4)
     *       .skipUntil(n -> n > 10)
     *       .toList();   // returns [] - no elements match the predicate
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param predicate a non-interfering, stateless predicate that tests each element to determine when to start including elements
     * @return a new stream consisting of the elements starting from the first element
     *         that matches the given predicate
     * @see #dropWhile(Object)
     * @see #takeWhile(Object)
     * @see #filter(Object)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    S skipUntil(P predicate);

    /**
     * Returns a stream consisting of the distinct elements of this stream.
     * This is an intermediate operation that removes duplicate elements based on
     * their {@code equals} method implementation.
     *
     * <p>Elements are compared for equality using their {@code equals} method. The first
     * occurrence of an element is retained while subsequent duplicates are discarded.
     * This operation may require storing elements encountered so far for determining duplicates,
     * making it a stateful operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Remove duplicates from a stream of integers
     * Stream.of(1, 2, 3, 2, 1, 4, 5, 4)
     *       .distinct()
     *       .toList();   // returns [1, 2, 3, 4, 5]
     *
     * // With objects, using equals() for comparison
     * Stream.of("apple", "banana", "apple", "cherry", "banana")
     *       .distinct()
     *       .toList();   // returns ["apple", "banana", "cherry"]
     *
     * // With nulls
     * Stream.of("apple", null, "banana", null)
     *       .distinct()
     *       .toList();   // returns ["apple", null, "banana"]
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. The internal state required to track duplicates
     * makes this operation less suitable for parallel processing.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream consisting of the distinct elements of this stream
     * @see #filter(Object)
     * @see #sorted()
     */
    @SequentialOnly
    @IntermediateOp
    S distinct();

    /**
     * Returns a stream consisting of the elements that are present in both this stream and the specified collection.
     * For elements that appear multiple times, the intersection contains the lesser number of occurrences.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream<Integer> stream = Stream.of(1, 2, 2, 3, 4);
     * Collection<Integer> collection = Arrays.asList(2, 2, 2, 3, 5);
     *
     * Stream<Integer> result = stream.intersection(collection);
     * // result will contain: 2, 2, 3
     * // Two occurrences of 2 (minimum count in both sources) and one occurrence of 3
     * }</pre>
     *
     * <p>The order of elements in the resulting stream is determined by their order in the original stream.
     * This method only runs sequentially, even in a parallel stream.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param c the collection to find common elements with this stream
     * @return a new stream containing elements present in both this stream and the specified collection,
     *         considering the minimum number of occurrences in either source
     * @see #difference(Collection)
     * @see #symmetricDifference(Collection)
     * @see N#intersection(Collection, Collection)
     * @see N#intersection(int[], int[])
     * @see Collection#retainAll(Collection)
     */
    @SequentialOnly
    @IntermediateOp
    S intersection(Collection<?> c);

    /**
     * Returns a stream consisting of the elements of this stream that are not present in the specified collection,
     * considering the number of occurrences of each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream<String> stream = Stream.of("A", "B", "B", "C", "D");
     * Collection<String> collection = Arrays.asList("B", "E", "A");
     * Stream<String> result = stream.difference(collection);   // result is ["B", "C", "D"]
     * // One 'B' remains because stream has one more occurrence than collection
     *
     * Stream<Integer> stream2 = Stream.of(1, 2, 2, 3);
     * Collection<Integer> collection2 = Arrays.asList(2, 2, 2);
     * Stream<Integer> result2 = stream2.difference(collection2);   // result is [1, 3]
     * // No '2' appears in the result because collection2 has more occurrences than stream2
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param c the collection to compare against this stream
     * @return a new stream containing the elements that are present in this stream but not in the specified collection,
     *         considering the number of occurrences.
     * @see #intersection(Collection)
     * @see #symmetricDifference(Collection)
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    @SequentialOnly
    @IntermediateOp
    S difference(Collection<?> c);

    /**
     * Returns a stream consisting of elements that are present in either this stream or the specified collection,
     * but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the symmetric difference contains occurrences that remain
     * after removing the minimum number of shared occurrences from both sources.
     *
     * <p>The order of elements from the original stream is preserved first, followed by
     * the remaining elements from the collection.
     * This method only runs sequentially, even in a parallel stream.
     *
     * <p><b>Note:</b> unlike {@link #intersection(Collection)} and {@link #difference(Collection)}, which accept a
     * {@code Collection<?>} (they only retain/remove this stream's own elements), this method accepts a
     * {@code Collection<? extends T>}: the collection's unmatched elements are emitted <i>into</i> the resulting
     * stream, so they must be assignable to {@code T}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream<Integer> stream = Stream.of(1, 2, 2, 3, 4);
     * Collection<Integer> collection = Arrays.asList(2, 3, 3, 5);
     *
     * Stream<Integer> result = stream.symmetricDifference(collection);
     * // result will contain: 1, 2, 4, 3, 5
     * // Elements explanation:
     * // - 1, 4: only in stream
     * // - 5: only in collection
     * // - 2: one occurrence in common, but stream has two instances, so one remains
     * // - 3: one occurrence in common, but collection has two instances, so one remains
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param c the collection to compare with this stream for symmetric difference
     * @return a new stream containing elements that are present in either this stream or the collection,
     *         but not in both, considering the number of occurrences
     * @see #intersection(Collection)
     * @see #difference(Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifference(int[], int[])
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see Iterables#symmetricDifference(Set, Set)
     */
    @SequentialOnly
    @IntermediateOp
    S symmetricDifference(Collection<? extends T> c);

    /**
     * Returns a stream consisting of the elements of this stream in reverse order.
     * This is an intermediate operation that loads all elements into memory when called.
     *
     * <p>The returned stream contains all the elements of the original stream but in the opposite order.
     * For example, if the original stream contains [1, 2, 3], the reversed stream will contain [3, 2, 1].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Reverse a stream of integers
     * Stream.of(1, 2, 3, 4, 5)
     *       .reversed()
     *       .toList();   // returns [5, 4, 3, 2, 1]
     *
     * // Reverse a stream of strings
     * Stream.of("apple", "banana", "cherry")
     *       .reversed()
     *       .toList();   // returns ["cherry", "banana", "apple"]
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Additionally, it triggers a terminal operation
     * internally to collect all elements before reversing them.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>Yes</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream consisting of the elements of this stream in reverse order
     * @see #sorted()
     * @see #shuffled()
     * @see #rotated(int)
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    S reversed();

    /**
     * Returns a stream consisting of the elements of this stream rotated by the specified distance.
     * This is an intermediate operation that loads all elements into memory when called.
     *
     * <p>The returned stream contains the same elements as the original stream but with their positions
     * shifted by the specified distance. A positive distance rotates elements to the right (ending elements move to the
     * beginning), while a negative distance rotates elements to the left (beginning elements
     * move to the end).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Rotate a stream of integers to the right by 2 positions
     * Stream.of(1, 2, 3, 4, 5)
     *       .rotated(2)
     *       .toList();   // returns [4, 5, 1, 2, 3]
     *
     * // Rotate a stream of integers to the left by 2 positions (negative distance)
     * Stream.of(1, 2, 3, 4, 5)
     *       .rotated(-2)
     *       .toList();   // returns [3, 4, 5, 1, 2]
     *
     * // Rotate by a distance larger than the stream size
     * Stream.of(1, 2, 3, 4, 5)
     *       .rotated(7)
     *       .toList();   // returns [4, 5, 1, 2, 3] - same as rotated(2) due to modulo operation
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Additionally, it triggers a terminal operation
     * internally to collect all elements before rotating them.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>Yes</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param distance the number of positions to rotate the elements; positive values rotate
     *                 elements to the right, negative values rotate elements to the left
     * @return a new stream consisting of the elements of this stream rotated by the specified distance
     * @see #reversed()
     * @see #shuffled()
     * @see #sorted()
     * @see Collections#rotate(List, int)
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    S rotated(int distance);

    /**
     * Returns a stream consisting of the elements of this stream in a random order.
     * This is an intermediate operation that loads all elements into memory when called.
     *
     * <p>The returned stream contains all the elements of the original stream but in a randomly
     * shuffled order. The randomization is performed using the default random number generator.
     * For a deterministic shuffle with a specific random seed, use {@link #shuffled(Random)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Shuffle a stream of integers
     * Stream.of(1, 2, 3, 4, 5)
     *       .shuffled()
     *       .toList();   // returns elements in random order, e.g., [3, 1, 5, 4, 2]
     *
     * // Shuffle a stream of strings
     * Stream.of("apple", "banana", "cherry", "date")
     *       .shuffled()
     *       .toList();   // returns elements in random order, e.g., ["cherry", "apple", "date", "banana"]
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Additionally, it triggers a terminal operation
     * internally to collect all elements before shuffling them.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>Yes</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream consisting of the elements of this stream in a random order
     * @see #shuffled(Random)
     * @see #reversed()
     * @see #sorted()
     * @see #rotated(int)
     * @see Collections#shuffle(List)
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    S shuffled();

    /**
     * Returns a stream consisting of the elements of this stream in a random order determined by the provided Random instance.
     * This is an intermediate operation that loads all elements into memory when called.
     *
     * <p>The returned stream contains all the elements of the original stream but in a randomly
     * shuffled order. Unlike {@link #shuffled()}, this method allows specifying a custom Random instance,
     * which is useful when deterministic shuffling with a specific seed is required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Shuffle a stream of integers with a seeded random for deterministic results
     * Random seededRandom = new Random(42);
     * Stream.of(1, 2, 3, 4, 5)
     *       .shuffled(seededRandom)
     *       .toList();   // returns elements in deterministic random order
     *
     * // Shuffle strings with custom random
     * Random customRandom = new Random(123);
     * Stream.of("apple", "banana", "cherry", "date")
     *       .shuffled(customRandom)
     *       .toList();   // returns elements in deterministic random order
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Additionally, it triggers a terminal operation
     * internally to collect all elements before shuffling them.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>Yes</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param rnd the Random instance to use for shuffling elements
     * @return a new stream consisting of the elements of this stream in a random order determined by the provided Random
     * @see #shuffled()
     * @see #reversed()
     * @see #sorted()
     * @see #rotated(int)
     * @see Collections#shuffle(List, Random)
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    S shuffled(Random rnd);

    /**
     * Returns a stream consisting of the elements of this stream in sorted order.
     * This is an intermediate operation that loads all elements into memory when called.
     *
     * <p>The returned stream contains all the elements of the original stream but arranged
     * according to their natural ordering. Elements must be comparable to be sorted with this method.
     * If the elements are not comparable, a {@code ClassCastException} will be thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort a stream of integers
     * Stream.of(5, 3, 1, 4, 2)
     *       .sorted()
     *       .toList();   // returns [1, 2, 3, 4, 5]
     *
     * // Sort a stream of strings
     * Stream.of("cherry", "apple", "banana", "date")
     *       .sorted()
     *       .toList();   // returns ["apple", "banana", "cherry", "date"]
     * }</pre>
     *
     * <p>This method triggers a terminal operation internally to collect all elements before sorting them.
     * Although this method is marked as {@code @ParallelSupported}, the sorting operation itself
     * may not be performed in parallel depending on the stream implementation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>Yes</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream consisting of the elements of this stream in sorted order
     * @see #reverseSorted()
     * @see #reversed()
     * @see #shuffled()
     * @see #rotated(int)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    S sorted();

    /**
     * Returns a stream consisting of the elements of this stream in reverse sorted order.
     * This is an intermediate operation that loads all elements into memory when called.
     *
     * <p>The returned stream contains all the elements of the original stream but arranged
     * in reverse natural ordering (descending order). Elements must be comparable to be sorted with
     * this method. If the elements are not comparable, a {@code ClassCastException} will be thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sort a stream of integers in descending order
     * Stream.of(5, 3, 1, 4, 2)
     *       .reverseSorted()
     *       .toList();   // returns [5, 4, 3, 2, 1]
     *
     * // Sort a stream of strings in reverse order
     * Stream.of("cherry", "apple", "banana", "date")
     *       .reverseSorted()
     *       .toList();   // returns ["date", "cherry", "banana", "apple"]
     * }</pre>
     *
     * <p>This method triggers a terminal operation internally to collect all elements before sorting them.
     * Although this method is marked as {@code @ParallelSupported}, the sorting operation itself
     * may not be performed in parallel depending on the stream implementation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>Yes</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream consisting of the elements of this stream in reverse sorted order
     * @see #sorted()
     * @see #reversed()
     * @see #shuffled()
     * @see #rotated(int)
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    S reverseSorted();

    /**
     * Returns a stream consisting of the elements of this stream, repeating indefinitely.
     *
     * <p>The returned stream contains all the elements of the original stream repeated
     * in the same order indefinitely. All elements are stored in memory after the first
     * iteration to support subsequent repetitions of the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create an infinite cycling stream of numbers that repeats [1, 2, 3]
     * Stream.of(1, 2, 3)
     *       .cycled()
     *       .limit(8)
     *       .toList();   // returns [1, 2, 3, 1, 2, 3, 1, 2]
     *
     * // Create a repeating stream of strings
     * Stream.of("a", "b", "c")
     *       .cycled()
     *       .limit(7)
     *       .toList();   // returns ["a", "b", "c", "a", "b", "c", "a"]
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. The stream produced is infinite unless limited
     * by operations like {@code limit()} or {@code takeWhile()}.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream consisting of the elements of this stream repeated indefinitely
     * @see #cycled(long)
     * @see #sorted()
     * @see #reversed()
     * @see #limit(long)
     * @see #takeWhile(Object)
     */
    @SequentialOnly
    @IntermediateOp
    S cycled();

    /**
     * Returns a stream consisting of the elements of this stream, repeating for the specified number of rounds.
     *
     * <p>The returned stream contains all the elements of the original stream repeated
     * for the specified number of rounds. All elements are stored in memory after the first
     * iteration to support subsequent repetitions of the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a stream that repeats [1, 2, 3] for 2 rounds
     * Stream.of(1, 2, 3)
     *       .cycled(2)
     *       .toList();   // returns [1, 2, 3, 1, 2, 3]
     *
     * // Create a repeating stream of strings for 3 rounds
     * Stream.of("a", "b")
     *       .cycled(3)
     *       .toList();   // returns ["a", "b", "a", "b", "a", "b"]
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Unlike {@link #cycled()}, this method produces
     * a finite stream with the exact number of elements equal to the original count multiplied
     * by the specified number of rounds.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param rounds the number of times to repeat the elements of this stream; must be non-negative.
     *               A value of {@code 0} produces an empty stream.
     * @return a new stream consisting of the elements of this stream repeated for the specified number of rounds
     * @throws IllegalArgumentException if {@code rounds} is negative
     * @see #cycled()
     * @see #sorted()
     * @see #reversed()
     */
    @SequentialOnly
    @IntermediateOp
    S cycled(long rounds);

    /**
     * Returns a stream consisting of the elements of this stream, each paired with its index in the stream.
     * This is an intermediate operation that preserves the order of elements.
     *
     * <p>The returned stream contains pairs of elements and their zero-based position indices.
     * The first element is paired with index 0, the second with index 1, and so on. This operation
     * can be useful for accessing both the element and its position in subsequent operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a stream of indexed strings
     * Stream.of("apple", "banana", "cherry")
     *       .indexed()
     *       .forEach(i -> System.out.println("Element at " + i.index() + ": " + i.value()));
     * // Output:
     * // Element at 0: apple
     * // Element at 1: banana
     * // Element at 2: cherry
     *
     * // Filtering based on index
     * Stream.of("a", "b", "c", "d", "e")
     *       .indexed()
     *       .filter(i -> i.index() % 2 == 0)
     *       .map(i -> i.value())
     *       .toList();   // returns ["a", "c", "e"] (elements at even indices)
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. The indexing operation requires maintaining order
     * and cannot be effectively parallelized.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream consisting of the elements of this stream paired with their indices
     */
    @SequentialOnly
    @IntermediateOp
    Stream<IT> indexed();

    /**
     * Returns a stream consisting of the remaining elements of this stream after skipping the first n elements.
     * This is an intermediate operation that preserves the order of elements.
     *
     * <p>The returned stream skips the first n elements of the original stream and includes all remaining elements.
     * If the stream contains fewer than n elements, an empty stream will be returned. This operation is useful for
     * discarding a fixed number of elements from the beginning of a stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Skip the first 3 elements
     * Stream.of(1, 2, 3, 4, 5, 6)
     *       .skip(3)
     *       .toList();   // returns [4, 5, 6]
     *
     * // Skip more elements than present in stream
     * Stream.of("a", "b", "c")
     *       .skip(5)
     *       .toList();   // returns [] (empty list)
     *
     * // Skip zero elements (no effect)
     * Stream.of("apple", "banana", "cherry")
     *       .skip(0)
     *       .toList();   // returns ["apple", "banana", "cherry"]
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Skipping requires processing elements in order from
     * the beginning of the stream.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param n the number of leading elements to skip
     * @return a new stream consisting of the remaining elements of this stream after discarding
     *         the first n elements
     * @throws IllegalArgumentException if {@code n} is negative
     * @see #limit(long)
     * @see #skipUntil(Object)
     * @see #dropWhile(Object)
     * @see #filter(Object)
     */
    @SequentialOnly
    @IntermediateOp
    S skip(long n) throws IllegalArgumentException;

    /**
     * Returns a stream consisting of the remaining elements of this stream after skipping the first n elements,
     * applying the provided action to each skipped element. This is an intermediate operation.
     *
     * <p>The returned stream skips the first n elements of the original stream and includes all remaining elements.
     * Each skipped element is passed to the provided action for processing. If the stream contains fewer than n
     * elements, an empty stream will be returned and the action will be applied to all available elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Skip the first 3 elements and collect them separately
     * List<Integer> skipped = new ArrayList<>();
     * Stream.of(1, 2, 3, 4, 5, 6)
     *       .skip(3, skipped::add)
     *       .toList();   // returns [4, 5, 6], and skipped contains [1, 2, 3]
     *
     * // Skip and log skipped elements
     * Stream.of("a", "b", "c", "d", "e")
     *       .skip(2, s -> System.out.println("Skipped: " + s))
     *       .toList();   // returns ["c", "d", "e"] and prints "Skipped: a", "Skipped: b"
     * }</pre>
     *
     * <p>Unlike the basic {@link #skip(long)} method, this variation allows you to process
     * the skipped elements rather than discarding them entirely.
     *
     * <p>This method is marked with {@code @ParallelSupported}, meaning the {@code onSkip} may be
     * performed concurrently for multiple skipped elements when used in a parallel stream.
     * The implementation should ensure the action is thread-safe in such cases.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param n the number of leading elements to skip
     * @param onSkip the action to perform on each skipped element
     * @return a new stream consisting of the remaining elements of this stream after skipping
     *         the first n elements
     * @throws IllegalArgumentException if {@code n} is negative
     * @see #skip(long)
     * @see #filter(Object, Object)
     * @see #limit(long)
     * @see #dropWhile(Object, Object)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    S skip(long n, C onSkip) throws IllegalArgumentException;

    /**
     * Returns a stream consisting of the first maxSize elements of this stream.
     * This is an intermediate operation that preserves the order of elements.
     *
     * <p>The returned stream contains at most maxSize elements from the beginning of the original stream.
     * If the original stream contains fewer than maxSize elements, the entire stream is returned.
     * This operation is useful for restricting the number of elements processed from an
     * unbounded or large stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Limit a stream to its first 3 elements
     * Stream.of(1, 2, 3, 4, 5, 6)
     *       .limit(3)
     *       .toList();   // returns [1, 2, 3]
     *
     * // Limit with more elements than present in stream (no effect)
     * Stream.of("a", "b", "c")
     *       .limit(10)
     *       .toList();   // returns ["a", "b", "c"]
     *
     * // Limit an infinite stream to make it finite
     * Stream.iterate(1, n -> n + 1)
     *       .limit(5)
     *       .toList();   // returns [1, 2, 3, 4, 5]
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Limiting requires processing elements in order from
     * the beginning of the stream.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param maxSize the maximum number of elements to include in the new stream
     * @return a new stream consisting of at most maxSize elements from this stream
     * @throws IllegalArgumentException if {@code maxSize} is negative
     * @see #skip(long)
     * @see #takeWhile(Object)
     * @see #step(long)
     * @see #limit(long, long)
     * @see java.util.stream.Stream#limit(long)
     */
    @SequentialOnly
    @IntermediateOp
    S limit(long maxSize) throws IllegalArgumentException;

    /**
     * Returns a stream consisting of elements from this stream, starting after skipping
     * the first {@code offset} elements and containing at most {@code maxSize} elements.
     *
     * <p>This is a convenience method equivalent to calling {@code skip(offset).limit(maxSize)},
     * but may be more efficient as it can be optimized internally.
     *
     * <p>Example usage:
     * <pre>{@code
     * // Get elements 3, 4, 5 from a stream of 1-10
     * Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
     *       .limit(2, 3)
     *       .toList();   // returns [3, 4, 5]
     *
     * // Pagination: get page 3 with page size 10 (elements 21-30)
     * Stream.rangeClosed(1, 100)
     *       .limit(20, 10)
     *       .toList();   // returns [21, 22, 23, 24, 25, 26, 27, 28, 29, 30]
     *
     * // If offset exceeds stream size, returns empty stream
     * Stream.of(1, 2, 3)
     *       .limit(5, 10)
     *       .toList();   // returns []
     *
     * // If fewer elements available than maxSize, returns remaining elements
     * Stream.of(1, 2, 3, 4, 5)
     *       .limit(3, 10)
     *       .toList();   // returns [4, 5]
     * }</pre>
     *
     * <p>This is an intermediate operation that only runs sequentially, even in parallel streams,
     * as noted by the {@code @SequentialOnly} annotation. Both skipping and limiting require
     * processing elements in order from the beginning of the stream.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param offset the number of leading elements to skip before starting to include elements
     * @param maxSize the maximum number of elements to include in the new stream
     * @return a new stream consisting of at most {@code maxSize} elements after skipping the first
     *         {@code offset} elements from this stream
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code offset} or {@code maxSize} is negative
     * @see #skip(long)
     * @see #limit(long)
     * @see #step(long)
     */
    @SequentialOnly
    @IntermediateOp
    S limit(long offset, long maxSize) throws IllegalArgumentException;

    /**
     * Returns a stream consisting of every 'step'th element of this stream.
     * This is an intermediate operation that preserves the order of elements.
     *
     * <p>The returned stream contains elements at positions 0, step, 2*step, and so on.
     * For example, with a step size of 2, only elements at even-indexed positions (0, 2, 4, ...)
     * will be included. This operation can be useful for sampling or reducing the density of elements
     * in a stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select every second element from a stream
     * Stream.of(1, 2, 3, 4, 5, 6, 7, 8)
     *       .step(2)
     *       .toList();   // returns [1, 3, 5, 7]
     *
     * // Select every third element
     * Stream.of("a", "b", "c", "d", "e", "f", "g")
     *       .step(3)
     *       .toList();   // returns ["a", "d", "g"]
     *
     * // Step size of 1 (no effect)
     * Stream.of(1, 2, 3)
     *       .step(1)
     *       .toList();   // returns [1, 2, 3]
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Stepping through elements requires processing
     * them in order, which is inherently sequential.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param step the interval between selected elements; must be positive (greater than 0)
     * @return a new stream consisting of every 'step'th element of this stream
     * @throws IllegalArgumentException if {@code step} is less than 1
     * @see #limit(long)
     * @see #skip(long)
     * @see #filter(Object)
     */
    @SequentialOnly
    @IntermediateOp
    S step(long step) throws IllegalArgumentException;

    /**
     * Returns a stream with a rate limit applied. The rate limit is specified by the <i>permitsPerSecond</i> parameter.
     * This is an intermediate operation that controls the rate at which elements are pulled from the stream.
     *
     * <p>The returned stream will limit the rate of element emission to approximately the specified number of
     * permits per second. This is useful for throttling operations when working with rate-limited resources
     * or when you need to control the pace of processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process elements at a rate of 2 per second
     * Stream.of(1, 2, 3, 4, 5)
     *       .rateLimited(2.0)
     *       .forEach(System.out::println);   // prints numbers at ~2 per second
     *
     * // Throttle API calls to 10 requests per second
     * Stream.of(apiRequests)
     *       .rateLimited(10.0)
     *       .forEach(request -> makeApiCall(request));
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Rate limiting requires sequential processing to maintain
     * the proper rate of element emission.
     *
     * <p><b>How it compares to the other time-based intermediate operators:</b></p>
     * <table border="1">
     *   <caption>{@code delay} vs. {@code rateLimited} vs. {@code debounce}</caption>
     *   <tr><th>Operator</th><th>Effect</th><th>Emits every element?</th><th>Drops elements?</th><th>Typical use</th></tr>
     *   <tr>
     *     <td>{@link #delay(Duration)}</td>
     *     <td>Sleeps a fixed {@code duration} before each element except the first (constant spacing between elements)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Pace a sequence with a fixed gap between elements</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #rateLimited(double)} / {@link #rateLimited(RateLimiter)}</td>
     *     <td>Blocks until a permit is available so throughput stays at or below a target rate (permits per second)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Cap throughput against a rate-limited resource</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #debounce(Duration)}</td>
     *     <td>Keeps the latest element in a burst; emits it when a later pull observes a quiet gap, or when upstream ends</td>
     *     <td>No</td>
     *     <td>Yes (superseded elements)</td>
     *     <td>Collapse rapid bursts into a single trailing value</td>
     *   </tr>
     * </table>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param permitsPerSecond the rate limit, specified as permits per second. Must be positive.
     * @return a new stream with the rate limit applied.
     * @throws IllegalArgumentException if {@code permitsPerSecond} is negative or zero
     * @see #rateLimited(RateLimiter)
     * @see #delay(Duration)
     * @see #debounce(Duration)
     * @see RateLimiter#create(double)
     */
    @SequentialOnly
    @IntermediateOp
    default S rateLimited(final double permitsPerSecond) {
        return rateLimited(RateLimiter.create(permitsPerSecond));
    }

    /**
     * Returns a stream with a rate limit applied. The rate limit is specified by the <i>RateLimiter</i> parameter.
     * This is an intermediate operation that controls the rate at which elements are pulled from the stream.
     *
     * <p>The returned stream will use the provided RateLimiter to control the rate of element emission.
     * This allows for more sophisticated rate limiting strategies, including burst support and custom
     * rate limiting implementations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a rate limiter with burst capacity
     * RateLimiter rateLimiter = RateLimiter.create(5.0);   // 5 permits per second
     * Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
     *       .rateLimited(rateLimiter)
     *       .forEach(System.out::println);   // processes at controlled rate
     *
     * // Share a rate limiter across multiple streams
     * RateLimiter sharedLimiter = RateLimiter.create(10.0);
     * stream1.rateLimited(sharedLimiter).forEach(processA);
     * stream2.rateLimited(sharedLimiter).forEach(processB);
     * // Both streams share the same 10 permits per second limit
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. Rate limiting requires sequential processing to maintain
     * the proper rate of element emission.
     *
     * <p><b>How it compares to the other time-based intermediate operators:</b></p>
     * <table border="1">
     *   <caption>{@code delay} vs. {@code rateLimited} vs. {@code debounce}</caption>
     *   <tr><th>Operator</th><th>Effect</th><th>Emits every element?</th><th>Drops elements?</th><th>Typical use</th></tr>
     *   <tr>
     *     <td>{@link #delay(Duration)}</td>
     *     <td>Sleeps a fixed {@code duration} before each element except the first (constant spacing between elements)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Pace a sequence with a fixed gap between elements</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #rateLimited(double)} / {@link #rateLimited(RateLimiter)}</td>
     *     <td>Blocks until a permit is available so throughput stays at or below a target rate (permits per second)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Cap throughput against a rate-limited resource</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #debounce(Duration)}</td>
     *     <td>Keeps the latest element in a burst; emits it when a later pull observes a quiet gap, or when upstream ends</td>
     *     <td>No</td>
     *     <td>Yes (superseded elements)</td>
     *     <td>Collapse rapid bursts into a single trailing value</td>
     *   </tr>
     * </table>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param rateLimiter the RateLimiter instance to use for controlling the rate of element emission
     * @return a new stream with the rate limit applied.
     * @see #rateLimited(double)
     * @see #delay(Duration)
     * @see #debounce(Duration)
     * @see RateLimiter
     * @see RateLimiter#acquire()
     */
    @SequentialOnly
    @IntermediateOp
    S rateLimited(RateLimiter rateLimiter);

    /**
     * Delay each element in this {@code Stream} by a given {@link Duration} except the first element.
     * This is an intermediate operation that introduces a time delay between processing of consecutive elements.
     *
     * <p>The returned stream will pause for the specified duration before emitting each element after
     * the first one. The first element is emitted immediately without delay. This is useful for
     * simulating time-based events, rate limiting operations, or creating timed sequences.
     *
     * <p>The delay is applied before each element (except the first) is passed to subsequent operations.
     * This means the delay occurs between elements, creating a consistent time gap in the stream processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Delay each element by 1 second (except the first)
     * Stream.of("A", "B", "C", "D")
     *       .delay(Duration.ofSeconds(1))
     *       .forEach(System.out::println);
     * // Prints "A" immediately, then "B" after 1s, "C" after 2s, "D" after 3s
     *
     * // Simulate events arriving at 500ms intervals
     * Stream.of("event1", "event2", "event3", "event4")
     *       .delay(Duration.ofMillis(500))
     *       .forEach(event -> processEvent(event));
     * // Processes events with 500ms delay between each
     *
     * // Rate limiting API calls
     * Stream.of(request1, request2, request3)
     *       .delay(Duration.ofMillis(100))
     *       .forEach(request -> apiClient.send(request));
     * // Sends first request immediately, others with 100ms spacing
     *
     * // Combine with other operations
     * List<Integer> results = Stream.of(1, 2, 3, 4, 5)
     *       .delay(Duration.ofMillis(200))
     *       .map(n -> n * 2)
     *       .filter(n -> n > 4)
     *       .toList();
     * // results = [6, 8, 10], processed with 200ms delays
     *
     * // Empty stream has no delay
     * Stream.<String>empty()
     *       .delay(Duration.ofSeconds(1))
     *       .forEach(System.out::println);
     * // Completes immediately as there are no elements
     * }</pre>
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation. The delay mechanism requires sequential processing
     * to maintain the proper timing between elements.
     *
     * <p><b>How it compares to the other time-based intermediate operators:</b></p>
     * <table border="1">
     *   <caption>{@code delay} vs. {@code rateLimited} vs. {@code debounce}</caption>
     *   <tr><th>Operator</th><th>Effect</th><th>Emits every element?</th><th>Drops elements?</th><th>Typical use</th></tr>
     *   <tr>
     *     <td>{@link #delay(Duration)}</td>
     *     <td>Sleeps a fixed {@code duration} before each element except the first (constant spacing between elements)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Pace a sequence with a fixed gap between elements</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #rateLimited(double)} / {@link #rateLimited(RateLimiter)}</td>
     *     <td>Blocks until a permit is available so throughput stays at or below a target rate (permits per second)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Cap throughput against a rate-limited resource</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #debounce(Duration)}</td>
     *     <td>Keeps the latest element in a burst; emits it when a later pull observes a quiet gap, or when upstream ends</td>
     *     <td>No</td>
     *     <td>Yes (superseded elements)</td>
     *     <td>Collapse rapid bursts into a single trailing value</td>
     *   </tr>
     * </table>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param duration the duration to delay each element in the stream (except the first element).
     *                 Must not be {@code null}.
     * @return a new stream with the delay applied to each element.
     * @throws IllegalArgumentException if {@code duration} is {@code null}
     * @see #debounce(Duration)
     * @see #rateLimited(double)
     * @see #rateLimited(RateLimiter)
     */
    @SequentialOnly
    @IntermediateOp
    S delay(Duration duration);

    /**
     * Delay each element in this {@code Stream} by a given {@link java.time.Duration} except the first element.
     * This is an intermediate operation that introduces a time delay between processing of consecutive elements.
     *
     * <p>The returned stream will pause for the specified duration before emitting each element after
     * the first one. The first element is emitted immediately without delay. This is useful for
     * simulating time-based events or for rate limiting operations.
     *
     * <p>This is a convenience overload that accepts a {@link java.time.Duration} instead of
     * {@link Duration}. The duration is converted to milliseconds internally.
     *
     * <p>This method only runs sequentially, even in parallel streams, as noted by the
     * {@code @SequentialOnly} annotation.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param duration the duration to delay each element in the stream (except the first element). Must not be {@code null}.
     * @return a new stream with the delay applied to each element.
     * @throws IllegalArgumentException if {@code duration} is {@code null}
     * @see #delay(Duration)
     * @see #rateLimited(double)
     * @see #rateLimited(RateLimiter)
     */
    @SequentialOnly
    @IntermediateOp
    default S delay(java.time.Duration duration) {
        final Duration durationToUse = duration == null ? null : Duration.ofMillis(duration.toMillis()); // to throw same exception as in the other overload for null.
        return delay(durationToUse);
    }

    /**
     * Returns a stream that performs pull-based trailing debouncing: it keeps the latest pulled element
     * and emits it when a later upstream pull shows that no newer element arrived for at least the specified
     * {@code duration}, or when the upstream is exhausted.
     *
     * <p>When elements are pulled rapidly &mdash; a "burst" in which each element arrives less than
     * {@code duration} after the previous one &mdash; all but the most recent element of the burst are
     * discarded; the most recent element survives. It is emitted when a later pull observes a gap of at least
     * {@code duration}, or when the upstream is exhausted. The final element of the stream is always emitted
     * without waiting for an additional timer. Arrival time is measured with {@link System#currentTimeMillis()}
     * at the moment each element is pulled from the upstream, so this operator is only meaningful for streams
     * whose upstream produces elements over time. For a stream that produces all of its elements immediately
     * (e.g. a stream over an in-memory collection), every gap is effectively zero, so only the single last
     * element is emitted.
     *
     * <p>This differs from {@link #rateLimited(double)}, which spreads permits evenly over time, and from
     * window/throttle operators that emit periodically. It is also not a scheduler-based debounce: it does
     * not start a background timer, and pending elements are emitted only while downstream iteration pulls
     * from this stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse rapid bursts: keep the latest value and emit it when iteration observes a 300ms quiet gap.
     * keystrokeStream
     *       .debounce(Duration.ofMillis(300))
     *       .forEach(text -> performSearch(text));
     * }</pre>
     *
     * <p><b>Behavior Details:</b></p>
     * <ul>
     *   <li>An element is emitted only when a later pull observes that the next element arrived at least
     *       {@code duration} later, or when it is the final pending element of the stream.</li>
     *   <li>Within a burst (consecutive elements less than {@code duration} apart), only the most recent
     *       element survives; the earlier ones are silently discarded.</li>
     *   <li>The relative order of the surviving elements is preserved.</li>
     *   <li>Arrival time is tracked with {@link System#currentTimeMillis()} at pull time.</li>
     * </ul>
     *
     * <p>Only the debounce step itself is forced sequential, as noted by the {@code @SequentialOnly} annotation.
     * If this method is invoked on a parallel stream, the returned stream may switch back to parallel mode
     * for downstream operations.
     *
     * <p><b>How it compares to the other time-based intermediate operators:</b></p>
     * <table border="1">
     *   <caption>{@code delay} vs. {@code rateLimited} vs. {@code debounce}</caption>
     *   <tr><th>Operator</th><th>Effect</th><th>Emits every element?</th><th>Drops elements?</th><th>Typical use</th></tr>
     *   <tr>
     *     <td>{@link #delay(Duration)}</td>
     *     <td>Sleeps a fixed {@code duration} before each element except the first (constant spacing between elements)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Pace a sequence with a fixed gap between elements</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #rateLimited(double)} / {@link #rateLimited(RateLimiter)}</td>
     *     <td>Blocks until a permit is available so throughput stays at or below a target rate (permits per second)</td>
     *     <td>Yes</td>
     *     <td>No</td>
     *     <td>Cap throughput against a rate-limited resource</td>
     *   </tr>
     *   <tr>
     *     <td>{@link #debounce(Duration)}</td>
     *     <td>Keeps the latest element in a burst; emits it when a later pull observes a quiet gap, or when upstream ends</td>
     *     <td>No</td>
     *     <td>Yes (superseded elements)</td>
     *     <td>Collapse rapid bursts into a single trailing value</td>
     *   </tr>
     * </table>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param duration the quiet period used to decide whether a pending element has been superseded.
     *                 Must not be {@code null} and must have a positive millisecond value.
     * @return a new stream that emits the most recent element of each burst when a later pull observes a
     *         quiet gap, and always emits the final pending element.
     * @throws IllegalArgumentException if {@code duration} is {@code null} or has a non-positive
     *         millisecond value
     * @see #delay(Duration)
     * @see #rateLimited(double)
     * @see #rateLimited(RateLimiter)
     */
    @SequentialOnly
    @IntermediateOp
    S debounce(Duration duration);

    /**
     * Performs the given action on the elements pulled by downstream/terminal operation.
     * This is an intermediate operation.
     *
     * <br />
     * Same as {@code onEach}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Log each element as it flows through the pipeline
     * Stream.of("apple", "banana", "cherry")
     *       .filter(s -> s.length() > 5)
     *       .peek(s -> System.out.println("Processing: " + s))
     *       .map(String::toUpperCase)
     *       .toList();   // prints "Processing: banana" and "Processing: cherry"
     *
     * // Count elements that pass through a stage
     * AtomicInteger count = new AtomicInteger();
     * Stream.of(1, 2, 3, 4, 5)
     *       .filter(n -> n % 2 == 0)
     *       .peek(n -> count.incrementAndGet())
     *       .toList();   // count is 2 after processing
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param action the action to be performed on the elements pulled by downstream/terminal operation
     * @return a new stream consisting of the elements of this stream with the provided action applied to each element.
     * @see #onEach(Object)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    default S peek(final C action) {
        return onEach(action);
    }

    /**
     * Performs the given action on the elements pulled by downstream/terminal operation.
     *
     * <p>The action is executed lazily as elements flow through the stream pipeline. It is only invoked
     * for elements that are actually consumed by a terminal operation.
     *
     * <p>This is an intermediate operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Debug intermediate results in a stream pipeline
     * Stream.of(1, 2, 3, 4, 5)
     *       .filter(n -> n % 2 == 0)
     *       .onEach(n -> System.out.println("Filtered value: " + n))
     *       .map(n -> n * n)
     *       .onEach(n -> System.out.println("Mapped value: " + n))
     *       .toList();
     * // Output: "Filtered value: 2", "Mapped value: 4", "Filtered value: 4", "Mapped value: 16"
     *
     * // Verify stream processing without modifying data
     * List<String> result = Stream.of("a", "b", "c")
     *       .onEach(System.out::println)  // prints each element for debugging
     *       .map(String::toUpperCase)
     *       .toList();
     * // result = ["A", "B", "C"]
     *
     * // Track elements passing through a filter
     * AtomicInteger count = new AtomicInteger();
     * Stream.of(1, 2, 3, 4, 5)
     *       .filter(n -> n % 2 == 0)
     *       .onEach(n -> count.incrementAndGet())
     *       .toList();
     * // count.get() == 2 (only even numbers passed)
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param action the action to be performed on the elements pulled by downstream/terminal operation
     * @return a new stream consisting of the elements of this stream with the provided action applied to each element.
     * @see #peek(Object)
     */
    @ParallelSupported
    @IntermediateOp
    S onEach(final C action);

    /**
     * Prepends the elements of the provided stream to this stream.
     * The elements of the provided stream will appear before the elements of this stream in the resulting stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Prepend header elements to a data stream
     * Stream<String> header = Stream.of("Header1", "Header2");
     * Stream<String> data = Stream.of("Data1", "Data2", "Data3");
     * data.prepend(header).toList();
     * // Returns ["Header1", "Header2", "Data1", "Data2", "Data3"]
     *
     * // Combine multiple streams with priority ordering
     * Stream<Integer> priorityItems = Stream.of(99, 100);
     * Stream<Integer> normalItems = Stream.of(1, 2, 3);
     * normalItems.prepend(priorityItems).toList();
     * // Returns [99, 100, 1, 2, 3]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param stream the stream whose elements should be prepended to this stream.
     * @return a new stream consisting of the elements of the provided stream followed by the elements of this stream.
     */
    @SequentialOnly
    @IntermediateOp
    S prepend(S stream);

    /**
     * Prepends the element of the provided optional to this stream.
     * If the optional is present, its element will appear before the elements of this stream in the resulting stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Prepend a default value if available
     * Optional<String> defaultValue = Optional.of("Default");
     * Stream<String> data = Stream.of("A", "B", "C");
     * data.prepend(defaultValue).toList();
     * // Returns ["Default", "A", "B", "C"]
     *
     * // Empty optional has no effect
     * Optional<Integer> empty = Optional.empty();
     * Stream<Integer> numbers = Stream.of(1, 2, 3);
     * numbers.prepend(empty).toList();
     * // Returns [1, 2, 3]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param op the optional whose element should be prepended to this stream.
     * @return a new stream consisting of the element of the provided optional (if present) followed by the elements of this stream.
     */
    @SequentialOnly
    @IntermediateOp
    S prepend(OT op);

    /**
     * Appends the elements of the provided stream to this stream.
     * The elements of the provided stream will appear after the elements of this stream in the resulting stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Append footer elements to a data stream
     * Stream<String> data = Stream.of("Data1", "Data2", "Data3");
     * Stream<String> footer = Stream.of("Footer1", "Footer2");
     * data.append(footer).toList();
     * // Returns ["Data1", "Data2", "Data3", "Footer1", "Footer2"]
     *
     * // Combine multiple data sources sequentially
     * Stream<Integer> batch1 = Stream.of(1, 2, 3);
     * Stream<Integer> batch2 = Stream.of(4, 5, 6);
     * batch1.append(batch2).toList();
     * // Returns [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param stream the stream whose elements should be appended to this stream.
     * @return a new stream consisting of the elements of this stream followed by the elements of the provided stream.
     */
    @SequentialOnly
    @IntermediateOp
    S append(S stream);

    /**
     * Appends the element of the provided optional to this stream.
     * If the optional is present, its element will appear after the elements of this stream in the resulting stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Append an optional suffix value
     * Stream<String> data = Stream.of("A", "B", "C");
     * Optional<String> suffix = Optional.of("END");
     * data.append(suffix).toList();
     * // Returns ["A", "B", "C", "END"]
     *
     * // Empty optional has no effect
     * Stream<Integer> numbers = Stream.of(1, 2, 3);
     * Optional<Integer> empty = Optional.empty();
     * numbers.append(empty).toList();
     * // Returns [1, 2, 3]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param op the optional whose element should be appended to this stream.
     * @return a new stream consisting of the elements of this stream followed by the element of the provided optional (if present).
     */
    @SequentialOnly
    @IntermediateOp
    S append(OT op);

    /**
     * Returns a stream consisting of the elements of this stream if it is not empty, or the elements
     * from the stream provided by the supplier if this stream is empty.
     * If this stream is empty, the supplier is called and the elements from the supplied stream will be returned instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Provide default values when stream is empty
     * Stream.<String>empty()
     *       .appendIfEmpty(() -> Stream.of("default1", "default2"))
     *       .toList();
     * // Returns ["default1", "default2"]
     *
     * // Non-empty stream - supplier is not called
     * Stream.of("A", "B")
     *       .appendIfEmpty(() -> Stream.of("default1", "default2"))
     *       .toList();
     * // Returns ["A", "B"]
     *
     * // Use with filtering to ensure results
     * Stream.of(1, 2, 3, 4)
     *       .filter(n -> n > 10)  // filters out all; no elements match
     *       .appendIfEmpty(() -> Stream.of(0))
     *       .toList();
     * // Returns [0]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param supplier the supplier that provides an alternative stream if this stream is empty.
     * @return a new stream consisting of the elements of this stream if not empty, or the elements from the supplied stream if this stream was empty.
     * @see #defaultIfEmpty(Supplier)
     */
    @SequentialOnly
    @IntermediateOp
    S appendIfEmpty(Supplier<? extends S> supplier);

    /**
     * Returns a stream consisting of the elements of this stream if it is not empty, or the elements
     * from the stream provided by the supplier if this stream is empty.
     * If this stream is empty, the elements from the supplied stream will be returned instead.
     * If this stream has any elements, the supplier is not called and the stream remains unchanged.
     *
     * <p><b>Note:</b> This method is functionally equivalent to {@link #appendIfEmpty(Supplier)} and delegates to it.
     * Both methods provide identical behavior - use whichever name better expresses the intent in your code.
     * {@code defaultIfEmpty} emphasizes providing default values, while {@code appendIfEmpty} emphasizes
     * appending elements when empty.
     *
     * <p>This is an intermediate operation that provides a fallback mechanism for empty streams.
     * It is particularly useful when you want to ensure a stream always produces results, or when
     * working with optional data that needs default values. The supplier is invoked lazily - only
     * when a terminal operation is executed and the stream is actually empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Empty stream gets default values
     * List<String> result = Stream.<String>empty()
     *       .defaultIfEmpty(() -> Stream.of("default1", "default2"))
     *       .toList();
     * // result = ["default1", "default2"]
     *
     * // Non-empty stream - supplier is not called
     * List<String> result = Stream.of("A", "B")
     *       .defaultIfEmpty(() -> Stream.of("default"))
     *       .toList();
     * // result = ["A", "B"]
     *
     * // Use with filtering to ensure results
     * List<Integer> result = Stream.of(1, 2, 3, 4)
     *       .filter(n -> n > 10)  // filters out all; no elements match
     *       .defaultIfEmpty(() -> Stream.of(0))
     *       .toList();
     * // result = [0]
     *
     * // Provide multiple defaults
     * List<Integer> numbers = Stream.of(1, 2, 3)
     *       .filter(n -> n > 100)
     *       .defaultIfEmpty(() -> Stream.of(-1, -2, -3))
     *       .toList();
     * // If no data > 100, returns [-1, -2, -3]
     *
     * // Database query with fallback
     * List<Product> products = productStream
     *       .filter(p -> p.getCategory().equals("Electronics"))
     *       .defaultIfEmpty(() -> Stream.of(Product.getDefaultProduct()))
     *       .toList();
     * // Returns Electronics products if available, otherwise default product
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param supplier the supplier that provides an alternative stream if this stream is empty.
     *                 Must not be {@code null}.
     * @return a new stream consisting of the elements of this stream if not empty, or the elements from the supplied stream if this stream was empty.
     * @see #appendIfEmpty(Supplier)
     */
    @SequentialOnly
    @IntermediateOp
    default S defaultIfEmpty(final Supplier<? extends S> supplier) {
        return appendIfEmpty(supplier);
    }

    /**
     * Throws a {@code NoSuchElementException} in the executed terminal operation if this {@code Stream} is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Ensure stream has elements before processing
     * Stream.of(1, 2, 3)
     *       .filter(n -> n > 5)
     *       .throwIfEmpty()  // throws NoSuchElementException in the subsequent toList() because no elements match
     *       .toList();
     *
     * // Valid when stream has elements
     * Stream.of(1, 2, 3)
     *       .filter(n -> n > 1)
     *       .throwIfEmpty()  // stream has elements, so no exception is thrown
     *       .toList();       // returns [2, 3]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a stream with the same elements as this stream, which throws if it turns out to be
     *         empty when a terminal operation is executed.
     * @throws java.util.NoSuchElementException if the stream is empty when a terminal operation is executed
     */
    @SequentialOnly
    @IntermediateOp
    S throwIfEmpty();

    /**
     * Throws a custom exception provided by the specified {@code exceptionSupplier} in the executed terminal operation if this {@code Stream} is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Throw custom exception with meaningful message
     * Stream.of(users)
     *       .filter(user -> user.isActive())
     *       .throwIfEmpty(() -> new IllegalStateException("No active users found"))
     *       .toList();
     *
     * // Use with business logic validation
     * Stream.of(orders)
     *       .filter(order -> order.getStatus() == Status.PENDING)
     *       .throwIfEmpty(() -> new BusinessException("No pending orders to process"))
     *       .forEach(this::processOrder);
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param exceptionSupplier the supplier of the exception to be thrown if this stream is empty.
     * @return a stream with the same elements as this stream, which throws if it turns out to be
     *         empty when a terminal operation is executed.
     * @throws RuntimeException the exception provided by the supplier if the stream is empty when a terminal operation is executed
     */
    @SequentialOnly
    @IntermediateOp
    S throwIfEmpty(Supplier<? extends RuntimeException> exceptionSupplier);

    /**
     * Executes the given action if the stream is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Log when stream is empty
     * Stream.of(data)
     *       .filter(item -> item.isValid())
     *       .ifEmpty(() -> System.out.println("No valid items found"))
     *       .forEach(this::process);
     *
     * // Track empty results
     * AtomicBoolean isEmpty = new AtomicBoolean(false);
     * Stream.of(1, 2, 3)
     *       .filter(n -> n > 10)
     *       .ifEmpty(() -> isEmpty.set(true))
     *       .toList();
     * // isEmpty.get() == true
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param action the action to be executed if the stream is empty
     * @return a stream with the same elements as this stream, which executes the action if it
     *         turns out to be empty when a terminal operation is executed
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    S ifEmpty(Runnable action);

    /**
     * Joins the elements of this stream into a single String, separated by the specified delimiter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join strings with comma separator
     * String result = Stream.of("apple", "banana", "cherry").join(", ");
     * // Returns "apple, banana, cherry"
     *
     * // Join numbers
     * String numbers = Stream.of(1, 2, 3, 4, 5).join("-");
     * // Returns "1-2-3-4-5"
     *
     * // Join with empty stream
     * String empty = Stream.<String>empty().join(", ");
     * // Returns ""
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param delimiter the sequence of characters to be used as a delimiter between each element in the resulting String.
     * @return a String consisting of the elements of this stream, separated by the specified delimiter.
     * @see #join(CharSequence, CharSequence, CharSequence)
     */
    @SequentialOnly
    @TerminalOp
    default String join(final CharSequence delimiter) {
        return join(delimiter, "", "");
    }

    /**
     * Joins the elements of this stream into a single String, separated by the specified delimiter, prefix, and suffix.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join with brackets and comma
     * String result = Stream.of("apple", "banana", "cherry").join(", ", "[", "]");
     * // Returns "[apple, banana, cherry]"
     *
     * // Create JSON-like array format
     * String json = Stream.of(1, 2, 3, 4).join(", ", "{values: [", "]}");
     * // Returns "{values: [1, 2, 3, 4]}"
     *
     * // SQL IN clause format
     * String sql = Stream.of("'a'", "'b'", "'c'").join(", ", "IN (", ")");
     * // Returns "IN ('a', 'b', 'c')"
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param delimiter the sequence of characters to be used as a delimiter between each element in the resulting String.
     * @param prefix the sequence of characters to be added at the beginning of the resulting String.
     * @param suffix the sequence of characters to be added at the end of the resulting String.
     * @return a String consisting of the elements of this stream, separated by the specified delimiter, and surrounded by the specified prefix and suffix.
     * @see #join(CharSequence)
     * @see #joinTo(Joiner)
     */
    @SequentialOnly
    @TerminalOp
    String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix);

    /**
     * Joins the elements of this stream into a single String using the provided Joiner.
     * The Joiner specifies the delimiter, prefix, and suffix to be used for joining.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use a custom Joiner configuration
     * Joiner joiner = Joiner.with(", ", "[", "]");
     * Stream.of("a", "b", "c").joinTo(joiner);
     * String result = joiner.toString();
     * // result is "[a, b, c]"
     *
     * // Reuse Joiner for multiple streams
     * Joiner sharedJoiner = Joiner.with("; ");
     * Stream.of("first", "second").joinTo(sharedJoiner);
     * Stream.of("third", "fourth").joinTo(sharedJoiner);
     * // sharedJoiner.toString() returns "first; second; third; fourth"
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param joiner the Joiner specifying how to join the elements of the stream.
     * @return the provided Joiner after appending all elements of the stream.
     * @see Joiner
     * @see #join(CharSequence, CharSequence, CharSequence)
     */
    @SequentialOnly
    @TerminalOp
    Joiner joinTo(final Joiner joiner);

    /**
     * Calculates the percentiles of the elements in the stream.
     * All elements will be loaded into memory and sorted if not yet.
     * The returned map contains the percentile values as keys and the corresponding elements as values.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p>This method uses predefined {@link Percentage} enum values to compute percentiles.
     * The available percentiles include common values such as:
     * <ul>
     *   <li>{@code Percentage._0_0001} - 0.0001% percentile</li>
     *   <li>{@code Percentage._1} - 1% percentile</li>
     *   <li>{@code Percentage._5} - 5% percentile</li>
     *   <li>{@code Percentage._10} - 10% percentile</li>
     *   <li>{@code Percentage._25} - 25% percentile (first quartile)</li>
     *   <li>{@code Percentage._50} - 50% percentile (median)</li>
     *   <li>{@code Percentage._75} - 75% percentile (third quartile)</li>
     *   <li>{@code Percentage._90} - 90% percentile</li>
     *   <li>{@code Percentage._95} - 95% percentile</li>
     *   <li>{@code Percentage._99} - 99% percentile</li>
     *   <li>{@code Percentage._99_99} - 99.99% percentile</li>
     *   <li>{@code Percentage._99_9999} - 99.9999% percentile</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
     *       .percentiles()
     *       .ifPresent(map -> {
     *           System.out.println("50th percentile (median): " + map.get(Percentage._50));
     *           System.out.println("90th percentile: " + map.get(Percentage._90));
     *       });
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an Optional containing a Map of Percentages to elements if the stream is not empty, otherwise an empty Optional.
     * @see N#percentilesOfSorted(int[])
     * @see Percentage
     */
    @SequentialOnly
    @TerminalOp
    u.Optional<Map<Percentage, T>> percentiles();

    /**
     * Counts the number of elements in the stream.
     * This is a terminal operation that consumes the stream and closes it after execution.
     *
     * <p><b>Performance Note:</b> DO NOT use {@code stream.count() > 0} to check if the stream is empty.
     * For better performance, use {@code stream.first().isPresent()} or {@code stream.first().isEmpty()},
     * which can short-circuit and avoid processing the entire stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long count = Stream.of(1, 2, 3, 4, 5).count();   // returns 5
     *
     * // ❌ Inefficient - processes entire stream
     * if (stream.count() > 0) { ... }
     *
     * // ✅ Efficient - stops at first element
     * if (stream.first().isPresent()) { ... }
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return the count of elements in the stream as a long value. Returns 0 if the stream is empty.
     * @see #first()
     */
    @SequentialOnly
    @TerminalOp
    long count();

    /**
     * Returns the first element of the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get first element from non-empty stream
     * Optional<String> first = Stream.of("apple", "banana", "cherry").first();
     * // first.get() returns "apple"
     *
     * // Empty stream returns empty Optional
     * Optional<Integer> empty = Stream.<Integer>empty().first();
     * // empty.isEmpty() is true
     *
     * // Find first matching element (more efficient than filter + first)
     * Optional<Integer> firstEven = Stream.of(1, 3, 4, 5, 6).filter(n -> n % 2 == 0).first();
     * // firstEven.get() returns 4
     *
     * // Check if stream has elements (efficient alternative to count() > 0)
     * boolean hasElements = Stream.of(data).first().isPresent();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an Optional containing the first element of the stream if it exists, otherwise an empty Optional.
     * @see #last()
     * @see #elementAt(long)
     */
    @SequentialOnly
    @TerminalOp
    OT first();

    /**
     * Returns the last element of the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get last element from non-empty stream
     * Optional<String> last = Stream.of("apple", "banana", "cherry").last();
     * // last.get() returns "cherry"
     *
     * // Empty stream returns empty Optional
     * Optional<Integer> empty = Stream.<Integer>empty().last();
     * // empty.isEmpty() is true
     *
     * // Find last matching element
     * Optional<Integer> lastEven = Stream.of(1, 2, 3, 4, 5, 6).filter(n -> n % 2 == 0).last();
     * // lastEven.get() returns 6
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an Optional containing the last element of the stream if it exists, otherwise an empty Optional.
     * @see #first()
     * @see #elementAt(long)
     */
    @SequentialOnly
    @TerminalOp
    OT last();

    /**
     * Returns the element at the specified position in this stream.
     * The position is zero-based, meaning the first element in the stream has a position of 0, the second has a position of 1, and so on.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get element at specific index
     * Optional<String> third = Stream.of("a", "b", "c", "d", "e").elementAt(2);
     * // third.get() returns "c" (zero-based index)
     *
     * // Position beyond stream size returns empty
     * Optional<Integer> notFound = Stream.of(1, 2, 3).elementAt(10);
     * // notFound.isEmpty() is true
     *
     * // Get second filtered element
     * Optional<Integer> result = Stream.of(1, 2, 3, 4, 5, 6)
     *       .filter(n -> n % 2 == 0)  // [2, 4, 6]
     *       .elementAt(1);            // gets the second even number
     * // result.get() returns 4
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param position the zero-based position of the element to return; must be non-negative.
     * @return an Optional containing the element at the specified position in this stream if it exists, otherwise an empty Optional.
     * @throws IllegalArgumentException if {@code position} is negative
     * @see #first()
     * @see #last()
     */
    @Beta
    @SequentialOnly
    @TerminalOp
    OT elementAt(long position);

    /**
     * Returns an {@code Optional} containing the only element of this stream if it contains exactly one element, or an empty {@code Optional} if the stream is empty.
     * If the stream contains more than one element, an exception is thrown.
     *
     * <br />
     * This is a terminal operation and will close the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Stream with exactly one element
     * Optional<String> single = Stream.of("only").onlyOne();
     * // single.get() returns "only"
     *
     * // Empty stream returns empty Optional
     * Optional<Integer> empty = Stream.<Integer>empty().onlyOne();
     * // empty.isEmpty() is true
     *
     * // Stream with multiple elements throws exception
     * try {
     *     Stream.of("a", "b", "c").onlyOne();   // throws TooManyElementsException
     * } catch (TooManyElementsException e) {
     *     // Handle error
     * }
     *
     * // Useful for validation when expecting single result
     * Optional<User> user = Stream.of(users)
     *       .filter(u -> u.getId().equals(targetId))
     *       .onlyOne();   // gets at most one user with that ID
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an {@code Optional} containing the only element of this stream, or an empty {@code Optional} if the stream is empty
     * @throws TooManyElementsException if the stream contains more than one element
     * @see #first()
     * @see #last()
     */
    @SequentialOnly
    @TerminalOp
    OT onlyOne() throws TooManyElementsException;

    /**
     * Collects the elements of this stream into an array.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     * For primitive streams (IntStream, LongStream, etc.), this returns a primitive array.
     * For object streams ({@link Stream}), this returns an {@code Object[]} array.
     * Use {@code Stream#toArray(IntFunction)} to get a typed array for object streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = IntStream.of(1, 2, 3, 4, 5).toArray();
     * // numbers = [1, 2, 3, 4, 5]
     *
     * Object[] objs = Stream.of("a", "ab", "abc")
     *     .filter(s -> s.length() > 1)
     *     .toArray();
     * // objs = ["ab", "abc"]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an array containing the elements of this stream; the concrete type depends on the stream type
     *         (e.g., {@code int[]} for {@link IntStream}, {@code Object[]} for {@link Stream})
     * @see #toList()
     * @see #toSet()
     */
    @SequentialOnly
    @TerminalOp
    A toArray();

    /**
     * Collects the elements of this stream into a modifiable List.
     * Unlike {@code java.util.stream.Stream#toList()}, this method returns a modifiable list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect stream to modifiable list
     * List<String> list = Stream.of("a", "b", "c").toList();
     * list.add("d");   // list is modifiable, so this succeeds
     * // list is now ["a", "b", "c", "d"]
     *
     * // Collect filtered results
     * List<Integer> evenNumbers = Stream.of(1, 2, 3, 4, 5, 6)
     *       .filter(n -> n % 2 == 0)
     *       .toList();
     * // evenNumbers = [2, 4, 6]
     *
     * // Empty stream returns empty list
     * List<String> empty = Stream.<String>empty().toList();
     * // empty.isEmpty() is true
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a modifiable List containing the elements of this stream.
     * @see #toSet()
     * @see #toImmutableList()
     * @see #toArray()
     */
    @SequentialOnly
    @TerminalOp
    List<T> toList();

    /**
     * Collects the elements of this stream into a modifiable Set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect stream to set (removes duplicates)
     * Set<String> set = Stream.of("a", "b", "c", "a", "b").toSet();
     * // set contains ["a", "b", "c"] - duplicates removed
     *
     * // Collect unique filtered results
     * Set<Integer> uniqueEvens = Stream.of(2, 2, 4, 4, 6, 6)
     *       .filter(n -> n % 2 == 0)
     *       .toSet();
     * // uniqueEvens = [2, 4, 6]
     *
     * // Check for unique values
     * Set<String> unique = Stream.of(data).toSet();
     * boolean containsDuplicates = unique.size() != data.size();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a modifiable Set containing the unique elements of this stream.
     * @see #toList()
     * @see #toImmutableSet()
     * @see #distinct()
     */
    @SequentialOnly
    @TerminalOp
    Set<T> toSet();

    /**
     * Collects the elements of this stream into an ImmutableList.
     * The returned list is immutable and cannot be modified after creation.
     * Attempting to modify the list will result in an UnsupportedOperationException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect stream elements into an immutable list
     * ImmutableList<String> list = Stream.of("a", "b", "c")
     *       .toImmutableList();
     * // list = ["a", "b", "c"]
     *
     * // Attempting to modify throws UnsupportedOperationException
     * try {
     *     list.add("d");   // throws UnsupportedOperationException
     * } catch (UnsupportedOperationException e) {
     *     // Expected - list is immutable
     * }
     *
     * // Safe to share across threads
     * ImmutableList<Integer> numbers = Stream.of(1, 2, 3, 4, 5)
     *       .filter(n -> n % 2 == 0)
     *       .toImmutableList();
     * // numbers = [2, 4]
     *
     * // Empty stream returns empty immutable list
     * ImmutableList<String> empty = Stream.<String>empty()
     *       .toImmutableList();
     * // empty.isEmpty() is true, empty is immutable
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an ImmutableList containing the elements of this stream.
     * @see #toList()
     */
    @SequentialOnly
    @TerminalOp
    ImmutableList<T> toImmutableList();

    /**
     * Collects the elements of this stream into an ImmutableSet.
     * The returned set is immutable and cannot be modified after creation.
     * Duplicate elements are automatically removed, maintaining only unique values.
     * Attempting to modify the set will result in an UnsupportedOperationException.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect unique elements into an immutable set
     * ImmutableSet<String> set = Stream.of("a", "b", "c", "b")
     *       .toImmutableSet();
     * // set = ["a", "b", "c"] - duplicates automatically removed
     *
     * // Attempting to modify throws UnsupportedOperationException
     * try {
     *     set.add("d");   // throws UnsupportedOperationException
     * } catch (UnsupportedOperationException e) {
     *     // Expected - set is immutable
     * }
     *
     * // Deduplication with filtering
     * ImmutableSet<Integer> uniqueEvens = Stream.of(2, 4, 2, 6, 4, 8)
     *       .filter(n -> n % 2 == 0)
     *       .toImmutableSet();
     * // uniqueEvens = [2, 4, 6, 8] - unique even numbers only
     *
     * // Safe to share immutable unique collection
     * ImmutableSet<String> tags = Stream.of("java", "stream", "java", "api")
     *       .toImmutableSet();
     * // tags = ["java", "stream", "api"] - unique and immutable
     *
     * // Empty stream returns empty immutable set
     * ImmutableSet<Integer> empty = Stream.<Integer>empty()
     *       .toImmutableSet();
     * // empty.isEmpty() is true, empty is immutable
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an ImmutableSet containing the unique elements of this stream.
     * @see #toSet()
     */
    @SequentialOnly
    @TerminalOp
    ImmutableSet<T> toImmutableSet();

    /**
     * Collects the elements of this stream into a Collection.
     * The type of Collection is determined by the provided supplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to LinkedList
     * LinkedList<String> linkedList = Stream.of("a", "b", "c")
     *       .toCollection(LinkedList::new);
     *
     * // Collect to TreeSet (sorted set)
     * TreeSet<Integer> treeSet = Stream.of(3, 1, 4, 1, 5, 9)
     *       .toCollection(TreeSet::new);
     * // treeSet contains [1, 3, 4, 5, 9] - sorted and unique
     *
     * // Collect to specific collection type
     * ArrayDeque<String> deque = Stream.of("first", "second", "third")
     *       .toCollection(ArrayDeque::new);
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <CC> The type of Collection to create.
     * @param supplier a supplier function that provides a new, empty Collection of the desired type.
     * @return a Collection of the desired type containing the elements of this stream.
     * @see #toList()
     * @see #toSet()
     */
    @SequentialOnly
    @TerminalOp
    <CC extends Collection<T>> CC toCollection(Supplier<? extends CC> supplier);

    /**
     * Collects the elements of this stream into a Multiset.
     * A Multiset is a collection that tracks the count of each element, allowing duplicates.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count occurrences of each element
     * Multiset<String> counts = Stream.of("a", "b", "a", "c", "b", "a").toMultiset();
     * // counts.getCount("a") returns 3
     * // counts.getCount("b") returns 2
     * // counts.getCount("c") returns 1
     *
     * // Count word frequencies
     * Multiset<String> wordFreq = Stream.of(text.split("\\s+")).toMultiset();
     * // wordFreq tracks how many times each word appears
     *
     * // Find most common elements
     * Multiset<Integer> numbers = Stream.of(1, 2, 2, 3, 3, 3).toMultiset();
     * // numbers.getCount(3) returns 3 - most frequent
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a Multiset containing the elements of this stream with their occurrence counts.
     * @see #toMultiset(Supplier)
     * @see Multiset
     */
    @SequentialOnly
    @TerminalOp
    Multiset<T> toMultiset();

    /**
     * Collects the elements of this stream into a Multiset.
     * The type of Multiset is determined by the provided supplier.
     * A Multiset is a collection that supports counting occurrences of elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into a custom Multiset implementation
     * Multiset<String> wordCounts = Stream.of("a", "b", "a", "c", "b", "a")
     *       .toMultiset(Multiset::new);
     * // wordCounts: {a=3, b=2, c=1}
     *
     * // Count occurrences with a specific supplier
     * Multiset<Integer> numbers = Stream.of(1, 2, 1, 3, 2, 1, 4)
     *       .toMultiset(() -> new Multiset<>());
     * // numbers: {1=3, 2=2, 3=1, 4=1}
     *
     * // Count duplicate values after transformation
     * Multiset<Integer> lengths = Stream.of("a", "bb", "ccc", "bb", "a")
     *       .map(String::length)
     *       .toMultiset(Multiset::new);
     * // lengths: {1=2, 2=2, 3=1}
     *
     * // Use with filtering to count specific elements
     * Multiset<String> filteredCounts = Stream.of("apple", "Banana", "apple", "cherry", "banana")
     *       .map(String::toLowerCase)
     *       .filter(s -> s.startsWith("a") || s.startsWith("b"))
     *       .toMultiset(Multiset::new);
     * // filteredCounts: {apple=2, banana=2}
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>Yes</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param supplier a supplier function that provides a new, empty Multiset of the desired type.
     * @return a Multiset of the desired type containing the elements of this stream.
     * @see #toMultiset()
     * @see Multiset
     */
    @SequentialOnly
    @TerminalOp
    Multiset<T> toMultiset(Supplier<? extends Multiset<T>> supplier); //NOSONAR

    /**
     * Prints at most the first thousand elements of this stream to the standard output in a formatted manner.
     * The output format is {@code [element1, element2, element3, ...]}.
     * If there are more than a thousand elements, only the first thousand will be printed followed by an ellipsis (...).
     * If this stream is empty, it will print {@code []}.
     *
     * <p>This is a terminal operation that will close the stream after execution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream.of("apple", "banana", "cherry").println();
     * // Output: [apple, banana, cherry]
     *
     * IntStream.range(1, 5).boxed().println();
     * // Output: [1, 2, 3, 4]
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @see #join(CharSequence, CharSequence, CharSequence)
     * @see System#out
     */
    @Beta
    @SequentialOnly
    @TerminalOp
    void println();

    /**
     * Returns an iterator for the elements of this stream.
     *
     * <p><b>Warning:</b> Remember to close this Stream after the iteration is done, if needed.
     * This method does not automatically close the stream, which may lead to resource leaks.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Using try-finally (not recommended)
     * Stream<String> stream = Stream.of("a", "b", "c");
     * try {
     *     Iterator<String> iter = stream.iterator();
     *     while (iter.hasNext()) {
     *         System.out.println(iter.next());
     *     }
     * } finally {
     *     stream.close();   // closes the stream explicitly
     * }
     *
     * // Using try-with-resources (better approach if you must use iterator)
     * try (Stream<String> stream = Stream.of("a", "b", "c")) {
     *     Iterator<String> iter = stream.iterator();
     *     while (iter.hasNext()) {
     *         process(iter.next());
     *     }
     * } // closes the stream automatically
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an iterator for the elements of this stream.
     * @deprecated advisory only and by design — this method is fully supported and is NOT slated for removal.
     *             The returned iterator does not auto-close the stream, so it may leak resources if you forget
     *             to close it. Use the try-with-resources form shown above, or prefer an eager terminal such as
     *             {@link #toList()} when lazy pull semantics are not required. It is intentionally kept
     *             {@code @Deprecated} to nudge callers toward auto-closing usage.
     * @see #toList()
     * @see #close()
     */
    @Deprecated
    @SequentialOnly
    ITER iterator();

    /**
     * Checks if the stream is running in parallel mode.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check default sequential stream
     * Stream<Integer> seq = Stream.of(1, 2, 3);
     * boolean isParallel = seq.isParallel();
     * // isParallel is false
     *
     * // Check parallel stream
     * Stream<Integer> par = Stream.of(1, 2, 3).parallel();
     * boolean isParallelNow = par.isParallel();
     * // isParallelNow is true
     *
     * // Conditionally apply parallel processing
     * Stream<String> stream = Stream.of(data);
     * if (data.size() > 10000) {
     *     stream = stream.parallel(8);
     * }
     * // Later check if parallelized
     * if (stream.isParallel()) {
     *     System.out.println("Processing in parallel mode");
     * }
     * }</pre>
     *
     * @return {@code true} if the stream is running in parallel mode, {@code false} otherwise.
     * @see #sequential()
     * @see #parallel()
     */
    boolean isParallel();

    /**
     * Switches the stream to sequential mode.
     * In sequential mode, the operations on the stream are performed in the order of the elements in the stream.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Switch parallel stream back to sequential
     * Stream.of(1, 2, 3, 4, 5)
     *       .parallel(8)              // processes in parallel
     *       .map(expensiveOperation)  // uses CPU-intensive operation
     *       .sequential()             // switches back to sequential
     *       .filter(quickFilter)      // sequential is faster for simple ops
     *       .toList();
     *
     * // Ensure sequential processing for stateful operations
     * Stream.of(data)
     *       .parallel()
     *       .map(transform)
     *       .sequential()  // maintains order for sorted()
     *       .sorted()
     *       .toList();
     *
     * // Default streams are already sequential
     * Stream<String> stream = Stream.of("a", "b", "c");
     * // stream.isParallel() is false
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream that is identical to this stream, but in sequential mode.
     * @see #parallel()
     * @see #isParallel()
     */
    @SequentialOnly
    @IntermediateOp
    S sequential();

    /**
     * Switches the stream to parallel mode.
     * This is an intermediate operation.
     *
     * <br />
     * Consider using {@code sps(int, Function)} if only the next operation needs to be parallelized. For example:
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * stream.parallel(maxThreadNum).map(f).filter(p)...;
     *
     * // Replace above line of code with "sps" if only "f" needs to be parallelized, and "p" is fast enough to be executed in sequential Stream.
     * stream.sps(maxThreadNum, s -> s.map(f)).filter(p)...;
     * // Or switch the stream back to sequential stream by sequential().
     * stream.parallel(maxThreadNum).map(f).sequential().filter(p)...;
     * }</pre>
     *
     * When to use parallel Streams?
     * <ul>
     * <li>First, do NOT and should NOT use parallel Streams if you don't have any problem with sequential Streams, because using parallel Streams has extra cost.</li>
     * <li>Consider using parallel Streams only when <a href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">N(the number of elements) * Q(cost per element of F, the per-element function (usually a lambda)) is big enough(e.g., IO involved. Network: DB/web service request..., Reading/Writing file...)</a>.</li>
     * <li>It's easy to test out the differences of performance by sequential Streams and parallel Streams with <a href="http://www.landawn.com/api-docs/com/landawn/abacus/util/Profiler.html">Profiler</a>.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Profiler.run(1, 1, 3, "sequential", () -> Stream.of(list).operation(F)...).printResult();
     * Profiler.run(1, 1, 3, "parallel", () -> Stream.of(list).parallel().operation(F)...).printResult();
     * }</pre>
     *
     * Here is a sample performance test with computer: CPU Intel i7-3520M 4-cores 2.9 GHz, JDK 1.8.0_101, Windows 7:
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     *      public void test_perf() {
     *         final String[] strs = new String[10_000];
     *         N.fill(strs, Strings.uuid());
     *
     *         final int m = 10;
     *         final Function<String, Long> mapper = str -> {
     *             long result = 0;
     *             for (int i = 0; i < m; i++) {
     *                 result += N.sum(str.toCharArray()) + 1;
     *             }
     *             return result;
     *         };
     *
     *         final MutableLong sum = MutableLong.of(0);
     *
     *         for (int i = 0, len = strs.length; i < len; i++) {
     *             sum.add(mapper.apply(strs[i]));
     *         }
     *
     *         final int threadNum = 1, loopNum = 100, roundNum = 3;
     *
     *         Profiler.run(threadNum, loopNum, roundNum, "For Loop", () -> {
     *             long result = 0;
     *             for (int i = 0, len = strs.length; i < len; i++) {
     *                 result += mapper.apply(strs[i]);
     *             }
     *             assertEquals(sum.longValue(), result);
     *         }).printResult();
     *
     *         Profiler.run(threadNum, loopNum, roundNum, "JDK Sequential",
     *                 () -> assertEquals(sum.longValue(), java.util.stream.Stream.of(strs).map(mapper).mapToLong(e -> e).sum())).printResult();
     *
     *         Profiler.run(threadNum, loopNum, roundNum, "JDK Parallel",
     *                 () -> assertEquals(sum.longValue(), java.util.stream.Stream.of(strs).parallel().map(mapper).mapToLong(e -> e).sum())).printResult();
     *
     *         Profiler.run(threadNum, loopNum, roundNum, "Abacus Sequential", () -> assertEquals(sum.longValue(), Stream.of(strs).map(mapper).mapToLong(e -> e).sum()))
     *                 .printResult();
     *
     *         Profiler.run(threadNum, loopNum, roundNum, "Abacus Parallel",
     *                 () -> assertEquals(sum.longValue(), Stream.of(strs).parallel().map(mapper).mapToLong(e -> e).sum())).printResult();
     *
     *         Profiler.run(threadNum, loopNum, roundNum, "Abacus Parallel by chunk", () -> assertEquals(sum.longValue(),
     *                 Stream.of(strs).split(100).parallel().map(it -> N.sumLong(it, e -> mapper.apply(e))).mapToLong(e -> e).sum())).printResult();
     *      }
     * }</pre>
     *
     * <b>And test result</b>: <i>Unit is milliseconds. N(the number of elements) is 10_000, Q(cost per element of F, the per-element function (usually a lambda), here is {@code mapper}) is calculated by: value of 'For loop' / N(10_000).</i>
     * <table>
     * <caption>Performance Benchmark Results (milliseconds)</caption>
     * <tr><th></th><th>  m = 1  </th><th>m = 10</th><th>m = 50</th><th>m = 100</th><th>m = 500</th><th>m = 1000</th></tr>
     * <tr><td>   Q   </td><td>0.00002</td><td>0.0002</td><td>0.001</td><td>0.002</td><td>0.01</td><td>0.02</td></tr>
     * <tr><td>For Loop</td><td>0.23</td><td>2.3</td><td>11</td><td>22</td><td>110</td><td>219</td></tr>
     * <tr><td>JDK Sequential</td><td>0.28</td><td>2.3</td><td>11</td><td>22</td><td>114</td><td>212</td></tr>
     * <tr><td>JDK Parallel</td><td>0.22</td><td>1.3</td><td>6</td><td>12</td><td>66</td><td>122</td></tr>
     * <tr><td>Abacus Sequential</td><td>0.3</td><td>2</td><td>11</td><td>22</td><td>112</td><td>212</td></tr>
     * <tr><td>Abacus Parallel</td><td>11</td><td>11</td><td>11</td><td>16</td><td>77</td><td>128</td></tr>
     * </table>
     *
     * <b>Comparison:</b>
     * <ul>
     * <li>Again, do NOT and should NOT use parallel Streams if you don't have any performance problem with sequential Streams, because using parallel Streams has extra cost.</li>
     * <li>Again, consider using parallel Streams only when <a href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">N(the number of elements) * Q(cost per element of F, the per-element function (usually a lambda)) is big enough</a>.</li>
     * <li>The implementation of parallel Streams in abacus-core is more than 10 times slower than parallel Streams in JDK when Q is tiny (here is less than 0.0002 milliseconds by the test):
     *      <ul>
     *      <li>The implementation of parallel Streams in JDK still can beat the sequential/for loop when Q is tiny (Here is 0.00002 milliseconds by the test).
     *      That's amazing, considering the extra cost brought by parallel computation. It's well done.</li>
     *      <li>The implementation of parallel Streams in abacus-core is pretty simple and straightforward.
     *      The extra cost (starting threads/synchronization/queue...) brought by parallel Streams in abacus-core is too big for tiny Q (Here is less than 0.001 milliseconds by the test).
     *      But it starts to be faster than sequential Streams when Q is big enough (Here is 0.001 milliseconds by the test) and starts to catch the parallel Streams in JDK when Q is bigger (Here is 0.01 milliseconds by the test).</li>
     *      <li>Consider using the parallel Streams in abacus-core when Q is big enough, especially when IO is involved in F.
     *      Because one IO operation(e.g., DB/web service request..., Reading/Writing file...) usually takes 1 to 1000 milliseconds, or even longer.
     *      By the parallel Streams APIs in Abacus, it's very simple to specify max thread numbers. Sometimes, it's much faster to execute IO/Network requests with a bit more threads.
     *      It's fair to say that the parallel Streams in abacus-core are highly efficient, may be as fast as or faster than the parallel Streams in JDK when Q is big enough, except when F is a CPU-intensive operation.
     *      Usually, the reason Q is big enough to consider using a parallel Stream is because IO/Network is involved in F.</li>
     *      </ul>
     * </li>
     * <li>All primitive types are supported by Stream APIs in abacus-core except boolean</li>
     * </ul>
     *
     * <br />
     * A bit more about Lambdas/Stream APIs, you may hear that Lambdas/Stream APIs are <a href="http://blog.takipi.com/benchmark-how-java-8-lambdas-and-streams-can-make-your-code-5-times-slower/">5 times slower than imperative programming</a>.
     * It's {@code true} when Q and F are VERY, VERY tiny, like <code>f = (int a, int b) -> a + b;</code>.
     * But if we look into the samples in the article and think about it, it just takes less than 1 millisecond to get the max value in 100k numbers.
     * There is a potential performance issue only if the "get the max value in 100K numbers" is called many, many times in your API or single request.
     * Otherwise, the difference between 0.1 milliseconds to 0.5 milliseconds can be totally ignored.
     * Usually we meet performance issue only if Q and F are big enough. However, the performance of Lambdas/Streams APIs is close to for loop when Q and F are big enough.
     * No matter in which scenario, we don't need to and should not be concerned about the performance of Lambdas/Stream APIs.
     *
     * <br />
     * Although it's in parallel Streams, it doesn't mean all the methods are executed in parallel.
     * Because the sequential way is as fast, or even faster than the parallel way for some methods, or is pretty challenging, if not possible, to implement the method by parallel approach.
     * Methods annotated with {@code @SequentialOnly} are executed in sequential way even in parallel Streams. For example:
     * <br />
     * <i>splitXXX/splitAt/splitBy/slidingXXX/collapse, distinct, reverse, rotate, shuffle, indexed, cached, top, kthLargest, count, toArray, toList, toSet, toMultiset...</i>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return a new stream that is identical to this stream but configured for parallel execution
     *         using the default thread count ({@code min(cpu_cores, 64)}, i.e. the CPU core count capped by the maximum allowed thread number per operation)
     * @see #sps(Function)
     * @see #sps(int, Function)
     * @see #parallel(int)
     * @see #parallel(Executor)
     * @see #parallel(int, Executor)
     * @see com.landawn.abacus.util.Profiler#run(int, int, int, com.landawn.abacus.util.Throwables.Runnable)
     * @see <a href="https://www.infoq.com/presentations/parallel-java-se-8#downloadPdf">Understanding Parallel Stream Performance in Java SE 8</a>
     * @see <a href="https://www.baeldung.com/java-when-to-use-parallel-stream">When to Use a Parallel Stream in Java</a>
     * @see <a href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel Streams</a>
     */
    @SequentialOnly
    @IntermediateOp
    S parallel();

    /**
     * Switches the stream to parallel mode with a specified maximum thread number.
     * This is an intermediate operation that enables parallel processing with control over thread count.
     *
     * <p>Consider using {@code sps(int, Function)} if only the next operation needs to be parallelized. For example:
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * stream.parallel(maxThreadNum).map(f).filter(p)...;
     *
     * // Replace above line of code with "sps" if only "f" needs to be parallelized, and "p" is fast enough to be executed in sequential Stream.
     * stream.sps(maxThreadNum, s -> s.map(f)).filter(p)...;
     * // Or switch the stream back to sequential stream by sequential().
     * stream.parallel(maxThreadNum).map(f).sequential().filter(p)...;
     * }</pre>
     *
     * <p>The actual number of threads used may be limited by the system. If the specified value is bigger than
     * the maximum allowed thread number per operation ({@code min(64, cpu_cores * 8)}), the maximum allowed
     * thread number per operation will be used. To parallelize this Stream with thread number bigger than
     * {@code min(64, cpu_cores * 8)}, please specify {@code executor} by calling {@link #parallel(int, Executor)}
     * or {@link #parallel(ParallelSettings)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process with up to 4 threads
     * Stream.of(data)
     *       .parallel(4)
     *       .map(expensiveOperation)
     *       .toList();
     *
     * // Process IO operations with more threads
     * Stream.of(urls)
     *       .parallel(20)
     *       .map(url -> fetchData(url))
     *       .toList();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param maxThreadNum the maximum number of threads to use for parallel operations. If the specified value is
     *        bigger than the maximum allowed thread number per operation ({@code min(64, cpu_cores * 8)}),
     *        maximum allowed thread number per operation will be used.
     * @return a new stream configured for parallel execution with the specified maximum thread count
     * @see #sps(Function)
     * @see #sps(int, Function)
     * @see #parallel()
     * @see #parallel(Executor)
     * @see #parallel(int, Executor)
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(int maxThreadNum);

    /**
     * Switches the stream to parallel mode with a specified Executor.
     * This is an intermediate operation that enables parallel processing using a custom thread pool.
     *
     * <p>Consider using {@code sps(int, Function)} if only the next operation needs to be parallelized. For example:
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * stream.parallel(executor).map(f).filter(p)...;
     *
     * // Replace above line of code with "sps" if only "f" needs to be parallelized, and "p" is fast enough to be executed in sequential Stream.
     * stream.sps(maxThreadNum, s -> s.map(f)).filter(p)...;
     * // Or switch the stream back to sequential stream by sequential().
     * stream.parallel(executor).map(f).sequential().filter(p)...;
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use a custom thread pool
     * ExecutorService customExecutor = Executors.newFixedThreadPool(10);
     * Stream.of(data)
     *       .parallel(customExecutor)
     *       .map(expensiveOperation)
     *       .toList();
     *
     * // Use a cached thread pool for IO operations
     * ExecutorService ioExecutor = Executors.newCachedThreadPool();
     * Stream.of(files)
     *       .parallel(ioExecutor)
     *       .map(file -> readFile(file))
     *       .toList();
     * }</pre>
     *
     * <p><b>&#9888;&#65039;</b> The call could hang if this executor is created by
     * {@code Executors.newVirtualThreadPerTaskExecutor()} and used by multiple calls in this stream.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param executor the Executor to use for parallel operations.
     *          {@code Executor} can be specified for bigger thread number than the maximum allowed thread number per operation ({@code min(64, cpu_cores * 8)}) or virtual thread.
     * @return a new stream configured for parallel execution with the specified executor
     * @see #sps(Function)
     * @see #sps(int, Function)
     * @see #parallel()
     * @see #parallel(int)
     * @see #parallel(int, Executor)
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(Executor executor);

    /**
     * Switches the stream to parallel mode with specified maximum thread number and Executor.
     * This is an intermediate operation that enables parallel processing with control over both thread count and thread pool.
     *
     * <p>Consider using {@code sps(int, Function)} if only the next operation needs to be parallelized. For example:
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * stream.parallel(maxThreadNum, executor).map(f).filter(p)...;
     *
     * // Replace above line of code with "sps" if only "f" needs to be parallelized, and "p" is fast enough to be executed in sequential Stream.
     * stream.sps(maxThreadNum, s -> s.map(f)).filter(p)...;
     * // Or switch the stream back to sequential stream by sequential().
     * stream.parallel(maxThreadNum, executor).map(f).sequential().filter(p)...;
     * }</pre>
     *
     * <p>If the specified maxThreadNum is bigger than the maximum allowed thread number per operation
     * ({@code min(64, cpu_cores * 8)}) and {@code executor} is not specified with a {@code non-null} value,
     * maximum allowed thread number per operation will be used. To parallelize this Stream with thread number
     * bigger than {@code min(64, cpu_cores * 8)}, please specify {@code executor} with a {@code non-null} value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use a custom executor with limited parallelism
     * ExecutorService executor = Executors.newFixedThreadPool(20);
     * Stream.of(data)
     *       .parallel(10, executor)  // uses at most 10 threads from the pool
     *       .map(expensiveOperation)
     *       .toList();
     * }</pre>
     *
     * <p><b>&#9888;&#65039;</b> The call could hang if this executor is created by
     * {@code Executors.newVirtualThreadPerTaskExecutor()} and used by multiple calls in this stream.
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param maxThreadNum the maximum number of threads to use for parallel operations. If the specified value
     *        is bigger than the maximum allowed thread number per operation ({@code min(64, cpu_cores * 8)})
     *        and {@code executor} is not specified with a {@code non-null} value, maximum allowed thread number
     *        per operation will be used.
     * @param executor the Executor to use for parallel operations.
     *          {@code Executor} can be specified for bigger thread number than the maximum allowed thread number per operation ({@code min(64, cpu_cores * 8)}) or virtual thread.
     * @return a new stream configured for parallel execution with the specified parameters
     * @see #sps(Function)
     * @see #sps(int, Function)
     * @see #parallel()
     * @see #parallel(int)
     * @see #parallel(Executor)
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(int maxThreadNum, Executor executor);

    /**
     * Switches the stream to parallel mode with the specified parallel settings.
     * This is an intermediate operation that enables parallel processing with fine-grained control over parallelization parameters.
     *
     * <p>Consider using {@code sps(int, Function)} if only the next operation needs to be parallelized. For example:
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * stream.parallel(ps).map(f).filter(p)...;
     *
     * // Replace above line of code with "sps" if only "f" needs to be parallelized, and "p" is fast enough to be executed in sequential Stream.
     * stream.sps(maxThreadNum, s -> s.map(f)).filter(p)...;
     * // Or switch the stream back to sequential stream by sequential().
     * stream.parallel(ps).map(f).sequential().filter(p)...;
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Configure parallel execution with specific settings
     * ParallelSettings ps = ParallelSettings.PS.create(10)
     *                                          .executor(customExecutor);
     * Stream.of(data)
     *       .parallel(ps)
     *       .map(expensiveOperation)
     *       .toList();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param ps the ParallelSettings object containing configuration for parallel execution,
     *        including maximum thread number, splitor strategy, and executor
     * @return a new stream configured for parallel execution with the specified settings
     * @see #sps(Function)
     * @see #sps(int, Function)
     * @see #parallel()
     * @see #parallel(int)
     * @see #parallel(Executor)
     * @see #parallel(int, Executor)
     * @see ParallelSettings
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    S parallel(ParallelSettings ps);

    /**
     * Temporarily switches the stream to parallel mode for the specified operation and then switches back to sequential mode.
     * This is useful when you want to parallelize only a specific operation in a stream pipeline.
     *
     * <p>This method is equivalent to: {@code stream.parallel().ops(map/filter/...).sequential()}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Parallelize only the expensive map operation
     * Stream.of(data)
     *       .filter(quickFilter)
     *       .sps(s -> s.map(expensiveOperation))  // processes only this part in parallel
     *       .filter(anotherQuickFilter)           // switches back to sequential
     *       .toList();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <SS> the type of the stream returned by the operation
     * @param ops the function that defines the operations to be performed in parallel mode
     * @return a new stream with the operations applied in parallel mode, then switched back to sequential
     * @see #sps(int, Function)
     * @see #sps(int, Executor, Function)
     * @see #parallel()
     * @see #parallel(Executor)
     * @see #parallel(int, Executor)
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    <SS extends BaseStream> SS sps(Function<? super S, ? extends SS> ops);

    /**
     * Temporarily switches the stream to parallel mode with specified thread count for the specified operation
     * and then switches back to sequential mode.
     * This is useful when you want to parallelize only a specific operation with controlled parallelism.
     *
     * <p>This method is equivalent to: {@code stream.parallel(maxThreadNum).ops(map/filter/...).sequential()}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Parallelize only the expensive map operation with 8 threads
     * Stream.of(data)
     *       .filter(quickFilter)
     *       .sps(8, s -> s.map(expensiveOperation))  // processes only this part in parallel with max 8 threads
     *       .filter(anotherQuickFilter)              // switches back to sequential
     *       .toList();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <SS> the type of the stream returned by the operation
     * @param maxThreadNum the maximum number of threads to use for the parallel operation
     * @param ops the function that defines the operations to be performed in parallel mode
     * @return a new stream with the operations applied in parallel mode, then switched back to sequential
     * @see #sps(Function)
     * @see #sps(int, Executor, Function)
     * @see #parallel()
     * @see #parallel(Executor)
     * @see #parallel(int, Executor)
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    <SS extends BaseStream> SS sps(int maxThreadNum, Function<? super S, ? extends SS> ops);

    /**
     * Temporarily switches the stream to parallel mode with specified thread count and executor for the specified
     * operation and then switches back to sequential mode.
     * This provides maximum control over the parallel execution of a specific operation.
     *
     * <p>This method is equivalent to: {@code stream.parallel(maxThreadNum, executor).ops(map/filter/...).sequential()}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Parallelize only the IO operation with custom executor
     * ExecutorService ioExecutor = Executors.newCachedThreadPool();
     * Stream.of(urls)
     *       .filter(url -> url.startsWith("https"))
     *       .sps(20, ioExecutor, s -> s.map(url -> fetchData(url)))  // processes only this part in parallel
     *       .map(data -> parseData(data))                            // switches back to sequential
     *       .toList();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <SS> the type of the stream returned by the operation
     * @param maxThreadNum the maximum number of threads to use for the parallel operation
     * @param executor the executor to use for the parallel operation
     * @param ops the function that defines the operations to be performed in parallel mode
     * @return a new stream with the operations applied in parallel mode, then switched back to sequential
     * @see #sps(Function)
     * @see #sps(int, Function)
     * @see #parallel()
     * @see #parallel(Executor)
     * @see #parallel(int, Executor)
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    <SS extends BaseStream> SS sps(int maxThreadNum, Executor executor, Function<? super S, ? extends SS> ops);

    /**
     * Temporarily switches the stream to sequential mode for the specified operation and then switches back to
     * parallel mode with the same configuration (maxThreadNum/splitor/executor).
     * This is useful when you have a parallel stream but want to execute a specific operation sequentially.
     *
     * <p>This method is equivalent to: {@code stream.sequential().ops(map/filter/...).parallel(sameMaxThreadNum, sameSplitor, sameExecutor)}
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // In a parallel stream, execute a stateful operation sequentially
     * Stream.of(data)
     *       .parallel(8)
     *       .map(expensiveOperation)     // processes in parallel
     *       .psp(s -> s.sorted())        // sorts sequentially
     *       .map(anotherOperation)       // switches back to parallel with same settings
     *       .toList();
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <SS> the type of the stream returned by the operation
     * @param ops the function that defines the operations to be performed in sequential mode
     * @return a new stream with the operations applied in sequential mode, then switched back to parallel
     *         with the same parallel configuration
     * @see #parallel()
     * @see #parallel(Executor)
     * @see #parallel(int, Executor)
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    <SS extends BaseStream> SS psp(Function<? super S, ? extends SS> ops);

    /**
     * Transforms the current stream into a new stream using the provided transformation function.
     * This method is useful for performing complex transformations that cannot be achieved with standard stream operations.
     *
     * <p>The transformation function receives the current stream and can apply any sequence of operations to it,
     * returning a potentially different type of stream. This provides maximum flexibility for stream manipulation.
     *
     * <p>To avoid eager loading by terminal operations invoked in the transfer function, use deferred execution:
     * <pre>{@code
     * stream.transform(s -> Stream.defer(() -> s.someTerminalOperation()));
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Transform to a different stream type
     * Stream<String> result = Stream.of(1, 2, 3, 4, 5)
     *     .transform(s -> s.filter(n -> n % 2 == 0)
     *                      .map(n -> "Even: " + n));
     * // result contains ["Even: 2", "Even: 4"]
     *
     * // Complex transformation with grouping
     * Stream<Map<Boolean, List<Integer>>> result = Stream.of(1, 2, 3, 4, 5)
     *     .transform(s -> Stream.of(s.collect(Collectors.partitioningBy(n -> n % 2 == 0))));
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <RS> the type of the new stream
     * @param transfer the transformation function that takes the current stream and returns a new stream
     * @return a new stream transformed by the provided function
     * @throws IllegalArgumentException if the provided function is {@code null}
     * @throws IllegalStateException if the stream has already been operated upon or closed
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    <RS extends BaseStream> RS transform(Function<? super S, ? extends RS> transfer); //NOSONAR

    //    /**
    //     * Transforms the current stream into a new stream using the provided transformation function.
    //     * This method allows for complex stream transformations that may change the stream type.
    //     *
    //     * <p><b>Usage Examples:</b></p>
    //     * <pre>{@code
    //     * // Transform stream with filtering and mapping
    //     * Stream<String> result = Stream.of(1, 2, 3, 4, 5)
    //     *     .__(s -> s.filter(n -> n % 2 == 0)
    //     *              .map(n -> "Even: " + n));
    //     * // result contains ["Even: 2", "Even: 4"]
    //     * }</pre>
    //     *
    //     * @param <RS> the type of the new stream
    //     * @param transfer the transformation function that takes the current stream and returns a new stream
    //     * @return a new stream transformed by the provided function
    //     * @throws IllegalArgumentException if the provided function is {@code null}
    //     * @throws IllegalStateException if the stream has already been operated upon or closed
    //     * @deprecated Use {@link #transform(Function)} instead
    //     * @see #transform(Function)
    //     */
    //    @Deprecated
    //    @Beta
    //    @SequentialOnly
    //    @IntermediateOp
    //    @SuppressWarnings("rawtypes")
    //    default <RS extends BaseStream> RS __(final Function<? super S, ? extends RS> transfer) { //NOSONAR
    //        return transform(transfer);
    //    }

    /**
     * Applies the provided function to the stream if it is not empty.
     * This is a terminal operation that allows conditional processing based on stream content.
     *
     * <p>If the stream contains at least one element, the function is applied to the entire stream.
     * If the stream is empty, an empty Optional is returned without invoking the function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average only if stream is not empty
     * Optional<Double> average = Stream.of(1, 2, 3, 4, 5)
     *     .applyIfNotEmpty(s -> s.mapToDouble(i -> i).average().orElse(0.0));
     * // Returns Optional.of(3.0)
     *
     * // Empty stream returns empty Optional
     * Optional<String> result = Stream.<String>empty()
     *     .applyIfNotEmpty(s -> s.collect(Collectors.joining(", ")));
     * // Returns Optional.empty()
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <R> the return type of the function
     * @param <E> the type of exception that the function can throw
     * @param func the function to be applied to the stream if it's not empty
     * @return an Optional containing the result of the function if the stream is not empty,
     *         or an empty Optional if the stream is empty
     * @throws E if the function throws an exception
     */
    @SequentialOnly
    @TerminalOp
    <R, E extends Exception> u.Optional<R> applyIfNotEmpty(Throwables.Function<? super S, ? extends R, E> func) throws E;

    /**
     * Applies the provided consumer to the stream if it is not empty.
     * This is a terminal operation that allows conditional actions based on stream content.
     *
     * <p>If the stream contains at least one element, the consumer is applied to the entire stream.
     * The returned OrElse object can be used to specify an alternative action if the stream is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process stream if not empty, otherwise perform alternative action
     * Stream.of(1, 2, 3)
     *     .acceptIfNotEmpty(s -> System.out.println("Sum: " + s.mapToInt(i -> i).sum()))
     *     .orElse(() -> System.out.println("Stream was empty"));
     * // Prints: Sum: 6
     *
     * // Empty stream triggers orElse action
     * Stream.<Integer>empty()
     *     .acceptIfNotEmpty(s -> System.out.println("Processing: " + s.toList()))
     *     .orElse(() -> System.out.println("No data to process"));
     * // Prints: No data to process
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the consumer can throw
     * @param action the consumer to be applied to the stream if it's not empty
     * @return an OrElse instance which can be used to perform further actions if the stream is empty
     * @throws E if the consumer throws an exception
     */
    @SequentialOnly
    @TerminalOp
    <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super S, E> action) throws E;

    /**
     * Registers a close handler to be invoked when the stream is closed.
     * This method can be called multiple times to register multiple handlers.
     * Handlers are invoked in the order they were added (first-added, first-invoked).
     *
     * <p>Close handlers are useful for cleaning up resources that were allocated for stream processing.
     * The handlers will be invoked when the stream is closed, either explicitly by calling {@link #close()}
     * or implicitly when a terminal operation completes.
     *
     * <p>Close handlers are typically used to release external resources like file handles, database
     * connections, or network sockets that were used during stream processing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Register cleanup actions
     * Stream.of(data)
     *     .onClose(() -> System.out.println("Releasing resources..."))
     *     .onClose(() -> releaseResources())
     *     .onClose(() -> System.out.println("Stream closed"))
     *     .map(expensiveOperation)
     *     .toList();
     * // All close handlers are invoked after toList() completes
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>No</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>Yes</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>No</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>Yes</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param closeHandler the Runnable to be invoked when the stream is closed
     * @return a stream with the close handler registered. This may be the same stream instance.
     * @throws IllegalStateException if the stream has already been operated upon or closed
     */
    @SequentialOnly
    @IntermediateOp
    S onClose(Runnable closeHandler);

    /**
     * Closes the stream, releasing any system resources associated with it.
     * If the stream is already closed, then invoking this method has no effect.
     *
     * <p>Streams are automatically closed when a terminal operation completes, so explicit closing
     * is typically not necessary. However, if a stream may be abandoned before a terminal operation
     * (e.g., due to an exception), it should be closed explicitly to ensure proper resource cleanup.
     *
     * <p>Close handlers are typically used to release external resources like file handles, database
     * connections, or network sockets that were used during stream processing.</p>
     *
     * <p>All registered close handlers (added via {@link #onClose(Runnable)}) are invoked when
     * this method is called, in the order they were registered.
     *
     * <p>As per the AutoCloseable contract, close methods are idempotent - calling close multiple
     * times has the same effect as calling it once.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Using try-with-resources for automatic closing
     * try (Stream<String> stream = Stream.of(data)) {
     *     stream.map(s -> s.toUpperCase())
     *           .filter(s -> s.length() > 5)
     *           .toList();
     * } // closes the stream automatically here
     *
     * // Manual closing when stream might not reach terminal operation
     * Stream<String> stream = Stream.of(data);
     * try {
     *     // Some operations that might throw exception
     * } finally {
     *     stream.close();   // closes the stream
     * }
     * }</pre>
     *
     */
    @Override
    void close();

    /**
     * Enum defining strategies for splitting stream elements among parallel threads.
     * This enum controls how work is distributed when a stream is processed in parallel.
     *
     * <p>The splitting strategy can significantly impact parallel performance depending on
     * the nature of the data and operations being performed. {@code ITERATOR} provides
     * fine-grained work distribution but with synchronization overhead, while {@code ARRAY}
     * can split work more efficiently but only works with array-backed streams.
     *
     * <p>If you need custom chunk sizes for parallel processing, consider using:
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * stream.split(chunkSize).parallel(...).map/filter/flatMap...(sliceList -> process(sliceList));
     * // Or
     * stream.split(chunkSize).sps(maxThreadNum, op)
     * }</pre>
     *
     * @see BaseStream#parallel(ParallelSettings)
     * @see Stream#sps(Function)
     * @see Stream#sps(int, Function)
     */
    enum Splitor {
        /**
         * Elements are fetched and processed by parallel threads one by one.
         * This strategy provides fine-grained work distribution but requires synchronization.
         *
         * <p>With this strategy, each thread obtains the next element by synchronizing
         * access to the iterator:
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * synchronized (elements) {
         *    if (elements.hasNext()) {
         *       next = elements.next();
         *    }
         * }
         * // Process the element...
         * }</pre>
         *
         * <p>This approach ensures good load balancing as threads that finish processing
         * their current element can immediately fetch the next one. However, the synchronization
         * overhead can become significant for very fast operations.
         *
         * <p>Best used when:
         * <ul>
         *   <li>Operations on each element take variable time</li>
         *   <li>Good load balancing is more important than minimal synchronization overhead</li>
         *   <li>The stream is not backed by an array</li>
         * </ul>
         *
         * @see Stream#sps(Function)
         * @see Stream#sps(int, Function)
         */
        ITERATOR,

        /**
         * Stream elements are split into equal chunks when backed by an array.
         * This strategy provides efficient work distribution with minimal synchronization overhead.
         *
         * <p>When the stream is array-backed, the array is divided into approximately equal
         * slices based on the number of threads:
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * int sliceSize = (toIndex - fromIndex) / threadNum +
         *                 ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);
         * }</pre>
         *
         * <p>Each thread processes its assigned slice independently without synchronization,
         * which can significantly improve performance for operations on large arrays.
         *
         * <p>If stream elements are not stored in an array, {@code ARRAY} behaves the same
         * as {@code ITERATOR}, falling back to synchronized element-by-element processing.
         *
         * <p>Best used when:
         * <ul>
         *   <li>The stream is backed by an array</li>
         *   <li>Operations on each element take similar time</li>
         *   <li>Minimizing synchronization overhead is important</li>
         *   <li>The array is large enough to benefit from splitting</li>
         * </ul>
         *
         * @see Stream#sps(Function)
         * @see Stream#sps(int, Function)
         */
        ARRAY
    }

    /**
     * Configuration class for parallel stream execution.
     * This class provides fine-grained control over how streams are parallelized.
     *
     * <p>Use the {@link PS} factory class to create instances:
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ParallelSettings settings = PS.create(8)
     *                               .executor(customExecutor);
     * }</pre>
     *
     * @see BaseStream#parallel(ParallelSettings)
     * @see PS
     */
    @Data
    @Accessors(fluent = true)
    final class ParallelSettings {
        private int maxThreadNum;
        //    /**
        //     * @deprecated Not supported yet.
        //     */
        //    @Deprecated
        //    private int executorNumForVirtualThread; // Not supported yet.

        /**
         * The work-splitting strategy (array-based vs iterator-based) used by parallel operations. This is an
         * internal performance-tuning hint that rarely changes results; it is retained for backward
         * compatibility but discouraged and may be removed in a future release. Configure only
         * {@code maxThreadNum} / {@code executor} instead.
         *
         * @deprecated by design — discouraged internal tuning hint; prefer {@code maxThreadNum}/{@code executor}.
         */
        @Deprecated
        private Splitor splitor;
        private Executor executor;
        // private boolean cancelUncompletedThreads = false;

        /**
         * Constructs a new ParallelSettings instance with default values.
         * Use the {@link PS} factory class methods to create properly configured instances.
         *
         * @see PS#create(int)
         * @see PS#create(Executor)
         */
        public ParallelSettings() {
            // Default constructor for Lombok @Data annotation
        }

        /**
         * Factory class for creating {@link ParallelSettings} instances.
         * Provides convenient static factory methods for common parallel configurations.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Create with just thread count
         * ParallelSettings ps1 = PS.create(8);
         *
         * // Create with executor
         * ParallelSettings ps2 = PS.create(executorService);
         *
         * // Create with all parameters
         * ParallelSettings ps3 = PS.create(8, Splitor.ARRAY, executorService);
         * }</pre>
         *
         */
        @Beta
        public static final class PS {
            private PS() {
                // utility class;
            }

            /**
             * Creates a ParallelSettings instance with the specified maximum thread number.
             *
             * @param maxThreadNum the maximum number of threads to use for parallel operations
             * @return a new ParallelSettings instance configured with the specified thread count
             */
            public static ParallelSettings create(final int maxThreadNum) {
                return new ParallelSettings().maxThreadNum(maxThreadNum);
            }

            /**
             * Creates a ParallelSettings instance with the specified splitor strategy.
             *
             * @param splitor the strategy for splitting work among parallel threads
             * @return a new ParallelSettings instance configured with the specified splitor
             * @deprecated by design — the {@code splitor} hint is a discouraged tuning knob; prefer
             *             {@link #create(int)} / {@link #create(Executor)}.
             */
            @Deprecated
            public static ParallelSettings create(final Splitor splitor) {
                return new ParallelSettings().splitor(splitor);
            }

            /**
             * Creates a ParallelSettings instance with the specified executor.
             *
             * @param executor the executor to use for parallel operations
             * @return a new ParallelSettings instance configured with the specified executor
             */
            public static ParallelSettings create(final Executor executor) {
                return new ParallelSettings().executor(executor);
            }

            /**
             * Creates a ParallelSettings instance with the specified maximum thread number and splitor strategy.
             *
             * @param maxThreadNum the maximum number of threads to use for parallel operations
             * @param splitor the strategy for splitting work among parallel threads
             * @return a new ParallelSettings instance configured with the specified parameters
             * @deprecated by design — the {@code splitor} hint is a discouraged tuning knob; prefer
             *             {@link #create(int)} / {@link #create(int, Executor)}.
             */
            @Deprecated
            public static ParallelSettings create(final int maxThreadNum, final Splitor splitor) {
                return new ParallelSettings().maxThreadNum(maxThreadNum).splitor(splitor);
            }

            /**
             * Creates a ParallelSettings instance with the specified maximum thread number and executor.
             *
             * @param maxThreadNum the maximum number of threads to use for parallel operations
             * @param executor the executor to use for parallel operations
             * @return a new ParallelSettings instance configured with the specified parameters
             */
            public static ParallelSettings create(final int maxThreadNum, final Executor executor) {
                return new ParallelSettings().maxThreadNum(maxThreadNum).executor(executor);
            }

            /**
             * Creates a ParallelSettings instance with the specified splitor strategy and executor.
             *
             * @param splitor the strategy for splitting work among parallel threads
             * @param executor the executor to use for parallel operations
             * @return a new ParallelSettings instance configured with the specified parameters
             * @deprecated by design — the {@code splitor} hint is a discouraged tuning knob; prefer
             *             {@link #create(Executor)} / {@link #create(int, Executor)}.
             */
            @Deprecated
            public static ParallelSettings create(final Splitor splitor, final Executor executor) {
                return new ParallelSettings().splitor(splitor).executor(executor);
            }

            /**
             * Creates a ParallelSettings instance with all configuration parameters.
             *
             * @param maxThreadNum the maximum number of threads to use for parallel operations
             * @param splitor the strategy for splitting work among parallel threads
             * @param executor the executor to use for parallel operations
             * @return a new ParallelSettings instance configured with all specified parameters
             * @deprecated by design — the {@code splitor} hint is a discouraged tuning knob; prefer
             *             {@link #create(int, Executor)}.
             */
            @Deprecated
            public static ParallelSettings create(final int maxThreadNum, final Splitor splitor, final Executor executor) {
                return new ParallelSettings().maxThreadNum(maxThreadNum).splitor(splitor).executor(executor);
            }
        }
    }
}
