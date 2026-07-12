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

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
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
 * An array-backed implementation of CharStream that provides efficient sequential access to a char array
 * or a portion of it. This implementation is optimized for working with primitive char arrays and offers
 * high performance for operations on contiguous char sequences.
 *
 * <p>Key features:
 * <ul>
 * <li>Zero-copy operations when possible, working directly on the underlying array</li>
 * <li>Support for partial array ranges via fromIndex and toIndex</li>
 * <li>Optimized implementations of filter, map, flatMap, and terminal operations</li>
 * <li>Efficient sorted stream handling with specialized algorithms</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * char[] data = {'a', 'b', 'c', 'd', 'e'};
 * CharStream stream = CharStream.of(data);
 * char max = stream.max().orElse('\0');
 * }</pre>
 *
 */
class ArrayCharStream extends AbstractCharStream {
    final char[] elements;
    final int fromIndex;
    final int toIndex;

    /**
     * Constructs an ArrayCharStream from the entire char array.
     *
     * @param values the char array to stream
     */
    ArrayCharStream(final char[] values) {
        this(values, 0, values.length);
    }

    /**
     * Constructs an ArrayCharStream from the entire char array with close handlers.
     *
     * @param values the char array to stream
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ArrayCharStream(final char[] values, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    /**
     * Constructs an ArrayCharStream from the entire char array with sorting state and close handlers.
     *
     * @param values the char array to stream
     * @param sorted whether the array elements are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ArrayCharStream(final char[] values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    /**
     * Constructs an ArrayCharStream from a range within the char array.
     *
     * @param values the char array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayCharStream(final char[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    /**
     * Constructs an ArrayCharStream from a range within the char array with close handlers.
     *
     * @param values the char array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @param closeHandlers handlers to execute when the stream is closed
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayCharStream(final char[] values, final int fromIndex, final int toIndex, final Collection<LocalRunnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    /**
     * Constructs an ArrayCharStream from a range within the char array with all configuration options.
     * This is the primary constructor that all other constructors delegate to.
     *
     * @param values the char array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @param sorted whether the array elements in the range are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayCharStream(final char[] values, final int fromIndex, final int toIndex, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, N.len(values));

        elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /**
     * Returns a stream consisting of elements that match the given predicate.
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
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                if (!hasNext && cursor < toIndex) {
                    do {
                        if (predicate.test(elements[cursor])) {
                            hasNext = true;
                            break;
                        }
                    } while (++cursor < toIndex);
                }

                return hasNext;
            }

            @Override
            public char nextChar() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    /**
     * Returns a stream of elements taken while the predicate holds; stops at the first failure.
     *
     * @param predicate the non-interfering, stateless predicate applied to each element
     * @return a new {@code CharStream} of the initial matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream takeWhile(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean hasMore = true;
            private boolean hasNext = false;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                if (!hasNext && hasMore && cursor < toIndex) {
                    if (predicate.test(elements[cursor])) {
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

                return elements[cursor++];
            }
        }, isSorted());
    }

    /**
     * Returns a stream that skips elements while the predicate is true, then includes all remaining elements.
     *
     * @param predicate the non-interfering, stateless predicate applied during the drop phase
     * @return a new {@code CharStream} with the initial matching prefix removed
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream dropWhile(final CharPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private int cursor = fromIndex;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (!hasNext && cursor < toIndex) {
                    if (!dropped) {
                        dropped = true;

                        do {
                            if (!predicate.test(elements[cursor])) {
                                hasNext = true;
                                break;
                            }
                        } while (++cursor < toIndex);
                    } else {
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

                return elements[cursor++];
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of every {@code step}-th element of this stream.
     *
     * @param step the distance between successive elements; must be positive
     * @return a new {@code CharStream} containing every {@code step}-th element
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code step} is less than 1
     */
    @Override
    public CharStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1 || fromIndex == toIndex) {
            return skip(0);
        }

        return newStream(new CharIteratorEx() { //NOSONAR
            private final int stepToUse = (int) N.min(step, Integer.MAX_VALUE);
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final char res = elements[cursor];
                cursor = cursor > toIndex - stepToUse ? toIndex : cursor + stepToUse;
                return res;
            }

            @Override
            public long count() {
                final long ret = (toIndex - cursor) % stepToUse == 0 ? (toIndex - cursor) / stepToUse : ((toIndex - cursor) / stepToUse) + 1;
                cursor = toIndex;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                cursor = n <= (toIndex - cursor) / stepToUse ? cursor + (int) (n * stepToUse) : toIndex;
            }

            @Override
            public char[] toArray() {
                final long rem = toIndex - cursor;
                final int len = (int) ((rem % stepToUse == 0) ? rem / stepToUse : rem / stepToUse + 1);
                final char[] a = new char[len];

                for (int i = 0; i < len; i++) {
                    a[i] = elements[cursor];
                    cursor = cursor > toIndex - stepToUse ? toIndex : cursor + stepToUse;
                }

                return a;
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of the results of applying the given function to each element.
     *
     * @param mapper the non-interfering, stateless function to apply to each element
     * @return a new {@code CharStream} containing the mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream map(final CharUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.applyAsChar(elements[cursor++]);
            }

            @Override
            public char[] toArray() {
                final char[] a = new char[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsChar(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    /**
     * Returns an {@code IntStream} consisting of the results of applying the given
     * {@code char}-to-{@code int} mapping function to each element of this stream.
     *
     * @param mapper the non-interfering, stateless function to apply to each element
     * @return a new {@code IntStream} containing the mapped int values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final CharToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.applyAsInt(elements[cursor++]);
            }

            @Override
            public int[] toArray() {
                final int[] a = new int[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsInt(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    /**
     * Returns an object-valued {@code Stream} by applying the given function to each element.
     *
     * @param <T> the type of the elements of the returned stream
     * @param mapper the non-interfering, stateless function to apply to each element
     * @return a new {@code Stream<T>} containing the mapped object values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> mapToObj(final CharFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.apply(elements[cursor++]);
            }

            @Override
            public <A> A[] toArray(A[] a) {
                final int len = toIndex - cursor;
                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                for (int i = 0; i < len; i++) {
                    a[i] = (A) mapper.apply(elements[cursor++]);
                }

                if (a.length > len) {
                    a[len] = null;
                }

                return a;
            }
        }, false, null);
    }

    /**
     * Returns a stream formed by replacing each element with the contents of a mapped {@code CharStream}.
     * Mapped streams are closed after their contents are consumed. {@code null} mapped streams are treated as empty.
     *
     * @param mapper the non-interfering, stateless function returning a {@code CharStream} for each element
     * @return a new {@code CharStream} consisting of the concatenated mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatMap(final CharFunction<? extends CharStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final CharIteratorEx iter = new CharIteratorEx() {
            private int cursor = fromIndex;
            private CharIterator cur = null;
            private CharStream s = null;
            private Deque<LocalRunnable> closeHandle = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    if (cursor < toIndex) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            StreamBase.close(tmp);
                        }

                        s = mapper.apply(elements[cursor++]);

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
     * Returns a stream formed by replacing each element with the elements of a mapped
     * {@code Collection<Character>}. {@code null} or empty collections contribute no elements.
     *
     * @param mapper the non-interfering, stateless function returning a collection for each element
     * @return a new {@code CharStream} consisting of the concatenated collection elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatmap(final CharFunction<? extends Collection<Character>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private Iterator<Character> cur = null;
            private Collection<Character> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && cursor < toIndex) {
                    c = mapper.apply(elements[cursor++]);
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
     * Returns a stream formed by replacing each element with the elements of a mapped {@code char[]}.
     * {@code null} or empty arrays contribute no elements. For parallel streams the superclass implementation is used.
     *
     * @param mapper the non-interfering, stateless function returning a {@code char[]} for each element
     * @return a new {@code CharStream} consisting of the concatenated array elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream flatMapArray(final CharFunction<char[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArray(mapper);
        }

        return newStream(new CharIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private char[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (cursor < toIndex) {
                        cur = mapper.apply(elements[cursor++]);
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
     * Returns an {@code IntStream} formed by replacing each element with the contents of a mapped {@code IntStream}.
     * Mapped streams are closed after their contents are consumed. {@code null} mapped streams are treated as empty.
     *
     * @param mapper the non-interfering, stateless function returning an {@code IntStream} for each element
     * @return a new {@code IntStream} consisting of the concatenated mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final CharFunction<? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = new IntIteratorEx() {
            private int cursor = fromIndex;
            private IntIterator cur = null;
            private IntStream s = null;
            private Deque<LocalRunnable> closeHandle = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    if (cursor < toIndex) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            StreamBase.close(tmp);
                        }

                        s = mapper.apply(elements[cursor++]);

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
     * Returns an object-valued {@code Stream<T>} formed by replacing each element with the contents of a mapped
     * {@code Stream<? extends T>}. Mapped streams are closed after consumption. {@code null} mapped streams are empty.
     *
     * @param <T> the type of elements in the returned stream
     * @param mapper the non-interfering, stateless function returning a {@code Stream} for each element
     * @return a new {@code Stream<T>} consisting of the concatenated mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatMapToObj(final CharFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() {
            private int cursor = fromIndex;
            private Iterator<? extends T> cur = null;
            private Stream<? extends T> s = null;
            private Deque<LocalRunnable> closeHandle = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    if (cursor < toIndex) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            StreamBase.close(tmp);
                        }

                        s = mapper.apply(elements[cursor++]);

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
     * Returns an object-valued {@code Stream<T>} formed by replacing each element with the elements of a mapped
     * {@code Collection<? extends T>}. {@code null} or empty collections contribute no elements.
     *
     * @param <T> the type of elements in the returned stream
     * @param mapper the non-interfering, stateless function returning a collection for each element
     * @return a new {@code Stream<T>} consisting of the concatenated collection elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatmapToObj(final CharFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;
            private Iterator<? extends T> cur = null;
            private Collection<? extends T> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && cursor < toIndex) {
                    c = mapper.apply(elements[cursor++]);
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
     * Uses an optimized adjacent-check for sorted streams, or a hash set otherwise.
     *
     * @return a new {@code CharStream} without duplicate elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new CharIteratorEx() { //NOSONAR
                private int prev = -1;
                private int cur = fromIndex;

                @Override
                public boolean hasNext() {
                    if (cur > fromIndex && cur < toIndex && elements[cur] == elements[prev]) {
                        //noinspection StatementWithEmptyBody
                        while (++cur < toIndex && elements[cur] == elements[prev]) {
                            // do nothing
                        }
                    }

                    return cur < toIndex;
                }

                @Override
                public char nextChar() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    prev = cur;
                    return elements[cur++];
                }
            }, isSorted());
        } else {
            final Set<Object> set = N.newHashSet();

            // noinspection resource
            return newStream(sequential().filter(set::add).iteratorEx(), isSorted());
        }
    }

    /**
     * Returns a stream of no more than {@code maxSize} elements. Implemented as a range restriction.
     *
     * @param maxSize the maximum number of elements; must be &gt;= 0
     * @return a new {@code CharStream} truncated to at most {@code maxSize} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    @Override
    public CharStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        if (toIndex - fromIndex <= maxSize) {
            return this;
        }

        return newStream(elements, fromIndex, maxSize < toIndex - fromIndex ? (int) (fromIndex + maxSize) : toIndex, isSorted());
    }

    /**
     * Returns a stream that skips the first {@code n} elements. Implemented as a range offset.
     *
     * @param n the number of leading elements to skip; must be &gt;= 0
     * @return a new {@code CharStream} with the first {@code n} elements omitted
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public CharStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0 || fromIndex == toIndex) {
            return this;
        }

        if (n >= toIndex - fromIndex) {
            return newStream(elements, toIndex, toIndex, isSorted());
        } else {
            return newStream(elements, (int) (fromIndex + n), toIndex, isSorted());
        }
    }

    /**
     * Returns a stream with the same elements, additionally performing the given action on each
     * element as elements are consumed.
     *
     * @param action the non-interfering action to perform on each element as it is consumed
     * @return a new {@code CharStream} with the side-effect action attached
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream onEach(final CharConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public char nextChar() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public char[] toArray() {
                final char[] a = new char[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, isSorted());
    }

    /**
     * Performs the given action for each element in encounter order, then closes the stream.
     *
     * @param <E> the type of exception the action may throw
     * @param action the non-interfering action to perform on each element
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception
     */
    @Override
    public <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(elements[i]);
            }
        } finally {
            close();
        }
    }

    /**
     * Returns a copy of the backing array slice. Optionally closes the stream.
     *
     * @param closeStream {@code true} to close the stream after collecting
     * @return a new {@code char[]} containing the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected char[] toArray(final boolean closeStream) {
        assertNotClosed();

        try {
            return N.copyOfRange(elements, fromIndex, toIndex);
        } finally {
            if (closeStream) {
                close();
            }
        }
    }

    /**
     * Collects the elements into a {@code CharList} and closes the stream.
     *
     * @return a {@code CharList} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharList toCharList() throws IllegalStateException {
        assertNotClosed();

        try {
            return CharList.of(N.copyOfRange(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Collects the elements into a {@code List<Character>} and closes the stream.
     *
     * @return a mutable {@code List<Character>} containing all elements in encounter order
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public List<Character> toList() throws IllegalStateException {
        assertNotClosed();

        try {
            final List<Character> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements into a {@code Set<Character>} and closes the stream.
     * Duplicate elements are silently discarded.
     *
     * @return a mutable {@code Set<Character>} containing the distinct elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Set<Character> toSet() throws IllegalStateException {
        assertNotClosed();

        try {
            final Set<Character> result = N.newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements into a collection created by the given supplier, then closes the stream.
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

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements into a {@code Multiset<Character>} and closes the stream.
     *
     * @return a {@code Multiset<Character>} representing element frequencies in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Character> toMultiset() throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Character> result = N.newMultiset(N.min(64, (toIndex - fromIndex) / 2));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements into a {@code Multiset<Character>} created by the given supplier, then closes the stream.
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

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
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

            for (int i = fromIndex; i < toIndex; i++) {
                Collectors.merge(result, keyMapper.apply(elements[i]), valueMapper.apply(elements[i]), mergeFunction);
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

            for (int i = fromIndex; i < toIndex; i++) {
                key = checkArgNotNull(keyMapper.apply(elements[i]), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    v = downstreamSupplier.get();
                    intermediate.put(key, v);
                }

                downstreamAccumulator.accept(v, elements[i]);
            }

            final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

            Collectors.replaceAll(intermediate, function);

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns the first element as an {@code OptionalChar}, or empty if the stream is empty. Closes the stream.
     *
     * @return an {@code OptionalChar} containing the first element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar first() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalChar.of(elements[fromIndex]) : OptionalChar.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the last element as an {@code OptionalChar}, or empty if the stream is empty. Closes the stream.
     *
     * @return an {@code OptionalChar} containing the last element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar last() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalChar.of(elements[toIndex - 1]) : OptionalChar.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the element at the given zero-based {@code position}, or empty if out of range. Closes the stream.
     *
     * @param position the zero-based index of the desired element; must be &gt;= 0
     * @return an {@code OptionalChar} containing the element at {@code position}, or empty if out of range
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code position} is negative
     */
    @Override
    public OptionalChar elementAt(final long position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);
        try {
            if (position < toIndex - fromIndex) {
                return OptionalChar.of(elements[fromIndex + (int) position]);
            } else {
                return OptionalChar.empty();
            }
        } finally {
            close();
        }
    }

    /**
     * Returns the single element as an {@code OptionalChar}, empty if the stream is empty,
     * or throws if more than one element exists. Closes the stream.
     *
     * @return an {@code OptionalChar} containing the single element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @throws TooManyElementsException if the stream contains more than one element
     */
    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalChar onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return OptionalChar.empty();
            } else if (size == 1) {
                return OptionalChar.of(elements[fromIndex]);
            } else {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(elements[fromIndex], ", ", elements[fromIndex + 1]));
            }
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
    public char reduce(final char identity, final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            char result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = accumulator.applyAsChar(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a reduction on the elements of this stream using an associative accumulation function,
     * and returns an {@code OptionalChar} describing the reduced value, or empty if the stream is empty.
     * Closes the stream.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalChar} containing the result, or empty if the stream has no elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar reduce(final CharBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalChar.empty();
            }

            char result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = accumulator.applyAsChar(result, elements[i]);
            }

            return OptionalChar.of(result);
        } finally {
            close();
        }
    }

    /**
     * Performs a mutable reduction on the elements of this stream using the provided supplier,
     * accumulator, and combiner functions, and returns the resulting container. Closes the stream.
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

            for (int i = fromIndex; i < toIndex; i++) {
                accumulator.accept(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns the minimum element of this stream, or an empty {@code OptionalChar} if the stream is empty.
     * If the stream is sorted, the minimum is read directly from the first position in constant time;
     * otherwise the entire backing array range is scanned. Closes the stream.
     *
     * @return an {@code OptionalChar} containing the minimum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalChar.empty();
            } else if (isSorted()) {
                return OptionalChar.of(elements[fromIndex]);
            }

            return OptionalChar.of(N.min(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns the maximum element of this stream, or an empty {@code OptionalChar} if the stream is empty.
     * If the stream is sorted, the maximum is read directly from the last position in constant time;
     * otherwise the entire backing array range is scanned. Closes the stream.
     *
     * @return an {@code OptionalChar} containing the maximum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalChar max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalChar.empty();
            } else if (isSorted()) {
                return OptionalChar.of(elements[toIndex - 1]);
            }

            return OptionalChar.of(N.max(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns the k-th largest element of this stream (1-based), or an empty {@code OptionalChar}
     * if the stream contains fewer than {@code k} elements. If the stream is sorted, the result is
     * retrieved via index arithmetic in constant time; otherwise a partial sort is performed.
     * Closes the stream.
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
            if (k > toIndex - fromIndex) {
                return OptionalChar.empty();
            } else if (isSorted()) {
                return OptionalChar.of(elements[toIndex - k]);
            }

            return OptionalChar.of(N.kthLargest(elements, fromIndex, toIndex, k));
        } finally {
            close();
        }
    }

    /**
     * Returns the sum of the char values (treated as unsigned integers) in this stream.
     * Closes the stream.
     *
     * @return the integer sum of all elements in this stream
     * @throws IllegalStateException if the stream is already closed
     * @throws ArithmeticException if the sum overflows the {@code int} range
     */
    @Override
    public int sum() throws IllegalStateException {
        assertNotClosed();

        try {
            return sum(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    /**
     * Returns the arithmetic average of the elements in this stream as an {@code OptionalDouble},
     * or an empty optional if the stream is empty. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the average, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble average() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            }

            // Accumulate into a long: sum() narrows via Numbers.toIntExact and would throw on a
            // large char stream whose sum exceeds Integer.MAX_VALUE, even though the average is
            // well-defined (matches IteratorCharStream.average and summaryStatistics).
            long sum = 0;

            for (int i = fromIndex; i < toIndex; i++) {
                sum += elements[i];
            }

            return OptionalDouble.of(((double) sum) / (toIndex - fromIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns the number of elements in this stream. This is a constant-time operation
     * based on the index range of the backing array ({@code toIndex - fromIndex}). Closes the stream.
     *
     * @return the element count
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public long count() throws IllegalStateException {
        assertNotClosed();

        try {
            return toIndex - fromIndex; //NOSONAR
        } finally {
            close();
        }
    }

    /**
     * Returns a {@code CharSummaryStatistics} describing various aggregate values (count, sum, min, max, average)
     * of the elements in this stream. Closes the stream.
     *
     * @return a {@code CharSummaryStatistics} for this stream's elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final CharSummaryStatistics result = new CharSummaryStatistics();

            for (int i = fromIndex; i < toIndex; i++) {
                result.accept(elements[i]);
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
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return true;
                }
            }

            return false;
        } finally {
            close();
        }
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
            for (int i = fromIndex; i < toIndex; i++) {
                if (!predicate.test(elements[i])) {
                    return false;
                }
            }

            return true;
        } finally {
            close();
        }
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
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return false;
                }
            }

            return true;
        } finally {
            close();
        }
    }

    /**
     * Returns the first element that matches the given predicate as an {@code OptionalChar},
     * or empty if no element matches. Iterates the backing array from {@code fromIndex} forward.
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
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return OptionalChar.of(elements[i]);
                }
            }

            return OptionalChar.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the last element that matches the given predicate as an {@code OptionalChar},
     * or empty if no element matches. Iterates the backing array from {@code toIndex - 1} backward.
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
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return OptionalChar.of(elements[i]);
                }
            }

            return OptionalChar.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns an {@code IntStream} consisting of the {@code int} values of the elements of this stream.
     * Each {@code char} element is widened to an {@code int} without sign-extension.
     *
     * @return an {@code IntStream} with the same elements widened to {@code int}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream asIntStream() throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return elements[cursor++];
            }

            @Override
            public long count() {
                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0 || cursor >= toIndex) {
                    return;
                }

                final long remaining = toIndex - cursor;
                cursor = n < remaining ? (int) (cursor + Math.min(n, Integer.MAX_VALUE - cursor)) : toIndex;
            }

            @Override
            public int[] toArray() {
                final int[] a = new int[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, isSorted());
    }

    /**
     * Returns a {@code CharIteratorEx} over the elements in this stream's backing array range.
     * This is an internal method used by stream infrastructure.
     *
     * @return a {@code CharIteratorEx} covering [{@code fromIndex}, {@code toIndex}) of the backing array
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    CharIteratorEx iteratorEx() {
        assertNotClosed();

        return CharIteratorEx.of(elements, fromIndex, toIndex);
    }

    /**
     * Returns this stream if it is non-empty; otherwise returns a stream produced by the given supplier.
     * The supplier is not invoked unless the stream is empty. Closes any produced stream on close.
     *
     * @param supplier a supplier of a substitute {@code CharStream} to use when this stream is empty
     * @return this stream if non-empty, or the stream produced by the supplier
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream appendIfEmpty(final Supplier<? extends CharStream> supplier) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
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
                        final CharStream s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }, false).onClose(() -> close(holder));
        } else {
            return this;
        }
    }

    /**
     * Executes the given action if this stream is empty, then returns an empty stream.
     * Has no effect if this stream is non-empty. The action runs at most once, when the
     * returned stream's iterator is first advanced.
     *
     * @param action a {@code Runnable} to execute if the stream is empty
     * @return this stream if non-empty, or an empty stream that invokes {@code action} once when iterated
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            return newStream(new CharIteratorEx() { //NOSONAR
                private boolean executed = false;

                @Override
                public boolean hasNext() {
                    if (!executed) {
                        executed = true;
                        action.run();
                    }

                    return false;
                }

                @Override
                public char nextChar() {
                    if (!executed) {
                        executed = true;
                        action.run();
                    }

                    throw new NoSuchElementException("No more elements available in stream iterator");
                }

                @Override
                public void advance(final long n) {
                    if (n <= 0) {
                        return;
                    }

                    if (!executed) {
                        executed = true;
                        action.run();
                    }
                }
            });
        } else {
            return this;
        }
    }

    /**
     * Returns a {@code Tuple3} of the backing array, {@code fromIndex}, and {@code toIndex}
     * for use by intermediate operations that can work directly on the underlying array.
     * This is an internal optimization method.
     *
     * @return a {@code Tuple3} containing the backing {@code char[]}, the inclusive start index, and the exclusive end index
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected Tuple3<char[], Integer, Integer> arrayForIntermediateOp() {
        assertNotClosed();

        return Tuple.of(elements, fromIndex, toIndex);
    }

    /**
     * Returns a new {@link ParallelArrayCharStream} that processes this stream's backing array
     * range in parallel using the specified configuration.
     *
     * @param maxThreadNum the maximum number of threads to use
     * @param splitor the strategy for splitting the work among threads
     * @param asyncExecutor the executor for asynchronous parallel tasks
     * @param cancelUncompletedThreads whether to cancel incomplete threads when the stream is closed
     * @return a parallel {@code CharStream} backed by the same array range
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected CharStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelArrayCharStream(elements, fromIndex, toIndex, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                closeHandlers());
    }

    /**
     * Returns an infinite stream that cycles through the elements of this stream repeatedly.
     * If this stream is empty, the returned stream is also empty.
     *
     * <pre>{@code
     * // CharStream.of('a', 'b', 'c').cycled().limit(7) => 'a', 'b', 'c', 'a', 'b', 'c', 'a'
     * }</pre>
     *
     * @return an infinite {@code CharStream} cycling through the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public CharStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new CharIterator() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex;
            }

            @Override
            public char nextChar() {
                if (fromIndex >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (cursor >= toIndex) {
                    cursor = fromIndex;
                }

                return elements[cursor++];
            }
        }, false);
    }

    /**
     * Returns a stream that cycles through the elements of this stream exactly {@code rounds} times.
     * If {@code rounds} is 0, returns an empty stream. If this stream is empty, the returned stream is empty.
     *
     * <pre>{@code
     * // CharStream.of('a', 'b').cycled(3) => 'a', 'b', 'a', 'b', 'a', 'b'
     * }</pre>
     *
     * @param rounds the number of times to cycle through the elements; must be &gt;= 0
     * @return a {@code CharStream} that repeats the elements of this stream {@code rounds} times
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code rounds} is negative
     */
    @Override
    public CharStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new CharIterator() { //NOSONAR
            private int cursor = fromIndex;
            private long roundsCompleted = 0;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex && roundsCompleted < rounds && (cursor < toIndex || rounds - roundsCompleted > 1);
            }

            @Override
            public char nextChar() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (cursor >= toIndex) {
                    cursor = fromIndex;
                    roundsCompleted++;
                }

                return elements[cursor++];
            }
        }, rounds <= 1 && isSorted());
    }

    /**
     * Returns {@code true} if this stream contains no elements (i.e. {@code fromIndex >= toIndex}).
     *
     * @return {@code true} if the stream is empty, {@code false} otherwise
     */
    @Override
    protected boolean isEmpty() {
        return fromIndex >= toIndex;
    }
}
