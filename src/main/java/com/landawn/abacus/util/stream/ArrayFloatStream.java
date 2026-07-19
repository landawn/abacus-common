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
import java.util.Comparator;
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
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
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
 * An array-backed implementation of FloatStream that provides efficient sequential access to a float array
 * or a portion of it. This implementation is optimized for working with primitive float arrays and offers
 * high performance for operations on contiguous float sequences.
 *
 * <p>Key features:
 * <ul>
 * <li>Zero-copy operations when possible, working directly on the underlying array</li>
 * <li>Support for partial array ranges via fromIndex and toIndex</li>
 * <li>Optimized implementations of filter, map, flatMap, and terminal operations</li>
 * <li>Efficient sorted stream handling with specialized algorithms</li>
 * <li>Type conversion support to other primitive stream types</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
 * FloatStream stream = FloatStream.of(data);
 * float max = stream.max().orElse(0.0f);
 *
 * // Filtering and mapping
 * FloatStream positive = FloatStream.of(data)
 *     .filter(n -> n > 2.0f)
 *     .map(n -> n * 1.5f);
 *
 * // Statistical operations
 * FloatStream stats = FloatStream.of(data);
 * FloatSummaryStatistics summary = stats.summaryStatistics();
 * System.out.println("Average: " + summary.getAverage());
 * }</pre>
 *
 */
class ArrayFloatStream extends AbstractFloatStream {
    final float[] elements;
    final int fromIndex;
    final int toIndex;

    /**
     * Constructs an ArrayFloatStream from the entire float array.
     * This constructor creates a stream that processes all elements in the provided array
     * from index 0 to the end of the array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
     * FloatStream stream = new ArrayFloatStream(data);
     * }</pre>
     *
     * @param values the float array to stream over
     */
    ArrayFloatStream(final float[] values) {
        this(values, 0, values.length);
    }

