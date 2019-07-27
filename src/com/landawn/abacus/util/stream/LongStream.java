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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.LongSummaryStatistics;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongBiPredicate;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.LongTernaryOperator;
import com.landawn.abacus.util.function.LongToDoubleFunction;
import com.landawn.abacus.util.function.LongToFloatFunction;
import com.landawn.abacus.util.function.LongToIntFunction;
import com.landawn.abacus.util.function.LongUnaryOperator;
import com.landawn.abacus.util.function.ObjLongConsumer;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToLongFunction;

/** 
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 * 
 * @see Stream 
 */
public abstract class LongStream extends StreamBase<Long, long[], LongPredicate, LongConsumer, LongList, OptionalLong, IndexedLong, LongIterator, LongStream> {

    static final Random RAND = new SecureRandom();

    LongStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, null, closeHandlers);
    }

    public abstract LongStream map(LongUnaryOperator mapper);

    public abstract IntStream mapToInt(LongToIntFunction mapper);

    public abstract FloatStream mapToFloat(LongToFloatFunction mapper);

    public abstract DoubleStream mapToDouble(LongToDoubleFunction mapper);

    public abstract <U> Stream<U> mapToObj(LongFunction<? extends U> mapper);

    public abstract LongStream flatMap(LongFunction<? extends LongStream> mapper);

    public abstract LongStream flattMap(LongFunction<long[]> mapper);

    public abstract IntStream flatMapToInt(LongFunction<? extends IntStream> mapper);

    public abstract FloatStream flatMapToFloat(LongFunction<? extends FloatStream> mapper);

    public abstract DoubleStream flatMapToDouble(LongFunction<? extends DoubleStream> mapper);

    public abstract <T> Stream<T> flatMapToObj(LongFunction<? extends Stream<T>> mapper);

    public abstract <T> Stream<T> flattMapToObj(LongFunction<? extends Collection<T>> mapper);

    public abstract <T> Stream<T> flatMappToObj(LongFunction<T[]> mapper);

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     * 
     * <br />
     * 
     * Returns a stream consisting of results of applying the given function to
     * the ranges created from the source elements.
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     *  
     * @param sameRange a non-interfering, stateless predicate to apply to
     *        the leftmost and next elements which returns true for elements
     *        which belong to the same range.
     * @param mapper a non-interfering, stateless function to apply to the
     *        range borders and produce the resulting element. If value was
     *        not merged to the interval, then mapper will receive the same
     *        value twice, otherwise it will receive the leftmost and the
     *        rightmost values which were merged to the range.
     * @return the new stream
     * @see #collapse(LongBiPredicate, LongBinaryOperator)
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    public abstract LongStream rangeMap(final LongBiPredicate sameRange, final LongBinaryOperator mapper);

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     * 
     * <br />
     * 
     * Returns a stream consisting of results of applying the given function to
     * the ranges created from the source elements.
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     *  
     * @param sameRange a non-interfering, stateless predicate to apply to
     *        the leftmost and next elements which returns true for elements
     *        which belong to the same range.
     * @param mapper a non-interfering, stateless function to apply to the
     *        range borders and produce the resulting element. If value was
     *        not merged to the interval, then mapper will receive the same
     *        value twice, otherwise it will receive the leftmost and the
     *        rightmost values which were merged to the range.
     * @return the new stream
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    public abstract <T> Stream<T> rangeMapp(final LongBiPredicate sameRange, final LongBiFunction<T> mapper);

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param collapsible
     * @return
     */
    @SequentialOnly
    public abstract Stream<LongList> collapse(final LongBiPredicate collapsible);

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param collapsible
     * @param mergeFunction
     * @return
     */
    @SequentialOnly
    public abstract LongStream collapse(final LongBiPredicate collapsible, final LongBinaryOperator mergeFunction);

    /**
     * Returns a {@code Stream} produced by iterative application of a accumulation function
     * to an initial element {@code init} and next element of the current stream.
     * Produces a {@code Stream} consisting of {@code init}, {@code acc(init, value1)},
     * {@code acc(acc(init, value1), value2)}, etc.
     *
     * <p>This is an intermediate operation.
     *
     * <p>Example:
     * <pre>
     * accumulator: (a, b) -&gt; a + b
     * stream: [1, 2, 3, 4, 5]
     * result: [1, 3, 6, 10, 15]
     * </pre>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param accumulator  the accumulation function
     * @return the new stream which has the extract same size as this stream.
     */
    @SequentialOnly
    public abstract LongStream scan(final LongBinaryOperator accumulator);

    /**
     * Returns a {@code Stream} produced by iterative application of a accumulation function
     * to an initial element {@code init} and next element of the current stream.
     * Produces a {@code Stream} consisting of {@code init}, {@code acc(init, value1)},
     * {@code acc(acc(init, value1), value2)}, etc.
     *
     * <p>This is an intermediate operation.
     *
     * <p>Example:
     * <pre>
     * init:10
     * accumulator: (a, b) -&gt; a + b
     * stream: [1, 2, 3, 4, 5]
     * result: [11, 13, 16, 20, 25]
     * </pre>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param init the initial value. it's only used once by <code>accumulator</code> to calculate the fist element in the returned stream. 
     * It will be ignored if this stream is empty and won't be the first element of the returned stream.
     * 
     * @param accumulator  the accumulation function
     * @return the new stream which has the extract same size as this stream.
     */
    @SequentialOnly
    public abstract LongStream scan(final long init, final LongBinaryOperator accumulator);

    /**
     * 
     * @param init
     * @param accumulator
     * @param initIncluded
     * @return
     */
    @SequentialOnly
    public abstract LongStream scan(final long init, final LongBinaryOperator accumulator, final boolean initIncluded);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract LongStream top(int n);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract LongStream top(final int n, Comparator<? super Long> comparator);

    public abstract LongList toLongList();

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public abstract <K, V> Map<K, V> toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    public abstract <K, V> Map<K, V> toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(LongFunction<? extends K> keyMapper, LongFunction<? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public abstract <K, A, D> Map<K, D> toMap(final LongFunction<? extends K> keyMapper, final Collector<Long, A, D> downstream);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public abstract <K, A, D, M extends Map<K, D>> M toMap(final LongFunction<? extends K> keyMapper, final Collector<Long, A, D> downstream,
            final Supplier<? extends M> mapFactory);

    public abstract long reduce(long identity, LongBinaryOperator op);

    public abstract OptionalLong reduce(LongBinaryOperator op);

    public abstract <R> R collect(Supplier<R> supplier, ObjLongConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * 
     * @param supplier
     * @param accumulator
     * @return
     */
    public abstract <R> R collect(Supplier<R> supplier, ObjLongConsumer<? super R> accumulator);

    public abstract <E extends Exception> void forEach(final Try.LongConsumer<E> action) throws E;

    public abstract <E extends Exception> boolean anyMatch(final Try.LongPredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean allMatch(final Try.LongPredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean noneMatch(final Try.LongPredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalLong findFirst(final Try.LongPredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalLong findLast(final Try.LongPredicate<E> predicate) throws E;

    public abstract <E extends Exception, E2 extends Exception> OptionalLong findFirstOrLast(Try.LongPredicate<E> predicateForFirst,
            Try.LongPredicate<E> predicateForLast) throws E, E2;

    public abstract <E extends Exception> OptionalLong findAny(final Try.LongPredicate<E> predicate) throws E;

    public abstract OptionalLong min();

    public abstract OptionalLong max();

    /**
     * 
     * @param k
     * @return OptionalByte.empty() if there is no element or count less than k, otherwise the kth largest element.
     */
    public abstract OptionalLong kthLargest(int k);

    public abstract long sum();

    public abstract OptionalDouble average();

    public abstract LongSummaryStatistics summarize();

    public abstract Pair<LongSummaryStatistics, Optional<Map<Percentage, Long>>> summarizeAndPercentiles();

    /**
     * 
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public abstract LongStream merge(final LongStream b, final LongBiFunction<Nth> nextSelector);

    public abstract LongStream zipWith(LongStream b, LongBinaryOperator zipFunction);

    public abstract LongStream zipWith(LongStream b, LongStream c, LongTernaryOperator zipFunction);

    public abstract LongStream zipWith(LongStream b, long valueForNoneA, long valueForNoneB, LongBinaryOperator zipFunction);

    public abstract LongStream zipWith(LongStream b, LongStream c, long valueForNoneA, long valueForNoneB, long valueForNoneC, LongTernaryOperator zipFunction);

    public abstract FloatStream asFloatStream();

    public abstract DoubleStream asDoubleStream();

    public abstract java.util.stream.LongStream toJdkStream();

    public abstract Stream<Long> boxed();

    /**
     * Remember to close this Stream after the iteration is done, if required.
     * 
     * @return
     */
    @SequentialOnly
    @Override
    public LongIterator iterator() {
        if (isEmptyCloseHandlers(closeHandlers) == false) {
            if (logger.isWarnEnabled()) {
                logger.warn("### Remember to close " + ClassUtil.getSimpleClassName(getClass()));
            }
        }

        return iteratorEx();
    }

    abstract LongIteratorEx iteratorEx();

    @Override
    public <R> R __(Function<? super LongStream, R> transfer) {
        return transfer.apply(this);
    }

    public static LongStream empty() {
        return new ArrayLongStream(N.EMPTY_LONG_ARRAY, true, null);
    }

    @SafeVarargs
    public static LongStream of(final long... a) {
        return N.isNullOrEmpty(a) ? empty() : new ArrayLongStream(a);
    }

    public static LongStream of(final long[] a, final int startIndex, final int endIndex) {
        return N.isNullOrEmpty(a) && (startIndex == 0 && endIndex == 0) ? empty() : new ArrayLongStream(a, startIndex, endIndex);
    }

    public static LongStream of(final Long[] a) {
        return Stream.of(a).mapToLong(Fnn.unboxL());
    }

    public static LongStream of(final Long[] a, final int startIndex, final int endIndex) {
        return Stream.of(a, startIndex, endIndex).mapToLong(Fnn.unboxL());
    }

    public static LongStream of(final Collection<Long> c) {
        return Stream.of(c).mapToLong(Fnn.unboxL());
    }

    public static LongStream of(final LongIterator iterator) {
        return iterator == null ? empty() : new IteratorLongStream(iterator);
    }

    /**
     * Lazy evaluation.
     * @param supplier
     * @return
     */
    public static LongStream of(final Supplier<LongList> supplier) {
        final LongIterator iter = new LongIteratorEx() {
            private LongIterator iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null) {
                    init();
                }

                return iterator.hasNext();
            }

            @Override
            public long nextLong() {
                if (iterator == null) {
                    init();
                }

                return iterator.nextLong();
            }

            private void init() {
                final LongList c = supplier.get();

                if (N.isNullOrEmpty(c)) {
                    iterator = LongIterator.empty();
                } else {
                    iterator = c.iterator();
                }
            }
        };

        return of(iter);
    }

    public static LongStream of(final java.util.stream.LongStream stream) {
        return of(new LongIteratorEx() {
            private PrimitiveIterator.OfLong iter = null;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.hasNext();
            }

            @Override
            public long nextLong() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.nextLong();
            }

            @Override
            public long count() {
                return iter == null ? stream.count() : super.count();
            }

            @Override
            public void skip(long n) {
                if (iter == null) {
                    iter = stream.skip(n).iterator();
                } else {
                    super.skip(n);
                }
            }

            @Override
            public long[] toArray() {
                return iter == null ? stream.toArray() : super.toArray();
            }
        }).__(s -> stream.isParallel() ? s.parallel() : s.sequential()).onClose(new Runnable() {
            @Override
            public void run() {
                stream.close();
            }
        });
    }

    private static final Function<long[], LongStream> flatMapper = new Function<long[], LongStream>() {
        @Override
        public LongStream apply(long[] t) {
            return LongStream.of(t);
        }
    };

    private static final Function<long[][], LongStream> flatMappper = new Function<long[][], LongStream>() {
        @Override
        public LongStream apply(long[][] t) {
            return LongStream.flat(t);
        }
    };

    public static LongStream flat(final long[][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToLong(flatMapper);
    }

    public static LongStream flat(final long[][] a, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (vertically == false) {
            return Stream.of(a).flatMapToLong(flatMapper);
        }

        long n = 0;

        for (long[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final LongIterator iter = new LongIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public long nextLong() {
                if (cnt++ >= count) {
                    throw new NoSuchElementException();
                }

                if (rowNum == rows) {
                    rowNum = 0;
                    colNum++;
                }

                while (a[rowNum] == null || colNum >= a[rowNum].length) {
                    if (rowNum < rows - 1) {
                        rowNum++;
                    } else {
                        rowNum = 0;
                        colNum++;
                    }
                }

                return a[rowNum++][colNum];
            }
        };

        return of(iter);
    }

    public static LongStream flat(final long[][] a, final long valueForNone, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (long[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = rows * cols;
        LongIterator iter = null;

        if (vertically) {
            iter = new LongIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public long nextLong() {
                    if (cnt++ >= count) {
                        throw new NoSuchElementException();
                    }

                    if (rowNum == rows) {
                        rowNum = 0;
                        colNum++;
                    }

                    if (a[rowNum] == null || colNum >= a[rowNum].length) {
                        rowNum++;
                        return valueForNone;
                    } else {
                        return a[rowNum++][colNum];
                    }
                }
            };

        } else {
            iter = new LongIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public long nextLong() {
                    if (cnt++ >= count) {
                        throw new NoSuchElementException();
                    }

                    if (colNum >= cols) {
                        colNum = 0;
                        rowNum++;
                    }

                    if (a[rowNum] == null || colNum >= a[rowNum].length) {
                        colNum++;
                        return valueForNone;
                    } else {
                        return a[rowNum][colNum++];
                    }
                }
            };
        }

        return of(iter);
    }

    public static LongStream flat(final long[][][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToLong(flatMappper);
    }

    public static LongStream range(final long startInclusive, final long endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        } else if (endExclusive - startInclusive < 0) {
            final long m = BigInteger.valueOf(endExclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(3)).longValue();
            return concat(range(startInclusive, startInclusive + m), range(startInclusive + m, (startInclusive + m) + m),
                    range((startInclusive + m) + m, endExclusive));
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long next = startInclusive;
            private long cnt = endExclusive - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                return next++;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - n;
                next += n;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public long[] toArray() {
                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static LongStream range(final long startInclusive, final long endExclusive, final long by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return empty();
        }

        if ((by > 0 && endExclusive - startInclusive < 0) || (by < 0 && startInclusive - endExclusive < 0)) {
            long m = BigInteger.valueOf(endExclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(3)).longValue();

            if ((by > 0 && by > m) || (by < 0 && by < m)) {
                return concat(range(startInclusive, startInclusive + by), range(startInclusive + by, endExclusive));
            } else {
                m = m > 0 ? m - m % by : m + m % by;
                return concat(range(startInclusive, startInclusive + m, by), range(startInclusive + m, (startInclusive + m) + m, by),
                        range((startInclusive + m) + m, endExclusive, by));
            }
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long next = startInclusive;
            private long cnt = (endExclusive - startInclusive) / by + ((endExclusive - startInclusive) % by == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                long result = next;
                next += by;
                return result;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - n;
                next += n * by;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public long[] toArray() {
                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static LongStream rangeClosed(final long startInclusive, final long endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        } else if (endInclusive - startInclusive + 1 <= 0) {
            final long m = BigInteger.valueOf(endInclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(3)).longValue();
            return concat(range(startInclusive, startInclusive + m), range(startInclusive + m, (startInclusive + m) + m),
                    rangeClosed((startInclusive + m) + m, endInclusive));
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long next = startInclusive;
            private long cnt = endInclusive - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                return next++;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - n;
                next += n;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public long[] toArray() {
                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static LongStream rangeClosed(final long startInclusive, final long endInclusive, final long by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        } else if (endInclusive > startInclusive != by > 0) {
            return empty();
        }

        if ((by > 0 && endInclusive - startInclusive < 0) || (by < 0 && startInclusive - endInclusive < 0) || ((endInclusive - startInclusive) / by + 1 <= 0)) {
            long m = BigInteger.valueOf(endInclusive).subtract(BigInteger.valueOf(startInclusive)).divide(BigInteger.valueOf(3)).longValue();

            if ((by > 0 && by > m) || (by < 0 && by < m)) {
                return concat(range(startInclusive, startInclusive + by), rangeClosed(startInclusive + by, endInclusive));
            } else {
                m = m > 0 ? m - m % by : m + m % by;
                return concat(range(startInclusive, startInclusive + m, by), range(startInclusive + m, (startInclusive + m) + m, by),
                        rangeClosed((startInclusive + m) + m, endInclusive, by));
            }
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long next = startInclusive;
            private long cnt = (endInclusive - startInclusive) / by + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                long result = next;
                next += by;
                return result;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - n;
                next += n * by;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public long[] toArray() {
                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static LongStream repeat(final long element, final long n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public long nextLong() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                return element;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - (int) n;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public long[] toArray() {
                final long[] result = new long[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = element;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static LongStream random() {
        return generate(new LongSupplier() {
            @Override
            public long getAsLong() {
                return RAND.nextLong();
            }
        });
    }

    public static LongStream iterate(final BooleanSupplier hasNext, final LongSupplier next) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorLongStream(new LongIteratorEx() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public long nextLong() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return next.getAsLong();
            }
        });
    }

    public static LongStream iterate(final long init, final BooleanSupplier hasNext, final LongUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorLongStream(new LongIteratorEx() {
            private long t = 0;
            private boolean isFirst = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public long nextLong() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsLong(t);
                }

                return t;
            }
        });
    }

    /**
     * 
     * @param init
     * @param hasNext test if has next by hasNext.test(init) for first time and hasNext.test(f.apply(previous)) for remaining.
     * @param f
     * @return
     */
    public static LongStream iterate(final long init, final LongPredicate hasNext, final LongUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorLongStream(new LongIteratorEx() {
            private long t = 0;
            private long cur = 0;
            private boolean isFirst = true;
            private boolean hasMore = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false && hasMore) {
                    if (isFirst) {
                        isFirst = false;
                        hasNextVal = hasNext.test(cur = init);
                    } else {
                        hasNextVal = hasNext.test(cur = f.applyAsLong(t));
                    }

                    if (hasNextVal == false) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public long nextLong() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                hasNextVal = false;
                return t;
            }
        });
    }

    public static LongStream iterate(final long init, final LongUnaryOperator f) {
        N.checkArgNotNull(f);

        return new IteratorLongStream(new LongIteratorEx() {
            private long t = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsLong(t);
                }

                return t;
            }
        });
    }

    public static LongStream generate(final LongSupplier s) {
        N.checkArgNotNull(s);

        return new IteratorLongStream(new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                return s.getAsLong();
            }
        });
    }

    /**
     * 
     * @param intervalInMillis
     * @return
     */
    public static LongStream interval(final long intervalInMillis) {
        return interval(0, intervalInMillis);
    }

    /**
     * Generates the long value by the specified period: [0, 1, 2, 3...]
     * 
     * @param delayInMillis
     * @param intervalInMillis
     * @return 
     */
    public static LongStream interval(final long delayInMillis, final long intervalInMillis) {
        return interval(delayInMillis, intervalInMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Generates the long value by the specified period: [0, 1, 2, 3...]
     * 
     * @param delay
     * @param interval
     * @param unit
     * @return
     */
    public static LongStream interval(final long delay, final long interval, final TimeUnit unit) {
        return of(new LongIteratorEx() {
            private final long intervalInMillis = unit.toMillis(interval);
            private long nextTime = System.currentTimeMillis() + unit.toMillis(delay);
            private long val = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public long nextLong() {
                long now = System.currentTimeMillis();

                if (now < nextTime) {
                    N.sleep(nextTime - now);
                }

                nextTime += intervalInMillis;

                return val++;
            }
        });
    }

    @SafeVarargs
    public static LongStream concat(final long[]... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorLongStream(new LongIteratorEx() {
            private final Iterator<long[]> iter = N.asList(a).iterator();
            private LongIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = LongIteratorEx.of(iter.next());
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
        });
    }

    @SafeVarargs
    public static LongStream concat(final LongIterator... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorLongStream(new LongIteratorEx() {
            private final Iterator<? extends LongIterator> iter = N.asList(a).iterator();
            private LongIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
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
        });
    }

    @SafeVarargs
    public static LongStream concat(final LongStream... a) {
        return N.isNullOrEmpty(a) ? empty() : concat(N.asList(a));
    }

    public static LongStream concat(final Collection<? extends LongStream> c) {
        return N.isNullOrEmpty(c) ? empty() : new IteratorLongStream(new LongIteratorEx() {
            private final Iterator<? extends LongStream> iterators = c.iterator();
            private LongStream cur;
            private LongIterator iter;

            @Override
            public boolean hasNext() {
                while ((iter == null || iter.hasNext() == false) && iterators.hasNext()) {
                    if (cur != null) {
                        cur.close();
                    }

                    cur = iterators.next();
                    iter = cur.iteratorEx();
                }

                return iter != null && iter.hasNext();
            }

            @Override
            public long nextLong() {
                if ((iter == null || iter.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return iter.nextLong();
            }
        }).onClose(newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static LongStream zip(final long[] a, final long[] b, final LongBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsLong(a[cursor], b[cursor++]);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static LongStream zip(final long[] a, final long[] b, final long[] c, final LongTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b) || N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsLong(a[cursor], b[cursor], c[cursor++]);
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static LongStream zip(final LongIterator a, final LongIterator b, final LongBinaryOperator zipFunction) {
        return new IteratorLongStream(new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public long nextLong() {
                return zipFunction.applyAsLong(a.nextLong(), b.nextLong());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static LongStream zip(final LongIterator a, final LongIterator b, final LongIterator c, final LongTernaryOperator zipFunction) {
        return new IteratorLongStream(new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public long nextLong() {
                return zipFunction.applyAsLong(a.nextLong(), b.nextLong(), c.nextLong());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static LongStream zip(final LongStream a, final LongStream b, final LongBinaryOperator zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static LongStream zip(final LongStream a, final LongStream b, final LongStream c, final LongTernaryOperator zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    public static LongStream zip(final Collection<? extends LongStream> c, final LongNFunction<Long> zipFunction) {
        return Stream.zip(c, zipFunction).mapToLong(ToLongFunction.UNBOX);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static LongStream zip(final long[] a, final long[] b, final long valueForNoneA, final long valueForNoneB, final LongBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private long ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsLong(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static LongStream zip(final long[] a, final long[] b, final long[] c, final long valueForNoneA, final long valueForNoneB, final long valueForNoneC,
            final LongTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b) && N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private long ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public long nextLong() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsLong(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
                        cursor < cLen ? c[cursor] : valueForNoneC);
                cursor++;
                return ret;
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static LongStream zip(final LongIterator a, final LongIterator b, final long valueForNoneA, final long valueForNoneB,
            final LongBinaryOperator zipFunction) {
        return new IteratorLongStream(new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public long nextLong() {
                if (a.hasNext()) {
                    return zipFunction.applyAsLong(a.nextLong(), b.hasNext() ? b.nextLong() : valueForNoneB);
                } else {
                    return zipFunction.applyAsLong(valueForNoneA, b.nextLong());
                }
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static LongStream zip(final LongIterator a, final LongIterator b, final LongIterator c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTernaryOperator zipFunction) {
        return new IteratorLongStream(new LongIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public long nextLong() {
                if (a.hasNext()) {
                    return zipFunction.applyAsLong(a.nextLong(), b.hasNext() ? b.nextLong() : valueForNoneB, c.hasNext() ? c.nextLong() : valueForNoneC);
                } else if (b.hasNext()) {
                    return zipFunction.applyAsLong(valueForNoneA, b.nextLong(), c.hasNext() ? c.nextLong() : valueForNoneC);
                } else {
                    return zipFunction.applyAsLong(valueForNoneA, valueForNoneB, c.nextLong());
                }
            }
        });
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static LongStream zip(final LongStream a, final LongStream b, final long valueForNoneA, final long valueForNoneB,
            final LongBinaryOperator zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static LongStream zip(final LongStream a, final LongStream b, final LongStream c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTernaryOperator zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    /**
     * Zip together the iterators until all of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    public static LongStream zip(final Collection<? extends LongStream> c, final long[] valuesForNone, final LongNFunction<Long> zipFunction) {
        return Stream.zip(c, valuesForNone, zipFunction).mapToLong(ToLongFunction.UNBOX);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static LongStream merge(final long[] a, final long[] b, final LongBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return new IteratorLongStream(new LongIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public long nextLong() {
                if (cursorA < lenA) {
                    if (cursorB < lenB) {
                        if (nextSelector.apply(a[cursorA], b[cursorB]) == Nth.FIRST) {
                            return a[cursorA++];
                        } else {
                            return b[cursorB++];
                        }
                    } else {
                        return a[cursorA++];
                    }
                } else if (cursorB < lenB) {
                    return b[cursorB++];
                } else {
                    throw new NoSuchElementException();
                }
            }
        });
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static LongStream merge(final long[] a, final long[] b, final long[] c, final LongBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), LongStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static LongStream merge(final LongIterator a, final LongIterator b, final LongBiFunction<Nth> nextSelector) {
        return new IteratorLongStream(new LongIteratorEx() {
            private long nextA = 0;
            private long nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public long nextLong() {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.nextLong())) == Nth.FIRST) {
                            hasNextA = false;
                            hasNextB = true;
                            return nextA;
                        } else {
                            return nextB;
                        }
                    } else {
                        hasNextA = false;
                        return nextA;
                    }
                } else if (hasNextB) {
                    if (a.hasNext()) {
                        if (nextSelector.apply((nextA = a.nextLong()), nextB) == Nth.FIRST) {
                            return nextA;
                        } else {
                            hasNextA = true;
                            hasNextB = false;
                            return nextB;
                        }
                    } else {
                        hasNextB = false;
                        return nextB;
                    }
                } else if (a.hasNext()) {
                    if (b.hasNext()) {
                        if (nextSelector.apply((nextA = a.nextLong()), (nextB = b.nextLong())) == Nth.FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.nextLong();
                    }
                } else if (b.hasNext()) {
                    return b.nextLong();
                } else {
                    throw new NoSuchElementException();
                }
            }
        });
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static LongStream merge(final LongIterator a, final LongIterator b, final LongIterator c, final LongBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static LongStream merge(final LongStream a, final LongStream b, final LongBiFunction<Nth> nextSelector) {
        return merge(a.iteratorEx(), b.iteratorEx(), nextSelector).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static LongStream merge(final LongStream a, final LongStream b, final LongStream c, final LongBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static LongStream merge(final Collection<? extends LongStream> c, final LongBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends LongStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends LongStream> iter = c.iterator();
        LongStream result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result, iter.next(), nextSelector);
        }

        return result;
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static LongStream parallelMerge(final Collection<? extends LongStream> c, final LongBiFunction<Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static LongStream parallelMerge(final Collection<? extends LongStream> c, final LongBiFunction<Nth> nextSelector, final int maxThreadNum) {
        N.checkArgument(maxThreadNum > 0, "'maxThreadNum' must not less than 1");

        if (maxThreadNum <= 1) {
            return merge(c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends LongStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        } else if (c.size() == 3) {
            final Iterator<? extends LongStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), iter.next(), nextSelector);
        }

        final Queue<LongStream> queue = N.newLinkedList();

        for (LongStream e : c) {
            queue.add(e);
        }

        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    LongStream a = null;
                    LongStream b = null;
                    LongStream c = null;

                    try {
                        while (eHolder.value() == null) {
                            synchronized (queue) {
                                if (cnt.intValue() > 2 && queue.size() > 1) {
                                    a = queue.poll();
                                    b = queue.poll();

                                    cnt.decrement();
                                } else {
                                    break;
                                }
                            }

                            c = LongStream.of(merge(a, b, nextSelector).toArray());

                            synchronized (queue) {
                                queue.offer(c);
                            }
                        }
                    } catch (Exception e) {
                        setError(eHolder, e);
                    }
                }
            }));
        }

        try {
            complete(futureList, eHolder);
        } finally {
            if (eHolder.value() != null) {
                IOUtil.closeAllQuietly(c);
            }
        }

        return merge(queue.poll(), queue.poll(), nextSelector);
    }

    public static abstract class LongStreamEx extends LongStream {
        private LongStreamEx(boolean sorted, Collection<Runnable> closeHandlers) {
            super(sorted, closeHandlers);
            // Factory class.
        }
    }
}
