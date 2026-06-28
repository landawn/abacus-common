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
import java.util.Deque;
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
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharToIntFunction;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.function.ObjCharConsumer;

/**
 * An iterator-based implementation of CharStream that processes char elements sequentially.
 * This class serves as the default sequential stream implementation for char values,
 * wrapping a CharIterator to provide stream operations.
 *
 * <p>This is an internal implementation class. Users should create streams through
 * the public CharStream factory methods rather than instantiating this class directly.
 *
 * @see CharStream
 * @see CharIteratorEx
 */
class IteratorCharStream extends AbstractCharStream {
    final CharIteratorEx elements;

    //    OptionalChar head;
    //    CharStream tail;

    //    CharStream head2;
    //    OptionalChar tail2;

    /**
     * Constructs an IteratorCharStream from a CharIterator.
     * Creates an unsorted stream with no close handlers.
     *
     * @param values the char iterator to wrap as a stream
     */
    IteratorCharStream(final CharIterator values) {
        this(values, null);
    }

    /**
     * Constructs an IteratorCharStream from a CharIterator with close handlers.
     * Creates an unsorted stream that will execute the provided close handlers when closed.
     *
     * @param values the char iterator to wrap as a stream
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorCharStream(final CharIterator values, final Collection<LocalRunnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    /**
     * Constructs an IteratorCharStream from a CharIterator with sorting and close handlers.
     * This is the primary constructor that all other constructors delegate to.
     *
     * @param values the char iterator to wrap as a stream
     * @param sorted {@code true} if the elements are already sorted in natural order, {@code false} otherwise
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorCharStream(final CharIterator values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        CharIteratorEx tmp = null;

        if (values instanceof CharIteratorEx) {
            tmp = (CharIteratorEx) values;
        } else {
            tmp = new CharIteratorEx() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public char nextChar() {
                    return values.nextChar();
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
     * @return a new {@code CharStream} containing only matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream filter(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private char next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.nextChar();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public char nextChar() {
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
     * @return a new {@code CharStream} of elements from the start while {@code predicate} is satisfied
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream takeWhile(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean hasMore = true;
            private boolean hasNext = false;
            private char next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext && hasMore && elements.hasNext()) {
                    next = elements.nextChar();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public char nextChar() {
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
     * @return a new {@code CharStream} with leading elements dropped while {@code predicate} holds
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream dropWhile(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private char next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextChar();

                            if (!predicate.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.nextChar();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public char nextChar() {
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
     * @return a new {@code CharStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream map(final CharUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public char nextChar() throws IllegalArgumentException {
                return mapper.applyAsChar(elements.nextChar());
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
    public IntStream mapToInt(final CharToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() throws IllegalArgumentException {
                return mapper.applyAsInt(elements.nextChar());
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
    public <T> Stream<T> mapToObj(final CharFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException {
                return mapper.apply(elements.nextChar());
            }

        }, false, null);
    }

    /**
     * Returns a stream consisting of the results of replacing each element of this stream
     * with the contents of a mapped stream. Lazily iterates the underlying iterator;
     * each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code CharStream}
     * @return a new {@code CharStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatMap(final CharFunction<? extends CharStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.nextChar());

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
            public void closeResource() throws IllegalStateException {
                if (closeHandle != null) {
                    StreamBase.close(closeHandle);
                }
            }
        };

        return newStream(iter, false, mergeCloseHandlers(iter::closeResource, closeHandlers())); //NOSONAR
    }

    /**
     * Returns a stream consisting of the results of replacing each element with the contents
     * of a mapped {@code Collection<Character>}. Lazily iterates the underlying iterator.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a collection of chars
     * @return a new {@code CharStream} formed by concatenating the mapped collections
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatmap(final CharFunction<? extends Collection<Character>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private Iterator<Character> cur = null;
            private Collection<Character> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextChar());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public char nextChar() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Character v = cur.next();
                return v == null ? (char) 0 : v;
            }
        }, false);
    }

    /**
     * Returns a stream formed by replacing each element of this stream with the contents
     * of a mapped {@code char[]} array. Lazily iterates the underlying iterator.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code char[]}
     * @return a new {@code CharStream} formed by concatenating the mapped arrays
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatMapArray(final CharFunction<char[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArray(mapper);
        }

        return newStream(new CharIteratorEx() { //NOSONAR
            private char[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.nextChar());
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

    /**
     * Returns an {@code IntStream} formed by replacing each element of this stream with the contents
     * of a mapped {@code IntStream}. Lazily iterates the underlying iterator;
     * each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to an {@code IntStream}
     * @return a new {@code IntStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final CharFunction<? extends IntStream> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.nextChar());

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
     * Returns a {@code Stream} formed by replacing each element of this stream with the contents
     * of a mapped {@code Stream<T>}. Lazily iterates the underlying iterator;
     * each mapped stream is closed after its contents are consumed.
     *
     * @param <T> the type of elements in the resulting stream
     * @param mapper a non-interfering, stateless function mapping each element to a {@code Stream<T>}
     * @return a new {@code Stream<T>} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatMapToObj(final CharFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
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

                        s = mapper.apply(elements.nextChar());

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
     * Returns a {@code Stream} formed by replacing each element of this stream with the contents
     * of a mapped {@code Collection<T>}. Lazily iterates the underlying iterator.
     *
     * @param <T> the type of elements in the resulting stream
     * @param mapper a non-interfering, stateless function mapping each element to a collection of objects
     * @return a new {@code Stream<T>} formed by concatenating the mapped collections
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatmapToObj(final CharFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<? extends T> cur = null;
            private Collection<? extends T> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextChar());
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
     * @return a new {@code CharStream} containing only distinct elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new CharIteratorEx() { //NOSONAR
                private boolean hasNext = false;
                private char prev = 0;
                private char next = 0;
                private boolean isFirst = true;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (elements.hasNext()) {
                            next = elements.nextChar();

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
                public char nextChar() {
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
     * @return a new {@code CharStream} containing at most {@code maxSize} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    @Override
    public CharStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        return newStream(new CharIteratorEx() { //NOSONAR
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public char nextChar() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;
                return elements.nextChar();
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
     * @return a new {@code CharStream} with the first {@code n} elements skipped
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public CharStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        //    if (n == 0) {
        //        return newStream(elements, isSorted());
        //    }

        if (n == 0) {
            return this;
        }

        return newStream(new CharIteratorEx() { //NOSONAR
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
            public char nextChar() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.nextChar();
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
            public char[] toArray() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.toArray();
            }
        }, isSorted());
    }

    /**
     * Returns a stream that performs the given action on each element as it is consumed,
     * primarily for debugging purposes. Lazily iterates the underlying iterator.
     *
     * @param action a non-interfering action to perform on each element as it is pulled
     * @return a new {@code CharStream} that applies the action to each element
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream onEach(final CharConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public char nextChar() {
                final char next = elements.nextChar();

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
    public <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.nextChar());
            }
        } finally {
            close();
        }
    }

    /**
     * Returns all remaining elements of this stream as a {@code char[]}, optionally closing the stream.
     * This is an internal method used by stream infrastructure.
     *
     * @param closeStream if {@code true}, the stream is closed after the array is collected
     * @return a {@code char[]} containing all remaining elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected char[] toArray(final boolean closeStream) {
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
     * Collects all elements of this stream into a {@code CharList} and closes the stream.
     *
     * @return a {@code CharList} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharList toCharList() throws IllegalStateException {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@code List<Character>} and closes the stream.
     *
     * @return a {@code List<Character>} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public List<Character> toList() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofList());
    }

    /**
     * Collects all elements of this stream into a {@code Set<Character>} and closes the stream.
     *
     * @return a {@code Set<Character>} containing the distinct elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Set<Character> toSet() throws IllegalStateException {
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
    public <C extends Collection<Character>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextChar());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@code Multiset<Character>} and closes the stream.
     * Elements with equal values will have their counts incremented accordingly.
     *
     * @return a {@code Multiset<Character>} representing element frequencies in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Character> toMultiset() throws IllegalStateException {
        assertNotClosed();

        return toMultiset(Suppliers.ofMultiset());
    }

    /**
     * Collects all elements of this stream into a {@code Multiset<Character>} created by the given supplier,
     * then closes the stream.
     *
     * @param supplier a function that returns a new, empty {@code Multiset} to populate
     * @return the {@code Multiset<Character>} populated with all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Character> toMultiset(final Supplier<? extends Multiset<Character>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Character> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextChar());
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.CharFunction<? extends K, E> keyMapper,
            final Throwables.CharFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            char next = 0;

            while (elements.hasNext()) {
                next = elements.nextChar();
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
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.CharFunction<? extends K, E> keyMapper,
            final Collector<? super Character, ?, D> downstream, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super Character> downstreamAccumulator = (BiConsumer<Object, ? super Character>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final Map<K, Object> intermediate = (Map<K, Object>) result;
            K key = null;
            Object v = null;
            char next = 0;

            while (elements.hasNext()) {
                next = elements.nextChar();
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
    public char reduce(final char identity, final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            char result = identity;

            while (elements.hasNext()) {
                result = accumulator.applyAsChar(result, elements.nextChar());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a reduction on the elements of this stream using an associative accumulation function,
     * and returns an {@code OptionalChar} describing the reduced value, or empty if the stream is empty.
     * Consumes the entire underlying iterator. Closes the stream.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalChar} containing the result, or empty if the stream has no elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar reduce(final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalChar.empty();
            }

            char result = elements.nextChar();

            while (elements.hasNext()) {
                result = accumulator.applyAsChar(result, elements.nextChar());
            }

            return OptionalChar.of(result);
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
     * CharList list = stream.collect(CharList::new, CharList::add, CharList::addAll);
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
    public <R> R collect(final Supplier<R> supplier, final ObjCharConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
            throws IllegalStateException {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.nextChar());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns the minimum element of this stream, or an empty {@code OptionalChar} if the stream is empty.
     * If the stream is sorted, the first element is returned without full iteration;
     * otherwise all elements are scanned. Closes the stream.
     *
     * @return an {@code OptionalChar} containing the minimum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalChar.empty();
            } else if (isSorted()) {
                return OptionalChar.of(elements.nextChar());
            }

            char candidate = elements.nextChar();
            char next = 0;

            while (elements.hasNext()) {
                next = elements.nextChar();

                if (next < candidate) {
                    candidate = next;
                }
            }

            return OptionalChar.of(candidate);
        } finally {
            close();
        }
    }

    /**
     * Returns the maximum element of this stream, or an empty {@code OptionalChar} if the stream is empty.
     * If the stream is sorted, the entire iterator is consumed to reach the last element;
     * otherwise all elements are scanned. Closes the stream.
     *
     * @return an {@code OptionalChar} containing the maximum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalChar.empty();
            } else if (isSorted()) {
                char next = 0;

                while (elements.hasNext()) {
                    next = elements.nextChar();
                }

                return OptionalChar.of(next);
            }

            char candidate = elements.nextChar();
            char next = 0;

            while (elements.hasNext()) {
                next = elements.nextChar();

                if (next > candidate) {
                    candidate = next;
                }
            }

            return OptionalChar.of(candidate);
        } finally {
            close();
        }
    }

    /**
     * Returns the k-th largest element of this stream (1-based), or an empty {@code OptionalChar}
     * if the stream contains fewer than {@code k} elements.
     * The underlying iterator is fully consumed. Closes the stream.
     *
     * @param k the 1-based rank of the desired largest element (e.g. 1 = largest, 2 = second largest)
     * @return an {@code OptionalChar} containing the k-th largest element, or empty if there are fewer than {@code k} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    @Override
    public OptionalChar kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (isSorted()) {
                // For sorted ascending, kthLargest = (k-1)th from the end.
                // Use a ring buffer of size k while draining the iterator.
                char[] window = null;
                int idx = 0;
                int size = 0;
                while (elements.hasNext()) {
                    final char v = elements.nextChar();
                    if (window == null) {
                        window = new char[k];
                    }
                    if (size < k) {
                        window[size++] = v;
                    } else {
                        window[idx] = v;
                        idx = (idx + 1) % k;
                    }
                }
                if (size < k) {
                    return OptionalChar.empty();
                }
                return OptionalChar.of(window[idx]);
            }

            if (!elements.hasNext()) {
                return OptionalChar.empty();
            }

            @SuppressWarnings("resource")
            final Optional<Character> optional = boxed().kthLargest(k, CHAR_COMPARATOR);

            return optional.isPresent() ? OptionalChar.of(optional.get()) : OptionalChar.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the sum of the char values (treated as unsigned integers) in this stream.
     * The underlying iterator is fully consumed. Closes the stream.
     *
     * @return the integer sum of all elements in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public int sum() throws IllegalStateException {
        assertNotClosed();

        try {
            long result = 0;

            while (elements.hasNext()) {
                result += elements.nextChar();
            }

            return Numbers.toIntExact(result);
        } finally {
            close();
        }
    }

    /**
     * Returns the arithmetic average of the elements in this stream as an {@code OptionalDouble},
     * or an empty optional if the stream is empty.
     * The underlying iterator is fully consumed. Closes the stream.
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
                sum += elements.nextChar();
                count++;
            } while (elements.hasNext());

            return OptionalDouble.of(((double) sum) / count);
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
     * Returns a {@code CharSummaryStatistics} describing various aggregate values (count, sum, min, max, average)
     * of the elements in this stream. The underlying iterator is fully consumed. Closes the stream.
     *
     * @return a {@code CharSummaryStatistics} for this stream's elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final CharSummaryStatistics result = new CharSummaryStatistics();

            while (elements.hasNext()) {
                result.accept(elements.nextChar());
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
    public <E extends Exception> boolean anyMatch(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextChar())) {
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
    public <E extends Exception> boolean allMatch(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (!predicate.test(elements.nextChar())) {
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
    public <E extends Exception> boolean noneMatch(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextChar())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    /**
     * Returns the first element that matches the given predicate as an {@code OptionalChar},
     * or empty if no element matches. Short-circuits as soon as a matching element is found.
     * Closes the stream.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to stream elements
     * @return an {@code OptionalChar} containing the first matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> OptionalChar findFirst(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                final char element = elements.nextChar();

                if (predicate.test(element)) {
                    return OptionalChar.of(element);
                }
            }
        } finally {
            close();
        }

        return OptionalChar.empty();
    }

    /**
     * Returns the last element that matches the given predicate as an {@code OptionalChar},
     * or empty if no element matches. The entire underlying iterator is consumed to find the last match.
     * Closes the stream.
     *
     * @param <E> the type of exception the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to stream elements
     * @return an {@code OptionalChar} containing the last matching element, or empty if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws
     */
    @Override
    public <E extends Exception> OptionalChar findLast(final Throwables.CharPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalChar.empty();
            }