    /**
     * Constructs an ArrayFloatStream from the entire float array with close handlers.
     * The close handlers will be executed when the stream is closed, allowing for
     * resource cleanup or other post-processing operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
     * List<LocalRunnable> handlers = new ArrayList<>();
     * handlers.add(() -> System.out.println("Stream closed"));
     * FloatStream stream = new ArrayFloatStream(data, handlers);
     * }</pre>
     *
     * @param values the float array to stream over
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     */
    ArrayFloatStream(final float[] values, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    /**
     * Constructs an ArrayFloatStream from the entire float array with sorting state and close handlers.
     * The sorted flag indicates whether the array is already in sorted order, which can be used
     * to optimize certain stream operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] sortedData = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
     * List<LocalRunnable> handlers = new ArrayList<>();
     * FloatStream stream = new ArrayFloatStream(sortedData, true, handlers);
     * }</pre>
     *
     * @param values the float array to stream over
     * @param sorted whether the array elements are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     */
    ArrayFloatStream(final float[] values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    /**
     * Constructs an ArrayFloatStream from a range within the float array.
     * This allows streaming over a subset of the array elements, from the specified
     * start index (inclusive) to the end index (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};
     * // Stream over elements from index 2 to 5 (exclusive): {3.0f, 4.0f, 5.0f}
     * FloatStream stream = new ArrayFloatStream(data, 2, 5);
     * }</pre>
     *
     * @param values the float array to stream over
     * @param fromIndex the start index (inclusive) of the range to stream
     * @param toIndex the end index (exclusive) of the range to stream
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayFloatStream(final float[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    /**
     * Constructs an ArrayFloatStream from a range within the float array with close handlers.
     * Combines range specification with close handler support for resource management.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] data = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};
     * List<LocalRunnable> handlers = new ArrayList<>();
     * handlers.add(() -> System.out.println("Stream closed"));
     * // Stream over elements from index 2 to 5 with close handlers
     * FloatStream stream = new ArrayFloatStream(data, 2, 5, handlers);
     * }</pre>
     *
     * @param values the float array to stream over
     * @param fromIndex the start index (inclusive) of the range to stream
     * @param toIndex the end index (exclusive) of the range to stream
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayFloatStream(final float[] values, final int fromIndex, final int toIndex, final Collection<LocalRunnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    /**
     * Constructs an ArrayFloatStream from a range within the float array with all configuration options.
     * This is the primary constructor that all other constructors delegate to. It provides full
     * control over the stream configuration including range, sorting state, and close handlers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] sortedData = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f};
     * List<LocalRunnable> handlers = new ArrayList<>();
     * handlers.add(() -> System.out.println("Stream closed"));
     * // Stream over sorted elements from index 2 to 6 with close handlers
     * FloatStream stream = new ArrayFloatStream(sortedData, 2, 6, true, handlers);
     * }</pre>
     *
     * @param values the float array to stream over
     * @param fromIndex the start index (inclusive) of the range to stream
     * @param toIndex the end index (exclusive) of the range to stream
     * @param sorted whether the array elements in the range are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayFloatStream(final float[] values, final int fromIndex, final int toIndex, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
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
     * @return a new {@code FloatStream} containing only matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream filter(final FloatPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
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
            public float nextFloat() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of elements from the start of this stream while the given predicate holds.
     * Once an element fails the predicate, no further elements are included.
     * Iterates the backing array from {@code fromIndex}.
     *
     * @param predicate the non-interfering, stateless predicate to apply to each element
     * @return a new {@code FloatStream} of elements from the start while {@code predicate} is satisfied
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream takeWhile(final FloatPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
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
            public float nextFloat() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    /**
     * Returns a stream that skips elements from the start of this stream while the given predicate holds,
     * then includes all remaining elements. Iterates the backing array from {@code fromIndex}.
     *
     * @param predicate the non-interfering, stateless predicate to apply to each element
     * @return a new {@code FloatStream} with leading elements dropped while {@code predicate} holds
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream dropWhile(final FloatPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
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
            public float nextFloat() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of every {@code step}-th element from this stream,
     * starting from the first element.
     *
     * <pre>{@code
     * // FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f).step(2) => 1.0f, 3.0f, 5.0f
     * }</pre>
     *
     * @param step the stride between successive elements; must be positive
     * @return a new {@code FloatStream} containing every {@code step}-th element
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code step} is not positive
     */
    @Override
    public FloatStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1 || fromIndex == toIndex) {
            return skip(0);
        }

        return newStream(new FloatIteratorEx() { //NOSONAR
            private final int stepToUse = (int) N.min(step, Integer.MAX_VALUE);
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final float res = elements[cursor];
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
            public float[] toArray() {
                final long rem = toIndex - cursor;
                final int len = (int) ((rem % stepToUse == 0) ? rem / stepToUse : rem / stepToUse + 1);
                final float[] a = new float[len];

                for (int i = 0; i < len; i++) {
                    a[i] = elements[cursor];
                    cursor = cursor > toIndex - stepToUse ? toIndex : cursor + stepToUse;
                }

                return a;
            }
        }, isSorted());
    }

