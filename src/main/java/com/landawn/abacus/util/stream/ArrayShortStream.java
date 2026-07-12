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
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortToIntFunction;
import com.landawn.abacus.util.function.ShortUnaryOperator;

/**
 * An array-backed implementation of ShortStream that provides efficient sequential access to a short array
 * or a portion of it. This implementation is optimized for working with primitive short arrays and offers
 * high performance for operations on contiguous short sequences.
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
 * short[] data = {1, 2, 3, 4, 5};
 * ShortStream stream = ShortStream.of(data);
 * short max = stream.max().orElse((short)0);
 *
 * // Filtering and mapping
 * ShortStream evenDoubled = ShortStream.of(data)
 *     .filter(n -> n % 2 == 0)
 *     .map(n -> (short)(n * 2));
 *
 * // Statistical operations
 * ShortStream stats = ShortStream.of(data);
 * ShortSummaryStatistics summary = stats.summaryStatistics();
 * System.out.println("Average: " + summary.getAverage());
 * }</pre>
 *
 */
class ArrayShortStream extends AbstractShortStream {
    final short[] elements;
    final int fromIndex;
    final int toIndex;

    /**
     * Constructs an ArrayShortStream from the entire short array.
     *
     * @param values the short array to stream
     */
    ArrayShortStream(final short[] values) {
        this(values, 0, values.length);
    }

