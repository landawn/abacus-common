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
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteTernaryOperator;
import com.landawn.abacus.util.function.ByteToIntFunction;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.ToByteFunction;

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
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorByteStream sequential;

    /**
     * Constructs a ParallelIteratorByteStream from a ByteIterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the ByteIterator to stream from
     * @param sorted whether the iterator elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorByteStream(final ByteIterator values, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorByteStream from a ByteStream with the specified configuration for parallel processing.
     * The stream is converted to an iterator internally.
     *
     * @param stream the ByteStream to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorByteStream(final ByteStream stream, final boolean sorted, final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor,
            final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(iterate(stream), sorted, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Constructs a ParallelIteratorByteStream from a boxed Stream of Bytes with the specified configuration for parallel processing.
     * The stream is unboxed to a ByteIterator internally.
     *
     * @param stream the Stream of Byte objects to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorByteStream(final Stream<Byte> stream, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(byteIterator(iterate(stream)), sorted, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Returns a parallel stream consisting of elements that match the given predicate.
     * If the stream can be processed sequentially (i.e., {@code maxThreadNum} is no more than 1), delegates to
     * the sequential implementation; otherwise boxes elements and uses the parallel object stream.
     *
     * <p>The encounter order of elements in the resulting stream is not guaranteed when
     * executing in parallel.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return a new parallel {@code ByteStream} of matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream filter(final BytePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final Stream<Byte> stream = boxed().filter(predicate::test);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream of elements that match the predicate until a worker observes
     * a non-matching element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise boxes elements and uses the parallel object stream.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix
     * semantics; later matching elements may be returned.
     *
     * @param predicate a non-interfering, stateless predicate applied to elements
     * @return a new parallel {@code ByteStream} of the elements selected by the parallel {@code takeWhile} operation
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream takeWhile(final BytePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final Stream<Byte> stream = boxed().takeWhile(predicate::test);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream after dropping matching elements until a worker observes a
     * non-matching element. If the stream can be
     * processed sequentially, delegates to the sequential implementation; otherwise boxes
     * elements and uses the parallel object stream.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix/suffix
     * semantics; later matching elements may be dropped.
     *
     * @param predicate a non-interfering, stateless predicate applied to elements
     * @return a new parallel {@code ByteStream} of the elements selected by the parallel {@code dropWhile} operation
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream dropWhile(final BytePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final Stream<Byte> stream = boxed().dropWhile(predicate::test);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@code ByteStream} consisting of the results of applying the given
     * function to each element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise boxes elements and uses the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code ByteStream} of mapped elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream map(final ByteUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        @SuppressWarnings("resource")
        final ByteStream stream = boxed().mapToByte(mapper::applyAsByte);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@code IntStream} consisting of the results of applying the given
     * function to each element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise boxes elements and maps using the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function mapping {@code byte} to {@code int}
     * @return a new parallel {@code IntStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final ByteToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToInt(mapper);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().mapToInt(mapper::applyAsInt);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel object {@code Stream} consisting of the results of applying the given
     * function to each element. If the stream can be processed sequentially, delegates to the
     * sequential implementation; otherwise boxes elements and maps using the parallel object stream.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel object {@code Stream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> mapToObj(final ByteFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToObj(mapper);
        }

        //noinspection resource
        return boxed().map(mapper::apply);
    }

    /**
     * Returns a parallel {@code ByteStream} consisting of the results of replacing each element
     * with the contents of the mapped {@code ByteStream}. If the stream can be processed
     * sequentially, delegates to the sequential {@code flatMap} wrapped in a parallel iterator
     * stream; otherwise boxes elements and flat-maps using the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code ByteStream} for each element
     * @return a new parallel {@code ByteStream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream flatMap(final ByteFunction<? extends ByteStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final ByteStream stream = boxed().flatMapToByte(mapper::apply);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel {@code ByteStream} consisting of the results of replacing each element
     * with the contents of the mapped collection. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatmap} wrapped in a parallel iterator stream; otherwise
     * boxes elements and flat-maps collections using the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code Collection<Byte>} for each element
     * @return a new parallel {@code ByteStream} of the flattened collection contents
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream flatmap(final ByteFunction<? extends Collection<Byte>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatmap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final ByteStream stream = boxed().flatmap(mapper::apply).mapToByte(ToByteFunction.UNBOX);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel {@code ByteStream} consisting of the results of replacing each element
     * with the contents of the mapped {@code byte[]} array. If the stream can be processed
     * sequentially, delegates to the sequential {@code flatMapArray} wrapped in a parallel iterator
     * stream; otherwise boxes elements and flat-maps using the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code byte[]} for each element
     * @return a new parallel {@code ByteStream} of the flattened array contents
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream flatMapArray(final ByteFunction<byte[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final ByteStream stream = boxed().flatMapToByte(value -> ByteStream.of(mapper.apply(value)));

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel {@code IntStream} consisting of the results of replacing each element
     * with the contents of the mapped {@code IntStream}. If the stream can be processed
     * sequentially, delegates to the sequential {@code flatMapToInt} wrapped in a parallel iterator
     * stream; otherwise boxes elements and flat-maps using the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function that returns an {@code IntStream} for each element
     * @return a new parallel {@code IntStream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final ByteFunction<? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().flatMapToInt(mapper::apply);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel object {@code Stream} consisting of the results of replacing each element
     * with the contents of the mapped {@code Stream}. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatMapToObj} wrapped in a parallel iterator stream;
     * otherwise boxes elements and flat-maps using the parallel object stream.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that returns a {@code Stream<T>} for each element
     * @return a new parallel object {@code Stream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatMapToObj(final ByteFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        //noinspection resource
        return boxed().flatMap(mapper::apply);
    }

    /**
     * Returns a parallel object {@code Stream} consisting of the results of replacing each element
     * with the contents of the mapped collection. If the stream can be processed sequentially,
     * delegates to the sequential {@code flatmapToObj} wrapped in a parallel iterator stream;
     * otherwise boxes elements and flat-maps collections using the parallel object stream.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function that returns a {@code Collection<T>} for each element
     * @return a new parallel object {@code Stream} of the flattened collection contents
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatmapToObj(final ByteFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatmapToObj(mapper), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        //noinspection resource
        return boxed().flatmap(mapper::apply);
    }

    /**
     * Returns a parallel stream that applies the given action to each element as elements are
     * consumed. The action is applied sequentially within the internal object stream before
     * the result is mapped back to bytes. This method is primarily used for debugging.
     *
     * <p>If the stream can be processed sequentially, delegates to the sequential implementation.
     *
     * @param action a non-interfering action to perform on each element
     * @return a new parallel {@code ByteStream} with the side-effecting action attached
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream onEach(final ByteConsumer action) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.onEach(action);
        }

        @SuppressWarnings("resource")
        final ByteStream stream = boxed().onEach(action::accept).sequential().mapToByte(ToByteFunction.UNBOX);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Performs the given action on each element of this stream in parallel. Multiple threads
     * consume elements concurrently via a shared synchronized iterator, each thread pulling
     * the next available element and applying the action.
     *
     * <p>The encounter order of action invocations is not guaranteed. Exceptions thrown by
     * the action are collected and re-thrown after all threads complete.
     *
     * @param <E> the type of exception the action may throw
     * @param action a non-interfering action to perform on each element
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @Override
    public <E extends Exception> void forEach(final Throwables.ByteConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            super.forEach(action);
            return;
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
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
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Throwables.ByteFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

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
     * @param <K> the type of the keys in the resulting map
     * @param <D> the type of the downstream reduction result
     * @param <M> the type of the resulting map
     * @param <E> the type of exception the key mapper may throw
     * @param keyMapper a non-interfering, stateless function to classify elements into groups
     * @param downstream a {@code Collector} used to collect elements in each group
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a {@code Map} grouping elements by the classifier with values aggregated by the downstream collector
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     */
    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Collector<? super Byte, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, E {
        assertNotClosed();

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
     * parallel across multiple threads.
     *
     * @param identity the identity value for the accumulation function (also the result for empty streams)
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the parallel reduction
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public byte reduce(final byte identity, final ByteBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator);
        }

        final List<ContinuableFuture<Byte>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
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
     * parallel across multiple threads.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalByte} with the result, or empty if the stream contains no elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalByte reduce(final ByteBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<Byte>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
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
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container
     * @param accumulator a non-interfering, stateless function that folds an element into a container
     * @param combiner a non-interfering, stateless function that merges two containers (used in parallel execution)
     * @return the result of the parallel mutable reduction
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjByteConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
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
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
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
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
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
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
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
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalByte findFirst(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Byte>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Byte> pair = new Pair<>();

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextByte());
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
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalByte findAny(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Byte> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
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
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalByte findLast(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Byte>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Byte> pair = new Pair<>();

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextByte());
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream zipWith(final ByteStream b, final ByteBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorByteStream(ByteStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        return new ParallelIteratorByteStream(Stream.parallelZip(boxed(), b.boxed(), zipFunction::applyAsByte, maxThreadNum), false, maxThreadNum, splitor,
                asyncExecutor, cancelUncompletedThreads, null);
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream zipWith(final ByteStream b, final ByteStream c, final ByteTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorByteStream(ByteStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorByteStream(Stream.parallelZip(boxed(), b.boxed(), c.boxed(), zipFunction::applyAsByte, maxThreadNum), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, null);
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream zipWith(final ByteStream b, final byte valueForNoneA, final byte valueForNoneB, final ByteBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorByteStream(ByteStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorByteStream(Stream.parallelZip(boxed(), b.boxed(), valueForNoneA, valueForNoneB, zipFunction::applyAsByte, maxThreadNum),
                false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream zipWith(final ByteStream b, final ByteStream c, final byte valueForNoneA, final byte valueForNoneB, final byte valueForNoneC,
            final ByteTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorByteStream(ByteStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorByteStream(
                Stream.parallelZip(boxed(), b.boxed(), c.boxed(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction::applyAsByte, maxThreadNum), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
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
                    tmp = new IteratorByteStream(elements, isSorted(), closeHandlers());
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
     * Returns the {@link BaseStream.Splitor} strategy configured for this stream. Since this
     * stream is iterator-backed, elements are always consumed via a shared synchronized iterator
     * regardless of the splitor setting.
     *
     * @return the splitor strategy for this parallel stream
     */
    @Override
    protected BaseStream.Splitor splitor() {
        // assertNotClosed();

        return splitor;
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
