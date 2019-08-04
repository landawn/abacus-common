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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Executor;
import java.util.stream.StreamSupport;

import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.ObjIteratorEx.QueuedIterator;

/**
 * 
 */
class IteratorStream<T> extends AbstractStream<T> {
    final ObjIteratorEx<T> elements;

    //    Optional<T> head;
    //    Stream<T> tail;

    //    Stream<T> head2;
    //    Optional<T> tail2;

    IteratorStream(final Iterator<? extends T> values) {
        this(values, null);
    }

    IteratorStream(final Iterator<? extends T> values, final Collection<Runnable> closeHandlers) {
        this(values, false, null, closeHandlers);
    }

    IteratorStream(final Iterator<? extends T> values, final boolean sorted, final Comparator<? super T> comparator, final Collection<Runnable> closeHandlers) {
        super(sorted, comparator, closeHandlers);

        checkArgNotNull(values);

        ObjIteratorEx<T> tmp = null;

        if (values instanceof ObjIteratorEx) {
            tmp = (ObjIteratorEx<T>) values;
        } else {
            tmp = new ObjIteratorEx<T>() {
                @Override
                public boolean hasNext() {
                    return values.hasNext();
                }

                @Override
                public T next() {
                    return values.next();
                }
            };
        }

        this.elements = tmp;
    }

    IteratorStream(final Stream<T> stream, final boolean sorted, final Comparator<? super T> comparator, final Deque<Runnable> closeHandlers) {
        this(stream.iteratorEx(), sorted, comparator, mergeCloseHandlers(stream, closeHandlers));
    }