            boolean hasResult = false;
            char element = 0;
            char result = 0;

            while (elements.hasNext()) {
                element = elements.nextChar();

                if (predicate.test(element)) {
                    result = element;
                    hasResult = true;
                }
            }

            return hasResult ? OptionalChar.of(result) : OptionalChar.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns an {@code IntStream} consisting of the {@code int} values of the elements of this stream.
     * Each {@code char} element is widened to an {@code int} without sign-extension.
     * Lazily iterates the underlying iterator.
     *
     * @return an {@code IntStream} with the same elements widened to {@code int}
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
                return elements.nextChar();
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
     * Returns the underlying {@code CharIteratorEx} for this stream.
     * This is an internal method used by stream infrastructure.
     *
     * @return the underlying {@code CharIteratorEx}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    CharIteratorEx iteratorEx() {
        assertNotClosed();

        return elements;
    }

    /**
     * Returns a stream with the same elements as this stream if it is non-empty; otherwise the supplier is invoked
     * and a stream of the supplied stream's elements is returned.
     * The supplier is not invoked until the stream is consumed. Closes any produced stream on close.
     *
     * @param supplier a supplier of a substitute {@code CharStream} to use when this stream is empty
     * @return a stream of this stream's elements if non-empty, or of the elements of the stream produced by the supplier
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream appendIfEmpty(final Supplier<? extends CharStream> supplier) throws IllegalStateException {
        assertNotClosed();

        final Holder<CharStream> holder = new Holder<>();

        return newStream(new CharIteratorEx() { //NOSONAR
            private CharIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                if (iter == null) {
                    init();
                }

                return iter.nextChar();
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
                        final CharStream s = supplier.get();
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
     * @return a {@code CharStream} that invokes {@code action} once on iteration if empty, then proceeds normally
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private CharIteratorEx iter;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public char nextChar() {
                if (iter == null) {
                    init();
                }

                return iter.nextChar();
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
     * Returns a new {@link ParallelIteratorCharStream} that processes this stream's elements
     * in parallel using the specified configuration.
     *
     * @param maxThreadNum the maximum number of threads to use
     * @param splitor the strategy for splitting the work among threads
     * @param asyncExecutor the executor for asynchronous parallel tasks
     * @param cancelUncompletedThreads whether to cancel incomplete threads when the stream is closed
     * @return a parallel {@code CharStream} backed by the same iterator
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected CharStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelIteratorCharStream(elements, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
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
