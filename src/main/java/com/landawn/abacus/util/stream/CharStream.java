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
import java.io.Reader;
import java.nio.CharBuffer;
import java.security.SecureRandom;
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
import com.landawn.abacus.annotation.Immutable;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiConsumers;
import com.landawn.abacus.util.Fn.FC;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.function.CharTernaryOperator;
import com.landawn.abacus.util.function.CharToIntFunction;
import com.landawn.abacus.util.function.CharTriPredicate;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.function.ObjCharConsumer;
import com.landawn.abacus.util.function.ToCharFunction;

/**
 * A specialized stream implementation for processing sequences of primitive {@code char} values
 * (Unicode code units in the range {@code 0} ({@code Character.MIN_VALUE}) to {@code 65535} ({@code Character.MAX_VALUE}))
 * with functional-style operations.
 * This abstract class extends {@link StreamBase} and provides comprehensive character-specific operations including
 * filtering, mapping, reduction, and collection operations optimized for char data types.
 *
 * <p>CharStream represents a sequence of primitive {@code char} elements — each element is a UTF-16 code unit
 * with an unsigned value in the range {@code 0} ({@code Character.MIN_VALUE}) to {@code 65535}
 * ({@code Character.MAX_VALUE}) — supporting sequential and parallel aggregate operations.
 * It provides a more efficient alternative to generic {@link Stream} when working specifically with character
 * values, avoiding boxing/unboxing overhead and offering character-specific utility methods for text processing.
 * Note that supplementary characters (code points above U+FFFF) are represented as surrogate pairs and require
 * two {@code char} elements in the stream.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Strongly typed for character operations, preventing ClassCastException</li>
 *   <li><b>Performance:</b> Optimized for char primitives, avoiding boxing overhead</li>
 *   <li><b>Text Processing:</b> Specialized methods for string and character manipulation</li>
 *   <li><b>I/O Integration:</b> Direct support for file reading and character stream processing</li>
 *   <li><b>Parallel Support:</b> Efficient parallel processing capabilities for large character datasets</li>
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
 *   <li><b>Text Processing:</b> Parsing, filtering, and transforming textual data</li>
 *   <li><b>File I/O:</b> Reading and processing text files character by character</li>
 *   <li><b>String Manipulation:</b> Advanced string operations using stream pipelines</li>
 *   <li><b>Character Analysis:</b> Statistical analysis of character frequencies and patterns</li>
 *   <li><b>Data Validation:</b> Validating character sequences and formats</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic character stream operations
 * CharStream.of("Hello World")
 *     .filter(c -> c != ' ')         // removes spaces
 *     .map(Character::toUpperCase)   // maps to uppercase
 *     .toArray();                    // ['H','E','L','L','O','W','O','R','L','D']
 *
 * // File processing with character streams
 * try (CharStream chars = CharStream.of(new File("text.txt"))) {
 *     chars.filter(Character::isLetter)  // keeps only letters
 *          .map(Character::toLowerCase)  // maps to lowercase
 *          .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
 *          .toString(); // collects to string
 * }                     // closes automatically
 *
 * // Statistical operations on characters
 * CharSummaryStatistics stats = CharStream.of("Programming")
 *     .filter(Character::isLetter)   // keeps only letters
 *     .summaryStatistics();          // gets min, max, count statistics
 *
 * // Parallel processing for large text data
 * CharStream.of(largeTextContent)
 *     .parallel()                      // switches to parallel processing
 *     .filter(Character::isUpperCase)  // filters uppercase letters
 *     .mapToInt(c -> c)                // maps to int stream
 *     .average();                      // gets average ASCII value
 *
 * // Advanced text operations
 * String result = Stream.of("Hello", "World")
 *     .flatMapToChar(s -> CharStream.of(s))   // maps to individual chars
 *     .filter(c -> c != 'l')                  // removes 'l' characters
 *     .mapToObj(String::valueOf)              // maps each char to string
 *     .collect(Collectors.joining());         // collects back to string
 * }</pre>
 *
 * <p><b>Character-Specific Operations:</b>
 * <ul>
 *   <li>{@code filter(CharPredicate)} - Filter characters based on conditions</li>
 *   <li>{@code map(CharUnaryOperator)} - Transform characters</li>
 *   <li>{@code flatMap(CharFunction)} - Flatten mapped sub-streams into a single stream</li>
 *   <li>{@code asIntStream()} - Convert to IntStream for Unicode code-unit processing</li>
 *   <li>{@code boxed()} - Convert to Stream&lt;Character&gt;</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li>Use CharStream instead of {@code Stream<Character>} to avoid boxing overhead</li>
 *   <li>Parallel processing benefits large text datasets (typically &gt; 10,000 characters)</li>
 *   <li>Sequential processing is more efficient for small text and simple operations</li>
 *   <li>Lazy evaluation means intermediate operations are not executed until terminal operations</li>
 * </ul>
 *
 * @see StreamBase
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see Stream
 * @see CharIterator
 * @see CharList
 * @see CharSummaryStatistics
 * @see Character
 * @see String
 *
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/package-summary.html">Java Stream API</a>
 * @see <a href="https://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel streams</a>
 */
@Immutable
@LazyEvaluation
public abstract class CharStream extends StreamBase<Character, char[], CharPredicate, CharConsumer, OptionalChar, IndexedChar, CharIterator, CharStream> {

    static final Random RAND = new SecureRandom();

