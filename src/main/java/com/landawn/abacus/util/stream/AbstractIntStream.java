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
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.IntSummaryStatistics;
import com.landawn.abacus.util.Joiner;
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
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.ObjIntConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToIntFunction;

/**
 *
 */
abstract class AbstractIntStream extends IntStream {

    AbstractIntStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public IntStream distinct() {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return newStream(this.sequential().filter(new IntPredicate() {
            @Override
            public boolean test(int value) {
                return set.add(value);
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public IntStream flattMap(final IntFunction<int[]> mapper) {
        assertNotClosed();

        return flatMap(new IntFunction<IntStream>() {
            @Override
            public IntStream apply(int t) {
                return IntStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flattMapToObj(final IntFunction<? extends Collection<T>> mapper) {
        assertNotClosed();

        return flatMapToObj(new IntFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(int t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flatMappToObj(final IntFunction<T[]> mapper) {
        assertNotClosed();

        return flatMapToObj(new IntFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(int t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public IntStream rangeMap(final IntBiPredicate sameRange, final IntBinaryOperator mapper) {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() {
            private int left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public int nextInt() {
                left = hasNext ? next : iter.nextInt();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextInt();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsInt(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapToObj(final IntBiPredicate sameRange, final IntBiFunction<T> mapper) {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<T>() {
            private int left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextInt();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextInt();

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
    public Stream<IntList> collapse(final IntBiPredicate collapsible) {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<IntList>() {
            private boolean hasNext = false;
            private int next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public IntList next() {
                final IntList result = new IntList(9);
                result.add(hasNext ? next : (next = iter.nextInt()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextInt()))) {
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
    public IntStream collapse(final IntBiPredicate collapsible, final IntBinaryOperator mergeFunction) {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() {
            private boolean hasNext = false;
            private int next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public int nextInt() {
                int res = hasNext ? next : (next = iter.nextInt());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextInt()))) {
                        res = mergeFunction.applyAsInt(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public IntStream skip(final long n, final IntConsumer action) {
        assertNotClosed();

        final IntPredicate filter = isParallel() ? new IntPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(int value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new IntPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(int value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public IntStream removeIf(final IntPredicate predicate) {
        assertNotClosed();

        return filter(new IntPredicate() {
            @Override
            public boolean test(int value) {
                return predicate.test(value) == false;
            }
        });
    }

    @Override
    public IntStream removeIf(final IntPredicate predicate, final IntConsumer actionOnDroppedItem) {
        assertNotClosed();

        return filter(new IntPredicate() {
            @Override
            public boolean test(int value) {
                if (predicate.test(value)) {
                    actionOnDroppedItem.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public IntStream filter(final IntPredicate predicate, final IntConsumer actionOnDroppedItem) {
        assertNotClosed();

        return filter(new IntPredicate() {
            @Override
            public boolean test(int value) {
                if (!predicate.test(value)) {
                    actionOnDroppedItem.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public IntStream dropWhile(final IntPredicate predicate, final IntConsumer actionOnDroppedItem) {
        assertNotClosed();

        return dropWhile(new IntPredicate() {
            @Override
            public boolean test(int value) {
                if (predicate.test(value)) {
                    actionOnDroppedItem.accept(value);
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public IntStream step(final long step) {
        assertNotClosed();

        checkArgPositive(step, "step");

        final long skip = step - 1;
        final IntIteratorEx iter = this.iteratorEx();

        final IntIterator intIterator = new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                final int next = iter.nextInt();
                iter.skip(skip);
                return next;
            }
        };

        return newStream(intIterator, sorted);
    }

    @Override
    public Stream<IntStream> split(final int chunkSize) {
        assertNotClosed();

        return splitToList(chunkSize).map(new Function<IntList, IntStream>() {
            @Override
            public IntStream apply(IntList t) {
                return new ArrayIntStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<IntStream> split(final IntPredicate predicate) {
        assertNotClosed();

        return splitToList(predicate).map(new Function<IntList, IntStream>() {
            @Override
            public IntStream apply(IntList t) {
                return new ArrayIntStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<IntStream> splitAt(final IntPredicate where) {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<IntStream>() {
            private int cursor = 0;
            private int next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public IntStream next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                IntStream result = null;

                if (cursor == 0) {
                    final IntList list = new IntList();

                    while (iter.hasNext()) {
                        next = iter.nextInt();

                        if (!where.test(next)) {
                            list.add(next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }

                    result = new ArrayIntStream(list.array(), 0, list.size(), sorted, null);
                } else {
                    IntIteratorEx iterEx = iter;

                    if (hasNext) {
                        iterEx = new IntIteratorEx() {
                            private boolean isFirst = true;

                            @Override
                            public boolean hasNext() {
                                return isFirst || iter.hasNext();
                            }

                            @Override
                            public int nextInt() {
                                if (hasNext() == false) {
                                    throw new NoSuchElementException();
                                }

                                if (isFirst) {
                                    isFirst = false;
                                    return next;
                                } else {
                                    return iter.nextInt();
                                }
                            }
                        };
                    }

                    result = new IteratorIntStream(iterEx, sorted, null);
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
                            next = iter.nextInt();

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
    public Stream<IntStream> sliding(final int windowSize, final int increment) {
        assertNotClosed();

        return slidingToList(windowSize, increment).map(new Function<IntList, IntStream>() {
            @Override
            public IntStream apply(IntList t) {
                return new ArrayIntStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public IntStream scan(final IntBinaryOperator accumulator) {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() {
            private int res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextInt());
                } else {
                    return (res = accumulator.applyAsInt(res, iter.nextInt()));
                }
            }
        }, false);
    }

    @Override
    public IntStream scan(final int init, final IntBinaryOperator accumulator) {
        assertNotClosed();

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() {
            private int res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                return (res = accumulator.applyAsInt(res, iter.nextInt()));
            }
        }, false);
    }

    @Override
    public IntStream scan(final int init, final IntBinaryOperator accumulator, final boolean initIncluded) {
        assertNotClosed();

        if (initIncluded == false) {
            return scan(init, accumulator);
        }

        final IntIteratorEx iter = iteratorEx();

        return newStream(new IntIteratorEx() {
            private boolean isFirst = true;
            private int res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsInt(res, iter.nextInt()));
            }
        }, false);
    }

    @Override
    public IntStream top(int n) {
        assertNotClosed();

        return top(n, INT_COMPARATOR);
    }

    @Override
    public IntStream intersection(final Collection<?> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new IntPredicate() {
            @Override
            public boolean test(int value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public IntStream difference(final Collection<?> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new IntPredicate() {
            @Override
            public boolean test(int value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public IntStream symmetricDifference(final Collection<Integer> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new IntPredicate() {
            @Override
            public boolean test(int value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).append(Stream.of(c).filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).mapToInt(ToIntFunction.UNBOX)).iteratorEx(), false);
    }

    @Override
    public IntStream reversed() {
        assertNotClosed();

        return newStream(new IntIteratorEx() {
            private boolean initialized = false;

            private int[] elements;
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
            public int nextInt() {
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
            public int[] toArray() {
                if (initialized == false) {
                    init();
                }

                final int[] a = new int[cursor - fromIndex];

                for (int i = 0, len = cursor - fromIndex; i < len; i++) {
                    a[i] = elements[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    final Tuple3<int[], Integer, Integer> tp = AbstractIntStream.this.array();

                    elements = tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false);
    }

    @Override
    public IntStream rotated(final int distance) {
        assertNotClosed();

        return newStream(new IntIteratorEx() {
            private boolean initialized = false;

            private int[] elements;
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
            public int nextInt() {
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
            public int[] toArray() {
                if (initialized == false) {
                    init();
                }

                final int[] a = new int[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = elements[((start + i) % len) + fromIndex];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    final Tuple3<int[], Integer, Integer> tp = AbstractIntStream.this.array();

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
    public IntStream shuffled(final Random rnd) {
        assertNotClosed();

        checkArgNotNull(rnd, "random");

        return lazyLoad(new Function<int[], int[]>() {
            @Override
            public int[] apply(final int[] a) {
                N.shuffle(a, rnd);
                return a;
            }
        }, false);
    }

    @Override
    public IntStream sorted() {
        assertNotClosed();

        if (sorted) {
            return newStream(iteratorEx(), sorted);
        }

        return lazyLoad(new Function<int[], int[]>() {
            @Override
            public int[] apply(final int[] a) {
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
    public IntStream reverseSorted() {
        assertNotClosed();

        return newStream(new IntIteratorEx() {
            private boolean initialized = false;
            private int[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public int nextInt() {
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
            public int[] toArray() {
                if (initialized == false) {
                    init();
                }

                final int[] a = new int[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractIntStream.this.toArray(false);

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

    private IntStream lazyLoad(final Function<int[], int[]> op, final boolean sorted) {
        return newStream(new IntIteratorEx() {
            private boolean initialized = false;
            private int[] aar;
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
            public int nextInt() {
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
            public int[] toArray() {
                if (initialized == false) {
                    init();
                }

                final int[] a = new int[len - cursor];

                for (int i = cursor; i < len; i++) {
                    a[i - cursor] = aar[i];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = op.apply(AbstractIntStream.this.toArray(false));
                    len = aar.length;
                }
            }
        }, sorted);
    }

    @Override
    public Stream<IndexedInt> indexed() {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        return newStream(this.sequential().mapToObj(new IntFunction<IndexedInt>() {
            @Override
            public IndexedInt apply(int t) {
                return IndexedInt.of(t, idx.getAndIncrement());
            }
        }).iteratorEx(), true, INDEXED_INT_COMPARATOR);
    }

    @Override
    @SafeVarargs
    public final IntStream prepend(final int... a) {
        assertNotClosed();

        return prepend(IntStream.of(a));
    }

    @Override
    public IntStream prepend(IntStream stream) {
        assertNotClosed();

        return IntStream.concat(stream, this);
    }

    @Override
    public IntStream prepend(final OptionalInt op) {
        assertNotClosed();

        return prepend(op.stream());
    }

    @Override
    @SafeVarargs
    public final IntStream append(final int... a) {
        assertNotClosed();

        return append(IntStream.of(a));
    }

    @Override
    public IntStream append(IntStream stream) {
        assertNotClosed();

        return IntStream.concat(this, stream);
    }

    @Override
    public IntStream append(final OptionalInt op) {
        assertNotClosed();

        return prepend(op.stream());
    }

    @Override
    @SafeVarargs
    public final IntStream appendIfEmpty(final int... a) {
        assertNotClosed();

        return appendIfEmpty(() -> IntStream.of(a));
    }

    @Override
    public IntStream merge(IntStream b, IntBiFunction<MergeResult> nextSelector) {
        assertNotClosed();

        return IntStream.merge(this, b, nextSelector);
    }

    @Override
    public IntStream zipWith(IntStream b, IntBinaryOperator zipFunction) {
        assertNotClosed();

        return IntStream.zip(this, b, zipFunction);
    }

    @Override
    public IntStream zipWith(IntStream b, IntStream c, IntTernaryOperator zipFunction) {
        assertNotClosed();

        return IntStream.zip(this, b, c, zipFunction);
    }

    @Override
    public IntStream zipWith(IntStream b, int valueForNoneA, int valueForNoneB, IntBinaryOperator zipFunction) {
        assertNotClosed();

        return IntStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public IntStream zipWith(IntStream b, IntStream c, int valueForNoneA, int valueForNoneB, int valueForNoneC, IntTernaryOperator zipFunction) {
        assertNotClosed();

        return IntStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    //    @Override
    //    public IntStream cached() {
    //        return newStream(toArray(), sorted);
    //    }

    @Override
    public <K, V> Map<K, V> toMap(IntFunction<? extends K> keyMapper, IntFunction<? extends V> valueMapper) {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(IntFunction<? extends K> keyMapper, IntFunction<? extends V> valueMapper, Supplier<? extends M> mapFactory) {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.<V> throwingMerger(), mapFactory);
    }

    @Override
    public <K, V> Map<K, V> toMap(IntFunction<? extends K> keyMapper, IntFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, A, D> Map<K, D> toMap(IntFunction<? extends K> keyMapper, Collector<Integer, A, D> downstream) {
        assertNotClosed();

        return toMap(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public <E extends Exception> void forEachIndexed(Throwables.IndexedIntConsumer<E> action) throws E {
        assertNotClosed();

        if (isParallel()) {
            final AtomicInteger idx = new AtomicInteger(0);

            forEach(new Throwables.IntConsumer<E>() {
                @Override
                public void accept(int t) throws E {
                    action.accept(idx.getAndIncrement(), t);
                }
            });
        } else {
            final MutableInt idx = MutableInt.of(0);

            forEach(new Throwables.IntConsumer<E>() {
                @Override
                public void accept(int t) throws E {
                    action.accept(idx.getAndIncrement(), t);
                }
            });
        }
    }

    @Override
    public OptionalInt first() {
        assertNotClosed();

        try {
            final IntIterator iter = this.iteratorEx();

            return iter.hasNext() ? OptionalInt.of(iter.nextInt()) : OptionalInt.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt last() {
        assertNotClosed();

        try {
            final IntIterator iter = this.iteratorEx();

            if (iter.hasNext() == false) {
                return OptionalInt.empty();
            }

            int next = iter.nextInt();

            while (iter.hasNext()) {
                next = iter.nextInt();
            }

            return OptionalInt.of(next);
        } finally {
            close();
        }
    }

    @Override
    public OptionalInt onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final IntIterator iter = this.iteratorEx();

            final OptionalInt result = iter.hasNext() ? OptionalInt.of(iter.nextInt()) : OptionalInt.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextInt()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalInt findAny(final Throwables.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        return findFirst(predicate);
    }

    @Override
    public <E extends Exception, E2 extends Exception> OptionalInt findFirstOrLast(Throwables.IntPredicate<E> predicateForFirst,
            Throwables.IntPredicate<E> predicateForLast) throws E, E2 {
        assertNotClosed();

        try {
            final IntIteratorEx iter = iteratorEx();
            MutableInt last = null;
            int next = 0;

            while (iter.hasNext()) {
                next = iter.nextInt();

                if (predicateForFirst.test(next)) {
                    return OptionalInt.of(next);
                } else if (predicateForLast.test(next)) {
                    if (last == null) {
                        last = MutableInt.of(next);
                    } else {
                        last.setValue(next);
                    }
                }
            }

            return last == null ? OptionalInt.empty() : OptionalInt.of(last.value());
        } finally {
            close();
        }
    }

    @Override
    public Optional<Map<Percentage, Integer>> percentiles() {
        assertNotClosed();

        try {
            final int[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentiles(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> summarizeAndPercentiles() {
        assertNotClosed();

        try {
            final int[] a = sorted().toArray();

            if (N.isNullOrEmpty(a)) {
                return Pair.of(new IntSummaryStatistics(), Optional.<Map<Percentage, Integer>> empty());
            } else {
                return Pair.of(new IntSummaryStatistics(a.length, sum(a), a[0], a[a.length - 1]), Optional.of(N.percentiles(a)));
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
            final IntIteratorEx iter = this.iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextInt());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjIntConsumer<? super R> accumulator) {
        assertNotClosed();

        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }
}
