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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executor;

import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.LongSummaryStatistics;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongToDoubleFunction;
import com.landawn.abacus.util.function.LongToFloatFunction;
import com.landawn.abacus.util.function.LongToIntFunction;
import com.landawn.abacus.util.function.LongUnaryOperator;
import com.landawn.abacus.util.function.ObjLongConsumer;
import com.landawn.abacus.util.function.Supplier;

/**
 * 
 */
class ArrayLongStream extends AbstractLongStream {
    final long[] elements;
    final int fromIndex;
    final int toIndex;

    ArrayLongStream(final long[] values) {
        this(values, 0, values.length);
    }

    ArrayLongStream(final long[] values, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, closeHandlers);
    }

    ArrayLongStream(final long[] values, final boolean sorted, final Collection<Runnable> closeHandlers) {
        this(values, 0, values.length, sorted, closeHandlers);
    }

    ArrayLongStream(final long[] values, final int fromIndex, final int toIndex) {
        this(values, fromIndex, toIndex, null);
    }

    ArrayLongStream(final long[] values, final int fromIndex, final int toIndex, final Collection<Runnable> closeHandlers) {
        this(values, fromIndex, toIndex, false, closeHandlers);
    }

    ArrayLongStream(final long[] values, final int fromIndex, final int toIndex, final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);

        checkFromToIndex(fromIndex, toIndex, N.len(values));

        this.elements = values;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    @Override
    public LongStream filter(final LongPredicate predicate) {
        return newStream(new LongIteratorEx() {
            private boolean hasNext = false;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                if (hasNext == false && cursor < toIndex) {
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
            public long nextLong() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public LongStream takeWhile(final LongPredicate predicate) {
        return newStream(new LongIteratorEx() {
            private boolean hasMore = true;
            private boolean hasNext = false;
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                if (hasNext == false && hasMore && cursor < toIndex) {
                    if (predicate.test(elements[cursor])) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public long nextLong() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public LongStream dropWhile(final LongPredicate predicate) {
        return newStream(new LongIteratorEx() {
            private boolean hasNext = false;
            private int cursor = fromIndex;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (hasNext == false && cursor < toIndex) {
                    if (dropped == false) {
                        dropped = true;

                        do {
                            if (predicate.test(elements[cursor]) == false) {
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
            public long nextLong() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return elements[cursor++];
            }
        }, sorted);
    }

    @Override
    public LongStream step(final long step) {
        checkArgPositive(step, "step");

        if (step == 1 || fromIndex == toIndex) {
            return newStream(elements, fromIndex, toIndex, sorted);
        }

        return newStream(new LongIteratorEx() {
            private final int stepp = (int) N.min(step, Integer.MAX_VALUE);
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final long res = elements[cursor];
                cursor = cursor > toIndex - stepp ? toIndex : cursor + stepp;
                return res;
            }

            @Override
            public long count() {
                return (toIndex - cursor) % stepp == 0 ? (toIndex - cursor) / stepp : ((toIndex - cursor) / stepp) + 1;
            }

            @Override
            public void skip(long n) {
                cursor = n <= (toIndex - cursor) / stepp ? cursor + (int) (n * stepp) : toIndex;
            }

            @Override
            public long[] toArray() {
                final long[] a = new long[(int) count()];

                for (int i = 0, len = a.length; i < len; i++, cursor += stepp) {
                    a[i] = elements[cursor];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public LongStream map(final LongUnaryOperator mapper) {
        return newStream(new LongIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsLong(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

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
    public IntStream mapToInt(final LongToIntFunction mapper) {
        return newStream(new IntIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public int nextInt() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsInt(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

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
    public FloatStream mapToFloat(final LongToFloatFunction mapper) {
        return newStream(new FloatIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsFloat(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

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
    public DoubleStream mapToDouble(final LongToDoubleFunction mapper) {
        return newStream(new DoubleIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.applyAsDouble(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

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
    public <U> Stream<U> mapToObj(final LongFunction<? extends U> mapper) {
        return newStream(new ObjIteratorEx<U>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public U next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return mapper.apply(elements[cursor++]);
            }

            //    @Override
            //    public long count() {
            //        return toIndex - cursor;
            //    }
            //
            //    @Override
            //    public void skip(long n) {
            //        checkArgNotNegative(n, "n");
            //
            //        cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            //    }

            @Override
            public <A> A[] toArray(A[] a) {
                a = a.length >= toIndex - cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = (A) mapper.apply(elements[cursor++]);
                }

                return a;
            }
        }, false, null);
    }

    @Override
    public LongStream flatMap(final LongFunction<? extends LongStream> mapper) {
        final LongIteratorEx iter = new LongIteratorEx() {
            private int cursor = fromIndex;
            private LongIterator cur = null;
            private LongStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

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
    public IntStream flatMapToInt(final LongFunction<? extends IntStream> mapper) {
        final IntIteratorEx iter = new IntIteratorEx() {
            private int cursor = fromIndex;
            private IntIterator cur = null;
            private IntStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

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
    public FloatStream flatMapToFloat(final LongFunction<? extends FloatStream> mapper) {
        final FloatIteratorEx iter = new FloatIteratorEx() {
            private int cursor = fromIndex;
            private FloatIterator cur = null;
            private FloatStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

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
    public DoubleStream flatMapToDouble(final LongFunction<? extends DoubleStream> mapper) {
        final DoubleIteratorEx iter = new DoubleIteratorEx() {
            private int cursor = fromIndex;
            private DoubleIterator cur = null;
            private DoubleStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

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
    public <T> Stream<T> flatMapToObj(final LongFunction<? extends Stream<T>> mapper) {
        final ObjIteratorEx<T> iter = new ObjIteratorEx<T>() {
            private int cursor = fromIndex;
            private Iterator<T> cur = null;
            private Stream<T> s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && cursor < toIndex) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements[cursor++]);

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
    public Stream<LongStream> split(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<LongStream>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public LongStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return new ArrayLongStream(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex), sorted, null);
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / chunkSize ? cursor + (int) n * chunkSize : toIndex;
            }
        }, false, null);
    }

    @Override
    public Stream<LongList> splitToList(final int chunkSize) {
        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ObjIteratorEx<LongList>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public LongList next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return new LongList(N.copyOfRange(elements, cursor, (cursor = chunkSize < toIndex - cursor ? cursor + chunkSize : toIndex)));
            }

            @Override
            public long count() {
                final long len = toIndex - cursor;
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void skip(long n) {
                final long len = toIndex - cursor;
                cursor = n <= len / chunkSize ? cursor + (int) n * chunkSize : toIndex;
            }
        }, false, null);
    }

    @Override
    public Stream<LongStream> split(final LongPredicate predicate) {
        return newStream(new ObjIteratorEx<LongStream>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public LongStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final int from = cursor;

                while (cursor < toIndex) {
                    if (from == cursor) {
                        preCondition = predicate.test(elements[cursor]);
                        cursor++;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        cursor++;
                    } else {
                        break;
                    }
                }

                return new ArrayLongStream(elements, from, cursor, sorted, null);
            }
        }, false, null);
    }

    @Override
    public Stream<LongList> splitToList(final LongPredicate predicate) {
        return newStream(new ObjIteratorEx<LongList>() {
            private int cursor = fromIndex;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public LongList next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final int from = cursor;

                while (cursor < toIndex) {
                    if (from == cursor) {
                        preCondition = predicate.test(elements[cursor]);
                        cursor++;
                    } else if (predicate.test(elements[cursor]) == preCondition) {
                        cursor++;
                    } else {

                        break;
                    }
                }

                return new LongList(N.copyOfRange(elements, from, cursor));
            }
        }, false, null);
    }

    @Override
    public Stream<LongStream> splitAt(final int where) {
        checkArgNotNegative(where, "where");

        final LongStream[] a = new LongStream[2];
        final int middleIndex = where < toIndex - fromIndex ? fromIndex + where : toIndex;
        a[0] = middleIndex == fromIndex ? LongStream.empty() : new ArrayLongStream(elements, fromIndex, middleIndex, sorted, null);
        a[1] = middleIndex == toIndex ? LongStream.empty() : new ArrayLongStream(elements, middleIndex, toIndex, sorted, null);

        return newStream(a, false, null);
    }

    @Override
    public Stream<LongStream> sliding(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<LongStream>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public LongStream next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final ArrayLongStream result = new ArrayLongStream(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex, sorted,
                        null);

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            @Override
            public long count() {
                if (toIndex - cursor == 0) {
                    return 0;
                } else if (toIndex - cursor <= windowSize) {
                    return 1;
                } else {
                    final long len = (toIndex - cursor) - windowSize;
                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n >= count()) {
                    cursor = toIndex;
                } else {
                    cursor += n * increment;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<LongList> slidingToList(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<LongList>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public LongList next() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                final LongList result = LongList.of(N.copyOfRange(elements, cursor, windowSize < toIndex - cursor ? cursor + windowSize : toIndex));

                cursor = increment < toIndex - cursor && windowSize < toIndex - cursor ? cursor + increment : toIndex;

                return result;
            }

            @Override
            public long count() {
                if (toIndex - cursor == 0) {
                    return 0;
                } else if (toIndex - cursor <= windowSize) {
                    return 1;
                } else {
                    final long len = (toIndex - cursor) - windowSize;
                    return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
                }
            }

            @Override
            public void skip(long n) {
                if (n >= count()) {
                    cursor = toIndex;
                } else {
                    cursor += n * increment;
                }
            }
        }, false, null);
    }

    @Override
    public LongStream top(final int n, final Comparator<? super Long> comparator) {
        checkArgPositive(n, "n");

        if (n >= toIndex - fromIndex) {
            return newStream(elements, fromIndex, toIndex, sorted);
        } else if (sorted && isSameComparator(comparator, cmp)) {
            return newStream(elements, toIndex - n, toIndex, sorted);
        }

        return newStream(new LongIteratorEx() {
            private boolean initialized = false;
            private long[] aar;
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
            public long nextLong() {
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
            public long[] toArray() {
                if (initialized == false) {
                    init();
                }

                final long[] a = new long[to - cursor];

                N.copy(aar, cursor, a, 0, to - cursor);

                return a;
            }

            @Override
            public LongList toList() {
                return LongList.of(toArray());
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = N.top(elements, fromIndex, toIndex, n, comparator);
                    to = aar.length;
                }
            }
        }, false);
    }

    @Override
    public LongStream peek(final LongConsumer action) {
        return newStream(new LongIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public long nextLong() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                action.accept(elements[cursor]);

                return elements[cursor++];
            }

            @Override
            public long[] toArray() {
                final long[] a = new long[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    action.accept(elements[cursor]);

                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public LongStream limit(final long maxSize) {
        checkArgNotNegative(maxSize, "maxSize");

        return newStream(elements, fromIndex, (int) (fromIndex + maxSize), sorted);
    }

    @Override
    public LongStream skip(long n) {
        checkArgNotNegative(n, "n");

        if (n >= toIndex - fromIndex) {
            return newStream(elements, toIndex, toIndex, sorted);
        } else {
            return newStream(elements, (int) (fromIndex + n), toIndex, sorted);
        }
    }

    @Override
    public <E extends Exception> void forEach(final Try.LongConsumer<E> action) throws E {
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
    public long[] toArray() {
        assertNotClosed();

        try {
            return N.copyOfRange(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    @Override
    public LongList toLongList() {
        assertNotClosed();

        try {
            return LongList.of(N.copyOfRange(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public List<Long> toList() {
        assertNotClosed();

        try {
            final List<Long> result = new ArrayList<>(toIndex - fromIndex);

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Set<Long> toSet() {
        assertNotClosed();

        try {
            final Set<Long> result = new HashSet<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <C extends Collection<Long>> C toCollection(Supplier<? extends C> supplier) {
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
    public Multiset<Long> toMultiset() {
        assertNotClosed();

        try {
            final Multiset<Long> result = new Multiset<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<Long> toMultiset(Supplier<? extends Multiset<Long>> supplier) {
        assertNotClosed();

        try {
            final Multiset<Long> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<Long> toLongMultiset() {
        assertNotClosed();

        try {
            final LongMultiset<Long> result = new LongMultiset<>(N.initHashCapacity(toIndex - fromIndex));

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<Long> toLongMultiset(Supplier<? extends LongMultiset<Long>> supplier) {
        assertNotClosed();

        try {
            final LongMultiset<Long> result = supplier.get();

            for (int i = fromIndex; i < toIndex; i++) {
                result.add(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction,
            Supplier<? extends M> mapFactory) {
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
    public <K, A, D, M extends Map<K, D>> M toMap(final LongFunction<? extends K> keyMapper, final Collector<Long, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, Long> downstreamAccumulator = downstream.accumulator();
            final Map<K, A> intermediate = (Map<K, A>) result;
            K key = null;
            A v = null;

            for (int i = fromIndex; i < toIndex; i++) {
                key = checkArgNotNull(keyMapper.apply(elements[i]), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    if ((v = downstreamSupplier.get()) != null) {
                        intermediate.put(key, v);
                    }
                }

                downstreamAccumulator.accept(v, elements[i]);
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
    public OptionalLong first() {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalLong.of(elements[fromIndex]) : OptionalLong.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong last() {
        assertNotClosed();

        try {
            return fromIndex < toIndex ? OptionalLong.of(elements[toIndex - 1]) : OptionalLong.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final int size = toIndex - fromIndex;

            if (size == 0) {
                return OptionalLong.empty();
            } else if (size == 1) {
                return OptionalLong.of(elements[fromIndex]);
            } else {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(elements[fromIndex], ", ", elements[fromIndex + 1]));
            }
        } finally {
            close();
        }
    }

    @Override
    public long reduce(long identity, LongBinaryOperator op) {
        assertNotClosed();

        try {
            long result = identity;

            for (int i = fromIndex; i < toIndex; i++) {
                result = op.applyAsLong(result, elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong reduce(LongBinaryOperator op) {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalLong.empty();
            }

            long result = elements[fromIndex];

            for (int i = fromIndex + 1; i < toIndex; i++) {
                result = op.applyAsLong(result, elements[i]);
            }

            return OptionalLong.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<? super R> accumulator, BiConsumer<R, R> combiner) {
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
    public OptionalLong min() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalLong.empty();
            } else if (sorted) {
                return OptionalLong.of(elements[fromIndex]);
            }

            return OptionalLong.of(N.min(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong max() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalLong.empty();
            } else if (sorted) {
                return OptionalLong.of(elements[toIndex - 1]);
            }

            return OptionalLong.of(N.max(elements, fromIndex, toIndex));
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong kthLargest(int k) {
        checkArgPositive(k, "k");
        assertNotClosed();

        try {
            if (k > toIndex - fromIndex) {
                return OptionalLong.empty();
            } else if (sorted) {
                return OptionalLong.of(elements[toIndex - k]);
            }

            return OptionalLong.of(N.kthLargest(elements, fromIndex, toIndex, k));
        } finally {
            close();
        }
    }

    @Override
    public long sum() {
        assertNotClosed();

        try {
            return sum(elements, fromIndex, toIndex);
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble average() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            }

            return OptionalDouble.of(sum() / toIndex - fromIndex);
        } finally {
            close();
        }
    }

    @Override
    public long count() {
        assertNotClosed();

        try {
            return toIndex - fromIndex;
        } finally {
            close();
        }
    }

    @Override
    public LongStream reversed() {
        return newStream(new LongIteratorEx() {
            private int cursor = toIndex;

            @Override
            public boolean hasNext() {
                return cursor > fromIndex;
            }

            @Override
            public long nextLong() {
                if (cursor <= fromIndex) {
                    throw new NoSuchElementException();
                }
                return elements[--cursor];
            }

            @Override
            public long count() {
                return cursor - fromIndex;
            }

            @Override
            public void skip(long n) {
                cursor = n < cursor - fromIndex ? cursor - (int) n : fromIndex;
            }

            @Override
            public long[] toArray() {
                final long[] a = new long[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }
        }, false);
    }

    @Override
    public LongStream rotated(final int distance) {
        if (distance == 0 || toIndex - fromIndex <= 1 || distance % (toIndex - fromIndex) == 0) {
            return newStream(elements, fromIndex, toIndex, sorted);
        }

        return newStream(new LongIteratorEx() {
            private final int len = toIndex - fromIndex;
            private int start;
            private int cnt = 0;

            {

                start = distance % len;

                if (start < 0) {
                    start += len;
                }

                start = len - start;
            }

            @Override
            public boolean hasNext() {
                return cnt < len;
            }

            @Override
            public long nextLong() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return elements[((start + cnt++) % len) + fromIndex];
            }

            @Override
            public long count() {
                return len - cnt;
            }

            @Override
            public void skip(long n) {
                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            @Override
            public long[] toArray() {
                final long[] a = new long[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }
        }, false);
    }

    @Override
    public LongSummaryStatistics summarize() {
        assertNotClosed();

        try {
            final LongSummaryStatistics result = new LongSummaryStatistics();

            for (int i = fromIndex; i < toIndex; i++) {
                result.accept(elements[i]);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.LongPredicate<E> predicate) throws E {
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
    public <E extends Exception> boolean allMatch(final Try.LongPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i]) == false) {
                    return false;
                }
            }

            return true;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.LongPredicate<E> predicate) throws E {
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
    public <E extends Exception> OptionalLong findFirst(final Try.LongPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = fromIndex; i < toIndex; i++) {
                if (predicate.test(elements[i])) {
                    return OptionalLong.of(elements[i]);
                }
            }

            return OptionalLong.empty();
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalLong findLast(final Try.LongPredicate<E> predicate) throws E {
        assertNotClosed();

        try {
            for (int i = toIndex - 1; i >= fromIndex; i--) {
                if (predicate.test(elements[i])) {
                    return OptionalLong.of(elements[i]);
                }
            }

            return OptionalLong.empty();
        } finally {
            close();
        }
    }

    @Override
    public FloatStream asFloatStream() {
        return newStream(new FloatIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public float nextFloat() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return elements[cursor++];
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public void skip(long n) {
                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public float[] toArray() {
                final float[] a = new float[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public DoubleStream asDoubleStream() {
        return newStream(new DoubleIteratorEx() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public double nextDouble() {
                if (cursor >= toIndex) {
                    throw new NoSuchElementException();
                }

                return elements[cursor++];
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public void skip(long n) {
                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public double[] toArray() {
                final double[] a = new double[toIndex - cursor];

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a[i] = elements[cursor++];
                }

                return a;
            }
        }, sorted);
    }

    @Override
    public java.util.stream.LongStream toJdkStream() {
        java.util.stream.LongStream s = java.util.stream.LongStream.of(elements);

        if (fromIndex > 0) {
            s = s.skip(fromIndex);
        }

        if (toIndex < elements.length) {
            s = s.limit(toIndex - fromIndex);
        }

        if (this.isParallel()) {
            s = s.parallel();
        }

        if (N.notNullOrEmpty(closeHandlers)) {
            s = s.onClose(() -> close(closeHandlers));
        }

        return s;
    }

    @Override
    public Stream<Long> boxed() {
        return new IteratorStream<>(iteratorEx(), sorted, sorted ? LONG_COMPARATOR : null, closeHandlers);
    }

    //    @Override
    //    public LongStream cached() {
    //        return this;
    //    }

    @Override
    LongIteratorEx iteratorEx() {
        return LongIteratorEx.of(elements, fromIndex, toIndex);
    }

    @Override
    public LongStream appendIfEmpty(final Supplier<LongStream> supplier) {
        if (fromIndex == toIndex) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

    @Override
    public LongStream parallel(final int maxThreadNum, final Splitor splitor) {
        return new ParallelArrayLongStream(elements, fromIndex, toIndex, sorted, checkMaxThreadNum(maxThreadNum), checkSplitor(splitor), asyncExecutor(),
                closeHandlers);
    }

    @Override
    public LongStream parallel(final int maxThreadNum, final Executor executor) {
        return new ParallelArrayLongStream(elements, fromIndex, toIndex, sorted, checkMaxThreadNum(maxThreadNum), splitor(), createAsyncExecutor(executor),
                closeHandlers);
    }

    @Override
    public LongStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new ArrayLongStream(elements, fromIndex, toIndex, sorted, newCloseHandlers);
    }
}