    /**
     * Returns a stream consisting of the results of applying the given function to the elements of this stream.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code FloatStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream map(final FloatUnaryOperator mapper) throws IllegalStateException {
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

    /**
     * Returns an {@code IntStream} consisting of the results of applying the given function to the elements of this stream.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code IntStream} of mapped int values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final FloatToIntFunction mapper) throws IllegalStateException {
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
     * Returns a {@code LongStream} consisting of the results of applying the given function to the elements of this stream.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code LongStream} of mapped long values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public LongStream mapToLong(final FloatToLongFunction mapper) throws IllegalStateException {
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

    /**
     * Returns a {@code DoubleStream} consisting of the results of applying the given function to the elements of this stream.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code DoubleStream} of mapped double values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream mapToDouble(final FloatToDoubleFunction mapper) throws IllegalStateException {
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

    /**
     * Returns a {@code Stream} consisting of the results of applying the given function to the elements of this stream.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param <T> the type of elements in the resulting stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code Stream<T>} of mapped object values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> mapToObj(final FloatFunction<? extends T> mapper) throws IllegalStateException {
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
     * Returns a stream formed by replacing each element with the contents of a mapped {@code FloatStream}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code FloatStream}
     * @return a new {@code FloatStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream flatMap(final FloatFunction<? extends FloatStream> mapper) throws IllegalStateException {
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
     * Returns a stream formed by replacing each element with the contents of a mapped {@code Collection<Float>}.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a collection of floats
     * @return a new {@code FloatStream} formed by concatenating the mapped collections
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream flatmap(final FloatFunction<? extends Collection<Float>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private Iterator<Float> cur = null;
            private Collection<Float> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && cursor < toIndex) {
                    c = mapper.apply(elements[cursor++]);
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

    /**
     * Returns a stream formed by replacing each element with the contents of a mapped {@code float[]} array.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code float[]}
     * @return a new {@code FloatStream} formed by concatenating the mapped arrays
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream flatMapArray(final FloatFunction<float[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArray(mapper);
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

    /**
     * Returns an {@code IntStream} formed by replacing each element with the contents of a mapped {@code IntStream}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to an {@code IntStream}
     * @return a new {@code IntStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final FloatFunction<? extends IntStream> mapper) throws IllegalStateException {
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
     * Returns a {@code LongStream} formed by replacing each element with the contents of a mapped {@code LongStream}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code LongStream}
     * @return a new {@code LongStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public LongStream flatMapToLong(final FloatFunction<? extends LongStream> mapper) throws IllegalStateException {
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

    /**
     * Returns a {@code DoubleStream} formed by replacing each element with the contents of a mapped {@code DoubleStream}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code DoubleStream}
     * @return a new {@code DoubleStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream flatMapToDouble(final FloatFunction<? extends DoubleStream> mapper) throws IllegalStateException {
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

    /**
     * Returns a {@code Stream} formed by replacing each element with the contents of a mapped {@code Stream<T>}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
     *
     * @param <T> the type of elements in the resulting stream
     * @param mapper a non-interfering, stateless function mapping each {@code float} element to a {@code Stream<T>}
     * @return a new {@code Stream<T>} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatMapToObj(final FloatFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
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
     * Returns a {@code Stream} formed by replacing each element with the contents of a mapped {@code Collection<T>}.
     * Iterates the backing array; each mapped collection is iterated after it is produced.
     *
     * @param <T> the type of elements in the resulting stream
     * @param mapper a non-interfering, stateless function mapping each {@code float} element to a {@code Collection<T>}
     * @return a new {@code Stream<T>} formed by concatenating the mapped collections
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> flatmapToObj(final FloatFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
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
     * For sorted streams, uses an adjacent-element comparison for O(n) deduplication.
     * For unsorted streams, uses a hash set to track seen values.
     *
     * @return a new {@code FloatStream} containing only distinct elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new FloatIteratorEx() { //NOSONAR
                private int prev = -1;
                private int cur = fromIndex;

                @Override
                public boolean hasNext() {
                    if (cur > fromIndex && cur < toIndex && N.equals(elements[cur], elements[prev])) {
                        //noinspection StatementWithEmptyBody
                        while (++cur < toIndex && N.equals(elements[cur], elements[prev])) {
                            // do nothing
                        }
                    }

                    return cur < toIndex;
                }

                @Override
                public float nextFloat() {
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
     * Returns a stream consisting of no more than {@code maxSize} elements from the beginning of this stream.
     * Operates in O(1) by adjusting the {@code toIndex} of the backing array range.
     *
     * @param maxSize the maximum number of elements the returned stream may contain; must be non-negative
     * @return a new {@code FloatStream} truncated to at most {@code maxSize} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code maxSize} is negative
     */
    @Override
    public FloatStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        if (toIndex - fromIndex <= maxSize) {
            return this;
        }

        return newStream(elements, fromIndex, maxSize < toIndex - fromIndex ? (int) (fromIndex + maxSize) : toIndex, isSorted());
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream after discarding the first {@code n} elements.
     * Operates in O(1) by advancing the {@code fromIndex} of the backing array range.
     *
     * @param n the number of leading elements to skip; must be non-negative
     * @return a new {@code FloatStream} with the first {@code n} elements removed
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public FloatStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
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
     * Returns a stream consisting of the top {@code n} elements of this stream according to the given comparator.
     * If the stream is sorted and the comparator matches the natural ordering, an O(1) tail-range restriction is applied.
     * Otherwise, a partial sort over the backing array is performed.
     *
     * @param n the number of top elements to return; must be non-negative
     * @param comparator the comparator used to determine element ordering
     * @return a new {@code FloatStream} containing the top {@code n} elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     */
    @Override
    public FloatStream top(final int n, final Comparator<? super Float> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return newStream(elements, fromIndex, fromIndex, isSorted());
        }

