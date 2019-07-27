/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.stream;

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
import java.util.concurrent.Executor;

import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Primitives;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortToIntFunction;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
class IteratorShortStream extends AbstractShortStream {
    ShortIteratorEx elements;

    //    OptionalShort head;
    //    ShortStream tail;

    //    ShortStream head2;
    //    OptionalShort tail2;

    IteratorShortStream(final ShortIterator values) {
        this(values, null);
    }

    IteratorShortStream(final ShortIterator values, final Collection<Runnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    IteratorShortStream(final ShortIterator values, final boolean sorted, final Collection<Runnable> closeHandlers) {
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

        this.elements = tmp;
    }

    @Override
    public ShortStream filter(final ShortPredicate predicate) {
        return newStream(new ShortIteratorEx() {
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
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
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }
        }, sorted);
    }

    @Override
    public ShortStream takeWhile(final ShortPredicate predicate) {
        return newStream(new ShortIteratorEx() {
            private boolean hasMore = true;
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                if (hasNext == false && hasMore && elements.hasNext()) {
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
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }

        }, sorted);
    }

    @Override
    public ShortStream dropWhile(final ShortPredicate predicate) {
        return newStream(new ShortIteratorEx() {
            private boolean hasNext = false;
            private short next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
                    if (dropped == false) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextShort();

                            if (predicate.test(next) == false) {
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
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }

        }, sorted);
    }

