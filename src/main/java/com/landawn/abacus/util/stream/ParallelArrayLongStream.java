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
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.function.LongTernaryOperator;
import com.landawn.abacus.util.function.LongToFloatFunction;
import com.landawn.abacus.util.u.OptionalLong;

/**
 * A parallel implementation of LongStream backed by an array that enables concurrent processing
 * of long elements across multiple threads. This class extends ArrayLongStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution model distributes work across multiple threads according to the configured
 * {@link BaseStream.SplitStrategy}:
 * <ul>
 * <li>{@link BaseStream.SplitStrategy#ARRAY} - divides the array into fixed-size slices, one per thread</li>
 * <li>{@link BaseStream.SplitStrategy#ITERATOR} - uses a shared cursor with synchronized access for load balancing</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel long stream
 * long[] largeLongArray = { 1L, -2L, 3L };
 * LongStream stream = LongStream.of(largeLongArray).parallel();
 *
 * // Process elements in parallel
 * long sum = stream.filter(l -> l > 0).sum();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> A parallel operation coordinates its own worker threads when they
 * access shared state. The stream instance itself is not intended to be driven concurrently by
 * multiple callers or reused for independent operations.
 *
 * <p><b>Encounter Order:</b> Unless an operation explicitly states otherwise, parallel intermediate
 * operations may emit elements in a different order from the source.
 *
 * <p><b>Failure handling:</b> Terminal operations wait for every submitted worker before closing
 * the stream or a temporary executor. The primary failure is rethrown and additional failures are suppressed.
 *
 * @see ArrayLongStream
 * @see LongStream#parallel()
 */
final class ParallelArrayLongStream extends ArrayLongStream {
    private final int maxThreadNum;
    private final SplitStrategy splitStrategy;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile ArrayLongStream sequential;

