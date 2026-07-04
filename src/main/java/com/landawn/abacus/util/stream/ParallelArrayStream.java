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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * A parallel implementation of Stream backed by an array that enables concurrent processing
 * of elements across multiple threads. This class extends ArrayStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution model distributes work across multiple threads using a splitor strategy:
 * <ul>
 * <li>{@code ARRAY} - Divides the array into fixed-size slices assigned to each thread</li>
 * <li>{@code ITERATOR} - Uses shared cursor with synchronized access for load balancing</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel stream
 * Stream<String> stream = Stream.of(largeStringArray).parallel();
 *
 * // Process elements in parallel
 * long count = stream.filter(s -> s.length() > 5).count();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Operations on this stream are thread-safe and properly synchronized
 * when accessing shared state during parallel execution.
 *
 * @param <T> the type of elements in the stream
 * @see ArrayStream
 * @see Stream#parallel()
 */
@SuppressFBWarnings("NM_WRONG_PACKAGE")
@SuppressWarnings("java:S2184")
final class ParallelArrayStream<T> extends ArrayStream<T> {
    private final int maxThreadNum;
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile ArrayStream<T> sequential;

    /**
     * Constructs a ParallelArrayStream with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the array to stream
     * @param fromIndex the start index (inclusive) of the range to process
     * @param toIndex the end index (exclusive) of the range to process
     * @param sorted whether the array elements in the range are in sorted order
     * @param comparator the comparator used to order elements, or null if using natural ordering
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelArrayStream(final T[] values, final int fromIndex, final int toIndex, final boolean sorted, final Comparator<? super T> comparator,
            final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads,
            final Collection<LocalRunnable> closeHandlers) {
        super(values, fromIndex, toIndex, sorted, comparator, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Returns a parallel stream consisting of the elements of this stream that match the given predicate.
     * Elements are tested concurrently across multiple threads; the output order is not guaranteed
     * to match the input order.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element to determine inclusion
     * @return a new parallel stream of matching elements
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> filter(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.filter(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<Iterator<T>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private T next = null;
                    private boolean hasNext = false;

                    @Override
                    public boolean hasNext() {
                        if (!hasNext) {
                            while (cursor < to) {
                                next = elements[cursor++];

                                if (predicate.test(next)) {
                                    hasNext = true;
                                    break;
                                }
                            }
                        }

                        return hasNext;
                    }

                    @Override
                    public T next() {
                        if (!hasNext && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNext = false;
                        return next;
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private T next = null;
                    private boolean hasNext = false;

                    @Override
                    public boolean hasNext() {
                        if (!hasNext) {
                            while (true) {
                                synchronized (elements) {
                                    if (cursor.value() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next)) {
                                    hasNext = true;
                                    break;
                                }
                            }
                        }

                        return hasNext;
                    }

                    @Override
                    public T next() {
                        if (!hasNext && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        hasNext = false;
                        return next;
                    }

                });
            }
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream of elements from this stream that match the given predicate until
     * a worker observes a non-matching element. Once any thread observes an element that does not match the predicate,
     * all threads stop producing elements. The output order is not guaranteed to match the input order.
     *
     * <p><b>&#9888; Parallel streams:</b> this operation does not guarantee encounter-order prefix
     * semantics; later matching elements may be returned.
     *
     * @param predicate a non-interfering, stateless predicate to apply to elements; once it returns
     *        {@code false} for any element, the stream terminates
     * @return a new parallel stream of matching elements selected by the parallel operation
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> takeWhile(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.takeWhile(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<Iterator<T>> iters = new ArrayList<>(threadNum);
        final AtomicBoolean hasMore = new AtomicBoolean(true);
        final MutableInt cursor = MutableInt.of(fromIndex);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext && hasMore.get()) {
                        synchronized (elements) {
                            if (cursor.value() < toIndex) {
                                next = elements[cursor.getAndIncrement()];
                                hasNext = true;
                            } else {
                                hasMore.set(false);
                            }
                        }

                        if (hasNext && !predicate.test(next)) {
                            hasNext = false;
                            hasMore.set(false);
                        }
                    }

                    return hasNext;
                }

                @Override
                public T next() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream after dropping matching elements until a worker observes a
     * non-matching element. After the drop condition is satisfied, all threads proceed concurrently
     * consuming the remainder of the stream.
     *
     * <p><b>&#9888;</b> In parallel streams there is no guarantee of encounter-order prefix semantics:
     * because elements are pulled and tested concurrently, the boundary between dropped and kept elements
     * is not exact. An element near the boundary that satisfies the predicate may be dropped even though
     * it follows the first non-matching element. For deterministic prefix semantics, drop sequentially
     * (e.g. {@code sequential().dropWhile(...)}).
     *
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return a new parallel stream of elements selected by the parallel drop operation
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.dropWhile(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<Iterator<T>> iters = new ArrayList<>(threadNum);
        final AtomicBoolean dropped = new AtomicBoolean(false);
        final MutableInt cursor = MutableInt.of(fromIndex);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        // Once the boundary element has been found ('dropped' is set), all threads switch to
                        // pass-through mode and consume the remaining elements via the shared cursor.
                        if (dropped.get()) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                    hasNext = true;
                                }
                            }
                        } else {
                            while (!dropped.get()) {
                                synchronized (elements) {
                                    if (cursor.value() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (!predicate.test(next)) {
                                    hasNext = true;
                                    dropped.set(true);
                                    break;
                                }
                            }

                            if (!hasNext && dropped.get()) {
                                synchronized (elements) {
                                    if (cursor.value() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                        hasNext = true;
                                    }
                                }
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public T next() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream consisting of the results of applying the given function to the elements
     * of this stream. The mapping function is applied concurrently across multiple threads; the output
     * order is not guaranteed to match the input order.
     *
     * @param <R> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel stream of mapped values
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> Stream<R> map(final Function<? super T, ? extends R> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.map(mapper);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<Iterator<R>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public R next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.apply(elements[cursor++]);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public R next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final R result = mapper.apply((T) next);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream by applying the given {@link BiFunction} to successive overlapping pairs
     * of elements with the specified increment. The window size is 2. Pairs are extracted in parallel
     * using a shared cursor; the output order is not guaranteed to match the input order.
     *
     * @param <R> the element type of the new stream
     * @param increment the step size between successive windows; must be positive
     * @param ignoreNotPaired if {@code true}, incomplete windows at the end are discarded; if {@code false},
     *        missing second elements are passed as {@code null}
     * @param mapper a non-interfering, stateless function applied to each pair of adjacent elements
     * @return a new parallel stream of results from the sliding pair mapping
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code increment} is not positive
     */
    @Override
    public <R> Stream<R> slidingMap(final int increment, final boolean ignoreNotPaired, final BiFunction<? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().slidingMap(increment, ignoreNotPaired, mapper).iteratorEx(), false, null, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, closeHandlers());
        }

        final int windowSize = 2;
        checkArgPositive(increment, "increment"); //NOSONAR

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<Iterator<R>> iters = new ArrayList<>(threadNum);
        final MutableInt curIndex = MutableInt.of(fromIndex);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private int cursor = -1;

                @Override
                public boolean hasNext() {
                    if (cursor == -1) {
                        synchronized (elements) {
                            if (ignoreNotPaired ? toIndex - curIndex.value() >= windowSize : curIndex.value() < toIndex) {
                                cursor = curIndex.value();
                                curIndex.setValue(increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex);
                            }
                        }
                    }

                    return cursor != -1;
                }

                @Override
                public R next() {
                    if (cursor == -1 && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final R result = mapper.apply(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null);
                    cursor = -1;
                    return result;
                }
            });
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream by applying the given {@link TriFunction} to successive overlapping
     * triples of elements with the specified increment. The window size is 3. Triples are extracted
     * in parallel using a shared cursor; the output order is not guaranteed to match the input order.
     *
     * @param <R> the element type of the new stream
     * @param increment the step size between successive windows; must be positive
     * @param ignoreNotPaired if {@code true}, incomplete windows at the end are discarded; if {@code false},
     *        missing elements in the window are passed as {@code null}
     * @param mapper a non-interfering, stateless function applied to each triple of elements
     * @return a new parallel stream of results from the sliding triple mapping
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code increment} is not positive
     */
    @Override
    public <R> Stream<R> slidingMap(final int increment, final boolean ignoreNotPaired, final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().slidingMap(increment, ignoreNotPaired, mapper).iteratorEx(), false, null, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, closeHandlers());
        }

        final int windowSize = 3;
        checkArgPositive(increment, cs.increment);

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<Iterator<R>> iters = new ArrayList<>(threadNum);
        final MutableInt curIndex = MutableInt.of(fromIndex);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private int cursor = -1;

                @Override
                public boolean hasNext() {
                    if (cursor == -1) {
                        synchronized (elements) {
                            if (ignoreNotPaired ? toIndex - curIndex.value() >= windowSize : curIndex.value() < toIndex) {
                                cursor = curIndex.value();
                                curIndex.setValue(increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex);
                            }
                        }
                    }

                    return cursor != -1;
                }

                @Override
                public R next() {
                    if (cursor == -1 && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final R result = mapper.apply(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null,
                            cursor < toIndex - 2 ? elements[cursor + 2] : null);
                    cursor = -1;
                    return result;
                }
            });
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream where the first element is mapped using {@code mapperForFirst} and all
     * remaining elements are mapped using {@code mapperForElse}. If the stream is empty, an empty stream
     * is returned. If the stream contains exactly one element, {@code mapperForFirst} is used for it.
     *
     * @param <R> the element type of the resulting stream
     * @param mapperForFirst a non-interfering, stateless function to apply to the first element
     * @param mapperForElse a non-interfering, stateless function to apply to all other elements
     * @return a new parallel stream with differentiated first-element mapping
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> Stream<R> mapFirstOrElse(final Function<? super T, ? extends R> mapperForFirst, final Function<? super T, ? extends R> mapperForElse)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapFirstOrElse(mapperForFirst, mapperForElse);
        }

        if (fromIndex == toIndex) {
            return (Stream<R>) this;
        } else if (toIndex - fromIndex == 1) {
            return map(mapperForFirst);
        } else {
            final Function<T, R> mapperForFirst2 = (Function<T, R>) mapperForFirst;
            final Function<T, R> mapperForElse2 = (Function<T, R>) mapperForElse;

            //noinspection resource
            return skip(1).map(mapperForElse2).prepend(Stream.of(elements[fromIndex]).map(mapperForFirst2));
        }
    }

    /**
     * Returns a parallel stream where the last element is mapped using {@code mapperForLast} and all
     * preceding elements are mapped using {@code mapperForElse}. If the stream is empty, an empty stream
     * is returned. If the stream contains exactly one element, {@code mapperForLast} is used for it.
     *
     * @param <R> the element type of the resulting stream
     * @param mapperForLast a non-interfering, stateless function to apply to the last element
     * @param mapperForElse a non-interfering, stateless function to apply to all preceding elements
     * @return a new parallel stream with differentiated last-element mapping
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> Stream<R> mapLastOrElse(final Function<? super T, ? extends R> mapperForLast, final Function<? super T, ? extends R> mapperForElse)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().mapLastOrElse(mapperForLast, mapperForElse).iteratorEx(), false, null, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, closeHandlers());
        }

        if (fromIndex == toIndex) {
            return (Stream<R>) this;
        } else if (toIndex - fromIndex == 1) {
            return map(mapperForLast);
        } else {
            final Function<T, R> mapperForLast2 = (Function<T, R>) mapperForLast;
            final Function<T, R> mapperForElse2 = (Function<T, R>) mapperForElse;

            //noinspection resource
            return limit(toIndex - fromIndex - 1).map(mapperForElse2).append(Stream.of(elements[toIndex - 1]).map(mapperForLast2));
        }
    }

    /**
     * Returns a parallel {@link CharStream} consisting of the results of applying the given
     * function to the elements of this stream. The mapping is performed concurrently across
     * multiple threads; the output order is not guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element to produce a {@code char}
     * @return a new parallel {@code CharStream} of mapped char values
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public CharStream mapToChar(final ToCharFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToChar(mapper);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Character>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public Character next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.applyAsChar(elements[cursor++]);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public Character next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final Character result = mapper.applyAsChar((T) next);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return new ParallelIteratorCharStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link ByteStream} consisting of the results of applying the given
     * function to the elements of this stream. The mapping is performed concurrently across
     * multiple threads; the output order is not guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element to produce a {@code byte}
     * @return a new parallel {@code ByteStream} of mapped byte values
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public ByteStream mapToByte(final ToByteFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToByte(mapper);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Byte>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public Byte next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.applyAsByte(elements[cursor++]);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public Byte next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final Byte result = mapper.applyAsByte((T) next);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return new ParallelIteratorByteStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link ShortStream} consisting of the results of applying the given
     * function to the elements of this stream. The mapping is performed concurrently across
     * multiple threads; the output order is not guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element to produce a {@code short}
     * @return a new parallel {@code ShortStream} of mapped short values
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public ShortStream mapToShort(final ToShortFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToShort(mapper);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Short>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public Short next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.applyAsShort(elements[cursor++]);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public Short next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final Short result = mapper.applyAsShort((T) next);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return new ParallelIteratorShortStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link IntStream} consisting of the results of applying the given
     * function to the elements of this stream. The mapping is performed concurrently across
     * multiple threads; the output order is not guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element to produce an {@code int}
     * @return a new parallel {@code IntStream} of mapped int values
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public IntStream mapToInt(final ToIntFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToInt(mapper);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Integer>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public Integer next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.applyAsInt(elements[cursor++]);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public Integer next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final Integer result = mapper.applyAsInt((T) next);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return new ParallelIteratorIntStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link LongStream} consisting of the results of applying the given
     * function to the elements of this stream. The mapping is performed concurrently across
     * multiple threads; the output order is not guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element to produce a {@code long}
     * @return a new parallel {@code LongStream} of mapped long values
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public LongStream mapToLong(final ToLongFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToLong(mapper);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Long>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public Long next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.applyAsLong(elements[cursor++]);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public Long next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final Long result = mapper.applyAsLong((T) next);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return new ParallelIteratorLongStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link FloatStream} consisting of the results of applying the given
     * function to the elements of this stream. The mapping is performed concurrently across
     * multiple threads; the output order is not guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element to produce a {@code float}
     * @return a new parallel {@code FloatStream} of mapped float values
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public FloatStream mapToFloat(final ToFloatFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToFloat(mapper);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Float>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public Float next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.applyAsFloat(elements[cursor++]);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public Float next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final Float result = mapper.applyAsFloat((T) next);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return new ParallelIteratorFloatStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link DoubleStream} consisting of the results of applying the given
     * function to the elements of this stream. The mapping is performed concurrently across
     * multiple threads; the output order is not guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element to produce a {@code double}
     * @return a new parallel {@code DoubleStream} of mapped double values
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToDouble(mapper);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Double>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public Double next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return mapper.applyAsDouble(elements[cursor++]);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public Double next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final Double result = mapper.applyAsDouble((T) next);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return new ParallelIteratorDoubleStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream consisting of the results of replacing each element of this stream with
     * the contents of the stream produced by applying the given mapping function. Each sub-stream is
     * opened and consumed by the thread that obtained the corresponding source element. Close handlers
     * on sub-streams are called when each sub-stream is exhausted. The output order is not guaranteed
     * to match the input order.
     *
     * @param <R> the element type of the new stream
     * @param mapper a non-interfering, stateless function that maps each element to a stream of results;
     *        the returned sub-stream may be {@code null}, in which case it is treated as empty
     * @return a new parallel stream formed by concatenating all mapped sub-streams
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatMap(mapper), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<R>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private Iterator<? extends R> cur = null;
                    private Stream<? extends R> s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while (cur == null || !cur.hasNext()) {
                            if (cursor < to) {
                                if (closeHandle != null) {
                                    final Deque<LocalRunnable> tmp = closeHandle;
                                    closeHandle = null;
                                    StreamBase.close(tmp);
                                }

                                s = mapper.apply(elements[cursor++]);

                                if (s == null) {
                                    cur = null;
                                } else {
                                    if (N.notEmpty(s.closeHandlers())) {
                                        closeHandle = s.closeHandlers();
                                    }

                                    cur = s.iteratorEx();
                                }
                            } else {
                                cur = null;
                                break;
                            }
                        }

                        return cur != null;
                    }

                    @Override
                    public R next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.next();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {
                    private T next = null;
                    private Iterator<? extends R> cur = null;
                    private Stream<? extends R> s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            if (closeHandle != null) {
                                final Deque<LocalRunnable> tmp = closeHandle;
                                closeHandle = null;
                                StreamBase.close(tmp);
                            }

                            s = mapper.apply(next);

                            if (s == null) {
                                cur = null;
                            } else {
                                if (N.notEmpty(s.closeHandlers())) {
                                    closeHandle = s.closeHandlers();
                                }

                                cur = s.iteratorEx();
                            }
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public R next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.next();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorStream<>(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel stream consisting of the results of replacing each element of this stream with
     * the contents of the collection produced by applying the given mapping function. The mapping is
     * performed concurrently; the output order is not guaranteed to match the input order.
     *
     * @param <R> the element type of the new stream
     * @param mapper a non-interfering, stateless function that maps each element to a collection of results;
     *        {@code null} or empty collections are treated as empty
     * @return a new parallel stream formed by concatenating all mapped collections
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> Stream<R> flatmap(final Function<? super T, ? extends Collection<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatmap(mapper), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<R>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private Iterator<? extends R> cur = null;
                    private Collection<? extends R> c = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && cursor < to) {
                            c = mapper.apply(elements[cursor++]);
                            cur = N.isEmpty(c) ? null : c.iterator();
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public R next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.next();
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {
                    private T next = null;
                    private Iterator<? extends R> cur = null;
                    private Collection<? extends R> c = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            c = mapper.apply(next);
                            cur = N.isEmpty(c) ? null : c.iterator();
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public R next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.next();
                    }
                });
            }
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream consisting of the results of replacing each element of this stream with
     * the contents of the array produced by applying the given mapping function. This is a convenience
     * method equivalent to calling {@link #flatmap(Function)} after wrapping arrays as lists.
     *
     * @param <R> the element type of the new stream
     * @param mapper a non-interfering, stateless function that maps each element to an array of results;
     *        {@code null} arrays are treated as empty
     * @return a new parallel stream formed by concatenating all mapped arrays
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressFBWarnings
    @Override
    public <R> Stream<R> flatMapArray(final Function<? super T, R[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatmap(t -> Array.asList(mapper.apply(t)));
    }

    /**
     * Returns a parallel {@link CharStream} consisting of the results of replacing each element of this
     * stream with the contents of the {@code CharStream} produced by the mapping function. Sub-streams
     * are opened and consumed by the thread that obtained the corresponding source element; close handlers
     * on sub-streams are invoked when each sub-stream is exhausted. Output order is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a {@code CharStream};
     *        {@code null} sub-streams are treated as empty
     * @return a new parallel {@code CharStream} formed by concatenating all mapped char sub-streams
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressFBWarnings
    @Override
    public CharStream flatMapToChar(final Function<? super T, ? extends CharStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorCharStream(sequential().flatMapToChar(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Character>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private CharIterator cur = null;
                    private CharStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while (cur == null || !cur.hasNext()) {
                            if (cursor < to) {
                                if (closeHandle != null) {
                                    final Deque<LocalRunnable> tmp = closeHandle;
                                    closeHandle = null;
                                    StreamBase.close(tmp);
                                }

                                s = mapper.apply(elements[cursor++]);

                                if (s == null) {
                                    cur = null;
                                } else {
                                    if (N.notEmpty(s.closeHandlers())) {
                                        closeHandle = s.closeHandlers();
                                    }

                                    cur = s.iteratorEx();
                                }
                            } else {
                                cur = null;
                                break;
                            }
                        }

                        return cur != null;
                    }

                    @Override
                    public Character next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextChar();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private T next = null;
                    private CharIterator cur = null;
                    private CharStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            if (closeHandle != null) {
                                final Deque<LocalRunnable> tmp = closeHandle;
                                closeHandle = null;
                                StreamBase.close(tmp);
                            }

                            s = mapper.apply(next);

                            if (s == null) {
                                cur = null;
                            } else {
                                if (N.notEmpty(s.closeHandlers())) {
                                    closeHandle = s.closeHandlers();
                                }

                                cur = s.iteratorEx();
                            }
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public Character next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextChar();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorCharStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link ByteStream} consisting of the results of replacing each element of this
     * stream with the contents of the {@code ByteStream} produced by the mapping function. Sub-streams
     * are opened and consumed by the thread that obtained the corresponding source element; close handlers
     * on sub-streams are invoked when each sub-stream is exhausted. Output order is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a {@code ByteStream};
     *        {@code null} sub-streams are treated as empty
     * @return a new parallel {@code ByteStream} formed by concatenating all mapped byte sub-streams
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressFBWarnings
    @Override
    public ByteStream flatMapToByte(final Function<? super T, ? extends ByteStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatMapToByte(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Byte>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private ByteIterator cur = null;
                    private ByteStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while (cur == null || !cur.hasNext()) {
                            if (cursor < to) {
                                if (closeHandle != null) {
                                    final Deque<LocalRunnable> tmp = closeHandle;
                                    closeHandle = null;
                                    StreamBase.close(tmp);
                                }

                                s = mapper.apply(elements[cursor++]);

                                if (s == null) {
                                    cur = null;
                                } else {
                                    if (N.notEmpty(s.closeHandlers())) {
                                        closeHandle = s.closeHandlers();
                                    }

                                    cur = s.iteratorEx();
                                }
                            } else {
                                cur = null;
                                break;
                            }
                        }

                        return cur != null;
                    }

                    @Override
                    public Byte next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextByte();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private T next = null;
                    private ByteIterator cur = null;
                    private ByteStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            if (closeHandle != null) {
                                final Deque<LocalRunnable> tmp = closeHandle;
                                closeHandle = null;
                                StreamBase.close(tmp);
                            }

                            s = mapper.apply(next);

                            if (s == null) {
                                cur = null;
                            } else {
                                if (N.notEmpty(s.closeHandlers())) {
                                    closeHandle = s.closeHandlers();
                                }

                                cur = s.iteratorEx();
                            }
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public Byte next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextByte();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorByteStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link ShortStream} consisting of the results of replacing each element of this
     * stream with the contents of the {@code ShortStream} produced by the mapping function. Sub-streams
     * are opened and consumed by the thread that obtained the corresponding source element; close handlers
     * on sub-streams are invoked when each sub-stream is exhausted. Output order is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a {@code ShortStream};
     *        {@code null} sub-streams are treated as empty
     * @return a new parallel {@code ShortStream} formed by concatenating all mapped short sub-streams
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressFBWarnings
    @Override
    public ShortStream flatMapToShort(final Function<? super T, ? extends ShortStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorShortStream(sequential().flatMapToShort(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Short>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private ShortIterator cur = null;
                    private ShortStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while (cur == null || !cur.hasNext()) {
                            if (cursor < to) {
                                if (closeHandle != null) {
                                    final Deque<LocalRunnable> tmp = closeHandle;
                                    closeHandle = null;
                                    StreamBase.close(tmp);
                                }

                                s = mapper.apply(elements[cursor++]);

                                if (s == null) {
                                    cur = null;
                                } else {
                                    if (N.notEmpty(s.closeHandlers())) {
                                        closeHandle = s.closeHandlers();
                                    }

                                    cur = s.iteratorEx();
                                }
                            } else {
                                cur = null;
                                break;
                            }
                        }

                        return cur != null;
                    }

                    @Override
                    public Short next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextShort();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private T next = null;
                    private ShortIterator cur = null;
                    private ShortStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            if (closeHandle != null) {
                                final Deque<LocalRunnable> tmp = closeHandle;
                                closeHandle = null;
                                StreamBase.close(tmp);
                            }

                            s = mapper.apply(next);

                            if (s == null) {
                                cur = null;
                            } else {
                                if (N.notEmpty(s.closeHandlers())) {
                                    closeHandle = s.closeHandlers();
                                }

                                cur = s.iteratorEx();
                            }
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public Short next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextShort();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorShortStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link IntStream} consisting of the results of replacing each element of this
     * stream with the contents of the {@code IntStream} produced by the mapping function. Sub-streams
     * are opened and consumed by the thread that obtained the corresponding source element; close handlers
     * on sub-streams are invoked when each sub-stream is exhausted. Output order is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to an {@code IntStream};
     *        {@code null} sub-streams are treated as empty
     * @return a new parallel {@code IntStream} formed by concatenating all mapped int sub-streams
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressFBWarnings
    @Override
    public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Integer>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private IntIterator cur = null;
                    private IntStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while (cur == null || !cur.hasNext()) {
                            if (cursor < to) {
                                if (closeHandle != null) {
                                    final Deque<LocalRunnable> tmp = closeHandle;
                                    closeHandle = null;
                                    StreamBase.close(tmp);
                                }

                                s = mapper.apply(elements[cursor++]);

                                if (s == null) {
                                    cur = null;
                                } else {
                                    if (N.notEmpty(s.closeHandlers())) {
                                        closeHandle = s.closeHandlers();
                                    }

                                    cur = s.iteratorEx();
                                }
                            } else {
                                cur = null;
                                break;
                            }
                        }

                        return cur != null;
                    }

                    @Override
                    public Integer next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextInt();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private T next = null;
                    private IntIterator cur = null;
                    private IntStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            if (closeHandle != null) {
                                final Deque<LocalRunnable> tmp = closeHandle;
                                closeHandle = null;
                                StreamBase.close(tmp);
                            }

                            s = mapper.apply(next);

                            if (s == null) {
                                cur = null;
                            } else {
                                if (N.notEmpty(s.closeHandlers())) {
                                    closeHandle = s.closeHandlers();
                                }

                                cur = s.iteratorEx();
                            }
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public Integer next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextInt();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorIntStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link LongStream} consisting of the results of replacing each element of this
     * stream with the contents of the {@code LongStream} produced by the mapping function. Sub-streams
     * are opened and consumed by the thread that obtained the corresponding source element; close handlers
     * on sub-streams are invoked when each sub-stream is exhausted. Output order is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a {@code LongStream};
     *        {@code null} sub-streams are treated as empty
     * @return a new parallel {@code LongStream} formed by concatenating all mapped long sub-streams
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressFBWarnings
    @Override
    public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMapToLong(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Long>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private LongIterator cur = null;
                    private LongStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while (cur == null || !cur.hasNext()) {
                            if (cursor < to) {
                                if (closeHandle != null) {
                                    final Deque<LocalRunnable> tmp = closeHandle;
                                    closeHandle = null;
                                    StreamBase.close(tmp);
                                }

                                s = mapper.apply(elements[cursor++]);

                                if (s == null) {
                                    cur = null;
                                } else {
                                    if (N.notEmpty(s.closeHandlers())) {
                                        closeHandle = s.closeHandlers();
                                    }

                                    cur = s.iteratorEx();
                                }
                            } else {
                                cur = null;
                                break;
                            }
                        }

                        return cur != null;
                    }

                    @Override
                    public Long next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextLong();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private T next = null;
                    private LongIterator cur = null;
                    private LongStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            if (closeHandle != null) {
                                final Deque<LocalRunnable> tmp = closeHandle;
                                closeHandle = null;
                                StreamBase.close(tmp);
                            }

                            s = mapper.apply(next);

                            if (s == null) {
                                cur = null;
                            } else {
                                if (N.notEmpty(s.closeHandlers())) {
                                    closeHandle = s.closeHandlers();
                                }

                                cur = s.iteratorEx();
                            }
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public Long next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextLong();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorLongStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link FloatStream} consisting of the results of replacing each element of this
     * stream with the contents of the {@code FloatStream} produced by the mapping function. Sub-streams
     * are opened and consumed by the thread that obtained the corresponding source element; close handlers
     * on sub-streams are invoked when each sub-stream is exhausted. Output order is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a {@code FloatStream};
     *        {@code null} sub-streams are treated as empty
     * @return a new parallel {@code FloatStream} formed by concatenating all mapped float sub-streams
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressFBWarnings
    @Override
    public FloatStream flatMapToFloat(final Function<? super T, ? extends FloatStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatMapToFloat(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Float>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private FloatIterator cur = null;
                    private FloatStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while (cur == null || !cur.hasNext()) {
                            if (cursor < to) {
                                if (closeHandle != null) {
                                    final Deque<LocalRunnable> tmp = closeHandle;
                                    closeHandle = null;
                                    StreamBase.close(tmp);
                                }

                                s = mapper.apply(elements[cursor++]);

                                if (s == null) {
                                    cur = null;
                                } else {
                                    if (N.notEmpty(s.closeHandlers())) {
                                        closeHandle = s.closeHandlers();
                                    }

                                    cur = s.iteratorEx();
                                }
                            } else {
                                cur = null;
                                break;
                            }
                        }

                        return cur != null;
                    }

                    @Override
                    public Float next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextFloat();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private T next = null;
                    private FloatIterator cur = null;
                    private FloatStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            if (closeHandle != null) {
                                final Deque<LocalRunnable> tmp = closeHandle;
                                closeHandle = null;
                                StreamBase.close(tmp);
                            }

                            s = mapper.apply(next);

                            if (s == null) {
                                cur = null;
                            } else {
                                if (N.notEmpty(s.closeHandlers())) {
                                    closeHandle = s.closeHandlers();
                                }

                                cur = s.iteratorEx();
                            }
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public Float next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextFloat();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorFloatStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link DoubleStream} consisting of the results of replacing each element of this
     * stream with the contents of the {@code DoubleStream} produced by the mapping function. Sub-streams
     * are opened and consumed by the thread that obtained the corresponding source element; close handlers
     * on sub-streams are invoked when each sub-stream is exhausted. Output order is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a {@code DoubleStream};
     *        {@code null} sub-streams are treated as empty
     * @return a new parallel {@code DoubleStream} formed by concatenating all mapped double sub-streams
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressFBWarnings
    @Override
    public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatMapToDouble(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ObjIteratorEx<Double>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    private DoubleIterator cur = null;
                    private DoubleStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while (cur == null || !cur.hasNext()) {
                            if (cursor < to) {
                                if (closeHandle != null) {
                                    final Deque<LocalRunnable> tmp = closeHandle;
                                    closeHandle = null;
                                    StreamBase.close(tmp);
                                }

                                s = mapper.apply(elements[cursor++]);

                                if (s == null) {
                                    cur = null;
                                } else {
                                    if (N.notEmpty(s.closeHandlers())) {
                                        closeHandle = s.closeHandlers();
                                    }

                                    cur = s.iteratorEx();
                                }
                            } else {
                                cur = null;
                                break;
                            }
                        }

                        return cur != null;
                    }

                    @Override
                    public Double next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextDouble();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private T next = null;
                    private DoubleIterator cur = null;
                    private DoubleStream s = null;
                    private Deque<LocalRunnable> closeHandle = null;

                    @Override
                    public boolean hasNext() {
                        while ((cur == null || !cur.hasNext()) && next != NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    next = (T) NONE;
                                    cur = null;
                                    break;
                                }
                            }

                            if (closeHandle != null) {
                                final Deque<LocalRunnable> tmp = closeHandle;
                                closeHandle = null;
                                StreamBase.close(tmp);
                            }

                            s = mapper.apply(next);

                            if (s == null) {
                                cur = null;
                            } else {
                                if (N.notEmpty(s.closeHandlers())) {
                                    closeHandle = s.closeHandlers();
                                }

                                cur = s.iteratorEx();
                            }
                        }

                        return cur != null && cur.hasNext();
                    }

                    @Override
                    public Double next() {
                        if ((cur == null || !cur.hasNext()) && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        return cur.nextDouble();
                    }

                    @Override
                    public void closeResource() {
                        if (closeHandle != null) {
                            StreamBase.close(closeHandle);
                        }
                    }
                });
            }
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorDoubleStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel stream consisting of the elements of this stream, additionally performing the
     * provided action on each element as elements are consumed. The action is invoked concurrently
     * by multiple threads as they process elements. This is a lazy intermediate operation.
     *
     * @param action a non-interfering action to perform on each element as it is consumed
     * @return a new parallel stream that invokes the action on each element
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> onEach(final Consumer<? super T> action) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.onEach(action);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<Iterator<T>> iters = new ArrayList<>(threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;
                iters.add(new ObjIteratorEx<>() {
                    private int cursor = fromIndex + sliceIndex * sliceSize;
                    private final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @Override
                    public boolean hasNext() {
                        return cursor < to;
                    }

                    @Override
                    public T next() {
                        if (cursor >= to) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        action.accept(elements[cursor]);

                        return elements[cursor++];
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                iters.add(new ObjIteratorEx<>() {

                    private Object next = NONE;

                    @Override
                    public boolean hasNext() {
                        if (next == NONE) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                }
                            }
                        }

                        return next != NONE;
                    }

                    @Override
                    public T next() {
                        if (next == NONE && !hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final T result = (T) next;
                        action.accept(result);
                        next = NONE;
                        return result;
                    }

                });
            }
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Performs the given action on each element of this stream in parallel, then runs the completion
     * callback after all elements have been processed. The {@code action} is invoked concurrently by
     * multiple threads. The {@code onComplete} callback is invoked exactly once after all threads finish,
     * on the calling thread. Element processing order is not guaranteed.
     *
     * @param <E> the type of exception that the action may throw
     * @param <E2> the type of exception that the completion callback may throw
     * @param action a non-interfering action to perform on each element
     * @param onComplete a callback to run after all elements have been processed
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the action throws an exception
     * @throws E2 if the completion callback throws an exception
     */
    @Override
    public <E extends Exception, E2 extends Exception> void forEach(final Throwables.Consumer<? super T, E> action, final Throwables.Runnable<E2> onComplete)
            throws E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            super.forEach(action, onComplete);
            return;
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            action.accept(elements[cursor++]);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            action.accept(next);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        try {
            complete(futureList, eHolder, (E) null);

            onComplete.run();
        } finally {
            try {
                shutdownTempExecutor(asyncExecutorToUse, asyncExecutor);
            } finally {
                close();
            }
        }
    }

    /**
     * Performs a flat-then-join action on elements of this stream in parallel. For each element {@code t},
     * {@code flatMapper} produces an iterable of values {@code U}, and for each pair {@code (t, u)}
     * the {@code action} is invoked. Processing is concurrent; invocation order of the action is
     * not guaranteed.
     *
     * @param <U> the type of values produced by the flat mapper
     * @param <E> the type of exception that {@code flatMapper} may throw
     * @param <E2> the type of exception that the {@code action} may throw
     * @param flatMapper a non-interfering function that maps each element to an iterable of values;
     *        {@code null} iterables are skipped
     * @param action a non-interfering bi-consumer accepting each element and the corresponding flat value
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if {@code flatMapper} throws an exception
     * @throws E2 if {@code action} throws an exception
     */
    @Override
    public <U, E extends Exception, E2 extends Exception> void forEach(final Throwables.Function<? super T, ? extends Iterable<? extends U>, E> flatMapper,
            final Throwables.BiConsumer<? super T, ? super U, E2> action) throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            super.forEach(flatMapper, action);
            return;
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    Iterable<? extends U> c = null;

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            c = flatMapper.apply(elements[cursor]);

                            if (c != null) {
                                for (final U u : c) {
                                    action.accept(elements[cursor], u);
                                }
                            }

                            cursor++;
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    Iterable<? extends U> c = null;
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            c = flatMapper.apply(next);

                            if (c != null) {
                                for (final U u : c) {
                                    action.accept(next, u);
                                }
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a two-level flat-then-join iteration in parallel. For each element {@code t}, applies
     * {@code flatMapper} to get an iterable of {@code T2} values; for each {@code T2} value, applies
     * {@code flatMapper2} to get an iterable of {@code T3} values; then invokes {@code action} for
     * each {@code (t, t2, t3)} triple. When using {@link Splitor#ARRAY}, threads operate on
     * pre-assigned array slices; with {@link Splitor#ITERATOR}, a shared cursor is used. If either
     * flat mapper returns {@code null}, no triples are produced at that level.
     *
     * @param <T2> the type of secondary values produced by the first flat mapper
     * @param <T3> the type of tertiary values produced by the second flat mapper
     * @param <E> the type of exception that {@code flatMapper} may throw
     * @param <E2> the type of exception that {@code flatMapper2} may throw
     * @param <E3> the type of exception that {@code action} may throw
     * @param flatMapper a function mapping each element to an iterable of {@code T2} values; may
     *        return {@code null}
     * @param flatMapper2 a function mapping each {@code T2} value to an iterable of {@code T3}
     *        values; may return {@code null}
     * @param action a tri-consumer invoked for each {@code (t, t2, t3)} triple
     * @throws E if {@code flatMapper} throws an exception
     * @throws E2 if {@code flatMapper2} throws an exception
     * @throws E3 if {@code action} throws an exception
     */
    @Override
    public <T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(
            final Throwables.Function<? super T, ? extends Iterable<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            super.forEach(flatMapper, flatMapper2, action);
            return;
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    Iterable<T2> c2 = null;
                    Iterable<T3> c3 = null;

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            c2 = flatMapper.apply(elements[cursor]);

                            if (c2 != null) {
                                for (final T2 t2 : c2) {
                                    c3 = flatMapper2.apply(t2);

                                    if (c3 != null) {
                                        for (final T3 t3 : c3) {
                                            action.accept(elements[cursor], t2, t3);
                                        }
                                    }
                                }
                            }

                            cursor++;
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    Iterable<T2> c2 = null;
                    Iterable<T3> c3 = null;
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            c2 = flatMapper.apply(next);

                            if (c2 != null) {
                                for (final T2 t2 : c2) {
                                    c3 = flatMapper2.apply(t2);

                                    if (c3 != null) {
                                        for (final T3 t3 : c3) {
                                            action.accept(next, t2, t3);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs the given action for each overlapping or sliding pair of consecutive elements, in
     * parallel. The window size is 2 and the window advances by {@code increment} positions between
     * invocations. Window boundaries are coordinated via a shared synchronized cursor across all
     * threads. When the second element of a pair is absent, {@code null} is passed for that position.
     *
     * @param <E> the type of exception that {@code action} may throw
     * @param increment the number of positions to advance the window between consecutive pairs; must
     *        be positive
     * @param action a non-interfering bi-consumer invoked for each pair
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code increment} is not positive
     * @throws E if the action throws an exception
     */
    @Override
    public <E extends Exception> void forEachPair(final int increment, final Throwables.BiConsumer<? super T, ? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            super.forEachPair(increment, action);
            return;
        }

        final int windowSize = 2;
        checkArgPositive(increment, cs.increment);

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt curIndex = MutableInt.of(fromIndex);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        for (int i = 0; i < threadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, new Runnable() {
                private int cursor = -1;

                @Override
                public void run() {
                    try {
                        while (curIndex.value() < toIndex && eHolder.value() == null) {
                            synchronized (elements) {
                                if (curIndex.value() < toIndex) {
                                    cursor = curIndex.value();
                                    curIndex.setValue(increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex);
                                } else {
                                    break;
                                }
                            }

                            action.accept(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs the given action for each overlapping or sliding triple of consecutive elements, in
     * parallel. The window size is 3 and the window advances by {@code increment} positions between
     * invocations. Window boundaries are coordinated via a shared synchronized cursor. When the
     * second or third element of a triple is absent, {@code null} is passed for the missing
     * positions.
     *
     * @param <E> the type of exception that {@code action} may throw
     * @param increment the number of positions to advance the window between consecutive triples;
     *        must be positive
     * @param action a non-interfering tri-consumer invoked for each triple
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code increment} is not positive
     * @throws E if the action throws an exception
     */
    @Override
    public <E extends Exception> void forEachTriple(final int increment, final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            super.forEachTriple(increment, action);
            return;
        }

        final int windowSize = 3;
        checkArgPositive(increment, cs.increment);

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt curIndex = MutableInt.of(fromIndex);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        for (int i = 0; i < threadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, new Runnable() {
                private int cursor = -1;

                @Override
                public void run() {
                    try {
                        while (curIndex.value() < toIndex && eHolder.value() == null) {
                            synchronized (elements) {
                                if (curIndex.value() < toIndex) {
                                    cursor = curIndex.value();
                                    curIndex.setValue(increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex);
                                } else {
                                    break;
                                }
                            }

                            action.accept(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null,
                                    cursor < toIndex - 2 ? elements[cursor + 2] : null);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Accumulates the elements of this stream into a map in parallel. Each element is mapped to a
     * key/value pair. When using {@link Splitor#ARRAY}, each thread builds a partial map over its
     * slice; with {@link Splitor#ITERATOR}, a shared synchronized cursor is used. All per-thread
     * partial maps are then merged using {@code mergeFunction} to resolve key collisions.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param <M> the type of the resulting map
     * @param <E> the type of exception that {@code keyMapper} may throw
     * @param <E2> the type of exception that {@code valueMapper} may throw
     * @param keyMapper a function to produce map keys from elements
     * @param valueMapper a function to produce map values from elements
     * @param mergeFunction a function to resolve collisions between values with the same key
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a map containing the accumulated key/value pairs
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        // return collect(Collectors.toMap(keyMapper, valueMapper, mapFactory));

        //    final M res = mapFactory.get();
        //    res.putAll(collect(Collectors.toConcurrentMap(keyMapper, valueMapper, mergeFunction)));
        //    return res;

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<M>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    final M map = mapFactory.get();

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            Collectors.merge(map, keyMapper.apply(elements[cursor]), valueMapper.apply(elements[cursor]), mergeFunction);
                            cursor++;
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return map;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final M map = mapFactory.get();
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            Collectors.merge(map, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return map;
                });
            }
        }

        // checkException(eHolder, (E) null, asyncExecutor, asyncExecutorToUse);

        M res = null;

        try {
            for (final ContinuableFuture<M> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (res == null) {
                    res = future.get();
                } else {
                    final M m = future.get();

                    for (final Map.Entry<K, V> entry : m.entrySet()) {
                        Collectors.merge(res, entry.getKey(), entry.getValue(), mergeFunction);
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwException(eHolder, (E) null);
            }

            throw toRuntimeException(e);
        } finally {
            try {
                shutdownTempExecutor(asyncExecutorToUse, asyncExecutor);
            } finally {
                close();
            }
        }

        if (eHolder.value() != null) {
            throwException(eHolder, (E) null);
        }

        return res;
    }

    /**
     * Groups the elements of this stream in parallel using the given key and value mappers and a
     * downstream {@link Collector}. When using {@link Splitor#ARRAY}, each thread groups its slice
     * independently; with {@link Splitor#ITERATOR}, a shared synchronized cursor is used. Per-thread
     * partial grouping maps are then merged by combining per-key downstream containers using the
     * downstream collector's combiner. The downstream finisher is applied to each grouped container
     * at the end.
     *
     * @param <K> the type of grouping keys
     * @param <V> the type of values fed into the downstream collector
     * @param <D> the type of the downstream collector's result
     * @param <M> the type of the resulting map
     * @param <E> the type of exception that {@code keyMapper} may throw
     * @param <E2> the type of exception that {@code valueMapper} may throw
     * @param keyMapper a function to extract the grouping key from each element; must not return
     *        {@code null}
     * @param valueMapper a function to extract the value to accumulate for each element
     * @param downstream a collector describing how per-key values are accumulated and finished
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a map from keys to finished downstream results
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, D, M extends Map<K, D>, E extends Exception, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.groupTo(keyMapper, valueMapper, downstream, mapFactory);
        }

        // return collect(Collectors.groupingBy(keyMapper, downstream, mapFactory));

        //    final M res = mapFactory.get();
        //    res.putAll(collect(Collectors.groupingByConcurrent(keyMapper, downstream)));
        //    return res;

        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final BiConsumer<Object, ? super V> downstreamAccumulator = (BiConsumer<Object, ? super V>) downstream.accumulator();
        final BinaryOperator<Object> downstreamCombiner = (BinaryOperator<Object>) downstream.combiner();
        final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Map<K, Object>>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @SuppressWarnings("rawtypes")
                    final Map<K, Object> map = (Map) mapFactory.get();
                    K key = null;
                    Object valueContainer = null;

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            key = checkArgNotNull(keyMapper.apply(elements[cursor]), "element cannot be mapped to a null key"); //NOSONAR

                            valueContainer = map.get(key);

                            if (valueContainer == null) {
                                valueContainer = downstreamSupplier.get();
                                map.put(key, valueContainer);
                            }

                            downstreamAccumulator.accept(valueContainer, valueMapper.apply(elements[cursor]));

                            cursor++;
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return map;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    @SuppressWarnings("rawtypes")
                    final Map<K, Object> map = (Map) mapFactory.get();
                    K key = null;
                    Object valueContainer = null;
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            key = checkArgNotNull(keyMapper.apply(next), "element cannot be mapped to a null key");
                            valueContainer = map.get(key);

                            if (valueContainer == null) {
                                valueContainer = downstreamSupplier.get();
                                map.put(key, valueContainer);
                            }

                            downstreamAccumulator.accept(valueContainer, valueMapper.apply(next));
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return map;
                });
            }
        }

        // checkException(eHolder, (E) null, asyncExecutor, asyncExecutorToUse);

        Map<K, Object> intermediate = null;

        try {
            for (final ContinuableFuture<Map<K, Object>> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (intermediate == null) {
                    intermediate = future.get();
                } else {
                    final Map<K, Object> m = future.get();
                    K key = null;

                    for (final Map.Entry<K, Object> entry : m.entrySet()) {
                        key = entry.getKey();

                        if (intermediate.containsKey(key)) {
                            intermediate.put(key, downstreamCombiner.apply(intermediate.get(key), m.get(key)));
                        } else {
                            intermediate.put(key, m.get(key));
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwException(eHolder, (E) null);
            }

            throw toRuntimeException(e);
        } finally {
            try {
                shutdownTempExecutor(asyncExecutorToUse, asyncExecutor);
            } finally {
                close();
            }
        }

        if (eHolder.value() != null) {
            throwException(eHolder, (E) null);
        }

        final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

        //noinspection DataFlowIssue
        Collectors.replaceAll(intermediate, function);

        return (M) intermediate;
    }

    /**
     * Groups the elements of this stream in parallel using a flat key extractor and a downstream
     * {@link Collector}. For each element, {@code flatKeyExtractor} may return multiple keys; the
     * element is accumulated into the group for each key. When using {@link Splitor#ARRAY}, each
     * thread processes its pre-assigned slice; with {@link Splitor#ITERATOR}, a shared cursor is
     * used. Per-thread partial maps are merged by combining per-key downstream containers. The
     * downstream finisher is applied at the end.
     *
     * @param <K> the type of grouping keys
     * @param <V> the type of values fed into the downstream collector
     * @param <D> the type of the downstream collector's result
     * @param <M> the type of the resulting map
     * @param <E> the type of exception that {@code flatKeyExtractor} may throw
     * @param <E2> the type of exception that {@code valueMapper} may throw
     * @param flatKeyExtractor a function extracting a collection of keys from each element; each key
     *        must be non-{@code null}
     * @param valueMapper a bi-function mapping a key and an element to the value to accumulate
     * @param downstream a collector describing how per-key values are accumulated and finished
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a map from keys to finished downstream results
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the flat key extractor throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, D, M extends Map<K, D>, E extends Exception, E2 extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.flatGroupTo(flatKeyExtractor, valueMapper, downstream, mapFactory);
        }

        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final BiConsumer<Object, ? super V> downstreamAccumulator = (BiConsumer<Object, ? super V>) downstream.accumulator();
        final BinaryOperator<Object> downstreamCombiner = (BinaryOperator<Object>) downstream.combiner();
        final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Map<K, Object>>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    @SuppressWarnings("rawtypes")
                    final Map<K, Object> map = (Map) mapFactory.get();

                    Iterator<? extends K> keyIter = null;
                    K key = null;
                    Object valueContainer = null;

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            final Collection<? extends K> kc = flatKeyExtractor.apply(elements[cursor]);

                            if (N.notEmpty(kc)) {
                                keyIter = kc.iterator();

                                while (keyIter.hasNext()) {
                                    key = checkArgNotNull(keyIter.next(), "element cannot be mapped to a null key");
                                    valueContainer = map.get(key);

                                    if (valueContainer == null) {
                                        valueContainer = downstreamSupplier.get();
                                        map.put(key, valueContainer);
                                    }

                                    downstreamAccumulator.accept(valueContainer, valueMapper.apply(key, elements[cursor]));
                                }
                            }

                            cursor++;
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return map;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    @SuppressWarnings("rawtypes")
                    final Map<K, Object> map = (Map) mapFactory.get();

                    Iterator<? extends K> keyIter = null;
                    K key = null;
                    Object valueContainer = null;
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            final Collection<? extends K> kc = flatKeyExtractor.apply(next);

                            if (N.notEmpty(kc)) {
                                keyIter = kc.iterator();

                                while (keyIter.hasNext()) {
                                    key = checkArgNotNull(keyIter.next(), "element cannot be mapped to a null key");
                                    valueContainer = map.get(key);

                                    if (valueContainer == null) {
                                        valueContainer = downstreamSupplier.get();
                                        map.put(key, valueContainer);
                                    }

                                    downstreamAccumulator.accept(valueContainer, valueMapper.apply(key, next));
                                }
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return map;
                });
            }
        }

        // checkException(eHolder, (E) null, asyncExecutor, asyncExecutorToUse);

        Map<K, Object> intermediate = null;

        try {
            for (final ContinuableFuture<Map<K, Object>> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (intermediate == null) {
                    intermediate = future.get();
                } else {
                    final Map<K, Object> m = future.get();
                    K key = null;

                    for (final Map.Entry<K, Object> entry : m.entrySet()) {
                        key = entry.getKey();

                        if (intermediate.containsKey(key)) {
                            intermediate.put(key, downstreamCombiner.apply(intermediate.get(key), m.get(key)));
                        } else {
                            intermediate.put(key, m.get(key));
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwException(eHolder, (E) null);
            }

            throw toRuntimeException(e);
        } finally {
            try {
                shutdownTempExecutor(asyncExecutorToUse, asyncExecutor);
            } finally {
                close();
            }
        }

        if (eHolder.value() != null) {
            throwException(eHolder, (E) null);
        }

        final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

        //noinspection DataFlowIssue
        Collectors.replaceAll(intermediate, function);

        return (M) intermediate;
    }

    /**
     * Accumulates the elements of this stream into a {@link Multimap} in parallel. Each element is
     * mapped to a key/value pair. When using {@link Splitor#ARRAY}, each thread builds a partial
     * multimap over its slice; with {@link Splitor#ITERATOR}, a shared synchronized cursor is used.
     * All per-thread partial multimaps are then merged by putting all values from each partial
     * result into the final map.
     *
     * @param <K> the type of multimap keys
     * @param <V> the type of multimap values
     * @param <C> the type of the collection used to hold values for each key
     * @param <M> the type of the resulting multimap
     * @param <E> the type of exception that {@code keyMapper} may throw
     * @param <E2> the type of exception that {@code valueMapper} may throw
     * @param keyMapper a function to produce multimap keys from elements
     * @param valueMapper a function to produce multimap values from elements
     * @param mapFactory a supplier providing a new empty multimap into which results are inserted
     * @return a multimap containing all accumulated key/value pairs
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>, E extends Exception, E2 extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E> keyMapper, final Throwables.Function<? super T, ? extends V, E2> valueMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.toMultimap(keyMapper, valueMapper, mapFactory);
        }

        // return collect(Collectors.toMultimap(keyMapper, valueMapper, mapFactory));

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<M>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    final M map = mapFactory.get();

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            map.put(keyMapper.apply(elements[cursor]), valueMapper.apply(elements[cursor]));
                            cursor++;
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return map;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final M map = mapFactory.get();
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            map.put(keyMapper.apply(next), valueMapper.apply(next));
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return map;
                });
            }
        }

        // checkException(eHolder, (E) null, asyncExecutor, asyncExecutorToUse);

        M res = null;

        try {
            for (final ContinuableFuture<M> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (res == null) {
                    res = future.get();
                } else {
                    res.putValues(future.get());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwException(eHolder, (E) null);
            }

            throw toRuntimeException(e);
        } finally {
            try {
                shutdownTempExecutor(asyncExecutorToUse, asyncExecutor);
            } finally {
                close();
            }
        }

        if (eHolder.value() != null) {
            throwException(eHolder, (E) null);
        }

        return res;
    }

    /**
     * Performs a parallel reduction on the elements of this stream using the provided associative
     * accumulation function. Each thread independently reduces its assigned partition, and the partial
     * results are then combined using the same {@code accumulator}. Because the accumulator is used
     * for both per-thread accumulation and result combination, it must be associative. The
     * {@code accumulator} should also be stateless and non-interfering.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@link Optional} describing the result of the reduction, or an empty Optional if the
     *         stream is empty
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Optional<T> reduce(final BinaryOperator<T> accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.reduce(accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<T>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    if (cursor >= to) {
                        return (T) NONE;
                    }

                    T result = elements[cursor++];

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            result = accumulator.apply(result, elements[cursor++]);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    T result = null;

                    synchronized (elements) {
                        if (cursor.value() < toIndex) {
                            result = elements[cursor.getAndIncrement()];
                        } else {
                            return (T) NONE;
                        }
                    }

                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            result = accumulator.apply(result, next);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        }

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        T result = (T) NONE;

        try {
            for (final ContinuableFuture<T> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                final T tmp = future.get();

                if (tmp == NONE) {
                    // continue;
                } else if (result == NONE) {
                    result = tmp;
                } else {
                    result = accumulator.apply(result, tmp);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwRuntimeException(eHolder);
            }

            throw toRuntimeException(e);
        } finally {
            try {
                shutdownTempExecutor(asyncExecutorToUse, asyncExecutor);
            } finally {
                close();
            }
        }

        if (eHolder.value() != null) {
            throwRuntimeException(eHolder);
        }

        return result == NONE ? Optional.empty() : Optional.of(result);
    }

    /**
     * Performs a parallel reduction on the elements of this stream using the provided identity value,
     * accumulation function, and combining function. Each thread independently accumulates its partition
     * starting from {@code identity}, and the partial results are then combined with {@code combiner}.
     * The {@code accumulator} and {@code combiner} must be non-interfering and stateless.
     *
     * @param <U> the type of the result
     * @param identity the identity value for the combining function and the default value if there are
     *        no elements
     * @param accumulator a non-interfering, stateless function for incorporating an additional element
     *        into a result
     * @param combiner an associative, non-interfering, stateless function for combining two partial
     *        reduction results
     * @return the result of the reduction
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <U> U reduce(final U identity, final BiFunction<? super U, ? super T, U> accumulator, final BinaryOperator<U> combiner)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.reduce(identity, accumulator, combiner);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<U>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    U result = identity;

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            result = accumulator.apply(result, elements[cursor++]);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    U result = identity;
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            result = accumulator.apply(result, next);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        }

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        U result = (U) NONE;

        try {
            for (final ContinuableFuture<U> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (result == NONE) {
                    result = future.get();
                } else {
                    result = combiner.apply(result, future.get());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwRuntimeException(eHolder);
            }

            throw toRuntimeException(e);
        } finally {
            try {
                shutdownTempExecutor(asyncExecutorToUse, asyncExecutor);
            } finally {
                close();
            }
        }

        if (eHolder.value() != null) {
            throwRuntimeException(eHolder);
        }

        return result == NONE ? identity : result;
    }

    /**
     * Performs a mutable parallel reduction on the elements of this stream. Each thread creates its
     * own result container via {@code supplier}, accumulates elements into it using {@code accumulator},
     * and the partial containers are combined with {@code combiner}. The {@code accumulator} and
     * {@code combiner} must be non-interfering and stateless.
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container
     * @param accumulator a non-interfering, stateless function that folds an element into a result container
     * @param combiner a non-interfering, stateless function that merges two result containers; the second
     *        container's contents are merged into the first
     * @return the result of the mutable reduction
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super T> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.collect(supplier, accumulator, combiner);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<R>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    final R container = supplier.get();

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            accumulator.accept(container, elements[cursor++]);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return container;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final R container = supplier.get();
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            accumulator.accept(container, next);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return container;
                });
            }
        }

        return completeAndCollectResult(futureList, eHolder, supplier, combiner, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a mutable parallel reduction on the elements of this stream using a {@link Collector}.
     * If the collector has both the {@link java.util.stream.Collector.Characteristics#CONCURRENT CONCURRENT}
     * and {@link java.util.stream.Collector.Characteristics#UNORDERED UNORDERED} characteristics, a single
     * shared container is used for accumulation across all threads. Otherwise,
     * each thread accumulates into its own container and the partial containers are combined using the
     * collector's combiner.
     *
     * @param <R> the type of the result
     * @param collector the {@code Collector} describing the reduction
     * @return the result of the reduction
     * @throws IllegalStateException if the stream has already been closed
     * @see java.util.stream.Collectors
     */
    @Override
    public <R> R collect(final Collector<? super T, ?, R> collector) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return sequential().collect(collector);
        }

        //    if (/*collector.characteristics().contains(Collector.Characteristics.CONCURRENT) == false
        //                   || */ collector.characteristics().contains(Collector.Characteristics.UNORDERED) == false) {
        //        return sequential().collect(collector);
        //    }

        final boolean isConcurrentCollector = N.notEmpty(collector.characteristics())
                && collector.characteristics().contains(Collector.Characteristics.CONCURRENT)
                && collector.characteristics().contains(Collector.Characteristics.UNORDERED);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final BinaryOperator<Object> combiner = (BinaryOperator<Object>) collector.combiner();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Object>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Object singleContainer = isConcurrentCollector ? supplier.get() : null;
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    final Object container = isConcurrentCollector ? singleContainer : supplier.get();

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            accumulator.accept(container, elements[cursor++]);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return container;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final Object container = isConcurrentCollector ? singleContainer : supplier.get();
                    T next = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            accumulator.accept(container, next);
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }

                    return container;
                });
            }
        }

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        Object container = isConcurrentCollector ? singleContainer : NONE;

        try {
            for (final ContinuableFuture<Object> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (isConcurrentCollector) {
                    future.get();
                } else {
                    if (container == NONE) {
                        container = future.get();
                    } else {
                        container = combiner.apply(container, future.get());
                    }
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwRuntimeException(eHolder);
            }

            throw toRuntimeException(e);
        } finally {
            try {
                shutdownTempExecutor(asyncExecutorToUse, asyncExecutor);
            } finally {
                close();
            }
        }

        if (eHolder.value() != null) {
            throwRuntimeException(eHolder);
        }

        return finisher.apply(container == NONE ? supplier.get() : container);
    }

    /**
     * Returns the minimum element of this stream according to the provided comparator, determined in
     * parallel. If the stream is already sorted with the same comparator, the first element is returned
     * directly without parallel scanning.
     *
     * @param comparator a non-interfering, stateless comparator to compare elements; if {@code null},
     *        a natural-ordering comparator treating {@code null} values as larger is used
     * @return an {@link Optional} describing the minimum element, or an empty Optional if the stream
     *         is empty
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Optional<T> min(Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator(), comparator)) {
                return Optional.of(elements[fromIndex]);
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        comparator = comparator == null ? NULL_MAX_COMPARATOR : comparator;

        return collect(Collectors.min(comparator));
    }

    /**
     * Returns the maximum element of this stream according to the provided comparator, determined in
     * parallel. If the stream is already sorted with the same comparator, the last element is returned
     * directly without parallel scanning.
     *
     * @param comparator a non-interfering, stateless comparator to compare elements; if {@code null},
     *        a natural-ordering comparator treating {@code null} values as smaller is used
     * @return an {@link Optional} describing the maximum element, or an empty Optional if the stream
     *         is empty
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Optional<T> max(Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator(), comparator)) {
                return Optional.of(elements[toIndex - 1]);
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        comparator = comparator == null ? NULL_MIN_COMPARATOR : comparator;

        return collect(Collectors.max(comparator));
    }

    /**
     * Returns whether any element of this stream matches the provided predicate. Uses short-circuit
     * parallel evaluation: once one thread finds a matching element, all threads are signalled to stop.
     * Returns {@code false} if the stream is empty.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if any element matches the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.anyMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(false);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    try {
                        while (cursor < to && result.isFalse() && eHolder.value() == null) {
                            if (predicate.test(elements[cursor++])) {
                                result.setTrue();
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    T next = null;

                    try {
                        while (result.isFalse() && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(next)) {
                                result.setTrue();
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns whether all elements of this stream match the provided predicate. Uses short-circuit
     * parallel evaluation: once one thread finds a non-matching element, all threads are signalled
     * to stop. Returns {@code true} if the stream is empty.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if all elements match the predicate (or the stream is empty),
     *         {@code false} otherwise
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.allMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    try {
                        while (cursor < to && result.isTrue() && eHolder.value() == null) {
                            if (!predicate.test(elements[cursor++])) {
                                result.setFalse();
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    T next = null;

                    try {
                        while (result.isTrue() && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            if (!predicate.test(next)) {
                                result.setFalse();
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns whether no element of this stream matches the provided predicate. Uses short-circuit
     * parallel evaluation: once one thread finds a matching element, all threads are signalled to stop.
     * Returns {@code true} if the stream is empty.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if no elements match the predicate (or the stream is empty),
     *         {@code false} otherwise
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.noneMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    try {
                        while (cursor < to && result.isTrue() && eHolder.value() == null) {
                            if (predicate.test(elements[cursor++])) {
                                result.setFalse();
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    T next = null;

                    try {
                        while (result.isTrue() && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(next)) {
                                result.setFalse();
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns whether the number of elements matching the predicate falls within the inclusive range
     * {@code [atLeast, atMost]}. Evaluation is performed in parallel; threads stop as soon as the
     * count exceeds {@code atMost}. The final answer is determined after all threads have finished.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param atLeast the minimum number of matching elements required (inclusive, non-negative)
     * @param atMost the maximum number of matching elements allowed (inclusive, non-negative,
     *        must be &gt;= {@code atLeast})
     * @param predicate a non-interfering, stateless predicate to test each element
     * @return {@code true} if the match count is in {@code [atLeast, atMost]}, {@code false} otherwise
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code atLeast} or {@code atMost} is negative, or if
     *         {@code atLeast > atMost}
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean hasMatchCountBetween(final long atLeast, final long atMost, final Throwables.Predicate<? super T, E> predicate)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNegative(atLeast, cs.atLeast);
        checkArgNotNegative(atMost, cs.atMost);
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.hasMatchCountBetween(atLeast, atMost, predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final AtomicLong cnt = new AtomicLong(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    try {
                        while (cursor < to && cnt.get() <= atMost && eHolder.value() == null) {
                            if (predicate.test(elements[cursor++]) && (cnt.incrementAndGet() > atMost)) {
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    T next = null;

                    try {
                        while (cnt.get() <= atMost && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(next) && (cnt.incrementAndGet() > atMost)) {
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return cnt.get() >= atLeast && cnt.get() <= atMost;
    }

    /**
     * Returns the element with the lowest index in this stream that matches the predicate, found via
     * parallel search. Multiple threads scan different ranges concurrently; the result is the matching
     * element at the smallest array index across all threads. Uses short-circuit evaluation: once a
     * candidate is found, threads searching higher indices are stopped.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to test each element
     * @return an {@link Optional} describing the first (lowest-index) matching element, or an empty
     *         Optional if no element matches
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> Optional<T> findFirst(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findFirst(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, T>> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    final Pair<Integer, T> pair = new Pair<>();

                    try {
                        while (cursor < to && (resultHolder.value() == null || cursor < resultHolder.value().left()) && eHolder.value() == null) {
                            pair.setLeft(cursor);
                            pair.setRight(elements[cursor++]);

                            if (predicate.test(pair.right())) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || pair.left() < resultHolder.value().left()) {
                                        resultHolder.setValue(pair.copy());
                                    }
                                }

                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final Pair<Integer, T> pair = new Pair<>();

                    try {
                        while (resultHolder.value() == null && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    pair.setLeft(cursor.value());
                                    pair.setRight(elements[cursor.getAndIncrement()]);
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(pair.right())) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || pair.left() < resultHolder.value().left()) {
                                        resultHolder.setValue(pair.copy());
                                    }
                                }

                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? Optional.empty() : Optional.of(resultHolder.value().right());
    }

    /**
     * Returns any element of this stream that matches the predicate, found via parallel search.
     * Multiple threads search concurrently; the first matching element found by any thread is returned.
     * Unlike {@link #findFirst(Throwables.Predicate)}, no ordering guarantee is provided — the result
     * may differ between runs. Uses short-circuit evaluation: once a match is found, other threads are
     * signalled to stop.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to test each element
     * @return an {@link Optional} describing any matching element, or an empty Optional if no element
     *         matches
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> Optional<T> findAny(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findAny(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<T> resultHolder = Holder.of((T) NONE);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    T next = null;

                    try {
                        while (cursor < to && resultHolder.value() == NONE && eHolder.value() == null) {
                            next = elements[cursor++];

                            if (predicate.test(next)) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == NONE) {
                                        resultHolder.setValue(next);
                                    }
                                }

                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    T next = null;

                    try {
                        while (resultHolder.value() == NONE && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(next)) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == NONE) {
                                        resultHolder.setValue(next);
                                    }
                                }

                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == NONE ? Optional.empty() : Optional.of(resultHolder.value());
    }

    /**
     * Returns the element with the highest index in this stream that matches the predicate, found via
     * parallel reverse search. Multiple threads scan different ranges in reverse concurrently; each
     * thread short-circuits after finding its own first (highest-index) candidate within its range.
     * The result is the matching element at the largest array index across all threads.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to test each element
     * @return an {@link Optional} describing the last (highest-index) matching element, or an empty
     *         Optional if no element matches
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> Optional<T> findLast(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findLast(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, T>> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final int from = fromIndex + sliceIndex * sliceSize;
                    int cursor = toIndex - from > sliceSize ? from + sliceSize : toIndex;
                    final Pair<Integer, T> pair = new Pair<>();

                    try {
                        while (cursor > from && (resultHolder.value() == null || cursor > resultHolder.value().left()) && eHolder.value() == null) {
                            pair.setLeft(cursor);
                            pair.setRight(elements[--cursor]);

                            if (predicate.test(pair.right())) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || pair.left() > resultHolder.value().left()) {
                                        resultHolder.setValue(pair.copy());
                                    }
                                }

                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(toIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final Pair<Integer, T> pair = new Pair<>();

                    try {
                        while (resultHolder.value() == null && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() > fromIndex) {
                                    pair.setLeft(cursor.value());
                                    pair.setRight(elements[cursor.decrementAndGet()]);
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(pair.right())) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || pair.left() > resultHolder.value().left()) {
                                        resultHolder.setValue(pair.copy());
                                    }
                                }

                                break;
                            }
                        }
                    } catch (final Exception e) {
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? Optional.empty() : Optional.of(resultHolder.value().right());
    }

    /**
     * Returns a parallel stream consisting of the elements of this stream whose mapped key is
     * contained in collection {@code c}, considering multiplicity. Each key occurrence is matched
     * and consumed from the collection at most once. The mapper is applied in parallel; access to
     * the shared {@link com.landawn.abacus.util.Multiset} tracking remaining candidates is
     * synchronized.
     *
     * @param <U> the type of the key used for matching
     * @param mapper a function to extract the key from each element
     * @param c the collection of keys; duplicates are treated as separate candidates
     * @return a new parallel stream of elements whose keys are in {@code c}
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <U> Stream<T> intersection(final Function<? super T, ? extends U> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().intersection(mapper, c).iteratorEx(), isSorted(), comparator(), maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, closeHandlers());
        }

        final Multiset<?> multiset = Multiset.create(c);

        if (this.isParallel()) {
            return filter(value -> {
                final Object key = mapper.apply(value);

                synchronized (multiset) {
                    if (multiset.isEmpty()) {
                        return false;
                    }

                    return multiset.remove(key);
                }
            });
        } else {
            return filter(value -> {
                final Object key = mapper.apply(value);

                if (multiset.isEmpty()) {
                    return false;
                }

                return multiset.remove(key);

            });
        }
    }

    /**
     * Returns a parallel stream consisting of the elements of this stream whose mapped key is
     * <em>not</em> contained in collection {@code c}, considering multiplicity. Each key occurrence
     * is matched and consumed from the collection at most once so that only the "extra" occurrences
     * remain in the output. The mapper is applied in parallel; access to the shared
     * {@link com.landawn.abacus.util.Multiset} is synchronized.
     *
     * @param <U> the type of the key used for matching
     * @param mapper a function to extract the key from each element
     * @param c the collection of keys to exclude; duplicates are treated as separate exclusions
     * @return a new parallel stream of elements whose keys are not in {@code c}
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <U> Stream<T> difference(final Function<? super T, ? extends U> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().difference(mapper, c).iteratorEx(), isSorted(), comparator(), maxThreadNum, splitor, asyncExecutor,
                    cancelUncompletedThreads, closeHandlers());
        }

        final Multiset<?> multiset = Multiset.create(c);

        if (this.isParallel()) {
            return filter(value -> {
                final Object key = mapper.apply(value);

                synchronized (multiset) {
                    if (multiset.isEmpty()) {
                        return true;
                    }

                    return !multiset.remove(key);
                }
            });
        } else {
            return filter(value -> {
                final Object key = mapper.apply(value);

                if (multiset.isEmpty()) {
                    return true;
                }

                return !multiset.remove(key);

            });
        }
    }

    /**
     * Returns a parallel stream that is the concatenation of this stream followed by the elements of
     * the given stream. The resulting stream preserves the parallel configuration (thread count,
     * splitor, and executor) of this stream.
     *
     * @param stream the stream to append; its elements follow the elements of this stream
     * @return a new parallel stream that concatenates this stream with the provided stream
     * @throws IllegalStateException if this stream has already been closed
     */
    @Override
    public Stream<T> append(final Stream<T> stream) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.concat(this, stream), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream that is the concatenation of the given stream followed by the elements
     * of this stream. The resulting stream preserves the parallel configuration (thread count,
     * splitor, and executor) of this stream.
     *
     * @param stream the stream to prepend; its elements precede the elements of this stream
     * @return a new parallel stream that concatenates the provided stream with this stream
     * @throws IllegalStateException if this stream has already been closed
     */
    @Override
    public Stream<T> prepend(final Stream<T> stream) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.concat(stream, this), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a new parallel stream that is the result of merging this stream with the given
     * collection using a selector function. At each step, {@code nextSelector} is applied to the
     * current head elements of both sequences to decide which one to emit next. The returned stream
     * inherits the parallel configuration of this stream.
     *
     * @param b the collection to merge with this stream
     * @param nextSelector a function that determines which of the two candidate elements to emit;
     *        returns {@link MergeResult#TAKE_FIRST} to take from this stream, or
     *        {@link MergeResult#TAKE_SECOND} to take from {@code b}
     * @return a new parallel stream of merged elements
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> mergeWith(final Collection<? extends T> b, final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.merge(iteratorEx(), N.iterate(b), nextSelector), false, null, maxThreadNum, splitor, asyncExecutor,
                cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a new parallel stream that is the result of merging this stream with the given stream
     * using a selector function. At each step, {@code nextSelector} is applied to the current head
     * elements of both streams to decide which one to emit next. The returned stream inherits the
     * parallel configuration of this stream.
     *
     * @param b the stream to merge with this stream
     * @param nextSelector a function that determines which of the two candidate elements to emit;
     *        returns {@link MergeResult#TAKE_FIRST} to take from this stream, or
     *        {@link MergeResult#TAKE_SECOND} to take from {@code b}
     * @return a new parallel stream of merged elements
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> mergeWith(final Stream<? extends T> b, final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.merge(this, b, nextSelector), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                null);
    }

    /**
     * Returns a parallel stream whose elements are the result of applying {@code zipFunction} to
     * corresponding element pairs from this stream and collection {@code b}. The zip terminates
     * when the shorter of the two sequences is exhausted.
     *
     * @param <T2> the element type of collection {@code b}
     * @param <R> the type of the zipped results
     * @param b the collection to zip with this stream
     * @param zipFunction a function to combine corresponding elements
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <T2, R> Stream<R> zipWith(final Collection<T2> b, final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.parallelZip(iteratorEx(), N.iterate(b), zipFunction, maxThreadNum), false, null, maxThreadNum, splitor,
                asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream whose elements are the result of applying {@code zipFunction} to
     * corresponding element pairs from this stream and collection {@code b}. If one sequence is
     * shorter, the substitution values {@code valueForNoneA} or {@code valueForNoneB} are used to
     * pad the shorter sequence so that all elements from both are included.
     *
     * @param <T2> the element type of collection {@code b}
     * @param <R> the type of the zipped results
     * @param b the collection to zip with this stream
     * @param valueForNoneA the padding value used when this stream is exhausted
     * @param valueForNoneB the padding value used when {@code b} is exhausted
     * @param zipFunction a function to combine corresponding elements
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <T2, R> Stream<R> zipWith(final Collection<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.parallelZip(iteratorEx(), N.iterate(b), valueForNoneA, valueForNoneB, zipFunction, maxThreadNum), false,
                null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream whose elements are the result of applying {@code zipFunction} to
     * corresponding element triples from this stream and collections {@code b} and {@code c}. The
     * zip terminates when the shortest of the three sequences is exhausted.
     *
     * @param <T2> the element type of collection {@code b}
     * @param <T3> the element type of collection {@code c}
     * @param <R> the type of the zipped results
     * @param b the second collection to zip with this stream
     * @param c the third collection to zip with this stream
     * @param zipFunction a function to combine corresponding element triples
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <T2, T3, R> Stream<R> zipWith(final Collection<T2> b, final Collection<T3> c,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.parallelZip(iteratorEx(), N.iterate(b), N.iterate(c), zipFunction, maxThreadNum), false, null, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream whose elements are the result of applying {@code zipFunction} to
     * corresponding element triples from this stream and collections {@code b} and {@code c}. If
     * any sequence is shorter, the corresponding substitution value is used to pad it so that all
     * elements from all three sequences are included.
     *
     * @param <T2> the element type of collection {@code b}
     * @param <T3> the element type of collection {@code c}
     * @param <R> the type of the zipped results
     * @param b the second collection to zip with this stream
     * @param c the third collection to zip with this stream
     * @param valueForNoneA the padding value used when this stream is exhausted
     * @param valueForNoneB the padding value used when {@code b} is exhausted
     * @param valueForNoneC the padding value used when {@code c} is exhausted
     * @param zipFunction a function to combine corresponding element triples
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <T2, T3, R> Stream<R> zipWith(final Collection<T2> b, final Collection<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(
                Stream.parallelZip(iteratorEx(), N.iterate(b), N.iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, maxThreadNum), false,
                null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream whose elements are the result of applying {@code zipFunction} to
     * corresponding element pairs from this stream and stream {@code b}. The zip terminates when
     * the shorter of the two streams is exhausted.
     *
     * @param <T2> the element type of stream {@code b}
     * @param <R> the type of the zipped results
     * @param b the stream to zip with this stream
     * @param zipFunction a function to combine corresponding elements
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <T2, R> Stream<R> zipWith(final Stream<T2> b, final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.parallelZip(this, b, zipFunction, maxThreadNum), false, null, maxThreadNum, splitor, asyncExecutor,
                cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream whose elements are the result of applying {@code zipFunction} to
     * corresponding element pairs from this stream and stream {@code b}. If one stream is shorter,
     * the substitution values {@code valueForNoneA} or {@code valueForNoneB} are used to pad it so
     * that all elements from both streams are included.
     *
     * @param <T2> the element type of stream {@code b}
     * @param <R> the type of the zipped results
     * @param b the stream to zip with this stream
     * @param valueForNoneA the padding value used when this stream is exhausted
     * @param valueForNoneB the padding value used when {@code b} is exhausted
     * @param zipFunction a function to combine corresponding elements
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <T2, R> Stream<R> zipWith(final Stream<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.parallelZip(this, b, valueForNoneA, valueForNoneB, zipFunction, maxThreadNum), false, null, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream whose elements are the result of applying {@code zipFunction} to
     * corresponding element triples from this stream and streams {@code b} and {@code c}. The zip
     * terminates when the shortest of the three streams is exhausted.
     *
     * @param <T2> the element type of stream {@code b}
     * @param <T3> the element type of stream {@code c}
     * @param <R> the type of the zipped results
     * @param b the second stream to zip with this stream
     * @param c the third stream to zip with this stream
     * @param zipFunction a function to combine corresponding element triples
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.parallelZip(this, b, c, zipFunction, maxThreadNum), false, null, maxThreadNum, splitor, asyncExecutor,
                cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream whose elements are the result of applying {@code zipFunction} to
     * corresponding element triples from this stream and streams {@code b} and {@code c}. If any
     * stream is shorter, the corresponding substitution value is used to pad it so that all elements
     * from all three streams are included.
     *
     * @param <T2> the element type of stream {@code b}
     * @param <T3> the element type of stream {@code c}
     * @param <R> the type of the zipped results
     * @param b the second stream to zip with this stream
     * @param c the third stream to zip with this stream
     * @param valueForNoneA the padding value used when this stream is exhausted
     * @param valueForNoneB the padding value used when {@code b} is exhausted
     * @param valueForNoneC the padding value used when {@code c} is exhausted
     * @param zipFunction a function to combine corresponding element triples
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if any required argument is invalid
     */
    @Override
    public <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.parallelZip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, maxThreadNum), false, null,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns {@code true}, indicating that this stream performs operations in parallel.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParallel() {
        return true;
    }

    /**
     * Returns a sequential {@link ArrayStream} view of this parallel stream's data. The sequential
     * stream shares the same underlying array segment and close handlers. The result is cached so
     * subsequent calls return the same instance.
     *
     * @return a sequential stream backed by the same array range as this parallel stream
     * @throws IllegalStateException if this stream has already been closed
     */
    @Override
    public Stream<T> sequential() throws IllegalStateException {
        assertNotClosed();

        ArrayStream<T> tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;
                if (tmp == null) {
                    tmp = new ArrayStream<>(elements, fromIndex, toIndex, isSorted(), comparator(), closeHandlers());
                    sequential = tmp;
                }
            }
        }

        return tmp;
    }

    @Override
    protected int maxThreadNum() {
        // assertNotClosed();

        return maxThreadNum;
    }

    @Override
    protected BaseStream.Splitor splitor() {
        //  assertNotClosed();

        return splitor;
    }

    @Override
    protected AsyncExecutor asyncExecutor() {
        // assertNotClosed();

        return asyncExecutor;
    }
}