    CharStream(final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
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
     * CharStream.of('a', 'b', 'c', 'd', 'e')
     *       .filter(x -> x > 'c')
     *       .toCharList();   // returns ['d', 'e']
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
     * @return a new stream consisting of the elements that match the given predicate
     * @see Stream#filter(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract CharStream filter(final CharPredicate predicate);

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
     * CharStream.of('a', 'b', 'c', 'd', 'e', 'b', 'a')
     *       .takeWhile(x -> x < 'd')
     *       .toArray();   // returns ['a', 'b', 'c']
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
     * @return a new stream consisting of elements from this stream until an element is encountered that doesn't match the predicate
     * @see Stream#takeWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract CharStream takeWhile(final CharPredicate predicate);

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
     * CharStream.of('a', 'b', 'c', 'd', 'e', 'b', 'a')
     *       .dropWhile(x -> x < 'd')
     *       .toArray();   // returns ['d', 'e', 'b', 'a']
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
     * @see Stream#dropWhile(Predicate)
     */
    @ParallelSupported
    @IntermediateOp
    @Override
    public abstract CharStream dropWhile(final CharPredicate predicate);

    /**
     * Returns a CharStream consisting of the results of applying the given
     * function to the elements of this stream. This is an intermediate operation
     * that transforms each char element using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('a', 'b', 'c')
     *       .map(c -> Character.toUpperCase(c))
     *       .toArray();   // ['A', 'B', 'C']
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param mapper a non-interfering, stateless function that transforms each element from char to char
     * @return a new CharStream consisting of the results of applying the mapper function to the elements of this stream
     * @see Stream#map(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream map(CharUnaryOperator mapper);

    /**
     * Returns an IntStream consisting of the results of applying the
     * given function to the elements of this stream. This is an intermediate operation
     * that transforms each char element to an int value using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert characters to their ASCII values
     * CharStream.of('A', 'B', 'C')
     *       .mapToInt(c -> (int) c)
     *       .toArray();   // [65, 66, 67]
     *
     * // Convert digits to their numeric values
     * CharStream.of('1', '2', '3')
     *       .mapToInt(c -> c - '0')
     *       .toArray();   // [1, 2, 3]
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param mapper a non-interfering, stateless function that transforms each element from char to int
     * @return a new IntStream consisting of the results of applying the mapper function to each element
     * @see #map(CharUnaryOperator)
     * @see #mapToObj(CharFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream mapToInt(CharToIntFunction mapper);

    /**
     * Returns an object-valued Stream consisting of the results of
     * applying the given function to the elements of this stream.
     * This is an intermediate operation that transforms each char element to an object of type T
     * using the provided mapper function.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert characters to strings
     * CharStream.of('a', 'b', 'c')
     *       .mapToObj(String::valueOf)
     *       .toList();   // ["a", "b", "c"]
     *
     * // Create objects from characters
     * CharStream.of('A', 'B', 'C')
     *       .mapToObj(c -> "Letter: " + c)
     *       .toList();   // ["Letter: A", "Letter: B", "Letter: C"]
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element from char to T
     * @return a new Stream of objects resulting from applying the mapper function to each element
     * @see #map(CharUnaryOperator)
     * @see #mapToInt(CharToIntFunction)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> mapToObj(CharFunction<? extends T> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each character to itself and its lowercase version
     * CharStream.of('A', 'B', 'C')
     *     .flatMap(c -> CharStream.of(c, Character.toLowerCase(c)))
     *     .forEach(System.out::print);   // prints AaBbCc
     *
     * // Duplicate each element
     * CharStream.of('x', 'y', 'z')
     *     .flatMap(c -> CharStream.of(c, c))
     *     .toCharList();   // returns ['x', 'x', 'y', 'y', 'z', 'z']
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param mapper a non-interfering, stateless function that transforms each element to a CharStream
     * @return a new {@link CharStream} consisting of the flattened contents of the mapped streams
     * @see Stream#flatMap(Function)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream flatMap(CharFunction<? extends CharStream> mapper);

    // public abstract CharStream flatmap(CharFunction<CharIterator> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of a mapped {@link java.util.Collection Collection} produced by applying the provided mapping function to each element.
     *
     * <p><b>Naming Convention:</b></p>
     * <p>This library uses specific naming for different {@code flatMap} variants:</p>
     * <ul>
     *   <li>{@link #flatMap(CharFunction) flatMap} (uppercase 'M') - transforms each element into a {@link CharStream}.</li>
     *   <li>{@link #flatmap(CharFunction) flatmap} (this method, lowercase 'm') - transforms each element into a {@link java.util.Collection Collection} of {@link Character}.</li>
     *   <li>{@link #flatMapArray(CharFunction) flatMapArray} - transforms each element into a {@code char[]}.</li>
     * </ul>
     *
     * <p>Each element is transformed into a collection, and all resulting collections are flattened
     * into a single {@code CharStream}. If the mapper returns {@code null} or an empty collection
     * for a given element, that element contributes no values to the resulting stream.
     *
     * <p><b>Null elements within a collection</b> returned by the mapper are <em>not</em> propagated as
     * exceptions. Each {@code null} {@link Character} entry is replaced with the primitive default value
     * {@code (char) 0} (a.k.a. {@code '\0'}) so that this operation never throws
     * {@link NullPointerException} when unboxing.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('a', 'b')
     *           .flatmap(c -> Arrays.asList(c, Character.toUpperCase(c)))
     *           .toArray();   // ['a', 'A', 'b', 'B']
     *
     * // null elements in a returned collection become (char) 0
     * CharStream.of('a')
     *           .flatmap(c -> Arrays.asList((Character) null, 'z'))
     *           .toArray();   // [(char) 0, 'z']
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param mapper a non-interfering, stateless function that transforms each element from char to {@code Collection<Character>}
     * @return a new {@code CharStream} consisting of the flattened contents of the collections produced by the mapper
     * @see #flatMap(CharFunction)
     * @see #flatMapArray(CharFunction)
     * @see Stream#flatmap(java.util.function.Function)
     */
    // @ai-ignore flatmap/flatMap naming - intentional: flatMap maps to CharStream, flatmap maps to Collection<Character>, flatMapArray maps to char[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream flatmap(CharFunction<? extends Collection<Character>> mapper); //NOSONAR

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with the contents
     * of the char array produced by applying the provided mapping function to each element.
     *
     * <p>This operation is stateless and can be parallelized if the stream supports parallel processing.
     * The mapper function should be non-interfering and stateless for correct behavior in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Expand each character to array of multiples
     * CharStream.of('a', 'b', 'c')
     *     .flatMapArray(c -> new char[] { c, Character.toUpperCase(c) })
     *     .toCharList();   // returns ['a', 'A', 'b', 'B', 'c', 'C']
     *
     * // Duplicate each digit
     * CharStream.of('1', '2', '3')
     *     .flatMapArray(c -> new char[] { c, c })
     *     .forEach(System.out::print);   // prints 112233
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param mapper a non-interfering, stateless function that transforms each element to a char array
     * @return a new {@code CharStream} consisting of the flattened contents of the arrays produced by the mapper
     * @see #flatMap(CharFunction)
     * @see #flatMapToInt(CharFunction)
     * @see #flatMapToObj(CharFunction)
     */
    // @ai-ignore flatMapArray/flatMap naming - intentional: flatMap maps to CharStream, flatMapArray maps to char[]. Do not suggest renaming.
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream flatMapArray(CharFunction<char[]> mapper); //NOSONAR

    /**
     * Returns an IntStream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each char to its ASCII value and lowercase ASCII value
     * CharStream.of('A', 'B', 'C')
     *     .flatMapToInt(c -> IntStream.of((int) c, (int) Character.toLowerCase(c)))
     *     .toIntList();   // returns [65, 97, 66, 98, 67, 99]
     *
     * // Generate int sequences based on each character
     * CharStream.of('a', 'b')
     *     .flatMapToInt(c -> IntStream.range(0, c - 'a' + 1))
     *     .toIntList();   // returns [0, 0, 1]
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param mapper a non-interfering, stateless function that transforms each element to an IntStream
     * @return a new {@link IntStream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract IntStream flatMapToInt(CharFunction<? extends IntStream> mapper);

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each char to uppercase and lowercase strings
     * CharStream.of('A', 'B', 'C')
     *     .flatMapToObj(c -> Stream.of(String.valueOf(c), String.valueOf(c).toLowerCase()))
     *     .toList();   // returns ["A", "a", "B", "b", "C", "c"]
     *
     * // Generate multiple objects from each character
     * CharStream.of('x', 'y')
     *     .flatMapToObj(c -> Stream.of("Char: " + c, "Code: " + (int) c))
     *     .toList();   // returns ["Char: x", "Code: 120", "Char: y", "Code: 121"]
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element to a Stream
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the mapped streams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapToObj(CharFunction<? extends Stream<? extends T>> mapper);

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of
     * this stream with the contents of a collection produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each char to collection of strings
     * CharStream.of('A', 'B', 'C')
     *     .flatmapToObj(c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toLowerCase(c))))
     *     .toList();   // returns ["A", "a", "B", "b", "C", "c"]
     *
     * // Use lists to generate multiple values
     * CharStream.of('x', 'y')
     *     .flatmapToObj(c -> Arrays.asList(c, Character.toUpperCase(c)))
     *     .toList();   // returns ['x', 'X', 'y', 'Y']
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element to a Collection
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the collections produced by the mapper
     */
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatmapToObj(CharFunction<? extends Collection<? extends T>> mapper); //NOSONAR

    /**
     * Returns an object-valued Stream consisting of the results of replacing each element of
     * this stream with the contents of an array produced by applying
     * the provided mapping function to each element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert each char to array of strings
     * CharStream.of('A', 'B', 'C')
     *     .flatMapArrayToObj(c -> new String[] { String.valueOf(c), String.valueOf(Character.toLowerCase(c)) })
     *     .toList();   // returns ["A", "a", "B", "b", "C", "c"]
     *
     * // Generate multiple objects from each character
     * CharStream.of('x', 'y')
     *     .flatMapArrayToObj(c -> new Character[] { c, Character.toUpperCase(c) })
     *     .toList();   // returns ['x', 'X', 'y', 'Y']
     * }</pre>
     *
     * <p>This is an intermediate operation.
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
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that transforms each element to an array
     * @return a new object-valued {@link Stream} consisting of the flattened contents of the arrays produced by the mapper
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract <T> Stream<T> flatMapArrayToObj(CharFunction<T[]> mapper);

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of this stream,
     * and only including non-empty results. This is useful for operations that may produce optional values,
     * allowing you to filter out empty optionals in a single operation.
     *
     * <p>This is an intermediate operation.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Extract only digit characters
     * CharStream.of('a', 'b', '1', '2', 'c')
     *       .mapPartial(c -> Character.isDigit(c) ? OptionalChar.of(c) : OptionalChar.empty())
     *       .toArray();   // returns ['1', '2']
     *
     * // Convert uppercase letters only
     * CharStream.of('a', 'B', 'c', 'D', 'e')
     *       .mapPartial(c -> Character.isUpperCase(c) ? OptionalChar.of(Character.toLowerCase(c)) : OptionalChar.empty())
     *       .toArray();   // returns ['b', 'd']
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
     * @param mapper a non-interfering, stateless function that transforms each element from char to OptionalChar
     * @return a new {@code CharStream} containing only the char values from non-empty {@code OptionalChar} results
     * @see #filter(CharPredicate)
     * @see #map(CharUnaryOperator)
     */
    @Beta
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream mapPartial(CharFunction<OptionalChar> mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the first and last element of the ranges in this stream,
     * where elements in a range are determined by the sameRange predicate.
     * The first argument tested by sameRange is the first element of the current range, and the second argument is the next element to check.
     * If {@code true} is returned, the next element belongs to the same range as the first element.
     *
     * <p>For example, if this stream contains elements ['a', 'b', 'c', 'j', 'k', 't', 'u'] and the sameRange predicate returns true
     * when the difference between elements is less than 2, the stream will map ranges ['a','b'], ['c','c'], ['j','k'], ['t','u'].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group consecutive characters and return first of each group
     * CharStream.of('a', 'b', 'c', 'x', 'y', 'z')
     *       .rangeMap((first, next) -> next - first <= 2, (first, last) -> first)
     *       .toCharList();   // returns ['a', 'x'] (first of ranges 'a'-'c' and 'x'-'z')
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
    public abstract CharStream rangeMap(final CharBiPredicate sameRange, final CharBinaryOperator mapper);

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the first and last element of the ranges in this stream,
     * where elements in a range are determined by the sameRange predicate.
     * The first argument tested by sameRange is the first element of the current range, and the second argument is the next element to check.
     * If {@code true} is returned, the next element belongs to the same range as the first element.
     *
     * <p>For example, if this stream contains elements ['a', 'b', 'c', 'j', 'k', 't', 'u'] and the sameRange predicate returns true
     * when the difference between elements is less than 2, the stream will map ranges ['a','b'], ['c','c'], ['j','k'], ['t','u'] to objects
     * using the mapper function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map character ranges to String representations
     * Stream<String> ranges = CharStream.of('a', 'b', 'c', 'j', 'k', 't', 'u')
     *     .rangeMapToObj(
     *         (first, next) -> next - first < 2,
     *         (first, last) -> first + "-" + last);
     * ranges.toList();   // returns ["a-b", "c-c", "j-k", "t-u"]
     *
     * // Map consecutive digit characters to Integer ranges
     * Stream<String> digitRanges = CharStream.of('0', '1', '2', '5', '6', '9')
     *     .rangeMapToObj(
     *         (first, next) -> next - first <= 1,
     *         (first, last) -> first == last ? String.valueOf(first) : first + ".." + last);
     * digitRanges.toList();   // returns ["0..1", "2", "5..6", "9"]
     *
     * // Empty stream returns empty result
     * CharStream.empty()
     *     .rangeMapToObj((first, next) -> true, (first, last) -> "")
     *     .toList();   // returns []
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
    public abstract <T> Stream<T> rangeMapToObj(final CharBiPredicate sameRange, final CharBiFunction<? extends T> mapper);

    /**
     * Collapses consecutive elements in the stream into groups based on a predicate.
     * Elements for which the predicate returns {@code true} when applied to adjacent elements are grouped together into lists.
     *
     * <p>For example, if this stream contains elements ['a', 'b', 'e', 'f', 'g', 'j'] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * the resulting stream will contain [['a', 'b'], ['e', 'f', 'g'], ['j']].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group consecutive characters with small gaps
     * Stream<CharList> groups = CharStream.of('a', 'b', 'e', 'f', 'g', 'j')
     *     .collapse((last, next) -> next - last < 3);
     * groups.map(CharList::toString).toList();   // returns ["[a, b]", "[e, f, g]", "[j]"]
     *
     * // Group identical adjacent characters
     * Stream<CharList> runs = CharStream.of('a', 'a', 'b', 'b', 'b', 'c')
     *     .collapse((last, next) -> next == last);
     * runs.map(CharList::toString).toList();   // returns ["[a, a]", "[b, b, b]", "[c]"]
     *
     * // Empty stream returns empty result
     * CharStream.empty()
     *     .collapse((last, next) -> true)
     *     .toList();   // returns []
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
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group.
     *        The first parameter is the last(not the first) element of the current group, and the second parameter is the next element to check.
     * @return a stream of lists, each containing a sequence of consecutive elements that are collapsible with each other
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<CharList> collapse(final CharBiPredicate collapsible);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * When adjacent elements satisfy the collapsible predicate, they are merged using the provided merge function.
     *
     * <p>For example, if this stream contains elements ['a', 'b', 'e', 'f', 'g', 'j'] and the predicate
     * tests whether the difference between consecutive elements is less than 3,
     * and the merge function returns the latter element, the resulting stream will contain ['b', 'g', 'j'].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse consecutive characters, keeping the last of each group
     * CharStream.of('a', 'b', 'e', 'f', 'g', 'j')
     *     .collapse((last, next) -> next - last < 3, (a, b) -> b)
     *     .toArray();   // returns ['b', 'g', 'j']
     *
     * // Collapse consecutive characters, keeping the first of each group
     * CharStream.of('a', 'b', 'e', 'f', 'g', 'j')
     *     .collapse((last, next) -> next - last < 3, (a, b) -> a)
     *     .toArray();   // returns ['a', 'e', 'j']
     *
     * // Collapse identical adjacent characters by keeping the last
     * CharStream.of('a', 'a', 'b', 'b', 'b', 'c')
     *     .collapse((last, next) -> next == last, (a, b) -> b)
     *     .toArray();   // returns ['a', 'b', 'c']
     *
     * // Single-element stream
     * CharStream.of('x')
     *     .collapse((last, next) -> true, (a, b) -> b)
     *     .toArray();   // returns ['x']
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
     * @param collapsible a predicate that determines if two consecutive elements should be collapsed into the same group.
     *        The first parameter is the last(not the first) element of the current group, and the second parameter is the next element to check.
     * @param mergeFunction a function to merge two collapsible elements into one
     * @return a stream of merged elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate, BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream collapse(final CharBiPredicate collapsible, final CharBinaryOperator mergeFunction);

    /**
     * Collapses consecutive elements in the stream by applying a merge function when elements are collapsible.
     * When adjacent elements satisfy the collapsible predicate, they are merged using the provided merge function.
     * The collapsible predicate takes three elements: the first and last elements of the current group, and the next element in the stream.
     *
     * <p>For example, if this stream contains elements ['a', 'b', 'e', 'f', 'g', 'j'] and the predicate
     * tests whether the next element is within 2 of the first element of the current group,
     * and the merge function returns the latter element, the resulting stream will contain ['b', 'g', 'j'].
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collapse with tri-predicate considering first and last of group
     * CharStream.of('a', 'b', 'e', 'f', 'g', 'j')
     *     .collapse((first, last, next) -> next - first <= 2, (a, b) -> b)
     *     .toArray();   // returns ['b', 'g', 'j']
     *
     * // Collapse where third element must be within range of first element
     * CharStream.of('a', 'b', 'c', 'x', 'y', 'z')
     *     .collapse((first, last, next) -> next - first <= 2, (a, b) -> b)
     *     .toArray();   // returns ['c', 'z']
     *
     * // Empty stream returns empty result
     * CharStream.empty()
     *     .collapse((first, last, next) -> true, (a, b) -> b)
     *     .toArray();   // returns []
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
     * @param collapsible a predicate that determines if the next element from this stream should be collapsed with the first and last elements of current group
     *          The collapsible predicate takes three elements: the first and last elements of current group, and the next element to check.
     * @param mergeFunction a function to merge two collapsible elements into one
     * @return a stream of merged elements
     * @throws IllegalStateException if the stream is already closed
     * @see Stream#collapse(BiPredicate, BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream collapse(final CharTriPredicate collapsible, final CharBinaryOperator mergeFunction);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * <p>For example, given a stream of characters ['a', 'b', 'c'] and an accumulator that returns the greater character,
     * the output would be a stream of characters ['a', 'b', 'c'] (each element is the running maximum).
     *
     * <p>This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Running maximum of characters
     * CharStream.of('b', 'a', 'd', 'c')
     *     .scan((a, b) -> a > b ? a : b)
     *     .toArray();   // ['b', 'b', 'd', 'd']
     *
     * // Running sum of character code points (as char)
     * CharStream.of((char)1, (char)2, (char)3, (char)4)
     *     .scan((a, b) -> (char)(a + b))
     *     .toArray();   // [(char)1, (char)3, (char)6, (char)10]
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
     * @param accumulator a {@code CharBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code CharStream} consisting of the results of the scan operation on the elements of the original stream.
     *         Returns an empty stream if this stream is empty.
     * @see Stream#scan(BinaryOperator)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream scan(final CharBinaryOperator accumulator);

    /**
     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
     * The scan operation takes an initial value and a binary operator (the accumulator) and applies it cumulatively on the stream elements,
     * successively combining each element in order from the start to produce a stream of accumulated results.
     *
     * <p>For example, given a stream of code-point values [(char)1, (char)2, (char)3, (char)4], an initial value of
     * {@code (char)10}, and an accumulator that performs addition, the output would be
     * [(char)11, (char)13, (char)16, (char)20].
     *
     * <p>This is an intermediate operation.
     * This operation is sequential only, even when called on a parallel stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Running maximum starting from a seed
     * CharStream.of('b', 'a', 'd', 'c')
     *     .scan('z', (a, b) -> a > b ? a : b)
     *     .toArray();   // ['z', 'z', 'z', 'z']
     *
     * // Sum of code-point values with initial offset
     * CharStream.of((char)1, (char)2, (char)3)
     *     .scan((char)10, (a, b) -> (char)(a + b))
     *     .toArray();   // [(char)11, (char)13, (char)16]
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
     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
     *        It will be ignored if this stream is empty and won't be the first element of the returned stream.
     * @param accumulator a {@code CharBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code CharStream} consisting of the results of the scan operation on the elements of the original stream.
     *         Returns an empty stream if this stream is empty.
     * @see Stream#scan(Object, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream scan(final char init, final CharBinaryOperator accumulator);

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
     * CharStream.of((char)1, (char)2, (char)3)
     *     .scan((char)10, true, (a, b) -> (char)(a + b))
     *     .toArray();   // [(char)10, (char)11, (char)13, (char)16]
     *
     * // Exclude the initial value from the result
     * CharStream.of((char)1, (char)2, (char)3)
     *     .scan((char)10, false, (a, b) -> (char)(a + b))
     *     .toArray();   // [(char)11, (char)13, (char)16]
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
     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
     * @param initIncluded if {@code true}, the {@code init} value is included as the first element of the returned stream;
     *        if {@code false}, the scan starts from the first stream element combined with {@code init}, and {@code init} itself is not emitted.
     * @param accumulator a {@code CharBinaryOperator} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
     * @return a new {@code CharStream} consisting of the results of the scan operation on the elements of the original stream.
     *         If {@code initIncluded} is {@code true}, the stream always starts with {@code init} (even if this stream is empty).
     *         If {@code initIncluded} is {@code false}, an empty stream is returned when this stream is empty.
     * @see Stream#scan(Object, boolean, BiFunction)
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream scan(final char init, final boolean initIncluded, final CharBinaryOperator accumulator);

    /**
     * Returns a stream consisting of the specified elements followed by the elements of this stream.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('c', 'd').prepend('a', 'b').toArray();   // returns ['a', 'b', 'c', 'd']
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
     * @param a the elements to prepend to this stream
     * @return a new stream with the specified elements prepended
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream prepend(final char... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('a', 'b').append('c', 'd').toArray();   // returns ['a', 'b', 'c', 'd']
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
     * @param a the elements to append to this stream
     * @return a new stream with the specified elements appended
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream append(final char... a);

    /**
     * Returns a stream consisting of the elements of this stream with the specified elements appended if this stream is empty.
     * If this stream is not empty, returns this stream unchanged.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.empty().appendIfEmpty('x', 'y').toArray();   // returns ['x', 'y']
     * CharStream.of('a').appendIfEmpty('x', 'y').toArray();   // returns ['a']
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
     * @param a the elements to append if this stream is empty
     * @return this stream if not empty, otherwise a new stream containing the specified elements
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream appendIfEmpty(final char... a);

    /**
     * Returns a CharList containing all elements of this stream.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect characters from a string
     * CharList list1 = CharStream.of("Hello")
     *     .toCharList();
     * // Result: ['H', 'e', 'l', 'l', 'o']
     *
     * // Filter and collect
     * CharList list2 = CharStream.of("Hello World")
     *     .filter(Character::isUpperCase)
     *     .toCharList();
     * // Result: ['H', 'W']
     *
     * // Transform and collect
     * CharList list3 = CharStream.of("abc")
     *     .map(Character::toUpperCase)
     *     .toCharList();
     * // Result: ['A', 'B', 'C']
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
     * @return a CharList containing all elements of this stream
     */
    @SequentialOnly
    @TerminalOp
    public abstract CharList toCharList();

    /**
     * Returns a Map whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * an {@code IllegalStateException} is thrown when the collection operation is performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Map characters to their ASCII values and uppercase versions
     * Map<Character, Character> map1 = CharStream.of("abc")
     *     .toMap(c -> c, c -> Character.toUpperCase(c));
     * // Result: {'a'='A', 'b'='B', 'c'='C'}
     *
     * // Map digit characters to their numeric values
     * Map<Character, Integer> map2 = CharStream.of("123")
     *     .toMap(c -> c, c -> c - '0');
     * // Result: {'1'=1, '2'=2, '3'=3}
     *
     * // Throws IllegalStateException on duplicate keys
     * CharStream.of("aab")
     *     .toMap(c -> c, c -> Character.toUpperCase(c));   // throws on duplicate key 'a'
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
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
     * @param <E> the type of exception that the key mapping function may throw
     * @param <E2> the type of exception that the value mapping function may throw
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @return a Map whose keys and values are the result of applying mapping functions to the input elements
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.CharFunction<? extends K, E> keyMapper,
            Throwables.CharFunction<? extends V, E2> valueMapper) throws E, E2;

    /**
     * Returns a Map whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * an {@code IllegalStateException} is thrown when the collection operation is performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into a LinkedHashMap to preserve insertion order
     * LinkedHashMap<Character, String> map1 = CharStream.of("abc")
     *     .toMap(c -> c, c -> "Value-" + c, LinkedHashMap::new);
     * // Result: {'a'="Value-a", 'b'="Value-b", 'c'="Value-c"} in insertion order
     *
     * // Collect into a TreeMap for sorted keys
     * TreeMap<Character, Integer> map2 = CharStream.of("cab")
     *     .toMap(c -> c, c -> (int) c, TreeMap::new);
     * // Result: {'a'=97, 'b'=98, 'c'=99} in natural order
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
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
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception that the key mapping function may throw
     * @param <E2> the type of exception that the value mapping function may throw
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mapFactory a supplier providing a new empty Map into which the results will be inserted
     * @return a Map whose keys and values are the result of applying mapping functions to the input elements
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @throws IllegalStateException if duplicate keys are encountered
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.CharFunction<? extends K, E> keyMapper,
            Throwables.CharFunction<? extends V, E2> valueMapper, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Returns a Map whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the results are merged
     * using the provided merging function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count character occurrences using merge function
     * Map<Character, Integer> map1 = CharStream.of("aabbcc")
     *     .toMap(c -> c, c -> 1, Integer::sum);
     * // Result: {'a'=2, 'b'=2, 'c'=2}
     *
     * // Keep first value on duplicate keys
     * Map<Character, String> map2 = CharStream.of("abac")
     *     .toMap(c -> c, c -> "First-" + c, (v1, v2) -> v1);
     * // Result: {'a'="First-a", 'b'="First-b", 'c'="First-c"}
     *
     * // Keep last value on duplicate keys
     * Map<Character, String> map3 = CharStream.of("abac")
     *     .toMap(c -> c, c -> "Last-" + c, (v1, v2) -> v2);
     * // Result: {'a'="Last-a", 'b'="Last-b", 'c'="Last-c"}
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
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
     * @param <E> the type of exception that the key mapping function may throw
     * @param <E2> the type of exception that the value mapping function may throw
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @return a Map whose keys and values are the result of applying mapping functions to the input elements
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(Throwables.CharFunction<? extends K, E> keyMapper,
            Throwables.CharFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction) throws E, E2;

    /**
     * Returns a Map whose keys and values are the result of applying the provided
     * mapping functions to the input elements.
     *
     * <p>If the mapped keys contain duplicates (according to {@link Object#equals(Object)}),
     * the value mapping function is applied to each equal element, and the results are merged
     * using the provided merging function. The Map is created by a provided supplier function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count character occurrences in a LinkedHashMap
     * LinkedHashMap<Character, Integer> map1 = CharStream.of("aabbcc")
     *     .toMap(c -> c, c -> 1, Integer::sum, LinkedHashMap::new);
     * // Result: {'a'=2, 'b'=2, 'c'=2} in insertion order
     *
     * // Collect to TreeMap with merge function for sorted output
     * TreeMap<Character, String> map2 = CharStream.of("abacbd")
     *     .toMap(c -> c, c -> String.valueOf(c), (v1, v2) -> v1 + v2, TreeMap::new);
     * // Result: {'a'="aa", 'b'="bb", 'c'="c", 'd'="d"} in natural order
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
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
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception that the key mapping function may throw
     * @param <E2> the type of exception that the value mapping function may throw
     * @param keyMapper a mapping function to produce keys
     * @param valueMapper a mapping function to produce values
     * @param mergeFunction a merge function, used to resolve collisions between values associated with the same key
     * @param mapFactory a supplier providing a new empty Map into which the results will be inserted
     * @return a Map whose keys and values are the result of applying mapping functions to the input elements
     * @throws E if the key mapping function throws an exception
     * @throws E2 if the value mapping function throws an exception
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(Throwables.CharFunction<? extends K, E> keyMapper,
            Throwables.CharFunction<? extends V, E2> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) throws E, E2;

    /**
     * Groups the elements of this stream according to a classification function and
     * performs a reduction operation on the values associated with each key using
     * the specified downstream Collector.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group characters by type (letter vs digit)
     * Map<String, List<Character>> map1 = CharStream.of("a1b2c3")
     *     .groupTo(c -> Character.isDigit(c) ? "digit" : "letter", Collectors.toList());
     * // Result: {"letter"=['a','b','c'], "digit"=['1','2','3']}
     *
     * // Group and count characters by uppercase/lowercase
     * Map<String, Long> map2 = CharStream.of("AaBbCc")
     *     .groupTo(c -> Character.isUpperCase(c) ? "upper" : "lower", Collectors.counting());
     * // Result: {"upper"=3, "lower"=3}
     *
     * // Group characters by ASCII value range
     * Map<Integer, String> map3 = CharStream.of("abc123")
     *     .groupTo(c -> c / 10, Collectors.mapping(String::valueOf, Collectors.joining()));
     * // Result: {4="1", 5="23", 9="abc"}
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
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
     * @param <E> the type of exception that the classification function may throw
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a Collector implementing the downstream reduction
     * @return a Map containing the results of the group-by operation
     * @throws E if the classification function throws an exception
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, E extends Exception> Map<K, D> groupTo(Throwables.CharFunction<? extends K, E> keyMapper,
            final Collector<? super Character, ?, D> downstream) throws E;

    /**
     * Groups the elements of this stream according to a classification function and
     * performs a reduction operation on the values associated with each key using
     * the specified downstream Collector. The Map produced by the Collector is created
     * with the supplied factory function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group into LinkedHashMap to preserve insertion order
     * LinkedHashMap<String, List<Character>> map1 = CharStream.of("a1b2c3")
     *     .groupTo(c -> Character.isDigit(c) ? "digit" : "letter",
     *              Collectors.toList(),
     *              LinkedHashMap::new);
     * // Result: {"letter"=['a','b','c'], "digit"=['1','2','3']} in insertion order
     *
     * // Group into TreeMap for sorted keys
     * TreeMap<Character, Long> map2 = CharStream.of("aabbcc")
     *     .groupTo(c -> c, Collectors.counting(), TreeMap::new);
     * // Result: {'a'=2, 'b'=2, 'c'=2} in natural order
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
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
     * @param <E> the type of exception that the classification function may throw
     * @param keyMapper a classifier function mapping input elements to keys
     * @param downstream a Collector implementing the downstream reduction
     * @param mapFactory a supplier providing a new empty Map into which the results will be inserted
     * @return a Map containing the results of the group-by operation
     * @throws E if the classification function throws an exception
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <K, D, M extends Map<K, D>, E extends Exception> M groupTo(Throwables.CharFunction<? extends K, E> keyMapper,
            final Collector<? super Character, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E;

    /**
     * Performs a reduction on the elements of this stream, using the provided identity value and
     * an associative accumulation function, and returns the reduced value.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the maximum character
     * char max = CharStream.of("hello")
     *     .reduce('a', (a, b) -> a > b ? a : b);
     * // Result: 'o'
     *
     * // Find the minimum character
     * char min = CharStream.of("hello")
     *     .reduce('z', (a, b) -> a < b ? a : b);
     * // Result: 'e'
     *
     * // Sum ASCII values (with identity)
     * char sum = CharStream.of("abc")
     *     .reduce((char) 0, (a, b) -> (char) (a + b));
     * // Result: character with value 294 (97+98+99)
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the reduction
     * @see Stream#reduce(Object, BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract char reduce(char identity, CharBinaryOperator accumulator);

    /**
     * Performs a reduction on the elements of this stream, using an associative accumulation function,
     * and returns an OptionalChar describing the reduced value, if any.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the maximum character (returns empty for empty stream)
     * OptionalChar max = CharStream.of("hello")
     *     .reduce((a, b) -> a > b ? a : b);
     * // Result: OptionalChar['o']
     *
     * // Find the minimum character
     * OptionalChar min = CharStream.of("world")
     *     .reduce((a, b) -> a < b ? a : b);
     * // Result: OptionalChar['d']
     *
     * // Empty stream returns empty Optional
     * OptionalChar empty = CharStream.empty()
     *     .reduce((a, b) -> a > b ? a : b);
     * // Result: OptionalChar.empty()
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an OptionalChar describing the result of the reduction
     * @see Stream#reduce(BinaryOperator)
     */
    @ParallelSupported
    @TerminalOp
    public abstract OptionalChar reduce(CharBinaryOperator accumulator);

    /**
     * Performs a mutable reduction operation on the elements of this stream using a
     * Collector-like pattern with supplier, accumulator, and combiner functions.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect characters into a StringBuilder
     * String result1 = CharStream.of("hello")
     *     .collect(StringBuilder::new,
     *              StringBuilder::append,
     *              StringBuilder::append)
     *     .toString();
     * // Result: "hello"
     *
     * // Collect into a custom container
     * List<Character> list = CharStream.of("abc")
     *     .collect(ArrayList::new,
     *              List::add,
     *              List::addAll);
     * // Result: ['a', 'b', 'c']
     *
     * // Collect uppercase letters into a set
     * Set<Character> set = CharStream.of("AaBbCc")
     *     .filter(Character::isUpperCase)
     *     .collect(HashSet::new,
     *              Set::add,
     *              Set::addAll);
     * // Result: {'A', 'B', 'C'}
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container.
     *                 For a parallel execution, this function may be called multiple times
     *                 and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function that must fold an element into a result container
     * @param combiner an associative, non-interfering, stateless function that accepts two result containers and merges their contents,
     *                 which must be compatible with the accumulator function. The combiner function must fold the elements
     *                 from the second result container into the first result container.
     * @return the result of the reduction
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     * @see BiConsumers#ofAddAll()
     * @see BiConsumers#ofPutAll()
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjCharConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * Performs a mutable reduction operation on the elements of this stream using only
     * a supplier and an accumulator function. This is a simplified collect operation for
     * use when the result container type has a built-in combining operation.
     *
     * <p>Only call this method when the returned type {@code R} is one of these types:
     * {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}.
     * Otherwise, please call {@link #collect(Supplier, ObjCharConsumer, BiConsumer)}.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect into StringBuilder (has built-in combining)
     * String result1 = CharStream.of("world")
     *     .collect(StringBuilder::new, StringBuilder::append)
     *     .toString();
     * // Result: "world"
     *
     * // Collect into ArrayList
     * List<Character> list = CharStream.of("abc")
     *     .collect(ArrayList::new, List::add);
     * // Result: ['a', 'b', 'c']
     *
     * // Collect into CharList (optimized primitive list)
     * CharList charList = CharStream.of("xyz")
     *     .collect(CharList::new, CharList::add);
     * // Result: ['x', 'y', 'z']
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <R> the type of the mutable result container. Must be one of:
     *            {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}
     * @param supplier a function that creates a new mutable result container.
     *                 For a parallel execution, this function may be called multiple times
     *                 and must return a fresh value each time.
     * @param accumulator an associative, non-interfering, stateless function that must fold an element into a result container
     * @return the result of the reduction
     * @throws RuntimeException if this stream is parallel and the result type {@code R} is not one of:
     *         {@code Collection/Map/StringBuilder/Multiset/Multimap/BooleanList/IntList/.../DoubleList}
     *         (the default combiner cannot merge the per-thread containers); sequential streams perform no such check.
     * @see #collect(Supplier, ObjCharConsumer, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer)
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    @ParallelSupported
    @TerminalOp
    public abstract <R> R collect(Supplier<R> supplier, ObjCharConsumer<? super R> accumulator);

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
     * CharStream.of('a', 'b', 'c', 'd', 'e')
     *     .forEach(System.out::println);
     * // prints: a, b, c, d, e (each on a new line)
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the action may throw
     * @param action a non-interfering action to perform on the elements
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws E;

    /**
     * Performs an action for each element of this stream, passing the element's index
     * (0-based position in the stream) as well as the element itself to the action.
     *
     * <p>This is a terminal operation that closes the stream after execution.
     *
     * <p>The behavior of this operation is explicitly nondeterministic for parallel streams.
     * For parallel stream pipelines, this operation does not guarantee to respect the encounter
     * order of the stream, as doing so would sacrifice the benefit of parallelism.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('x', 'y', 'z')
     *     .forEachIndexed((index, value) ->
     *         System.out.println("Element at index " + index + ": " + value));
     * // prints:
     * // Element at index 0: x
     * // Element at index 1: y
     * // Element at index 2: z
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the action may throw
     * @param action a non-interfering action to perform on the elements, taking both index and element
     * @throws E if the action throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> void forEachIndexed(Throwables.IntCharConsumer<E> action) throws E;

    /**
     * Returns whether any elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result. If the stream is empty then {@code false} is returned
     * and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if any character is uppercase
     * boolean hasUpper = CharStream.of("Hello World")
     *     .anyMatch(Character::isUpperCase);   // returns true
     *
     * // Check if any character is a digit
     * boolean hasDigit = CharStream.of("abc")
     *     .anyMatch(Character::isDigit);   // returns false
     *
     * // Empty stream returns false
     * boolean result = CharStream.empty()
     *     .anyMatch(c -> c == 'a');   // returns false
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if any elements of the stream match the provided predicate, otherwise {@code false}
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean anyMatch(final Throwables.CharPredicate<E> predicate) throws E;

    /**
     * Returns whether all elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result. If the stream is empty then {@code true} is returned
     * and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check if all characters are lowercase
     * boolean allLower = CharStream.of("hello")
     *     .allMatch(Character::isLowerCase);   // returns true
     *
     * // Check if all characters are letters
     * boolean allLetters = CharStream.of("Hello123")
     *     .allMatch(Character::isLetter);   // returns false
     *
     * // Empty stream returns true
     * boolean result = CharStream.empty()
     *     .allMatch(c -> c == 'a');   // returns true
     *
     * // Validate all characters are within a range
     * boolean inRange = CharStream.of("abc")
     *     .allMatch(c -> c >= 'a' && c <= 'z');   // returns true
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if either all elements of the stream match the provided predicate or the stream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean allMatch(final Throwables.CharPredicate<E> predicate) throws E;

    /**
     * Returns whether no elements of this stream match the provided predicate.
     * May not evaluate the predicate on all elements if not necessary for
     * determining the result. If the stream is empty then {@code true} is returned
     * and the predicate is not evaluated.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Check that no characters are digits
     * boolean noDigits = CharStream.of("Hello")
     *     .noneMatch(Character::isDigit);   // returns true
     *
     * // Check that no characters are uppercase
     * boolean noUpper = CharStream.of("Hello World")
     *     .noneMatch(Character::isUpperCase);   // returns false (has 'H' and 'W')
     *
     * // Empty stream returns true
     * boolean result = CharStream.empty()
     *     .noneMatch(c -> c == 'a');   // returns true
     *
     * // Validate no special characters
     * boolean noSpecial = CharStream.of("abc123")
     *     .noneMatch(c -> !Character.isLetterOrDigit(c));   // returns true
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return {@code true} if either no elements of the stream match the provided predicate or the stream is empty, otherwise {@code false}
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> boolean noneMatch(final Throwables.CharPredicate<E> predicate) throws E;

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalChar}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalChar first = CharStream.of('a', 'b', 'c', 'd', 'e')
     *                                 .findFirst();   // returns OptionalChar.of('a')
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an {@code OptionalChar} containing the first element of the stream, or an empty {@code OptionalChar} if the stream is empty
     * @see #first()
     * @see #findAny()
     * @see #findFirst(Throwables.CharPredicate)
     * @see #findAny(Throwables.CharPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalChar findFirst() {
        return first();
    }

    /**
     * Returns the first element in the stream, if present, otherwise returns an empty {@code OptionalChar}.
     * This is a terminal operation that short-circuits on the first element.
     *
     * <p>This method is a deterministic alias of {@link #first()} and {@link #findFirst()}; despite the
     * JDK-style name it returns the FIRST element, not an arbitrary one, even for parallel streams.</p>
     *
     * <p>Note: This method is an alias for {@link #first()} to align with the standard Stream API naming conventions.
     * The current implementation always delegates to {@link #first()}, returning the first element in encounter order.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalChar any = CharStream.of('a', 'b', 'c', 'd', 'e')
     *                              .findAny();   // returns OptionalChar.of('a')
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @return an {@code OptionalChar} containing the first element of the stream, or an empty {@code OptionalChar} if the stream is empty
     * @see #first()
     * @see #findFirst()
     * @see #findFirst(Throwables.CharPredicate)
     * @see #findAny(Throwables.CharPredicate)
     */
    @ParallelSupported
    @TerminalOp
    public OptionalChar findAny() {
        return first();
    }

    /**
     * Returns an {@link OptionalChar} describing the first element of this stream that matches
     * the given predicate, or an empty {@code OptionalChar} if no such element exists.
     *
     * <p>If the stream is empty or no element matches the predicate, then an empty {@code OptionalChar} is returned.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalChar firstDigit = CharStream.of('a', 'b', '1', '2', 'c')
     *     .findFirst(Character::isDigit);   // OptionalChar['1']
     *
     * OptionalChar firstUpper = CharStream.of("helloWorld")
     *     .findFirst(Character::isUpperCase);   // OptionalChar['W']
     *
     * OptionalChar notFound = CharStream.of("hello")
     *     .findFirst(Character::isDigit);   // OptionalChar.empty()
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalChar} describing the first element that matches the predicate,
     *         or an empty {@code OptionalChar} if no such element exists
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalChar findFirst(final Throwables.CharPredicate<E> predicate) throws E;

    /**
     * Returns an {@link OptionalChar} describing some element of the stream that matches
     * the given predicate, or an empty {@code OptionalChar} if no such element exists.
     *
     * <p>This is a short-circuiting terminal operation.
     *
     * <p>The behavior of this operation is explicitly nondeterministic; it is
     * free to select any element in the stream that matches the predicate. This is to allow for maximal
     * performance in parallel operations; the cost is that multiple invocations
     * on the same source may not return the same result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalChar anyDigit = CharStream.of('a', 'b', '1', '2', 'c')
     *     .findAny(Character::isDigit);   // OptionalChar['1'] or OptionalChar['2'] in parallel
     *
     * OptionalChar anyUpper = CharStream.of("helloWorld")
     *     .findAny(Character::isUpperCase);   // OptionalChar['W']
     *
     * OptionalChar notFound = CharStream.of("hello")
     *     .findAny(Character::isDigit);   // OptionalChar.empty()
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalChar} describing some element that matches the predicate,
     *         or an empty {@code OptionalChar} if no such element exists
     * @throws E if the predicate throws an exception
     */
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalChar findAny(final Throwables.CharPredicate<E> predicate) throws E;

    /**
     * Returns an {@link OptionalChar} describing the last element of this stream that matches
     * the given predicate, or an empty {@code OptionalChar} if no such element exists.
     *
     * <p>Consider using: {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * OptionalChar lastDigit = CharStream.of('a', '1', 'b', '2', 'c')
     *     .findLast(Character::isDigit);   // OptionalChar['2']
     *
     * OptionalChar lastUpper = CharStream.of("helloWorldFoo")
     *     .findLast(Character::isUpperCase);   // OptionalChar['F']
     *
     * OptionalChar notFound = CharStream.of("hello")
     *     .findLast(Character::isDigit);   // OptionalChar.empty()
     * }</pre>
     *
     * <p><b>Operation characteristics:</b></p>
     * <table border="1">
     *   <caption>Operation characteristics</caption>
     *   <tr><th></th><th>Yes/No</th><th>Description</th></tr>
     *   <tr><td>{@code @TerminalOp}</td><td>Yes</td><td>Consumes the stream and produces a final result or side effect, triggering execution of the pipeline.</td></tr>
     *   <tr><td>{@code @IntermediateOp}</td><td>No</td><td>Returns a new stream and is evaluated lazily; the source is not consumed until a terminal operation runs.</td></tr>
     *   <tr><td>{@code @TerminalOpTriggered}</td><td>No</td><td>Internally consumes and buffers the elements before returning a new stream. The upstream stream may be closed.</td></tr>
     *   <tr><td>{@code @ParallelSupported}</td><td>Yes</td><td>May be executed on a parallelized stream (e.g. one created via {@code parallel()}).</td></tr>
     *   <tr><td>{@code @SequentialOnly}</td><td>No</td><td>Will always be executed sequentially, even in a parallel stream.</td></tr>
     *   <tr><td>Loads all elements into memory</td><td>No</td><td>Buffers all elements of this stream in memory in order to produce its result.</td></tr>
     * </table>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate that tests each element
     * @return an {@code OptionalChar} describing the last element that matches the predicate,
     *         or an empty {@code OptionalChar} if no such element exists
     * @throws E if the predicate throws an exception
     */
    @Beta
    @ParallelSupported
    @TerminalOp
    public abstract <E extends Exception> OptionalChar findLast(final Throwables.CharPredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalChar} describing the minimum element of this
     * stream, or an empty optional if this stream is empty. This is a special
     * case of a reduction and is equivalent to:
     * <pre>{@code
     *     return reduce((a, b) -> a <= b ? a : b);
     * }</pre>
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('e', 'b', 'h', 'a', 'i').min();   // returns OptionalChar['a']
     *
     * CharStream.empty().min();   // returns OptionalChar.empty()
     *
     * // Safe retrieval with default value
     * char minValue = CharStream.of('x', 'y', 'z').min().orElse('a');   // returns 'x'
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
     * @return an {@code OptionalChar} containing the minimum element of this
     *         stream, or an empty {@code OptionalChar} if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalChar min();

    /**
     * Returns an {@code OptionalChar} describing the maximum element of this
     * stream, or an empty optional if this stream is empty. This is a special
     * case of a reduction and is equivalent to:
     * <pre>{@code
     *     return reduce((a, b) -> a >= b ? a : b);
     * }</pre>
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('e', 'b', 'h', 'a', 'i').max();   // returns OptionalChar['i']
     *
     * CharStream.empty().max();   // returns OptionalChar.empty()
     *
     * // Safe retrieval with default value
     * char maxValue = CharStream.of('x', 'y', 'z').max().orElse('a');   // returns 'z'
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
     * @return an {@code OptionalChar} containing the maximum element of this
     *         stream, or an empty {@code OptionalChar} if the stream is empty
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalChar max();

    /**
     * Returns the k-th largest element in the stream.
     * If the stream contains fewer than k elements, an empty {@code OptionalChar} is returned.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the 2nd largest element
     * CharStream.of('j', 'z', 'p', 'b', 'x').kthLargest(2);   // returns OptionalChar['x']
     *
     * // Find the largest element (same as max)
     * CharStream.of('e', 'b', 'h', 'a').kthLargest(1);   // returns OptionalChar['h']
     *
     * // When k exceeds stream size
     * CharStream.of('a', 'b', 'c').kthLargest(5);   // returns OptionalChar.empty()
     * }</pre>
     *
     * <p><b>Note:</b> {@code CharStream} (like {@code ByteStream}) intentionally omits the {@code top(n)} /
     * {@code top(n, Comparator)} family that {@code ShortStream}/{@code IntStream} provide — for the small
     * {@code char} domain the n largest elements are easily obtained otherwise: use {@code sorted()}
     * (ascending) and take the tail, or {@code boxed().top(n)} on the resulting {@code Stream<Character>}.
     * This is by design.
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
     * @param k the position of the element to find (1-based, so k=1 returns the largest element,
     *          k=2 returns the second-largest, etc.)
     * @return an {@code OptionalChar} containing the k-th largest element, or an empty {@code OptionalChar}
     *         if the stream contains fewer than k elements
     * @throws IllegalArgumentException if k is less than 1
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalChar kthLargest(int k);

    /**
     * Returns the sum of the Unicode code-unit values of all elements in this stream as an {@code int}.
     * The values are accumulated in a {@code long} and then converted to {@code int}. For an empty
     * stream, {@code 0} is returned. This is a special case of a reduction.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Note:</b> if the total sum exceeds {@link Integer#MAX_VALUE} (possible for very large
     * streams), an {@link ArithmeticException} is thrown rather than silently overflowing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum of Unicode code-unit values of 'A' (65), 'B' (66), 'C' (67), 'D' (68)
     * int total = CharStream.of('A', 'B', 'C', 'D').sum();   // returns 266
     *
     * CharStream.empty().sum();   // returns 0
     *
     * // Sum of code-unit values of '1' (49), '2' (50), '3' (51)
     * int digitSum = CharStream.of("123".toCharArray()).sum();   // returns 150
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
     * @return the sum of Unicode code-unit values of elements in this stream as an {@code int},
     *         or {@code 0} if the stream is empty
     * @throws ArithmeticException if the sum overflows an {@code int}
     * @see #average()
     * @see #summaryStatistics()
     */
    @SequentialOnly
    @TerminalOp
    public abstract int sum();

    /**
     * Returns an {@code OptionalDouble} describing the arithmetic mean of the Unicode code-unit values
     * of elements of this stream, or an empty optional if this stream is empty.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Average of 'A'(65), 'B'(66), 'C'(67), 'D'(68) = 266/4 = 66.5
     * CharStream.of('A', 'B', 'C', 'D').average();   // returns OptionalDouble[66.5]
     *
     * // Average of 'a'(97), 'b'(98), 'c'(99) = 294/3 = 98.0
     * CharStream.of('a', 'b', 'c').average();   // returns OptionalDouble[98.0]
     *
     * CharStream.empty().average();   // returns OptionalDouble.empty()
     *
     * // Safe retrieval with default value
     * double avg = CharStream.of('x', 'y', 'z').average().orElse(0.0);   // returns 121.0
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
     * @return an {@code OptionalDouble} containing the arithmetic mean of the Unicode code-unit values,
     *         or an empty optional if the stream is empty
     * @see #sum()
     * @see #summaryStatistics()
     */
    @SequentialOnly
    @TerminalOp
    public abstract OptionalDouble average();

    /**
     * Returns statistics about the elements of this stream, including count, sum,
     * min, max, and average.
     *
     * <p>This is a terminal operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharSummaryStatistics stats = CharStream.of('A', 'B', 'C', 'D', 'E').summaryStatistics();
     * System.out.println("Count: " + stats.getCount());       // 5
     * System.out.println("Sum: " + stats.getSum());           // 335
     * System.out.println("Min: " + stats.getMin());           // 'A'
     * System.out.println("Max: " + stats.getMax());           // 'E'
     * System.out.println("Average: " + stats.getAverage());   // 67.0
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
     * @return a CharSummaryStatistics describing various summary data about the
     *         elements of this stream
     */
    @SequentialOnly
    @TerminalOp
    public abstract CharSummaryStatistics summaryStatistics();

    /**
     * Returns a pair consisting of CharSummaryStatistics for the elements of this stream,
     * and an Optional containing a map of percentiles if the stream is not empty.
     * To calculate the percentiles of the elements in the stream, all elements will be loaded into memory and sorted if not yet.
     *
     * <p>This is a terminal operation and can only be processed sequentially.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get both statistics and percentiles
     * Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> result =
     *     CharStream.of("Hello World")
     *         .summaryStatisticsAndPercentiles();
     *
     * CharSummaryStatistics stats = result.left();
     * System.out.println("Count: " + stats.getCount());
     * System.out.println("Min: " + stats.getMin());
     * System.out.println("Max: " + stats.getMax());
     *
     * // Access percentile values
     * result.right().ifPresent(percentiles -> {
     *     Character median = percentiles.get(Percentage.of(50));
     *     Character p25 = percentiles.get(Percentage.of(25));
     *     Character p75 = percentiles.get(Percentage.of(75));
     *     System.out.println("Median (50th percentile): " + median);
     *     System.out.println("25th percentile: " + p25);
     *     System.out.println("75th percentile: " + p75);
     * });
     *
     * // Empty stream returns empty percentiles
     * Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> emptyResult =
     *     CharStream.empty().summaryStatisticsAndPercentiles();
     * // emptyResult.right() is Optional.empty()
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
     * @return a Pair containing CharSummaryStatistics and an Optional map of percentile values.
     *         The map's keys are Percentage values and the values are the corresponding percentile values.
     *         The Optional is empty if the stream is empty.
     */
    @SequentialOnly
    @TerminalOp
    public abstract Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> summaryStatisticsAndPercentiles();

    /**
     * Merges this stream with another stream, selecting elements based on the provided selector function.
     * The selector function determines which stream's next element to choose when both streams have elements available.
     *
     * <p>This is an intermediate operation and the stream is sequential only.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('a', 'c', 'e')
     *       .mergeWith(CharStream.of('b', 'd', 'f'), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *       .toArray();   // returns ['a', 'b', 'c', 'd', 'e', 'f']
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
     * @param b the other stream to merge with
     * @param nextSelector a function to determine which element should be selected as the next element.
     *                     Takes elements from both streams and returns {@code MergeResult.TAKE_FIRST} to select
     *                     the element from this stream, or {@code MergeResult.TAKE_SECOND} to select from stream b
     * @return a new stream containing elements merged from both streams
     */
    @SequentialOnly
    @IntermediateOp
    public abstract CharStream mergeWith(final CharStream b, final CharBiFunction<MergeResult> nextSelector);

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
     * // Combine characters using max function
     * CharStream.of('a', 'b', 'c')
     *           .zipWith(CharStream.of('x', 'y', 'z', 'w'),
     *                    (a, b) -> a > b ? a : b)
     *           .toCharList();   // returns ['x', 'y', 'z'] (length of shorter stream)
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
     * @param b the CharStream to be combined with the current CharStream. Must be {@code non-null}.
     * @param zipFunction a CharBinaryOperator that determines the combination of elements in the combined CharStream. Must be {@code non-null}.
     * @return a new CharStream that is the result of combining the current CharStream with the given CharStream
     * @see #zipWith(CharStream, char, char, CharBinaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream zipWith(CharStream b, CharBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the shortest of the three streams.
     * If any stream is longer, its remaining elements are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Combine three streams selecting the maximum character
     * CharStream.of('a', 'b', 'c')
     *           .zipWith(CharStream.of('x', 'y'), CharStream.of('1', '2'),
     *                    (a, b, c) -> a > b ? (a > c ? a : c) : (b > c ? b : c))
     *           .toCharList();   // returns ['x', 'y'] (length of shortest stream)
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
     * @param b the second CharStream to be combined with the current CharStream. Will be closed along with this CharStream.
     * @param c the third CharStream to be combined with the current CharStream. Will be closed along with this CharStream.
     * @param zipFunction a CharTernaryOperator that determines the combination of elements in the combined CharStream. Must be {@code non-null}.
     * @return a new CharStream that is the result of combining the current CharStream with the given CharStreams
     * @see #zipWith(CharStream, CharStream, char, char, char, CharTernaryOperator)
     */
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream zipWith(CharStream b, CharStream c, CharTernaryOperator zipFunction);

    /**
     * Zips this stream with the given stream using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from two streams pairwise.
     *
     * <p>The resulting stream will have the length of the longer of the two streams.
     * Default values are used when one stream runs out of elements before the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Zip with default values when streams have different lengths
     * CharStream.of('a', 'b', 'c')
     *           .zipWith(CharStream.of('x'), '\0', '\0', (a, b) -> (char)(a + b))
     *           .toCharList();   // returns [(char)('a'+'x'), (char)('b'+'\0'), (char)('c'+'\0')]
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
     * @param b the CharStream to be combined with the current CharStream. Will be closed along with this CharStream.
     * @param valueForNoneA the default value to use for the current CharStream when it runs out of elements
     * @param valueForNoneB the default value to use for the given CharStream when it runs out of elements
     * @param zipFunction a CharBinaryOperator that determines the combination of elements in the combined CharStream. Must be {@code non-null}.
     * @return a new CharStream that is the result of combining the current CharStream with the given CharStream
     */
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream zipWith(CharStream b, char valueForNoneA, char valueForNoneB, CharBinaryOperator zipFunction);

    /**
     * Zips this stream with two other streams using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the longest of the three streams.
     * Default values are used when any stream runs out of elements before the others.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Zip three streams with default values for missing elements
     * CharStream.of('a', 'b', 'c')
     *           .zipWith(CharStream.of('x'), CharStream.of('1'),
     *                    '\0', '\0', '\0',
     *                    (a, b, c) -> (char)(a + b + c))
     *           .toCharList();   // returns [(char)('a'+'x'+'1'), (char)('b'+'\0'+'\0'), (char)('c'+'\0'+'\0')]
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
     * @param b the second CharStream to be combined with the current CharStream. Will be closed along with this CharStream.
     * @param c the third CharStream to be combined with the current CharStream. Will be closed along with this CharStream.
     * @param valueForNoneA the default value to use for the current CharStream when it runs out of elements
     * @param valueForNoneB the default value to use for the second CharStream when it runs out of elements
     * @param valueForNoneC the default value to use for the third CharStream when it runs out of elements
     * @param zipFunction a CharTernaryOperator that determines the combination of elements in the combined CharStream. Must be {@code non-null}.
     * @return a new CharStream that is the result of combining the current CharStream with the given CharStreams
     */
    @ParallelSupported
    @IntermediateOp
    public abstract CharStream zipWith(CharStream b, CharStream c, char valueForNoneA, char valueForNoneB, char valueForNoneC, CharTernaryOperator zipFunction);

    /**
     * Converts this CharStream to an IntStream.
     * Each char element is widened to an int value.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('a', 'b', 'c').asIntStream().toArray();   // returns [97, 98, 99]
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
     * @return an IntStream consisting of the elements of this stream converted to int
     */
    @SequentialOnly
    @IntermediateOp
    public abstract IntStream asIntStream();

    /**
     * Returns a Stream consisting of the elements of this stream, each boxed to a Character.
     *
     * <p>This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.of('a', 'b', 'c').boxed().toList();   // returns ['a', 'b', 'c']
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
     * @return a Stream consisting of the elements of this stream, each boxed to a Character
     */
    @SequentialOnly
    @IntermediateOp
    public abstract Stream<Character> boxed();

    abstract CharIteratorEx iteratorEx();

    // private static final CharStream EMPTY_STREAM = new ArrayCharStream(N.EMPTY_CHAR_ARRAY, true, null);

    /**
     * Returns an empty sequential {@code CharStream} with no elements.
     *
     * <p>This is a factory method that creates an immutable empty stream, which can be useful
     * as a starting point for stream operations or as a default return value. The returned stream
     * performs no operations and immediately completes any terminal operation with empty results.
     *
     * <p>Each call to this method returns a new empty stream instance. Like all streams,
     * the returned stream can only be consumed once; call this method again to obtain a fresh empty stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Return an empty stream as a default
     * CharStream getCharsOrEmpty(String str) {
     *     return str == null ? CharStream.empty() : CharStream.of(str);
     * }
     *
     * // Use as initial value for stream concatenation
     * CharStream result = CharStream.empty();
     * for (String s : strings) {
     *     result = CharStream.concat(result, CharStream.of(s));
     * }
     *
     * // Terminal operations on empty stream
     * CharStream.empty().count();       // returns 0
     * CharStream.empty().findFirst();   // returns OptionalChar.empty()
     * CharStream.empty().toArray();     // returns empty char array
     * }</pre>
     *
     * @return an empty sequential {@code CharStream}
     * @see #of(char...)
     * @see #ofNullable(Character)
     */
    public static CharStream empty() {
        return new ArrayCharStream(N.EMPTY_CHAR_ARRAY, true, null);
    }

    /**
     * Returns a CharStream that is lazily populated by an input supplier.
     * This is a static factory method that defers stream creation until the returned stream is first traversed or closed.
     *
     * <p>The supplier is memoized and invoked at most once. Closing the returned stream before traversal may still
     * invoke the supplier so the supplied stream can be closed.
     *
     * <p>Note: it's equivalent to {@code Stream.just(supplier).flatMapToChar(it -> it.get())}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Defer expensive stream creation until needed
     * CharStream deferred = CharStream.defer(() -> {
     *     System.out.println("Stream created");
     *     return CharStream.of("expensive computation result");
     * });
     * // "Stream created" is not printed until the stream is consumed or closed
     * deferred.forEach(System.out::print);   // prints "Stream created", then the result
     *
     * // Conditional stream creation
     * CharStream conditional = CharStream.defer(() ->
     *     System.currentTimeMillis() % 2 == 0
     *         ? CharStream.of("even")
     *         : CharStream.of("odd")
     * );
     * }</pre>
     *
     * @param supplier the supplier that provides the CharStream
     * @return a new CharStream supplied by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#defer(Supplier)
     */
    public static CharStream defer(final Supplier<CharStream> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Supplier<CharStream> s = Fn.memoize(supplier);
        return Stream.just(s).flatMapToChar(Supplier::get).onClose(newCloseHandler(s));
    }

    /**
     * Returns a sequential {@code CharStream} containing a single element if the specified value is non-null,
     * otherwise returns an empty stream.
     *
     * <p>This method provides null-safe stream creation for single {@code Character} values. It's particularly
     * useful when working with nullable {@code Character} objects that need to be safely converted to a stream
     * without explicit null checks. If the input is null, an empty stream is returned instead of throwing
     * a {@code NullPointerException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Handle potentially null Character
     * Character ch = getOptionalCharacter();  // null may be returned
     * CharStream.ofNullable(ch)
     *     .forEach(System.out::print);  // prints nothing if ch is null
     *
     * // Use in stream operations
     * CharStream.ofNullable(nullableChar)
     *     .map(Character::toUpperCase)
     *     .findFirst()
     *     .ifPresent(System.out::println);
     *
     * // Chaining with other streams
     * CharStream.concat(
     *     CharStream.of("hello"),
     *     CharStream.ofNullable(separatorChar),
     *     CharStream.of("world")
     * ).forEach(System.out::print);
     *
     * // Conditional processing
     * CharStream.ofNullable(optionalChar)
     *     .filter(c -> c >= 'a' && c <= 'z')
     *     .count();  // returns 0 or 1
     * }</pre>
     *
     * @param e the {@code Character} element to include in the stream, may be null
     * @return a {@code CharStream} containing the unboxed char value if the element is non-null,
     *         otherwise an empty {@code CharStream}
     * @see #empty()
     * @see #of(char...)
     * @see java.util.Optional#ofNullable(Object)
     */
    public static CharStream ofNullable(final Character e) {
        return e == null ? empty() : of(e);
    }

    /**
     * Returns a sequential {@code CharStream} whose elements are the specified char values.
     *
     * <p>This is a factory method that creates a stream from a varargs array of primitive char values.
     * The stream will contain all elements in the order they appear in the array. If the array is null
     * or empty, an empty stream is returned.
     *
     * <p>This method is optimized for primitive char values and avoids boxing overhead. It's the
     * most efficient way to create a {@code CharStream} from a known set of char values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from explicit char values
     * CharStream.of('a', 'b', 'c')
     *     .forEach(System.out::print);  // prints abc
     *
     * // Use with array reference
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * CharStream.of(chars)
     *     .map(Character::toUpperCase)
     *     .toArray();  // returns ['H', 'E', 'L', 'L', 'O']
     *
     * // Create single element stream
     * CharStream.of('x')
     *     .count();  // returns 1
     *
     * // Empty array produces empty stream
     * CharStream.of(new char[0])
     *     .findAny();  // returns OptionalChar.empty()
     *
     * // Chaining operations
     * long vowelCount = CharStream.of('a', 'e', 'i', 'o', 'u', 'b', 'c', 'd')
     *     .filter(c -> "aeiou".indexOf(c) >= 0)
     *     .count();  // returns 5
     * }</pre>
     *
     * @param a the char elements of the new stream (varargs or array)
     * @return a new sequential {@code CharStream} containing the specified elements,
     *         or an empty stream if the array is null or empty
     * @see #of(char[], int, int)
     * @see #of(CharSequence)
     * @see #empty()
     */
    public static CharStream of(final char... a) {
        return N.isEmpty(a) ? empty() : new ArrayCharStream(a);
    }

    /**
     * Returns a sequential {@code CharStream} whose elements are the specified values from the given array
     * between {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     *
     * <p>This factory method creates a stream from a sub-range of a char array, allowing you to process
     * only a portion of the array without creating a copy. The stream will contain elements from
     * {@code a[fromIndex]} up to but not including {@code a[toIndex]}.
     *
     * <p>If the array is null or empty and the indices are both zero (empty range), an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process a sub-range of an array
     * char[] chars = {'a', 'b', 'c', 'd', 'e', 'f'};
     * CharStream.of(chars, 2, 5)
     *     .forEach(System.out::print);  // prints cde
     *
     * // Skip first and last elements
     * char[] data = {'[', 'H', 'e', 'l', 'l', 'o', ']'};
     * String content = CharStream.of(data, 1, data.length - 1)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "Hello"
     *
     * // Process middle section
     * char[] buffer = "0123456789".toCharArray();
     * CharStream.of(buffer, 3, 7)
     *     .toArray();  // returns ['3', '4', '5', '6']
     *
     * // Empty range
     * CharStream.of(chars, 2, 2)
     *     .count();  // returns 0
     * }</pre>
     *
     * @param a the array containing the elements
     * @param fromIndex the starting index (inclusive), must be non-negative
     * @param toIndex the ending index (exclusive), must be less than or equal to array length
     * @return a CharStream containing the specified range of elements
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the array length, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(char...)
     * @see #of(CharSequence, int, int)
     */
    public static CharStream of(final char[] a, final int fromIndex, final int toIndex) {
        return isEmptyRange(N.len(a), fromIndex, toIndex) ? empty() : new ArrayCharStream(a, fromIndex, toIndex);
    }

    /**
     * Returns a sequential {@code CharStream} containing all characters from the specified {@code CharSequence}.
     *
     * <p>This factory method creates a stream from any {@code CharSequence} implementation, including
     * {@code String}, {@code StringBuilder}, {@code StringBuffer}, and {@code CharBuffer}. The stream
     * will contain characters in the order they appear in the sequence.
     *
     * <p>If the {@code CharSequence} is null or empty, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from String
     * CharStream.of("Hello")
     *     .forEach(System.out::print);  // prints Hello
     *
     * // Process StringBuilder content
     * StringBuilder sb = new StringBuilder("Java");
     * CharStream.of(sb)
     *     .map(Character::toUpperCase)
     *     .forEach(System.out::print);  // prints JAVA
     *
     * // Count vowels in a string
     * long vowelCount = CharStream.of("Programming")
     *     .filter(c -> "aeiouAEIOU".indexOf(c) >= 0)
     *     .count();  // returns 3
     *
     * // Convert to uppercase and collect
     * String upper = CharStream.of("hello world")
     *     .map(Character::toUpperCase)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "HELLO WORLD"
     *
     * // Works with StringBuffer
     * StringBuffer buffer = new StringBuffer("test");
     * CharStream.of(buffer)
     *     .toArray();  // returns ['t', 'e', 's', 't']
     * }</pre>
     *
     * @param str the {@code CharSequence} containing the characters; may be null
     * @return a new sequential {@code CharStream} containing all characters from the {@code CharSequence},
     *         or an empty stream if the sequence is null or empty
     * @see #of(CharSequence, int, int)
     * @see #of(char...)
     * @see CharSequence
     * @see String
     */
    public static CharStream of(final CharSequence str) {
        return Strings.isEmpty(str) ? empty() : of(str, 0, str.length());
    }

    /**
     * Returns a sequential {@code CharStream} containing characters from a subsequence of the specified
     * {@code CharSequence} between {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     *
     * <p>This factory method creates a stream from a portion of a {@code CharSequence}, allowing you to
     * process a substring or subsequence without creating intermediate string objects. The stream will
     * contain characters from {@code str.charAt(fromIndex)} up to but not including {@code str.charAt(toIndex)}.
     *
     * <p>If the {@code CharSequence} is null or empty, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Extract substring as stream
     * CharStream.of("Hello World", 6, 11)
     *     .forEach(System.out::print);  // prints World
     *
     * // Process middle portion of a string
     * String text = "The quick brown fox";
     * CharStream.of(text, 4, 9)
     *     .toArray();  // returns ['q', 'u', 'i', 'c', 'k']
     *
     * // Skip prefix and suffix
     * String quoted = "\"Hello\"";
     * String unquoted = CharStream.of(quoted, 1, quoted.length() - 1)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "Hello"
     *
     * // Process StringBuilder range
     * StringBuilder sb = new StringBuilder("abcdefgh");
     * CharStream.of(sb, 2, 6)
     *     .map(Character::toUpperCase)
     *     .forEach(System.out::print);  // prints CDEF
     *
     * // Empty range
     * CharStream.of("text", 2, 2)
     *     .count();  // returns 0
     * }</pre>
     *
     * @param str the {@code CharSequence} containing the characters; may be null
     * @param fromIndex the starting index (inclusive), must be non-negative
     * @param toIndex the ending index (exclusive), must be less than or equal to sequence length
     * @return a new sequential {@code CharStream} containing the specified range of characters
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the {@code CharSequence} length, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(CharSequence)
     * @see #of(char[], int, int)
     * @see CharSequence#charAt(int)
     */
    public static CharStream of(final CharSequence str, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, N.len(str));

        if (Strings.isEmpty(str)) {
            return empty();
        }

        //    if (str instanceof String && IOUtil.JAVA_VERSION.atMost(JavaVersion.JAVA_1_8)) {
        //        return of(com.landawn.abacus.util.InternalUtil.getCharsForReadOnly((String) str), fromIndex, toIndex);
        //    }

        final CharIteratorEx iter = new CharIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
                return str.charAt(cursor++);
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor; //NOSONAR
                cursor = toIndex; // count() consumes the remaining elements; the iterator must be exhausted afterwards
                return ret;
            }
        };