    @Override
    public ShortStream map(final ShortUnaryOperator mapper) {
        return newStream(new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public short nextShort() {
                return mapper.applyAsShort(elements.nextShort());
            }

            //    @Override
            //    public long count() {
            //        return elements.count();
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        elements.skip(n);
            //    }
        }, false);
    }

    @Override
    public IntStream mapToInt(final ShortToIntFunction mapper) {
        return newStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() {
                return mapper.applyAsInt(elements.nextShort());
            }

            //    @Override
            //    public long count() {
            //        return elements.count();
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        elements.skip(n);
            //    }
        }, false);
    }

    @Override
    public <U> Stream<U> mapToObj(final ShortFunction<? extends U> mapper) {
        return newStream(new ObjIteratorEx<U>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(elements.nextShort());
            }

            //    @Override
            //    public long count() {
            //        return elements.count();
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        elements.skip(n);
            //    }
        }, false, null);
    }

    @Override
    public ShortStream flatMap(final ShortFunction<? extends ShortStream> mapper) {
        final ShortIteratorEx iter = new ShortIteratorEx() {
            private ShortIterator cur = null;
            private ShortStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextShort());

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public short nextShort() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextShort();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorShortStream(iter, newCloseHandlers);
    }

    @Override
    public IntStream flatMapToInt(final ShortFunction<? extends IntStream> mapper) {
        final IntIteratorEx iter = new IntIteratorEx() {
            private IntIterator cur = null;
            private IntStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextShort());

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public int nextInt() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextInt();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorIntStream(iter, newCloseHandlers);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final ShortFunction<? extends Stream<T>> mapper) {
        final ObjIteratorEx<T> iter = new ObjIteratorEx<T>() {
            private Iterator<? extends T> cur = null;
            private Stream<? extends T> s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextShort());

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        final Deque<Runnable> tmp = s.closeHandlers;

                        closeHandle = new Runnable() {
                            @Override
                            public void run() {
                                Stream.close(tmp);
                            }
                        };
                    }

                    cur = s.iteratorEx();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public void close() {
                if (closeHandle != null) {
                    final Runnable tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Runnable> newCloseHandlers = N.isNullOrEmpty(closeHandlers) ? new LocalArrayDeque<Runnable>(1)
                : new LocalArrayDeque<Runnable>(closeHandlers);

        newCloseHandlers.add(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });

        return new IteratorStream<>(iter, newCloseHandlers);
    }

    @Override
    public Stream<ShortList> splitToList(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<ShortList>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public ShortList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final ShortList result = new ShortList(chunkSize);

                while (result.size() < chunkSize && elements.hasNext()) {
                    result.add(elements.nextShort());
                }

                return result;
            }

            @Override
            public long count() {
                final long len = elements.count();
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void skip(long n) {
                elements.skip(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, false, null);
    }

    @Override
    public Stream<ShortList> splitToList(final ShortPredicate predicate) {
        return newStream(new ObjIteratorEx<ShortList>() {
            private short next;
            private boolean hasNext = false;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return hasNext == true || elements.hasNext();
            }

            @Override
            public ShortList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final ShortList result = new ShortList();

                if (hasNext == false) {
                    next = elements.nextShort();
                    hasNext = true;
                }

                while (hasNext) {
                    if (result.size() == 0) {
                        result.add(next);
                        preCondition = predicate.test(next);
                        next = (hasNext = elements.hasNext()) ? elements.nextShort() : 0;
                    } else if (predicate.test(next) == preCondition) {
                        result.add(next);
                        next = (hasNext = elements.hasNext()) ? elements.nextShort() : 0;
                    } else {
                        break;
                    }
                }

                return result;
            }

        }, false, null);
    }

    @Override
    public Stream<ShortStream> splitAt(final int where) {
        checkArgNotNegative(where, "where");

        return newStream(new ObjIteratorEx<ShortStream>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public ShortStream next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                ShortStream result = null;

                if (cursor == 0) {
                    final ShortList list = new ShortList();
                    int cnt = 0;

                    while (cnt++ < where && elements.hasNext()) {
                        list.add(elements.nextShort());
                    }

                    result = new ArrayShortStream(list.array(), 0, list.size(), sorted, null);
                } else {
                    result = new IteratorShortStream(elements, sorted, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                elements.count();

                return 2 - cursor;
            }

            @Override
            public void skip(long n) {
                if (n == 0) {
                    return;
                } else if (n == 1) {
                    if (cursor == 0) {
                        elements.skip(where);
                    } else {
                        elements.skip(Long.MAX_VALUE);
                    }
                } else {
                    elements.skip(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }
        }, false, null);
    }

    @Override
    public Stream<ShortList> slidingToList(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<ShortList>() {
            private ShortList prev = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.nextShort();
                    }

                    toSkip = false;
                }

                return elements.hasNext();
            }

            @Override
            public ShortList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                ShortList result = null;
                int cnt = 0;

                if (prev != null && increment < windowSize) {
                    cnt = windowSize - increment;

                    if (cnt <= 8) {
                        result = new ShortList(windowSize);

                        for (int i = windowSize - cnt; i < windowSize; i++) {
                            result.add(prev.get(i));
                        }
                    } else {
                        final short[] dest = new short[windowSize];
                        N.copy(prev.array(), windowSize - cnt, dest, 0, cnt);
                        result = ShortList.of(dest, cnt);
                    }
                }

                if (result == null) {
                    result = new ShortList(windowSize);
                }

                while (cnt++ < windowSize && elements.hasNext()) {
                    result.add(elements.nextShort());
                }

                toSkip = increment > windowSize;

                return prev = result;
            }

            @Override
            public long count() {
                final int prevSize = increment >= windowSize ? 0 : (prev == null ? 0 : prev.size());
                final long len = prevSize + elements.count();

                if (len == prevSize) {
                    return 0;
                } else if (len <= windowSize) {
                    return 1;
                } else {
                    final long rlen = len - windowSize;
                    return 1 + (rlen % increment == 0 ? rlen / increment : rlen / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n == 0) {
                    return;
                }

                if (increment >= windowSize) {
                    elements.skip(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    final ShortList tmp = new ShortList(windowSize);

                    if (N.isNullOrEmpty(prev)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.skip(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (prev == null ? 0 : prev.size());

                        if (m < prevSize) {
                            tmp.addAll(prev.copy((int) m, prevSize));
                        } else {
                            elements.skip(m - prevSize);
                        }
                    }

                    int cnt = tmp.size();

                    while (cnt++ < windowSize && elements.hasNext()) {
                        tmp.add(elements.nextShort());
                    }

                    prev = tmp;
                }
            }
        }, false, null);
    }

    @Override
    public ShortStream top(int n) {
        return top(n, SHORT_COMPARATOR);
    }

    @Override
    public ShortStream top(final int n, final Comparator<? super Short> comparator) {
        checkArgPositive(n, "n");

        return newStream(new ShortIteratorEx() {
            private boolean initialized = false;
            private short[] aar;
            private int cursor = 0;
            private int to;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor < to;
            }

            @Override
            public short nextShort() {
                if (initialized == false) {
                    init();
                }

                if (cursor >= to) {
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return to - cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n > to - cursor ? to : cursor + (int) n;
            }

            @Override
            public short[] toArray() {
                if (initialized == false) {
                    init();
                }

                final short[] a = new short[to - cursor];

                N.copy(aar, cursor, a, 0, to - cursor);

                return a;
            }

            @Override
            public ShortList toList() {
                return ShortList.of(toArray());
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    if (sorted && isSameComparator(comparator, cmp)) {
                        final LinkedList<Short> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.nextShort());
                        }

                        aar = Primitives.unbox(N.EMPTY_SHORT_OBJ_ARRAY);
                    } else {
                        final Queue<Short> heap = new PriorityQueue<>(n, comparator);

                        Short next = null;
                        while (elements.hasNext()) {
                            next = elements.nextShort();

                            if (heap.size() >= n) {
                                if (comparator.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = Primitives.unbox(heap.toArray(N.EMPTY_SHORT_OBJ_ARRAY));
                    }

                    to = aar.length;
                }
            }
        }, false);
    }

    @Override
    public ShortStream peek(final ShortConsumer action) {
        return newStream(new ShortIteratorEx() {
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
        }, sorted);
    }

    @Override
    public ShortStream limit(final long maxSize) {
        checkArgNotNegative(maxSize, "maxSize");

        return newStream(new ShortIteratorEx() {
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public short nextShort() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException();
                }

                cnt++;
                return elements.nextShort();
            }

            @Override
            public void skip(long n) {
                elements.skip(n);
            }
        }, sorted);
    }

    @Override
    public ShortStream skip(final long n) {
        checkArgNotNegative(n, "n");

        return newStream(new ShortIteratorEx() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.hasNext();
            }

            @Override
            public short nextShort() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.nextShort();
            }

            @Override
            public long count() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.count();
            }

            @Override
            public void skip(long n2) {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                elements.skip(n2);
            }

            @Override
            public short[] toArray() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.toArray();
            }
        }, sorted);
    }

    @Override
    public <E extends Exception> void forEach(final Try.ShortConsumer<E> action) throws E {
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
    public short[] toArray() {
        assertNotClosed();

        try {
            return elements.toArray();
        } finally {
            close();
        }
    }

    @Override
    public ShortList toShortList() {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    @Override
    public List<Short> toList() {
        return toCollection(Suppliers.<Short> ofList());
    }

    @Override
    public Set<Short> toSet() {
        return toCollection(Suppliers.<Short> ofSet());
    }

    @Override
    public <C extends Collection<Short>> C toCollection(Supplier<? extends C> supplier) {
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
    public Multiset<Short> toMultiset() {
        return toMultiset(Suppliers.<Short> ofMultiset());
    }

    @Override
    public Multiset<Short> toMultiset(Supplier<? extends Multiset<Short>> supplier) {
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
    public LongMultiset<Short> toLongMultiset() {
        return toLongMultiset(Suppliers.<Short> ofLongMultiset());
    }

    @Override
    public LongMultiset<Short> toLongMultiset(Supplier<? extends LongMultiset<Short>> supplier) {
        assertNotClosed();

        try {
            final LongMultiset<Short> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextShort());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(ShortFunction<? extends K> keyMapper, ShortFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction,
            Supplier<? extends M> mapFactory) {
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
    public <K, A, D, M extends Map<K, D>> M toMap(final ShortFunction<? extends K> keyMapper, final Collector<Short, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, Short> downstreamAccumulator = downstream.accumulator();
            final Map<K, A> intermediate = (Map<K, A>) result;
            K key = null;
            A v = null;
            short next = 0;

            while (elements.hasNext()) {
                next = elements.nextShort();
                key = checkArgNotNull(keyMapper.apply(next), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    if ((v = downstreamSupplier.get()) != null) {
                        intermediate.put(key, v);
                    }
                }

                downstreamAccumulator.accept(v, next);
            }

            final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
                @Override
                public A apply(K k, A v) {
                    return (A) downstream.finisher().apply(v);
                }
            };

            Collectors.replaceAll(intermediate, function);

            return result;
        } finally {
            close();
        }
    }

    @Override
    public short reduce(short identity, ShortBinaryOperator op) {
        assertNotClosed();

        try {
            short result = identity;

            while (elements.hasNext()) {
                result = op.applyAsShort(result, elements.nextShort());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort reduce(ShortBinaryOperator op) {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalShort.empty();
            }

            short result = elements.nextShort();

            while (elements.hasNext()) {
                result = op.applyAsShort(result, elements.nextShort());
            }

            return OptionalShort.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjShortConsumer<? super R> accumulator, BiConsumer<R, R> combiner) {
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
    public OptionalShort min() {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalShort.empty();
            } else if (sorted) {
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
    public OptionalShort max() {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalShort.empty();
            } else if (sorted) {
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
    public OptionalShort kthLargest(int k) {
        checkArgPositive(k, "k");
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalShort.empty();
            }

            final Optional<Short> optional = boxed().kthLargest(k, SHORT_COMPARATOR);

            return optional.isPresent() ? OptionalShort.of(optional.get()) : OptionalShort.empty();
        } finally {
            close();
        }
    }

    @Override
    public int sum() {
        assertNotClosed();

        try {
            long result = 0;

            while (elements.hasNext()) {
                result += elements.nextShort();
            }

            return N.toIntExact(result);
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble average() {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalDouble.empty();
            }

            long sum = 0;
            long count = 0;

            while (elements.hasNext()) {
                sum += elements.nextShort();
                count++;
            }

            return OptionalDouble.of(((double) sum) / count);
        } finally {
            close();
        }
    }

    @Override
    public long count() {
        assertNotClosed();

        try {
            return elements.count();
        } finally {
            close();
        }
    }

    @Override
    public ShortSummaryStatistics summarize() {
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
    public <E extends Exception> boolean anyMatch(final Try.ShortPredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean allMatch(final Try.ShortPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextShort()) == false) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.ShortPredicate<E> predicate) throws E {
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
    public <E extends Exception> OptionalShort findFirst(final Try.ShortPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                short e = elements.nextShort();

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
    public <E extends Exception> OptionalShort findLast(final Try.ShortPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
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
    public IntStream asIntStream() {
        return newStream(new IntIteratorEx() {
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
            public void skip(long n) {
                elements.skip(n);
            }
        }, sorted);
    }

    @Override
    public Stream<Short> boxed() {
        return new IteratorStream<>(iteratorEx(), sorted, sorted ? SHORT_COMPARATOR : null, closeHandlers);
    }

    @Override
    ShortIteratorEx iteratorEx() {
        return elements;
    }

    @Override
    public ShortStream appendIfEmpty(final Supplier<ShortStream> supplier) {
        if (elements.hasNext() == false) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

    @Override
    public ShortStream parallel(int maxThreadNum, Splitor splitor) {
        return new ParallelIteratorShortStream(elements, sorted, checkMaxThreadNum(maxThreadNum), checkSplitor(splitor), asyncExecutor(), closeHandlers);
    }

    @Override
    public ShortStream parallel(final int maxThreadNum, final Executor executor) {
        return new ParallelIteratorShortStream(elements, sorted, checkMaxThreadNum(maxThreadNum), splitor(), createAsyncExecutor(executor), closeHandlers);
    }

    @Override
    public ShortStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new IteratorShortStream(elements, sorted, newCloseHandlers);
    }
}
