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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatToDoubleFunction;
import com.landawn.abacus.util.function.FloatToIntFunction;
import com.landawn.abacus.util.function.FloatToLongFunction;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.function.ObjFloatConsumer;

/**
 * An iterator-based implementation of FloatStream that processes float elements sequentially.
 * This class serves as the default sequential stream implementation for float values,
 * wrapping a FloatIterator to provide stream operations.
 *
 * <p>This is an internal implementation class. Users should create streams through
 * the public FloatStream factory methods rather than instantiating this class directly.
 *
 * @see FloatStream
 * @see FloatIteratorEx
 */
class IteratorFloatStream extends AbstractFloatStream {
    final FloatIteratorEx elements;

    //    OptionalFloat head;
    //    FloatStream tail;

    //    FloatStream head2;
    //    OptionalFloat tail2;

    /**
     * Constructs an IteratorFloatStream from a FloatIterator.
     * Creates an unsorted stream with no close handlers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iterator = FloatIterator.of(1.0f, 2.5f, 3.7f, 4.2f, 5.9f);
     * IteratorFloatStream stream = new IteratorFloatStream(iterator);
     * stream.forEach(System.out::println);   // prints 1.0, 2.5, 3.7, 4.2, 5.9
     * }</pre>
     *
     * @param values the float iterator to wrap as a stream
     */
    IteratorFloatStream(final FloatIterator values) {
        this(values, null);
    }

