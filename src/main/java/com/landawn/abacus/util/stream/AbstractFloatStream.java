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
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.KahanSummation;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableFloat;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatBiPredicate;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatTernaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjFloatConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToFloatFunction;

/**
 * 
 */
abstract class AbstractFloatStream extends FloatStream {

    AbstractFloatStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, closeHandlers);
    }

    @Override
    public FloatStream distinct() {
        final Set<Object> set = new HashSet<>();

        return newStream(this.sequential().filter(new FloatPredicate() {
            @Override
            public boolean test(float value) {
                return set.add(value);
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public FloatStream flattMap(final FloatFunction<float[]> mapper) {
        return flatMap(new FloatFunction<FloatStream>() {
            @Override
            public FloatStream apply(float t) {
                return FloatStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flattMapToObj(final FloatFunction<? extends Collection<T>> mapper) {
        return flatMapToObj(new FloatFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(float t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <T> Stream<T> flatMappToObj(final FloatFunction<T[]> mapper) {
        return flatMapToObj(new FloatFunction<Stream<T>>() {
            @Override
            public Stream<T> apply(float t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public FloatStream rangeMap(final FloatBiPredicate sameRange, final FloatBinaryOperator mapper) {
        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() {
            private float left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public float nextFloat() {
                left = hasNext ? next : iter.nextFloat();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextFloat();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.applyAsFloat(left, right);
            }
        }, false);
    }

    @Override
    public <T> Stream<T> rangeMapp(final FloatBiPredicate sameRange, final FloatBiFunction<T> mapper) {
        final FloatIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<T>() {
            private float left = 0, right = 0, next = 0;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                left = hasNext ? next : iter.nextFloat();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.nextFloat();

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
    public Stream<FloatList> collapse(final FloatBiPredicate collapsible) {
        final FloatIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<FloatList>() {
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public FloatList next() {
                final FloatList result = new FloatList(9);
                result.add(hasNext ? next : (next = iter.nextFloat()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextFloat()))) {
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
    public FloatStream collapse(final FloatBiPredicate collapsible, final FloatBinaryOperator mergeFunction) {
        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() {
            private boolean hasNext = false;
            private float next = 0;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public float nextFloat() {
                float res = hasNext ? next : (next = iter.nextFloat());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.nextFloat()))) {
                        res = mergeFunction.applyAsFloat(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false);
    }

    @Override
    public FloatStream skip(final long n, final FloatConsumer action) {
        final FloatPredicate filter = isParallel() ? new FloatPredicate() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(float value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new FloatPredicate() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(float value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public FloatStream removeIf(final FloatPredicate predicate) {
        checkArgNotNull(predicate);

        return filter(new FloatPredicate() {
            @Override
            public boolean test(float value) {
                return predicate.test(value) == false;
            }
        });
    }

    @Override
    public FloatStream removeIf(final FloatPredicate predicate, final FloatConsumer action) {
        checkArgNotNull(predicate);
        checkArgNotNull(predicate);

        return filter(new FloatPredicate() {
            @Override
            public boolean test(float value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public FloatStream dropWhile(final FloatPredicate predicate, final FloatConsumer action) {
        checkArgNotNull(predicate);
        checkArgNotNull(action);

        return dropWhile(new FloatPredicate() {
            @Override
            public boolean test(float value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public FloatStream step(final long step) {
        checkArgPositive(step, "step");

        final long skip = step - 1;
        final FloatIteratorEx iter = this.iteratorEx();

        final FloatIterator floatIterator = new FloatIteratorEx() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                final float next = iter.nextFloat();
                iter.skip(skip);
                return next;
            }
        };

        return newStream(floatIterator, sorted);
    }

    @Override
    public Stream<FloatStream> split(final int chunkSize) {
        return splitToList(chunkSize).map(new Function<FloatList, FloatStream>() {
            @Override
            public FloatStream apply(FloatList t) {
                return new ArrayFloatStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<FloatStream> split(final FloatPredicate predicate) {
        return splitToList(predicate).map(new Function<FloatList, FloatStream>() {
            @Override
            public FloatStream apply(FloatList t) {
                return new ArrayFloatStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public Stream<FloatStream> splitBy(final FloatPredicate where) {
        checkArgNotNull(where);

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new ObjIteratorEx<FloatStream>() {
            private int cursor = 0;
            private float next = 0;
            private boolean hasNext = false;

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

                    while (iter.hasNext()) {
                        next = iter.nextFloat();

                        if (where.test(next)) {
                            list.add(next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }

                    result = new ArrayFloatStream(list.array(), 0, list.size(), sorted, null);
                } else {
                    FloatIteratorEx iterEx = iter;

                    if (hasNext) {
                        iterEx = new FloatIteratorEx() {
                            private boolean isFirst = true;

                            @Override
                            public boolean hasNext() {
                                return isFirst || iter.hasNext();
                            }

                            @Override
                            public float nextFloat() {
                                if (hasNext() == false) {
                                    throw new NoSuchElementException();
                                }

                                if (isFirst) {
                                    isFirst = false;
                                    return next;
                                } else {
                                    return iter.nextFloat();
                                }
                            }
                        };
                    }

                    result = new IteratorFloatStream(iterEx, sorted, null);
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
                            next = iter.nextFloat();

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
    public Stream<FloatStream> sliding(final int windowSize, final int increment) {
        return slidingToList(windowSize, increment).map(new Function<FloatList, FloatStream>() {
            @Override
            public FloatStream apply(FloatList t) {
                return new ArrayFloatStream(t.array(), 0, t.size(), sorted, null);
            }
        });
    }

    @Override
    public FloatStream scan(final FloatBinaryOperator accumulator) {
        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() {
            private float res = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.nextFloat());
                } else {
                    return (res = accumulator.applyAsFloat(res, iter.nextFloat()));
                }
            }
        }, false);
    }

    @Override
    public FloatStream scan(final float init, final FloatBinaryOperator accumulator) {
        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() {
            private float res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public float nextFloat() {
                return (res = accumulator.applyAsFloat(res, iter.nextFloat()));
            }
        }, false);
    }

    @Override
    public FloatStream scan(final float init, final FloatBinaryOperator accumulator, final boolean initIncluded) {
        if (initIncluded == false) {
            return scan(init, accumulator);
        }

        final FloatIteratorEx iter = iteratorEx();

        return newStream(new FloatIteratorEx() {
            private boolean isFirst = true;
            private float res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public float nextFloat() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.applyAsFloat(res, iter.nextFloat()));
            }
        }, false);
    }

    @Override
    public FloatStream top(int n) {
        return top(n, FLOAT_COMPARATOR);
    }

    @Override
    public FloatStream intersection(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new FloatPredicate() {
            @Override
            public boolean test(float value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public FloatStream difference(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new FloatPredicate() {
            @Override
            public boolean test(float value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).iteratorEx(), sorted);
    }

    @Override
    public FloatStream symmetricDifference(final Collection<Float> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new FloatPredicate() {
            @Override
            public boolean test(float value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).append(Stream.of(c).filter(new Predicate<Float>() {
            @Override
            public boolean test(Float value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).mapToFloat(ToFloatFunction.UNBOX)).iteratorEx(), false);
    }

    @Override
    public FloatStream reversed() {
        return newStream(new FloatIteratorEx() {
            private boolean initialized = false;
            private float[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public float nextFloat() {
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
            public float[] toArray() {
                if (initialized == false) {
                    init();
                }

                final float[] a = new float[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractFloatStream.this.toArray();
                    cursor = aar.length;
                }
            }
        }, false);
    }

    @Override
    public FloatStream shuffled(final Random rnd) {
        return lazyLoad(new Function<float[], float[]>() {
            @Override
            public float[] apply(final float[] a) {
                N.shuffle(a, rnd);
                return a;
            }
        }, false);
    }

    @Override
    public FloatStream rotated(final int distance) {
        return newStream(new FloatIteratorEx() {
            private boolean initialized = false;
            private float[] aar;
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
            public float nextFloat() {
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
            public float[] toArray() {
                if (initialized == false) {
                    init();
                }

                final float[] a = new float[len - cnt];

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = aar[(start + i) % len];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractFloatStream.this.toArray();
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
    public FloatStream sorted() {
        if (sorted) {
            return newStream(iteratorEx(), sorted);
        }

        return lazyLoad(new Function<float[], float[]>() {
            @Override
            public float[] apply(final float[] a) {
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
    public FloatStream reverseSorted() {
        return newStream(new FloatIteratorEx() {
            private boolean initialized = false;
            private float[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public float nextFloat() {
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
            public float[] toArray() {
                if (initialized == false) {
                    init();
                }

                final float[] a = new float[cursor];

                for (int i = 0; i < cursor; i++) {
                    a[i] = aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = AbstractFloatStream.this.toArray();

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

    private FloatStream lazyLoad(final Function<float[], float[]> op, final boolean sorted) {
        return newStream(new FloatIteratorEx() {
            private boolean initialized = false;
            private float[] aar;
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
            public float nextFloat() {
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
            public float[] toArray() {
                if (initialized == false) {
                    init();
                }

                final float[] a = new float[len - cursor];

                for (int i = cursor; i < len; i++) {
                    a[i - cursor] = aar[i];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = op.apply(AbstractFloatStream.this.toArray());
                    len = aar.length;
                }
            }
        }, sorted);
    }

    @Override
    public Stream<IndexedFloat> indexed() {
        final MutableLong idx = MutableLong.of(0);

        return newStream(this.sequential().mapToObj(new FloatFunction<IndexedFloat>() {
            @Override
            public IndexedFloat apply(float t) {
                return IndexedFloat.of(t, idx.getAndIncrement());
            }
        }).iteratorEx(), true, INDEXED_FLOAT_COMPARATOR);
    }

    @Override
    public FloatStream append(FloatStream stream) {
        return FloatStream.concat(this, stream);
    }

    @Override
    public FloatStream prepend(FloatStream stream) {
        return FloatStream.concat(stream, this);
    }

    @Override
    public FloatStream merge(FloatStream b, FloatBiFunction<Nth> nextSelector) {
        return FloatStream.merge(this, b, nextSelector);
    }

    @Override
    public FloatStream zipWith(FloatStream b, FloatBinaryOperator zipFunction) {
        return FloatStream.zip(this, b, zipFunction);
    }

    @Override
    public FloatStream zipWith(FloatStream b, FloatStream c, FloatTernaryOperator zipFunction) {
        return FloatStream.zip(this, b, c, zipFunction);
    }

    @Override
    public FloatStream zipWith(FloatStream b, float valueForNoneA, float valueForNoneB, FloatBinaryOperator zipFunction) {
        return FloatStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public FloatStream zipWith(FloatStream b, FloatStream c, float valueForNoneA, float valueForNoneB, float valueForNoneC, FloatTernaryOperator zipFunction) {
        return FloatStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    //    @Override
    //    public FloatStream cached() {
    //        return newStream(toArray(), sorted);
    //    }

    @Override
    public <K, V> Map<K, V> toMap(FloatFunction<? extends K> keyMapper, FloatFunction<? extends V> valueMapper) {
        return toMap(keyMapper, valueMapper, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(FloatFunction<? extends K> keyMapper, FloatFunction<? extends V> valueMapper, Supplier<? extends M> mapFactory) {
        return toMap(keyMapper, valueMapper, Fn.<V> throwingMerger(), mapFactory);
    }

    @Override
    public <K, V> Map<K, V> toMap(FloatFunction<? extends K> keyMapper, FloatFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, A, D> Map<K, D> toMap(FloatFunction<? extends K> keyMapper, Collector<Float, A, D> downstream) {
        return toMap(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public double sum() {
        assertNotClosed();

        try {
            return summation().sum();
        } finally {
            close();
        }
    }

    private KahanSummation summation() {
        final KahanSummation summation = new KahanSummation();

        final FloatConsumer action = new FloatConsumer() {
            @Override
            public void accept(float t) {
                summation.add(t);
            }
        };

        this.forEach(action);
        return summation;
    }

    @Override
    public OptionalDouble average() {
        assertNotClosed();

        try {
            return summation().average();
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat first() {
        assertNotClosed();

        try {
            final FloatIterator iter = this.iteratorEx();

            return iter.hasNext() ? OptionalFloat.of(iter.nextFloat()) : OptionalFloat.empty();
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat last() {
        assertNotClosed();

        try {
            final FloatIterator iter = this.iteratorEx();

            if (iter.hasNext() == false) {
                return OptionalFloat.empty();
            }

            float next = iter.nextFloat();

            while (iter.hasNext()) {
                next = iter.nextFloat();
            }

            return OptionalFloat.of(next);
        } finally {
            close();
        }
    }

    @Override
    public OptionalFloat onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final FloatIterator iter = this.iteratorEx();

            final OptionalFloat result = iter.hasNext() ? OptionalFloat.of(iter.nextFloat()) : OptionalFloat.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.nextFloat()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> OptionalFloat findAny(final Try.FloatPredicate<E> predicate) throws E {
        return findFirst(predicate);
    }

    @Override
    public <E extends Exception, E2 extends Exception> OptionalFloat findFirstOrLast(Try.FloatPredicate<E> predicateForFirst,
            Try.FloatPredicate<E> predicateForLast) throws E, E2 {
        assertNotClosed();

        try {
            final FloatIteratorEx iter = iteratorEx();
            MutableFloat last = null;
            float next = 0;

            while (iter.hasNext()) {
                next = iter.nextFloat();

                if (predicateForFirst.test(next)) {
                    return OptionalFloat.of(next);
                } else if (predicateForLast.test(next)) {
                    if (last == null) {
                        last = MutableFloat.of(next);
                    } else {
                        last.setValue(next);
                    }
                }
            }

            return last == null ? OptionalFloat.empty() : OptionalFloat.of(last.value());
        } finally {
            close();
        }
    }

    @Override
    public Optional<Map<Percentage, Float>> percentiles() {
        assertNotClosed();

        try {
            final float[] a = sorted().toArray();

            if (a.length == 0) {
                return Optional.empty();
            }

            return Optional.of(N.percentiles(a));
        } finally {
            close();
        }
    }

    @Override
    public Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> summarizeAndPercentiles() {
        assertNotClosed();

        try {
            final float[] a = sorted().toArray();

            if (N.isNullOrEmpty(a)) {
                return Pair.of(new FloatSummaryStatistics(), Optional.<Map<Percentage, Float>> empty());
            } else {
                return Pair.of(new FloatSummaryStatistics(a.length, sum(a), a[0], a[a.length - 1]), Optional.of(N.percentiles(a)));
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
            final FloatIteratorEx iter = this.iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.nextFloat());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, ObjFloatConsumer<? super R> accumulator) {
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }
}