        return new IteratorCharStream(iter);
    }

    /**
     * Returns a sequential {@code CharStream} containing the unboxed char values from an array of
     * {@code Character} objects.
     *
     * <p>This factory method converts a {@code Character[]} to a {@code CharStream} by unboxing each
     * {@code Character} object to its primitive char value. The stream will contain all elements in
     * the order they appear in the array.
     *
     * <p><b>Important:</b> Null elements in the array are unboxed to {@code '\0'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert Character array to CharStream
     * Character[] chars = {'a', 'b', 'c'};
     * CharStream.of(chars)
     *     .forEach(System.out::print);  // prints abc
     *
     * // Process boxed characters
     * Character[] letters = {'H', 'E', 'L', 'L', 'O'};
     * CharStream.of(letters)
     *     .map(Character::toLowerCase)
     *     .toArray();  // returns ['h', 'e', 'l', 'l', 'o']
     *
     * // Count specific characters
     * Character[] data = {'a', 'b', 'a', 'c', 'a'};
     * long count = CharStream.of(data)
     *     .filter(c -> c == 'a')
     *     .count();  // returns 3
     *
     * // Null elements are unboxed to '\0'
     * Character[] withNull = {'a', null, 'b'};
     * CharStream.of(withNull).toArray();  // returns ['a', '\0', 'b']
     * }</pre>
     *
     * @param a the array of {@code Character} objects; may be null or empty
     * @return a new sequential {@code CharStream} containing the unboxed char values,
     *         or an empty stream if the array is null or empty
     * @see #of(char...)
     * @see #of(Character[], int, int)
     * @see #of(Collection)
     */
    public static CharStream of(final Character[] a) {
        return Stream.of(a).mapToChar(FC.unbox());
    }

    /**
     * Returns a sequential {@code CharStream} containing the unboxed char values from a range of a
     * {@code Character} array between {@code fromIndex} (inclusive) and {@code toIndex} (exclusive).
     *
     * <p>This factory method converts a sub-range of a {@code Character[]} to a {@code CharStream}
     * by unboxing each {@code Character} object to its primitive char value. The stream will contain
     * elements from {@code a[fromIndex]} up to but not including {@code a[toIndex]}.
     *
     * <p><b>Important:</b> Null elements in the selected range are unboxed to {@code '\0'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process a sub-range of Character array
     * Character[] chars = {'a', 'b', 'c', 'd', 'e', 'f'};
     * CharStream.of(chars, 2, 5)
     *     .forEach(System.out::print);  // prints cde
     *
     * // Skip first and last elements
     * Character[] data = {'[', 'H', 'e', 'l', 'l', 'o', ']'};
     * CharStream.of(data, 1, data.length - 1)
     *     .toArray();  // returns ['H', 'e', 'l', 'l', 'o']
     *
     * // Process middle section
     * Character[] letters = {'A', 'B', 'C', 'D', 'E'};
     * CharStream.of(letters, 1, 4)
     *     .map(Character::toLowerCase)
     *     .toArray();  // returns ['b', 'c', 'd']
     *
     * // Empty range
     * CharStream.of(chars, 2, 2)
     *     .count();  // returns 0
     * }</pre>
     *
     * @param a the array of {@code Character} objects
     * @param fromIndex the starting index (inclusive), must be non-negative
     * @param toIndex the ending index (exclusive), must be less than or equal to array length
     * @return a new sequential {@code CharStream} containing the unboxed char values from the specified range
     * @throws IndexOutOfBoundsException if {@code fromIndex} is negative, {@code toIndex} is greater than
     *         the array length, or {@code fromIndex} is greater than {@code toIndex}
     * @see #of(Character[])
     * @see #of(char[], int, int)
     */
    public static CharStream of(final Character[] a, final int fromIndex, final int toIndex) {
        return Stream.of(a, fromIndex, toIndex).mapToChar(FC.unbox());
    }

    /**
     * Returns a sequential {@code CharStream} containing the unboxed char values from a {@code Collection}
     * of {@code Character} objects.
     *
     * <p>This factory method converts a {@code Collection<Character>} to a {@code CharStream} by
     * unboxing each {@code Character} object to its primitive char value. The stream will process
     * elements in the iteration order of the collection.
     *
     * <p><b>Important:</b> Null elements in the collection are unboxed to {@code '\0'}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert List<Character> to CharStream
     * List<Character> chars = Arrays.asList('h', 'e', 'l', 'l', 'o');
     * CharStream.of(chars)
     *     .forEach(System.out::print);  // prints hello
     *
     * // Process Set<Character>
     * Set<Character> uniqueChars = new HashSet<>(Arrays.asList('a', 'b', 'c', 'a'));
     * CharStream.of(uniqueChars)
     *     .sorted()
     *     .toArray();  // returns ['a', 'b', 'c']
     *
     * // Transform and collect
     * List<Character> lowercase = Arrays.asList('a', 'b', 'c');
     * char[] uppercase = CharStream.of(lowercase)
     *     .map(Character::toUpperCase)
     *     .toArray();  // returns ['A', 'B', 'C']
     *
     * // Count specific characters
     * Collection<Character> data = Arrays.asList('a', 'b', 'a', 'c', 'a');
     * long count = CharStream.of(data)
     *     .filter(c -> c == 'a')
     *     .count();  // returns 3
     *
     * // Empty collection
     * CharStream.of(Collections.emptyList())
     *     .findAny();  // returns OptionalChar.empty()
     * }</pre>
     *
     * @param c the {@code Collection} of {@code Character} objects; may be null or empty
     * @return a new sequential {@code CharStream} containing the unboxed char values,
     *         or an empty stream if the collection is null or empty
     * @see #of(Character[])
     * @see #of(char...)
     * @see Stream#of(Collection)
     */
    public static CharStream of(final Collection<Character> c) {
        return Stream.of(c).mapToChar(FC.unbox());
    }

    /**
     * Returns a sequential {@code CharStream} containing the elements provided by the specified
     * {@code CharIterator}.
     *
     * <p>This factory method creates a stream from a {@code CharIterator}, which is a specialized
     * iterator for primitive char values. The stream will consume the iterator and produce elements
     * in the order they are returned by the iterator.
     *
     * <p>If the iterator is null, an empty stream is returned.
     *
     * <p><b>Note:</b> The iterator should not be used after being passed to this method, as the
     * stream will consume it during processing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from CharIterator
     * CharIterator iter = CharIterator.of('a', 'b', 'c');
     * CharStream.of(iter)
     *     .forEach(System.out::print);  // prints abc
     *
     * // Process iterator from char array
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * CharIterator charIter = CharIterator.of(chars);
     * CharStream.of(charIter)
     *     .map(Character::toUpperCase)
     *     .toArray();  // returns ['H', 'E', 'L', 'L', 'O']
     *
     * // Custom CharIterator implementation
     * CharIterator customIter = new CharIterator() {
     *     private int count = 0;
     *     public boolean hasNext() { return count < 3; }
     *     public char nextChar() { return (char)('a' + count++); }
     * };
     * CharStream.of(customIter)
     *     .count();  // returns 3
     *
     * // Null iterator produces empty stream
     * CharStream.of((CharIterator) null)
     *     .findAny();  // returns OptionalChar.empty()
     * }</pre>
     *
     * @param iterator the {@code CharIterator} providing the elements; may be null
     * @return a new sequential {@code CharStream} containing the elements from the iterator,
     *         or an empty stream if the iterator is null
     * @see CharIterator
     * @see #of(char...)
     * @see #of(CharSequence)
     */
    public static CharStream of(final CharIterator iterator) {
        return iterator == null ? empty() : new IteratorCharStream(iterator);
    }

    /**
     * Returns a sequential {@code CharStream} containing characters from the specified {@code CharBuffer}
     * from its current position to its limit.
     *
     * <p>This factory method creates a stream from a {@code CharBuffer}, reading characters from
     * the buffer's current position up to (but not including) its limit. The buffer's position
     * is not modified by creating the stream, but will be accessed during stream operations.
     *
     * <p>If the buffer is null, an empty stream is returned.
     *
     * <p><b>Note:</b> The stream reads from the buffer using its {@code get(int)} method, which
     * does not modify the buffer's position. However, the buffer should not be modified concurrently
     * during stream processing to ensure consistent behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create stream from CharBuffer
     * CharBuffer buffer = CharBuffer.wrap("Hello");
     * CharStream.of(buffer)
     *     .forEach(System.out::print);  // prints Hello
     *
     * // Process CharBuffer with position/limit
     * CharBuffer buf = CharBuffer.wrap("0123456789");
     * buf.position(3);
     * buf.limit(7);
     * CharStream.of(buf)
     *     .toArray();  // returns ['3', '4', '5', '6']
     *
     * // Transform buffer content
     * CharBuffer data = CharBuffer.allocate(5);
     * data.put("hello");
     * data.flip();  // switches to read mode
     * CharStream.of(data)
     *     .map(Character::toUpperCase)
     *     .forEach(System.out::print);  // prints HELLO
     *
     * // Count characters in buffer range
     * CharBuffer textBuf = CharBuffer.wrap("Programming");
     * textBuf.position(3).limit(8);
     * long count = CharStream.of(textBuf)
     *     .count();  // returns 5
     *
     * // Null buffer produces empty stream
     * CharStream.of((CharBuffer) null)
     *     .findAny();  // returns OptionalChar.empty()
     * }</pre>
     *
     * @param buf the {@code CharBuffer} to read from; may be null
     * @return a new sequential {@code CharStream} containing characters from the buffer's current
     *         position to its limit, or an empty stream if the buffer is null
     * @see CharBuffer
     * @see #of(char...)
     * @see #of(CharSequence)
     */
    public static CharStream of(final CharBuffer buf) {
        if (buf == null) {
            return empty();
        }

        //noinspection resource
        return IntStream.range(buf.position(), buf.limit()).mapToChar(buf::get);
    }

    /**
     * Returns a sequential {@code CharStream} containing characters read from the specified {@code File}.
     *
     * <p>This factory method creates a stream that reads characters from a file using the platform's
     * default character encoding. The file is opened when the stream is created and should be properly
     * closed after use, preferably using a try-with-resources statement.
     *
     * <p>The returned stream is automatically configured to close the underlying {@code FileReader}
     * when the stream is closed, ensuring proper resource cleanup.
     *
     * <p><b>Important:</b> Always use try-with-resources or explicitly close the stream to prevent
     * resource leaks. The file reader will remain open until the stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read and process file characters with automatic resource management
     * try (CharStream chars = CharStream.of(new File("data.txt"))) {
     *     chars.filter(Character::isLetter)
     *          .forEach(System.out::print);
     * }  // closes the file automatically
     *
     * // Count lines in a file (by counting newline characters)
     * try (CharStream chars = CharStream.of(new File("document.txt"))) {
     *     long lineCount = chars.filter(c -> c == '\n').count() + 1;
     *     System.out.println("Lines: " + lineCount);
     * }
     *
     * // Extract all digits from a file
     * try (CharStream chars = CharStream.of(new File("numbers.txt"))) {
     *     char[] digits = chars.filter(Character::isDigit).toArray();
     * }
     *
     * // Convert file content to uppercase
     * try (CharStream chars = CharStream.of(new File("input.txt"))) {
     *     String result = chars.map(Character::toUpperCase)
     *                          .mapToObj(String::valueOf)
     *                          .collect(Collectors.joining());
     * }
     *
     * // Read first 100 characters from a large file
     * try (CharStream chars = CharStream.of(new File("largefile.txt"))) {
     *     char[] first100 = chars.limit(100).toArray();
     * }
     * }</pre>
     *
     * <p><b>Note:</b> By design this factory reads using the platform-default charset (via
     * {@link java.io.FileReader}). For explicit charset control, open the file yourself and pass an
     * {@code InputStreamReader} configured for the desired {@link java.nio.charset.Charset} to
     * {@link #of(Reader)}.
     *
     * @param file the {@code File} to read from; must not be null
     * @return a new sequential {@code CharStream} containing the characters from the file
     * @throws UncheckedIOException if an I/O error occurs opening the file
     * @throws NullPointerException if file is null
     * @see #of(Reader)
     * @see #of(Reader, boolean)
     * @see java.io.FileReader
     */
    public static CharStream of(final File file) {
        return of(IOUtil.newFileReader(file), true);
    }

    /**
     * Returns a sequential {@code CharStream} containing characters read from the specified {@code Reader}.
     *
     * <p>This factory method creates a stream that reads characters from a {@code Reader}. The reader
     * will <b>NOT</b> be automatically closed when the stream is closed or consumed. The caller is
     * responsible for managing the reader's lifecycle and closing it appropriately.
     *
     * <p>If you want the reader to be automatically closed when the stream is closed, use
     * {@link #of(Reader, boolean)} with {@code true} as the second parameter.
     *
     * <p>If the reader is null, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read from a reader that will be managed separately
     * Reader reader = new StringReader("Hello World");
     * CharStream.of(reader)
     *     .filter(Character::isLetter)
     *     .forEach(System.out::print);  // prints HelloWorld
     * reader.close();                   // closes the reader manually
     *
     * // Process characters from InputStreamReader
     * Reader inputReader = new InputStreamReader(inputStream);
     * CharStream.of(inputReader)
     *     .map(Character::toLowerCase)
     *     .toArray();
     * inputReader.close();  // cleanup is manual
     *
     * // Read from BufferedReader
     * BufferedReader bufferedReader = new BufferedReader(new FileReader("file.txt"));
     * long charCount = CharStream.of(bufferedReader).count();
     * bufferedReader.close();  // closes manually
     *
     * // Null reader produces empty stream
     * CharStream.of((Reader) null)
     *     .findAny();  // returns OptionalChar.empty()
     * }</pre>
     *
     * @param reader the {@code Reader} to read from; may be null
     * @return a new sequential {@code CharStream} containing the characters from the reader,
     *         or an empty stream if the reader is null
     * @throws UncheckedIOException if an I/O error occurs during reading
     * @see #of(Reader, boolean)
     * @see #of(File)
     * @see java.io.Reader
     */
    public static CharStream of(final Reader reader) {
        return of(reader, false);
    }

    /**
     * Returns a sequential {@code CharStream} containing characters read from the specified {@code Reader},
     * with an option to automatically close the reader when the stream is closed.
     *
     * <p>This factory method creates a stream that reads characters from a {@code Reader}. If
     * {@code closeReaderWhenStreamIsClosed} is {@code true}, the reader will be automatically closed
     * when the stream's {@link #close()} method is called or when a terminal operation completes.
     * This is useful when you want the stream to manage the reader's lifecycle.
     *
     * <p>If {@code closeReaderWhenStreamIsClosed} is {@code false}, the reader will remain open
     * after the stream is closed, and the caller is responsible for closing it.
     *
     * <p>If the reader is null, an empty stream is returned.
     *
     * <p><b>Recommended:</b> When {@code closeReaderWhenStreamIsClosed} is {@code true}, use
     * try-with-resources to ensure proper resource cleanup even if exceptions occur.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Automatic reader cleanup with try-with-resources
     * try (CharStream chars = CharStream.of(new FileReader("data.txt"), true)) {
     *     chars.filter(Character::isDigit)
     *          .forEach(System.out::print);
     * }  // closes the reader automatically
     *
     * // Manual reader management
     * Reader reader = new StringReader("Hello");
     * CharStream chars = CharStream.of(reader, false);
     * chars.forEach(System.out::print);
     * chars.close();    // closes the stream, reader stays open
     * reader.close();   // closes the reader manually
     *
     * // Process multiple streams from the same reader
     * BufferedReader bufferedReader = new BufferedReader(new FileReader("file.txt"));
     * // First pass - don't close reader
     * CharStream.of(bufferedReader, false)
     *     .filter(Character::isLetter)
     *     .count();
     * // Second pass - now close reader
     * CharStream.of(bufferedReader, true)
     *     .filter(Character::isDigit)
     *     .count();
     * // Reader is closed after second stream completes
     *
     * // Read from InputStreamReader with automatic cleanup
     * try (CharStream chars = CharStream.of(
     *         new InputStreamReader(new FileInputStream("input.txt")), true)) {
     *     char[] data = chars.limit(1000).toArray();
     * }  // closes the InputStreamReader automatically
     *
     * // Null reader produces empty stream
     * try (CharStream chars = CharStream.of(null, true)) {
     *     chars.findAny();  // returns OptionalChar.empty()
     * }
     * }</pre>
     *
     * @param reader the {@code Reader} to read from; may be null
     * @param closeReaderWhenStreamIsClosed if {@code true}, the reader will be automatically closed
     *        when the stream is closed; if {@code false}, the caller must close the reader manually
     * @return a new sequential {@code CharStream} containing the characters from the reader,
     *         or an empty stream if the reader is null
     * @throws UncheckedIOException if an I/O error occurs during reading
     * @see #of(Reader)
     * @see #of(File)
     * @see java.io.Reader
     */
    public static CharStream of(final Reader reader, final boolean closeReaderWhenStreamIsClosed) {
        if (reader == null) {
            return empty();
        }

        final CharIterator iter = new CharIterator() {
            private final char[] buf = new char[8192];
            private boolean isEnd = false;
            private int count = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                if (idx >= count && !isEnd) {
                    try {
                        count = reader.read(buf);
                        idx = 0;
                        isEnd = count <= 0;
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }

                return count > idx;
            }

            @Override
            public char nextChar() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return buf[idx++];
            }
        };

        if (closeReaderWhenStreamIsClosed) {
            //noinspection resource
            return of(iter).onClose(Fn.close(reader));
        } else {
            return of(iter);
        }
    }

    private static final Function<char[], CharStream> flatMapper = CharStream::of;

    private static final Function<char[][], CharStream> flattMapper = CharStream::flatten;

    /**
     * Returns a sequential {@code CharStream} containing all characters from a two-dimensional char array,
     * flattened in row-major order (left to right, top to bottom).
     *
     * <p>This factory method flattens a 2D char array into a single stream by concatenating rows
     * sequentially. Each row is processed completely before moving to the next row. Null or empty
     * rows are skipped automatically.
     *
     * <p>If the input array is null or empty, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten a 2D char array
     * char[][] matrix = {
     *     {'a', 'b', 'c'},
     *     {'d', 'e', 'f'},
     *     {'g', 'h', 'i'}
     * };
     * CharStream.flatten(matrix)
     *     .forEach(System.out::print);  // prints abcdefghi
     *
     * // Handle arrays with different row lengths
     * char[][] jagged = {
     *     {'a', 'b'},
     *     {'c', 'd', 'e'},
     *     {'f'}
     * };
     * CharStream.flatten(jagged)
     *     .toArray();  // returns ['a', 'b', 'c', 'd', 'e', 'f']
     *
     * // Skip null rows
     * char[][] withNull = {
     *     {'a', 'b'},
     *     null,
     *     {'c', 'd'}
     * };
     * CharStream.flatten(withNull)
     *     .toArray();  // returns ['a', 'b', 'c', 'd']
     *
     * // Process and collect
     * char[][] data = {{'h', 'e'}, {'l', 'l'}, {'o'}};
     * String result = CharStream.flatten(data)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "hello"
     *
     * // Empty array produces empty stream
     * CharStream.flatten(new char[0][])
     *     .count();  // returns 0
     * }</pre>
     *
     * @param a the two-dimensional char array to flatten; may be null or empty
     * @return a sequential {@code CharStream} containing all characters from the array in row-major order,
     *         or an empty stream if the array is null or empty
     * @see #flatten(char[][], boolean)
     * @see #flatten(char[][], char, boolean)
     * @see #flatten(char[][][])
     */
    public static CharStream flatten(final char[][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToChar(flatMapper);
    }

    /**
     * Returns a sequential {@code CharStream} containing all characters from a two-dimensional char array,
     * flattened either horizontally (row by row) or vertically (column by column).
     *
     * <p>This factory method flattens a 2D char array into a single stream, with the order determined
     * by the {@code vertically} parameter:
     * <ul>
     *   <li>If {@code vertically} is {@code false}: elements are read row by row, left to right,
     *       top to bottom (row-major order)</li>
     *   <li>If {@code vertically} is {@code true}: elements are read column by column, top to bottom,
     *       left to right (column-major order)</li>
     * </ul>
     *
     * <p>If the input array is null or empty, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[][] matrix = {
     *     {'a', 'b', 'c'},
     *     {'d', 'e', 'f'},
     *     {'g', 'h', 'i'}
     * };
     *
     * // Flatten horizontally (row by row)
     * CharStream.flatten(matrix, false)
     *     .forEach(System.out::print);  // prints abcdefghi
     *
     * // Flatten vertically (column by column)
     * CharStream.flatten(matrix, true)
     *     .forEach(System.out::print);  // prints adgbehcfi
     *
     * // Handle jagged arrays vertically (skips null/missing positions)
     * char[][] jagged = {
     *     {'a', 'b', 'c'},
     *     {'d', 'e'},
     *     {'f'}
     * };
     * CharStream.flatten(jagged, true)
     *     .toArray();  // returns ['a', 'd', 'f', 'b', 'e', 'c']
     *
     * // Single row - both directions produce same result
     * char[][] singleRow = {{'a', 'b', 'c'}};
     * CharStream.flatten(singleRow, true)
     *     .toArray();  // returns ['a', 'b', 'c']
     *
     * // Process vertically and collect
     * char[][] grid = {{'1', '2'}, {'3', '4'}};
     * String result = CharStream.flatten(grid, true)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining(","));  // "1,3,2,4"
     * }</pre>
     *
     * @param a the two-dimensional char array to flatten; may be null or empty
     * @param vertically if {@code true}, flatten column by column (column-major order);
     *                   if {@code false}, flatten row by row (row-major order)
     * @return a sequential {@code CharStream} containing all characters from the array in the specified order,
     *         or an empty stream if the array is null or empty
     * @see #flatten(char[][])
     * @see #flatten(char[][], char, boolean)
     */
    public static CharStream flatten(final char[][] a, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (!vertically) {
            return Stream.of(a).flatMapToChar(flatMapper);
        }

        long n = 0;

        for (final char[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final CharIterator iter = new CharIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public char nextChar() {
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
     * Returns a sequential {@code CharStream} containing all characters from a two-dimensional char array,
     * flattened either horizontally or vertically with alignment padding for jagged arrays.
     *
     * <p>This factory method flattens a 2D char array (which may have rows of different lengths) into
     * a single stream, filling missing positions with a padding character to ensure rectangular alignment.
     * The flattening order is determined by the {@code vertically} parameter:
     * <ul>
     *   <li>If {@code vertically} is {@code false}: flatten row by row, padding short rows to match
     *       the longest row length</li>
     *   <li>If {@code vertically} is {@code true}: flatten column by column, padding short columns
     *       to match the longest column length</li>
     * </ul>
     *
     * <p>The resulting stream will have {@code rows * maxRowLength} elements (or {@code maxColumnLength * columns}
     * for vertical flattening), where missing positions are filled with {@code valueForAlignment}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten jagged array horizontally with padding
     * char[][] jagged = {
     *     {'a', 'b', 'c'},
     *     {'d', 'e'},
     *     {'f'}
     * };
     * CharStream.flatten(jagged, '-', false)
     *     .forEach(System.out::print);  // prints abcde-f--
     *
     * // Flatten vertically with padding
     * CharStream.flatten(jagged, '*', true)
     *     .forEach(System.out::print);  // prints adfbe*c**
     *
     * // Create aligned grid
     * char[][] data = {
     *     {'1', '2'},
     *     {'3'},
     *     {'4', '5', '6'}
     * };
     * char[] aligned = CharStream.flatten(data, '0', false)
     *     .toArray();  // returns ['1', '2', '0', '3', '0', '0', '4', '5', '6']
     *
     * // Pad with space for formatting
     * char[][] table = {
     *     {'A', 'B'},
     *     {'C'}
     * };
     * String formatted = CharStream.flatten(table, ' ', false)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "ABC "
     *
     * // Process padded matrix vertically
     * char[][] matrix = {
     *     {'x', 'y'},
     *     {'z'}
     * };
     * CharStream.flatten(matrix, '_', true)
     *     .forEach(System.out::print);  // prints xzy_
     * }</pre>
     *
     * @param a the two-dimensional char array to flatten; may be null or empty
     * @param valueForAlignment the char value to use for padding when rows/columns have different lengths
     * @param vertically if {@code true}, flatten column by column with vertical padding;
     *                   if {@code false}, flatten row by row with horizontal padding
     * @return a sequential {@code CharStream} containing all characters from the array with padding,
     *         or an empty stream if the array is null or empty
     * @see #flatten(char[][])
     * @see #flatten(char[][], boolean)
     */
    public static CharStream flatten(final char[][] a, final char valueForAlignment, final boolean vertically) {
        if (N.isEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (final char[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = (long) rows * cols;
        CharIterator iter = null;

        if (vertically) {
            iter = new CharIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public char nextChar() {
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
            iter = new CharIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public char nextChar() {
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
     * Returns a sequential {@code CharStream} containing all characters from a three-dimensional char array,
     * flattened in natural order (depth-first, then row-major order).
     *
     * <p>This factory method flattens a 3D char array into a single stream by processing dimensions
     * in order: first by the outermost dimension (depth), then by the middle dimension (rows), and
     * finally by the innermost dimension (columns). Null or empty sub-arrays are skipped automatically.
     *
     * <p>If the input array is null or empty, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Flatten a 3D char array
     * char[][][] cube = {
     *     {{'a', 'b'}, {'c', 'd'}},
     *     {{'e', 'f'}, {'g', 'h'}}
     * };
     * CharStream.flatten(cube)
     *     .forEach(System.out::print);  // prints abcdefgh
     *
     * // Handle irregular 3D arrays
     * char[][][] irregular = {
     *     {{'1', '2'}, {'3'}},
     *     {{'4'}},
     *     {{'5', '6'}, {'7', '8', '9'}}
     * };
     * CharStream.flatten(irregular)
     *     .toArray();  // returns ['1', '2', '3', '4', '5', '6', '7', '8', '9']
     *
     * // Skip null sub-arrays
     * char[][][] withNull = {
     *     {{'a', 'b'}, null},
     *     null,
     *     {{'c', 'd'}}
     * };
     * CharStream.flatten(withNull)
     *     .toArray();  // returns ['a', 'b', 'c', 'd']
     *
     * // Process and collect
     * char[][][] data = {
     *     {{'h', 'e'}, {'l', 'l'}},
     *     {{'o'}}
     * };
     * String result = CharStream.flatten(data)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "hello"
     *
     * // Count total characters
     * char[][][] matrix = {
     *     {{'a', 'b', 'c'}, {'d', 'e', 'f'}},
     *     {{'g', 'h', 'i'}, {'j', 'k', 'l'}}
     * };
     * long count = CharStream.flatten(matrix).count();  // returns 12
     * }</pre>
     *
     * @param a the three-dimensional char array to flatten; may be null or empty
     * @return a sequential {@code CharStream} containing all characters from the array in natural order,
     *         or an empty stream if the array is null or empty
     * @see #flatten(char[][])
     * @see #flatten(char[][], boolean)
     */
    public static CharStream flatten(final char[][][] a) {
        return N.isEmpty(a) ? empty() : Stream.of(a).flatMapToChar(flattMapper);
    }

    /**
     * Returns a sequential ordered {@code CharStream} from {@code startInclusive} (inclusive) to
     * {@code endExclusive} (exclusive) by an incremental step of {@code 1}.
     *
     * <p>This factory method generates a sequential stream of characters starting from {@code startInclusive}
     * and incrementing by 1 until reaching (but not including) {@code endExclusive}. If {@code startInclusive}
     * is greater than or equal to {@code endExclusive}, an empty stream is returned.
     *
     * <p>The returned stream is ordered and sequential, making it suitable for generating character sequences,
     * iterating over character ranges, or processing alphabetic ranges.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate lowercase letters a-y (the end character is exclusive)
     * CharStream.range('a', 'z')
     *     .forEach(System.out::print);  // prints abcdefghijklmnopqrstuvwxy
     *
     * // Generate first 5 digits
     * CharStream.range('0', '5')
     *     .forEach(System.out::print);  // prints 01234
     *
     * // Create character array range
     * char[] chars = CharStream.range('A', 'F')
     *     .toArray();  // returns ['A', 'B', 'C', 'D', 'E']
     *
     * // Count characters in range
     * long count = CharStream.range('a', (char) ('z' + 1))
     *     .count();  // returns 26
     *
     * // Empty stream when start >= end
     * CharStream.range('z', 'a')
     *     .count();  // returns 0
     *
     * // Generate and transform
     * String uppercase = CharStream.range('a', 'f')
     *     .map(Character::toUpperCase)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "ABCDE"
     *
     * // Use with ASCII values
     * CharStream.range((char) 65, (char) 70)
     *     .forEach(System.out::print);  // prints ABCDE
     * }</pre>
     *
     * @param startInclusive the starting character (inclusive)
     * @param endExclusive the ending character (exclusive)
     * @return a sequential ordered {@code CharStream} from {@code startInclusive} to {@code endExclusive},
     *         incrementing by 1, or an empty stream if {@code startInclusive >= endExclusive}
     * @see #range(char, char, int)
     * @see #rangeClosed(char, char)
     * @see IntStream#range(int, int)
     */
    public static CharStream range(final char startInclusive, final char endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private char next = startInclusive;
            private int cnt = endExclusive - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
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

                cnt = n >= cnt ? 0 : cnt - (int) n;
                next += (char) n;
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public char[] toArray() {
                final char[] result = new char[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates a CharStream of characters from startInclusive (inclusive) to endExclusive (exclusive) by the specified incremental step.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream.range('a', 'f', 2).toArray();    // returns ['a', 'c', 'e']
     * CharStream.range('z', 'u', -2).toArray();   // returns ['z', 'x', 'v']
     * }</pre>
     *
     * @param startInclusive the starting character (inclusive)
     * @param endExclusive the ending character (exclusive)
     * @param by the incremental step. Must not be zero.
     * @return a CharStream of characters from startInclusive to endExclusive with the specified step
     * @throws IllegalArgumentException if by is zero
     */
    public static CharStream range(final char startInclusive, final char endExclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private char next = startInclusive;
            private final int diff = endExclusive - startInclusive;
            private int cnt = Math.abs(diff) / Math.abs(by) + (Math.abs(diff) % Math.abs(by) == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final char result = next;
                next += (char) by;
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
                next += (char) (n * by);
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public char[] toArray() {
                final char[] result = new char[cnt];

                for (int i = 0; i < cnt; i++, next += (char) by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a sequential ordered {@code CharStream} from {@code startInclusive} (inclusive) to
     * {@code endInclusive} (inclusive) by an incremental step of {@code 1}.
     *
     * <p>This factory method generates a sequential stream of characters starting from {@code startInclusive}
     * and incrementing by 1 until reaching and including {@code endInclusive}. If {@code startInclusive}
     * is greater than {@code endInclusive}, an empty stream is returned. If they are equal, a stream
     * with a single element is returned.
     *
     * <p>The returned stream is ordered and sequential. This method is similar to {@link #range(char, char)}
     * but includes the end character, making it more intuitive for ranges where the endpoint should be included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate lowercase letters a-z (inclusive)
     * CharStream.rangeClosed('a', 'z')
     *     .forEach(System.out::print);  // prints abcdefghijklmnopqrstuvwxyz
     *
     * // Generate digits 0-9 (inclusive)
     * CharStream.rangeClosed('0', '9')
     *     .forEach(System.out::print);  // prints 0123456789
     *
     * // Create character array from A to E
     * char[] chars = CharStream.rangeClosed('A', 'E')
     *     .toArray();  // returns ['A', 'B', 'C', 'D', 'E']
     *
     * // Single character stream when start == end
     * CharStream.rangeClosed('x', 'x')
     *     .count();  // returns 1
     *
     * // Empty stream when start > end
     * CharStream.rangeClosed('z', 'a')
     *     .count();  // returns 0
     *
     * // Generate vowels
     * String vowels = CharStream.rangeClosed('a', 'e')
     *     .filter(c -> "aeiou".indexOf(c) >= 0)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "ae"
     *
     * // Count letters in range
     * long count = CharStream.rangeClosed('A', 'Z')
     *     .count();  // returns 26
     * }</pre>
     *
     * @param startInclusive the starting character (inclusive)
     * @param endInclusive the ending character (inclusive)
     * @return a sequential ordered {@code CharStream} from {@code startInclusive} to {@code endInclusive},
     *         incrementing by 1, or an empty stream if {@code startInclusive > endInclusive}
     * @see #rangeClosed(char, char, int)
     * @see #range(char, char)
     * @see IntStream#rangeClosed(int, int)
     */
    public static CharStream rangeClosed(final char startInclusive, final char endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private char next = startInclusive;
            private int cnt = endInclusive - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
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

                cnt = n >= cnt ? 0 : cnt - (int) n;
                next += (char) n;
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public char[] toArray() {
                final char[] result = new char[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Returns a sequential ordered {@code CharStream} from {@code startInclusive} (inclusive) to
     * {@code endInclusive} (inclusive) by the specified incremental step.
     *
     * <p>This factory method generates a sequential stream of characters starting from {@code startInclusive}
     * and incrementing by {@code by} until reaching or passing {@code endInclusive}. The last element will
     * be the largest value that does not exceed {@code endInclusive} when incrementing (or the smallest value
     * when decrementing with a negative step).
     *
     * <p>The {@code by} parameter can be positive (ascending) or negative (descending), but must not be zero.
     * If the direction of {@code by} is inconsistent with the relationship between {@code startInclusive} and
     * {@code endInclusive}, an empty stream is returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate every other letter a, c, e, ...
     * CharStream.rangeClosed('a', 'j', 2)
     *     .forEach(System.out::print);  // prints acegi
     *
     * // Descending range with negative step
     * CharStream.rangeClosed('z', 'u', -2)
     *     .forEach(System.out::print);  // prints zxv
     *
     * // Generate every third digit
     * CharStream.rangeClosed('0', '9', 3)
     *     .toArray();  // returns ['0', '3', '6', '9']
     *
     * // Single element when start == end
     * CharStream.rangeClosed('x', 'x', 5)
     *     .count();  // returns 1
     *
     * // Empty stream with inconsistent direction
     * CharStream.rangeClosed('a', 'z', -1)
     *     .count();  // returns 0 (start < end but step is negative)
     *
     * // Reverse alphabet by 3s
     * CharStream.rangeClosed('z', 'a', -3)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());  // "zwtqnkheb" (z, w, t, q, n, k, h, e, b)
     *
     * // Step larger than range
     * CharStream.rangeClosed('a', 'c', 10)
     *     .toArray();  // returns ['a'] (only start is within range)
     * }</pre>
     *
     * @param startInclusive the starting character (inclusive)
     * @param endInclusive the ending character (inclusive)
     * @param by the incremental step; must not be zero; can be negative for descending sequences
     * @return a sequential ordered {@code CharStream} from {@code startInclusive} to {@code endInclusive}
     *         with the specified step, or an empty stream if the direction is inconsistent
     * @throws IllegalArgumentException if {@code by} is zero
     * @see #rangeClosed(char, char)
     * @see #range(char, char, int)
     */
    public static CharStream rangeClosed(final char startInclusive, final char endInclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' cannot be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        } else if (endInclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private char next = startInclusive;
            private int cnt = Math.abs((endInclusive - startInclusive) / by) + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final char result = next;
                next += (char) by;
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
                next += (char) (n * by);
            }

            @Override
            public long count() {
                final long ret = cnt;
                cnt = 0;
                return ret;
            }

            @Override
            public char[] toArray() {
                final char[] result = new char[cnt];

                for (int i = 0; i < cnt; i++, next += (char) by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates a CharStream that repeats the specified element for the given number of times.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Repeat a character 5 times
     * CharStream.repeat('a', 5)
     *     .forEach(System.out::print);   // prints aaaaa
     *
     * // Create a string of repeated characters
     * String stars = CharStream.repeat('*', 10)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());
     * // Result: "**********"
     *
     * // Empty stream for n=0
     * CharStream.repeat('x', 0)
     *     .count();   // 0
     * }</pre>
     *
     * @param element the character to repeat
     * @param n the number of times to repeat the element
     * @return a CharStream containing n copies of the specified element
     * @throws IllegalArgumentException if n is negative
     */
    public static CharStream repeat(final char element, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return empty();
        } else if (n < 10) {
            return of(Array.repeat(element, (int) n));
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
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
            public char[] toArray() {
                if (cnt > Integer.MAX_VALUE) {
                    throw new IllegalStateException("Cannot create array larger than Integer.MAX_VALUE: " + cnt);
                }

                final char[] result = new char[(int) cnt];

                Arrays.fill(result, element);

                cnt = 0;

                return result;
            }
        });
    }

    /**
     * Creates an infinite sequential unordered {@code CharStream} of random characters.
     * The characters are randomly and uniformly generated from the entire {@code char} range
     * (Unicode code units {@code 0} ({@code Character.MIN_VALUE}) to {@code 65535} ({@code Character.MAX_VALUE}),
     * inclusive). Note that this includes surrogate code units (U+D800 to U+DFFF), so the
     * generated stream may contain lone surrogates that do not form valid Unicode scalar values.
     * Use {@link #filter(CharPredicate)} to restrict the range if needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 10 random characters
     * CharStream.random()
     *     .limit(10)
     *     .forEach(System.out::print);
     *
     * // Generate random printable ASCII letters (filtering the full range)
     * String randomLetters = CharStream.random()
     *     .filter(c -> c >= 'A' && c <= 'Z')
     *     .limit(20)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());
     * }</pre>
     *
     * @return an infinite {@code CharStream} of random {@code char} values uniformly distributed
     *         over the range {@code [0, Character.MAX_VALUE]}
     */
    public static CharStream random() {
        final int mod = Character.MAX_VALUE + 1;

        return generate(() -> (char) RAND.nextInt(mod));
    }

    /**
     * Creates an infinite CharStream of random characters within the specified range.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 10 random lowercase letters
     * CharStream.random('a', (char) ('z' + 1))
     *     .limit(10)
     *     .forEach(System.out::print);
     *
     * // Generate random digits
     * String randomDigits = CharStream.random('0', (char) ('9' + 1))
     *     .limit(5)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());
     * // Result: e.g., "47293"
     * }</pre>
     *
     * @param startInclusive the lower bound (inclusive) of the random character range
     * @param endExclusive the upper bound (exclusive) of the random character range
     * @return an infinite CharStream of random characters within the specified range
     * @throws IllegalArgumentException if startInclusive is greater than or equal to endExclusive
     */
    public static CharStream random(final char startInclusive, final char endExclusive) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' (" + startInclusive + ") must be less than 'endExclusive' (" + endExclusive + ")");
        }

        final int mod = endExclusive - startInclusive;

        return generate(() -> (char) (RAND.nextInt(mod) + startInclusive));
    }

    /**
     * Creates an infinite CharStream of random characters selected from the given candidates.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate random vowels
     * CharStream.random(new char[] {'a', 'e', 'i', 'o', 'u'})
     *     .limit(10)
     *     .forEach(System.out::print);
     * // Result: e.g., "aeiouaeiou"
     *
     * // Generate random symbols
     * String randomSymbols = CharStream.random(new char[] {'!', '@', '#', '$', '%'})
     *     .limit(8)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());
     * // Result: e.g., "!@#$%!@#"
     * }</pre>
     *
     * @param candidates the array of candidate characters to randomly select from
     * @return an infinite CharStream of random characters from the candidates array,
     *         or an empty stream if the candidates array is {@code null} or empty
     */
    public static CharStream random(final char[] candidates) {
        if (N.isEmpty(candidates)) {
            return empty();
        }

        final int n = candidates.length;

        return generate(() -> candidates[RAND.nextInt(n)]);
    }

    /**
     * Creates a CharStream that iterates using the given hasNext and next suppliers.
     * The stream continues as long as hasNext returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Iterate with external state
     * AtomicInteger counter = new AtomicInteger(0);
     * CharStream.iterate(() -> counter.get() < 5,
     *                    () -> (char) ('a' + counter.getAndIncrement()))
     *     .forEach(System.out::print);   // prints abcde
     *
     * // Iterate from a reader-like source
     * BufferedReader reader = new BufferedReader(new StringReader("abc"));
     * CharStream.iterate(() -> {
     *     try { return reader.ready(); }
     *     catch (IOException e) { return false; }
     * }, () -> {
     *     try { return (char) reader.read(); }
     *     catch (IOException e) { throw new UncheckedIOException(e); }
     * }).forEach(System.out::print);
     * }</pre>
     *
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param next a CharSupplier that provides the next char in the iteration
     * @return a CharStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or next is null
     * @see Stream#iterate(BooleanSupplier, Supplier)
     */
    public static CharStream iterate(final BooleanSupplier hasNext, final CharSupplier next) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorCharStream(new CharIteratorEx() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (!hasNextVal) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public char nextChar() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return next.getAsChar();
            }
        });
    }

    /**
     * Creates a CharStream that starts with an initial value and iterates by applying a function,
     * continuing as long as the hasNext supplier returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Iterate with limit on count
     * AtomicInteger count = new AtomicInteger(0);
     * CharStream.iterate('a', () -> count.getAndIncrement() < 5, c -> (char) (c + 1))
     *     .forEach(System.out::print);   // prints abcde
     *
     * // Iterate until a condition
     * CharStream.iterate('A', () -> Math.random() < 0.8, c -> (char) (c + 1))
     *     .limit(10)
     *     .forEach(System.out::print);   // prints varying output
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext a BooleanSupplier that returns {@code true} if the iteration should continue
     * @param f a function to apply to the previous element to generate the next element
     * @return a CharStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or f is null
     * @see Stream#iterate(Object, BooleanSupplier, UnaryOperator)
     */
    public static CharStream iterate(final char init, final BooleanSupplier hasNext, final CharUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorCharStream(new CharIteratorEx() {
            private char cur = 0;
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
            public char nextChar() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsChar(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Creates a CharStream that starts with an initial value and iterates by applying a function,
     * continuing as long as the hasNext predicate returns {@code true} for the current value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate lowercase letters a-z
     * CharStream.iterate('a', c -> c <= 'z', c -> (char) (c + 1))
     *     .forEach(System.out::print);   // prints abcdefghijklmnopqrstuvwxyz
     *
     * // Generate characters until a certain value
     * CharStream.iterate('A', c -> c < 'E', c -> (char) (c + 1))
     *     .forEach(System.out::print);   // prints ABCD
     *
     * // Generate sequence with custom step
     * CharStream.iterate('a', c -> c < 'f', c -> (char) (c + 2))
     *     .forEach(System.out::print);   // prints ace
     * }</pre>
     *
     * @param init the initial value
     * @param hasNext predicate to test if the iteration should continue. Tested with init for the first element,
     *                then with the result of applying f to the previous element for subsequent elements.
     * @param f a function to apply to the previous element to generate the next element
     * @return a CharStream of elements generated by the iteration
     * @throws IllegalArgumentException if hasNext or f is null
     * @see Stream#iterate(Object, Predicate, UnaryOperator)
     */
    public static CharStream iterate(final char init, final CharPredicate hasNext, final CharUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorCharStream(new CharIteratorEx() {
            private char cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsChar(cur));
                    }

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public char nextChar() {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return cur;
            }
        });
    }

    /**
     * Creates an infinite CharStream that starts with an initial value and generates
     * subsequent values by applying the given function to the previous value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate infinite sequence starting from 'a'
     * CharStream.iterate('a', c -> (char) (c + 1))
     *     .limit(5)
     *     .forEach(System.out::print);   // prints abcde
     *
     * // Generate sequence with custom step
     * CharStream.iterate('A', c -> (char) (c + 2))
     *     .limit(5)
     *     .forEach(System.out::print);   // prints ACEGI
     *
     * // Cycle through characters
     * CharStream.iterate('a', c -> c < 'z' ? (char) (c + 1) : 'a')
     *     .limit(30)
     *     .forEach(System.out::print);
     * }</pre>
     *
     * @param init the initial value
     * @param f a function to apply to the previous element to generate the next element
     * @return an infinite CharStream of elements generated by iterative application of f
     * @throws IllegalArgumentException if f is null
     * @see Stream#iterate(Object, UnaryOperator)
     */
    public static CharStream iterate(final char init, final CharUnaryOperator f) throws IllegalArgumentException {
        N.checkArgNotNull(f);

        return new IteratorCharStream(new CharIteratorEx() {
            private char cur = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public char nextChar() {
                if (isFirst) {
                    isFirst = false;
                    cur = init;
                } else {
                    cur = f.applyAsChar(cur);
                }

                return cur;
            }
        });
    }

    /**
     * Creates an infinite CharStream where each element is generated by the provided CharSupplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate constant characters
     * CharStream.generate(() -> 'x')
     *     .limit(5)
     *     .forEach(System.out::print);   // prints xxxxx
     *
     * // Generate random characters using supplier
     * CharStream.generate(() -> (char) ('a' + new Random().nextInt(26)))
     *     .limit(10)
     *     .forEach(System.out::print);   // prints random lowercase letters
     *
     * // Generate characters from external state
     * AtomicInteger counter = new AtomicInteger(0);
     * CharStream.generate(() -> (char) ('A' + (counter.getAndIncrement() % 26)))
     *     .limit(30)
     *     .forEach(System.out::print);   // prints ABCDEFGHIJKLMNOPQRSTUVWXYZABCD
     * }</pre>
     *
     * @param s the CharSupplier that provides the elements of the stream
     * @return an infinite CharStream generated by the given supplier
     * @throws IllegalArgumentException if the supplier is null
     * @see Stream#generate(Supplier)
     */
    public static CharStream generate(final CharSupplier s) throws IllegalArgumentException {
        N.checkArgNotNull(s);

        return new IteratorCharStream(new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public char nextChar() {
                return s.getAsChar();
            }
        });
    }

    /**
     * Concatenates multiple char arrays into a single CharStream.
     * The arrays are processed in the order they appear in the input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple char arrays
     * CharStream.concat(new char[] {'a', 'b'}, new char[] {'c', 'd'})
     *     .forEach(System.out::print);   // prints abcd
     *
     * // Concatenate arrays of different lengths
     * CharStream.concat(new char[] {'x'}, new char[] {}, new char[] {'y', 'z'})
     *     .forEach(System.out::print);   // prints xyz
     * }</pre>
     *
     * @param a the arrays of chars to concatenate
     * @return a CharStream containing all the chars from the input arrays in order
     * @see Stream#concat(Object[][])
     */
    public static CharStream concat(final char[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Arrays.asList(a));
    }

    /**
     * Concatenates multiple CharIterators into a single CharStream.
     * The iterators are processed in the order they appear in the input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate CharIterators
     * CharIterator iter1 = CharIterator.of('a', 'b');
     * CharIterator iter2 = CharIterator.of('c', 'd');
     * CharStream.concat(iter1, iter2)
     *     .forEach(System.out::print);   // prints abcd
     * }</pre>
     *
     * @param a the CharIterators to concatenate
     * @return a CharStream containing all the chars from the input iterators in order
     * @see Stream#concat(Iterator[])
     */
    public static CharStream concat(final CharIterator... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concatIterators(Array.asList(a));
    }

    /**
     * Concatenates multiple CharStreams into a single CharStream.
     * The streams are processed in the order they appear in the input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple CharStreams
     * CharStream.concat(CharStream.of("ab"), CharStream.of("cd"))
     *     .forEach(System.out::print);   // prints abcd
     *
     * // Concatenate with transformations
     * CharStream.concat(
     *     CharStream.of("hello").filter(Character::isLowerCase),
     *     CharStream.of("WORLD").filter(Character::isUpperCase)
     * ).forEach(System.out::print);   // prints helloWORLD
     * }</pre>
     *
     * @param a the CharStreams to concatenate
     * @return a CharStream containing all the chars from the input streams in order
     * @see Stream#concat(Stream[])
     */
    public static CharStream concat(final CharStream... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a list of char arrays into a single CharStream.
     * The arrays are processed in the order they appear in the list.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate a list of char arrays
     * List<char[]> arrays = Arrays.asList(
     *     new char[] {'a', 'b'},
     *     new char[] {'c', 'd'},
     *     new char[] {'e', 'f'}
     * );
     * CharStream.concat(arrays)
     *     .forEach(System.out::print);   // prints abcdef
     * }</pre>
     *
     * @param c the list of char arrays to concatenate
     * @return a CharStream containing all the chars from the input arrays in order
     * @see #concat(char[]...)
     * @see #concat(Collection)
     */
    @Beta
    public static CharStream concat(final List<char[]> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return of(new CharIteratorEx() {
            private final Iterator<char[]> iter = c.iterator();
            private char[] cur;
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
            public char nextChar() {
                if ((cur == null || cursor >= cur.length) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[cursor++];
            }
        });
    }

    /**
     * Concatenates a collection of CharStreams into a single CharStream.
     * The streams are processed in the order they appear in the collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate a collection of streams
     * List<CharStream> streams = Arrays.asList(
     *     CharStream.of("ab"),
     *     CharStream.of("cd"),
     *     CharStream.of("ef")
     * );
     * CharStream.concat(streams)
     *     .forEach(System.out::print);   // prints abcdef
     * }</pre>
     *
     * @param streams the collection of CharStreams to concatenate
     * @return a CharStream containing all the chars from the input streams in order
     * @see Stream#concat(Collection)
     */
    public static CharStream concat(final Collection<? extends CharStream> streams) {
        return N.isEmpty(streams) ? empty() : new IteratorCharStream(new CharIteratorEx() { //NOSONAR
            private final Iterator<? extends CharStream> iterators = streams.iterator();
            private CharStream cur;
            private CharIterator iter;

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
            public char nextChar() {
                if ((iter == null || !iter.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.nextChar();
            }
        }).onClose(newCloseHandler(streams));
    }

    /**
     * Concatenates a collection of CharIterators into a single CharStream.
     * The iterators are processed in the order they appear in the collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Concatenate multiple CharIterators
     * CharIterator iter1 = CharIterator.of('a', 'b', 'c');
     * CharIterator iter2 = CharIterator.of('d', 'e', 'f');
     * CharIterator iter3 = CharIterator.of('g', 'h', 'i');
     *
     * CharStream.concatIterators(Arrays.asList(iter1, iter2, iter3))
     *     .forEach(System.out::print);   // prints abcdefghi
     *
     * // Concatenate iterators from different sources
     * List<CharIterator> iterators = new ArrayList<>();
     * iterators.add(CharIterator.of("Hello".toCharArray()));
     * iterators.add(CharIterator.of("World".toCharArray()));
     *
     * String result = CharStream.concatIterators(iterators)
     *     .mapToObj(String::valueOf)
     *     .collect(Collectors.joining());   // "HelloWorld"
     * }</pre>
     *
     * @param charIterators the collection of CharIterators to concatenate
     * @return a CharStream containing all the chars from the input iterators in order
     * @see Stream#concatIterators(Collection)
     */
    @Beta
    public static CharStream concatIterators(final Collection<? extends CharIterator> charIterators) {
        if (N.isEmpty(charIterators)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final Iterator<? extends CharIterator> iter = charIterators.iterator();
            private CharIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                    cur = iter.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public char nextChar() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextChar();
            }
        });
    }

    /**
     * Zips two char arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two char arrays element-by-element
     * using the provided zip function. The stream ends when the shorter array runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] a = {'a', 'b', 'c'};
     * char[] b = {'x', 'y', 'z'};
     * CharStream.zip(a, b, (x, y) -> (char)(x + y - 'a'))
     *         .toArray();   // returns ['x', 'z', '|']
     * }</pre>
     *
     * @param a the first char array
     * @param b the second char array
     * @param zipFunction the function to combine elements from both arrays. Must be {@code non-null}.
     * @return a stream of combined values, or an empty stream if either input array is {@code null} or empty
     * @see Stream#zip(Object[], Object[], BiFunction)
     */
    public static CharStream zip(final char[] a, final char[] b, final CharBinaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public char nextChar() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int idx = cursor++;
                return zipFunction.applyAsChar(a[idx], b[idx]);
            }
        });
    }

    /**
     * Zips three char arrays into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three char arrays element-by-element
     * using the provided zip function. The stream ends when the shortest array runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] a = {'a', 'b', 'c'};
     * char[] b = {'x', 'y', 'z'};
     * char[] c = {'1', '2', '3'};
     * CharStream.zip(a, b, c, (x, y, z) -> (char)(x + y - 'a' + z - '1'))
     *         .toArray();   // returns array of combined characters
     * }</pre>
     *
     * @param a the first char array
     * @param b the second char array
     * @param c the third char array
     * @param zipFunction the function to combine elements from all three arrays. Must be {@code non-null}.
     * @return a stream of combined values, or an empty stream if any input array is {@code null} or empty
     */
    public static CharStream zip(final char[] a, final char[] b, final char[] c, final CharTernaryOperator zipFunction) {
        if (N.isEmpty(a) || N.isEmpty(b) || N.isEmpty(c)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public char nextChar() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int idx = cursor++;
                return zipFunction.applyAsChar(a[idx], b[idx], c[idx]);
            }
        });
    }

    /**
     * Zips two char iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two char iterators element-by-element
     * using the provided zip function. The stream ends when either iterator runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter1 = CharIterator.of('a', 'b', 'c');
     * CharIterator iter2 = CharIterator.of('x', 'y', 'z');
     * CharStream.zip(iter1, iter2, (x, y) -> (char)(x + y - 'a'))
     *         .toArray();   // returns ['x', 'z', '|']
     * }</pre>
     *
     * @param a the first char iterator. Can be {@code null} (treated as empty)
     * @param b the second char iterator. Can be {@code null} (treated as empty)
     * @param zipFunction the function to combine elements from both iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, BiFunction)
     */
    public static CharStream zip(final CharIterator a, final CharIterator b, final CharBinaryOperator zipFunction) {
        return new IteratorCharStream(new CharIteratorEx() {
            private final CharIterator iterA = a == null ? CharIterator.empty() : a;
            private final CharIterator iterB = b == null ? CharIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public char nextChar() {
                return zipFunction.applyAsChar(iterA.nextChar(), iterB.nextChar());
            }
        });
    }

    /**
     * Zips three char iterators into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three char iterators element-by-element
     * using the provided zip function. The stream ends when any iterator runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter1 = CharIterator.of('a', 'b', 'c');
     * CharIterator iter2 = CharIterator.of('x', 'y', 'z');
     * CharIterator iter3 = CharIterator.of('1', '2', '3');
     * CharStream.zip(iter1, iter2, iter3, (x, y, z) -> (char)(x + y - 'a' + z - '1'))
     *         .toArray();   // returns array of combined characters
     * }</pre>
     *
     * @param a the first char iterator. Can be {@code null} (treated as empty)
     * @param b the second char iterator. Can be {@code null} (treated as empty)
     * @param c the third char iterator. Can be {@code null} (treated as empty)
     * @param zipFunction the function to combine elements from all three iterators. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static CharStream zip(final CharIterator a, final CharIterator b, final CharIterator c, final CharTernaryOperator zipFunction) {
        return new IteratorCharStream(new CharIteratorEx() {
            private final CharIterator iterA = a == null ? CharIterator.empty() : a;
            private final CharIterator iterB = b == null ? CharIterator.empty() : b;
            private final CharIterator iterC = c == null ? CharIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public char nextChar() {
                return zipFunction.applyAsChar(iterA.nextChar(), iterB.nextChar(), iterC.nextChar());
            }
        });
    }

    /**
     * Zips two char streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from two char streams element-by-element
     * using the provided zip function. The stream ends when either stream runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream stream1 = CharStream.of('a', 'b', 'c');
     * CharStream stream2 = CharStream.of('x', 'y', 'z');
     * CharStream.zip(stream1, stream2, (x, y) -> (char)(x + y - 'a'))
     *         .toArray();   // returns ['x', 'z', '|']
     * }</pre>
     *
     * @param a the first char stream
     * @param b the second char stream
     * @param zipFunction the function to combine elements from both streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, BiFunction)
     */
    public static CharStream zip(final CharStream a, final CharStream b, final CharBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three char streams into a single stream until one of them runs out of values.
     * This is a static factory method that combines elements from three char streams element-by-element
     * using the provided zip function. The stream ends when any stream runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream stream1 = CharStream.of('a', 'b', 'c');
     * CharStream stream2 = CharStream.of('x', 'y', 'z');
     * CharStream stream3 = CharStream.of('1', '2', '3');
     * CharStream.zip(stream1, stream2, stream3, (x, y, z) -> (char)(x + y - 'a' + z - '1'))
     *         .toArray();   // returns array of combined characters
     * }</pre>
     *
     * @param a the first char stream
     * @param b the second char stream
     * @param c the third char stream
     * @param zipFunction the function to combine elements from all three streams. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static CharStream zip(final CharStream a, final CharStream b, final CharStream c, final CharTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), zipFunction).onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple char streams into a single stream until any of them runs out of values.
     * This is a static factory method that combines elements from multiple char streams element-by-element
     * using the provided zip function. The stream ends when any stream runs out of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream stream1 = CharStream.of('a', 'b', 'c');
     * CharStream stream2 = CharStream.of('x', 'y', 'z');
     * CharStream.zip(Arrays.asList(stream1, stream2), chars -> (char)(chars[0] + chars[1] - 'a'))
     *         .toArray();   // returns ['x', 'z', '|']
     * }</pre>
     *
     * @param streams the collection of char streams to zip
     * @param zipFunction the function to combine elements from all streams. Must be {@code non-null}.
     * @return a stream of combined values. Empty if the collection is {@code null} or empty
     * @see Stream#zip(Collection, Function)
     */
    public static CharStream zip(final Collection<? extends CharStream> streams, final CharNFunction<Character> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, zipFunction).mapToChar(ToCharFunction.UNBOX);
    }

    /**
     * Zips two char arrays into a single stream until all of them run out of values, padding shorter arrays with default values.
     * This is a static factory method that combines elements from two char arrays element-by-element
     * using the provided zip function. The stream ends when the longer array runs out of values.
     * When one array runs out of values, the provided default value is used for that array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] a = {'a', 'b'};
     * char[] b = {'x', 'y', 'z'};
     * CharStream.zip(a, b, '0', '0', (x, y) -> (char)(x + y - 'a'))
     *         .toArray();   // returns ['x', 'z', 'I']
     * }</pre>
     *
     * @param a the first char array
     * @param b the second char array
     * @param valueForNoneA the default value to use when the first array runs out of values
     * @param valueForNoneB the default value to use when the second array runs out of values
     * @param zipFunction the function to combine elements from both arrays. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Object[], Object[], Object, Object, BiFunction)
     */
    public static CharStream zip(final char[] a, final char[] b, final char valueForNoneA, final char valueForNoneB, final CharBinaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private char ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public char nextChar() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsChar(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips three char arrays into a single stream until all of them run out of values, padding shorter arrays with default values.
     * This is a static factory method that combines elements from three char arrays element-by-element
     * using the provided zip function. The stream ends when the longest array runs out of values.
     * When an array runs out of values, the provided default value is used for that array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] a = {'a', 'b'};
     * char[] b = {'x', 'y', 'z'};
     * char[] c = {'1', '2'};
     * CharStream.zip(a, b, c, '0', '0', '0', (x, y, z) -> (char)(x + y - 'a' + z - '1'))
     *         .toArray();   // returns array of combined characters
     * }</pre>
     *
     * @param a the first char array
     * @param b the second char array
     * @param c the third char array
     * @param valueForNoneA the default value to use when the first array runs out of values
     * @param valueForNoneB the default value to use when the second array runs out of values
     * @param valueForNoneC the default value to use when the third array runs out of values
     * @param zipFunction the function to combine elements from all three arrays. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static CharStream zip(final char[] a, final char[] b, final char[] c, final char valueForNoneA, final char valueForNoneB, final char valueForNoneC,
            final CharTernaryOperator zipFunction) {
        if (N.isEmpty(a) && N.isEmpty(b) && N.isEmpty(c)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private char ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public char nextChar() {
                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                ret = zipFunction.applyAsChar(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
                        cursor < cLen ? c[cursor] : valueForNoneC);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zips two char iterators into a single stream until all of them run out of values, padding with default values.
     * This is a static factory method that combines elements from two char iterators element-by-element
     * using the provided zip function. The stream ends when both iterators run out of values.
     * When an iterator runs out of values, the provided default value is used for that iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter1 = CharIterator.of('a', 'b');
     * CharIterator iter2 = CharIterator.of('x', 'y', 'z');
     * CharStream.zip(iter1, iter2, '0', '0', (x, y) -> (char)(x + y - 'a'))
     *         .toArray();   // returns ['x', 'z', 'I']
     * }</pre>
     *
     * @param a the first char iterator, may be {@code null} (treated as empty)
     * @param b the second char iterator, may be {@code null} (treated as empty)
     * @param valueForNoneA the default value to use when the first iterator runs out of values
     * @param valueForNoneB the default value to use when the second iterator runs out of values
     * @param zipFunction the function to combine elements from both iterators. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Iterator, Iterator, Object, Object, BiFunction)
     */
    public static CharStream zip(final CharIterator a, final CharIterator b, final char valueForNoneA, final char valueForNoneB,
            final CharBinaryOperator zipFunction) {
        return new IteratorCharStream(new CharIteratorEx() {
            private final CharIterator iterA = a == null ? CharIterator.empty() : a;
            private final CharIterator iterB = b == null ? CharIterator.empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public char nextChar() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsChar(iterA.nextChar(), iterB.hasNext() ? iterB.nextChar() : valueForNoneB);
                } else {
                    return zipFunction.applyAsChar(valueForNoneA, iterB.nextChar());
                }
            }
        });
    }

    /**
     * Zips three char iterators into a single stream until all of them run out of values, padding with default values.
     * This is a static factory method that combines elements from three char iterators element-by-element
     * using the provided zip function. The stream ends when all iterators run out of values.
     * When an iterator runs out of values, the provided default value is used for that iterator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharIterator iter1 = CharIterator.of('a', 'b');
     * CharIterator iter2 = CharIterator.of('x', 'y', 'z');
     * CharIterator iter3 = CharIterator.of('1', '2');
     * CharStream.zip(iter1, iter2, iter3, '0', '0', '0', (x, y, z) -> (char)(x + y - 'a' + z - '1'))
     *         .toArray();   // returns array of combined characters
     * }</pre>
     *
     * @param a the first char iterator, may be {@code null} (treated as empty)
     * @param b the second char iterator, may be {@code null} (treated as empty)
     * @param c the third char iterator, may be {@code null} (treated as empty)
     * @param valueForNoneA the default value to use when the first iterator runs out of values
     * @param valueForNoneB the default value to use when the second iterator runs out of values
     * @param valueForNoneC the default value to use when the third iterator runs out of values
     * @param zipFunction the function to combine elements from all three iterators. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static CharStream zip(final CharIterator a, final CharIterator b, final CharIterator c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTernaryOperator zipFunction) {
        return new IteratorCharStream(new CharIteratorEx() {
            private final CharIterator iterA = a == null ? CharIterator.empty() : a;
            private final CharIterator iterB = b == null ? CharIterator.empty() : b;
            private final CharIterator iterC = c == null ? CharIterator.empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public char nextChar() {
                if (iterA.hasNext()) {
                    return zipFunction.applyAsChar(iterA.nextChar(), iterB.hasNext() ? iterB.nextChar() : valueForNoneB,
                            iterC.hasNext() ? iterC.nextChar() : valueForNoneC);
                } else if (iterB.hasNext()) {
                    return zipFunction.applyAsChar(valueForNoneA, iterB.nextChar(), iterC.hasNext() ? iterC.nextChar() : valueForNoneC);
                } else {
                    return zipFunction.applyAsChar(valueForNoneA, valueForNoneB, iterC.nextChar());
                }
            }
        });
    }

    /**
     * Zips two char streams into a single stream until all of them run out of values, padding with default values.
     * This is a static factory method that combines elements from two char streams element-by-element
     * using the provided zip function. The stream ends when both streams run out of values.
     * When a stream runs out of values, the provided default value is used for that stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream stream1 = CharStream.of('a', 'b');
     * CharStream stream2 = CharStream.of('x', 'y', 'z');
     * CharStream.zip(stream1, stream2, '0', '0', (x, y) -> (char)(x + y - 'a'))
     *         .toArray();   // returns ['x', 'z', 'I']
     * }</pre>
     *
     * @param a the first char stream
     * @param b the second char stream
     * @param valueForNoneA the default value to use when the first stream runs out of values
     * @param valueForNoneB the default value to use when the second stream runs out of values
     * @param zipFunction the function to combine elements from both streams. Must be {@code non-null}.
     * @return a stream of combined values
     * @see Stream#zip(Stream, Stream, Object, Object, BiFunction)
     */
    public static CharStream zip(final CharStream a, final CharStream b, final char valueForNoneA, final char valueForNoneB,
            final CharBinaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(a, b));
    }

    /**
     * Zips three char streams into a single stream until all of them run out of values, padding with default values.
     * This is a static factory method that combines elements from three char streams element-by-element
     * using the provided zip function. The stream ends when all streams run out of values.
     * When a stream runs out of values, the provided default value is used for that stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream stream1 = CharStream.of('a', 'b');
     * CharStream stream2 = CharStream.of('x', 'y', 'z');
     * CharStream stream3 = CharStream.of('1', '2');
     * CharStream.zip(stream1, stream2, stream3, '0', '0', '0', (x, y, z) -> (char)(x + y - 'a' + z - '1'))
     *         .toArray();   // returns array of combined characters
     * }</pre>
     *
     * @param a the first char stream
     * @param b the second char stream
     * @param c the third char stream
     * @param valueForNoneA the default value to use when the first stream runs out of values
     * @param valueForNoneB the default value to use when the second stream runs out of values
     * @param valueForNoneC the default value to use when the third stream runs out of values
     * @param zipFunction the function to combine elements from all three streams. Must be {@code non-null}.
     * @return a stream of combined values
     */
    public static CharStream zip(final CharStream a, final CharStream b, final CharStream c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTernaryOperator zipFunction) {
        return zip(iterate(a), iterate(b), iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zips multiple char streams into a single stream until all of them run out of values, padding with default values.
     * This is a static factory method that combines elements from multiple char streams element-by-element
     * using the provided zip function. The stream ends when all streams run out of values.
     * When a stream runs out of values, the corresponding default value is used for that stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharStream stream1 = CharStream.of('a', 'b');
     * CharStream stream2 = CharStream.of('x', 'y', 'z');
     * char[] defaults = {'0', '0'};
     * CharStream.zip(Arrays.asList(stream1, stream2), defaults, chars -> (char)(chars[0] + chars[1] - 'a'))
     *         .toArray();   // returns ['x', 'z', 'I']
     * }</pre>
     *
     * @param streams the collection of char streams to zip
     * @param valuesForNone array of default values to use when the corresponding stream runs out of values, must have the same size as the streams collection
     * @param zipFunction the function to combine elements from all streams. Must be {@code non-null}.
     * @return a stream of combined values. Empty if the collection is {@code null} or empty
     * @throws IllegalArgumentException if the size of {@code valuesForNone} doesn't match the size of the streams collection
     * @see Stream#zip(Collection, char[], CharNFunction)
     */
    public static CharStream zip(final Collection<? extends CharStream> streams, final char[] valuesForNone, final CharNFunction<Character> zipFunction) {
        //noinspection resource
        return Stream.zip(streams, valuesForNone, zipFunction).mapToChar(ToCharFunction.UNBOX);
    }

    /**
     * Merges two char arrays into a single CharStream based on a selector function.
     * The selector determines which element to choose when both arrays have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted char arrays in ascending order
     * char[] a = {'a', 'c', 'e'};
     * char[] b = {'b', 'd', 'f'};
     * CharStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .forEach(System.out::print);   // prints abcdef
     *
     * // Merge arrays with custom logic (always take from first array first)
     * CharStream.merge(a, b, (x, y) -> MergeResult.TAKE_FIRST)
     *     .forEach(System.out::print);   // prints acebdf
     * }</pre>
     *
     * @param a the first char array
     * @param b the second char array
     * @param nextSelector a function to determine which element should be selected next.
     *                     Returns MergeResult.TAKE_FIRST to select from the first array,
     *                     or MergeResult.TAKE_SECOND to select from the second array.
     * @return a CharStream containing the merged elements
     * @see Stream#merge(Object[], Object[], BiFunction)
     */
    public static CharStream merge(final char[] a, final char[] b, final CharBiFunction<MergeResult> nextSelector) {
        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public char nextChar() {
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
     * Merges three char arrays into a single CharStream based on a selector function.
     * The selector determines which element to choose when multiple arrays have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted char arrays
     * char[] a = {'a', 'd', 'g'};
     * char[] b = {'b', 'e', 'h'};
     * char[] c = {'c', 'f', 'i'};
     * CharStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .forEach(System.out::print);   // prints abcdefghi
     * }</pre>
     *
     * @param a the first char array
     * @param b the second char array
     * @param c the third char array
     * @param nextSelector a function to determine which element should be selected next
     * @return a CharStream containing the merged elements
     * @see Stream#merge(Object[], Object[], Object[], BiFunction)
     */
    public static CharStream merge(final char[] a, final char[] b, final char[] c, final CharBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), CharStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * Merges two CharIterators into a single CharStream based on a selector function.
     * The selector determines which element to choose when both iterators have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two CharIterators with sorted data
     * CharIterator iter1 = CharIterator.of('a', 'c', 'e');
     * CharIterator iter2 = CharIterator.of('b', 'd', 'f');
     * CharStream.merge(iter1, iter2, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .forEach(System.out::print);   // prints abcdef
     * }</pre>
     *
     * @param a the first CharIterator
     * @param b the second CharIterator
     * @param nextSelector a function to determine which element should be selected next.
     *                     Returns MergeResult.TAKE_FIRST to select from the first iterator,
     *                     or MergeResult.TAKE_SECOND to select from the second iterator.
     * @return a CharStream containing the merged elements
     * @see Stream#merge(Iterator, Iterator, BiFunction)
     */
    public static CharStream merge(final CharIterator a, final CharIterator b, final CharBiFunction<MergeResult> nextSelector) {
        return new IteratorCharStream(new CharIteratorEx() {
            private final CharIterator iterA = a == null ? CharIterator.empty() : a;
            private final CharIterator iterB = b == null ? CharIterator.empty() : b;
            private char nextA = 0;
            private char nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() || iterB.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public char nextChar() {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.nextChar())) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextChar()), nextB) == MergeResult.TAKE_FIRST) {
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
                        if (nextSelector.apply((nextA = iterA.nextChar()), (nextB = iterB.nextChar())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.nextChar();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.nextChar();
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     * Merges three CharIterators into a single CharStream based on a selector function.
     * The selector determines which element to choose when multiple iterators have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three CharIterators with sorted data
     * CharIterator iter1 = CharIterator.of('a', 'd', 'g');
     * CharIterator iter2 = CharIterator.of('b', 'e', 'h');
     * CharIterator iter3 = CharIterator.of('c', 'f', 'i');
     * CharStream.merge(iter1, iter2, iter3, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .forEach(System.out::print);   // prints abcdefghi
     * }</pre>
     *
     * @param a the first CharIterator
     * @param b the second CharIterator
     * @param c the third CharIterator
     * @param nextSelector a function to determine which element should be selected next
     * @return a CharStream containing the merged elements
     * @see Stream#merge(Iterator, Iterator, Iterator, BiFunction)
     */
    public static CharStream merge(final CharIterator a, final CharIterator b, final CharIterator c, final CharBiFunction<MergeResult> nextSelector) {
        //noinspection resource
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * Merges two CharStreams into a single CharStream based on a selector function.
     * The selector determines which element to choose when both streams have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge two sorted CharStreams
     * CharStream stream1 = CharStream.of("ace");
     * CharStream stream2 = CharStream.of("bdf");
     * CharStream.merge(stream1, stream2, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .forEach(System.out::print);   // prints abcdef
     *
     * // Merge with custom logic (always take from first stream first)
     * CharStream.merge(
     *     CharStream.of("135"),
     *     CharStream.of("246"),
     *     (x, y) -> MergeResult.TAKE_FIRST
     * ).forEach(System.out::print);   // prints 135246
     * }</pre>
     *
     * @param a the first CharStream
     * @param b the second CharStream
     * @param nextSelector a function to determine which element should be selected next.
     *                     Returns MergeResult.TAKE_FIRST to select from the first stream,
     *                     or MergeResult.TAKE_SECOND to select from the second stream.
     * @return a CharStream containing the merged elements
     * @see Stream#merge(Stream, Stream, BiFunction)
     */
    public static CharStream merge(final CharStream a, final CharStream b, final CharBiFunction<MergeResult> nextSelector) {
        return merge(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(a, b));
    }

    /**
     * Merges three CharStreams into a single CharStream based on a selector function.
     * The selector determines which element to choose when multiple streams have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge three sorted CharStreams
     * CharStream stream1 = CharStream.of("adg");
     * CharStream stream2 = CharStream.of("beh");
     * CharStream stream3 = CharStream.of("cfi");
     * CharStream.merge(stream1, stream2, stream3,
     *     (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .forEach(System.out::print);   // prints abcdefghi
     * }</pre>
     *
     * @param a the first CharStream
     * @param b the second CharStream
     * @param c the third CharStream
     * @param nextSelector a function to determine which element should be selected next
     * @return a CharStream containing the merged elements
     * @see Stream#merge(Stream, Stream, Stream, BiFunction)
     */
    public static CharStream merge(final CharStream a, final CharStream b, final CharStream c, final CharBiFunction<MergeResult> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * Merges a collection of CharStreams into a single CharStream based on a selector function.
     * The selector determines which element to choose when multiple streams have remaining elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Merge multiple sorted CharStreams in ascending order
     * List<CharStream> streams = Arrays.asList(
     *     CharStream.of("adg"),
     *     CharStream.of("beh"),
     *     CharStream.of("cfi")
     * );
     * CharStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .forEach(System.out::print);   // prints abcdefghi
     *
     * // Merge with custom priority logic
     * List<CharStream> customStreams = Arrays.asList(
     *     CharStream.of("123"),
     *     CharStream.of("456"),
     *     CharStream.of("789")
     * );
     * CharStream.merge(customStreams, (x, y) -> x > y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     *     .forEach(System.out::print);   // prints in descending order priority
     * }</pre>
     *
     * @param streams the collection of CharStreams to merge
     * @param nextSelector a function to determine which element should be selected next.
     *                     Takes two elements from different streams and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or MergeResult.TAKE_SECOND to select the second.
     * @return a CharStream containing the merged elements
     * @see Stream#merge(Collection, BiFunction)
     */
    public static CharStream merge(final Collection<? extends CharStream> streams, final CharBiFunction<MergeResult> nextSelector) {
        if (N.isEmpty(streams)) {
            return empty();
        } else if (streams.size() == 1) {
            return streams.iterator().next();
        } else if (streams.size() == 2) {
            final Iterator<? extends CharStream> iter = streams.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends CharStream> iter = streams.iterator();
        CharStream result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * An extension class for CharStream to provide additional functionality.
     */
    public abstract static class CharStreamEx extends CharStream {
        /**
         * Constructor for CharStreamEx.
         *
         * @param sorted whether the stream is sorted
         * @param closeHandlers collection of close handlers
         */
        private CharStreamEx(final boolean sorted, final Collection<LocalRunnable> closeHandlers) { //NOSONAR
            super(sorted, closeHandlers);
        }
    }
}
