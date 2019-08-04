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
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IndexedShort;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.MutableShort;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBiPredicate;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortTernaryOperator;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToShortFunction;

/**
 * 
 */
abstract class AbstractShortStream extends ShortStream {

    AbstractShortStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public ShortStream distinct() {
        final Set<Object> set = new HashSet<>();

        return newStream(this.sequential().filter(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                return set.add(value);
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public ShortStream flattMap(final ShortFunction<short[]> mapper) {
        return flatMap(new ShortFunction<ShortStream>() {
            @Override
            public ShortStream apply(short t) {
                return ShortStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flattMapToObj(final ShortFunction<? extends Collection<T>> mapper) {
        return flatMapToObj(new ShortFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(short t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flatMappToObj(final ShortFunction<T[]> mapper) {
        return flatMapToObj(new ShortFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(short t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public ShortStream rangeMap(final ShortBiPredicate sameRange, final ShortBinaryOperator mapper) {
        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() {
            private short left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public short nextShort() {
                left = hasNext ? next : iter.nextShort();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextShort();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsShort(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapp(final ShortBiPredicate sameRange, final ShortBiFunction<T> mapper) {
        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<T>() {
            private short left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextShort();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextShort();

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
    public Stream<ShortList> collapse(final ShortBiPredicate collapsible) {
        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<ShortList>() {
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public ShortList next() {
                final ShortList result = new ShortList(9);
                result.add(hasNext ? next : (next = iter.nextShort()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextShort()))) {
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
    public ShortStream collapse(final ShortBiPredicate collapsible, final ShortBinaryOperator mergeFunction) {
        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() {
            private boolean hasNext = false;
            private short next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public short nextShort() {
                short res = hasNext ? next : (next = iter.nextShort());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextShort()))) {
                        res = mergeFunction.applyAsShort(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public ShortStream skip(final long n, final ShortConsumer action) {
        final ShortPredicate filter = isParallel() ? new ShortPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(short value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new ShortPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(short value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public ShortStream removeIf(final ShortPredicate predicate) {
        checkArgNotNull(predicate);

        return filter(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                return predicate.test(value) == false;
            }
        });
    }

    @Override
    public ShortStream removeIf(final ShortPredicate predicate, final ShortConsumer action) {
        checkArgNotNull(predicate);
        checkArgNotNull(predicate);

        return filter(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public ShortStream dropWhile(final ShortPredicate predicate, final ShortConsumer action) {
        checkArgNotNull(predicate);
        checkArgNotNull(action);

        return dropWhile(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public ShortStream step(final long step) {
        checkArgPositive(step, "step");

        final long skip = step - 1;
        final ShortIteratorEx iter = this.iteratorEx();

        final ShortIterator shortIterator = new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                final short next = iter.nextShort();
                iter.skip(skip);
                return next;
            }
        };

        return newStream(shortIterator, sorted);
    }

    @Override
    public Stream<ShortStream> split(final int chunkSize) {
        return splitToList(chunkSize).map(new Function<ShortList, ShortStream>() {
            @Override
            public ShortStream apply(ShortList t) {
                return new ArrayShortStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<ShortStream> split(final ShortPredicate predicate) {
        return splitToList(predicate).map(new Function<ShortList, ShortStream>() {
            @Override
            public ShortStream apply(ShortList t) {
                return new ArrayShortStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<ShortStream> splitBy(final ShortPredicate where) {
        checkArgNotNull(where);

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<ShortStream>() {
            private int cursor = 0;
            private short next = 0;
            private boolean hasNext = false;

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

                    while (iter.hasNext()) {
                        next = iter.nextShort();

                        if (where.test(next)) {
                            list.add(next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }

                    result = new ArrayShortStream(list.array(), 0, list.size(), sorted, null);
                } else {
                    ShortIteratorEx iterEx = iter;

                    if (hasNext) {
                        iterEx = new ShortIteratorEx() {
                            private boolean isFirst = true;

                            @Override
                            public boolean hasNext() {
                                return isFirst || iter.hasNext();
                            }

                            @Override
                            public short nextShort() {
                                if (hasNext() == false) {
                                    throw new NoSuchElementException();
                                }

                                if (isFirst) {
                                    isFirst = false;
                                    return next;
                                } else {
                                    return iter.nextShort();
                                }
                            }
                        };
                    }

                    result = new IteratorShortStream(iterEx, sorted, null);
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
                            next = iter.nextShort();

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
    public Stream<ShortStream> sliding(final int windowSize, final int increment) {
        return slidingToList(windowSize, increment).map(new Function<ShortList, ShortStream>() {
            @Override
            public ShortStream apply(ShortList t) {
                return new ArrayShortStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public ShortStream scan(final ShortBinaryOperator accumulator) {
        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() {
            private short res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextShort());
                } else {
                    return (res = accumulator.applyAsShort(res, iter.nextShort()));
                }
            }
        }, false);
    }

    @Override
    public ShortStream scan(final short init, final ShortBinaryOperator accumulator) {
        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() {
            private short res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public short nextShort() {
                return (res = accumulator.applyAsShort(res, iter.nextShort()));
            }
        }, false);
    }

    @Override
    public ShortStream scan(final short init, final ShortBinaryOperator accumulator, final boolean initIncluded) {
        if (initIncluded == false) {
            return scan(init, accumulator);
        }

        final ShortIteratorEx iter = iteratorEx();

        return newStream(new ShortIteratorEx() {
            private boolean isFirst = true;
            private short res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public short nextShort() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsShort(res, iter.nextShort()));
            }
        }, false);
    }

    @Override
    public ShortStream top(int n) {
        return top(n, SHORT_COMPARATOR);
    }

    @Override
    public ShortStream intersection(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public ShortStream difference(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public ShortStream symmetricDifference(final Collection<Short> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new ShortPredicate() {
            @Override
            public boolean test(short value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).append(Stream.of(c).filter(new Predicate<Short>() {
            @Override
            public boolean test(Short value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).mapToShort(ToShortFunction.UNBOX)).iteratorEx(), false);
    }

    @Override
    public ShortStream reversed() {
        return newStream(new ShortIteratorEx() {
            private boolean initialized = false;
            private short[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public short nextShort() {
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
            public short[] toArray() {
                if (initialized == false) {
                    init();
                }

                final short[] a = new short[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractShortStream.this.toArray();
                    cursor = aar.length;
                }
            }
        }, false);
    }

    @Override
    public ShortStream shuffled(final Random rnd) {
        return lazyLoad(new Function<short[], short[]>() {
            @Override
            public short[] apply(final short[] a) {
                N.shuffle(a, rnd);
                return a;
            }
        }, false);
    }

    @Override
    public ShortStream rotated(final int distance) {
        return newStream(new ShortIteratorEx() {
            private boolean initialized = false;
            private short[] aar;
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
            public short nextShort() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return aar[(start + cnt++) % len];
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
            public short[] toArray() {
                if (initialized == false) {
                    init();
                }

                final short[] a = new short[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = aar[(start + i) % len];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractShortStream.this.toArray();
                    len = aar.length;

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
    public ShortStream sorted() {
        if (sorted) {
            return newStream(iteratorEx(), sorted);
        }

        return lazyLoad(new Function<short[], short[]>() {
            @Override
            public short[] apply(final short[] a) {
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
    public ShortStream reverseSorted() {
        return newStream(new ShortIteratorEx() {
            private boolean initialized = false;
            private short[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public short nextShort() {
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
            public short[] toArray() {
                if (initialized == false) {
                    init();
                }

                final short[] a = new short[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractShortStream.this.toArray();

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

    private ShortStream lazyLoad(final Function<short[], short[]> op, final boolean sorted) {
        return newStream(new ShortIteratorEx() {
            private boolean initialized = false;
            private short[] aar;
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
            public short nextShort() {
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
            public short[] toArray() {
                if (initialized == false) {
                    init();
                }

                final short[] a = new short[len - cursor];

                for (int i = cursor; i < len; i++) {
                    a[i - cursor] = aar[i];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = op.apply(AbstractShortStream.this.toArray());
                    len = aar.length;
                }
            }
        }, sorted);
    }

    @Override
    public Stream<IndexedShort> indexed() {
        return newStream(this.sequential().mapToObj(new ShortFunction<IndexedShort>() {
            final MutableLong idx = MutableLong.of(0);

            @Override
            public IndexedShort apply(short t) {
                return IndexedShort.of(t, idx.getAndIncrement());
            }
        }).iteratorEx(), true, INDEXED_SHORT_COMPARATOR);
    }

    @Override
    public ShortStream append(ShortStream stream) {
        return ShortStream.concat(this, stream);
    }

    @Override
    public ShortStream prepend(ShortStream stream) {
        return ShortStream.concat(stream, this);
    }

    @Override
    public ShortStream merge(ShortStream b, ShortBiFunction<Nth> nextSelector) {
        return ShortStream.merge(this, b, nextSelector);
    }

    @Override
    public ShortStream zipWith(ShortStream b, ShortBinaryOperator zipFunction) {
        return ShortStream.zip(this, b, zipFunction);
    }

    @Override
    public ShortStream zipWith(ShortStream b, ShortStream c, ShortTernaryOperator zipFunction) {
        return ShortStream.zip(this, b, c, zipFunction);
    }

    @Override
    public ShortStream zipWith(ShortStream b, short valueForNoneA, short valueForNoneB, ShortBinaryOperator zipFunction) {
        return ShortStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public ShortStream zipWith(ShortStream b, ShortStream c, short valueForNoneA, short valueForNoneB, short valueForNoneC, ShortTernaryOperator zipFunction) {
        return ShortStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    //    @Override
    //    public ShortStream cached() {
    //        return newStream(toArray(), sorted);
    //    }

    @Override
    public <K, V> Map<K, V> toMap(ShortFunction<? extends K> keyMapper, ShortFunction<? extends V> valueMapper) {
        return toMap(keyMapper, valueMapper, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(ShortFunction<? extends K> keyMapper, ShortFunction<? extends V> valueMapper, Supplier<? extends M> mapFactory) {
        return toMap(keyMapper, valueMapper, Fn.<V> throwingMerger(), mapFactory);
    }

    @Override
    public <K, V> Map<K, V> toMap(ShortFunction<? extends K> keyMapper, ShortFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, A, D> Map<K, D> toMap(ShortFunction<? extends K> keyMapper, Collector<Short, A, D> downstream) {
        return toMap(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public OptionalShort first() {
        assertNotClosed();

        try {
            final ShortIterator iter = this.iteratorEx();

            return iter.hasNext() ? OptionalShort.of(iter.nextShort()) : OptionalShort.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort last() {
        assertNotClosed();

        try {
            final ShortIterator iter = this.iteratorEx();

            if (iter.hasNext() == false) {
                return OptionalShort.empty();
            }

            short next = iter.nextShort();

            while (iter.hasNext()) {
                next = iter.nextShort();
            }

            return OptionalShort.of(next);
        } finally {
            close();
        }
    }

    @Override
    public OptionalShort onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final ShortIterator iter = this.iteratorEx();

            final OptionalShort result = iter.hasNext() ? OptionalShort.of(iter.nextShort()) : OptionalShort.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextShort()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalShort findAny(final Try.ShortPredicate<E> predicate) throws E {
        return findFirst(predicate);
    }

    @Override
    public <E extends Exception, E2 extends Exception> OptionalShort findFirstOrLast(Try.ShortPredicate<E> predicateForFirst,
            Try.ShortPredicate<E> predicateForLast) throws E, E2 {
        assertNotClosed();

        try {
            final ShortIteratorEx iter = iteratorEx();
            MutableShort last = null;
            short next = 0;

            while (iter.hasNext()) {
                next = iter.nextShort();

                if (predicateForFirst.test(next)) {
                    return OptionalShort.of(next);
                } else if (predicateForLast.test(next)) {
                    if (last == null) {
                        last = MutableShort.of(next);
                    } else {
                        last.setValue(next);
                    }
                }
            }

            return last == null ? OptionalShort.empty() : OptionalShort.of(last.value());
        } finally {
            close();
        }
    }

    @Override
    public Optional<Map<Percentage, Short>> percentiles() {
        assertNotClosed();

        try {
            final short[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentiles(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> summarizeAndPercentiles() {
        assertNotClosed();

        try {
            final short[] a = sorted().toArray();

            if (N.isNullOrEmpty(a)) {
                return Pair.of(new ShortSummaryStatistics(), Optional.<Map<Percentage, Short>> empty());
            } else {
                return Pair.of(new ShortSummaryStatistics(a.length, sum(a), a[0], a[a.length - 1]), Optional.of(N.percentiles(a)));
            }
        } finally {
            close();
        }
    }

    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        assertNotClosed();

        try {
            final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseCachedBuffer(true);
            final ShortIteratorEx iter = this.iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextShort());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjShortConsumer<? super R> accumulator) {
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }
}
