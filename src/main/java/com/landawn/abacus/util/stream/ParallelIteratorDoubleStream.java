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

import static com.landawn.abacus.util.function.ToDoubleFunction.UNBOX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleTernaryOperator;
import com.landawn.abacus.util.function.DoubleToFloatFunction;

/**
 * A parallel implementation of DoubleStream backed by an iterator that enables concurrent processing
 * of double elements across multiple threads. This class extends IteratorDoubleStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution distributes work using synchronized iterator access to ensure
 * thread-safe element consumption while maintaining high throughput for CPU-intensive operations.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel double stream from an iterator
 * DoubleIterator doubleIterator = DoubleIterator.of(1.0, -2.0, 3.0);
 * DoubleStream stream = DoubleStream.of(doubleIterator).parallel();
 *
 * // Process elements in parallel
 * double sum = stream.filter(d -> d > 0.0).sum();
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
 * @see IteratorDoubleStream
 * @see DoubleStream#parallel()
 */
final class ParallelIteratorDoubleStream extends IteratorDoubleStream {
    private final int maxThreadNum;
    private final SplitStrategy splitStrategy;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile IteratorDoubleStream sequential;

    /**
     * Constructs a ParallelIteratorDoubleStream from a DoubleIterator with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the DoubleIterator to stream from; it is consumed lazily and must not be advanced externally
     * @param sorted whether the iterator elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed, may be {@code null}
     */
    ParallelIteratorDoubleStream(final DoubleIterator values, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitStrategy = splitStrategy == null ? DEFAULT_SPLIT_STRATEGY : splitStrategy;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Constructs a ParallelIteratorDoubleStream from a DoubleStream with the specified configuration for parallel processing.
     * The source stream's iterator is adopted, and the source stream's own close handlers are merged
     * with the specified ones, so closing this stream also closes the source stream.
     *
     * @param stream the source {@code DoubleStream} to iterate over; a {@code null} stream is treated as empty
     * @param sorted whether the stream elements are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations ({@code 0} uses the default)
     * @param splitStrategy the strategy for dividing work among threads ({@code null} uses the default)
     * @param asyncExecutor the executor for running parallel tasks ({@code null} uses the default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers additional close handlers to execute when the stream is closed, may be {@code null}
     */
    ParallelIteratorDoubleStream(final DoubleStream stream, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(iterate(stream), sorted, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Constructs a ParallelIteratorDoubleStream from a boxed {@code Stream<Double>} with the specified configuration for
     * parallel processing. The source stream's iterator is adopted and unboxed to a {@code DoubleIterator}, and the
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
    ParallelIteratorDoubleStream(final Stream<Double> stream, final boolean sorted, final int maxThreadNum, final SplitStrategy splitStrategy,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Deque<LocalRunnable> closeHandlers) {
        this(doubleIterator(iterate(stream)), sorted, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                mergeCloseHandlers(closeHandlers, stream));
    }

    @Override
    public DoubleStream filter(final DoublePredicate predicate) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.filter(predicate);
        }

        final Stream<Double> stream = boxed().filter(predicate::test);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public DoubleStream takeWhile(final DoublePredicate predicate) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.takeWhile(predicate);
        }

        final Stream<Double> stream = boxed().takeWhile(predicate::test);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public DoubleStream dropWhile(final DoublePredicate predicate) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.dropWhile(predicate);
        }

        final Stream<Double> stream = boxed().dropWhile(predicate::test);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public DoubleStream map(final DoubleUnaryOperator mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.map(mapper);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().mapToDouble(mapper::applyAsDouble);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public IntStream mapToInt(final DoubleToIntFunction mapper) throws IllegalArgumentException, IllegalStateException {
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
    public LongStream mapToLong(final DoubleToLongFunction mapper) throws IllegalArgumentException, IllegalStateException {
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
    public FloatStream mapToFloat(final DoubleToFloatFunction mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToFloat(mapper);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().mapToFloat(mapper::applyAsFloat);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public <T> Stream<T> mapToObj(final DoubleFunction<? extends T> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            return super.mapToObj(mapper);
        }

        //noinspection resource
        return boxed().map(mapper::apply);
    }

    @Override
    public DoubleStream flatMap(final DoubleFunction<? extends DoubleStream> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatMap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().flatMapToDouble(mapper::apply);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public DoubleStream flatmap(final DoubleFunction<? extends Collection<Double>> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatmap(mapper), false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().flatmap(mapper::apply).mapToDouble(UNBOX);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public DoubleStream flatMapArray(final DoubleFunction<double[]> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorDoubleStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().flatMapToDouble(value -> DoubleStream.of(mapper.apply(value)));

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public IntStream flatMapToInt(final DoubleFunction<? extends IntStream> mapper) throws IllegalArgumentException, IllegalStateException {
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
    public LongStream flatMapToLong(final DoubleFunction<? extends LongStream> mapper) throws IllegalArgumentException, IllegalStateException {
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
    public FloatStream flatMapToFloat(final DoubleFunction<? extends FloatStream> mapper) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        if (canBeSequential(maxThreadNum)) {
            //noinspection resource
            return new ParallelIteratorFloatStream(sequential().flatMapToFloat(mapper), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final FloatStream stream = boxed().flatMapToFloat(mapper::apply);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final DoubleFunction<? extends Stream<? extends T>> mapper) throws IllegalArgumentException, IllegalStateException {
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
    public <T> Stream<T> flatmapToObj(final DoubleFunction<? extends Collection<? extends T>> mapper) throws IllegalArgumentException, IllegalStateException {
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
    public DoubleStream onEach(final DoubleConsumer action) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        if (canBeSequential(maxThreadNum)) {
            return super.onEach(action);
        }

        @SuppressWarnings("resource")
        final DoubleStream stream = boxed().onEach(action::accept).sequential().mapToDouble(UNBOX);

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.DoubleConsumer<E> action) throws IllegalArgumentException, IllegalStateException, E {
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
                double next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextDouble();
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Throwables.DoubleFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalArgumentException, IllegalStateException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Double, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Double, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Collector<? super Double, ?, D> downstream, final Supplier<? extends M> mapFactory)
            throws IllegalArgumentException, IllegalStateException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        if (canBeSequential(maxThreadNum)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Double, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    @Override
    public double reduce(final double identity, final DoubleBinaryOperator accumulator) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(identity, accumulator);
        }

        final List<ContinuableFuture<Double>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                double result = identity;
                double next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextDouble();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsDouble(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Double result = null;

            for (final Double partialResult : partialResults) {
                result = result == null ? partialResult : accumulator.applyAsDouble(result, partialResult);
            }

            return result == null ? identity : result;
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    @Override
    public OptionalDouble reduce(final DoubleBinaryOperator accumulator) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        if (canBeSequential(maxThreadNum)) {
            return super.reduce(accumulator);
        }

        final List<ContinuableFuture<Double>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                double result = 0;

                synchronized (elements) {
                    if (elements.hasNext()) {
                        result = elements.nextDouble();
                    } else {
                        return null;
                    }
                }

                double next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextDouble();
                            } else {
                                break;
                            }
                        }

                        result = accumulator.applyAsDouble(result, next);
                    }
                } catch (final Throwable e) { // NOSONAR
                    setError(eHolder, e);
                }

                return result;
            });
        }

        return completeAndFinishResults(futureList, eHolder, partialResults -> {
            Double result = null;

            for (final Double partialResult : partialResults) {
                if (partialResult != null) {
                    result = result == null ? partialResult : accumulator.applyAsDouble(result, partialResult);
                }
            }

            return result == null ? OptionalDouble.empty() : OptionalDouble.of(result);
        }, this, asyncExecutor, asyncExecutorToUse);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjDoubleConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
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
                double next = 0;

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextDouble();
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
    public <E extends Exception> boolean anyMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalArgumentException, IllegalStateException, E {
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
                double next = 0;

                try {
                    while (result.isFalse() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextDouble();
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
    public <E extends Exception> boolean allMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalArgumentException, IllegalStateException, E {
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
                double next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextDouble();
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
    public <E extends Exception> boolean noneMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalArgumentException, IllegalStateException, E {
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
                double next = 0;

                try {
                    while (result.isTrue() && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextDouble();
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
    public <E extends Exception> OptionalDouble findFirst(final Throwables.DoublePredicate<E> predicate)
            throws IllegalArgumentException, IllegalStateException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findFirst(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Double>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Double> pair = new Pair<>();

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextDouble());
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

        return resultHolder.value() == null ? OptionalDouble.empty() : OptionalDouble.of(resultHolder.value().right());
    }

    @Override
    public <E extends Exception> OptionalDouble findAny(final Throwables.DoublePredicate<E> predicate)
            throws IllegalArgumentException, IllegalStateException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findAny(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Double> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                double next = 0;

                try {
                    while (resultHolder.value() == null && eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                next = elements.nextDouble();
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

        return resultHolder.value() == null ? OptionalDouble.empty() : OptionalDouble.of(resultHolder.value());
    }

    @Override
    public <E extends Exception> OptionalDouble findLast(final Throwables.DoublePredicate<E> predicate)
            throws IllegalArgumentException, IllegalStateException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        if (canBeSequential(maxThreadNum)) {
            return super.findLast(predicate);
        }

        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(maxThreadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Long, Double>> resultHolder = new Holder<>();
        final MutableLong index = MutableLong.of(0);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, maxThreadNum);

        for (int i = 0; i < maxThreadNum; i++) {
            asyncExecutorToUse = execute(asyncExecutorToUse, maxThreadNum, i, futureList, () -> {
                final Pair<Long, Double> pair = new Pair<>();

                try {
                    while (eHolder.value() == null) {
                        synchronized (elements) {
                            if (elements.hasNext()) {
                                pair.setLeft(index.getAndIncrement());
                                pair.setRight(elements.nextDouble());
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

        return resultHolder.value() == null ? OptionalDouble.empty() : OptionalDouble.of(resultHolder.value().right());
    }

    @Override
    public DoubleStream zipWith(final DoubleStream b, final DoubleBinaryOperator zipFunction) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorDoubleStream(DoubleStream.zip(this, b, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorDoubleStream(Stream.parallelZip(boxed(), b.boxed(), zipFunction::applyAsDouble, maxThreadNum), false, maxThreadNum,
                splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public DoubleStream zipWith(final DoubleStream b, final DoubleStream c, final DoubleTernaryOperator zipFunction)
            throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorDoubleStream(DoubleStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitStrategy, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorDoubleStream(Stream.parallelZip(boxed(), b.boxed(), c.boxed(), zipFunction::applyAsDouble, maxThreadNum), false,
                maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public DoubleStream zipWith(final DoubleStream b, final double valueForNoneA, final double valueForNoneB, final DoubleBinaryOperator zipFunction)
            throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorDoubleStream(DoubleStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitStrategy,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorDoubleStream(Stream.parallelZip(boxed(), b.boxed(), valueForNoneA, valueForNoneB, zipFunction::applyAsDouble, maxThreadNum),
                false, maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public DoubleStream zipWith(final DoubleStream b, final DoubleStream c, final double valueForNoneA, final double valueForNoneB, final double valueForNoneC,
            final DoubleTernaryOperator zipFunction) throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        if (canBeSequential(maxThreadNum)) {
            return new ParallelIteratorDoubleStream(DoubleStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorDoubleStream(
                Stream.parallelZip(boxed(), b.boxed(), c.boxed(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction::applyAsDouble, maxThreadNum), false,
                maxThreadNum, splitStrategy, asyncExecutor, cancelUncompletedThreads, null);
    }

    @Override
    public boolean isParallel() {
        return true;
    }

    @Override
    public DoubleStream sequential() throws IllegalStateException {
        assertNotClosed();

        IteratorDoubleStream tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;

                if (tmp == null) {
                    tmp = new IteratorDoubleStream(elements, isSorted(), closeHandlersForNewStream());
                    sequential = tmp;
                }
            }
        }

        return tmp;
    }

    @Override
    protected int maxThreadNum() {
        //  assertNotClosed();

        return maxThreadNum;
    }

    @Override
    protected BaseStream.SplitStrategy splitStrategy() {
        // assertNotClosed();

        return splitStrategy;
    }

    @Override
    protected AsyncExecutor asyncExecutor() {
        //   assertNotClosed();

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
