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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.FB;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteSupplier;
import com.landawn.abacus.util.function.ByteTernaryOperator;
import com.landawn.abacus.util.function.ByteToIntFunction;
import com.landawn.abacus.util.function.ByteTriPredicate;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * A specialized stream implementation for processing sequences of primitive {@code byte} values
 * (range {@code -128} to {@code 127}) with functional-style operations.
 * This abstract class extends {@link StreamBase} and provides comprehensive byte-specific operations including
 * filtering, mapping, reduction, and collection operations optimized for byte data types.
 *
 * <p>ByteStream represents a sequence of primitive {@code byte} elements (each in the range
 * {@code Byte.MIN_VALUE} (-128) to {@code Byte.MAX_VALUE} (127)) supporting sequential and parallel
 * aggregate operations. It provides a more efficient alternative to generic {@link Stream} when working
 * specifically with byte values, avoiding boxing/unboxing overhead and offering byte-specific utility methods.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed for byte operations, preventing ClassCastException</li>
 *   <li><b>Performance:</b> Optimized for byte primitives, avoiding boxing overhead</li>
 *   <li><b>Comprehensive API:</b> Full range of stream operations tailored for byte processing</li>
 *   <li><b>I/O Integration:</b> Direct support for file and stream I/O operations</li>
 *   <li><b>Parallel Support:</b> Efficient parallel processing capabilities for large byte datasets</li>
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
 *   <li><b>File Processing:</b> Reading and processing binary files, byte buffers</li>
 *   <li><b>Data Transformation:</b> Converting byte arrays, encoding/decoding operations</li>
 *   <li><b>Network I/O:</b> Processing network packets, protocol data</li>
 *   <li><b>Cryptography:</b> Processing cryptographic data, hash calculations</li>
 *   <li><b>Image Processing:</b> Basic byte-level image data manipulation</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic byte stream operations
 * ByteStream.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5)
 *     .filter(b -> b > 2)        // keeps bytes > 2
 *     .map(b -> (byte)(b * 2))   // transforms each byte by doubling
 *     .sum();                    // returns 24
 *
 * // File I/O operations
 * try (ByteStream bytes = ByteStream.of(new File("data.bin"))) {
 *     bytes.takeWhile(b -> b != 0) // keeps bytes until null terminator
 *          .toArray();             // returns byte array
 * }                                // closes the stream automatically
 *
 * // Statistical operations
 * ByteSummaryStatistics stats = ByteStream.of(byteArray)
 *     .filter(b -> b >= 0)    // keeps only non-negative bytes
 *     .summaryStatistics();   // gets min, max, avg, count
 *
 * // Parallel processing for large datasets
 * ByteStream.of(largeByteArray)
 *     .parallel()                  // switches to parallel processing
 *     .filter(this::isValidByte)   // filters in parallel
 *     .map(this::transformByte)    // transforms in parallel
 *     .sequential()                // switches back to sequential
 *     .toByteList();               // collects results
 *
 * // Integration with other streams
 * Stream<String> strings = Stream.of("Hello", "World");
 * IntStream bytes = strings
 *     .flatMapToInt(s -> ByteStream.of(s.getBytes()).asIntStream()) // maps each string to its bytes
 *     .map(b -> b & 0xFF);                                          // transforms to unsigned int
 * }</pre>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li>Use ByteStream instead of {@code Stream<Byte>} to avoid boxing overhead</li>
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
 * @see ByteIterator
 * @see ByteList
 * @see ByteSummaryStatistics
 * @see java.nio.ByteBuffer
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@LazyEvaluation
public abstract class ByteStream extends StreamBase<Byte, byte[], BytePredicate, ByteConsumer, OptionalByte, IndexedByte, ByteIterator, ByteStream> {

    static final Random RAND = new SecureRandom();

    ByteStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
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
     * ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 2, (byte) 1)
     *       .takeWhile(x -> x < 4)
     *       .toArray();   // returns [(byte) 1, (byte) 2, (byte) 3]
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
    public abstract ByteStream takeWhile(final BytePredicate predicate);

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
     * ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 2, (byte) 1)
     *       .dropWhile(x -> x < 4)
     *       .toArray();   // returns [(byte) 4, (byte) 5, (byte) 2, (byte) 1]
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
    public abstract ByteStream dropWhile(final BytePredicate predicate);

    /**
     * Returns a ByteStream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each byte element using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4)
     *       .map(x -> (byte) (x * 2))
     *       .toArray();   // returns [(byte) 2, (byte) 4, (byte) 6, (byte) 8]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to byte
     * @return a new ByteStream consisting of the results of applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream map(ByteUnaryOperator mapper);

    /**
     * Returns an IntStream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each byte element to an int value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2, (byte)3)
     *       .mapToInt(x -> x * 10)
     *       .toArray();   // [10, 20, 30]
     *
     * // Convert bytes to their unsigned int values
     * ByteStream.of((byte)-1, (byte)-2)
     *       .mapToInt(b -> b & 0xFF)
     *       .toArray();   // [255, 254]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to int
     * @return a new IntStream consisting of the results of applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see #map(ByteUnaryOperator)
     * @see #mapToObj(ByteFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapToInt(ByteToIntFunction mapper);

    /**
     * Returns an object-valued Stream consisting of the results of applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each byte element to an object of type T using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert bytes to characters
     * ByteStream.of((byte)65, (byte)66, (byte)67)
     *       .mapToObj(b -> String.valueOf((char)b))
     *       .toList();   // ["A", "B", "C"]
     *
     * // Convert bytes to hex strings (mask with 0xFF to treat as unsigned)
     * ByteStream.of((byte)1, (byte)15, (byte)127)
     *       .mapToObj(b -> String.format("%02X", b & 0xFF))
     *       .toList();   // ["01", "0F", "7F"]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to T
     * @return a new Stream of objects resulting from applying the mapper function to each element
     * @throws IllegalStateException if the stream is already closed
     * @see #map(ByteUnaryOperator)
     * @see #mapToInt(ByteToIntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> mapToObj(ByteFunction<? extends T> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped stream produced by applying the provided mapping function to each element.
     * Each non-null mapped stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2)
     *     .flatMap(x -> ByteStream.of(x, (byte)(x * 10)))
     *     .toArray();   // [1, 10, 2, 20]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to ByteStream
     * @return a new {@link ByteStream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapArray(ByteFunction)
     * @see #flatMapToInt(ByteFunction)
     * @see #flatMapToObj(ByteFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream flatMap(ByteFunction<? extends ByteStream> mapper);

    // @ParallelSupported
    // @IntermediateOp
    // public abstract ByteStream flatmap(ByteFunction<ByteIterator> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped {@link java.util.Collection Collection} produced by applying the provided mapping function to each element.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(ByteFunction) flatMap} (uppercase 'M') - transforms each element into a {@link ByteStream}.</li>
     *   <li>{@link #flatmap(ByteFunction) flatmap} (this method, lowercase 'm') - transforms each element into a {@link java.util.Collection Collection} of {@link Byte}.</li>
     *   <li>{@link #flatMapArray(ByteFunction) flatMapArray} - transforms each element into a {@code byte[]}.</li>
     * </ul>
     *
     * <p>Each element is transformed into a collection, and all resulting collections are flattened
     * into a single {@code ByteStream}. If the mapper returns {@code null} or an empty collection
     * for a given element, that element contributes no values to the resulting stream.
     *
     * <p><b>Null elements within a collection</b> returned by the mapper are <em>not</em> propagated as
     * exceptions. Each {@code null} {@link Byte} entry is replaced with the primitive default value
     * {@code (byte) 0} so that this operation never throws {@link NullPointerException} when unboxing.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte) 1, (byte) 2)
     *           .flatmap(x -> Arrays.asList((byte) x, (byte) (x * 10)))
     *           .toArray();   // [1, 10, 2, 20]
     *
     * // null elements in a returned collection become (byte) 0
     * ByteStream.of((byte) 1)
     *           .flatmap(x -> Arrays.asList((Byte) null, (byte) 7))
     *           .toArray();   // [0, 7]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to {@code Collection<Byte>}
     * @return a new {@code ByteStream} consisting of the flattened contents of the collections produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(ByteFunction)
     * @see #flatMapArray(ByteFunction)
     * @see Stream#flatmap(java.util.function.Function)
     */
    // @ai-ignore flatmap/flatMap naming - intentional: flatMap maps to ByteStream, flatmap maps to Collection<Byte>, flatMapArray maps to byte[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream flatmap(ByteFunction<? extends Collection<Byte>> mapper); //NOSONAR

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of the array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2)
     *     .flatMapArray(x -> new byte[] {x, (byte)(x * 10)})
     *     .toArray();   // [1, 10, 2, 20]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to byte[]
     * @return a new {@code ByteStream} consisting of the flattened contents of the arrays produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(ByteFunction)
     * @see #flatMapToInt(ByteFunction)
     * @see #flatMapToObj(ByteFunction)
     */
    // @ai-ignore flatMapArray/flatMap naming - intentional: flatMap maps to ByteStream, flatMapArray maps to byte[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream flatMapArray(ByteFunction<byte[]> mapper); //NOSONAR

    /**
     * Returns an IntStream consisting of the results of replacing each element of this stream with the contents
     * of a mapped stream produced by applying the provided mapping function to each element.
     * Each non-null mapped stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2)
     *       .flatMapToInt(b -> IntStream.range(0, b))
     *       .toArray();   // [0, 0, 1]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to IntStream
     * @return a new {@link IntStream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(ByteFunction)
     * @see #flatMapToObj(ByteFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatMapToInt(ByteFunction<? extends IntStream> mapper);

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of this stream
     * with the contents of a mapped stream produced by applying the provided mapping function to each element.
     * Each non-null mapped stream is closed after its contents are consumed or when the resulting
     * stream is closed. A null mapped stream is treated as empty.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2)
     *       .flatMapToObj(x -> Stream.of("byte:" + x, "doubled:" + (x * 2)))
     *       .toList();   // ["byte:1", "doubled:2", "byte:2", "doubled:4"]
     *
     * // Convert bytes to multiple strings
     * ByteStream.of((byte)65, (byte)66)
     *       .flatMapToObj(b -> Stream.of(String.valueOf((char)b), String.valueOf(b)))
     *       .toList();   // ["A", "65", "B", "66"]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to Stream&lt;T&gt;
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMap(ByteFunction)
     * @see #flatMapToInt(ByteFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapToObj(ByteFunction<? extends Stream<? extends T>> mapper);

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of this stream
     * with the contents of a collection produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2)
     *       .flatmapToObj(b -> List.of("item" + b, "value" + b))
     *       .toList();   // ["item1", "value1", "item2", "value2"]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to Collection&lt;T&gt;
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the collections produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapToObj(ByteFunction)
     * @see #flatMapArrayToObj(ByteFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatmapToObj(ByteFunction<? extends Collection<? extends T>> mapper); //NOSONAR

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of this stream
     * with the contents of an array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)2, (byte)3)
     *       .flatMapArrayToObj(b -> new String[] {"val" + b, "item" + b})
     *       .toList();   // ["val2", "item2", "val3", "item3"]
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to T[]
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the arrays produced by the mapper
     * @throws IllegalStateException if the stream is already closed
     * @see #flatMapToObj(ByteFunction)
     * @see #flatmapToObj(ByteFunction)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapArrayToObj(ByteFunction<T[]> mapper);

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of this stream,
     * and only including non-empty results. This is useful for operations that may produce optional values,
     * allowing you to filter out empty optionals in a single operation.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert bytes to positive values only
     * ByteStream.of((byte)-2, (byte)-1, (byte)0, (byte)1, (byte)2)
     *     .mapPartial(b -> b > 0 ? OptionalByte.of(b) : OptionalByte.empty())
     *     .toArray();   // [1, 2]
     *
     * // Safe division operation
     * ByteStream.of((byte)10, (byte)20, (byte)0, (byte)30)
     *     .mapPartial(b -> b != 0 ? OptionalByte.of((byte)(100 / b)) : OptionalByte.empty())
     *     .toArray();   // [10, 5, 3]
     * }</pre>
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.
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
     * @param mapper a non-interfering, stateless function that transforms each element from byte to OptionalByte
     * @return a new ByteStream containing only values from non-empty OptionalByte results
     * @throws IllegalStateException if the stream is already closed
     * @see #map(ByteUnaryOperator)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream mapPartial(ByteFunction<OptionalByte> mapper);

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
     * // Group consecutive bytes within distance 2 and return the sum of first+last
     * ByteStream.of((byte)1, (byte)2, (byte)3, (byte)10, (byte)11)
     *     .rangeMap((first, next) -> next - first < 3, (first, last) -> (byte)(first + last))
     *     .toArray();   // [(byte)(1+3), (byte)(10+11)] = [4, 21]
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
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ByteStream rangeMap(final ByteBiPredicate sameRange, final ByteBinaryOperator mapper);

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
     * // Group consecutive bytes and map each range to a string summary
     * ByteStream.of((byte)1, (byte)2, (byte)3, (byte)10, (byte)11)
     *     .rangeMapToObj((first, next) -> next - first < 3, (first, last) -> first + ".." + last)
     *     .toList();   // ["1..3", "10..11"]
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
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract <T> Stream<T> rangeMapToObj(final ByteBiPredicate sameRange, final ByteBiFunction<? extends T> mapper);

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
     * // Group consecutive bytes with difference <= 2
     * List<ByteList> groups = ByteStream.of((byte)1, (byte)2, (byte)5, (byte)6, (byte)7, (byte)10)
     *     .collapse((a, b) -> b - a <= 2)
     *     .toList();   // [[1, 2], [5, 6, 7], [10]]
     *
     * // Group identical consecutive bytes
     * List<ByteList> identical = ByteStream.of((byte)1, (byte)1, (byte)2, (byte)2, (byte)2, (byte)3)
     *     .collapse((a, b) -> a == b)
     *     .toList();   // [[1, 1], [2, 2, 2], [3]]
     *
     * // Empty stream returns empty result
     * List<ByteList> empty = ByteStream.empty()
     *     .collapse((a, b) -> true)
     *     .toList();   // []
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
    public abstract Stream<ByteList> collapse(final ByteBiPredicate collapsible);

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
     * // Merge consecutive bytes with difference < 3 by summing
     * byte[] result1 = ByteStream.of((byte)1, (byte)2, (byte)5, (byte)6, (byte)7, (byte)10)
     *     .collapse((a, b) -> Math.abs(a - b) < 3, (a, b) -> (byte)(a + b))
     *     .toArray();   // [3, 18, 10]
     *
     * // Merge identical consecutive bytes by keeping the last
     * byte[] result2 = ByteStream.of((byte)1, (byte)1, (byte)2, (byte)3)
     *     .collapse((a, b) -> a == b, (a, b) -> b)
     *     .toArray();   // [1, 2, 3]
     *
     * // No collapsible elements — each element stays separate
     * byte[] result3 = ByteStream.of((byte)1, (byte)10, (byte)20)
     *     .collapse((a, b) -> Math.abs(a - b) < 3, (a, b) -> (byte)(a + b))
     *     .toArray();   // [1, 10, 20]
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
    public abstract ByteStream collapse(final ByteBiPredicate collapsible, final ByteBinaryOperator mergeFunction);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements satisfy a three-element predicate.
     * When adjacent elements satisfy the collapsible predicate (applied with the first and last elements of the current group and the next element), they are merged using the provided merge function.
     * The collapsible predicate takes three elements: the first and last elements of the group, and the next element in the stream.
     *
     * <p><b>Implementation Note:</b>
     * For example, if this stream contains elements [1, 3, 6, 10, 15, 16] and the predicate
     * tests whether the difference between the next element and the first element of the current group is less than 4,
     * and the merge function sums the elements, the resulting stream will contain [4, 6, 10, 31].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse using tri-predicate: collapse when next - first <= 3
     * byte[] result1 = ByteStream.of((byte)1, (byte)3, (byte)6, (byte)10, (byte)15, (byte)16)
     *     .collapse((first, last, next) -> next - first <= 3, (a, b) -> (byte)(a + b))
     *     .toArray();   // [4, 6, 10, 31]
     *
     * // Collapse ranges where next - first <= 2
     * byte[] result2 = ByteStream.of((byte)1, (byte)2, (byte)10, (byte)11)
     *     .collapse((first, last, next) -> next - first <= 2, (a, b) -> (byte)(a + b))
     *     .toArray();   // [3, 21]
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
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ByteStream collapse(final ByteTriPredicate collapsible, final ByteBinaryOperator mergeFunction);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * <p>For example, given a stream of numbers [1, 2, 3, 4], and an accumulator that performs addition,
     * the output would be a stream of numbers [1, 3, 6, 10].
     *
     * <p>This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2, (byte)3, (byte)4)
     *     .scan((a, b) -> (byte)(a + b))
     *     .toArray();   // [1, 3, 6, 10]
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
     * @param accumulator a {@code ByteBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code ByteStream} consisting of the results of the scan operation on the elements of the original stream.
     *         Returns an empty stream if this stream is empty.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ByteStream scan(final ByteBinaryOperator accumulator);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes an initial value and a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * <p>For example, given a stream of numbers [1, 2, 3, 4], an initial value of 10, and an accumulator that performs addition,
     * the output would be a stream of numbers [11, 13, 16, 20].
     *
     * <p>This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2, (byte)3, (byte)4)
     *     .scan((byte)10, (a, b) -> (byte)(a + b))
     *     .toArray();   // [11, 13, 16, 20]
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
     * @param accumulator a {@code ByteBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code ByteStream} consisting of the results of the scan operation on the elements of the original stream.
     *         Returns an empty stream if this stream is empty.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(Object, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ByteStream scan(final byte init, final ByteBinaryOperator accumulator);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes an initial value and a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * <p>This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Include the initial value in the result
     * ByteStream.of((byte)1, (byte)2, (byte)3)
     *     .scan((byte)10, true, (a, b) -> (byte)(a + b))
     *     .toArray();   // [10, 11, 13, 16]
     *
     * // Exclude the initial value from the result
     * ByteStream.of((byte)1, (byte)2, (byte)3)
     *     .scan((byte)10, false, (a, b) -> (byte)(a + b))
     *     .toArray();   // [11, 13, 16]
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
     * @param initIncluded if {@code true}, the {@code init} value is included as the first element of the returned stream;
     *        if {@code false}, the scan starts from the first stream element combined with {@code init}, and {@code init} itself is not emitted.
     * @param accumulator a {@code ByteBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code ByteStream} consisting of the results of the scan operation on the elements of the original stream.
     *         If this stream is empty and {@code initIncluded} is {@code false}, returns an empty stream;
     *         if this stream is empty and {@code initIncluded} is {@code true}, returns a single-element stream containing {@code init}.
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#scan(Object, boolean, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract ByteStream scan(final byte init, final boolean initIncluded, final ByteBinaryOperator accumulator);

    /**
     * Returns a stream consisting of the specified elements followed by the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)3, (byte)4)
     *     .prepend((byte)1, (byte)2)
     *     .toArray();   // returns [(byte)1, (byte)2, (byte)3, (byte)4]
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
    public abstract ByteStream prepend(final byte... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2)
     *     .append((byte)3, (byte)4)
     *     .toArray();   // returns [(byte)1, (byte)2, (byte)3, (byte)4]
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
    public abstract ByteStream append(final byte... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended if this stream is empty.
     * If this stream is not empty, returns this stream unchanged.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With empty stream
     * ByteStream.empty()
     *     .appendIfEmpty((byte)1, (byte)2)
     *     .toArray();   // returns [(byte)1, (byte)2]
     *
     * // With non-empty stream
     * ByteStream.of((byte)10, (byte)20)
     *     .appendIfEmpty((byte)1, (byte)2)
     *     .toArray();   // returns [(byte)10, (byte)20]
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
    public abstract ByteStream appendIfEmpty(final byte... a);

    /**
     * Collects all elements of this stream into a ByteList.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteStream.of((byte)1, (byte)2, (byte)3).toByteList();
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
     * @return a ByteList containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract ByteList toByteList();

    /**
     * Returns a Map containing the results of applying the given functions to the elements of this stream.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<Byte, String> map = ByteStream.of((byte)1, (byte)2, (byte)3)
     *     .toMap(b -> b, b -> "Value" + b);
     * // Result: {1=Value1, 2=Value2, 3=Value3}
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
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a function to produce keys for the map
     * @param valueMapper a function to produce values for the map
     * @return a Map whose keys and values are the result of applying the provided mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.ByteFunction<? extends K, E> keyMapper,
            Throwables.ByteFunction<? extends V, E2> valueMapper) throws E, E2;

    /**
     * Returns a Map containing the results of applying the given functions to the elements of this stream.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LinkedHashMap<Byte, String> map = ByteStream.of((byte)3, (byte)1, (byte)2)
     *     .toMap(b -> b, b -> "Value" + b, LinkedHashMap::new);
     * // Result: LinkedHashMap {3=Value3, 1=Value1, 2=Value2} (maintains insertion order)
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
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a function to produce keys for the map
     * @param valueMapper a function to produce values for the map
     * @param mapFactory a function which returns a new, empty Map into which the results will be inserted
     * @return a Map whose keys and values are the result of applying the provided mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.ByteFunction<? extends K, E> keyMapper,
            Throwables.ByteFunction<? extends V, E2> valueMapper, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Returns a Map containing the results of applying the given functions to the elements of this stream.
     * If the mapped keys contain duplicates, the value mapping function is applied to each equal element,
     * and the results are merged using the provided merging function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = ByteStream.of((byte)1, (byte)2, (byte)11, (byte)12)
     *     .toMap(b -> b < 10 ? "small" : "large",
     *            b -> (int) b,
     *            Integer::sum);
     * // Result: {small=3, large=23}
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
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a function to produce keys for the map
     * @param valueMapper a function to produce values for the map
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @return a Map whose keys and values are the result of applying the provided mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.ByteFunction<? extends K, E> keyMapper,
            Throwables.ByteFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction) throws E, E2;

    /**
     * Returns a Map containing the results of applying the given functions to the elements of this stream.
     * If the mapped keys contain duplicates, the value mapping function is applied to each equal element,
     * and the results are merged using the provided merging function.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TreeMap<String, Integer> map = ByteStream.of((byte)1, (byte)2, (byte)11, (byte)12)
     *     .toMap(b -> b < 10 ? "small" : "large",
     *            b -> (int) b,
     *            Integer::sum,
     *            TreeMap::new);
     * // Result: TreeMap {large=23, small=3} (sorted by key)
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
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a function to produce keys for the map
     * @param valueMapper a function to produce values for the map
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @param mapFactory a function which returns a new, empty Map into which the results will be inserted
     * @return a Map whose keys and values are the result of applying the provided mapping functions to the input elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.ByteFunction<? extends K, E> keyMapper,
            Throwables.ByteFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Groups the elements of this stream according to a classification function,
     * and returns the results in a Map where the keys are produced by the classification function
     * and the mapped values are the results of applying the downstream collector to elements that map to the associated key.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Long> grouped = ByteStream.of((byte)1, (byte)2, (byte)11, (byte)12, (byte)13)
     *     .groupTo(b -> b < 10 ? "small" : "large", Collectors.counting());
     * // Result: {small=2, large=3}
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
     * @param <E> the type of exception thrown by the classifier
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a Collector implementing the downstream reduction
     * @return a Map containing the results of the group-by operation
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the classifier function throws an exception
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, E extends Exception> Map<K, D> groupTo(Throwables.ByteFunction<? extends K, E> keyMapper,
            final Collector<? super Byte, ?, D> downstream) throws E;

    /**
     * Groups the elements of this stream according to a classification function,
     * and returns the results in a Map where the keys are produced by the classification function
     * and the mapped values are the results of applying the downstream collector to elements that map to the associated key.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TreeMap<String, Long> grouped = ByteStream.of((byte)1, (byte)2, (byte)11, (byte)12, (byte)13)
     *     .groupTo(b -> b < 10 ? "small" : "large",
     *              Collectors.counting(),
     *              TreeMap::new);
     * // Result: TreeMap {large=3, small=2} (sorted by key)
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
     * @param <E> the type of exception thrown by the classifier
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a Collector implementing the downstream reduction
     * @param mapFactory a function which, when called, produces a new empty Map of the desired type
     * @return a Map containing the results of the group-by operation
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the classifier function throws an exception
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, M extends Map<K, D>, E extends Exception> M groupTo(Throwables.ByteFunction<? extends K, E> keyMapper,
            final Collector<? super Byte, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E;

    /**
     * Performs a reduction on the elements of this stream, using the provided accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current reduced value (or the initial value for the first element), and the current stream element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte sum = ByteStream.of((byte)1, (byte)2, (byte)3).reduce((byte)0, (a, b) -> (byte)(a + b));   // 6
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
     * @param identity the initial value of the reduction operation
     * @param accumulator the function for combining the current reduced value and the current stream element
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#reduce(Object, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract byte reduce(byte identity, ByteBinaryOperator accumulator);

    /**
     * Performs a reduction on the elements of this stream, using the provided accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current reduced value and the current stream element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalByte product = ByteStream.of((byte)2, (byte)3, (byte)4)
     *     .reduce((a, b) -> (byte)(a * b));   // OptionalByte[24]
     *
     * OptionalByte max = ByteStream.of((byte)5, (byte)2, (byte)8, (byte)1)
     *     .reduce((a, b) -> a > b ? a : b);   // OptionalByte[8]
     *
     * OptionalByte empty = ByteStream.empty().reduce((a, b) -> (byte)(a + b));   // OptionalByte.empty()
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
     * @return an OptionalByte describing the result of the reduction. If the stream is empty, an empty {@code OptionalByte} is returned.
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @TerminalOp
    public abstract OptionalByte reduce(ByteBinaryOperator accumulator);

    /**
     * Performs a mutable reduction operation on the elements of this stream using a Collector.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteStream.of((byte)1, (byte)2, (byte)3)
     *     .collect(ByteList::new, ByteList::add, ByteList::addAll);
     * // Result: ByteList [1, 2, 3]
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
    public abstract <R> R collect(Supplier<R> supplier, ObjByteConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * Performs a mutable reduction operation on the elements of this stream using a Collector.
     *
     * <br />
     * Only call this method when the returned type {@code R} is one of these types: {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * Otherwise, please call {@link #collect(Supplier, ObjByteConsumer, BiConsumer)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteList list = ByteStream.of((byte)1, (byte)2, (byte)3)
     *     .collect(ByteList::new, ByteList::add);
     * // Result: ByteList [1, 2, 3]
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
     * @param <R> The type of the result. It must be  {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * @param supplier a function that creates a new result container. For a parallel execution, this function may be called multiple times and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function for incorporating an additional element into a result.
     * @throws IllegalStateException if the stream is already closed
     * @throws RuntimeException if this stream is parallel and the result type {@code R} is not one of:
     *         {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}
     *         (the default combiner cannot merge the per-thread containers); sequential streams perform no such check.
     * @return the result of the reduction
     * @see #collect(Supplier, ObjByteConsumer, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjByteConsumer<? super R> accumulator);

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
     * ByteStream.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5)
     *     .forEach(System.out::println);
     * // prints: 1, 2, 3, 4, 5 (each on a new line)
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
    public abstract <E extends Exception> void forEach(Throwables.ByteConsumer<E> action) throws E;

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
     * ByteStream.of((byte)10, (byte)20, (byte)30)
     *     .forEachIndexed((index, value) ->
     *         System.out.println("Element at index " + index + ": " + value));
     * // prints:
     * // Element at index 0: 10
     * // Element at index 1: 20
     * // Element at index 2: 30
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
     * @param action a non-interfering action to perform on the elements, where the first parameter is the index and the second is the element
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEachIndexed(Throwables.IntByteConsumer<E> action) throws E;

    /**
     * Returns whether any elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code false} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean hasNegative = ByteStream.of((byte)1, (byte)-2, (byte)3)
     *     .anyMatch(b -> b < 0);   // true
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
    public abstract <E extends Exception> boolean anyMatch(final Throwables.BytePredicate<E> predicate) throws E;

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code true} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean allPositive = ByteStream.of((byte)1, (byte)2, (byte)3)
     *     .allMatch(b -> b > 0);   // true
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
    public abstract <E extends Exception> boolean allMatch(final Throwables.BytePredicate<E> predicate) throws E;

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for determining the result.
     * If the stream is empty then {@code true} is returned and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean noNegatives = ByteStream.of((byte)1, (byte)2, (byte)3)
     *     .noneMatch(b -> b < 0);   // true
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
    public abstract <E extends Exception> boolean noneMatch(final Throwables.BytePredicate<E> predicate) throws E;

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalByte}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalByte first = ByteStream.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5)
     *                                 .findFirst();   // returns OptionalByte.of(1)
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
     * @return an {@code OptionalByte} containing the first element of the stream, or an empty {@code OptionalByte} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #first()
     * @see #findAny()
     * @see #findFirst(Throwables.BytePredicate)
     * @see #findAny(Throwables.BytePredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalByte findFirst() {
        assertNotClosed();

        return first();
    }

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalByte}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()} and {@link #findFirst()}; despite the
     * JDK-style name it returns the FIRST element, not an arbitrary one, even for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.
     * Unlike the general contract of {@code findAny}, this implementation always returns the first element,
     * even on parallel streams.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalByte any = ByteStream.of((byte)1, (byte)2, (byte)3, (byte)4, (byte)5)
     *                              .findAny();   // returns OptionalByte.of(1)
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
     * @return an {@code OptionalByte} containing the first element of the stream, or an empty {@code OptionalByte} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @see #first()
     * @see #findFirst()
     * @see #findFirst(Throwables.BytePredicate)
     * @see #findAny(Throwables.BytePredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalByte findAny() {
        assertNotClosed();

        return first();
    }

    /**
     * Returns an {@link OptionalByte} describing the first element of this stream that matches the given predicate,
     * or an empty {@code OptionalByte} if no such element exists.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalByte firstNegative = ByteStream.of((byte)1, (byte)2, (byte)-3, (byte)4)
     *     .findFirst(b -> b < 0);   // OptionalByte[-3]
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
     * @return an {@code OptionalByte} describing the first element of this stream that matches the given predicate,
     *         or an empty {@code OptionalByte} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalByte findFirst(final Throwables.BytePredicate<E> predicate) throws E;

    /**
     * Returns an {@link OptionalByte} describing some element of this stream that matches the given predicate,
     * or an empty {@code OptionalByte} if no such element exists.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is free to select any element in the stream.
     * This is to allow for maximal performance in parallel operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalByte anyNegative = ByteStream.of((byte)1, (byte)2, (byte)-3, (byte)-4)
     *     .findAny(b -> b < 0);   // OptionalByte[-3] (or -4 in parallel streams)
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
     * @return an {@code OptionalByte} describing some element of this stream that matches the given predicate,
     *         or an empty {@code OptionalByte} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalByte findAny(final Throwables.BytePredicate<E> predicate) throws E;

    /**
     * Returns an {@link OptionalByte} describing the last element of this stream that matches the given predicate,
     * or an empty {@code OptionalByte} if no such element exists.
     *
     * <p>This is a terminal operation.
     *
     * <p>Consider using: {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalByte lastNegative = ByteStream.of((byte)1, (byte)-2, (byte)3, (byte)-4)
     *     .findLast(b -> b < 0);   // OptionalByte[-4]
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
     * @return an {@code OptionalByte} describing the last element of this stream that matches the given predicate,
     *         or an empty {@code OptionalByte} if no such element exists
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalByte findLast(final Throwables.BytePredicate<E> predicate) throws E;

    /**
     * Returns an {@link OptionalByte} describing the minimum element of this stream,
     * or an empty {@code OptionalByte} if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte) 5, (byte) 2, (byte) 8, (byte) 1, (byte) 9).min();                   // returns OptionalByte[1]
     *
     * ByteStream.empty().min();                                                                // returns OptionalByte.empty()
     *
     * // Safe retrieval with default value
     * byte minValue = ByteStream.of((byte) 10, (byte) 20, (byte) 30).min().orElse((byte) 0);   // returns 10
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
     * @return an {@code OptionalByte} containing the minimum element of this stream,
     *         or an empty {@code OptionalByte} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalByte min();

    /**
     * Returns an {@link OptionalByte} describing the maximum element of this stream,
     * or an empty {@code OptionalByte} if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte) 5, (byte) 2, (byte) 8, (byte) 1, (byte) 9).max();                   // returns OptionalByte[9]
     *
     * ByteStream.empty().max();                                                                // returns OptionalByte.empty()
     *
     * // Safe retrieval with default value
     * byte maxValue = ByteStream.of((byte) 10, (byte) 20, (byte) 30).max().orElse((byte) 0);   // returns 30
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
     * @return an {@code OptionalByte} containing the maximum element of this stream,
     *         or an empty {@code OptionalByte} if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalByte max();

    /**
     * Returns the <i>k-th</i> largest element in the stream.
     * If the stream is empty or the count of elements is less than k, an empty {@code OptionalByte} is returned.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the 2nd largest element
     * ByteStream.of((byte) 10, (byte) 30, (byte) 20, (byte) 50, (byte) 40).kthLargest(2);   // returns OptionalByte[40]
     *
     * // Find the largest element (same as max)
     * ByteStream.of((byte) 5, (byte) 2, (byte) 8, (byte) 1).kthLargest(1);   // returns OptionalByte[8]
     *
     * // When k exceeds stream size
     * ByteStream.of((byte) 1, (byte) 2, (byte) 3).kthLargest(5);   // returns OptionalByte.empty()
     * }</pre>
     *
     * <p><b>Note:</b> {@code ByteStream} (like {@code CharStream}) intentionally omits the {@code top(n)} /
     * {@code top(n, Comparator)} family that {@code ShortStream}/{@code IntStream} provide — for the small
     * {@code byte} domain the n largest elements are easily obtained otherwise: use {@code sorted()}
     * (ascending) and take the tail, or {@code boxed().top(n)} on the resulting {@code Stream<Byte>}.
     * This is by design.
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
     * @param k the position (1-based) of the largest element to retrieve
     * @return an {@code OptionalByte} containing the k-th largest element, or an empty {@code OptionalByte}
     *         if the stream is empty or the count of elements is less than k
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if k is less than 1
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalByte kthLargest(int k);

    /**
     * Returns the sum of all elements in this stream as an int. This is a terminal operation.
     *
     * <p>The sum is accumulated in a {@code long} and then converted to {@code int}. For empty streams, returns 0.
     *
     * <p><b>Note:</b> if the total sum exceeds {@link Integer#MAX_VALUE} (possible only when summing an
     * extremely large number of elements), an {@link ArithmeticException} is thrown rather than silently overflowing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int total = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).sum();   // returns 15
     *
     * // Sum of byte array
     * byte[] data = {10, 20, 30, 40};
     * int sum = ByteStream.of(data).sum();   // returns 100
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
     * @return the sum of elements in this stream as an int. Returns 0 if the stream is empty.
     * @throws IllegalStateException if the stream is already closed
     * @throws ArithmeticException if the sum overflows an {@code int}
     * @see #average()
     * @see #reduce(byte, ByteBinaryOperator)
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
     * ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).average();   // returns OptionalDouble[3.0]
     *
     * ByteStream.of((byte) 10, (byte) 20, (byte) 30).average();                    // returns OptionalDouble[20.0]
     *
     * ByteStream.empty().average();                                                // returns OptionalDouble.empty()
     *
     * // Safe retrieval with default value
     * double avg = ByteStream.of((byte) 100, (byte) 50).average().orElse(0.0);     // returns 75.0
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
     * @return an OptionalDouble containing the arithmetic mean of the elements of this stream,
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
     * ByteSummaryStatistics stats = ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).summaryStatistics();
     * System.out.println("Count: " + stats.getCount());       // count is 5
     * System.out.println("Sum: " + stats.getSum());           // sum is 15
     * System.out.println("Min: " + stats.getMin());           // min is 1
     * System.out.println("Max: " + stats.getMax());           // max is 5
     * System.out.println("Average: " + stats.getAverage());   // average is 3.0
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
     * @return a {@code ByteSummaryStatistics} describing various summary data about the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract ByteSummaryStatistics summaryStatistics();

    /**
     * Returns a pair consisting of ByteSummaryStatistics for the elements of this stream,
     * and an Optional containing a map of percentiles if the stream is not empty.
     * To calculate the percentiles of the elements in the stream, all elements will be loaded into memory and sorted if not yet.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> result =
     *     ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5).summaryStatisticsAndPercentiles();
     * ByteSummaryStatistics stats = result.left();
     * Optional<Map<Percentage, Byte>> percentiles = result.right();
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
     * @return a {@code Pair} containing a {@code ByteSummaryStatistics} describing various summary data
     *         and an {@code Optional<Map<Percentage, Byte>>} containing percentile values
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @TerminalOp
    public abstract Pair<ByteSummaryStatistics, Optional<Map<Percentage, Byte>>> summaryStatisticsAndPercentiles();

    /**
     * Merges this stream with another stream, selecting elements based on the provided selector function.
     * The selector function determines which element to take next from the two streams.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)3, (byte)5)
     *     .mergeWith(ByteStream.of((byte)2, (byte)4, (byte)6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // [1, 2, 3, 4, 5, 6]
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
    public abstract ByteStream mergeWith(final ByteStream b, final ByteBiFunction<MergeResult> nextSelector);

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
     * ByteStream.of((byte)1, (byte)2, (byte)3)
     *           .zipWith(ByteStream.of((byte)10, (byte)20, (byte)30, (byte)40),
     *                    (a, b) -> (byte)(a + b))
     *           .toArray();   // returns [(byte)11, (byte)22, (byte)33]
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
     * @param b the ByteStream to be combined with the current ByteStream. Must be {@code non-null}.
     * @param zipFunction a ByteBinaryOperator that determines the combination of elements in the combined ByteStream. Must be {@code non-null}.
     * @return a new ByteStream that is the result of combining the current ByteStream with the given ByteStream
     * @throws IllegalStateException if the stream is already closed
     * @see #zipWith(ByteStream, byte, byte, ByteBinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream zipWith(ByteStream b, ByteBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the shortest of the three streams.
     * If any stream is longer, its remaining elements are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2, (byte)3)
     *           .zipWith(ByteStream.of((byte)10, (byte)20), ByteStream.of((byte)100, (byte)100),
     *                    (a, b, c) -> (byte)(a + b + c))
     *           .toArray();   // returns [(byte)111, (byte)122]
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
     * @param b the second ByteStream to be combined with the current ByteStream. Will be closed along with this ByteStream.
     * @param c the third ByteStream to be combined with the current ByteStream. Will be closed along with this ByteStream.
     * @param zipFunction a ByteTernaryOperator that determines the combination of elements in the combined ByteStream. Must be {@code non-null}.
     * @return a new ByteStream that is the result of combining the current ByteStream with the given ByteStreams
     * @throws IllegalStateException if the stream is already closed
     * @see #zipWith(ByteStream, ByteStream, byte, byte, byte, ByteTernaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream zipWith(ByteStream b, ByteStream c, ByteTernaryOperator zipFunction);

    /**
     * Zips this stream with the given stream using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from two streams pairwise.
     *
     * <p>The resulting stream will have the length of the longer of the two streams.
     * Default values are used when one stream runs out of elements before the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2, (byte)3)
     *           .zipWith(ByteStream.of((byte)10), (byte)0, (byte)0, (a, b) -> (byte)(a + b))
     *           .toArray();   // returns [(byte)11, (byte)2, (byte)3]
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
     * @param b the ByteStream to be combined with the current ByteStream. Will be closed along with this ByteStream.
     * @param valueForNoneA the default value to use for the current ByteStream when it runs out of elements
     * @param valueForNoneB the default value to use for the given ByteStream when it runs out of elements
     * @param zipFunction a ByteBinaryOperator that determines the combination of elements in the combined ByteStream. Must be {@code non-null}.
     * @return a new ByteStream that is the result of combining the current ByteStream with the given ByteStream
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream zipWith(ByteStream b, byte valueForNoneA, byte valueForNoneB, ByteBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the longest of the three streams.
     * Default values are used when any stream runs out of elements before the others.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2, (byte)3)
     *           .zipWith(ByteStream.of((byte)10), ByteStream.of((byte)100),
     *                    (byte)0, (byte)0, (byte)0,
     *                    (a, b, c) -> (byte)(a + b + c))
     *           .toArray();   // returns [(byte)111, (byte)2, (byte)3]
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
     * @param b the second ByteStream to be combined with the current ByteStream. Will be closed along with this ByteStream.
     * @param c the third ByteStream to be combined with the current ByteStream. Will be closed along with this ByteStream.
     * @param valueForNoneA the default value to use for the current ByteStream when it runs out of elements
     * @param valueForNoneB the default value to use for the second ByteStream when it runs out of elements
     * @param valueForNoneC the default value to use for the third ByteStream when it runs out of elements
     * @param zipFunction a ByteTernaryOperator that determines the combination of elements in the combined ByteStream. Must be {@code non-null}.
     * @return a new ByteStream that is the result of combining the current ByteStream with the given ByteStreams
     * @throws IllegalStateException if the stream is already closed
     */
    @ParallelSupported
    @IntermediateOp
    public abstract ByteStream zipWith(ByteStream b, ByteStream c, byte valueForNoneA, byte valueForNoneB, byte valueForNoneC, ByteTernaryOperator zipFunction);

    /**
     * Converts this ByteStream to an IntStream by widening each byte value to an int.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2, (byte)3).asIntStream().toArray();   // [1, 2, 3]
     * }</pre>
     *
     * <p><b>Note:</b> {@code byte} widens directly only to {@code int} here, by design. To continue widening
     * to {@code long}/{@code double}, chain on the returned {@link IntStream} (e.g.
     * {@code asIntStream().asLongStream()}); {@code ByteStream} deliberately does not duplicate the full
     * widening ladder.
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
     * @return an IntStream consisting of the elements of this stream, converted to int
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream asIntStream();

    /**
     * Returns a Stream consisting of the elements of this stream, each boxed to a Byte.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream<Byte> boxedStream = ByteStream.of((byte)1, (byte)2, (byte)3)
     *     .boxed();   // returns Stream<Byte> containing [1, 2, 3]
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
     * @return a Stream consisting of the elements of this stream, each boxed to a Byte
     * @throws IllegalStateException if the stream is already closed
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<Byte> boxed();

    abstract ByteIteratorEx iteratorEx();

    // private static final ByteStream EMPTY_STREAM = new ArrayByteStream(N.EMPTY_BYTE_ARRAY, true, null);

    /**
     * Returns an empty ByteStream with no elements.
     * This is a static factory method that creates a stream with zero elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream empty = ByteStream.empty();
     * long count = empty.count();   // 0
     *
     * // Useful as a default return value
     * public ByteStream getBytes() {
     *     return hasBytes() ? ByteStream.of(bytes) : ByteStream.empty();
     * }
     * }</pre>
     *
     * @return an empty ByteStream
     * @see #ofNullable(Byte)
     */
    public static ByteStream empty() {
        return new ArrayByteStream(N.EMPTY_BYTE_ARRAY, true, null);
    }

    /**
     * Returns a ByteStream that is lazily populated by an input supplier.
     * This is a static factory method that defers stream creation until the returned stream is first traversed or closed.
     *
     * <p>The supplier is memoized and invoked at most once. Closing the returned stream before traversal may still
     * invoke the supplier so the supplied stream can be closed.
     *
     * <p><b>Implementation Note:</b> it's equivalent to {@code Stream.just(supplier).flatMapToByte(it -> it.get())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Lazy creation — stream is only built when consumed
     * ByteStream stream = ByteStream.defer(() -> ByteStream.of((byte)1, (byte)2, (byte)3));
     * stream.toArray();   // [1, 2, 3]
     *
     * // Defer an expensive computation
     * ByteStream deferred = ByteStream.defer(() -> {
     *     byte[] data = loadBytesFromNetwork(); // invokes an expensive call
     *     return ByteStream.of(data);
     * });
     * // loadBytesFromNetwork() is not called until deferred is consumed or closed
     *
     * // Null supplier throws IllegalArgumentException
     * ByteStream.defer(null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param supplier the supplier that provides the ByteStream
     * @return a new ByteStream supplied by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#defer(Supplier)
     */
    public static ByteStream defer(final Supplier<ByteStream> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Supplier<ByteStream> s = Fn.memoize(supplier);
        return Stream.just(s).flatMapToByte(Supplier::get).onClose(newCloseHandler(s));
    }

    /**
     * Returns a ByteStream containing a single element if the provided element is {@code non-null},
     * otherwise returns an empty ByteStream.
     * This is a static factory method that handles potential {@code null} values gracefully.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Byte value = Byte.valueOf((byte)42);
     * ByteStream.ofNullable(value).toArray();   // [(byte)42]
     *
     * Byte nullValue = null;
     * ByteStream.ofNullable(nullValue).count();   // 0
     *
     * // Useful for optional values
     * Byte maybeByte = getOptionalByte();
     * ByteStream stream = ByteStream.ofNullable(maybeByte);
     * }</pre>
     *
     * @param e the element, possibly null
     * @return a ByteStream containing the element if {@code non-null}, otherwise an empty stream
     * @see #empty()
     * @see #of(byte...)
     */
    public static ByteStream ofNullable(final Byte e) {
        return e == null ? empty() : of(e);
    }

    /**
     * Returns a ByteStream whose elements are the specified values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.of((byte)1, (byte)2, (byte)3).toArray();   // [1, 2, 3]
     * }</pre>
     *
     * @param a the elements of the new stream
     * @return a new ByteStream consisting of the specified elements
     */
    public static ByteStream of(final byte... a) {
        return N.isEmpty(a) ? empty() : new ArrayByteStream(a);
    }

    /**
     * Returns a ByteStream whose elements are the specified values from the array
     * between fromIndex (inclusive) and toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr = {1, 2, 3, 4, 5};
     * ByteStream.of(arr, 1, 4).toArray();   // [2, 3, 4]
     * }</pre>
     *
     * @param a the array containing the elements
     * @param fromIndex the starting index, inclusive
     * @param toIndex the ending index, exclusive
     * @return a ByteStream containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the array length, or {@code fromIndex} is greater than {@code toIndex}
     */
    public static ByteStream of(final byte[] a, final int fromIndex, final int toIndex) {
        return isEmptyRange(N.len(a), fromIndex, toIndex) ? empty() : new ArrayByteStream(a, fromIndex, toIndex);
    }

    /**
     * Returns a ByteStream whose elements are the unboxed values from the specified Byte array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Byte[] bytes = {Byte.valueOf((byte)1), Byte.valueOf((byte)2), Byte.valueOf((byte)3)};
     * ByteStream.of(bytes).toArray();   // [1, 2, 3]
     * }</pre>
     *
     * @param a the array of Byte objects
     * @return a new ByteStream containing the unboxed values from the array
     */
    public static ByteStream of(final Byte[] a) {
        return Stream.of(a).mapToByte(FB.unbox());
    }

    /**
     * Returns a ByteStream whose elements are the unboxed values from the specified Byte array
     * between fromIndex (inclusive) and toIndex (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Byte[] bytes = {Byte.valueOf((byte)1), Byte.valueOf((byte)2), Byte.valueOf((byte)3), Byte.valueOf((byte)4)};
     * ByteStream.of(bytes, 1, 3).toArray();   // [2, 3]
     * }</pre>
     *
     * @param a the array of Byte objects
     * @param fromIndex the starting index, inclusive
     * @param toIndex the ending index, exclusive
     * @return a new ByteStream containing the unboxed values from the specified array range
     * @throws IndexOutOfBoundsException if fromIndex is negative, toIndex is less than fromIndex,
     *         or toIndex is greater than the array length
     */
    public static ByteStream of(final Byte[] a, final int fromIndex, final int toIndex) {
        return Stream.of(a, fromIndex, toIndex).mapToByte(FB.unbox());
    }

    /**
     * Returns a ByteStream whose elements are the unboxed values from the specified collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Byte> list = Arrays.asList((byte)1, (byte)2, (byte)3);
     * ByteStream.of(list).toArray();   // [1, 2, 3]
     * }</pre>
     *
     * @param c the collection of Byte objects
     * @return a new ByteStream containing the unboxed values from the collection
     */
    public static ByteStream of(final Collection<Byte> c) {
        return Stream.of(c).mapToByte(FB.unbox());
    }

    /**
     * Returns a ByteStream whose elements are provided by the specified {@link ByteIterator}.
     * Returns an empty stream if {@code iterator} is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator iter = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * ByteStream.of(iter).toArray();   // [1, 2, 3]
     * }</pre>
     *
     * @param iterator the iterator providing the elements (may be {@code null})
     * @return a new ByteStream backed by {@code iterator}, or an empty stream if {@code iterator} is {@code null}
     */
    public static ByteStream of(final ByteIterator iterator) {
        return iterator == null ? empty() : new IteratorByteStream(iterator);
    }

    /**
     * Returns a ByteStream whose elements are the bytes from the specified {@link ByteBuffer}
     * between its current {@link ByteBuffer#position() position} (inclusive) and
     * {@link ByteBuffer#limit() limit} (exclusive). Returns an empty stream if {@code buf}
     * is {@code null}.
     *
     * <p>The buffer's position is <b>not</b> advanced by stream consumption — the stream
     * reads bytes via absolute indexed {@code get(int)} access, so the buffer remains
     * usable afterwards.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteBuffer buffer = ByteBuffer.wrap(new byte[]{1, 2, 3});
     * ByteStream stream = ByteStream.of(buffer);
     * // Stream of bytes [1, 2, 3]
     * }</pre>
     *
     * @param buf the ByteBuffer containing the elements (may be {@code null})
     * @return a new ByteStream over bytes in {@code buf} from {@code position} (inclusive)
     *         to {@code limit} (exclusive), or an empty stream if {@code buf} is {@code null}
     */
    public static ByteStream of(final ByteBuffer buf) {
        if (buf == null) {
            return empty();
        }

        //noinspection resource
        return IntStream.range(buf.position(), buf.limit()).mapToByte(buf::get);
    }

    /**
     * Returns a ByteStream that reads bytes from the specified file. The underlying
     * {@code FileInputStream} is opened immediately and is closed automatically when
     * the returned stream is closed.
     *
     * <p>For correct resource cleanup, callers should use the returned stream with
     * try-with-resources or otherwise ensure {@link ByteStream#close()} is invoked.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (ByteStream stream = ByteStream.of(new File("data.bin"))) {
     *     byte[] bytes = stream.toArray();
     * }
     * }</pre>
     *
     * @param file the file to read from
     * @return a new ByteStream over the bytes read from {@code file}
     * @throws UncheckedIOException if the file cannot be opened, or if an I/O error
     *         occurs while later reading from the underlying stream
     */
    public static ByteStream of(final File file) {
        return of(IOUtil.newFileInputStream(file), true);
    }

    /**
     * Returns a ByteStream that reads bytes from the specified {@link InputStream}.
     * The input stream is <b>not</b> closed when the returned stream is closed —
     * the caller retains ownership of {@code is}. Returns an empty stream if
     * {@code is} is {@code null}.
     *
     * <p>To have the underlying input stream closed automatically, use
     * {@link #of(InputStream, boolean) of(is, true)} or {@link #of(File)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream is = new ByteArrayInputStream(new byte[]{1, 2, 3});
     * ByteStream stream = ByteStream.of(is);
     * byte[] bytes = stream.toArray();   // [1, 2, 3]
     * }</pre>
     *
     * @param is the input stream to read from (may be {@code null})
     * @return a new ByteStream over the bytes read from {@code is}, or an empty
     *         stream if {@code is} is {@code null}
     * @throws UncheckedIOException if an I/O error occurs while reading from {@code is}
     *         during stream consumption
     * @see #of(InputStream, boolean)
     */
    public static ByteStream of(final InputStream is) {
        return of(is, false);
    }

    /**
     * Returns a ByteStream reading bytes from the specified InputStream with configurable auto-close behavior.
     * This is a static factory method that allows control over whether the underlying input stream should be
     * automatically closed when the ByteStream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Auto-close the input stream when ByteStream is closed
     * try (InputStream is = new FileInputStream("data.bin");
     *      ByteStream stream = ByteStream.of(is, true)) {
     *     byte[] bytes = stream.toArray();
     * } // closes the input stream automatically
     *
     * // Keep input stream open after ByteStream is closed
     * InputStream sharedStream = getSharedInputStream();
     * ByteStream stream = ByteStream.of(sharedStream, false);
     * byte[] bytes = stream.toArray();
     * // sharedStream is still open for other operations
     * }</pre>
     *
     * @param is the input stream to read from (may be {@code null})
     * @param closeInputStreamWhenStreamIsClosed if {@code true}, the input stream will be closed when the ByteStream is closed;
     *                                           if {@code false}, the input stream remains open
     * @return a new ByteStream consisting of bytes read from the specified InputStream, or an empty stream if {@code is} is {@code null}
     * @throws UncheckedIOException if an I/O error occurs during reading
     * @see #of(InputStream)
     * @see #of(File)
     */
    public static ByteStream of(final InputStream is, final boolean closeInputStreamWhenStreamIsClosed) {
        if (is == null) {
            return empty();
        }

        final ByteIterator iter = new ByteIterator() {
            private final byte[] buf = new byte[8192];
            private boolean isEnd = false;
            private int count = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                if (idx >= count && !isEnd) {
                    try {
                        count = is.read(buf);
                        idx = 0;
                        isEnd = count <= 0;
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                return count > idx;
            }

            @Override
            public byte nextByte() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return buf[idx++];
            }
        };

        if (closeInputStreamWhenStreamIsClosed) {
            //noinspection resource
            return of(iter).onClose(Fn.close(is));
        } else {
            return of(iter);
        }
    }

    private static final Function<byte[], ByteStream> flatMapper = ByteStream::of;

    private static final Function<byte[][], ByteStream> flattMapper = ByteStream::flatten;

    /**
     * Returns a ByteStream whose elements are all the elements from the input two-dimensional array, flattened in row-major order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[][] array = {{1, 2}, {3, 4, 5}};
     * ByteStream.flatten(array).toArray();   // [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @return a new ByteStream containing all elements from the two-dimensional array
     */
    public static ByteStream flatten(final byte[][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToByte(flatMapper);
    }

    /**
     * Returns a ByteStream whose elements are all the elements from the input two-dimensional array, flattened either
     * in row-major order (vertically = false) or column-major order (vertically = true).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[][] array = {{1, 2}, {3, 4}};
     * ByteStream.flatten(array, false).toArray();   // [1, 2, 3, 4] (row by row)
     * ByteStream.flatten(array, true).toArray();    // [1, 3, 2, 4] (column by column)
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @param vertically if {@code true}, elements are read column by column; if {@code false}, row by row
     * @return a new ByteStream containing all elements from the two-dimensional array
     */
    public static ByteStream flatten(final byte[][] a, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (!vertically) {
            return Stream.of(a).flatMapToByte(flatMapper);
        }

        long n = 0;

        for (final byte[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final ByteIterator iter = new ByteIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public byte nextByte() {
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
     * Returns a ByteStream whose elements are all the elements from the input two-dimensional array, flattened either
     * in row-major order (vertically = false) or column-major order (vertically = true).
     * If rows have different lengths, the valueForAlignment is used to pad shorter rows.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[][] array = {{1, 2}, {3}};
     * ByteStream.flatten(array, (byte)0, false).toArray();   // [1, 2, 3, 0] (row by row with padding)
     * ByteStream.flatten(array, (byte)0, true).toArray();    // [1, 3, 2, 0] (column by column with padding)
     * }</pre>
     *
     * @param a the two-dimensional array to flatten
     * @param valueForAlignment the element to append so each row/column has the same number of elements
     * @param vertically if {@code true}, elements are read column by column; if {@code false}, row by row
     * @return a new ByteStream containing all elements from the two-dimensional array with alignment padding
     */
    public static ByteStream flatten(final byte[][] a, final byte valueForAlignment, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (final byte[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = (long) rows * cols;
        ByteIterator iter = null;

        if (vertically) {
            iter = new ByteIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public byte nextByte() {
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
            iter = new ByteIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public byte nextByte() {
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
     * Returns a ByteStream whose elements are all the elements from the input three-dimensional array, flattened in row-major order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[][][] array = {{{1, 2}, {3}}, {{4, 5}}};
     * ByteStream.flatten(array).toArray();   // [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param a the three-dimensional array to flatten
     * @return a new ByteStream containing all elements from the three-dimensional array
     */
    public static ByteStream flatten(final byte[][][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToByte(flattMapper);
    }

    /**
     * Returns a ByteStream whose elements are the values from startInclusive (inclusive)
     * to endExclusive (exclusive) by an incremental step of 1.
     *
     * <p>An empty stream is returned if startInclusive is greater than or equal to endExclusive.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.range((byte)1, (byte)5).toArray();   // [1, 2, 3, 4]
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endExclusive the exclusive upper bound
     * @return a new ByteStream consisting of values from startInclusive (inclusive) to endExclusive (exclusive)
     */
    public static ByteStream range(final byte startInclusive, final byte endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private byte next = startInclusive;
            private int cnt = endExclusive - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public byte nextByte() {
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
                next += (byte) n;
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public byte[] toArray() {
                final byte[] result = new byte[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a ByteStream whose elements are the values from startInclusive (inclusive)
     * to endExclusive (exclusive) by the specified incremental step.
     *
     * <p>An empty stream is returned if startInclusive is equal to endExclusive or if the range
     * and step have opposing signs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.range((byte)0, (byte)10, (byte)2).toArray();    // [0, 2, 4, 6, 8]
     * ByteStream.range((byte)10, (byte)0, (byte)-2).toArray();   // [10, 8, 6, 4, 2]
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endExclusive the exclusive upper bound
     * @param by the incremental step
     * @return a new ByteStream consisting of values from startInclusive (inclusive) to endExclusive (exclusive) by the specified step
     * @throws IllegalArgumentException if {@code by} is zero
     */
    public static ByteStream range(final byte startInclusive, final byte endExclusive, final byte by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private byte next = startInclusive;
            private int cnt = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public byte nextByte() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                final byte result = next;
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
                next += (byte) (n * by);
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public byte[] toArray() {
                final byte[] result = new byte[cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a ByteStream whose elements are the values from startInclusive (inclusive)
     * to endInclusive (inclusive) by an incremental step of 1.
     *
     * <p>An empty stream is returned if startInclusive is greater than endInclusive.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.rangeClosed((byte)1, (byte)5).toArray();   // [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endInclusive the inclusive upper bound
     * @return a new ByteStream consisting of values from startInclusive (inclusive) to endInclusive (inclusive)
     */
    public static ByteStream rangeClosed(final byte startInclusive, final byte endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private byte next = startInclusive;
            private int cnt = endInclusive - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public byte nextByte() {
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
                next += (byte) n;
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public byte[] toArray() {
                final byte[] result = new byte[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a ByteStream whose elements are the values from startInclusive (inclusive)
     * to endInclusive (inclusive) by the specified incremental step.
     *
     * <p>An empty stream is returned if the range and step have opposing signs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.rangeClosed((byte)0, (byte)10, (byte)2).toArray();    // [0, 2, 4, 6, 8, 10]
     * ByteStream.rangeClosed((byte)10, (byte)0, (byte)-2).toArray();   // [10, 8, 6, 4, 2, 0]
     * }</pre>
     *
     * @param startInclusive the (inclusive) initial value
     * @param endInclusive the inclusive upper bound
     * @param by the incremental step
     * @return a new ByteStream consisting of values from startInclusive (inclusive) to endInclusive (inclusive) by the specified step
     * @throws IllegalArgumentException if {@code by} is zero
     */
    public static ByteStream rangeClosed(final byte startInclusive, final byte endInclusive, final byte by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        } else if (endInclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private byte next = startInclusive;
            private int cnt = (endInclusive - startInclusive) / by + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public byte nextByte() {
                if (cnt <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt--;
                final byte result = next;
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
                next += (byte) (n * by);
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public byte[] toArray() {
                final byte[] result = new byte[cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a sequential ByteStream where each element is equal to the supplied value, repeated n times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.repeat((byte)7, 3).toArray();   // [7, 7, 7]
     * }</pre>
     *
     * @param element the element to repeat
     * @param n the number of times to repeat the element
     * @return a new ByteStream consisting of the element repeated n times
     * @throws IllegalArgumentException if n is negative
     */
    public static ByteStream repeat(final byte element, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return empty();
        } else if (n < 10) {
            return of(Array.repeat(element, (int) n));
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public byte nextByte() {
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
            public byte[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final byte[] result = new byte[(int) cnt];

                Arrays.fill(result, element);

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns an infinite sequential unordered {@code ByteStream} where each element is a randomly
     * generated {@code byte}. The random values are uniformly distributed over the full signed
     * byte range: {@code Byte.MIN_VALUE} (-128) to {@code Byte.MAX_VALUE} (127), inclusive.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.random().limit(5).forEach(System.out::println);   // prints 5 random bytes
     *
     * // Count positive bytes among 1000 random samples
     * long positiveCount = ByteStream.random().limit(1000).filter(b -> b > 0).count();
     * }</pre>
     *
     * @return an infinite {@code ByteStream} of random {@code byte} values uniformly distributed
     *         over the range {@code [Byte.MIN_VALUE, Byte.MAX_VALUE]}
     * @see #generate(ByteSupplier)
     */
    public static ByteStream random() {
        final int bound = Byte.MAX_VALUE - Byte.MIN_VALUE + 1;

        return generate(() -> (byte) (RAND.nextInt(bound) + Byte.MIN_VALUE));
    }

    /**
     * Creates a stream that iterates using the given <i>hasNext</i> and <i>next</i> suppliers.
     * The stream continues producing elements as long as the {@code hasNext} supplier returns {@code true}.
     * After that supplier first returns {@code false}, the stream remains exhausted and the supplier
     * is not invoked again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Iterate over bytes from an external source
     * byte[] src = {1, 2, 3, 4, 5};
     * int[] idx = {0};
     * ByteStream.iterate(() -> idx[0] < src.length, () -> src[idx[0]++])
     *     .toArray();   // [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param next a ByteSupplier that provides the next byte in the iteration
     * @return a ByteStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>next</i> is null
     * @see Stream#iterate(BooleanSupplier, Supplier)
     */
    public static ByteStream iterate(final BooleanSupplier hasNext, final ByteSupplier next) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorByteStream(new ByteIteratorEx() {
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
            public byte nextByte() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return next.getAsByte();
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
     * MutableByte count = MutableByte.of((byte)0);
     * ByteStream.iterate((byte)1, () -> count.incrementAndGet() < 5, x -> (byte)(x * 2))
     *     .toArray();   // [1, 2, 4, 8]
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param f a function to apply to the previous element to generate the next element
     * @return a ByteStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>f</i> is null
     * @see Stream#iterate(Object, BooleanSupplier, UnaryOperator)
     */
    public static ByteStream iterate(final byte init, final BooleanSupplier hasNext, final ByteUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorByteStream(new ByteIteratorEx() {
            private byte cur = 0;
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
            public byte nextByte() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsByte(cur);
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
     * ByteStream.iterate((byte)1, x -> x < 10, x -> (byte)(x * 2)).toArray();   // [1, 2, 4, 8]
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext determines if the returned stream has next by hasNext.test(init) for the first time and hasNext.test(f.apply(previous)) for remaining.
     * @param f a function to apply to the previous element to generate the next element
     * @return a ByteStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>hasNext</i> or <i>f</i> is null
     * @see Stream#iterate(Object, Predicate, UnaryOperator)
     */
    public static ByteStream iterate(final byte init, final BytePredicate hasNext, final ByteUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorByteStream(new ByteIteratorEx() {
            private byte cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsByte(cur));
                    }

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public byte nextByte() {
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.iterate((byte)1, x -> (byte)(x * 2)).limit(5).toArray();   // [1, 2, 4, 8, 16]
     * }</pre>
     *
     * @param init the initial value
     * @param f a function to apply to the previous element to generate the next element
     * @return an infinite ByteStream of elements generated by the iteration
     * @throws IllegalArgumentException if <i>f</i> is null
     * @see Stream#iterate(Object, UnaryOperator)
     */
    public static ByteStream iterate(final byte init, final ByteUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(f);

        return new IteratorByteStream(new ByteIteratorEx() {
            private byte cur = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public byte nextByte() {
                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsByte(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Generates an infinite ByteStream using the provided ByteSupplier.
     * The supplier is used to generate each element of the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream.generate(() -> (byte)42).limit(3).toArray();   // [42, 42, 42]
     * }</pre>
     *
     * @param s the ByteSupplier that provides the elements of the stream
     * @return an infinite ByteStream generated by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#generate(Supplier)
     */
    public static ByteStream generate(final ByteSupplier s) throws IllegalArgumentException {
        N.checkArgNotNull(s);

        return new IteratorByteStream(new ByteIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public byte nextByte() {
                return s.getAsByte();
            }
        });
    }

    /**
     * Concatenates multiple arrays of bytes into a single ByteStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] arr1 = {1, 2};
     * byte[] arr2 = {3, 4};
     * ByteStream.concat(arr1, arr2).toArray();   // [1, 2, 3, 4]
     * }</pre>
     *
     * @param a the arrays of bytes to concatenate
     * @return a ByteStream containing all the bytes from the input arrays
     * @see Stream#concat(Object[][])
     */
    public static ByteStream concat(final byte[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Arrays.asList(a));
    }

    /**
     * Concatenates multiple ByteIterators into a single ByteStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator iter1 = ByteIterator.of((byte)1, (byte)2);
     * ByteIterator iter2 = ByteIterator.of((byte)3, (byte)4);
     * ByteStream.concat(iter1, iter2).toArray();   // [1, 2, 3, 4]
     * }</pre>
     *
     * @param a the ByteIterators to concatenate
     * @return a ByteStream containing all the bytes from the input ByteIterators in order
     * @see Stream#concat(Iterator[])
     */
    public static ByteStream concat(final ByteIterator... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concatIterators(Array.asList(a));
    }

    /**
     * Concatenates multiple ByteStreams into a single ByteStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream s1 = ByteStream.of((byte)1, (byte)2);
     * ByteStream s2 = ByteStream.of((byte)3, (byte)4);
     * ByteStream.concat(s1, s2).toArray();   // [1, 2, 3, 4]
     * }</pre>
     *
     * @param a the ByteStreams to concatenate
     * @return a ByteStream containing all the bytes from the input ByteStreams in order
     * @see Stream#concat(Stream[])
     */
    public static ByteStream concat(final ByteStream... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a list of byte arrays into a single ByteStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<byte[]> list = Arrays.asList(new byte[]{1, 2}, new byte[]{3, 4});
     * ByteStream.concat(list).toArray();   // [1, 2, 3, 4]
     * }</pre>
     *
     * @param c the list of byte arrays to concatenate
     * @return a ByteStream containing all the bytes from the input arrays in order
     * @see Stream#concat(Object[][])
     */
    @Beta
    public static ByteStream concat(final List<byte[]> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return of(new ByteIteratorEx() {
            private final Iterator<byte[]> iter = c.iterator();
            private byte[] cur;
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
            public byte nextByte() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        });
    }

    /**
     * Concatenates a collection of ByteStream into a single ByteStream.
     * The collection's membership and encounter order are snapshotted when this method is called.
     * Closing the returned stream closes every snapshotted input stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<ByteStream> streams = Arrays.asList(
     *     ByteStream.of((byte)1, (byte)2),
     *     ByteStream.of((byte)3, (byte)4)
     * );
     * ByteStream.concat(streams).toArray();   // [1, 2, 3, 4]
     * }</pre>
     *
     * @param streams the collection of ByteStream to concatenate; {@code null} elements are treated as empty streams
     * @return a ByteStream containing all the bytes from the input collection of ByteStream
     * @see Stream#concat(Collection)
     */
    public static ByteStream concat(final Collection<? extends ByteStream> streams) {
        if (N.isEmpty(streams)) {
            return empty();
        }

        final List<? extends ByteStream> sources = new ArrayList<>(streams);

        return new IteratorByteStream(new ByteIteratorEx() { //NOSONAR
            private final Iterator<? extends ByteStream> iterators = sources.iterator();
            private ByteStream cur;
            private ByteIterator iter;

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
            public byte nextByte() {
                if ((iter == null || !iter.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextByte();
            }
        }).onClose(newCloseHandler(sources));
    }

    /**
     * Concatenates a collection of ByteIterator into a single ByteStream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<ByteIterator> iterators = Arrays.asList(
     *     ByteIterator.of((byte)1, (byte)2),
     *     ByteIterator.of((byte)3, (byte)4)
     * );
     * ByteStream.concatIterators(iterators).toArray();   // [1, 2, 3, 4]
     * }</pre>
     *
     * @param byteIterators the collection of ByteIterator to concatenate
     * @return a ByteStream containing all the bytes from the input collection of ByteIterator
     * @see Stream#concatIterators(Collection)
     */
    @Beta
    public static ByteStream concatIterators(final Collection<? extends ByteIterator> byteIterators) {
        if (N.isEmpty(byteIterators)) {
            return empty();
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private final Iterator<? extends ByteIterator> iter = byteIterators.iterator();
            private ByteIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public byte nextByte() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextByte();
            }
        });
    }

    /**
     * Zips two byte arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two arrays by applying
     * the specified function to each pair of values at the same position.
     *
     * <p>The resulting stream ends when either array runs out of values, even if the other
     * array has remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] a = {1, 2, 3};
     * byte[] b = {10, 20, 30};
     * ByteStream.zip(a, b, (x, y) -> (byte)(x + y))
     *           .toByteList();   // returns [11, 22, 33]
     * }</pre>
     *
     * @param a the first byte array (may be {@code null} or empty — yields an empty stream)
     * @param b the second byte array (may be {@code null} or empty — yields an empty stream)
     * @param zipFunction the function to combine elements from both arrays; must be non-null
     *        if both input arrays are non-empty
     * @return a stream of combined values, or an empty stream if either input array is
     *         {@code null} or empty
     * @see Stream#zip(Object[], Object[], BiFunction)
     */
    public static ByteStream zip(final byte[] a, final byte[] b, final ByteBinaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b)) {
            return empty();
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public byte nextByte() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsByte(a[cursor], b[cursor++]);
            }
        });
    }

    /**
     * Zips three byte arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three arrays by applying
     * the specified function to each triple of values at the same position.
     *
     * <p>The resulting stream ends when any array runs out of values, even if the other
     * arrays have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] a = {1, 2, 3};
     * byte[] b = {10, 20, 30};
     * byte[] c = {100, 100, 100};
     * ByteStream.zip(a, b, c, (x, y, z) -> (byte)(x + y - z))
     *           .toByteList();   // returns [-89, -78, -67]
     * }</pre>
     *
     * @param a the first byte array (may be {@code null} or empty — yields an empty stream)
     * @param b the second byte array (may be {@code null} or empty — yields an empty stream)
     * @param c the third byte array (may be {@code null} or empty — yields an empty stream)
     * @param zipFunction the function to combine elements from all three arrays; must be non-null
     *        if all input arrays are non-empty
     * @return a stream of combined values, or an empty stream if any input array is
     *         {@code null} or empty
     * @see Stream#zip(Object[], Object[], Object[], TriFunction)
     */
    public static ByteStream zip(final byte[] a, final byte[] b, final byte[] c, final ByteTernaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b) || N.isEmpty(c)) {
            return empty();
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public byte nextByte() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.applyAsByte(a[cursor], b[cursor], c[cursor++]);
            }
        });
    }

    /**
     * Zips two byte iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two iterators by applying
     * the specified function to each pair of values.
     *
     * <p>The resulting stream ends when either iterator runs out of values, even if the other
     * iterator has remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator a = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * ByteIterator b = ByteIterator.of((byte)10, (byte)20, (byte)30);
     * ByteStream.zip(a, b, (x, y) -> (byte)(x + y))
     *           .toByteList();   // returns [11, 22, 33]
     * }</pre>
     *
     * @param a the first ByteIterator. Can be {@code null} (treated as empty)
     * @param b the second ByteIterator. Can be {@code null} (treated as empty)
     * @param zipFunction the function to combine elements from both iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, BiFunction)
     */
    public static ByteStream zip(final ByteIterator a, final ByteIterator b, final ByteBinaryOperator zipFunction) {
        return new IteratorByteStream(new ByteIteratorEx() {
            private final ByteIterator iterA = a == null ? ByteIterator.empty() : a;
            private final ByteIterator iterB = b == null ? ByteIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public byte nextByte() {
                return zipFunction.applyAsByte(iterA.nextByte(), iterB.nextByte());
            }
        });
    }

    /**
     * Zips three byte iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three iterators by applying
     * the specified function to each triple of values.
     *
     * <p>The resulting stream ends when any iterator runs out of values, even if the other
     * iterators have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator a = ByteIterator.of((byte)1, (byte)2, (byte)3);
     * ByteIterator b = ByteIterator.of((byte)10, (byte)20, (byte)30);
     * ByteIterator c = ByteIterator.of((byte)100, (byte)100, (byte)100);
     * ByteStream.zip(a, b, c, (x, y, z) -> (byte)(x + y - z))
     *           .toByteList();   // returns [-89, -78, -67]
     * }</pre>
     *
     * @param a the first ByteIterator. Can be {@code null} (treated as empty)
     * @param b the second ByteIterator. Can be {@code null} (treated as empty)
     * @param c the third ByteIterator. Can be {@code null} (treated as empty)
     * @param zipFunction the function to combine elements from all three iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Iterator, TriFunction)
     */
    public static ByteStream zip(final ByteIterator a, final ByteIterator b, final ByteIterator c, final ByteTernaryOperator zipFunction) {
        return new IteratorByteStream(new ByteIteratorEx() {
            private final ByteIterator iterA = a == null ? ByteIterator.empty() : a;
            private final ByteIterator iterB = b == null ? ByteIterator.empty() : b;
            private final ByteIterator iterC = c == null ? ByteIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public byte nextByte() {
                return zipFunction.applyAsByte(iterA.nextByte(), iterB.nextByte(), iterC.nextByte());
            }
        });
    }

    /**
     * Zips two byte streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two streams by applying
     * the specified function to each pair of values.
     *
     * <p>The resulting stream ends when either stream runs out of values, even if the other
     * stream has remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream a = ByteStream.of((byte)1, (byte)2, (byte)3);
     * ByteStream b = ByteStream.of((byte)10, (byte)20, (byte)30);
     * ByteStream.zip(a, b, (x, y) -> (byte)(x + y))
     *           .toByteList();   // returns [11, 22, 33]
     * }</pre>
     *
     * @param a the first ByteStream
     * @param b the second ByteStream
     * @param zipFunction the function to combine elements from both streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, BiFunction)
     */
    public static ByteStream zip(final ByteStream a, final ByteStream b, final ByteBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three byte streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three streams by applying
     * the specified function to each triple of values.
     *
     * <p>The resulting stream ends when any stream runs out of values, even if the other
     * streams have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream a = ByteStream.of((byte)1, (byte)2, (byte)3);
     * ByteStream b = ByteStream.of((byte)10, (byte)20, (byte)30);
     * ByteStream c = ByteStream.of((byte)100, (byte)100, (byte)100);
     * ByteStream.zip(a, b, c, (x, y, z) -> (byte)(x + y - z))
     *           .toByteList();   // returns [-89, -78, -67]
     * }</pre>
     *
     * @param a the first ByteStream
     * @param b the second ByteStream
     * @param c the third ByteStream
     * @param zipFunction the function to combine elements from all three streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Stream, TriFunction)
     */
    public static ByteStream zip(final ByteStream a, final ByteStream b, final ByteStream c, final ByteTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), zipFunction).onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple byte streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from multiple streams by applying
     * the specified function to each array of values at the same position.
     *
     * <p>The resulting stream ends when any stream runs out of values, even if the other
     * streams have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<ByteStream> streams = Arrays.asList(
     *     ByteStream.of((byte)1, (byte)2),
     *     ByteStream.of((byte)10, (byte)20),
     *     ByteStream.of((byte)100, (byte)100)
     * );
     * ByteStream.zip(streams, args -> (byte)(args[0] + args[1] - args[2]))
     *           .toByteList();   // returns [-89, -78]
     * }</pre>
     *
     * @param streams the collection of ByteStream instances to zip; its contents are snapshotted, and {@code null} streams are treated as empty
     * @param zipFunction the function to combine elements from all streams. Must be {@code non-null}.
     * @return a stream of combined values. Empty if the collection is {@code null} or empty
     * @see Stream#zip(Collection, Function)
     */
    public static ByteStream zip(final Collection<? extends ByteStream> streams, final ByteNFunction<Byte> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, zipFunction).mapToByte(ToByteFunction.UNBOX);
    }

    /**
     * Zips two byte arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two arrays by applying
     * the specified function to each pair of values at the same position.
     *
     * <p>The resulting stream ends when all arrays run out of values. If one array runs out
     * before the others, the specified default value is used for the exhausted array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] a = {1, 2, 3, 4};
     * byte[] b = {10, 20};
     * ByteStream.zip(a, b, (byte)0, (byte)0, (x, y) -> (byte)(x + y))
     *           .toByteList();   // returns [11, 22, 3, 4]
     * }</pre>
     *
     * @param a the first byte array
     * @param b the second byte array
     * @param valueForNoneA the default value to use if the first array is shorter
     * @param valueForNoneB the default value to use if the second array is shorter
     * @param zipFunction the function to combine elements from both arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object, Object, BiFunction)
     */
    public static ByteStream zip(final byte[] a, final byte[] b, final byte valueForNoneA, final byte valueForNoneB, final ByteBinaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b)) {
            return empty();
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private byte ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public byte nextByte() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsByte(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips three byte arrays into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three arrays by applying
     * the specified function to each triple of values at the same position.
     *
     * <p>The resulting stream ends when all arrays run out of values. If one array runs out
     * before the others, the specified default value is used for the exhausted array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] a = {1, 2, 3, 4};
     * byte[] b = {10, 20};
     * byte[] c = {100};
     * ByteStream.zip(a, b, c, (byte)0, (byte)0, (byte)0, (x, y, z) -> (byte)(x + y + z))
     *           .toByteList();   // returns [111, 22, 3, 4]
     * }</pre>
     *
     * @param a the first byte array
     * @param b the second byte array
     * @param c the third byte array
     * @param valueForNoneA the default value to use if the first array is shorter
     * @param valueForNoneB the default value to use if the second array is shorter
     * @param valueForNoneC the default value to use if the third array is shorter
     * @param zipFunction the function to combine elements from all three arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object[], Object, Object, Object, TriFunction)
     */
    public static ByteStream zip(final byte[] a, final byte[] b, final byte[] c, final byte valueForNoneA, final byte valueForNoneB, final byte valueForNoneC,
            final ByteTernaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b) && N.isEmpty(c)) {
            return empty();
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private byte ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public byte nextByte() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsByte(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
                        cursor < cLen ? c[cursor] : valueForNoneC);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips two byte iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two iterators by applying
     * the specified function to each pair of values.
     *
     * <p>The resulting stream ends when all iterators run out of values. If one iterator runs out
     * before the others, the specified default value is used for the exhausted iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator a = ByteIterator.of((byte)1, (byte)2, (byte)3, (byte)4);
     * ByteIterator b = ByteIterator.of((byte)10, (byte)20);
     * ByteStream.zip(a, b, (byte)0, (byte)0, (x, y) -> (byte)(x + y))
     *           .toByteList();   // returns [11, 22, 3, 4]
     * }</pre>
     *
     * @param a the first ByteIterator, may be {@code null} (treated as empty)
     * @param b the second ByteIterator, may be {@code null} (treated as empty)
     * @param valueForNoneA the default value to use if the first iterator is shorter
     * @param valueForNoneB the default value to use if the second iterator is shorter
     * @param zipFunction the function to combine elements from both iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Object, Object, BiFunction)
     */
    public static ByteStream zip(final ByteIterator a, final ByteIterator b, final byte valueForNoneA, final byte valueForNoneB,
            final ByteBinaryOperator zipFunction) {
        return new IteratorByteStream(new ByteIteratorEx() {
            private final ByteIterator iterA = a == null ? ByteIterator.empty() : a;
            private final ByteIterator iterB = b == null ? ByteIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public byte nextByte() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsByte(iterA.nextByte(), iterB.hasNext() ? iterB.nextByte() : valueForNoneB);
                } else {
                    return zipFunction.applyAsByte(valueForNoneA, iterB.nextByte());
                }
            }
        });
    }

    /**
     * Zips three byte iterators into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three iterators by applying
     * the specified function to each triple of values.
     *
     * <p>The resulting stream ends when all iterators run out of values. If one iterator runs out
     * before the others, the specified default value is used for the exhausted iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator a = ByteIterator.of((byte)1, (byte)2, (byte)3, (byte)4);
     * ByteIterator b = ByteIterator.of((byte)10, (byte)20);
     * ByteIterator c = ByteIterator.of((byte)100);
     * ByteStream.zip(a, b, c, (byte)0, (byte)0, (byte)0, (x, y, z) -> (byte)(x + y + z))
     *           .toByteList();   // returns [111, 22, 3, 4]
     * }</pre>
     *
     * @param a the first ByteIterator, may be {@code null} (treated as empty)
     * @param b the second ByteIterator, may be {@code null} (treated as empty)
     * @param c the third ByteIterator, may be {@code null} (treated as empty)
     * @param valueForNoneA the default value to use if the first iterator is shorter
     * @param valueForNoneB the default value to use if the second iterator is shorter
     * @param valueForNoneC the default value to use if the third iterator is shorter
     * @param zipFunction the function to combine elements from all three iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Iterator, Object, Object, Object, TriFunction)
     */
    public static ByteStream zip(final ByteIterator a, final ByteIterator b, final ByteIterator c, final byte valueForNoneA, final byte valueForNoneB,
            final byte valueForNoneC, final ByteTernaryOperator zipFunction) {
        return new IteratorByteStream(new ByteIteratorEx() {
            private final ByteIterator iterA = a == null ? ByteIterator.empty() : a;
            private final ByteIterator iterB = b == null ? ByteIterator.empty() : b;
            private final ByteIterator iterC = c == null ? ByteIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public byte nextByte() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsByte(iterA.nextByte(), iterB.hasNext() ? iterB.nextByte() : valueForNoneB,
                            iterC.hasNext() ? iterC.nextByte() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.applyAsByte(valueForNoneA, iterB.nextByte(), iterC.hasNext() ? iterC.nextByte() : valueForNoneC);
                } else {
                    return zipFunction.applyAsByte(valueForNoneA, valueForNoneB, iterC.nextByte());
                }
            }
        });
    }

    /**
     * Zips two byte streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from two streams by applying
     * the specified function to each pair of values.
     *
     * <p>The resulting stream ends when all streams run out of values. If one stream runs out
     * before the others, the specified default value is used for the exhausted stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream a = ByteStream.of((byte)1, (byte)2, (byte)3, (byte)4);
     * ByteStream b = ByteStream.of((byte)10, (byte)20);
     * ByteStream.zip(a, b, (byte)0, (byte)0, (x, y) -> (byte)(x + y))
     *           .toByteList();   // returns [11, 22, 3, 4]
     * }</pre>
     *
     * @param a the first ByteStream
     * @param b the second ByteStream
     * @param valueForNoneA the default value to use if the first stream is shorter
     * @param valueForNoneB the default value to use if the second stream is shorter
     * @param zipFunction the function to combine elements from both streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Object, Object, BiFunction)
     */
    public static ByteStream zip(final ByteStream a, final ByteStream b, final byte valueForNoneA, final byte valueForNoneB,
            final ByteBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three byte streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from three streams by applying
     * the specified function to each triple of values.
     *
     * <p>The resulting stream ends when all streams run out of values. If one stream runs out
     * before the others, the specified default value is used for the exhausted stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream a = ByteStream.of((byte)1, (byte)2, (byte)3, (byte)4);
     * ByteStream b = ByteStream.of((byte)10, (byte)20);
     * ByteStream c = ByteStream.of((byte)100);
     * ByteStream.zip(a, b, c, (byte)0, (byte)0, (byte)0, (x, y, z) -> (byte)(x + y + z))
     *           .toByteList();   // returns [111, 22, 3, 4]
     * }</pre>
     *
     * @param a the first ByteStream
     * @param b the second ByteStream
     * @param c the third ByteStream
     * @param valueForNoneA the default value to use if the first stream is shorter
     * @param valueForNoneB the default value to use if the second stream is shorter
     * @param valueForNoneC the default value to use if the third stream is shorter
     * @param zipFunction the function to combine elements from all three streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Stream, Object, Object, Object, TriFunction)
     */
    public static ByteStream zip(final ByteStream a, final ByteStream b, final ByteStream c, final byte valueForNoneA, final byte valueForNoneB,
            final byte valueForNoneC, final ByteTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple byte streams into a single stream until all of them run out of values.
     * This is a static factory method that combines elements from multiple streams by applying
     * the specified function to each array of values at the same position.
     *
     * <p>The resulting stream ends when all streams run out of values. If one stream runs out
     * before the others, the specified default value is used for the exhausted stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<ByteStream> streams = Arrays.asList(
     *     ByteStream.of((byte)1, (byte)2, (byte)3),
     *     ByteStream.of((byte)10, (byte)20),
     *     ByteStream.of((byte)100)
     * );
     * byte[] defaults = {(byte)0, (byte)0, (byte)0};
     * ByteStream.zip(streams, defaults, args -> (byte)(args[0] + args[1] + args[2]))
     *           .toByteList();   // returns [111, 22, 3]
     * }</pre>
     *
     * @param streams the collection of ByteStream instances to zip; its contents are snapshotted, and {@code null} streams are treated as empty
     * @param valuesForNone the default values to use for exhausted streams, must have the same size as the streams collection
     * @param zipFunction the function to combine elements from all streams. Must be {@code non-null}.
     * @return a stream of combined values. Empty if the collection is {@code null} or empty
     * @throws IllegalArgumentException if the size of {@code valuesForNone} doesn't match the size of the streams collection
     * @see Stream#zip(Collection, List, Function)
     */
    public static ByteStream zip(final Collection<? extends ByteStream> streams, final byte[] valuesForNone, final ByteNFunction<Byte> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, valuesForNone, zipFunction).mapToByte(ToByteFunction.UNBOX);
    }

    /**
     * Merges two byte arrays into a single ByteStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] a = {1, 3, 5, 7};
     * byte[] b = {2, 4, 6};
     * ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // [1, 2, 3, 4, 5, 6, 7]
     * }</pre>
     *
     * @param a the first byte array
     * @param b the second byte array
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ByteStream containing the merged elements from the two input arrays
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Object[], Object[], BiFunction)
     */
    public static ByteStream merge(final byte[] a, final byte[] b, final ByteBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return new IteratorByteStream(new ByteIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public byte nextByte() {
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
     * Merges three byte arrays into a single ByteStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the three arrays.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] a = {1, 4, 7};
     * byte[] b = {2, 5, 8};
     * byte[] c = {3, 6, 9};
     * ByteStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * }</pre>
     *
     * @param a the first byte array
     * @param b the second byte array
     * @param c the third byte array
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ByteStream containing the merged elements from the three input arrays
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Object[], Object[], Object[], BiFunction)
     */
    public static ByteStream merge(final byte[] a, final byte[] b, final byte[] c, final ByteBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), ByteStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * Merges two ByteIterators into a single ByteStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator a = ByteIterator.of((byte)1, (byte)3, (byte)5);
     * ByteIterator b = ByteIterator.of((byte)2, (byte)4, (byte)6);
     * ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @param a the first ByteIterator
     * @param b the second ByteIterator
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ByteStream containing the merged elements from the two input iterators
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Iterator, Iterator, BiFunction)
     */
    public static ByteStream merge(final ByteIterator a, final ByteIterator b, final ByteBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return new IteratorByteStream(new ByteIteratorEx() {
            private final ByteIterator iterA = a == null ? ByteIterator.empty() : a;
            private final ByteIterator iterB = b == null ? ByteIterator.empty() : b;
            private byte nextA = 0;
            private byte nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public byte nextByte() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.nextByte())) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextByte()), nextB) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextByte()), (nextB = iterB.nextByte())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.nextByte();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.nextByte();
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     * Merges three ByteIterators into a single ByteStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the three iterators.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteIterator a = ByteIterator.of((byte)1, (byte)4, (byte)7);
     * ByteIterator b = ByteIterator.of((byte)2, (byte)5, (byte)8);
     * ByteIterator c = ByteIterator.of((byte)3, (byte)6, (byte)9);
     * ByteStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * }</pre>
     *
     * @param a the first ByteIterator
     * @param b the second ByteIterator
     * @param c the third ByteIterator
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ByteStream containing the merged elements from the three input iterators
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Iterator, Iterator, Iterator, BiFunction)
     */
    public static ByteStream merge(final ByteIterator a, final ByteIterator b, final ByteIterator c, final ByteBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * Merges two ByteStreams into a single ByteStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the two streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream a = ByteStream.of((byte)1, (byte)3, (byte)5, (byte)7);
     * ByteStream b = ByteStream.of((byte)2, (byte)4, (byte)6);
     * ByteStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // [1, 2, 3, 4, 5, 6, 7]
     * }</pre>
     *
     * @param a the first ByteStream
     * @param b the second ByteStream
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ByteStream containing the merged elements from the two input streams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Stream, Stream, BiFunction)
     */
    public static ByteStream merge(final ByteStream a, final ByteStream b, final ByteBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        return merge(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(a, b));
    }

    /**
     * Merges three ByteStreams into a single ByteStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the three streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteStream a = ByteStream.of((byte)1, (byte)4, (byte)7);
     * ByteStream b = ByteStream.of((byte)2, (byte)5, (byte)8);
     * ByteStream c = ByteStream.of((byte)3, (byte)6, (byte)9);
     * ByteStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * }</pre>
     *
     * @param a the first ByteStream
     * @param b the second ByteStream
     * @param c the third ByteStream
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ByteStream containing the merged elements from the three input streams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Stream, Stream, Stream, BiFunction)
     */
    public static ByteStream merge(final ByteStream a, final ByteStream b, final ByteStream c, final ByteBiFunction<MergeResult> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * Merges a collection of ByteStream into a single ByteStream based on the provided nextSelector function.
     * The nextSelector function determines which element to take next from the multiple streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<ByteStream> streams = Arrays.asList(
     *     ByteStream.of((byte)1, (byte)4, (byte)7),
     *     ByteStream.of((byte)2, (byte)5, (byte)8),
     *     ByteStream.of((byte)3, (byte)6, (byte)9)
     * );
     * ByteStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .toArray();   // [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * }</pre>
     *
     * @param streams the collection of ByteStream instances to merge; a {@code null} collection and {@code null} elements are treated as empty
     * @param nextSelector a function to determine which element should be selected as the next element. Must not be {@code null}.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a ByteStream containing the merged elements from the input ByteStreams
     * @throws IllegalArgumentException if {@code nextSelector} is {@code null}
     * @see Stream#merge(Collection, BiFunction)
     */
    public static ByteStream merge(final Collection<? extends ByteStream> streams, final ByteBiFunction<MergeResult> nextSelector) {
        N.checkArgNotNull(nextSelector, cs.nextSelector);

        if (N.isEmpty(streams)) {
            return empty();
        } else if (streams.size() == 1) {
            final ByteStream stream = streams.iterator().next();
            return stream == null ? empty() : stream;
        } else if (streams.size() == 2) {
            final Iterator<? extends ByteStream> iter = streams.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends ByteStream> iter = streams.iterator();
        ByteStream result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * Abstract base class for extended ByteStream implementations.
     * This class provides the basic structure for custom ByteStream implementations.
     */
    public abstract static class ByteStreamEx extends ByteStream {
        private ByteStreamEx(final boolean sorted, final Collection<LocalRunnable> closeHandlers) { //NOSONAR
            super(sorted, closeHandlers);
        }
    }
}
