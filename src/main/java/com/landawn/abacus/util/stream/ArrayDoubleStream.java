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
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
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

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatIterator;
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
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.DoubleToFloatFunction;

/**
 * An array-backed implementation of DoubleStream that provides efficient sequential access to a double array
 * or a portion of it. This implementation is optimized for working with primitive double arrays and offers
 * high performance for operations on contiguous double sequences.
 *
 * <p>Key features:
 * <ul>
 * <li>Zero-copy operations when possible, working directly on the underlying array</li>
 * <li>Support for partial array ranges via fromIndex and toIndex</li>
 * <li>Optimized implementations of filter, map, flatMap, and terminal operations</li>
 * <li>Efficient sorted stream handling with specialized algorithms</li>
 * <li>Inherits Kahan-summation-based {@code sum()} and {@code average()} from {@link AbstractDoubleStream} for numerical stability</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * double[] data = {1.5, 2.5, 3.5, 4.5, 5.5};
 * DoubleStream stream = DoubleStream.of(data);
 * double max = stream.max().orElse(0.0);
 * }</pre>
 *
 */
class ArrayDoubleStream extends AbstractDoubleStream {
    final double[] elements;
    final int fromIndex;
    final int toIndex;

    /**
     * Constructs an ArrayDoubleStream from the entire double array.
     * This constructor creates a stream that processes all elements in the provided array
     * from index 0 to the end of the array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] data = {1.5, 2.5, 3.5, 4.5, 5.5};
     * DoubleStream stream = new ArrayDoubleStream(data);
     * }</pre>
     *
     * @param values the double array to stream over
     */
    ArrayDoubleStream(final double[] values) {
        this(values, 0, values.length);
    }

