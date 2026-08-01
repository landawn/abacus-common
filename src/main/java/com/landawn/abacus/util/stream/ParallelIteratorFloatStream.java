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
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatTernaryOperator;
import com.landawn.abacus.util.function.FloatToDoubleFunction;
import com.landawn.abacus.util.function.FloatToIntFunction;
import com.landawn.abacus.util.function.FloatToLongFunction;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.function.ObjFloatConsumer;
import com.landawn.abacus.util.function.ToFloatFunction;

/**
 * A parallel implementation of FloatStream backed by an iterator that enables concurrent processing
 * of float elements across multiple threads. This class extends IteratorFloatStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution distributes work using synchronized iterator access to ensure
 * thread-safe element consumption while maintaining high throughput for CPU-intensive operations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel float stream from an iterator
 * FloatIterator floatIterator = FloatIterator.of(1.0f, -2.0f, 3.0f);
 * FloatStream stream = FloatStream.of(floatIterator).parallel();
 *
 * // Process elements in parallel
 * double sum = stream.filter(f -> f > 0.0f).sum();
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
 * @see IteratorFloatStream
 * @see FloatStream#parallel()
 */
final class ParallelIteratorFloatStream extends IteratorFloatStream {
    private final int maxThreadNum;
    private final SplitStrategy splitStrategy;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorFloatStream sequential;

