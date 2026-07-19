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

import java.math.BigInteger;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
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
import java.util.function.Function;
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
import java.util.stream.StreamSupport;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.LongToFloatFunction;

/**
 * An iterator-based implementation of LongStream that processes long elements sequentially.
 * This class serves as the default sequential stream implementation for long values,
 * wrapping a LongIterator to provide stream operations.
 *
 * <p>This is an internal implementation class. Users should create streams through
 * the public LongStream factory methods rather than instantiating this class directly.
 *
 * @see LongStream
 * @see LongIteratorEx
 */
class IteratorLongStream extends AbstractLongStream {
    final LongIteratorEx elements;

    //    OptionalLong head;
    //    LongStream tail;

    //    LongStream head2;
    //    OptionalLong tail2;

    /**
     * Constructs an IteratorLongStream from a LongIterator.
     * Creates an unsorted stream with no close handlers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iterator = LongIterator.of(1L, 2L, 3L, 4L, 5L);
     * IteratorLongStream stream = new IteratorLongStream(iterator);
     * stream.forEach(System.out::println);   // prints 1, 2, 3, 4, 5
     * }</pre>
     *
     * @param values the long iterator to wrap as a stream
     */
    IteratorLongStream(final LongIterator values) {
        this(values, null);
    }

    /**
     * Constructs an IteratorLongStream from a LongIterator with close handlers.
     * Creates an unsorted stream that will execute the provided close handlers when closed.
     * The close handlers are invoked in order when the stream's close() method is called,
     * ensuring proper resource cleanup.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongIterator iterator = LongIterator.of(1L, 2L, 3L, 4L, 5L);
     * List<LocalRunnable> closeHandlers = new ArrayList<>();
     * closeHandlers.add(() -> System.out.println("Stream closed"));
     * IteratorLongStream stream = new IteratorLongStream(iterator, closeHandlers);
     * try {
     *     stream.forEach(System.out::println);
     * } finally {
     *     stream.close();   // invokes all close handlers
     * }
     * }</pre>
     *
     * @param values the long iterator to wrap as a stream
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorLongStream(final LongIterator values, final Collection<LocalRunnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    /**
     * Constructs an IteratorLongStream from a LongIterator with sorting and close handlers.
     * This is the primary constructor that all other constructors delegate to. The sorted flag
     * allows optimization of operations like min(), max(), and distinct() when elements are
     * known to be in natural ascending order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a sorted stream with close handlers
     * LongIterator sortedIterator = LongIterator.of(1L, 2L, 3L, 4L, 5L);
     * List<LocalRunnable> closeHandlers = new ArrayList<>();
     * closeHandlers.add(() -> System.out.println("Cleanup complete"));
     *
     * IteratorLongStream stream = new IteratorLongStream(sortedIterator, true, closeHandlers);
     * try {
     *     OptionalLong min = stream.min();                 // returns the first element (optimized for sorted input)
     *     System.out.println("Min: " + min.getAsLong());   // prints 1
     * } finally {
     *     stream.close();
     * }
     * }</pre>
     *
     * @param values the long iterator to wrap as a stream
     * @param sorted {@code true} if the elements are already sorted in natural order, {@code false} otherwise
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorLongStream(final LongIterator values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        LongIteratorEx tmp = null;

        if (values instanceof LongIteratorEx) {
            tmp = (LongIteratorEx) values;
        } else {
            tmp = new LongIteratorEx() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public long nextLong() {
                    return values.nextLong();
                }
            };
        }

        elements = tmp;
    }

    @Override
    public LongStream filter(final LongPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.nextLong();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
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

                hasNext = false;

                return next;
            }
        }, isSorted());
    }

    @Override
    public LongStream takeWhile(final LongPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean hasMore = true;
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext && hasMore && elements.hasNext()) {
                    next = elements.nextLong();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public long nextLong() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted());
    }

    @Override
    public LongStream dropWhile(final LongPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private long next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextLong();

                            if (!predicate.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.nextLong();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public long nextLong() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted());
    }

    @Override
    public LongStream map(final LongUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public long nextLong() throws IllegalArgumentException {
                return mapper.applyAsLong(elements.nextLong());
            }

        }, false);
    }

    @Override
    public IntStream mapToInt(final LongToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() throws IllegalArgumentException {
                return mapper.applyAsInt(elements.nextLong());
            }

        }, false);
    }

    @Override
    public FloatStream mapToFloat(final LongToFloatFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() throws IllegalArgumentException {
                return mapper.applyAsFloat(elements.nextLong());
            }

        }, false);
    }

    @Override
    public DoubleStream mapToDouble(final LongToDoubleFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() throws IllegalArgumentException {
                return mapper.applyAsDouble(elements.nextLong());
            }

        }, false);
    }

    @Override
    public <T> Stream<T> mapToObj(final LongFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException {
                return mapper.apply(elements.nextLong());
            }

        }, false, null);
    }

    @Override
    public LongStream flatMap(final LongFunction<? extends LongStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final LongIteratorEx iter = new LongIteratorEx() {
            private LongIterator cur = null;
            private LongStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextLong());

                        if (s == null) {
                            cur = null;
                        } else {
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
            public void closeResource() throws IllegalStateException {
                closeMappedStream();
            }

            private void closeMappedStream() {
                if (s != null) {
                    final Runnable closeAction = s::close;
                    s = null;
                    cur = null;
                    closeAction.run();
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    @Override
    public LongStream flatmap(final LongFunction<? extends Collection<Long>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private Iterator<Long> cur = null;
            private Collection<Long> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextLong());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public long nextLong() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Long v = cur.next();
                return v == null ? 0L : v;
            }
        }, false);
    }

    @Override
    public LongStream flatMapArray(final LongFunction<long[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArray(mapper);
        }

        return newStream(new LongIteratorEx() { //NOSONAR
            private long[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.nextLong());
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
            public long nextLong() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    @Override
    public IntStream flatMapToInt(final LongFunction<? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = new IntIteratorEx() {
            private IntIterator cur = null;
            private IntStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextLong());

                        if (s == null) {
                            cur = null;
                        } else {
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
                closeMappedStream();
            }

            private void closeMappedStream() {
                if (s != null) {
                    final Runnable closeAction = s::close;
                    s = null;
                    cur = null;
                    closeAction.run();
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    @Override
    public FloatStream flatMapToFloat(final LongFunction<? extends FloatStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final FloatIteratorEx iter = new FloatIteratorEx() {
            private FloatIterator cur = null;
            private FloatStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextLong());

                        if (s == null) {
                            cur = null;
                        } else {
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
                closeMappedStream();
            }

            private void closeMappedStream() {
                if (s != null) {
                    final Runnable closeAction = s::close;
                    s = null;
                    cur = null;
                    closeAction.run();
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    @Override
    public DoubleStream flatMapToDouble(final LongFunction<? extends DoubleStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final DoubleIteratorEx iter = new DoubleIteratorEx() {
            private DoubleIterator cur = null;
            private DoubleStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextLong());

                        if (s == null) {
                            cur = null;
                        } else {
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
            public void closeResource() {
                closeMappedStream();
            }

            private void closeMappedStream() {
                if (s != null) {
                    final Runnable closeAction = s::close;
                    s = null;
                    cur = null;
                    closeAction.run();
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    @Override
    public <T> Stream<T> flatMapToObj(final LongFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() {
            private Iterator<? extends T> cur = null;
            private Stream<? extends T> s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextLong());

                        if (s == null) {
                            cur = null;
                        } else {
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
                closeMappedStream();
            }

            private void closeMappedStream() {
                if (s != null) {
                    final Runnable closeAction = s::close;
                    s = null;
                    cur = null;
                    closeAction.run();
                }
            }
        };

        return newStream(iter, false, null, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    @Override
    public <T> Stream<T> flatmapToObj(final LongFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<? extends T> cur = null;
            private Collection<? extends T> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextLong());
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

    @Override
    public LongStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new LongIteratorEx() { //NOSONAR
                private boolean hasNext = false;
                private long prev = 0;
                private long next = 0;
                private boolean isFirst = true;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (elements.hasNext()) {
                            next = elements.nextLong();

                            if (isFirst) {
                                isFirst = false;
                                hasNext = true;
                                break;
                            } else if (next != prev) {
                                hasNext = true;
                                break;
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

    @Override
    public LongStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        return newStream(new LongIteratorEx() { //NOSONAR
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public long nextLong() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;
                return elements.nextLong();
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

    @Override
    public LongStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        //    if (n == 0) {
        //        return newStream(elements, isSorted());
        //    }

        if (n == 0) {
            return this;
        }

        return newStream(new LongIteratorEx() { //NOSONAR
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
            public long nextLong() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.nextLong();
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
                if (n2 <= 0) {
                    return;
                }

                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                elements.advance(n2);
            }

            @Override
            public long[] toArray() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.toArray();
            }
        }, isSorted());
    }

    @Override
    public LongStream top(final int n) throws IllegalStateException {
        assertNotClosed();

        return top(n, LONG_COMPARATOR);
    }

    @Override
    public LongStream top(final int n, final Comparator<? super Long> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return newStream(LongIteratorEx.empty(), false);
        }

        return newStream(new LongIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private long[] aar;
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
            public long nextLong() {
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
            public long[] toArray() {
                if (!initialized) {
                    init();
                }

                final long[] a = new long[to - cursor];
                N.copy(aar, cursor, a, 0, to - cursor);
                cursor = to; // consume all elements
                return a;
            }

            @Override
            public LongList toList() {
                return LongList.of(toArray());
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    if (isSorted() && isSameComparator(comparator, comparator())) {
                        final LinkedList<Long> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.nextLong());
                        }

                        aar = Array.unbox(queue.toArray(N.EMPTY_LONG_OBJ_ARRAY));
                    } else {
                        final Comparator<? super Long> cmp = comparator == null ? LONG_COMPARATOR : comparator;
                        final Queue<Long> heap = new PriorityQueue<>(Math.min(n, 16), cmp);

                        Long next = null;
                        while (elements.hasNext()) {
                            next = elements.nextLong();

                            if (heap.size() >= n) {
                                if (cmp.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = Array.unbox(heap.toArray(N.EMPTY_LONG_OBJ_ARRAY));
                    }

                    to = aar.length;
                }
            }
        }, false);
    }

    @Override
    public LongStream onEach(final LongConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public long nextLong() {
                final long next = elements.nextLong();
                action.accept(next);
                return next;
            }
        }, isSorted());
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.LongConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.nextLong());
            }
        } finally {
            close();
        }
    }

    @Override
    protected long[] toArray(final boolean closeStream) {
        assertNotClosed();

        try {
            return elements.toArray();
        } finally {
            if (closeStream) {
                close();
            }
        }
    }

    @Override
    public LongList toLongList() throws IllegalStateException {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    @Override
    public List<Long> toList() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofList());
    }

    @Override
    public Set<Long> toSet() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofSet());
    }

    @Override
    public <C extends Collection<Long>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextLong());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Long> toMultiset() throws IllegalStateException {
        assertNotClosed();

        return toMultiset(Suppliers.ofMultiset());
    }

    @Override
    public Multiset<Long> toMultiset(final Supplier<? extends Multiset<Long>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Long> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextLong());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Throwables.LongFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            long next = 0;

            while (elements.hasNext()) {
                next = elements.nextLong();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.LongFunction<? extends K, E> keyMapper,
            final Collector<? super Long, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super Long> downstreamAccumulator = (BiConsumer<Object, ? super Long>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final Map<K, Object> intermediate = (Map<K, Object>) result;
            K key = null;
            Object v = null;
            long next = 0;

            while (elements.hasNext()) {
                next = elements.nextLong();
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

    @Override
    public long reduce(final long identity, final LongBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            long result = identity;

            while (elements.hasNext()) {
                result = accumulator.applyAsLong(result, elements.nextLong());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong reduce(final LongBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalLong.empty();
            }

            long result = elements.nextLong();

            while (elements.hasNext()) {
                result = accumulator.applyAsLong(result, elements.nextLong());
            }

            return OptionalLong.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjLongConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.nextLong());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalLong.empty();
            } else if (isSorted()) {
                return OptionalLong.of(elements.nextLong());
            }

            long candidate = elements.nextLong();
            long next = 0;

            while (elements.hasNext()) {
                next = elements.nextLong();

                if (next < candidate) {
                    candidate = next;
                }
            }

            return OptionalLong.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalLong.empty();
            } else if (isSorted()) {
                long next = 0;

                while (elements.hasNext()) {
                    next = elements.nextLong();
                }

                return OptionalLong.of(next);
            }

            long candidate = elements.nextLong();
            long next = 0;

            while (elements.hasNext()) {
                next = elements.nextLong();

                if (next > candidate) {
                    candidate = next;
                }
            }

            return OptionalLong.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (isSorted()) {
                // For sorted ascending, kthLargest = (k-1)th from the end.
                // Use a ring buffer of size k while draining the iterator.
                long[] window = null;
                int idx = 0;
                int size = 0;
                while (elements.hasNext()) {
                    final long v = elements.nextLong();
                    if (window == null) {
                        window = new long[Math.min(k, 16)];
                    }
                    if (size < k) {
                        if (size == window.length) {
                            window = java.util.Arrays.copyOf(window, (int) Math.min(k, (long) window.length * 2));
                        }

                        window[size++] = v;
                    } else {
                        window[idx] = v;
                        idx = (idx + 1) % k;
                    }
                }
                if (size < k) {
                    return OptionalLong.empty();
                }
                return OptionalLong.of(window[idx]);
            }

            if (!elements.hasNext()) {
                return OptionalLong.empty();
            }

            @SuppressWarnings("resource")
            final Optional<Long> optional = boxed().kthLargest(k, LONG_COMPARATOR);

            return optional.isPresent() ? OptionalLong.of(optional.get()) : OptionalLong.empty();
        } finally {
            close();
        }
    }

    @Override
    public long sum() throws IllegalStateException {
        assertNotClosed();

        try {
            long result = 0;

            while (elements.hasNext()) {
                result += elements.nextLong();
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns the arithmetic mean without allowing the running integral sum to overflow.
     * A {@link BigInteger} accumulator is allocated only if addition overflows a {@code long}.
     *
     * @return an {@code OptionalDouble} containing the arithmetic mean, or an empty optional if this stream is empty
     * @throws IllegalStateException if this stream is already closed
     */
    @Override
    public OptionalDouble average() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            }

            long sum = 0;
            long count = 0;
            BigInteger overflowSafeSum = null;

            do {
                final long value = elements.nextLong();

                if (overflowSafeSum == null) {
                    try {
                        sum = Math.addExact(sum, value);
                    } catch (final ArithmeticException e) {
                        overflowSafeSum = BigInteger.valueOf(sum).add(BigInteger.valueOf(value));
                    }
                } else {
                    overflowSafeSum = overflowSafeSum.add(BigInteger.valueOf(value));
                }

                count++;
            } while (elements.hasNext());

            return OptionalDouble.of((overflowSafeSum == null ? (double) sum : overflowSafeSum.doubleValue()) / count);
        } finally {
            close();
        }
    }

    @Override
    public long count() throws IllegalStateException {
        assertNotClosed();

        try {
            return elements.count();
        } finally {
            close();
        }
    }

    @Override
    public LongSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final LongSummaryStatistics result = new LongSummaryStatistics();

            while (elements.hasNext()) {
                result.accept(elements.nextLong());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextLong())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    @Override
    public <E extends Exception> boolean allMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (!predicate.test(elements.nextLong())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextLong())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> OptionalLong findFirst(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                final long e = elements.nextLong();

                if (predicate.test(e)) {
                    return OptionalLong.of(e);
                }
            }
        } finally {
            close();
        }

        return OptionalLong.empty();
    }

    @Override
    public <E extends Exception> OptionalLong findLast(final Throwables.LongPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalLong.empty();
            }

            boolean hasResult = false;
            long e = 0;
            long result = 0;

            while (elements.hasNext()) {
                e = elements.nextLong();

                if (predicate.test(e)) {
                    result = e;
                    hasResult = true;
                }
            }

            return hasResult ? OptionalLong.of(result) : OptionalLong.empty();
        } finally {
            close();
        }
    }

    @Override
    public FloatStream asFloatStream() throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() {
                return elements.nextLong();
            }

            @Override
            public long count() {
                return elements.count();
            }

            @Override
            public void advance(final long n) {
                elements.advance(n);
            }
        }, isSorted());
    }

    @Override
    public DoubleStream asDoubleStream() throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() {
                return elements.nextLong();
            }

            @Override
            public long count() {
                return elements.count();
            }

            @Override
            public void advance(final long n) {
                elements.advance(n);
            }
        }, isSorted());
    }

    @Override
    public java.util.stream.LongStream toJdkStream() throws IllegalStateException {
        assertNotClosed();

        final PrimitiveIterator.OfLong spliterator = new PrimitiveIterator.OfLong() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public long nextLong() {
                return elements.nextLong();
            }
        };

        if (isEmptyCloseHandlers(closeHandlers())) {
            //noinspection MagicConstant
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(spliterator, DEFAULT_CHARACTERISTICS_PRIMITIVE_JDK_STREAM), isParallel());
        } else {
            //noinspection MagicConstant
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(spliterator, DEFAULT_CHARACTERISTICS_PRIMITIVE_JDK_STREAM), isParallel())
                    .onClose(this::close);
        }
    }

    @Override
    LongIteratorEx iteratorEx() {
        assertNotClosed();

        return elements;
    }

    @Override
    public LongStream appendIfEmpty(final Supplier<? extends LongStream> supplier) throws IllegalStateException {
        assertNotClosed();

        final Holder<LongStream> holder = new Holder<>();

        return newStream(new LongIteratorEx() { //NOSONAR
            private LongIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (iter == null) {
                    init();
                }

                return iter.nextLong();
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
                        final LongStream s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }
        }, false).onClose(() -> close(holder));
    }

    @Override
    public LongStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private LongIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (iter == null) {
                    init();
                }

                return iter.nextLong();
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

    @Override
    protected LongStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelIteratorLongStream(elements, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    protected boolean isEmpty() {
        return !elements.hasNext();
    }
}
