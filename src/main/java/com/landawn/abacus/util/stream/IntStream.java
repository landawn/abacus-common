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

import java.lang.reflect.Array;
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
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.IntSummaryStatistics;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntNFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.IntSupplier;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToCharFunction;
import com.landawn.abacus.util.function.IntToDoubleFunction;
import com.landawn.abacus.util.function.IntToFloatFunction;
import com.landawn.abacus.util.function.IntToLongFunction;
import com.landawn.abacus.util.function.IntToShortFunction;
import com.landawn.abacus.util.function.IntUnaryOperator;
import com.landawn.abacus.util.function.ObjIntConsumer;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToIntFunction;

/** 
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 * 
 * @see Stream 
 */
public abstract class IntStream extends StreamBase<Integer, int[], IntPredicate, IntConsumer, IntList, OptionalInt, IndexedInt, IntIterator, IntStream> {

    static final Random RAND = new SecureRandom();

    IntStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, null, closeHandlers);
    }

    public abstract IntStream map(IntUnaryOperator mapper);

    public abstract CharStream mapToChar(IntToCharFunction mapper);

    public abstract ByteStream mapToByte(IntToByteFunction mapper);

    public abstract ShortStream mapToShort(IntToShortFunction mapper);

    public abstract LongStream mapToLong(IntToLongFunction mapper);

    public abstract FloatStream mapToFloat(IntToFloatFunction mapper);

    public abstract DoubleStream mapToDouble(IntToDoubleFunction mapper);

    public abstract <U> Stream<U> mapToObj(IntFunction<? extends U> mapper);

    public abstract IntStream flatMap(IntFunction<? extends IntStream> mapper);

    public abstract IntStream flattMap(IntFunction<int[]> mapper);

    public abstract CharStream flatMapToChar(IntFunction<? extends CharStream> mapper);

    public abstract ByteStream flatMapToByte(IntFunction<? extends ByteStream> mapper);

    public abstract ShortStream flatMapToShort(IntFunction<? extends ShortStream> mapper);

    public abstract LongStream flatMapToLong(IntFunction<? extends LongStream> mapper);

    public abstract FloatStream flatMapToFloat(IntFunction<? extends FloatStream> mapper);

    public abstract DoubleStream flatMapToDouble(IntFunction<? extends DoubleStream> mapper);

    public abstract <T> Stream<T> flatMapToObj(IntFunction<? extends Stream<T>> mapper);

    public abstract <T> Stream<T> flattMapToObj(IntFunction<? extends Collection<T>> mapper);

    public abstract <T> Stream<T> flatMappToObj(IntFunction<T[]> mapper);

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
     * @see #collapse(IntBiPredicate, IntBinaryOperator)
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    public abstract IntStream rangeMap(final IntBiPredicate sameRange, final IntBinaryOperator mapper);

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
    public abstract <T> Stream<T> rangeMapp(final IntBiPredicate sameRange, final IntBiFunction<T> mapper);

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
    public abstract Stream<IntList> collapse(final IntBiPredicate collapsible);

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
    public abstract IntStream collapse(final IntBiPredicate collapsible, final IntBinaryOperator mergeFunction);

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
    public abstract IntStream scan(final IntBinaryOperator accumulator);

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
    public abstract IntStream scan(final int init, final IntBinaryOperator accumulator);

    /**
     * 
     * @param init
     * @param accumulator
     * @param initIncluded
     * @return
     */
    @SequentialOnly
    public abstract IntStream scan(final int init, final IntBinaryOperator accumulator, final boolean initIncluded);

    /**
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract IntStream top(int n);

    /**
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @param comparator
     * @return
     */
    @SequentialOnly
    public abstract IntStream top(final int n, Comparator<? super Integer> comparator);

    public abstract IntList toIntList();

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public abstract <K, V> Map<K, V> toMap(IntFunction<? extends K> keyMapper, IntFunction<? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(IntFunction<? extends K> keyMapper, IntFunction<? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    public abstract <K, V> Map<K, V> toMap(IntFunction<? extends K> keyMapper, IntFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(IntFunction<? extends K> keyMapper, IntFunction<? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public abstract <K, A, D> Map<K, D> toMap(final IntFunction<? extends K> keyMapper, final Collector<Integer, A, D> downstream);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public abstract <K, A, D, M extends Map<K, D>> M toMap(final IntFunction<? extends K> keyMapper, final Collector<Integer, A, D> downstream,
            final Supplier<? extends M> mapFactory);

    public abstract int reduce(int identity, IntBinaryOperator op);

    public abstract OptionalInt reduce(IntBinaryOperator op);

    public abstract <R> R collect(Supplier<R> supplier, ObjIntConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * 
     * @param supplier
     * @param accumulator
     * @return
     */
    public abstract <R> R collect(Supplier<R> supplier, ObjIntConsumer<? super R> accumulator);

    public abstract <E extends Exception> void forEach(final Try.IntConsumer<E> action) throws E;

    public abstract <E extends Exception> boolean anyMatch(final Try.IntPredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean allMatch(final Try.IntPredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean noneMatch(final Try.IntPredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalInt findFirst(final Try.IntPredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalInt findLast(final Try.IntPredicate<E> predicate) throws E;

    public abstract <E extends Exception, E2 extends Exception> OptionalInt findFirstOrLast(Try.IntPredicate<E> predicateForFirst,
            Try.IntPredicate<E> predicateForLast) throws E, E2;

    public abstract <E extends Exception> OptionalInt findAny(final Try.IntPredicate<E> predicate) throws E;

    public abstract OptionalInt min();

    public abstract OptionalInt max();

    /**
     * 
     * @param k
     * @return OptionalByte.empty() if there is no element or count less than k, otherwise the kth largest element.
     */
    public abstract OptionalInt kthLargest(int k);

    public abstract int sum();

    public abstract OptionalDouble average();

    public abstract IntSummaryStatistics summarize();

    public abstract Pair<IntSummaryStatistics, Optional<Map<Percentage, Integer>>> summarizeAndPercentiles();

    /**
     * 
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public abstract IntStream merge(final IntStream b, final IntBiFunction<Nth> nextSelector);

    public abstract IntStream zipWith(IntStream b, IntBinaryOperator zipFunction);

    public abstract IntStream zipWith(IntStream b, IntStream c, IntTernaryOperator zipFunction);

    public abstract IntStream zipWith(IntStream b, int valueForNoneA, int valueForNoneB, IntBinaryOperator zipFunction);

    public abstract IntStream zipWith(IntStream b, IntStream c, int valueForNoneA, int valueForNoneB, int valueForNoneC, IntTernaryOperator zipFunction);

    public abstract LongStream asLongStream();

    public abstract FloatStream asFloatStream();

    public abstract DoubleStream asDoubleStream();

    public abstract java.util.stream.IntStream toJdkStream();

    public abstract Stream<Integer> boxed();

    /**
     * Remember to close this Stream after the iteration is done, if required.
     * 
     * @return
     */
    @SequentialOnly
    @Override
    public IntIterator iterator() {
        if (isEmptyCloseHandlers(closeHandlers) == false) {
            if (logger.isWarnEnabled()) {
                logger.warn("### Remember to close " + ClassUtil.getSimpleClassName(getClass()));
            }
        }

        return iteratorEx();
    }

    abstract IntIteratorEx iteratorEx();

    @Override
    public <R> R __(Function<? super IntStream, R> transfer) {
        return transfer.apply(this);
    }

    public static IntStream empty() {
        return new ArrayIntStream(N.EMPTY_INT_ARRAY, true, null);
    }

    @SafeVarargs
    public static IntStream of(final int... a) {
        return N.isNullOrEmpty(a) ? empty() : new ArrayIntStream(a);
    }

    public static IntStream of(final int[] a, final int startIndex, final int endIndex) {
        return N.isNullOrEmpty(a) && (startIndex == 0 && endIndex == 0) ? empty() : new ArrayIntStream(a, startIndex, endIndex);
    }

    public static IntStream of(final Integer[] a) {
        return Stream.of(a).mapToInt(Fnn.unboxI());
    }

    public static IntStream of(final Integer[] a, final int startIndex, final int endIndex) {
        return Stream.of(a, startIndex, endIndex).mapToInt(Fnn.unboxI());
    }

    public static IntStream of(final Collection<Integer> c) {
        return Stream.of(c).mapToInt(Fnn.unboxI());
    }

    public static IntStream of(final IntIterator iterator) {
        return iterator == null ? empty() : new IteratorIntStream(iterator);
    }

    /**
     * Lazy evaluation.
     * @param supplier
     * @return
     */
    public static IntStream of(final Supplier<IntList> supplier) {
        final IntIterator iter = new IntIteratorEx() {
            private IntIterator iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null) {
                    init();
                }

                return iterator.hasNext();
            }

            @Override
            public int nextInt() {
                if (iterator == null) {
                    init();
                }

                return iterator.nextInt();
            }

            private void init() {
                final IntList c = supplier.get();

                if (N.isNullOrEmpty(c)) {
                    iterator = IntIterator.empty();
                } else {
                    iterator = c.iterator();
                }
            }
        };

        return of(iter);
    }

    public static IntStream of(final java.util.stream.IntStream stream) {
        return of(new IntIteratorEx() {
            private PrimitiveIterator.OfInt iter = null;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.hasNext();
            }

            @Override
            public int nextInt() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.nextInt();
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
            public int[] toArray() {
                return iter == null ? stream.toArray() : super.toArray();
            }
        }).__(s -> stream.isParallel() ? s.parallel() : s.sequential()).onClose(new Runnable() {
            @Override
            public void run() {
                stream.close();
            }
        });
    }

    public static IntStream ofCodePoints(final CharSequence str) {
        if (N.isNullOrEmpty(str)) {
            return empty();
        }

        final IntIterator iter = new IntIterator() {
            private final int len = str.length();
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                char c1 = str.charAt(cursor++);

                if (Character.isHighSurrogate(c1) && cursor < len) {
                    char c2 = str.charAt(cursor);
                    if (Character.isLowSurrogate(c2)) {
                        cursor++;
                        return Character.toCodePoint(c1, c2);
                    }
                }

                return c1;
            }
        };

        return of(iter);
    }

    private static final Function<int[], IntStream> flatMapper = new Function<int[], IntStream>() {
        @Override
        public IntStream apply(int[] t) {
            return IntStream.of(t);
        }
    };

    private static final Function<int[][], IntStream> flatMappper = new Function<int[][], IntStream>() {
        @Override
        public IntStream apply(int[][] t) {
            return IntStream.flat(t);
        }
    };

    public static IntStream flat(final int[][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToInt(flatMapper);
    }

    public static IntStream flat(final int[][] a, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (vertically == false) {
            return Stream.of(a).flatMapToInt(flatMapper);
        }

        long n = 0;

        for (int[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final IntIterator iter = new IntIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public int nextInt() {
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

    public static IntStream flat(final int[][] a, final int valueForNone, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (int[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = rows * cols;
        IntIterator iter = null;

        if (vertically) {
            iter = new IntIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public int nextInt() {
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
            iter = new IntIteratorEx() {

                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public int nextInt() {
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

    public static IntStream flat(final int[][][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToInt(flatMappper);
    }

    @SafeVarargs
    public static IntStream from(final char... a) {
        return N.isNullOrEmpty(a) ? empty() : from(a, 0, a.length);
    }

    public static IntStream from(final char[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
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
            public int[] toArray() {
                final int[] result = new int[toIndex - cursor];

                for (int i = cursor; i < toIndex; i++) {
                    result[i - cursor] = a[i];
                }

                return result;
            }
        });
    }

    @SafeVarargs
    public static IntStream from(final byte... a) {
        return N.isNullOrEmpty(a) ? empty() : from(a, 0, a.length);
    }

    public static IntStream from(final byte[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
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
            public int[] toArray() {
                final int[] result = new int[toIndex - cursor];

                for (int i = cursor; i < toIndex; i++) {
                    result[i - cursor] = a[i];
                }

                return result;
            }
        });
    }

    @SafeVarargs
    public static IntStream from(final short... a) {
        return N.isNullOrEmpty(a) ? empty() : from(a, 0, a.length);
    }

    public static IntStream from(final short[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (fromIndex == toIndex) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
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
            public int[] toArray() {
                final int[] result = new int[toIndex - cursor];

                for (int i = cursor; i < toIndex; i++) {
                    result[i - cursor] = a[i];
                }

                return result;
            }
        });
    }

    public static IntStream range(final int startInclusive, final int endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private int next = startInclusive;
            private long cnt = endExclusive * 1L - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
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
            public int[] toArray() {
                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static IntStream range(final int startInclusive, final int endExclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private int next = startInclusive;
            private long cnt = (endExclusive * 1L - startInclusive) / by + ((endExclusive * 1L - startInclusive) % by == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                int result = next;
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
            public int[] toArray() {
                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static IntStream rangeClosed(final int startInclusive, final int endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private int next = startInclusive;
            private long cnt = endInclusive * 1L - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
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
            public int[] toArray() {
                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static IntStream rangeClosed(final int startInclusive, final int endInclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        } else if (endInclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private int next = startInclusive;
            private long cnt = (endInclusive * 1L - startInclusive) / by + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                int result = next;
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
            public int[] toArray() {
                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static IntStream repeat(final int element, final long n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public int nextInt() {
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
            public int[] toArray() {
                final int[] result = new int[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = element;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static IntStream random() {
        return generate(new IntSupplier() {
            @Override
            public int getAsInt() {
                return RAND.nextInt();
            }
        });
    }

    public static IntStream random(final int startInclusive, final int endExclusive) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' must be less than 'endExclusive'");
        }

        final long mod = (long) endExclusive - (long) startInclusive;

        if (mod < Integer.MAX_VALUE) {
            final int n = (int) mod;

            return generate(new IntSupplier() {
                @Override
                public int getAsInt() {
                    return RAND.nextInt(n) + startInclusive;
                }
            });
        } else {
            return generate(new IntSupplier() {

                @Override
                public int getAsInt() {
                    return (int) (Math.abs(RAND.nextLong() % mod) + startInclusive);
                }

            });
        }
    }

    public static IntStream iterate(final BooleanSupplier hasNext, final IntSupplier next) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorIntStream(new IntIteratorEx() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public int nextInt() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return next.getAsInt();
            }
        });
    }

    public static IntStream iterate(final int init, final BooleanSupplier hasNext, final IntUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorIntStream(new IntIteratorEx() {
            private int t = 0;
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
            public int nextInt() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsInt(t);
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
    public static IntStream iterate(final int init, final IntPredicate hasNext, final IntUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorIntStream(new IntIteratorEx() {
            private int t = 0;
            private int cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsInt(t));
                    }

                    if (hasNextVal == false) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public int nextInt() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                hasNextVal = false;
                return t;
            }
        });
    }

    public static IntStream iterate(final int init, final IntUnaryOperator f) {
        N.checkArgNotNull(f);

        return new IteratorIntStream(new IntIteratorEx() {
            private int t = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsInt(t);
                }

                return t;
            }
        });
    }

    public static IntStream generate(final IntSupplier s) {
        N.checkArgNotNull(s);

        return new IteratorIntStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public int nextInt() {
                return s.getAsInt();
            }
        });
    }

    /**
     * 
     * @param source
     * @param indexFunc
     * @return
     * @see #ofIndices(Object, int, int, BiFunction)
     */
    public static <AC> IntStream ofIndices(final AC source, final BiFunction<? super AC, Integer, Integer> indexFunc) {
        return ofIndices(source, 0, indexFunc);
    };

    /**
     * 
     * @param source
     * @param fromIndex
     * @param indexFunc
     * @return
     * @see #ofIndices(Object, int, int, BiFunction)
     */
    public static <AC> IntStream ofIndices(final AC source, final int fromIndex, final BiFunction<? super AC, Integer, Integer> indexFunc) {
        return ofIndices(source, fromIndex, 1, indexFunc);
    };

    /**
     * <pre>
     * <code>
     * // Forwards:
     * int[] a = {1, 2, 3, 2, 5, 1};
     * IntStream.ofIndices(a, N::indexOf).println(); // [0, 5]
     * IntStream.ofIndices(a, 1, N::indexOf).println(); // [5]
     * 
     * // Backwards
     * IntStream.ofIndices(a, 5, -1, N::lastIndexOf).println(); // [5, 0]
     * IntStream.ofIndices(a, 4, -1, N::lastIndexOf).println(); // [0]
     * 
     * // OR
     * // Forwards:  
     * int[] source = { 1, 2, 3, 1, 2, 1 };
     * int[] targetSubArray = { 1, 2 }; 
     * IntStream.ofIndices(source, (a, fromIndex) -> Index.ofSubArray(a, fromIndex, targetSubArray, 0, targetSubArray.length).orElse(-1)).println(); // [0, 3] 
     * 
     * // Backwards
     * IntStream.ofIndices(source, 5, -2, (a, fromIndex) -> Index.ofSubArray(a, fromIndex, targetSubArray, 0, targetSubArray.length).orElse(-1))
                    .println(); // [3, 0]
     * </code>
     * </pre>
     * 
     * @param source
     * @param fromIndex
     * @param increment
     * @param indexFunc
     * @return
     * @see #ofIndices(Object, int, int, BiFunction)
     */
    public static <AC> IntStream ofIndices(final AC source, final int fromIndex, final int increment,
            final BiFunction<? super AC, Integer, Integer> indexFunc) {
        @SuppressWarnings("rawtypes")
        final int sourceLen = source.getClass().isArray() ? Array.getLength(source)
                : (source instanceof Collection ? ((Collection) source).size()
                        : (source instanceof CharSequence ? ((CharSequence) source).length() : Integer.MAX_VALUE));

        return ofIndices(source, fromIndex, increment, sourceLen, indexFunc);
    };

    public static <AC> IntStream ofIndices(final AC source, final int fromIndex, final int increment, final int sourceLen,
            final BiFunction<? super AC, Integer, Integer> indexFunc) {
        N.checkArgNotNegative(fromIndex, "fromIndex");
        N.checkArgument(increment != 0, "'increment' can't be zero");
        N.checkArgNotNull(indexFunc, "indexFunc");

        if (source == null) {
            return IntStream.empty();
        }

        final IntUnaryOperator f = new IntUnaryOperator() {
            @Override
            public int applyAsInt(int idx) {
                return ((increment > 0 && idx >= sourceLen - increment) || (increment < 0 && idx + increment < 0)) ? N.INDEX_NOT_FOUND
                        : indexFunc.apply(source, idx + increment);
            }
        };

        return iterate(indexFunc.apply(source, fromIndex), IntPredicate.NOT_NEGATIVE, f);
    };

    @SafeVarargs
    public static IntStream concat(final int[]... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorIntStream(new IntIteratorEx() {
            private final Iterator<int[]> iter = N.asList(a).iterator();
            private IntIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = IntIteratorEx.of(iter.next());
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
        });
    }

    @SafeVarargs
    public static IntStream concat(final IntIterator... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorIntStream(new IntIteratorEx() {
            private final Iterator<? extends IntIterator> iter = N.asList(a).iterator();
            private IntIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
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
        });
    }

    @SafeVarargs
    public static IntStream concat(final IntStream... a) {
        return N.isNullOrEmpty(a) ? empty() : concat(N.asList(a));
    }

    public static IntStream concat(final Collection<? extends IntStream> c) {
        return N.isNullOrEmpty(c) ? empty() : new IteratorIntStream(new IntIteratorEx() {
            private final Iterator<? extends IntStream> iterators = c.iterator();
            private IntStream cur;
            private IntIterator iter;

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
            public int nextInt() {
                if ((iter == null || iter.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return iter.nextInt();
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
    public static IntStream zip(final int[] a, final int[] b, final IntBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsInt(a[cursor], b[cursor++]);
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
    public static IntStream zip(final int[] a, final int[] b, final int[] c, final IntTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b) || N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsInt(a[cursor], b[cursor], c[cursor++]);
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
    public static IntStream zip(final IntIterator a, final IntIterator b, final IntBinaryOperator zipFunction) {
        return new IteratorIntStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public int nextInt() {
                return zipFunction.applyAsInt(a.nextInt(), b.nextInt());
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
    public static IntStream zip(final IntIterator a, final IntIterator b, final IntIterator c, final IntTernaryOperator zipFunction) {
        return new IteratorIntStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public int nextInt() {
                return zipFunction.applyAsInt(a.nextInt(), b.nextInt(), c.nextInt());
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
    public static IntStream zip(final IntStream a, final IntStream b, final IntBinaryOperator zipFunction) {
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
    public static IntStream zip(final IntStream a, final IntStream b, final IntStream c, final IntTernaryOperator zipFunction) {
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
    public static IntStream zip(final Collection<? extends IntStream> c, final IntNFunction<Integer> zipFunction) {
        return Stream.zip(c, zipFunction).mapToInt(ToIntFunction.UNBOX);
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
    public static IntStream zip(final int[] a, final int[] b, final int valueForNoneA, final int valueForNoneB, final IntBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private int ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsInt(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
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
    public static IntStream zip(final int[] a, final int[] b, final int[] c, final int valueForNoneA, final int valueForNoneB, final int valueForNoneC,
            final IntTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b) && N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private int ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public int nextInt() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsInt(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
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
    public static IntStream zip(final IntIterator a, final IntIterator b, final int valueForNoneA, final int valueForNoneB,
            final IntBinaryOperator zipFunction) {
        return new IteratorIntStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public int nextInt() {
                if (a.hasNext()) {
                    return zipFunction.applyAsInt(a.nextInt(), b.hasNext() ? b.nextInt() : valueForNoneB);
                } else {
                    return zipFunction.applyAsInt(valueForNoneA, b.nextInt());
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
    public static IntStream zip(final IntIterator a, final IntIterator b, final IntIterator c, final int valueForNoneA, final int valueForNoneB,
            final int valueForNoneC, final IntTernaryOperator zipFunction) {
        return new IteratorIntStream(new IntIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public int nextInt() {
                if (a.hasNext()) {
                    return zipFunction.applyAsInt(a.nextInt(), b.hasNext() ? b.nextInt() : valueForNoneB, c.hasNext() ? c.nextInt() : valueForNoneC);
                } else if (b.hasNext()) {
                    return zipFunction.applyAsInt(valueForNoneA, b.nextInt(), c.hasNext() ? c.nextInt() : valueForNoneC);
                } else {
                    return zipFunction.applyAsInt(valueForNoneA, valueForNoneB, c.nextInt());
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
    public static IntStream zip(final IntStream a, final IntStream b, final int valueForNoneA, final int valueForNoneB, final IntBinaryOperator zipFunction) {
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
    public static IntStream zip(final IntStream a, final IntStream b, final IntStream c, final int valueForNoneA, final int valueForNoneB,
            final int valueForNoneC, final IntTernaryOperator zipFunction) {
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
    public static IntStream zip(final Collection<? extends IntStream> c, final int[] valuesForNone, final IntNFunction<Integer> zipFunction) {
        return Stream.zip(c, valuesForNone, zipFunction).mapToInt(ToIntFunction.UNBOX);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static IntStream merge(final int[] a, final int[] b, final IntBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return new IteratorIntStream(new IntIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public int nextInt() {
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
    public static IntStream merge(final int[] a, final int[] b, final int[] c, final IntBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), IntStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static IntStream merge(final IntIterator a, final IntIterator b, final IntBiFunction<Nth> nextSelector) {
        return new IteratorIntStream(new IntIteratorEx() {
            private int nextA = 0;
            private int nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public int nextInt() {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.nextInt())) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.nextInt()), nextB) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.nextInt()), (nextB = b.nextInt())) == Nth.FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.nextInt();
                    }
                } else if (b.hasNext()) {
                    return b.nextInt();
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
    public static IntStream merge(final IntIterator a, final IntIterator b, final IntIterator c, final IntBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static IntStream merge(final IntStream a, final IntStream b, final IntBiFunction<Nth> nextSelector) {
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
    public static IntStream merge(final IntStream a, final IntStream b, final IntStream c, final IntBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static IntStream merge(final Collection<? extends IntStream> c, final IntBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends IntStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends IntStream> iter = c.iterator();
        IntStream result = merge(iter.next(), iter.next(), nextSelector);

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
    public static IntStream parallelMerge(final Collection<? extends IntStream> c, final IntBiFunction<Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static IntStream parallelMerge(final Collection<? extends IntStream> c, final IntBiFunction<Nth> nextSelector, final int maxThreadNum) {
        N.checkArgument(maxThreadNum > 0, "'maxThreadNum' must not less than 1");

        if (maxThreadNum <= 1) {
            return merge(c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends IntStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        } else if (c.size() == 3) {
            final Iterator<? extends IntStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), iter.next(), nextSelector);
        }

        final Queue<IntStream> queue = N.newLinkedList();

        for (IntStream e : c) {
            queue.add(e);
        }

        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    IntStream a = null;
                    IntStream b = null;
                    IntStream c = null;

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

                            c = IntStream.of(merge(a, b, nextSelector).toArray());

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

    public static abstract class IntStreamEx extends IntStream {
        private IntStreamEx(boolean sorted, Collection<Runnable> closeHandlers) {
            super(sorted, closeHandlers);
            // Factory class.
        }
    }
}
