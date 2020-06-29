/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.stream;

import java.io.Closeable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.annotation.TerminalOpTriggered;
import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.Immutable;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;

/**
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 *
 * @param <T> the type of the stream elements
 * @param <A> the type of array
 * @param <P> the type of predicate
 * @param <C> the type of consumer
 * @param <PL> the type of PrimitiveList/List
 * @param <OT> the type of Optional
 * @param <IT> the type of Indexed
 * @param <ITER> the type of Iterator
 * @param <S> the type of of the stream implementing {@code BaseStream}
 * @see Stream
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 */
public interface BaseStream<T, A, P, C, PL, OT, IT, ITER, S extends BaseStream<T, A, P, C, PL, OT, IT, ITER, S>> extends Closeable, Immutable {

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate.
     *
     * @param predicate
     * @return
     */
    @ParallelSupported
    @IntermediateOp
    S filter(P predicate);

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate.
     *
     * @param predicate
     * @param actionOnDroppedItem
     * @return
     */
    @ParallelSupported
    @IntermediateOp
    @Beta
    S filter(P predicate, C actionOnDroppedItem);

    /**
     * Keep the elements until the given predicate returns false. The stream should be sorted, which means if x is the first element: <code>predicate.text(x)</code> returns false, any element y behind x: <code>predicate.text(y)</code> should returns false.
     *
     * In parallel Streams, the elements after the first element which <code>predicate</code> returns false may be tested by predicate too.
     *
     * @param predicate
     * @return
     */
    @ParallelSupported
    @IntermediateOp
    S takeWhile(P predicate);

    /**
     * Remove the elements until the given predicate returns false. The stream should be sorted, which means if x is the first element: <code>predicate.text(x)</code> returns true, any element y behind x: <code>predicate.text(y)</code> should returns true.
     *
     * In parallel Streams, the elements after the first element which <code>predicate</code> returns false may be tested by predicate too.
     *
     * @param predicate
     * @return
     */
    @ParallelSupported
    @IntermediateOp
    S dropWhile(P predicate);

    /**
     * Remove the elements until the given predicate returns false. The stream should be sorted, which means if x is the first element: <code>predicate.text(x)</code> returns true, any element y behind x: <code>predicate.text(y)</code> should returns true.
     *
     * In parallel Streams, the elements after the first element which <code>predicate</code> returns false may be tested by predicate too.
     *
     *
     * @param predicate
     * @param actionOnDroppedItem
     * @return
     */
    @ParallelSupported
    @IntermediateOp
    @Beta
    S dropWhile(P predicate, C actionOnDroppedItem);

    @ParallelSupported
    @IntermediateOp
    @Beta
    S skipUntil(P predicate);

    /**
     *
     * @param predicate
     * @return
     * @deprecated
     */
    @Deprecated
    @ParallelSupported
    @IntermediateOp
    S removeIf(P predicate);

    /**
     *
     * @param predicate
     * @param actionOnDroppedItem
     * @return
     * @deprecated
     */
    @Deprecated
    @ParallelSupported
    @IntermediateOp
    S removeIf(P predicate, C actionOnDroppedItem);

    /**
     * Returns Stream of {@code S} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Stream<S> split(int chunkSize);

    /**
     * Returns Stream of {@code PL} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Stream<PL> splitToList(int chunkSize);

    /**
     * Split the stream by the specified predicate.
     *
     *
     * This stream should be sorted by value which is used to verify the border.
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param predicate
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Stream<S> split(final P predicate);

    /**
     * Split the stream by the specified predicate.
     *
     * This method only run sequentially, even in parallel stream.
     *
     * @param predicate
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Stream<PL> splitToList(final P predicate);

    /**
     * Split the stream into two pieces at <code>where</code>.
     * The first piece will be loaded into memory.
     *
     * @param where
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Stream<S> splitAt(int where);

    /**
     * Split the stream into two pieces at <code>where</code> turns to {@code true}.
     * The first piece will be loaded into memory.
     *
     * <pre>
     * <code>
     * Stream.of(1, 3, 2, 4, 2, 5).splitAt(it -> it >= 4).forEach(s -> s.println()); // [1, 3, 2], [4, 2, 5]
     * </code>
     * </pre>
     *
     * @param where
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Stream<S> splitAt(P where);

    /**
     *
     * @param windowSize
     * @return
     * @see #sliding(int, int)
     */
    @SequentialOnly
    @IntermediateOp
    Stream<S> sliding(int windowSize);

