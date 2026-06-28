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

import java.util.ArrayDeque;
import java.util.ArrayList;
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
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * An iterator-based implementation of Stream that processes elements sequentially.
 * This class serves as the default sequential stream implementation for object references,
 * wrapping an Iterator to provide stream operations.
 *
 * <p>This is an internal implementation class. Users should create streams through
 * the public Stream factory methods rather than instantiating this class directly.
 *
 * @param <T> the type of stream elements
 * @see Stream
 * @see ObjIteratorEx
 */
class IteratorStream<T> extends AbstractStream<T> {
    final ObjIteratorEx<T> elements;

    //    Optional<T> head;
    //    Stream<T> tail;

    //    Stream<T> head2;
    //    Optional<T> tail2;

    /**
     * Constructs an IteratorStream from an Iterator.
     * Creates an unsorted stream with no close handlers.
     *
     * @param values the iterator to wrap as a stream
     */
    IteratorStream(final Iterator<? extends T> values) {
        this(values, null);
    }

    /**
     * Constructs an IteratorStream from an Iterator with close handlers.
     * Creates an unsorted stream with no comparator that will execute the provided close handlers when closed.
     *
     * @param values the iterator to wrap as a stream
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorStream(final Iterator<? extends T> values, final Collection<LocalRunnable> closeHandlers) {
        this(values, false, null, closeHandlers);
    }

    /**
     * Constructs an IteratorStream from an Iterator with sorting, comparator, and close handlers.
     * This is the primary constructor that all other constructors delegate to.
     *
     * @param values the iterator to wrap as a stream
     * @param sorted {@code true} if the elements are already sorted according to the comparator, {@code false} otherwise
     * @param comparator the comparator used for ordering, may be {@code null} for natural ordering
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorStream(final Iterator<? extends T> values, final boolean sorted, final Comparator<? super T> comparator,
            final Collection<LocalRunnable> closeHandlers) {
        super(sorted, comparator, closeHandlers);
        checkArgNotNull(values);

        ObjIteratorEx<T> tmp = null;

        if (values instanceof ObjIteratorEx) {
            tmp = (ObjIteratorEx<T>) values;
        } else {
            tmp = new ObjIteratorEx<>() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public T next() {
                    return values.next();
                }
            };
        }

        elements = tmp;
    }

    IteratorStream(final Stream<T> stream, final boolean sorted, final Comparator<? super T> comparator, final Deque<LocalRunnable> closeHandlers) {
        this(iterate(stream), sorted, comparator, mergeCloseHandlers(closeHandlers, stream));
    }

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate.
     *
     * <p>This is a stateless intermediate operation. The sorted state and comparator of this
     * stream are preserved in the returned stream.
     *
     * @param predicate the predicate to apply to each element; must not be {@code null}
     * @return a new stream containing only the elements that match the predicate
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> filter(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.next();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        }, isSorted(), comparator());
    }

    /**
     * Returns a stream consisting of the elements from this stream, taking elements
     * as long as the given predicate returns {@code true}. Once the predicate returns
     * {@code false} for an element, no further elements are included.
     *
     * <p>This is a short-circuit stateful intermediate operation. The sorted state and
     * comparator of this stream are preserved in the returned stream.
     *
     * @param predicate the predicate to apply to each element to decide whether to include it;
     *                  must not be {@code null}
     * @return a new stream containing the longest prefix of elements matching the predicate
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> takeWhile(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasMore = true;
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                if (!hasNext && hasMore && elements.hasNext()) {
                    next = elements.next();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted(), comparator());
    }

    /**
     * Returns a stream that skips elements from the start of this stream as long as the given
     * predicate returns {@code true}, and then includes all remaining elements.
     *
     * <p>This is a stateful intermediate operation. The sorted state and comparator of this
     * stream are preserved in the returned stream.
     *
     * @param predicate the predicate applied to leading elements to decide how many to skip;
     *                  must not be {@code null}
     * @return a new stream that drops the longest prefix of elements matching the predicate
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.next();

                            if (!predicate.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.next();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted(), comparator());
    }

    /**
     * Returns a stream consisting of the results of applying the given mapper function
     * to the elements of this stream.
     *
     * <p>This is a stateless intermediate operation. The resulting stream is not sorted.
     *
     * @param <R> the type of elements produced by the mapper
     * @param mapper the function to apply to each element; must not be {@code null}
     * @return a new stream consisting of mapped elements
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> Stream<R> map(final Function<? super T, ? extends R> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public R next() throws IllegalArgumentException {
                return mapper.apply(elements.next());
            }

        }, false, null);
    }

    @Override
    public <R> Stream<R> slidingMap(final int increment, final boolean ignoreNotPaired, final BiFunction<? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        final int windowSize = 2;
        checkArgPositive(increment, "increment"); //NOSONAR

        return newStream(new ObjIteratorEx<>() {
            @SuppressWarnings("unchecked")
            private final T none = (T) NONE;
            private T prev = none;
            private R ret = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                if (ignoreNotPaired && prev == none && elements.hasNext()) {
                    prev = elements.next();
                }

                return elements.hasNext(); // || (!ignoreNotPaired && prev != none);
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                toSkip = increment > windowSize;

                if (ignoreNotPaired) {
                    if (increment == 1) {
                        return mapper.apply(prev, ((prev = (elements.hasNext() ? elements.next() : none)) == none) ? null : prev);
                    } else {
                        ret = mapper.apply(prev, elements.next());
                        prev = none;
                        return ret;
                    }
                } else if (increment == 1) {
                    return mapper.apply(prev == none ? elements.next() : prev, ((prev = (elements.hasNext() ? elements.next() : none)) == none) ? null : prev);
                } else {
                    return mapper.apply(elements.next(), elements.hasNext() ? elements.next() : null);
                }
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> slidingMap(final int increment, final boolean ignoreNotPaired, final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        final int windowSize = 3;
        checkArgPositive(increment, cs.increment);

        return newStream(new ObjIteratorEx<>() {
            @SuppressWarnings("unchecked")
            private final T none = (T) NONE;
            private T prev = none;
            private T prevPrev = none;
            private R ret = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                if (ignoreNotPaired) {
                    if (prevPrev == none && elements.hasNext()) {
                        prevPrev = elements.next();
                    }

                    if (prev == none && elements.hasNext()) {
                        prev = elements.next();
                    }
                }

                return elements.hasNext(); // || (!ignoreNotPaired && prev != none);
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                toSkip = increment > windowSize;

                if (ignoreNotPaired) {
                    if (increment == 1) {
                        return mapper.apply(prevPrev, (prevPrev = prev), prev = elements.next());
                    } else if (increment == 2) {
                        ret = mapper.apply(prevPrev, prev, prevPrev = elements.next());
                        prev = none;
                        return ret;
                    } else {
                        ret = mapper.apply(prevPrev, prev, elements.next());
                        prevPrev = none;
                        prev = none;
                        return ret;
                    }
                } else if (increment == 1) {
                    return mapper.apply(prevPrev == none ? elements.next() : prevPrev,
                            (prevPrev = (prev == none ? (elements.hasNext() ? elements.next() : none) : prev)) == none ? null : prevPrev,
                            ((prev = (elements.hasNext() ? elements.next() : none)) == none) ? null : prev);
                } else if (increment == 2) {
                    return mapper.apply(prev == none ? elements.next() : prev, elements.hasNext() ? elements.next() : null,
                            ((prev = (elements.hasNext() ? elements.next() : none)) == none) ? null : prev);
                } else {
                    ret = mapper.apply(elements.next(), elements.hasNext() ? elements.next() : null, elements.hasNext() ? elements.next() : null);
                    prevPrev = none;
                    prev = none;
                    return ret;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> mapFirst(final Function<? super T, ? extends T> mapperForFirst) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException {
                if (isFirst) {
                    isFirst = false;
                    return mapperForFirst.apply(elements.next());
                } else {
                    return elements.next();
                }
            }

            @Override
            public void advance(final long n) {
                if (n > 0) {
                    if (hasNext()) {
                        next();
                    }

                    elements.advance(n - 1);
                }
            }

            @Override
            public long count() {
                if (hasNext()) {
                    next();
                    return elements.count() + 1;
                }

                return 0;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> mapFirstOrElse(final Function<? super T, ? extends R> mapperForFirst, final Function<? super T, ? extends R> mapperForElse)
            throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public R next() throws IllegalArgumentException {
                if (isFirst) {
                    isFirst = false;
                    return mapperForFirst.apply(elements.next());
                } else {
                    return mapperForElse.apply(elements.next());
                }
            }

        }, false, null);
    }

    @Override
    public Stream<T> mapLast(final Function<? super T, ? extends T> mapperForLast) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException {
                next = elements.next();

                if (hasNext = elements.hasNext()) {
                    return next;
                } else {
                    return mapperForLast.apply(next);
                }
            }

        }, false, null);
    }

    @Override
    public <R> Stream<R> mapLastOrElse(final Function<? super T, ? extends R> mapperForLast, final Function<? super T, ? extends R> mapperForElse)
            throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public R next() throws IllegalArgumentException {
                final T next = elements.next();

                if (elements.hasNext()) {
                    return mapperForElse.apply(next);
                } else {
                    return mapperForLast.apply(next);
                }
            }

        }, false, null);
    }

    @Override
    public CharStream mapToChar(final ToCharFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public char nextChar() throws IllegalArgumentException {
                return mapper.applyAsChar(elements.next());
            }

        }, false);
    }

    @Override
    public ByteStream mapToByte(final ToByteFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public byte nextByte() throws IllegalArgumentException {
                return mapper.applyAsByte(elements.next());
            }

        }, false);
    }

    @Override
    public ShortStream mapToShort(final ToShortFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public short nextShort() throws IllegalArgumentException {
                return mapper.applyAsShort(elements.next());
            }

        }, false);
    }

    @Override
    public IntStream mapToInt(final ToIntFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() throws IllegalArgumentException {
                return mapper.applyAsInt(elements.next());
            }

        }, false);
    }

    @Override
    public LongStream mapToLong(final ToLongFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public long nextLong() throws IllegalArgumentException {
                return mapper.applyAsLong(elements.next());
            }

        }, false);
    }

    @Override
    public FloatStream mapToFloat(final ToFloatFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() throws IllegalArgumentException {
                return mapper.applyAsFloat(elements.next());
            }

        }, false);
    }

    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() throws IllegalArgumentException {
                return mapper.applyAsDouble(elements.next());
            }

        }, false);
    }

    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<R> iter = new ObjIteratorEx<>() {
            private Iterator<? extends R> cur = null;
            private Stream<? extends R> s = null;
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

                        s = mapper.apply(elements.next());

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
            public R next() {
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
    public <R> Stream<R> flatmap(final Function<? super T, ? extends Collection<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<? extends R> cur = null;
            private Collection<? extends R> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.next());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        }, false, null);
    }

    @SuppressFBWarnings
    @Override
    public <R> Stream<R> flatMapArray(final Function<? super T, R[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private R[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
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
            public R next() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false, null);
    }

    @Override
    public CharStream flatMapToChar(final Function<? super T, ? extends CharStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = new CharIteratorEx() {
            private CharIterator cur = null;
            private CharStream s = null;
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

                        s = mapper.apply(elements.next());

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
            public char nextChar() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextChar();
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
    public CharStream flatMapArrayToChar(final Function<? super T, char[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToChar(mapper);
        }

        return newStream(new CharIteratorEx() { //NOSONAR
            private char[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
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
            public char nextChar() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    @Override
    public ByteStream flatMapToByte(final Function<? super T, ? extends ByteStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = new ByteIteratorEx() {
            private ByteIterator cur = null;
            private ByteStream s = null;
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

                        s = mapper.apply(elements.next());

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
            public byte nextByte() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextByte();
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
    public ByteStream flatMapArrayToByte(final Function<? super T, byte[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToByte(mapper);
        }

        return newStream(new ByteIteratorEx() { //NOSONAR
            private byte[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
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
            public byte nextByte() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    @Override
    public ShortStream flatMapToShort(final Function<? super T, ? extends ShortStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = new ShortIteratorEx() {
            private ShortIterator cur = null;
            private ShortStream s = null;
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

                        s = mapper.apply(elements.next());

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
            public short nextShort() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextShort();
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
    public ShortStream flatMapArrayToShort(final Function<? super T, short[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToShort(mapper);
        }

        return newStream(new ShortIteratorEx() { //NOSONAR
            private short[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
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
            public short nextShort() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    @Override
    public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.next());

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
    public IntStream flatMapArrayToInt(final Function<? super T, int[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToInt(mapper);
        }

        return newStream(new IntIteratorEx() { //NOSONAR
            private int[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
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
            public int nextInt() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    @Override
    public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.next());

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
    public LongStream flatMapArrayToLong(final Function<? super T, long[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToLong(mapper);
        }

        return newStream(new LongIteratorEx() { //NOSONAR
            private long[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
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
    public FloatStream flatMapToFloat(final Function<? super T, ? extends FloatStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.next());

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

    @Override
    public FloatStream flatMapArrayToFloat(final Function<? super T, float[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToFloat(mapper);
        }

        return newStream(new FloatIteratorEx() { //NOSONAR
            private float[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
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
    public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.next());

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
    public DoubleStream flatMapArrayToDouble(final Function<? super T, double[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToDouble(mapper);
        }

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private double[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
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

    @Override
    public <C extends Collection<T>> Stream<C> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(chunkSize, cs.chunkSize);
        checkArgNotNull(collectionSupplier, cs.collectionSupplier);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public C next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C result = collectionSupplier.apply(chunkSize);
                int cnt = 0;

                while (cnt < chunkSize && elements.hasNext()) {
                    result.add(elements.next());
                    cnt++;
                }

                return result;
            }

            @Override
            public long count() {
                final long len = elements.count();
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void advance(final long n) {
                elements.advance(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> split(final int chunkSize, final Collector<? super T, ?, R> collector) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(chunkSize, cs.chunkSize);
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Object container = supplier.get();
                int cnt = 0;

                while (cnt < chunkSize && elements.hasNext()) {
                    accumulator.accept(container, elements.next());
                    cnt++;
                }

                return finisher.apply(container);
            }

            @Override
            public long count() {
                final long len = elements.count();
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void advance(final long n) {
                elements.advance(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> split(final Predicate<? super T> predicate, final Supplier<? extends C> collectionSupplier)
            throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private T next = (T) NONE;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return next != NONE || elements.hasNext();
            }

            @Override
            public C next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C result = collectionSupplier.get();
                boolean isFirst = true;

                if (next == NONE) {
                    next = elements.next();
                }

                while (next != NONE) {
                    if (isFirst) {
                        result.add(next);
                        preCondition = predicate.test(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                        isFirst = false;
                    } else if (predicate.test(next) == preCondition) {
                        result.add(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                    } else {
                        break;
                    }
                }

                return result;
            }

        }, false, null);
    }

    @Override
    public <R> Stream<R> split(final Predicate<? super T> predicate, final Collector<? super T, ?, R> collector) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private T next = (T) NONE;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return next != NONE || elements.hasNext();
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Object container = supplier.get();
                boolean isFirst = true;

                if (next == NONE) {
                    next = elements.next();
                }

                while (next != NONE) {
                    if (isFirst) {
                        accumulator.accept(container, next);
                        preCondition = predicate.test(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                        isFirst = false;
                    } else if (predicate.test(next) == preCondition) {
                        accumulator.accept(container, next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                    } else {

                        break;
                    }
                }

                return finisher.apply(container);
            }

        }, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> sliding(final int windowSize, final int increment, final IntFunction<? extends C> collectionSupplier)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collectionSupplier, cs.collectionSupplier);

        return newStream(new ObjIteratorEx<>() {
            private Deque<T> queue = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                // Stream.of(1, 2, 3).sliding(2, 1) will return [[1, 2], [2, 3]], not [[1, 2], [2, 3], [3]]
                // But Stream.of(1).sliding(2, 1) will return [[1]], not []
                // Why? we need to check if the queue is not empty?
                // Not really, because elements.hasNext() is used to check if there are more elements to process.
                // In first case, elements.hasNext() will return false after processing [2, 3], so hasNext will return false.
                // In second case, elements.hasNext() will return true before processing the first element,
                // so the partial window [1] is emitted.
                return elements.hasNext(); // || (queue != null && !queue.isEmpty());
            }

            @Override
            public C next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(N.max(0, windowSize - increment));
                }

                final C result = collectionSupplier.apply(windowSize);
                int cnt = 0;

                if (queue.size() > 0 && increment < windowSize) {
                    cnt = queue.size();

                    result.addAll(queue);

                    if (queue.size() <= increment) {
                        queue.clear();
                    } else {
                        for (int i = 0; i < increment; i++) {
                            queue.removeFirst();
                        }
                    }
                }

                T next = null;

                while (cnt < windowSize && elements.hasNext()) {
                    next = elements.next();
                    result.add(next);
                    cnt++;

                    if (cnt > increment) {
                        queue.add(next);
                    }
                }

                toSkip = increment > windowSize;

                return result;
            }

            @Override
            public long count() {
                final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());
                final long len = prevSize + elements.count();
                // After advance(n), the queue may hold only leftover-prefix elements from a
                // previous window with no new source elements available; no new window can
                // start in that case.
                if (len == prevSize) {
                    return 0;
                }
                return countSlidingWindows(len, windowSize, increment);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (increment >= windowSize) {
                    //noinspection DuplicateExpressions
                    elements.advance(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    @SuppressWarnings("DuplicateExpressions")
                    final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                    final int prevSize = queue == null ? 0 : queue.size(); //NOSONAR

                    if (m < prevSize) {
                        for (int i = 0; i < m; i++) {
                            queue.removeFirst();
                        }
                    } else {
                        if (N.notEmpty(queue)) {
                            queue.clear();
                        }

                        if (m - prevSize > 0) {
                            elements.advance(m - prevSize);
                        }
                    }
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(windowSize);
                }

                final int countToKeepInQueue = windowSize - increment;
                int cnt = queue.size();

                while (cnt++ < countToKeepInQueue && elements.hasNext()) {
                    queue.add(elements.next());
                }
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> sliding(final int windowSize, final int increment, final Collector<? super T, ?, R> collector)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return newStream(new ObjIteratorEx<>() {
            private Deque<T> queue = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                // Stream.of(1, 2, 3).sliding(2, 1) will return [[1, 2], [2, 3]], not [[1, 2], [2, 3], [3]]
                // But Stream.of(1).sliding(2, 1) will return [[1]], not []
                // Why? we need to check if the queue is not empty?
                // Not really, because elements.hasNext() is used to check if there are more elements to process.
                // In first case, elements.hasNext() will return false after processing [2, 3], so hasNext will return false.
                // In second case, elements.hasNext() will return true before processing the first element,
                // so the partial window [1] is emitted.
                return elements.hasNext(); // || (queue != null && !queue.isEmpty());
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (increment < windowSize && queue == null) {
                    queue = new ArrayDeque<>(windowSize - increment);
                }

                final Object container = supplier.get();
                int cnt = 0;

                if (increment < windowSize && queue.size() > 0) {
                    cnt = queue.size();

                    for (final T e : queue) {
                        accumulator.accept(container, e);
                    }

                    if (queue.size() <= increment) {
                        queue.clear();
                    } else {
                        for (int i = 0; i < increment; i++) {
                            queue.removeFirst();
                        }
                    }
                }

                T next = null;

                while (cnt < windowSize && elements.hasNext()) {
                    next = elements.next();
                    accumulator.accept(container, next);
                    cnt++;

                    if (cnt > increment) {
                        queue.add(next);
                    }
                }

                toSkip = increment > windowSize;

                return finisher.apply(container);
            }

            @Override
            public long count() {
                final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());
                final long len = prevSize + elements.count();
                // After advance(n), the queue may hold only leftover-prefix elements from a
                // previous window with no new source elements available; no new window can
                // start in that case.
                if (len == prevSize) {
                    return 0;
                }
                return countSlidingWindows(len, windowSize, increment);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (increment >= windowSize) {
                    //noinspection DuplicateExpressions
                    elements.advance(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    @SuppressWarnings("DuplicateExpressions")
                    final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                    final int prevSize = queue == null ? 0 : queue.size(); //NOSONAR

                    if (m < prevSize) {
                        for (int i = 0; i < m; i++) {
                            queue.removeFirst();
                        }
                    } else {
                        if (N.notEmpty(queue)) {
                            queue.clear();
                        }

                        if (m - prevSize > 0) {
                            elements.advance(m - prevSize);
                        }
                    }
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(windowSize);
                }

                final int countToKeepInQueue = windowSize - increment;
                int cnt = queue.size();

                while (cnt++ < countToKeepInQueue && elements.hasNext()) {
                    queue.add(elements.next());
                }
            }
        }, false, null);
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream.
     * If this stream is sorted by natural ordering, the deduplication is performed with an efficient
     * adjacent-equality check; otherwise a hash set is used.
     *
     * <p>This is a stateful intermediate operation.
     *
     * @return a new stream containing only the distinct elements of this stream
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted() && comparator() == null) {
            return newStream(new ObjIteratorEx<>() { //NOSONAR
                private boolean hasNext = false;
                private T prev = null;
                private T next = null;
                private boolean isFirst = true;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (elements.hasNext()) {
                            next = elements.next();

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
                public T next() {
                    if (!hasNext && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    prev = next;

                    return next;
                }
            }, isSorted(), comparator());
        } else {
            final Set<Object> set = N.newHashSet();

            // noinspection resource
            return newStream(sequential().filter(value -> set.add(hashKey(value))).iteratorEx(), isSorted(), comparator());
        }
    }

    /**
     * Returns a stream consisting of at most {@code maxSize} elements from the start of this stream.
     *
     * <p>This is a short-circuit stateful intermediate operation. The sorted state and comparator
     * of this stream are preserved in the returned stream.
     *
     * @param maxSize the maximum number of elements the returned stream may contain; must be
     *                non-negative
     * @return a stream containing at most {@code maxSize} elements
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    @Override
    public Stream<T> limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public T next() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;
                return elements.next();
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                elements.advance(N.min(n, maxSize - cnt));

                cnt = n >= maxSize - cnt ? maxSize : cnt + n;
            }
        }, isSorted(), comparator());
    }

    /**
     * Returns a stream that skips the first {@code n} elements of this stream.
     *
     * <p>This is a stateful intermediate operation. If {@code n} is zero, {@code this}
     * is returned unchanged. The sorted state and comparator of this stream are preserved.
     *
     * @param n the number of leading elements to skip; must be non-negative
     * @return a stream consisting of the remaining elements after skipping {@code n}
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public Stream<T> skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        //    if (n == 0) {
        //        return newStream(elements, isSorted(), comparator());
        //    }

        if (n == 0) {
            return this;
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
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
            public T next() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.next();
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
            public <A> A[] toArray(final A[] a) {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.toArray(a);
            }
        }, isSorted(), comparator());
    }

    @Override
    public Stream<T> top(final int n, final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return newStream(ObjIteratorEx.empty(), false, null);
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean initialized = false;
            private T[] aar = null;
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
            public T next() {
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
                cursor = n < remaining ? (int) (cursor + Math.min(n, Integer.MAX_VALUE - cursor)) : to;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                if (!initialized) {
                    init();
                }

                final int len = to - cursor;
                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);
                N.copy(aar, cursor, a, 0, len);

                if (a.length > len) {
                    a[len] = null;
                }

                cursor = to;
                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    if (isSorted() && isSameComparator(comparator, comparator())) {
                        final LinkedList<T> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.next());
                        }

                        aar = queue.toArray((T[]) new Object[queue.size()]);
                    } else {
                        final Comparator<? super T> cmp = comparator == null ? NATURAL_COMPARATOR : comparator;
                        final Queue<T> heap = new PriorityQueue<>(n, cmp);

                        T next = null;
                        while (elements.hasNext()) {
                            next = elements.next();

                            if (heap.size() >= n) {
                                if (cmp.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = heap.toArray((T[]) new Object[heap.size()]);
                    }

                    to = aar.length;
                }
            }
        }, false, null);
    }

    /**
     * Returns a stream that performs the given action on each element as elements are consumed.
     *
     * <p>This is a stateless intermediate operation. The sorted state and comparator are
     * preserved in the returned stream.
     *
     * @param action the action to perform on each element; must not be {@code null}
     * @return a new stream that passes each element through {@code action} before yielding it
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Stream<T> onEach(final Consumer<? super T> action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() {
                final T next = elements.next();
                action.accept(next);
                return next;
            }
        }, isSorted(), comparator());
    }

    @Override
    public <E extends Exception, E2 extends Exception> void forEach(final Throwables.Consumer<? super T, E> action, final Throwables.Runnable<E2> onComplete)
            throws E, E2 {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.next());
            }

            onComplete.run();
        } finally {
            close();
        }
    }

    @Override
    public <U, E extends Exception, E2 extends Exception> void forEach(final Throwables.Function<? super T, ? extends Iterable<? extends U>, E> flatMapper,
            final Throwables.BiConsumer<? super T, ? super U, E2> action) throws IllegalStateException, E, E2 {
        assertNotClosed();

        Iterable<? extends U> c = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c = flatMapper.apply(next);

                if (c != null) {
                    for (final U u : c) {
                        action.accept(next, u);
                    }
                }
            }
        } finally {
            close();
        }
    }

    @Override
    public <T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(
            final Throwables.Function<? super T, ? extends Iterable<T2>, E> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E2> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        assertNotClosed();

        Iterable<T2> c2 = null;
        Iterable<T3> c3 = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c2 = flatMapper.apply(next);

                if (c2 != null) {
                    for (final T2 t2 : c2) {
                        c3 = flatMapper2.apply(t2);

                        if (c3 != null) {
                            for (final T3 t3 : c3) {
                                action.accept(next, t2, t3);
                            }
                        }
                    }
                }
            }
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> void forEachPair(final int increment, final Throwables.BiConsumer<? super T, ? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        final int windowSize = 2;
        checkArgPositive(increment, cs.increment);

        try {
            boolean isFirst = true;
            T prev = null;

            while (elements.hasNext()) {
                if (increment > windowSize && !isFirst) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    if (!elements.hasNext()) {
                        break;
                    }
                }

                if (increment == 1) {
                    action.accept(isFirst ? elements.next() : prev, (prev = (elements.hasNext() ? elements.next() : null)));
                } else {
                    action.accept(elements.next(), elements.hasNext() ? elements.next() : null);
                }

                isFirst = false;
            }
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> void forEachTriple(final int increment, final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        final int windowSize = 3;
        checkArgPositive(increment, cs.increment);

        try {
            boolean isFirst = true;
            T prev = null;
            T prev2 = null;

            while (elements.hasNext()) {
                if (increment > windowSize && !isFirst) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    if (!elements.hasNext()) {
                        break;
                    }
                }

                if (increment == 1) {
                    action.accept(isFirst ? elements.next() : prev2, (prev2 = (isFirst ? (elements.hasNext() ? elements.next() : null) : prev)),
                            (prev = (elements.hasNext() ? elements.next() : null)));

                } else if (increment == 2) {
                    action.accept(isFirst ? elements.next() : prev, elements.hasNext() ? elements.next() : null,
                            (prev = (elements.hasNext() ? elements.next() : null)));
                } else {
                    action.accept(elements.next(), elements.hasNext() ? elements.next() : null, elements.hasNext() ? elements.next() : null);
                }

                isFirst = false;
            }
        } finally {
            close();
        }
    }

    @Override
    protected Object[] toArray(final boolean closeStream) {
        assertNotClosed();

        try {
            return elements.toArray(N.EMPTY_OBJECT_ARRAY);
        } finally {
            if (closeStream) {
                close();
            }
        }
    }

    /**
     * Collects all elements of this stream into a {@link List}, in encounter order.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @return a mutable {@link List} containing all stream elements
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public List<T> toList() throws IllegalStateException {
        assertNotClosed();

        try {
            final List<T> result = new ArrayList<>();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@link Set}.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     * Duplicate elements are silently discarded; iteration order of the returned
     * set is unspecified.
     *
     * @return a mutable {@link Set} containing the distinct elements of this stream
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Set<T> toSet() throws IllegalStateException {
        assertNotClosed();

        try {
            final Set<T> result = N.newHashSet();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<T> toMultiset() throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<T> result = new Multiset<>();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<T> toMultiset(final Supplier<? extends Multiset<T>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<T> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>, E extends Exception, E2 extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E> keyMapper, final Throwables.Function<? super T, ? extends V, E2> valueMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                result.put(keyMapper.apply(next), valueMapper.apply(next));
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a left fold on the elements of this stream using the given binary operator,
     * returning an Optional containing the result or an empty Optional if the stream is empty.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param accumulator a function that combines the running result with the next element;
     *                    must not be {@code null}
     * @return an Optional containing the result of folding all elements left-to-right,
     *         or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Optional<T> foldLeft(final BinaryOperator<T> accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            }

            T result = elements.next();

            while (elements.hasNext()) {
                result = accumulator.apply(result, elements.next());
            }

            return Optional.of(result);
        } finally {
            close();
        }
    }

    /**
     * Performs a left fold on the elements of this stream using the given accumulator function
     * and an initial identity value.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param <U> the type of the accumulated result
     * @param identity the initial accumulation value
     * @param accumulator a function combining the running result with the next element;
     *                    must not be {@code null}
     * @return the result of folding all elements left-to-right, starting from {@code identity}
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <U> U foldLeft(final U identity, final BiFunction<? super U, ? super T, U> accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            U result = identity;

            while (elements.hasNext()) {
                result = accumulator.apply(result, elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a right fold on the elements of this stream using the given binary operator.
     * The elements are consumed in reverse order by first reversing the stream.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param accumulator the function to combine elements right-to-left; must not be {@code null}
     * @return an Optional containing the result of folding all elements right-to-left,
     *         or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Optional<T> foldRight(final BinaryOperator<T> accumulator) {
        //noinspection resource
        return reversed().foldLeft(accumulator);
    }

    /**
     * Performs a right fold on the elements of this stream using the given accumulator function
     * and an initial identity value.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param <U> the type of the accumulated result
     * @param identity the initial accumulation value
     * @param accumulator a function combining the running result with the next element right-to-left;
     *                    must not be {@code null}
     * @return the result of folding all elements right-to-left, starting from {@code identity}
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <U> U foldRight(final U identity, final BiFunction<? super U, ? super T, U> accumulator) {
        //noinspection resource
        return reversed().foldLeft(identity, accumulator);
    }

    /**
     * Performs a reduction on the elements of this stream using the given binary operator,
     * returning an Optional with the result or an empty Optional if the stream is empty.
     * This method delegates to {@link #foldLeft(BinaryOperator)}.
     *
     * <p>This is a terminal operation.
     *
     * @param accumulator the associative, non-interfering, stateless function used to reduce
     *                    elements; must not be {@code null}
     * @return an Optional describing the reduction result, or an empty Optional if the stream
     *         is empty
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Optional<T> reduce(final BinaryOperator<T> accumulator) {
        return foldLeft(accumulator);
    }

    /**
     * Performs a reduction on the elements of this stream using the given identity value,
     * accumulator function, and combiner. In sequential mode the combiner is not used.
     * This method delegates to {@link #foldLeft(Object, BiFunction)}.
     *
     * <p>This is a terminal operation.
     *
     * @param <U> the type of the accumulated result
     * @param identity the identity value for the accumulation function
     * @param accumulator a function to integrate a stream element into the running result;
     *                    must not be {@code null}
     * @param combiner a function to combine two partial results (unused in sequential mode);
     *                 must not be {@code null}
     * @return the result of accumulating all elements with {@code accumulator}
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <U> U reduce(final U identity, final BiFunction<? super U, ? super T, U> accumulator, final BinaryOperator<U> combiner) {
        return foldLeft(identity, accumulator);
    }

    /**
     * Performs a mutable reduction on the elements of this stream using the given supplier,
     * accumulator, and combiner functions.
     *
     * <p>This is a terminal operation. The stream is closed after this call. In sequential
     * mode the combiner is not invoked.
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new result container; must not be {@code null}
     * @param accumulator a function that folds an element into the result container;
     *                    must not be {@code null}
     * @param combiner a function that merges two result containers (unused in sequential mode);
     *                 must not be {@code null}
     * @return the populated result container
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super T> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a mutable reduction on the elements of this stream using a {@link Collector}.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param <R> the type of the result
     * @param collector the {@link Collector} describing the reduction; must not be {@code null}
     * @return the result of the collection
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public <R> R collect(final Collector<? super T, ?, R> collector) throws IllegalStateException {
        assertNotClosed();

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        try {
            final Object container = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(container, elements.next());
            }

            return finisher.apply(container);
        } finally {
            close();
        }
    }

    /**
     * Returns a stream consisting of the last {@code n} elements of this stream.
     * If this stream contains fewer than {@code n} elements, all elements are returned.
     *
     * <p>This is a stateful intermediate operation that buffers up to {@code n} elements
     * from the tail of the underlying iterator.
     *
     * @param n the maximum number of trailing elements to include; must be non-negative
     * @return a new stream containing at most the last {@code n} elements
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code n} is negative
     * @see #skipLast(int)
     */
    @Override
    public Stream<T> takeLast(final int n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<T> iter;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (!initialized) {
                    init();
                }

                return iter.next();
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Deque<T> deque = new ArrayDeque<>(Math.min(1024, n));

                    try {
                        while (elements.hasNext()) {
                            if (deque.size() >= n) {
                                deque.pollFirst();
                            }

                            deque.offerLast(elements.next());
                        }
                    } finally {
                        IteratorStream.this.close();
                    }

                    iter = deque.iterator();
                }
            }
        }, isSorted(), comparator());
    }

    /**
     * Returns a stream that excludes the last {@code n} elements of this stream.
     * If {@code n <= 0}, all elements are included. Elements are output as
     * they arrive, using an internal sliding buffer of size {@code n}.
     *
     * <p>This is a stateful intermediate operation.
     *
     * @param n the number of trailing elements to skip; non-positive values result in all elements being included
     * @return a new stream without the last {@code n} elements
     * @throws IllegalStateException if the stream has already been closed
     * @see #takeLast(int)
     */
    @Override
    public Stream<T> skipLast(final int n) throws IllegalStateException {
        assertNotClosed();

        if (n <= 0) {
            return newStream(elements, isSorted(), comparator());
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Deque<T> deque = null;

            @Override
            public boolean hasNext() {
                if (deque == null) {
                    deque = new ArrayDeque<>(Math.min(1024, n));

                    while (deque.size() < n && elements.hasNext()) {
                        deque.offerLast(elements.next());
                    }
                }

                return elements.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                deque.offerLast(elements.next());

                return deque.pollFirst();
            }

        }, isSorted(), comparator());
    }

    /**
     * Returns an Optional containing the minimum element of this stream according to the given
     * comparator, or an empty Optional if the stream is empty.
     *
     * <p>This is a terminal operation. When the stream is sorted according to the given comparator,
     * the first element is returned in O(1) time. The stream is closed after this call.
     *
     * @param comparator the comparator used to compare elements; if {@code null}, nulls-last natural
     *                   ordering is used
     * @return an Optional containing the minimum element, or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Optional<T> min(Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator, comparator())) {
                return Optional.of(elements.next());
            }

            comparator = comparator == null ? NULL_MAX_COMPARATOR : comparator;

            T candidate = elements.next();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                if (comparator.compare(next, candidate) < 0) {
                    candidate = next;
                }
            }

            return Optional.of(candidate);
        } finally {
            close();
        }
    }

    /**
     * Returns an Optional containing the maximum element of this stream according to the given
     * comparator, or an empty Optional if the stream is empty.
     *
     * <p>This is a terminal operation. When the stream is sorted according to the given comparator,
     * the last element is returned by scanning to the end of the iterator. The stream is closed
     * after this call.
     *
     * @param comparator the comparator used to compare elements; if {@code null}, nulls-first natural
     *                   ordering is used
     * @return an Optional containing the maximum element, or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public Optional<T> max(Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator, comparator())) {
                T next = null;

                while (elements.hasNext()) {
                    next = elements.next();
                }

                return Optional.of(next);
            }

            comparator = comparator == null ? NULL_MIN_COMPARATOR : comparator;
            T candidate = elements.next();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                if (comparator.compare(next, candidate) > 0) {
                    candidate = next;
                }
            }

            return Optional.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> kthLargest(final int k, Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator, comparator())) {
                final LinkedList<T> queue = new LinkedList<>();

                while (elements.hasNext()) {
                    if (queue.size() >= k) {
                        queue.poll();
                    }

                    queue.offer(elements.next());
                }

                return queue.size() < k ? Optional.empty() : Optional.of(queue.peek());
            }

            comparator = comparator == null ? NATURAL_COMPARATOR : comparator;
            final Queue<T> queue = new PriorityQueue<>(k, comparator);
            T e = null;

            while (elements.hasNext()) {
                e = elements.next();

                if (queue.size() < k) {
                    queue.offer(e);
                } else {
                    if (comparator.compare(e, queue.peek()) > 0) {
                        queue.poll();
                        queue.offer(e);
                    }
                }
            }

            return queue.size() < k ? Optional.empty() : Optional.of(queue.peek());
        } finally {
            close();
        }
    }

    /**
     * Returns the count of elements in this stream by draining the underlying iterator.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @return the number of elements in this stream
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public long count() throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        try {
            return elements.count();
        } finally {
            close();
        }
    }

    /**
     * Returns {@code true} if any element of this stream matches the given predicate.
     * Returns {@code false} if the stream is empty.
     *
     * <p>This is a terminal short-circuit operation. The stream is closed after this call.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to apply to elements; must not be {@code null}
     * @return {@code true} if at least one element matches the predicate, {@code false} otherwise
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    /**
     * Returns {@code true} if every element of this stream matches the given predicate,
     * or if the stream is empty.
     *
     * <p>This is a terminal short-circuit operation. The stream is closed after this call.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to apply to elements; must not be {@code null}
     * @return {@code true} if all elements match the predicate (or the stream is empty),
     *         {@code false} otherwise
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (!predicate.test(elements.next())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    /**
     * Returns {@code true} if no element of this stream matches the given predicate,
     * or if the stream is empty.
     *
     * <p>This is a terminal short-circuit operation. The stream is closed after this call.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to apply to elements; must not be {@code null}
     * @return {@code true} if no element matches the predicate (or the stream is empty),
     *         {@code false} otherwise
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean hasMatchCountBetween(final long atLeast, final long atMost, final Throwables.Predicate<? super T, E> predicate)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNegative(atLeast, cs.atLeast);
        checkArgNotNegative(atMost, cs.atMost);
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");

        long cnt = 0;

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next()) && (++cnt > atMost)) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return cnt >= atLeast && cnt <= atMost;
    }

    /**
     * Returns an Optional containing the first element of this stream that matches the given
     * predicate, or an empty Optional if no element matches.
     *
     * <p>This is a terminal short-circuit operation. The stream is closed after this call.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test elements against; must not be {@code null}
     * @return an Optional containing the first matching element, or an empty Optional if none match
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> Optional<T> findFirst(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            T e = null;

            while (elements.hasNext()) {
                e = elements.next();

                if (predicate.test(e)) {
                    return Optional.of(e);
                }
            }
        } finally {
            close();
        }

        return Optional.empty();
    }

    /**
     * Returns an Optional containing the last element of this stream that matches the given
     * predicate, or an empty Optional if no element matches.
     *
     * <p>This is a terminal operation that must consume the entire iterator to find the last
     * match. The stream is closed after this call.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test elements against; must not be {@code null}
     * @return an Optional containing the last matching element, or an empty Optional if none match
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> Optional<T> findLast(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            T result = (T) NONE;
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                if (predicate.test(next)) {
                    result = next;
                }
            }

            return result == NONE ? Optional.empty() : Optional.of(result);
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> appendIfEmpty(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        if (N.isEmpty(c)) {
            return newStream(elements, isSorted(), comparator());
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<? extends T> iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (iter == null) {
                    init();
                }

                return iter.next();
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (iter == null) {
                    init();
                }

                if (iter == elements) {
                    elements.advance(n);
                } else {
                    super.advance(n);
                }
            }

            @Override
            public long count() {
                if (iter == null) {
                    init();
                }

                if (iter == elements) {
                    return elements.count();
                } else {
                    return super.count();
                }
            }

            private void init() {
                if (iter == null) {
                    if (elements.hasNext()) {
                        iter = elements;
                    } else {
                        iter = c.iterator();
                    }
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> appendIfEmpty(final Supplier<? extends Stream<T>> supplier) throws IllegalStateException {
        assertNotClosed();

        final Holder<Stream<T>> holder = new Holder<>();

        return newStream(new ObjIteratorEx<T>() { //NOSONAR
            private ObjIteratorEx<? extends T> iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (iter == null) {
                    init();
                }

                return iter.next();
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
                        final Stream<T> s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }
        }, false, null).onClose(() -> close(holder));
    }

    @Override
    public Stream<T> ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private ObjIteratorEx<T> iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (iter == null) {
                    init();
                }

                return iter.next();
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
        }, isSorted(), comparator());
    }

    @Override
    ObjIteratorEx<T> iteratorEx() {
        assertNotClosed();

        return elements;
    }

    /**
     * Creates a parallel version of this iterator-backed stream using the specified parameters.
     *
     * @param maxThreadNum the maximum number of threads for parallel execution
     * @param splitor the strategy used to split the iterator for parallel processing
     * @param asyncExecutor the executor for submitting parallel tasks
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @return a new {@link ParallelIteratorStream} wrapping the same underlying iterator
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    protected Stream<T> parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelIteratorStream<>(elements, isSorted(), comparator(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                closeHandlers());
    }

    /**
     * Returns a standard JDK {@link java.util.stream.Stream} backed by this stream's underlying iterator.
     * Close handlers registered on this stream are forwarded to the returned JDK stream.
     *
     * <p>This is a terminal operation that transfers ownership to the returned JDK stream.
     *
     * @return a JDK {@link java.util.stream.Stream} over the elements of this stream
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public java.util.stream.Stream<T> toJdkStream() throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("MagicConstant")
        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(elements, DEFAULT_CHARACTERISTICS_OBJ_JDK_STREAM);

        if (isEmptyCloseHandlers(closeHandlers())) {
            return StreamSupport.stream(spliterator, isParallel());
        } else {
            return StreamSupport.stream(spliterator, isParallel()).onClose(() -> close(closeHandlers()));
        }
    }

    /**
     * Returns {@code true} if the underlying iterator has no more elements.
     *
     * @return {@code true} if this stream is empty
     */
    @Override
    protected boolean isEmpty() {
        return !elements.hasNext();
    }
}