    /**
     * Constructs a ParallelArrayLongStream with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the long array to stream; it is used directly, not copied, so callers must not mutate it afterwards
     * @param fromIndex the start index (inclusive) of the range to process
     * @param toIndex the end index (exclusive) of the range to process
     * @param sorted whether the array elements in the range are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed, may be {@code null}
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}; a {@code null} array is treated as having length zero
     */
    ParallelArrayLongStream(final long[] values, final int fromIndex, final int toIndex, final boolean sorted, final int maxThreadNum,
            final SplitStrategy splitStrategy, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads,
            final Collection<LocalRunnable> closeHandlers) throws IndexOutOfBoundsException {
        super(values, fromIndex, toIndex, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitStrategy = splitStrategy == null ? DEFAULT_SPLIT_STRATEGY : splitStrategy;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Returns a parallel stream consisting of elements that match the given predicate. If the
     * stream can be processed sequentially (e.g., few elements), delegates to the sequential
     * implementation; otherwise evaluates the predicate on worker threads, which pass the
     * retained elements to the returned stream through a bounded primitive buffer without
     * boxing.
     *
     * <p>The encounter order of elements in the resulting stream is not guaranteed when
     * executing in parallel.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return a new parallel {@code LongStream} of matching elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Override
    public LongStream filter(final LongPredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.filter(predicate);
        }

        // Each worker needs a source element; excess workers only allocate buffers and submit idle tasks.
        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<LongIterator> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new LongIterator() {
                private long next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (true) {
                            synchronized (source) {
                                if (source.hasNext()) {
                                    next = source.nextLong();
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
                public long nextLong() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorLongStream(ParallelPrimitiveBuffer.concatLong(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel stream of elements that match the predicate until a worker observes a
     * non-matching element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise evaluates the predicate on worker threads, which
     * pass the retained elements to the returned stream through a bounded primitive buffer
     * without boxing.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix
     * semantics; later matching elements may be returned.
     *
     * @param predicate a non-interfering, stateless predicate applied to elements
     * @return a new parallel {@code LongStream} of the elements selected by the parallel {@code takeWhile} operation
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Override
    public LongStream takeWhile(final LongPredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.takeWhile(predicate);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<LongIterator> iters = new ArrayList<>(threadNum);
        final AtomicBoolean hasMore = new AtomicBoolean(true);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new LongIterator() {
                private long next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext && hasMore.get()) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
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
                public long nextLong() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorLongStream(ParallelPrimitiveBuffer.concatLong(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel stream after dropping the leading elements that match the predicate.
     * If the stream can be processed sequentially, delegates to the sequential implementation;
     * otherwise evaluates the predicate on worker threads, which pass the retained elements to
     * the returned stream through a bounded primitive buffer without boxing.
     *
     * <p>The first non-matching element and all remaining elements are retained, but their output
     * order is not guaranteed to match the source encounter order.
     *
     * @param predicate a non-interfering, stateless predicate applied to elements
     * @return a new parallel {@code LongStream} of the elements selected by the parallel {@code dropWhile} operation
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Override
    public LongStream dropWhile(final LongPredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.dropWhile(predicate);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<LongIterator> iters = new ArrayList<>(threadNum);
        final AtomicBoolean dropped = new AtomicBoolean(false);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new LongIterator() {
                private long next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        // Once the boundary element has been found ('dropped' is set), all threads switch to
                        // pass-through mode and consume the remaining elements via the shared cursor.
                        if (dropped.get()) {
                            synchronized (source) {
                                if (source.hasNext()) {
                                    next = source.nextLong();
                                    hasNext = true;
                                }
                            }
                        } else {
                            while (!dropped.get()) {
                                // Take the element AND decide about it under the same lock. Testing the predicate
                                // outside it let a thread that had not yet observed `dropped` consume an element
                                // belonging to the output and silently discard it - dropWhile must drop only the
                                // LEADING prefix. The drop phase is inherently sequential; only it is serialized,
                                // the pass-through phase above stays parallel.
                                synchronized (source) {
                                    if (dropped.get()) {
                                        break;
                                    }

                                    if (!source.hasNext()) {
                                        break;
                                    }

                                    next = source.nextLong();

                                    if (!predicate.test(next)) {
                                        hasNext = true;
                                        dropped.set(true);
                                    }
                                }

                                if (hasNext) {
                                    break;
                                }
                            }

                            if (!hasNext && dropped.get()) {
                                synchronized (source) {
                                    if (source.hasNext()) {
                                        next = source.nextLong();
                                        hasNext = true;
                                    }
                                }
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public long nextLong() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorLongStream(ParallelPrimitiveBuffer.concatLong(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code LongStream} consisting of the results of applying the given
     * function to each element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise applies the mapper on worker threads, which pass
     * the results to the returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code LongStream} of mapped elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public LongStream map(final LongUnaryOperator mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.map(mapper);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<LongIterator> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new LongIterator() {
                private long next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                                hasNext = true;
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public long nextLong() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final long result = mapper.applyAsLong(next);
                    hasNext = false;
                    return result;
                }
            });
        }

        return new ParallelIteratorLongStream(ParallelPrimitiveBuffer.concatLong(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code IntStream} consisting of the results of applying the given function
     * to each element. If the stream can be processed sequentially, delegates to the sequential
     * implementation; otherwise applies the mapper on worker threads, which pass the results to the
     * returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function mapping {@code long} to {@code int}
     * @return a new parallel {@code IntStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public IntStream mapToInt(final LongToIntFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToInt(mapper);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<IntIterator> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new IntIterator() {
                private long next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                                hasNext = true;
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public int nextInt() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final int result = mapper.applyAsInt(next);
                    hasNext = false;
                    return result;
                }
            });
        }

        return new ParallelIteratorIntStream(ParallelPrimitiveBuffer.concatInt(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code FloatStream} consisting of the results of applying the given
     * function to each element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise applies the mapper on worker threads, which pass the
     * results to the returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function mapping {@code long} to {@code float}
     * @return a new parallel {@code FloatStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public FloatStream mapToFloat(final LongToFloatFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToFloat(mapper);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<FloatIterator> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new FloatIterator() {
                private long next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                                hasNext = true;
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public float nextFloat() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final float result = mapper.applyAsFloat(next);
                    hasNext = false;
                    return result;
                }
            });
        }

        return new ParallelIteratorFloatStream(ParallelPrimitiveBuffer.concatFloat(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code DoubleStream} consisting of the results of applying the given
     * function to each element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise applies the mapper on worker threads, which pass the
     * results to the returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function mapping {@code long} to {@code double}
     * @return a new parallel {@code DoubleStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public DoubleStream mapToDouble(final LongToDoubleFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToDouble(mapper);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<DoubleIterator> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new DoubleIterator() {
                private long next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                                hasNext = true;
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public double nextDouble() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final double result = mapper.applyAsDouble(next);
                    hasNext = false;
                    return result;
                }
            });
        }

        return new ParallelIteratorDoubleStream(ParallelPrimitiveBuffer.concatDouble(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel object {@code Stream} consisting of the results of applying the given
     * function to each element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise applies the mapper to the primitive elements on worker
     * threads, which pass the mapped objects to the returned stream through a bounded buffer.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel object {@code Stream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public <T> Stream<T> mapToObj(final LongFunction<? extends T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToObj(mapper);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<Iterator<T>> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private long next;
                private boolean ready;

                @Override
                public boolean hasNext() {
                    if (!ready) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                                ready = true;
                            }
                        }
                    }

                    return ready;
                }

                @Override
                public T next() throws NoSuchElementException {
                    if (!ready && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final T result = mapper.apply(next);
                    ready = false;
                    return result;
                }
            });
        }

        return new ParallelIteratorStream<>(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null,
                maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code LongStream} consisting of the results of replacing each element
     * with the contents of the mapped {@code LongStream}. If the stream can be processed
     * sequentially, delegates to the sequential {@code flatMap} wrapped in a parallel iterator
     * stream; otherwise flat-maps on worker threads, which pass the flattened elements to the
     * returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code LongStream} for each element
     * @return a new parallel {@code LongStream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public LongStream flatMap(final LongFunction<? extends LongStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<LongIteratorEx> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new LongIteratorEx() {
                private long next;
                private boolean exhausted;
                private LongIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final LongStream mapped = mapper.apply(next);

                        if (!mappedStream.adopt(mapped)) {
                            // closeResource() already ran, so nothing else will ever close this one.
                            if (mapped != null) {
                                mapped.close();
                            }

                            cur = null;
                            break;
                        }

                        cur = mapped == null ? null : mapped.iteratorEx();
                    }

                    return cur != null && cur.hasNext();
                }

                @Override
                public long nextLong() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.nextLong();
                }

                @Override
                public void closeResource() {
                    closeMappedStream(true);
                }

                private void closeMappedStream() {
                    closeMappedStream(false);
                }

                private void closeMappedStream(final boolean noMoreAdoptions) {
                    @SuppressWarnings("resource")
                    final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> tmp = mappedStream.detach(noMoreAdoptions);

                    if (!noMoreAdoptions) {
                        cur = null;
                    }

                    if (tmp != null) {
                        tmp.close();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = closeHandlersForNewStream(mergeCloseHandler(iters));

        return new ParallelIteratorLongStream(ParallelPrimitiveBuffer.concatLong(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@code LongStream} consisting of the results of replacing each element
     * with the contents of the mapped collection. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatmap} wrapped in a parallel iterator stream; otherwise
     * flat-maps on worker threads, which unbox each collection element into a bounded primitive
     * buffer that feeds the returned stream.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code Collection<Long>} for each element
     * @return a new parallel {@code LongStream} of the flattened collection contents
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public LongStream flatmap(final LongFunction<? extends Collection<Long>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatmap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<LongIterator> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new LongIterator() {
                private long next;
                private boolean exhausted;
                private Iterator<? extends Long> cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        cur = null;
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final Collection<? extends Long> c = mapper.apply(next);
                        cur = N.isEmpty(c) ? null : c.iterator();
                    }

                    if (cur != null && cur.hasNext()) {
                        return true;
                    }

                    // An exhausted iterator can retain the entire mapped collection.
                    cur = null;
                    return false;
                }

                @Override
                public long nextLong() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final Long value = cur.next();
                    return value == null ? (long) 0 : value;
                }
            });
        }

        return new ParallelIteratorLongStream(ParallelPrimitiveBuffer.concatLong(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code LongStream} consisting of the results of replacing each element
     * with the contents of the mapped {@code long[]} array. If the stream can be processed
     * sequentially, delegates to the sequential {@code flatMapArray} wrapped in a parallel iterator
     * stream; otherwise flat-maps on worker threads, which pass the flattened elements to the
     * returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code long[]} for each element
     * @return a new parallel {@code LongStream} of the flattened array contents
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public LongStream flatMapArray(final LongFunction<long[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<LongIterator> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new LongIterator() {
                private long next;
                private boolean exhausted;
                private LongIterator cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        cur = LongIterator.of(mapper.apply(next));
                    }

                    return cur != null && cur.hasNext();
                }

                @Override
                public long nextLong() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.nextLong();
                }
            });
        }

        return new ParallelIteratorLongStream(ParallelPrimitiveBuffer.concatLong(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code IntStream} consisting of the results of replacing each element with
     * the contents of the mapped {@code IntStream}. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatMapToInt} wrapped in a parallel iterator stream;
     * otherwise flat-maps on worker threads, which pass the flattened elements to the returned
     * stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function that returns an {@code IntStream} for each element
     * @return a new parallel {@code IntStream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public IntStream flatMapToInt(final LongFunction<? extends IntStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<IntIteratorEx> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new IntIteratorEx() {
                private long next;
                private boolean exhausted;
                private IntIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final IntStream mapped = mapper.apply(next);

                        if (!mappedStream.adopt(mapped)) {
                            // closeResource() already ran, so nothing else will ever close this one.
                            if (mapped != null) {
                                mapped.close();
                            }

                            cur = null;
                            break;
                        }

                        cur = mapped == null ? null : mapped.iteratorEx();
                    }

                    return cur != null && cur.hasNext();
                }

                @Override
                public int nextInt() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.nextInt();
                }

                @Override
                public void closeResource() {
                    closeMappedStream(true);
                }

                private void closeMappedStream() {
                    closeMappedStream(false);
                }

                private void closeMappedStream(final boolean noMoreAdoptions) {
                    @SuppressWarnings("resource")
                    final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> tmp = mappedStream.detach(noMoreAdoptions);

                    if (!noMoreAdoptions) {
                        cur = null;
                    }

                    if (tmp != null) {
                        tmp.close();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = closeHandlersForNewStream(mergeCloseHandler(iters));

        return new ParallelIteratorIntStream(ParallelPrimitiveBuffer.concatInt(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@code FloatStream} consisting of the results of replacing each element with
     * the contents of the mapped {@code FloatStream}. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatMapToFloat} wrapped in a parallel iterator stream;
     * otherwise flat-maps on worker threads, which pass the flattened elements to the returned stream
     * through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code FloatStream} for each element
     * @return a new parallel {@code FloatStream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public FloatStream flatMapToFloat(final LongFunction<? extends FloatStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatMapToFloat(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<FloatIteratorEx> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new FloatIteratorEx() {
                private long next;
                private boolean exhausted;
                private FloatIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final FloatStream mapped = mapper.apply(next);

                        if (!mappedStream.adopt(mapped)) {
                            // closeResource() already ran, so nothing else will ever close this one.
                            if (mapped != null) {
                                mapped.close();
                            }

                            cur = null;
                            break;
                        }

                        cur = mapped == null ? null : mapped.iteratorEx();
                    }

                    return cur != null && cur.hasNext();
                }

                @Override
                public float nextFloat() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.nextFloat();
                }

                @Override
                public void closeResource() {
                    closeMappedStream(true);
                }

                private void closeMappedStream() {
                    closeMappedStream(false);
                }

                private void closeMappedStream(final boolean noMoreAdoptions) {
                    @SuppressWarnings("resource")
                    final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> tmp = mappedStream.detach(noMoreAdoptions);

                    if (!noMoreAdoptions) {
                        cur = null;
                    }

                    if (tmp != null) {
                        tmp.close();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = closeHandlersForNewStream(mergeCloseHandler(iters));

        return new ParallelIteratorFloatStream(ParallelPrimitiveBuffer.concatFloat(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel {@code DoubleStream} consisting of the results of replacing each element with
     * the contents of the mapped {@code DoubleStream}. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatMapToDouble} wrapped in a parallel iterator stream;
     * otherwise flat-maps on worker threads, which pass the flattened elements to the returned stream
     * through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code DoubleStream} for each element
     * @return a new parallel {@code DoubleStream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public DoubleStream flatMapToDouble(final LongFunction<? extends DoubleStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatMapToDouble(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<DoubleIteratorEx> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new DoubleIteratorEx() {
                private long next;
                private boolean exhausted;
                private DoubleIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final DoubleStream mapped = mapper.apply(next);

                        if (!mappedStream.adopt(mapped)) {
                            // closeResource() already ran, so nothing else will ever close this one.
                            if (mapped != null) {
                                mapped.close();
                            }

                            cur = null;
                            break;
                        }

                        cur = mapped == null ? null : mapped.iteratorEx();
                    }

                    return cur != null && cur.hasNext();
                }

                @Override
                public double nextDouble() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.nextDouble();
                }

                @Override
                public void closeResource() {
                    closeMappedStream(true);
                }

                private void closeMappedStream() {
                    closeMappedStream(false);
                }

                private void closeMappedStream(final boolean noMoreAdoptions) {
                    @SuppressWarnings("resource")
                    final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> tmp = mappedStream.detach(noMoreAdoptions);

                    if (!noMoreAdoptions) {
                        cur = null;
                    }

                    if (tmp != null) {
                        tmp.close();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = closeHandlersForNewStream(mergeCloseHandler(iters));

        return new ParallelIteratorDoubleStream(ParallelPrimitiveBuffer.concatDouble(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel object {@code Stream} consisting of the results of replacing each element
     * with the contents of the mapped {@code Stream}. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatMapToObj} wrapped in a parallel iterator stream;
     * otherwise flat-maps the primitive elements on worker threads, which pass the elements of the
     * mapped streams to the returned stream through a bounded buffer.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that returns a {@code Stream<T>} for each element
     * @return a new parallel object {@code Stream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public <T> Stream<T> flatMapToObj(final LongFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<ObjIteratorEx<T>> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private long next;
                private boolean exhausted;
                private Iterator<? extends T> cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final Stream<? extends T> mapped = mapper.apply(next);

                        if (!mappedStream.adopt(mapped)) {
                            // closeResource() already ran, so nothing else will ever close this one.
                            if (mapped != null) {
                                mapped.close();
                            }

                            cur = null;
                            break;
                        }

                        cur = mapped == null ? null : mapped.iteratorEx();
                    }

                    return cur != null && cur.hasNext();
                }

                @Override
                public T next() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.next();
                }

                @Override
                public void closeResource() {
                    closeMappedStream(true);
                }

                private void closeMappedStream() {
                    closeMappedStream(false);
                }

                private void closeMappedStream(final boolean noMoreAdoptions) {
                    @SuppressWarnings("resource")
                    final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> tmp = mappedStream.detach(noMoreAdoptions);

                    if (!noMoreAdoptions) {
                        cur = null;
                    }

                    if (tmp != null) {
                        tmp.close();
                    }
                }
            });
        }

        final Deque<LocalRunnable> newCloseHandlers = closeHandlersForNewStream(mergeCloseHandler(iters));

        return new ParallelIteratorStream<>(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null,
                maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    /**
     * Returns a parallel object {@code Stream} consisting of the results of replacing each element
     * with the contents of the mapped collection. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatmapToObj} wrapped in a parallel iterator stream;
     * otherwise flat-maps the primitive elements on worker threads, which pass the elements of the
     * mapped collections to the returned stream through a bounded buffer.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that returns a {@code Collection<T>} for each element
     * @return a new parallel object {@code Stream} of the flattened collection contents
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public <T> Stream<T> flatmapToObj(final LongFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatmapToObj(mapper), false, null, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final int threadNum = N.min(maxThreadNum, toIndex - fromIndex);
        final LongIterator source = iteratorEx();
        final List<ObjIteratorEx<T>> iters = new ArrayList<>(threadNum);

        for (int i = 0; i < threadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private long next;
                private boolean exhausted;
                private Iterator<? extends T> cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        cur = null;
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextLong();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final Collection<? extends T> c = mapper.apply(next);
                        cur = N.isEmpty(c) ? null : c.iterator();
                    }

                    if (cur != null && cur.hasNext()) {
                        return true;
                    }

                    // An exhausted iterator can retain the entire mapped collection.
                    cur = null;
                    return false;
                }

                @Override
                public T next() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.next();
                }
            });
        }

        return new ParallelIteratorStream<>(Stream.parallelConcatIterators(iters, iters.size(), cancelUncompletedThreads, asyncExecutor), false, null,
                maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel stream that applies the given action to each element as elements are
     * consumed. The action may be invoked concurrently from multiple worker threads.
     * This method is primarily used for debugging.
     *
     * <p>If the stream can be processed sequentially, delegates to the sequential implementation.
     *
     * @param action a non-interfering action to perform on each element
     * @return a new parallel {@code LongStream} with the side-effecting action attached
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    @Override
    public LongStream onEach(final LongConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.onEach(action);
        }

        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    /**
     * Performs the given action on each element of this stream in parallel. The array is
     * divided into slices (when using {@code ARRAY} splitStrategy) or elements are consumed via a
     * shared synchronized cursor (when using {@code ITERATOR} splitStrategy), with each thread
     * processing its assigned elements concurrently.
     *
     * <p>The encounter order of action invocations is not guaranteed. Once the action throws,
     * the workers stop taking further elements, so some elements may never be processed; after
     * all workers have finished, the first exception is rethrown and any later ones are added to
     * it as suppressed exceptions.
     *
     * @param <E> the type of exception the action may throw
     * @param action a non-interfering action to perform on each element
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws E if the action throws an exception
     */
    @Override
    public <E extends Exception> void forEach(final Throwables.LongConsumer<E> action) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            super.forEach(action);
            return;
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            action.accept(elements[cursor++]);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    long next = 0;

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
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Collects elements of this stream into a {@link Map} in parallel by applying the given key and value
     * mappers to each element. If the stream can be processed sequentially, delegates to the sequential
     * implementation; otherwise boxes elements and uses the parallel object stream.
     *
     * <p>If multiple elements map to the same key, the {@code mergeFunction} is used to resolve collisions.
     *
     * <p>In the parallel path the boxed object stream's threads take elements one at a time from a shared
     * iterator (whatever the split strategy) into separate partial maps, which are merged afterwards, so
     * colliding values reach {@code mergeFunction} in no particular order. The merge function must therefore
     * be commutative as well as associative; for example {@code (a, b) -> a} does not reliably keep the value
     * of the first element in encounter order.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param <M> the type of the resulting map
     * @param <E> the type of exception the key mapper may throw
     * @param <E2> the type of exception the value mapper may throw
     * @param keyMapper a non-interfering, stateless function to produce map keys
     * @param valueMapper a non-interfering, stateless function to produce map values
     * @param mergeFunction a merge function used to resolve key collisions
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a {@code Map} whose keys and values are derived from this stream's elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if any of {@code keyMapper}, {@code valueMapper}, {@code mergeFunction}, or
     *         {@code mapFactory} is {@code null}.
     * @throws NullPointerException if {@code mapFactory} returns {@code null}.
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Throwables.LongFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, NullPointerException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Long, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Long, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    /**
     * Groups elements of this stream into a {@link Map} in parallel by applying the given classifier
     * and collecting values with the downstream {@link Collector}. If the stream can be processed
     * sequentially, delegates to the sequential implementation; otherwise boxes elements and uses
     * the parallel object stream.
     *
     * <p>In the parallel path the boxed object stream's threads take elements one at a time from a shared
     * iterator (whatever the split strategy), so each downstream container receives an interleaved subset of
     * its group's elements; an order-sensitive downstream result (for example {@code Collectors.toList()}) may
     * not follow the encounter order.
     *
     * @param <K> the type of the keys in the resulting map
     * @param <D> the type of the downstream reduction result
     * @param <M> the type of the resulting map
     * @param <E> the type of exception the key mapper may throw
     * @param keyMapper a non-interfering, stateless function to classify elements into groups
     * @param downstream a {@code Collector} used to collect elements in each group
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a {@code Map} grouping elements by the classifier with values aggregated by the downstream collector
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code keyMapper}, {@code downstream}, or {@code mapFactory} is {@code null}.
     * @throws NullPointerException if {@code mapFactory} returns {@code null} or {@code keyMapper} returns a {@code null} key.
     * @throws E if the key mapper throws an exception
     */
    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Collector<? super Long, ?, D> downstream, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, NullPointerException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Long, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    /**
     * Performs a parallel reduction on elements of this stream using the provided identity value
     * and associative accumulation function. The array is partitioned across threads, each thread
     * computes a partial result, and the partial results are combined using the same accumulator.
     *
     * <p>The accumulator function must be associative to produce correct results when run in
     * parallel across multiple threads. With {@link BaseStream.SplitStrategy#ARRAY}, each thread
     * reduces one contiguous slice and the partial results are combined in slice order. With the
     * default {@link BaseStream.SplitStrategy#ITERATOR}, threads take elements one at a time from a
     * shared cursor, so each partial result covers an interleaved subset of the elements; the
     * accumulator must then also be commutative, otherwise the result can differ from a sequential
     * reduction and from run to run.
     *
     * @param identity the identity value for the accumulation function (also the result for empty streams)
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the parallel reduction
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code accumulator} is {@code null}.
     */
    @Override
    public long reduce(final long identity, final LongBinaryOperator accumulator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.reduce(identity, accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Long>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);

                    long result = identity;

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            result = accumulator.applyAsLong(result, elements[cursor++]);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    long result = identity;
                    long next = 0;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            result = accumulator.applyAsLong(result, next);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Long result = null;

            for (final Long partialResult : partialResults) {
                result = result == null ? partialResult : accumulator.applyAsLong(result, partialResult);
            }

            return result == null ? identity : result;
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a parallel reduction on elements of this stream using the provided associative
     * accumulation function, with no identity value. The array is partitioned across threads,
     * each thread computes a partial result, and the partial results are combined using the same
     * accumulator. Returns an empty {@code OptionalLong} if the stream is empty.
     *
     * <p>The accumulator function must be associative to produce correct results when run in
     * parallel across multiple threads. With {@link BaseStream.SplitStrategy#ARRAY}, each thread
     * reduces one contiguous slice and the partial results are combined in slice order. With the
     * default {@link BaseStream.SplitStrategy#ITERATOR}, threads take elements one at a time from a
     * shared cursor, so each partial result covers an interleaved subset of the elements; the
     * accumulator must then also be commutative, otherwise the result can differ from a sequential
     * reduction and from run to run.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalLong} with the result, or empty if the stream contains no elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code accumulator} is {@code null}.
     */
    @Override
    public OptionalLong reduce(final LongBinaryOperator accumulator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.reduce(accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Long>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);

                    if (cursor >= to) {
                        return null;
                    }

                    long result = elements[cursor++];

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            result = accumulator.applyAsLong(result, elements[cursor++]);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    long result = 0;

                    synchronized (elements) {
                        if (cursor.value() < toIndex) {
                            result = elements[cursor.getAndIncrement()];
                        } else {
                            return null;
                        }
                    }

                    long next = 0;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            result = accumulator.applyAsLong(result, next);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Long result = null;

            for (final Long partialResult : partialResults) {
                if (partialResult != null) {
                    result = result == null ? partialResult : accumulator.applyAsLong(result, partialResult);
                }
            }

            return result == null ? OptionalLong.empty() : OptionalLong.of(result);
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a mutable parallel reduction on elements of this stream. Each thread accumulates
     * elements into its own container created by the {@code supplier}, and the per-thread containers
     * are merged using the {@code combiner} after all threads complete.
     *
     * <p>With {@link BaseStream.SplitStrategy#ARRAY}, each container receives one contiguous slice and
     * the containers are merged in slice order, so encounter order is preserved. With the default
     * {@link BaseStream.SplitStrategy#ITERATOR}, threads take elements one at a time from a shared
     * cursor, so each container receives an interleaved subset of the elements; an order-sensitive
     * result (for example a {@code List}) may then not follow the encounter order.
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container
     * @param accumulator a non-interfering, stateless function that folds an element into a container
     * @param combiner a non-interfering, stateless function that merges two containers (used in parallel execution)
     * @return the result of the parallel mutable reduction
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if any of {@code supplier}, {@code accumulator}, or {@code combiner} is {@code null}
     * @throws NullPointerException if {@code supplier} returns {@code null} (checked for every container it creates,
     *         also on an empty stream; the stream is closed)
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjLongConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException, IllegalArgumentException, NullPointerException {
        assertNotClosed();

        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(accumulator, cs.accumulator);
        checkArgNotNull(combiner, cs.combiner);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.collect(supplier, accumulator, combiner);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<R>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);

                    // A null container is recorded like any other worker failure: execute(..) runs this worker under
                    // callWithErrorCapture, so it stops the other workers and is rethrown as the NullPointerException
                    // itself (the object streams make the same check inside their try block).
                    final R container = N.requireNonNull(supplier.get(), "supplier returned null");

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            accumulator.accept(container, elements[cursor++]);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }

                    return container;
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    // A null container is recorded like any other worker failure: execute(..) runs this worker under
                    // callWithErrorCapture, so it stops the other workers and is rethrown as the NullPointerException
                    // itself (the object streams make the same check inside their try block).
                    final R container = N.requireNonNull(supplier.get(), "supplier returned null");
                    long next = 0;

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
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }

                    return container;
                });
            }
        }

        return completeAndCollectResult(futureList, eHolder, supplier, combiner, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Returns {@code true} if any element of this stream matches the given predicate, evaluated
     * in parallel. Threads stop as soon as a matching element is found. Returns {@code false} if
     * the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if any element matches the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.anyMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(false);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);

                    try {
                        while (cursor < to && result.isFalse() && eHolder.value() == null) {
                            if (predicate.test(elements[cursor++])) {
                                result.setTrue();
                                break;
                            }
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    long next = 0;

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
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns {@code true} if all elements of this stream match the given predicate, evaluated
     * in parallel. Threads stop as soon as a non-matching element is found. Returns {@code true}
     * if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if all elements match the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.allMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);

                    try {
                        while (cursor < to && result.isTrue() && eHolder.value() == null) {
                            if (!predicate.test(elements[cursor++])) {
                                result.setFalse();
                                break;
                            }
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    long next = 0;

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
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns {@code true} if no elements of this stream match the given predicate, evaluated
     * in parallel. Threads stop as soon as a matching element is found. Returns {@code true}
     * if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if no elements match the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.noneMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);

                    try {
                        while (cursor < to && result.isTrue() && eHolder.value() == null) {
                            if (predicate.test(elements[cursor++])) {
                                result.setFalse();
                                break;
                            }
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                });
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    long next = 0;

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
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }
                });
            }
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return result.value();
    }

    /**
     * Returns an {@code OptionalLong} with the first element (in encounter order) matching the
     * predicate, evaluated in parallel. Multiple threads search concurrently; the element with
     * the lowest array index among all matches found by any thread is returned.
     *
     * <p>Returns an empty {@code OptionalLong} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalLong} with the first matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalLong findFirst(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findFirst(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Long>> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);
                    int index = 0;
                    long next = 0;

                    try {
                        while (cursor < to && (resultHolder.value() == null || cursor < resultHolder.value().left()) && eHolder.value() == null) {
                            index = cursor;
                            next = elements[cursor++];

                            if (predicate.test(next)) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || index < resultHolder.value().left()) {
                                        resultHolder.setValue(Pair.of(index, next));
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
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int index = 0;
                    long next = 0;

                    try {
                        while (resultHolder.value() == null && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    index = cursor.value();
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(next)) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || index < resultHolder.value().left()) {
                                        resultHolder.setValue(Pair.of(index, next));
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
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? OptionalLong.empty() : OptionalLong.of(resultHolder.value().right());
    }

    /**
     * Returns an {@code OptionalLong} with any element matching the predicate, evaluated in
     * parallel. The first match found by any thread is returned with no guarantee of encounter
     * order. Threads stop as soon as a match is recorded.
     *
     * <p>Returns an empty {@code OptionalLong} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalLong} with any matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalLong findAny(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findAny(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Long> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    final int to = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);
                    long next = 0;

                    try {
                        while (cursor < to && resultHolder.value() == null && eHolder.value() == null) {
                            next = elements[cursor++];

                            if (predicate.test(next)) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null) {
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
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    long next = 0;

                    try {
                        while (resultHolder.value() == null && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(next)) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null) {
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
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? OptionalLong.empty() : OptionalLong.of(resultHolder.value());
    }

    /**
     * Returns an {@code OptionalLong} with the last element (in encounter order) matching the
     * predicate, evaluated in parallel. Multiple threads search from the end of the array
     * concurrently; the element with the highest array index among all matches is returned.
     *
     * <p>Returns an empty {@code OptionalLong} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalLong} with the last matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalLong findLast(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findLast(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Long>> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitStrategy == SplitStrategy.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    final int from = arraySliceBoundary(fromIndex, toIndex, sliceIndex, sliceSize);
                    int cursor = arraySliceBoundary(fromIndex, toIndex, sliceIndex + 1, sliceSize);
                    int index = 0;
                    long next = 0;

                    try {
                        while (cursor > from && (resultHolder.value() == null || cursor > resultHolder.value().left()) && eHolder.value() == null) {
                            index = cursor;
                            next = elements[--cursor];

                            if (predicate.test(next)) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || index > resultHolder.value().left()) {
                                        resultHolder.setValue(Pair.of(index, next));
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
        } else {
            final MutableInt cursor = MutableInt.of(toIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, eHolder, () -> {
                    int index = 0;
                    long next = 0;

                    try {
                        while (resultHolder.value() == null && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() > fromIndex) {
                                    index = cursor.value();
                                    next = elements[cursor.decrementAndGet()];
                                } else {
                                    break;
                                }
                            }

                            if (predicate.test(next)) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || index > resultHolder.value().left()) {
                                        resultHolder.setValue(Pair.of(index, next));
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
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? OptionalLong.empty() : OptionalLong.of(resultHolder.value().right());
    }

    /**
     * Returns a parallel {@code LongStream} formed by zipping this stream with stream {@code b},
     * applying the given function to corresponding element pairs. If the stream can be processed
     * sequentially, uses {@code LongStream.zip}; otherwise uses {@code Stream.parallelZip} for
     * concurrent element-pair processing. The zipped stream length equals the shorter input stream.
     *
     * @param b the second stream to zip with
     * @param zipFunction a function applied to corresponding elements of the two streams
     * @return a new parallel {@code LongStream} of zipped results
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b} or {@code zipFunction} is {@code null}
     */
    @Override
    public LongStream zipWith(final LongStream b, final LongBinaryOperator zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return new ParallelIteratorLongStream(LongStream.zip(this, b, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, this::boxed, b::boxed,
                (boxedA, boxedB) -> new ParallelIteratorLongStream(Stream.parallelZip(boxedA, boxedB, zipFunction::applyAsLong, maxThreadNum, asyncExecutor),
                        false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * Returns a parallel {@code LongStream} formed by zipping this stream with streams {@code b}
     * and {@code c}, applying the given function to corresponding element triples. If the stream
     * can be processed sequentially, uses {@code LongStream.zip}; otherwise uses
     * {@code Stream.parallelZip} for concurrent element-triple processing. The zipped stream
     * length equals the shortest input stream.
     *
     * @param b the second stream to zip with
     * @param c the third stream to zip with
     * @param zipFunction a function applied to corresponding elements of the three streams
     * @return a new parallel {@code LongStream} of zipped results
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b}, {@code c}, or {@code zipFunction} is {@code null}
     */
    @Override
    public LongStream zipWith(final LongStream b, final LongStream c, final LongTernaryOperator zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(c, cs.c);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return new ParallelIteratorLongStream(LongStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, c, this::boxed, b::boxed, c::boxed,
                (boxedA, boxedB, boxedC) -> new ParallelIteratorLongStream(
                        Stream.parallelZip(boxedA, boxedB, boxedC, zipFunction::applyAsLong, maxThreadNum, asyncExecutor), false, maxThreadNum, splitStrategy,
                        asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * Returns a parallel {@code LongStream} formed by zipping this stream with stream {@code b}
     * using padding values when one stream is shorter than the other. If the stream can be
     * processed sequentially, uses {@code LongStream.zip}; otherwise uses {@code Stream.parallelZip}.
     * The zipped stream length equals the longer input stream.
     *
     * @param b the second stream to zip with
     * @param valueForNoneA the padding value used when this stream is exhausted before {@code b}
     * @param valueForNoneB the padding value used when {@code b} is exhausted before this stream
     * @param zipFunction a function applied to corresponding elements (with padding as needed)
     * @return a new parallel {@code LongStream} of zipped results
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b} or {@code zipFunction} is {@code null}
     */
    @Override
    public LongStream zipWith(final LongStream b, final long valueForNoneA, final long valueForNoneB, final LongBinaryOperator zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return new ParallelIteratorLongStream(LongStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitStrategy,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, this::boxed, b::boxed,
                (boxedA, boxedB) -> new ParallelIteratorLongStream(
                        Stream.parallelZip(boxedA, boxedB, valueForNoneA, valueForNoneB, zipFunction::applyAsLong, maxThreadNum, asyncExecutor), false,
                        maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * Returns a parallel {@code LongStream} formed by zipping this stream with streams {@code b}
     * and {@code c} using padding values when streams have unequal lengths. If the stream can be
     * processed sequentially, uses {@code LongStream.zip}; otherwise uses {@code Stream.parallelZip}.
     * The zipped stream length equals the longest input stream.
     *
     * @param b the second stream to zip with
     * @param c the third stream to zip with
     * @param valueForNoneA the padding value used when this stream is exhausted
     * @param valueForNoneB the padding value used when {@code b} is exhausted
     * @param valueForNoneC the padding value used when {@code c} is exhausted
     * @param zipFunction a function applied to corresponding elements (with padding as needed)
     * @return a new parallel {@code LongStream} of zipped results
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b}, {@code c}, or {@code zipFunction} is {@code null}
     */
    @Override
    public LongStream zipWith(final LongStream b, final LongStream c, final long valueForNoneA, final long valueForNoneB, final long valueForNoneC,
            final LongTernaryOperator zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(c, cs.c);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return new ParallelIteratorLongStream(LongStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, c, this::boxed, b::boxed, c::boxed,
                (boxedA, boxedB,
                        boxedC) -> new ParallelIteratorLongStream(Stream.parallelZip(boxedA, boxedB, boxedC, valueForNoneA, valueForNoneB, valueForNoneC,
                                zipFunction::applyAsLong, maxThreadNum, asyncExecutor), false, maxThreadNum, splitStrategy, asyncExecutor,
                                cancelUncompletedThreads, null));
    }

    /**
     * Returns {@code true}, indicating this is a parallel stream.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isParallel() {
        return true;
    }

    /**
     * Returns a sequential {@code LongStream} view of this parallel stream backed by the same
     * underlying array. The returned stream shares the same close handlers as this stream. Before
     * either view is terminated, subsequent calls return the same lazily created instance.
     *
     * @return a sequential {@code LongStream} over the same elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public LongStream sequential() throws IllegalStateException {
        assertNotClosed();

        ArrayLongStream tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;
                if (tmp == null) {
                    tmp = new ArrayLongStream(elements, fromIndex, toIndex, isSorted(), closeHandlersForNewStream());
                    sequential = tmp;
                }
            }
        }

        return tmp;
    }

    /**
     * Returns the maximum number of threads used for parallel execution of this stream.
     *
     * @return the maximum thread count configured for this parallel stream
     */
    @Override
    protected int maxThreadNum() {
        // assertNotClosed();

        return maxThreadNum;
    }

    /**
     * Returns the {@link BaseStream.SplitStrategy} strategy used to partition elements across threads.
     * {@code ARRAY} divides the backing array into fixed slices; {@code ITERATOR} uses a shared
     * synchronized cursor for dynamic load balancing.
     *
     * @return the splitStrategy strategy for this parallel stream
     */
    @Override
    protected BaseStream.SplitStrategy splitStrategy() {
        //  assertNotClosed();

        return splitStrategy;
    }

    /**
     * Returns the {@link AsyncExecutor} used to submit parallel tasks for this stream.
     *
     * @return the async executor for this parallel stream
     */
    @Override
    protected AsyncExecutor asyncExecutor() {
        // assertNotClosed();

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
