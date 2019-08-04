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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;

import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.function.CharTernaryOperator;
import com.landawn.abacus.util.function.CharToIntFunction;
import com.landawn.abacus.util.function.CharUnaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjCharConsumer;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToCharFunction;

/**
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 * <br />
 *
 * @see Stream
 */
public abstract class CharStream
        extends StreamBase<Character, char[], CharPredicate, CharConsumer, CharList, OptionalChar, IndexedChar, CharIterator, CharStream> {

    static final Random RAND = new SecureRandom();

    CharStream(final boolean sorted, final Collection<Runnable> closeHandlers) {
        super(sorted, null, closeHandlers);
    }

    /**
     * Returns a stream consisting of the results of applying the given
     * function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonCharerference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract CharStream map(CharUnaryOperator mapper);

    /**
     * Returns a {@code IntStream} consisting of the results of applying the
     * given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonInterference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract IntStream mapToInt(CharToIntFunction mapper);

    /**
     * Returns an object-valued {@code Stream} consisting of the results of
     * applying the given function to the elements of this stream.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">
     *     intermediate operation</a>.
     *
     * @param <U> the element type of the new stream
     * @param mapper a <a href="package-summary.html#NonCharerference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element
     * @return the new stream
     */
    public abstract <U> Stream<U> mapToObj(CharFunction<? extends U> mapper);

    /**
     * Returns a stream consisting of the results of replacing each element of
     * this stream with the contents of a mapped stream produced by applying
     * the provided mapping function to each element.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @param mapper a <a href="package-summary.html#NonCharerference">non-interfering</a>,
     *               <a href="package-summary.html#Statelessness">stateless</a>
     *               function to apply to each element which produces an
     *               {@code CharStream} of new values
     * @return the new stream
     * @see Stream#flatMap(Function)
     */
    public abstract CharStream flatMap(CharFunction<? extends CharStream> mapper);

    public abstract CharStream flattMap(CharFunction<char[]> mapper);

    public abstract IntStream flatMapToInt(CharFunction<? extends IntStream> mapper);

    public abstract <T> Stream<T> flatMapToObj(CharFunction<? extends Stream<T>> mapper);

    public abstract <T> Stream<T> flattMapToObj(CharFunction<? extends Collection<T>> mapper);

    public abstract <T> Stream<T> flatMappToObj(CharFunction<T[]> mapper);

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
     * @see #collapse(CharBiPredicate, CharBinaryOperator)
     * @see Stream#rangeMap(BiPredicate, BiFunction)
     */
    @SequentialOnly
    public abstract CharStream rangeMap(final CharBiPredicate sameRange, final CharBinaryOperator mapper);

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
    public abstract <T> Stream<T> rangeMapp(final CharBiPredicate sameRange, final CharBiFunction<T> mapper);

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
    public abstract Stream<CharList> collapse(final CharBiPredicate collapsible);

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
    public abstract CharStream collapse(final CharBiPredicate collapsible, final CharBinaryOperator mergeFunction);

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
    public abstract CharStream scan(final CharBinaryOperator accumulator);

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
    public abstract CharStream scan(final char init, final CharBinaryOperator accumulator);

    /**
     * 
     * @param init
     * @param accumulator
     * @param initIncluded
     * @return
     */
    @SequentialOnly
    public abstract CharStream scan(final char init, final CharBinaryOperator accumulator, final boolean initIncluded);

    public abstract CharList toCharList();

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    public abstract <K, V> Map<K, V> toMap(CharFunction<? extends K> keyMapper, CharFunction<? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(CharFunction<? extends K> keyMapper, CharFunction<? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    public abstract <K, V> Map<K, V> toMap(CharFunction<? extends K> keyMapper, CharFunction<? extends V> valueMapper, BinaryOperator<V> mergeFunction);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    public abstract <K, V, M extends Map<K, V>> M toMap(CharFunction<? extends K> keyMapper, CharFunction<? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    public abstract <K, A, D> Map<K, D> toMap(final CharFunction<? extends K> keyMapper, final Collector<Character, A, D> downstream);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    public abstract <K, A, D, M extends Map<K, D>> M toMap(final CharFunction<? extends K> keyMapper, final Collector<Character, A, D> downstream,
            final Supplier<? extends M> mapFactory);

    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using the provided identity value and an
     * <a href="package-summary.html#Associativity">associative</a>
     * accumulation function, and returns the reduced value.  This is equivalent
     * to:
     * <pre>{@code
     *     int result = identity;
     *     for (int element : this stream)
     *         result = accumulator.applyAsChar(result, element)
     *     return result;
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code identity} value must be an identity for the accumulator
     * function. This means that for all {@code x},
     * {@code accumulator.apply(identity, x)} is equal to {@code x}.
     * The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @apiNote Sum, min, max, and average are all special cases of reduction.
     * Summing a stream of numbers can be expressed as:
     *
     * <pre>{@code
     *     int sum = integers.reduce(0, (a, b) -> a+b);
     * }</pre>
     *
     * or more compactly:
     *
     * <pre>{@code
     *     int sum = integers.reduce(0, Chareger::sum);
     * }</pre>
     *
     * <p>While this may seem a more roundabout way to perform an aggregation
     * compared to simply mutating a running total in a loop, reduction
     * operations parallelize more gracefully, without needing additional
     * synchronization and with greatly reduced risk of data races.
     *
     * @param identity the identity value for the accumulating function
     * @param op an <a href="package-summary.html#Associativity">associative</a>,
     *           <a href="package-summary.html#NonCharerference">non-interfering</a>,
     *           <a href="package-summary.html#Statelessness">stateless</a>
     *           function for combining two values
     * @return the result of the reduction
     * @see #sum()
     * @see #min()
     * @see #max()
     * @see #average()
     */
    public abstract char reduce(char identity, CharBinaryOperator op);

    /**
     * Performs a <a href="package-summary.html#Reduction">reduction</a> on the
     * elements of this stream, using an
     * <a href="package-summary.html#Associativity">associative</a> accumulation
     * function, and returns an {@code OptionalChar} describing the reduced value,
     * if any. This is equivalent to:
     * <pre>{@code
     *     boolean foundAny = false;
     *     int result = null;
     *     for (int element : this stream) {
     *         if (!foundAny) {
     *             foundAny = true;
     *             result = element;
     *         }
     *         else
     *             result = accumulator.applyAsChar(result, element);
     *     }
     *     return foundAny ? OptionalChar.of(result) : OptionalChar.empty();
     * }</pre>
     *
     * but is not constrained to execute sequentially.
     *
     * <p>The {@code accumulator} function must be an
     * <a href="package-summary.html#Associativity">associative</a> function.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param op an <a href="package-summary.html#Associativity">associative</a>,
     *           <a href="package-summary.html#NonCharerference">non-interfering</a>,
     *           <a href="package-summary.html#Statelessness">stateless</a>
     *           function for combining two values
     * @return the result of the reduction
     * @see #reduce(int, CharBinaryOperator)
     */
    public abstract OptionalChar reduce(CharBinaryOperator op);

    /**
     * Performs a <a href="package-summary.html#MutableReduction">mutable
     * reduction</a> operation on the elements of this stream.  A mutable
     * reduction is one in which the reduced value is a mutable result container,
     * such as an {@code ArrayList}, and elements are incorporated by updating
     * the state of the result rather than by replacing the result.  This
     * produces a result equivalent to:
     * <pre>{@code
     *     R result = supplier.get();
     *     for (int element : this stream)
     *         accumulator.accept(result, element);
     *     return result;
     * }</pre>
     *
     * <p>Like {@link #reduce(int, CharBinaryOperator)}, {@code collect} operations
     * can be parallelized without requiring additional synchronization.
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @param <R> type of the result
     * @param supplier a function that creates a new result container. For a
     *                 parallel execution, this function may be called
     *                 multiple times and must return a fresh value each time.
     * @param accumulator an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonCharerference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for incorporating an additional element into a result
     * @param combiner an <a href="package-summary.html#Associativity">associative</a>,
     *                    <a href="package-summary.html#NonCharerference">non-interfering</a>,
     *                    <a href="package-summary.html#Statelessness">stateless</a>
     *                    function for combining two values, which must be
     *                    compatible with the accumulator function
     * @return the result of the reduction
     * @see Stream#collect(Supplier, BiConsumer, BiConsumer)
     */
    public abstract <R> R collect(Supplier<R> supplier, ObjCharConsumer<? super R> accumulator, BiConsumer<R, R> combiner);

    /**
     * 
     * @param supplier
     * @param accumulator
     * @return
     */
    public abstract <R> R collect(Supplier<R> supplier, ObjCharConsumer<? super R> accumulator);

    public abstract <E extends Exception> void forEach(final Try.CharConsumer<E> action) throws E;

    public abstract <E extends Exception> boolean anyMatch(final Try.CharPredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean allMatch(final Try.CharPredicate<E> predicate) throws E;

    public abstract <E extends Exception> boolean noneMatch(final Try.CharPredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalChar findFirst(final Try.CharPredicate<E> predicate) throws E;

    public abstract <E extends Exception> OptionalChar findLast(final Try.CharPredicate<E> predicate) throws E;

    public abstract <E extends Exception, E2 extends Exception> OptionalChar findFirstOrLast(Try.CharPredicate<E> predicateForFirst,
            Try.CharPredicate<E> predicateForLast) throws E, E2;

    public abstract <E extends Exception> OptionalChar findAny(final Try.CharPredicate<E> predicate) throws E;

    /**
     * Returns an {@code OptionalChar} describing the minimum element of this
     * stream, or an empty optional if this stream is empty.  This is a special
     * case of a <a href="package-summary.html#Reduction">reduction</a>
     * and is equivalent to:
     * <pre>{@code
     *     return reduce(Chareger::min);
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal operation</a>.
     *
     * @return an {@code OptionalChar} containing the minimum element of this
     * stream, or an empty {@code OptionalChar} if the stream is empty
     */
    public abstract OptionalChar min();

    /**
     * Returns an {@code OptionalChar} describing the maximum element of this
     * stream, or an empty optional if this stream is empty.  This is a special
     * case of a <a href="package-summary.html#Reduction">reduction</a>
     * and is equivalent to:
     * <pre>{@code
     *     return reduce(Chareger::max);
     * }</pre>
     *
     * <p>This is a <a href="package-summary.html#StreamOps">terminal
     * operation</a>.
     *
     * @return an {@code OptionalChar} containing the maximum element of this
     * stream, or an empty {@code OptionalChar} if the stream is empty
     */
    public abstract OptionalChar max();

    /**
     * 
     * @param k
     * @return OptionalByte.empty() if there is no element or count less than k, otherwise the kth largest element.
     */
    public abstract OptionalChar kthLargest(int k);

    public abstract int sum();

    public abstract OptionalDouble average();

    public abstract CharSummaryStatistics summarize();

    public abstract Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> summarizeAndPercentiles();

    /**
     * 
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public abstract CharStream merge(final CharStream b, final CharBiFunction<Nth> nextSelector);

    public abstract CharStream zipWith(CharStream b, CharBinaryOperator zipFunction);

    public abstract CharStream zipWith(CharStream b, CharStream c, CharTernaryOperator zipFunction);

    public abstract CharStream zipWith(CharStream b, char valueForNoneA, char valueForNoneB, CharBinaryOperator zipFunction);

    public abstract CharStream zipWith(CharStream b, CharStream c, char valueForNoneA, char valueForNoneB, char valueForNoneC, CharTernaryOperator zipFunction);

    /**
     * Returns a {@code LongStream} consisting of the elements of this stream,
     * converted to {@code long}.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @return a {@code LongStream} consisting of the elements of this stream,
     * converted to {@code long}
     */
    public abstract IntStream asIntStream();

    /**
     * Returns a {@code Stream} consisting of the elements of this stream,
     * each boxed to an {@code Chareger}.
     *
     * <p>This is an <a href="package-summary.html#StreamOps">intermediate
     * operation</a>.
     *
     * @return a {@code Stream} consistent of the elements of this stream,
     * each boxed to an {@code Chareger}
     */
    public abstract Stream<Character> boxed();

    /**
     * Remember to close this Stream after the iteration is done, if required.
     * 
     * @return
     */
    @SequentialOnly
    @Override
    public CharIterator iterator() {
        if (isEmptyCloseHandlers(closeHandlers) == false) {
            if (logger.isWarnEnabled()) {
                logger.warn("### Remember to close " + ClassUtil.getSimpleClassName(getClass()));
            }
        }

        return iteratorEx();
    }

    abstract CharIteratorEx iteratorEx();

    @Override
    public <R> R __(Function<? super CharStream, R> transfer) {
        return transfer.apply(this);
    }

    public static CharStream empty() {
        return new ArrayCharStream(N.EMPTY_CHAR_ARRAY, true, null);
    }

    @SafeVarargs
    public static CharStream of(final char... a) {
        return N.isNullOrEmpty(a) ? empty() : new ArrayCharStream(a);
    }

    public static CharStream of(final char[] a, final int startIndex, final int endIndex) {
        return N.isNullOrEmpty(a) && (startIndex == 0 && endIndex == 0) ? empty() : new ArrayCharStream(a, startIndex, endIndex);
    }

    /**
     * Takes the chars in the specified String as the elements of the Stream
     * 
     * @param str
     * @return
     */
    public static CharStream of(final CharSequence str) {
        return N.isNullOrEmpty(str) ? empty() : of(str, 0, str.length());
    }

    /**
     * Takes the chars in the specified String as the elements of the Stream
     * 
     * @param str
     * @param startIndex
     * @param endIndex
     * @return
     */
    @SuppressWarnings("deprecation")
    public static CharStream of(final CharSequence str, final int startIndex, final int endIndex) {
        N.checkFromToIndex(startIndex, endIndex, N.len(str));

        if (N.isNullOrEmpty(str)) {
            return empty();
        }

        if (str instanceof String) {
            return of(StringUtil.getCharsForReadOnly((String) str), startIndex, endIndex);
        }

        final CharIteratorEx iter = new CharIteratorEx() {
            private int cursor = startIndex;

            @Override
            public boolean hasNext() {
                return cursor < endIndex;
            }

            @Override
            public char nextChar() {
                return str.charAt(cursor++);
            }

            @Override
            public long count() {
                return endIndex - cursor;
            }
        };

        return new IteratorCharStream(iter);
    }

    public static CharStream of(final Character[] a) {
        return Stream.of(a).mapToChar(Fnn.unboxC());
    }

    public static CharStream of(final Character[] a, final int startIndex, final int endIndex) {
        return Stream.of(a, startIndex, endIndex).mapToChar(Fnn.unboxC());
    }

    public static CharStream of(final Collection<Character> c) {
        return Stream.of(c).mapToChar(Fnn.unboxC());
    }

    public static CharStream of(final CharIterator iterator) {
        return iterator == null ? empty() : new IteratorCharStream(iterator);
    }

    /**
     * Lazy evaluation.
     * @param supplier
     * @return
     */
    public static CharStream of(final Supplier<CharList> supplier) {
        final CharIterator iter = new CharIteratorEx() {
            private CharIterator iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null) {
                    init();
                }

                return iterator.hasNext();
            }

            @Override
            public char nextChar() {
                if (iterator == null) {
                    init();
                }

                return iterator.nextChar();
            }

            private void init() {
                final CharList c = supplier.get();

                if (N.isNullOrEmpty(c)) {
                    iterator = CharIterator.empty();
                } else {
                    iterator = c.iterator();
                }
            }
        };

        return of(iter);
    }

    private static final Function<char[], CharStream> flatMapper = new Function<char[], CharStream>() {
        @Override
        public CharStream apply(char[] t) {
            return CharStream.of(t);
        }
    };

    private static final Function<char[][], CharStream> flatMappper = new Function<char[][], CharStream>() {
        @Override
        public CharStream apply(char[][] t) {
            return CharStream.flat(t);
        }
    };

    public static CharStream flat(final char[][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToChar(flatMapper);
    }

    public static CharStream flat(final char[][] a, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (vertically == false) {
            return Stream.of(a).flatMapToChar(flatMapper);
        }

        long n = 0;

        for (char[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final CharIterator iter = new CharIteratorEx() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public char nextChar() {
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

    public static CharStream flat(final char[][] a, final char valueForNone, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (char[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = rows * cols;
        CharIterator iter = null;

        if (vertically) {
            iter = new CharIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public char nextChar() {
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
            iter = new CharIteratorEx() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public char nextChar() {
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

    public static CharStream flat(final char[][][] a) {
        return N.isNullOrEmpty(a) ? empty() : Stream.of(a).flatMapToChar(flatMappper);
    }

    public static CharStream range(final char startInclusive, final char endExclusive) {
        if (startInclusive >= endExclusive) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private char next = startInclusive;
            private int cnt = endExclusive * 1 - startInclusive;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
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
            public char[] toArray() {
                final char[] result = new char[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static CharStream range(final char startInclusive, final char endExclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' can't be zero");
        }

        if (endExclusive == startInclusive || endExclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private char next = startInclusive;
            private int cnt = (endExclusive * 1 - startInclusive) / by + ((endExclusive * 1 - startInclusive) % by == 0 ? 0 : 1);

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                char result = next;
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
            public char[] toArray() {
                final char[] result = new char[cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static CharStream rangeClosed(final char startInclusive, final char endInclusive) {
        if (startInclusive > endInclusive) {
            return empty();
        } else if (startInclusive == endInclusive) {
            return of(startInclusive);
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private char next = startInclusive;
            private int cnt = endInclusive * 1 - startInclusive + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
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
            public char[] toArray() {
                final char[] result = new char[cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = next++;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static CharStream rangeClosed(final char startInclusive, final char endInclusive, final int by) {
        if (by == 0) {
            throw new IllegalArgumentException("'by' can't be zero");
        }

        if (endInclusive == startInclusive) {
            return of(startInclusive);
        } else if (endInclusive > startInclusive != by > 0) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private char next = startInclusive;
            private int cnt = (endInclusive * 1 - startInclusive) / by + 1;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                char result = next;
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
            public char[] toArray() {
                final char[] result = new char[cnt];

                for (int i = 0; i < cnt; i++, next += by) {
                    result[i] = next;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static CharStream repeat(final char element, final long n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public char nextChar() {
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
            public char[] toArray() {
                final char[] result = new char[(int) cnt];

                for (int i = 0; i < cnt; i++) {
                    result[i] = element;
                }

                cnt = 0;

                return result;
            }
        });
    }

    public static CharStream random() {
        final int mod = Character.MAX_VALUE + 1;

        return generate(new CharSupplier() {
            @Override
            public char getAsChar() {
                return (char) RAND.nextInt(mod);
            }
        });
    }

    public static CharStream random(final char startInclusive, final char endExclusive) {
        if (startInclusive >= endExclusive) {
            throw new IllegalArgumentException("'startInclusive' must be less than 'endExclusive'");
        }

        final int mod = endExclusive - startInclusive;

        return generate(new CharSupplier() {
            @Override
            public char getAsChar() {
                return (char) (RAND.nextInt(mod) + startInclusive);
            }
        });
    }

    public static CharStream random(final char[] candicates) {
        if (N.isNullOrEmpty(candicates)) {
            return empty();
        } else if (candicates.length >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }

        final int n = candicates.length;

        return generate(new CharSupplier() {
            @Override
            public char getAsChar() {
                return candicates[RAND.nextInt(n)];
            }
        });
    }

    public static CharStream iterate(final BooleanSupplier hasNext, final CharSupplier next) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return new IteratorCharStream(new CharIteratorEx() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public char nextChar() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return next.getAsChar();
            }
        });
    }

    public static CharStream iterate(final char init, final BooleanSupplier hasNext, final CharUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorCharStream(new CharIteratorEx() {
            private char t = 0;
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
            public char nextChar() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;

                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsChar(t);
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
    public static CharStream iterate(final char init, final CharPredicate hasNext, final CharUnaryOperator f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return new IteratorCharStream(new CharIteratorEx() {
            private char t = 0;
            private char cur = 0;
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
                        hasNextVal = hasNext.test(cur = f.applyAsChar(t));
                    }

                    if (hasNextVal == false) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public char nextChar() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                hasNextVal = false;
                return t;
            }
        });
    }

    public static CharStream iterate(final char init, final CharUnaryOperator f) {
        N.checkArgNotNull(f);

        return new IteratorCharStream(new CharIteratorEx() {
            private char t = 0;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public char nextChar() {
                if (isFirst) {
                    isFirst = false;
                    t = init;
                } else {
                    t = f.applyAsChar(t);
                }

                return t;
            }
        });
    }

    public static CharStream generate(final CharSupplier s) {
        N.checkArgNotNull(s);

        return new IteratorCharStream(new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public char nextChar() {
                return s.getAsChar();
            }
        });
    }

    @SafeVarargs
    public static CharStream concat(final char[]... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorCharStream(new CharIteratorEx() {
            private final Iterator<char[]> iter = N.asList(a).iterator();
            private CharIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = CharIteratorEx.of(iter.next());
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
        });
    }

    @SafeVarargs
    public static CharStream concat(final CharIterator... a) {
        return N.isNullOrEmpty(a) ? empty() : new IteratorCharStream(new CharIteratorEx() {
            private final Iterator<? extends CharIterator> iter = N.asList(a).iterator();
            private CharIterator cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next();
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
        });
    }

    @SafeVarargs
    public static CharStream concat(final CharStream... a) {
        return N.isNullOrEmpty(a) ? empty() : concat(N.asList(a));
    }

    public static CharStream concat(final Collection<? extends CharStream> c) {
        return N.isNullOrEmpty(c) ? empty() : new IteratorCharStream(new CharIteratorEx() {
            private final Iterator<? extends CharStream> iterators = c.iterator();
            private CharStream cur;
            private CharIterator iter;

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
            public char nextChar() {
                if ((iter == null || iter.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return iter.nextChar();
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
    public static CharStream zip(final char[] a, final char[] b, final CharBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public char nextChar() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsChar(a[cursor], b[cursor++]);
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
    public static CharStream zip(final char[] a, final char[] b, final char[] c, final CharTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) || N.isNullOrEmpty(b) || N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public char nextChar() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return zipFunction.applyAsChar(a[cursor], b[cursor], c[cursor++]);
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
    public static CharStream zip(final CharIterator a, final CharIterator b, final CharBinaryOperator zipFunction) {
        return new IteratorCharStream(new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public char nextChar() {
                return zipFunction.applyAsChar(a.nextChar(), b.nextChar());
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
    public static CharStream zip(final CharIterator a, final CharIterator b, final CharIterator c, final CharTernaryOperator zipFunction) {
        return new IteratorCharStream(new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public char nextChar() {
                return zipFunction.applyAsChar(a.nextChar(), b.nextChar(), c.nextChar());
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
    public static CharStream zip(final CharStream a, final CharStream b, final CharBinaryOperator zipFunction) {
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
    public static CharStream zip(final CharStream a, final CharStream b, final CharStream c, final CharTernaryOperator zipFunction) {
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
    public static CharStream zip(final Collection<? extends CharStream> c, final CharNFunction<Character> zipFunction) {
        return Stream.zip(c, zipFunction).mapToChar(ToCharFunction.UNBOX);
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
    public static CharStream zip(final char[] a, final char[] b, final char valueForNoneA, final char valueForNoneB, final CharBinaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), len = N.max(aLen, bLen);
            private int cursor = 0;
            private char ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public char nextChar() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsChar(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB);
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
    public static CharStream zip(final char[] a, final char[] b, final char[] c, final char valueForNoneA, final char valueForNoneB, final char valueForNoneC,
            final CharTernaryOperator zipFunction) {
        if (N.isNullOrEmpty(a) && N.isNullOrEmpty(b) && N.isNullOrEmpty(c)) {
            return empty();
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int aLen = N.len(a), bLen = N.len(b), cLen = N.len(c), len = N.max(aLen, bLen, cLen);
            private int cursor = 0;
            private char ret = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public char nextChar() {
                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                ret = zipFunction.applyAsChar(cursor < aLen ? a[cursor] : valueForNoneA, cursor < bLen ? b[cursor] : valueForNoneB,
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
    public static CharStream zip(final CharIterator a, final CharIterator b, final char valueForNoneA, final char valueForNoneB,
            final CharBinaryOperator zipFunction) {
        return new IteratorCharStream(new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public char nextChar() {
                if (a.hasNext()) {
                    return zipFunction.applyAsChar(a.nextChar(), b.hasNext() ? b.nextChar() : valueForNoneB);
                } else {
                    return zipFunction.applyAsChar(valueForNoneA, b.nextChar());
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
    public static CharStream zip(final CharIterator a, final CharIterator b, final CharIterator c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTernaryOperator zipFunction) {
        return new IteratorCharStream(new CharIteratorEx() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public char nextChar() {
                if (a.hasNext()) {
                    return zipFunction.applyAsChar(a.nextChar(), b.hasNext() ? b.nextChar() : valueForNoneB, c.hasNext() ? c.nextChar() : valueForNoneC);
                } else if (b.hasNext()) {
                    return zipFunction.applyAsChar(valueForNoneA, b.nextChar(), c.hasNext() ? c.nextChar() : valueForNoneC);
                } else {
                    return zipFunction.applyAsChar(valueForNoneA, valueForNoneB, c.nextChar());
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
    public static CharStream zip(final CharStream a, final CharStream b, final char valueForNoneA, final char valueForNoneB,
            final CharBinaryOperator zipFunction) {
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
    public static CharStream zip(final CharStream a, final CharStream b, final CharStream c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTernaryOperator zipFunction) {
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
    public static CharStream zip(final Collection<? extends CharStream> c, final char[] valuesForNone, final CharNFunction<Character> zipFunction) {
        return Stream.zip(c, valuesForNone, zipFunction).mapToChar(ToCharFunction.UNBOX);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static CharStream merge(final char[] a, final char[] b, final CharBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return new IteratorCharStream(new CharIteratorEx() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public char nextChar() {
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
    public static CharStream merge(final char[] a, final char[] b, final char[] c, final CharBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), CharStream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static CharStream merge(final CharIterator a, final CharIterator b, final CharBiFunction<Nth> nextSelector) {
        return new IteratorCharStream(new CharIteratorEx() {
            private char nextA = 0;
            private char nextB = 0;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || hasNextA || hasNextB;
            }

            @Override
            public char nextChar() {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.nextChar())) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.nextChar()), nextB) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.nextChar()), (nextB = b.nextChar())) == Nth.FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.nextChar();
                    }
                } else if (b.hasNext()) {
                    return b.nextChar();
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
    public static CharStream merge(final CharIterator a, final CharIterator b, final CharIterator c, final CharBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static CharStream merge(final CharStream a, final CharStream b, final CharBiFunction<Nth> nextSelector) {
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
    public static CharStream merge(final CharStream a, final CharStream b, final CharStream c, final CharBiFunction<Nth> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static CharStream merge(final Collection<? extends CharStream> c, final CharBiFunction<Nth> nextSelector) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends CharStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends CharStream> iter = c.iterator();
        CharStream result = merge(iter.next(), iter.next(), nextSelector);

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
    public static CharStream parallelMerge(final Collection<? extends CharStream> c, final CharBiFunction<Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static CharStream parallelMerge(final Collection<? extends CharStream> c, final CharBiFunction<Nth> nextSelector, final int maxThreadNum) {
        N.checkArgument(maxThreadNum > 0, "'maxThreadNum' must not less than 1");

        if (maxThreadNum <= 1) {
            return merge(c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends CharStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        } else if (c.size() == 3) {
            final Iterator<? extends CharStream> iter = c.iterator();
            return merge(iter.next(), iter.next(), iter.next(), nextSelector);
        }

        final Queue<CharStream> queue = N.newLinkedList();

        for (CharStream e : c) {
            queue.add(e);
        }

        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    CharStream a = null;
                    CharStream b = null;
                    CharStream c = null;

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

                            c = CharStream.of(merge(a, b, nextSelector).toArray());

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
