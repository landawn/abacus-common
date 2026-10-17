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
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToCharFunction;
import com.landawn.abacus.util.function.IntToFloatFunction;
import com.landawn.abacus.util.function.IntToShortFunction;
import com.landawn.abacus.util.u.OptionalInt;

/**
 * A parallel implementation of IntStream backed by an iterator that enables concurrent processing
 * of int elements across multiple threads. This class extends IteratorIntStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution distributes work using synchronized iterator access to ensure
 * thread-safe element consumption while maintaining high throughput for CPU-intensive operations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel int stream from an iterator
 * IntIterator intIterator = IntIterator.of(1, -2, 3);
 * IntStream stream = IntStream.of(intIterator).parallel();
 *
 * // Process elements in parallel
 * int sum = stream.filter(i -> i > 0).sum();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> A parallel operation synchronizes its own worker threads while they
 * consume the underlying iterator. The stream instance itself is not intended to be driven
 * concurrently by multiple callers or reused for independent operations.
 *
 * <p><b>Encounter Order:</b> Unless an operation explicitly states otherwise, parallel intermediate
 * operations may emit elements in a different order from the source.
 *
 * <p><b>Failure Handling:</b> Terminal operations wait for every submitted worker before closing
 * the stream or a temporary executor. Partial results are finished before cleanup; the primary
 * failure is rethrown and additional failures are suppressed.
 *
 * @see IteratorIntStream
 * @see IntStream#parallel()
 */
final class ParallelIteratorIntStream extends IteratorIntStream {
    private final int maxThreadNum;
    private final SplitStrategy splitStrategy;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorIntStream sequential;

