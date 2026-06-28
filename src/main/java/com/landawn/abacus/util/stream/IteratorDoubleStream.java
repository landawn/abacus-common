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

import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleToFloatFunction;

/**
 * An iterator-based implementation of DoubleStream that processes double elements sequentially.
 * This class serves as the default sequential stream implementation for double values,
 * wrapping a DoubleIterator to provide stream operations.
 *
 * <p>This is an internal implementation class. Users should create streams through
 * the public DoubleStream factory methods rather than instantiating this class directly.
 *
 * @see DoubleStream
 * @see DoubleIteratorEx
 */
class IteratorDoubleStream extends AbstractDoubleStream {
    final DoubleIteratorEx elements;

    //    OptionalDouble head;
    //    DoubleStream tail;

    //    DoubleStream head2;
    //    OptionalDouble tail2;

    /**
     * Constructs an IteratorDoubleStream from a DoubleIterator.
     * Creates an unsorted stream with no close handlers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iterator = DoubleIterator.of(1.0, 2.5, 3.7, 4.2, 5.9);
     * IteratorDoubleStream stream = new IteratorDoubleStream(iterator);
     * stream.forEach(System.out::println);   // prints 1.0, 2.5, 3.7, 4.2, 5.9
     * }</pre>
     *
     * @param values the double iterator to wrap as a stream
     */
    IteratorDoubleStream(final DoubleIterator values) {
        this(values, null);
    }

