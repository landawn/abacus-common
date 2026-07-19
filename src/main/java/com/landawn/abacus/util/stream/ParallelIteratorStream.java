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
import com.landawn.abacus.util.MutableLong;
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
 * A parallel implementation of Stream backed by an iterator that enables concurrent processing
 * of elements across multiple threads. This class extends IteratorStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution distributes work using synchronized iterator access to ensure
 * thread-safe element consumption while maintaining high throughput for CPU-intensive operations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel stream from an iterator
 * Stream<String> stream = Stream.of(iterator).parallel();
 *
 * // Process elements in parallel
 * long count = stream.filter(s -> s.length() > 5).count();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> A parallel operation synchronizes its own worker threads while they
 * consume the underlying iterator. The stream instance itself is not intended to be driven
 * concurrently by multiple callers or reused for independent operations.
 *
 * <p><b>Failure Handling:</b> Terminal operations wait for every submitted worker before closing
 * the stream or a temporary executor. Partial results are finished before cleanup; the primary
 * failure is rethrown and additional failures are suppressed.
 *
 * @param <T> the type of elements in the stream
 * @see IteratorStream
 * @see Stream#parallel()
 */
@SuppressFBWarnings("NM_WRONG_PACKAGE")
final class ParallelIteratorStream<T> extends IteratorStream<T> {
    private final int maxThreadNum;
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorStream<T> sequential;

