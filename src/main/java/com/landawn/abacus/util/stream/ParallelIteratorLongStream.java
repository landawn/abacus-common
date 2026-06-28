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

import static com.landawn.abacus.util.function.ToLongFunction.UNBOX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongTernaryOperator;
import com.landawn.abacus.util.function.LongToFloatFunction;

/**
 * A parallel implementation of LongStream backed by an iterator that enables concurrent processing
 * of long elements across multiple threads. This class extends IteratorLongStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution distributes work using synchronized iterator access to ensure
 * thread-safe element consumption while maintaining high throughput for CPU-intensive operations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel long stream from an iterator
 * LongStream stream = LongStream.of(longIterator).parallel();
 *
 * // Process elements in parallel
 * long sum = stream.filter(l -> l > 0).sum();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Operations on this stream are thread-safe and properly synchronized
 * when accessing the underlying iterator during parallel execution.
 *
 * @see IteratorLongStream
 * @see LongStream#parallel()
 */
final class ParallelIteratorLongStream extends IteratorLongStream {
    private final int maxThreadNum;
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorLongStream sequential;

    /**
     * Constructs a ParallelIteratorLongStream from a LongIterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the LongIterator to stream from
     * @param sorted whether the iterator elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorLongStream(final LongIterator values, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorLongStream from a LongStream with the specified configuration for parallel processing.
     * The stream is converted to an iterator internally.
     *
     * @param stream the LongStream to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorLongStream(final LongStream stream, final boolean sorted, final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor,
            final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(iterate(stream), sorted, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Constructs a ParallelIteratorLongStream from a boxed Stream of Longs with the specified configuration for parallel processing.
     * The stream is unboxed to a LongIterator internally.
     *
     * @param stream the Stream of Long objects to convert and stream from
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelIteratorLongStream(final Stream<Long> stream, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(longIterator(iterate(stream)), sorted, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    @Override
    public LongStream filter(final LongPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final Stream<Long> stream = boxed().filter(predicate::test);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public LongStream takeWhile(final LongPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final Stream<Long> stream = boxed().takeWhile(predicate::test);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public LongStream dropWhile(final LongPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final Stream<Long> stream = boxed().dropWhile(predicate::test);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public LongStream map(final LongUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().mapToLong(mapper::applyAsLong);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public IntStream mapToInt(final LongToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToInt(mapper);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().mapToInt(mapper::applyAsInt);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public FloatStream mapToFloat(final LongToFloatFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToFloat(mapper);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().mapToFloat(mapper::applyAsFloat);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public DoubleStream mapToDouble(final LongToDoubleFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToDouble(mapper);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().mapToDouble(mapper::applyAsDouble);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public <T> Stream<T> mapToObj(final LongFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.mapToObj(mapper);
        }

        //noinspection resource
        return boxed().map(mapper::apply);
    }

    @Override
    public LongStream flatMap(final LongFunction<? extends LongStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().flatMapToLong(mapper::apply);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public LongStream flatmap(final LongFunction<? extends Collection<Long>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatmap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().flatmap(mapper::apply).mapToLong(UNBOX);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public LongStream flatMapArray(final LongFunction<long[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().flatMapToLong(value -> LongStream.of(mapper.apply(value)));

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public IntStream flatMapToInt(final LongFunction<? extends IntStream> mapper) throws IllegalStateException {
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

    @Override
    public FloatStream flatMapToFloat(final LongFunction<? extends FloatStream> mapper) throws IllegalStateException {
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
    public DoubleStream flatMapToDouble(final LongFunction<? extends DoubleStream> mapper) throws IllegalStateException {
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
    public <T> Stream<T> flatMapToObj(final LongFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
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
    public <T> Stream<T> flatmapToObj(final LongFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
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
    public LongStream onEach(final LongConsumer action) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.onEach(action);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().onEach(action::accept).sequential().mapToLong(UNBOX);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.LongConsumer<E> action) throws IllegalStateException, E {
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
                long next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextLong();
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Throwables.LongFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Long, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Long, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Collector<? super Long, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Long, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    @Override
    public long reduce(final long identity, final LongBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator);
        }

        final List<ContinuableFuture<Long>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                long result = identity;
                long next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextLong();
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

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        Long result = null;

        try {
            for (final ContinuableFuture<Long> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (result == null) {
                    result = future.get();
                } else {
                    result = accumulator.applyAsLong(result, future.get());
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

    @Override
    public OptionalLong reduce(final LongBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<Long>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                long result = 0;

                synchronized (elements) {
                    if (elements.hasNext()) {
                        result = elements.nextLong();
                    } else {
                        return null;
                    }
                }

                long next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextLong();
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

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        Long result = null;

        try {
            for (final ContinuableFuture<Long> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                final Long tmp = future.get();

                if (tmp == null) {
                    // continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result = accumulator.applyAsLong(result, tmp);
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

        return result == null ? OptionalLong.empty() : OptionalLong.of(result);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjLongConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
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
                long next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextLong();
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
    public <E extends Exception> boolean anyMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
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
                long next = 0;

                try {
                    while (result.isFalse() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextLong();
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
    public <E extends Exception> boolean allMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
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
                long next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextLong();
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
    public <E extends Exception> boolean noneMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
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
                long next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextLong();
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
    public <E extends Exception> OptionalLong findFirst(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Long>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Long> pair = new Pair<>();

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextLong());
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

        return resultHolder.value() == null ? OptionalLong.empty() : OptionalLong.of(resultHolder.value().right());
    }

    @Override
    public <E extends Exception> OptionalLong findAny(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Long> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                long next = 0;

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextLong();
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

        return resultHolder.value() == null ? OptionalLong.empty() : OptionalLong.of(resultHolder.value());
    }

    @Override
    public <E extends Exception> OptionalLong findLast(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Long>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Long> pair = new Pair<>();

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextLong());
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

        return resultHolder.value() == null ? OptionalLong.empty() : OptionalLong.of(resultHolder.value().right());
    }

    @Override
    public LongStream zipWith(final LongStream b, final LongBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorLongStream(LongStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        return new ParallelIteratorLongStream(Stream.parallelZip(boxed(), b.boxed(), zipFunction::applyAsLong, maxThreadNum), false, maxThreadNum, splitor,
                asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public LongStream zipWith(final LongStream b, final LongStream c, final LongTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorLongStream(LongStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorLongStream(Stream.parallelZip(boxed(), b.boxed(), c.boxed(), zipFunction::applyAsLong, maxThreadNum), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public LongStream zipWith(final LongStream b, final long valueForNoneA, final long valueForNoneB, final LongBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorLongStream(LongStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorLongStream(Stream.parallelZip(boxed(), b.boxed(), valueForNoneA, valueForNoneB, zipFunction::applyAsLong, maxThreadNum),
                false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public LongStream zipWith(final LongStream b, final LongStream c, final long valueForNoneA, final long valueForNoneB, final long valueForNoneC,
            final LongTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorLongStream(LongStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorLongStream(
                Stream.parallelZip(boxed(), b.boxed(), c.boxed(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction::applyAsLong, maxThreadNum), false,
                maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public boolean isParallel() {
        return true;
    }

    @Override
    public LongStream sequential() throws IllegalStateException {
        assertNotClosed();

        IteratorLongStream tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;

                if (tmp == null) {
                    tmp = new IteratorLongStream(elements, isSorted(), closeHandlers());
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
        // assertNotClosed();

        return splitor;
    }

    @Override
    protected AsyncExecutor asyncExecutor() {
        //  assertNotClosed();

        return asyncExecutor;
    }
}
