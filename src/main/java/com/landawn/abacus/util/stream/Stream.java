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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.ExceptionalStream;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.JdbcUtil;
import com.landawn.abacus.util.Keyed;
import com.landawn.abacus.util.LineIterator;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NoCachingNoUpdating;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.ByteTriFunction;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharTriFunction;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleNFunction;
import com.landawn.abacus.util.function.DoubleTriFunction;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatNFunction;
import com.landawn.abacus.util.function.FloatTriFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntNFunction;
import com.landawn.abacus.util.function.IntTriFunction;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.LongTriFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.function.ShortTriFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.UnaryOperator;
import com.landawn.abacus.util.stream.ObjIteratorEx.QueuedIterator;

/**
 * Note: This class includes codes copied from StreamEx: https://github.com/amaembo/streamex under Apache License, version 2.0.
 * <br />
 * 
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 * 
 * @param <T> the type of the stream elements 
 * @see IntStream
 * @see LongStream
 * @see DoubleStream 
 */
public abstract class Stream<T>
        extends StreamBase<T, Object[], Predicate<? super T>, Consumer<? super T>, List<T>, Optional<T>, Indexed<T>, ObjIterator<T>, Stream<T>> {

    static final Random RAND = new SecureRandom();

    Stream(final boolean sorted, final Comparator<? super T> cmp, final Collection<Runnable> closeHandlers) {
        super(sorted, cmp, closeHandlers);
    }

    @SequentialOnly
    public <U> Stream<U> select(Class<U> targetType) {
        if (isParallel()) {
            return (Stream<U>) sequential().filter(Fn.instanceOf(targetType)).parallel(maxThreadNum(), splitor());
        } else {
            return (Stream<U>) filter(Fn.instanceOf(targetType));
        }
    }

    @ParallelSupported
    public abstract <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    //    public abstract <R> Stream<R> biMap(BiFunction<? super T, ? super T, ? extends R> mapper);
    //
    //    /**
    //     * Returns a stream consisting of the results of applying the given function
    //     * to the every two adjacent elements of this stream.
    //     * 
    //     * <pre>
    //     * <code>
    //     * Stream.of("a", "b", "c", "d", "e").biMap((i, j) -> i + "-" + j).println();
    //     * // print out: [a-b, c-d, e-null]
    //     * </code>
    //     * </pre>
    //     * 
    //     * @param mapper
    //     * @param ignoreNotPaired flag to identify if need to ignore the last element when the total length of the stream is odd number. Default value is false
    //     * @return
    //     */
    //    public abstract <R> Stream<R> biMap(BiFunction<? super T, ? super T, ? extends R> mapper, boolean ignoreNotPaired);
    //
    //    public abstract <R> Stream<R> triMap(TriFunction<? super T, ? super T, ? super T, ? extends R> mapper);
    //
    //    /**
    //     * Returns a stream consisting of the results of applying the given function
    //     * to the every three adjacent elements of this stream.
    //     * 
    //     * <pre>
    //     * <code>
    //     * Stream.of("a", "b", "c", "d", "e").triMap((i, j, k) -> i + "-" + j + "-" + k).println();
    //     * // print out: [a-b-c, d-e-null]
    //     * </code>
    //     * </pre>
    //     * 
    //     * @param mapper
    //     * @param ignoreNotPaired  flag to identify if need to ignore the last one or two elements when the total length of the stream is not multiple of 3. Default value is false
    //     * @return
    //     */
    //    public abstract <R> Stream<R> triMap(TriFunction<? super T, ? super T, ? super T, ? extends R> mapper, boolean ignoreNotPaired);

    @ParallelSupported
    public abstract <R> Stream<R> slidingMap(BiFunction<? super T, ? super T, R> mapper);

    /**
     * Slide with <code>windowSize = 2</code> and the specified <code>increment</code>, then <code>map</code> by the specified <code>mapper</code>.
     * 
     * @param mapper
     * @param increment
     * @return
     */
    @ParallelSupported
    public abstract <R> Stream<R> slidingMap(BiFunction<? super T, ? super T, R> mapper, int increment);

    @ParallelSupported
    public abstract <R> Stream<R> slidingMap(BiFunction<? super T, ? super T, R> mapper, int increment, boolean ignoreNotPaired);

    @ParallelSupported
    public abstract <R> Stream<R> slidingMap(TriFunction<? super T, ? super T, ? super T, R> mapper);

    /**
     * Slide with <code>windowSize = 3</code> and the specified <code>increment</code>, then <code>map</code> by the specified <code>mapper</code>.
     * 
     * @param mapper
     * @param increment
     * @return
     */
    @ParallelSupported
    public abstract <R> Stream<R> slidingMap(TriFunction<? super T, ? super T, ? super T, R> mapper, int increment);

    @ParallelSupported
    public abstract <R> Stream<R> slidingMap(TriFunction<? super T, ? super T, ? super T, R> mapper, int increment, boolean ignoreNotPaired);

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     * 
     * <br />
     * 
     * Returns a stream consisting of results of applying the given function to
     * the ranges created from the source elements.
     * 
     * <pre>
     * <code>
     * Stream.of("a", "ab", "ac", "b", "c", "cb").rangeMap((a, b) -> b.startsWith(a), (a, b) -> a + "->" + b).toList(); // a->ac, b->b, c->cb
     * </code>
     * </pre>
     * 
     * <p>
     * This is a <a href="package-summary.html#StreamOps">quasi-intermediate</a>
     * partial reduction operation.
     * 
     * @param <U> the type of the resulting elements
     * @param sameRange a non-interfering, stateless predicate to apply to
     *        the leftmost and next elements which returns true for elements
     *        which belong to the same range.
     * @param mapper a non-interfering, stateless function to apply to the
     *        range borders and produce the resulting element. If value was
     *        not merged to the interval, then mapper will receive the same
     *        value twice, otherwise it will receive the leftmost and the
     *        rightmost values which were merged to the range.
     * @return the new stream
     * @see #collapse(BiPredicate, BinaryOperator)
     */
    @SequentialOnly
    public abstract <U> Stream<U> rangeMap(final BiPredicate<? super T, ? super T> sameRange, final BiFunction<? super T, ? super T, ? extends U> mapper);

    @ParallelSupported
    public abstract Stream<T> mapFirst(Function<? super T, ? extends T> mapperForFirst);

    @ParallelSupported
    public abstract <R> Stream<R> mapFirstOrElse(Function<? super T, ? extends R> mapperForFirst, Function<? super T, ? extends R> mapperForElse);

    @ParallelSupported
    public abstract Stream<T> mapLast(Function<? super T, ? extends T> mapperForLast);

    @ParallelSupported
    public abstract <R> Stream<R> mapLastOrElse(Function<? super T, ? extends R> mapperForLast, Function<? super T, ? extends R> mapperForElse);

    @ParallelSupported
    public abstract CharStream mapToChar(ToCharFunction<? super T> mapper);

    @ParallelSupported
    public abstract ByteStream mapToByte(ToByteFunction<? super T> mapper);

    @ParallelSupported
    public abstract ShortStream mapToShort(ToShortFunction<? super T> mapper);

    @ParallelSupported
    public abstract IntStream mapToInt(ToIntFunction<? super T> mapper);

    @ParallelSupported
    public abstract LongStream mapToLong(ToLongFunction<? super T> mapper);

    @ParallelSupported
    public abstract FloatStream mapToFloat(ToFloatFunction<? super T> mapper);

    @ParallelSupported
    public abstract DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper);

    // public abstract <K, V> EntryStream<K, V> mapToEntry();

    @ParallelSupported
    public abstract <K, V> EntryStream<K, V> mapToEntry(Function<? super T, ? extends Map.Entry<? extends K, ? extends V>> mapper);

    @ParallelSupported
    public abstract <K, V> EntryStream<K, V> mapToEntry(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper);

    // public abstract <U> Stream<U> mapp(Function<? super T, ? extends Optional<? extends U>> mapper);

    @ParallelSupported
    public abstract <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper);

    @ParallelSupported
    public abstract <R> Stream<R> flattMap(Function<? super T, ? extends Collection<? extends R>> mapper);

    @ParallelSupported
    public abstract <R> Stream<R> flatMapp(Function<? super T, R[]> mapper);

    @ParallelSupported
    public abstract CharStream flatMapToChar(Function<? super T, ? extends CharStream> mapper);

    @ParallelSupported
    public abstract CharStream flattMapToChar(Function<? super T, char[]> mapper);

    @ParallelSupported
    public abstract ByteStream flatMapToByte(Function<? super T, ? extends ByteStream> mapper);

    @ParallelSupported
    public abstract ByteStream flattMapToByte(Function<? super T, byte[]> mapper);

    @ParallelSupported
    public abstract ShortStream flatMapToShort(Function<? super T, ? extends ShortStream> mapper);

    @ParallelSupported
    public abstract ShortStream flattMapToShort(Function<? super T, short[]> mapper);

    @ParallelSupported
    public abstract IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper);

    @ParallelSupported
    public abstract IntStream flattMapToInt(Function<? super T, int[]> mapper);

    @ParallelSupported
    public abstract LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper);

    @ParallelSupported
    public abstract LongStream flattMapToLong(Function<? super T, long[]> mapper);

    @ParallelSupported
    public abstract FloatStream flatMapToFloat(Function<? super T, ? extends FloatStream> mapper);

    @ParallelSupported
    public abstract FloatStream flattMapToFloat(Function<? super T, float[]> mapper);

    @ParallelSupported
    public abstract DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper);

    @ParallelSupported
    public abstract DoubleStream flattMapToDouble(Function<? super T, double[]> mapper);

    @ParallelSupported
    public abstract <K, V> EntryStream<K, V> flatMapToEntry(Function<? super T, ? extends Stream<? extends Map.Entry<? extends K, ? extends V>>> mapper);

    @ParallelSupported
    public abstract <K, V> EntryStream<K, V> flattMapToEntry(Function<? super T, ? extends Map<? extends K, ? extends V>> mapper);

    @ParallelSupported
    public abstract <K, V> EntryStream<K, V> flatMappToEntry(Function<? super T, ? extends EntryStream<? extends K, ? extends V>> mapper);

    @ParallelSupported
    public abstract <K> Stream<Map.Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> keyMapper);

    @ParallelSupported
    public abstract <K> Stream<Map.Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends Map<K, List<T>>> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public abstract <K, V> Stream<Map.Entry<K, List<V>>> groupBy(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V> Stream<Map.Entry<K, List<V>>> groupBy(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Supplier<? extends Map<K, List<V>>> mapFactory);

    @ParallelSupported
    public abstract <K, A, D> Stream<Map.Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream);

    @ParallelSupported
    public abstract <K, A, D> Stream<Map.Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory);

    @ParallelSupported
    public abstract <K, V, A, D> Stream<Map.Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream);

    @ParallelSupported
    public abstract <K, V, A, D> Stream<Map.Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream, final Supplier<? extends Map<K, D>> mapFactory);

    @ParallelSupported
    public abstract <K, V> Stream<Map.Entry<K, V>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction);

    @ParallelSupported
    public abstract <K, V> Stream<Map.Entry<K, V>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory);

    //    @ParallelSupported
    //    public abstract <K> Stream<Map.Entry<K, List<T>>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper);
    //
    //    @ParallelSupported
    //    public abstract <K> Stream<Map.Entry<K, List<T>>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final Supplier<? extends Map<K, List<T>>> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @return
    //     * @see Collectors#toMultimap(Function, Function)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V> Stream<Map.Entry<K, List<V>>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#toMultimap(Function, Function, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V> Stream<Map.Entry<K, List<V>>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends Map<K, List<V>>> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K, A, D> Stream<Map.Entry<K, D>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final Collector<? super T, A, D> downstream);
    //
    //    @ParallelSupported
    //    public abstract <K, A, D> Stream<Map.Entry<K, D>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final Collector<? super T, A, D> downstream, final Supplier<? extends Map<K, D>> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K, V, A, D> Stream<Map.Entry<K, D>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream);
    //
    //    @ParallelSupported
    //    public abstract <K, V, A, D> Stream<Map.Entry<K, D>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream,
    //            final Supplier<? extends Map<K, D>> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K, V> Stream<Map.Entry<K, V>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction);
    //
    //    @ParallelSupported
    //    public abstract <K, V> Stream<Map.Entry<K, V>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction,
    //            final Supplier<? extends Map<K, V>> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K> Stream<Map.Entry<K, List<T>>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper);
    //
    //    @ParallelSupported
    //    public abstract <K> Stream<Map.Entry<K, List<T>>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final Supplier<? extends Map<K, List<T>>> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @return
    //     * @see Collectors#toMultimap(Function, Function)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V> Stream<Map.Entry<K, List<V>>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#toMultimap(Function, Function, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V> Stream<Map.Entry<K, List<V>>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends Map<K, List<V>>> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K, A, D> Stream<Map.Entry<K, D>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final Collector<? super T, A, D> downstream);
    //
    //    @ParallelSupported
    //    public abstract <K, A, D> Stream<Map.Entry<K, D>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final Collector<? super T, A, D> downstream, final Supplier<? extends Map<K, D>> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K, V, A, D> Stream<Map.Entry<K, D>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream);
    //
    //    @ParallelSupported
    //    public abstract <K, V, A, D> Stream<Map.Entry<K, D>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream,
    //            final Supplier<? extends Map<K, D>> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K, V> Stream<Map.Entry<K, V>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction);
    //
    //    @ParallelSupported
    //    public abstract <K, V> Stream<Map.Entry<K, V>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction,
    //            final Supplier<? extends Map<K, V>> mapFactory);

    /**
     * 
     * @param predicate
     * @return
     * @see Collectors#partitioningBy(Predicate)
     */
    @ParallelSupported
    public abstract Stream<Map.Entry<Boolean, List<T>>> partitionBy(final Predicate<? super T> predicate);

    /**
     * 
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    @ParallelSupported
    public abstract <A, D> Stream<Map.Entry<Boolean, D>> partitionBy(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream);

    @ParallelSupported
    public <K> Stream<Map.Entry<K, Integer>> countBy(final Function<? super T, ? extends K> keyMapper) {
        return groupBy(keyMapper, Collectors.countingInt());
    }

    @ParallelSupported
    public abstract <K> EntryStream<K, List<T>> groupByToEntry(final Function<? super T, ? extends K> keyMapper);

    @ParallelSupported
    public abstract <K> EntryStream<K, List<T>> groupByToEntry(final Function<? super T, ? extends K> keyMapper,
            final Supplier<? extends Map<K, List<T>>> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public abstract <K, V> EntryStream<K, List<V>> groupByToEntry(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V> EntryStream<K, List<V>> groupByToEntry(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Supplier<? extends Map<K, List<V>>> mapFactory);

    @ParallelSupported
    public abstract <K, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream);

    @ParallelSupported
    public abstract <K, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory);

    @ParallelSupported
    public abstract <K, V, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream);

    @ParallelSupported
    public abstract <K, V, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream, final Supplier<? extends Map<K, D>> mapFactory);

    @ParallelSupported
    public abstract <K, V> EntryStream<K, V> groupByToEntry(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction);

    @ParallelSupported
    public abstract <K, V> EntryStream<K, V> groupByToEntry(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory);

    /**
     * 
     * @param predicate
     * @return
     * @see Collectors#partitioningBy(Predicate)
     */
    @ParallelSupported
    public abstract EntryStream<Boolean, List<T>> partitionByToEntry(final Predicate<? super T> predicate);

    /**
     * 
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    @ParallelSupported
    public abstract <A, D> EntryStream<Boolean, D> partitionByToEntry(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream);

    @ParallelSupported
    public <K> EntryStream<K, Integer> countByToEntry(final Function<? super T, ? extends K> keyMapper) {
        return groupByToEntry(keyMapper, Collectors.countingInt());
    }

    @SequentialOnly
    public abstract Stream<Stream<T>> collapse(final BiPredicate<? super T, ? super T> collapsible);

    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> collapse(final BiPredicate<? super T, ? super T> collapsible, Supplier<? extends C> supplier);

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     * 
     * <p>Example:
     * <pre>
     * <code>
     * Stream.of(new Integer[0]).collapse((a, b) -> a < b, (a, b) -> a + b) => []
     * Stream.of(1).collapse((a, b) -> a < b, (a, b) -> a + b) => [1]
     * Stream.of(1, 2).collapse((a, b) -> a < b, (a, b) -> a + b) => [3]
     * Stream.of(1, 2, 3).collapse((a, b) -> a < b, (a, b) -> a + b) => [6]
     * Stream.of(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b, (a, b) -> a + b) => [6, 3, 2, 1]
     * </code>
     * </pre>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param collapsible
     * @param mergeFunction
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> collapse(final BiPredicate<? super T, ? super T> collapsible, final BiFunction<? super T, ? super T, T> mergeFunction);

    @SequentialOnly
    public abstract <U> Stream<U> collapse(final BiPredicate<? super T, ? super T> collapsible, final U init, final BiFunction<U, ? super T, U> op);

    @SequentialOnly
    public abstract <R> Stream<R> collapse(final BiPredicate<? super T, ? super T> collapsible, final Supplier<R> supplier,
            final BiConsumer<? super R, ? super T> accumulator);

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     * 
     * <p>Example:
     * <pre>
     * <code>
     * Stream.of(new Integer[0]).collapse((a, b) -> a < b, Collectors.summingInt(Fn.unboxI())) => []
     * Stream.of(1).collapse((a, b) -> a < b, Collectors.summingInt(Fn.unboxI())) => [1]
     * Stream.of(1, 2).collapse((a, b) -> a < b, Collectors.summingInt(Fn.unboxI())) => [3]
     * Stream.of(1, 2, 3).collapse((a, b) -> a < b, Collectors.summingInt(Fn.unboxI())) => [6]
     * Stream.of(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b, Collectors.summingInt(Fn.unboxI())) => [6, 3, 2, 1]
     * </code>
     * </pre>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param collapsible
     * @param collector
     * @return
     */
    @SequentialOnly
    public abstract <R, A> Stream<R> collapse(final BiPredicate<? super T, ? super T> collapsible, final Collector<? super T, A, R> collector);

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
     * <code>
     * Stream.of(new Integer[0]).scan((a, b) -> a + b) => []
     * Stream.of(1).scan((a, b) -> a + b) => [1]
     * Stream.of(1, 2).scan((a, b) -> a + b) => [1, 3]
     * Stream.of(1, 2, 3).scan((a, b) -> a + b) => [1, 3, 6]
     * Stream.of(1, 2, 3, 3, 2, 1).scan((a, b) -> a + b) => [1, 3, 6, 9, 11, 12]
     * </code>
     * </pre>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param accumulator  the accumulation function
     * @return the new stream which has the extract same size as this stream.
     */
    @SequentialOnly
    public abstract Stream<T> scan(final BiFunction<? super T, ? super T, T> accumulator);

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
     * <code>
     * Stream.of(new Integer[0]).scan(10, (a, b) -> a + b) => []
     * Stream.of(1).scan(10, (a, b) -> a + b) => [11]
     * Stream.of(1, 2).scan(10, (a, b) -> a + b) => [11, 13]
     * Stream.of(1, 2, 3).scan(10, (a, b) -> a + b) => [11, 13, 16]
     * Stream.of(1, 2, 3, 3, 2, 1).scan(10, (a, b) -> a + b) => [11, 13, 16, 19, 21, 22]
     * </code>
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
    public abstract <U> Stream<U> scan(final U init, final BiFunction<U, ? super T, U> accumulator);

    /**
     * 
     * @param init
     * @param accumulator
     * @param initIncluded
     * @return
     */
    @SequentialOnly
    public abstract <U> Stream<U> scan(final U init, final BiFunction<U, ? super T, U> accumulator, final boolean initIncluded);

    /**
     * Returns Stream of Stream with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @SequentialOnly
    public abstract Stream<Set<T>> splitToSet(int chunkSize);

    /**
     * Returns Stream of Stream with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @param collectionSupplier
     * @return
     */
    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> split(int chunkSize, IntFunction<? extends C> collectionSupplier);

    /**
     * 
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @param collector
     * @return
     */
    @SequentialOnly
    public abstract <A, R> Stream<R> split(int chunkSize, Collector<? super T, A, R> collector);

    @SequentialOnly
    public abstract Stream<Set<T>> splitToSet(Predicate<? super T> predicate);

    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> split(Predicate<? super T> predicate, Supplier<? extends C> collectionSupplier);

    @SequentialOnly
    public abstract <A, R> Stream<R> split(Predicate<? super T> predicate, Collector<? super T, A, R> collector);

    @SequentialOnly
    public abstract <A, R> Stream<R> splitAt(int where, Collector<? super T, A, R> collector);

    @SequentialOnly
    public abstract <A, R> Stream<R> splitBy(Predicate<? super T> where, Collector<? super T, A, R> collector);

    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> sliding(int windowSize, IntFunction<? extends C> collectionSupplier);

    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> sliding(int windowSize, int increment, IntFunction<? extends C> collectionSupplier);

    @SequentialOnly
    public abstract <A, R> Stream<R> sliding(int windowSize, Collector<? super T, A, R> collector);

    @SequentialOnly
    public abstract <A, R> Stream<R> sliding(int windowSize, int increment, Collector<? super T, A, R> collector);

    /**
     * Split this stream by the specified duration.
     * 
     * @param duration
     * @return
     * @see Fn#window(Duration, LongSupplier)
     */
    @SequentialOnly
    public abstract Stream<Stream<T>> window(Duration duration);

    /**
     * 
     * @param duration
     * @param startTime
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract Stream<Stream<T>> window(Duration duration, LongSupplier startTime);

    /**
     * 
     * @param duration
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract Stream<List<T>> windowToList(Duration duration);

    /**
     * 
     * @param duration
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract Stream<Set<T>> windowToSet(Duration duration);

    /**
     * 
     * @param duration
     * @param collectionSupplier
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> window(Duration duration, Supplier<? extends C> collectionSupplier);

    /**
     * 
     * @param duration
     * @param startTime
     * @param collectionSupplier
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> window(Duration duration, LongSupplier startTime, Supplier<? extends C> collectionSupplier);

    /**
     * 
     * @param duration
     * @param collector
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <A, R> Stream<R> window(Duration duration, Collector<? super T, A, R> collector);

    /**
     * 
     * @param duration
     * @param startTime
     * @param collector
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <A, R> Stream<R> window(Duration duration, LongSupplier startTime, Collector<? super T, A, R> collector);

    /**
     * 
     * @param duration
     * @param incrementInMillis
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract Stream<Stream<T>> window(Duration duration, long incrementInMillis);

    /**
     * 
     * @param duration
     * @param incrementInMillis
     * @param startTime
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract Stream<Stream<T>> window(Duration duration, long incrementInMillis, LongSupplier startTime);

    /**
     * 
     * @param duration
     * @param incrementInMillis
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract Stream<List<T>> windowToList(Duration duration, long incrementInMillis);

    /**
     * 
     * @param duration
     * @param incrementInMillis
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract Stream<Set<T>> windowToSet(Duration duration, long incrementInMillis);

    /**
     * 
     * @param duration
     * @param incrementInMillis
     * @param collectionSupplier
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> window(Duration duration, long incrementInMillis, Supplier<? extends C> collectionSupplier);

    /**
     * 
     * @param duration
     * @param incrementInMillis
     * @param startTime
     * @param collectionSupplier
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> window(Duration duration, long incrementInMillis, LongSupplier startTime,
            Supplier<? extends C> collectionSupplier);

    /**
     * 
     * @param duration
     * @param incrementInMillis
     * @param collector
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <A, R> Stream<R> window(Duration duration, long incrementInMillis, Collector<? super T, A, R> collector);

    /**
     * 
     * @param duration
     * @param incrementInMillis
     * @param startTime
     * @param collector
     * @return
     * @see #window(Duration)
     * @see Fn#window(Duration, LongSupplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <A, R> Stream<R> window(Duration duration, long incrementInMillis, LongSupplier startTime, Collector<? super T, A, R> collector);

    /** 
     * 
     * @param maxWindowSize
     * @param maxDuration
     * @return
     * @see #window(int, Duration, LongSupplier, Supplier)
     * @see Fn#window(int, Duration, LongSupplier, Supplier)
     */
    @SequentialOnly
    public abstract Stream<Stream<T>> window(int maxWindowSize, Duration maxDuration);

    /**
     * 
     * @param maxWindowSize
     * @param maxDuration
     * @param startTime
     * @return
     * @see #window(int, Duration, LongSupplier, Supplier)
     * @see Fn#window(int, Duration, LongSupplier, Supplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract Stream<Stream<T>> window(int maxWindowSize, Duration maxDuration, LongSupplier startTime);

    /**
     * 
     * @param maxWindowSize 
     * @param maxDuration
     * @param collectionSupplier
     * @return
     * @see #window(int, Duration, LongSupplier, Supplier)
     * @see Fn#window(int, Duration, LongSupplier, Supplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> window(int maxWindowSize, Duration maxDuration, Supplier<? extends C> collectionSupplier);

    /**
     * Split this stream at where {@code maxWindowSize} or {@code maxDuration} reaches first.
     * 
     * @param maxWindowSize
     * @param maxDuration
     * @param startTime
     * @param collectionSupplier
     * @return
     * @see #window(int, Duration, LongSupplier, Supplier)
     * @see Fn#window(int, Duration, LongSupplier, Supplier)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <C extends Collection<T>> Stream<C> window(int maxWindowSize, Duration maxDuration, LongSupplier startTime,
            Supplier<? extends C> collectionSupplier);

    /**
     * 
     * @param maxWindowSize
     * @param maxDuration
     * @param collector
     * @return
     * @see #window(int, Duration, LongSupplier, Collector)
     * @see Fn#window(int, Duration, LongSupplier, Collector)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <A, R> Stream<R> window(int maxWindowSize, Duration maxDuration, Collector<? super T, A, R> collector);

    /**
     * Split this stream at where {@code maxWindowSize} or {@code maxDuration} reaches first.
     * 
     * @param maxWindowSize
     * @param maxDuration
     * @param startTime
     * @param collector
     * @return
     * @see #window(Duration, long, LongSupplier, Collector)
     * @see Fn#window(int, Duration, LongSupplier, Collector)
     * @see #sliding(int, int, Collector)
     */
    @SequentialOnly
    public abstract <A, R> Stream<R> window(int maxWindowSize, Duration maxDuration, LongSupplier startTime, Collector<? super T, A, R> collector);

    /**
     * <code>Stream.of(1).intersperse(9) --> [1]</code>
     * <code>Stream.of(1, 2, 3).intersperse(9) --> [1, 9, 2, 9, 3]</code>
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param delimiter
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> intersperse(T delimiter);

    /**
     * Distinct and filter by occurrences.
     * 
     * @param occurrencesFilter
     * @return
     */
    @ParallelSupported
    public Stream<T> distinct(final Predicate<? super Long> occurrencesFilter) {
        final Supplier<? extends Map<T, Long>> supplier = isParallel() ? Suppliers.<T, Long> ofConcurrentHashMap() : Suppliers.<T, Long> ofLinkedHashMap();

        return groupBy(Fn.<T> identity(), Collectors.counting(), supplier).filter(Fn.<T, Long> testByValue(occurrencesFilter)).map(Fn.<T, Long> key());
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code> 
     * 
     * @param keyMapper don't change value of the input parameter.
     * @return
     */
    @ParallelSupported
    public abstract Stream<T> distinctBy(Function<? super T, ?> keyMapper);

    /**
     * Distinct and filter by occurrences.
     * 
     * @param keyMapper
     * @param occurrencesFilter
     * @return
     */
    public <K> Stream<T> distinctBy(final Function<? super T, K> keyMapper, final Predicate<? super Long> occurrencesFilter) {
        final Supplier<? extends Map<Keyed<K, T>, Long>> supplier = isParallel() ? Suppliers.<Keyed<K, T>, Long> ofConcurrentHashMap()
                : Suppliers.<Keyed<K, T>, Long> ofLinkedHashMap();

        return groupBy(Fn.<K, T> keyed(keyMapper), Collectors.counting(), supplier).filter(Fn.<Keyed<K, T>, Long> testByValue(occurrencesFilter))
                .map(Fn.<T, K, Long> kk());
    }

    /**
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> top(int n);

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @param comparator
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> top(int n, Comparator<? super T> comparator);

    @ParallelSupported
    public abstract Stream<T> sorted(Comparator<? super T> comparator);

    @SuppressWarnings("rawtypes")
    @ParallelSupported
    public abstract Stream<T> sortedBy(Function<? super T, ? extends Comparable> keyMapper);

    @ParallelSupported
    public abstract Stream<T> sortedByInt(ToIntFunction<? super T> keyMapper);

    @ParallelSupported
    public abstract Stream<T> sortedByLong(ToLongFunction<? super T> keyMapper);

    @ParallelSupported
    public abstract Stream<T> sortedByDouble(ToDoubleFunction<? super T> keyMapper);

    @ParallelSupported
    public abstract <E extends Exception> void forEach(Try.Consumer<? super T, E> action) throws E;

    @ParallelSupported
    public abstract <E extends Exception, E2 extends Exception> void forEach(Try.Consumer<? super T, E> action, Try.Runnable<E2> onComplete) throws E, E2;

    public abstract <U, E extends Exception, E2 extends Exception> void forEach(final Try.Function<? super T, ? extends Collection<U>, E> flatMapper,
            final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2;

    public abstract <T2, T3, E extends Exception, E2 extends Exception, E3 extends Exception> void forEach(
            final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper, final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2,
            final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action) throws E, E2, E3;

    @ParallelSupported
    public abstract <E extends Exception> void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action) throws E;

    /**
     * Slide with <code>windowSize = 2</code> and the specified <code>increment</code>, then <code>consume</code> by the specified <code>mapper</code>.
     * 
     * @param mapper
     * @param increment
     * @return
     */
    @ParallelSupported
    public abstract <E extends Exception> void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action, final int increment) throws E;

    @ParallelSupported
    public abstract <E extends Exception> void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action) throws E;

    /**
     * Slide with <code>windowSize = 3</code> and the specified <code>increment</code>, then <code>consume</code> by the specified <code>mapper</code>.
     * 
     * @param mapper
     * @param increment
     * @return
     */
    @ParallelSupported
    public abstract <E extends Exception> void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action, final int increment) throws E;

    @ParallelSupported
    public abstract <E extends Exception> boolean anyMatch(Try.Predicate<? super T, E> predicate) throws E;

    @ParallelSupported
    public abstract <E extends Exception> boolean allMatch(Try.Predicate<? super T, E> predicate) throws E;

    @ParallelSupported
    public abstract <E extends Exception> boolean noneMatch(Try.Predicate<? super T, E> predicate) throws E;

    @ParallelSupported
    public abstract <E extends Exception> boolean nMatch(long atLeast, long atMost, Try.Predicate<? super T, E> predicate) throws E;

    @ParallelSupported
    public abstract <E extends Exception> Optional<T> findFirst(Try.Predicate<? super T, E> predicate) throws E;

    @ParallelSupported
    public abstract <E extends Exception> Optional<T> findLast(Try.Predicate<? super T, E> predicate) throws E;

    @SequentialOnly
    public abstract <E extends Exception, E2 extends Exception> Optional<T> findFirstOrLast(Try.Predicate<? super T, E> predicateForFirst,
            Try.Predicate<? super T, E2> predicateForLast) throws E, E2;

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param init
     * @param predicateForFirst
     * @param predicateForLast
     * @return
     */
    @SequentialOnly
    public abstract <U, E extends Exception, E2 extends Exception> Optional<T> findFirstOrLast(final U init,
            final Try.BiPredicate<? super T, ? super U, E> predicateForFirst, final Try.BiPredicate<? super T, ? super U, E2> predicateForLast) throws E, E2;

    /**
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param preFunc
     * @param predicateForFirst
     * @param predicateForLast
     * @return
     */
    @SequentialOnly
    public abstract <U, E extends Exception, E2 extends Exception> Optional<T> findFirstOrLast(final Function<? super T, U> preFunc,
            final Try.BiPredicate<? super T, ? super U, E> predicateForFirst, final Try.BiPredicate<? super T, ? super U, E2> predicateForLast) throws E, E2;

    @ParallelSupported
    public abstract <E extends Exception> Optional<T> findAny(Try.Predicate<? super T, E> predicate) throws E;

    @SequentialOnly
    public abstract boolean containsAll(T... a);

    @SequentialOnly
    public abstract boolean containsAll(Collection<? extends T> c);

    @SequentialOnly
    public abstract <A> A[] toArray(IntFunction<A[]> generator);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    public <K, V> ImmutableMap<K, V> toImmutableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return ImmutableMap.of(toMap(keyMapper, valueMapper));
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    public <K, V> ImmutableMap<K, V> toImmutableMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        return ImmutableMap.of(toMap(keyMapper, valueMapper, mergeFunction));
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    public abstract <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    public abstract <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V, M extends Map<K, V>> M toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    public abstract <K, V, M extends Map<K, V>> M toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    public abstract <K, A, D> Map<K, D> toMap(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream);

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    public abstract <K, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream,
            final Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    public abstract <K, V, A, D> Map<K, D> toMap(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, A, D> downstream);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    public abstract <K, V, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream, final Supplier<? extends M> mapFactory);

    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @return
    //     * @see Collectors#toMap(Function, Function)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V> Map<K, V> flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mergeFunction
    //     * @return
    //     * @see Collectors#toMap(Function, Function, BinaryOperator)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V> Map<K, V> flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#toMap(Function, Function, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, M extends Map<K, V>> M flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mergeFunction
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, M extends Map<K, V>> M flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper 
    //     * @param downstream
    //     * @return
    //     * @see Collectors#groupingBy(Function, Collector)
    //     */
    //    @ParallelSupported
    //    public abstract <K, A, D> Map<K, D> flatToMap(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final Collector<? super T, A, D> downstream);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param downstream
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#groupingBy(Function, Collector, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, A, D, M extends Map<K, D>> M flatToMap(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param downstream
    //     * @return
    //     * @see Collectors#groupingBy(Function, Collector)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, A, D> Map<K, D> flatToMap(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param downstream
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#groupingBy(Function, Collector, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, A, D, M extends Map<K, D>> M flatToMap(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream,
    //            final Supplier<? extends M> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @return
    //     * @see Collectors#flattToMap(Function, Function)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V> Map<K, V> flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mergeFunction
    //     * @return
    //     * @see Collectors#flattToMap(Function, Function, BinaryOperator)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V> Map<K, V> flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#flattToMap(Function, Function, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, M extends Map<K, V>> M flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mergeFunction
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#flattToMap(Function, Function, BinaryOperator, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, M extends Map<K, V>> M flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction, Supplier<? extends M> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param downstream
    //     * @return
    //     * @see Collectors#groupingBy(Function, Collector)
    //     */
    //    @ParallelSupported
    //    public abstract <K, A, D> Map<K, D> flattToMap(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final Collector<? super T, A, D> downstream);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param downstream
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#groupingBy(Function, Collector, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, A, D, M extends Map<K, D>> M flattToMap(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param downstream
    //     * @return
    //     * @see Collectors#groupingBy(Function, Collector)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, A, D> Map<K, D> flattToMap(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param downstream
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#groupingBy(Function, Collector, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, A, D, M extends Map<K, D>> M flattToMap(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream,
    //            final Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @return
     * @see Collectors#groupingBy(Function)
     */
    @ParallelSupported
    public abstract <K> Map<K, List<T>> groupTo(Function<? super T, ? extends K> keyMapper);

    /**
     * 
     * @param keyMapper
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, M extends Map<K, List<T>>> M groupTo(final Function<? super T, ? extends K> keyMapper, final Supplier<? extends M> mapFactory);

    @ParallelSupported
    public abstract <K, V> Map<K, List<V>> groupTo(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V, M extends Map<K, List<V>>> M groupTo(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @return
    //     * @see Collectors#groupingBy(Function)
    //     */
    //    @ParallelSupported
    //    public abstract <K> Map<K, List<T>> flatGroupTo(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#groupingBy(Function, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, M extends Map<K, List<T>>> M flatGroupTo(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final Supplier<? extends M> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K, V> Map<K, List<V>> flatGroupTo(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#toMultimap(Function, Function, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, M extends Map<K, List<V>>> M flatGroupTo(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @return
    //     * @see Collectors#groupingBy(Function)
    //     */
    //    @ParallelSupported
    //    public abstract <K> Map<K, List<T>> flattGroupTo(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#groupingBy(Function, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, M extends Map<K, List<T>>> M flattGroupTo(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final Supplier<? extends M> mapFactory);
    //
    //    @ParallelSupported
    //    public abstract <K, V> Map<K, List<V>> flattGroupTo(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper);
    //
    //    /**
    //     * 
    //     * @param flatKeyMapper
    //     * @param valueMapper
    //     * @param mapFactory
    //     * @return
    //     * @see Collectors#toMultimap(Function, Function, Supplier)
    //     */
    //    @ParallelSupported
    //    public abstract <K, V, M extends Map<K, List<V>>> M flattGroupTo(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param predicate
     * @return
     * @see Collectors#partitioningBy(Predicate)
     */
    @ParallelSupported
    public abstract Map<Boolean, List<T>> partitionTo(final Predicate<? super T> predicate);

    /**
     * 
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    @ParallelSupported
    public abstract <A, D> Map<Boolean, D> partitionTo(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream);

    /**
     * 
     * @param keyMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public abstract <K> ListMultimap<K, T> toMultimap(Function<? super T, ? extends K> keyMapper);

    /**
     * 
     * @param keyMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(Function<? super T, ? extends K> keyMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public abstract <K, V> ListMultimap<K, V> toMultimap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper);

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M toMultimap(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param flatKeyMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public abstract <K> ListMultimap<K, T> flatToMultimap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper);

    /**
     * 
     * @param flatKeyMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V extends Collection<T>, M extends Multimap<K, T, V>> M flatToMultimap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param flatKeyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public abstract <K, V> ListMultimap<K, V> flatToMultimap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
            BiFunction<? super K, ? super T, ? extends V> valueMapper);

    /**
     * 
     * @param flatKeyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M flatToMultimap(
            Function<? super T, ? extends Stream<? extends K>> flatKeyMapper, BiFunction<? super K, ? super T, ? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @param flatKeyMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public abstract <K> ListMultimap<K, T> flattToMultimap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper);

    /**
     * 
     * @param flatKeyMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V extends Collection<T>, M extends Multimap<K, T, V>> M flattToMultimap(
            Function<? super T, ? extends Collection<? extends K>> flatKeyMapper, Supplier<? extends M> mapFactory);

    /**
     * 
     * @param flatKeyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public abstract <K, V> ListMultimap<K, V> flattToMultimap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
            BiFunction<? super K, ? super T, ? extends V> valueMapper);

    /**
     * 
     * @param flatKeyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public abstract <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M flattToMultimap(
            Function<? super T, ? extends Collection<? extends K>> flatKeyMapper, BiFunction<? super K, ? super T, ? extends V> valueMapper,
            Supplier<? extends M> mapFactory);

    /**
     * 
     * @return
     */
    @SequentialOnly
    public abstract DataSet toDataSet();

    /**
     * @param isFirstTitle
     * @return
     */
    @SequentialOnly
    public abstract DataSet toDataSet(boolean isFirstTitle);

    /**
     * 
     * @param columnNames it can be null or empty if this is Map or entity stream.
     * @return
     */
    @SequentialOnly
    public abstract DataSet toDataSet(final List<String> columnNames);

    @ParallelSupported
    public abstract T reduce(T identity, BinaryOperator<T> accumulator);

    @ParallelSupported
    public abstract Optional<T> reduce(BinaryOperator<T> accumulator);

    @ParallelSupported
    public abstract <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner);

    @ParallelSupported
    public abstract <R> R collect(Supplier<R> supplier, BiConsumer<? super R, ? super T> accumulator, BiConsumer<R, R> combiner);

    @ParallelSupported
    public abstract <R> R collect(Supplier<R> supplier, BiConsumer<? super R, ? super T> accumulator);

    @ParallelSupported
    public abstract <R, A> R collect(Collector<? super T, A, R> collector);

    public abstract <R, A> R collect(java.util.stream.Collector<? super T, A, R> collector);

    @ParallelSupported
    public abstract <R, A, RR> RR collectAndThen(Collector<? super T, A, R> downstream, Function<? super R, RR> func);

    @ParallelSupported
    public abstract <R, A, RR> RR collectAndThen(java.util.stream.Collector<? super T, A, R> downstream, java.util.function.Function<? super R, RR> func);

    @SequentialOnly
    public abstract <R> R toListAndThen(Function<? super List<T>, R> func);

    @SequentialOnly
    public abstract <R> R toSetAndThen(Function<? super Set<T>, R> func);

    /**
     * A queue with size up to <code>n</code> will be maintained to filter out the last <code>n</code> elements. 
     * It may cause <code>out of memory error</code> if <code>n</code> is big enough.
     * 
     * <br />
     * 
     * All the elements will be loaded to get the last {@code n} elements and the Stream will be closed after that, if a terminal operation is triggered.
     *
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> last(int n);

    /**
     * A queue with size up to <code>n</code> will be maintained to filter out the last <code>n</code> elements. 
     * It may cause <code>out of memory error</code> if <code>n</code> is big enough.
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param n
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> skipLast(int n);

    @ParallelSupported
    public abstract Optional<T> min(Comparator<? super T> comparator);

    @SuppressWarnings("rawtypes")
    @ParallelSupported
    public Optional<T> minBy(final Function<? super T, ? extends Comparable> keyMapper) {
        final Comparator<? super T> comparator = Fn.comparingBy(keyMapper);

        return min(comparator);
    }

    @ParallelSupported
    public abstract Optional<T> max(Comparator<? super T> comparator);

    @SuppressWarnings("rawtypes")
    @ParallelSupported
    public Optional<T> maxBy(final Function<? super T, ? extends Comparable> keyMapper) {
        final Comparator<? super T> comparator = Fn.comparingBy(keyMapper);

        return max(comparator);
    }

    /**
     * 
     * @param k
     * @param comparator
     * @return Optional.empty() if there is no element or count less than k, otherwise the kth largest element.
     */
    @ParallelSupported
    public abstract Optional<T> kthLargest(int k, Comparator<? super T> comparator);

    @ParallelSupported
    public abstract int sumInt(ToIntFunction<? super T> mapper);

    @ParallelSupported
    public abstract long sumLong(ToLongFunction<? super T> mapper);

    @ParallelSupported
    public abstract double sumDouble(ToDoubleFunction<? super T> mapper);

    @ParallelSupported
    public abstract OptionalDouble averageInt(ToIntFunction<? super T> mapper);

    @ParallelSupported
    public abstract OptionalDouble averageLong(ToLongFunction<? super T> mapper);

    @ParallelSupported
    public abstract OptionalDouble averageDouble(ToDoubleFunction<? super T> mapper);

    @SequentialOnly
    public abstract Optional<Map<Percentage, T>> percentiles(Comparator<? super T> comparator);

    /**
     * <pre>
     * <code>
     * Stream.of(1, 2, 3).combinations().forEach(Fn.println());
     * // output
     * []
     * [1]
     * [2]
     * [3]
     * [1, 2]
     * [1, 3]
     * [2, 3]
     * [1, 2, 3]
     * </code>
     * </pre>
     * 
     * @return
     */
    @SequentialOnly
    public abstract Stream<List<T>> combinations();

    /**
     * <pre>
     * <code>
     * Stream.of(1, 2, 3).combinations(2).forEach(Fn.println());
     * // output
     * [1, 2]
     * [1, 3]
     * [2, 3]
     * </code>
     * </pre>
     * 
     * @param len
     * @return
     */
    @SequentialOnly
    public abstract Stream<List<T>> combinations(int len);

    /**
     * It's same as {@code N.cartesianProduct(N.repeat(toList(), len))} if {@code repeat} is {@code true}.
     * <pre>
     * <code>
     * Stream.of(1, 2, 3).combinations(2, true).forEach(Fn.println());
     * // output
     * [1, 1]
     * [1, 2]
     * [1, 3]
     * [2, 1]
     * [2, 2]
     * [2, 3]
     * [3, 1]
     * [3, 2]
     * [3, 3]
     * </code>
     * </pre>
     * 
     * @param len
     * @param repeat
     * @return
     */
    @SequentialOnly
    public abstract Stream<List<T>> combinations(int len, boolean repeat);

    /**
     * <pre>
     * <code>
     * Stream.of(1, 2, 3).permutations().forEach(Fn.println());
     * // output
     * [1, 2, 3]
     * [1, 3, 2]
     * [3, 1, 2]
     * [3, 2, 1]
     * [2, 3, 1]
     * [2, 1, 3]
     * </code>
     * </pre>
     * 
     * @return
     */
    @SequentialOnly
    public abstract Stream<List<T>> permutations();

    /**
     * <pre>
     * <code>
     * Stream.of(1, 2, 3).orderedPermutations().forEach(Fn.println());
     * // output
     * [1, 2, 3]
     * [1, 3, 2]
     * [2, 1, 3]
     * [2, 3, 1]
     * [3, 1, 2]
     * [3, 2, 1]
     * </code>
     * </pre>
     * 
     * @return
     */
    @SequentialOnly
    public abstract Stream<List<T>> orderedPermutations();

    @SequentialOnly
    public abstract Stream<List<T>> orderedPermutations(Comparator<? super T> comparator);

    @SequentialOnly
    @SafeVarargs
    public final Stream<List<T>> cartesianProduct(Collection<? extends T>... cs) {
        return cartesianProduct(Arrays.asList(cs));
    }

    @SequentialOnly
    public abstract Stream<List<T>> cartesianProduct(Collection<? extends Collection<? extends T>> cs);

    /**
     * 
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    @ParallelSupported
    public abstract <U> Stream<Pair<T, U>> innerJoin(Collection<U> b, Function<? super T, ?> leftKeyMapper, Function<? super U, ?> rightKeyMapper);

    /**
     * 
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param predicate
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    @ParallelSupported
    public abstract <U> Stream<Pair<T, U>> innerJoin(Collection<U> b, BiPredicate<? super T, ? super U> predicate);

    /**
     * 
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    @ParallelSupported
    public abstract <U> Stream<Pair<T, U>> fullJoin(Collection<U> b, Function<? super T, ?> leftKeyMapper, Function<? super U, ?> rightKeyMapper);

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param predicate
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    @ParallelSupported
    public abstract <U> Stream<Pair<T, U>> fullJoin(Collection<U> b, BiPredicate<? super T, ? super U> predicate);

    /**
     * 
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    @ParallelSupported
    public abstract <U> Stream<Pair<T, U>> leftJoin(Collection<U> b, Function<? super T, ?> leftKeyMapper, Function<? super U, ?> rightKeyMapper);

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param predicate
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    @ParallelSupported
    public abstract <U> Stream<Pair<T, U>> leftJoin(Collection<U> b, BiPredicate<? super T, ? super U> predicate);

    /**
     * 
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    @ParallelSupported
    public abstract <U> Stream<Pair<T, U>> rightJoin(Collection<U> b, Function<? super T, ?> leftKeyMapper, Function<? super U, ?> rightKeyMapper);

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     * 
     * @param b
     * @param predicate
     * @return
     * @see <a href="http://stackoverflow.com/questions/5706437/whats-the-difference-between-inner-join-left-join-right-join-and-ful
     */
    @ParallelSupported
    public abstract <U> Stream<Pair<T, U>> rightJoin(Collection<U> b, BiPredicate<? super T, ? super U> predicate);

    @SequentialOnly
    public abstract boolean hasDuplicates();

    @ParallelSupported
    public abstract Stream<T> skipNull();

    @ParallelSupported
    public abstract Stream<T> peekFirst(Consumer<? super T> action);

    @ParallelSupported
    public abstract Stream<T> peekLast(Consumer<? super T> action);

    /**
     * Intersect with the specified Collection by the values mapped by <code>mapper</code>.
     * 
     * @param mapper
     * @param c
     * @return
     * @see IntList#intersection(IntList)
     */
    @ParallelSupported
    public abstract Stream<T> intersection(Function<? super T, ?> mapper, Collection<?> c);

    /**
     * Except with the specified Collection by the values mapped by <code>mapper</code>.
     * 
     * @param mapper
     * @param c
     * @return
     * @see IntList#difference(IntList)
     */
    @ParallelSupported
    public abstract Stream<T> difference(Function<? super T, ?> mapper, Collection<?> c);

    @SafeVarargs
    @SequentialOnly
    public final Stream<T> append(T... a) {
        return append(Arrays.asList(a));
    }

    @SequentialOnly
    public abstract Stream<T> append(Collection<? extends T> c);

    @SequentialOnly
    public abstract Stream<T> appendAlll(Collection<? extends Collection<? extends T>> cs);

    @SafeVarargs
    @SequentialOnly
    public final Stream<T> prepend(T... a) {
        return prepend(Arrays.asList(a));
    }

    @SequentialOnly
    public abstract Stream<T> prepend(Collection<? extends T> c);

    @SequentialOnly
    public abstract Stream<T> prependAlll(Collection<? extends Collection<? extends T>> cs);

    @SafeVarargs
    @SequentialOnly
    public final Stream<T> appendIfEmpty(T... a) {
        return appendIfEmpty(Arrays.asList(a));
    }

    @SequentialOnly
    public abstract Stream<T> appendIfEmpty(Collection<? extends T> c);

    //    /**
    //     * Returns a reusable stream which can be repeatedly used.
    //     * 
    //     * <br />
    //     * All elements will be loaded to memory.
    //     * 
    //     * @param generator
    //     * @return
    //     */
    //    @SequentialOnly
    //    public abstract Stream<T> cached(IntFunction<T[]> generator);

    /**
     * The Stream will be closed finally, no matter it's empty or not.
     * 
     * @param func
     * @return
     */
    @Beta
    public abstract <R, E extends Exception> Optional<R> applyIfNotEmpty(Try.Function<? super Stream<T>, R, E> func) throws E;

    /**
     * The Stream will be closed finally, no matter it's empty or not.
     * 
     * @param action
     * 
     */
    @Beta
    public abstract <E extends Exception> void acceptIfNotEmpty(Try.Consumer<? super Stream<T>, E> action) throws E;

    /**
     * Returns a new Stream with elements from a temporary queue which is filled by reading the elements from this Stream asynchronously.
     * Default queue size is 64.
     * 
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> queued();

    /**
     * Returns a new Stream with elements from a temporary queue which is filled by reading the elements from this Stream asynchronously.
     * 
     * @param queueSize
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> queued(int queueSize);

    /**
     * 
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    @SequentialOnly
    public abstract Stream<T> merge(final Stream<? extends T> b, final BiFunction<? super T, ? super T, Nth> nextSelector);

    @ParallelSupported
    public abstract <T2, R> Stream<R> zipWith(final Stream<T2> b, final BiFunction<? super T, ? super T2, R> zipFunction);

    @ParallelSupported
    public abstract <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final TriFunction<? super T, ? super T2, ? super T3, R> zipFunction);

    @ParallelSupported
    public abstract <T2, R> Stream<R> zipWith(final Stream<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final BiFunction<? super T, ? super T2, R> zipFunction);

    @ParallelSupported
    public abstract <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final TriFunction<? super T, ? super T2, ? super T3, R> zipFunction);

    @SequentialOnly
    public abstract long persist(File file, Try.Function<? super T, String, IOException> toLine) throws IOException;

    @SequentialOnly
    public abstract long persist(OutputStream os, Try.Function<? super T, String, IOException> toLine) throws IOException;

    @SequentialOnly
    public abstract long persist(Writer writer, Try.Function<? super T, String, IOException> toLine) throws IOException;

    @SequentialOnly
    public abstract long persist(final Connection conn, final String insertSQL, final int batchSize, final int batchInterval,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) throws SQLException;

    @SequentialOnly
    public abstract long persist(final PreparedStatement stmt, final int batchSize, final int batchInterval,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) throws SQLException;

    /**
     * Remember to close this Stream after the iteration is done, if required.
     * 
     * @return
     */
    @SequentialOnly
    public abstract java.util.stream.Stream<T> toJdkStream();

    /**
     * Remember to close this Stream after the iteration is done, if required.
     * 
     * @return
     */
    @SequentialOnly
    @Override
    public ObjIterator<T> iterator() {
        if (isEmptyCloseHandlers(closeHandlers) == false) {
            if (logger.isWarnEnabled()) {
                logger.warn("### Remember to close " + ClassUtil.getSimpleClassName(getClass()));
            }
        }

        return iteratorEx();
    }

    abstract ObjIteratorEx<T> iteratorEx();

    /** 
     * 
     * @param action a terminal operation should be called.
     * @return
     */
    public <E extends Exception> ContinuableFuture<Void> asyncRun(final Try.Consumer<? super Stream<T>, E> action) {
        checkArgNotNull(action, "action");

        return DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<E>() {
            @Override
            public void run() throws E {
                action.accept(Stream.this);
            }
        });
    }

    /**
     * 
     * @param action a terminal operation should be called.
     * @param executor
     * @return
     */
    public <E extends Exception> ContinuableFuture<Void> asyncRun(final Try.Consumer<? super Stream<T>, E> action, final Executor executor) {
        checkArgNotNull(action, "action");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.run(new Try.Runnable<E>() {
            @Override
            public void run() throws E {
                action.accept(Stream.this);
            }
        }, executor);
    }

    /**
     * 
     * @param action a terminal operation should be called.
     * @return
     */
    public <R, E extends Exception> ContinuableFuture<R> asyncCall(final Try.Function<? super Stream<T>, R, E> action) {
        checkArgNotNull(action, "action");

        return DEFAULT_ASYNC_EXECUTOR.execute(new Try.Callable<R, E>() {
            @Override
            public R call() throws E {
                return action.apply(Stream.this);
            }
        });
    }

    /**
     * 
     * @param action a terminal operation should be called.
     * @param executor
     * @return
     */
    public <R, E extends Exception> ContinuableFuture<R> asyncCall(final Try.Function<? super Stream<T>, R, E> action, final Executor executor) {
        checkArgNotNull(action, "action");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.call(new Try.Callable<R, E>() {
            @Override
            public R call() throws E {
                return action.apply(Stream.this);
            }
        }, executor);
    }

    @SequentialOnly
    @Override
    public <R> R __(Function<? super Stream<T>, R> transfer) {
        return transfer.apply(this);
    }

    /**
     * To reduce the memory footprint, Only one instance of <code>DisposableEntry</code> is created, 
     * and the same entry instance is returned and set with different keys/values during iteration of the returned stream.
     * The elements only can be retrieved one by one, can't be modified or saved.
     * The returned Stream doesn't support the operations which require two or more elements at the same time: (e.g. sort/distinct/pairMap/slidingMap/sliding/split/toList/toSet/...).
     * , and can't be parallel stream.
     * Operations: filter/map/toMap/groupBy/groupTo/... are supported. 
     * 
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * 
     * @see DisposableEntry
     * @see NoCachingNoUpdating
     * 
     * @deprecated
     */
    @Deprecated
    @SequentialOnly
    @Beta
    public abstract <K, V> Stream<DisposableEntry<K, V>> mapToDisposableEntry(Function<? super T, ? extends K> keyMapper,
            Function<? super T, ? extends V> valueMapper);

    public static <T> Stream<T> empty() {
        return new ArrayStream<>((T[]) N.EMPTY_OBJECT_ARRAY, true, NATURAL_COMPARATOR, null);
    }

    public static <T> Stream<T> just(final T a) {
        return of(N.asArray(a));
    }

    public static <T> Stream<T> ofNullable(T t) {
        return t == null ? Stream.<T> empty() : of(t);
    }

    @SafeVarargs
    public static <T> Stream<T> of(final T... a) {
        return N.isNullOrEmpty(a) ? (Stream<T>) empty() : of(a, 0, a.length);
    }

    /**
     *
     * @param a
     * @param startIndex
     * @param endIndex
     * @return
     */
    public static <T> Stream<T> of(final T[] a, final int startIndex, final int endIndex) {
        return N.isNullOrEmpty(a) && (startIndex == 0 && endIndex == 0) ? (Stream<T>) empty() : new ArrayStream<>(a, startIndex, endIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public static <T> Stream<T> of(final Collection<? extends T> c) {
        return N.isNullOrEmpty(c) ? (Stream<T>) empty() : of(c, 0, c.size());
    }

    /**
     * 
     * @param c
     * @param startIndex
     * @param endIndex
     * @return
     */
    public static <T> Stream<T> of(final Collection<? extends T> c, int startIndex, int endIndex) {
        N.checkFromToIndex(startIndex, endIndex, N.size(c));

        if (N.isNullOrEmpty(c) && (startIndex == 0 && endIndex == 0)) {
            return empty();
        }

        // return new CollectionStream<T>(c);
        // return new ArrayStream<T>((T[]) c.toArray()); // faster

        if (isListElementDataFieldGettable && listElementDataField != null && c instanceof ArrayList) {
            T[] array = null;

            try {
                array = (T[]) listElementDataField.get(c);
            } catch (Throwable e) {
                // ignore;
                isListElementDataFieldGettable = false;
            }

            if (array != null) {
                return of(array, startIndex, endIndex);
            }
        }

        if (startIndex == 0 && endIndex == c.size()) {
            // return (c.size() > 10 && (c.size() < 1000 || (c.size() < 100000 && c instanceof ArrayList))) ? streamOf((T[]) c.toArray()) : c.stream();
            return of(ObjIteratorEx.of(c));
        } else {
            return of(ObjIteratorEx.of(c), startIndex, endIndex);
        }
    }

    public static <K, V> Stream<Map.Entry<K, V>> of(final Map<K, V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.entrySet());
    }

    public static <T> Stream<T> of(final Iterable<? extends T> iterable) {
        if (iterable == null) {
            return Stream.<T> empty();
        } else if (iterable instanceof Collection) {
            return of((Collection<T>) iterable);
        } else {
            return of(iterable.iterator());
        }
    }

    /**
     *
     * @param iterator
     * @return
     */
    public static <T> Stream<T> of(final Iterator<? extends T> iterator) {
        if (iterator == null) {
            return empty();
        }

        return new IteratorStream<>(iterator);
    }

    /**
     * 
     * @param c
     * @param startIndex
     * @param endIndex
     * @return
     */
    static <T> Stream<T> of(final Iterator<? extends T> iterator, int startIndex, int endIndex) {
        if (iterator == null && (startIndex == 0 && endIndex == 0)) {
            return empty();
        }

        if (startIndex < 0 || endIndex < startIndex) {
            throw new IllegalArgumentException("startIndex(" + startIndex + ") or endIndex(" + endIndex + ") is invalid");
        }

        return Stream.<T> of(iterator).skip(startIndex).limit(endIndex - startIndex);
    }

    public static <T> Stream<T> of(final java.util.stream.Stream<T> stream) {
        return of(new ObjIteratorEx<T>() {
            private Iterator<T> iter = null;

            @Override
            public boolean hasNext() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (iter == null) {
                    iter = stream.iterator();
                }

                return iter.next();
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
            public Object[] toArray() {
                return iter == null ? stream.toArray() : super.toArray();
            }

            @Override
            public <A> A[] toArray(final A[] a) {
                return iter == null ? stream.toArray(new IntFunction<A[]>() {
                    @Override
                    public A[] apply(int value) {
                        return a;
                    }
                }) : super.toArray(a);
            }
        }).__(s -> stream.isParallel() ? s.parallel() : s.sequential()).onClose(new Runnable() {
            @Override
            public void run() {
                stream.close();
            }
        });
    }

    public static Stream<Boolean> of(final boolean[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Boolean> of(final boolean[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Boolean>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Boolean next() {
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Boolean.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Character> of(char[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Character> of(final char[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Character>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Character next() {
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Character.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Byte> of(byte[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Byte> of(final byte[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Byte>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Byte next() {
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Byte.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Short> of(short[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Short> of(final short[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Short>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Short next() {
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Short.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Integer> of(int[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Integer> of(final int[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Integer>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Integer next() {
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Integer.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Long> of(long[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Long> of(final long[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Long>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Long next() {
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Long.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Float> of(float[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Float> of(final float[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Float>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Float next() {
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Float.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    public static Stream<Double> of(double[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(a, 0, a.length);
    }

    public static Stream<Double> of(final double[] a, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, N.len(a));

        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(new ObjIteratorEx<Double>() {
            private int cursor = fromIndex;

            @Override
            public boolean hasNext() {
                return cursor < toIndex;
            }

            @Override
            public Double next() {
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
            public <A> A[] toArray(A[] a2) {
                a2 = a2.length >= toIndex - cursor ? a2 : (A[]) N.newArray(a2.getClass().getComponentType(), toIndex - cursor);

                for (int i = 0, len = toIndex - cursor; i < len; i++) {
                    a2[i] = (A) Double.valueOf(a[cursor++]);
                }

                return a2;
            }
        });
    }

    /**
     * Lazy evaluation.
     * @param supplier
     * @return
     */
    public static <T> Stream<T> of(final Supplier<Collection<? extends T>> supplier) {
        final Iterator<T> iter = new ObjIteratorEx<T>() {
            private Iterator<? extends T> iterator = null;

            @Override
            public boolean hasNext() {
                if (iterator == null) {
                    init();
                }

                return iterator.hasNext();
            }

            @Override
            public T next() {
                if (iterator == null) {
                    init();
                }

                return iterator.next();
            }

            private void init() {
                final Collection<? extends T> c = supplier.get();

                if (N.isNullOrEmpty(c)) {
                    iterator = ObjIterator.empty();
                } else {
                    iterator = c.iterator();
                }
            }
        };

        return of(iter);
    }

    public static <K> Stream<K> ofKeys(Map<K, ?> map) {
        if (map == null || map.size() == 0) {
            return Stream.empty();
        }

        return of(map.keySet());
    }

    public static <K, V> Stream<K> ofKeys(Map<K, V> map, Predicate<? super V> valueFilter) {
        if (map == null || map.size() == 0) {
            return StreamEx.empty();
        }

        return EntryStream.of(map).filterByValue(valueFilter).keys();
    }

    public static <V> Stream<V> ofValues(Map<?, V> map) {
        if (map == null || map.size() == 0) {
            return Stream.empty();
        }

        return of(map.values());
    }

    public static <K, V> Stream<V> ofValues(Map<K, V> map, Predicate<? super K> keyFilter) {
        if (map == null || map.size() == 0) {
            return Stream.empty();
        }

        return EntryStream.of(map).filterByKey(keyFilter).values();
    }

    public static <T> Stream<T> flat(final Collection<? extends Collection<? extends T>> c) {
        return of(c).flattMap(Fn.<Collection<? extends T>> identity());
    }

    public static <T> Stream<T> flat(final T[][] a) {
        return of(a).flatMapp(Fn.<T[]> identity());
    }

    public static <T> Stream<T> flat(final T[][] a, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        } else if (vertically == false) {
            return of(a).flatMapp(Fn.<T[]> identity());
        }

        long n = 0;

        for (T[] e : a) {
            n += N.len(e);
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final long count = n;

        final Iterator<T> iter = new ObjIteratorEx<T>() {
            private int rowNum = 0;
            private int colNum = 0;
            private long cnt = 0;

            @Override
            public boolean hasNext() {
                return cnt < count;
            }

            @Override
            public T next() {
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

    public static <T> Stream<T> flat(final T[][] a, final T valueForNone, final boolean vertically) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        } else if (a.length == 1) {
            return of(a[0]);
        }

        long n = 0;
        int maxLen = 0;

        for (T[] e : a) {
            n += N.len(e);
            maxLen = N.max(maxLen, N.len(e));
        }

        if (n == 0) {
            return empty();
        }

        final int rows = N.len(a);
        final int cols = maxLen;
        final long count = rows * cols;
        Iterator<T> iter = null;

        if (vertically) {
            iter = new ObjIteratorEx<T>() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public T next() {
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
            iter = new ObjIteratorEx<T>() {
                private int rowNum = 0;
                private int colNum = 0;
                private long cnt = 0;

                @Override
                public boolean hasNext() {
                    return cnt < count;
                }

                @Override
                public T next() {
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

    public static <T> Stream<T> flat(final T[][][] a) {
        return of(a).flatMapp(e -> e).flatMapp(Fn.<T[]> identity());
    }

    public static <T> Stream<T> repeat(final T element, final long n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return empty();
        }

        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private long cnt = n;

            @Override
            public boolean hasNext() {
                return cnt > 0;
            }

            @Override
            public T next() {
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
            public <A> A[] toArray(A[] a) {
                a = a.length >= cnt ? a : N.copyOf(a, (int) cnt);

                for (int i = 0; i < cnt; i++) {
                    a[i] = (A) element;
                }

                cnt = 0;

                return a;
            }
        });
    }

    public static <T> Stream<T> iterate(final BooleanSupplier hasNext, final Supplier<? extends T> next) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(next);

        return of(new ObjIteratorEx<T>() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public T next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return next.get();
            }
        });
    }

    /**
     * Returns a sequential ordered {@code Stream} produced by iterative
     * application of a function {@code f} to an initial element {@code init},
     * producing a {@code Stream} consisting of {@code init}, {@code f(init)},
     * {@code f(f(init))}, etc.
     *
     * <p>The first element (position {@code 0}) in the {@code Stream} will be
     * the provided {@code init}.  For {@code n > 0}, the element at position
     * {@code n}, will be the result of applying the function {@code f} to the
     * element at position {@code n - 1}.
     * 
     * @param init
     * @param hasNext
     * @param f
     * @return
     */
    public static <T> Stream<T> iterate(final T init, final BooleanSupplier hasNext, final UnaryOperator<T> f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return of(new ObjIteratorEx<T>() {
            private T t = (T) NONE;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public T next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return t = (t == NONE) ? init : f.apply(t);
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
    public static <T> Stream<T> iterate(final T init, final Predicate<? super T> hasNext, final UnaryOperator<T> f) {
        N.checkArgNotNull(hasNext);
        N.checkArgNotNull(f);

        return of(new ObjIteratorEx<T>() {
            private T t = (T) NONE;
            private T cur = (T) NONE;
            private boolean hasMore = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() {
                if (hasNextVal == false && hasMore) {
                    hasNextVal = hasNext.test((cur = (t == NONE ? init : f.apply(t))));

                    if (hasNextVal == false) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public T next() {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                cur = (T) NONE;
                hasNextVal = false;
                return t;
            }
        });
    }

    public static <T> Stream<T> iterate(final T init, final UnaryOperator<T> f) {
        N.checkArgNotNull(f);

        return of(new ObjIteratorEx<T>() {
            private T t = (T) NONE;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return t = t == NONE ? init : f.apply(t);
            }
        });
    }

    public static <T> Stream<T> generate(final Supplier<T> s) {
        N.checkArgNotNull(s);

        return of(new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                return s.get();
            }
        });
    }

    /**
     * 
     * @param intervalInMillis
     * @param s
     * @return
     */
    public static <T> Stream<T> interval(final long intervalInMillis, final Supplier<T> s) {
        return interval(0, intervalInMillis, s);
    }

    /**
     * 
     * @param delayInMillis
     * @param intervalInMillis
     * @param s
     * @return
     * @see java.util.concurrent.TimeUnit
     */
    public static <T> Stream<T> interval(final long delayInMillis, final long intervalInMillis, final Supplier<T> s) {
        return interval(delayInMillis, intervalInMillis, TimeUnit.MILLISECONDS, s);
    }

    /**
     * 
     * @param delay
     * @param interval
     * @param unit
     * @param s
     * @return
     */
    public static <T> Stream<T> interval(final long delay, final long interval, final TimeUnit unit, final Supplier<T> s) {
        N.checkArgNotNull(s);

        return LongStream.interval(delay, interval, unit).mapToObj(new LongFunction<T>() {
            @Override
            public T apply(long value) {
                return s.get();
            }
        });
    }

    public static <T> Stream<T> interval(final long intervalInMillis, final LongFunction<T> s) {
        return interval(0, intervalInMillis, s);
    }

    /**
     * 
     * @param delayInMillis
     * @param intervalInMillis
     * @param s
     * @return
     * @see java.util.concurrent.TimeUnit
     */
    public static <T> Stream<T> interval(final long delayInMillis, final long intervalInMillis, final LongFunction<T> s) {
        return interval(delayInMillis, intervalInMillis, TimeUnit.MILLISECONDS, s);
    }

    /**
     * 
     * @param delay
     * @param interval
     * @param unit
     * @param s
     * @return
     */
    public static <T> Stream<T> interval(final long delay, final long interval, final TimeUnit unit, final LongFunction<T> s) {
        N.checkArgNotNull(s);

        return LongStream.interval(delay, interval, unit).mapToObj(s);
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @return
     * @throws UncheckedSQLException 
     */
    public static Stream<Object[]> rows(final ResultSet resultSet) throws UncheckedSQLException {
        return rows(Object[].class, resultSet);
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param targetClass Array/List/Map or Entity with getter/setter methods.
     * @param resultSet
     * @return
     * @throws UncheckedSQLException 
     */
    public static <T> Stream<T> rows(final Class<T> targetClass, final ResultSet resultSet) throws UncheckedSQLException {
        return ExceptionalStream.rows(targetClass, resultSet).unchecked();
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @param rowMapper
     * @return
     * @throws UncheckedSQLException 
     */
    public static <T> Stream<T> rows(final ResultSet resultSet, final JdbcUtil.RowMapper<T> rowMapper) throws UncheckedSQLException {
        return ExceptionalStream.rows(resultSet, rowMapper).unchecked();
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @param rowMapper
     * @return
     * @throws UncheckedSQLException 
     */
    public static <T> Stream<T> rows(final ResultSet resultSet, final JdbcUtil.BiRowMapper<T> rowMapper) throws UncheckedSQLException {
        return ExceptionalStream.rows(resultSet, rowMapper).unchecked();
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @param columnIndex starts from 0, not 1.
     * @return
     * @throws UncheckedSQLException 
     */
    public static <T> Stream<T> rows(final ResultSet resultSet, final int columnIndex) throws UncheckedSQLException {
        return ExceptionalStream.<T> rows(resultSet, columnIndex).unchecked();
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @param columnName
     * @return
     * @throws UncheckedSQLException 
     */
    public static <T> Stream<T> rows(final ResultSet resultSet, final String columnName) throws UncheckedSQLException {
        return ExceptionalStream.<T> rows(resultSet, columnName).unchecked();
    }

    public static Stream<String> lines(final File file) throws UncheckedIOException {
        return lines(file, Charsets.UTF_8);
    }

    public static Stream<String> lines(final File file, final Charset charset) throws UncheckedIOException {
        final ObjIteratorEx<String> iter = createLazyLineIterator(file, null, charset, null, true);

        return of(iter).onClose(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });
    }

    public static Stream<String> lines(final Path path) throws UncheckedIOException {
        return lines(path, Charsets.UTF_8);
    }

    public static Stream<String> lines(final Path path, final Charset charset) throws UncheckedIOException {
        final ObjIteratorEx<String> iter = createLazyLineIterator(null, path, charset, null, true);

        return of(iter).onClose(new Runnable() {
            @Override
            public void run() {
                iter.close();
            }
        });
    }

    /**
     * It's user's responsibility to close the input <code>reader</code> after the stream is finished.
     * 
     * @param reader
     * @return
     * @throws UncheckedIOException
     */
    public static Stream<String> lines(final Reader reader) throws UncheckedIOException {
        N.checkArgNotNull(reader);

        return of(createLazyLineIterator(null, null, Charsets.UTF_8, reader, false));
    }

    private static ObjIteratorEx<String> createLazyLineIterator(final File file, final Path path, final Charset charset, final Reader reader,
            final boolean closeReader) {
        return ObjIteratorEx.of(new Supplier<ObjIteratorEx<String>>() {
            private ObjIteratorEx<String> lazyIter = null;

            @Override
            public synchronized ObjIteratorEx<String> get() {
                if (lazyIter == null) {
                    lazyIter = new ObjIteratorEx<String>() {
                        private BufferedReader bufferedReader;

                        {
                            if (reader != null) {
                                bufferedReader = reader instanceof BufferedReader ? ((BufferedReader) reader) : new BufferedReader(reader);
                            } else if (file != null) {
                                bufferedReader = IOUtil.newBufferedReader(file, charset == null ? Charsets.UTF_8 : charset);
                            } else {
                                bufferedReader = IOUtil.newBufferedReader(path, charset == null ? Charsets.UTF_8 : charset);
                            }
                        }

                        private final LineIterator lineIterator = new LineIterator(bufferedReader);

                        @Override
                        public boolean hasNext() {
                            return lineIterator.hasNext();
                        }

                        @Override
                        public String next() {
                            return lineIterator.next();
                        }

                        @Override
                        public void close() {
                            if (closeReader) {
                                IOUtil.closeQuietly(reader);
                            }
                        }
                    };
                }

                return lazyIter;
            }
        });
    }

    public static Stream<File> listFiles(final File parentPath) throws UncheckedIOException {
        if (!parentPath.exists()) {
            return empty();
        }

        return of(parentPath.listFiles());
    }

    public static Stream<File> listFiles(final File parentPath, final boolean recursively) throws UncheckedIOException {
        if (!parentPath.exists()) {
            return empty();
        } else if (recursively == false) {
            return of(parentPath.listFiles());
        }

        final ObjIterator<File> iter = new ObjIterator<File>() {
            private final Queue<File> paths = N.asLinkedList(parentPath);
            private File[] subFiles = null;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if ((subFiles == null || cursor >= subFiles.length) && paths.size() > 0) {
                    cursor = 0;
                    subFiles = null;

                    while (paths.size() > 0) {
                        subFiles = paths.poll().listFiles();

                        if (N.notNullOrEmpty(subFiles)) {
                            break;
                        }
                    }
                }

                return subFiles != null && cursor < subFiles.length;
            }

            @Override
            public File next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (subFiles[cursor].isDirectory()) {
                    paths.offer(subFiles[cursor]);
                }

                return subFiles[cursor++];
            }
        };

        return of(iter);
    }

    public static <T> Stream<T> observe(final BlockingQueue<T> queue, final Duration duration) {
        return observe(queue, duration, Fn.emptyAction());
    }

    /**
     * Sample code:
     * 
     * <pre>
     * <code>
     * final BlockingQueue<String> queue = new ArrayBlockingQueue<>(32);
     * Stream.observe(queue, Duration.ofMillis(100)).filter(s -> s.startsWith("a")).asyncRun(s -> s.forEach(Fn.println()));
     * N.asList("a", "b", "ab", "bc", "1", "a").forEach(queue::add);
     * N.sleep(10);
     * N.println("==================");
     * N.sleep(100);
     * N.println("==================");
     * N.sleep(10);
     * </code>
     * </pre>
     * 
     * @param queue
     * @param duration
     * @param onComplete
     * @return
     */
    public static <T> Stream<T> observe(final BlockingQueue<T> queue, final Duration duration, final Runnable onComplete) {
        N.checkArgNotNull(queue, "queue");
        N.checkArgNotNull(duration, "duration");
        N.checkArgNotNull(onComplete, "onComplete");

        final long now = System.currentTimeMillis();
        final long endTime = duration.toMillis() >= Long.MAX_VALUE - now ? Long.MAX_VALUE : now + duration.toMillis();

        final Iterator<T> iter = new ObjIterator<T>() {
            private T next = null;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    final long curTime = System.currentTimeMillis();

                    if (curTime <= endTime) {
                        try {
                            next = queue.poll(endTime - curTime, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }

                return next != null;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final T res = next;
                next = null;
                return res;
            }
        };

        return of(iter).onClose(onComplete);
    }

    public static <T> Stream<T> observe(final BlockingQueue<T> queue, final BooleanSupplier hasMore, final long maxWaitIntervalInMillis) {
        return observe(queue, hasMore, maxWaitIntervalInMillis, Fn.emptyAction());
    }

    /**
     * Sample code:
     * 
     * <pre>
     * <code>
     * final BlockingQueue<String> queue = new ArrayBlockingQueue<>(32);
     * final MutableBoolean hasMore = MutableBoolean.of(true);
     * Stream.observe(queue, () -> hasMore.value(), 10).filter(s -> s.startsWith("a")).asyncRun(s -> s.forEach(Fn.println()));
     * N.asList("a", "b", "ab", "bc", "1", "a").forEach(queue::add);
     * N.println("==================");
     * hasMore.setFalse();
     * N.sleep(50);
     * N.println("==================");
     * </code>
     * </pre>
     * 
     * @param queue
     * @param hasMore
     * @param maxWaitIntervalInMillis
     * @param isCompleted it will will be set to {@code true} if Stream is completed and the upstream should not continue to put elements to queue when it's completed.
     *        This is an output parameter.
     * @return
     */
    public static <T> Stream<T> observe(final BlockingQueue<T> queue, final BooleanSupplier hasMore, final long maxWaitIntervalInMillis,
            final Runnable onComplete) {
        N.checkArgNotNull(queue, "queue");
        N.checkArgNotNull(hasMore, "hasMore");
        N.checkArgPositive(maxWaitIntervalInMillis, "maxWaitIntervalInMillis");
        N.checkArgNotNull(onComplete, "onComplete");

        final Iterator<T> iter = new ObjIterator<T>() {
            private T next = null;

            @Override
            public boolean hasNext() {
                if (next == null && (hasMore.getAsBoolean() || queue.size() > 0)) {
                    try {
                        do {
                            next = queue.poll(maxWaitIntervalInMillis, TimeUnit.MILLISECONDS);
                        } while (next == null && (hasMore.getAsBoolean() || queue.size() > 0));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                return next != null;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final T res = next;
                next = null;
                return res;
            }
        };

        return of(iter).onClose(onComplete);
    }

    @SafeVarargs
    public static <T> Stream<T> concat(final T[]... a) {
        return N.isNullOrEmpty(a) ? (Stream<T>) empty() : new IteratorStream<>(new ObjIteratorEx<T>() {
            private final Iterator<T[]> iter = N.asList(a).iterator();
            private Iterator<T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = ObjIteratorEx.of(iter.next());
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        });
    }

    @SafeVarargs
    public static <T> Stream<T> concat(final Collection<? extends T>... a) {
        return N.isNullOrEmpty(a) ? (Stream<T>) empty() : new IteratorStream<>(new ObjIteratorEx<T>() {
            private final Iterator<Collection<? extends T>> iter = N.asList(a).iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iter.hasNext()) {
                    cur = iter.next().iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        });
    }

    @SafeVarargs
    public static <T> Stream<T> concat(final Iterator<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return concatt(N.asList(a));
    }

    @SafeVarargs
    public static <T> Stream<T> concat(final Stream<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return concat(N.asList(a));
    }

    public static <T> Stream<T> concat(final Collection<? extends Stream<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        return of(new ObjIteratorEx<T>() {
            private final Iterator<? extends Stream<? extends T>> iterators = c.iterator();
            private Stream<? extends T> cur;
            private Iterator<? extends T> iter;

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
            public T next() {
                if ((iter == null || iter.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return iter.next();
            }
        }).onClose(newCloseHandler(c));
    }

    public static <T> Stream<T> concatt(final Collection<? extends Iterator<? extends T>> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        return of(new ObjIteratorEx<T>() {
            private final Iterator<? extends Iterator<? extends T>> iterators = c.iterator();
            private Iterator<? extends T> cur;

            @Override
            public boolean hasNext() {
                while ((cur == null || cur.hasNext() == false) && iterators.hasNext()) {
                    cur = iterators.next();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public T next() {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        });
    }

    // NO NEED.
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @return
    //     */
    //    public static <T> Stream<T> parallelConcat(final T[]... a) {
    //        return parallelConcat(a, DEFAULT_READING_THREAD_NUM, calculateQueueSize(a.length));
    //    }
    //
    //    /**
    //     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
    //     * 
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, a.length)
    //     * @param queueSize Default value is N.min(128, a.length * 16)
    //     * @return
    //     */
    //    public static <T> Stream<T> parallelConcat(final T[][] a, final int readThreadNum, final int queueSize) {
    //        if (N.isNullOrEmpty(a)) {
    //            return empty();
    //        }
    //
    //        final Iterator<? extends T>[] iters = new Iterator[a.length];
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            iters[i] = ImmutableIterator.of(a[i]);
    //        }
    //
    //        return parallelConcat(iters, readThreadNum, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @return
    //     */
    //    public static <T> Stream<T> parallelConcat(final Collection<? extends T>... a) {
    //        return parallelConcat(a, DEFAULT_READING_THREAD_NUM, calculateQueueSize(a.length));
    //    }
    //
    //    /**
    //     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
    //     * 
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, a.length)
    //     * @param queueSize Default value is N.min(128, a.length * 16)
    //     * @return
    //     */
    //    public static <T> Stream<T> parallelConcat(final Collection<? extends T>[] a, final int readThreadNum, final int queueSize) {
    //        if (N.isNullOrEmpty(a)) {
    //            return empty();
    //        }
    //
    //        final Iterator<? extends T>[] iters = new Iterator[a.length];
    //
    //        for (int i = 0, len = a.length; i < len; i++) {
    //            iters[i] = a[i].iterator();
    //        }
    //
    //        return parallelConcat(iters, readThreadNum, queueSize);
    //    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Stream<T> parallelConcat(final Iterator<? extends T>... a) {
        return parallelConcat(a, DEFAULT_READING_THREAD_NUM, calculateQueueSize(a.length));
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
     * 
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, a.length)
     * @param queueSize Default value is N.min(128, a.length * 16)
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Iterator<? extends T>[] a, final int readThreadNum, final int queueSize) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return parallelConcatt(N.asList(a), readThreadNum, queueSize);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T> Stream<T> parallelConcat(final Stream<? extends T>... a) {
        return parallelConcat(a, DEFAULT_READING_THREAD_NUM, calculateQueueSize(a.length));
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
     * 
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, a.length)
     * @param queueSize Default value is N.min(128, a.length * 16)
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Stream<? extends T>[] a, final int readThreadNum, final int queueSize) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return parallelConcat(N.asList(a), readThreadNum, queueSize);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Collection<? extends Stream<? extends T>> c) {
        return parallelConcat(c, DEFAULT_READING_THREAD_NUM);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param readThreadNum
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Collection<? extends Stream<? extends T>> c, final int readThreadNum) {
        return parallelConcat(c, readThreadNum, calculateQueueSize(c.size()));
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
     * 
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, c.size())
     * @param queueSize Default value is N.min(128, c.size() * 16)
     * @return
     */
    public static <T> Stream<T> parallelConcat(final Collection<? extends Stream<? extends T>> c, final int readThreadNum, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final AtomicInteger threadCounter = new AtomicInteger(c.size());
        final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);
        final MutableBoolean disposableChecked = MutableBoolean.of(false);

        final Iterator<? extends Stream<? extends T>> iterators = c.iterator();
        final int threadNum = Math.min(c.size(), readThreadNum);

        for (int i = 0; i < threadNum; i++) {
            DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    try {
                        while (onGoing.value()) {
                            Stream<? extends T> s = null;
                            Iterator<? extends T> iter = null;

                            synchronized (iterators) {
                                if (iterators.hasNext()) {
                                    s = iterators.next();
                                    iter = s.iteratorEx();
                                } else {
                                    break;
                                }
                            }

                            T next = null;

                            while (onGoing.value() && iter.hasNext()) {
                                next = iter.next();

                                if (next == null) {
                                    next = (T) NONE;
                                } else if (disposableChecked.isFalse()) {
                                    disposableChecked.setTrue();

                                    if (next instanceof NoCachingNoUpdating) {
                                        throw new RuntimeException("Can't run NoCachingNoUpdating Objects in parallel Stream or Queue");
                                    }
                                }

                                if (queue.offer(next) == false) {
                                    while (onGoing.value()) {
                                        if (queue.offer(next, 100, TimeUnit.MILLISECONDS)) {
                                            break;
                                        }
                                    }
                                }
                            }

                            if (s != null) {
                                s.close();
                            }
                        }
                    } catch (Exception e) {
                        setError(eHolder, e, onGoing);
                    } finally {
                        threadCounter.decrementAndGet();
                    }
                }
            });
        }

        return of(new QueuedIterator<T>(queueSize) {
            T next = null;

            @Override
            public boolean hasNext() {
                try {
                    if (next == null && (next = queue.poll()) == null) {
                        while (onGoing.value() && (threadCounter.get() > 0 || queue.size() > 0)) { // (queue.size() > 0 || counter.get() > 0) is wrong. has to check counter first
                            if ((next = queue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                }

                if (eHolder.value() != null) {
                    throwError(eHolder, onGoing);
                }

                return next != null;
            }

            @Override
            public T next() {
                if (next == null && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                T result = next == NONE ? null : next;
                next = null;
                return result;
            }
        }).onClose(newCloseHandler(c)).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @return
     */
    public static <T> Stream<T> parallelConcatt(final Collection<? extends Iterator<? extends T>> c) {
        return parallelConcatt(c, DEFAULT_READING_THREAD_NUM);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param readThreadNum
     * @return
     */
    public static <T> Stream<T> parallelConcatt(final Collection<? extends Iterator<? extends T>> c, final int readThreadNum) {
        return parallelConcatt(c, readThreadNum, calculateQueueSize(c.size()));
    }

    /**
     * Returns a Stream with elements from a temporary queue which is filled by reading the elements from the specified iterators in parallel.
     * 
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelConcat(a,b, ...)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param readThreadNum - count of threads used to read elements from iterator to queue. Default value is min(8, c.size())
     * @param queueSize Default value is N.min(128, c.size() * 16)
     * @return
     */
    public static <T> Stream<T> parallelConcatt(final Collection<? extends Iterator<? extends T>> c, final int readThreadNum, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final AtomicInteger threadCounter = new AtomicInteger(c.size());
        final ArrayBlockingQueue<T> queue = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);
        final MutableBoolean disposableChecked = MutableBoolean.of(false);

        final Iterator<? extends Iterator<? extends T>> iterators = c.iterator();
        final int threadNum = Math.min(c.size(), readThreadNum);

        for (int i = 0; i < threadNum; i++) {
            DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    try {
                        while (onGoing.value()) {
                            Iterator<? extends T> iter = null;

                            synchronized (iterators) {
                                if (iterators.hasNext()) {
                                    iter = iterators.next();
                                } else {
                                    break;
                                }
                            }

                            T next = null;

                            while (onGoing.value() && iter.hasNext()) {
                                next = iter.next();

                                if (next == null) {
                                    next = (T) NONE;
                                } else if (disposableChecked.isFalse()) {
                                    disposableChecked.setTrue();

                                    if (next instanceof NoCachingNoUpdating) {
                                        throw new RuntimeException("Can't run NoCachingNoUpdating Objects in parallel Stream or Queue");
                                    }
                                }

                                if (queue.offer(next) == false) {
                                    while (onGoing.value()) {
                                        if (queue.offer(next, 100, TimeUnit.MILLISECONDS)) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        setError(eHolder, e, onGoing);
                    } finally {
                        threadCounter.decrementAndGet();
                    }
                }
            });
        }

        return of(new QueuedIterator<T>(queueSize) {
            T next = null;

            @Override
            public boolean hasNext() {
                try {
                    if (next == null && (next = queue.poll()) == null) {
                        while (onGoing.value() && (threadCounter.get() > 0 || queue.size() > 0)) { // (queue.size() > 0 || counter.get() > 0) is wrong. has to check counter first
                            if ((next = queue.poll(1, TimeUnit.MILLISECONDS)) != null) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                }

                if (eHolder.value() != null) {
                    throwError(eHolder, onGoing);
                }

                return next != null;
            }

            @Override
            public T next() {
                if (next == null && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                T result = next == NONE ? null : next;
                next = null;
                return result;
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final char[] a, final char[] b, final CharBiFunction<R> zipFunction) {
        return zip(CharIteratorEx.of(a), CharIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final char[] a, final char[] b, final char[] c, final CharTriFunction<R> zipFunction) {
        return zip(CharIteratorEx.of(a), CharIteratorEx.of(b), CharIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final CharIterator a, final CharIterator b, final CharBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextChar(), b.nextChar());
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
    public static <R> Stream<R> zip(final CharIterator a, final CharIterator b, final CharIterator c, final CharTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextChar(), b.nextChar(), c.nextChar());
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
    public static <R> Stream<R> zip(final CharStream a, final CharStream b, final CharBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final CharStream a, final CharStream b, final CharStream c, final CharTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends CharStream> c, final CharNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final CharIterator[] iters = new CharIterator[len];
        int i = 0;

        for (CharStream s : c) {
            iters[i++] = s.iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i].hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final char[] args = new char[len];

                for (int i = 0; i < len; i++) {
                    args[i] = iters[i].nextChar();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
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
    public static <R> Stream<R> zip(final char[] a, final char[] b, final char valueForNoneA, final char valueForNoneB, final CharBiFunction<R> zipFunction) {
        return zip(CharIteratorEx.of(a), CharIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <R> Stream<R> zip(final char[] a, final char[] b, final char[] c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTriFunction<R> zipFunction) {
        return zip(CharIteratorEx.of(a), CharIteratorEx.of(b), CharIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <R> Stream<R> zip(final CharIterator a, final CharIterator b, final char valueForNoneA, final char valueForNoneB,
            final CharBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextChar() : valueForNoneA, b.hasNext() ? b.nextChar() : valueForNoneB);
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
    public static <R> Stream<R> zip(final CharIterator a, final CharIterator b, final CharIterator c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextChar() : valueForNoneA, b.hasNext() ? b.nextChar() : valueForNoneB,
                        c.hasNext() ? c.nextChar() : valueForNoneC);
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
    public static <R> Stream<R> zip(final CharStream a, final CharStream b, final char valueForNoneA, final char valueForNoneB,
            final CharBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final CharStream a, final CharStream b, final CharStream c, final char valueForNoneA, final char valueForNoneB,
            final char valueForNoneC, final CharTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends CharStream> c, final char[] valuesForNone, final CharNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final CharStream[] ss = c.toArray(new CharStream[len]);
        final CharIterator[] iters = new CharIterator[len];

        for (int i = 0; i < len; i++) {
            iters[i] = ss[i].iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                            ss[i].close();
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final char[] args = new char[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].nextChar();
                    } else {
                        args[i] = valuesForNone[i];
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
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
    public static <R> Stream<R> zip(final byte[] a, final byte[] b, final ByteBiFunction<R> zipFunction) {
        return zip(ByteIteratorEx.of(a), ByteIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final byte[] a, final byte[] b, final byte[] c, final ByteTriFunction<R> zipFunction) {
        return zip(ByteIteratorEx.of(a), ByteIteratorEx.of(b), ByteIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ByteIterator a, final ByteIterator b, final ByteBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextByte(), b.nextByte());
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
    public static <R> Stream<R> zip(final ByteIterator a, final ByteIterator b, final ByteIterator c, final ByteTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextByte(), b.nextByte(), c.nextByte());
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
    public static <R> Stream<R> zip(final ByteStream a, final ByteStream b, final ByteBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final ByteStream a, final ByteStream b, final ByteStream c, final ByteTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends ByteStream> c, final ByteNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final ByteIterator[] iters = new ByteIterator[len];
        int i = 0;

        for (ByteStream s : c) {
            iters[i++] = s.iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i].hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final byte[] args = new byte[len];

                for (int i = 0; i < len; i++) {
                    args[i] = iters[i].nextByte();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
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
    public static <R> Stream<R> zip(final byte[] a, final byte[] b, final byte valueForNoneA, final byte valueForNoneB, final ByteBiFunction<R> zipFunction) {
        return zip(ByteIteratorEx.of(a), ByteIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <R> Stream<R> zip(final byte[] a, final byte[] b, final byte[] c, final byte valueForNoneA, final byte valueForNoneB,
            final byte valueForNoneC, final ByteTriFunction<R> zipFunction) {
        return zip(ByteIteratorEx.of(a), ByteIteratorEx.of(b), ByteIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <R> Stream<R> zip(final ByteIterator a, final ByteIterator b, final byte valueForNoneA, final byte valueForNoneB,
            final ByteBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextByte() : valueForNoneA, b.hasNext() ? b.nextByte() : valueForNoneB);
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
    public static <R> Stream<R> zip(final ByteIterator a, final ByteIterator b, final ByteIterator c, final byte valueForNoneA, final byte valueForNoneB,
            final byte valueForNoneC, final ByteTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextByte() : valueForNoneA, b.hasNext() ? b.nextByte() : valueForNoneB,
                        c.hasNext() ? c.nextByte() : valueForNoneC);
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
    public static <R> Stream<R> zip(final ByteStream a, final ByteStream b, final byte valueForNoneA, final byte valueForNoneB,
            final ByteBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final ByteStream a, final ByteStream b, final ByteStream c, final byte valueForNoneA, final byte valueForNoneB,
            final byte valueForNoneC, final ByteTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends ByteStream> c, final byte[] valuesForNone, final ByteNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final ByteStream[] ss = c.toArray(new ByteStream[len]);
        final ByteIterator[] iters = new ByteIterator[len];

        for (int i = 0; i < len; i++) {
            iters[i] = ss[i].iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                            ss[i].close();
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final byte[] args = new byte[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].nextByte();
                    } else {
                        args[i] = valuesForNone[i];
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
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
    public static <R> Stream<R> zip(final short[] a, final short[] b, final ShortBiFunction<R> zipFunction) {
        return zip(ShortIteratorEx.of(a), ShortIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final short[] a, final short[] b, final short[] c, final ShortTriFunction<R> zipFunction) {
        return zip(ShortIteratorEx.of(a), ShortIteratorEx.of(b), ShortIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final ShortIterator a, final ShortIterator b, final ShortBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextShort(), b.nextShort());
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
    public static <R> Stream<R> zip(final ShortIterator a, final ShortIterator b, final ShortIterator c, final ShortTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextShort(), b.nextShort(), c.nextShort());
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
    public static <R> Stream<R> zip(final ShortStream a, final ShortStream b, final ShortBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final ShortStream a, final ShortStream b, final ShortStream c, final ShortTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends ShortStream> c, final ShortNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final ShortIterator[] iters = new ShortIterator[len];
        int i = 0;

        for (ShortStream s : c) {
            iters[i++] = s.iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i].hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final short[] args = new short[len];

                for (int i = 0; i < len; i++) {
                    args[i] = iters[i].nextShort();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
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
    public static <R> Stream<R> zip(final short[] a, final short[] b, final short valueForNoneA, final short valueForNoneB,
            final ShortBiFunction<R> zipFunction) {
        return zip(ShortIteratorEx.of(a), ShortIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <R> Stream<R> zip(final short[] a, final short[] b, final short[] c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTriFunction<R> zipFunction) {
        return zip(ShortIteratorEx.of(a), ShortIteratorEx.of(b), ShortIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <R> Stream<R> zip(final ShortIterator a, final ShortIterator b, final short valueForNoneA, final short valueForNoneB,
            final ShortBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextShort() : valueForNoneA, b.hasNext() ? b.nextShort() : valueForNoneB);
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
    public static <R> Stream<R> zip(final ShortIterator a, final ShortIterator b, final ShortIterator c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextShort() : valueForNoneA, b.hasNext() ? b.nextShort() : valueForNoneB,
                        c.hasNext() ? c.nextShort() : valueForNoneC);
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
    public static <R> Stream<R> zip(final ShortStream a, final ShortStream b, final short valueForNoneA, final short valueForNoneB,
            final ShortBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final ShortStream a, final ShortStream b, final ShortStream c, final short valueForNoneA, final short valueForNoneB,
            final short valueForNoneC, final ShortTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends ShortStream> c, final short[] valuesForNone, final ShortNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final ShortStream[] ss = c.toArray(new ShortStream[len]);
        final ShortIterator[] iters = new ShortIterator[len];

        for (int i = 0; i < len; i++) {
            iters[i] = ss[i].iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                            ss[i].close();
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final short[] args = new short[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].nextShort();
                    } else {
                        args[i] = valuesForNone[i];
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
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
    public static <R> Stream<R> zip(final int[] a, final int[] b, final IntBiFunction<R> zipFunction) {
        return zip(IntIteratorEx.of(a), IntIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final int[] a, final int[] b, final int[] c, final IntTriFunction<R> zipFunction) {
        return zip(IntIteratorEx.of(a), IntIteratorEx.of(b), IntIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final IntIterator a, final IntIterator b, final IntBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextInt(), b.nextInt());
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
    public static <R> Stream<R> zip(final IntIterator a, final IntIterator b, final IntIterator c, final IntTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextInt(), b.nextInt(), c.nextInt());
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
    public static <R> Stream<R> zip(final IntStream a, final IntStream b, final IntBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final IntStream a, final IntStream b, final IntStream c, final IntTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends IntStream> c, final IntNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final IntIterator[] iters = new IntIterator[len];
        int i = 0;

        for (IntStream s : c) {
            iters[i++] = s.iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i].hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final int[] args = new int[len];

                for (int i = 0; i < len; i++) {
                    args[i] = iters[i].nextInt();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
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
    public static <R> Stream<R> zip(final int[] a, final int[] b, final int valueForNoneA, final int valueForNoneB, final IntBiFunction<R> zipFunction) {
        return zip(IntIteratorEx.of(a), IntIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <R> Stream<R> zip(final int[] a, final int[] b, final int[] c, final int valueForNoneA, final int valueForNoneB, final int valueForNoneC,
            final IntTriFunction<R> zipFunction) {
        return zip(IntIteratorEx.of(a), IntIteratorEx.of(b), IntIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <R> Stream<R> zip(final IntIterator a, final IntIterator b, final int valueForNoneA, final int valueForNoneB,
            final IntBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextInt() : valueForNoneA, b.hasNext() ? b.nextInt() : valueForNoneB);
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
    public static <R> Stream<R> zip(final IntIterator a, final IntIterator b, final IntIterator c, final int valueForNoneA, final int valueForNoneB,
            final int valueForNoneC, final IntTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextInt() : valueForNoneA, b.hasNext() ? b.nextInt() : valueForNoneB,
                        c.hasNext() ? c.nextInt() : valueForNoneC);
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
    public static <R> Stream<R> zip(final IntStream a, final IntStream b, final int valueForNoneA, final int valueForNoneB,
            final IntBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final IntStream a, final IntStream b, final IntStream c, final int valueForNoneA, final int valueForNoneB,
            final int valueForNoneC, final IntTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends IntStream> c, final int[] valuesForNone, final IntNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final IntStream[] ss = c.toArray(new IntStream[len]);
        final IntIterator[] iters = new IntIterator[len];

        for (int i = 0; i < len; i++) {
            iters[i] = ss[i].iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                            ss[i].close();
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final int[] args = new int[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].nextInt();
                    } else {
                        args[i] = valuesForNone[i];
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
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
    public static <R> Stream<R> zip(final long[] a, final long[] b, final LongBiFunction<R> zipFunction) {
        return zip(LongIteratorEx.of(a), LongIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final long[] a, final long[] b, final long[] c, final LongTriFunction<R> zipFunction) {
        return zip(LongIteratorEx.of(a), LongIteratorEx.of(b), LongIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final LongIterator a, final LongIterator b, final LongBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextLong(), b.nextLong());
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
    public static <R> Stream<R> zip(final LongIterator a, final LongIterator b, final LongIterator c, final LongTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextLong(), b.nextLong(), c.nextLong());
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
    public static <R> Stream<R> zip(final LongStream a, final LongStream b, final LongBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final LongStream a, final LongStream b, final LongStream c, final LongTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends LongStream> c, final LongNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final LongIterator[] iters = new LongIterator[len];
        int i = 0;

        for (LongStream s : c) {
            iters[i++] = s.iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i].hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final long[] args = new long[len];

                for (int i = 0; i < len; i++) {
                    args[i] = iters[i].nextLong();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
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
    public static <R> Stream<R> zip(final long[] a, final long[] b, final long valueForNoneA, final long valueForNoneB, final LongBiFunction<R> zipFunction) {
        return zip(LongIteratorEx.of(a), LongIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <R> Stream<R> zip(final long[] a, final long[] b, final long[] c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTriFunction<R> zipFunction) {
        return zip(LongIteratorEx.of(a), LongIteratorEx.of(b), LongIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <R> Stream<R> zip(final LongIterator a, final LongIterator b, final long valueForNoneA, final long valueForNoneB,
            final LongBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextLong() : valueForNoneA, b.hasNext() ? b.nextLong() : valueForNoneB);
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
    public static <R> Stream<R> zip(final LongIterator a, final LongIterator b, final LongIterator c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextLong() : valueForNoneA, b.hasNext() ? b.nextLong() : valueForNoneB,
                        c.hasNext() ? c.nextLong() : valueForNoneC);
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
    public static <R> Stream<R> zip(final LongStream a, final LongStream b, final long valueForNoneA, final long valueForNoneB,
            final LongBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final LongStream a, final LongStream b, final LongStream c, final long valueForNoneA, final long valueForNoneB,
            final long valueForNoneC, final LongTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends LongStream> c, final long[] valuesForNone, final LongNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final LongStream[] ss = c.toArray(new LongStream[len]);
        final LongIterator[] iters = new LongIterator[len];

        for (int i = 0; i < len; i++) {
            iters[i] = ss[i].iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                            ss[i].close();
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final long[] args = new long[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].nextLong();
                    } else {
                        args[i] = valuesForNone[i];
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
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
    public static <R> Stream<R> zip(final float[] a, final float[] b, final FloatBiFunction<R> zipFunction) {
        return zip(FloatIteratorEx.of(a), FloatIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final float[] a, final float[] b, final float[] c, final FloatTriFunction<R> zipFunction) {
        return zip(FloatIteratorEx.of(a), FloatIteratorEx.of(b), FloatIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final FloatIterator a, final FloatIterator b, final FloatBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextFloat(), b.nextFloat());
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
    public static <R> Stream<R> zip(final FloatIterator a, final FloatIterator b, final FloatIterator c, final FloatTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextFloat(), b.nextFloat(), c.nextFloat());
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
    public static <R> Stream<R> zip(final FloatStream a, final FloatStream b, final FloatBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final FloatStream a, final FloatStream b, final FloatStream c, final FloatTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends FloatStream> c, final FloatNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final FloatIterator[] iters = new FloatIterator[len];
        int i = 0;

        for (FloatStream s : c) {
            iters[i++] = s.iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i].hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final float[] args = new float[len];

                for (int i = 0; i < len; i++) {
                    args[i] = iters[i].nextFloat();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
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
    public static <R> Stream<R> zip(final float[] a, final float[] b, final float valueForNoneA, final float valueForNoneB,
            final FloatBiFunction<R> zipFunction) {
        return zip(FloatIteratorEx.of(a), FloatIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <R> Stream<R> zip(final float[] a, final float[] b, final float[] c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTriFunction<R> zipFunction) {
        return zip(FloatIteratorEx.of(a), FloatIteratorEx.of(b), FloatIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <R> Stream<R> zip(final FloatIterator a, final FloatIterator b, final float valueForNoneA, final float valueForNoneB,
            final FloatBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextFloat() : valueForNoneA, b.hasNext() ? b.nextFloat() : valueForNoneB);
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
    public static <R> Stream<R> zip(final FloatIterator a, final FloatIterator b, final FloatIterator c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextFloat() : valueForNoneA, b.hasNext() ? b.nextFloat() : valueForNoneB,
                        c.hasNext() ? c.nextFloat() : valueForNoneC);
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
    public static <R> Stream<R> zip(final FloatStream a, final FloatStream b, final float valueForNoneA, final float valueForNoneB,
            final FloatBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final FloatStream a, final FloatStream b, final FloatStream c, final float valueForNoneA, final float valueForNoneB,
            final float valueForNoneC, final FloatTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends FloatStream> c, final float[] valuesForNone, final FloatNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final FloatStream[] ss = c.toArray(new FloatStream[len]);
        final FloatIterator[] iters = new FloatIterator[len];

        for (int i = 0; i < len; i++) {
            iters[i] = ss[i].iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                            ss[i].close();
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final float[] args = new float[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].nextFloat();
                    } else {
                        args[i] = valuesForNone[i];
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
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
    public static <R> Stream<R> zip(final double[] a, final double[] b, final DoubleBiFunction<R> zipFunction) {
        return zip(DoubleIteratorEx.of(a), DoubleIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final double[] a, final double[] b, final double[] c, final DoubleTriFunction<R> zipFunction) {
        return zip(DoubleIteratorEx.of(a), DoubleIteratorEx.of(b), DoubleIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <R> Stream<R> zip(final DoubleIterator a, final DoubleIterator b, final DoubleBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextDouble(), b.nextDouble());
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
    public static <R> Stream<R> zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final DoubleTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.nextDouble(), b.nextDouble(), c.nextDouble());
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
    public static <R> Stream<R> zip(final DoubleStream a, final DoubleStream b, final DoubleBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final DoubleTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends DoubleStream> c, final DoubleNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final DoubleIterator[] iters = new DoubleIterator[len];
        int i = 0;

        for (DoubleStream s : c) {
            iters[i++] = s.iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i].hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final double[] args = new double[len];

                for (int i = 0; i < len; i++) {
                    args[i] = iters[i].nextDouble();
                }

                return zipFunction.apply(args);
            }
        }).onClose(newCloseHandler(c));
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
    public static <R> Stream<R> zip(final double[] a, final double[] b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<R> zipFunction) {
        return zip(DoubleIteratorEx.of(a), DoubleIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <R> Stream<R> zip(final double[] a, final double[] b, final double[] c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTriFunction<R> zipFunction) {
        return zip(DoubleIteratorEx.of(a), DoubleIteratorEx.of(b), DoubleIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <R> Stream<R> zip(final DoubleIterator a, final DoubleIterator b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextDouble() : valueForNoneA, b.hasNext() ? b.nextDouble() : valueForNoneB);
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
    public static <R> Stream<R> zip(final DoubleIterator a, final DoubleIterator b, final DoubleIterator c, final double valueForNoneA,
            final double valueForNoneB, final double valueForNoneC, final DoubleTriFunction<R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.nextDouble() : valueForNoneA, b.hasNext() ? b.nextDouble() : valueForNoneB,
                        c.hasNext() ? c.nextDouble() : valueForNoneC);
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
    public static <R> Stream<R> zip(final DoubleStream a, final DoubleStream b, final double valueForNoneA, final double valueForNoneB,
            final DoubleBiFunction<R> zipFunction) {
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
    public static <R> Stream<R> zip(final DoubleStream a, final DoubleStream b, final DoubleStream c, final double valueForNoneA, final double valueForNoneB,
            final double valueForNoneC, final DoubleTriFunction<R> zipFunction) {
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
    @SuppressWarnings("resource")
    public static <R> Stream<R> zip(final Collection<? extends DoubleStream> c, final double[] valuesForNone, final DoubleNFunction<R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.length) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final DoubleStream[] ss = c.toArray(new DoubleStream[len]);
        final DoubleIterator[] iters = new DoubleIterator[len];

        for (int i = 0; i < len; i++) {
            iters[i] = ss[i].iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                            ss[i].close();
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final double[] args = new double[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].nextDouble();
                    } else {
                        args[i] = valuesForNone[i];
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(args);
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
    public static <A, B, R> Stream<R> zip(final A[] a, final B[] b, final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(ObjIteratorEx.of(a), ObjIteratorEx.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final A[] a, final B[] b, final C[] c, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(ObjIteratorEx.of(a), ObjIteratorEx.of(b), ObjIteratorEx.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, R> Stream<R> zip(final Collection<? extends A> a, final Collection<? extends B> b,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, R> Stream<R> zip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), c.iterator(), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param a
     * @param b
     * @return
     */
    public static <A, B, R> Stream<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.next(), b.next());
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
    public static <A, B, C, R> Stream<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public R next() {
                return zipFunction.apply(a.next(), b.next(), c.next());
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
    public static <A, B, R> Stream<R> zip(final Stream<? extends A> a, final Stream<? extends B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
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
    public static <A, B, C, R> Stream<R> zip(final Stream<? extends A> a, final Stream<? extends B> b, final Stream<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    public static <T, R> Stream<R> zip(final List<? extends Collection<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Collection<? extends T> e : c) {
            iterList.add(ObjIterator.of(e));
        }

        return zipp(iterList, zipFunction);
    }

    /**
     * Zip together the iterators until one of them runs out of values.
     * Each array of values is combined into a single value using the supplied zipFunction function.
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> zip(final Collection<? extends Stream<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Stream<? extends T> e : c) {
            iterList.add(e.iteratorEx());
        }

        return zipp(iterList, zipFunction).onClose(newCloseHandler(c));
    }

    public static <T, R> Stream<R> zipp(final Collection<? extends Iterator<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final Iterator<? extends T>[] iters = c.toArray(new Iterator[len]);

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i].hasNext() == false) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R next() {
                final Object[] args = new Object[len];

                for (int i = 0; i < len; i++) {
                    args[i] = iters[i].next();
                }

                return zipFunction.apply(Arrays.asList((T[]) args));
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
    public static <A, B, R> Stream<R> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(ObjIteratorEx.of(a), ObjIteratorEx.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <A, B, C, R> Stream<R> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(ObjIteratorEx.of(a), ObjIteratorEx.of(b), ObjIteratorEx.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <A, B, R> Stream<R> zip(final Collection<? extends A> a, final Collection<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <A, B, C, R> Stream<R> zip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(a.iterator(), b.iterator(), c.iterator(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <A, B, R> Stream<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.next() : valueForNoneA, b.hasNext() ? b.next() : valueForNoneB);
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
    public static <A, B, C, R> Stream<R> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.next() : valueForNoneA, b.hasNext() ? b.next() : valueForNoneB,
                        c.hasNext() ? c.next() : valueForNoneC);
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
    public static <A, B, R> Stream<R> zip(final Stream<? extends A> a, final Stream<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
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
    public static <A, B, C, R> Stream<R> zip(final Stream<? extends A> a, final Stream<? extends B> b, final Stream<? extends C> c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return zip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    public static <T, R> Stream<R> zip(final List<? extends Collection<? extends T>> c, final List<? extends T> valuesForNone,
            Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.size()) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Collection<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return zipp(iterList, valuesForNone, zipFunction);
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
    public static <T, R> Stream<R> zip(final Collection<? extends Stream<? extends T>> c, final List<? extends T> valuesForNone,
            final Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.size()) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final Stream<? extends T>[] ss = c.toArray(new Stream[len]);
        final ObjIterator<? extends T>[] iters = new ObjIterator[len];

        for (int i = 0; i < len; i++) {
            iters[i] = ss[i].iteratorEx();
        }

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                            ss[i].close();
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final Object[] args = new Object[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].next();
                    } else {
                        args[i] = valuesForNone.get(i);
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(Arrays.asList((T[]) args));
            }
        });
    }

    /**
     * 
     * @param c
     * @param valuesForNone value to fill for any iterator runs out of values.
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> zipp(final Collection<? extends Iterator<? extends T>> c, final List<? extends T> valuesForNone,
            final Function<? super List<? extends T>, R> zipFunction) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.size()) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final Iterator<? extends T>[] iters = c.toArray(new Iterator[len]);

        return new IteratorStream<>(new ObjIteratorEx<R>() {
            @Override
            public boolean hasNext() {
                for (int i = 0; i < len; i++) {
                    if (iters[i] != null) {
                        if (iters[i].hasNext()) {
                            return true;
                        } else if (iters[i] != null) {
                            iters[i] = null;
                        }
                    }
                }

                return false;
            }

            @Override
            public R next() {
                final Object[] args = new Object[len];
                boolean hasNext = false;

                for (int i = 0; i < len; i++) {
                    if (iters[i] != null && iters[i].hasNext()) {
                        hasNext = true;
                        args[i] = iters[i].next();
                    } else {
                        args[i] = valuesForNone.get(i);
                    }
                }

                if (hasNext == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(Arrays.asList((T[]) args));
            }
        });
    }

    // NO NEED.
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final A[] a, final B[] b, final BiFunction<? super A, ? super B, R> zipFunction) {
    //        return parallelZip(a, b, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 32
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final A[] a, final B[] b, final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
    //        return parallelZip(ImmutableIterator.of(a), ImmutableIterator.of(b), zipFunction, queueSize);
    //    }
    //
    //    public static <A, B, C, R> Stream<R> parallelZip(final A[] a, final B[] b, final C[] c, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
    //        return parallelZip(a, b, c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 32
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final A[] a, final B[] b, final C[] c, final TriFunction<? super A, ? super B, ? super C, R> zipFunction,
    //            final int queueSize) {
    //        return parallelZip(ImmutableIterator.of(a), ImmutableIterator.of(b), ImmutableIterator.of(c), zipFunction, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b,
    //            final BiFunction<? super A, ? super B, R> zipFunction) {
    //        return parallelZip(a, b, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 32
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b,
    //            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
    //        return parallelZip(a.iterator(), b.iterator(), zipFunction, queueSize);
    //    }
    //
    //    public static <A, B, C, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
    //            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
    //        return parallelZip(a, b, c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 32
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
    //            final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
    //        return parallelZip(a.iterator(), b.iterator(), c.iterator(), zipFunction, queueSize);
    //    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return parallelZip(a, b, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
        final AtomicInteger threadCounterA = new AtomicInteger(1);
        final AtomicInteger threadCounterB = new AtomicInteger(1);
        final BlockingQueue<A> queueA = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<B> queueB = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(a, b, DEFAULT_ASYNC_EXECUTOR, threadCounterA, threadCounterB, queueA, queueB, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            A nextA = null;
            B nextB = null;

            @Override
            public boolean hasNext() {
                if (nextA == null || nextB == null) {
                    try {
                        while (nextA == null && onGoing.value() && (threadCounterA.get() > 0 || queueA.size() > 0)) { // (threadCounterA.get() > 0 || queueA.size() > 0) is wrong. has to check counter first
                            nextA = queueA.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextA == null) {
                            onGoing.setFalse();

                            return false;
                        }

                        while (nextB == null && onGoing.value() && (threadCounterB.get() > 0 || queueB.size() > 0)) { // (threadCounterB.get() > 0 || queueB.size() > 0) is wrong. has to check counter first
                            nextB = queueB.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextB == null) {
                            onGoing.setFalse();

                            return false;
                        }
                    } catch (Exception e) {
                        setError(eHolder, e, onGoing);
                    }

                    if (eHolder.value() != null) {
                        throwError(eHolder, onGoing);
                    }
                }

                return true;
            }

            @Override
            public R next() {
                if ((nextA == null || nextB == null) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                boolean isOK = false;

                try {
                    final R result = zipFunction.apply(nextA == NONE ? null : nextA, nextB == NONE ? null : nextB);
                    nextA = null;
                    nextB = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    public static <A, B, C, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return parallelZip(a, b, c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
        final AtomicInteger threadCounterA = new AtomicInteger(1);
        final AtomicInteger threadCounterB = new AtomicInteger(1);
        final AtomicInteger threadCounterC = new AtomicInteger(1);
        final BlockingQueue<A> queueA = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<B> queueB = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<C> queueC = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(a, b, c, DEFAULT_ASYNC_EXECUTOR, threadCounterA, threadCounterB, threadCounterC, queueA, queueB, queueC, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            A nextA = null;
            B nextB = null;
            C nextC = null;

            @Override
            public boolean hasNext() {
                if (nextA == null || nextB == null || nextC == null) {
                    try {
                        while (nextA == null && onGoing.value() && (threadCounterA.get() > 0 || queueA.size() > 0)) { // (threadCounterA.get() > 0 || queueA.size() > 0) is wrong. has to check counter first
                            nextA = queueA.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextA == null) {
                            onGoing.setFalse();

                            return false;
                        }

                        while (nextB == null && onGoing.value() && (threadCounterB.get() > 0 || queueB.size() > 0)) { // (threadCounterB.get() > 0 || queueB.size() > 0) is wrong. has to check counter first
                            nextB = queueB.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextB == null) {
                            onGoing.setFalse();

                            return false;
                        }

                        while (nextC == null && onGoing.value() && (threadCounterC.get() > 0 || queueC.size() > 0)) { // (threadCounterC.get() > 0 || queueC.size() > 0) is wrong. has to check counter first
                            nextC = queueC.poll(1, TimeUnit.MILLISECONDS);
                        }

                        if (nextC == null) {
                            onGoing.setFalse();

                            return false;
                        }
                    } catch (Exception e) {
                        setError(eHolder, e, onGoing);
                    }

                    if (eHolder.value() != null) {
                        throwError(eHolder, onGoing);
                    }
                }

                return true;
            }

            @Override
            public R next() {
                if ((nextA == null || nextB == null || nextC == null) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                boolean isOK = false;

                try {
                    final R result = zipFunction.apply(nextA == NONE ? null : nextA, nextB == NONE ? null : nextB, nextC == NONE ? null : nextC);
                    nextA = null;
                    nextB = null;
                    nextC = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final BiFunction<? super A, ? super B, R> zipFunction) {
        return parallelZip(a, b, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final BiFunction<? super A, ? super B, R> zipFunction,
            final int queueSize) {
        return parallelZip(a.iteratorEx(), b.iteratorEx(), zipFunction, queueSize).onClose(newCloseHandler(N.asList(a, b)));
    }

    public static <A, B, C, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final Stream<C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return parallelZip(a, b, c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final Stream<C> c,
            final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
        return parallelZip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), zipFunction, queueSize).onClose(newCloseHandler(N.asList(a, b, c)));
    }

    public static <T, R> Stream<R> parallelZip(final List<? extends Collection<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        return parallelZip(c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    public static <T, R> Stream<R> parallelZip(final List<? extends Collection<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction,
            final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Collection<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return parallelZipp(iterList, zipFunction, queueSize);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> parallelZip(final Collection<? extends Stream<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        return parallelZip(c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <T, R> Stream<R> parallelZip(final Collection<? extends Stream<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction,
            final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Stream<? extends T> e : c) {
            iterList.add(e.iteratorEx());
        }

        return parallelZipp(iterList, zipFunction, queueSize).onClose(newCloseHandler(c));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> parallelZipp(final Collection<? extends Iterator<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction) {
        return parallelZipp(c, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <T, R> Stream<R> parallelZipp(final Collection<? extends Iterator<? extends T>> c, final Function<? super List<? extends T>, R> zipFunction,
            final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();
        final AtomicInteger[] counters = new AtomicInteger[len];
        final BlockingQueue<Object>[] queues = new ArrayBlockingQueue[len];
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(c, queueSize, DEFAULT_ASYNC_EXECUTOR, counters, queues, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            Object[] next = null;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = new Object[len];

                    for (int i = 0; i < len; i++) {
                        try {
                            while (next[i] == null && onGoing.value() && (counters[i].get() > 0 || queues[i].size() > 0)) { // (counters[i].get() > 0 || queues[i].size() > 0) is wrong. has to check counter first
                                next[i] = queues[i].poll(1, TimeUnit.MILLISECONDS);
                            }

                            if (next[i] == null) {
                                onGoing.setFalse();

                                return false;
                            }
                        } catch (Exception e) {
                            setError(eHolder, e, onGoing);
                        }

                        if (eHolder.value() != null) {
                            throwError(eHolder, onGoing);
                        }
                    }

                } else {
                    for (int i = 0; i < len; i++) {
                        if (next[i] == null) {
                            return false;
                        }
                    }
                }

                return true;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                for (int i = 0; i < len; i++) {
                    if (next[i] == NONE) {
                        next[i] = null;
                    }
                }

                boolean isOK = false;

                try {
                    R result = zipFunction.apply(Arrays.asList((T[]) next));
                    next = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    // NO NEED.
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
    //            final BiFunction<? super A, ? super B, R> zipFunction) {
    //        return parallelZip(a, b, valueForNoneA, valueForNoneB, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 32
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
    //            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
    //        return parallelZip(ImmutableIterator.of(a), ImmutableIterator.of(b), valueForNoneA, valueForNoneB, zipFunction, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
    //            final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
    //        return parallelZip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 32
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
    //            final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
    //        return parallelZip(ImmutableIterator.of(a), ImmutableIterator.of(b), ImmutableIterator.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final A valueForNoneA,
    //            final B valueForNoneB, final BiFunction<? super A, ? super B, R> zipFunction) {
    //        return parallelZip(a, b, valueForNoneA, valueForNoneB, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 32
    //     * @return
    //     */
    //    public static <A, B, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final A valueForNoneA,
    //            final B valueForNoneB, final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
    //        return parallelZip(a.iterator(), b.iterator(), valueForNoneA, valueForNoneB, zipFunction, queueSize);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
    //            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
    //        return parallelZip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    //    }
    //
    //    /**
    //     * Put the stream in try-catch to stop the back-end reading thread if error happens
    //     * <br />
    //     * <code>
    //     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
    //     *            stream.forEach(N::println);
    //     *        }
    //     * </code>
    //     * 
    //     * @param a
    //     * @param b
    //     * @param c
    //     * @param valueForNoneA
    //     * @param valueForNoneB
    //     * @param valueForNoneC
    //     * @param zipFunction
    //     * @param queueSize for each iterator. Default value is 32
    //     * @return
    //     */
    //    public static <A, B, C, R> Stream<R> parallelZip(final Collection<? extends A> a, final Collection<? extends B> b, final Collection<? extends C> c,
    //            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction,
    //            final int queueSize) {
    //        return parallelZip(a.iterator(), b.iterator(), c.iterator(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, queueSize);
    //    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return parallelZip(a, b, valueForNoneA, valueForNoneB, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
        final AtomicInteger threadCounterA = new AtomicInteger(1);
        final AtomicInteger threadCounterB = new AtomicInteger(1);
        final BlockingQueue<A> queueA = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<B> queueB = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(a, b, DEFAULT_ASYNC_EXECUTOR, threadCounterA, threadCounterB, queueA, queueB, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            A nextA = null;
            B nextB = null;

            @Override
            public boolean hasNext() {
                if (nextA == null && nextB == null) {
                    try {
                        while (nextA == null && onGoing.value() && (threadCounterA.get() > 0 || queueA.size() > 0)) { // (threadCounterA.get() > 0 || queueA.size() > 0) is wrong. has to check counter first
                            nextA = queueA.poll(1, TimeUnit.MILLISECONDS);
                        }

                        while (nextB == null && onGoing.value() && (threadCounterB.get() > 0 || queueB.size() > 0)) { // (threadCounterB.get() > 0 || queueB.size() > 0) is wrong. has to check counter first
                            nextB = queueB.poll(1, TimeUnit.MILLISECONDS);
                        }
                    } catch (Exception e) {
                        setError(eHolder, e, onGoing);
                    }

                    if (eHolder.value() != null) {
                        throwError(eHolder, onGoing);
                    }
                }

                if (nextA != null || nextB != null) {
                    return true;
                } else {
                    onGoing.setFalse();
                    return false;
                }
            }

            @Override
            public R next() {
                if ((nextA == null && nextB == null) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                nextA = nextA == NONE ? null : (nextA == null ? valueForNoneA : nextA);
                nextB = nextB == NONE ? null : (nextB == null ? valueForNoneB : nextB);
                boolean isOK = false;

                try {
                    final R result = zipFunction.apply(nextA, nextB);
                    nextA = null;
                    nextB = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return parallelZip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction,
            final int queueSize) {
        final AtomicInteger threadCounterA = new AtomicInteger(1);
        final AtomicInteger threadCounterB = new AtomicInteger(1);
        final AtomicInteger threadCounterC = new AtomicInteger(1);
        final BlockingQueue<A> queueA = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<B> queueB = new ArrayBlockingQueue<>(queueSize);
        final BlockingQueue<C> queueC = new ArrayBlockingQueue<>(queueSize);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);

        readToQueue(a, b, c, DEFAULT_ASYNC_EXECUTOR, threadCounterA, threadCounterB, threadCounterC, queueA, queueB, queueC, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            A nextA = null;
            B nextB = null;
            C nextC = null;

            @Override
            public boolean hasNext() {
                if (nextA == null && nextB == null && nextC == null) {
                    try {
                        while (nextA == null && onGoing.value() && (threadCounterA.get() > 0 || queueA.size() > 0)) { // (threadCounterA.get() > 0 || queueA.size() > 0) is wrong. has to check counter first
                            nextA = queueA.poll(1, TimeUnit.MILLISECONDS);
                        }

                        while (nextB == null && onGoing.value() && (threadCounterB.get() > 0 || queueB.size() > 0)) { // (threadCounterB.get() > 0 || queueB.size() > 0) is wrong. has to check counter first
                            nextB = queueB.poll(1, TimeUnit.MILLISECONDS);
                        }

                        while (nextC == null && onGoing.value() && (threadCounterC.get() > 0 || queueC.size() > 0)) { // (threadCounterC.get() > 0 || queueC.size() > 0) is wrong. has to check counter first
                            nextC = queueC.poll(1, TimeUnit.MILLISECONDS);
                        }
                    } catch (Exception e) {
                        setError(eHolder, e, onGoing);
                    }

                    if (eHolder.value() != null) {
                        throwError(eHolder, onGoing);
                    }
                }

                if (nextA != null || nextB != null || nextC != null) {
                    return true;
                } else {
                    onGoing.setFalse();

                    return false;
                }
            }

            @Override
            public R next() {
                if ((nextA == null && nextB == null && nextC == null) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                nextA = nextA == NONE ? null : (nextA == null ? valueForNoneA : nextA);
                nextB = nextB == NONE ? null : (nextB == null ? valueForNoneB : nextB);
                nextC = nextC == NONE ? null : (nextC == null ? valueForNoneC : nextC);
                boolean isOK = false;

                try {
                    final R result = zipFunction.apply(nextA, nextB, nextC);
                    nextA = null;
                    nextB = null;
                    nextC = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction) {
        return parallelZip(a, b, valueForNoneA, valueForNoneB, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <A, B, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final A valueForNoneA, final B valueForNoneB,
            final BiFunction<? super A, ? super B, R> zipFunction, final int queueSize) {
        return parallelZip(a.iteratorEx(), b.iteratorEx(), valueForNoneA, valueForNoneB, zipFunction, queueSize).onClose(newCloseHandler(N.asList(a, b)));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final Stream<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction) {
        return parallelZip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <A, B, C, R> Stream<R> parallelZip(final Stream<A> a, final Stream<B> b, final Stream<C> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final TriFunction<? super A, ? super B, ? super C, R> zipFunction, final int queueSize) {
        return parallelZip(a.iteratorEx(), b.iteratorEx(), c.iteratorEx(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction, queueSize)
                .onClose(newCloseHandler(N.asList(a, b, c)));
    }

    public static <T, R> Stream<R> parallelZip(final List<? extends Collection<? extends T>> c, final List<? extends T> valuesForNone,
            Function<? super List<? extends T>, R> zipFunction) {
        return parallelZip(c, valuesForNone, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    public static <T, R> Stream<R> parallelZip(final List<? extends Collection<? extends T>> c, final List<? extends T> valuesForNone,
            Function<? super List<? extends T>, R> zipFunction, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.size()) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Collection<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return parallelZipp(iterList, valuesForNone, zipFunction, queueSize);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param valuesForNone
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> parallelZip(final Collection<? extends Stream<? extends T>> c, final List<? extends T> valuesForNone,
            Function<? super List<? extends T>, R> zipFunction) {
        return parallelZip(c, valuesForNone, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param valuesForNone
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <T, R> Stream<R> parallelZip(final Collection<? extends Stream<? extends T>> c, final List<? extends T> valuesForNone,
            Function<? super List<? extends T>, R> zipFunction, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        final int len = c.size();

        if (len != valuesForNone.size()) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(len);

        for (Stream<? extends T> e : c) {
            iterList.add(e.iteratorEx());
        }

        return parallelZipp(iterList, valuesForNone, zipFunction, queueSize).onClose(newCloseHandler(c));
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param valuesForNone
     * @param zipFunction
     * @return
     */
    public static <T, R> Stream<R> parallelZipp(final Collection<? extends Iterator<? extends T>> c, final List<? extends T> valuesForNone,
            Function<? super List<? extends T>, R> zipFunction) {
        return parallelZipp(c, valuesForNone, zipFunction, DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    /**
     * Put the stream in try-catch to stop the back-end reading thread if error happens
     * <br />
     * <code>
     * try (Stream<Integer> stream = Stream.parallelZip(a, b, zipFunction)) {
     *            stream.forEach(N::println);
     *        }
     * </code>
     * 
     * @param c
     * @param valuesForNone
     * @param zipFunction
     * @param queueSize for each iterator. Default value is 32
     * @return
     */
    public static <T, R> Stream<R> parallelZipp(final Collection<? extends Iterator<? extends T>> c, final List<? extends T> valuesForNone,
            final Function<? super List<? extends T>, R> zipFunction, final int queueSize) {
        if (N.isNullOrEmpty(c)) {
            return Stream.empty();
        }

        if (c.size() != valuesForNone.size()) {
            throw new IllegalArgumentException("The size of 'valuesForNone' must be same as the size of the collection of iterators");
        }

        final int len = c.size();
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean onGoing = MutableBoolean.of(true);
        final AtomicInteger[] counters = new AtomicInteger[len];
        final BlockingQueue<Object>[] queues = new ArrayBlockingQueue[len];

        readToQueue(c, queueSize, DEFAULT_ASYNC_EXECUTOR, counters, queues, eHolder, onGoing);

        return of(new QueuedIterator<R>(queueSize) {
            Object[] next = null;

            @Override
            public boolean hasNext() {
                if (next == null) {
                    next = new Object[len];

                    for (int i = 0; i < len; i++) {
                        try {
                            while (next[i] == null && onGoing.value() && (counters[i].get() > 0 || queues[i].size() > 0)) { // (counters[i].get() > 0 || queues[i].size() > 0) is wrong. has to check counter first
                                next[i] = queues[i].poll(1, TimeUnit.MILLISECONDS);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e, onGoing);
                        }

                        if (eHolder.value() != null) {
                            throwError(eHolder, onGoing);
                        }
                    }
                }

                for (int i = 0; i < len; i++) {
                    if (next[i] != null) {
                        return true;
                    }
                }

                onGoing.setFalse();
                return false;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                for (int i = 0; i < len; i++) {
                    next[i] = next[i] == NONE ? null : (next[i] == null ? valuesForNone.get(i) : next[i]);
                }

                boolean isOK = false;
                try {
                    R result = zipFunction.apply(Arrays.asList((T[]) next));
                    next = null;
                    isOK = true;
                    return result;
                } finally {
                    // error happened
                    if (isOK == false) {
                        onGoing.setFalse();
                    }
                }
            }
        }).onClose(new Runnable() {
            @Override
            public void run() {
                onGoing.setFalse();
            }
        });
    }

    //    /**
    //     * 
    //     * @param c
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     */
    //    public static <T, L, R> Pair<Stream<L>, Stream<R>> unzip(final Collection<? extends T> c, final BiConsumer<? super T, Pair<L, R>> unzip) {
    //        final Pair<List<L>, List<R>> p = Seq.unzip(c, unzip);
    //
    //        return Pair.of(Stream.of(p.left), Stream.of(p.right));
    //    }
    //
    //    /**
    //     * 
    //     * @param iter
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     */
    //    public static <T, L, R> Pair<Stream<L>, Stream<R>> unzip(final Iterator<? extends T> iter, final BiConsumer<? super T, Pair<L, R>> unzip) {
    //        final Pair<List<L>, List<R>> p = Iterators.unzip(iter, unzip);
    //
    //        return Pair.of(Stream.of(p.left), Stream.of(p.right));
    //    }
    //
    //    /**
    //     * 
    //     * @param c
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     */
    //    public static <T, L, M, R> Triple<Stream<L>, Stream<M>, Stream<R>> unzipp(final Collection<? extends T> c,
    //            final BiConsumer<? super T, Triple<L, M, R>> unzip) {
    //        final Triple<List<L>, List<M>, List<R>> p = Seq.unzipp(c, unzip);
    //
    //        return Triple.of(Stream.of(p.left), Stream.of(p.middle), Stream.of(p.right));
    //    }
    //
    //    /**
    //     * 
    //     * @param iter
    //     * @param unzip the second parameter is an output parameter.
    //     * @return
    //     */
    //    public static <T, L, M, R> Triple<Stream<L>, Stream<M>, Stream<R>> unzipp(final Iterator<? extends T> iter,
    //            final BiConsumer<? super T, Triple<L, M, R>> unzip) {
    //        final Triple<List<L>, List<M>, List<R>> p = Iterators.unzipp(iter, unzip);
    //
    //        return Triple.of(Stream.of(p.left), Stream.of(p.middle), Stream.of(p.right));
    //    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final T[] a, final T[] b, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public T next() {
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
    public static <T> Stream<T> merge(final T[] a, final T[] b, final T[] c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), Stream.of(c).iteratorEx(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Collection<? extends T> a, final Collection<? extends T> b,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(a.iterator(), b.iterator(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Collection<? extends T> a, final Collection<? extends T> b, final Collection<? extends T> c,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(a.iterator(), b.iterator(), c.iterator(), nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return new IteratorStream<>(new ObjIteratorEx<T>() {
            private T nextA = null;
            private T nextB = null;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() {
                return hasNextA || hasNextB || a.hasNext() || b.hasNext();
            }

            @Override
            public T next() {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.next())) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.next()), nextB) == Nth.FIRST) {
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
                        if (nextSelector.apply((nextA = a.next()), (nextB = b.next())) == Nth.FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.next();
                    }
                } else if (b.hasNext()) {
                    return b.next();
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
    public static <T> Stream<T> merge(final Iterator<? extends T> a, final Iterator<? extends T> b, final Iterator<? extends T> c,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), c, nextSelector);
    }

    /**
     * 
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Stream<? extends T> a, final Stream<? extends T> b, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return merge(a.iteratorEx(), b.iteratorEx(), nextSelector).onClose(newCloseHandler(N.asList(a, b)));
    }

    public static <T> Stream<T> merge(final Stream<? extends T> a, final Stream<? extends T> b, final Stream<? extends T> c,
            final BiFunction<? super T, ? super T, Nth> nextSelector) {

        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    public static <T> Stream<T> merge(final List<? extends Collection<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        N.checkArgNotNull(nextSelector);

        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return of(c.iterator().next());
        } else if (c.size() == 2) {
            final Iterator<? extends Collection<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(c.size());

        for (Collection<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return mergge(iterList, nextSelector);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> merge(final Collection<? extends Stream<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        N.checkArgNotNull(nextSelector);

        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return (Stream<T>) c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends Stream<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends Stream<? extends T>> iter = c.iterator();
        Stream<T> result = merge(iter.next(), iter.next(), nextSelector);

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
    public static <T> Stream<T> mergge(final Collection<? extends Iterator<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        N.checkArgNotNull(nextSelector);

        if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return of(c.iterator().next());
        } else if (c.size() == 2) {
            final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        }

        final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
        Stream<T> result = merge(iter.next(), iter.next(), nextSelector);

        while (iter.hasNext()) {
            result = merge(result.iteratorEx(), iter.next(), nextSelector);
        }

        return result;
    }

    public static <T> Stream<T> parallelMerge(final List<? extends Collection<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    public static <T> Stream<T> parallelMerge(final List<? extends Collection<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector,
            final int maxThreadNum) {
        N.checkArgNotNull(nextSelector);

        if (maxThreadNum <= 1) {
            return merge(c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return of(c.iterator().next());
        } else if (c.size() == 2) {
            final Iterator<? extends Collection<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), nextSelector);
        } else if (c.size() == 3) {
            final Iterator<? extends Collection<? extends T>> iter = c.iterator();
            return merge(iter.next(), iter.next(), iter.next(), nextSelector);
        }

        final List<Iterator<? extends T>> iterList = new ArrayList<>(c.size());

        for (Collection<? extends T> e : c) {
            iterList.add(e.iterator());
        }

        return parallelMergge(iterList, nextSelector, maxThreadNum);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> parallelMerge(final Collection<? extends Stream<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return parallelMerge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static <T> Stream<T> parallelMerge(final Collection<? extends Stream<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector,
            final int maxThreadNum) {
        N.checkArgument(maxThreadNum > 0, "'maxThreadNum' must not less than 1");

        if (maxThreadNum <= 1) {
            return merge(c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return (Stream<T>) c.iterator().next();
        } else if (c.size() == 2) {
            final Iterator<? extends Stream<? extends T>> iter = c.iterator();
            return merge(iter.next().queued(), iter.next().queued(), nextSelector);
        } else if (c.size() == 3) {
            final Iterator<? extends Stream<? extends T>> iter = c.iterator();
            return merge(iter.next().queued(), iter.next().queued(), iter.next().queued(), nextSelector);
        }

        final Queue<Stream<T>> queue = N.newLinkedList();

        for (Stream<? extends T> e : c) {
            queue.add((Stream<T>) e);
        }

        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    Stream<T> a = null;
                    Stream<T> b = null;
                    Stream<T> c = null;

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

                            c = Stream.of((T[]) merge(a, b, nextSelector).toArray());

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

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T> Stream<T> parallelMergge(final Collection<? extends Iterator<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return parallelMergge(c, nextSelector, DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * 
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @param maxThreadNum
     * @return
     */
    public static <T> Stream<T> parallelMergge(final Collection<? extends Iterator<? extends T>> c, final BiFunction<? super T, ? super T, Nth> nextSelector,
            final int maxThreadNum) {
        N.checkArgument(maxThreadNum > 0, "'maxThreadNum' must not less than 1");

        if (maxThreadNum <= 1) {
            return mergge(c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return empty();
        } else if (c.size() == 1) {
            return of(c.iterator().next());
        } else if (c.size() == 2) {
            final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            final Iterator<? extends T> a = iter.next();
            final Iterator<? extends T> b = iter.next();
            return merge(a instanceof QueuedIterator ? a : Stream.of(a).queued().iteratorEx(),
                    b instanceof QueuedIterator ? b : Stream.of(b).queued().iteratorEx(), nextSelector);
        } else if (c.size() == 3) {
            final Iterator<? extends Iterator<? extends T>> iter = c.iterator();
            final Iterator<? extends T> iterA = iter.next();
            final Iterator<? extends T> iterB = iter.next();
            final Iterator<? extends T> iterC = iter.next();
            return merge(iterA instanceof QueuedIterator ? iterA : Stream.of(iterA).queued().iteratorEx(),
                    iterB instanceof QueuedIterator ? iterB : Stream.of(iterB).queued().iteratorEx(),
                    iterC instanceof QueuedIterator ? iterC : Stream.of(iterC).queued().iteratorEx(), nextSelector);
        }

        final Queue<Iterator<? extends T>> queue = N.newLinkedList(c);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableInt cnt = MutableInt.of(c.size());
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(c.size() - 1);

        for (int i = 0, n = N.min(maxThreadNum, c.size() / 2 + 1); i < n; i++) {
            futureList.add(DEFAULT_ASYNC_EXECUTOR.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    Iterator<? extends T> a = null;
                    Iterator<? extends T> b = null;
                    Iterator<? extends T> c = null;

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

                            c = (Iterator<? extends T>) ObjIteratorEx.of(merge(a instanceof QueuedIterator ? a : Stream.of(a).queued().iteratorEx(),
                                    b instanceof QueuedIterator ? b : Stream.of(b).queued().iteratorEx(), nextSelector).toArray());

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

        complete(futureList, eHolder);

        final Iterator<? extends T> a = queue.poll();
        final Iterator<? extends T> b = queue.poll();

        return merge(a instanceof QueuedIterator ? a : Stream.of(a).queued().iteratorEx(), b instanceof QueuedIterator ? b : Stream.of(b).queued().iteratorEx(),
                nextSelector);
    }

    private static <B, A> void readToQueue(final Iterator<? extends A> a, final Iterator<? extends B> b, final AsyncExecutor asyncExecutor,
            final AtomicInteger threadCounterA, final AtomicInteger threadCounterB, final BlockingQueue<A> queueA, final BlockingQueue<B> queueB,
            final Holder<Throwable> eHolder, final MutableBoolean onGoing) {
        asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
            @Override
            public void run() {
                try {
                    A nextA = null;

                    while (onGoing.value() && a.hasNext()) {
                        nextA = a.next();

                        if (nextA == null) {
                            nextA = (A) NONE;
                        }

                        while (onGoing.value() && queueA.offer(nextA, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterA.decrementAndGet();
                }
            }
        });

        asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
            @Override
            public void run() {
                try {
                    B nextB = null;

                    while (onGoing.value() && b.hasNext()) {
                        nextB = b.next();

                        if (nextB == null) {
                            nextB = (B) NONE;
                        }

                        while (onGoing.value() && queueB.offer(nextB, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterB.decrementAndGet();
                }
            }
        });
    }

    private static <B, C, A> void readToQueue(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final AsyncExecutor asyncExecutor, final AtomicInteger threadCounterA, final AtomicInteger threadCounterB, final AtomicInteger threadCounterC,
            final BlockingQueue<A> queueA, final BlockingQueue<B> queueB, final BlockingQueue<C> queueC, final Holder<Throwable> eHolder,
            final MutableBoolean onGoing) {
        asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
            @Override
            public void run() {
                try {
                    A nextA = null;

                    while (onGoing.value() && a.hasNext()) {
                        nextA = a.next();

                        if (nextA == null) {
                            nextA = (A) NONE;
                        }

                        while (onGoing.value() && queueA.offer(nextA, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterA.decrementAndGet();
                }
            }
        });

        asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
            @Override
            public void run() {
                try {
                    B nextB = null;

                    while (onGoing.value() && b.hasNext()) {
                        nextB = b.next();

                        if (nextB == null) {
                            nextB = (B) NONE;
                        }

                        while (onGoing.value() && queueB.offer(nextB, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterB.decrementAndGet();
                }
            }
        });

        asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
            @Override
            public void run() {
                try {
                    C nextC = null;

                    while (onGoing.value() && c.hasNext()) {
                        nextC = c.next();

                        if (nextC == null) {
                            nextC = (C) NONE;
                        }

                        while (onGoing.value() && queueC.offer(nextC, 100, TimeUnit.MILLISECONDS) == false) {
                            // continue
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    threadCounterC.decrementAndGet();
                }
            }
        });
    }

    private static void readToQueue(final Collection<? extends Iterator<?>> c, final int queueSize, final AsyncExecutor asyncExecutor,
            final AtomicInteger[] counters, final BlockingQueue<Object>[] queues, final Holder<Throwable> eHolder, final MutableBoolean onGoing) {
        int idx = 0;

        for (Iterator<?> e : c) {
            counters[idx] = new AtomicInteger(1);
            queues[idx] = new ArrayBlockingQueue<>(queueSize);

            final Iterator<?> iter = e;
            final AtomicInteger count = counters[idx];
            final BlockingQueue<Object> queue = queues[idx];

            asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                @Override
                public void run() {
                    try {
                        Object next = null;

                        while (onGoing.value() && iter.hasNext()) {
                            next = iter.next();

                            if (next == null) {
                                next = NONE;
                            }

                            while (onGoing.value() && queue.offer(next, 100, TimeUnit.MILLISECONDS) == false) {
                                // continue
                            }
                        }
                    } catch (Exception e) {
                        setError(eHolder, e, onGoing);
                    } finally {
                        count.decrementAndGet();
                    }
                }
            });

            idx++;
        }
    }

    public static abstract class StreamEx<T> extends Stream<T> {
        private StreamEx(boolean sorted, Comparator<? super T> cmp, Collection<Runnable> closeHandlers) {
            super(sorted, cmp, closeHandlers);
            // Factory class.
        }
    }

    /**
     * SOO = Sequential Only Operations.
     * These operations run sequentially only, even under parallel stream.
     * 
     * Mostly, if an operation is a single operation, or has to be executed in order,
     *  or there is no benefit to execute it in parallel, it will run sequentially, even under parallel stream.
     *
     */
    public static enum SOO {
        split, splitToList, splitToSet, splitAt, splitBy, sliding, slidingToList, //
        intersection, difference, symmetricDifference, //
        reversed, shuffled, rotated, distinct, hasDuplicates, //
        append, prepend, indexed, skip, skipLast, limit, step, //
        join, queued, merge, persist, //
        combinations, permutations, orderedPermutations, percentiles, cartesianProduct, //
        collapse, rangeMap, scan, intersperse, top, kthLargest, last, //
        count, findFirstOrLast, findFirstAndLast, //
        toArray, toList, toSet, toMultiset, toLongMultiset, toMatrix, toDataSet, //
        boxed, iterator, asIntStream, asLongStream, asFloatStream, asDoubleStream, //
        select, println, onClosed, close;
    }

    /**
     * PSO = Parallel supported Operations.
     * These operations run in parallel under parallel stream.
     * 
     * Mostly, if an operation can be executed in parallel and has benefit to execute it in parallel. it will run in parallel under parallel stream.
     * 
     * @author haiyangl
     *
     */
    public static enum PSO {
        map, slidingMap, mapToEntry, mapTo_, mapFirst, mapFirstOrElse, mapLast, mapLastOrElse, //
        flatMap, flattMap, flatMapp, flatMapTo_, //
        filter, takeWhile, dropWhile, removeIf, skipNull, //
        sorted, reverseSorted, distinctBy, distinct_filter, peek, peekFirst, peekLast, //
        groupBy, groupByEntry, groupTo, partitionBy, partitionByToEntry, paritionTo, countBy, countByToEntry, //
        min, minBy, max, maxBy, sumInt, sumLong, sumDouble, averageInt, averageLong, averageDouble, //
        toMap, toMulitmap, summerize, summerizze, //
        forEach, forEachPair, forEachTriple, anyMatch, allMatch, noneMatch, findFirst, findLast, findAny, //
        reduce, collect, innerJoin, fullJoin, leftJoin, rightJoin, zipWith;
    }

    /**
     * LAIO = Loading All Intermediate Operations.
     * 
     * Intermediate operations which will load or go through all the elements in the stream.
     * <br />
     * These operation are stateful intermediate operations, which means that subsequent operations no longer operate on the backing collection/iterator, but on an internal state.
     * 
     */
    public static enum LAIO {
        sorted, sortedBy, reverseSorted, resversed, shuffled, rotated, last_n, //
        groupBy, groupByToEntity, partitionBy, partitionByToEntity, countBy, countByToEntry; //
        // HEADD, TAILL, HEAD_AND_TAILL;
    }
}