    /**
     * Constructs an IteratorDoubleStream from a DoubleIterator with close handlers.
     * Creates an unsorted stream that will execute the provided close handlers when closed.
     * The close handlers are invoked in order when the stream's close() method is called,
     * ensuring proper resource cleanup.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleIterator iterator = DoubleIterator.of(1.0, 2.5, 3.7, 4.2, 5.9);
     * List<LocalRunnable> closeHandlers = new ArrayList<>();
     * closeHandlers.add(() -> System.out.println("Stream closed"));
     * IteratorDoubleStream stream = new IteratorDoubleStream(iterator, closeHandlers);
     * try {
     *     stream.forEach(System.out::println);
     * } finally {
     *     stream.close();   // invokes all close handlers
     * }
     * }</pre>
     *
     * @param values the double iterator to wrap as a stream
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorDoubleStream(final DoubleIterator values, final Collection<LocalRunnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    /**
     * Constructs an IteratorDoubleStream from a DoubleIterator with sorting and close handlers.
     * This is the primary constructor that all other constructors delegate to. The sorted flag
     * allows optimization of operations like max(), kthLargest(), and distinct() when elements are
     * known to be in natural ascending order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a sorted stream with close handlers
     * DoubleIterator sortedIterator = DoubleIterator.of(1.0, 2.5, 3.7, 4.2, 5.9);
     * List<LocalRunnable> closeHandlers = new ArrayList<>();
     * closeHandlers.add(() -> System.out.println("Cleanup complete"));
     *
     * IteratorDoubleStream stream = new IteratorDoubleStream(sortedIterator, true, closeHandlers);
     * try {
     *     OptionalDouble min = stream.min();                 // returns 1.0 (the minimum)
     *     System.out.println("Min: " + min.getAsDouble());   // prints 1.0
     * } finally {
     *     stream.close();
     * }
     * }</pre>
     *
     * @param values the double iterator to wrap as a stream
     * @param sorted {@code true} if the elements are already sorted in natural order, {@code false} otherwise
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorDoubleStream(final DoubleIterator values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        DoubleIteratorEx tmp = null;

        if (values instanceof DoubleIteratorEx) {
            tmp = (DoubleIteratorEx) values;
        } else {
            tmp = new DoubleIteratorEx() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public double nextDouble() {
                    return values.nextDouble();
                }
            };
        }

        elements = tmp;
    }

    /**
     * Returns a stream consisting of elements of this stream that match the given predicate.
     * Lazily iterates the underlying iterator, advancing it only as the returned stream is consumed.
     *
     * @param predicate the non-interfering, stateless predicate to apply to each element
     * @return a new {@code DoubleStream} containing only matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream filter(final DoublePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private double next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.nextDouble();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
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

                hasNext = false;

                return next;
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of elements from this stream while the given predicate holds.
     * Once an element fails the predicate, no further elements are included.
     * Lazily iterates the underlying iterator.
     *
     * @param predicate the non-interfering, stateless predicate to apply to each element
     * @return a new {@code DoubleStream} of elements from the start while {@code predicate} is satisfied
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream takeWhile(final DoublePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean hasMore = true;
            private boolean hasNext = false;
            private double next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext && hasMore && elements.hasNext()) {
                    next = elements.nextDouble();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public double nextDouble() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted());
    }

    /**
     * Returns a stream that skips elements from the start of this stream while the given predicate holds,
     * then includes all remaining elements. Lazily iterates the underlying iterator.
     *
     * @param predicate the non-interfering, stateless predicate to apply to each element
     * @return a new {@code DoubleStream} with leading elements dropped while {@code predicate} holds
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream dropWhile(final DoublePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private double next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextDouble();

                            if (!predicate.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.nextDouble();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public double nextDouble() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted());
    }

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of this stream.
     * Lazily iterates the underlying iterator.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code DoubleStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream map(final DoubleUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() throws IllegalArgumentException {
                return mapper.applyAsDouble(elements.nextDouble());
            }

        }, false);
    }

    /**
     * Returns an {@code IntStream} consisting of the results of applying the given function to the elements of this stream.
     * Lazily iterates the underlying iterator.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code IntStream} of mapped int values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final DoubleToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() throws IllegalArgumentException {
                return mapper.applyAsInt(elements.nextDouble());
            }

        }, false);
    }

    /**
     * Returns a {@code LongStream} consisting of the results of applying the given function to the elements of this stream.
     * Lazily iterates the underlying iterator.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code LongStream} of mapped long values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public LongStream mapToLong(final DoubleToLongFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public long nextLong() throws IllegalArgumentException {
                return mapper.applyAsLong(elements.nextDouble());
            }

        }, false);
    }

    /**
     * Returns a {@code FloatStream} consisting of the results of applying the given function to the elements of this stream.
     * Lazily iterates the underlying iterator.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code FloatStream} of mapped float values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream mapToFloat(final DoubleToFloatFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() throws IllegalArgumentException {
                return mapper.applyAsFloat(elements.nextDouble());
            }

        }, false);
    }

    /**
     * Returns a {@code Stream} consisting of the results of applying the given function to the elements of this stream.
     * Lazily iterates the underlying iterator.
     *
     * @param <T> the type of elements of the new stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code Stream<T>} of mapped object values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> mapToObj(final DoubleFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException {
                return mapper.apply(elements.nextDouble());
            }

        }, false, null);
    }

    /**
     * Returns a stream formed by replacing each element with the contents of a mapped {@code DoubleStream}.
     * Lazily iterates the underlying iterator; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code DoubleStream}
     * @return a new {@code DoubleStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream flatMap(final DoubleFunction<? extends DoubleStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = new DoubleIteratorEx() {
            private DoubleIterator cur = null;
            private DoubleStream s = null;
            private Deque<LocalRunnable> closeHandle = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            StreamBase.close(tmp);
                        }

                        s = mapper.apply(elements.nextDouble());

                        if (s == null) {
                            cur = null;
                        } else {
                            if (N.notEmpty(s.closeHandlers())) {
                                closeHandle = s.closeHandlers();
                            }

                            cur = s.iteratorEx();
                        }
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null;
            }

            @Override
            public double nextDouble() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextDouble();
            }

            @Override
            public void closeResource() throws IllegalStateException {
                if (closeHandle != null) {
                    StreamBase.close(closeHandle);
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    /**
     * Returns a stream formed by replacing each element with the contents of a mapped {@code Collection<Double>}.
     * Lazily iterates the underlying iterator.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a collection of doubles
     * @return a new {@code DoubleStream} formed by concatenating the mapped collections
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream flatmap(final DoubleFunction<? extends Collection<Double>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private Iterator<Double> cur = null;
            private Collection<Double> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextDouble());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public double nextDouble() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Double v = cur.next();
                return v == null ? 0d : v;
            }
        }, false);
    }

    /**
     * Returns a stream formed by replacing each element with the contents of a mapped {@code double[]} array.
     * Lazily iterates the underlying iterator.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code double[]}
     * @return a new {@code DoubleStream} formed by concatenating the mapped arrays
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream flatMapArray(final DoubleFunction<double[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArray(mapper);
        }

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private double[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.nextDouble());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public double nextDouble() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    /**
     * Returns an {@code IntStream} formed by replacing each element with the contents of a mapped {@code IntStream}.
     * Lazily iterates the underlying iterator; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to an {@code IntStream}
     * @return a new {@code IntStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final DoubleFunction<? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = new IntIteratorEx() {
            private IntIterator cur = null;
            private IntStream s = null;
            private Deque<LocalRunnable> closeHandle = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            StreamBase.close(tmp);
                        }

                        s = mapper.apply(elements.nextDouble());

                        if (s == null) {
                            cur = null;
                        } else {
                            if (N.notEmpty(s.closeHandlers())) {
                                closeHandle = s.closeHandlers();
                            }

                            cur = s.iteratorEx();
                        }
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null;
            }

            @Override
            public int nextInt() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextInt();
            }

            @Override
            public void closeResource() {
                if (closeHandle != null) {
                    StreamBase.close(closeHandle);
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    /**
     * Returns a {@code LongStream} formed by replacing each element with the contents of a mapped {@code LongStream}.
     * Lazily iterates the underlying iterator; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code LongStream}
     * @return a new {@code LongStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public LongStream flatMapToLong(final DoubleFunction<? extends LongStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = new LongIteratorEx() {
            private LongIterator cur = null;
            private LongStream s = null;
            private Deque<LocalRunnable> closeHandle = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            StreamBase.close(tmp);
                        }

                        s = mapper.apply(elements.nextDouble());

                        if (s == null) {
                            cur = null;
                        } else {
                            if (N.notEmpty(s.closeHandlers())) {
                                closeHandle = s.closeHandlers();
                            }

                            cur = s.iteratorEx();
                        }
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null;
            }

            @Override
            public long nextLong() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextLong();
            }

            @Override
            public void closeResource() {
                if (closeHandle != null) {
                    StreamBase.close(closeHandle);
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    /**
     * Returns a {@code FloatStream} formed by replacing each element with the contents of a mapped {@code FloatStream}.
     * Lazily iterates the underlying iterator; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code FloatStream}
     * @return a new {@code FloatStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream flatMapToFloat(final DoubleFunction<? extends FloatStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = new FloatIteratorEx() {
            private FloatIterator cur = null;
            private FloatStream s = null;
            private Deque<LocalRunnable> closeHandle = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            StreamBase.close(tmp);
                        }

                        s = mapper.apply(elements.nextDouble());

                        if (s == null) {
                            cur = null;
                        } else {
                            if (N.notEmpty(s.closeHandlers())) {
                                closeHandle = s.closeHandlers();
                            }

                            cur = s.iteratorEx();
                        }
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null;
            }

            @Override
            public float nextFloat() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextFloat();
            }

            @Override
            public void closeResource() {
                if (closeHandle != null) {
                    StreamBase.close(closeHandle);
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    /**
     * Returns a {@code Stream} formed by replacing each element with the contents of a mapped {@code Stream<T>}.
     * Lazily iterates the underlying iterator; each mapped stream is closed after its contents are consumed.
     *
     * @param <T> the type of elements in the resulting stream
     * @param mapper a non-interfering, stateless function mapping each element to a {@code Stream<T>}
     * @return a new {@code Stream<T>} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatMapToObj(final DoubleFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() {
            private Iterator<? extends T> cur = null;
            private Stream<? extends T> s = null;
            private Deque<LocalRunnable> closeHandle = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            StreamBase.close(tmp);
                        }

                        s = mapper.apply(elements.nextDouble());

                        if (s == null) {
                            cur = null;
                        } else {
                            if (N.notEmpty(s.closeHandlers())) {
                                closeHandle = s.closeHandlers();
                            }

                            cur = s.iteratorEx();
                        }
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null;
            }

            @Override
            public T next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            @Override
            public void closeResource() {
                if (closeHandle != null) {
                    StreamBase.close(closeHandle);
                }
            }
        };

        return newStream(iter, false, null, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    /**
     * Returns a {@code Stream} formed by replacing each element with the contents of a mapped {@code Collection<T>}.
     * Lazily iterates the underlying iterator.
     *
     * @param <T> the type of elements in the resulting stream
     * @param mapper a non-interfering, stateless function mapping each element to a collection of objects
     * @return a new {@code Stream<T>} formed by concatenating the mapped collections
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatmapToObj(final DoubleFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<? extends T> cur = null;
            private Collection<? extends T> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextDouble());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        }, false, null);
    }

    /**
     * Returns a stream consisting of distinct elements of this stream.
     * If the stream is sorted, adjacent-element comparison is used (efficient O(n));
     * otherwise a hash set is used to track seen elements.
     * Lazily iterates the underlying iterator.
     *
     * @return a new {@code DoubleStream} containing only distinct elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new DoubleIteratorEx() { //NOSONAR
                private boolean hasNext = false;
                private double prev = 0;
                private double next = 0;
                private boolean isFirst = true;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (elements.hasNext()) {
                            next = elements.nextDouble();

                            if (isFirst) {
                                isFirst = false;
                                hasNext = true;
                                break;
                            } else if (!N.equals(next, prev)) {
                                hasNext = true;
                                break;
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

                    hasNext = false;
                    prev = next;

                    return next;
                }
            }, isSorted());
        } else {
            final Set<Object> set = N.newHashSet();

            // noinspection resource
            return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
        }
    }

    /**
     * Returns a stream consisting of at most {@code maxSize} elements from the start of this stream.
     * Lazily iterates the underlying iterator and stops after {@code maxSize} elements are pulled.
     *
     * @param maxSize the maximum number of elements the stream should contain; must be &gt;= 0
     * @return a new {@code DoubleStream} containing at most {@code maxSize} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    @Override
    public DoubleStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public double nextDouble() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;
                return elements.nextDouble();
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                elements.advance(N.min(n, maxSize - cnt));

                cnt = n >= maxSize - cnt ? maxSize : cnt + n;
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream after discarding the first {@code n} elements.
     * The underlying iterator is advanced lazily when the returned stream is first consumed.
     *
     * @param n the number of leading elements to skip; must be &gt;= 0
     * @return a new {@code DoubleStream} with the first {@code n} elements skipped
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public DoubleStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        //    if (n == 0) {
        //        return newStream(elements, isSorted());
        //    }

        if (n == 0) {
            return this;
        }

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.hasNext();
            }

            @Override
            public double nextDouble() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.nextDouble();
            }

            @Override
            public long count() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.count();
            }

            @Override
            public void advance(final long n2) {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                elements.advance(n2);
            }

            @Override
            public double[] toArray() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.toArray();
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of the top {@code n} elements of this stream
     * according to natural ordering. The underlying iterator is fully consumed.
     *
     * @param n the number of top elements to include; must be &gt;= 0
     * @return a new {@code DoubleStream} containing the top {@code n} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public DoubleStream top(final int n) throws IllegalStateException {
        assertNotClosed();

        return top(n, DOUBLE_COMPARATOR);
    }

    /**
     * Returns a stream consisting of the top {@code n} elements of this stream according to the given comparator.
     * The underlying iterator is fully consumed to determine the top elements.
     *
     * @param n the number of top elements to include; must be &gt;= 0
     * @param comparator the comparator used to determine element ordering
     * @return a new {@code DoubleStream} containing the top {@code n} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public DoubleStream top(final int n, final Comparator<? super Double> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return newStream(DoubleIteratorEx.empty(), false);
        }

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private double[] aar;
            private int cursor = 0;
            private int to;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor < to;
            }

            @Override
            public double nextDouble() {
                if (!initialized) {
                    init();
                }

                if (cursor >= to) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return aar[cursor++];
            }

            @Override
            public long count() {
                if (!initialized) {
                    init();
                }

                final long ret = to - cursor;
                cursor = to; // consume all elements
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                final long remaining = to - cursor;
                cursor = n >= remaining ? to : (int) (cursor + Math.min(n, Integer.MAX_VALUE - cursor));
            }

            @Override
            public double[] toArray() {
                if (!initialized) {
                    init();
                }

                final double[] a = new double[to - cursor];
                N.copy(aar, cursor, a, 0, to - cursor);
                cursor = to; // consume all elements
                return a;
            }

            @Override
            public DoubleList toList() {
                return DoubleList.of(toArray());
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    if (isSorted() && isSameComparator(comparator, comparator())) {
                        final LinkedList<Double> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.nextDouble());
                        }

                        aar = Array.unbox(queue.toArray(N.EMPTY_DOUBLE_OBJ_ARRAY));
                    } else {
                        final Comparator<? super Double> cmp = comparator == null ? DOUBLE_COMPARATOR : comparator;
                        final Queue<Double> heap = new PriorityQueue<>(n, cmp);

                        Double next = null;
                        while (elements.hasNext()) {
                            next = elements.nextDouble();

                            if (heap.size() >= n) {
                                if (cmp.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = Array.unbox(heap.toArray(N.EMPTY_DOUBLE_OBJ_ARRAY));
                    }

                    to = aar.length;
                }
            }
        }, false);
    }

    /**
     * Returns a stream that performs the given action on each element as it is consumed,
     * primarily for debugging purposes. Lazily iterates the underlying iterator.
     *
     * @param action a non-interfering action to perform on each element as it is pulled
     * @return a new {@code DoubleStream} that applies the action to each element
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream onEach(final DoubleConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() {
                final double next = elements.nextDouble();
                action.accept(next);
                return next;
            }
        }, isSorted());
    }

    /**
     * Performs the given action on each element of this stream, in encounter order. Closes the stream.
     *
     * @param <E> the type of exception the action may throw
     * @param action a non-interfering action to perform on each element
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws
     */
    @Override
    public <E extends Exception> void forEach(final Throwables.DoubleConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.nextDouble());
            }
        } finally {
            close();
        }
    }

    /**
     * Returns all remaining elements of this stream as a {@code double[]}, optionally closing the stream.
     * This is an internal method used by stream infrastructure.
     *
     * @param closeStream if {@code true}, the stream is closed after the array is collected
     * @return a {@code double[]} containing all remaining elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected double[] toArray(final boolean closeStream) {
        assertNotClosed();

        try {
            return elements.toArray();
        } finally {
            if (closeStream) {
                close();
            }
        }
    }

    /**
     * Collects all elements of this stream into a {@code DoubleList} and closes the stream.
     *
     * @return a {@code DoubleList} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleList toDoubleList() throws IllegalStateException {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@code List<Double>} and closes the stream.
     *
     * @return a {@code List<Double>} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public List<Double> toList() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofList());
    }

    /**
     * Collects all elements of this stream into a {@code Set<Double>} and closes the stream.
     *
     * @return a {@code Set<Double>} containing the distinct elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Set<Double> toSet() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofSet());
    }

    /**
     * Collects all elements of this stream into a collection created by the given supplier, then closes the stream.
     *
     * @param <C> the type of the resulting collection
     * @param supplier a function that returns a new, empty collection to populate
     * @return the collection populated with all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <C extends Collection<Double>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextDouble());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@code Multiset<Double>} and closes the stream.
     * Elements with equal values will have their counts incremented accordingly.
     *
     * @return a {@code Multiset<Double>} representing element frequencies in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Double> toMultiset() throws IllegalStateException {
        assertNotClosed();

        return toMultiset(Suppliers.ofMultiset());
    }

    /**
     * Collects all elements of this stream into a {@code Multiset<Double>} created by the given supplier,
     * then closes the stream.
     *
     * @param supplier a function that returns a new, empty {@code Multiset} to populate
     * @return the {@code Multiset<Double>} populated with all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Double> toMultiset(final Supplier<? extends Multiset<Double>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Double> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextDouble());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements into a map using {@code keyMapper} and {@code valueMapper}, with collision resolution via
     * {@code mergeFunction}. Closes the stream.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param <M> the type of the resulting map
     * @param <E> the exception type that {@code keyMapper} may throw
     * @param <E2> the exception type that {@code valueMapper} may throw
     * @param keyMapper function to derive the map key from each element
     * @param valueMapper function to derive the map value from each element
     * @param mergeFunction function to resolve collisions between values with the same key
     * @param mapFactory supplier that creates a new, empty map of the desired type
     * @return a map containing the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     * @throws E if {@code keyMapper} throws
     * @throws E2 if {@code valueMapper} throws
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Throwables.DoubleFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            double next = 0;

            while (elements.hasNext()) {
                next = elements.nextDouble();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Groups elements by {@code keyMapper}, applies a downstream collector to each group, and closes the stream.
     *
     * @param <K> the type of map keys
     * @param <D> the type of downstream reduction result
     * @param <M> the type of the resulting map
     * @param <E> the exception type that {@code keyMapper} may throw
     * @param keyMapper function to classify each element into a group key
     * @param downstream collector for aggregating elements within each group
     * @param mapFactory supplier that creates a new, empty map of the desired type
     * @return a map from group key to downstream collection result
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code keyMapper} returns a {@code null} key
     * @throws E if {@code keyMapper} throws
     */
    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Collector<? super Double, ?, D> downstream, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super Double> downstreamAccumulator = (BiConsumer<Object, ? super Double>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final Map<K, Object> intermediate = (Map<K, Object>) result;
            K key = null;
            Object v = null;
            double next = 0;

            while (elements.hasNext()) {
                next = elements.nextDouble();
                key = checkArgNotNull(keyMapper.apply(next), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    v = downstreamSupplier.get();
                    intermediate.put(key, v);
                }

                downstreamAccumulator.accept(v, next);
            }

            final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

            Collectors.replaceAll(intermediate, function);

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a reduction on the elements of this stream using the provided identity value
     * and an associative accumulation function, and returns the reduced value.
     * Consumes the entire underlying iterator. Closes the stream.
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public double reduce(final double identity, final DoubleBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            double result = identity;

            while (elements.hasNext()) {
                result = accumulator.applyAsDouble(result, elements.nextDouble());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a reduction on the elements of this stream using an associative accumulation function,
     * and returns an {@code OptionalDouble} describing the reduced value, or empty if the stream is empty.
     * Consumes the entire underlying iterator. Closes the stream.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalDouble} containing the result, or empty if the stream has no elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble reduce(final DoubleBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            }

            double result = elements.nextDouble();

            while (elements.hasNext()) {
                result = accumulator.applyAsDouble(result, elements.nextDouble());
            }

            return OptionalDouble.of(result);
        } finally {
            close();
        }
    }

    /**
     * Performs a mutable reduction on the elements of this stream using the provided supplier,
     * accumulator, and combiner functions, and returns the resulting container.
     * Consumes the entire underlying iterator. Closes the stream.
     *
     * <pre>{@code
     * DoubleList list = stream.collect(DoubleList::new, DoubleList::add, DoubleList::addAll);
     * }</pre>
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new result container
     * @param accumulator a function that folds an element into the container
     * @param combiner a function that combines two partial containers (used in parallel; ignored here)
     * @return the populated result container
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjDoubleConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.nextDouble());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns the minimum element of this stream, or an empty {@code OptionalDouble} if the stream is empty.
     * All elements are scanned using {@code Math.min} so that a {@code NaN} element propagates to the
     * result; no sorted-stream shortcut is used because {@code NaN} sorts last. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the minimum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            }

            // No sorted-first-element shortcut for doubles: NaN sorts LAST, so the first element of
            // a sorted stream silently dropped the NaN that min() must propagate. The Math.min scan
            // below (NaN-propagating, consistent with java.util.stream.DoubleStream.min) is correct
            // for sorted input too.
            double candidate = elements.nextDouble();

            while (elements.hasNext()) {
                candidate = Math.min(candidate, elements.nextDouble());
            }

            return OptionalDouble.of(candidate);
        } finally {
            close();
        }
    }

    /**
     * Returns the maximum element of this stream, or an empty {@code OptionalDouble} if the stream is empty.
     * If the stream is sorted, the entire iterator is consumed to reach the last element;
     * otherwise all elements are scanned. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the maximum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            } else if (isSorted()) {
                double next = 0;

                while (elements.hasNext()) {
                    next = elements.nextDouble();
                }

                return OptionalDouble.of(next);
            }

            // Use Math.max so NaN propagates, consistent with ArrayDoubleStream.max and
            // java.util.DoubleSummaryStatistics / java.util.stream.DoubleStream.max.
            double candidate = elements.nextDouble();

            while (elements.hasNext()) {
                candidate = Math.max(candidate, elements.nextDouble());
            }

            return OptionalDouble.of(candidate);
        } finally {
            close();
        }
    }

    /**
     * Returns the k-th largest element of this stream (1-based), or an empty {@code OptionalDouble}
     * if the stream contains fewer than {@code k} elements.
     * The underlying iterator is fully consumed. Closes the stream.
     *
     * @param k the 1-based rank of the desired largest element (e.g. 1 = largest, 2 = second largest)
     * @return an {@code OptionalDouble} containing the k-th largest element, or empty if there are fewer than {@code k} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    @Override
    public OptionalDouble kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (isSorted()) {
                // For sorted ascending, kthLargest = (k-1)th from the end.
                // Use a ring buffer of size k while draining the iterator.
                double[] window = null;
                int idx = 0;
                int size = 0;
                while (elements.hasNext()) {
                    final double v = elements.nextDouble();
                    if (window == null) {
                        window = new double[k];
                    }
                    if (size < k) {
                        window[size++] = v;
                    } else {
                        window[idx] = v;
                        idx = (idx + 1) % k;
                    }
                }
                if (size < k) {
                    return OptionalDouble.empty();
                }
                return OptionalDouble.of(window[idx]);
            }

            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            }

            @SuppressWarnings("resource")
            final Optional<Double> optional = boxed().kthLargest(k, DOUBLE_COMPARATOR);

            return optional.isPresent() ? OptionalDouble.of(optional.get()) : OptionalDouble.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the number of elements in this stream.
     * The underlying iterator is fully consumed to count elements. Closes the stream.
     *
     * @return the element count
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public long count() throws IllegalStateException {
        assertNotClosed();

        try {
            return elements.count();
        } finally {
            close();
        }
    }

    /**
     * Returns a {@code DoubleSummaryStatistics} describing various aggregate values (count, sum, min, max, average)
     * of the elements in this stream. The underlying iterator is fully consumed. Closes the stream.
     *
     * @return a {@code DoubleSummaryStatistics} for this stream's elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final DoubleSummaryStatistics result = new DoubleSummaryStatistics();

            while (elements.hasNext()) {
                result.accept(elements.nextDouble());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns {@code true} if any element of this stream matches the provided predicate.
     * Short-circuits as soon as a match is found. Closes the stream.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to stream elements
     * @return {@code true} if any element matches, {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextDouble())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    /**
     * Returns {@code true} if all elements of this stream match the provided predicate,
     * or if the stream is empty. Short-circuits as soon as a non-matching element is found.
     * Closes the stream.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to stream elements
     * @return {@code true} if all elements match (or the stream is empty), {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (!predicate.test(elements.nextDouble())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    /**
     * Returns {@code true} if no elements of this stream match the provided predicate,
     * or if the stream is empty. Short-circuits as soon as a matching element is found.
     * Closes the stream.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to stream elements
     * @return {@code true} if no elements match (or the stream is empty), {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextDouble())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    /**
     * Returns the first element that matches the given predicate as an {@code OptionalDouble},
     * or empty if no element matches. Short-circuits as soon as a matching element is found.
     * Closes the stream.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to stream elements
     * @return an {@code OptionalDouble} containing the first matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> OptionalDouble findFirst(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                final double e = elements.nextDouble();

                if (predicate.test(e)) {
                    return OptionalDouble.of(e);
                }
            }
        } finally {
            close();
        }

        return OptionalDouble.empty();
    }

    /**
     * Returns the last element that matches the given predicate as an {@code OptionalDouble},
     * or empty if no element matches. The entire underlying iterator is consumed to find the last match.
     * Closes the stream.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to stream elements
     * @return an {@code OptionalDouble} containing the last matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> OptionalDouble findLast(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            }

            boolean hasResult = false;
            double e = 0;
            double result = 0;

            while (elements.hasNext()) {
                e = elements.nextDouble();

                if (predicate.test(e)) {
                    result = e;
                    hasResult = true;
                }
            }

            return hasResult ? OptionalDouble.of(result) : OptionalDouble.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns a {@code java.util.stream.DoubleStream} view of this stream's elements,
     * bridging to the JDK standard stream API. Close handlers are propagated.
     *
     * @return a JDK {@code java.util.stream.DoubleStream} backed by this stream's elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public java.util.stream.DoubleStream toJdkStream() throws IllegalStateException {
        assertNotClosed();

        final PrimitiveIterator.OfDouble spliterator = new PrimitiveIterator.OfDouble() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() {
                return elements.nextDouble();
            }
        };

        if (isEmptyCloseHandlers(closeHandlers())) {
            //noinspection MagicConstant
            return StreamSupport.doubleStream(Spliterators.spliteratorUnknownSize(spliterator, DEFAULT_CHARACTERISTICS_PRIMITIVE_JDK_STREAM), isParallel());
        } else {
            //noinspection MagicConstant
            return StreamSupport.doubleStream(Spliterators.spliteratorUnknownSize(spliterator, DEFAULT_CHARACTERISTICS_PRIMITIVE_JDK_STREAM), isParallel())
                    .onClose(() -> close(closeHandlers()));
        }
    }

    /**
     * Returns the underlying {@code DoubleIteratorEx} for this stream.
     * This is an internal method used by stream infrastructure.
     *
     * @return the underlying {@code DoubleIteratorEx}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    DoubleIteratorEx iteratorEx() {
        assertNotClosed();

        return elements;
    }

    /**
     * Returns a stream with the same elements as this stream if it is non-empty; otherwise the supplier is invoked
     * and a stream of the supplied stream's elements is returned.
     * The supplier is not invoked until the stream is consumed. Closes any produced stream on close.
     *
     * @param supplier a supplier of a substitute {@code DoubleStream} to use when this stream is empty
     * @return a stream of this stream's elements if non-empty, or of the elements of the stream produced by the supplier
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream appendIfEmpty(final Supplier<? extends DoubleStream> supplier) throws IllegalStateException {
        assertNotClosed();

        final Holder<DoubleStream> holder = new Holder<>();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private DoubleIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (iter == null) {
                    init();
                }

                return iter.nextDouble();
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (iter == null) {
                    init();
                }

                iter.advance(n);
            }

            @Override
            public long count() {
                if (iter == null) {
                    init();
                }

                return iter.count();
            }

            private void init() {
                if (iter == null) {
                    if (elements.hasNext()) {
                        iter = elements;
                    } else {
                        final DoubleStream s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }
        }, false).onClose(() -> close(holder));
    }

    /**
     * Executes the given action if this stream is empty, then returns a stream of the same elements.
     * The action is invoked lazily when the returned stream is first iterated.
     *
     * @param action a {@code Runnable} to execute if the stream is empty
     * @return a {@code DoubleStream} that invokes {@code action} once on iteration if empty, then proceeds normally
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private DoubleIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (iter == null) {
                    init();
                }

                return iter.nextDouble();
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (iter == null) {
                    init();
                }

                iter.advance(n);
            }

            @Override
            public long count() {
                if (iter == null) {
                    init();
                }

                return iter.count();
            }

            private void init() {
                if (iter == null) {
                    iter = elements;

                    if (!iter.hasNext()) {
                        action.run();
                    }
                }
            }
        }, isSorted());
    }

    /**
     * Returns a new {@link ParallelIteratorDoubleStream} that processes this stream's elements
     * in parallel using the specified configuration.
     *
     * @param maxThreadNum the maximum number of threads to use
     * @param splitor the strategy for splitting the work among threads
     * @param asyncExecutor the executor for asynchronous parallel tasks
     * @param cancelUncompletedThreads whether to cancel incomplete threads when the stream is closed
     * @return a parallel {@code DoubleStream} backed by the same iterator
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected DoubleStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelIteratorDoubleStream(elements, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns {@code true} if this stream has no more elements to iterate (i.e. the underlying iterator
     * reports {@code hasNext() == false}).
     *
     * @return {@code true} if the stream is empty, {@code false} otherwise
     */
    @Override
    protected boolean isEmpty() {
        return !elements.hasNext();
    }
}
