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
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
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
 * An array-backed implementation of ByteStream that provides efficient sequential access to a byte array
 * or a portion of it. This implementation is optimized for working with primitive byte arrays and offers
 * high performance for operations on contiguous byte sequences.
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
 * byte[] data = {1, 2, 3, 4, 5};
 * ByteStream stream = ByteStream.of(data);
 * byte max = stream.max().orElse((byte)0);
 * }</pre>
 *
 */
class ArrayByteStream extends AbstractByteStream {
    final byte[] elements;
    final int fromIndex;
    final int toIndex;

    /**
     * Constructs an ArrayByteStream from the entire byte array.
     *
     * @param values the byte array to stream
     */
    ArrayByteStream(final byte[] values) {
        this(values, 0, values.length);
    }

    /**
     * Constructs an ArrayByteStream from the entire byte array with close handlers.
     *
     * @param values the byte array to stream
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ArrayByteStream(final byte[] values, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    /**
     * Constructs an ArrayByteStream from the entire byte array with sorting state and close handlers.
     *
     * @param values the byte array to stream
     * @param sorted whether the array elements are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ArrayByteStream(final byte[] values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    /**
     * Constructs an ArrayByteStream from a range within the byte array.
     *
     * @param values the byte array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayByteStream(final byte[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    /**
     * Constructs an ArrayByteStream from a range within the byte array with close handlers.
     *
     * @param values the byte array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @param closeHandlers handlers to execute when the stream is closed
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayByteStream(final byte[] values, final int fromIndex, final int toIndex, final Collection<LocalRunnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    /**
     * Constructs an ArrayByteStream from a range within the byte array with all configuration options.
     * This is the primary constructor that all other constructors delegate to.
     *
     * @param values the byte array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @param sorted whether the array elements in the range are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayByteStream(final byte[] values, final int fromIndex, final int toIndex, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, N.len(values));

        elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /**
     * Returns a stream consisting of elements that match the given predicate.
     * Iterates over the backing array from {@code fromIndex} to {@code toIndex},
     * including only elements for which {@code predicate.test(element)} returns {@code true}.
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
            public byte nextByte() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    /**
     * Returns a stream of elements taken from this stream as long as the given predicate
     * holds {@code true}. Once an element fails the predicate, the stream terminates immediately,
     * even if subsequent elements would match.
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
            public byte nextByte() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    /**
     * Returns a stream that skips elements as long as the given predicate is {@code true},
     * then includes all remaining elements. The predicate is evaluated only during the
     * initial drop phase; once it returns {@code false} for the first time, all subsequent
     * elements are included regardless of the predicate.
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
            public byte nextByte() {
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
     * The first element (index 0 within this stream) is always included; after that,
     * every element at positions {@code step}, {@code 2*step}, {@code 3*step}, etc. is included.
     *
     * <pre>{@code
     * byte[] data = {1, 2, 3, 4, 5, 6};
     * // step(2) yields: 1, 3, 5
     * ByteStream.of(data).step(2).forEach(System.out::println);
     * }</pre>
     *
     * @param step the distance between successive elements; must be positive
     * @return a new {@code ByteStream} containing every {@code step}-th element
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code step} is less than 1
     */
    @Override
    public ByteStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1 || fromIndex == toIndex) {
            return skip(0);
        }

        return newStream(new ByteIteratorEx() { //NOSONAR
            private final int stepToUse = (int) N.min(step, Integer.MAX_VALUE);
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final byte res = elements[cursor];
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
            public byte[] toArray() {
                final long rem = toIndex - cursor;
                final int len = (int) ((rem % stepToUse == 0) ? rem / stepToUse : rem / stepToUse + 1);
                final byte[] a = new byte[len];

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
     * The resulting stream has the same number of elements as the source but with each byte
     * transformed by the {@code mapper} function.
     *
     * @param mapper the non-interfering, stateless function to apply to each element
     * @return a new {@code ByteStream} containing the mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream map(final ByteUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.applyAsByte(elements[cursor++]);
            }

            @Override
            public byte[] toArray() {
                final byte[] a = new byte[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsByte(elements[cursor++]);
                }

                return a;
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
     * Returns a stream formed by replacing each element of this stream with the contents
     * of a mapped stream produced by applying the given mapper function. The mapped streams
     * are closed after their contents are consumed. If the mapped stream is {@code null},
     * it is treated as an empty stream.
     *
     * @param mapper the non-interfering, stateless function returning a {@code ByteStream} for each element
     * @return a new {@code ByteStream} consisting of the concatenated mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream flatMap(final ByteFunction<? extends ByteStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final ByteIteratorEx iter = new ByteIteratorEx() {
            private int cursor = fromIndex;
            private ByteIterator cur = null;
            private ByteStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (cursor < toIndex) {
                        s = mapper.apply(elements[cursor++]);

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
     * {@code Collection<Byte>}. This is a convenience variant of {@link #flatMap} that
     * accepts a collection-returning function instead of a stream-returning function.
     * {@code null} or empty collections result in no elements being contributed.
     *
     * @param mapper the non-interfering, stateless function returning a collection for each element
     * @return a new {@code ByteStream} consisting of the concatenated collection elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream flatmap(final ByteFunction<? extends Collection<Byte>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private Iterator<Byte> cur = null;
            private Collection<Byte> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && cursor < toIndex) {
                    c = mapper.apply(elements[cursor++]);
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
     * {@code byte[]} array. This is a convenience variant of {@link #flatMap} that
     * accepts an array-returning function. {@code null} or empty arrays contribute no elements.
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
            private int cursor = fromIndex;
            private byte[] cur = null;
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
            public byte nextByte() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    /**
     * Returns an {@code IntStream} formed by replacing each element with the contents
     * of a mapped {@code IntStream}. Mapped streams are closed after their contents
     * are consumed. {@code null} mapped streams are treated as empty.
     *
     * @param mapper the non-interfering, stateless function returning an {@code IntStream} for each element
     * @return a new {@code IntStream} consisting of the concatenated mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final ByteFunction<? extends IntStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final IntIteratorEx iter = new IntIteratorEx() {
            private int cursor = fromIndex;
            private IntIterator cur = null;
            private IntStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (cursor < toIndex) {
                        s = mapper.apply(elements[cursor++]);

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
            private int cursor = fromIndex;
            private Iterator<? extends T> cur = null;
            private Stream<? extends T> s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (cursor < toIndex) {
                        s = mapper.apply(elements[cursor++]);

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
     * the elements of a mapped {@code Collection<? extends T>}. This is a convenience
     * variant of {@link #flatMapToObj} that accepts a collection-returning function.
     * {@code null} or empty collections contribute no elements.
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
     * If the stream is already sorted, an optimized single-pass algorithm is used;
     * otherwise a hash set is used to track seen elements. The relative order of
     * first occurrences is preserved.
     *
     * @return a new {@code ByteStream} without duplicate elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new ByteIteratorEx() { //NOSONAR
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
                public byte nextByte() {
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
     * Returns a stream consisting of no more than {@code maxSize} elements from the start
     * of this stream. If the stream has fewer elements than {@code maxSize}, all elements
     * are included. For array-backed streams this is implemented as a range restriction
     * with no additional iteration cost.
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

        if (toIndex - fromIndex <= maxSize) {
            return this;
        }

        return newStream(elements, fromIndex, maxSize < toIndex - fromIndex ? (int) (fromIndex + maxSize) : toIndex, isSorted());
    }

    /**
     * Returns a stream that skips the first {@code n} elements of this stream.
     * If the stream contains fewer than {@code n} elements, an empty stream is returned.
     * For array-backed streams this is implemented as a range offset with no iteration cost.
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
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public byte nextByte() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public byte[] toArray() {
                final byte[] a = new byte[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = elements[cursor++];
                }

                return a;
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
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(elements[i]);
            }
        } finally {
            close();
        }
    }

    /**
     * Returns a copy of the backing array slice as a new {@code byte[]}. Optionally closes
     * the stream after collecting. Directly copies the sub-range without additional iteration.
     *
     * @param closeStream {@code true} to close the stream after the array is collected
     * @return a new {@code byte[]} containing the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected byte[] toArray(final boolean closeStream) {
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
     * Collects the elements of this stream into a {@code ByteList} and closes the stream.
     * Creates the list directly from the backing array range for efficiency.
     *
     * @return a {@code ByteList} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteList toByteList() throws IllegalStateException {
        assertNotClosed();

        try {
            return ByteList.of(N.copyOfRange(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Collects the elements of this stream into a {@code List<Byte>} and closes the stream.
     * The list is pre-allocated with the exact capacity of this stream's element range.
     *
     * @return a mutable {@code List<Byte>} containing all elements of this stream in encounter order
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public List<Byte> toList() throws IllegalStateException {
        assertNotClosed();

        try {
            final List<Byte> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
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

        try {
            final Set<Byte> result = N.newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
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

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements of this stream into a {@code Multiset<Byte>} and closes the stream.
     * Elements with equal values will have their counts incremented accordingly.
     *
     * @return a {@code Multiset<Byte>} representing element frequencies in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Byte> toMultiset() throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Byte> result = N.newMultiset(N.min(64, (toIndex - fromIndex) / 2));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
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

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
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

            for (int i = fromIndex; i < toIndex; i++) {
                Collectors.merge(result, keyMapper.apply(elements[i]), valueMapper.apply(elements[i]), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Groups the elements of this stream by a key extracted by {@code keyMapper}, applies a
     * downstream collector to each group, and closes the stream. The result is a map from
     * each key to the collected value for that group.
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
     * Returns the first element of this stream as an {@code OptionalByte}, or an empty optional
     * if the stream is empty. Closes the stream after the result is determined.
     *
     * @return an {@code OptionalByte} containing the first element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalByte first() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalByte.of(elements[fromIndex]) : OptionalByte.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the last element of this stream as an {@code OptionalByte}, or an empty optional
     * if the stream is empty. Closes the stream after the result is determined.
     *
     * @return an {@code OptionalByte} containing the last element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalByte last() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalByte.of(elements[toIndex - 1]) : OptionalByte.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the element at the given zero-based {@code position} within this stream,
     * or an empty optional if the position is beyond the stream's length. Closes the stream.
     *
     * @param position the zero-based index of the desired element; must be &gt;= 0
     * @return an {@code OptionalByte} containing the element at {@code position}, or empty if out of range
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code position} is negative
     */
    @Override
    public OptionalByte elementAt(final long position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        try {
            if (position < toIndex - fromIndex) {
                return OptionalByte.of(elements[fromIndex + (int) position]);
            } else {
                return OptionalByte.empty();
            }
        } finally {
            close();
        }
    }

    /**
     * Returns the single element of this stream, or an empty optional if the stream is empty.
     * Throws {@code TooManyElementsException} if the stream contains more than one element.
     * Closes the stream after the result is determined.
     *
     * @return an {@code OptionalByte} containing the single element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @throws TooManyElementsException if the stream contains more than one element
     */
    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalByte onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return OptionalByte.empty();
            } else if (size == 1) {
                return OptionalByte.of(elements[fromIndex]);
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
     * <pre>{@code
     * byte sum = ByteStream.of(data).reduce((byte)0, (a, b) -> (byte)(a + b));
     * }</pre>
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

            for (int i = fromIndex; i < toIndex; i++) {
                result = accumulator.applyAsByte(result, elements[i]);
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
            if (fromIndex == toIndex) {
                return OptionalByte.empty();
            }

            byte result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = accumulator.applyAsByte(result, elements[i]);
            }

            return OptionalByte.of(result);
        } finally {
            close();
        }
    }

    /**
     * Performs a mutable reduction on the elements of this stream and closes the stream.
     * A mutable reduction accumulates input elements into a mutable result container.
     * The {@code combiner} is used only in parallel executions and is ignored here.
     *
     * <pre>{@code
     * ByteList list = ByteStream.of(data).collect(ByteList::new, ByteList::add, (l1, l2) -> l1.addAll(l2));
     * }</pre>
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

            for (int i = fromIndex; i < toIndex; i++) {
                accumulator.accept(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns the minimum element of this stream, or an empty optional if the stream is empty.
     * If the stream is already sorted, the first element is returned directly; otherwise the
     * minimum is computed in a single pass. Closes the stream.
     *
     * @return an {@code OptionalByte} containing the minimum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalByte min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalByte.empty();
            } else if (isSorted()) {
                return OptionalByte.of(elements[fromIndex]);
            }

            return OptionalByte.of(N.min(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns the maximum element of this stream, or an empty optional if the stream is empty.
     * If the stream is already sorted, the last element is returned directly; otherwise the
     * maximum is computed in a single pass. Closes the stream.
     *
     * @return an {@code OptionalByte} containing the maximum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalByte max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalByte.empty();
            } else if (isSorted()) {
                return OptionalByte.of(elements[toIndex - 1]);
            }

            return OptionalByte.of(N.max(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns the k-th largest element of this stream, or an empty optional if the stream
     * contains fewer than {@code k} elements. If the stream is already sorted, the result is
     * obtained by direct index access; otherwise a partial-sort algorithm is used. Closes the stream.
     *
     * @param k the rank of the desired element (1 = largest, 2 = second largest, etc.); must be positive
     * @return an {@code OptionalByte} containing the k-th largest element, or empty if not enough elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code k} is less than 1
     */
    @Override
    public OptionalByte kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (k > toIndex - fromIndex) {
                return OptionalByte.empty();
            } else if (isSorted()) {
                return OptionalByte.of(elements[toIndex - k]);
            }

            return OptionalByte.of(N.kthLargest(elements, fromIndex, toIndex, k));
        } finally {
            close();
        }
    }

    /**
     * Returns the sum of all elements in this stream as an {@code int}. Closes the stream.
     * Returns 0 if the stream is empty.
     *
     * @return the sum of all elements
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
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            }

            // Accumulate into a long: sum() narrows via Numbers.toIntExact and would throw on a
            // large byte stream whose sum exceeds Integer.MAX_VALUE, even though the average is
            // well-defined (matches IteratorByteStream.average and summaryStatistics).
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
     * Returns the count of elements in this stream and closes the stream.
     * For array-backed streams this is a constant-time operation based on the index range.
     *
     * @return the number of elements in this stream
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
     * Evaluates the predicate in encounter order and short-circuits on the first match.
     * Closes the stream after evaluation.
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
     * Returns {@code true} if all elements of this stream match the provided predicate.
     * Evaluates the predicate in encounter order and short-circuits on the first non-match.
     * Returns {@code true} for empty streams. Closes the stream after evaluation.
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
     * Returns {@code true} if no element of this stream matches the provided predicate.
     * Evaluates the predicate in encounter order and short-circuits on the first match.
     * Returns {@code true} for empty streams. Closes the stream after evaluation.
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
     * Returns the first element matching the given predicate as an {@code OptionalByte},
     * or an empty optional if no match is found. Evaluates elements in encounter order
     * and short-circuits on the first match. Closes the stream after evaluation.
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
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return OptionalByte.of(elements[i]);
                }
            }

            return OptionalByte.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the last element matching the given predicate as an {@code OptionalByte},
     * or an empty optional if no match is found. Evaluates elements in reverse encounter order
     * and short-circuits on the first match found from the end. Closes the stream.
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
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return OptionalByte.of(elements[i]);
                }
            }

            return OptionalByte.empty();
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
     * Returns the internal {@code ByteIteratorEx} for this stream, backed directly by the
     * underlying array range. This is an internal method used by the stream implementation.
     *
     * @return a {@code ByteIteratorEx} over the array range {@code [fromIndex, toIndex)}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    ByteIteratorEx iteratorEx() {
        assertNotClosed();

        return ByteIteratorEx.of(elements, fromIndex, toIndex);
    }

    /**
     * Returns this stream if it is non-empty, or the stream produced by the given supplier
     * if this stream is empty. The supplier's stream is only invoked if needed.
     * The returned stream carries a close handler that closes the supplier's stream on completion.
     *
     * @param supplier a supplier of a fallback {@code ByteStream} to use if this stream is empty
     * @return this stream if non-empty, or the fallback stream otherwise
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream appendIfEmpty(final Supplier<? extends ByteStream> supplier) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
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
                        final ByteStream s = supplier.get();
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

        if (fromIndex == toIndex) {
            return newStream(new ByteIteratorEx() { //NOSONAR
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
                public byte nextByte() {
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
     * Returns the underlying array together with its {@code fromIndex} and {@code toIndex}
     * bounds, for use by intermediate operators that can work directly on the backing array.
     * This is an internal optimization method.
     *
     * @return a {@code Tuple3} of the backing array and its inclusive start and exclusive end indices
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected Tuple3<byte[], Integer, Integer> arrayForIntermediateOp() {
        assertNotClosed();

        return Tuple.of(elements, fromIndex, toIndex);
    }

    /**
     * Creates a parallel version of this stream using the given thread pool configuration.
     * Returns a {@link ParallelArrayByteStream} that operates on the same array range.
     *
     * @param maxThreadNum the maximum number of threads to use for parallel execution
     * @param splitor the strategy for splitting work across threads
     * @param asyncExecutor the executor to use for parallel tasks
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @return a parallel {@code ByteStream} backed by the same array range
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected ByteStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelArrayByteStream(elements, fromIndex, toIndex, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                closeHandlers());
    }

    /**
     * Returns an infinite stream that cycles through the elements of this stream repeatedly.
     * The stream will endlessly loop over the elements in the array range. If the array range
     * is empty, an empty stream is returned.
     *
     * <pre>{@code
     * byte[] data = {1, 2, 3};
     * // limit(7) yields: 1, 2, 3, 1, 2, 3, 1
     * ByteStream.of(data).cycled().limit(7).forEach(System.out::println);
     * }</pre>
     *
     * @return an infinite {@code ByteStream} that cycles through the elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public ByteStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ByteIterator() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex;
            }

            @Override
            public byte nextByte() {
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
     * The total number of elements in the returned stream equals
     * {@code (toIndex - fromIndex) * rounds}. If {@code rounds} is 0 an empty stream is returned.
     *
     * <pre>{@code
     * byte[] data = {1, 2, 3};
     * // yields: 1, 2, 3, 1, 2, 3
     * ByteStream.of(data).cycled(2).forEach(System.out::println);
     * }</pre>
     *
     * @param rounds the number of times to cycle through the elements; must be &gt;= 0
     * @return a {@code ByteStream} repeating the elements {@code rounds} times
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code rounds} is negative
     */
    @Override
    public ByteStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new ByteIterator() { //NOSONAR
            private int cursor = fromIndex;
            private long roundsCompleted = 0;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex && roundsCompleted < rounds && (cursor < toIndex || rounds - roundsCompleted > 1);
            }

            @Override
            public byte nextByte() {
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
     * Returns {@code true} if this stream contains no elements (i.e., {@code fromIndex >= toIndex}).
     * This is a constant-time check against the array bounds.
     *
     * @return {@code true} if the stream is empty, {@code false} otherwise
     */
    @Override
    protected boolean isEmpty() {
        return fromIndex >= toIndex;
    }
}