    /**
     * Constructs an ArrayShortStream from the entire short array with close handlers.
     *
     * @param values the short array to stream
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     */
    ArrayShortStream(final short[] values, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    /**
     * Constructs an ArrayShortStream from the entire short array with sorting state and close handlers.
     *
     * @param values the short array to stream
     * @param sorted whether the array elements are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     */
    ArrayShortStream(final short[] values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    /**
     * Constructs an ArrayShortStream from a range within the short array.
     *
     * @param values the short array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayShortStream(final short[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    /**
     * Constructs an ArrayShortStream from a range within the short array with close handlers.
     *
     * @param values the short array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayShortStream(final short[] values, final int fromIndex, final int toIndex, final Collection<LocalRunnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    /**
     * Constructs an ArrayShortStream from a range within the short array with all configuration options.
     * This is the primary constructor that all other constructors delegate to.
     *
     * @param values the short array to stream
     * @param fromIndex the start index (inclusive) of the range
     * @param toIndex the end index (exclusive) of the range
     * @param sorted whether the array elements in the range are in sorted order
     * @param closeHandlers handlers to execute when the stream is closed, can be null
     * @throws IndexOutOfBoundsException if {@code fromIndex < 0}, {@code toIndex > values.length},
     *         or {@code fromIndex > toIndex}
     */
    ArrayShortStream(final short[] values, final int fromIndex, final int toIndex, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, N.len(values));

        elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public ShortStream filter(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
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
            public short nextShort() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    @Override
    public ShortStream takeWhile(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
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
            public short nextShort() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    @Override
    public ShortStream dropWhile(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
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
            public short nextShort() {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, isSorted());
    }

    @Override
    public ShortStream step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1 || fromIndex == toIndex) {
            return skip(0);
        }

        return newStream(new ShortIteratorEx() { //NOSONAR
            private final int stepToUse = (int) N.min(step, Integer.MAX_VALUE);
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final short res = elements[cursor];
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
            public short[] toArray() {
                final long rem = toIndex - cursor;
                final int len = (int) ((rem % stepToUse == 0) ? rem / stepToUse : rem / stepToUse + 1);
                final short[] a = new short[len];

                for (int i = 0; i < len; i++) {
                    a[i] = elements[cursor];
                    cursor = cursor > toIndex - stepToUse ? toIndex : cursor + stepToUse;
                }

                return a;
            }
        }, isSorted());
    }

    @Override
    public ShortStream map(final ShortUnaryOperator mapper) throws IllegalStateException {
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
    public IntStream mapToInt(final ShortToIntFunction mapper) throws IllegalStateException {
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
    public <T> Stream<T> mapToObj(final ShortFunction<? extends T> mapper) throws IllegalStateException {
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

    @Override
    public ShortStream flatMap(final ShortFunction<? extends ShortStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = new ShortIteratorEx() {
            private int cursor = fromIndex;
            private ShortIterator cur = null;
            private ShortStream s = null;
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
            public short nextShort() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.nextShort();
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
    public ShortStream flatmap(final ShortFunction<? extends Collection<Short>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private int cursor = fromIndex;
            private Iterator<Short> cur = null;
            private Collection<Short> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && cursor < toIndex) {
                    c = mapper.apply(elements[cursor++]);
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public short nextShort() {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Short v = cur.next();
                return v == null ? (short) 0 : v;
            }
        }, false);
    }

    @Override
    public ShortStream flatMapArray(final ShortFunction<short[]> mapper) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return super.flatMapArray(mapper);
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
    public IntStream flatMapToInt(final ShortFunction<? extends IntStream> mapper) throws IllegalStateException {
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

    @Override
    public <T> Stream<T> flatMapToObj(final ShortFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
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

    @Override
    public <T> Stream<T> flatmapToObj(final ShortFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
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

    @Override
    public ShortStream distinct() throws IllegalStateException {
        assertNotClosed();

        if (isSorted()) {
            return newStream(new ShortIteratorEx() { //NOSONAR
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
                public short nextShort() {
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

    @Override
    public ShortStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        if (toIndex - fromIndex <= maxSize) {
            return this;
        }

        return newStream(elements, fromIndex, maxSize < toIndex - fromIndex ? (int) (fromIndex + maxSize) : toIndex, isSorted());
    }

    @Override
    public ShortStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
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

    @Override
    public ShortStream top(final int n, final Comparator<? super Short> comparator) throws IllegalStateException, IllegalArgumentException {
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

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean initialized = false;
            private short[] aar;
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
            public short nextShort() {
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
            public short[] toArray() {
                if (!initialized) {
                    init();
                }

                final short[] a = new short[to - cursor];
                N.copy(aar, cursor, a, 0, to - cursor);
                cursor = to; // consume all elements
                return a;
            }

            @Override
            public ShortList toList() {
                return ShortList.of(toArray());
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

    @Override
    public ShortStream onEach(final ShortConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public short nextShort() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public short[] toArray() {
                final short[] a = new short[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, isSorted());
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.ShortConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                action.accept(elements[i]);
            }
        } finally {
            close();
        }
    }

    @Override
    protected short[] toArray(final boolean closeStream) {
        assertNotClosed();

        try {
            return N.copyOfRange(elements, fromIndex, toIndex);
        } finally {
            if (closeStream) {
                close();
            }
        }
    }

    @Override
    public ShortList toShortList() throws IllegalStateException {
        assertNotClosed();

        try {
            return ShortList.of(N.copyOfRange(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public List<Short> toList() throws IllegalStateException {
        assertNotClosed();

        try {
            final List<Short> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Set<Short> toSet() throws IllegalStateException {
        assertNotClosed();

        try {
            final Set<Short> result = N.newHashSet(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <C extends Collection<Short>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
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

    @Override
    public Multiset<Short> toMultiset() throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Short> result = N.newMultiset(N.min(64, (toIndex - fromIndex) / 2));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Short> toMultiset(final Supplier<? extends Multiset<Short>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Short> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.ShortFunction<? extends K, E> keyMapper,
            final Throwables.ShortFunction<? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory)
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
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.ShortFunction<? extends K, E> keyMapper,
            final Collector<? super Short, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super Short> downstreamAccumulator = (BiConsumer<Object, ? super Short>) downstream.accumulator();
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

    @Override
    public OptionalShort first() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalShort.of(elements[fromIndex]) : OptionalShort.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort last() throws IllegalStateException {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalShort.of(elements[toIndex - 1]) : OptionalShort.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort elementAt(final long position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        try {
            if (position < toIndex - fromIndex) {
                return OptionalShort.of(elements[fromIndex + (int) position]);
            } else {
                return OptionalShort.empty();
            }
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public OptionalShort onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return OptionalShort.empty();
            } else if (size == 1) {
                return OptionalShort.of(elements[fromIndex]);
            } else {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(elements[fromIndex], ", ", elements[fromIndex + 1]));
            }
        } finally {
            close();
        }
    }

    @Override
    public short reduce(final short identity, final ShortBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            short result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = accumulator.applyAsShort(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort reduce(final ShortBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalShort.empty();
            }

            short result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = accumulator.applyAsShort(result, elements[i]);
            }

            return OptionalShort.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjShortConsumer<? super R> accumulator, final BiConsumer<R, R> combiner)
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

    @Override
    public OptionalShort min() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalShort.empty();
            } else if (isSorted()) {
                return OptionalShort.of(elements[fromIndex]);
            }

            return OptionalShort.of(N.min(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalShort.empty();
            } else if (isSorted()) {
                return OptionalShort.of(elements[toIndex - 1]);
            }

            return OptionalShort.of(N.max(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (k > toIndex - fromIndex) {
                return OptionalShort.empty();
            } else if (isSorted()) {
                return OptionalShort.of(elements[toIndex - k]);
            }

            return OptionalShort.of(N.kthLargest(elements, fromIndex, toIndex, k));
        } finally {
            close();
        }
    }

    @Override
    public int sum() throws IllegalStateException {
        assertNotClosed();

        try {
            return sum(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble average() throws IllegalStateException {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            }

            // Accumulate into a long: sum() narrows via Numbers.toIntExact and would throw on a
            // large short stream whose sum exceeds Integer.MAX_VALUE, even though the average is
            // well-defined (matches IteratorShortStream.average and summaryStatistics).
            long sum = 0;

            for (int i = fromIndex; i < toIndex; i++) {
                sum += elements[i];
            }

            return OptionalDouble.of(((double) sum) / (toIndex - fromIndex));
        } finally {
            close();
        }
    }

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
    public ShortSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final ShortSummaryStatistics result = new ShortSummaryStatistics();

            for (int i = fromIndex; i < toIndex; i++) {
                result.accept(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> boolean allMatch(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> boolean noneMatch(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
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
    public <E extends Exception> OptionalShort findFirst(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return OptionalShort.of(elements[i]);
                }
            }

            return OptionalShort.empty();
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalShort findLast(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return OptionalShort.of(elements[i]);
                }
            }

            return OptionalShort.empty();
        } finally {
            close();
        }
    }

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

    @Override
    ShortIteratorEx iteratorEx() {
        assertNotClosed();

        return ShortIteratorEx.of(elements, fromIndex, toIndex);
    }

    @Override
    public ShortStream appendIfEmpty(final Supplier<? extends ShortStream> supplier) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            final Holder<ShortStream> holder = new Holder<>();

            return newStream(new ShortIteratorEx() { //NOSONAR
                private ShortIteratorEx iter;

                @Override
                public boolean hasNext() {
                    if (iter == null) {
                        init();
                    }

                    return iter.hasNext();
                }

                @Override
                public short nextShort() {
                    if (iter == null) {
                        init();
                    }

                    return iter.nextShort();
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
                        final ShortStream s = supplier.get();
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
    public ShortStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

        if (fromIndex == toIndex) {
            return newStream(new ShortIteratorEx() { //NOSONAR
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
                public short nextShort() {
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
    protected Tuple3<short[], Integer, Integer> arrayForIntermediateOp() {
        assertNotClosed();

        return Tuple.of(elements, fromIndex, toIndex);
    }

    @Override
    protected ShortStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelArrayShortStream(elements, fromIndex, toIndex, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads,
                closeHandlers());
    }

    @Override
    public ShortStream cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIterator() { //NOSONAR
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex;
            }

            @Override
            public short nextShort() {
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
    public ShortStream cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new ShortIterator() { //NOSONAR
            private int cursor = fromIndex;
            private long roundsCompleted = 0;

            @Override
            public boolean hasNext() {
                return fromIndex < toIndex && roundsCompleted < rounds && (cursor < toIndex || rounds - roundsCompleted > 1);
            }

            @Override
            public short nextShort() {
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