    /**
     * Constructs a ParallelIteratorStream from an Iterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the Iterator to stream from
     * @param sorted whether the iterator elements are in sorted order
     * @param comparator the comparator used to order elements, or {@code null} if using natural ordering
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorStream(final Iterator<? extends T> values, final boolean sorted, final Comparator<? super T> comparator, final int maxThreadNum,
            final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, comparator, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorStream from a Stream with the specified configuration for parallel processing.
     * The stream is converted to an iterator internally.
     *
     * @param stream the Stream to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param comparator the comparator used to order elements, or {@code null} if using natural ordering
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorStream(final Stream<T> stream, final boolean sorted, final Comparator<? super T> comparator, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(iterate(stream), sorted, comparator, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Returns a parallel stream consisting of the elements of this stream that match the given predicate.
     * Elements are consumed from the underlying iterator via a shared synchronized cursor and tested
     * concurrently across multiple threads. The output order is not guaranteed to match the input order.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element to determine inclusion
     * @return a new parallel stream of matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> filter(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final List<Iterator<T>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (true) {
                            synchronized (elements) {
                                if (elements.hasNext()) {
                                    next = elements.next();
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

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream consisting of the elements of this stream that are taken while the
     * given predicate holds. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor and tested concurrently. Once any thread encounters an element that does
     * not match the predicate, an {@link AtomicBoolean} flag signals all threads to stop producing
     * further elements. The output order is not guaranteed to match the input order.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix
     * semantics; later matching elements may be returned.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element; the stream
     *        stops when the predicate returns {@code false}
     * @return a new parallel stream of matching elements selected by the parallel operation
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> takeWhile(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final List<Iterator<T>> iters = new ArrayList<>(maxThreadNum);
        final AtomicBoolean hasMore = new AtomicBoolean(true);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext && hasMore.get()) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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
     * Returns a parallel stream that drops elements while the given predicate holds and then passes
     * all remaining elements through. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor. Each thread loops, pulling and testing elements, until one of them
     * encounters an element that fails the predicate (signalled by an {@link AtomicBoolean} flag);
     * after that, all threads proceed concurrently to consume the
     * remainder of the stream. The output order of post-drop elements is not guaranteed to match the
     * input order.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix/suffix
     * semantics; later matching elements may be dropped.
     *
     * @param predicate a non-interfering, stateless predicate; elements are dropped while it returns
     *        {@code true}
     * @return a new parallel stream of elements selected by the parallel drop operation
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final List<Iterator<T>> iters = new ArrayList<>(maxThreadNum);
        final AtomicBoolean dropped = new AtomicBoolean(false);

        for (int i = 0; i < maxThreadNum; i++) {
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
                                if (elements.hasNext()) {
                                    next = elements.next();
                                    hasNext = true;
                                }
                            }
                        } else {
                            while (!dropped.get()) {
                                synchronized (elements) {
                                    if (elements.hasNext()) {
                                        next = elements.next();
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
                                    if (elements.hasNext()) {
                                        next = elements.next();
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
     * Returns a parallel stream consisting of the results of applying the given mapper function to
     * the elements of this stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor, and the mapper is applied concurrently across multiple threads. The output
     * order is not guaranteed to match the input order.
     *
     * @param <R> the type of the elements in the returned stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel stream of mapped results
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> Stream<R> map(final Function<? super T, ? extends R> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        final List<Iterator<R>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream produced by applying a {@link BiFunction} mapper over a sliding
     * window of size 2 across the elements of this stream. Elements are consumed from the underlying
     * iterator via a shared synchronized cursor. The {@code increment} parameter controls how many
     * positions the window advances between successive invocations. If {@code ignoreNotPaired} is
     * {@code true}, an incomplete final window (where the second element is absent) is silently
     * dropped; otherwise it is passed to the mapper with a {@code null} second argument.
     *
     * @param <R> the type of the elements in the returned stream
     * @param increment the number of positions to advance the window between applications; must be
     *        positive
     * @param ignoreNotPaired if {@code true}, skip the final window when fewer than 2 elements remain
     * @param mapper a non-interfering, stateless function applied to each pair of elements
     * @return a new parallel stream of mapped pair results
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code increment} is not positive
     */
    @Override
    public <R> Stream<R> slidingMap(final int increment, final boolean ignoreNotPaired, final BiFunction<? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().slidingMap(increment, ignoreNotPaired, mapper).iteratorEx(), false, null, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, closeHandlers());
        }

        final int windowSize = 2;
        checkArgPositive(increment, cs.increment); //NOSONAR

        final List<Iterator<R>> iters = new ArrayList<>(maxThreadNum);
        final MutableBoolean isFirst = MutableBoolean.of(true);
        final Holder<T> prev = new Holder<>();

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                @SuppressWarnings("unchecked")
                private final T NONE = (T) StreamBase.NONE; //NOSONAR

                private T first = NONE;
                private T second = NONE;

                @Override
                public boolean hasNext() {
                    if (first == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                if (increment > windowSize && isFirst.isFalse()) {
                                    int skipNum = increment - windowSize;

                                    while (skipNum-- > 0 && elements.hasNext()) {
                                        elements.next();
                                    }
                                }

                                if (elements.hasNext()) {
                                    if (increment == 1) {
                                        first = isFirst.isTrue() ? elements.next() : prev.value();
                                        second = elements.hasNext() ? elements.next() : NONE;

                                        prev.setValue(second);

                                    } else {
                                        first = elements.next();
                                        second = elements.hasNext() ? elements.next() : NONE;
                                    }
                                }

                                isFirst.setFalse();
                            }
                        }
                    }

                    return ignoreNotPaired ? second != NONE : first != NONE;
                }

                @Override
                public R next() {
                    if ((ignoreNotPaired ? second == NONE : first == NONE) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final R result = mapper.apply(first, second == NONE ? null : second);
                    first = NONE;
                    second = NONE;
                    return result;
                }
            });
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream produced by applying a {@link TriFunction} mapper over a sliding
     * window of size 3 across the elements of this stream. Elements are consumed from the underlying
     * iterator via a shared synchronized cursor. The {@code increment} parameter controls how many
     * positions the window advances between successive invocations. If {@code ignoreNotPaired} is
     * {@code true}, an incomplete final window (where the third element is absent) is silently
     * dropped; otherwise missing trailing elements are passed as {@code null} to the mapper.
     *
     * @param <R> the type of the elements in the returned stream
     * @param increment the number of positions to advance the window between applications; must be
     *        positive
     * @param ignoreNotPaired if {@code true}, skip the final window when fewer than 3 elements remain
     * @param mapper a non-interfering, stateless function applied to each triple of elements
     * @return a new parallel stream of mapped triple results
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code increment} is not positive
     */
    @Override
    public <R> Stream<R> slidingMap(final int increment, final boolean ignoreNotPaired, final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().slidingMap(increment, ignoreNotPaired, mapper).iteratorEx(), false, null, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, closeHandlers());
        }

        final int windowSize = 3;
        checkArgPositive(increment, cs.increment);

        final List<Iterator<R>> iters = new ArrayList<>(maxThreadNum);
        final MutableBoolean isFirst = MutableBoolean.of(true);
        final Holder<T> prev = new Holder<>();
        final Holder<T> prev2 = new Holder<>();

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                @SuppressWarnings("unchecked")
                private final T NONE = (T) StreamBase.NONE; //NOSONAR

                private T first = NONE;
                private T second = NONE;
                private T third = NONE;

                @Override
                public boolean hasNext() {
                    if (first == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                if (increment > windowSize && isFirst.isFalse()) {
                                    int skipNum = increment - windowSize;

                                    while (skipNum-- > 0 && elements.hasNext()) {
                                        elements.next();
                                    }
                                }

                                if (elements.hasNext()) {
                                    if (increment == 1) {
                                        first = isFirst.isTrue() ? elements.next() : prev2.value();
                                        second = isFirst.isTrue() ? (elements.hasNext() ? elements.next() : NONE) : prev.value();
                                        third = elements.hasNext() ? elements.next() : NONE;

                                        prev2.setValue(second);
                                        prev.setValue(third);
                                    } else if (increment == 2) {
                                        first = isFirst.isTrue() ? elements.next() : prev.value();
                                        second = elements.hasNext() ? elements.next() : NONE;
                                        third = elements.hasNext() ? elements.next() : NONE;

                                        prev.setValue(third);
                                    } else {
                                        first = elements.next();
                                        second = elements.hasNext() ? elements.next() : NONE;
                                        third = elements.hasNext() ? elements.next() : NONE;
                                    }
                                }

                                isFirst.setFalse();
                            }
                        }
                    }

                    return ignoreNotPaired ? third != NONE : first != NONE;
                }

                @Override
                public R next() {
                    if ((ignoreNotPaired ? third == NONE : first == NONE) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final R result = mapper.apply(first, second == NONE ? null : second, third == NONE ? null : third);
                    first = NONE;
                    second = NONE;
                    third = NONE;
                    return result;
                }
            });
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream in which the first element is mapped with {@code mapperForFirst} and
     * all remaining elements are mapped with {@code mapperForElse}. The first element is consumed
     * eagerly from the underlying iterator before the parallel pipeline is assembled; the rest of the
     * elements are processed concurrently.
     *
     * @param <R> the type of the elements in the returned stream
     * @param mapperForFirst a non-interfering, stateless function applied to the first element
     * @param mapperForElse a non-interfering, stateless function applied to all elements after the
     *        first
     * @return a new parallel stream with the split mapping applied
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> Stream<R> mapFirstOrElse(final Function<? super T, ? extends R> mapperForFirst, final Function<? super T, ? extends R> mapperForElse)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapFirstOrElse(mapperForFirst, mapperForElse);
        }

        if (elements.hasNext()) {
            final Function<T, R> mapperForFirst2 = (Function<T, R>) mapperForFirst;
            final Function<T, R> mapperForElse2 = (Function<T, R>) mapperForElse;
            final T first = elements.next();

            //noinspection resource
            return map(mapperForElse2).prepend(Stream.of(first).map(mapperForFirst2));
        } else {
            return (Stream<R>) this;
        }
    }

    /**
     * Returns a parallel stream in which the last element is mapped with {@code mapperForLast} and
     * all other elements are mapped with {@code mapperForElse}. Elements are consumed from the
     * underlying iterator via a shared synchronized cursor; each thread checks whether the iterator
     * is exhausted after consuming an element to identify the last one. The output order is not
     * guaranteed to match the input order.
     *
     * @param <R> the type of the elements in the returned stream
     * @param mapperForLast a non-interfering, stateless function applied to the last element
     * @param mapperForElse a non-interfering, stateless function applied to all elements before the
     *        last
     * @return a new parallel stream with the split mapping applied
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> Stream<R> mapLastOrElse(final Function<? super T, ? extends R> mapperForLast, final Function<? super T, ? extends R> mapperForElse)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapLastOrElse(mapperForLast, mapperForElse);
        }

        final List<Iterator<R>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;
                private boolean isLast = false;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();

                                if (!elements.hasNext()) {
                                    isLast = true;
                                }
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

                    final R result = isLast ? mapperForLast.apply((T) next) : mapperForElse.apply((T) next);
                    next = NONE;
                    return result;
                }
            });
        }

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel {@link CharStream} consisting of the results of applying the given mapper
     * to the elements of this stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor and mapped concurrently across multiple threads. The output order is not
     * guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code CharStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream mapToChar(final ToCharFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToChar(mapper);
        }

        final List<Iterator<Character>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return new ParallelIteratorCharStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link ByteStream} consisting of the results of applying the given mapper
     * to the elements of this stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor and mapped concurrently across multiple threads. The output order is not
     * guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code ByteStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream mapToByte(final ToByteFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToByte(mapper);
        }

        final List<Iterator<Byte>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return new ParallelIteratorByteStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link ShortStream} consisting of the results of applying the given mapper
     * to the elements of this stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor and mapped concurrently across multiple threads. The output order is not
     * guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code ShortStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream mapToShort(final ToShortFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToShort(mapper);
        }

        final List<Iterator<Short>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return new ParallelIteratorShortStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link IntStream} consisting of the results of applying the given mapper
     * to the elements of this stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor and mapped concurrently across multiple threads. The output order is not
     * guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code IntStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final ToIntFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToInt(mapper);
        }

        final List<Iterator<Integer>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return new ParallelIteratorIntStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link LongStream} consisting of the results of applying the given mapper
     * to the elements of this stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor and mapped concurrently across multiple threads. The output order is not
     * guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code LongStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public LongStream mapToLong(final ToLongFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToLong(mapper);
        }

        final List<Iterator<Long>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return new ParallelIteratorLongStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link FloatStream} consisting of the results of applying the given mapper
     * to the elements of this stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor and mapped concurrently across multiple threads. The output order is not
     * guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code FloatStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream mapToFloat(final ToFloatFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToFloat(mapper);
        }

        final List<Iterator<Float>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return new ParallelIteratorFloatStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@link DoubleStream} consisting of the results of applying the given mapper
     * to the elements of this stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor and mapped concurrently across multiple threads. The output order is not
     * guaranteed to match the input order.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code DoubleStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToDouble(mapper);
        }

        final List<Iterator<Double>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return new ParallelIteratorDoubleStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream produced by replacing each element of this stream with the contents
     * of a mapped sub-stream. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor, and each thread independently expands its elements. If the mapper returns
     * {@code null} for an element, it is treated as an empty stream. Each mapped sub-stream is closed
     * when it is exhausted or when the returned stream is closed.
     *
     * @param <R> the type of the elements in the returned stream
     * @param mapper a non-interfering, stateless function that maps each element to a sub-stream;
     *        may return {@code null}
     * @return a new parallel stream of the concatenated sub-stream contents
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatMap(mapper), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<R>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private Iterator<? extends R> cur = null;
                private Stream<? extends R> s = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        closeMappedStream();

                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                next = (T) NONE;
                                cur = null;
                                break;
                            }
                        }

                        s = mapper.apply(next);

                        if (s == null) {
                            cur = null;
                        } else {
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
                    closeMappedStream();
                }

                private void closeMappedStream() {
                    if (s != null) {
                        final Stream<? extends R> tmp = s;
                        s = null;
                        cur = null;
                        tmp.close();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorStream<>(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel stream produced by replacing each element of this stream with the contents
     * of a mapped {@link Collection}. Elements are consumed from the underlying iterator via a shared
     * synchronized cursor, and each thread independently expands its elements. If the mapper returns
     * {@code null} or an empty collection for an element, that element contributes nothing to the
     * output.
     *
     * @param <R> the type of the elements in the returned stream
     * @param mapper a non-interfering, stateless function that maps each element to a collection;
     *        may return {@code null} or an empty collection
     * @return a new parallel stream of the concatenated collection contents
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> Stream<R> flatmap(final Function<? super T, ? extends Collection<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatmap(mapper), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<R>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private Iterator<? extends R> cur = null;
                private Collection<? extends R> c = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Returns a parallel stream produced by replacing each element of this stream with the contents
     * of a mapped array. This is a convenience wrapper around {@link #flatmap(Function)} that
     * converts the result of the mapper to a {@link java.util.List} before expanding. Elements are
     * processed concurrently via a shared synchronized cursor.
     *
     * @param <R> the type of elements in the mapped arrays and the returned stream
     * @param mapper a non-interfering, stateless function that maps each element to an array
     * @return a new parallel stream of the concatenated array contents
     * @throws IllegalStateException if the stream is already closed
     */
    @SuppressFBWarnings
    @Override
    public <R> Stream<R> flatMapArray(final Function<? super T, R[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatmap(t -> Array.asList(mapper.apply(t)));
    }

    /**
     * Returns a parallel {@link CharStream} produced by replacing each element of this stream with
     * the contents of a mapped {@code CharStream}. Elements are consumed from the underlying
     * iterator via a shared synchronized cursor, and each thread independently expands its elements.
     * If the mapper returns {@code null} for an element, it is treated as an empty stream. Each non-null
     * mapped stream is closed after its contents are consumed or when the resulting stream is closed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a
     *        {@code CharStream}; may return {@code null}
     * @return a new parallel {@code CharStream} of the concatenated sub-stream contents
     * @throws IllegalStateException if the stream is already closed
     */
    @SuppressFBWarnings
    @Override
    public CharStream flatMapToChar(final Function<? super T, ? extends CharStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorCharStream(sequential().flatMapToChar(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<Character>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private CharIterator cur = null;
                private CharStream s = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        closeMappedStream();

                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                next = (T) NONE;
                                cur = null;
                                break;
                            }
                        }

                        s = mapper.apply(next);

                        if (s == null) {
                            cur = null;
                        } else {
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
                    closeMappedStream();
                }

                private void closeMappedStream() {
                    if (s != null) {
                        final Runnable closeAction = s::close;
                        s = null;
                        cur = null;
                        closeAction.run();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorCharStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link ByteStream} produced by replacing each element of this stream with
     * the contents of a mapped {@code ByteStream}. Elements are consumed from the underlying
     * iterator via a shared synchronized cursor, and each thread independently expands its elements.
     * If the mapper returns {@code null} for an element, it is treated as an empty stream. Each non-null
     * mapped stream is closed after its contents are consumed or when the resulting stream is closed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a
     *        {@code ByteStream}; may return {@code null}
     * @return a new parallel {@code ByteStream} of the concatenated sub-stream contents
     * @throws IllegalStateException if the stream is already closed
     */
    @SuppressFBWarnings
    @Override
    public ByteStream flatMapToByte(final Function<? super T, ? extends ByteStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatMapToByte(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<Byte>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private ByteIterator cur = null;
                private ByteStream s = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        closeMappedStream();

                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                next = (T) NONE;
                                cur = null;
                                break;
                            }
                        }

                        s = mapper.apply(next);

                        if (s == null) {
                            cur = null;
                        } else {
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
                    closeMappedStream();
                }

                private void closeMappedStream() {
                    if (s != null) {
                        final Runnable closeAction = s::close;
                        s = null;
                        cur = null;
                        closeAction.run();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorByteStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link ShortStream} produced by replacing each element of this stream with
     * the contents of a mapped {@code ShortStream}. Elements are consumed from the underlying
     * iterator via a shared synchronized cursor, and each thread independently expands its elements.
     * If the mapper returns {@code null} for an element, it is treated as an empty stream. Each non-null
     * mapped stream is closed after its contents are consumed or when the resulting stream is closed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a
     *        {@code ShortStream}; may return {@code null}
     * @return a new parallel {@code ShortStream} of the concatenated sub-stream contents
     * @throws IllegalStateException if the stream is already closed
     */
    @SuppressFBWarnings
    @Override
    public ShortStream flatMapToShort(final Function<? super T, ? extends ShortStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorShortStream(sequential().flatMapToShort(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<Short>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private ShortIterator cur = null;
                private ShortStream s = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        closeMappedStream();

                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                next = (T) NONE;
                                cur = null;
                                break;
                            }
                        }

                        s = mapper.apply(next);

                        if (s == null) {
                            cur = null;
                        } else {
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
                    closeMappedStream();
                }

                private void closeMappedStream() {
                    if (s != null) {
                        final Runnable closeAction = s::close;
                        s = null;
                        cur = null;
                        closeAction.run();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorShortStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link IntStream} produced by replacing each element of this stream with
     * the contents of a mapped {@code IntStream}. Elements are consumed from the underlying iterator
     * via a shared synchronized cursor, and each thread independently expands its elements. If the
     * mapper returns {@code null} for an element, it is treated as an empty stream. Each non-null
     * mapped stream is closed after its contents are consumed or when the resulting stream is closed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to an
     *        {@code IntStream}; may return {@code null}
     * @return a new parallel {@code IntStream} of the concatenated sub-stream contents
     * @throws IllegalStateException if the stream is already closed
     */
    @SuppressFBWarnings
    @Override
    public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<Integer>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private IntIterator cur = null;
                private IntStream s = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        closeMappedStream();

                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                next = (T) NONE;
                                cur = null;
                                break;
                            }
                        }

                        s = mapper.apply(next);

                        if (s == null) {
                            cur = null;
                        } else {
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
                    closeMappedStream();
                }

                private void closeMappedStream() {
                    if (s != null) {
                        final Runnable closeAction = s::close;
                        s = null;
                        cur = null;
                        closeAction.run();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorIntStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link LongStream} produced by replacing each element of this stream with
     * the contents of a mapped {@code LongStream}. Elements are consumed from the underlying
     * iterator via a shared synchronized cursor, and each thread independently expands its elements.
     * If the mapper returns {@code null} for an element, it is treated as an empty stream. Each non-null
     * mapped stream is closed after its contents are consumed or when the resulting stream is closed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a
     *        {@code LongStream}; may return {@code null}
     * @return a new parallel {@code LongStream} of the concatenated sub-stream contents
     * @throws IllegalStateException if the stream is already closed
     */
    @SuppressFBWarnings
    @Override
    public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMapToLong(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<Long>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private LongIterator cur = null;
                private LongStream s = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        closeMappedStream();

                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                next = (T) NONE;
                                cur = null;
                                break;
                            }
                        }

                        s = mapper.apply(next);

                        if (s == null) {
                            cur = null;
                        } else {
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
                    closeMappedStream();
                }

                private void closeMappedStream() {
                    if (s != null) {
                        final Runnable closeAction = s::close;
                        s = null;
                        cur = null;
                        closeAction.run();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorLongStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link FloatStream} produced by replacing each element of this stream with
     * the contents of a mapped {@code FloatStream}. Elements are consumed from the underlying
     * iterator via a shared synchronized cursor, and each thread independently expands its elements.
     * If the mapper returns {@code null} for an element, it is treated as an empty stream. Each non-null
     * mapped stream is closed after its contents are consumed or when the resulting stream is closed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a
     *        {@code FloatStream}; may return {@code null}
     * @return a new parallel {@code FloatStream} of the concatenated sub-stream contents
     * @throws IllegalStateException if the stream is already closed
     */
    @SuppressFBWarnings
    @Override
    public FloatStream flatMapToFloat(final Function<? super T, ? extends FloatStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatMapToFloat(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<Float>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private FloatIterator cur = null;
                private FloatStream s = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        closeMappedStream();

                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                next = (T) NONE;
                                cur = null;
                                break;
                            }
                        }

                        s = mapper.apply(next);

                        if (s == null) {
                            cur = null;
                        } else {
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
                    closeMappedStream();
                }

                private void closeMappedStream() {
                    if (s != null) {
                        final Runnable closeAction = s::close;
                        s = null;
                        cur = null;
                        closeAction.run();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorFloatStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@link DoubleStream} produced by replacing each element of this stream with
     * the contents of a mapped {@code DoubleStream}. Elements are consumed from the underlying
     * iterator via a shared synchronized cursor, and each thread independently expands its elements.
     * If the mapper returns {@code null} for an element, it is treated as an empty stream. Each non-null
     * mapped stream is closed after its contents are consumed or when the resulting stream is closed.
     *
     * @param mapper a non-interfering, stateless function that maps each element to a
     *        {@code DoubleStream}; may return {@code null}
     * @return a new parallel {@code DoubleStream} of the concatenated sub-stream contents
     * @throws IllegalStateException if the stream is already closed
     */
    @SuppressFBWarnings
    @Override
    public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatMapToDouble(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final List<ObjIteratorEx<Double>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private T next = null;
                private DoubleIterator cur = null;
                private DoubleStream s = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && next != NONE) {
                        closeMappedStream();

                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                next = (T) NONE;
                                cur = null;
                                break;
                            }
                        }

                        s = mapper.apply(next);

                        if (s == null) {
                            cur = null;
                        } else {
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
                    closeMappedStream();
                }

                private void closeMappedStream() {
                    if (s != null) {
                        final Runnable closeAction = s::close;
                        s = null;
                        cur = null;
                        closeAction.run();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = mergeCloseHandler(iters);

        return new ParallelIteratorDoubleStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel stream that applies the given action to each element as it is consumed.
     * This is a lazy intermediate operation; the action is invoked concurrently by multiple threads
     * as elements are pulled through the pipeline. No ordering guarantee is provided regarding when
     * or by which thread the action is applied.
     *
     * @param action a non-interfering action to perform on each element
     * @return a new parallel stream with the side-effect action wired in
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> onEach(final Consumer<? super T> action) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.onEach(action);
        }

        final List<Iterator<T>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private Object next = NONE;

                @Override
                public boolean hasNext() {
                    if (next == NONE) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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

        return newStream(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null);
    }

    /**
     * Performs the given action for each element of this stream in parallel, then invokes
     * {@code onComplete} exactly once after all threads have finished processing successfully. If
     * an action invocation fails, {@code onComplete} is not invoked. Elements are
     * consumed from the underlying iterator via a shared synchronized cursor; the action is called
     * concurrently across multiple threads with no ordering guarantee. {@code onComplete} is called
     * on the calling thread after all worker threads complete.
     *
     * @param <E> the type of exception that {@code action} may throw
     * @param <E2> the type of exception that {@code onComplete} may throw
     * @param action a non-interfering action to perform on each element
     * @param onComplete a runnable invoked once after all elements have been processed successfully
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     * @throws E2 if the {@code onComplete} runnable throws an exception
     */
    @Override
    public <E extends Exception, E2 extends Exception> void forEach(final Throwables.Consumer<? super T, E> action, final Throwables.Runnable<E2> onComplete)
            throws E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            super.forEach(action, onComplete);
            return;
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        action.accept(next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
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
     * For each element of this stream, applies {@code flatMapper} to produce an {@link Iterable} of
     * secondary values, then invokes {@code action} for each (element, secondary) pair. Processing is
     * done concurrently across multiple threads via a shared synchronized cursor. If {@code flatMapper}
     * returns {@code null} for an element, that element contributes no pairs.
     *
     * @param <U> the type of secondary values produced by the flat mapper
     * @param <E> the type of exception that {@code flatMapper} may throw
     * @param <E2> the type of exception that {@code action} may throw
     * @param flatMapper a non-interfering function mapping each element to an iterable of secondary
     *        values; may return {@code null}
     * @param action a non-interfering bi-consumer invoked for each (element, secondary) pair
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the flat mapper throws an exception
     * @throws E2 if the action throws an exception
     */
    @Override
    public <U, E extends Exception, E2 extends Exception> void forEach(final Throwables.Function<? super T, ? extends Iterable<? extends U>, E> flatMapper,
            final Throwables.BiConsumer<? super T, ? super U, E2> action) throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            super.forEach(flatMapper, action);
            return;
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                Iterable<? extends U> c = null;
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a two-level flat-then-join iteration in parallel. For each element {@code t}, applies
     * {@code flatMapper} to get an iterable of {@code T2} values; for each {@code T2} value, applies
     * {@code flatMapper2} to get an iterable of {@code T3} values; then invokes {@code action} for
     * each {@code (t, t2, t3)} triple. Processing is concurrent via a shared synchronized cursor.
     * If either flat mapper returns {@code null} for an element, that element contributes no triples
     * at that level.
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
     * @throws IllegalStateException if the stream is already closed
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

        if (canBeSequential(maxThreadNum)) {
            super.forEach(flatMapper, flatMapper2, action);
            return;
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                Iterable<T2> c2 = null;
                Iterable<T3> c3 = null;
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs the given action for each overlapping or sliding pair of consecutive elements, in
     * parallel. The window size is 2 and the window advances by {@code increment} positions between
     * invocations. Elements are consumed from the underlying iterator via a shared synchronized
     * cursor. When the second element of a pair is absent, {@code null} is passed for that position.
     *
     * @param <E> the type of exception that {@code action} may throw
     * @param increment the number of positions to advance the window between consecutive pairs; must
     *        be positive
     * @param action a non-interfering bi-consumer invoked for each pair
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code increment} is not positive
     * @throws E if the action throws an exception
     */
    @Override
    public <E extends Exception> void forEachPair(final int increment, final Throwables.BiConsumer<? super T, ? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            super.forEachPair(increment, action);
            return;
        }

        final int windowSize = 2;
        checkArgPositive(increment, cs.increment);

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<T> prev = new Holder<>();
        final MutableBoolean isFirst = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, new Runnable() {
                private T first = null;
                private T second = null;

                @Override
                public void run() {
                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (elements.hasNext()) {
                                    if (increment > windowSize && isFirst.isFalse()) {
                                        int skipNum = increment - windowSize;

                                        while (skipNum-- > 0 && elements.hasNext()) {
                                            elements.next();
                                        }

                                        if (!elements.hasNext()) {
                                            break;
                                        }
                                    }

                                    if (increment == 1) {
                                        first = isFirst.isTrue() ? elements.next() : prev.value();
                                        second = elements.hasNext() ? elements.next() : null;

                                        prev.setValue(second);
                                    } else {
                                        first = elements.next();
                                        second = elements.hasNext() ? elements.next() : null;
                                    }

                                    isFirst.setFalse();
                                } else {
                                    break;
                                }
                            }

                            action.accept(first, second);
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
     * invocations. Elements are consumed from the underlying iterator via a shared synchronized
     * cursor. When the second or third element of a triple is absent, {@code null} is passed for
     * the missing positions.
     *
     * @param <E> the type of exception that {@code action} may throw
     * @param increment the number of positions to advance the window between consecutive triples;
     *        must be positive
     * @param action a non-interfering tri-consumer invoked for each triple
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code increment} is not positive
     * @throws E if the action throws an exception
     */
    @Override
    public <E extends Exception> void forEachTriple(final int increment, final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            super.forEachTriple(increment, action);
            return;
        }

        final int windowSize = 3;
        checkArgPositive(increment, cs.increment);

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<T> prev = new Holder<>();
        final Holder<T> prev2 = new Holder<>();
        final MutableBoolean isFirst = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, new Runnable() {
                private T first = null;
                private T second = null;
                private T third = null;

                @Override
                public void run() {
                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (elements.hasNext()) {
                                    if (increment > windowSize && isFirst.isFalse()) {
                                        int skipNum = increment - windowSize;

                                        while (skipNum-- > 0 && elements.hasNext()) {
                                            elements.next();
                                        }

                                        if (!elements.hasNext()) {
                                            break;
                                        }
                                    }

                                    if (increment == 1) {
                                        first = isFirst.isTrue() ? elements.next() : prev2.value();
                                        second = isFirst.isTrue() ? (elements.hasNext() ? elements.next() : null) : prev.value();
                                        third = elements.hasNext() ? elements.next() : null;

                                        prev2.setValue(second);
                                        prev.setValue(third);
                                    } else if (increment == 2) {
                                        first = isFirst.isTrue() ? elements.next() : prev.value();
                                        second = elements.hasNext() ? elements.next() : null;
                                        third = elements.hasNext() ? elements.next() : null;

                                        prev.setValue(third);
                                    } else {
                                        first = elements.next();
                                        second = elements.hasNext() ? elements.next() : null;
                                        third = elements.hasNext() ? elements.next() : null;
                                    }

                                    isFirst.setFalse();
                                } else {
                                    break;
                                }
                            }

                            action.accept(first, second, third);
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
     * key/value pair using the provided functions. Elements are consumed from the underlying iterator
     * via a shared synchronized cursor; each thread builds its own partial map, which are then merged
     * using {@code mergeFunction} to resolve key collisions.
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
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        // return collect(Collectors.toMap(keyMapper, valueMapper, mapFactory));

        //    final M res = mapFactory.get();
        //    res.putAll(collect(Collectors.toConcurrentMap(keyMapper, valueMapper, mergeFunction)));
        //    return res;

        final List<ContinuableFuture<M>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final M map = mapFactory.get();
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        Collectors.merge(map, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return map;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialMaps -> {
            M res = null;

            for (final M partialMap : partialMaps) {
                if (res == null) {
                    res = partialMap;
                } else {
                    for (final Map.Entry<K, V> entry : partialMap.entrySet()) {
                        Collectors.merge(res, entry.getKey(), entry.getValue(), mergeFunction);
                    }
                }
            }

            return res;
        }, this, asyncExecutor, asyncExecutorToUse, (E) null);
    }

    /**
     * Groups the elements of this stream in parallel using the given key and value mappers and a
     * downstream {@link Collector}. Each thread builds its own partial grouping map; the partial maps
     * are then merged by combining per-key downstream containers using the downstream collector's
     * combiner. The downstream finisher is applied to each grouped container at the end.
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
     * @param downstream a collector describing how the per-key values are accumulated and finished
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a map from keys to finished downstream results
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, D, M extends Map<K, D>, E extends Exception, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws IllegalArgumentException, IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
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

        final List<ContinuableFuture<Map<K, Object>>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                @SuppressWarnings("rawtypes")
                final Map<K, Object> map = (Map) mapFactory.get();
                K key = null;
                Object valueContainer = null;
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return map;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialMaps -> {
            Map<K, Object> intermediate = null;

            for (final Map<K, Object> partialMap : partialMaps) {
                if (intermediate == null) {
                    intermediate = partialMap;
                } else {
                    K key = null;

                    for (final Map.Entry<K, Object> entry : partialMap.entrySet()) {
                        key = entry.getKey();

                        if (intermediate.containsKey(key)) {
                            intermediate.put(key, downstreamCombiner.apply(intermediate.get(key), entry.getValue()));
                        } else {
                            intermediate.put(key, entry.getValue());
                        }
                    }
                }
            }

            final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

            //noinspection DataFlowIssue
            Collectors.replaceAll(intermediate, function);

            return (M) intermediate;
        }, this, asyncExecutor, asyncExecutorToUse, (E) null);
    }

    /**
     * Groups the elements of this stream in parallel using a flat key extractor and a downstream
     * {@link Collector}. For each element, {@code flatKeyExtractor} may return multiple keys; the
     * element is accumulated into the group for each key. Each thread builds its own partial grouping
     * map, and the partial maps are merged by combining per-key downstream containers. The downstream
     * finisher is applied at the end.
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
     * @param downstream a collector describing how the per-key values are accumulated and finished
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a map from keys to finished downstream results
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the flat key extractor throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, D, M extends Map<K, D>, E extends Exception, E2 extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.flatGroupTo(flatKeyExtractor, valueMapper, downstream, mapFactory);
        }

        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final BiConsumer<Object, ? super V> downstreamAccumulator = (BiConsumer<Object, ? super V>) downstream.accumulator();
        final BinaryOperator<Object> downstreamCombiner = (BinaryOperator<Object>) downstream.combiner();
        final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

        final List<ContinuableFuture<Map<K, Object>>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                @SuppressWarnings("rawtypes")
                final Map<K, Object> map = (Map) mapFactory.get();
                Iterator<? extends K> keyIter = null;
                K key = null;
                Object valueContainer = null;
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return map;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialMaps -> {
            Map<K, Object> intermediate = null;

            for (final Map<K, Object> partialMap : partialMaps) {
                if (intermediate == null) {
                    intermediate = partialMap;
                } else {
                    K key = null;

                    for (final Map.Entry<K, Object> entry : partialMap.entrySet()) {
                        key = entry.getKey();

                        if (intermediate.containsKey(key)) {
                            intermediate.put(key, downstreamCombiner.apply(intermediate.get(key), entry.getValue()));
                        } else {
                            intermediate.put(key, entry.getValue());
                        }
                    }
                }
            }

            final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

            //noinspection DataFlowIssue
            Collectors.replaceAll(intermediate, function);

            return (M) intermediate;
        }, this, asyncExecutor, asyncExecutorToUse, (E) null);
    }

    /**
     * Accumulates the elements of this stream into a {@link Multimap} in parallel. Each element is
     * mapped to a key/value pair using the provided functions. Elements are consumed from the
     * underlying iterator via a shared synchronized cursor; each thread builds its own partial
     * multimap, which are then merged by putting all values from each partial result into the final
     * map.
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
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>, E extends Exception, E2 extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E> keyMapper, final Throwables.Function<? super T, ? extends V, E2> valueMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.toMultimap(keyMapper, valueMapper, mapFactory);
        }

        // return collect(Collectors.toMultimap(keyMapper, valueMapper, mapFactory));

        final List<ContinuableFuture<M>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final M map = mapFactory.get();
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        map.put(keyMapper.apply(next), valueMapper.apply(next));
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return map;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialMaps -> {
            M res = null;

            for (final M partialMap : partialMaps) {
                if (res == null) {
                    res = partialMap;
                } else {
                    res.putValues(partialMap);
                }
            }

            return res;
        }, this, asyncExecutor, asyncExecutorToUse, (E) null);
    }

    /**
     * Performs a parallel reduction on the elements of this stream using the given associative
     * accumulator. Each thread independently reduces its share of elements into a partial result;
     * the partial results are then combined using the same accumulator. Because the accumulator
     * serves as both the per-thread reduction function and the cross-thread combiner, it must be
     * associative.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two
     *        values; used both within a thread and to merge partial results across threads
     * @return an {@link Optional} describing the result, or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> reduce(final BinaryOperator<T> accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<T>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                T result = (T) NONE;
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        result = result == NONE ? next : accumulator.apply(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            T result = (T) NONE;

            for (final T partialResult : partialResults) {
                if (partialResult != NONE) {
                    result = result == NONE ? partialResult : accumulator.apply(result, partialResult);
                }
            }

            return result == NONE ? Optional.empty() : Optional.of(result);
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a parallel reduction on the elements of this stream using an identity value,
     * an accumulator function, and a combiner. Each thread starts with {@code identity} and
     * accumulates its share of elements; the per-thread results are then combined pairwise using
     * {@code combiner} to produce the final result.
     *
     * @param <U> the type of the result
     * @param identity the identity value for the combiner; also used as the initial value for each
     *        per-thread accumulation
     * @param accumulator a non-interfering, stateless function to fold elements into a partial result
     * @param combiner a non-interfering, stateless, associative function to combine two partial
     *        results produced by different threads
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <U> U reduce(final U identity, final BiFunction<? super U, ? super T, U> accumulator, final BinaryOperator<U> combiner)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator, combiner);
        }

        final List<ContinuableFuture<U>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                U result = identity;
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.apply(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            U result = (U) NONE;

            for (final U partialResult : partialResults) {
                result = result == NONE ? partialResult : combiner.apply(result, partialResult);
            }

            return result == NONE ? identity : result;
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a mutable parallel reduction on the elements of this stream. Each thread creates its
     * own result container using {@code supplier}, accumulates its share of elements using
     * {@code accumulator}, and then all per-thread containers are merged into a single container
     * using {@code combiner}.
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container for each thread
     * @param accumulator a non-interfering, stateless function to fold an element into a container
     * @param combiner a function that merges two containers; the second container's contents are
     *        folded into the first
     * @return the final merged result container
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super T> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.collect(supplier, accumulator, combiner);
        }

        final List<ContinuableFuture<R>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final R container = supplier.get();
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        accumulator.accept(container, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return container;
            });
        }

        return completeAndCollectResult(futureList, eHolder, supplier, combiner, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a mutable parallel reduction on the elements of this stream using a {@link Collector}.
     * If the collector has both the {@link java.util.stream.Collector.Characteristics#CONCURRENT CONCURRENT}
     * and {@link java.util.stream.Collector.Characteristics#UNORDERED UNORDERED} characteristics,
     * a single shared container is created and all threads accumulate into it
     * directly. Otherwise, each thread accumulates into its own container and the per-thread
     * containers are combined using the collector's combiner.
     *
     * @param <R> the type of the result
     * @param collector the {@code Collector} describing the reduction
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     * @see java.util.stream.Collectors
     */
    @Override
    public <R> R collect(final Collector<? super T, ?, R> collector) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return sequential().collect(collector);
        }

        //    if (/*collector.characteristics().contains(Collector.Characteristics.CONCURRENT) == false
        //               || */ collector.characteristics().contains(Collector.Characteristics.UNORDERED) == false) {
        //        return sequential().collect(collector);
        //    }

        final boolean isConcurrentCollector = N.notEmpty(collector.characteristics())
                && collector.characteristics().contains(Collector.Characteristics.CONCURRENT)
                && collector.characteristics().contains(Collector.Characteristics.UNORDERED);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final BinaryOperator<Object> combiner = (BinaryOperator<Object>) collector.combiner();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        final List<ContinuableFuture<Object>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Object singleContainer = isConcurrentCollector ? supplier.get() : null;
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Object container = isConcurrentCollector ? singleContainer : supplier.get();
                T next = null;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        accumulator.accept(container, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return container;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Object container = isConcurrentCollector ? singleContainer : NONE;

            if (!isConcurrentCollector) {
                for (final Object partialResult : partialResults) {
                    container = container == NONE ? partialResult : combiner.apply(container, partialResult);
                }
            }

            return finisher.apply(container == NONE ? supplier.get() : container);
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Returns the minimum element of this stream according to the given comparator. If the stream
     * is already sorted with a compatible comparator, the first element is returned immediately
     * without a full parallel scan. Otherwise, the minimum is found via a parallel reduction using
     * a {@link Collectors#min(Comparator)} collector. A {@code null} comparator is treated as a
     * null-max comparator (nulls sort last).
     *
     * @param comparator a comparator to compare elements; may be {@code null}
     * @return an {@link Optional} describing the minimum element, or an empty Optional if the stream
     *         is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> min(Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        boolean isDone = true;

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator, comparator())) {
                return Optional.of(elements.next());
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
     * Returns the maximum element of this stream according to the given comparator. If the stream
     * is already sorted with a compatible comparator, the iterator is drained sequentially to
     * return the last element without a full parallel scan. Otherwise, the maximum is found via a
     * parallel reduction using a {@link Collectors#max(Comparator)} collector. A {@code null}
     * comparator is treated as a null-min comparator (nulls sort first).
     *
     * @param comparator a comparator to compare elements; may be {@code null}
     * @return an {@link Optional} describing the maximum element, or an empty Optional if the stream
     *         is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> max(Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        boolean isDone = true;

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator, comparator())) {
                T next = null;

                while (elements.hasNext()) {
                    next = elements.next();
                }

                return Optional.of(next);
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
     * Returns {@code true} if any element of this stream matches the given predicate. Elements are
     * tested concurrently across multiple threads via a shared synchronized cursor. Once any thread
     * finds a matching element, all threads stop via short-circuit evaluation.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if at least one element matches the predicate; {@code false} if no
     *         element matches or the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.anyMatch(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(false);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                T next = null;

                try {
                    while (result.isFalse() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        if (predicate.test(next)) {
                            synchronized (result) {
                                result.setTrue();
                            }
                            break;
                        }
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns {@code true} if all elements of this stream match the given predicate, or if the
     * stream is empty. Elements are tested concurrently across multiple threads via a shared
     * synchronized cursor. Once any thread encounters an element that does not match, all threads
     * stop via short-circuit evaluation.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if every element matches the predicate or the stream is empty;
     *         {@code false} if any element fails the predicate
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.allMatch(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                T next = null;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        if (!predicate.test(next)) {
                            synchronized (result) {
                                result.setFalse();
                            }
                            break;
                        }
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns {@code true} if no elements of this stream match the given predicate, or if the
     * stream is empty. Elements are tested concurrently across multiple threads via a shared
     * synchronized cursor. Once any thread encounters a matching element, all threads stop via
     * short-circuit evaluation.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if no element matches the predicate or the stream is empty;
     *         {@code false} if any element matches the predicate
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.noneMatch(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                T next = null;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        if (predicate.test(next)) {
                            synchronized (result) {
                                result.setFalse();
                            }
                            break;
                        }
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns {@code true} if the number of elements matching the predicate falls within the
     * inclusive range {@code [atLeast, atMost]}. Elements are tested concurrently across multiple
     * threads via a shared synchronized cursor. Once the match count exceeds {@code atMost}, all
     * threads stop via short-circuit evaluation.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param atLeast the minimum number of matches required (inclusive, non-negative)
     * @param atMost the maximum number of matches allowed (inclusive, non-negative, &gt;= atLeast)
     * @param predicate a non-interfering, stateless predicate to test each element
     * @return {@code true} if the match count is &gt;= {@code atLeast} and &lt;= {@code atMost}
     * @throws IllegalStateException if the stream is already closed
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
        checkArgument(atLeast <= atMost, "'atLeast' (%s) must be <= 'atMost' (%s)", atLeast, atMost);

        if (canBeSequential(maxThreadNum)) {
            return super.hasMatchCountBetween(atLeast, atMost, predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final AtomicLong cnt = new AtomicLong(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                T next = null;

                try {
                    while (cnt.get() <= atMost && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
                            } else {
                                break;
                            }
                        }

                        if (predicate.test(next) && (cnt.incrementAndGet() > atMost)) {
                            break;
                        }
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return cnt.get() >= atLeast && cnt.get() <= atMost;
    }

    /**
     * Returns the element encountered first (by iterator position) among those that match the
     * predicate, found via parallel search. Each element is tagged with a monotonically increasing
     * index as it is consumed from the shared synchronized cursor. When a thread finds a match, it
     * records the (index, element) pair; once a match is recorded, the other threads stop pulling
     * new elements, but elements already in flight are still tested and replace the recorded
     * result if they carry a smaller index. The result is the matching element with the smallest
     * index seen across all threads.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to test each element
     * @return an {@link Optional} describing the first (lowest-index) matching element, or an empty
     *         Optional if no element matches
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> Optional<T> findFirst(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, T>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, T> pair = new Pair<>();

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.next());
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
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? Optional.empty() : Optional.of(resultHolder.value().right());
    }

    /**
     * Returns any element that matches the predicate, found via parallel search with no ordering
     * guarantee. Elements are tested concurrently across multiple threads via a shared synchronized
     * cursor. The first thread to find a matching element records it and all threads stop via
     * short-circuit evaluation. The result may be any matching element — not necessarily the one
     * with the smallest position in the stream.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to test each element
     * @return an {@link Optional} describing some matching element, or an empty Optional if no
     *         element matches
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> Optional<T> findAny(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<T> resultHolder = Holder.of((T) NONE);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                T next = null;

                try {
                    while (resultHolder.value() == NONE && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.next();
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
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == NONE ? Optional.empty() : Optional.of(resultHolder.value());
    }

    /**
     * Returns the element encountered last (by iterator position) among those that match the
     * predicate, found via parallel scan. Each element is tagged with a monotonically increasing
     * index as it is consumed from the shared synchronized cursor; all elements are examined because
     * short-circuit evaluation is not possible without knowing what comes later. The result is the
     * matching element with the largest index seen across all threads.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to test each element
     * @return an {@link Optional} describing the last (highest-index) matching element, or an empty
     *         Optional if no element matches
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> Optional<T> findLast(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, T>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, T> pair = new Pair<>();

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.next());
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
                        }
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? Optional.empty() : Optional.of(resultHolder.value().right());
    }

    /**
     * Returns a parallel stream consisting of the elements of this stream whose mapped key is
     * contained in collection {@code c}, considering multiplicity. Each key occurrence is matched
     * and consumed from the collection at most once. The mapper is applied in parallel; access to
     * the shared {@link Multiset} tracking remaining candidates is synchronized.
     *
     * @param <U> the type of the key used for matching
     * @param mapper a function to extract the key from each element
     * @param c the collection of keys; duplicates are treated as separate candidates
     * @return a new parallel stream of elements whose keys are in {@code c}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <U> Stream<T> intersection(final Function<? super T, ? extends U> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.intersection(mapper, c);
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
     * remain in the output. The mapper is applied in parallel; access to the shared {@link Multiset}
     * is synchronized.
     *
     * @param <U> the type of the key used for matching
     * @param mapper a function to extract the key from each element
     * @param c the collection of keys to exclude; duplicates are treated as separate exclusions
     * @return a new parallel stream of elements whose keys are not in {@code c}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <U> Stream<T> difference(final Function<? super T, ? extends U> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.difference(mapper, c);
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
     * Returns a new parallel stream that consists of the elements of this stream followed by the
     * elements of the given stream. The resulting stream preserves the parallel configuration
     * ({@code maxThreadNum}, {@code splitor}, {@code asyncExecutor}, {@code cancelUncompletedThreads})
     * of this stream.
     *
     * @param stream the stream to append after this stream
     * @return a new parallel stream containing the elements of both streams in order
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> append(final Stream<T> stream) throws IllegalStateException {
        assertNotClosed();

        return new ParallelIteratorStream<>(Stream.concat(this, stream), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a new parallel stream that consists of the elements of the given stream followed by
     * the elements of this stream. The resulting stream preserves the parallel configuration
     * ({@code maxThreadNum}, {@code splitor}, {@code asyncExecutor}, {@code cancelUncompletedThreads})
     * of this stream.
     *
     * @param stream the stream to prepend before this stream
     * @return a new parallel stream containing the elements of both streams in order
     * @throws IllegalStateException if the stream is already closed
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
     * @throws IllegalStateException if the stream is already closed
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
     * @throws IllegalStateException if the stream is already closed
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
     * when the shorter of the two sequences is exhausted. Pairing is done in parallel using
     * {@link Stream#parallelZip}.
     *
     * @param <T2> the element type of collection {@code b}
     * @param <R> the type of the zipped results
     * @param b the collection to zip with this stream
     * @param zipFunction a function to combine corresponding elements
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream is already closed
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
     * @throws IllegalStateException if the stream is already closed
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
     * @throws IllegalStateException if the stream is already closed
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
     * @throws IllegalStateException if the stream is already closed
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
     * the shorter of the two streams is exhausted. Pairing is done in parallel using
     * {@link Stream#parallelZip}.
     *
     * @param <T2> the element type of stream {@code b}
     * @param <R> the type of the zipped results
     * @param b the stream to zip with this stream
     * @param zipFunction a function to combine corresponding elements
     * @return a new parallel stream of zipped results
     * @throws IllegalStateException if the stream is already closed
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
     * @throws IllegalStateException if the stream is already closed
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
     * @throws IllegalStateException if the stream is already closed
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
     * @throws IllegalStateException if the stream is already closed
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
     * Always returns {@code true}, as this is a parallel stream implementation.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParallel() {
        return true;
    }

    /**
     * Returns a sequential {@link IteratorStream} view backed by the same underlying iterator and
     * close handlers as this stream. The sequential stream is lazily created and cached; subsequent
     * calls return the same instance.
     *
     * @return a sequential stream over the same elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> sequential() throws IllegalStateException {
        assertNotClosed();

        IteratorStream<T> tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;
                if (tmp == null) {
                    tmp = new IteratorStream<>(elements, isSorted(), comparator(), closeHandlers());
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
        //  assertNotClosed();

        return asyncExecutor;
    }

    /**
     * Returns whether unfinished parallel tasks should be cancelled when the stream is closed.
     *
     * @return {@code true} if unfinished tasks should be cancelled
     */
    @Override
    protected boolean cancelUncompletedThreads() {
        return cancelUncompletedThreads;
    }
}
