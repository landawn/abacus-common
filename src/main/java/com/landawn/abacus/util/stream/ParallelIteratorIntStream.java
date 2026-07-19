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

import static com.landawn.abacus.util.function.ToIntFunction.UNBOX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
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
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToCharFunction;
import com.landawn.abacus.util.function.IntToFloatFunction;
import com.landawn.abacus.util.function.IntToShortFunction;

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
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorIntStream sequential;

    /**
     * Constructs a ParallelIteratorIntStream from an IntIterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the IntIterator to stream from
     * @param sorted whether the iterator elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorIntStream(final IntIterator values, final boolean sorted, final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor,
            final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorIntStream from an IntStream with the specified configuration for parallel processing.
     * The stream is converted to an iterator internally.
     *
     * @param stream the IntStream to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorIntStream(final IntStream stream, final boolean sorted, final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor,
            final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(iterate(stream), sorted, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Constructs a ParallelIteratorIntStream from a boxed Stream of Integers with the specified configuration for parallel processing.
     * The stream is unboxed to an IntIterator internally.
     *
     * @param stream the Stream of Integer objects to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorIntStream(final Stream<Integer> stream, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(intIterator(iterate(stream)), sorted, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
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
    public IntStream filter(final IntPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final Stream<Integer> stream = boxed().filter(predicate::test);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public IntStream takeWhile(final IntPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final Stream<Integer> stream = boxed().takeWhile(predicate::test);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public IntStream dropWhile(final IntPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final Stream<Integer> stream = boxed().dropWhile(predicate::test);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public IntStream map(final IntUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().mapToInt(mapper::applyAsInt);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public CharStream mapToChar(final IntToCharFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToChar(mapper);
        }

        @SuppressWarnings("resource")
        final CharStream stream = boxed().mapToChar(mapper::applyAsChar);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public ByteStream mapToByte(final IntToByteFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToByte(mapper);
        }

        @SuppressWarnings("resource")
        final ByteStream stream = boxed().mapToByte(mapper::applyAsByte);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public ShortStream mapToShort(final IntToShortFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToShort(mapper);
        }

        @SuppressWarnings("resource")
        final ShortStream stream = boxed().mapToShort(mapper::applyAsShort);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public LongStream mapToLong(final IntToLongFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToLong(mapper);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().mapToLong(mapper::applyAsLong);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public FloatStream mapToFloat(final IntToFloatFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToFloat(mapper);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().mapToFloat(mapper::applyAsFloat);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public DoubleStream mapToDouble(final IntToDoubleFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToDouble(mapper);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().mapToDouble(mapper::applyAsDouble);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public <T> Stream<T> mapToObj(final IntFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToObj(mapper);
        }

        //noinspection resource
        return boxed().map(mapper::apply);
    }

    @Override
    public IntStream flatMap(final IntFunction<? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().flatMapToInt(mapper::apply);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public IntStream flatmap(final IntFunction<? extends Collection<Integer>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatmap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().flatmap(mapper::apply).mapToInt(UNBOX);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public IntStream flatMapArray(final IntFunction<int[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().flatMapToInt(value -> IntStream.of(mapper.apply(value)));

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public CharStream flatMapToChar(final IntFunction<? extends CharStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorCharStream(sequential().flatMapToChar(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final CharStream stream = boxed().flatMapToChar(mapper::apply);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public ByteStream flatMapToByte(final IntFunction<? extends ByteStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorByteStream(sequential().flatMapToByte(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final ByteStream stream = boxed().flatMapToByte(mapper::apply);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public ShortStream flatMapToShort(final IntFunction<? extends ShortStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorShortStream(sequential().flatMapToShort(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final ShortStream stream = boxed().flatMapToShort(mapper::apply);

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public LongStream flatMapToLong(final IntFunction<? extends LongStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMapToLong(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().flatMapToLong(mapper::apply);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public FloatStream flatMapToFloat(final IntFunction<? extends FloatStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatMapToFloat(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().flatMapToFloat(mapper::apply);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public DoubleStream flatMapToDouble(final IntFunction<? extends DoubleStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatMapToDouble(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().flatMapToDouble(mapper::apply);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final IntFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        //noinspection resource
        return boxed().flatMap(mapper::apply);
    }

    @Override
    public <T> Stream<T> flatmapToObj(final IntFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatmapToObj(mapper), false, null, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        //noinspection resource
        return boxed().flatmap(mapper::apply);
    }

    @Override
    public IntStream onEach(final IntConsumer action) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.onEach(action);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().onEach(action::accept).sequential().mapToInt(UNBOX);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.IntConsumer<E> action) throws IllegalStateException, E {
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

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Throwables.IntFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Integer, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Integer, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.IntFunction<? extends K, E> keyMapper,
            final Collector<? super Integer, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Integer, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    @Override
    public int reduce(final int identity, final IntBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator);
        }

        final List<ContinuableFuture<Integer>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
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

    @Override
    public OptionalInt reduce(final IntBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<Integer>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
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

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjIntConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
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
    public <E extends Exception> boolean anyMatch(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> boolean allMatch(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> boolean noneMatch(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> OptionalInt findFirst(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Integer>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Integer> pair = new Pair<>();

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextInt());
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

        return resultHolder.value() == null ? OptionalInt.empty() : OptionalInt.of(resultHolder.value().right());
    }

    @Override
    public <E extends Exception> OptionalInt findAny(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Integer> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
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
    public <E extends Exception> OptionalInt findLast(final Throwables.IntPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Integer>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Integer> pair = new Pair<>();

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextInt());
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

        return resultHolder.value() == null ? OptionalInt.empty() : OptionalInt.of(resultHolder.value().right());
    }

    @Override
    public IntStream zipWith(final IntStream b, final IntBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorIntStream(IntStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        return new ParallelIteratorIntStream(Stream.parallelZip(boxed(), b.boxed(), zipFunction::applyAsInt, maxThreadNum), false, maxThreadNum, splitor,
                asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public IntStream zipWith(final IntStream b, final IntStream c, final IntTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorIntStream(IntStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        return new ParallelIteratorIntStream(Stream.parallelZip(boxed(), b.boxed(), c.boxed(), zipFunction::applyAsInt, maxThreadNum), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public IntStream zipWith(final IntStream b, final int valueForNoneA, final int valueForNoneB, final IntBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorIntStream(IntStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorIntStream(Stream.parallelZip(boxed(), b.boxed(), valueForNoneA, valueForNoneB, zipFunction::applyAsInt, maxThreadNum), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public IntStream zipWith(final IntStream b, final IntStream c, final int valueForNoneA, final int valueForNoneB, final int valueForNoneC,
            final IntTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorIntStream(IntStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorIntStream(
                Stream.parallelZip(boxed(), b.boxed(), c.boxed(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction::applyAsInt, maxThreadNum), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
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
                    tmp = new IteratorIntStream(elements, isSorted(), closeHandlers());
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
    protected BaseStream.Splitor splitor() {
        //   assertNotClosed();

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