    /**
     * Constructs an IteratorFloatStream from a FloatIterator with close handlers.
     * Creates an unsorted stream that will execute the provided close handlers when closed.
     * The close handlers are invoked in order when the stream's close() method is called,
     * ensuring proper resource cleanup.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatIterator iterator = FloatIterator.of(1.0f, 2.5f, 3.7f, 4.2f, 5.9f);
     * List<LocalRunnable> closeHandlers = new ArrayList<>();
     * closeHandlers.add(() -> System.out.println("Stream closed"));
     * IteratorFloatStream stream = new IteratorFloatStream(iterator, closeHandlers);
     * try {
     *     stream.forEach(System.out::println);
     * } finally {
     *     stream.close();   // invokes all close handlers
     * }
     * }</pre>
     *
     * @param values the float iterator to wrap as a stream
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorFloatStream(final FloatIterator values, final Collection<LocalRunnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    /**
     * Constructs an IteratorFloatStream from a FloatIterator with sorting and close handlers.
     * This is the primary constructor that all other constructors delegate to. The sorted flag
     * allows optimization of operations like max(), kthLargest(), and distinct() when elements are
     * known to be in natural ascending order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a sorted stream with close handlers
     * FloatIterator sortedIterator = FloatIterator.of(1.0f, 2.5f, 3.7f, 4.2f, 5.9f);
     * List<LocalRunnable> closeHandlers = new ArrayList<>();
     * closeHandlers.add(() -> System.out.println("Cleanup complete"));
     *
     * IteratorFloatStream stream = new IteratorFloatStream(sortedIterator, true, closeHandlers);
     * try {
     *     OptionalFloat min = stream.min();          // returns 1.0 (the minimum)
     *     System.out.println("Min: " + min.get());   // prints 1.0
     * } finally {
     *     stream.close();
     * }
     * }</pre>
     *
     * @param values the float iterator to wrap as a stream
     * @param sorted {@code true} if the elements are already sorted in natural order, {@code false} otherwise
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorFloatStream(final FloatIterator values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        FloatIteratorEx tmp = null;

        if (values instanceof FloatIteratorEx) {
            tmp = (FloatIteratorEx) values;
        } else {
            tmp = new FloatIteratorEx() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public float nextFloat() {
                    return values.nextFloat();
                }
            };
        }

        elements = tmp;
    }

    @Override
    public FloatStream filter(final FloatPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.nextFloat();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public float nextFloat() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        }, isSorted());
    }

    @Override
    public FloatStream takeWhile(final FloatPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean hasMore = true;
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext && hasMore && elements.hasNext()) {
                    next = elements.nextFloat();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public float nextFloat() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted());
    }

    @Override
    public FloatStream dropWhile(final FloatPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private float next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextFloat();

                            if (!predicate.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.nextFloat();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public float nextFloat() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted());
    }

    @Override
    public FloatStream map(final FloatUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() throws IllegalArgumentException {
                return mapper.applyAsFloat(elements.nextFloat());
            }

        }, false);
    }

    @Override
    public IntStream mapToInt(final FloatToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() throws IllegalArgumentException {
                return mapper.applyAsInt(elements.nextFloat());
            }

        }, false);
    }

    @Override
    public LongStream mapToLong(final FloatToLongFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public long nextLong() throws IllegalArgumentException {
                return mapper.applyAsLong(elements.nextFloat());
            }

        }, false);
    }

    @Override
    public DoubleStream mapToDouble(final FloatToDoubleFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() throws IllegalArgumentException {
                return mapper.applyAsDouble(elements.nextFloat());
            }

        }, false);
    }

    @Override
    public <T> Stream<T> mapToObj(final FloatFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException {
                return mapper.apply(elements.nextFloat());
            }

        }, false, null);
    }

    @Override
    public FloatStream flatMap(final FloatFunction<? extends FloatStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.nextFloat());

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
            public void closeResource() throws IllegalStateException {
                if (closeHandle != null) {
                    StreamBase.close(closeHandle);
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    @Override
    public FloatStream flatmap(final FloatFunction<? extends Collection<Float>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private Iterator<Float> cur = null;
            private Collection<Float> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextFloat());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public float nextFloat() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Float v = cur.next();
                return v == null ? 0f : v;
            }
        }, false);
    }

    @Override
    public FloatStream flatMapArray(final FloatFunction<float[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArray(mapper);
        }

        return newStream(new FloatIteratorEx() { //NOSONAR
            private float[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.nextFloat());
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
            public float nextFloat() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    @Override
    public IntStream flatMapToInt(final FloatFunction<? extends IntStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.nextFloat());

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

    @Override
    public LongStream flatMapToLong(final FloatFunction<? extends LongStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.nextFloat());

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

    @Override
    public DoubleStream flatMapToDouble(final FloatFunction<? extends DoubleStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.nextFloat());

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
            public void closeResource() {
                if (closeHandle != null) {
                    StreamBase.close(closeHandle);
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    @Override
    public <T> Stream<T> flatMapToObj(final FloatFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.nextFloat());

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

    @Override
    public <T> Stream<T> flatmapToObj(final FloatFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<? extends T> cur = null;
            private Collection<? extends T> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextFloat());
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
    public FloatStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new FloatIteratorEx() { //NOSONAR
                private boolean hasNext = false;
                private float prev = 0;
                private float next = 0;
                private boolean isFirst = true;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (elements.hasNext()) {
                            next = elements.nextFloat();

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
                public float nextFloat() {
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
    public FloatStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        return newStream(new FloatIteratorEx() { //NOSONAR
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public float nextFloat() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;
                return elements.nextFloat();
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
    public FloatStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        //    if (n == 0) {
        //        return newStream(elements, isSorted());
        //    }

        if (n == 0) {
            return this;
        }

        return newStream(new FloatIteratorEx() { //NOSONAR
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
            public float nextFloat() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.nextFloat();
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
            public float[] toArray() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.toArray();
            }
        }, isSorted());
    }

    @Override
    public FloatStream top(final int n) throws IllegalStateException {
        assertNotClosed();

        return top(n, FLOAT_COMPARATOR);
    }

    @Override
    public FloatStream top(final int n, final Comparator<? super Float> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return newStream(FloatIteratorEx.empty(), false);
        }

        return newStream(new FloatIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private float[] aar;
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
            public float nextFloat() {
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
            public float[] toArray() {
                if (!initialized) {
                    init();
                }

                final float[] a = new float[to - cursor];
                N.copy(aar, cursor, a, 0, to - cursor);
                cursor = to; // consume all elements
                return a;
            }

            @Override
            public FloatList toList() {
                return FloatList.of(toArray());
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    if (isSorted() && isSameComparator(comparator, comparator())) {
                        final LinkedList<Float> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.nextFloat());
                        }

                        aar = Array.unbox(queue.toArray(N.EMPTY_FLOAT_OBJ_ARRAY));
                    } else {
                        final Comparator<? super Float> cmp = comparator == null ? FLOAT_COMPARATOR : comparator;
                        final Queue<Float> heap = new PriorityQueue<>(n, cmp);

                        Float next = null;
                        while (elements.hasNext()) {
                            next = elements.nextFloat();

                            if (heap.size() >= n) {
                                if (cmp.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = Array.unbox(heap.toArray(N.EMPTY_FLOAT_OBJ_ARRAY));
                    }

                    to = aar.length;
                }
            }
        }, false);
    }

    @Override
    public FloatStream onEach(final FloatConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() {
                final float next = elements.nextFloat();
                action.accept(next);
                return next;
            }
        }, isSorted());
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.FloatConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.nextFloat());
            }
        } finally {
            close();
        }
    }

    @Override
    protected float[] toArray(final boolean closeStream) {
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
    public FloatList toFloatList() throws IllegalStateException {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    @Override
    public List<Float> toList() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofList());
    }

    @Override
    public Set<Float> toSet() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofSet());
    }

    @Override
    public <C extends Collection<Float>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Float> toMultiset() throws IllegalStateException {
        assertNotClosed();

        return toMultiset(Suppliers.ofMultiset());
    }

    @Override
    public Multiset<Float> toMultiset(final Supplier<? extends Multiset<Float>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Float> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Throwables.FloatFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            float next = 0;

            while (elements.hasNext()) {
                next = elements.nextFloat();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Collector<? super Float, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super Float> downstreamAccumulator = (BiConsumer<Object, ? super Float>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final Map<K, Object> intermediate = (Map<K, Object>) result;
            K key = null;
            Object v = null;
            float next = 0;

            while (elements.hasNext()) {
                next = elements.nextFloat();
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
    public float reduce(final float identity, final FloatBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            float result = identity;

            while (elements.hasNext()) {
                result = accumulator.applyAsFloat(result, elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat reduce(final FloatBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalFloat.empty();
            }

            float result = elements.nextFloat();

            while (elements.hasNext()) {
                result = accumulator.applyAsFloat(result, elements.nextFloat());
            }

            return OptionalFloat.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjFloatConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalFloat.empty();
            }

            // No sorted-first-element shortcut for floats: NaN sorts LAST, so the first element of
            // a sorted stream silently dropped the NaN that min() must propagate. The Math.min scan
            // below (NaN-propagating, consistent with java.util.stream.DoubleStream.min semantics)
            // is correct for sorted input too.
            float candidate = elements.nextFloat();

            while (elements.hasNext()) {
                candidate = Math.min(candidate, elements.nextFloat());
            }

            return OptionalFloat.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalFloat.empty();
            } else if (isSorted()) {
                float next = 0;

                while (elements.hasNext()) {
                    next = elements.nextFloat();
                }

                return OptionalFloat.of(next);
            }

            // Use Math.max so NaN propagates, consistent with ArrayFloatStream.max and
            // FloatSummaryStatistics / java.util.stream.DoubleStream.max semantics.
            float candidate = elements.nextFloat();

            while (elements.hasNext()) {
                candidate = Math.max(candidate, elements.nextFloat());
            }

            return OptionalFloat.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (isSorted()) {
                // For sorted ascending, kthLargest = (k-1)th from the end.
                // Use a ring buffer of size k while draining the iterator.
                float[] window = null;
                int idx = 0;
                int size = 0;
                while (elements.hasNext()) {
                    final float v = elements.nextFloat();
                    if (window == null) {
                        window = new float[k];
                    }
                    if (size < k) {
                        window[size++] = v;
                    } else {
                        window[idx] = v;
                        idx = (idx + 1) % k;
                    }
                }
                if (size < k) {
                    return OptionalFloat.empty();
                }
                return OptionalFloat.of(window[idx]);
            }

            if (!elements.hasNext()) {
                return OptionalFloat.empty();
            }

            @SuppressWarnings("resource")
            final Optional<Float> optional = boxed().kthLargest(k, FLOAT_COMPARATOR);

            return optional.isPresent() ? OptionalFloat.of(optional.get()) : OptionalFloat.empty();
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
    public FloatSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final FloatSummaryStatistics result = new FloatSummaryStatistics();

            while (elements.hasNext()) {
                result.accept(elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextFloat())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    @Override
    public <E extends Exception> boolean allMatch(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (!predicate.test(elements.nextFloat())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextFloat())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> OptionalFloat findFirst(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                final float e = elements.nextFloat();

                if (predicate.test(e)) {
                    return OptionalFloat.of(e);
                }
            }
        } finally {
            close();
        }

        return OptionalFloat.empty();
    }

    @Override
    public <E extends Exception> OptionalFloat findLast(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalFloat.empty();
            }

            boolean hasResult = false;
            float e = 0;
            float result = 0;

            while (elements.hasNext()) {
                e = elements.nextFloat();

                if (predicate.test(e)) {
                    result = e;
                    hasResult = true;
                }
            }

            return hasResult ? OptionalFloat.of(result) : OptionalFloat.empty();
        } finally {
            close();
        }
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
                return elements.nextFloat();
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
    FloatIteratorEx iteratorEx() {
        assertNotClosed();

        return elements;
    }

    @Override
    public FloatStream appendIfEmpty(final Supplier<? extends FloatStream> supplier) throws IllegalStateException {
        assertNotClosed();

        final Holder<FloatStream> holder = new Holder<>();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private FloatIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (iter == null) {
                    init();
                }

                return iter.nextFloat();
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
                        final FloatStream s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }
        }, false).onClose(() -> close(holder));
    }

    @Override
    public FloatStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private FloatIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (iter == null) {
                    init();
                }

                return iter.nextFloat();
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
    protected FloatStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelIteratorFloatStream(elements, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    protected boolean isEmpty() {
        return !elements.hasNext();
    }
}
