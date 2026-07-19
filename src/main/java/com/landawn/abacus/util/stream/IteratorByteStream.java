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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteToIntFunction;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.ObjByteConsumer;

/**
 * An iterator-based implementation of ByteStream that processes byte elements sequentially.
 * This class serves as the default sequential stream implementation for byte values,
 * wrapping a ByteIterator to provide stream operations.
 *
 * <p>This is an internal implementation class. Users should create streams through
 * the public ByteStream factory methods rather than instantiating this class directly.
 *
 * @see ByteStream
 * @see ByteIteratorEx
 */
class IteratorByteStream extends AbstractByteStream {
    final ByteIteratorEx elements;

    //    OptionalByte head;
    //    ByteStream tail;

    //    ByteStream head2;
    //    OptionalByte tail2;

    /**
     * Constructs an IteratorByteStream from a ByteIterator.
     * Creates an unsorted stream with no close handlers.
     *
     * @param values the byte iterator to wrap as a stream
     */
    IteratorByteStream(final ByteIterator values) {
        this(values, null);
    }

    /**
     * Constructs an IteratorByteStream from a ByteIterator with close handlers.
     * Creates an unsorted stream that will execute the provided close handlers when closed.
     *
     * @param values the byte iterator to wrap as a stream
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorByteStream(final ByteIterator values, final Collection<LocalRunnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    /**
     * Constructs an IteratorByteStream from a ByteIterator with sorting and close handlers.
     * This is the primary constructor that all other constructors delegate to.
     *
     * @param values the byte iterator to wrap as a stream
     * @param sorted {@code true} if the elements are already sorted in natural order, {@code false} otherwise
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorByteStream(final ByteIterator values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        ByteIteratorEx tmp = null;

        if (values instanceof ByteIteratorEx) {
            tmp = (ByteIteratorEx) values;
        } else {
            tmp = new ByteIteratorEx() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public byte nextByte() {
                    return values.nextByte();
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
     * @return a new {@code ByteStream} containing only matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream filter(final BytePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.nextByte();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public byte nextByte() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        }, isSorted());
    }

    /**
     * Returns a stream of elements taken from this stream as long as the given predicate holds {@code true}.
     * Once an element fails the predicate, the stream terminates immediately. Lazily pulls from
     * the underlying iterator.
     *
     * @param predicate the non-interfering, stateless predicate applied to each element
     * @return a new {@code ByteStream} of the initial matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream takeWhile(final BytePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean hasMore = true;
            private boolean hasNext = false;
            private byte next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext && hasMore && elements.hasNext()) {
                    next = elements.nextByte();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public byte nextByte() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted());
    }

    /**
     * Returns a stream that skips elements as long as the given predicate is {@code true}, then includes
     * all remaining elements. Once the predicate returns {@code false} for the first time, all
     * subsequent elements are included without further predicate evaluation.
     *
     * @param predicate the non-interfering, stateless predicate applied during the drop phase
     * @return a new {@code ByteStream} with the initial matching prefix removed
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream dropWhile(final BytePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private byte next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextByte();

                            if (!predicate.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.nextByte();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public byte nextByte() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, isSorted());
    }

    /**
     * Returns a stream consisting of the results of applying the given function to each element.
     * Each byte element consumed from the underlying iterator is transformed by {@code mapper}.
     *
     * @param mapper the non-interfering, stateless function to apply to each element
     * @return a new {@code ByteStream} containing the mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream map(final ByteUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public byte nextByte() throws IllegalArgumentException {
                return mapper.applyAsByte(elements.nextByte());
            }

        }, false);
    }

    /**
     * Returns an {@code IntStream} consisting of the results of applying the given
     * {@code byte}-to-{@code int} mapping function to each element of this stream.
     *
     * @param mapper the non-interfering, stateless function to apply to each element
     * @return a new {@code IntStream} containing the mapped int values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final ByteToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() throws IllegalArgumentException {
                return mapper.applyAsInt(elements.nextByte());
            }

        }, false);
    }

    /**
     * Returns an object-valued {@code Stream} consisting of the results of applying the given
     * function to each element of this stream. Each byte element is mapped to a reference type {@code T}.
     *
     * @param <T> the type of the elements of the returned stream
     * @param mapper the non-interfering, stateless function to apply to each element
     * @return a new {@code Stream<T>} containing the mapped object values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> mapToObj(final ByteFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException {
                return mapper.apply(elements.nextByte());
            }

        }, false, null);
    }

    /**
     * Returns a stream formed by replacing each element with the contents of a mapped
     * {@code ByteStream}. Mapped streams are closed after their contents are consumed.
     * {@code null} mapped streams are treated as empty.
     *
     * @param mapper the non-interfering, stateless function returning a {@code ByteStream} for each element
     * @return a new {@code ByteStream} consisting of the concatenated mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream flatMap(final ByteFunction<? extends ByteStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = new ByteIteratorEx() {
            private ByteIterator cur = null;
            private ByteStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextByte());

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
            public byte nextByte() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextByte();
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

    /**
     * Returns a stream formed by replacing each element with the elements of a mapped
     * {@code Collection<Byte>}. {@code null} or empty collections contribute no elements.
     *
     * @param mapper the non-interfering, stateless function returning a collection for each element
     * @return a new {@code ByteStream} consisting of the concatenated collection elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream flatmap(final ByteFunction<? extends Collection<Byte>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private Iterator<Byte> cur = null;
            private Collection<Byte> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextByte());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public byte nextByte() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Byte v = cur.next();
                return v == null ? (byte) 0 : v;
            }
        }, false);
    }

    /**
     * Returns a stream formed by replacing each element with the elements of a mapped
     * {@code byte[]} array. {@code null} or empty arrays contribute no elements.
     * For parallel streams the superclass implementation is used.
     *
     * @param mapper the non-interfering, stateless function returning a {@code byte[]} for each element
     * @return a new {@code ByteStream} consisting of the concatenated array elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream flatMapArray(final ByteFunction<byte[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArray(mapper);
        }

        return newStream(new ByteIteratorEx() { //NOSONAR
            private byte[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.nextByte());
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

    /**
     * Returns an {@code IntStream} formed by replacing each element with the contents of a mapped
     * {@code IntStream}. Mapped streams are closed after their contents are consumed.
     * {@code null} mapped streams are treated as empty.
     *
     * @param mapper the non-interfering, stateless function returning an {@code IntStream} for each element
     * @return a new {@code IntStream} consisting of the concatenated mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final ByteFunction<? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = new IntIteratorEx() {
            private IntIterator cur = null;
            private IntStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextByte());

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

    /**
     * Returns an object-valued {@code Stream<T>} formed by replacing each element with the
     * contents of a mapped {@code Stream<? extends T>}. Mapped streams are closed after
     * their contents are consumed. {@code null} mapped streams are treated as empty.
     *
     * @param <T> the type of elements in the returned stream
     * @param mapper the non-interfering, stateless function returning a {@code Stream} for each element
     * @return a new {@code Stream<T>} consisting of the concatenated mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatMapToObj(final ByteFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() {
            private Iterator<? extends T> cur = null;
            private Stream<? extends T> s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextByte());

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

    /**
     * Returns an object-valued {@code Stream<T>} formed by replacing each element with
     * the elements of a mapped {@code Collection<? extends T>}. {@code null} or empty
     * collections contribute no elements.
     *
     * @param <T> the type of elements in the returned stream
     * @param mapper the non-interfering, stateless function returning a collection for each element
     * @return a new {@code Stream<T>} consisting of the concatenated collection elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatmapToObj(final ByteFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<? extends T> cur = null;
            private Collection<? extends T> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextByte());
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
     * Returns a stream consisting of the distinct elements of this stream.
     * If the stream is already sorted, an optimized adjacent-check algorithm is used;
     * otherwise a hash set is used to track seen elements.
     *
     * @return a new {@code ByteStream} without duplicate elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new ByteIteratorEx() { //NOSONAR
                private boolean hasNext = false;
                private byte prev = 0;
                private byte next = 0;
                private boolean isFirst = true;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (elements.hasNext()) {
                            next = elements.nextByte();

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
                public byte nextByte() {
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
     * Returns a stream consisting of no more than {@code maxSize} elements from the start
     * of this stream. Lazily pulls from the underlying iterator, stopping after
     * {@code maxSize} elements have been consumed.
     *
     * @param maxSize the maximum number of elements the returned stream may contain; must be &gt;= 0
     * @return a new {@code ByteStream} truncated to at most {@code maxSize} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    @Override
    public ByteStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        return newStream(new ByteIteratorEx() { //NOSONAR
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public byte nextByte() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;
                return elements.nextByte();
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
     * Returns a stream that skips the first {@code n} elements of this stream.
     * If the stream contains fewer than {@code n} elements, an empty stream is returned.
     * The underlying iterator is advanced lazily when the returned stream is first consumed.
     *
     * @param n the number of leading elements to skip; must be &gt;= 0
     * @return a new {@code ByteStream} with the first {@code n} elements omitted
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public ByteStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        //    if (n == 0) {
        //        return newStream(elements, isSorted());
        //    }

        if (n == 0) {
            return this;
        }

        return newStream(new ByteIteratorEx() { //NOSONAR
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
            public byte nextByte() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.nextByte();
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
            public byte[] toArray() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.toArray();
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of the elements of this stream, additionally performing
     * the provided action on each element as elements are consumed from the resulting stream.
     * This is primarily a debugging aid; the action is applied in encounter order.
     *
     * @param action the non-interfering action to perform on each element as it is consumed
     * @return a new {@code ByteStream} with the side-effect action attached
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream onEach(final ByteConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public byte nextByte() {
                final byte next = elements.nextByte();

                action.accept(next);
                return next;
            }
        }, isSorted());
    }

    /**
     * Performs the given action for each element of this stream in encounter order,
     * then closes the stream. Any exception thrown by the action is propagated to the caller.
     *
     * @param <E> the type of exception the action may throw
     * @param action the non-interfering action to perform on each element
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @Override
    public <E extends Exception> void forEach(final Throwables.ByteConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.nextByte());
            }
        } finally {
            close();
        }
    }

    /**
     * Collects all elements from the underlying iterator into a new {@code byte[]}.
     * Optionally closes the stream after collecting.
     *
     * @param closeStream {@code true} to close the stream after the array is collected
     * @return a new {@code byte[]} containing the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected byte[] toArray(final boolean closeStream) {
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
     * Collects the elements of this stream into a {@code ByteList} and closes the stream.
     *
     * @return a {@code ByteList} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteList toByteList() throws IllegalStateException {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    /**
     * Collects the elements of this stream into a {@code List<Byte>} and closes the stream.
     *
     * @return a mutable {@code List<Byte>} containing all elements of this stream in encounter order
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public List<Byte> toList() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofList());
    }

    /**
     * Collects the elements of this stream into a {@code Set<Byte>} and closes the stream.
     * Duplicate elements are silently discarded.
     *
     * @return a mutable {@code Set<Byte>} containing the distinct elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Set<Byte> toSet() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofSet());
    }

    /**
     * Collects the elements of this stream into a collection created by the given supplier,
     * then closes the stream.
     *
     * @param <C> the type of the resulting collection
     * @param supplier a function that returns a new, empty collection into which results are inserted
     * @return the collection populated with all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <C extends Collection<Byte>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements of this stream into a {@code Multiset<Byte>} and closes the stream.
     *
     * @return a {@code Multiset<Byte>} representing element frequencies in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Byte> toMultiset() throws IllegalStateException {
        assertNotClosed();

        return toMultiset(Suppliers.ofMultiset());
    }

    /**
     * Collects the elements of this stream into a {@code Multiset<Byte>} created by the given
     * supplier, then closes the stream.
     *
     * @param supplier a function that returns a new, empty {@code Multiset} to populate
     * @return the {@code Multiset<Byte>} populated with all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Byte> toMultiset(final Supplier<? extends Multiset<Byte>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Byte> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements of this stream into a map and closes the stream. Each element is
     * mapped to a key via {@code keyMapper} and a value via {@code valueMapper}. When two
     * elements map to the same key, {@code mergeFunction} resolves the conflict.
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Throwables.ByteFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            byte next = 0;

            while (elements.hasNext()) {
                next = elements.nextByte();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Groups the elements of this stream by a key extracted by {@code keyMapper}, applies a
     * downstream collector to each group, and closes the stream.
     *
     * <p>Keys must not be {@code null}; a {@code null} key causes an {@code IllegalArgumentException}.
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
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.ByteFunction<? extends K, E> keyMapper,
            final Collector<? super Byte, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super Byte> downstreamAccumulator = (BiConsumer<Object, ? super Byte>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final Map<K, Object> intermediate = (Map<K, Object>) result;

            K key = null;
            Object v = null;
            byte next = 0;

            while (elements.hasNext()) {
                next = elements.nextByte();
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
     * and an associative accumulation function, and returns the reduced value. Closes the stream.
     *
     * @param identity the identity value for the accumulating function
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return the result of the reduction
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public byte reduce(final byte identity, final ByteBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            byte result = identity;

            while (elements.hasNext()) {
                result = accumulator.applyAsByte(result, elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a reduction on the elements of this stream using an associative accumulation
     * function, returning an {@code OptionalByte} describing the result, or an empty optional
     * if the stream is empty. Closes the stream.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalByte} with the reduction result, or empty if stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalByte reduce(final ByteBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalByte.empty();
            }

            byte result = elements.nextByte();

            while (elements.hasNext()) {
                result = accumulator.applyAsByte(result, elements.nextByte());
            }

            return OptionalByte.of(result);
        } finally {
            close();
        }
    }

    /**
     * Performs a mutable reduction on the elements of this stream and closes the stream.
     * The {@code combiner} is provided for API compatibility but is not used in sequential execution.
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new result container
     * @param accumulator a function that folds an element into the result container
     * @param combiner a function that combines two result containers (used in parallel)
     * @return the populated result container
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjByteConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns the minimum element of this stream, or an empty optional if the stream is empty.
     * If the stream is already sorted, the first element is returned directly; otherwise a
     * linear scan is performed. Closes the stream.
     *
     * @return an {@code OptionalByte} containing the minimum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalByte min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalByte.empty();
            } else if (isSorted()) {
                return OptionalByte.of(elements.nextByte());
            }

            byte candidate = elements.nextByte();
            byte next = 0;

            while (elements.hasNext()) {
                next = elements.nextByte();

                if (next < candidate) {
                    candidate = next;
                }
            }

            return OptionalByte.of(candidate);
        } finally {
            close();
        }
    }

    /**
     * Returns the maximum element of this stream, or an empty optional if the stream is empty.
     * If the stream is already sorted, the last element is returned by exhausting the iterator;
     * otherwise a linear scan is performed. Closes the stream.
     *
     * @return an {@code OptionalByte} containing the maximum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalByte max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalByte.empty();
            } else if (isSorted()) {
                byte next = 0;

                while (elements.hasNext()) {
                    next = elements.nextByte();
                }

                return OptionalByte.of(next);
            }

            byte candidate = elements.nextByte();
            byte next = 0;

            while (elements.hasNext()) {
                next = elements.nextByte();

                if (next > candidate) {
                    candidate = next;
                }
            }

            return OptionalByte.of(candidate);
        } finally {
            close();
        }
    }

    /**
     * Returns the k-th largest element of this stream, or an empty optional if the stream
     * contains fewer than {@code k} elements. The iterator is fully consumed. Closes the stream.
     *
     * @param k the rank of the desired element (1 = largest); must be positive
     * @return an {@code OptionalByte} containing the k-th largest element, or empty if not enough elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code k} is less than 1
     */
    @Override
    public OptionalByte kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (isSorted()) {
                // For sorted ascending, kthLargest = (k-1)th from the end.
                // Use a ring buffer of size k while draining the iterator.
                byte[] window = null;
                int idx = 0;
                int size = 0;
                while (elements.hasNext()) {
                    final byte v = elements.nextByte();
                    if (window == null) {
                        window = new byte[Math.min(k, 16)];
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
                    return OptionalByte.empty();
                }
                return OptionalByte.of(window[idx]);
            }

            if (!elements.hasNext()) {
                return OptionalByte.empty();
            }

            @SuppressWarnings("resource")
            final Optional<Byte> optional = boxed().kthLargest(k, BYTE_COMPARATOR);

            return optional.isPresent() ? OptionalByte.of(optional.get()) : OptionalByte.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the sum of all elements in this stream as an {@code int}. The sum is
     * accumulated as a {@code long} to avoid intermediate overflow, then converted.
     * Returns 0 if the stream is empty. Closes the stream.
     *
     * @return the sum of all elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public int sum() throws IllegalStateException {
        assertNotClosed();

        try {
            long result = 0;

            while (elements.hasNext()) {
                result += elements.nextByte();
            }

            return Numbers.toIntExact(result);
        } finally {
            close();
        }
    }

    /**
     * Returns the arithmetic mean of all elements as an {@code OptionalDouble},
     * or an empty optional if the stream is empty. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the average, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
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

            do {
                sum += elements.nextByte();
                count++;
            } while (elements.hasNext());

            return OptionalDouble.of(((double) sum) / count);
        } finally {
            close();
        }
    }

    /**
     * Returns the count of elements in this stream and closes the stream.
     * The underlying iterator is fully consumed to determine the count.
     *
     * @return the number of elements in this stream
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
     * Returns a {@code ByteSummaryStatistics} describing various summary data about the
     * elements of this stream (count, sum, min, max, average), then closes the stream.
     *
     * @return a {@code ByteSummaryStatistics} for all elements in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final ByteSummaryStatistics result = new ByteSummaryStatistics();

            while (elements.hasNext()) {
                result.accept(elements.nextByte());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns {@code true} if any element of this stream matches the provided predicate.
     * Short-circuits on the first match. Closes the stream after evaluation.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate the predicate to apply to elements
     * @return {@code true} if at least one element matches, {@code false} otherwise (including empty stream)
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextByte())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    /**
     * Returns {@code true} if all elements of this stream match the provided predicate.
     * Short-circuits on the first non-match. Returns {@code true} for empty streams.
     * Closes the stream after evaluation.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate the predicate to apply to elements
     * @return {@code true} if all elements match (or the stream is empty), {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (!predicate.test(elements.nextByte())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    /**
     * Returns {@code true} if no element of this stream matches the provided predicate.
     * Short-circuits on the first match. Returns {@code true} for empty streams.
     * Closes the stream after evaluation.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate the predicate to apply to elements
     * @return {@code true} if no elements match (or the stream is empty), {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextByte())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    /**
     * Returns the first element matching the given predicate as an {@code OptionalByte},
     * or an empty optional if no match is found. Short-circuits on the first match.
     * Closes the stream after evaluation.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate the predicate to apply to elements
     * @return an {@code OptionalByte} with the first matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> OptionalByte findFirst(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                final byte element = elements.nextByte();

                if (predicate.test(element)) {
                    return OptionalByte.of(element);
                }
            }
        } finally {
            close();
        }

        return OptionalByte.empty();
    }

    /**
     * Returns the last element matching the given predicate as an {@code OptionalByte},
     * or an empty optional if no match is found. The entire stream must be consumed to
     * find the last match. Closes the stream after evaluation.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate the predicate to apply to elements
     * @return an {@code OptionalByte} with the last matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> OptionalByte findLast(final Throwables.BytePredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalByte.empty();
            }

            boolean hasResult = false;
            byte element = 0;
            byte result = 0;

            while (elements.hasNext()) {
                element = elements.nextByte();

                if (predicate.test(element)) {
                    result = element;
                    hasResult = true;
                }
            }

            return hasResult ? OptionalByte.of(result) : OptionalByte.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns an {@code IntStream} that contains the same elements as this stream,
     * with each byte value widened to an {@code int}. The resulting stream preserves
     * the sorted order of the original if applicable.
     *
     * @return an {@code IntStream} consisting of the elements of this stream widened to {@code int}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream asIntStream() throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() {
                return elements.nextByte();
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

    /**
     * Returns the underlying {@code ByteIteratorEx} for this stream. This is an internal
     * method used by the stream implementation to access the wrapped iterator directly.
     *
     * @return the underlying {@code ByteIteratorEx}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    ByteIteratorEx iteratorEx() {
        assertNotClosed();

        return elements;
    }

    /**
     * Returns this stream if it is non-empty, or the stream produced by the given supplier
     * if this stream is empty. The supplier's stream is only invoked lazily when needed.
     * The returned stream carries a close handler that closes the supplier's stream.
     *
     * @param supplier a supplier of a fallback {@code ByteStream} to use if this stream is empty
     * @return this stream if non-empty, or the fallback stream otherwise
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream appendIfEmpty(final Supplier<? extends ByteStream> supplier) throws IllegalStateException {
        assertNotClosed();

        final Holder<ByteStream> holder = new Holder<>();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private ByteIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (iter == null) {
                    init();
                }

                return iter.nextByte();
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
                        final ByteStream s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }
        }, false).onClose(() -> close(holder));
    }

    /**
     * Executes the given action if this stream is empty, then returns a stream with the same
     * elements as this stream. The action is invoked at most once, lazily when the returned
     * stream is consumed.
     *
     * @param action the action to run if this stream has no elements
     * @return a stream equivalent to this stream, with the side-effect action attached if empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private ByteIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public byte nextByte() {
                if (iter == null) {
                    init();
                }

                return iter.nextByte();
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
     * Creates a parallel version of this stream using the given thread pool configuration.
     * Returns a {@link ParallelIteratorByteStream} wrapping the same iterator.
     *
     * @param maxThreadNum the maximum number of threads to use for parallel execution
     * @param splitor the strategy for splitting work across threads
     * @param asyncExecutor the executor to use for parallel tasks
     * @param cancelUncompletedThreads whether to cancel incomplete threads when the stream is closed
     * @return a parallel {@code ByteStream} wrapping the same iterator
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected ByteStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelIteratorByteStream(elements, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    /**
     * Returns {@code true} if the underlying iterator has no more elements.
     *
     * @return {@code true} if the stream is empty, {@code false} otherwise
     */
    @Override
    protected boolean isEmpty() {
        return !elements.hasNext();
    }
}