    /**
     * Constructs a ParallelIteratorIntStream from an IntIterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the IntIterator to stream from; it is consumed lazily and must not be advanced externally
     * @param sorted whether the iterator elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed, may be {@code null}
     */
    ParallelIteratorIntStream(final IntIterator values, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitStrategy = splitStrategy == null ? DEFAULT_SPLIT_STRATEGY : splitStrategy;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorIntStream from an IntStream with the specified configuration for parallel processing.
     * The source stream's iterator is adopted, and the source stream's own close handlers are merged
     * with the specified ones, so closing this stream also closes the source stream.
     *
     * @param stream the source {@code IntStream} to iterate over; a {@code null} stream is treated as empty
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers additional close handlers to execute when the stream is closed, may be {@code null}
     * @throws IllegalStateException if {@code stream} is already closed
     */
    ParallelIteratorIntStream(final IntStream stream, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) throws IllegalStateException {
        this(iterate(stream), sorted, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Constructs a ParallelIteratorIntStream from a boxed {@code Stream<Integer>} with the specified configuration for
     * parallel processing. The source stream's iterator is adopted and unboxed to an {@code IntIterator}, and the
     * source stream's own close handlers are merged with the specified ones, so closing this stream also closes
     * the source stream. A {@code null} element in the source stream causes a {@code NullPointerException} when
     * that element is unboxed.
     *
     * @param stream the source boxed stream to iterate over; a {@code null} stream is treated as empty
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers additional close handlers to execute when the stream is closed, may be {@code null}
     * @throws IllegalStateException if {@code stream} is already closed
     */
    ParallelIteratorIntStream(final Stream<Integer> stream, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) throws IllegalStateException {
        this(intIterator(iterate(stream)), sorted, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate,
     * evaluated in parallel. Elements are consumed from the shared synchronized iterator across
     * multiple threads. Falls back to sequential execution if the thread count is insufficient
     * to justify parallel overhead.
     *
     * <p>The order of elements in the resulting stream is not guaranteed.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return the new stream
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Override
    public IntStream filter(final IntPredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final IntIterator source = iteratorEx();
        final List<IntIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIterator() {
                private int next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (true) {
                            synchronized (source) {
                                if (source.hasNext()) {
                                    next = source.nextInt();
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
                public int nextInt() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorIntStream(ParallelPrimitiveBuffer.concatInt(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    @Override
    public IntStream takeWhile(final IntPredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final IntIterator source = iteratorEx();
        final List<IntIterator> iters = new ArrayList<>(maxThreadNum);
        final AtomicBoolean hasMore = new AtomicBoolean(true);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIterator() {
                private int next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext && hasMore.get()) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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
                public int nextInt() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorIntStream(ParallelPrimitiveBuffer.concatInt(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    @Override
    public IntStream dropWhile(final IntPredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final IntIterator source = iteratorEx();
        final List<IntIterator> iters = new ArrayList<>(maxThreadNum);
        final AtomicBoolean dropped = new AtomicBoolean(false);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIterator() {
                private int next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        // Once the boundary element has been found ('dropped' is set), all threads switch to
                        // pass-through mode and consume the remaining elements via the shared cursor.
                        if (dropped.get()) {
                            synchronized (source) {
                                if (source.hasNext()) {
                                    next = source.nextInt();
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

                                    next = source.nextInt();

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
                                        next = source.nextInt();
                                        hasNext = true;
                                    }
                                }
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public int nextInt() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorIntStream(ParallelPrimitiveBuffer.concatInt(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    @Override
    public IntStream map(final IntUnaryOperator mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        final IntIterator source = iteratorEx();
        final List<IntIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIterator() {
                private int next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public CharStream mapToChar(final IntToCharFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToChar(mapper);
        }

        final IntIterator source = iteratorEx();
        final List<CharIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new CharIterator() {
                private int next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
                                hasNext = true;
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public char nextChar() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final char result = mapper.applyAsChar(next);
                    hasNext = false;
                    return result;
                }
            });
        }

        return new ParallelIteratorCharStream(ParallelPrimitiveBuffer.concatChar(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    @Override
    public ByteStream mapToByte(final IntToByteFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToByte(mapper);
        }

        final IntIterator source = iteratorEx();
        final List<ByteIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIterator() {
                private int next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
                                hasNext = true;
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public byte nextByte() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final byte result = mapper.applyAsByte(next);
                    hasNext = false;
                    return result;
                }
            });
        }

        return new ParallelIteratorByteStream(ParallelPrimitiveBuffer.concatByte(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    @Override
    public ShortStream mapToShort(final IntToShortFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToShort(mapper);
        }

        final IntIterator source = iteratorEx();
        final List<ShortIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ShortIterator() {
                private int next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
                                hasNext = true;
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public short nextShort() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final short result = mapper.applyAsShort(next);
                    hasNext = false;
                    return result;
                }
            });
        }

        return new ParallelIteratorShortStream(ParallelPrimitiveBuffer.concatShort(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    @Override
    public LongStream mapToLong(final IntToLongFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToLong(mapper);
        }

        final IntIterator source = iteratorEx();
        final List<LongIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new LongIterator() {
                private int next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public FloatStream mapToFloat(final IntToFloatFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToFloat(mapper);
        }

        final IntIterator source = iteratorEx();
        final List<FloatIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new FloatIterator() {
                private int next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public DoubleStream mapToDouble(final IntToDoubleFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToDouble(mapper);
        }

        final IntIterator source = iteratorEx();
        final List<DoubleIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new DoubleIterator() {
                private int next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public <T> Stream<T> mapToObj(final IntFunction<? extends T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToObj(mapper);
        }

        final IntIterator source = iteratorEx();
        final List<Iterator<T>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private int next;
                private boolean ready;

                @Override
                public boolean hasNext() {
                    if (!ready) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public IntStream flatMap(final IntFunction<? extends IntStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final IntIterator source = iteratorEx();
        final List<IntIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIteratorEx() {
                private int next;
                private boolean exhausted;
                private IntIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public IntStream flatmap(final IntFunction<? extends Collection<Integer>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatmap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final IntIterator source = iteratorEx();
        final List<IntIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIterator() {
                private int next;
                private boolean exhausted;
                private Iterator<? extends Integer> cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        cur = null;
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final Collection<? extends Integer> c = mapper.apply(next);
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
                public int nextInt() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final Integer value = cur.next();
                    return value == null ? (int) 0 : value;
                }
            });
        }

        return new ParallelIteratorIntStream(ParallelPrimitiveBuffer.concatInt(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    @Override
    public IntStream flatMapArray(final IntFunction<int[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final IntIterator source = iteratorEx();
        final List<IntIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIterator() {
                private int next;
                private boolean exhausted;
                private IntIterator cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        cur = IntIterator.of(mapper.apply(next));
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
            });
        }

        return new ParallelIteratorIntStream(ParallelPrimitiveBuffer.concatInt(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    @Override
    public CharStream flatMapToChar(final IntFunction<? extends CharStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorCharStream(sequential().flatMapToChar(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final IntIterator source = iteratorEx();
        final List<CharIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new CharIteratorEx() {
                private int next;
                private boolean exhausted;
                private CharIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final CharStream mapped = mapper.apply(next);

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
                public char nextChar() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.nextChar();
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

        return new ParallelIteratorCharStream(ParallelPrimitiveBuffer.concatChar(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    @Override
    public ByteStream flatMapToByte(final IntFunction<? extends ByteStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatMapToByte(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final IntIterator source = iteratorEx();
        final List<ByteIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIteratorEx() {
                private int next;
                private boolean exhausted;
                private ByteIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final ByteStream mapped = mapper.apply(next);

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
                public byte nextByte() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.nextByte();
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

        return new ParallelIteratorByteStream(ParallelPrimitiveBuffer.concatByte(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    @Override
    public ShortStream flatMapToShort(final IntFunction<? extends ShortStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorShortStream(sequential().flatMapToShort(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final IntIterator source = iteratorEx();
        final List<ShortIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ShortIteratorEx() {
                private int next;
                private boolean exhausted;
                private ShortIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final ShortStream mapped = mapper.apply(next);

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
                public short nextShort() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.nextShort();
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

        return new ParallelIteratorShortStream(ParallelPrimitiveBuffer.concatShort(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, newCloseHandlers);
    }

    @Override
    public LongStream flatMapToLong(final IntFunction<? extends LongStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMapToLong(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final IntIterator source = iteratorEx();
        final List<LongIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new LongIteratorEx() {
                private int next;
                private boolean exhausted;
                private LongIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public FloatStream flatMapToFloat(final IntFunction<? extends FloatStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatMapToFloat(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final IntIterator source = iteratorEx();
        final List<FloatIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new FloatIteratorEx() {
                private int next;
                private boolean exhausted;
                private FloatIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public DoubleStream flatMapToDouble(final IntFunction<? extends DoubleStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatMapToDouble(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final IntIterator source = iteratorEx();
        final List<DoubleIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new DoubleIteratorEx() {
                private int next;
                private boolean exhausted;
                private DoubleIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public <T> Stream<T> flatMapToObj(final IntFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final IntIterator source = iteratorEx();
        final List<ObjIteratorEx<T>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private int next;
                private boolean exhausted;
                private Iterator<? extends T> cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public <T> Stream<T> flatmapToObj(final IntFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatmapToObj(mapper), false, null, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final IntIterator source = iteratorEx();
        final List<ObjIteratorEx<T>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private int next;
                private boolean exhausted;
                private Iterator<? extends T> cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        cur = null;
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextInt();
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

    @Override
    public IntStream onEach(final IntConsumer action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        if (canBeSequential(maxThreadNum)) {
            return super.onEach(action);
        }

        return map(value -> {
            action.accept(value);
            return value;
        });
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.IntConsumer<E> action) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        if (canBeSequential(maxThreadNum)) {
            super.forEach(action);
            return;
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                int next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextInt();
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

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In this parallel stream, threads take elements one at a time from a shared cursor into separate
     * partial maps, which are merged afterwards, so colliding values reach {@code mergeFunction} in no
     * particular order. The merge function must therefore be commutative as well as associative; for example
     * {@code (a, b) -> a} does not reliably keep the value of the first element in encounter order.
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Throwables.IntFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, NullPointerException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Integer, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Integer, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In this parallel stream, threads take elements one at a time from a shared cursor, so each
     * downstream container receives an interleaved subset of its group's elements; an order-sensitive
     * downstream result (for example {@code Collectors.toList()}) may not follow the encounter order.
     */
    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Collector<? super Integer, ?, D> downstream, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, NullPointerException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Integer, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In this parallel stream, threads take elements one at a time from a shared cursor, so each
     * partial result covers an interleaved subset of the elements. The accumulator must therefore be
     * commutative as well as associative; otherwise the result can differ from a sequential reduction
     * and from run to run.
     */
    @Override
    public int reduce(final int identity, final IntBinaryOperator accumulator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator);
        }

        final List<ContinuableFuture<Integer>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                int result = identity;
                int next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextInt();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsInt(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Integer result = null;

            for (final Integer partialResult : partialResults) {
                result = result == null ? partialResult : accumulator.applyAsInt(result, partialResult);
            }

            return result == null ? identity : result;
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In this parallel stream, threads take elements one at a time from a shared cursor, so each
     * partial result covers an interleaved subset of the elements. The accumulator must therefore be
     * commutative as well as associative; otherwise the result can differ from a sequential reduction
     * and from run to run.
     */
    @Override
    public OptionalInt reduce(final IntBinaryOperator accumulator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<Integer>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                int result = 0;

                synchronized (elements) {
                    if (elements.hasNext()) {
                        result = elements.nextInt();
                    } else {
                        return null;
                    }
                }

                int next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextInt();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsInt(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Integer result = null;

            for (final Integer partialResult : partialResults) {
                if (partialResult != null) {
                    result = result == null ? partialResult : accumulator.applyAsInt(result, partialResult);
                }
            }

            return result == null ? OptionalInt.empty() : OptionalInt.of(result);
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * {@inheritDoc}
     *
     * <p>In this parallel stream, threads take elements one at a time from a shared cursor, so each
     * container receives an interleaved subset of the elements; an order-sensitive result (for example
     * an {@code IntList}) may not follow the encounter order.
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjIntConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException, IllegalArgumentException, NullPointerException {
        assertNotClosed();

        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(accumulator, cs.accumulator);
        checkArgNotNull(combiner, cs.combiner);

        if (canBeSequential(maxThreadNum)) {
            return super.collect(supplier, accumulator, combiner);
        }

        final List<ContinuableFuture<R>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                // A null container is recorded like any other worker failure: execute(..) runs this worker under
                // callWithErrorCapture, so it stops the other workers and is rethrown as the NullPointerException
                // itself (the object streams make the same check inside their try block).
                final R container = N.requireNonNull(supplier.get(), "supplier returned null");
                int next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextInt();
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

    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.anyMatch(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(false);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                int next = 0;

                try {
                    while (result.isFalse() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextInt();
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

    @Override
    public <E extends Exception> boolean allMatch(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.allMatch(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                int next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextInt();
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

    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.noneMatch(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                int next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextInt();
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

    @Override
    public <E extends Exception> OptionalInt findFirst(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Integer>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                // Primitive locals: no boxed index/element per taken element (inside the lock); a Pair is created only on a match.
                long nextIndex = 0;
                int next = 0;

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                nextIndex = index.getAndIncrement();
                                next = elements.nextInt();
                            } else {
                                break;
                            }
                        }

                        if (predicate.test(next)) {
                            synchronized (resultHolder) {
                                if (resultHolder.value() == null || nextIndex < resultHolder.value().left()) {
                                    resultHolder.setValue(Pair.of(nextIndex, next));
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

        return resultHolder.value() == null ? OptionalInt.empty() : OptionalInt.of(resultHolder.value().right());
    }

    @Override
    public <E extends Exception> OptionalInt findAny(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Integer> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                int next = 0;

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextInt();
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

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? OptionalInt.empty() : OptionalInt.of(resultHolder.value());
    }

    @Override
    public <E extends Exception> OptionalInt findLast(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Integer>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                // Primitive locals: no boxed index/element per taken element (inside the lock); a Pair is created only on a match.
                long nextIndex = 0;
                int next = 0;
                boolean found = false;
                long lastMatchIndex = 0;
                int lastMatch = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                nextIndex = index.getAndIncrement();
                                next = elements.nextInt();
                            } else {
                                break;
                            }
                        }

                        if (predicate.test(next)) {
                            found = true;
                            lastMatchIndex = nextIndex;
                            lastMatch = next;
                        }
                    }

                    if (found) {
                        synchronized (resultHolder) {
                            if (resultHolder.value() == null || lastMatchIndex > resultHolder.value().left()) {
                                resultHolder.setValue(Pair.of(lastMatchIndex, lastMatch));
                            }
                        }
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }
            });
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? OptionalInt.empty() : OptionalInt.of(resultHolder.value().right());
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b} or {@code zipFunction} is {@code null}
     */
    @Override
    public IntStream zipWith(final IntStream b, final IntBinaryOperator zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorIntStream(IntStream.zip(this, b, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, this::boxed, b::boxed,
                (boxedA, boxedB) -> new ParallelIteratorIntStream(Stream.parallelZip(boxedA, boxedB, zipFunction::applyAsInt, maxThreadNum, asyncExecutor),
                        false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b}, {@code c}, or {@code zipFunction} is {@code null}
     */
    @Override
    public IntStream zipWith(final IntStream b, final IntStream c, final IntTernaryOperator zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(c, cs.c);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorIntStream(IntStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, c, this::boxed, b::boxed, c::boxed,
                (boxedA, boxedB, boxedC) -> new ParallelIteratorIntStream(
                        Stream.parallelZip(boxedA, boxedB, boxedC, zipFunction::applyAsInt, maxThreadNum, asyncExecutor), false, maxThreadNum, splitStrategy,
                        asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b} or {@code zipFunction} is {@code null}
     */
    @Override
    public IntStream zipWith(final IntStream b, final int valueForNoneA, final int valueForNoneB, final IntBinaryOperator zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorIntStream(IntStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitStrategy,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, this::boxed, b::boxed,
                (boxedA, boxedB) -> new ParallelIteratorIntStream(
                        Stream.parallelZip(boxedA, boxedB, valueForNoneA, valueForNoneB, zipFunction::applyAsInt, maxThreadNum, asyncExecutor), false,
                        maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b}, {@code c}, or {@code zipFunction} is {@code null}
     */
    @Override
    public IntStream zipWith(final IntStream b, final IntStream c, final int valueForNoneA, final int valueForNoneB, final int valueForNoneC,
            final IntTernaryOperator zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(c, cs.c);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorIntStream(IntStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, c, this::boxed, b::boxed, c::boxed,
                (boxedA, boxedB,
                        boxedC) -> new ParallelIteratorIntStream(Stream.parallelZip(boxedA, boxedB, boxedC, valueForNoneA, valueForNoneB, valueForNoneC,
                                zipFunction::applyAsInt, maxThreadNum, asyncExecutor), false, maxThreadNum, splitStrategy, asyncExecutor,
                                cancelUncompletedThreads, null));
    }

    @Override
    public boolean isParallel() {
        return true;
    }

    @Override
    public IntStream sequential() throws IllegalStateException {
        assertNotClosed();

        IteratorIntStream tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;

                if (tmp == null) {
                    tmp = new IteratorIntStream(elements, isSorted(), closeHandlersForNewStream());
                    sequential = tmp;
                }
            }
        }

        return tmp;
    }

    @Override
    protected int maxThreadNum() {
        //    assertNotClosed();

        return maxThreadNum;
    }

    @Override
    protected BaseStream.SplitStrategy splitStrategy() {
        //   assertNotClosed();

        return splitStrategy;
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