        if (n >= toIndex - fromIndex) {
            return newStream(elements, fromIndex, toIndex, isSorted());
        } else if (isSorted() && isSameComparator(comparator, comparator())) {
            return newStream(elements, toIndex - n, toIndex, isSorted());
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
                    aar = N.top(elements, fromIndex, toIndex, n, comparator);
                    to = aar.length;
                }
            }
        }, false);
    }

    /**
     * Returns a stream that applies the given action to each element as elements are consumed
     * from the returned stream. This is an intermediate operation; the action is executed lazily.
     *
     * @param action a non-interfering action to perform on each element
     * @return a new {@code FloatStream} that invokes {@code action} on each element before passing it downstream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream onEach(final FloatConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public float[] toArray() {
                final float[] a = new float[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, isSorted());
    }

    /**
     * Performs the given action on each element of this stream. This is a terminal operation.
     * Iterates the backing array from {@code fromIndex} (inclusive) to {@code toIndex} (exclusive)
     * and closes the stream when done.
     *
     * @param <E> the type of exception that the action may throw
     * @param action a non-interfering action to perform on each element
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the action throws an exception of type {@code E}
     */
    @Override
    public <E extends Exception> void forEach(final Throwables.FloatConsumer<E> action) throws IllegalStateException, E {
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
     * Returns an array containing all elements of this stream.
     * Copies the backing array range {@code [fromIndex, toIndex)} into a new array.
     *
     * @param closeStream if {@code true}, the stream is closed after the array is produced
     * @return a {@code float[]} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected float[] toArray(final boolean closeStream) {
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
     * Collects all elements of this stream into a {@link FloatList}. This is a terminal operation.
     * Copies the backing array range {@code [fromIndex, toIndex)} into a new {@code FloatList} and closes the stream.
     *
     * @return a {@code FloatList} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatList toFloatList() throws IllegalStateException {
        assertNotClosed();

        try {
            return FloatList.of(N.copyOfRange(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@link List}. This is a terminal operation.
     * Boxes each {@code float} value into a {@link Float} and adds it to an {@code ArrayList}.
     * Closes the stream when done.
     *
     * @return a {@code List<Float>} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public List<Float> toList() throws IllegalStateException {
        assertNotClosed();

        try {
            final List<Float> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@link Set}. This is a terminal operation.
     * Boxes each {@code float} value into a {@link Float} and adds it to a hash set.
     * Closes the stream when done.
     *
     * @return a {@code Set<Float>} containing the distinct elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Set<Float> toSet() throws IllegalStateException {
        assertNotClosed();

        try {
            final Set<Float> result = N.newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a collection created by the given supplier.
     * This is a terminal operation. Boxes each {@code float} value and adds it to the supplied collection.
     * Closes the stream when done.
     *
     * @param <C> the type of the resulting collection
     * @param supplier a function that produces a new empty collection
     * @return the collection supplied by {@code supplier} populated with all stream elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <C extends Collection<Float>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
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
     * Collects all elements of this stream into a {@link Multiset} using the default implementation.
     * This is a terminal operation. Boxes each {@code float} value and adds it to the multiset.
     * Closes the stream when done.
     *
     * @return a {@code Multiset<Float>} containing all elements of this stream with their occurrence counts
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Float> toMultiset() throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Float> result = N.newMultiset(N.min(64, (toIndex - fromIndex) / 2));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects all elements of this stream into a {@link Multiset} created by the given supplier.
     * This is a terminal operation. Boxes each {@code float} value and adds it to the supplied multiset.
     * Closes the stream when done.
     *
     * @param supplier a function that produces a new empty {@code Multiset<Float>}
     * @return the multiset supplied by {@code supplier} populated with all stream elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Float> toMultiset(final Supplier<? extends Multiset<Float>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Float> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Collects the elements of this stream into a {@link Map} using the provided key and value mappers.
     * This is a terminal operation. When two elements map to the same key, the values are merged
     * using the provided {@code mergeFunction}. Closes the stream when done.
     *
     * @param <K> the type of map keys
     * @param <V> the type of map values
     * @param <M> the type of the resulting map
     * @param <E> the type of exception the key mapper may throw
     * @param <E2> the type of exception the value mapper may throw
     * @param keyMapper a function to produce map keys from stream elements
     * @param valueMapper a function to produce map values from stream elements
     * @param mergeFunction a merge function to resolve key collisions
     * @param mapFactory a supplier providing a new empty map of the desired type
     * @return a map whose keys and values are derived from the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     * @throws E if the key mapper throws
     * @throws E2 if the value mapper throws
     */
    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.FloatFunction<? extends K, E> keyMapper,
            final Throwables.FloatFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
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
     * Groups the elements of this stream by a classifier key, collecting each group's values with
     * the specified downstream {@link Collector}. This is a terminal operation. Closes the stream when done.
     *
     * @param <K> the type of the grouping keys
     * @param <D> the type of the downstream reduction result
     * @param <M> the type of the resulting map
     * @param <E> the type of exception the key mapper may throw
     * @param keyMapper a function mapping each element to a grouping key; must not return {@code null}
     * @param downstream a {@code Collector} that reduces the elements of each group
     * @param mapFactory a supplier providing a new empty map of the desired type
     * @return a map from keys to downstream-collected group results
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code keyMapper} returns {@code null} for any element
     * @throws E if the key mapper throws
     */
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
     * Returns an {@link OptionalFloat} describing the first element of this stream,
     * or an empty optional if the stream is empty. This is a terminal operation. Closes the stream.
     *
     * @return an {@code OptionalFloat} with the first element, or empty if the stream has no elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalFloat first() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalFloat.of(elements[fromIndex]) : OptionalFloat.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns an {@link OptionalFloat} describing the last element of this stream,
     * or an empty optional if the stream is empty. This is a terminal operation. Closes the stream.
     *
     * @return an {@code OptionalFloat} with the last element, or empty if the stream has no elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalFloat last() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalFloat.of(elements[toIndex - 1]) : OptionalFloat.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns an {@link OptionalFloat} describing the element at the given zero-based {@code position}
     * in this stream, or an empty optional if the position is beyond the end. This is a terminal operation.
     * Provides O(1) direct access using the backing array. Closes the stream.
     *
     * @param position the zero-based index of the desired element; must be non-negative
     * @return an {@code OptionalFloat} with the element at {@code position}, or empty if out of range
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code position} is negative
     */
    @Override
    public OptionalFloat elementAt(final long position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        try {
            if (position < toIndex - fromIndex) {
                return OptionalFloat.of(elements[fromIndex + (int) position]);
            } else {
                return OptionalFloat.empty();
            }
        } finally {
            close();
        }
    }

    /**
     * Returns an {@link OptionalFloat} describing the only element of this stream,
     * or an empty optional if the stream is empty. Throws {@link TooManyElementsException}
     * if the stream contains more than one element. This is a terminal operation. Closes the stream.
     *
     * @return an {@code OptionalFloat} with the single element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @throws TooManyElementsException if the stream contains more than one element
     */
    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalFloat onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return OptionalFloat.empty();
            } else if (size == 1) {
                return OptionalFloat.of(elements[fromIndex]);
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
    public float reduce(final float identity, final FloatBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            float result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = accumulator.applyAsFloat(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a reduction on the elements of this stream using an associative accumulation function,
     * and returns an {@link OptionalFloat} describing the reduced value, if any. Closes the stream.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalFloat} with the reduced value, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalFloat reduce(final FloatBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalFloat.empty();
            }

            float result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = accumulator.applyAsFloat(result, elements[i]);
            }

            return OptionalFloat.of(result);
        } finally {
            close();
        }
    }

    /**
     * Performs a mutable reduction on the elements of this stream using a supplier, accumulator, and combiner.
     * This is a terminal operation. Closes the stream.
     *
     * <pre>{@code
     * FloatList list = stream.collect(FloatList::new, FloatList::add, FloatList::addAll);
     * }</pre>
     *
     * @param <R> the type of the mutable result container
     * @param supplier a function that creates a new mutable result container
     * @param accumulator a function that folds an element into a result container
     * @param combiner a function that combines two result containers (used in parallel reductions)
     * @return the mutable result container populated with all stream elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjFloatConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
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
     * Returns an {@link OptionalFloat} describing the minimum element of this stream,
     * or an empty optional if the stream is empty. This is a terminal operation.
     * For sorted streams, the minimum is read in O(1) (the first element, or NaN if the sorted range
     * ends in NaN, matching the NaN-propagating scan); otherwise scans the backing array.
     * Closes the stream.
     *
     * @return an {@code OptionalFloat} with the minimum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalFloat min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalFloat.empty();
            } else if (isSorted()) {
                // NaN sorts last, so the sorted shortcut must check the tail to keep propagating
                // NaN like the Math.min scan below (and like max(), whose last element IS the NaN).
                return OptionalFloat.of(Float.isNaN(elements[toIndex - 1]) ? elements[toIndex - 1] : elements[fromIndex]);
            }

            return OptionalFloat.of(N.min(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns an {@link OptionalFloat} describing the maximum element of this stream,
     * or an empty optional if the stream is empty. This is a terminal operation.
     * For sorted streams, returns the last element directly (O(1)); otherwise scans the backing array.
     * Closes the stream.
     *
     * @return an {@code OptionalFloat} with the maximum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalFloat max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalFloat.empty();
            } else if (isSorted()) {
                return OptionalFloat.of(elements[toIndex - 1]);
            }

            return OptionalFloat.of(N.max(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns an {@link OptionalFloat} describing the {@code k}-th largest element of this stream,
     * or an empty optional if there are fewer than {@code k} elements. This is a terminal operation.
     * For sorted streams, provides O(1) access via reverse index; otherwise uses a partial sort.
     * Closes the stream.
     *
     * @param k the 1-based rank of the largest element to find (e.g. {@code k=1} returns the maximum)
     * @return an {@code OptionalFloat} with the {@code k}-th largest element, or empty if insufficient elements
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code k} is not positive
     */
    @Override
    public OptionalFloat kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (k > toIndex - fromIndex) {
                return OptionalFloat.empty();
            } else if (isSorted()) {
                return OptionalFloat.of(elements[toIndex - k]);
            }

            return OptionalFloat.of(N.kthLargest(elements, fromIndex, toIndex, k));
        } finally {
            close();
        }
    }

    /**
     * Returns the number of elements in this stream. This is a terminal operation.
     * Computes the count in O(1) as {@code toIndex - fromIndex} without iterating the backing array.
     * Closes the stream.
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

    @Override
    public FloatSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final FloatSummaryStatistics result = new FloatSummaryStatistics();

            for (int i = fromIndex; i < toIndex; i++) {
                result.accept(elements[i]);
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

    @Override
    public <E extends Exception> boolean allMatch(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
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

    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> OptionalFloat findFirst(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return OptionalFloat.of(elements[i]);
                }
            }

            return OptionalFloat.empty();
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalFloat findLast(final Throwables.FloatPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return OptionalFloat.of(elements[i]);
                }
            }

            return OptionalFloat.empty();
        } finally {
            close();
        }
    }

    @Override
    public DoubleStream asDoubleStream() throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
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
            public double[] toArray() {
                final double[] a = new double[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, isSorted());
    }

    @Override
    FloatIteratorEx iteratorEx() {
        assertNotClosed();

        return FloatIteratorEx.of(elements, fromIndex, toIndex);
    }

    @Override
    public FloatStream appendIfEmpty(final Supplier<? extends FloatStream> supplier) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
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
                        final FloatStream s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }, false).onClose(() -> close(holder));
        } else {
            return this;
        }
    }

    @Override
    public FloatStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            return newStream(new FloatIteratorEx() { //NOSONAR
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
                public float nextFloat() {
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
    protected Tuple3<float[], Integer, Integer> arrayForIntermediateOp() {
        assertNotClosed();

        return Tuple.of(elements, fromIndex, toIndex);
    }

    @Override
    protected FloatStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelArrayFloatStream(elements, fromIndex, toIndex, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                closeHandlers());
    }

    @Override
    public FloatStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new FloatIterator() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex;
            }

            @Override
            public float nextFloat() {
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

    @Override
    public FloatStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new FloatIterator() { //NOSONAR
            private int cursor = fromIndex;
            private long roundsCompleted = 0;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex && roundsCompleted < rounds && (cursor < toIndex || rounds - roundsCompleted > 1);
            }

            @Override
            public float nextFloat() {
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

    @Override
    protected boolean isEmpty() {
        return fromIndex >= toIndex;
    }
}
