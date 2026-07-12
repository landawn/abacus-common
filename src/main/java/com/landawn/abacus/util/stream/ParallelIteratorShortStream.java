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
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortTernaryOperator;
import com.landawn.abacus.util.function.ShortToIntFunction;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.function.ToShortFunction;

/**
 * A parallel implementation of ShortStream backed by an iterator that enables concurrent processing
 * of short elements across multiple threads. This class extends IteratorShortStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution distributes work using synchronized iterator access to ensure
 * thread-safe element consumption while maintaining high throughput for CPU-intensive operations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel short stream from an iterator
 * ShortStream stream = ShortStream.of(shortIterator).parallel();
 *
 * // Process elements in parallel
 * int sum = stream.filter(s -> s > 0).sum();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Operations on this stream are thread-safe and properly synchronized
 * when accessing the underlying iterator during parallel execution.
 *
 * @see IteratorShortStream
 * @see ShortStream#parallel()
 */
final class ParallelIteratorShortStream extends IteratorShortStream {
    private final int maxThreadNum;
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorShortStream sequential;

    /**
     * Constructs a ParallelIteratorShortStream from a ShortIterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the ShortIterator to stream from
     * @param sorted whether the iterator elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorShortStream(final ShortIterator values, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorShortStream from a ShortStream with the specified configuration for parallel processing.
     * The stream is converted to an iterator internally.
     *
     * @param stream the ShortStream to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorShortStream(final ShortStream stream, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(iterate(stream), sorted, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Constructs a ParallelIteratorShortStream from a boxed Stream of Shorts with the specified configuration for parallel processing.
     * The stream is unboxed to a ShortIterator internally.
     *
     * @param stream the Stream of Short objects to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorShortStream(final Stream<Short> stream, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(shortIterator(iterate(stream)), sorted, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
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
     */
    @Override
    public ShortStream filter(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final Stream<Short> stream = boxed().filter(predicate::test);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a stream of elements that match the predicate until a worker observes a
     * non-matching element, evaluated in parallel. Falls back to sequential execution if the
     * thread count is insufficient to justify parallel overhead.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix
     * semantics; later matching elements may be returned.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return the new stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream takeWhile(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final Stream<Short> stream = boxed().takeWhile(predicate::test);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a stream after dropping matching elements until a worker observes a
     * non-matching element, evaluated in parallel. Falls
     * back to sequential execution if the thread count is insufficient to justify parallel overhead.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix/suffix
     * semantics; later matching elements may be dropped.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return the new stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream dropWhile(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final Stream<Short> stream = boxed().dropWhile(predicate::test);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a stream consisting of the results of applying the given mapper function to the
     * elements of this stream, evaluated in parallel. Falls back to sequential execution if the
     * thread count is insufficient to justify parallel overhead.
     *
     * <p>The encounter order of mapped elements is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return the new stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream map(final ShortUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        @SuppressWarnings("resource")
        final ShortStream stream = boxed().mapToShort(mapper::applyAsShort);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns an {@link IntStream} consisting of the results of applying the given mapper function
     * to the elements of this stream, evaluated in parallel. Falls back to sequential execution if
     * the thread count is insufficient to justify parallel overhead.
     *
     * <p>The encounter order of mapped elements is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return the new {@code IntStream}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final ShortToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToInt(mapper);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().mapToInt(mapper::applyAsInt);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns an object-valued {@link Stream} consisting of the results of applying the given
     * mapper function to the elements of this stream, evaluated in parallel. Falls back to
     * sequential execution if the thread count is insufficient to justify parallel overhead.
     *
     * <p>The encounter order of mapped elements is not guaranteed.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return the new {@code Stream}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> mapToObj(final ShortFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToObj(mapper);
        }

        //noinspection resource
        return boxed().map(mapper::apply);
    }

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with
     * the contents of a mapped stream produced by applying the given mapper function, evaluated
     * in parallel. Falls back to sequential execution if the thread count is insufficient to
     * justify parallel overhead.
     *
     * <p>The encounter order of elements from mapped streams is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function to apply to each element, returning a ShortStream
     * @return the new stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream flatMap(final ShortFunction<? extends ShortStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorShortStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final ShortStream stream = boxed().flatMapToShort(mapper::apply);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with
     * the contents of a mapped collection produced by applying the given mapper function, evaluated
     * in parallel. Falls back to sequential execution if the thread count is insufficient to
     * justify parallel overhead.
     *
     * <p>The encounter order of elements from mapped collections is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function to apply to each element, returning a Collection of Short
     * @return the new stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream flatmap(final ShortFunction<? extends Collection<Short>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorShortStream(sequential().flatmap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final ShortStream stream = boxed().flatmap(mapper::apply).mapToShort(ToShortFunction.UNBOX);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a stream consisting of the results of replacing each element of this stream with
     * the contents of a mapped {@code short[]} array produced by applying the given mapper function,
     * evaluated in parallel. Falls back to sequential execution if the thread count is insufficient
     * to justify parallel overhead.
     *
     * <p>The encounter order of elements from mapped arrays is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function to apply to each element, returning a short array
     * @return the new stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream flatMapArray(final ShortFunction<short[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorShortStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final ShortStream stream = boxed().flatMapToShort(value -> ShortStream.of(mapper.apply(value)));

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns an {@link IntStream} consisting of the results of replacing each element of this
     * stream with the contents of a mapped IntStream produced by applying the given mapper
     * function, evaluated in parallel. Falls back to sequential execution if the thread count
     * is insufficient to justify parallel overhead.
     *
     * <p>The encounter order of elements from mapped streams is not guaranteed.
     *
     * @param mapper a non-interfering, stateless function to apply to each element, returning an IntStream
     * @return the new {@code IntStream}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final ShortFunction<? extends IntStream> mapper) throws IllegalStateException {
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
     * Returns an object-valued {@link Stream} consisting of the results of replacing each element
     * of this stream with the contents of a mapped Stream produced by applying the given mapper
     * function, evaluated in parallel. Falls back to sequential execution if the thread count
     * is insufficient to justify parallel overhead.
     *
     * <p>The encounter order of elements from mapped streams is not guaranteed.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element, returning a Stream
     * @return the new {@code Stream}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatMapToObj(final ShortFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
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
     * Returns an object-valued {@link Stream} consisting of the results of replacing each element
     * of this stream with the contents of a mapped Collection produced by applying the given mapper
     * function, evaluated in parallel. Falls back to sequential execution if the thread count
     * is insufficient to justify parallel overhead.
     *
     * <p>The encounter order of elements from mapped collections is not guaranteed.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element, returning a Collection
     * @return the new {@code Stream}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatmapToObj(final ShortFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
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
     * Returns a stream consisting of the elements of this stream, additionally performing the
     * provided action on each element as elements are consumed from the resulting stream. The
     * action is applied sequentially within the internal object stream before the result is
     * mapped back to shorts. Falls back to sequential execution if the thread count is
     * insufficient to justify parallel overhead.
     *
     * <p>This is an intermediate operation primarily intended for debugging and logging purposes.
     *
     * @param action a non-interfering action to perform on each element as it is consumed
     * @return the new stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream onEach(final ShortConsumer action) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.onEach(action);
        }

        @SuppressWarnings("resource")
        final ShortStream stream = boxed().onEach(action::accept).sequential().mapToShort(ToShortFunction.UNBOX);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Performs the given action on each element of this stream in parallel. Multiple threads
     * consume elements from the shared synchronized iterator concurrently, with each thread
     * processing its obtained elements independently.
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
    public <E extends Exception> void forEach(final Throwables.ShortConsumer<E> action) throws IllegalStateException, E {
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
                short next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextShort();
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
     * Accumulates the elements of this stream into a {@link Map} using the provided key and value
     * mapper functions, evaluated in parallel. Falls back to sequential execution if the thread
     * count is insufficient to justify parallel overhead.
     *
     * <p>If multiple elements map to the same key, the merge function is used to resolve the
     * conflict. The map is created using the provided factory.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception the key mapper may throw
     * @param <E2> the type of exception the value mapper may throw
     * @param keyMapper a function to map each element to a key
     * @param valueMapper a function to map each element to a value
     * @param mergeFunction a function to resolve key collisions
     * @param mapFactory a supplier providing the result map
     * @return a Map containing the stream elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.ShortFunction<? extends K, E> keyMapper,
            final Throwables.ShortFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Short, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Short, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    /**
     * Groups the elements of this stream into a {@link Map} by the given key mapper, collecting
     * each group using the provided downstream collector, evaluated in parallel. Falls back to
     * sequential execution if the thread count is insufficient to justify parallel overhead.
     *
     * @param <K> the type of map keys
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting Map
     * @param <E> the type of exception the key mapper may throw
     * @param keyMapper a function to classify elements into groups
     * @param downstream a Collector for accumulating elements within each group
     * @param mapFactory a supplier providing the result map
     * @return a Map containing grouped stream elements
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     */
    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.ShortFunction<? extends K, E> keyMapper,
            final Collector<? super Short, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Short, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    /**
     * Performs a reduction on the elements of this stream using the provided identity value and
     * accumulation function, evaluated in parallel. Each thread independently reduces a subset
     * of elements using the identity as the initial value, and the partial results are combined
     * by additional accumulator applications.
     *
     * <p>The accumulator function must be associative and stateless to ensure correct parallel results.
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator a non-interfering, stateless, associative function for combining two values
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public short reduce(final short identity, final ShortBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator);
        }

        final List<ContinuableFuture<Short>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                short result = identity;
                short next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextShort();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsShort(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        Short result = null;

        try {
            for (final ContinuableFuture<Short> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (result == null) {
                    result = future.get();
                } else {
                    result = accumulator.applyAsShort(result, future.get());
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

        return result == null ? identity : result;
    }

    /**
     * Performs a reduction on the elements of this stream using the provided accumulation
     * function, evaluated in parallel. Returns an {@link OptionalShort} describing the result,
     * or an empty optional if the stream is empty. Each thread independently reduces a subset
     * of elements; partial results are then combined.
     *
     * <p>The accumulator function must be associative and stateless to ensure correct parallel results.
     *
     * @param accumulator a non-interfering, stateless, associative function for combining two values
     * @return an OptionalShort with the result of the reduction, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalShort reduce(final ShortBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<Short>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                short result = 0;

                synchronized (elements) {
                    if (elements.hasNext()) {
                        result = elements.nextShort();
                    } else {
                        return null;
                    }
                }

                short next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextShort();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsShort(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        Short result = null;

        try {
            for (final ContinuableFuture<Short> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                final Short tmp = future.get();

                if (tmp == null) {
                    // continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result = accumulator.applyAsShort(result, tmp);
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

        return result == null ? OptionalShort.empty() : OptionalShort.of(result);
    }

    /**
     * Performs a mutable reduction on the elements of this stream in parallel. Each thread
     * creates its own result container using the supplier, accumulates elements into it, and
     * then the partial containers are combined using the combiner function.
     *
     * <p>The supplier, accumulator, and combiner functions must be non-interfering and stateless.
     * The combiner must be compatible with the accumulator for correct parallel results.
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new result container
     * @param accumulator a function to fold an element into a result container
     * @param combiner a function to combine two partial result containers
     * @return the result of the mutable reduction
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjShortConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
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
                short next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextShort();
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
     * Returns whether any elements of this stream match the given predicate, evaluated in
     * parallel. May not evaluate the predicate on all elements if a match is found early.
     * Falls back to sequential execution if the thread count is insufficient to justify
     * parallel overhead.
     *
     * <p>Returns {@code false} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if any elements match the predicate, otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
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
                short next = 0;

                try {
                    while (result.isFalse() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextShort();
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
     * Returns whether all elements of this stream match the given predicate, evaluated in
     * parallel. May not evaluate the predicate on all elements if a non-matching element is
     * found early. Falls back to sequential execution if the thread count is insufficient to
     * justify parallel overhead.
     *
     * <p>Returns {@code true} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if all elements match the predicate (or the stream is empty), otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
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
                short next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextShort();
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
     * Returns whether no elements of this stream match the given predicate, evaluated in
     * parallel. May not evaluate the predicate on all elements if a matching element is found
     * early. Falls back to sequential execution if the thread count is insufficient to justify
     * parallel overhead.
     *
     * <p>Returns {@code true} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if no elements match the predicate (or the stream is empty), otherwise {@code false}
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
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
                short next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextShort();
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
     * Returns an {@link OptionalShort} describing the first element of this stream that matches
     * the given predicate, respecting encounter order. Multiple threads search concurrently;
     * each thread tracks the sequential index of matching elements using a shared {@code MutableLong}
     * counter, and the result with the lowest encounter index is returned.
     *
     * <p>Falls back to sequential execution if the thread count is insufficient to justify
     * parallel overhead.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an OptionalShort with the first matching element in encounter order, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalShort findFirst(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Short>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Short> pair = new Pair<>();

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextShort());
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

        return resultHolder.value() == null ? OptionalShort.empty() : OptionalShort.of(resultHolder.value().right());
    }

    /**
     * Returns an {@link OptionalShort} describing some element of this stream that matches the
     * given predicate, with no guarantee of which matching element is returned. Multiple threads
     * search concurrently; the first thread to find a match sets the result and all threads
     * stop searching.
     *
     * <p>Falls back to sequential execution if the thread count is insufficient to justify
     * parallel overhead.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an OptionalShort with some matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalShort findAny(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Short> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                short next = 0;

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextShort();
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

        return resultHolder.value() == null ? OptionalShort.empty() : OptionalShort.of(resultHolder.value());
    }

    /**
     * Returns an {@link OptionalShort} describing the last element of this stream that matches
     * the given predicate. Because this stream is iterator-backed, all elements must be consumed
     * to determine the last match. Multiple threads process all elements concurrently, tracking
     * the highest encounter index of any matching element using a shared {@code MutableLong} counter.
     *
     * <p>Falls back to sequential execution if the thread count is insufficient to justify
     * parallel overhead.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an OptionalShort with the last matching element in encounter order, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalShort findLast(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Short>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Short> pair = new Pair<>();

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextShort());
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

        return resultHolder.value() == null ? OptionalShort.empty() : OptionalShort.of(resultHolder.value().right());
    }

    /**
     * Returns a stream formed by element-wise combining the elements of this stream and the
     * given stream {@code b} using the provided zip function. When the thread count is sufficient,
     * uses {@link Stream#parallelZip} for concurrent zipping; otherwise uses sequential
     * {@link ShortStream#zip}.
     *
     * <p>The resulting stream has the length of the shorter of the two input streams.
     *
     * @param b the second stream to zip with
     * @param zipFunction a function to combine pairs of elements
     * @return the new zipped stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream zipWith(final ShortStream b, final ShortBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorShortStream(ShortStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        return new ParallelIteratorShortStream(Stream.parallelZip(boxed(), b.boxed(), zipFunction::applyAsShort, maxThreadNum), false, maxThreadNum, splitor,
                asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a stream formed by element-wise combining the elements of this stream and the
     * given streams {@code b} and {@code c} using the provided zip function. When the thread
     * count is sufficient, uses {@link Stream#parallelZip} for concurrent zipping; otherwise
     * uses sequential {@link ShortStream#zip}.
     *
     * <p>The resulting stream has the length of the shortest of the three input streams.
     *
     * @param b the second stream to zip with
     * @param c the third stream to zip with
     * @param zipFunction a function to combine triples of elements
     * @return the new zipped stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream zipWith(final ShortStream b, final ShortStream c, final ShortTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorShortStream(ShortStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorShortStream(Stream.parallelZip(boxed(), b.boxed(), c.boxed(), zipFunction::applyAsShort, maxThreadNum), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a stream formed by element-wise combining the elements of this stream and the
     * given stream {@code b} using the provided zip function, padding the shorter stream with
     * the provided default values. When the thread count is sufficient, uses
     * {@link Stream#parallelZip} for concurrent zipping; otherwise uses sequential
     * {@link ShortStream#zip}.
     *
     * <p>The resulting stream has the length of the longer of the two input streams.
     *
     * @param b the second stream to zip with
     * @param valueForNoneA the default value used when this stream is exhausted
     * @param valueForNoneB the default value used when stream {@code b} is exhausted
     * @param zipFunction a function to combine pairs of elements
     * @return the new zipped stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream zipWith(final ShortStream b, final short valueForNoneA, final short valueForNoneB, final ShortBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorShortStream(ShortStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorShortStream(Stream.parallelZip(boxed(), b.boxed(), valueForNoneA, valueForNoneB, zipFunction::applyAsShort, maxThreadNum),
                false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a stream formed by element-wise combining the elements of this stream and the
     * given streams {@code b} and {@code c} using the provided zip function, padding shorter
     * streams with the provided default values. When the thread count is sufficient, uses
     * {@link Stream#parallelZip} for concurrent zipping; otherwise uses sequential
     * {@link ShortStream#zip}.
     *
     * <p>The resulting stream has the length of the longest of the three input streams.
     *
     * @param b the second stream to zip with
     * @param c the third stream to zip with
     * @param valueForNoneA the default value used when this stream is exhausted
     * @param valueForNoneB the default value used when stream {@code b} is exhausted
     * @param valueForNoneC the default value used when stream {@code c} is exhausted
     * @param zipFunction a function to combine triples of elements
     * @return the new zipped stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream zipWith(final ShortStream b, final ShortStream c, final short valueForNoneA, final short valueForNoneB, final short valueForNoneC,
            final ShortTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorShortStream(ShortStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorShortStream(
                Stream.parallelZip(boxed(), b.boxed(), c.boxed(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction::applyAsShort, maxThreadNum), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns {@code true} since this stream executes operations in parallel.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParallel() {
        return true;
    }

    /**
     * Returns an equivalent sequential {@link ShortStream} backed by the same underlying iterator.
     * The sequential stream is created lazily and cached for reuse on subsequent calls.
     *
     * @return a sequential ShortStream over the same elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ShortStream sequential() throws IllegalStateException {
        assertNotClosed();

        IteratorShortStream tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;

                if (tmp == null) {
                    tmp = new IteratorShortStream(elements, isSorted(), closeHandlers());
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
        //  assertNotClosed();

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
        //  assertNotClosed();

        return splitor;
    }

    /**
     * Returns the {@link AsyncExecutor} used to submit parallel tasks for this stream.
     *
     * @return the async executor for this parallel stream
     */
    @Override
    protected AsyncExecutor asyncExecutor() {
        //   assertNotClosed();

        return asyncExecutor;
    }
}