    /**
     *
     * @param windowSize
     * @return
     * @see #sliding(int, int)
     */
    @SequentialOnly
    @IntermediateOp
    Stream<PL> slidingToList(int windowSize);

    /**
     * <code>Stream.of(1, 2, 3, 4, 5, 6, 7, 8).sliding(3, 1).forEach(Stream::println)</code>
     * <br /> output: <br />
     * [1, 2, 3] <br />
     * [2, 3, 4] <br />
     * [3, 4, 5] <br />
     * [4, 5, 6] <br />
     * [5, 6, 7] <br />
     * [6, 7, 8] <br />
     *
     * <br>============================================================================</br>
     * <code>Stream.of(1, 2, 3, 4, 5, 6, 7, 8).sliding(3, 3).forEach(Stream::println)</code>
     * <br /> output: <br />
     * [1, 2, 3] <br />
     * [4, 5, 6] <br />
     * [7, 8] <br />
     *
     * <br>============================================================================</br>
     * <code>Stream.of(1, 2, 3, 4, 5, 6, 7, 5).sliding(3, 5).forEach(Stream::println)</code>
     * <br /> output: <br />
     * [1, 2, 3] <br />
     * [6, 7, 8] <br />
     *
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param windowSize
     * @param increment
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Stream<S> sliding(int windowSize, int increment);

    /**
     *
     * @param windowSize
     * @param increment
     * @return
     * @see #sliding(int, int)
     */
    @SequentialOnly
    @IntermediateOp
    Stream<PL> slidingToList(int windowSize, int increment);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param c
     * @return
     * @see IntList#intersection(IntList)
     */
    @SequentialOnly
    @IntermediateOp
    S intersection(Collection<?> c);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param c
     * @return
     * @see IntList#difference(IntList)
     */
    @SequentialOnly
    @IntermediateOp
    S difference(Collection<?> c);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param c
     * @return
     * @see IntList#symmetricDifference(IntList)
     */
    @SequentialOnly
    @IntermediateOp
    S symmetricDifference(Collection<T> c);

    /**
     * <br />
     * All elements will be loaded to memory and sorted if not yet.
     *
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Optional<Map<Percentage, T>> percentiles();

    /**
     *
     * <br />
     * This method only run sequentially, even in parallel stream and all elements will be loaded to memory.
     *
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    S reversed();

    /**
     *
     * <br />
     * This method only run sequentially, even in parallel stream and all elements will be loaded to memory.
     *
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    S rotated(int distance);

    /**
     *
     * <br />
     * This method only run sequentially, even in parallel stream and all elements will be loaded to memory.
     *
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    S shuffled();

    /**
     *
     * <br />
     * This method only run sequentially, even in parallel stream and all elements will be loaded to memory.
     *
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    @TerminalOpTriggered
    S shuffled(Random rnd);

    /**
     * Returns a stream consisting of the distinct elements of this stream.
     *
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S distinct();

    /**
     * Returns a stream consisting of the elements of this stream in sorted order.
     *
     * <br />
     * All elements will be loaded to memory.
     *
     * @return
     */
    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    S sorted();

    //    /**
    //     * <br />
    //     * This method only run sequentially, even in parallel stream and all elements will be loaded to memory.
    //     *
    //     * @return
    //     */
    //    @SequentialOnly
    //    S cached();

    @ParallelSupported
    @IntermediateOp
    @TerminalOpTriggered
    S reverseSorted();

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    Stream<IT> indexed();

