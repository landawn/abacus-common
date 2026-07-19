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
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
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
 * An iterator-based implementation of ShortStream that processes short elements sequentially.
 * This class serves as the default sequential stream implementation for short values,
 * wrapping a ShortIterator to provide stream operations.
 *
 * <p>This is an internal implementation class. Users should create streams through
 * the public ShortStream factory methods rather than instantiating this class directly.
 *
 * @see ShortStream
 * @see ShortIteratorEx
 */
class IteratorShortStream extends AbstractShortStream {
    final ShortIteratorEx elements;

    //    OptionalShort head;
    //    ShortStream tail;

    //    ShortStream head2;
    //    OptionalShort tail2;

    /**
     * Constructs an IteratorShortStream from a ShortIterator.
     * Creates an unsorted stream with no close handlers.
     *
     * @param values the short iterator to wrap as a stream
     */
    IteratorShortStream(final ShortIterator values) {
        this(values, null);
    }

    /**
     * Constructs an IteratorShortStream from a ShortIterator with close handlers.
     * Creates an unsorted stream that will execute the provided close handlers when closed.
     *
     * @param values the short iterator to wrap as a stream
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorShortStream(final ShortIterator values, final Collection<LocalRunnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    /**
     * Constructs an IteratorShortStream from a ShortIterator with sorting and close handlers.
     * This is the primary constructor that all other constructors delegate to.
     *
     * @param values the short iterator to wrap as a stream
     * @param sorted {@code true} if the elements are already sorted in natural order, {@code false} otherwise
     * @param closeHandlers collection of close handlers to execute when the stream is closed, may be null
     */
    IteratorShortStream(final ShortIterator values, final boolean sorted, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, closeHandlers);

        ShortIteratorEx tmp = null;

