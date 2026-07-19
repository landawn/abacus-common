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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Array;
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
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 * An array-backed implementation of Stream that provides efficient sequential access to an object array
 * or a portion of it. This implementation is optimized for working with object arrays and offers
 * high performance for operations on contiguous object sequences.
 *
 * <p>Key features:
 * <ul>
 * <li>Zero-copy operations when possible, working directly on the underlying array</li>
 * <li>Support for partial array ranges via fromIndex and toIndex</li>
 * <li>Optimized implementations of filter, map, flatMap, and terminal operations</li>
 * <li>Efficient sorted stream handling with custom comparators</li>
 * <li>Type-safe operations on generic arrays</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String[] data = {"apple", "banana", "cherry"};
 * Stream<String> stream = Stream.of(data);
 * String longest = stream.max(Comparator.comparing(String::length)).orElse("");
 * }</pre>
 *
 * @param <T> the type of stream elements
 */
class ArrayStream<T> extends AbstractStream<T> {
    final T[] elements;
    final int fromIndex;
    final int toIndex;

    /**
     * Constructs an ArrayStream from the entire object array.
     *
     * @param values the object array to stream
     */
    ArrayStream(final T[] values) {
        this(values, 0, values.length);
    }

    /**
     * Constructs an ArrayStream from the entire object array with close handlers.
     *
     * @param values the object array to stream
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ArrayStream(final T[] values, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    /**
     * Constructs an ArrayStream from the entire object array with sorting state, comparator, and close handlers.
     *
     * @param values the object array to stream
     * @param sorted whether the array elements are in sorted order
     * @param comparator the comparator for ordering elements, or {@code null} if using natural order
     * @param closeHandlers handlers to execute when the stream is closed
     */
    ArrayStream(final T[] values, final boolean sorted, final Comparator<? super T> comparator, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, sorted, comparator, closeHandlers);
    }

    /**
     * Constructs an ArrayStream from a range within the object array.
     *
     * @param values the object array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length}, or {@code fromIndex > toIndex}
     */
    ArrayStream(final T[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    /**
     * Constructs an ArrayStream from a range within the object array with close handlers.
     *
     * @param values the object array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @param closeHandlers handlers to execute when the stream is closed
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length}, or {@code fromIndex > toIndex}
     */
    ArrayStream(final T[] values, final int fromIndex, final int toIndex, final Collection<LocalRunnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, null, closeHandlers);
    }

    /**
     * Constructs an ArrayStream from a range within the object array with all configuration options.
     * This is the primary constructor that all other constructors delegate to.
     *
     * @param values the object array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @param sorted whether the array elements in the range are in sorted order
     * @param comparator the comparator for ordering elements, or {@code null} if using natural order
     * @param closeHandlers handlers to execute when the stream is closed
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayStream(final T[] values, final int fromIndex, final int toIndex, final boolean sorted, final Comparator<? super T> comparator,
            final Collection<LocalRunnable> closeHandlers) {
        super(sorted, comparator, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, N.len(values));

        elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    /**
     * Returns a stream consisting of the elements of this stream that match the given predicate.
     *
     * <p>This is a stateless intermediate operation that iterates directly over the backing array.
     * The sorted state and comparator of this stream are preserved in the returned stream.
     *
     * @param predicate the predicate to apply to each element; must not be {@code null}
     * @return a new stream containing only the elements that match the predicate
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> filter(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
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
            public T next() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> takeWhile(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
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
            public T next() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
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
            public T next() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted(), comparator());
    }

    /**
     * Returns a stream consisting of every {@code step}-th element of this stream.
     * The first element is always included; subsequent elements are included at intervals
     * of {@code step}.
     *
     * <p>This is a stateful intermediate operation. If {@code step == 1}, this method
     * returns an equivalent stream without copying the backing array.
     *
     * <pre>{@code
     * // From [0, 1, 2, 3, 4, 5] with step=2: [0, 2, 4]
     * Stream.of(0, 1, 2, 3, 4, 5).step(2);
     * }</pre>
     *
     * @param step the step size between returned elements; must be positive
     * @return a new stream containing every {@code step}-th element
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code step} is not positive
     */
    @Override
    public Stream<T> step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1 || fromIndex == toIndex) {
            return skip(0);
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private final int stepToUse = (int) N.min(step, Integer.MAX_VALUE);
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T res = elements[cursor];
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
            public <A> A[] toArray(A[] a) {
                // Don't use count() here — it now drains the iterator (sets cursor=toIndex)
                // to honour the IteratorEx.count() contract. Compute the size directly.
                final long rem = toIndex - cursor;
                final int len = (int) ((rem % stepToUse == 0) ? rem / stepToUse : rem / stepToUse + 1);
                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                for (int i = 0; i < len; i++) {
                    a[i] = (A) elements[cursor];
                    cursor = cursor > toIndex - stepToUse ? toIndex : cursor + stepToUse;
                }

                if (a.length > len) {
                    a[len] = null;
                }

                return a;
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> Stream<R> map(final Function<? super T, ? extends R> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.apply(elements[cursor++]);
            }

            @Override
            public <A> A[] toArray(A[] a) throws IllegalArgumentException {
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

    @Override
    public <R> Stream<R> slidingMap(final int increment, final boolean ignoreNotPaired, final BiFunction<? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        final int windowSize = 2;
        checkArgPositive(increment, "increment"); //NOSONAR

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return ignoreNotPaired ? toIndex - cursor >= windowSize : cursor < toIndex;
            }

            @Override
            public R next() throws IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final R result = mapper.apply(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

        }, false, null);
    }

    @Override
    public <R> Stream<R> slidingMap(final int increment, final boolean ignoreNotPaired, final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        final int windowSize = 3;
        checkArgPositive(increment, cs.increment);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return ignoreNotPaired ? toIndex - cursor >= windowSize : cursor < toIndex;
            }

            @Override
            public R next() throws IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final R result = mapper.apply(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null,
                        cursor < toIndex - 2 ? elements[cursor + 2] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

        }, false, null);
    }

    @Override
    public Stream<T> mapFirst(final Function<? super T, ? extends T> mapperForFirst) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            return newStream(elements, fromIndex, toIndex, isSorted(), comparator());
        } else if (toIndex - fromIndex == 1) {
            return map(mapperForFirst);
        } else {
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

                    if (cursor == fromIndex) {
                        return mapperForFirst.apply(elements[cursor++]);
                    } else {
                        return elements[cursor++];
                    }
                }

                @Override
                public long count() {
                    if (!hasNext()) {
                        return 0;
                    }
                    // IteratorEx.count() must consume the iterator. We must apply the
                    // mapperForFirst exactly once (next()) and then advance cursor to toIndex
                    // so a subsequent hasNext() correctly returns false.
                    next();
                    final long rest = (long) toIndex - cursor;
                    cursor = toIndex;
                    return rest + 1;
                }

                @Override
                public void advance(long n) {
                    if (n <= 0) {
                        return;
                    }

                    if ((n > 0) && hasNext()) {
                        next();
                        n -= 1;
                        final long remaining = toIndex - cursor;
                        cursor = n < remaining ? (int) (cursor + Math.min(n, Integer.MAX_VALUE - cursor)) : toIndex;
                    }
                }

                @Override
                public <A> A[] toArray(A[] a) {
                    final int len = toIndex - cursor;
                    a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                    for (int i = 0; i < len; i++) {
                        if (cursor == fromIndex) {
                            a[i] = (A) mapperForFirst.apply(elements[cursor++]);
                        } else {
                            a[i] = (A) elements[cursor++];
                        }
                    }

                    if (a.length > len) {
                        a[len] = null;
                    }

                    return a;
                }
            }, false, null);
        }
    }

    @Override
    public <R> Stream<R> mapFirstOrElse(final Function<? super T, ? extends R> mapperForFirst, final Function<? super T, ? extends R> mapperForElse)
            throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            return (Stream<R>) this;
        } else if (toIndex - fromIndex == 1) {
            return map(mapperForFirst);
        } else {
            return newStream(new ObjIteratorEx<>() { //NOSONAR
                private int cursor = fromIndex;

                @Override
                public boolean hasNext() {
                    return cursor < toIndex;
                }

                @Override
                public R next() throws IllegalArgumentException {
                    if (cursor >= toIndex) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    if (cursor == fromIndex) {
                        return mapperForFirst.apply(elements[cursor++]);
                    } else {
                        return mapperForElse.apply(elements[cursor++]);
                    }
                }

                @Override
                public <A> A[] toArray(A[] a) {
                    final int len = toIndex - cursor;
                    a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                    for (int i = 0; i < len; i++) {
                        if (cursor == fromIndex) {
                            a[i] = (A) mapperForFirst.apply(elements[cursor++]);
                        } else {
                            a[i] = (A) mapperForElse.apply(elements[cursor++]);
                        }
                    }

                    if (a.length > len) {
                        a[len] = null;
                    }

                    return a;
                }
            }, false, null);
        }
    }

    @Override
    public Stream<T> mapLast(final Function<? super T, ? extends T> mapperForLast) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            return newStream(elements, fromIndex, toIndex, isSorted(), comparator());
        } else if (toIndex - fromIndex == 1) {
            return map(mapperForLast);
        } else {
            return newStream(new ObjIteratorEx<>() { //NOSONAR
                private final int last = toIndex - 1;
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

                    if (cursor == last) {
                        return mapperForLast.apply(elements[cursor++]);
                    } else {
                        return elements[cursor++];
                    }
                }

                @Override
                public <A> A[] toArray(A[] a) {
                    final int len = toIndex - cursor;
                    a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                    for (int i = 0; i < len; i++) {
                        if (cursor == last) {
                            a[i] = (A) mapperForLast.apply(elements[cursor++]);
                        } else {
                            a[i] = (A) elements[cursor++];
                        }
                    }

                    if (a.length > len) {
                        a[len] = null;
                    }

                    return a;
                }
            }, false, null);
        }
    }

    @Override
    public <R> Stream<R> mapLastOrElse(final Function<? super T, ? extends R> mapperForLast, final Function<? super T, ? extends R> mapperForElse)
            throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private final int last = toIndex - 1;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (cursor == last) {
                    return mapperForLast.apply(elements[cursor++]);
                } else {
                    return mapperForElse.apply(elements[cursor++]);
                }
            }

            @Override
            public <A> A[] toArray(A[] a) {
                final int len = toIndex - cursor;
                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                for (int i = 0; i < len; i++) {
                    if (cursor == last) {
                        a[i] = (A) mapperForLast.apply(elements[cursor++]);
                    } else {
                        a[i] = (A) mapperForElse.apply(elements[cursor++]);
                    }
                }

                if (a.length > len) {
                    a[len] = null;
                }

                return a;
            }
        }, false, null);
    }

    @Override
    public CharStream mapToChar(final ToCharFunction<? super T> mapper) throws IllegalStateException {
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

    @Override
    public ByteStream mapToByte(final ToByteFunction<? super T> mapper) throws IllegalStateException {
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

    @Override
    public ShortStream mapToShort(final ToShortFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.applyAsShort(elements[cursor++]);
            }

            @Override
            public short[] toArray() {
                final short[] a = new short[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsShort(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public IntStream mapToInt(final ToIntFunction<? super T> mapper) throws IllegalStateException {
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

    @Override
    public LongStream mapToLong(final ToLongFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new LongIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.applyAsLong(elements[cursor++]);
            }

            @Override
            public long[] toArray() {
                final long[] a = new long[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsLong(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public FloatStream mapToFloat(final ToFloatFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.applyAsFloat(elements[cursor++]);
            }

            @Override
            public float[] toArray() {
                final float[] a = new float[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsFloat(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() throws IllegalArgumentException {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return mapper.applyAsDouble(elements[cursor++]);
            }

            @Override
            public double[] toArray() {
                final double[] a = new double[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = mapper.applyAsDouble(elements[cursor++]);
                }

                return a;
            }
        }, false);
    }

    @Override
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<R> iter = new ObjIteratorEx<>() {
            private int cursor = fromIndex;
            private Iterator<? extends R> cur = null;
            private Stream<? extends R> s = null;

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
            public R next() {
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
                    final Stream<? extends R> tmp = s;
                    s = null;
                    cur = null;
                    tmp.close();
                }
            }
        };

        return newStream(iter, false, null).onClose(iter::closeResource); //NOSONAR
    }

    @Override
    public <R> Stream<R> flatmap(final Function<? super T, ? extends Collection<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;
            private Iterator<? extends R> cur = null;
            private Collection<? extends R> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && cursor < toIndex) {
                    c = mapper.apply(elements[cursor++]);
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
            private int cursor = fromIndex;
            private R[] cur = null;
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
            private int cursor = fromIndex;
            private CharIterator cur = null;
            private CharStream s = null;

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
            public char nextChar() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextChar();
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
    public CharStream flatMapArrayToChar(final Function<? super T, char[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToChar(mapper);
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

    @Override
    public ByteStream flatMapToByte(final Function<? super T, ? extends ByteStream> mapper) throws IllegalStateException {
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
    public ByteStream flatMapArrayToByte(final Function<? super T, byte[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToByte(mapper);
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

    @Override
    public ShortStream flatMapToShort(final Function<? super T, ? extends ShortStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = new ShortIteratorEx() {
            private int cursor = fromIndex;
            private ShortIterator cur = null;
            private ShortStream s = null;

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
            public short nextShort() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextShort();
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
    public ShortStream flatMapArrayToShort(final Function<? super T, short[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToShort(mapper);
        }

        return newStream(new ShortIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private short[] cur = null;
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

    @Override
    public IntStream flatMapArrayToInt(final Function<? super T, int[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToInt(mapper);
        }

        return newStream(new IntIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private int[] cur = null;
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
            private int cursor = fromIndex;
            private LongIterator cur = null;
            private LongStream s = null;

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
            public long nextLong() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextLong();
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
    public LongStream flatMapArrayToLong(final Function<? super T, long[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToLong(mapper);
        }

        return newStream(new LongIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private long[] cur = null;
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
            private int cursor = fromIndex;
            private FloatIterator cur = null;
            private FloatStream s = null;

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
    public FloatStream flatMapArrayToFloat(final Function<? super T, float[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToFloat(mapper);
        }

        return newStream(new FloatIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private float[] cur = null;
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
            private int cursor = fromIndex;
            private DoubleIterator cur = null;
            private DoubleStream s = null;

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
    public DoubleStream flatMapArrayToDouble(final Function<? super T, double[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArrayToDouble(mapper);
        }

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private double[] cur = null;
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
            public double nextDouble() {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, false);
    }

    @Override
    public Stream<List<T>> split(final int chunkSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(chunkSize, cs.chunkSize);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                // return Stream.slice(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex));

                return Array.asList(N.copyOfRange(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex)));
            }

            @Override
            public long count() {
                // IteratorEx.count() must consume the iterator, so drain the cursor
                // before returning (a subsequent hasNext() must return false).
                final long len = toIndex - cursor; //NOSONAR
                cursor = toIndex;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                final long len = toIndex - cursor; //NOSONAR
                final long advance = n <= len / chunkSize ? n * chunkSize : len;
                cursor = (int) (cursor + Math.min(advance, Integer.MAX_VALUE - cursor));
            }
        }, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(chunkSize, cs.chunkSize);
        checkArgNotNull(collectionSupplier, cs.collectionSupplier);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public C next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C result = collectionSupplier.apply(Math.min(toIndex - cursor, chunkSize));

                for (final int to = (cursor < toIndex - chunkSize ? cursor + chunkSize : toIndex); cursor < to; cursor++) {
                    result.add(elements[cursor]); //NOSONAR
                }

                return result;
            }

            @Override
            public long count() {
                // IteratorEx.count() must consume the iterator, so drain the cursor
                // before returning (a subsequent hasNext() must return false).
                final long len = toIndex - cursor; //NOSONAR
                cursor = toIndex;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                final long len = toIndex - cursor; //NOSONAR
                final long advance = n <= len / chunkSize ? n * chunkSize : len;
                cursor = (int) (cursor + Math.min(advance, Integer.MAX_VALUE - cursor));
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
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Object container = supplier.get();

                for (final int to = (cursor < toIndex - chunkSize ? cursor + chunkSize : toIndex); cursor < to; cursor++) {
                    accumulator.accept(container, elements[cursor]);
                }

                return finisher.apply(container);
            }

            @Override
            public long count() {
                // IteratorEx.count() must consume the iterator, so drain the cursor
                // before returning (a subsequent hasNext() must return false).
                final long len = toIndex - cursor; //NOSONAR
                cursor = toIndex;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                final long len = toIndex - cursor; //NOSONAR
                final long advance = n <= len / chunkSize ? n * chunkSize : len;
                cursor = (int) (cursor + Math.min(advance, Integer.MAX_VALUE - cursor));
            }
        }, false, null);
    }

    @Override
    public Stream<List<T>> split(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final int from = cursor;

                while (cursor < toIndex) {
                    if (from == cursor) {
                        preCondition = predicate.test(elements[cursor]);
                    } else if (predicate.test(elements[cursor]) != preCondition) {
                        break;
                    }

                    cursor++;
                }

                return StreamBase.slice(elements, from, cursor);
            }
        }, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> split(final Predicate<? super T> predicate, final Supplier<? extends C> collectionSupplier)
            throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public C next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C result = collectionSupplier.get();
                boolean isFirst = true;

                while (cursor < toIndex) {
                    if (isFirst) {
                        preCondition = predicate.test(elements[cursor]);
                        result.add(elements[cursor]);
                        cursor++;
                        isFirst = false;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        result.add(elements[cursor]);
                        cursor++;
                    } else {

                        break;
                    }
                }

                return result;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> split(final Predicate<? super T> predicate, final Collector<? super T, ?, R> collector)
            throws IllegalArgumentException, IllegalStateException {
        assertNotClosed();
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Object container = supplier.get();
                boolean isFirst = true;

                while (cursor < toIndex) {
                    if (isFirst) {
                        preCondition = predicate.test(elements[cursor]);
                        accumulator.accept(container, elements[cursor]);
                        cursor++;
                        isFirst = false;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        accumulator.accept(container, elements[cursor]);
                        cursor++;
                    } else {

                        break;
                    }
                }

                return finisher.apply(container);
            }
        }, false, null);
    }

    /**
     * Splits this stream at the given {@code position} into a stream of exactly two streams:
     * the first containing elements {@code [0, position)} and the second containing
     * elements {@code [position, size)}.
     *
     * <p>This is a stateful intermediate operation. A position beyond the stream's size
     * causes the second sub-stream to be empty.
     *
     * @param position the split point (exclusive end for the first sub-stream); must be
     *                 non-negative
     * @return a stream of two sub-streams split at {@code position}
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code position} is negative
     */
    @Override
    public Stream<Stream<T>> splitAt(final int position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        final Stream<T>[] a = new Stream[2];
        final int middleIndex = position < toIndex - fromIndex ? fromIndex + position : toIndex;
        a[0] = middleIndex == fromIndex ? (Stream<T>) Stream.empty() : newStream(elements, fromIndex, middleIndex, isSorted(), comparator(), null);
        a[1] = middleIndex == toIndex ? (Stream<T>) Stream.empty() : newStream(elements, middleIndex, toIndex, isSorted(), comparator(), null);

        return newStream(a, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> sliding(final int windowSize, final int increment, final IntFunction<? extends C> collectionSupplier)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkSlidingWindowSize(windowSize, increment);
        checkArgNotNull(collectionSupplier, cs.collectionSupplier);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public C next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C result = collectionSupplier.apply(Math.min(windowSize, toIndex - cursor));

                for (int i = cursor, to = windowSize < toIndex - cursor ? cursor + windowSize : toIndex; i < to; i++) {
                    result.add(elements[i]); //NOSONAR
                }

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            @Override
            public long count() {
                final long ret = countSlidingWindows(toIndex - cursor, windowSize, increment);
                cursor = toIndex;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                // count() drains (sets cursor=toIndex), so compute the remaining-window
                // count locally without touching iterator state.
                final long remaining = countSlidingWindows(toIndex - cursor, windowSize, increment);
                if (n >= remaining) {
                    cursor = toIndex;
                } else {
                    cursor += (int) (n * increment);
                }
            }

        }, false, null);
    }

    @Override
    public <R> Stream<R> sliding(final int windowSize, final int increment, final Collector<? super T, ?, R> collector)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkSlidingWindowSize(windowSize, increment);
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public R next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Object container = supplier.get();

                for (int i = cursor, to = windowSize < toIndex - cursor ? cursor + windowSize : toIndex; i < to; i++) {
                    accumulator.accept(container, elements[i]);
                }

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return finisher.apply(container);
            }

            @Override
            public long count() {
                final long ret = countSlidingWindows(toIndex - cursor, windowSize, increment);
                cursor = toIndex;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                // count() drains (sets cursor=toIndex), so compute the remaining-window
                // count locally without touching iterator state.
                final long remaining = countSlidingWindows(toIndex - cursor, windowSize, increment);
                if (n >= remaining) {
                    cursor = toIndex;
                } else {
                    cursor += (int) (n * increment);
                }
            }

        }, false, null);
    }

    private void checkSlidingWindowSize(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
    }

    /**
     * Returns a stream consisting of the distinct elements of this stream, preserving encounter order.
     * Equality and hashing, rather than ordering equivalence, determine whether elements are duplicates.
     * This remains true for naturally sorted streams because a type's natural ordering is not required
     * to be consistent with {@link Object#equals(Object)}.
     *
     * <p>This is a stateful intermediate operation that maintains a set of previously seen elements.
     *
     * @return a new stream containing only the distinct elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        // A sorted stream cannot safely use adjacent equality alone: Comparable explicitly permits
        // orderings that are inconsistent with equals, so equal elements need not be adjacent.
        // noinspection resource
        return newStream(sequential().filter(value -> set.add(hashKey(value))).iteratorEx(), isSorted(), comparator());
    }

    /**
     * Returns a stream consisting of at most {@code maxSize} elements from the start of this stream.
     *
     * <p>This is a short-circuit stateful intermediate operation. If this stream already contains
     * at most {@code maxSize} elements, {@code this} is returned unchanged (no copy).
     * Otherwise a new view over the same backing array is created.
     *
     * @param maxSize the maximum number of elements the returned stream may contain; must be
     *                non-negative
     * @return a stream containing at most {@code maxSize} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    @Override
    public Stream<T> limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        if (toIndex - fromIndex <= maxSize) {
            return this;
        }

        return newStream(elements, fromIndex, maxSize < toIndex - fromIndex ? (int) (fromIndex + maxSize) : toIndex, isSorted(), comparator());
    }

    /**
     * Returns a stream that skips the first {@code n} elements of this stream.
     *
     * <p>This is a stateful intermediate operation. If {@code n} is zero or the stream is
     * already empty, {@code this} is returned unchanged. Otherwise a new view over the
     * same backing array starting at {@code fromIndex + n} is created.
     *
     * @param n the number of leading elements to skip; must be non-negative
     * @return a stream consisting of the remaining elements after skipping {@code n}
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public Stream<T> skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0 || fromIndex == toIndex) {
            return this;
        }

        if (n >= toIndex - fromIndex) {
            return newStream(elements, toIndex, toIndex, isSorted(), comparator());
        } else {
            return newStream(elements, (int) (fromIndex + n), toIndex, isSorted(), comparator());
        }
    }

    @Override
    public Stream<T> top(final int n, final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return newStream(elements, fromIndex, fromIndex, isSorted(), comparator());
        }

        if (n >= toIndex - fromIndex) {
            return newStream(elements, fromIndex, toIndex, isSorted(), comparator());
        } else if (isSorted() && isSameComparator(comparator, comparator())) {
            return newStream(elements, toIndex - n, toIndex, isSorted(), comparator());
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean initialized = false;
            private T[] sortedTopElements;
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

                return sortedTopElements[cursor++];
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
            public <A> A[] toArray(A[] a) {
                if (!initialized) {
                    init();
                }

                final int len = to - cursor;
                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);
                N.copy(sortedTopElements, cursor, a, 0, len);

                if (a.length > len) {
                    a[len] = null;
                }

                cursor = to; // consume all elements
                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    final List<T> topElements = N.top(elements, fromIndex, toIndex, n, comparator);
                    sortedTopElements = topElements.toArray((T[]) new Object[topElements.size()]);
                    to = sortedTopElements.length;
                }
            }
        }, false, null);
    }

    /**
     * Returns a stream that performs the given action on each element as elements are consumed.
     *
     * <p>This is a stateless intermediate operation intended primarily for debugging.
     * The sorted state and comparator are preserved in the returned stream.
     *
     * @param action the action to perform on each element; must not be {@code null}
     * @return a new stream that passes each element through {@code action} before yielding it
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Stream<T> onEach(final Consumer<? super T> action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public T next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public <A> A[] toArray(A[] a) {
                final int len = toIndex - cursor;
                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                for (int i = 0; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = (A) elements[cursor++];
                }

                if (a.length > len) {
                    a[len] = null;
                }

                return a;
            }
        }, isSorted(), comparator());
    }

    @Override
    public <E extends Exception, E2 extends Exception> void forEach(final Throwables.Consumer<? super T, E> action, final Throwables.Runnable<E2> onComplete)
            throws E, E2 {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(elements[i]);
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

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                c = flatMapper.apply(elements[i]);

                if (c != null) {
                    for (final U u : c) {
                        action.accept(elements[i], u);
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

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                c2 = flatMapper.apply(elements[i]);

                if (c2 != null) {
                    for (final T2 t2 : c2) {
                        c3 = flatMapper2.apply(t2);

                        if (c3 != null) {
                            for (final T3 t3 : c3) {
                                action.accept(elements[i], t2, t3);
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
            int cursor = fromIndex;

            while (cursor < toIndex) {
                action.accept(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;
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
            int cursor = fromIndex;

            while (cursor < toIndex) {
                action.accept(elements[cursor], cursor < toIndex - 1 ? elements[cursor + 1] : null, cursor < toIndex - 2 ? elements[cursor + 2] : null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;
            }
        } finally {
            close();
        }
    }

    @Override
    protected Object[] toArray(final boolean closeStream) {
        assertNotClosed();

        try {
            return N.copyOfRange(elements, fromIndex, toIndex);
        } finally {
            if (closeStream) {
                close();
            }
        }
    }

    <A> A[] toArray(A[] a) {
        assertNotClosed();

        try {
            if (a.length < (toIndex - fromIndex)) {
                a = N.newArray(a.getClass().getComponentType(), toIndex - fromIndex);
            }

            N.copy(elements, fromIndex, a, 0, toIndex - fromIndex);

            return a;
        } finally {
            close();
        }
    }

    /**
     * Returns an array containing all elements of this stream, using the provided generator
     * to allocate the returned array.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param <A> the component type of the array
     * @param generator a function that produces a new array of the desired type and the
     *                  provided length; must not be {@code null}
     * @return an array containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <A> A[] toArray(final IntFunction<A[]> generator) throws IllegalStateException {
        assertNotClosed();

        try {
            return toArray(generator.apply(toIndex - fromIndex));
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@link List}, in encounter order.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @return a mutable {@link List} containing all stream elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public List<T> toList() throws IllegalStateException {
        assertNotClosed();

        try {
            return N.toList(elements, fromIndex, toIndex);
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Set<T> toSet() throws IllegalStateException {
        assertNotClosed();

        try {
            return N.toSet(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    @Override
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final C result = supplier.get();

            if (toIndex - fromIndex == elements.length) {
                result.addAll(Arrays.asList(elements));
            } else if (toIndex - fromIndex > 9 && result instanceof List) {
                result.addAll(Arrays.asList(N.copyOfRange(elements, fromIndex, toIndex)));
            } else {
                //noinspection ManualArrayToCollectionCopy
                for (int i = fromIndex; i < toIndex; i++) {
                    result.add(elements[i]); //NOSONAR
                }
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
            final Multiset<T> result = N.newMultiset(N.min(64, (toIndex - fromIndex) / 2));

            //noinspection ManualArrayToCollectionCopy
            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]); //NOSONAR
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

            //noinspection ManualArrayToCollectionCopy
            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]); //NOSONAR
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

            for (int i = fromIndex; i < toIndex; i++) {
                Collectors.merge(result, keyMapper.apply(elements[i]), valueMapper.apply(elements[i]), mergeFunction);
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

            for (int i = fromIndex; i < toIndex; i++) {
                result.put(keyMapper.apply(elements[i]), valueMapper.apply(elements[i]));
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns an Optional containing the first element of this stream, or an empty Optional
     * if the stream is empty.
     *
     * <p>This is a terminal short-circuit operation. The stream is closed after this call.
     *
     * @return an Optional containing the first element, or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> first() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? Optional.of(elements[fromIndex]) : Optional.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns an Optional containing the last element of this stream, or an empty Optional
     * if the stream is empty.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @return an Optional containing the last element, or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> last() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? Optional.of(elements[toIndex - 1]) : Optional.empty();
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> elementAt(final long position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        try {
            if (position < toIndex - fromIndex) {
                return Optional.of(elements[fromIndex + (int) position]);
            } else {
                return Optional.empty();
            }
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public Optional<T> onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return Optional.empty();
            } else if (size == 1) {
                return Optional.of(elements[fromIndex]);
            } else {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(elements[fromIndex], ", ", elements[fromIndex + 1]));
            }
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> foldLeft(final BinaryOperator<T> accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            }

            T result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = accumulator.apply(result, elements[i]);
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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <U> U foldLeft(final U identity, final BiFunction<? super U, ? super T, U> accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            U result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = accumulator.apply(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a right fold on the elements of this stream using the given binary operator,
     * traversing the backing array from the last element to the first.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param accumulator a function that combines the running result with the current element
     *                    (applied right-to-left); must not be {@code null}
     * @return an Optional containing the result of folding all elements right-to-left,
     *         or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> foldRight(final BinaryOperator<T> accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            } else if (toIndex - fromIndex == 1) {
                return Optional.of(elements[fromIndex]);
            }

            T result = elements[toIndex - 1];

            for (int i = toIndex - 2; i >= fromIndex; i--) {
                result = accumulator.apply(result, elements[i]);
            }

            return Optional.of(result);
        } finally {
            close();
        }
    }

    /**
     * Performs a right fold on the elements of this stream using the given accumulator function
     * and an initial identity value, traversing the backing array from the last element to the first.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param <U> the type of the accumulated result
     * @param identity the initial accumulation value
     * @param accumulator a function combining the running result with the current element
     *                    (applied right-to-left); must not be {@code null}
     * @return the result of folding all elements right-to-left, starting from {@code identity}
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <U> U foldRight(final U identity, final BiFunction<? super U, ? super T, U> accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            U result = identity;

            for (int i = toIndex - 1; i >= fromIndex; i--) {
                result = accumulator.apply(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a reduction on the elements of this stream using the given binary operator,
     * returning an Optional with the result or an empty Optional if the stream is empty.
     * This method delegates to {@link #foldLeft(BinaryOperator)}.
     *
     * <p>This is a terminal operation.
     *
     * @param accumulator the associative function used to reduce elements; must not be {@code null}
     * @return an Optional describing the reduction result, or an empty Optional if the stream
     *         is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> reduce(final BinaryOperator<T> accumulator) {
        assertNotClosed();

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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <U> U reduce(final U identity, final BiFunction<? super U, ? super T, U> accumulator, final BinaryOperator<U> combiner) {
        assertNotClosed();

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
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super T> accumulator, final BiConsumer<R, R> combiner)
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
     * Performs a mutable reduction on the elements of this stream using a {@link Collector}.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @param <R> the type of the result
     * @param collector the {@link Collector} describing the reduction; must not be {@code null}
     * @return the result of the collection
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Collector<? super T, ?, R> collector) throws IllegalStateException {
        assertNotClosed();

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        try {
            final Object container = supplier.get();
            for (int i = fromIndex; i < toIndex; i++) {
                accumulator.accept(container, elements[i]);
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
     * <p>This is an optimized intermediate operation that creates a new
     * view over the same backing array without copying elements.
     *
     * @param n the maximum number of trailing elements to include; must be non-negative
     * @return a new stream containing at most the last {@code n} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     * @see #skipLast(int)
     */
    @Override
    public Stream<T> takeLast(final int n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (toIndex - fromIndex <= n) {
            return newStream(elements, fromIndex, toIndex, isSorted(), comparator());
        }

        return newStream(elements, toIndex - n, toIndex, isSorted(), comparator());
    }

    /**
     * Returns a stream that excludes the last {@code n} elements of this stream.
     * If {@code n == 0}, an equivalent stream containing all elements is returned.
     *
     * <p>This is an optimized intermediate operation that creates a new view over the
     * same backing array without copying elements.
     *
     * @param n the number of trailing elements to skip; must be non-negative
     * @return a new stream without the last {@code n} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     * @see #takeLast(int)
     */
    @Override
    public Stream<T> skipLast(final int n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return newStream(elements, fromIndex, toIndex, isSorted(), comparator());
        }

        return newStream(elements, fromIndex, N.max(fromIndex, toIndex - n), isSorted(), comparator());
    }

    /**
     * Returns an Optional containing the minimum element of this stream according to the given
     * comparator, or an empty Optional if the stream is empty.
     *
     * <p>This is a terminal operation. When the stream is sorted according to the given comparator,
     * the first element is returned directly in O(1) time.
     *
     * @param comparator the comparator used to compare elements; if {@code null}, nulls-last natural
     *                   ordering is used
     * @return an Optional containing the minimum element, or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> min(Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator(), comparator)) {
                return Optional.of(elements[fromIndex]);
            }

            comparator = comparator == null ? NULL_MAX_COMPARATOR : comparator;

            return Optional.of(N.min(elements, fromIndex, toIndex, comparator));
        } finally {
            close();
        }
    }

    /**
     * Returns an Optional containing the maximum element of this stream according to the given
     * comparator, or an empty Optional if the stream is empty.
     *
     * <p>This is a terminal operation. When the stream is sorted according to the given comparator,
     * the last element is returned directly in O(1) time.
     *
     * @param comparator the comparator used to compare elements; if {@code null}, nulls-first natural
     *                   ordering is used
     * @return an Optional containing the maximum element, or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Optional<T> max(Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator(), comparator)) {
                return Optional.of(elements[toIndex - 1]);
            }

            comparator = comparator == null ? NULL_MIN_COMPARATOR : comparator;

            return Optional.of(N.max(elements, fromIndex, toIndex, comparator));
        } finally {
            close();
        }
    }

    @Override
    public List<T> maxAll(final Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return collect(Collectors.maxAll(comparator));
        } else {
            try {
                final List<T> result = new ArrayList<>();

                if (fromIndex == toIndex) {
                    return result;
                }

                if (isSorted() && isSameComparator(comparator(), comparator)) {
                    final Comparator<? super T> cmp = comparator() == null ? NULL_MIN_COMPARATOR : comparator();
                    final T candidate = elements[toIndex - 1];
                    result.add(candidate);

                    for (int i = toIndex - 2; i >= fromIndex; i--) {
                        if (cmp.compare(elements[i], candidate) == 0) {
                            result.add(elements[i]);
                        } else {
                            break;
                        }
                    }
                } else {
                    final Comparator<? super T> cmp = comparator == null ? NULL_MIN_COMPARATOR : comparator;
                    T candidate = elements[fromIndex];
                    result.add(candidate);
                    int cp = 0;

                    for (int i = fromIndex + 1; i < toIndex; i++) {
                        cp = cmp.compare(elements[i], candidate);

                        if (cp == 0) {
                            result.add(elements[i]);
                        } else if (cp > 0) {
                            result.clear();
                            result.add(elements[i]);
                            candidate = elements[i];
                        }
                    }
                }

                return result;
            } finally {
                close();
            }
        }
    }

    @Override
    public Optional<T> kthLargest(final int k, final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (k > toIndex - fromIndex) {
                return Optional.empty();
            } else if (isSorted() && isSameComparator(comparator(), comparator)) {
                return Optional.of(elements[toIndex - k]);
            }

            return Optional.of(N.kthLargest(elements, fromIndex, toIndex, k, comparator));
        } finally {
            close();
        }
    }

    /**
     * Returns the count of elements in this stream.
     *
     * <p>This is an optimized terminal operation that returns the answer in O(1) time by
     * computing {@code toIndex - fromIndex} directly without iterating. The stream is closed
     * after this call.
     *
     * @return the number of elements in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public long count() throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        try {
            return toIndex - fromIndex; //NOSONAR
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
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
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
     * Returns {@code true} if every element of this stream matches the given predicate,
     * or if the stream is empty.
     *
     * <p>This is a terminal short-circuit operation. The stream is closed after this call.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to apply to elements; must not be {@code null}
     * @return {@code true} if all elements match the predicate (or the stream is empty),
     *         {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> boolean allMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
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
     * Returns {@code true} if no element of this stream matches the given predicate,
     * or if the stream is empty.
     *
     * <p>This is a terminal short-circuit operation. The stream is closed after this call.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to apply to elements; must not be {@code null}
     * @return {@code true} if no element matches the predicate (or the stream is empty),
     *         {@code false} otherwise
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
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

    @Override
    public <E extends Exception> boolean hasMatchCountBetween(final long atLeast, final long atMost, final Throwables.Predicate<? super T, E> predicate)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNegative(atLeast, cs.atLeast);
        checkArgNotNegative(atMost, cs.atMost);
        checkArgument(atLeast <= atMost, "'atLeast' (%s) must be <= 'atMost' (%s)", atLeast, atMost);

        long cnt = 0;

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i]) && (++cnt > atMost)) {
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
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> Optional<T> findFirst(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return Optional.of(elements[i]);
                }
            }

            return Optional.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns an Optional containing the last element of this stream that matches the given
     * predicate, or an empty Optional if no element matches.
     *
     * <p>This is a terminal operation that traverses the backing array in reverse. The stream
     * is closed after this call.
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test elements against; must not be {@code null}
     * @return an Optional containing the last matching element, or an empty Optional if none match
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the predicate throws a checked exception
     */
    @Override
    public <E extends Exception> Optional<T> findLast(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return Optional.of(elements[i]);
                }
            }

            return Optional.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns an infinite stream that cycles through the elements of this stream repeatedly.
     *
     * <p>This is an intermediate operation. The stream must be terminated with a limit operation
     * to avoid an infinite loop.
     *
     * <pre>{@code
     * // Produces [1, 2, 3, 1, 2, 3, 1]
     * Stream.of(1, 2, 3).cycled().limit(7);
     * }</pre>
     *
     * @return a new infinite stream cycling through this stream's elements
     * @throws IllegalStateException if the stream is already closed
     * @see #cycled(long)
     */
    @Override
    public Stream<T> cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex;
            }

            @Override
            public T next() {
                if (fromIndex >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (cursor >= toIndex) {
                    cursor = fromIndex;
                }

                return elements[cursor++];
            }
        }, false, null);
    }

    /**
     * Returns a stream that cycles through the elements of this stream for exactly {@code rounds}
     * complete repetitions.
     *
     * <p>This is a stateful intermediate operation. A value of {@code 0} returns an empty stream;
     * a value of {@code 1} is equivalent to this stream unchanged.
     *
     * @param rounds the number of full cycles to produce; must be non-negative
     * @return a new stream cycling this stream's elements {@code rounds} times
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code rounds} is negative
     * @see #cycled()
     */
    @Override
    public Stream<T> cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = fromIndex;
            private long m = 0;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex && m < rounds && (cursor < toIndex || rounds - m > 1);
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (cursor >= toIndex) {
                    cursor = fromIndex;
                    m++;
                }

                return elements[cursor++];
            }
        }, rounds <= 1 && isSorted(), rounds <= 1 ? comparator() : null);
    }

    @Override
    public Stream<T> buffered(final int bufferSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(bufferSize, cs.bufferSize);

        // return this;
        return newStream(elements, fromIndex, toIndex, isSorted(), comparator());
    }

    @Override
    public Stream<T> appendIfEmpty(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        return fromIndex == toIndex ? append(c) : this;
    }

    @Override
    public Stream<T> appendIfEmpty(final Supplier<? extends Stream<T>> supplier) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
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
                        final Stream<T> s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }, false, null).onClose(() -> close(holder));
        } else {
            return this;
        }
    }

    @Override
    public Stream<T> ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            return newStream(new ObjIteratorEx<>() { //NOSONAR
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
                public T next() {
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

    @Override
    ObjIteratorEx<T> iteratorEx() {
        assertNotClosed();

        return ObjIteratorEx.of(elements, fromIndex, toIndex);
    }

    @Override
    protected Tuple3<Object[], Integer, Integer> arrayForIntermediateOp() {
        assertNotClosed();

        return Tuple.of(elements, fromIndex, toIndex);
    }

    /**
     * Creates a parallel version of this array-backed stream using the specified parameters.
     *
     * @param maxThreadNum the maximum number of threads for parallel execution
     * @param splitor the strategy used to split the array for parallel processing
     * @param asyncExecutor the executor for submitting parallel tasks
     * @param cancelUncompletedThreads whether to cancel uncompleted threads when the stream is closed
     * @return a new {@link ParallelArrayStream} over the same backing array range
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected Stream<T> parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelArrayStream<>(elements, fromIndex, toIndex, isSorted(), comparator(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                closeHandlers());
    }

    /**
     * Returns a standard JDK {@link java.util.stream.Stream} backed by this stream's underlying array.
     * Close handlers registered on this stream are forwarded to the returned JDK stream.
     *
     * <p>This is a terminal operation that transfers ownership to the returned JDK stream.
     *
     * @return a JDK {@link java.util.stream.Stream} over the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public java.util.stream.Stream<T> toJdkStream() throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            if (isEmptyCloseHandlers(closeHandlers())) {
                return Arrays.stream(elements, fromIndex, toIndex).parallel();
            } else {
                return Arrays.stream(elements, fromIndex, toIndex).parallel().onClose(this::close);
            }
        } else {
            if (isEmptyCloseHandlers(closeHandlers())) {
                return Arrays.stream(elements, fromIndex, toIndex);
            } else {
                return Arrays.stream(elements, fromIndex, toIndex).onClose(this::close);
            }
        }
    }

    /**
     * Returns {@code true} if this stream contains no elements (i.e., {@code fromIndex >= toIndex}).
     *
     * @return {@code true} if this stream is empty
     */
    @Override
    protected boolean isEmpty() {
        return fromIndex >= toIndex;
    }
}