    /**
     *
     * @param n
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S skip(long n);

    /**
     *
     * @param n
     * @param consumer
     * @return
     */
    @ParallelSupported
    @IntermediateOp
    @Beta
    S skip(long n, C consumer);

    /**
     *
     * @param maxSize
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S limit(long maxSize);

    //    /**
    //     * Same as: {@code stream.skip(from).limit(to - from)}.
    //     *
    //     * @param from
    //     * @param to
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @SequentialOnly
    //    S slice(long from, long to);

    @SequentialOnly
    @IntermediateOp
    S step(long step);

    /**
     *
     * @param action
     * @return
     */
    @ParallelSupported
    @IntermediateOp
    S peek(C action);

    /**
     * Same as {@code peek}
     *
     * @param action
     * @return
     * @see #peek(Object)
     */
    @ParallelSupported
    @IntermediateOp
    S onEach(C action);

    @SequentialOnly
    @IntermediateOp
    S prepend(S stream);

    @SequentialOnly
    @IntermediateOp
    S prepend(OT op);

    @SequentialOnly
    @IntermediateOp
    S append(S s);

    @SequentialOnly
    @IntermediateOp
    S append(OT op);

    @SequentialOnly
    @IntermediateOp
    S appendIfEmpty(Supplier<? extends S> supplier);

    /**
     * This is a terminal operation. That's to say this stream will be closed after this operation.
     *
     * @param <R>
     * @param <E>
     * @param func
     * @return
     * @throws E
     */
    @TerminalOp
    <R, E extends Exception> Optional<R> applyIfNotEmpty(Throwables.Function<? super S, R, E> func) throws E;

