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

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedShort;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.ShortSummaryStatistics;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBiPredicate;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.ShortSupplier;
import com.landawn.abacus.util.function.ShortTernaryOperator;
import com.landawn.abacus.util.function.ShortToIntFunction;
import com.landawn.abacus.util.function.ShortUnaryOperator;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToShortFunction;

/** 
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 * 
 * @see Stream
 */
public abstract class ShortStream
        extends StreamBase<Short, short[], ShortPredicate, ShortConsumer, ShortList, OptionalShort, IndexedShort, ShortIterator, ShortStream> {

    static final Random RAND = new SecureRandom();

    ShortStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, null, closeHandlers);
    }

    public abstract ShortStream map(ShortUnaryOperator mapper);

    public abstract IntStream mapToInt(ShortToIntFunction mapper);

    public abstract <U> Stream<U> mapToObj(ShortFunction<? extends U> mapper);

    public abstract ShortStream flatMap(ShortFunction<? extends ShortStream> mapper);

    public abstract ShortStream flattMap(ShortFunction<short[]> mapper);

    public abstract IntStream flatMapToInt(ShortFunction<? extends IntStream> mapper);

    public abstract <T> Stream<T> flatMapToObj(ShortFunction<? extends Stream<T>> mapper);

    public abstract <T> Stream<T> flattMapToObj(ShortFunction<? extends Collection<T>> mapper);

    public abstract <T> Stream<T> flatMappToObj(ShortFunction<T[]> mapper);

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
     * @see #collapse(ShortBiPredicate, ShortBinaryOperator)
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    public abstract ShortStream rangeMap(final ShortBiPredicate sameRange, final ShortBinaryOperator mapper);

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
    public abstract <T> Stream<T> rangeMapp(final ShortBiPredicate sameRange, final ShortBiFunction<T> mapper);

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
    public abstract Stream<ShortList> collapse(final ShortBiPredicate collapsible);

    public abstract ShortStream collapse(final ShortBiPredicate collapsible, final ShortBinaryOperator mergeFunction);

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
    public abstract ShortStream scan(final ShortBinaryOperator accumulator);

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
    public abstract ShortStream scan(final short init, final ShortBinaryOperator accumulator);

    /**
     * 
     * @param init
     * @param accumulator
     * @param initIncluded
     * @return
     */
    @SequentialOnly
    public abstract ShortStream scan(final short init, final ShortBinaryOperator accumulator, final boolean initIncluded);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract ShortStream top(int n);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract ShortStream top(final int n, Comparator<? super Short> comparator);

    public abstract ShortList toShortList();

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public abstract <K, V> Map<K, V> toMap(ShortFunction<? extends K> keyMapper, ShortFunction<? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(ShortFunction<? extends K> keyMapper, ShortFunction<? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    public abstract <K, V> Map<K, V> toMap(ShortFunction<? extends K> keyMapper, ShortFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(ShortFunction<? extends K> keyMapper, ShortFunction<? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public abstract <K, A, D> Map<K, D> toMap(final ShortFunction<? extends K> keyMapper, final Collector<Short, A, D> downstream);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public abstract <K, A, D, M extends Map<K, D>> M toMap(final ShortFunction<? extends K> keyMapper, final Collector<Short, A, D> downstream,
            final Supplier<? extends M> mapFactory);

    public abstract short reduce(short identity, ShortBinaryOperator op);

    public abstract OptionalShort reduce(ShortBinaryOperator op);

    public abstract <R> R collect(Supplier<R> supplier, ObjShortConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * 
     * @param supplier
     * @param accumulator
     * @return
     */
    public abstract <R> R collect(Supplier<R> supplier, ObjShortConsumer<? super R> accumulator);

    public abstract <E extends Exception> void forEach(final Try.ShortConsumer<E> action) throws E;

    public abstract <E extends Exception> boolean anyMatch(final Try.ShortPredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean allMatch(final Try.ShortPredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean noneMatch(final Try.ShortPredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalShort findFirst(final Try.ShortPredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalShort findLast(final Try.ShortPredicate<E> predicate) throws E;

    public abstract <E extends Exception, E2 extends Exception> OptionalShort findFirstOrLast(Try.ShortPredicate<E> predicateForFirst,
            Try.ShortPredicate<E> predicateForLast) throws E, E2;

    public abstract <E extends Exception> OptionalShort findAny(final Try.ShortPredicate<E> predicate) throws E;

    public abstract OptionalShort min();

    public abstract OptionalShort max();

    /**
     * 
     * @param k
     * @return OptionalByte.empty() if there is no element or count less than k, otherwise the kth largest element.
     */
    public abstract OptionalShort kthLargest(int k);

    public abstract int sum();

    public abstract OptionalDouble average();

    public abstract ShortSummaryStatistics summarize();

    public abstract Pair<ShortSummaryStatistics, Optional<Map<Percentage, Short>>> summarizeAndPercentiles();

    /**
     * 
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public abstract ShortStream merge(final ShortStream b, final ShortBiFunction<Nth> nextSelector);

    public abstract ShortStream zipWith(ShortStream b, ShortBinaryOperator zipFunction);

    public abstract ShortStream zipWith(ShortStream b, ShortStream c, ShortTernaryOperator zipFunction);

    public abstract ShortStream zipWith(ShortStream b, short valueForNoneA, short valueForNoneB, ShortBinaryOperator zipFunction);

    public abstract ShortStream zipWith(ShortStream b, ShortStream c, short valueForNoneA, short valueForNoneB, short valueForNoneC,
            ShortTernaryOperator zipFunction);

    public abstract IntStream asIntStream();

    public abstract Stream<Short> boxed();

    /**
     * Remember to close this Stream after the iteration is done, if required.
     * 
     * @return
     */
    @SequentialOnly
    @Override
    public ShortIterator iterator() {
        if (isEmptyCloseHandlers(closeHandlers) == false) {
            if (logger.isWarnEnabled()) {
                logger.warn("### Remember to close " + ClassUtil.getSimpleClassName(getClass()));
            }
        }

        return iteratorEx();
    }

    abstract ShortIteratorEx iteratorEx();

    @Override
    public <R> R __(Function<? super ShortStream, R> transfer) {
        return transfer.apply(this);
    }

    public static ShortStream empty() {
        return new ArrayShortStream(N.EMPTY_SHORT_ARRAY, true, null);
    }

    @SafeVarargs
    public static ShortStream of(final short... a) {
        return N.isNullOrEmpty(a) ? empty() : new ArrayShortStream(a);
    }

    public static ShortStream of(final short[] a, final int startIndex, final int endIndex) {
        return N.isNullOrEmpty(a) && (startIndex == 0 && endIndex == 0) ? empty() : new ArrayShortStream(a, startIndex, endIndex);
    }

    public static ShortStream of(final Short[] a) {
        return Stream.of(a).mapToShort(Fnn.unboxS());
    }

    public static ShortStream of(final Short[] a, final int startIndex, final int endIndex) {
        return Stream.of(a, startIndex, endIndex).mapToShort(Fnn.unboxS());
    }

    public static ShortStream of(final Collection<Short> c) {
        return Stream.of(c).mapToShort(Fnn.unboxS());
    }

    public static ShortStream of(final ShortIterator iterator) {
        return iterator == null ? empty() : new IteratorShortStream(iterator);
    }

    /**
     * Lazy evaluation.
     * @param supplier
     * @return
     */
    public static ShortStream of(final Supplier<ShortList> supplier) {
        final ShortIterator iter = new ShortIteratorEx() {
            private ShortIterator iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null) {
                    init();
                }

                return iterator.hasNext();
            }

            @Override
            public short nextShort() {
                if (iterator == null) {
                    init();
                }

                return iterator.nextShort();
            }

            private void init() {
                final ShortList c = supplier.get();

                if (N.isNullOrEmpty(c)) {
                    iterator = ShortIterator.empty();
                } else {
                    iterator = c.iterator();
                }
            }
        };

        return of(iter);
    }

    private static final Function<short[], ShortStream> flatMapper = new Function<short[], ShortStream>() {
        @Override
        public ShortStream apply(short[] t) {
            return ShortStream.of(t);
        }
    };

    private static final Function<short[][], ShortStream> flatMappper = new Function<short[][], ShortStream>() {
        @Override
        public ShortStream apply(short[][] t) {
            return ShortStream.flat(t);
        }
    };

    public static ShortStream flat(final short[][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToShort(flatMapper);
    }

    public static ShortStream flat(final short[][] a, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (vertically == false) {
            return Stream.of(a).flatMapToShort(flatMapper);
        }

        long n = 0;

        for (short[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final ShortIterator iter = new ShortIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public short nextShort() {
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

    public static ShortStream flat(final short[][] a, final short valueForNone, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (short[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = rows * cols;
        ShortIterator iter = null;

        if (vertically) {
            iter = new ShortIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public short nextShort() {
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
            iter = new ShortIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public short nextShort() {
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

    public static ShortStream flat(final short[][][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToShort(flatMappper);
    }

    public static ShortStream range(final short startInclusive, final short endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private short next = startInclusive;
            private int cnt = endExclusive * 1 - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                return next++;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - (int) n;
                next += n;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public short[] toArray() {
                final short[] result = new short[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static ShortStream range(final short startInclusive, final short endExclusive, final short by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private short next = startInclusive;
            private int cnt = (endExclusive * 1 - startInclusive) / by + ((endExclusive * 1 - startInclusive) % by == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                short result = next;
                next += by;
                return result;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - (int) n;
                next += n * by;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public short[] toArray() {
                final short[] result = new short[cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static ShortStream rangeClosed(final short startInclusive, final short endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private short next = startInclusive;
            private int cnt = endInclusive * 1 - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                return next++;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - (int) n;
                next += n;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public short[] toArray() {
                final short[] result = new short[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static ShortStream rangeClosed(final short startInclusive, final short endInclusive, final short by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        } else if (endInclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private short next = startInclusive;
            private int cnt = (endInclusive * 1 - startInclusive) / by + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                short result = next;
                next += by;
                return result;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cnt = n >= cnt ? 0 : cnt - (int) n;
                next += n * by;
            }

            @Override
            public long count() {
                return cnt;
            }

            @Override
            public short[] toArray() {
                final short[] result = new short[cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static ShortStream repeat(final short element, final long n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public short nextShort() {
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
            public short[] toArray() {
                final short[] result = new short[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = element;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static ShortStream random() {
        final int bound = Short.MAX_VALUE - Short.MIN_VALUE + 1;

        return generate(new ShortSupplier() {
            @Override
            public short getAsShort() {
                return (short) (RAND.nextInt(bound) + Short.MIN_VALUE);
            }
        });
    }

    public static ShortStream iterate(final BooleanSupplier hasNext, final ShortSupplier next) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorShortStream(new ShortIteratorEx() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public short nextShort() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return next.getAsShort();
            }
        });
    }

    public static ShortStream iterate(final short init, final BooleanSupplier hasNext, final ShortUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorShortStream(new ShortIteratorEx() {
            private short t = 0;
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
            public short nextShort() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsShort(t);
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
    public static ShortStream iterate(final short init, final ShortPredicate hasNext, final ShortUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorShortStream(new ShortIteratorEx() {
            private short t = 0;
            private short cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsShort(t));
                    }

                    if (hasNextVal == false) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public short nextShort() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                hasNextVal = false;
                return t;
            }
        });
    }

    public static ShortStream iterate(final short init, final ShortUnaryOperator f) {
        N.checkArgNotNull(f);

        return new IteratorShortStream(new ShortIteratorEx() {
            private short t = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public short nextShort() {
                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsShort(t);
                }

                return t;
            }
        });
    }

    public static ShortStream generate(final ShortSupplier s) {
        N.checkArgNotNull(s);

        return new IteratorShortStream(new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public short nextShort() {
                return s.getAsShort();
            }
        });
    }

    @SafeVarargs
    public static ShortStream concat(final short[]... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorShortStream(new ShortIteratorEx() {
            private final Iterator<short[]> iter = N.asList(a).iterator();
            private ShortIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = ShortIteratorEx.of(iter.next());
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
        });
    }

    @SafeVarargs
    public static ShortStream concat(final ShortIterator... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorShortStream(new ShortIteratorEx() {
            private final Iterator<? extends ShortIterator> iter = N.asList(a).iterator();
            private ShortIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
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
        });
    }

    @SafeVarargs
    public static ShortStream concat(final ShortStream... a) {
        return N.isNullOrEmpty(a) ? empty() : concat(N.asList(a));
    }

    public static ShortStream concat(final Collection<? extends ShortStream> c) {
        return N.isNullOrEmpty(c) ? empty() : new IteratorShortStream(new ShortIteratorEx() {
            private final Iterator<? extends ShortStream> iterators = c.iterator();
            private ShortStream cur;
            private ShortIterator iter;

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
            public short nextShort() {
                if ((iter == null || iter.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return iter.nextShort();
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
    public static ShortStream zip(final short[] a, final short[] b, final ShortBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public short nextShort() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsShort(a[cursor], b[cursor++]);
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
    public static ShortStream zip(final short[] a, final short[] b, final short[] c, final ShortTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b) || N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public short nextShort() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsShort(a[cursor], b[cursor], c[cursor++]);
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
    public static ShortStream zip(final ShortIterator a, final ShortIterator b, final ShortBinaryOperator zipFunction) {
        return new IteratorShortStream(new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public short nextShort() {
                return zipFunction.applyAsShort(a.nextShort(), b.nextShort());
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
    public static ShortStream zip(final ShortIterator a, final ShortIterator b, final ShortIterator c, final ShortTernaryOperator zipFunction) {
        return new IteratorShortStream(new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public short nextShort() {
                return zipFunction.applyAsShort(a.nextShort(), b.nextShort(), c.nextShort());
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
    public static ShortStream zip(final ShortStream a, final ShortStream b, final ShortBinaryOperator zipFunction) {
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
    public static ShortStream zip(final ShortStream a, final ShortStream b, final ShortStream c, final ShortTernaryOperator zipFunction) {
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
    public static ShortStream zip(final Collection<? extends ShortStream> c, final ShortNFunction<Short> zipFunction) {
        return Stream.zip(c, zipFunction).mapToShort(ToShortFunction.UNBOX);
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
    public static ShortStream zip(final short[] a, final short[] b, final short valueForNoneA, final short valueForNoneB,
            final ShortBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private short ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public short nextShort() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsShort(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
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
    public static ShortStream zip(final short[] a, final short[] b, final short[] c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b) && N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private short ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public short nextShort() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsShort(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
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
    public static ShortStream zip(final ShortIterator a, final ShortIterator b, final short valueForNoneA, final short valueForNoneB,
            final ShortBinaryOperator zipFunction) {
        return new IteratorShortStream(new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public short nextShort() {
                if (a.hasNext()) {
                    return zipFunction.applyAsShort(a.nextShort(), b.hasNext() ? b.nextShort() : valueForNoneB);
                } else {
                    return zipFunction.applyAsShort(valueForNoneA, b.nextShort());
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
    public static ShortStream zip(final ShortIterator a, final ShortIterator b, final ShortIterator c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTernaryOperator zipFunction) {
        return new IteratorShortStream(new ShortIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public short nextShort() {
                if (a.hasNext()) {
                    return zipFunction.applyAsShort(a.nextShort(), b.hasNext() ? b.nextShort() : valueForNoneB, c.hasNext() ? c.nextShort() : valueForNoneC);
                } else if (b.hasNext()) {
                    return zipFunction.applyAsShort(valueForNoneA, b.nextShort(), c.hasNext() ? c.nextShort() : valueForNoneC);
                } else {
                    return zipFunction.applyAsShort(valueForNoneA, valueForNoneB, c.nextShort());
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
    public static ShortStream zip(final ShortStream a, final ShortStream b, final short valueForNoneA, final short valueForNoneB,
            final ShortBinaryOperator zipFunction) {
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
    public static ShortStream zip(final ShortStream a, final ShortStream b, final ShortStream c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTernaryOperator zipFunction) {
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
    public static ShortStream zip(final Collection<? extends ShortStream> c, final short[] valuesForNone, final ShortNFunction<Short> zipFunction) {
        return Stream.zip(c, valuesForNone, zipFunction).mapToShort(ToShortFunction.UNBOX);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static ShortStream merge(final short[] a, final short[] b, final ShortBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return new IteratorShortStream(new ShortIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public short nextShort() {
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
    public static ShortStream merge(final short[] a, final short[] b, final short[] c, final ShortBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), ShortStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static ShortStream merge(final ShortIterator a, final ShortIterator b, final ShortBiFunction<Nth> nextSelector) {
        return new IteratorShortStream(new ShortIteratorEx() {
            private short nextA = 0;
            private short nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public short nextShort() {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.nextShort())) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.nextShort()), nextB) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.nextShort()), (nextB = b.nextShort())) == Nth.FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.nextShort();
                    }
                } else if (b.hasNext()) {
                    return b.nextShort();
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
    public static ShortStream merge(final ShortIterator a, final ShortIterator b, final ShortIterator c, final ShortBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static ShortStream merge(final ShortStream a, final ShortStream b, final ShortBiFunction<Nth> nextSelector) {
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
    public static ShortStream merge(final ShortStream a, final ShortStream b, final ShortStream c, final ShortBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static ShortStream merge(final Collection<? extends ShortStream> c, final ShortBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends ShortStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends ShortStream> iter = c.iterator();
        ShortStream result = merge(iter.next(), iter.next(), nextSelector);

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
    public static ShortStream parallelMerge(final Collection<? extends ShortStream> c, final ShortBiFunction<Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static ShortStream parallelMerge(final Collection<? extends ShortStream> c, final ShortBiFunction<Nth> nextSelector, final int maxThreadNum) {
        N.checkArgument(maxThreadNum > 0, "'maxThreadNum' must not less than 1");

        if (maxThreadNum <= 1) {
            return merge(c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends ShortStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        } else if (c.size() == 3) {
            final Iterator<? extends ShortStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), iter.next(), nextSelector);
        }

        final Queue<ShortStream> queue = N.newLinkedList();

        for (ShortStream e : c) {
            queue.add(e);
        }

        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    ShortStream a = null;
                    ShortStream b = null;
                    ShortStream c = null;

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

                            c = ShortStream.of(merge(a, b, nextSelector).toArray());

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
}