    @Override
    public Stream<T> filter(final Predicate<? super T> predicate) {
        return newStream(new ObjIteratorEx<T>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
                    while (elements.hasNext()) {
                        next = elements.next();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> takeWhile(final Predicate<? super T> predicate) {
        return newStream(new ObjIteratorEx<T>() {
            private boolean hasMore = true;
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                if (hasNext == false && hasMore && elements.hasNext()) {
                    next = elements.next();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }

        }, sorted, cmp);
    }

    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate) {
        return newStream(new ObjIteratorEx<T>() {
            private boolean hasNext = false;
            private T next = null;
            private boolean dropped = false;

            @Override
            public boolean hasNext() {
                if (hasNext == false) {
                    if (dropped == false) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.next();

                            if (predicate.test(next) == false) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.next();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() {
                if (hasNext == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return next;
            }

        }, sorted, cmp);
    }

    @Override
    public <R> Stream<R> map(final Function<? super T, ? extends R> mapper) {
        return newStream(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public R next() {
                return mapper.apply(elements.next());
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

    //    @Override
    //    public <R> Stream<R> biMap(final BiFunction<? super T, ? super T, ? extends R> mapper, final boolean ignoreNotPaired) {
    //        return newStream(new ObjIteratorEx<R>() {
    //            private T pre = (T) NONE;
    //
    //            @Override
    //            public boolean hasNext() {
    //                if (ignoreNotPaired && pre == NONE) {
    //                    if (elements.hasNext()) {
    //                        pre = elements.next();
    //                    } else {
    //                        return false;
    //                    }
    //                }
    //
    //                return elements.hasNext();
    //            }
    //
    //            @Override
    //            public R next() {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                if (ignoreNotPaired) {
    //                    final R res = mapper.apply(pre, elements.next());
    //                    pre = (T) NONE;
    //                    return res;
    //                } else {
    //                    return mapper.apply(elements.next(), elements.hasNext() ? elements.next() : null);
    //                }
    //            }
    //
    //            //            @Override
    //            //            public void skip(long n) {
    //            //                checkArgNotNegative(n, "n");
    //            //
    //            //                elements.skip(n > Long.MAX_VALUE / 2 ? Long.MAX_VALUE : n * 2);
    //            //            }
    //            //
    //            //            @Override
    //            //            public long count() {
    //            //                final long count = elements.count();
    //            //                return count % 2 == 0 || ignoreNotPaired ? count / 2 : count / 2 + 1;
    //            //            }
    //        }, closeHandlers);
    //    }
    //
    //    @Override
    //    public <R> Stream<R> triMap(final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper, final boolean ignoreNotPaired) {
    //        return newStream(new ObjIteratorEx<R>() {
    //            private T prepre = (T) NONE;
    //            private T pre = (T) NONE;
    //
    //            @Override
    //            public boolean hasNext() {
    //                if (ignoreNotPaired && pre == NONE) {
    //                    if (elements.hasNext()) {
    //                        prepre = elements.next();
    //
    //                        if (elements.hasNext()) {
    //                            pre = elements.next();
    //                        } else {
    //                            return false;
    //                        }
    //                    } else {
    //                        return false;
    //                    }
    //                }
    //
    //                return elements.hasNext();
    //            }
    //
    //            @Override
    //            public R next() {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                if (ignoreNotPaired) {
    //                    final R res = mapper.apply(prepre, pre, elements.next());
    //                    prepre = (T) NONE;
    //                    pre = (T) NONE;
    //                    return res;
    //                } else {
    //                    return mapper.apply(elements.next(), elements.hasNext() ? elements.next() : null, elements.hasNext() ? elements.next() : null);
    //                }
    //            }
    //
    //            //            @Override
    //            //            public void skip(long n) {
    //            //                checkArgNotNegative(n, "n");
    //            //
    //            //                elements.skip(n > Long.MAX_VALUE / 3 ? Long.MAX_VALUE : n * 3);
    //            //            }
    //            //
    //            //            @Override
    //            //            public long count() {
    //            //                final long count = elements.count();
    //            //                return count % 3 == 0 || ignoreNotPaired ? count / 3 : count / 3 + 1;
    //            //            }
    //        }, closeHandlers);
    //    }

    @Override
    public <R> Stream<R> slidingMap(final BiFunction<? super T, ? super T, R> mapper, final int increment, final boolean ignoreNotPaired) {
        final int windowSize = 2;

        checkArgPositive(increment, "increment");

        return newStream(new ObjIteratorEx<R>() {
            @SuppressWarnings("unchecked")
            private final T NONE = (T) StreamBase.NONE;
            private T prev = NONE;
            private T _1 = NONE;

            @Override
            public boolean hasNext() {
                if (increment > windowSize && prev != NONE) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    prev = NONE;
                }

                if (ignoreNotPaired && _1 == NONE && elements.hasNext()) {
                    _1 = elements.next();
                }

                return elements.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (ignoreNotPaired) {
                    final R res = mapper.apply(_1, (prev = elements.next()));
                    _1 = increment == 1 ? prev : NONE;
                    return res;
                } else {
                    if (increment == 1) {
                        return mapper.apply(prev == NONE ? elements.next() : prev, (prev = (elements.hasNext() ? elements.next() : null)));
                    } else {
                        return mapper.apply(elements.next(), (prev = (elements.hasNext() ? elements.next() : null)));
                    }
                }
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> slidingMap(final TriFunction<? super T, ? super T, ? super T, R> mapper, final int increment, final boolean ignoreNotPaired) {
        final int windowSize = 3;

        checkArgPositive(increment, "increment");

        return newStream(new ObjIteratorEx<R>() {
            @SuppressWarnings("unchecked")
            private final T NONE = (T) StreamBase.NONE;
            private T prev = NONE;
            private T prev2 = NONE;
            private T _1 = NONE;
            private T _2 = NONE;

            @Override
            public boolean hasNext() {
                if (increment > windowSize && prev != NONE) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    prev = NONE;
                }

                if (ignoreNotPaired) {
                    if (_1 == NONE && elements.hasNext()) {
                        _1 = elements.next();
                    }

                    if (_2 == NONE && elements.hasNext()) {
                        _2 = elements.next();
                    }
                }

                return elements.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (ignoreNotPaired) {
                    final R res = mapper.apply(_1, _2, (prev = elements.next()));
                    _1 = increment == 1 ? _2 : (increment == 2 ? prev : NONE);
                    _2 = increment == 1 ? prev : NONE;
                    return res;
                } else {
                    if (increment == 1) {
                        return mapper.apply(prev2 == NONE ? elements.next() : prev2,
                                (prev2 = (prev == NONE ? (elements.hasNext() ? elements.next() : null) : prev)),
                                (prev = (elements.hasNext() ? elements.next() : null)));

                    } else if (increment == 2) {
                        return mapper.apply(prev == NONE ? elements.next() : prev, elements.hasNext() ? elements.next() : null,
                                (prev = (elements.hasNext() ? elements.next() : null)));
                    } else {
                        return mapper.apply(elements.next(), elements.hasNext() ? elements.next() : null,
                                (prev = (elements.hasNext() ? elements.next() : null)));
                    }
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> mapFirst(final Function<? super T, ? extends T> mapperForFirst) {
        checkArgNotNull(mapperForFirst);

        return newStream(new ObjIteratorEx<T>() {
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() {
                if (isFirst) {
                    isFirst = false;
                    return mapperForFirst.apply(elements.next());
                } else {
                    return elements.next();
                }
            }

            //            @Override
            //            public void skip(long n) {
            //                checkArgNotNegative(n, "n");
            //
            //                if (n > 0) {
            //                    isFirst = false;
            //                }
            //
            //                elements.skip(n);
            //            }
            //
            //            @Override
            //            public long count() {
            //                isFirst = false;
            //
            //                return elements.count();
            //            }

            @Override
            public void skip(long n) {
                if (n > 0) {
                    if (hasNext()) {
                        next();
                    }

                    elements.skip(n - 1);
                }
            }

            @Override
            public long count() {
                if (hasNext()) {
                    next();
                    return elements.count() + 1;
                }

                return 0;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> mapFirstOrElse(final Function<? super T, ? extends R> mapperForFirst, final Function<? super T, ? extends R> mapperForElse) {
        checkArgNotNull(mapperForFirst);
        checkArgNotNull(mapperForElse);

        return newStream(new ObjIteratorEx<R>() {
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public R next() {
                if (isFirst) {
                    isFirst = false;
                    return mapperForFirst.apply(elements.next());
                } else {
                    return mapperForElse.apply(elements.next());
                }
            }

            //            @Override
            //            public long count() {
            //                isFirst = false;
            //
            //                return elements.count();
            //            }
            //
            //            @Override
            //            public void skip(long n) {
            //                checkArgNotNegative(n, "n");
            //
            //                if (n > 0) {
            //                    isFirst = false;
            //                }
            //
            //                elements.skip(n);
            //            }
        }, false, null);
    }

    @Override
    public Stream<T> mapLast(final Function<? super T, ? extends T> mapperForLast) {
        checkArgNotNull(mapperForLast);

        return newStream(new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() {
                final T next = elements.next();

                if (elements.hasNext()) {
                    return next;
                } else {
                    return mapperForLast.apply(next);
                }
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
    public <R> Stream<R> mapLastOrElse(final Function<? super T, ? extends R> mapperForLast, final Function<? super T, ? extends R> mapperForElse) {
        checkArgNotNull(mapperForLast);
        checkArgNotNull(mapperForElse);

        return newStream(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public R next() {
                final T next = elements.next();

                if (elements.hasNext()) {
                    return mapperForElse.apply(next);
                } else {
                    return mapperForLast.apply(next);
                }
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
    public CharStream mapToChar(final ToCharFunction<? super T> mapper) {
        return newStream(new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public char nextChar() {
                return mapper.applyAsChar(elements.next());
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
    public ByteStream mapToByte(final ToByteFunction<? super T> mapper) {
        return newStream(new ByteIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public byte nextByte() {
                return mapper.applyAsByte(elements.next());
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
    public ShortStream mapToShort(final ToShortFunction<? super T> mapper) {
        return newStream(new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public short nextShort() {
                return mapper.applyAsShort(elements.next());
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
    public IntStream mapToInt(final ToIntFunction<? super T> mapper) {
        return newStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public int nextInt() {
                return mapper.applyAsInt(elements.next());
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
    public LongStream mapToLong(final ToLongFunction<? super T> mapper) {
        return newStream(new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public long nextLong() {
                return mapper.applyAsLong(elements.next());
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
    public FloatStream mapToFloat(final ToFloatFunction<? super T> mapper) {
        return newStream(new FloatIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public float nextFloat() {
                return mapper.applyAsFloat(elements.next());
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
    public DoubleStream mapToDouble(final ToDoubleFunction<? super T> mapper) {
        return newStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public double nextDouble() {
                return mapper.applyAsDouble(elements.next());
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
    public <R> Stream<R> flatMap(final Function<? super T, ? extends Stream<? extends R>> mapper) {
        final ObjIteratorEx<R> iter = new ObjIteratorEx<R>() {
            private Iterator<? extends R> cur = null;
            private Stream<? extends R> s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.next());

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
            public R next() {
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
    public CharStream flatMapToChar(final Function<? super T, ? extends CharStream> mapper) {
        final CharIteratorEx iter = new CharIteratorEx() {
            private CharIterator cur = null;
            private CharStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.next());

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
            public char nextChar() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextChar();
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

        return new IteratorCharStream(iter, newCloseHandlers);
    }

    @Override
    public ByteStream flatMapToByte(final Function<? super T, ? extends ByteStream> mapper) {
        final ByteIteratorEx iter = new ByteIteratorEx() {
            private ByteIterator cur = null;
            private ByteStream s = null;
            private Runnable closeHandle = null;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Runnable tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.next());

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
            public byte nextByte() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.nextByte();
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

        return new IteratorByteStream(iter, newCloseHandlers);
    }

    @Override
    public ShortStream flatMapToShort(final Function<? super T, ? extends ShortStream> mapper) {
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

                    s = mapper.apply(elements.next());

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
    public IntStream flatMapToInt(final Function<? super T, ? extends IntStream> mapper) {
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

                    s = mapper.apply(elements.next());

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
    public LongStream flatMapToLong(final Function<? super T, ? extends LongStream> mapper) {
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

                    s = mapper.apply(elements.next());

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
    public FloatStream flatMapToFloat(final Function<? super T, ? extends FloatStream> mapper) {
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

                    s = mapper.apply(elements.next());

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
    public DoubleStream flatMapToDouble(final Function<? super T, ? extends DoubleStream> mapper) {
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

                    s = mapper.apply(elements.next());

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
    public <C extends Collection<T>> Stream<C> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier) {
        checkArgPositive(chunkSize, "chunkSize");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new ObjIteratorEx<C>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public C next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final C result = collectionSupplier.apply(chunkSize);
                int cnt = 0;

                while (cnt < chunkSize && elements.hasNext()) {
                    result.add(elements.next());
                    cnt++;
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
    public <A, R> Stream<R> split(final int chunkSize, final Collector<? super T, A, R> collector) {
        checkArgPositive(chunkSize, "chunkSize");
        checkArgNotNull(collector);

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();
                int cnt = 0;

                while (cnt < chunkSize && elements.hasNext()) {
                    accumulator.accept(container, elements.next());
                    cnt++;
                }

                return finisher.apply(container);
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
    public <C extends Collection<T>> Stream<C> split(final Predicate<? super T> predicate, final Supplier<? extends C> collectionSupplier) {
        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new ObjIteratorEx<C>() {
            private T next = (T) NONE;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return next != NONE || elements.hasNext();
            }

            @Override
            public C next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final C result = collectionSupplier.get();
                boolean isFirst = true;

                if (next == NONE) {
                    next = elements.next();
                }

                while (next != NONE) {
                    if (isFirst) {
                        result.add(next);
                        preCondition = predicate.test(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                        isFirst = false;
                    } else if (predicate.test(next) == preCondition) {
                        result.add(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                    } else {

                        break;
                    }
                }

                return result;
            }

        }, false, null);
    }

    @Override
    public <A, R> Stream<R> split(final Predicate<? super T> predicate, final Collector<? super T, A, R> collector) {
        checkArgNotNull(predicate);
        checkArgNotNull(collector);

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ObjIteratorEx<R>() {
            private T next = (T) NONE;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() {
                return next != NONE || elements.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();
                boolean isFirst = true;

                if (next == NONE) {
                    next = elements.next();
                }

                while (next != NONE) {
                    if (isFirst) {
                        accumulator.accept(container, next);
                        preCondition = predicate.test(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                        isFirst = false;
                    } else if (predicate.test(next) == preCondition) {
                        accumulator.accept(container, next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                    } else {

                        break;
                    }
                }

                return finisher.apply(container);
            }

        }, false, null);
    }

    @Override
    public Stream<Stream<T>> splitAt(final int where) {
        checkArgNotNegative(where, "where");

        return newStream(new ObjIteratorEx<Stream<T>>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public Stream<T> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                Stream<T> result = null;

                if (cursor == 0) {
                    final List<T> list = new ArrayList<>();
                    int cnt = 0;

                    while (cnt++ < where && elements.hasNext()) {
                        list.add(elements.next());
                    }

                    result = new ArrayStream<T>(Stream.toArray(list), 0, list.size(), sorted, cmp, null);
                } else {
                    result = new IteratorStream<T>(elements, sorted, cmp, null);
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
    public <A, R> Stream<R> splitAt(final int where, final Collector<? super T, A, R> collector) {
        checkArgNotNegative(where, "where");
        checkArgNotNull(collector, "collector");

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ObjIteratorEx<R>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public R next() {
                if (hasNext()) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();

                if (cursor == 0) {
                    int cnt = 0;

                    while (cnt++ < where && elements.hasNext()) {
                        accumulator.accept(container, elements.next());
                    }
                } else {
                    while (elements.hasNext()) {
                        accumulator.accept(container, elements.next());
                    }
                }

                cursor++;

                return finisher.apply(container);
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
    public Stream<List<T>> slidingToList(final int windowSize, final int increment) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ObjIteratorEx<List<T>>() {
            private List<T> prev = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                return elements.hasNext();
            }

            @Override
            public List<T> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final List<T> result = new ArrayList<>(windowSize);
                int cnt = 0;

                if (prev != null && increment < windowSize) {
                    cnt = windowSize - increment;

                    if (cnt <= 8) {
                        for (int i = windowSize - cnt; i < windowSize; i++) {
                            result.add(prev.get(i));
                        }
                    } else {
                        result.addAll(prev.subList(windowSize - cnt, windowSize));
                    }
                }

                while (cnt++ < windowSize && elements.hasNext()) {
                    result.add(elements.next());
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
                    final List<T> tmp = new ArrayList<>(windowSize);

                    if (N.isNullOrEmpty(prev)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.skip(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (prev == null ? 0 : prev.size());

                        if (m < prevSize) {
                            tmp.addAll(prev.subList((int) m, prevSize));
                        } else {
                            elements.skip(m - prevSize);
                        }
                    }

                    int cnt = tmp.size();

                    while (cnt++ < windowSize && elements.hasNext()) {
                        tmp.add(elements.next());
                    }

                    prev = tmp;
                }
            }
        }, false, null);
    }

    @Override
    public <C extends Collection<T>> Stream<C> sliding(final int windowSize, final int increment, final IntFunction<? extends C> collectionSupplier) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new ObjIteratorEx<C>() {
            private Deque<T> queue = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                return elements.hasNext();
            }

            @Override
            public C next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(N.max(0, windowSize - increment));
                }

                final C result = collectionSupplier.apply(windowSize);
                int cnt = 0;

                if (queue.size() > 0 && increment < windowSize) {
                    cnt = queue.size();

                    for (T e : queue) {
                        result.add(e);
                    }

                    if (queue.size() <= increment) {
                        queue.clear();
                    } else {
                        for (int i = 0; i < increment; i++) {
                            queue.removeFirst();
                        }
                    }
                }

                T next = null;

                while (cnt++ < windowSize && elements.hasNext()) {
                    next = elements.next();
                    result.add(next);

                    if (cnt > increment) {
                        queue.add(next);
                    }
                }

                toSkip = increment > windowSize;

                return result;
            }

            @Override
            public long count() {
                final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());
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
                    if (N.isNullOrEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.skip(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());

                        if (m < prevSize) {
                            for (int i = 0; i < m; i++) {
                                queue.removeFirst();
                            }
                        } else {
                            if (queue != null) {
                                queue.clear();
                            }

                            elements.skip(m - prevSize);
                        }
                    }

                    if (queue == null) {
                        queue = new ArrayDeque<>(windowSize);
                    }

                    int cnt = queue.size();

                    while (cnt++ < windowSize && elements.hasNext()) {
                        queue.add(elements.next());
                    }
                }
            }
        }, false, null);
    }

    @Override
    public <A, R> Stream<R> sliding(final int windowSize, final int increment, final Collector<? super T, A, R> collector) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collector);

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ObjIteratorEx<R>() {
            private Deque<T> queue = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                return elements.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(N.max(0, windowSize - increment));
                }

                final A container = supplier.get();
                int cnt = 0;

                if (queue.size() > 0 && increment < windowSize) {
                    cnt = queue.size();

                    for (T e : queue) {
                        accumulator.accept(container, e);
                    }

                    if (queue.size() <= increment) {
                        queue.clear();
                    } else {
                        for (int i = 0; i < increment; i++) {
                            queue.removeFirst();
                        }
                    }
                }

                T next = null;

                while (cnt++ < windowSize && elements.hasNext()) {
                    next = elements.next();
                    accumulator.accept(container, next);

                    if (cnt > increment) {
                        queue.add(next);
                    }
                }

                toSkip = increment > windowSize;

                return finisher.apply(container);
            }

            @Override
            public long count() {
                final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());
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
                    if (N.isNullOrEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.skip(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());

                        if (m < prevSize) {
                            for (int i = 0; i < m; i++) {
                                queue.removeFirst();
                            }
                        } else {
                            if (queue != null) {
                                queue.clear();
                            }

                            elements.skip(m - prevSize);
                        }
                    }

                    if (queue == null) {
                        queue = new ArrayDeque<>(windowSize);
                    }

                    int cnt = queue.size();

                    while (cnt++ < windowSize && elements.hasNext()) {
                        queue.add(elements.next());
                    }
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> top(final int n, final Comparator<? super T> comparator) {
        checkArgPositive(n, "n");

        return newStream(new ObjIteratorEx<T>() {
            private boolean initialized = false;
            private T[] aar = null;
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
            public T next() {
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

                cursor = n < to - cursor ? cursor + (int) n : to;
            }

            @Override
            public <A> A[] toArray(A[] b) {
                if (initialized == false) {
                    init();
                }

                b = b.length >= to - cursor ? b : (A[]) N.newArray(b.getClass().getComponentType(), to - cursor);

                N.copy(aar, cursor, b, 0, to - cursor);

                return b;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    if (sorted && isSameComparator(comparator, cmp)) {
                        final LinkedList<T> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.next());
                        }

                        aar = (T[]) queue.toArray();
                    } else {
                        final Queue<T> heap = new PriorityQueue<>(n, comparator);

                        T next = null;
                        while (elements.hasNext()) {
                            next = elements.next();

                            if (heap.size() >= n) {
                                if (comparator.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = (T[]) heap.toArray();
                    }

                    to = aar.length;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> peek(final Consumer<? super T> action) {
        return newStream(new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                return elements.hasNext();
            }

            @Override
            public T next() {
                final T next = elements.next();
                action.accept(next);
                return next;
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> limit(final long maxSize) {
        checkArgNotNegative(maxSize, "maxSize");

        return newStream(new ObjIteratorEx<T>() {
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public T next() {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException();
                }

                cnt++;
                return elements.next();
            }

            @Override
            public void skip(long n) {
                elements.skip(n);
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> skip(final long n) {
        checkArgNotNegative(n, "n");

        return newStream(new ObjIteratorEx<T>() {
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
            public T next() {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.next();
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
            public <A> A[] toArray(A[] a) {
                if (skipped == false) {
                    skipped = true;
                    elements.skip(n);
                }

                return elements.toArray(a);
            }
        }, sorted, cmp);
    }

    @Override
    public <E extends Exception, E2 extends Exception> void forEach(final Try.Consumer<? super T, E> action, final Try.Runnable<E2> onComplete) throws E, E2 {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                action.accept(elements.next());
            }

            onComplete.run();
        } finally {
            close();
        }
    }

    @Override
    public <U, E extends Exception, E2 extends Exception> void forEach(final Try.Function<? super T, ? extends Collection<U>, E> flatMapper,
            final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        assertNotClosed();

        Collection<U> c = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c = flatMapper.apply(next);

                if (N.notNullOrEmpty(c)) {
                    for (U u : c) {
                        action.accept(next, u);
                    }
                }
            }
        } finally {
            close();
        }
    }

    @Override
    public <T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3 {
        assertNotClosed();

        Collection<T2> c2 = null;
        Collection<T3> c3 = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c2 = flatMapper.apply(next);

                if (N.notNullOrEmpty(c2)) {
                    for (T2 t2 : c2) {
                        c3 = flatMapper2.apply(t2);

                        if (N.notNullOrEmpty(c3)) {
                            for (T3 t3 : c3) {
                                action.accept(next, t2, t3);
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
    public <E extends Exception> void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action, final int increment) throws E {
        final int windowSize = 2;
        checkArgPositive(increment, "increment");
        assertNotClosed();

        try {
            boolean isFirst = true;
            T prev = null;

            while (elements.hasNext()) {
                if (increment > windowSize && isFirst == false) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    if (elements.hasNext() == false) {
                        break;
                    }
                }

                if (increment == 1) {
                    action.accept(isFirst ? elements.next() : prev, (prev = (elements.hasNext() ? elements.next() : null)));
                } else {
                    action.accept(elements.next(), elements.hasNext() ? elements.next() : null);
                }

                isFirst = false;
            }
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action, final int increment) throws E {
        final int windowSize = 3;
        checkArgPositive(increment, "increment");
        assertNotClosed();

        try {
            boolean isFirst = true;
            T prev = null;
            T prev2 = null;

            while (elements.hasNext()) {
                if (increment > windowSize && isFirst == false) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    if (elements.hasNext() == false) {
                        break;
                    }
                }

                if (increment == 1) {
                    action.accept(isFirst ? elements.next() : prev2, (prev2 = (isFirst ? (elements.hasNext() ? elements.next() : null) : prev)),
                            (prev = (elements.hasNext() ? elements.next() : null)));

                } else if (increment == 2) {
                    action.accept(isFirst ? elements.next() : prev, elements.hasNext() ? elements.next() : null,
                            (prev = (elements.hasNext() ? elements.next() : null)));
                } else {
                    action.accept(elements.next(), elements.hasNext() ? elements.next() : null, elements.hasNext() ? elements.next() : null);
                }

                isFirst = false;
            }
        } finally {
            close();
        }
    }

    @Override
    public Object[] toArray() {
        return toArray(N.EMPTY_OBJECT_ARRAY);
    }

    <A> A[] toArray(A[] a) {
        assertNotClosed();

        try {
            return elements.toArray(a);
        } finally {
            close();
        }
    }

    @Override
    public List<T> toList() {
        assertNotClosed();

        try {
            final List<T> result = new ArrayList<>();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Set<T> toSet() {
        assertNotClosed();

        try {
            final Set<T> result = new HashSet<>();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <C extends Collection<T>> C toCollection(Supplier<? extends C> supplier) {
        assertNotClosed();

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<T> toMultiset() {
        assertNotClosed();

        try {
            final Multiset<T> result = new Multiset<>();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Multiset<T> toMultiset(Supplier<? extends Multiset<T>> supplier) {
        assertNotClosed();

        try {
            final Multiset<T> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<T> toLongMultiset() {
        assertNotClosed();

        try {
            final LongMultiset<T> result = new LongMultiset<>();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public LongMultiset<T> toLongMultiset(Supplier<? extends LongMultiset<T>> supplier) {
        assertNotClosed();

        try {
            final LongMultiset<T> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                Collectors.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, A, D> downstream, final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
            final Map<K, A> intermediate = (Map<K, A>) result;
            K key = null;
            A v = null;
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                key = checkArgNotNull(keyMapper.apply(next), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    if ((v = downstreamSupplier.get()) != null) {
                        intermediate.put(key, v);
                    }
                }

                downstreamAccumulator.accept(v, valueMapper.apply(next));
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

    //    @Override
    //    public <K, V, M extends Map<K, V>> M flatToMap(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) {
    //        assertNotClosed();
    //
    //        try {
    //            final M result = mapFactory.get();
    //            ObjIterator<? extends K> keyIter = null;
    //            K k = null;
    //            T next = null;
    //
    //            while (elements.hasNext()) {
    //                next = elements.next();
    //
    //                try (Stream<? extends K> ks = flatKeyMapper.apply(next)) {
    //                    keyIter = ks.iterator();
    //
    //                    while (keyIter.hasNext()) {
    //                        k = keyIter.next();
    //                        Collectors.merge(result, k, valueMapper.apply(k, next), mergeFunction);
    //                    }
    //                }
    //            }
    //
    //            return result;
    //        } finally {
    //            close();
    //        }
    //    }
    //
    //    @Override
    //    public <K, V, A, D, M extends Map<K, D>> M flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Collector<? super V, A, D> downstream, Supplier<? extends M> mapFactory) {
    //        assertNotClosed();
    //
    //        try {
    //            final M result = mapFactory.get();
    //            final Supplier<A> downstreamSupplier = downstream.supplier();
    //            final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
    //            final Function<A, D> downstreamFinisher = downstream.finisher();
    //            final Map<K, A> intermediate = (Map<K, A>) result;
    //
    //            ObjIterator<? extends K> keyIter = null;
    //            K k = null;
    //            A v = null;
    //
    //            T next = null;
    //
    //            while (elements.hasNext()) {
    //                next = elements.next();
    //
    //                try (Stream<? extends K> ks = flatKeyMapper.apply(next)) {
    //                    keyIter = ks.iterator();
    //
    //                    while (keyIter.hasNext()) {
    //                        k = checkArgNotNull(keyIter.next(), "element cannot be mapped to a null key");
    //
    //                        if ((v = intermediate.get(k)) == null) {
    //                            if ((v = downstreamSupplier.get()) != null) {
    //                                intermediate.put(k, v);
    //                            }
    //                        }
    //
    //                        downstreamAccumulator.accept(v, valueMapper.apply(k, next));
    //                    }
    //                }
    //            }
    //
    //            final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
    //                @Override
    //                public A apply(K k, A v) {
    //                    return (A) downstreamFinisher.apply(v);
    //                }
    //            };
    //
    //            Collectors.replaceAll(intermediate, function);
    //
    //            return result;
    //        } finally {
    //            close();
    //        }
    //    }
    //
    //    @Override
    //    public <K, V, M extends Map<K, V>> M flattToMap(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory) {
    //        assertNotClosed();
    //
    //        try {
    //            final M result = mapFactory.get();
    //            Collection<? extends K> ks = null;
    //
    //            T next = null;
    //
    //            while (elements.hasNext()) {
    //                next = elements.next();
    //
    //                ks = flatKeyMapper.apply(next);
    //
    //                if (N.notNullOrEmpty(ks)) {
    //                    for (K k : ks) {
    //                        Collectors.merge(result, k, valueMapper.apply(k, next), mergeFunction);
    //                    }
    //                }
    //            }
    //
    //            return result;
    //        } finally {
    //            close();
    //        }
    //    }
    //
    //    @Override
    //    public <K, V, A, D, M extends Map<K, D>> M flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Collector<? super V, A, D> downstream, Supplier<? extends M> mapFactory) {
    //        assertNotClosed();
    //
    //        try {
    //            final M result = mapFactory.get();
    //            final Supplier<A> downstreamSupplier = downstream.supplier();
    //            final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
    //            final Function<A, D> downstreamFinisher = downstream.finisher();
    //            final Map<K, A> intermediate = (Map<K, A>) result;
    //
    //            Collection<? extends K> ks = null;
    //            A v = null;
    //
    //            T next = null;
    //
    //            while (elements.hasNext()) {
    //                next = elements.next();
    //                ks = flatKeyMapper.apply(next);
    //
    //                if (N.notNullOrEmpty(ks)) {
    //                    for (K k : ks) {
    //                        checkArgNotNull(k, "element cannot be mapped to a null key");
    //
    //                        if ((v = intermediate.get(k)) == null) {
    //                            if ((v = downstreamSupplier.get()) != null) {
    //                                intermediate.put(k, v);
    //                            }
    //                        }
    //
    //                        downstreamAccumulator.accept(v, valueMapper.apply(k, next));
    //                    }
    //                }
    //            }
    //
    //            final BiFunction<? super K, ? super A, ? extends A> function = new BiFunction<K, A, A>() {
    //                @Override
    //                public A apply(K k, A v) {
    //                    return (A) downstreamFinisher.apply(v);
    //                }
    //            };
    //
    //            Collectors.replaceAll(intermediate, function);
    //
    //            return result;
    //        } finally {
    //            close();
    //        }
    //    }

    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M toMultimap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                result.put(keyMapper.apply(next), valueMapper.apply(next));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M flatToMultimap(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();
            ObjIterator<? extends K> keyIter = null;
            K k = null;
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                try (Stream<? extends K> ks = flatKeyMapper.apply(next)) {
                    keyIter = ks.iteratorEx();

                    while (keyIter.hasNext()) {
                        k = keyIter.next();
                        result.put(k, valueMapper.apply(k, next));
                    }
                }
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M flattToMultimap(
            final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper, final BiFunction<? super K, ? super T, ? extends V> valueMapper,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        try {
            final M result = mapFactory.get();

            Collection<? extends K> ks = null;
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                ks = flatKeyMapper.apply(next);

                if (N.notNullOrEmpty(ks)) {
                    for (K k : ks) {
                        result.put(k, valueMapper.apply(k, next));
                    }
                }
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        assertNotClosed();

        try {
            T result = identity;

            while (elements.hasNext()) {
                result = accumulator.apply(result, elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return Optional.empty();
            }

            T result = elements.next();

            while (elements.hasNext()) {
                result = accumulator.apply(result, elements.next());
            }

            return Optional.of(result);
        } finally {
            close();
        }
    }

    @Override
    public <U> U reduce(final U identity, final BiFunction<U, ? super T, U> accumulator, final BinaryOperator<U> combiner) {
        assertNotClosed();

        try {
            U result = identity;

            while (elements.hasNext()) {
                result = accumulator.apply(result, elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<? super R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        assertNotClosed();

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        assertNotClosed();

        try {
            final A container = collector.supplier().get();
            final BiConsumer<A, ? super T> accumulator = collector.accumulator();

            while (elements.hasNext()) {
                accumulator.accept(container, elements.next());
            }

            return collector.finisher().apply(container);
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> last(final int n) {
        checkArgNotNegative(n, "n");

        if (n == 0) {
            return new IteratorStream<>(ObjIteratorEx.EMPTY, sorted, cmp, closeHandlers);
        }

        return newStream(new ObjIteratorEx<T>() {
            private Iterator<T> iter;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (initialized == false) {
                    init();
                }

                return iter.next();
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    final Deque<T> dqueue = n <= 1024 ? new ArrayDeque<T>(n) : new LinkedList<T>();

                    try {
                        while (elements.hasNext()) {
                            if (dqueue.size() >= n) {
                                dqueue.pollFirst();
                            }

                            dqueue.offerLast(elements.next());
                        }
                    } finally {
                        IteratorStream.this.close();
                    }

                    iter = dqueue.iterator();
                }
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> skipLast(final int n) {
        if (n <= 0) {
            return newStream(elements, sorted, cmp);
        }

        return newStream(new ObjIteratorEx<T>() {
            private Deque<T> dqueue = null;

            @Override
            public boolean hasNext() {
                if (dqueue == null) {
                    dqueue = n <= 1024 ? new ArrayDeque<T>(n) : new LinkedList<T>();

                    while (dqueue.size() < n && elements.hasNext()) {
                        dqueue.offerLast(elements.next());
                    }
                }

                return elements.hasNext();
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                dqueue.offerLast(elements.next());

                return dqueue.pollFirst();
            }

        }, sorted, cmp);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return Optional.empty();
            } else if (sorted && isSameComparator(comparator, cmp)) {
                return Optional.of(elements.next());
            }

            comparator = comparator == null ? NATURAL_COMPARATOR : comparator;
            T candidate = elements.next();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                if (comparator.compare(next, candidate) < 0) {
                    candidate = next;
                }
            }

            return Optional.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return Optional.empty();
            } else if (sorted && isSameComparator(comparator, cmp)) {
                T next = null;

                while (elements.hasNext()) {
                    next = elements.next();
                }

                return Optional.of(next);
            }

            comparator = comparator == null ? NATURAL_COMPARATOR : comparator;
            T candidate = elements.next();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                if (comparator.compare(next, candidate) > 0) {
                    candidate = next;
                }
            }

            return Optional.of(candidate);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> kthLargest(int k, Comparator<? super T> comparator) {
        checkArgPositive(k, "k");
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return Optional.empty();
            } else if (sorted && isSameComparator(comparator, cmp)) {
                final LinkedList<T> queue = new LinkedList<>();

                while (elements.hasNext()) {
                    if (queue.size() >= k) {
                        queue.poll();
                    }

                    queue.offer(elements.next());
                }

                return queue.size() < k ? (Optional<T>) Optional.empty() : Optional.of(queue.peek());
            }

            comparator = comparator == null ? NATURAL_COMPARATOR : comparator;
            final Queue<T> queue = new PriorityQueue<>(k, comparator);
            T e = null;

            while (elements.hasNext()) {
                e = elements.next();

                if (queue.size() < k) {
                    queue.offer(e);
                } else {
                    if (comparator.compare(e, queue.peek()) > 0) {
                        queue.poll();
                        queue.offer(e);
                    }
                }
            }

            return queue.size() < k ? (Optional<T>) Optional.empty() : Optional.of(queue.peek());
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
    public <E extends Exception> boolean anyMatch(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next())) {
                    return true;
                }
            }
        } finally {
            close();
        }

        return false;
    }

    @Override
    public <E extends Exception> boolean allMatch(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next()) == false) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next())) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return true;
    }

    @Override
    public <E extends Exception> boolean nMatch(final long atLeast, final long atMost, Try.Predicate<? super T, E> predicate) throws E {
        checkArgNotNegative(atLeast, "atLeast");
        checkArgNotNegative(atMost, "atMost");
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");

        assertNotClosed();

        long cnt = 0;

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next())) {
                    if (++cnt > atMost) {
                        return false;
                    }
                }
            }
        } finally {
            close();
        }

        return cnt >= atLeast && cnt <= atMost;
    }

    @Override
    public <E extends Exception> Optional<T> findFirst(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                T e = elements.next();

                if (predicate.test(e)) {
                    return Optional.of(e);
                }
            }
        } finally {
            close();
        }

        return (Optional<T>) Optional.empty();
    }

    @Override
    public <E extends Exception> Optional<T> findLast(final Try.Predicate<? super T, E> predicate) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return (Optional<T>) Optional.empty();
            }

            boolean hasResult = false;
            T e = null;
            T result = null;

            while (elements.hasNext()) {
                e = elements.next();

                if (predicate.test(e)) {
                    result = e;
                    hasResult = true;
                }
            }

            return hasResult ? Optional.of(result) : (Optional<T>) Optional.empty();
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> appendIfEmpty(Collection<? extends T> c) {
        return elements.hasNext() == false ? append(c) : this;
    }

    @Override
    public Stream<T> appendIfEmpty(final Supplier<Stream<T>> supplier) {
        if (elements.hasNext() == false) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

    @Override
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Try.Function<? super Stream<T>, R, E> func) throws E {
        try {
            if (elements.hasNext()) {
                return Optional.of(func.apply(this));
            } else {
                return Optional.empty();
            }
        } finally {
            if (isClosed == false) {
                close();
            }
        }
    }

    @Override
    public <E extends Exception> void acceptIfNotEmpty(Try.Consumer<? super Stream<T>, E> action) throws E {
        try {
            if (elements.hasNext()) {
                action.accept(this);
            }
        } finally {
            if (isClosed == false) {
                close();
            }
        }
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterator asynchronously.
     * 
     * @param stream
     * @param queueSize Default value is 8
     * @return
     */
    @Override
    public Stream<T> queued(int queueSize) {
        final Iterator<T> iter = iteratorEx();

        if (iter instanceof QueuedIterator && ((QueuedIterator<? extends T>) iter).max() >= queueSize) {
            return newStream(elements, sorted, cmp);
        } else {
            return newStream(Stream.parallelConcatt(Arrays.asList(iter), 1, queueSize), sorted, cmp);
        }
    }

    @Override
    ObjIteratorEx<T> iteratorEx() {
        return elements;
    }

    @Override
    public Stream<T> parallel(int maxThreadNum, Splitor splitor) {
        return new ParallelIteratorStream<>(elements, sorted, cmp, checkMaxThreadNum(maxThreadNum), checkSplitor(splitor), asyncExecutor(), closeHandlers);
    }

    @Override
    public Stream<T> parallel(final int maxThreadNum, final Executor executor) {
        return new ParallelIteratorStream<>(elements, sorted, cmp, checkMaxThreadNum(maxThreadNum), splitor(), createAsyncExecutor(executor), closeHandlers);
    }

    @Override
    public java.util.stream.Stream<T> toJdkStream() {
        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(elements, Spliterator.ORDERED);

        if (N.isNullOrEmpty(closeHandlers)) {
            return StreamSupport.stream(() -> spliterator, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL, isParallel());
        } else {
            return StreamSupport.stream(() -> spliterator, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL, isParallel())
                    .onClose(() -> close(closeHandlers));
        }
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new IteratorStream<>(elements, sorted, cmp, newCloseHandlers);
    }
}