    /**
     * Constructs a ParallelIteratorFloatStream from a FloatIterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the FloatIterator to stream from; it is consumed lazily and must not be advanced externally
     * @param sorted whether the iterator elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed, may be {@code null}
     */
    ParallelIteratorFloatStream(final FloatIterator values, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitStrategy = splitStrategy == null ? DEFAULT_SPLIT_STRATEGY : splitStrategy;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorFloatStream from a FloatStream with the specified configuration for parallel processing.
     * The source stream's iterator is adopted, and the source stream's own close handlers are merged
     * with the specified ones, so closing this stream also closes the source stream.
     *
     * @param stream the source {@code FloatStream} to iterate over; a {@code null} stream is treated as empty
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers additional close handlers to execute when the stream is closed, may be {@code null}
     */
    ParallelIteratorFloatStream(final FloatStream stream, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(iterate(stream), sorted, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Constructs a ParallelIteratorFloatStream from a boxed {@code Stream<Float>} with the specified configuration for
     * parallel processing. The source stream's iterator is adopted and unboxed to a {@code FloatIterator}, and the
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
     */
    ParallelIteratorFloatStream(final Stream<Float> stream, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(floatIterator(iterate(stream)), sorted, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                mergeCloseHandlers(closeHandlers, stream));
    }

    @Override
    public FloatStream filter(final FloatPredicate predicate) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final Stream<Float> stream = boxed().filter(predicate::test);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public FloatStream takeWhile(final FloatPredicate predicate) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final Stream<Float> stream = boxed().takeWhile(predicate::test);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public FloatStream dropWhile(final FloatPredicate predicate) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final Stream<Float> stream = boxed().dropWhile(predicate::test);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public FloatStream map(final FloatUnaryOperator mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().mapToFloat(mapper::applyAsFloat);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public IntStream mapToInt(final FloatToIntFunction mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToInt(mapper);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().mapToInt(mapper::applyAsInt);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public LongStream mapToLong(final FloatToLongFunction mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToLong(mapper);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().mapToLong(mapper::applyAsLong);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public DoubleStream mapToDouble(final FloatToDoubleFunction mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToDouble(mapper);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().mapToDouble(mapper::applyAsDouble);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public <T> Stream<T> mapToObj(final FloatFunction<? extends T> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToObj(mapper);
        }

        //noinspection resource
        return boxed().map(mapper::apply);
    }

    @Override
    public FloatStream flatMap(final FloatFunction<? extends FloatStream> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatMap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().flatMapToFloat(mapper::apply);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public FloatStream flatmap(final FloatFunction<? extends Collection<Float>> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatmap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().flatmap(mapper::apply).mapToFloat(ToFloatFunction.UNBOX);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public FloatStream flatMapArray(final FloatFunction<float[]> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().flatMapToFloat(value -> FloatStream.of(mapper.apply(value)));

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public IntStream flatMapToInt(final FloatFunction<? extends IntStream> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().flatMapToInt(mapper::apply);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public LongStream flatMapToLong(final FloatFunction<? extends LongStream> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorLongStream(sequential().flatMapToLong(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final LongStream stream = boxed().flatMapToLong(mapper::apply);

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public DoubleStream flatMapToDouble(final FloatFunction<? extends DoubleStream> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatMapToDouble(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().flatMapToDouble(mapper::apply);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final FloatFunction<? extends Stream<? extends T>> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        //noinspection resource
        return boxed().flatMap(mapper::apply);
    }

    @Override
    public <T> Stream<T> flatmapToObj(final FloatFunction<? extends Collection<? extends T>> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorStream<>(sequential().flatmapToObj(mapper), false, null, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        //noinspection resource
        return boxed().flatmap(mapper::apply);
    }

    @Override
    public FloatStream onEach(final FloatConsumer action) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        if (canBeSequential(maxThreadNum)) {
            return super.onEach(action);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().onEach(action::accept).sequential().mapToFloat(ToFloatFunction.UNBOX);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.FloatConsumer<E> action) throws IllegalArgumentException, IllegalStateException, E {
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
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                float next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextFloat();
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Throwables.FloatFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalArgumentException, IllegalStateException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Float, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Float, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Collector<? super Float, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalArgumentException, IllegalStateException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Float, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    @Override
    public float reduce(final float identity, final FloatBinaryOperator accumulator) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator);
        }

        final List<ContinuableFuture<Float>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                float result = identity;
                float next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextFloat();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsFloat(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Float result = null;

            for (final Float partialResult : partialResults) {
                result = result == null ? partialResult : accumulator.applyAsFloat(result, partialResult);
            }

            return result == null ? identity : result;
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    @Override
    public OptionalFloat reduce(final FloatBinaryOperator accumulator) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<Float>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                float result = 0;

                synchronized (elements) {
                    if (elements.hasNext()) {
                        result = elements.nextFloat();
                    } else {
                        return null;
                    }
                }

                float next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextFloat();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsFloat(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Float result = null;

            for (final Float partialResult : partialResults) {
                if (partialResult != null) {
                    result = result == null ? partialResult : accumulator.applyAsFloat(result, partialResult);
                }
            }

            return result == null ? OptionalFloat.empty() : OptionalFloat.of(result);
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjFloatConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalArgumentException, IllegalStateException {
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
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final R container = supplier.get();
                float next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextFloat();
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
    public <E extends Exception> boolean anyMatch(final Throwables.FloatPredicate<E> predicate) throws IllegalArgumentException, IllegalStateException, E {
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
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                float next = 0;

                try {
                    while (result.isFalse() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextFloat();
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
    public <E extends Exception> boolean allMatch(final Throwables.FloatPredicate<E> predicate) throws IllegalArgumentException, IllegalStateException, E {
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
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                float next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextFloat();
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
    public <E extends Exception> boolean noneMatch(final Throwables.FloatPredicate<E> predicate) throws IllegalArgumentException, IllegalStateException, E {
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
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                float next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextFloat();
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
    public <E extends Exception> OptionalFloat findFirst(final Throwables.FloatPredicate<E> predicate)
            throws IllegalArgumentException, IllegalStateException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Float>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Float> pair = new Pair<>();

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextFloat());
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

        return resultHolder.value() == null ? OptionalFloat.empty() : OptionalFloat.of(resultHolder.value().right());
    }

    @Override
    public <E extends Exception> OptionalFloat findAny(final Throwables.FloatPredicate<E> predicate) throws IllegalArgumentException, IllegalStateException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Float> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                float next = 0;

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextFloat();
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

        return resultHolder.value() == null ? OptionalFloat.empty() : OptionalFloat.of(resultHolder.value());
    }

    @Override
    public <E extends Exception> OptionalFloat findLast(final Throwables.FloatPredicate<E> predicate)
            throws IllegalArgumentException, IllegalStateException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Float>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Float> pair = new Pair<>();

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextFloat());
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

        return resultHolder.value() == null ? OptionalFloat.empty() : OptionalFloat.of(resultHolder.value().right());
    }

    @Override
    public FloatStream zipWith(final FloatStream b, final FloatBinaryOperator zipFunction) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorFloatStream(FloatStream.zip(this, b, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorFloatStream(Stream.parallelZip(boxed(), b.boxed(), zipFunction::applyAsFloat, maxThreadNum), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public FloatStream zipWith(final FloatStream b, final FloatStream c, final FloatTernaryOperator zipFunction)
            throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorFloatStream(FloatStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorFloatStream(Stream.parallelZip(boxed(), b.boxed(), c.boxed(), zipFunction::applyAsFloat, maxThreadNum), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public FloatStream zipWith(final FloatStream b, final float valueForNoneA, final float valueForNoneB, final FloatBinaryOperator zipFunction)
            throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorFloatStream(FloatStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitStrategy,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorFloatStream(Stream.parallelZip(boxed(), b.boxed(), valueForNoneA, valueForNoneB, zipFunction::applyAsFloat, maxThreadNum),
                false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public FloatStream zipWith(final FloatStream b, final FloatStream c, final float valueForNoneA, final float valueForNoneB, final float valueForNoneC,
            final FloatTernaryOperator zipFunction) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorFloatStream(FloatStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorFloatStream(
                Stream.parallelZip(boxed(), b.boxed(), c.boxed(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction::applyAsFloat, maxThreadNum), false,
                maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public boolean isParallel() {
        return true;
    }

    @Override
    public FloatStream sequential() throws IllegalStateException {
        assertNotClosed();

        IteratorFloatStream tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;

                if (tmp == null) {
                    tmp = new IteratorFloatStream(elements, isSorted(), closeHandlersForNewStream());
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
    protected BaseStream.SplitStrategy splitStrategy() {
        //  assertNotClosed();

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
