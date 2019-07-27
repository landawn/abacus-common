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

import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Primitives;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatToDoubleFunction;
import com.landawn.abacus.util.function.FloatToIntFunction;
import com.landawn.abacus.util.function.FloatToLongFunction;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.function.ObjFloatConsumer;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
class IteratorFloatStream extends AbstractFloatStream {
    final FloatIteratorEx elements;

    //    OptionalFloat head;
    //    FloatStream tail;

    //    FloatStream head2;
    //    OptionalFloat tail2;

    IteratorFloatStream(final FloatIterator values) {
        this(values, null);
    }

    IteratorFloatStream(final FloatIterator values, final Collection<Runnable> closeHandlers) {
        this(values, false, closeHandlers);
    }

    IteratorFloatStream(final FloatIterator values, final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);

        FloatIteratorEx tmp = null;

        if (values instanceof FloatIteratorEx) {
            tmp = (FloatIteratorEx) values;
        } else {
            tmp = new FloatIteratorEx() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public float nextFloat() {
                    return values.nextFloat();
                }
            };
        }

        this.elements = tmp;
    }

    @Override
    public FloatStream filter(final FloatPredicate predicate) {
        return newStream(new FloatIteratorEx() {
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
                    while (elements.hasNext()) {
                        next = elements.nextFloat();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public float nextFloat() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }
        }, sorted);
    }

    @Override
    public FloatStream takeWhile(final FloatPredicate predicate) {
        return newStream(new FloatIteratorEx() {
            private boolean hasMore = true;
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                if (hasNext == false && hasMore && elements.hasNext()) {
                    next = elements.nextFloat();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public float nextFloat() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }

        }, sorted);
    }

    @Override
    public FloatStream dropWhile(final FloatPredicate predicate) {
        return newStream(new FloatIteratorEx() {
            private boolean hasNext = false;
            private float next = 0;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
                    if (dropped == false) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.nextFloat();

                            if (predicate.test(next) == false) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.nextFloat();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public float nextFloat() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }

        }, sorted);
    }

    @Override
    public FloatStream map(final FloatUnaryOperator mapper) {
        return newStream(new FloatIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() {
                return mapper.applyAsFloat(elements.nextFloat());
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
    public IntStream mapToInt(final FloatToIntFunction mapper) {
        return newStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() {
                return mapper.applyAsInt(elements.nextFloat());
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
    public LongStream mapToLong(final FloatToLongFunction mapper) {
        return newStream(new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public long nextLong() {
                return mapper.applyAsLong(elements.nextFloat());
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
    public DoubleStream mapToDouble(final FloatToDoubleFunction mapper) {
        return newStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() {
                return mapper.applyAsDouble(elements.nextFloat());
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
    public <U> Stream<U> mapToObj(final FloatFunction<? extends U> mapper) {
        return newStream(new ObjIteratorEx<U>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public U next() {
                return mapper.apply(elements.nextFloat());
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
    public FloatStream flatMap(final FloatFunction<? extends FloatStream> mapper) {
        final FloatIteratorEx iter = new FloatIteratorEx() {
            private FloatIterator cur = null;
            private FloatStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextFloat());

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
            public float nextFloat() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextFloat();
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

        return new IteratorFloatStream(iter, newCloseHandlers);
    }

    @Override
    public IntStream flatMapToInt(final FloatFunction<? extends IntStream> mapper) {
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

                    s = mapper.apply(elements.nextFloat());

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
    public LongStream flatMapToLong(final FloatFunction<? extends LongStream> mapper) {
        final LongIteratorEx iter = new LongIteratorEx() {
            private LongIterator cur = null;
            private LongStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextFloat());

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
            public long nextLong() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextLong();
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

        return new IteratorLongStream(iter, newCloseHandlers);
    }

    @Override
    public DoubleStream flatMapToDouble(final FloatFunction<? extends DoubleStream> mapper) {
        final DoubleIteratorEx iter = new DoubleIteratorEx() {
            private DoubleIterator cur = null;
            private DoubleStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.nextFloat());

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
            public double nextDouble() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextDouble();
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

        return new IteratorDoubleStream(iter, newCloseHandlers);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final FloatFunction<? extends Stream<T>> mapper) {
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

                    s = mapper.apply(elements.nextFloat());

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
    public Stream<FloatList> splitToList(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<FloatList>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public FloatList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final FloatList result = new FloatList(chunkSize);

                while (result.size() < chunkSize && elements.hasNext()) {
                    result.add(elements.nextFloat());
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
    public Stream<FloatList> splitToList(final FloatPredicate predicate) {
        return newStream(new ObjIteratorEx<FloatList>() {
            private float next;
            private boolean hasNext = false;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return hasNext == true || elements.hasNext();
            }

            @Override
            public FloatList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final FloatList result = new FloatList();

                if (hasNext == false) {
                    next = elements.nextFloat();
                    hasNext = true;
                }

                while (hasNext) {
                    if (result.size() == 0) {
                        result.add(next);
                        preCondition = predicate.test(next);
                        next = (hasNext = elements.hasNext()) ? elements.nextFloat() : 0;
                    } else if (predicate.test(next) == preCondition) {
                        result.add(next);
                        next = (hasNext = elements.hasNext()) ? elements.nextFloat() : 0;
                    } else {
                        break;
                    }
                }

                return result;
            }

        }, false, null);
    }

    @Override
    public Stream<FloatStream> splitAt(final int where) {
        checkArgNotNegative(where, "where");

        return newStream(new ObjIteratorEx<FloatStream>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public FloatStream next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                FloatStream result = null;

                if (cursor == 0) {
                    final FloatList list = new FloatList();
                    int cnt = 0;

                    while (cnt++ < where && elements.hasNext()) {
                        list.add(elements.nextFloat());
                    }

                    result = new ArrayFloatStream(list.array(), 0, list.size(), sorted, null);
                } else {
                    result = new IteratorFloatStream(elements, sorted, null);
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
    public Stream<FloatList> slidingToList(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<FloatList>() {
            private FloatList prev = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.nextFloat();
                    }

                    toSkip = false;
                }

                return elements.hasNext();
            }

            @Override
            public FloatList next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                FloatList result = null;
                int cnt = 0;

                if (prev != null && increment < windowSize) {
                    cnt = windowSize - increment;

                    if (cnt <= 8) {
                        result = new FloatList(windowSize);

                        for (int i = windowSize - cnt; i < windowSize; i++) {
                            result.add(prev.get(i));
                        }
                    } else {
                        final float[] dest = new float[windowSize];
                        N.copy(prev.array(), windowSize - cnt, dest, 0, cnt);
                        result = FloatList.of(dest, cnt);
                    }
                }

                if (result == null) {
                    result = new FloatList(windowSize);
                }

                while (cnt++ < windowSize && elements.hasNext()) {
                    result.add(elements.nextFloat());
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
                    final FloatList tmp = new FloatList(windowSize);

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
                        tmp.add(elements.nextFloat());
                    }

                    prev = tmp;
                }
            }
        }, false, null);
    }

    @Override
    public FloatStream top(int n) {
        return top(n, FLOAT_COMPARATOR);
    }

    @Override
    public FloatStream top(final int n, final Comparator<? super Float> comparator) {
        checkArgPositive(n, "n");

        return newStream(new FloatIteratorEx() {
            private boolean initialized = false;
            private float[] aar;
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
            public float nextFloat() {
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
            public float[] toArray() {
                if (initialized == false) {
                    init();
                }

                final float[] a = new float[to - cursor];

                N.copy(aar, cursor, a, 0, to - cursor);

                return a;
            }

            @Override
            public FloatList toList() {
                return FloatList.of(toArray());
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    if (sorted && isSameComparator(comparator, cmp)) {
                        final LinkedList<Float> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.nextFloat());
                        }

                        aar = Primitives.unbox(N.EMPTY_FLOAT_OBJ_ARRAY);
                    } else {
                        final Queue<Float> heap = new PriorityQueue<>(n, comparator);

                        Float next = null;
                        while (elements.hasNext()) {
                            next = elements.nextFloat();

                            if (heap.size() >= n) {
                                if (comparator.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = Primitives.unbox(heap.toArray(N.EMPTY_FLOAT_OBJ_ARRAY));
                    }

                    to = aar.length;
                }
            }
        }, false);
    }

    @Override
    public FloatStream peek(final FloatConsumer action) {
        return newStream(new FloatIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() {
                final float next = elements.nextFloat();
                action.accept(next);
                return next;
            }
        }, sorted);
    }

    @Override
    public FloatStream limit(final long maxSize) {
        checkArgNotNegative(maxSize, "maxSize");

        return newStream(new FloatIteratorEx() {
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public float nextFloat() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException();
                }

                cnt++;
                return elements.nextFloat();
            }

            @Override
            public void skip(long n) {
                elements.skip(n);
            }
        }, sorted);
    }

    @Override
    public FloatStream skip(final long n) {
        checkArgNotNegative(n, "n");

        return newStream(new FloatIteratorEx() {
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
            public float nextFloat() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.nextFloat();
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
            public float[] toArray() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.toArray();
            }
        }, sorted);
    }

    @Override
    public <E extends Exception> void forEach(final Try.FloatConsumer<E> action) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.nextFloat());
            }
        } finally {
            close();
        }
    }

    @Override
    public float[] toArray() {
        assertNotClosed();

        try {
            return elements.toArray();
        } finally {
            close();
        }
    }

    @Override
    public FloatList toFloatList() {
        assertNotClosed();

        try {
            return elements.toList();
        } finally {
            close();
        }
    }

    @Override
    public List<Float> toList() {
        return toCollection(Suppliers.<Float> ofList());
    }

    @Override
    public Set<Float> toSet() {
        return toCollection(Suppliers.<Float> ofSet());
    }

    @Override
    public <C extends Collection<Float>> C toCollection(Supplier<? extends C> supplier) {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Float> toMultiset() {
        return toMultiset(Suppliers.<Float> ofMultiset());
    }

    @Override
    public Multiset<Float> toMultiset(Supplier<? extends Multiset<Float>> supplier) {
        assertNotClosed();

        try {
            final Multiset<Float> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<Float> toLongMultiset() {
        return toLongMultiset(Suppliers.<Float> ofLongMultiset());
    }

    @Override
    public LongMultiset<Float> toLongMultiset(Supplier<? extends LongMultiset<Float>> supplier) {
        assertNotClosed();

        try {
            final LongMultiset<Float> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(FloatFunction<? extends K> keyMapper, FloatFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction,
            Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            float next = 0;

            while (elements.hasNext()) {
                next = elements.nextFloat();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, A, D, M extends Map<K, D>> M toMap(final FloatFunction<? extends K> keyMapper, final Collector<Float, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, Float> downstreamAccumulator = downstream.accumulator();
            final Map<K, A> intermediate = (Map<K, A>) result;
            K key = null;
            A v = null;
            float next = 0;

            while (elements.hasNext()) {
                next = elements.nextFloat();
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
    public float reduce(float identity, FloatBinaryOperator op) {
        assertNotClosed();

        try {
            float result = identity;

            while (elements.hasNext()) {
                result = op.applyAsFloat(result, elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat reduce(FloatBinaryOperator op) {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalFloat.empty();
            }

            float result = elements.nextFloat();

            while (elements.hasNext()) {
                result = op.applyAsFloat(result, elements.nextFloat());
            }

            return OptionalFloat.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjFloatConsumer<? super R> accumulator, BiConsumer<R, R> combiner) {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat min() {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalFloat.empty();
            } else if (sorted) {
                return OptionalFloat.of(elements.nextFloat());
            }

            float candidate = elements.nextFloat();
            float next = 0;

            while (elements.hasNext()) {
                next = elements.nextFloat();

                if (N.compare(next, candidate) < 0) {
                    candidate = next;
                }
            }

            return OptionalFloat.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat max() {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalFloat.empty();
            } else if (sorted) {
                float next = 0;

                while (elements.hasNext()) {
                    next = elements.nextFloat();
                }

                return OptionalFloat.of(next);
            }

            float candidate = elements.nextFloat();
            float next = 0;

            while (elements.hasNext()) {
                next = elements.nextFloat();

                if (N.compare(next, candidate) > 0) {
                    candidate = next;
                }
            }

            return OptionalFloat.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat kthLargest(int k) {
        checkArgPositive(k, "k");
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalFloat.empty();
            }

            final Optional<Float> optional = boxed().kthLargest(k, FLOAT_COMPARATOR);

            return optional.isPresent() ? OptionalFloat.of(optional.get()) : OptionalFloat.empty();
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
    public FloatSummaryStatistics summarize() {
        assertNotClosed();

        try {
            final FloatSummaryStatistics result = new FloatSummaryStatistics();

            while (elements.hasNext()) {
                result.accept(elements.nextFloat());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.FloatPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextFloat())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    @Override
    public <E extends Exception> boolean allMatch(final Try.FloatPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextFloat()) == false) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.FloatPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.nextFloat())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> OptionalFloat findFirst(final Try.FloatPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                float e = elements.nextFloat();

                if (predicate.test(e)) {
                    return OptionalFloat.of(e);
                }
            }
        } finally {
            close();
        }

        return OptionalFloat.empty();
    }

    @Override
    public <E extends Exception> OptionalFloat findLast(final Try.FloatPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalFloat.empty();
            }

            boolean hasResult = false;
            float e = 0;
            float result = 0;

            while (elements.hasNext()) {
                e = elements.nextFloat();

                if (predicate.test(e)) {
                    result = e;
                    hasResult = true;
                }
            }

            return hasResult ? OptionalFloat.of(result) : OptionalFloat.empty();
        } finally {
            close();
        }
    }

    @Override
    public DoubleStream asDoubleStream() {
        return newStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() {
                return elements.nextFloat();
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
    public Stream<Float> boxed() {
        return new IteratorStream<>(iteratorEx(), sorted, sorted ? FLOAT_COMPARATOR : null, closeHandlers);
    }

    @Override
    FloatIteratorEx iteratorEx() {
        return elements;
    }

    @Override
    public FloatStream appendIfEmpty(final Supplier<FloatStream> supplier) {
        if (elements.hasNext() == false) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

    @Override
    public FloatStream parallel(int maxThreadNum, Splitor splitor) {
        return new ParallelIteratorFloatStream(elements, sorted, checkMaxThreadNum(maxThreadNum), checkSplitor(splitor), asyncExecutor(), closeHandlers);
    }

    @Override
    public FloatStream parallel(final int maxThreadNum, final Executor executor) {
        return new ParallelIteratorFloatStream(elements, sorted, checkMaxThreadNum(maxThreadNum), splitor(), createAsyncExecutor(executor), closeHandlers);
    }

    @Override
    public FloatStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new IteratorFloatStream(elements, sorted, newCloseHandlers);
    }
}
