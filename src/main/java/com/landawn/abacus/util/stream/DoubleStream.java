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
import java.util.PrimitiveIterator;
import java.util.Queue;
import java.util.Random;

import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.DoubleSummaryStatistics;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoubleBinaryOperator;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoubleFunction;
import com.landawn.abacus.util.function.DoubleNFunction;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.DoubleSupplier;
import com.landawn.abacus.util.function.DoubleTernaryOperator;
import com.landawn.abacus.util.function.DoubleToFloatFunction;
import com.landawn.abacus.util.function.DoubleToIntFunction;
import com.landawn.abacus.util.function.DoubleToLongFunction;
import com.landawn.abacus.util.function.DoubleUnaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjDoubleConsumer;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;

/** 
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 * 
 * @see Stream 
 */
public abstract class DoubleStream
        extends StreamBase<Double, double[], DoublePredicate, DoubleConsumer, DoubleList, OptionalDouble, IndexedDouble, DoubleIterator, DoubleStream> {

    static final Random RAND = new SecureRandom();

    DoubleStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, null, closeHandlers);
    }

    public abstract DoubleStream map(DoubleUnaryOperator mapper);

    public abstract IntStream mapToInt(DoubleToIntFunction mapper);

    public abstract LongStream mapToLong(DoubleToLongFunction mapper);

    public abstract FloatStream mapToFloat(DoubleToFloatFunction mapper);

    public abstract <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper);

    public abstract DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper);

    public abstract DoubleStream flattMap(DoubleFunction<double[]> mapper);

    public abstract IntStream flatMapToInt(DoubleFunction<? extends IntStream> mapper);

    public abstract LongStream flatMapToLong(DoubleFunction<? extends LongStream> mapper);

    public abstract FloatStream flatMapToFloat(DoubleFunction<? extends FloatStream> mapper);

    public abstract <T> Stream<T> flatMapToObj(DoubleFunction<? extends Stream<T>> mapper);

    public abstract <T> Stream<T> flattMapToObj(DoubleFunction<? extends Collection<T>> mapper);

    public abstract <T> Stream<T> flatMappToObj(DoubleFunction<T[]> mapper);

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
     * @see #collapse(DoubleBiPredicate, DoubleBinaryOperator)
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    public abstract DoubleStream rangeMap(final DoubleBiPredicate sameRange, final DoubleBinaryOperator mapper);

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
    public abstract <T> Stream<T> rangeMapp(final DoubleBiPredicate sameRange, final DoubleBiFunction<T> mapper);

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
    public abstract Stream<DoubleList> collapse(final DoubleBiPredicate collapsible);

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
    public abstract DoubleStream collapse(final DoubleBiPredicate collapsible, final DoubleBinaryOperator mergeFunction);

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
    public abstract DoubleStream scan(final DoubleBinaryOperator accumulator);

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
    public abstract DoubleStream scan(final double init, final DoubleBinaryOperator accumulator);

    /**
     * 
     * @param init
     * @param accumulator
     * @param initIncluded
     * @return
     */
    @SequentialOnly
    public abstract DoubleStream scan(final double init, final DoubleBinaryOperator accumulator, final boolean initIncluded);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract DoubleStream top(int n);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @param comparator
     * @return
     */
    @SequentialOnly
    public abstract DoubleStream top(final int n, Comparator<? super Double> comparator);

    public abstract DoubleList toDoubleList();

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public abstract <K, V> Map<K, V> toMap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    public abstract <K, V> Map<K, V> toMap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(DoubleFunction<? extends K> keyMapper, DoubleFunction<? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public abstract <K, A, D> Map<K, D> toMap(final DoubleFunction<? extends K> keyMapper, final Collector<Double, A, D> downstream);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public abstract <K, A, D, M extends Map<K, D>> M toMap(final DoubleFunction<? extends K> keyMapper, final Collector<Double, A, D> downstream,
            final Supplier<? extends M> mapFactory);

    public abstract double reduce(double identity, DoubleBinaryOperator op);

    public abstract OptionalDouble reduce(DoubleBinaryOperator op);

    public abstract <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    public abstract <R> R collect(Supplier<R> supplier, ObjDoubleConsumer<? super R> accumulator);

    public abstract <E extends Exception> void forEach(final Try.DoubleConsumer<E> action) throws E;

    public abstract <E extends Exception> boolean anyMatch(final Try.DoublePredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean allMatch(final Try.DoublePredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean noneMatch(final Try.DoublePredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalDouble findFirst(final Try.DoublePredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalDouble findLast(final Try.DoublePredicate<E> predicate) throws E;

    public abstract <E extends Exception, E2 extends Exception> OptionalDouble findFirstOrLast(Try.DoublePredicate<E> predicateForFirst,
            Try.DoublePredicate<E> predicateForLast) throws E, E2;

    public abstract <E extends Exception> OptionalDouble findAny(final Try.DoublePredicate<E> predicate) throws E;

    public abstract OptionalDouble min();

    public abstract OptionalDouble max();

    /**
     * 
     * @param k
     * @return OptionalByte.empty() if there is no element or count less than k, otherwise the kth largest element.
     */
    public abstract OptionalDouble kthLargest(int k);

    public abstract double sum();

    public abstract OptionalDouble average();

    public abstract DoubleSummaryStatistics summarize();

    public abstract Pair<DoubleSummaryStatistics, Optional<Map<Percentage, Double>>> summarizeAndPercentiles();

    /**
     * 
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public abstract DoubleStream merge(final DoubleStream b, final DoubleBiFunction<Nth> nextSelector);

    public abstract DoubleStream zipWith(DoubleStream b, DoubleBinaryOperator zipFunction);

    public abstract DoubleStream zipWith(DoubleStream b, DoubleStream c, DoubleTernaryOperator zipFunction);

    public abstract DoubleStream zipWith(DoubleStream b, double valueForNoneA, double valueForNoneB, DoubleBinaryOperator zipFunction);

    public abstract DoubleStream zipWith(DoubleStream b, DoubleStream c, double valueForNoneA, double valueForNoneB, double valueForNoneC,
            DoubleTernaryOperator zipFunction);

    public abstract java.util.stream.DoubleStream toJdkStream();

    public abstract Stream<Double> boxed();

    /**
     * Remember to close this Stream after the iteration is done, if required.
     * 
     * @return
     */
    @Override
    public DoubleIterator iterator() {
        if (isEmptyCloseHandlers(closeHandlers) == false) {
            if (logger.isWarnEnabled()) {
                logger.warn("### Remember to close " + ClassUtil.getSimpleClassName(getClass()));
            }
        }

        return iteratorEx();
    }

    abstract DoubleIteratorEx iteratorEx();

    @Override
    public <R> R __(Function<? super DoubleStream, R> transfer) {
        return transfer.apply(this);
    }

    public static DoubleStream empty() {
        return new ArrayDoubleStream(N.EMPTY_DOUBLE_ARRAY, true, null);
    }

    @SafeVarargs
    public static DoubleStream of(final double... a) {
        return N.isNullOrEmpty(a) ? empty() : new ArrayDoubleStream(a);
    }

    public static DoubleStream of(final double[] a, final int startIndex, final int endIndex) {
        return N.isNullOrEmpty(a) && (startIndex == 0 && endIndex == 0) ? empty() : new ArrayDoubleStream(a, startIndex, endIndex);
    }

    public static DoubleStream of(final Double[] a) {
        return Stream.of(a).mapToDouble(Fnn.unboxD());
    }

    public static DoubleStream of(final Double[] a, final int startIndex, final int endIndex) {
        return Stream.of(a, startIndex, endIndex).mapToDouble(Fnn.unboxD());
    }

    public static DoubleStream of(final Collection<Double> c) {
        return Stream.of(c).mapToDouble(Fnn.unboxD());
    }

    public static DoubleStream of(final DoubleIterator iterator) {
        return iterator == null ? empty() : new IteratorDoubleStream(iterator);
    }

    /**
     * Lazy evaluation.
     * @param supplier
     * @return
     */
    public static DoubleStream of(final Supplier<DoubleList> supplier) {
        final DoubleIterator iter = new DoubleIteratorEx() {
            private DoubleIterator iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null) {
                    init();
                }

                return iterator.hasNext();
            }

            @Override
            public double nextDouble() {
                if (iterator == null) {
                    init();
                }

                return iterator.nextDouble();
            }

            private void init() {
                final DoubleList c = supplier.get();

                if (N.isNullOrEmpty(c)) {
                    iterator = DoubleIterator.empty();
                } else {
                    iterator = c.iterator();
                }
            }
        };

        return of(iter);
    }

    private static final Function<double[], DoubleStream> flatMapper = new Function<double[], DoubleStream>() {
        @Override
        public DoubleStream apply(double[] t) {
            return DoubleStream.of(t);
        }
    };

    private static final Function<double[][], DoubleStream> flatMappper = new Function<double[][], DoubleStream>() {
        @Override
        public DoubleStream apply(double[][] t) {
            return DoubleStream.flat(t);
        }
    };

    public static DoubleStream of(final java.util.stream.DoubleStream stream) {
        return of(new DoubleIteratorEx() {
            private PrimitiveIterator.OfDouble iter = null;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.hasNext();
            }

            @Override
            public double nextDouble() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.nextDouble();
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
            public double[] toArray() {
                return iter == null ? stream.toArray() : super.toArray();
            }
        }).__(s -> stream.isParallel() ? s.parallel() : s.sequential()).onClose(new Runnable() {
            @Override
            public void run() {
                stream.close();
            }
        });
    }

    public static DoubleStream flat(final double[][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToDouble(flatMapper);
    }

    public static DoubleStream flat(final double[][] a, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (vertically == false) {
            return Stream.of(a).flatMapToDouble(flatMapper);
        }

        long n = 0;

        for (double[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final DoubleIterator iter = new DoubleIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public double nextDouble() {
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

    public static DoubleStream flat(final double[][] a, final double valueForNone, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (double[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = rows * cols;
        DoubleIterator iter = null;

        if (vertically) {
            iter = new DoubleIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public double nextDouble() {
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
            iter = new DoubleIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public double nextDouble() {
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

    public static DoubleStream flat(final double[][][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToDouble(flatMappper);
    }

    @SafeVarargs
    public static DoubleStream from(final float... a) {
        return N.isNullOrEmpty(a) ? empty() : from(a, 0, a.length);
    }

    public static DoubleStream from(final float[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
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

                return a[cursor++];
            }

            @Override
            public long count() {
                return toIndex - cursor;
            }

            @Override
            public void skip(long n) {
                N.checkArgNotNegative(n, "n");

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            @Override
            public double[] toArray() {
                final double[] result = new double[toIndex - cursor];

                for (int i = cursor; i < toIndex; i++) {
                    result[i - cursor] = a[i];
                }

                return result;
            }
        });
    }

    public static DoubleStream repeat(final double element, final long n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public double nextDouble() {
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
            public double[] toArray() {
                final double[] result = new double[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = element;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static DoubleStream random() {
        return generate(new DoubleSupplier() {
            @Override
            public double getAsDouble() {
                return RAND.nextDouble();
            }
        });
    }

    public static DoubleStream iterate(final BooleanSupplier hasNext, final DoubleSupplier next) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public double nextDouble() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return next.getAsDouble();
            }
        });
    }

    public static DoubleStream iterate(final double init, final BooleanSupplier hasNext, final DoubleUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private double t = 0;
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
            public double nextDouble() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsDouble(t);
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
    public static DoubleStream iterate(final double init, final DoublePredicate hasNext, final DoubleUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private double t = 0;
            private double cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsDouble(t));
                    }

                    if (hasNextVal == false) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public double nextDouble() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                hasNextVal = false;
                return t;
            }
        });
    }

    public static DoubleStream iterate(final double init, final DoubleUnaryOperator f) {
        N.checkArgNotNull(f);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private double t = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double nextDouble() {
                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsDouble(t);
                }

                return t;
            }
        });
    }

    public static DoubleStream generate(final DoubleSupplier s) {
        N.checkArgNotNull(s);

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public double nextDouble() {
                return s.getAsDouble();
            }
        });
    }

    @SafeVarargs
    public static DoubleStream concat(final double[]... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorDoubleStream(new DoubleIteratorEx() {
            private final Iterator<double[]> iter = N.asList(a).iterator();
            private DoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = DoubleIteratorEx.of(iter.next());
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
        });
    }

    @SafeVarargs
    public static DoubleStream concat(final DoubleIterator... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorDoubleStream(new DoubleIteratorEx() {
            private final Iterator<? extends DoubleIterator> iter = N.asList(a).iterator();
            private DoubleIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
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
        });
    }

    @SafeVarargs
    public static DoubleStream concat(final DoubleStream... a) {
        return N.isNullOrEmpty(a) ? empty() : concat(N.asList(a));
    }

    public static DoubleStream concat(final Collection<? extends DoubleStream> c) {
        return N.isNullOrEmpty(c) ? empty() : new IteratorDoubleStream(new DoubleIteratorEx() {
            private final Iterator<? extends DoubleStream> iterators = c.iterator();
            private DoubleStream cur;
            private DoubleIterator iter;

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
            public double nextDouble() {
                if ((iter == null || iter.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return iter.nextDouble();
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
    public static DoubleStream zip(final double[] a, final double[] b, final DoubleBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public double nextDouble() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsDouble(a[cursor], b[cursor++]);
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
    public static DoubleStream zip(final double[] a, final double[] b, final double[] c, final DoubleTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b) || N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public double nextDouble() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsDouble(a[cursor], b[cursor], c[cursor++]);
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
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleBinaryOperator zipFunction) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public double nextDouble() {
                return zipFunction.applyAsDouble(a.nextDouble(), b.nextDouble());
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
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final DoubleTernaryOperator zipFunction) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public double nextDouble() {
                return zipFunction.applyAsDouble(a.nextDouble(), b.nextDouble(), c.nextDouble());
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
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleBinaryOperator zipFunction) {
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
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final DoubleTernaryOperator zipFunction) {
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
    public static DoubleStream zip(final Collection<? extends DoubleStream> c, final DoubleNFunction<Double> zipFunction) {
        return Stream.zip(c, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
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
    public static DoubleStream zip(final double[] a, final double[] b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private double ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public double nextDouble() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsDouble(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
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
    public static DoubleStream zip(final double[] a, final double[] b, final double[] c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b) && N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private double ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public double nextDouble() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsDouble(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
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
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBinaryOperator zipFunction) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public double nextDouble() {
                if (a.hasNext()) {
                    return zipFunction.applyAsDouble(a.nextDouble(), b.hasNext() ? b.nextDouble() : valueForNoneB);
                } else {
                    return zipFunction.applyAsDouble(valueForNoneA, b.nextDouble());
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
    public static DoubleStream zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final double valueForNoneA,
            final double valueForNoneB, final double valueForNoneC, final DoubleTernaryOperator zipFunction) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public double nextDouble() {
                if (a.hasNext()) {
                    return zipFunction.applyAsDouble(a.nextDouble(), b.hasNext() ? b.nextDouble() : valueForNoneB,
                            c.hasNext() ? c.nextDouble() : valueForNoneC);
                } else if (b.hasNext()) {
                    return zipFunction.applyAsDouble(valueForNoneA, b.nextDouble(), c.hasNext() ? c.nextDouble() : valueForNoneC);
                } else {
                    return zipFunction.applyAsDouble(valueForNoneA, valueForNoneB, c.nextDouble());
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
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBinaryOperator zipFunction) {
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
    public static DoubleStream zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTernaryOperator zipFunction) {
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
    public static DoubleStream zip(final Collection<? extends DoubleStream> c, final double[] valuesForNone, final DoubleNFunction<Double> zipFunction) {
        return Stream.zip(c, valuesForNone, zipFunction).mapToDouble(ToDoubleFunction.UNBOX);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final double[] a, final double[] b, final DoubleBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public double nextDouble() {
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
    public static DoubleStream merge(final double[] a, final double[] b, final double[] c, final DoubleBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), DoubleStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final DoubleIterator a, final DoubleIterator b, final DoubleBiFunction<Nth> nextSelector) {
        return new IteratorDoubleStream(new DoubleIteratorEx() {
            private double nextA = 0;
            private double nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public double nextDouble() {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.nextDouble())) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.nextDouble()), nextB) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.nextDouble()), (nextB = b.nextDouble())) == Nth.FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.nextDouble();
                    }
                } else if (b.hasNext()) {
                    return b.nextDouble();
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
    public static DoubleStream merge(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final DoubleBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final DoubleStream a, final DoubleStream b, final DoubleBiFunction<Nth> nextSelector) {
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
    public static DoubleStream merge(final DoubleStream a, final DoubleStream b, final DoubleStream c, final DoubleBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static DoubleStream merge(final Collection<? extends DoubleStream> c, final DoubleBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends DoubleStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends DoubleStream> iter = c.iterator();
        DoubleStream result = merge(iter.next(), iter.next(), nextSelector);

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
    public static DoubleStream parallelMerge(final Collection<? extends DoubleStream> c, final DoubleBiFunction<Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static DoubleStream parallelMerge(final Collection<? extends DoubleStream> c, final DoubleBiFunction<Nth> nextSelector, final int maxThreadNum) {
        N.checkArgument(maxThreadNum > 0, "'maxThreadNum' must not less than 1");

        if (maxThreadNum <= 1) {
            return merge(c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends DoubleStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        } else if (c.size() == 3) {
            final Iterator<? extends DoubleStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), iter.next(), nextSelector);
        }

        final Queue<DoubleStream> queue = N.newLinkedList();

        for (DoubleStream e : c) {
            queue.add(e);
        }

        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    DoubleStream a = null;
                    DoubleStream b = null;
                    DoubleStream c = null;

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

                            c = DoubleStream.of(merge(a, b, nextSelector).toArray());

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

    public static abstract class DoubleStreamEx extends DoubleStream {
        private DoubleStreamEx(boolean sorted, Collection<Runnable> closeHandlers) {
            super(sorted, closeHandlers);
            // Factory class.
        }
    }
}