    /**
     * Constructs an ArrayDoubleStream from the entire double array with close handlers.
     * The close handlers will be executed when the stream is closed, allowing for
     * resource cleanup or other post-processing operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] data = {1.5, 2.5, 3.5, 4.5, 5.5};
     * List<LocalRunnable> handlers = new ArrayList<>();
     * handlers.add(() -> System.out.println("Stream closed"));
     * DoubleStream stream = new ArrayDoubleStream(data, handlers);
     * }</pre>
     *
     * @param values the double array to stream over
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     */
    ArrayDoubleStream(final double[] values, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    /**
     * Constructs an ArrayDoubleStream from the entire double array with sorting state and close handlers.
     * The sorted flag indicates whether the array is already in sorted order, which can be used
     * to optimize certain stream operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] sortedData = {1.5, 2.5, 3.5, 4.5, 5.5};
     * List<LocalRunnable> handlers = new ArrayList<>();
     * DoubleStream stream = new ArrayDoubleStream(sortedData, true, handlers);
     * }</pre>
     *
     * @param values the double array to stream over
     * @param sorted whether the array elements are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     */
    ArrayDoubleStream(final double[] values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    /**
     * Constructs an ArrayDoubleStream from a range within the double array.
     * This allows streaming over a subset of the array elements, from the specified
     * start index (inclusive) to the end index (exclusive).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] data = {1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5};
     * // Stream over elements from index 2 to 5 (exclusive): {3.5, 4.5, 5.5}
     * DoubleStream stream = new ArrayDoubleStream(data, 2, 5);
     * }</pre>
     *
     * @param values the double array to stream over
     * @param fromIndex the start index (inclusive) of the range to stream
     * @param toIndex the end index (exclusive) of the range to stream
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayDoubleStream(final double[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    /**
     * Constructs an ArrayDoubleStream from a range within the double array with close handlers.
     * Combines range specification with close handler support for resource management.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] data = {1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5};
     * List<LocalRunnable> handlers = new ArrayList<>();
     * handlers.add(() -> System.out.println("Stream closed"));
     * // Stream over elements from index 2 to 5 with close handlers
     * DoubleStream stream = new ArrayDoubleStream(data, 2, 5, handlers);
     * }</pre>
     *
     * @param values the double array to stream over
     * @param fromIndex the start index (inclusive) of the range to stream
     * @param toIndex the end index (exclusive) of the range to stream
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayDoubleStream(final double[] values, final int fromIndex, final int toIndex, final Collection<LocalRunnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    /**
     * Constructs an ArrayDoubleStream from a range within the double array with all configuration options.
     * This is the primary constructor that all other constructors delegate to. It provides full
     * control over the stream configuration including range, sorting state, and close handlers.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] sortedData = {1.5, 2.5, 3.5, 4.5, 5.5, 6.5, 7.5, 8.5};
     * List<LocalRunnable> handlers = new ArrayList<>();
     * handlers.add(() -> System.out.println("Stream closed"));
     * // Stream over sorted elements from index 2 to 6 with close handlers
     * DoubleStream stream = new ArrayDoubleStream(sortedData, 2, 6, true, handlers);
     * }</pre>
     *
     * @param values the double array to stream over
     * @param fromIndex the start index (inclusive) of the range to stream
     * @param toIndex the end index (exclusive) of the range to stream
     * @param sorted whether the array elements in the range are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayDoubleStream(final double[] values, final int fromIndex, final int toIndex, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
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
     * @return a new {@code DoubleStream} containing only matching elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream filter(final DoublePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
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
            public double nextDouble() {
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
     * @return a new {@code DoubleStream} of elements from the start while {@code predicate} is satisfied
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream takeWhile(final DoublePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
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
            public double nextDouble() {
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
     * @return a new {@code DoubleStream} with leading elements dropped while {@code predicate} holds
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream dropWhile(final DoublePredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
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
            public double nextDouble() {
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
     * // DoubleStream.of(1.0, 2.0, 3.0, 4.0, 5.0).step(2) => 1.0, 3.0, 5.0
     * }</pre>
     *
     * @param step the stride between successive elements; must be positive
     * @return a new {@code DoubleStream} containing every {@code step}-th element
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code step} is not positive
     */
    @Override
    public DoubleStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1 || fromIndex == toIndex) {
            return skip(0);
        }

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private final int stepToUse = (int) N.min(step, Integer.MAX_VALUE);
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

                final double res = elements[cursor];
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
            public double[] toArray() {
                final long rem = toIndex - cursor;
                final int len = (int) ((rem % stepToUse == 0) ? rem / stepToUse : rem / stepToUse + 1);
                final double[] a = new double[len];

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
     * @return a new {@code DoubleStream} of mapped values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream map(final DoubleUnaryOperator mapper) throws IllegalStateException {
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
     * Returns an {@code IntStream} consisting of the results of applying the given function to the elements of this stream.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code IntStream} of mapped int values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream mapToInt(final DoubleToIntFunction mapper) throws IllegalStateException {
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
    public LongStream mapToLong(final DoubleToLongFunction mapper) throws IllegalStateException {
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
     * Returns a {@code FloatStream} consisting of the results of applying the given function to the elements of this stream.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code FloatStream} of mapped float values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream mapToFloat(final DoubleToFloatFunction mapper) throws IllegalStateException {
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
     * Returns a {@code Stream} consisting of the results of applying the given function to the elements of this stream.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param <T> the type of elements in the resulting stream
     * @param mapper a non-interfering, stateless function to apply to each element
     * @return a new {@code Stream<T>} of mapped object values
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public <T> Stream<T> mapToObj(final DoubleFunction<? extends T> mapper) throws IllegalStateException {
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
     * Returns a stream formed by replacing each element with the contents of a mapped {@code DoubleStream}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code DoubleStream}
     * @return a new {@code DoubleStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream flatMap(final DoubleFunction<? extends DoubleStream> mapper) throws IllegalStateException {
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
     * Returns a stream formed by replacing each element with the contents of a mapped {@code Collection<Double>}.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a collection of doubles
     * @return a new {@code DoubleStream} formed by concatenating the mapped collections
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream flatmap(final DoubleFunction<? extends Collection<Double>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private Iterator<Double> cur = null;
            private Collection<Double> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && cursor < toIndex) {
                    c = mapper.apply(elements[cursor++]);
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
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
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

    /**
     * Returns an {@code IntStream} formed by replacing each element with the contents of a mapped {@code IntStream}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to an {@code IntStream}
     * @return a new {@code IntStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public IntStream flatMapToInt(final DoubleFunction<? extends IntStream> mapper) throws IllegalStateException {
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
    public LongStream flatMapToLong(final DoubleFunction<? extends LongStream> mapper) throws IllegalStateException {
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
     * Returns a {@code FloatStream} formed by replacing each element with the contents of a mapped {@code FloatStream}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
     *
     * @param mapper a non-interfering, stateless function mapping each element to a {@code FloatStream}
     * @return a new {@code FloatStream} formed by concatenating the mapped streams
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public FloatStream flatMapToFloat(final DoubleFunction<? extends FloatStream> mapper) throws IllegalStateException {
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

    /**
     * Returns a {@code Stream} formed by replacing each element with the contents of a mapped {@code Stream<T>}.
     * Iterates the backing array; each mapped stream is closed after its contents are consumed.
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
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
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
     * Returns a stream consisting of distinct elements of this stream.
     * If the stream is sorted, adjacent-element comparison is used (efficient O(n));
     * otherwise a hash set is used to track seen elements.
     * Iterates the backing array from {@code fromIndex} to {@code toIndex}.
     *
     * @return a new {@code DoubleStream} containing only distinct elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new DoubleIteratorEx() { //NOSONAR
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
                public double nextDouble() {
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
     * Returns a stream consisting of at most {@code maxSize} elements from the start of this stream.
     * Implemented as an index range restriction — no additional iteration cost.
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

        if (toIndex - fromIndex <= maxSize) {
            return this;
        }

        return newStream(elements, fromIndex, maxSize < toIndex - fromIndex ? (int) (fromIndex + maxSize) : toIndex, isSorted());
    }

    /**
     * Returns a stream consisting of the remaining elements of this stream after discarding the first {@code n} elements.
     * Implemented as a {@code fromIndex} offset — no additional iteration cost.
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
     * If the stream is sorted and the comparator matches the natural order, an O(1) range restriction is applied;
     * otherwise a partial sort is performed.
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
            return newStream(elements, fromIndex, fromIndex, isSorted());
        }

        if (n >= toIndex - fromIndex) {
            return newStream(elements, fromIndex, toIndex, isSorted());
        } else if (isSorted() && isSameComparator(comparator, comparator())) {
            return newStream(elements, toIndex - n, toIndex, isSorted());
        }

        return newStream(new DoubleIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private double[] sortedTopElements;
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
            public double[] toArray() {
                if (!initialized) {
                    init();
                }

                final double[] a = new double[to - cursor];
                N.copy(sortedTopElements, cursor, a, 0, to - cursor);
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
                    sortedTopElements = N.top(elements, fromIndex, toIndex, n, comparator);
                    to = sortedTopElements.length;
                }
            }
        }, false);
    }

    /**
     * Returns a stream that performs the given action on each element as it is consumed,
     * primarily for debugging purposes. Iterates the backing array from {@code fromIndex}.
     *
     * @param action a non-interfering action to perform on each element as it is pulled
     * @return a new {@code DoubleStream} that applies the action to each element
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream onEach(final DoubleConsumer action) throws IllegalStateException {
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

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public double[] toArray() {
                final double[] a = new double[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = elements[cursor++];
                }

                return a;
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
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(elements[i]);
            }
        } finally {
            close();
        }
    }

    /**
     * Returns all elements of this stream as a {@code double[]}, optionally closing the stream.
     * This is an internal method used by stream infrastructure.
     *
     * @param closeStream if {@code true}, the stream is closed after the array is collected
     * @return a {@code double[]} containing all elements in the range [{@code fromIndex}, {@code toIndex})
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected double[] toArray(final boolean closeStream) {
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
     * Collects all elements of this stream into a {@code DoubleList} and closes the stream.
     *
     * @return a {@code DoubleList} containing all elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleList toDoubleList() throws IllegalStateException {
        assertNotClosed();

        try {
            return DoubleList.of(N.copyOfRange(elements, fromIndex, toIndex));
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

        try {
            final List<Double> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
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

        try {
            final Set<Double> result = N.newHashSet(toIndex - fromIndex);

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
    public <C extends Collection<Double>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
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
     * Collects all elements of this stream into a {@code Multiset<Double>} and closes the stream.
     * Elements with equal values will have their counts incremented accordingly.
     *
     * @return a {@code Multiset<Double>} representing element frequencies in this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public Multiset<Double> toMultiset() throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Double> result = N.newMultiset(N.min(64, (toIndex - fromIndex) / 2));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
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
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.DoubleFunction<? extends K, E> keyMapper,
            final Throwables.DoubleFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
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
     * Returns the first element as an {@code OptionalDouble}, or empty if the stream is empty. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the first element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble first() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalDouble.of(elements[fromIndex]) : OptionalDouble.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the last element as an {@code OptionalDouble}, or empty if the stream is empty. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the last element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble last() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalDouble.of(elements[toIndex - 1]) : OptionalDouble.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the element at the given zero-based {@code position}, or empty if out of range. Closes the stream.
     *
     * @param position the zero-based index of the desired element; must be &gt;= 0
     * @return an {@code OptionalDouble} containing the element at {@code position}, or empty if out of range
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code position} is negative
     */
    @Override
    public OptionalDouble elementAt(final long position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        try {
            if (position < toIndex - fromIndex) {
                return OptionalDouble.of(elements[fromIndex + (int) position]);
            } else {
                return OptionalDouble.empty();
            }
        } finally {
            close();
        }

    }

    /**
     * Returns the single element as an {@code OptionalDouble}, empty if the stream is empty,
     * or throws if more than one element exists. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the single element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     * @throws TooManyElementsException if the stream contains more than one element
     */
    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalDouble onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return OptionalDouble.empty();
            } else if (size == 1) {
                return OptionalDouble.of(elements[fromIndex]);
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
    public double reduce(final double identity, final DoubleBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            double result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = accumulator.applyAsDouble(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Performs a reduction on the elements of this stream using an associative accumulation function,
     * and returns an {@code OptionalDouble} describing the reduced value, or empty if the stream is empty.
     * Closes the stream.
     *
     * @param accumulator an associative, non-interfering, stateless function for combining two values
     * @return an {@code OptionalDouble} containing the result, or empty if the stream has no elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble reduce(final DoubleBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            }

            double result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = accumulator.applyAsDouble(result, elements[i]);
            }

            return OptionalDouble.of(result);
        } finally {
            close();
        }
    }

    /**
     * Performs a mutable reduction on the elements of this stream using the provided supplier,
     * accumulator, and combiner functions, and returns the resulting container. Closes the stream.
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

            for (int i = fromIndex; i < toIndex; i++) {
                accumulator.accept(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Returns the minimum element of this stream, or an empty {@code OptionalDouble} if the stream is empty.
     * If the stream is sorted, the minimum is read in constant time (the first element, or NaN if the
     * sorted range ends in NaN, matching the NaN-propagating scan); otherwise the entire backing array
     * range is scanned. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the minimum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            } else if (isSorted()) {
                // NaN sorts last, so the sorted shortcut must check the tail to keep propagating
                // NaN like the Math.min scan below (and like max(), whose last element IS the NaN).
                return OptionalDouble.of(Double.isNaN(elements[toIndex - 1]) ? elements[toIndex - 1] : elements[fromIndex]);
            }

            return OptionalDouble.of(N.min(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns the maximum element of this stream, or an empty {@code OptionalDouble} if the stream is empty.
     * If the stream is sorted, the maximum is read directly from the last position in constant time;
     * otherwise the entire backing array range is scanned. Closes the stream.
     *
     * @return an {@code OptionalDouble} containing the maximum element, or empty if the stream is empty
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public OptionalDouble max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            } else if (isSorted()) {
                return OptionalDouble.of(elements[toIndex - 1]);
            }

            return OptionalDouble.of(N.max(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    /**
     * Returns the k-th largest element of this stream (1-based), or an empty {@code OptionalDouble}
     * if the stream contains fewer than {@code k} elements. If the stream is sorted, the result is
     * retrieved via index arithmetic in constant time; otherwise a partial sort is performed.
     * Closes the stream.
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
            if (k > toIndex - fromIndex) {
                return OptionalDouble.empty();
            } else if (isSorted()) {
                return OptionalDouble.of(elements[toIndex - k]);
            }

            return OptionalDouble.of(N.kthLargest(elements, fromIndex, toIndex, k));
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
     * Returns a {@code DoubleSummaryStatistics} describing various aggregate values (count, sum, min, max, average)
     * of the elements in this stream. Closes the stream.
     *
     * @return a {@code DoubleSummaryStatistics} for this stream's elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final DoubleSummaryStatistics result = new DoubleSummaryStatistics();

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
    public <E extends Exception> boolean anyMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> boolean allMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> boolean noneMatch(final Throwables.DoublePredicate<E> predicate) throws IllegalStateException, E {
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
     * Returns the first element that matches the given predicate as an {@code OptionalDouble},
     * or empty if no element matches. Iterates the backing array from {@code fromIndex} forward.
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
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return OptionalDouble.of(elements[i]);
                }
            }

            return OptionalDouble.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the last element that matches the given predicate as an {@code OptionalDouble},
     * or empty if no element matches. Iterates the backing array from {@code toIndex - 1} backward.
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
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return OptionalDouble.of(elements[i]);
                }
            }

            return OptionalDouble.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns a {@code java.util.stream.DoubleStream} view of this stream's elements,
     * bridging to the JDK standard stream API. If the stream is parallel, a parallel JDK stream is returned.
     * Close handlers are propagated.
     *
     * @return a JDK {@code java.util.stream.DoubleStream} backed by this stream's elements
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public java.util.stream.DoubleStream toJdkStream() throws IllegalStateException {
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
     * Returns a {@code DoubleIteratorEx} over the elements in this stream's backing array range.
     * This is an internal method used by stream infrastructure.
     *
     * @return a {@code DoubleIteratorEx} covering [{@code fromIndex}, {@code toIndex}) of the backing array
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    DoubleIteratorEx iteratorEx() {
        assertNotClosed();

        return DoubleIteratorEx.of(elements, fromIndex, toIndex);
    }

    /**
     * Returns this stream if it is non-empty; otherwise returns a stream produced by the given supplier.
     * The supplier is not invoked unless the stream is empty. Closes any produced stream on close.
     *
     * @param supplier a supplier of a substitute {@code DoubleStream} to use when this stream is empty
     * @return this stream if non-empty, or the stream produced by the supplier
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream appendIfEmpty(final Supplier<? extends DoubleStream> supplier) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
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
                        final DoubleStream s = supplier.get();
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
    public DoubleStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            return newStream(new DoubleIteratorEx() { //NOSONAR
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
                public double nextDouble() {
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
     * @return a {@code Tuple3} containing the backing {@code double[]}, the inclusive start index, and the exclusive end index
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected Tuple3<double[], Integer, Integer> arrayForIntermediateOp() {
        assertNotClosed();

        return Tuple.of(elements, fromIndex, toIndex);
    }

    /**
     * Returns a new {@link ParallelArrayDoubleStream} that processes this stream's backing array
     * range in parallel using the specified configuration.
     *
     * @param maxThreadNum the maximum number of threads to use
     * @param splitor the strategy for splitting the work among threads
     * @param asyncExecutor the executor for asynchronous parallel tasks
     * @param cancelUncompletedThreads whether to cancel incomplete threads when the stream is closed
     * @return a parallel {@code DoubleStream} backed by the same array range
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    protected DoubleStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelArrayDoubleStream(elements, fromIndex, toIndex, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                closeHandlers());
    }

    /**
     * Returns an infinite stream that cycles through the elements of this stream repeatedly.
     * If this stream is empty, the returned stream is also empty.
     *
     * <pre>{@code
     * // DoubleStream.of(1.0, 2.0, 3.0).cycled().limit(7) => 1.0, 2.0, 3.0, 1.0, 2.0, 3.0, 1.0
     * }</pre>
     *
     * @return an infinite {@code DoubleStream} cycling through the elements of this stream
     * @throws IllegalStateException if the stream is already closed
     */
    @Override
    public DoubleStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new DoubleIterator() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex;
            }

            @Override
            public double nextDouble() {
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
     * // DoubleStream.of(1.0, 2.0).cycled(3) => 1.0, 2.0, 1.0, 2.0, 1.0, 2.0
     * }</pre>
     *
     * @param rounds the number of times to cycle through the elements; must be &gt;= 0
     * @return a {@code DoubleStream} that repeats the elements of this stream {@code rounds} times
     * @throws IllegalStateException if the stream is already closed
     * @throws IllegalArgumentException if {@code rounds} is negative
     */
    @Override
    public DoubleStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new DoubleIterator() { //NOSONAR
            private int cursor = fromIndex;
            private long roundsCompleted = 0;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex && roundsCompleted < rounds && (cursor < toIndex || rounds - roundsCompleted > 1);
            }

            @Override
            public double nextDouble() {
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
