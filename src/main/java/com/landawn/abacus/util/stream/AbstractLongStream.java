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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.LongSummaryStatistics;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongBiPredicate;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongTernaryOperator;
import com.landawn.abacus.util.function.ObjLongConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToLongFunction;

/**
 *
 */
abstract class AbstractLongStream extends LongStream {

    AbstractLongStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public LongStream distinct() {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return newStream(this.sequential().filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                return set.add(value);
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public LongStream flattMap(final LongFunction<long[]> mapper) {
        assertNotClosed();

        return flatMap(new LongFunction<LongStream>() {
            @Override
            public LongStream apply(long t) {
                return LongStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flattMapToObj(final LongFunction<? extends Collection<T>> mapper) {
        assertNotClosed();

        return flatMapToObj(new LongFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(long t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flatMappToObj(final LongFunction<T[]> mapper) {
        assertNotClosed();

        return flatMapToObj(new LongFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(long t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public LongStream rangeMap(final LongBiPredicate sameRange, final LongBinaryOperator mapper) {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() {
            private long left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public long nextLong() {
                left = hasNext ? next : iter.nextLong();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextLong();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsLong(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapToObj(final LongBiPredicate sameRange, final LongBiFunction<T> mapper) {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<T>() {
            private long left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextLong();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextLong();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.apply(left, right);
            }
        }, false, null);
    }

    @Override
    public Stream<LongList> collapse(final LongBiPredicate collapsible) {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<LongList>() {
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public LongList next() {
                final LongList result = new LongList(9);
                result.add(hasNext ? next : (next = iter.nextLong()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextLong()))) {
                        result.add(next);
                    } else {
                        break;
                    }
                }

                return result;
            }
        }, false, null);
    }

    @Override
    public LongStream collapse(final LongBiPredicate collapsible, final LongBinaryOperator mergeFunction) {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() {
            private boolean hasNext = false;
            private long next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public long nextLong() {
                long res = hasNext ? next : (next = iter.nextLong());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextLong()))) {
                        res = mergeFunction.applyAsLong(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public LongStream skip(final long n, final LongConsumer action) {
        assertNotClosed();

        final LongPredicate filter = isParallel() ? new LongPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(long value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new LongPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(long value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public LongStream removeIf(final LongPredicate predicate) {
        assertNotClosed();

        return filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                return predicate.test(value) == false;
            }
        });
    }

    @Override
    public LongStream removeIf(final LongPredicate predicate, final LongConsumer actionOnDroppedItem) {
        assertNotClosed();

        return filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                if (predicate.test(value)) {
                    actionOnDroppedItem.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public LongStream filter(final LongPredicate predicate, final LongConsumer actionOnDroppedItem) {
        assertNotClosed();

        return filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                if (!predicate.test(value)) {
                    actionOnDroppedItem.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public LongStream dropWhile(final LongPredicate predicate, final LongConsumer actionOnDroppedItem) {
        assertNotClosed();

        return dropWhile(new LongPredicate() {
            @Override
            public boolean test(long value) {
                if (predicate.test(value)) {
                    actionOnDroppedItem.accept(value);
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public LongStream step(final long step) {
        assertNotClosed();

        checkArgPositive(step, "step");

        final long skip = step - 1;
        final LongIteratorEx iter = this.iteratorEx();

        final LongIterator longIterator = new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                final long next = iter.nextLong();
                iter.skip(skip);
                return next;
            }
        };

        return newStream(longIterator, sorted);
    }

    @Override
    public Stream<LongStream> split(final int chunkSize) {
        assertNotClosed();

        return splitToList(chunkSize).map(new Function<LongList, LongStream>() {
            @Override
            public LongStream apply(LongList t) {
                return new ArrayLongStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<LongStream> split(final LongPredicate predicate) {
        assertNotClosed();

        return splitToList(predicate).map(new Function<LongList, LongStream>() {
            @Override
            public LongStream apply(LongList t) {
                return new ArrayLongStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<LongStream> splitAt(final LongPredicate where) {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<LongStream>() {
            private int cursor = 0;
            private long next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public LongStream next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                LongStream result = null;

                if (cursor == 0) {
                    final LongList list = new LongList();

                    while (iter.hasNext()) {
                        next = iter.nextLong();

                        if (!where.test(next)) {
                            list.add(next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }

                    result = new ArrayLongStream(list.array(), 0, list.size(), sorted, null);
                } else {
                    LongIteratorEx iterEx = iter;

                    if (hasNext) {
                        iterEx = new LongIteratorEx() {
                            private boolean isFirst = true;

                            @Override
                            public boolean hasNext() {
                                return isFirst || iter.hasNext();
                            }

                            @Override
                            public long nextLong() {
                                if (hasNext() == false) {
                                    throw new NoSuchElementException();
                                }

                                if (isFirst) {
                                    isFirst = false;
                                    return next;
                                } else {
                                    return iter.nextLong();
                                }
                            }
                        };
                    }

                    result = new IteratorLongStream(iterEx, sorted, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                iter.count();

                return 2 - cursor;
            }

            @Override
            public void skip(long n) {
                if (n == 0) {
                    return;
                } else if (n == 1) {
                    if (cursor == 0) {
                        while (iter.hasNext()) {
                            next = iter.nextLong();

                            if (where.test(next) == false) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else {
                        iter.skip(Long.MAX_VALUE);
                    }
                } else {
                    iter.skip(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }

        }, false, null);
    }

    @Override
    public Stream<LongStream> sliding(final int windowSize, final int increment) {
        assertNotClosed();

        return slidingToList(windowSize, increment).map(new Function<LongList, LongStream>() {
            @Override
            public LongStream apply(LongList t) {
                return new ArrayLongStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public LongStream scan(final LongBinaryOperator accumulator) {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() {
            private long res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextLong());
                } else {
                    return (res = accumulator.applyAsLong(res, iter.nextLong()));
                }
            }
        }, false);
    }

    @Override
    public LongStream scan(final long init, final LongBinaryOperator accumulator) {
        assertNotClosed();

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() {
            private long res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                return (res = accumulator.applyAsLong(res, iter.nextLong()));
            }
        }, false);
    }

    @Override
    public LongStream scan(final long init, final LongBinaryOperator accumulator, final boolean initIncluded) {
        assertNotClosed();

        if (initIncluded == false) {
            return scan(init, accumulator);
        }

        final LongIteratorEx iter = iteratorEx();

        return newStream(new LongIteratorEx() {
            private boolean isFirst = true;
            private long res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsLong(res, iter.nextLong()));
            }
        }, false);
    }

    @Override
    public LongStream top(int n) {
        assertNotClosed();

        return top(n, LONG_COMPARATOR);
    }

    @Override
    public LongStream intersection(final Collection<?> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public LongStream difference(final Collection<?> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public LongStream symmetricDifference(final Collection<Long> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new LongPredicate() {
            @Override
            public boolean test(long value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).append(Stream.of(c).filter(new Predicate<Long>() {
            @Override
            public boolean test(Long value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).mapToLong(ToLongFunction.UNBOX)).iteratorEx(), false);
    }

    @Override
    public LongStream reversed() {
        assertNotClosed();

        return newStream(new LongIteratorEx() {
            private boolean initialized = false;

            private long[] elements;
            private int fromIndex = -1;
            private int toIndex = -1;

            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > fromIndex;
            }

            @Override
            public long nextLong() {
                if (initialized == false) {
                    init();
                }

                if (cursor <= fromIndex) {
                    throw new NoSuchElementException();
                }

                return elements[--cursor];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return cursor - fromIndex;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n < cursor - fromIndex ? cursor - (int) n : fromIndex;
            }

            @Override
            public long[] toArray() {
                if (initialized == false) {
                    init();
                }

                final long[] a = new long[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    final Tuple3<long[], Integer, Integer> tp = AbstractLongStream.this.array();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public LongStream rotated(final int distance) {
        assertNotClosed();

        return newStream(new LongIteratorEx() {
            private boolean initialized = false;

            private long[] elements;
            private int fromIndex = -1;
            private int toIndex = -1;

            private int len;
            private int start;
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

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
                if (initialized == false) {
                    init();
                }

                return len - cnt;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            @Override
            public long[] toArray() {
                if (initialized == false) {
                    init();
                }

                final long[] a = new long[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    final Tuple3<long[], Integer, Integer> tp = AbstractLongStream.this.array();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    len = toIndex - fromIndex;

                    if (len > 0) {
                        start = distance % len;

                        if (start < 0) {
                            start += len;
                        }

                        start = len - start;
                    }
                }
            }
        }, distance == 0 && sorted);
    }

    @Override
    public LongStream shuffled(final Random rnd) {
        assertNotClosed();

        checkArgNotNull(rnd, "random");

        return lazyLoad(new Function<long[], long[]>() {
            @Override
            public long[] apply(final long[] a) {
                N.shuffle(a, rnd);
                return a;
            }
        }, false);
    }

    @Override
    public LongStream sorted() {
        assertNotClosed();

        if (sorted) {
            return newStream(iteratorEx(), sorted);
        }

        return lazyLoad(new Function<long[], long[]>() {
            @Override
            public long[] apply(final long[] a) {
                if (isParallel()) {
                    N.parallelSort(a);
                } else {
                    N.sort(a);
                }

                return a;
            }
        }, true);
    }

    @Override
    public LongStream reverseSorted() {
        assertNotClosed();

        return newStream(new LongIteratorEx() {
            private boolean initialized = false;
            private long[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public long nextLong() {
                if (initialized == false) {
                    init();
                }

                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }

                return aar[--cursor];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n < cursor ? cursor - (int) n : 0;
            }

            @Override
            public long[] toArray() {
                if (initialized == false) {
                    init();
                }

                final long[] a = new long[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractLongStream.this.toArray(false);

                    if (isParallel()) {
                        N.parallelSort(aar);
                    } else {
                        N.sort(aar);
                    }

                    cursor = aar.length;
                }
            }
        }, false);
    }

    private LongStream lazyLoad(final Function<long[], long[]> op, final boolean sorted) {
        return newStream(new LongIteratorEx() {
            private boolean initialized = false;
            private long[] aar;
            private int cursor = 0;
            private int len;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (initialized == false) {
                    init();
                }

                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return len - cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n > len - cursor ? len : cursor + (int) n;
            }

            @Override
            public long[] toArray() {
                if (initialized == false) {
                    init();
                }

                final long[] a = new long[len - cursor];

                for (int i = cursor; i < len; i++) {
                    a[i - cursor] = aar[i];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = op.apply(AbstractLongStream.this.toArray(false));
                    len = aar.length;
                }
            }
        }, sorted);
    }

    @Override
    public Stream<IndexedLong> indexed() {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        return newStream(this.sequential().mapToObj(new LongFunction<IndexedLong>() {
            @Override
            public IndexedLong apply(long t) {
                return IndexedLong.of(t, idx.getAndIncrement());
            }
        }).iteratorEx(), true, INDEXED_LONG_COMPARATOR);
    }

    @Override
    @SafeVarargs
    public final LongStream prepend(final long... a) {
        assertNotClosed();

        return prepend(LongStream.of(a));
    }

    @Override
    public LongStream prepend(LongStream stream) {
        assertNotClosed();

        return LongStream.concat(stream, this);
    }

    @Override
    public LongStream prepend(final OptionalLong op) {
        assertNotClosed();

        return prepend(op.stream());
    }

    @Override
    @SafeVarargs
    public final LongStream append(final long... a) {
        assertNotClosed();

        return append(LongStream.of(a));
    }

    @Override
    public LongStream append(LongStream stream) {
        assertNotClosed();

        return LongStream.concat(this, stream);
    }

    @Override
    public LongStream append(final OptionalLong op) {
        assertNotClosed();

        return prepend(op.stream());
    }

    @Override
    @SafeVarargs
    public final LongStream appendIfEmpty(final long... a) {
        assertNotClosed();

        return appendIfEmpty(() -> LongStream.of(a));
    }

    @Override
    public LongStream merge(LongStream b, LongBiFunction<MergeResult> nextSelector) {
        assertNotClosed();

        return LongStream.merge(this, b, nextSelector);
    }

    @Override
    public LongStream zipWith(LongStream b, LongBinaryOperator zipFunction) {
        assertNotClosed();

        return LongStream.zip(this, b, zipFunction);
    }

    @Override
    public LongStream zipWith(LongStream b, LongStream c, LongTernaryOperator zipFunction) {
        assertNotClosed();

        return LongStream.zip(this, b, c, zipFunction);
    }

    @Override
    public LongStream zipWith(LongStream b, long valueForNoneA, long valueForNoneB, LongBinaryOperator zipFunction) {
        assertNotClosed();

        return LongStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public LongStream zipWith(LongStream b, LongStream c, long valueForNoneA, long valueForNoneB, long valueForNoneC, LongTernaryOperator zipFunction) {
        assertNotClosed();

        return LongStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    //    @Override
    //    public LongStream cached() {
    //        return newStream(toArray(), sorted);
    //    }

    @Override
    public <K, V> Map<K, V> toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends V> valueMapper) {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends V> valueMapper, Supplier<? extends M> mapFactory) {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.<V> throwingMerger(), mapFactory);
    }

    @Override
    public <K, V> Map<K, V> toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, A, D> Map<K, D> toMap(LongFunction<? extends K> keyMapper, Collector<Long, A, D> downstream) {
        assertNotClosed();

        return toMap(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(Throwables.IndexedLongConsumer<E> action) throws E {
        assertNotClosed();

        if (isParallel()) {
            final AtomicInteger idx = new AtomicInteger(0);

            forEach(new Throwables.LongConsumer<E>() {
                @Override
                public void accept(long t) throws E {
                    action.accept(idx.getAndIncrement(), t);
                }
            });
        } else {
            final MutableInt idx = MutableInt.of(0);

            forEach(new Throwables.LongConsumer<E>() {
                @Override
                public void accept(long t) throws E {
                    action.accept(idx.getAndIncrement(), t);
                }
            });
        }
    }

    @Override
    public OptionalLong first() {
        assertNotClosed();

        try {
            final LongIterator iter = this.iteratorEx();

            return iter.hasNext() ? OptionalLong.of(iter.nextLong()) : OptionalLong.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong last() {
        assertNotClosed();

        try {
            final LongIterator iter = this.iteratorEx();

            if (iter.hasNext() == false) {
                return OptionalLong.empty();
            }

            long next = iter.nextLong();

            while (iter.hasNext()) {
                next = iter.nextLong();
            }

            return OptionalLong.of(next);
        } finally {
            close();
        }
    }

    @Override
    public OptionalLong onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final LongIterator iter = this.iteratorEx();

            final OptionalLong result = iter.hasNext() ? OptionalLong.of(iter.nextLong()) : OptionalLong.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextLong()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalLong findAny(final Throwables.LongPredicate<E> predicate) throws E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public <E extends Exception, E2 extends Exception> OptionalLong findFirstOrLast(Throwables.LongPredicate<E> predicateForFirst,
            Throwables.LongPredicate<E> predicateForLast) throws E, E2 {
        assertNotClosed();

        try {
            final LongIteratorEx iter = iteratorEx();
            MutableLong last = null;
            long next = 0;

            while (iter.hasNext()) {
                next = iter.nextLong();

                if (predicateForFirst.test(next)) {
                    return OptionalLong.of(next);
                } else if (predicateForLast.test(next)) {
                    if (last == null) {
                        last = MutableLong.of(next);
                    } else {
                        last.setValue(next);
                    }
                }
            }

            return last == null ? OptionalLong.empty() : OptionalLong.of(last.value());
        } finally {
            close();
        }
    }

    @Override
    public Optional<Map<Percentage, Long>> percentiles() {
        assertNotClosed();

        try {
            final long[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentiles(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> summarizeAndPercentiles() {
        assertNotClosed();

        try {
            final long[] a = sorted().toArray();

            if (N.isNullOrEmpty(a)) {
                return Pair.of(new LongSummaryStatistics(), Optional.<Map<Percentage, Long>> empty());
            } else {
                return Pair.of(new LongSummaryStatistics(a.length, sum(a), a[0], a[a.length - 1]), Optional.of(N.percentiles(a)));
            }
        } finally {
            close();
        }
    }

    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        assertNotClosed();

        try {
            final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseCachedBuffer();
            final LongIteratorEx iter = this.iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextLong());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjLongConsumer<? super R> accumulator) {
        assertNotClosed();

        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }
}