        if (values instanceof ShortIteratorEx) {
            tmp = (ShortIteratorEx) values;
        } else {
            tmp = new ShortIteratorEx() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public short nextShort() {
                    return values.nextShort();
                }
            };
        }

        elements = tmp;
    }

    @Override
    public ShortStream filter(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.nextShort();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
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

                return next;
            }
        }, isSorted());
    }

    @Override
    public ShortStream takeWhile(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean hasMore = true;
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                if (!hasNext && hasMore && elements.hasNext()) {
                    next = elements.nextShort();

                    if (predicate.test(next)) {
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

                return next;
            }

        }, isSorted());
    }

    @Override
    public ShortStream dropWhile(final ShortPredicate predicate) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private boolean hasNext = false;
            private short next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextShort();

                            if (!predicate.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.nextShort();
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

                return next;
            }

        }, isSorted());
    }

    @Override
    public ShortStream map(final ShortUnaryOperator mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public short nextShort() throws IllegalArgumentException {
                return mapper.applyAsShort(elements.nextShort());
            }

        }, false);
    }

    @Override
    public IntStream mapToInt(final ShortToIntFunction mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new IntIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() throws IllegalArgumentException {
                return mapper.applyAsInt(elements.nextShort());
            }

        }, false);
    }

    @Override
    public <T> Stream<T> mapToObj(final ShortFunction<? extends T> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException {
                return mapper.apply(elements.nextShort());
            }

        }, false, null);
    }

    @Override
    public ShortStream flatMap(final ShortFunction<? extends ShortStream> mapper) throws IllegalStateException {
        assertNotClosed();

        final ShortIteratorEx iter = new ShortIteratorEx() {
            private ShortIterator cur = null;
            private ShortStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextShort());

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

    @Override
    public ShortStream flatmap(final ShortFunction<? extends Collection<Short>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            private Iterator<Short> cur = null;
            private Collection<Short> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextShort());
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
            private short[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.nextShort());
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
            private IntIterator cur = null;
            private IntStream s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextShort());

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
    public <T> Stream<T> flatMapToObj(final ShortFunction<? extends Stream<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() {
            private Iterator<? extends T> cur = null;
            private Stream<? extends T> s = null;

            @Override
            public boolean hasNext() {
                while (cur == null || !cur.hasNext()) {
                    closeMappedStream();

                    if (elements.hasNext()) {
                        s = mapper.apply(elements.nextShort());

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

    @Override
    public <T> Stream<T> flatmapToObj(final ShortFunction<? extends Collection<? extends T>> mapper) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<? extends T> cur = null;
            private Collection<? extends T> c = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.nextShort());
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
                private boolean hasNext = false;
                private short prev = 0;
                private short next = 0;
                private boolean isFirst = true;

                @Override
                public boolean hasNext() {
                    if (!hasNext) {
                        while (elements.hasNext()) {
                            next = elements.nextShort();

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
                public short nextShort() {
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
    public ShortStream limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        return newStream(new ShortIteratorEx() { //NOSONAR
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public short nextShort() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;
                return elements.nextShort();
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
    public ShortStream skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        //    if (n == 0) {
        //        return newStream(elements, isSorted());
        //    }

        if (n == 0) {
            return this;
        }

        return newStream(new ShortIteratorEx() { //NOSONAR
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
            public short nextShort() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.nextShort();
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
            public short[] toArray() {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.toArray();
            }
        }, isSorted());
    }

    @Override
    public ShortStream top(final int n) throws IllegalStateException {
        assertNotClosed();

        return top(n, SHORT_COMPARATOR);
    }

    @Override
    public ShortStream top(final int n, final Comparator<? super Short> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return newStream(ShortIteratorEx.empty(), false);
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
                    if (isSorted() && isSameComparator(comparator, comparator())) {
                        final LinkedList<Short> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.nextShort());
                        }

                        aar = Array.unbox(queue.toArray(N.EMPTY_SHORT_OBJ_ARRAY));
                    } else {
                        final Comparator<? super Short> cmp = comparator == null ? SHORT_COMPARATOR : comparator;
                        final Queue<Short> heap = new PriorityQueue<>(Math.min(n, 16), cmp);

                        Short next = null;
                        while (elements.hasNext()) {
                            next = elements.nextShort();

                            if (heap.size() >= n) {
                                if (cmp.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = Array.unbox(heap.toArray(N.EMPTY_SHORT_OBJ_ARRAY));
                    }

                    to = aar.length;
                }
            }
        }, false);
    }

    @Override
    public ShortStream onEach(final ShortConsumer action) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ShortIteratorEx() { //NOSONAR
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public short nextShort() {
                final short next = elements.nextShort();

                action.accept(next);
                return next;
            }
        }, isSorted());
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.ShortConsumer<E> action) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.nextShort());
            }
        } finally {
            close();
        }
    }

    @Override
    protected short[] toArray(final boolean closeStream) {
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
    public ShortList toShortList() throws IllegalStateException {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    @Override
    public List<Short> toList() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofList());
    }

    @Override
    public Set<Short> toSet() throws IllegalStateException {
        assertNotClosed();

        return toCollection(Suppliers.ofSet());
    }

    @Override
    public <C extends Collection<Short>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextShort());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Short> toMultiset() throws IllegalStateException {
        assertNotClosed();

        return toMultiset(Suppliers.ofMultiset());
    }

    @Override
    public Multiset<Short> toMultiset(final Supplier<? extends Multiset<Short>> supplier) throws IllegalStateException {
        assertNotClosed();

        try {
            final Multiset<Short> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextShort());
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
            short next = 0;

            while (elements.hasNext()) {
                next = elements.nextShort();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
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
            short next = 0;

            while (elements.hasNext()) {
                next = elements.nextShort();
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
    public short reduce(final short identity, final ShortBinaryOperator accumulator) throws IllegalStateException {
        assertNotClosed();

        try {
            short result = identity;

            while (elements.hasNext()) {
                result = accumulator.applyAsShort(result, elements.nextShort());
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
            if (!elements.hasNext()) {
                return OptionalShort.empty();
            }

            short result = elements.nextShort();

            while (elements.hasNext()) {
                result = accumulator.applyAsShort(result, elements.nextShort());
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

            while (elements.hasNext()) {
                accumulator.accept(result, elements.nextShort());
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
            if (!elements.hasNext()) {
                return OptionalShort.empty();
            } else if (isSorted()) {
                return OptionalShort.of(elements.nextShort());
            }

            short candidate = elements.nextShort();
            short next = 0;

            while (elements.hasNext()) {
                next = elements.nextShort();

                if (next < candidate) {
                    candidate = next;
                }
            }

            return OptionalShort.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort max() throws IllegalStateException {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalShort.empty();
            } else if (isSorted()) {
                short next = 0;

                while (elements.hasNext()) {
                    next = elements.nextShort();
                }

                return OptionalShort.of(next);
            }

            short candidate = elements.nextShort();
            short next = 0;

            while (elements.hasNext()) {
                next = elements.nextShort();

                if (next > candidate) {
                    candidate = next;
                }
            }

            return OptionalShort.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort kthLargest(final int k) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (isSorted()) {
                // For sorted ascending, kthLargest = (k-1)th from the end.
                // Use a ring buffer of size k while draining the iterator.
                short[] window = null;
                int idx = 0;
                int size = 0;
                while (elements.hasNext()) {
                    final short v = elements.nextShort();
                    if (window == null) {
                        window = new short[Math.min(k, 16)];
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
                    return OptionalShort.empty();
                }
                return OptionalShort.of(window[idx]);
            }

            if (!elements.hasNext()) {
                return OptionalShort.empty();
            }

            @SuppressWarnings("resource")
            final Optional<Short> optional = boxed().kthLargest(k, SHORT_COMPARATOR);

            return optional.isPresent() ? OptionalShort.of(optional.get()) : OptionalShort.empty();
        } finally {
            close();
        }
    }

    @Override
    public int sum() throws IllegalStateException {
        assertNotClosed();

        try {
            long result = 0;

            while (elements.hasNext()) {
                result += elements.nextShort();
            }

            return Numbers.toIntExact(result);
        } finally {
            close();
        }
    }

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
                sum += elements.nextShort();
                count++;
            } while (elements.hasNext());

            return OptionalDouble.of(((double) sum) / count);
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
    public ShortSummaryStatistics summaryStatistics() throws IllegalStateException {
        assertNotClosed();

        try {
            final ShortSummaryStatistics result = new ShortSummaryStatistics();

            while (elements.hasNext()) {
                result.accept(elements.nextShort());
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
            while (elements.hasNext()) {
                if (predicate.test(elements.nextShort())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    @Override
    public <E extends Exception> boolean allMatch(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (!predicate.test(elements.nextShort())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextShort())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> OptionalShort findFirst(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                final short e = elements.nextShort();

                if (predicate.test(e)) {
                    return OptionalShort.of(e);
                }
            }
        } finally {
            close();
        }

        return OptionalShort.empty();
    }

    @Override
    public <E extends Exception> OptionalShort findLast(final Throwables.ShortPredicate<E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalShort.empty();
            }

            boolean hasResult = false;
            short e = 0;
            short result = 0;

            while (elements.hasNext()) {
                e = elements.nextShort();

                if (predicate.test(e)) {
                    result = e;
                    hasResult = true;
                }
            }

            return hasResult ? OptionalShort.of(result) : OptionalShort.empty();
        } finally {
            close();
        }
    }

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
                return elements.nextShort();
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
    ShortIteratorEx iteratorEx() {
        assertNotClosed();

        return elements;
    }

    @Override
    public ShortStream appendIfEmpty(final Supplier<? extends ShortStream> supplier) throws IllegalStateException {
        assertNotClosed();

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
                    if (elements.hasNext()) {
                        iter = elements;
                    } else {
                        final ShortStream s = supplier.get();
                        holder.setValue(s);
                        iter = s.iteratorEx();
                    }
                }
            }
        }, false).onClose(() -> close(holder));
    }

    @Override
    public ShortStream ifEmpty(final Runnable action) throws IllegalStateException {
        assertNotClosed();

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
                    iter = elements;

                    if (!iter.hasNext()) {
                        action.run();
                    }
                }
            }
        }, isSorted());
    }

    @Override
    protected ShortStream parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads) {
        assertNotClosed();

        return new ParallelIteratorShortStream(elements, isSorted(), maxThreadNum, splitor, asyncExecutor, cancelUncompletedThreads, closeHandlers());
    }

    @Override
    protected boolean isEmpty() {
        return !elements.hasNext();
    }
}