    /**
     * This is a terminal operation. That's to say this stream will be closed after this operation.
     *
     * @param <E>
     * @param action
     * @throws E
     * @return
     */
    @TerminalOp
    <E extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super S, E> action) throws E;

    //    /**
    //     * <br />
    //     * This method only run sequentially, even in parallel stream and all elements will be loaded to memory.
    //     *
    //     * @return
    //     */
    //    @SequentialOnly
    //    S cached();

    @SequentialOnly
    @TerminalOp
    String join(CharSequence delimiter);

    @SequentialOnly
    @TerminalOp
    String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix);

    //    /**
    //     * Same as: {@code stream.skip(from).limit(to - from)}.
    //     *
    //     * @param from
    //     * @param to
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @SequentialOnly
    //    S slice(long from, long to);

    @SequentialOnly
    @TerminalOp
    long count();

    @SequentialOnly
    @TerminalOp
    OT first();

    @SequentialOnly
    @TerminalOp
    OT last();

    /**
     *
     * @return
     * @throws DuplicatedResultException if there are more than one element in this stream.
     */
    @SequentialOnly
    @TerminalOp
    OT onlyOne() throws DuplicatedResultException;

    @SequentialOnly
    @TerminalOp
    A toArray();

    @SequentialOnly
    @TerminalOp
    List<T> toList();

    @SequentialOnly
    @TerminalOp
    Set<T> toSet();

    @SequentialOnly
    @TerminalOp
    ImmutableList<T> toImmutableList();

    @SequentialOnly
    @TerminalOp
    ImmutableSet<T> toImmutableSet();

    @SequentialOnly
    @TerminalOp
    <CC extends Collection<T>> CC toCollection(Supplier<? extends CC> supplier);

    @SequentialOnly
    @TerminalOp
    Multiset<T> toMultiset();

    @SequentialOnly
    @TerminalOp
    Multiset<T> toMultiset(Supplier<? extends Multiset<T>> supplier);

    @SequentialOnly
    @TerminalOp
    LongMultiset<T> toLongMultiset();

    @SequentialOnly
    @TerminalOp
    LongMultiset<T> toLongMultiset(Supplier<? extends LongMultiset<T>> supplier);

    @SequentialOnly
    @TerminalOp
    @Beta
    void println();

    /**
     * Returns an iterator for the elements of this stream.
     *
     * <br />
     * Remember to close this Stream after the iteration is done, if needed.
     *
     * @return
     * @deprecated ? may cause memory/resource leak if forget to close this {@code Stream}
     */
    @Deprecated
    @SequentialOnly
    ITER iterator();

    @SequentialOnly
    @IntermediateOp
    @Beta
    @SuppressWarnings("rawtypes")
    <SS extends BaseStream> SS __(Function<? super S, ? extends SS> transfer);

    //    @SequentialOnly
    //    Try<S> tried();

    /**
     *
     * @param closeHandler
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S onClose(Runnable closeHandler);

    /**
     *
     */
    @SequentialOnly
    @Override
    void close();

    boolean isParallel();

    @SequentialOnly
    @IntermediateOp
    S sequential();

    @SequentialOnly
    @IntermediateOp
    S parallel();

    /**
     *
     * @param maxThreadNum
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(int maxThreadNum);

    /**
     *
     * @param splitor
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(Splitor splitor);

    /**
     * Returns an equivalent stream that is parallel. May return itself if the stream was already parallel with the same <code>maxThreadNum</code> and <code>splitor</code> as the specified ones.
     *
     * <br></br>
     * When to use parallel Streams?
     * <ul>
     * <li>First of all, do NOT and should NOT use parallel Streams if you don't have any problem with sequential Streams, because using parallel Streams has extra cost.</li>
     * <li>Consider using parallel Streams only when <a href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">N(the number of elements) * Q(cost per element of F, the per-element function (usually a lambda)) is big enough(e.g. IO involved. Network: DB/web service request..., Reading/Writing file...)</a>.</li>
     * <li>It's easy to test out the differences of performance by sequential Streams and parallel Streams with <a href="http://www.landawn.com/api-docs/com/landawn/abacus/util/Profiler.html">Profiler</a>:</li>
     * </ul>
     * <pre>
     * <code>
     * Profiler.run(1, 1, 3, "sequential", () -> Stream.of(list).operation(F)...).printResult();
     * Profiler.run(1, 1, 3, "parallel", () -> Stream.of(list).parallel().operation(F)...).printResult();
     * </code>
     * </pre>
     *
     * Here is a sample performance test with computer: CPU Intel i7-3520M 4-cores 2.9 GHz, JDK 1.8.0_101, Windows 7:
     *
     * <pre>
     * <code>
     * public void test_perf() {
     *     final String[] strs = new String[10_000];
     *     N.fill(strs, N.uuid());
     *
     *     final int m = 1;
     *     final Function<String, Long> mapper = str -> {
     *         long result = 0;
     *         for (int i = 0; i < m; i++) {
     *             result += sum(str.toCharArray()) + 1;
     *         }
     *         return result;
     *     };
     *
     *     final MutableLong sum = MutableLong.of(0);
     *
     *     for (int i = 0, len = strs.length; i < len; i++) {
     *         sum.add(mapper.apply(strs[i]));
     *     }
     *
     *     final int threadNum = 1, loopNum = 100, roundNum = 3;
     *
     *     Profiler.run(threadNum, loopNum, roundNum, "For Loop", () -> {
     *         long result = 0;
     *         for (int i = 0, len = strs.length; i < len; i++) {
     *             result += mapper.apply(strs[i]);
     *         }
     *         assertEquals(sum.longValue(), result);
     *     }).printResult();
     *
     *     Profiler.run(threadNum, loopNum, roundNum, "JDK Sequential",
     *             () -> assertEquals(sum.longValue(), java.util.stream.Stream.of(strs).map(mapper).mapToLong(e -> e).sum())).printResult();
     *
     *     Profiler.run(threadNum, loopNum, roundNum, "JDK Parallel",
     *             () -> assertEquals(sum.longValue(), java.util.stream.Stream.of(strs).parallel().map(mapper).mapToLong(e -> e).sum())).printResult();
     *
     *     Profiler.run(threadNum, loopNum, roundNum, "Abcus Sequential",
     *             () -> assertEquals(sum.longValue(), Stream.of(strs).map(mapper).mapToLong(e -> e).sum().longValue())).printResult();
     *
     *     Profiler.run(threadNum, loopNum, roundNum, "Abcus Parallel",
     *             () -> assertEquals(sum.longValue(), Stream.of(strs).parallel().map(mapper).mapToLong(e -> e).sum().longValue())).printResult();
     * }
     * </code>
     * </pre>
     * <b>And test result</b>: <i>Unit is milliseconds. N(the number of elements) is 10_000, Q(cost per element of F, the per-element function (usually a lambda), here is <code>mapper</code>) is calculated by: value of 'For loop' / N(10_000).</i>
     * <table>
     * <tr><th></th><th>  m = 1  </th><th>m = 10</th><th>m = 50</th><th>m = 100</th><th>m = 500</th><th>m = 1000</th></tr>
     * <tr><td>   Q   </td><td>0.00002</td><td>0.0002</td><td>0.001</td><td>0.002</td><td>0.01</td><td>0.02</td></tr>
     * <tr><td>For Loop</td><td>0.23</td><td>2.3</td><td>11</td><td>22</td><td>110</td><td>219</td></tr>
     * <tr><td>JDK Sequential</td><td>0.28</td><td>2.3</td><td>11</td><td>22</td><td>114</td><td>212</td></tr>
     * <tr><td>JDK Parallel</td><td>0.22</td><td>1.3</td><td>6</td><td>12</td><td>66</td><td>122</td></tr>
     * <tr><td>Abcus Sequential</td><td>0.3</td><td>2</td><td>11</td><td>22</td><td>112</td><td>212</td></tr>
     * <tr><td>Abcus Parallel</td><td>11</td><td>11</td><td>11</td><td>16</td><td>77</td><td>128</td></tr>
     * </table>
     *
     * <b>Comparison:</b>
     * <ul>
     * <li>Again, do NOT and should NOT use parallel Streams if you don't have any performance problem with sequential Streams, because using parallel Streams has extra cost.</li>
     * <li>Again, consider using parallel Streams only when <a href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">N(the number of elements) * Q(cost per element of F, the per-element function (usually a lambda)) is big enough</a>.</li>
     * <li>The implementation of parallel Streams in Abacus is more than 10 times, slower than parallel Streams in JDK when Q is tiny(here is less than 0.0002 milliseconds by the test): </li>
     * <ul>
     *      <li>The implementation of parallel Streams in JDK 8 still can beat the sequential/for loop when Q is tiny(Here is 0.00002 milliseconds by the test).
     *      That's amazing, considering the extra cost brought by parallel computation. It's well done.</li>
     *      <li>The implementation of parallel Streams in Abacus is pretty simple and straight forward.
     *      The extra cost(starting threads/synchronization/queue...) brought by parallel Streams in Abacus is too bigger to tiny Q(Here is less than 0.001 milliseconds by the test).
     *      But it starts to be faster than sequential Streams when Q is big enough(Here is 0.001 milliseconds by the test) and starts to catch the parallel Streams in JDK when Q is bigger(Here is 0.01 milliseconds by the test).</li>
     *      <li>Consider using the parallel Streams in Abacus when Q is big enough, specially when IO involved in F.
     *      Because one IO operation(e.g. DB/web service request..., Reading/Writing file...) usually takes 1 to 1000 milliseconds, or even longer.
     *      By the parallel Streams APIs in Abacus, it's very simple to specify max thread numbers. Sometimes, it's much faster to execute IO/Network requests with a bit more threads.
     *      It's fair to say that the parallel Streams in Abacus is high efficient, may same as or faster than the parallel Streams in JDK when Q is big enough, except F is heavy cpu-used operation.
     *      Most of the times, the Q is big enough to consider using parallel Stream is because IO/Network is involved in F.</li>
     * </ul>
     * <li>JDK 7 is supported by the Streams in Abacus. It's perfect to work with <a href="https://github.com/orfjackal/retrolambda">retrolambda</a> on Android</li>
     * <li>All primitive types are supported by Stream APIs in Abacus except boolean</li>
     * </ul>
     *
     * <br></br>
     * A bit more about Lambdas/Stream APIs, you may heard that Lambdas/Stream APIs is <a href="http://blog.takipi.com/benchmark-how-java-8-lambdas-and-streams-can-make-your-code-5-times-slower/">5 time slower than imperative programming</a>.
     * It's true when Q and F is VERY, VERY tiny, like <code>f = (int a, int b) -> a + b;</code>.
     * But if we look into the samples in the article and think about it: it just takes less than 1 milliseconds to get the max value in 100k numbers.
     * There is potential performance issue only if the "get the max value in 100K numbers" call many, many times in your API or single request.
     * Otherwise, the difference between 0.1 milliseconds to 0.5 milliseconds can be totally ignored.
     * Usually we meet performance issue only if Q and F is big enough. However, the performance of Lambdas/Streams APIs is closed to for loop when Q and F is big enough.
     * No matter in which scenario, We don't need and should not concern the performance of Lambdas/Stream APIs.
     *
     * <br></br>
     * Although it's is parallel Streams, it doesn't means all the methods are executed in parallel.
     * Because the sequential way is as fast, or even faster than the parallel way for some methods, or is pretty difficult, if not possible, to implement the method by parallel approach.
     * Here are the methods which are executed sequentially even in parallel Streams.
     * <br></br>
     * <i>splitXXX/splitAt/splitBy/slidingXXX/collapse, distinct, reverse, rotate, shuffle, indexed, cached, top, kthLargest, count, toArray, toList, toList, toSet, toMultiset, toLongMultiset,
     * intersection(Collection c), difference(Collection c), symmetricDifference(Collection c), forEach(identity, accumulator, predicate), findFirstOrLast, findFirstAndLast</i>
     *
     * @param maxThreadNum Default value is the number of cpu-cores. Steps/operations will be executed sequentially if <code>maxThreadNum</code> is 1.
     * @param splitor The target array is split by ranges for multiple threads if splitor is <code>splitor.ARRAY</code> and target stream composed by array. It looks like:
     *
     * <pre><code>
     * for (int i = 0; i < maxThreadNum; i++) {
     *     final int sliceIndex = i;
     *
     *     futureList.add(asyncExecutor.execute(new Runnable() {
     *         public void run() {
     *             int cursor = fromIndex + sliceIndex * sliceSize;
     *             final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
     *             while (cursor < to) {
     *                 action.accept(elements[cursor++]);
     *             }
     *        }
     *    }));
     * }
     * </code></pre>
     *        Otherwise, each thread will get the elements from the target array/iterator in the stream one by one with the target array/iterator synchronized. It looks like:
     * <pre><code>
     * for (int i = 0; i < maxThreadNum; i++) {
     *     futureList.add(asyncExecutor.execute(new Runnable() {
     *         public void run() {
     *             T next = null;
     *
     *             while (true) {
     *                 synchronized (elements) {
     *                     if (cursor.intValue() < toIndex) {
     *                         next = elements[cursor.getAndIncrement()];
     *                     } else {
     *                         break;
     *                     }
     *                 }
     *
     *                 action.accept(next);
     *             }
     *         }
     *     }));
     * }
     * </code></pre>
     *        Using <code>splitor.ARRAY</code> only when F (the per-element function (usually a lambda)) is very tiny and the cost of synchronization on the target array/iterator is too big to it.
     *        For the F involving IO or taking 'long' to complete, choose <code>splitor.ITERATOR</code>. Default value is <code>splitor.ITERATOR</code>.
     * @return
     * @see MergeResult
     * @see com.landawn.abacus.util.Profiler#run(int, int, int, String, Runnable)
     * @see <a href="https://www.infoq.com/presentations/parallel-java-se-8#downloadPdf">Understanding Parallel Stream Performance in Java SE 8</a>
     * @see <a href="http://gee.cs.oswego.edu/dl/html/StreamParallelGuidance.html">When to use parallel Streams</a>
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(int maxThreadNum, Splitor splitor);

    /**
     *
     * @param maxThreadNum
     * @param splitor
     * @param executor should be able to execute {@code maxThreadNum} * {@code following up operations} in parallel.
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(int maxThreadNum, Splitor splitor, Executor executor);

    /**
     *
     * @param maxThreadNum
     * @param executor should be able to execute {@code maxThreadNum} * {@code following up operations} in parallel.
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(int maxThreadNum, Executor executor);

    /**
     *
     * @param executor should be able to execute {@code maxThreadNum} * {@code following up operations} in parallel.
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    S parallel(Executor executor);

    /**
     * Temporarily switch the stream to sequence for operations {@code ops} and then switch back to parallel stream with same {@code maxThreadNum/splitor/asyncExecutor}.
     * <br />
     * {@code parallelStream().sequence().map(ops).parallel(sameMaxThreadNum, sameSplitor, sameAsyncExecutor)}
     * 
     * @param <SS>
     * @param ops
     * @return
     */
    @SequentialOnly
    @IntermediateOp
    @Beta
    @SuppressWarnings("rawtypes")
    <SS extends BaseStream> SS psp(Function<? super S, ? extends SS> ops);

    //    /**
    //     * Returns a new sequential {@code SS} by apply {@code thisStream.parallel()} to the specified {@code func}.
    //     * It's equal to:
    //     * <pre>
    //     * <code>
    //     * thisStream.parallel().(action by func).sequential();
    //     * </code>
    //     * </pre>
    //     *
    //     * @param func
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    <SS extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?, ?>> SS parallelOnly(Function<? super S, SS> func);
    //
    //    /**
    //     * Returns a new sequential {@code SS} by apply {@code thisStream.parallel(maxThreadNum)} to the specified {@code func}.
    //     * It's equal to:
    //     * <pre>
    //     * <code>
    //     * thisStream.parallel(maxThreadNum).(action by func).sequential();
    //     * </code>
    //     * </pre>
    //     *
    //     * @param maxThreadNum
    //     * @param func
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    <SS extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?, ?>> SS parallelOnly(int maxThreadNum, Function<? super S, SS> func);
    //
    //    /**
    //     * Returns a new sequential {@code S} by apply {@code thisStream.parallel(maxThreadNum, executor)} to the specified {@code func}.
    //     *
    //     * <pre>
    //     * <code>
    //     * thisStream.parallel(maxThreadNum, executor).(action by func).sequential();
    //     * </code>
    //     * </pre>
    //     *
    //     * @param maxThreadNum
    //     * @param executor should be able to execute {@code maxThreadNum} * {@code following up operations} in parallel.
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    <SS extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?, ?>> SS parallelOnly(int maxThreadNum, Executor executor, Function<? super S, SS> func);

    //    /**
    //     * Return the underlying <code>maxThreadNum</code> if the stream is parallel, otherwise <code>1</code> is returned.
    //     *
    //     * @return
    //     */
    //    int maxThreadNum();

    //    /**
    //     * Returns a parallel stream with the specified <code>maxThreadNum</code> . Or return
    //     * itself, either because the stream was already parallel with same <code>maxThreadNum</code>, or because
    //     * it's a sequential stream.
    //     *
    //     * @param maxThreadNum
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    S maxThreadNum(int maxThreadNum);

    //    /**
    //     * Return the underlying <code>splitor</code> if the stream is parallel, otherwise the default value <code>splitor.ITERATOR</code> is returned.
    //     *
    //     * @return
    //     */
    //    Splitor splitor();

    //    /**
    //     * Returns a parallel stream with the specified <code>splitor</code> . Or return
    //     * itself, either because the stream was already parallel with same <code>splitor</code>, or because
    //     * it's a sequential stream.
    //     *
    //     * @param splitor
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    S splitor(Splitor splitor);

    public enum Splitor {
        ARRAY, ITERATOR;
    }
}
