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
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharTernaryOperator;
import com.landawn.abacus.util.function.CharToIntFunction;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.function.ObjCharConsumer;
import com.landawn.abacus.util.function.ToCharFunction;

/**
 * A parallel implementation of CharStream backed by an array that enables concurrent processing
 * of char elements across multiple threads. This class extends ArrayCharStream and overrides
 * key operations to execute them in parallel using a configurable thread pool.
 *
 * <p>The parallel execution model distributes work across multiple threads using a splitor strategy:
 * <ul>
 * <li>{@code ARRAY} - Divides the array into fixed-size slices assigned to each thread</li>
 * <li>{@code ITERATOR} - Uses shared cursor with synchronized access for load balancing</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create a parallel char stream
 * CharStream stream = CharStream.of(largeCharArray).parallel();
 *
 * // Process elements in parallel
 * long count = stream.filter(c -> Character.isUpperCase(c)).count();
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Operations on this stream are thread-safe and properly synchronized
 * when accessing shared state during parallel execution.
 *
 * @see ArrayCharStream
 * @see CharStream#parallel()
 */
final class ParallelArrayCharStream extends ArrayCharStream {
    private final int maxThreadNum;
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private final boolean cancelUncompletedThreads;
    private volatile ArrayCharStream sequential;

    /**
     * Constructs a ParallelArrayCharStream with the specified configuration for parallel processing.
     * This constructor initializes all parameters for controlling parallel execution behavior.
     *
     * @param values the char array to stream
     * @param fromIndex the start index (inclusive) of the range to process
     * @param toIndex the end index (exclusive) of the range to process
     * @param sorted whether the array elements in the range are in sorted order
     * @param maxThreadNum the maximum number of threads to use for parallel operations (0 uses default)
     * @param splitor the strategy for dividing work among threads (null uses default)
     * @param asyncExecutor the executor for running parallel tasks (null uses default)
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ParallelArrayCharStream(final char[] values, final int fromIndex, final int toIndex, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads, final Collection<LocalRunnable> closeHandlers) {
        super(values, fromIndex, toIndex, sorted, closeHandlers);

        this.maxThreadNum = maxThreadNum == 0 ? DEFAULT_MAX_THREAD_NUM : maxThreadNum;
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExecutor == null ? DEFAULT_ASYNC_EXECUTOR : asyncExecutor;
        this.cancelUncompletedThreads = cancelUncompletedThreads;
    }

    /**
     * Returns a parallel stream consisting of elements that match the given predicate.
     * If the stream can be processed sequentially (e.g., few elements), delegates to the
     * sequential implementation; otherwise boxes elements and uses the parallel object stream.
     *
     * <p>The encounter order of elements in the resulting stream is not guaranteed when
     * executing in parallel.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return a new parallel {@code CharStream} of matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream filter(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.filter(predicate);
        }

        final Stream<Character> stream = boxed().filter(predicate::test);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream of elements that match the predicate until a worker observes
     * a non-matching element.
     * If the stream can be processed sequentially, delegates to the sequential implementation;
     * otherwise boxes elements and delegates to the parallel object stream's {@code takeWhile}.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix
     * semantics; later matching elements may be returned.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return a new parallel {@code CharStream} of the elements selected by the parallel {@code takeWhile} operation
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream takeWhile(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.takeWhile(predicate);
        }

        final Stream<Character> stream = boxed().takeWhile(predicate::test);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream after dropping matching elements until a worker observes a
     * non-matching element. If the stream can be processed sequentially,
     * delegates to the sequential implementation; otherwise boxes elements and delegates to
     * the parallel object stream's {@code dropWhile}.
     *
     * <p><b>&#9888;&#65039; Parallel streams:</b> this operation does not guarantee encounter-order prefix/suffix
     * semantics; later matching elements may be dropped.
     *
     * @param predicate a non-interfering, stateless predicate to apply to each element
     * @return a new parallel {@code CharStream} of the elements selected by the parallel {@code dropWhile} operation
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream dropWhile(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.dropWhile(predicate);
        }

        final Stream<Character> stream = boxed().dropWhile(predicate::test);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel stream consisting of results of applying the given mapper to each element.
     * If the stream can be processed sequentially, delegates to the sequential implementation;
     * otherwise boxes elements and maps using the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code CharStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream map(final CharUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.map(mapper);
        }

        @SuppressWarnings("resource")
        final CharStream stream = boxed().mapToChar(mapper::applyAsChar);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel {@code IntStream} consisting of results of applying the given mapper
     * to each element. If the stream can be processed sequentially, delegates to the sequential
     * implementation; otherwise boxes elements and maps using the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code IntStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final CharToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToInt(mapper);
        }

        @SuppressWarnings("resource")
        final IntStream stream = boxed().mapToInt(mapper::applyAsInt);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns a parallel object {@code Stream} consisting of results of applying the given mapper
     * to each element. If the stream can be processed sequentially, delegates to the sequential
     * implementation; otherwise boxes elements and maps using the parallel object stream.
     *
     * @param <T> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new parallel {@code Stream} of mapped objects
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> mapToObj(final CharFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.mapToObj(mapper);
        }

        //noinspection resource
        return boxed().map(mapper::apply);
    }

    /**
     * Returns a parallel stream consisting of the results of replacing each element with the
     * contents of the mapped stream. If the stream can be processed sequentially, delegates to
     * the sequential {@code flatMap} wrapped in a parallel iterator stream; otherwise boxes
     * elements and flat-maps using the parallel object stream.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code CharStream} for each element
     * @return a new parallel {@code CharStream} of the flattened mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatMap(final CharFunction<? extends CharStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorCharStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final CharStream stream = boxed().flatMapToChar(mapper::apply);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream consisting of the results of replacing each element with the
     * elements from the mapped collection. If the stream can be processed sequentially, delegates
     * to the sequential {@code flatmap} wrapped in a parallel iterator stream; otherwise boxes
     * elements, flat-maps collections, and unboxes back to chars.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code Collection<Character>} for each element
     * @return a new parallel {@code CharStream} of the flattened collection contents
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatmap(final CharFunction<? extends Collection<Character>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorCharStream(sequential().flatmap(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        @SuppressWarnings("resource")
        final CharStream stream = boxed().flatmap(mapper::apply).mapToChar(ToCharFunction.UNBOX);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream consisting of the results of replacing each element with the
     * contents of the mapped char array. If the stream can be processed sequentially, delegates
     * to the sequential {@code flatMapArray} wrapped in a parallel iterator stream; otherwise
     * boxes elements and flat-maps using {@code CharStream.of(array)}.
     *
     * @param mapper a non-interfering, stateless function that returns a {@code char[]} for each element
     * @return a new parallel {@code CharStream} of the flattened array contents
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatMapArray(final CharFunction<char[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            //noinspection resource
            return new ParallelIteratorCharStream(sequential().flatMapArray(mapper), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        @SuppressWarnings("resource")
        final CharStream stream = boxed().flatMapToChar(value -> CharStream.of(mapper.apply(value)));

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
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
    public IntStream flatMapToInt(final CharFunction<? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
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
    public <T> Stream<T> flatMapToObj(final CharFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
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
    public <T> Stream<T> flatmapToObj(final CharFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
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
     * the result is mapped back to chars. This method is primarily used for debugging.
     *
     * <p>If the stream can be processed sequentially, delegates to the sequential implementation.
     *
     * @param action a non-interfering action to perform on each element
     * @return a new parallel {@code CharStream} with the side-effecting action attached
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream onEach(final CharConsumer action) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.onEach(action);
        }

        @SuppressWarnings("resource")
        final CharStream stream = boxed().onEach(action::accept).sequential().mapToChar(ToCharFunction.UNBOX);

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Performs the given action on each element of this stream in parallel. The array is
     * divided into slices (when using {@code ARRAY} splitor) or elements are consumed via a
     * shared synchronized cursor (when using {@code ITERATOR} splitor), with each thread
     * processing its assigned elements concurrently.
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
    public <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            super.forEach(action);
            return;
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

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
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    char next = 0;

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
     * Collects elements of this stream into a {@code Map} in parallel by applying the given
     * key and value mapper functions. If the stream can be processed sequentially, delegates
     * to the sequential implementation; otherwise boxes elements and delegates to the parallel
     * object stream's {@code toMap}.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param <M> the type of the resulting map
     * @param <E> the type of exception thrown by the key mapper
     * @param <E2> the type of exception thrown by the value mapper
     * @param keyMapper a function to produce map keys from elements
     * @param valueMapper a function to produce map values from elements
     * @param mergeFunction a function to resolve collisions between values for the same key
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a {@code Map} whose keys and values are produced by applying the mapper functions
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     * @throws E2 if the value mapper throws an exception
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.CharFunction<? extends K, E> keyMapper,
            final Throwables.CharFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Throwables.Function<? super Character, ? extends K, E> keyMapper2 = keyMapper::apply;

        final Throwables.Function<? super Character, ? extends V, E2> valueMapper2 = valueMapper::apply;

        //noinspection resource
        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    /**
     * Groups elements of this stream into a {@code Map} in parallel, using the key mapper to
     * classify elements and the downstream collector to reduce grouped elements. If the stream
     * can be processed sequentially, delegates to the sequential implementation; otherwise
     * boxes elements and delegates to the parallel object stream's {@code groupTo}.
     *
     * @param <K> the type of map keys (group classifiers)
     * @param <D> the result type of the downstream reduction
     * @param <M> the type of the resulting map
     * @param <E> the type of exception thrown by the key mapper
     * @param keyMapper a classifier function mapping elements to keys
     * @param downstream a {@code Collector} implementing the downstream reduction for each group
     * @param mapFactory a supplier providing a new empty map into which results are inserted
     * @return a {@code Map} from keys to downstream reduction results
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws an exception
     */
    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.CharFunction<? extends K, E> keyMapper,
            final Collector<? super Character, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.groupTo(keyMapper, downstream, mapFactory);
        }

        final Throwables.Function<? super Character, ? extends K, E> keyMapper2 = keyMapper::apply;

        //noinspection resource
        return boxed().groupTo(keyMapper2, downstream, mapFactory);
    }

    /**
     * Performs a parallel reduction on elements of this stream using the provided identity value
     * and associative accumulation function. The array is partitioned across threads, each thread
     * computes a partial result, and the partial results are combined using the same accumulator.
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
    public char reduce(final char identity, final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.reduce(identity, accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Character>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    char result = identity;

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            result = accumulator.applyAsChar(result, elements[cursor++]);
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
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    char result = identity;
                    char next = 0;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            result = accumulator.applyAsChar(result, next);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        }

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        Character result = null;

        try {
            for (final ContinuableFuture<Character> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                if (result == null) {
                    result = future.get();
                } else {
                    result = accumulator.applyAsChar(result, future.get());
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
     * Performs a parallel reduction on elements of this stream using an associative accumulation
     * function, returning an {@code OptionalChar} with the result, or an empty optional if the
     * stream is empty. The array is partitioned across threads, each thread computes a partial
     * result, and partial results are combined using the same accumulator.
     *
     * <p>The accumulator function must be associative to produce correct results when run in
     * parallel across multiple threads.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalChar} with the reduction result, or empty if the stream has no elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar reduce(final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.reduce(accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Character>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    if (cursor >= to) {
                        return null;
                    }

                    char result = elements[cursor++];

                    try {
                        while (cursor < to && eHolder.value() == null) {
                            result = accumulator.applyAsChar(result, elements[cursor++]);
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
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    char result = 0;

                    synchronized (elements) {
                        if (cursor.value() < toIndex) {
                            result = elements[cursor.getAndIncrement()];
                        } else {
                            return null;
                        }
                    }

                    char next = 0;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    next = elements[cursor.getAndIncrement()];
                                } else {
                                    break;
                                }
                            }

                            result = accumulator.applyAsChar(result, next);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        setError(eHolder, e);
                    }

                    return result;
                });
            }
        }

        // checkRuntimeException(eHolder, asyncExecutor, asyncExecutorToUse);

        Character result = null;

        try {
            for (final ContinuableFuture<Character> future : futureList) {
                if (eHolder.value() != null) {
                    break;
                }

                final Character tmp = future.get();

                if (tmp == null) {
                    // continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result = accumulator.applyAsChar(result, tmp);
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

        return result == null ? OptionalChar.empty() : OptionalChar.of(result);
    }

    /**
     * Performs a mutable parallel reduction on elements of this stream. Each thread creates its
     * own result container using the supplier, accumulates elements into it, and then the
     * per-thread containers are combined using the combiner function.
     *
     * <p>The combiner must be compatible with the accumulator: combining two containers must
     * produce the same result as accumulating all elements into one container.
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container
     * @param accumulator a function that folds an element into a result container
     * @param combiner a function that combines two result containers into one
     * @return the result of the parallel mutable reduction
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjCharConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.collect(supplier, accumulator, combiner);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<R>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    final R container = supplier.get();

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
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final R container = supplier.get();
                    char next = 0;

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
     * Returns whether any element of this stream matches the provided predicate, evaluated in
     * parallel. As soon as one thread finds a matching element, the other threads stop processing.
     * Returns {@code false} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if any element matches the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.anyMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(false);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

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
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    char next = 0;

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
     * Returns whether all elements of this stream match the provided predicate, evaluated in
     * parallel. As soon as one thread finds a non-matching element, the other threads stop
     * processing. Returns {@code true} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if all elements match the predicate (or the stream is empty), {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.allMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

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
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    char next = 0;

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
     * Returns whether no elements of this stream match the provided predicate, evaluated in
     * parallel. As soon as one thread finds a matching element, the other threads stop processing.
     * Returns {@code true} if the stream is empty.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return {@code true} if no elements match the predicate (or the stream is empty), {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.noneMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

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
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    char next = 0;

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
     * Returns an {@code OptionalChar} with the first element (in encounter order) matching the
     * predicate, evaluated in parallel. Multiple threads search concurrently; the element with
     * the lowest array index among all matches found by any thread is returned.
     *
     * <p>Returns an empty {@code OptionalChar} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalChar} with the first matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalChar findFirst(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findFirst(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Character>> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    final Pair<Integer, Character> pair = new Pair<>();

                    try {
                        while (cursor < to && (resultHolder.value() == null || cursor < resultHolder.value().left()) && eHolder.value() == null) {
                            pair.setLeft(cursor);
                            pair.setRight(elements[cursor++]);

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
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final Pair<Integer, Character> pair = new Pair<>();

                    try {
                        while (resultHolder.value() == null && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() < toIndex) {
                                    pair.setLeft(cursor.value());
                                    pair.setRight(elements[cursor.getAndIncrement()]);
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
        }

        completeAndShutdownTempExecutor(futureList, eHolder, this, asyncExecutor, asyncExecutorToUse);

        return resultHolder.value() == null ? OptionalChar.empty() : OptionalChar.of(resultHolder.value().right());
    }

    /**
     * Returns an {@code OptionalChar} with any element matching the predicate, evaluated in
     * parallel. Unlike {@code findFirst}, this method returns whichever matching element is
     * found first by any thread, without regard for encounter order, potentially offering
     * better performance than {@code findFirst} in parallel execution.
     *
     * <p>Returns an empty {@code OptionalChar} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalChar} with any matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalChar findAny(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findAny(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Character> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    char next = 0;

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
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    char next = 0;

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

        return resultHolder.value() == null ? OptionalChar.empty() : OptionalChar.of(resultHolder.value());
    }

    /**
     * Returns an {@code OptionalChar} with the last element (in encounter order) matching the
     * predicate, evaluated in parallel. Multiple threads search the array from the end concurrently;
     * the element with the highest array index among all matches found by any thread is returned.
     *
     * <p>Returns an empty {@code OptionalChar} if no element matches.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements
     * @return an {@code OptionalChar} with the last matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws an exception
     */
    @Override
    public <E extends Exception> OptionalChar findLast(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return super.findLast(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Character>> resultHolder = new Holder<>();
        AsyncExecutor asyncExecutorToUse = checkAsyncExecutor(asyncExecutor, threadNum);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final int from = fromIndex + sliceIndex * sliceSize;
                    int cursor = toIndex - from > sliceSize ? from + sliceSize : toIndex;
                    final Pair<Integer, Character> pair = new Pair<>();

                    try {
                        while (cursor > from && (resultHolder.value() == null || cursor > resultHolder.value().left()) && eHolder.value() == null) {
                            pair.setLeft(cursor);
                            pair.setRight(elements[--cursor]);

                            if (predicate.test(pair.right())) {
                                synchronized (resultHolder) {
                                    if (resultHolder.value() == null || pair.left() > resultHolder.value().left()) {
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
        } else {
            final MutableInt cursor = MutableInt.of(toIndex);

            for (int i = 0; i < threadNum; i++) {
                asyncExecutorToUse = execute(asyncExecutorToUse, threadNum, i, futureList, () -> {
                    final Pair<Integer, Character> pair = new Pair<>();

                    try {
                        while (resultHolder.value() == null && eHolder.value() == null) {
                            synchronized (elements) {
                                if (cursor.value() > fromIndex) {
                                    pair.setLeft(cursor.value());
                                    pair.setRight(elements[cursor.decrementAndGet()]);
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

        return resultHolder.value() == null ? OptionalChar.empty() : OptionalChar.of(resultHolder.value().right());
    }

    /**
     * Returns a parallel stream formed by zipping this stream with stream {@code b}, applying
     * the zip function to corresponding elements. If the stream can be processed sequentially,
     * uses sequential zip; otherwise uses {@code Stream.parallelZip} for concurrent element pairing.
     *
     * <p>The resulting stream has length equal to the shorter of the two input streams.
     *
     * @param b the second stream to zip with
     * @param zipFunction a function applied to corresponding elements of both streams
     * @return a new parallel {@code CharStream} of zipped results
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream zipWith(final CharStream b, final CharBinaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return new ParallelIteratorCharStream(CharStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                    null);
        }

        return new ParallelIteratorCharStream(Stream.parallelZip(boxed(), b.boxed(), zipFunction::applyAsChar, maxThreadNum), false, maxThreadNum, splitor,
                asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream formed by zipping this stream with streams {@code b} and {@code c},
     * applying the ternary zip function to corresponding elements. If the stream can be processed
     * sequentially, uses sequential zip; otherwise uses {@code Stream.parallelZip} for concurrent
     * element pairing.
     *
     * <p>The resulting stream has length equal to the shortest of the three input streams.
     *
     * @param b the second stream to zip with
     * @param c the third stream to zip with
     * @param zipFunction a function applied to corresponding elements of all three streams
     * @return a new parallel {@code CharStream} of zipped results
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream zipWith(final CharStream b, final CharStream c, final CharTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return new ParallelIteratorCharStream(CharStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, asyncExecutor,
                    cancelUncompletedThreads, null);
        }

        return new ParallelIteratorCharStream(Stream.parallelZip(boxed(), b.boxed(), c.boxed(), zipFunction::applyAsChar, maxThreadNum), false, maxThreadNum,
                splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream formed by zipping this stream with stream {@code b} using padding
     * values for the shorter stream. The zip function is applied to corresponding elements, with
     * {@code valueForNoneA} used when this stream is exhausted and {@code valueForNoneB} used when
     * stream {@code b} is exhausted.
     *
     * <p>If the stream can be processed sequentially, uses sequential zip; otherwise uses
     * {@code Stream.parallelZip} for concurrent element pairing.
     *
     * @param b the second stream to zip with
     * @param valueForNoneA the padding value used when this stream has fewer elements than {@code b}
     * @param valueForNoneB the padding value used when {@code b} has fewer elements than this stream
     * @param zipFunction a function applied to corresponding (possibly padded) elements
     * @return a new parallel {@code CharStream} of zipped results, with length equal to the longer stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream zipWith(final CharStream b, final char valueForNoneA, final char valueForNoneB, final CharBinaryOperator zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return new ParallelIteratorCharStream(CharStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor,
                    asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorCharStream(Stream.parallelZip(boxed(), b.boxed(), valueForNoneA, valueForNoneB, zipFunction::applyAsChar, maxThreadNum),
                false, maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, null);
    }

    /**
     * Returns a parallel stream formed by zipping this stream with streams {@code b} and {@code c}
     * using padding values for shorter streams. The ternary zip function is applied to corresponding
     * elements, with padding values used when any stream is exhausted.
     *
     * <p>If the stream can be processed sequentially, uses sequential zip; otherwise uses
     * {@code Stream.parallelZip} for concurrent element pairing.
     *
     * @param b the second stream to zip with
     * @param c the third stream to zip with
     * @param valueForNoneA the padding value used when this stream is exhausted
     * @param valueForNoneB the padding value used when {@code b} is exhausted
     * @param valueForNoneC the padding value used when {@code c} is exhausted
     * @param zipFunction a function applied to corresponding (possibly padded) elements
     * @return a new parallel {@code CharStream} of zipped results, with length equal to the longest stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream zipWith(final CharStream b, final CharStream c, final char valueForNoneA, final char valueForNoneB, final char valueForNoneC,
            final CharTernaryOperator zipFunction) throws IllegalStateException {
        assertNotClosed();

        if (canBeSequential(maxThreadNum, fromIndex, toIndex)) {
            return new ParallelIteratorCharStream(CharStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                    splitor, asyncExecutor, cancelUncompletedThreads, null);
        }

        return new ParallelIteratorCharStream(
                Stream.parallelZip(boxed(), b.boxed(), c.boxed(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction::applyAsChar, maxThreadNum), false,
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
     * Returns a sequential {@code CharStream} view of this parallel stream backed by the same
     * underlying array. The sequential stream is lazily created and cached for reuse. The
     * returned stream shares the same close handlers as this stream.
     *
     * @return a sequential {@code CharStream} over the same elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream sequential() throws IllegalStateException {
        assertNotClosed();

        ArrayCharStream tmp = sequential;

        if (tmp == null) {
            synchronized (this) {
                tmp = sequential;
                if (tmp == null) {
                    tmp = new ArrayCharStream(elements, fromIndex, toIndex, isSorted(), closeHandlers());
                    sequential = tmp;
                }
            }
        }

        return tmp;
    }

    /**
     * Returns the maximum number of threads configured for parallel operations on this stream.
     *
     * @return the maximum thread count
     */
    @Override
    protected int maxThreadNum() {
        // assertNotClosed();

        return maxThreadNum;
    }

    /**
     * Returns the {@link BaseStream.Splitor} strategy used to divide work among threads.
     *
     * @return the splitor strategy
     */
    @Override
    protected BaseStream.Splitor splitor() {
        // assertNotClosed();

        return splitor;
    }

    /**
     * Returns the {@link AsyncExecutor} used to run parallel tasks for this stream.
     *
     * @return the async executor
     */
    @Override
    protected AsyncExecutor asyncExecutor() {
        // assertNotClosed();

        return asyncExecutor;
    }
}
