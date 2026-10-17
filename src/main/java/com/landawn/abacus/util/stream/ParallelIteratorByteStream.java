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
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteTernaryOperator;
import com.landawn.abacus.util.function.ByteToIntFunction;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.u.OptionalByte;

/**
 * A parallel implementation of ByteStream backed by an iterator that enables concurrent processing
 * of byte elements across multiple threads. This class extends IteratorByteStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution distributes work using synchronized iterator access to ensure
 * thread-safe element consumption while maintaining high throughput for CPU-intensive operations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel byte stream from an iterator
 * ByteIterator byteIterator = ByteIterator.of((byte) 1, (byte) -2, (byte) 3);
 * ByteStream stream = ByteStream.of(byteIterator).parallel();
 *
 * // Process elements in parallel
 * int sum = stream.filter(b -> b > 0)
 *                 .mapToInt(b -> b & 0xFF)
 *                 .sum();
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
 * @see IteratorByteStream
 * @see ByteStream#parallel()
 */
final class ParallelIteratorByteStream extends IteratorByteStream {
    private final int maxThreadNum;
    private final SplitStrategy splitStrategy;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorByteStream sequential;

    /**
     * Constructs a ParallelIteratorByteStream from a ByteIterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the ByteIterator to stream from; it is consumed lazily and must not be advanced externally
     * @param sorted whether the iterator elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed, may be {@code null}
     */
    ParallelIteratorByteStream(final ByteIterator values, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitStrategy = splitStrategy == null ? DEFAULT_SPLIT_STRATEGY : splitStrategy;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorByteStream from a ByteStream with the specified configuration for parallel processing.
     * The source stream's iterator is adopted, and the source stream's own close handlers are merged
     * with the specified ones, so closing this stream also closes the source stream.
     *
     * @param stream the source {@code ByteStream} to iterate over; a {@code null} stream is treated as empty
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers additional close handlers to execute when the stream is closed, may be {@code null}
     * @throws IllegalStateException if {@code stream} is already closed
     */
    ParallelIteratorByteStream(final ByteStream stream, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) throws IllegalStateException {
        this(iterate(stream), sorted, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Constructs a ParallelIteratorByteStream from a boxed {@code Stream<Byte>} with the specified configuration for
     * parallel processing. The source stream's iterator is adopted and unboxed to a {@code ByteIterator}, and the
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
    ParallelIteratorByteStream(final Stream<Byte> stream, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) throws IllegalStateException {
        this(byteIterator(iterate(stream)), sorted, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Returns a parallel stream consisting of elements that match the given predicate. If the stream can be
     * processed sequentially (i.e., {@code maxThreadNum} is no more than 1), delegates to the sequential
     * implementation; otherwise evaluates the predicate on worker threads, which pass the retained elements to
     * the returned stream through a bounded primitive buffer without boxing.
     *
     * <p>The encounter order of elements in the resulting stream is not guaranteed when
     * executing in parallel.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return a new parallel {@code ByteStream} of matching elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Override
    public ByteStream filter(final BytePredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final ByteIterator source = iteratorEx();
        final List<ByteIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIterator() {
                private byte next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (true) {
                            synchronized (source) {
                                if (source.hasNext()) {
                                    next = source.nextByte();
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
                public byte nextByte() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorByteStream(ParallelPrimitiveBuffer.concatByte(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
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
     * @return a new parallel {@code ByteStream} of the elements selected by the parallel {@code takeWhile} operation
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Override
    public ByteStream takeWhile(final BytePredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final ByteIterator source = iteratorEx();
        final List<ByteIterator> iters = new ArrayList<>(maxThreadNum);
        final AtomicBoolean hasMore = new AtomicBoolean(true);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIterator() {
                private byte next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext && hasMore.get()) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
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
                public byte nextByte() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorByteStream(ParallelPrimitiveBuffer.concatByte(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
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
     * @return a new parallel {@code ByteStream} of the elements selected by the parallel {@code dropWhile} operation
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     */
    @Override
    public ByteStream dropWhile(final BytePredicate predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final ByteIterator source = iteratorEx();
        final List<ByteIterator> iters = new ArrayList<>(maxThreadNum);
        final AtomicBoolean dropped = new AtomicBoolean(false);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIterator() {
                private byte next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        // Once the boundary element has been found ('dropped' is set), all threads switch to
                        // pass-through mode and consume the remaining elements via the shared cursor.
                        if (dropped.get()) {
                            synchronized (source) {
                                if (source.hasNext()) {
                                    next = source.nextByte();
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

                                    next = source.nextByte();

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
                                        next = source.nextByte();
                                        hasNext = true;
                                    }
                                }
                            }
                        }
                    }

                    return hasNext;
                }

                @Override
                public byte nextByte() throws NoSuchElementException {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    return next;
                }
            });
        }

        return new ParallelIteratorByteStream(ParallelPrimitiveBuffer.concatByte(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code ByteStream} consisting of the results of applying the given
     * function to each element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise applies the mapper on worker threads, which pass
     * the results to the returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code ByteStream} of mapped elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public ByteStream map(final ByteUnaryOperator mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        final ByteIterator source = iteratorEx();
        final List<ByteIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIterator() {
                private byte next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
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

    /**
     * Returns a parallel {@code IntStream} consisting of the results of applying the given function
     * to each element. If the stream can be processed sequentially, delegates to the sequential
     * implementation; otherwise applies the mapper on worker threads, which pass the results to the
     * returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function mapping {@code byte} to {@code int}
     * @return a new parallel {@code IntStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public IntStream mapToInt(final ByteToIntFunction mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToInt(mapper);
        }

        final ByteIterator source = iteratorEx();
        final List<IntIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIterator() {
                private byte next;
                private boolean hasNext;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
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
    public <T> Stream<T> mapToObj(final ByteFunction<? extends T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToObj(mapper);
        }

        final ByteIterator source = iteratorEx();
        final List<Iterator<T>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private byte next;
                private boolean ready;

                @Override
                public boolean hasNext() {
                    if (!ready) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
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
     * Returns a parallel {@code ByteStream} consisting of the results of replacing each element
     * with the contents of the mapped {@code ByteStream}. If the stream can be processed
     * sequentially, delegates to the sequential {@code flatMap} wrapped in a parallel iterator
     * stream; otherwise flat-maps on worker threads, which pass the flattened elements to the
     * returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code ByteStream} for each element
     * @return a new parallel {@code ByteStream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public ByteStream flatMap(final ByteFunction<? extends ByteStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatMap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final ByteIterator source = iteratorEx();
        final List<ByteIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIteratorEx() {
                private byte next;
                private boolean exhausted;
                private ByteIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
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

    /**
     * Returns a parallel {@code ByteStream} consisting of the results of replacing each element
     * with the contents of the mapped collection. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatmap} wrapped in a parallel iterator stream; otherwise
     * flat-maps on worker threads, which unbox each collection element into a bounded primitive
     * buffer that feeds the returned stream.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code Collection<Byte>} for each element
     * @return a new parallel {@code ByteStream} of the flattened collection contents
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public ByteStream flatmap(final ByteFunction<? extends Collection<Byte>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatmap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final ByteIterator source = iteratorEx();
        final List<ByteIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIterator() {
                private byte next;
                private boolean exhausted;
                private Iterator<? extends Byte> cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        cur = null;
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        final Collection<? extends Byte> c = mapper.apply(next);
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
                public byte nextByte() throws NoSuchElementException {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final Byte value = cur.next();
                    return value == null ? (byte) 0 : value;
                }
            });
        }

        return new ParallelIteratorByteStream(ParallelPrimitiveBuffer.concatByte(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlersForNewStream());
    }

    /**
     * Returns a parallel {@code ByteStream} consisting of the results of replacing each element
     * with the contents of the mapped {@code byte[]} array. If the stream can be processed
     * sequentially, delegates to the sequential {@code flatMapArray} wrapped in a parallel iterator
     * stream; otherwise flat-maps on worker threads, which pass the flattened elements to the
     * returned stream through a bounded primitive buffer without boxing.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code byte[]} for each element
     * @return a new parallel {@code ByteStream} of the flattened array contents
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code mapper} is {@code null}.
     */
    @Override
    public ByteStream flatMapArray(final ByteFunction<byte[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final ByteIterator source = iteratorEx();
        final List<ByteIterator> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ByteIterator() {
                private byte next;
                private boolean exhausted;
                private ByteIterator cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
                            } else {
                                exhausted = true;
                                cur = null;
                                break;
                            }
                        }

                        cur = ByteIterator.of(mapper.apply(next));
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
            });
        }

        return new ParallelIteratorByteStream(ParallelPrimitiveBuffer.concatByte(iters, cancelUncompletedThreads, asyncExecutor), false, maxThreadNum,
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
    public IntStream flatMapToInt(final ByteFunction<? extends IntStream> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        final ByteIterator source = iteratorEx();
        final List<IntIteratorEx> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new IntIteratorEx() {
                private byte next;
                private boolean exhausted;
                private IntIterator cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
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
    public <T> Stream<T> flatMapToObj(final ByteFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final ByteIterator source = iteratorEx();
        final List<ObjIteratorEx<T>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private byte next;
                private boolean exhausted;
                private Iterator<? extends T> cur = null;
                private final MappedStreamHolder mappedStream = new MappedStreamHolder();

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        closeMappedStream();

                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
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
    public <T> Stream<T> flatmapToObj(final ByteFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatmapToObj(mapper), false, null, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        final ByteIterator source = iteratorEx();
        final List<ObjIteratorEx<T>> iters = new ArrayList<>(maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            iters.add(new ObjIteratorEx<>() {
                private byte next;
                private boolean exhausted;
                private Iterator<? extends T> cur = null;

                @Override
                public boolean hasNext() {
                    while ((cur == null || !cur.hasNext()) && !exhausted) {
                        cur = null;
                        synchronized (source) {
                            if (source.hasNext()) {
                                next = source.nextByte();
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
     * @return a new parallel {@code ByteStream} with the side-effecting action attached
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    @Override
    public ByteStream onEach(final ByteConsumer action) throws IllegalStateException, IllegalArgumentException {
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

    /**
     * Performs the given action on each element of this stream in parallel. Multiple threads
     * consume elements concurrently via a shared synchronized iterator, each thread pulling
     * the next available element and applying the action.
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
    public <E extends Exception> void forEach(final Throwables.ByteConsumer<E> action) throws IllegalStateException, IllegalArgumentException, E {
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
                byte next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextByte();
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
     * Collects elements of this stream into a {@link Map} in parallel by applying the given key and value
     * mappers to each element. If the stream can be processed sequentially, delegates to the sequential
     * implementation; otherwise boxes elements and uses the parallel object stream.
     *
     * <p>If multiple elements map to the same key, the {@code mergeFunction} is used to resolve collisions.
     *
     * <p>In this parallel stream, threads take elements one at a time from a shared cursor into separate
     * partial maps, which are merged afterwards, so colliding values reach {@code mergeFunction} in no
     * particular order. The merge function must therefore be commutative as well as associative; for example
     * {@code (a, b) -> a} does not reliably keep the value of the first element in encounter order.
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Throwables.ByteFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, NullPointerException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Byte, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Byte, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    /**
     * Groups elements of this stream into a {@link Map} in parallel by applying the given classifier
     * and collecting values with the downstream {@link Collector}. If the stream can be processed
     * sequentially, delegates to the sequential implementation; otherwise boxes elements and uses
     * the parallel object stream.
     *
     * <p>In this parallel stream, threads take elements one at a time from a shared cursor, so each
     * downstream container receives an interleaved subset of its group's elements; an order-sensitive
     * downstream result (for example {@code Collectors.toList()}) may not follow the encounter order.
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
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Collector<? super Byte, ?, D> downstream, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, NullPointerException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Byte, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    /**
     * Performs a parallel reduction on elements of this stream using the provided identity value
     * and associative accumulation function. Multiple threads consume elements concurrently via a
     * shared synchronized iterator, each thread computing a partial result, which are then combined
     * using the same accumulator.
     *
     * <p>The accumulator function must be associative to produce correct results when run in
     * parallel across multiple threads. Because threads take elements one at a time from the shared
     * iterator (regardless of the split strategy), each partial result covers an interleaved subset
     * of the elements; the accumulator must therefore also be commutative, otherwise the result can
     * differ from a sequential reduction and from run to run.
     *
     * @param identity the identity value for the accumulation function (also the result for empty streams)
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the parallel reduction
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code accumulator} is {@code null}.
     */
    @Override
    public byte reduce(final byte identity, final ByteBinaryOperator accumulator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator);
        }

        final List<ContinuableFuture<Byte>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                byte result = identity;
                byte next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextByte();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsByte(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Byte result = null;

            for (final Byte partialResult : partialResults) {
                result = result == null ? partialResult : accumulator.applyAsByte(result, partialResult);
            }

            return result == null ? identity : result;
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a parallel reduction on elements of this stream using the provided associative
     * accumulation function, with no identity value. Multiple threads consume elements concurrently
     * via a shared synchronized iterator, each thread computing a partial result, which are then
     * combined. Returns an empty {@code OptionalByte} if the stream is empty.
     *
     * <p>The accumulator function must be associative to produce correct results when run in
     * parallel across multiple threads. Because threads take elements one at a time from the shared
     * iterator (regardless of the split strategy), each partial result covers an interleaved subset
     * of the elements; the accumulator must therefore also be commutative, otherwise the result can
     * differ from a sequential reduction and from run to run.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalByte} with the result, or empty if the stream contains no elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code accumulator} is {@code null}.
     */
    @Override
    public OptionalByte reduce(final ByteBinaryOperator accumulator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<Byte>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                byte result = 0;

                synchronized (elements) {
                    if (elements.hasNext()) {
                        result = elements.nextByte();
                    } else {
                        return null;
                    }
                }

                byte next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextByte();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsByte(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Byte result = null;

            for (final Byte partialResult : partialResults) {
                if (partialResult != null) {
                    result = result == null ? partialResult : accumulator.applyAsByte(result, partialResult);
                }
            }

            return result == null ? OptionalByte.empty() : OptionalByte.of(result);
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    /**
     * Performs a mutable parallel reduction on elements of this stream. Each thread accumulates
     * elements into its own container created by the {@code supplier}, consuming elements via a
     * shared synchronized iterator. The per-thread containers are merged using the {@code combiner}
     * after all threads complete.
     *
     * <p>Because threads take elements one at a time from the shared iterator, each container receives
     * an interleaved subset of the elements; an order-sensitive result (for example a {@code List})
     * may not follow the encounter order.
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
    public <R> R collect(final Supplier<R> supplier, final ObjByteConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
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
                byte next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextByte();
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
     * Returns {@code true} if any element of this stream matches the given predicate, evaluated
     * in parallel. Multiple threads consume elements via a shared synchronized iterator; threads
     * stop as soon as a matching element is found. Returns {@code false} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if any element matches the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
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
                byte next = 0;

                try {
                    while (result.isFalse() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextByte();
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
     * Returns {@code true} if all elements of this stream match the given predicate, evaluated
     * in parallel. Multiple threads consume elements via a shared synchronized iterator; threads
     * stop as soon as a non-matching element is found. Returns {@code true} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if all elements match the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
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
                byte next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextByte();
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
     * Returns {@code true} if no elements of this stream match the given predicate, evaluated
     * in parallel. Multiple threads consume elements via a shared synchronized iterator; threads
     * stop as soon as a matching element is found. Returns {@code true} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if no elements match the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
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
                byte next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextByte();
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
     * Returns an {@code OptionalByte} with the first element (in encounter order) matching the
     * predicate, evaluated in parallel. Multiple threads consume elements via a shared synchronized
     * iterator; each match is recorded with its sequential index and the element with the lowest
     * index is returned.
     *
     * <p>Returns an empty {@code OptionalByte} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalByte} with the first matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalByte findFirst(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Byte>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                // Primitive locals instead of a reused Pair: setLeft(..) boxed the index of every element scanned.
                long candidateIndex = 0;
                byte candidate = 0;

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                candidateIndex = index.getAndIncrement();
                                candidate = elements.nextByte();
                            } else {
                                break;
                            }
                        }

                        if (predicate.test(candidate)) {
                            synchronized (resultHolder) {
                                if (resultHolder.value() == null || candidateIndex < resultHolder.value().left()) {
                                    resultHolder.setValue(Pair.of(candidateIndex, candidate));
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

        return resultHolder.value() == null ? OptionalByte.empty() : OptionalByte.of(resultHolder.value().right());
    }

    /**
     * Returns an {@code OptionalByte} with any element matching the predicate, evaluated in
     * parallel. Multiple threads consume elements via a shared synchronized iterator; the first
     * match found by any thread is returned with no guarantee of encounter order.
     *
     * <p>Returns an empty {@code OptionalByte} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalByte} with any matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalByte findAny(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Byte> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                byte next = 0;

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextByte();
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

        return resultHolder.value() == null ? OptionalByte.empty() : OptionalByte.of(resultHolder.value());
    }

    /**
     * Returns an {@code OptionalByte} with the last element (in encounter order) matching the
     * predicate, evaluated in parallel. Multiple threads consume all elements via a shared
     * synchronized iterator, tracking sequential indices; the element with the highest index
     * among all matches is returned.
     *
     * <p>Returns an empty {@code OptionalByte} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalByte} with the last matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code predicate} is {@code null}.
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalByte findLast(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Byte>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, eHolder, () -> {
                // Primitive locals instead of a reused Pair: setLeft(..) boxed the index of every element scanned.
                long candidateIndex = 0;
                byte candidate = 0;
                boolean found = false;
                long lastMatchIndex = 0;
                byte lastMatch = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                candidateIndex = index.getAndIncrement();
                                candidate = elements.nextByte();
                            } else {
                                break;
                            }
                        }

                        if (predicate.test(candidate)) {
                            found = true;
                            lastMatchIndex = candidateIndex;
                            lastMatch = candidate;
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

        return resultHolder.value() == null ? OptionalByte.empty() : OptionalByte.of(resultHolder.value().right());
    }

    /**
     * Returns a parallel {@code ByteStream} formed by zipping this stream with stream {@code b},
     * applying the given function to corresponding element pairs. If the stream can be processed
     * sequentially, uses {@code ByteStream.zip}; otherwise uses {@code Stream.parallelZip} for
     * concurrent element-pair processing. The zipped stream length equals the shorter input stream.
     *
     * @param b the second stream to zip with
     * @param zipFunction a function applied to corresponding elements of the two streams
     * @return a new parallel {@code ByteStream} of zipped results
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b} or {@code zipFunction} is {@code null}
     */
    @Override
    public ByteStream zipWith(final ByteStream b, final ByteBinaryOperator zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorByteStream(ByteStream.zip(this, b, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, this::boxed, b::boxed,
                (boxedA, boxedB) -> new ParallelIteratorByteStream(Stream.parallelZip(boxedA, boxedB, zipFunction::applyAsByte, maxThreadNum, asyncExecutor),
                        false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * Returns a parallel {@code ByteStream} formed by zipping this stream with streams {@code b}
     * and {@code c}, applying the given function to corresponding element triples. If the stream
     * can be processed sequentially, uses {@code ByteStream.zip}; otherwise uses
     * {@code Stream.parallelZip} for concurrent element-triple processing. The zipped stream
     * length equals the shortest input stream.
     *
     * @param b the second stream to zip with
     * @param c the third stream to zip with
     * @param zipFunction a function applied to corresponding elements of the three streams
     * @return a new parallel {@code ByteStream} of zipped results
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b}, {@code c}, or {@code zipFunction} is {@code null}
     */
    @Override
    public ByteStream zipWith(final ByteStream b, final ByteStream c, final ByteTernaryOperator zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(c, cs.c);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorByteStream(ByteStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, c, this::boxed, b::boxed, c::boxed,
                (boxedA, boxedB, boxedC) -> new ParallelIteratorByteStream(
                        Stream.parallelZip(boxedA, boxedB, boxedC, zipFunction::applyAsByte, maxThreadNum, asyncExecutor), false, maxThreadNum, splitStrategy,
                        asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * Returns a parallel {@code ByteStream} formed by zipping this stream with stream {@code b}
     * using padding values when one stream is shorter than the other. If the stream can be
     * processed sequentially, uses {@code ByteStream.zip}; otherwise uses {@code Stream.parallelZip}.
     * The zipped stream length equals the longer input stream.
     *
     * @param b the second stream to zip with
     * @param valueForNoneA the padding value used when this stream is exhausted before {@code b}
     * @param valueForNoneB the padding value used when {@code b} is exhausted before this stream
     * @param zipFunction a function applied to corresponding elements (with padding as needed)
     * @return a new parallel {@code ByteStream} of zipped results
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b} or {@code zipFunction} is {@code null}
     */
    @Override
    public ByteStream zipWith(final ByteStream b, final byte valueForNoneA, final byte valueForNoneB, final ByteBinaryOperator zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorByteStream(ByteStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitStrategy,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, this::boxed, b::boxed,
                (boxedA, boxedB) -> new ParallelIteratorByteStream(
                        Stream.parallelZip(boxedA, boxedB, valueForNoneA, valueForNoneB, zipFunction::applyAsByte, maxThreadNum, asyncExecutor), false,
                        maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null));
    }

    /**
     * Returns a parallel {@code ByteStream} formed by zipping this stream with streams {@code b}
     * and {@code c} using padding values when streams have unequal lengths. If the stream can be
     * processed sequentially, uses {@code ByteStream.zip}; otherwise uses {@code Stream.parallelZip}.
     * The zipped stream length equals the longest input stream.
     *
     * @param b the second stream to zip with
     * @param c the third stream to zip with
     * @param valueForNoneA the padding value used when this stream is exhausted
     * @param valueForNoneB the padding value used when {@code b} is exhausted
     * @param valueForNoneC the padding value used when {@code c} is exhausted
     * @param zipFunction a function applied to corresponding elements (with padding as needed)
     * @return a new parallel {@code ByteStream} of zipped results
     *
     * @throws IllegalStateException if this stream or a source stream is already closed
     * @throws IllegalArgumentException if {@code b}, {@code c}, or {@code zipFunction} is {@code null}
     */
    @Override
    public ByteStream zipWith(final ByteStream b, final ByteStream c, final byte valueForNoneA, final byte valueForNoneB, final byte valueForNoneC,
            final ByteTernaryOperator zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(c, cs.c);
        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorByteStream(ByteStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
        }

        // If boxing b (or c) fails, e.g. because it is already closed, close this stream too, as the sequential zip path does.
        return closingOpenedSources(this, b, c, this::boxed, b::boxed, c::boxed,
                (boxedA, boxedB,
                        boxedC) -> new ParallelIteratorByteStream(Stream.parallelZip(boxedA, boxedB, boxedC, valueForNoneA, valueForNoneB, valueForNoneC,
                                zipFunction::applyAsByte, maxThreadNum, asyncExecutor), false, maxThreadNum, splitStrategy, asyncExecutor,
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
     * Returns a sequential {@code ByteStream} view of this parallel stream backed by the same
     * underlying iterator. The sequential stream is lazily created and cached for reuse. The
     * returned stream shares the same close handlers as this stream.
     *
     * @return a sequential {@code ByteStream} over the same elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream sequential() throws IllegalStateException {
        assertNotClosed();

        IteratorByteStream tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;
                if (tmp == null) {
                    tmp = new IteratorByteStream(elements, isSorted(), closeHandlersForNewStream());
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
     * Returns the {@link BaseStream.SplitStrategy} strategy configured for this stream. Since this
     * stream is iterator-backed, elements are always consumed via a shared synchronized iterator
     * regardless of the splitStrategy setting.
     *
     * @return the splitStrategy strategy for this parallel stream
     */
    @Override
    protected BaseStream.SplitStrategy splitStrategy() {
        // assertNotClosed();

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
