/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.stream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.LongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;
import java.util.stream.Collector;

import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BufferedCsvWriter;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.CsvUtil;
import com.landawn.abacus.util.DataSourceUtil;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiFunctions;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.Iterables;
import com.landawn.abacus.util.Iterators;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.PermutationIterator;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.ObjIteratorEx.BufferedIterator;

/**
 * Abstract base implementation of {@link Stream} providing common functionality for object stream operations.
 * This class sits between the {@link BaseStream} interface and the concrete stream implementations
 * (such as {@code ArrayStream} and {@code IteratorStream}), providing shared logic that is reused
 * across all object-stream subtypes.
 *
 * <p>Direct instantiation is not possible. Use the factory methods on {@link Stream} to obtain instances.
 *
 * <p>This implementation covers:
 * <ul>
 * <li>Rate limiting and delay operations for controlling stream processing speed</li>
 * <li>Filtering, mapping, and transformation operations for objects</li>
 * <li>FlatMap operations targeting all primitive and object stream types</li>
 * <li>Sliding, collapsing, and range-mapping operations</li>
 * <li>Collection, grouping, and aggregation operations</li>
 * <li>Sorting with custom comparators</li>
 * <li>Distinct operations based on object equality or custom key extractors</li>
 * <li>Join operations for combining elements into strings</li>
 * <li>Persistence operations for writing to files, {@link java.io.OutputStream}, {@link java.io.Writer},
 *     databases ({@link java.sql.PreparedStatement}), and CSV/JSON formats</li>
 * </ul>
 *
 * <p>Methods annotated with {@code @SequentialOnly} are always executed sequentially even when the
 * stream is in parallel mode. Methods annotated with {@code @ParallelSupported} may execute their
 * mapped function concurrently across threads when the stream is parallel.
 *
 * @param <T> the type of stream elements
 *
 * @see Stream
 * @see BaseStream
 */
@SuppressWarnings({ "java:S1192", "java:S1698", "java:S1845", "java:S2445", "java:S3077" })
abstract class AbstractStream<T> extends Stream<T> {

    /**
     * Constructs an AbstractStream with the specified sorting state, comparator, and close handlers.
     *
     * @param sorted whether the stream elements are in sorted order
     * @param cmp the comparator for ordering elements, or {@code null} if using natural order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractStream(final boolean sorted, final Comparator<? super T> cmp, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, cmp, closeHandlers);
    }

    @Override
    public <U> Stream<U> select(final Class<? extends U> targetType) {
        if (isParallel()) {
            //noinspection resource
            return (Stream<U>) sequential().filter(Fn.instanceOf(targetType)).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return (Stream<U>) filter(Fn.instanceOf(targetType));
        }
    }

    @Override
    public <U> Stream<Pair<T, U>> pairWith(final Function<? super T, ? extends U> extractor) {
        return map(t -> Pair.of(t, extractor.apply(t)));
    }

    @Override
    public Stream<T> skipUntil(final Predicate<? super T> predicate) {
        return dropWhile(Fn.not(predicate));
    }

    @Override
    public Stream<T> filter(final Predicate<? super T> predicate, final Consumer<? super T> onDrop) throws IllegalStateException {
        assertNotClosed();

        return filter(value -> {
            if (!predicate.test(value)) {
                onDrop.accept(value);
                return false;
            }

            return true;
        });
    }

    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate, final Consumer<? super T> onDrop) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(value -> {
            if (predicate.test(value)) {
                onDrop.accept(value);
                return true;
            }

            return false;
        });
    }

    @Override
    public Stream<T> step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final ObjIteratorEx<T> iter = iteratorEx();

        final Iterator<T> iterator = new ObjIteratorEx<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final T next = iter.next();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(iterator, isSorted(), comparator());
    }

    @Override
    public <R> Stream<R> slidingMap(final BiFunction<? super T, ? super T, ? extends R> mapper) throws IllegalStateException {
        assertNotClosed();

        return slidingMap(1, mapper);
    }

    @Override
    public <R> Stream<R> slidingMap(final int increment, final BiFunction<? super T, ? super T, ? extends R> mapper) throws IllegalStateException {
        assertNotClosed();

        return slidingMap(increment, false, mapper);
    }

    @Override
    public <R> Stream<R> slidingMap(final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper) throws IllegalStateException {
        assertNotClosed();

        return slidingMap(1, mapper);
    }

    @Override
    public <R> Stream<R> slidingMap(final int increment, final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper) throws IllegalStateException {
        assertNotClosed();

        return slidingMap(increment, false, mapper);
    }

    @Override
    public <R> Stream<R> mapIfNotNull(final Function<? super T, ? extends R> mapper) {
        //noinspection resource
        return skipNulls().map(mapper);
    }

    @Override
    public <K, V> EntryStream<K, V> mapToEntry(final Function<? super T, ? extends Map.Entry<? extends K, ? extends V>> mapper) throws IllegalStateException {
        assertNotClosed();

        final Function<T, T> secondMapper = Fn.identity();

        if (mapper == secondMapper) {
            return EntryStream.of((Stream<Map.Entry<K, V>>) this);
        }

        return EntryStream.of(map(mapper));
    }

    @Override
    public <K, V> EntryStream<K, V> mapToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper)
            throws IllegalStateException {
        assertNotClosed();

        final Function<T, Map.Entry<K, V>> mapper = t -> new SimpleImmutableEntry<>(keyMapper.apply(t), valueMapper.apply(t));

        return mapToEntry(mapper);
    }

    @Override
    public <R> Stream<R> flattMap(final Function<? super T, ? extends java.util.stream.Stream<? extends R>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMap(t -> Stream.from(mapper.apply(t)));
    }

    @Override
    public CharStream flatMapArrayToChar(final Function<? super T, char[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToChar(t -> CharStream.of(mapper.apply(t)));
    }

    @Override
    public CharStream flatmapToChar(final Function<? super T, ? extends Collection<Character>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToChar(t -> CharStream.of(mapper.apply(t)));
    }

    @Override
    public ByteStream flatMapArrayToByte(final Function<? super T, byte[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToByte(t -> ByteStream.of(mapper.apply(t)));
    }

    @Override
    public ByteStream flatmapToByte(final Function<? super T, ? extends Collection<Byte>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToByte(t -> ByteStream.of(mapper.apply(t)));
    }

    @Override
    public ShortStream flatMapArrayToShort(final Function<? super T, short[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToShort(t -> ShortStream.of(mapper.apply(t)));
    }

    @Override
    public ShortStream flatmapToShort(final Function<? super T, ? extends Collection<Short>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToShort(t -> ShortStream.of(mapper.apply(t)));
    }

    @Override
    public IntStream flatMapArrayToInt(final Function<? super T, int[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToInt(t -> IntStream.of(mapper.apply(t)));
    }

    @Override
    public IntStream flatmapToInt(final Function<? super T, ? extends Collection<Integer>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToInt(t -> IntStream.of(mapper.apply(t)));
    }

    @Override
    public LongStream flatMapArrayToLong(final Function<? super T, long[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToLong(t -> LongStream.of(mapper.apply(t)));
    }

    @Override
    public LongStream flatmapToLong(final Function<? super T, ? extends Collection<Long>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToLong(t -> LongStream.of(mapper.apply(t)));
    }

    @Override
    public FloatStream flatMapArrayToFloat(final Function<? super T, float[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToFloat(t -> FloatStream.of(mapper.apply(t)));
    }

    @Override
    public FloatStream flatmapToFloat(final Function<? super T, ? extends Collection<Float>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToFloat(t -> FloatStream.of(mapper.apply(t)));
    }

    @Override
    public DoubleStream flatMapArrayToDouble(final Function<? super T, double[]> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToDouble(t -> DoubleStream.of(mapper.apply(t)));
    }

    @Override
    public DoubleStream flatmapToDouble(final Function<? super T, ? extends Collection<Double>> mapper) throws IllegalStateException {
        assertNotClosed();

        return flatMapToDouble(t -> DoubleStream.of(mapper.apply(t)));
    }

    @Override
    public <R> Stream<R> flatmapIfNotNull(final Function<? super T, ? extends Collection<? extends R>> mapper) {
        //noinspection resource
        return skipNulls().flatmap(mapper);
    }

    @Override
    public <U, R> Stream<R> flatmapIfNotNull(final Function<? super T, ? extends Collection<? extends U>> mapper,
            final Function<? super U, ? extends Collection<? extends R>> secondMapper) {
        //noinspection resource
        return skipNulls().flatmap(mapper).skipNulls().flatmap(secondMapper);
    }

    @Override
    public <K, V> EntryStream<K, V> flatMapToEntry(final Function<? super T, ? extends Stream<? extends Map.Entry<? extends K, ? extends V>>> mapper)
            throws IllegalStateException {
        assertNotClosed();

        return EntryStream.of(flatMap(mapper));
    }

    @Override
    public <K, V> EntryStream<K, V> flatmapToEntry(final Function<? super T, ? extends Map<? extends K, ? extends V>> mapper) throws IllegalStateException {
        assertNotClosed();

        final Function<? super T, ? extends Collection<Entry<K, V>>> secondMapper = t -> N.nullToEmpty((Map<K, V>) mapper.apply(t)).entrySet();

        //noinspection resource
        return flatmap(secondMapper).mapToEntry(Fn.identity());
    }

    @Override
    public <K, V> EntryStream<K, V> flattMapToEntry(final Function<? super T, ? extends EntryStream<? extends K, ? extends V>> mapper)
            throws IllegalStateException {
        assertNotClosed();

        final Function<? super T, ? extends Stream<? extends Entry<? extends K, ? extends V>>> secondMapper = t -> {
            final EntryStream<? extends K, ? extends V> s = mapper.apply(t);

            return s == null ? Stream.empty() : s.entries();
        };

        return flatMapToEntry(secondMapper);
    }

    @Override
    public <R> Stream<R> mapMulti(final BiConsumer<? super T, ? super Consumer<R>> mapper) {
        final boolean isParallel = isParallel();

        if (isParallel) {
            final Function<T, Collection<R>> secondMapper = t -> {
                final SpinedBuffer<R> buffer = new SpinedBuffer<>();

                mapper.accept(t, buffer);

                return buffer;
            };

            return flatmap(secondMapper);
        } else {
            final Deque<R> queue = new ArrayDeque<>();

            final Consumer<R> consumer = queue::offer;

            @SuppressWarnings("resource")
            final ObjIteratorEx<T> iter = iteratorEx();

            return newStream(new ObjIteratorEx<>() { //NOSONAR
                @Override
                public boolean hasNext() {
                    if (queue.size() == 0) {
                        while (iter.hasNext()) {
                            mapper.accept(iter.next(), consumer);

                            if (queue.size() > 0) {
                                break;
                            }
                        }
                    }

                    return queue.size() > 0;
                }

                @Override
                public R next() {
                    if (queue.size() == 0 && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return queue.poll();
                }
            }, false, null);
        }
    }

    @Override
    public IntStream mapMultiToInt(final BiConsumer<? super T, ? super IntConsumer> mapper) {
        final Function<T, IntStream> secondMapper = t -> {
            final SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();

            mapper.accept(t, buffer);

            return IntStream.of(buffer.iterator());
        };

        return flatMapToInt(secondMapper);
    }

    @Override
    public LongStream mapMultiToLong(final BiConsumer<? super T, ? super LongConsumer> mapper) {
        final Function<T, LongStream> secondMapper = t -> {
            final SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();

            mapper.accept(t, buffer);

            return LongStream.of(buffer.iterator());
        };

        return flatMapToLong(secondMapper);
    }

    @Override
    public DoubleStream mapMultiToDouble(final BiConsumer<? super T, ? super DoubleConsumer> mapper) {
        final Function<T, DoubleStream> secondMapper = t -> {
            final SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();

            mapper.accept(t, buffer);

            return DoubleStream.of(buffer.iterator());
        };

        return flatMapToDouble(secondMapper);
    }

    @Override
    public <R> Stream<R> mapPartial(final Function<? super T, Optional<R>> mapper) {
        final Predicate<Optional<R>> predicate = Fn.isPresent();
        final Function<Optional<R>, R> func = Fn.getIfPresentOrElseNull();
        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(predicate).map(func));
        } else {
            //noinspection resource

            return map(mapper).filter(predicate).map(func);
        }
    }

    @Override
    public IntStream mapPartialToInt(final Function<? super T, OptionalInt> mapper) {
        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(Fn.IS_PRESENT_INT).mapToInt(Fn.GET_AS_INT));
        } else {
            //noinspection resource
            return map(mapper).filter(Fn.IS_PRESENT_INT).mapToInt(Fn.GET_AS_INT);
        }
    }

    @Override
    public LongStream mapPartialToLong(final Function<? super T, OptionalLong> mapper) {
        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(Fn.IS_PRESENT_LONG).mapToLong(Fn.GET_AS_LONG));
        } else {
            //noinspection resource
            return map(mapper).filter(Fn.IS_PRESENT_LONG).mapToLong(Fn.GET_AS_LONG);
        }
    }

    @Override
    public DoubleStream mapPartialToDouble(final Function<? super T, OptionalDouble> mapper) {
        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(Fn.IS_PRESENT_DOUBLE).mapToDouble(Fn.GET_AS_DOUBLE));
        } else {
            //noinspection resource
            return map(mapper).filter(Fn.IS_PRESENT_DOUBLE).mapToDouble(Fn.GET_AS_DOUBLE);
        }
    }

    @Override
    public <R> Stream<R> mapPartialJdk(final Function<? super T, java.util.Optional<R>> mapper) {
        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(Fn.isPresentJdk()).map(Fn.getIfPresentOrElseNullJdk()));
        } else {
            //noinspection resource
            return map(mapper).filter(Fn.isPresentJdk()).map(Fn.getIfPresentOrElseNullJdk());
        }
    }

    @Override
    public IntStream mapPartialToIntJdk(final Function<? super T, java.util.OptionalInt> mapper) {
        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(Fn.IS_PRESENT_INT_JDK).mapToInt(Fn.GET_AS_INT_JDK));
        } else {
            //noinspection resource
            return map(mapper).filter(Fn.IS_PRESENT_INT_JDK).mapToInt(Fn.GET_AS_INT_JDK);
        }
    }

    @Override
    public LongStream mapPartialToLongJdk(final Function<? super T, java.util.OptionalLong> mapper) {
        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(Fn.IS_PRESENT_LONG_JDK).mapToLong(Fn.GET_AS_LONG_JDK));
        } else {
            //noinspection resource
            return map(mapper).filter(Fn.IS_PRESENT_LONG_JDK).mapToLong(Fn.GET_AS_LONG_JDK);
        }
    }

    @Override
    public DoubleStream mapPartialToDoubleJdk(final Function<? super T, java.util.OptionalDouble> mapper) {
        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(Fn.IS_PRESENT_DOUBLE_JDK).mapToDouble(Fn.GET_AS_DOUBLE_JDK));
        } else {
            //noinspection resource
            return map(mapper).filter(Fn.IS_PRESENT_DOUBLE_JDK).mapToDouble(Fn.GET_AS_DOUBLE_JDK);
        }
    }

    @Override
    public <U> Stream<U> rangeMap(final BiPredicate<? super T, ? super T> sameRange, final BiFunction<? super T, ? super T, ? extends U> mapper)
            throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private T left = null, right = null, next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                left = hasNext ? next : iter.next();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.next();

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
    public Stream<List<T>> collapse(final BiPredicate<? super T, ? super T> collapsible) throws IllegalStateException {
        assertNotClosed();

        return collapse(collapsible, Suppliers.ofList());
    }

    @Override
    public <C extends Collection<T>> Stream<C> collapse(final BiPredicate<? super T, ? super T> collapsible, final Supplier<? extends C> supplier)
            throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private final ObjIteratorEx<T> iter = iteratorEx();
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public C next() {
                final C c = supplier.get();
                c.add(hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        c.add(next);
                    } else {
                        break;
                    }
                }

                return c;
            }
        }, false, null);
    }

    @Override
    public Stream<T> collapse(final BiPredicate<? super T, ? super T> collapsible, final BinaryOperator<T> mergeFunction) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                T res = hasNext ? next : (next = iter.next());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        res = mergeFunction.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> collapse(final BiPredicate<? super T, ? super T> collapsible, final U init, final BiFunction<? super U, ? super T, U> op)
            throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() {
                U res = op.apply(init, hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        res = op.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> collapse(final BiPredicate<? super T, ? super T> collapsible, final Collector<? super T, ?, R> collector)
            throws IllegalStateException {
        assertNotClosed();
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() {
                final Object c = supplier.get();
                accumulator.accept(c, hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        accumulator.accept(c, next);
                    } else {
                        break;
                    }
                }

                return finisher.apply(c);
            }
        }, false, null);
    }

    @Override
    public Stream<List<T>> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible) throws IllegalStateException {
        assertNotClosed();

        return collapse(collapsible, Suppliers.ofList());
    }

    @Override
    public <C extends Collection<T>> Stream<C> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible, final Supplier<? extends C> supplier)
            throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private final ObjIteratorEx<T> iter = iteratorEx();
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public C next() {
                final T first = hasNext ? next : (next = iter.next());
                final C c = supplier.get();
                c.add(first);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        c.add(next);
                    } else {
                        break;
                    }
                }

                return c;
            }
        }, false, null);
    }

    @Override
    public Stream<T> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible, final BinaryOperator<T> mergeFunction)
            throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                final T first = hasNext ? next : (next = iter.next());
                T res = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        res = mergeFunction.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible, final U init, final BiFunction<? super U, ? super T, U> op)
            throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() {
                final T first = hasNext ? next : (next = iter.next());
                U res = op.apply(init, first);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        res = op.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible, final Collector<? super T, ?, R> collector)
            throws IllegalStateException {
        assertNotClosed();
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() {
                final T first = hasNext ? next : (next = iter.next());
                final Object c = supplier.get();
                accumulator.accept(c, first);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        accumulator.accept(c, next);
                    } else {
                        break;
                    }
                }

                return finisher.apply(c);
            }
        }, false, null);
    }

    @Override
    public Stream<T> scan(final BinaryOperator<T> accumulator) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private T res = null;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.next());
                } else {
                    return (res = accumulator.apply(res, iter.next()));
                }
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> scan(final U init, final BiFunction<? super U, ? super T, U> accumulator) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private U res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public U next() {
                return (res = accumulator.apply(res, iter.next()));
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> scan(final U init, final boolean initIncluded, final BiFunction<? super U, ? super T, U> accumulator) throws IllegalStateException {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean isFirst = true;
            private U res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public U next() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.apply(res, iter.next()));
            }
        }, false, null);
    }

    //    @Override
    //    public Stream<Stream<T>> split(final int chunkSize) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return splitToList(chunkSize).map(listToStreamMapper());
    //    }

    @Override
    public Stream<List<T>> split(final int chunkSize) throws IllegalStateException {
        assertNotClosed();

        return split(chunkSize, IntFunctions.ofList());
    }

    //    @Override
    //    public Stream<Set<T>> splitToSet(final int chunkSize) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return split(chunkSize, IntFunctions.<T> ofSet());
    //    }

    //    @Override
    //    public Stream<Stream<T>> split(final Predicate<? super T> predicate) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return splitToList(predicate).map(listToStreamMapper());
    //    }

    @Override
    public Stream<List<T>> split(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return split(predicate, Suppliers.ofList());
    }

    //    @Override
    //    public Stream<Set<T>> splitToSet(final Predicate<? super T> predicate) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return split(predicate, Suppliers.<T> ofSet());
    //    }

    @Override
    public Stream<Stream<T>> splitAt(final int position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        final IteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public Stream<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                Stream<T> result = null;

                if (cursor == 0) {
                    final List<T> list = new ArrayList<>();
                    int cnt = 0;

                    while (cnt++ < position && iter.hasNext()) {
                        list.add(iter.next());
                    }

                    result = newStream(StreamBase.toArray(list), 0, list.size(), isSorted(), comparator(), null);
                } else {
                    result = newStream(iter, isSorted(), comparator(), null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                iter.count();

                final long ret = 2 - cursor; //NOSONAR
                cursor = 2;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                } else if ((n == 1) && (cursor == 0)) {
                    iter.advance(position);
                } else {
                    iter.advance(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> splitAt(final int position, final Collector<? super T, ?, R> collector) throws IllegalStateException {
        return splitAt(position).map(s -> s.collect(collector));
    }

    @Override
    public Stream<Stream<T>> splitAt(final Predicate<? super T> where) throws IllegalStateException {
        assertNotClosed();

        final IteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = 0;
            private T next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public Stream<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                Stream<T> result = null;

                if (cursor == 0) {
                    final List<T> list = new ArrayList<>();

                    while (iter.hasNext()) {
                        next = iter.next();

                        if (!where.test(next)) {
                            list.add(next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }

                    result = newStream(StreamBase.toArray(list), 0, list.size(), isSorted(), comparator(), null);
                } else {
                    IteratorEx<T> iterEx = iter;

                    if (hasNext) {
                        iterEx = new ObjIteratorEx<>() {
                            private boolean isFirst = true;

                            @Override
                            public boolean hasNext() {
                                return isFirst || iter.hasNext();
                            }

                            @Override
                            public T next() {
                                if (!hasNext()) {
                                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                                }

                                if (isFirst) {
                                    isFirst = false;
                                    return next;
                                } else {
                                    return iter.next();
                                }
                            }
                        };
                    }

                    result = new IteratorStream<>(iterEx, isSorted(), comparator(), null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                iter.count();

                final long ret = 2 - cursor; //NOSONAR
                cursor = 2;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                } else if ((n == 1) && (cursor == 0)) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (where.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                } else {
                    iter.advance(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }

        }, false, null);
    }

    @Override
    public <R> Stream<R> splitAt(final Predicate<? super T> where, final Collector<? super T, ?, R> collector) throws IllegalStateException {
        return splitAt(where).map(s -> s.collect(collector));
    }

    //    @Override
    //    public Stream<Stream<T>> sliding(final int windowSize, final int increment) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return slidingToList(windowSize, increment).map(listToStreamMapper());
    //    }

    @Override
    public Stream<List<T>> sliding(final int windowSize) {
        return sliding(windowSize, IntFunctions.ofList());
    }

    @Override
    public Stream<List<T>> sliding(final int windowSize, final int increment) throws IllegalStateException {
        assertNotClosed();

        return sliding(windowSize, increment, IntFunctions.ofList());
    }

    //    @Override
    //    public Stream<Set<T>> slidingToSet(final int windowSize, final int increment) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return sliding(windowSize, increment, IntFunctions.<T> ofSet());
    //    }

    @Override
    public <C extends Collection<T>> Stream<C> sliding(final int windowSize, final IntFunction<? extends C> collectionSupplier) throws IllegalStateException {
        assertNotClosed();

        return sliding(windowSize, 1, collectionSupplier);
    }

    @Override
    public <R> Stream<R> sliding(final int windowSize, final Collector<? super T, ?, R> collector) throws IllegalStateException {
        assertNotClosed();

        return sliding(windowSize, 1, collector);
    }

    @Override
    public Stream<T> intersperse(final T delimiter) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private boolean toInsert = false;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() throws IllegalStateException, IllegalArgumentException {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (toInsert) {
                    toInsert = false;
                    return delimiter;
                } else {
                    final T res = iter.next();
                    toInsert = true;
                    return res;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> onFirst(final Consumer<? super T> action) throws IllegalStateException {
        assertNotClosed();

        final Function<? super T, ? extends T> mapperForFirst = t -> {
            action.accept(t);
            return t;
        };

        return mapFirst(mapperForFirst);
    }

    @Override
    public Stream<T> onLast(final Consumer<? super T> action) throws IllegalStateException {
        assertNotClosed();

        final Function<? super T, ? extends T> mapperForLast = t -> {
            action.accept(t);
            return t;
        };

        return mapLast(mapperForLast);
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.Consumer<? super T, E> action) throws IllegalStateException, E {
        assertNotClosed();

        forEach(action, Fn.emptyAction());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntObjConsumer<? super T, E> action) throws IllegalStateException, E {
        assertNotClosed();

        if (isParallel()) {
            final AtomicInteger idx = new AtomicInteger(0);

            forEach(t -> action.accept(idx.getAndIncrement(), t));
        } else {
            final MutableInt idx = MutableInt.of(0);

            forEach(t -> action.accept(idx.getAndIncrement(), t));
        }
    }

    @Override
    public <E extends Exception> void forEachUntil(final Throwables.BiConsumer<? super T, MutableBoolean, E> action) throws IllegalStateException, E {
        assertNotClosed();

        final MutableBoolean flagToBreak = MutableBoolean.of(false);

        final Throwables.Consumer<? super T, E> tmp = t -> action.accept(t, flagToBreak);

        if (isParallel()) {
            this.psp(s -> s.takeWhile(value -> flagToBreak.isFalse())).forEach(tmp);
        } else {
            //noinspection resource
            takeWhile(value -> flagToBreak.isFalse()).forEach(tmp);
        }
    }

    @Override
    public <E extends Exception> void forEachUntil(final MutableBoolean flagToBreak, final Throwables.Consumer<? super T, E> action)
            throws IllegalStateException, E {
        assertNotClosed();

        if (isParallel()) {
            this.psp(s -> s.takeWhile(value -> flagToBreak.isFalse())).forEach(action);
        } else {
            //noinspection resource
            takeWhile(value -> flagToBreak.isFalse()).forEach(action);
        }
    }

    @Override
    public <E extends Exception> void forEachPair(final Throwables.BiConsumer<? super T, ? super T, E> action) throws IllegalStateException, E {
        assertNotClosed();

        forEachPair(1, action);
    }

    @Override
    public <E extends Exception> void forEachTriple(final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action) throws IllegalStateException, E {
        assertNotClosed();

        forEachTriple(1, action);
    }

    //    @Override
    //    public Optional<T> reduceUntil(final BinaryOperator<T> accumulator, final Predicate<? super T> conditionToBreak) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final MutableBoolean flagToBreak = MutableBoolean.of(false);
    //
    //        final BinaryOperator<T> newAccumulator = (t, u) -> {
    //            final T ret = accumulator.apply(t, u);
    //
    //            if (flagToBreak.isFalse() && conditionToBreak.test(ret)) {
    //                flagToBreak.setValue(true);
    //            }
    //
    //            return ret;
    //        };
    //
    //        if (isParallel()) {
    //            //noinspection resource
    //            return psp(s -> s.takeWhile(value -> flagToBreak.isFalse())).reduce(newAccumulator);
    //        } else {
    //            //noinspection resource
    //            return takeWhile(value -> flagToBreak.isFalse()).reduce(newAccumulator);
    //        }
    //    }
    //
    //    @Override
    //    public Optional<T> reduceUntil(final BinaryOperator<T> accumulator, final BiPredicate<? super T, ? super T> conditionToBreak) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final MutableBoolean flagToBreak = MutableBoolean.of(false);
    //
    //        final BinaryOperator<T> newAccumulator = (t, u) -> {
    //            final T ret = accumulator.apply(t, u);
    //
    //            if (flagToBreak.isFalse() && conditionToBreak.test(ret, t)) {
    //                flagToBreak.setValue(true);
    //            }
    //
    //            return ret;
    //        };
    //
    //        if (isParallel()) {
    //            //noinspection resource
    //            return psp(s -> s.takeWhile(value -> flagToBreak.isFalse())).reduce(newAccumulator);
    //        } else {
    //            //noinspection resource
    //            return takeWhile(value -> flagToBreak.isFalse()).reduce(newAccumulator);
    //        }
    //    }
    //
    //    @Override
    //    public <U> U reduceUntil(final U identity, final BiFunction<? super U, ? super T, U> accumulator, final BinaryOperator<U> combiner,
    //            final Predicate<? super U> conditionToBreak) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final MutableBoolean flagToBreak = MutableBoolean.of(false);
    //
    //        final BiFunction<U, T, U> newAccumulator = (u, t) -> {
    //            final U ret = accumulator.apply(u, t);
    //
    //            if (flagToBreak.isFalse() && conditionToBreak.test(ret)) {
    //                flagToBreak.setValue(true);
    //            }
    //
    //            return ret;
    //        };
    //
    //        if (isParallel()) {
    //            //noinspection resource
    //            return psp(s -> s.takeWhile(value -> flagToBreak.isFalse())).reduce(identity, newAccumulator, combiner);
    //        } else {
    //            //noinspection resource
    //            return takeWhile(value -> flagToBreak.isFalse()).reduce(identity, newAccumulator, combiner);
    //        }
    //    }
    //
    //    @Override
    //    public <U> U reduceUntil(final U identity, final BiFunction<? super U, ? super T, U> accumulator, final BinaryOperator<U> combiner,
    //            final BiPredicate<? super U, ? super T> conditionToBreak) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final MutableBoolean flagToBreak = MutableBoolean.of(false);
    //
    //        final BiFunction<U, T, U> newAccumulator = (u, t) -> {
    //            final U ret = accumulator.apply(u, t);
    //
    //            if (flagToBreak.isFalse() && conditionToBreak.test(ret, t)) {
    //                flagToBreak.setValue(true);
    //            }
    //
    //            return ret;
    //        };
    //
    //        if (isParallel()) {
    //            //noinspection resource
    //            return psp(s -> s.takeWhile(value -> flagToBreak.isFalse())).reduce(identity, newAccumulator, combiner);
    //        } else {
    //            //noinspection resource
    //            return takeWhile(value -> flagToBreak.isFalse()).reduce(identity, newAccumulator, combiner);
    //        }
    //    }

    @Override
    public <K> Stream<Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> keyMapper) throws IllegalStateException {
        assertNotClosed();

        return groupBy(keyMapper, Suppliers.ofMap());
    }

    @Override
    public <K> Stream<Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> keyMapper, final Supplier<? extends Map<K, List<T>>> mapFactory)
            throws IllegalStateException {
        assertNotClosed();

        return groupBy(keyMapper, Fn.identity(), mapFactory);
    }

    @Override
    public <K, V> Stream<Entry<K, List<V>>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper)
            throws IllegalStateException {
        assertNotClosed();

        return groupBy(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V> Stream<Map.Entry<K, List<V>>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Supplier<? extends Map<K, List<V>>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        return groupBy(keyMapper, valueMapper, Collectors.toList(), mapFactory);
    }

    @Override
    public <K, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException {
        assertNotClosed();

        return groupBy(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, ?, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        return groupBy(keyMapper, Fn.identity(), downstream, mapFactory);
    }

    @Override
    public <K, V, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, ?, D> downstream) throws IllegalStateException {
        assertNotClosed();

        return groupBy(keyMapper, valueMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, V, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, ?, D> downstream, final Supplier<? extends Map<K, D>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean initialized = false;
            private Iterator<Entry<K, D>> iter = null;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public Entry<K, D> next() {
                if (!initialized) {
                    init();
                }

                return iter.next();
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    iter = AbstractStream.this.groupTo(Fn.from(keyMapper), Fn.from(valueMapper), downstream, mapFactory).entrySet().iterator();
                }
            }
        }, false, null);
    }

    @Override
    public <K, V> Stream<Entry<K, V>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction) throws IllegalStateException {
        assertNotClosed();

        return groupBy(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, V> Stream<Entry<K, V>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<Entry<K, V>> iter = null;

            @Override
            public boolean hasNext() {
                init();
                return iter.hasNext();
            }

            @Override
            public Entry<K, V> next() {
                init();
                return iter.next();
            }

            private void init() {
                if (iter == null) {
                    iter = AbstractStream.this.toMap(Fn.from(keyMapper), Fn.from(valueMapper), mergeFunction, mapFactory).entrySet().iterator();
                }
            }
        }, false, null);
    }

    @Override
    public Stream<Entry<Boolean, List<T>>> partitionBy(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return partitionBy(predicate, Collectors.toList());
    }

    @Override
    public <D> Stream<Entry<Boolean, D>> partitionBy(final Predicate<? super T> predicate, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<Entry<Boolean, D>> iter = null;

            @Override
            public boolean hasNext() {
                init();
                return iter.hasNext();
            }

            @Override
            public Entry<Boolean, D> next() {
                init();
                return iter.next();
            }

            private void init() {
                if (iter == null) {
                    iter = AbstractStream.this.partitionTo(Fn.from(predicate), downstream).entrySet().iterator();
                }
            }
        }, false, null);
    }

    @Override
    public EntryStream<Boolean, List<T>> partitionByToEntry(final Predicate<? super T> predicate) throws IllegalStateException {
        assertNotClosed();

        return partitionByToEntry(predicate, Collectors.toList());
    }

    @Override
    public <D> EntryStream<Boolean, D> partitionByToEntry(final Predicate<? super T> predicate, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException {
        assertNotClosed();

        //noinspection resource
        return partitionBy(predicate, downstream).mapToEntry(Fn.identity());
    }

    @Override
    public <K> EntryStream<K, List<T>> groupByToEntry(final Function<? super T, ? extends K> keyMapper) throws IllegalStateException {
        assertNotClosed();

        return groupByToEntry(keyMapper, Suppliers.ofMap());
    }

    @Override
    public <K> EntryStream<K, List<T>> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Supplier<? extends Map<K, List<T>>> mapFactory)
            throws IllegalStateException {
        assertNotClosed();

        return groupByToEntry(keyMapper, Fn.identity(), mapFactory);
    }

    @Override
    public <K, V> EntryStream<K, List<V>> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper)
            throws IllegalStateException {
        assertNotClosed();

        return groupByToEntry(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V> EntryStream<K, List<V>> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Supplier<? extends Map<K, List<V>>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        //noinspection resource
        return groupBy(keyMapper, valueMapper, mapFactory).mapToEntry(Fn.identity());
    }

    @Override
    public <K, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException {
        assertNotClosed();

        return groupByToEntry(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, ?, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        //noinspection resource
        return groupBy(keyMapper, downstream, mapFactory).mapToEntry(Fn.identity());
    }

    @Override
    public <K, V, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, ?, D> downstream) throws IllegalStateException {
        assertNotClosed();

        return groupByToEntry(keyMapper, valueMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, V, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, ?, D> downstream, final Supplier<? extends Map<K, D>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        //noinspection resource
        return groupBy(keyMapper, valueMapper, downstream, mapFactory).mapToEntry(Fn.identity());
    }

    @Override
    public <K, V> EntryStream<K, V> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction) throws IllegalStateException {
        assertNotClosed();

        return groupByToEntry(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, V> EntryStream<K, V> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        //noinspection resource
        return groupBy(keyMapper, valueMapper, mergeFunction, mapFactory).mapToEntry(Fn.identity());
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, E extends Exception> Map<K, List<T>> groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper) throws IllegalStateException, E {
        assertNotClosed();

        return groupTo(keyMapper, Suppliers.ofMap());
    }

    @Override
    public <K, M extends Map<K, List<T>>, E extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, E {
        assertNotClosed();

        return groupTo(keyMapper, Collectors.toList(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, List<V>> groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return groupTo(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, List<V>>, E extends Exception, E2 extends Exception> M groupTo(
            final Throwables.Function<? super T, ? extends K, E> keyMapper, final Throwables.Function<? super T, ? extends V, E2> valueMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return groupTo(keyMapper, valueMapper, Collectors.toList(), mapFactory);
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Collector<? super T, ?, D> downstream) throws E {
        assertNotClosed();

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Collector<? super T, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, E {
        assertNotClosed();

        return groupTo(keyMapper, Fn.identity(), downstream, mapFactory);
    }

    @Override
    public <K, V, D, E extends Exception, E2 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream)
            throws IllegalStateException, E, E2 {
        assertNotClosed();

        return groupTo(keyMapper, valueMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, V, D, M extends Map<K, D>, E extends Exception, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws E, E2 {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final ObjIteratorEx<T> iter = iteratorEx();
            final M result = mapFactory.get();

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super V> downstreamAccumulator = (BiConsumer<Object, ? super V>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final Map<K, Object> intermediate = (Map<K, Object>) result;
            K key = null;
            Object v = null;
            T next = null;

            while (iter.hasNext()) {
                next = iter.next();
                key = checkArgNotNull(keyMapper.apply(next), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    v = downstreamSupplier.get();
                    intermediate.put(key, v);
                }

                downstreamAccumulator.accept(v, valueMapper.apply(next));
            }

            final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

            Collectors.replaceAll(intermediate, function);

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <K, E extends Exception> Map<K, List<T>> flatGroupTo(final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor)
            throws E {
        return flatGroupTo(flatKeyExtractor, Suppliers.ofMap());
    }

    @Override
    public <K, M extends Map<K, List<T>>, E extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor, final Supplier<? extends M> mapFactory) throws E {
        return flatGroupTo(flatKeyExtractor, BiFunctions.selectSecond(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, List<V>> flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper) throws E, E2 {
        return flatGroupTo(flatKeyExtractor, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, List<V>>, E extends Exception, E2 extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        return flatGroupTo(flatKeyExtractor, valueMapper, Collectors.toList(), mapFactory);
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> flatGroupTo(final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Collector<? super T, ?, D> downstream) throws E {
        return flatGroupTo(flatKeyExtractor, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor, final Collector<? super T, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws E {
        return flatGroupTo(flatKeyExtractor, BiFunctions.selectSecond(), downstream, mapFactory);
    }

    @Override
    public <K, V, D, E extends Exception, E2 extends Exception> Map<K, D> flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream) throws E, E2 {
        return flatGroupTo(flatKeyExtractor, valueMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, V, D, M extends Map<K, D>, E extends Exception, E2 extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws E, E2 {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final ObjIteratorEx<T> iter = iteratorEx();
            final M result = mapFactory.get();

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super V> downstreamAccumulator = (BiConsumer<Object, ? super V>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final Map<K, Object> intermediate = (Map<K, Object>) result;

            Collection<? extends K> ks = null;
            Object v = null;

            T next = null;

            while (iter.hasNext()) {
                next = iter.next();
                ks = flatKeyExtractor.apply(next);

                if (N.notEmpty(ks)) {
                    for (final K k : ks) {
                        checkArgNotNull(k, "element cannot be mapped to a null key");

                        if ((v = intermediate.get(k)) == null) {
                            v = downstreamSupplier.get();
                            intermediate.put(k, v);
                        }

                        downstreamAccumulator.accept(v, valueMapper.apply(k, next));
                    }
                }
            }

            final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

            Collectors.replaceAll(intermediate, function);

            return result;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> Map<Boolean, List<T>> partitionTo(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException, E {
        assertNotClosed();

        return partitionTo(predicate, Collectors.toList());
    }

    @Override
    public <D, E extends Exception> Map<Boolean, D> partitionTo(final Throwables.Predicate<? super T, E> predicate, final Collector<? super T, ?, D> downstream)
            throws E {
        assertNotClosed();

        final Throwables.Function<T, Boolean, E> keyMapper = predicate::test;

        final Supplier<Map<Boolean, D>> mapFactory = () -> N.newHashMap(2);

        final Map<Boolean, D> map = groupTo(keyMapper, downstream, mapFactory);

        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

        if (!map.containsKey(Boolean.TRUE)) {
            map.put(Boolean.TRUE, downstreamFinisher.apply(downstreamSupplier.get()));
        }

        if (!map.containsKey(Boolean.FALSE)) {
            map.put(Boolean.FALSE, downstreamFinisher.apply(downstreamSupplier.get()));
        }

        return map;
    }

    @Override
    public <K, E extends Exception> ListMultimap<K, T> toMultimap(final Throwables.Function<? super T, ? extends K, E> keyMapper)
            throws IllegalStateException, E {
        assertNotClosed();

        return toMultimap(keyMapper, Suppliers.ofListMultimap());
    }

    @Override
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>, E extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E> keyMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E {
        assertNotClosed();

        return toMultimap(keyMapper, Fn.identity(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> ListMultimap<K, V> toMultimap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return toMultimap(keyMapper, valueMapper, Suppliers.ofListMultimap());
    }

    @Override
    public long sumInt(final ToIntFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return collect(Collectors.summingIntToLong(mapper));
    }

    @Override
    public long sumLong(final ToLongFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return collect(Collectors.summingLong(mapper));
    }

    @Override
    public double sumDouble(final ToDoubleFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return collect(Collectors.summingDouble(mapper));
    }

    @Override
    public OptionalDouble averageInt(final ToIntFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return collect(Collectors.averagingIntOrEmpty(mapper));
    }

    @Override
    public OptionalDouble averageLong(final ToLongFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return collect(Collectors.averagingLongOrEmpty(mapper));
    }

    @Override
    public OptionalDouble averageDouble(final ToDoubleFunction<? super T> mapper) throws IllegalStateException {
        assertNotClosed();

        return collect(Collectors.averagingDoubleOrEmpty(mapper));
    }

    @Override
    public List<T> minAll(final Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return collect(Collectors.minAll(comparator));
        } else {
            try {
                @SuppressWarnings("resource")
                final ObjIteratorEx<T> iter = iteratorEx();
                final List<T> result = new ArrayList<>();

                if (!iter.hasNext()) {
                    return result;
                }

                T candidate = iter.next();
                result.add(candidate);

                if (isSorted() && isSameComparator(comparator(), comparator)) {
                    final Comparator<? super T> cmp = comparator() == null ? NULL_MAX_COMPARATOR : comparator();
                    T next = null;

                    while (iter.hasNext()) {
                        next = iter.next();

                        if (cmp.compare(next, candidate) == 0) {
                            result.add(next);
                        } else {
                            break;
                        }
                    }
                } else {
                    final Comparator<? super T> cmp = comparator == null ? NULL_MAX_COMPARATOR : comparator;
                    T next = null;
                    int cp = 0;

                    while (iter.hasNext()) {
                        next = iter.next();
                        cp = cmp.compare(next, candidate);

                        if (cp == 0) {
                            result.add(next);
                        } else if (cp < 0) {
                            result.clear();
                            result.add(next);
                            candidate = next;
                        }
                    }
                }

                return result;
            } finally {
                close();
            }
        }
    }

    @Override
    public List<T> maxAll(final Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return collect(Collectors.maxAll(comparator));
        } else {
            try {
                @SuppressWarnings("resource")
                final ObjIteratorEx<T> iter = iteratorEx();
                final List<T> result = new ArrayList<>();

                if (!iter.hasNext()) {
                    return result;
                }

                T candidate = iter.next();
                result.add(candidate);

                final Comparator<? super T> cmp = comparator == null ? NULL_MIN_COMPARATOR : comparator;

                T next = null;
                int cp = 0;

                while (iter.hasNext()) {
                    next = iter.next();
                    cp = cmp.compare(next, candidate);

                    if (cp == 0) {
                        result.add(next);
                    } else if (cp > 0) {
                        result.clear();
                        result.add(next);
                        candidate = next;
                    }
                }

                return result;
            } finally {
                close();
            }
        }
    }

    @Override
    public <E extends Exception> Optional<T> findAny(final Throwables.Predicate<? super T, E> predicate) throws E {
        return findFirst(predicate);
    }

    @SafeVarargs
    @Override
    public final boolean containsAll(final T... a) throws IllegalStateException {
        assertNotClosed();

        try {
            if (N.isEmpty(a)) {
                return true;
            } else if (a.length == 1 || (a.length == 2 && N.equals(a[0], a[1]))) {
                return anyMatch(Fn.equal(a[0]));
            } else if (a.length == 2) {
                //noinspection resource
                return filter(new Predicate<>() {
                    private final T val1 = a[0];
                    private final T val2 = a[1];

                    @Override
                    public boolean test(final T t) {
                        return N.equals(t, val1) || N.equals(t, val2);
                    }
                }).distinct().limit(2).count() == 2;
            } else {
                return containsAll(N.toSet(a));
            }
        } finally {
            close();
        }
    }

    @Override
    public boolean containsAll(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        try {
            if (N.isEmpty(c)) {
                return true;
            } else if (c.size() == 1) {
                final T val = c instanceof List ? ((List<T>) c).get(0) : c.iterator().next();
                return anyMatch(Fn.equal(val));
            } else {
                final Set<T> set = c instanceof Set ? (Set<T>) c : N.newHashSet(c);
                final int distinctCount = set.size();

                //noinspection resource
                return filter(set::contains).distinct().limit(distinctCount).count() == distinctCount;
            }
        } finally {
            close();
        }
    }

    @SafeVarargs
    @Override
    public final boolean containsAny(final T... a) throws IllegalStateException {
        assertNotClosed();

        try {
            if (N.isEmpty(a)) {
                return false;
            } else if (a.length == 1 || (a.length == 2 && N.equals(a[0], a[1]))) {
                return anyMatch(Fn.equal(a[0]));
            } else if (a.length == 2) {
                return anyMatch(new com.landawn.abacus.util.function.Predicate<>() {
                    private final T val1 = a[0];
                    private final T val2 = a[1];

                    @Override
                    public boolean test(final T t) {
                        return N.equals(t, val1) || N.equals(t, val2);
                    }
                });
            } else {
                final Set<T> set = N.toSet(a);

                return anyMatch(set::contains);
            }
        } finally {
            close();
        }
    }

    @Override
    public boolean containsAny(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        try {
            if (N.isEmpty(c)) {
                return false;
            } else if (c.size() == 1) {
                final T val = c instanceof List ? ((List<T>) c).get(0) : c.iterator().next();
                return anyMatch(Fn.equal(val));
            } else {
                final Set<T> set = c instanceof Set ? (Set<T>) c : N.newHashSet(c);

                return anyMatch(set::contains);
            }
        } finally {
            close();
        }
    }

    @SafeVarargs
    @Override
    public final boolean containsNone(final T... a) throws IllegalStateException {
        assertNotClosed();

        try {
            if (N.isEmpty(a)) {
                return true;
            }

            return !containsAny(a);
        } finally {
            close();
        }
    }

    @Override
    public boolean containsNone(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        try {
            if (N.isEmpty(c)) {
                return true;
            }

            return !containsAny(c);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> first() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();

            if (!iter.hasNext()) {
                return Optional.empty();
            }

            return Optional.of(iter.next());
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> last() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();

            if (!iter.hasNext()) {
                return Optional.empty();
            }

            T next = iter.next();

            while (iter.hasNext()) {
                next = iter.next();
            }

            return Optional.of(next);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> elementAt(final long position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        try {
            if (position == 0) {
                return first();
            } else {
                //noinspection resource
                return skip(position).first();
            }
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public Optional<T> onlyOne() throws IllegalStateException, TooManyElementsException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();

            final Optional<T> result = iter.hasNext() ? Optional.of(iter.next()) : Optional.empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.next()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> rateLimited(final RateLimiter rateLimiter) throws IllegalArgumentException {
        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final Consumer<T> action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public Stream<T> delay(final Duration delay) throws IllegalArgumentException {
        checkArgNotNull(delay, cs.delay);

        final long millis = delay.toMillis();

        final Consumer<T> action = new Consumer<>() {
            private boolean isFirst = true;

            @Override
            public void accept(final T it) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    N.sleepUninterruptibly(millis);
                }
            }
        };

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public Stream<T> debounce(Duration duration) {
        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            // Debounce is inherently sequential and time-ordered; run it on the sequential view.
            return psp(s -> s.debounce(duration));
        } else {
            return newStream(new ObjIteratorEx<>() {
                private final ObjIteratorEx<T> iter = iteratorEx();
                private final long durationMillis = duration.toMillis();
                private T prev; // the most recent element of the current burst, awaiting a quiet gap
                private boolean hasPrev = false;
                private long prevTime = 0;
                private T next;
                private boolean hasNext = false;

                @Override
                public boolean hasNext() {
                    if (hasNext) {
                        return true;
                    }

                    while (iter.hasNext()) {
                        final T val = iter.next();
                        final long now = System.currentTimeMillis();

                        if (!hasPrev) {
                            prev = val;
                            prevTime = now;
                            hasPrev = true;
                        } else if (now - prevTime >= durationMillis) {
                            // prev was followed by a quiet gap >= duration -> emit it; val starts the next burst.
                            next = prev;
                            hasNext = true;
                            prev = val;
                            prevTime = now;
                            return true;
                        } else {
                            // val arrived within the quiet window -> it supersedes prev.
                            prev = val;
                            prevTime = now;
                        }
                    }

                    // Source exhausted: the most recent pending element is always emitted.
                    if (hasPrev) {
                        next = prev;
                        hasNext = true;
                        hasPrev = false;
                        prev = null;
                        return true;
                    }

                    return false;
                }

                @Override
                public T next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    hasNext = false;
                    final T res = next;
                    next = null;
                    return res;
                }
            }, isSorted(), comparator());
        }
    }

    @Override
    public Stream<T> skipNulls() {
        if (isParallel()) {
            //noinspection resource
            return sequential().filter(Fn.notNull()).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return filter(Fn.notNull());
        }
    }

    @Override
    public Stream<T> skipRange(final int startInclusive, final int endExclusive) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(startInclusive, cs.startInclusive);
        checkArgNotNegative(endExclusive, cs.endExclusive);
        checkArgNotNegative(endExclusive - startInclusive, "endExclusive - startInclusive");

        if (startInclusive == endExclusive) {
            return skip(0);
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private final ObjIteratorEx<T> iter = iteratorEx();
            private final MutableLong idx = MutableLong.of(0);
            private boolean skipped = false;

            @Override
            public boolean hasNext() {
                if (!skipped && idx.value() >= startInclusive) {
                    skipped = true;

                    while (iter.hasNext() && idx.value() < endExclusive) {
                        iter.next();
                        idx.increment();
                    }
                }

                return iter.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                idx.increment();

                return iter.next();
            }
        }, isSorted(), comparator());
    }

    @Override
    public Stream<T> skip(final long n, final Consumer<? super T> action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(action, cs.action);

        if (n == 0) {
            return this;
        }

        final Predicate<T> filter = isParallel() ? new Predicate<>() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(final T value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new Predicate<>() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(final T value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public Stream<T> intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(multiset::remove).iteratorEx(), isSorted(), comparator());
    }

    @Override
    public <U> Stream<T> intersection(final Function<? super T, ? extends U> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.isEmpty() && multiset.remove(mapper.apply(value))).iteratorEx(), isSorted(), comparator());
    }

    @Override
    public Stream<T> difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).iteratorEx(), isSorted(), comparator());
    }

    @Override
    public <U> Stream<T> difference(final Function<? super T, ? extends U> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> multiset.isEmpty() || !multiset.remove(mapper.apply(value))).iteratorEx(), isSorted(), comparator());
    }

    @Override
    public Stream<T> symmetricDifference(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return newStream(sequential().filter(value -> !multiset.remove(value)).append(Stream.<T> of(c).filter(multiset::remove)).iteratorEx(), false, null);
    }

    @Override
    public Stream<T> reversed() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean initialized = false;

            private T[] elements;
            private int fromIndex = -1;
            private int toIndex = -1;

            private int cursor;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor > fromIndex;
            }

            @Override
            public T next() {
                if (!initialized) {
                    init();
                }

                if (cursor <= fromIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return elements[--cursor];
            }

            @Override
            public long count() {
                if (!initialized) {
                    init();
                }

                final long ret = cursor - fromIndex;
                cursor = fromIndex;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                cursor = n < cursor - fromIndex ? cursor - (int) n : fromIndex;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                if (!initialized) {
                    init();
                }

                final int len = cursor - fromIndex;

                a = a.length >= len ? a : (A[]) N.newArray(a.getClass().getComponentType(), len);

                for (int i = 0; i < len; i++) {
                    a[i] = (A) elements[cursor - i - 1];
                }

                if (a.length > len) {
                    a[len] = null;
                }

                cursor = fromIndex; // Move cursor to the end after copying.

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<Object[], Integer, Integer> tp = AbstractStream.this.arrayForIntermediateOp();

                    elements = (T[]) tp._1;
                    fromIndex = tp._2;
                    toIndex = tp._3;

                    cursor = toIndex;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean initialized = false;

            private T[] elements;
            private int fromIndex = -1;
            private int toIndex = -1;

            private int len;
            private int start;
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cnt < len;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return elements[((start + cnt++) % len) + fromIndex];
            }

            @Override
            public long count() {
                if (!initialized) {
                    init();
                }

                final long ret = len - cnt;
                cnt = len;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                if (!initialized) {
                    init();
                }

                final int remaining = len - cnt;

                a = a.length >= remaining ? a : (A[]) N.newArray(a.getClass().getComponentType(), remaining);

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = (A) elements[((start + i) % len) + fromIndex];
                }

                if (a.length > remaining) {
                    a[remaining] = null;
                }

                cnt = len; // Move cursor to the end after copying.

                return a;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<Object[], Integer, Integer> tp = AbstractStream.this.arrayForIntermediateOp();

                    elements = (T[]) tp._1;
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
        }, distance == 0 && isSorted(), distance == 0 ? comparator() : null);
    }

    @Override
    public Stream<T> shuffled(final Random rnd) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rnd, cs.rnd);

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false, null);
    }

    @Override
    public Stream<T> sorted() {
        return sorted(NATURAL_COMPARATOR);
    }

    @Override
    public Stream<T> sorted(final Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        final Comparator<? super T> cmpToUse = comparator == null ? NATURAL_COMPARATOR : comparator;

        if (isSorted() && cmpToUse == comparator()) {
            return this;
        }

        return lazyLoad(a -> {
            if (isParallel()) {
                N.parallelSort((T[]) a, cmpToUse);
            } else {
                N.sort((T[]) a, cmpToUse);
            }

            return a;
        }, true, cmpToUse);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Stream<T> sortedBy(final Function<? super T, ? extends Comparable> keyMapper) {
        final Comparator<? super T> comparator = Comparators.comparingBy(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByInt(final ToIntFunction<? super T> keyMapper) {
        final Comparator<? super T> comparator = Comparators.comparingInt(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByLong(final ToLongFunction<? super T> keyMapper) {
        final Comparator<? super T> comparator = Comparators.comparingLong(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByDouble(final ToDoubleFunction<? super T> keyMapper) {
        final Comparator<? super T> comparator = Comparators.comparingDouble(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> reverseSorted() {
        return sorted(REVERSED_COMPARATOR);
    }

    @Override
    public Stream<T> reverseSorted(final Comparator<? super T> comparator) {
        final Comparator<? super T> cmpToUse = Comparators.reverseOrder(comparator);

        return sorted(cmpToUse);
    }

    @Override
    public Stream<T> reverseSortedByInt(final ToIntFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingInt(keyMapper);

        return sorted(cmpToUse);
    }

    @Override
    public Stream<T> reverseSortedByLong(final ToLongFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingLong(keyMapper);

        return sorted(cmpToUse);
    }

    @Override
    public Stream<T> reverseSortedByDouble(final ToDoubleFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingDouble(keyMapper);

        return sorted(cmpToUse);
    }

    @Override
    public Stream<T> reverseSortedBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingBy(keyMapper);

        return sorted(cmpToUse);
    }

    private Stream<T> lazyLoad(final UnaryOperator<Object[]> op, final boolean sorted, final Comparator<? super T> cmp) {
        return Stream.defer(() -> newStream((T[]) op.apply(toArrayForIntermediateOp()), sorted, cmp)).onClose(this::close);
    }

    @Override
    public Stream<T> distinctBy(final Function<? super T, ?> keyMapper) throws IllegalStateException {
        assertNotClosed();

        final Predicate<T> predicate = new Predicate<>() {
            private final Set<Object> set = isParallel() ? ConcurrentHashMap.newKeySet() : N.newHashSet();

            @Override
            public boolean test(final T value) {
                return set.add(hashKey(keyMapper.apply(value)));
            }
        };

        return filter(predicate);
    }

    @Override
    public Stream<T> top(final int n) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return top(n, NATURAL_COMPARATOR);
    }

    @Override
    public Optional<Map<Percentage, T>> percentiles() throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Object[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Optional.empty();
            }

            return Optional.of((Map<Percentage, T>) N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Optional<Map<Percentage, T>> percentiles(final Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Object[] a = sorted(comparator).toArray();

            if (N.isEmpty(a)) {
                return Optional.empty();
            }

            return Optional.of((Map<Percentage, T>) N.percentilesOfSorted(a));
        } finally {
            close();
        }
    }

    @Override
    public Stream<List<T>> combinations() throws IllegalStateException {
        assertNotClosed();

        if (this instanceof ArrayStream<T> s) {
            final int count = s.toIndex - s.fromIndex;

            //noinspection resource
            return newStream(IntStream.rangeClosed(0, count).flatMapToObj(len -> Stream.of(s.elements, s.fromIndex, s.toIndex).combinations(len)).iteratorEx(),
                    false, null);
        } else {
            //noinspection resource
            return newStream((T[]) toArray(), false, null).combinations(); //NOSONAR
        }
    }

    @Override
    public Stream<List<T>> combinations(final int len) throws IndexOutOfBoundsException, IllegalStateException {
        assertNotClosed();

        if (this instanceof ArrayStream<T> s) {
            final int count = s.toIndex - s.fromIndex;
            checkFromIndexSize(0, len, count);

            if (len == 0) {
                return newStream(N.asArray(N.emptyList()), false, null);
            } else if (len == 1) {
                return map(N::toList);
            } else if (len == count) {
                return newStream(N.asArray(toList()), false, null);
            } else {
                final T[] a = s.elements;
                final int fromIndex = s.fromIndex;
                final int toIndex = s.toIndex;

                return newStream(new ObjIteratorEx<>() { //NOSONAR
                    private final int[] indices = Array.range(fromIndex, fromIndex + len);

                    @Override
                    public boolean hasNext() {
                        return indices[0] <= toIndex - len;
                    }

                    @Override
                    public List<T> next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final List<T> result = new ArrayList<>(len);

                        for (final int idx : indices) {
                            result.add(a[idx]);
                        }

                        if (++indices[len - 1] == toIndex) {
                            for (int i = len - 1; i > 0; i--) {
                                if (indices[i] > toIndex - (len - i)) {
                                    indices[i - 1]++;

                                    for (int j = i; j < len; j++) {
                                        indices[j] = indices[j - 1] + 1;
                                    }
                                }
                            }
                        }

                        return result;
                    }

                }, false, null);
            }
        } else {
            //noinspection resource
            return newStream((T[]) toArray(), false, null).combinations(len); //NOSONAR
        }
    }

    @Override
    public Stream<List<T>> combinations(final int len, final boolean repeat) throws IllegalStateException {
        assertNotClosed();

        if (!repeat) {
            return combinations(len);
        } else {
            return newStream(new ObjIteratorEx<>() { //NOSONAR
                private boolean initialized = false;
                private List<List<T>> list = null;
                private int size = 0;
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    if (!initialized) {
                        init();
                    }

                    return cursor < size;
                }

                @Override
                public List<T> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return list.get(cursor++);
                }

                @Override
                public void advance(final long n) {
                    if (n <= 0) {
                        return;
                    }

                    if (!initialized) {
                        init();
                    }

                    cursor = n <= size - cursor ? cursor + (int) n : size;
                }

                @Override
                public long count() {
                    if (!initialized) {
                        init();
                    }

                    final long ret = size - cursor;
                    cursor = size;
                    return ret;
                }

                private void init() {
                    if (!initialized) {
                        initialized = true;
                        list = Iterables.cartesianProduct(N.repeat(AbstractStream.this.toList(), len));
                        size = list.size();
                    }
                }
            }, false, null);
        }
    }

    @Override
    public Stream<List<T>> permutations() throws IllegalStateException {
        assertNotClosed();

        return newStream(PermutationIterator.of(toList()), false, null);
    }

    @Override
    public Stream<List<T>> orderedPermutations() throws IllegalStateException {
        assertNotClosed();

        return orderedPermutations(NATURAL_COMPARATOR);
    }

    @Override
    public Stream<List<T>> orderedPermutations(final Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        final Iterator<List<T>> iter = PermutationIterator.ordered(toList(), comparator == null ? NATURAL_COMPARATOR : comparator);

        return newStream(iter, false, null);
    }

    @Override
    public Stream<List<T>> cartesianProduct(final Collection<? extends Collection<? extends T>> cs) throws IllegalStateException {
        assertNotClosed();

        final List<Collection<? extends T>> cList = new ArrayList<>(cs.size() + 1);
        cList.add(toList());
        cList.addAll(cs);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean initialized = false;
            private List<List<T>> list = null;
            private int size = 0;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor < size;
            }

            @Override
            public List<T> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return list.get(cursor++);
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                cursor = n <= size - cursor ? cursor + (int) n : size;
            }

            @Override
            public long count() {
                if (!initialized) {
                    init();
                }

                final long ret = size - cursor;
                cursor = size;
                return ret;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    list = Iterables.cartesianProduct(cList);
                    size = list.size();
                }
            }

        }, false, null);
    }

    @Override
    public <A> A[] toArray(final IntFunction<A[]> generator) throws IllegalStateException {
        assertNotClosed();

        try {
            final Object[] src = toArray();
            final A[] dest = generator.apply(src.length);
            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        } finally {
            close();
        }
    }

    @Override
    public Dataset toDataset() throws IllegalStateException {
        assertNotClosed();

        return N.newDataset(toList());
    }

    @Override
    public Dataset toDataset(final List<String> columnNames) throws IllegalStateException {
        assertNotClosed();

        return N.newDataset(columnNames, toList());
    }

    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) throws IllegalStateException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseBuffer();
            @SuppressWarnings("resource")
            final IteratorEx<T> iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.next());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @Override
    public Joiner joinTo(final Joiner joiner) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(joiner, cs.joiner);

        try {
            @SuppressWarnings("resource")
            final IteratorEx<T> iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.next());
            }

            return joiner;
        } finally {
            close();
        }
    }

    @Override
    public boolean containsDuplicates() throws IllegalStateException {
        assertNotClosed();

        try {
            final Set<T> set = N.newHashSet();
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();

            while (iter.hasNext()) {
                if (!set.add(iter.next())) {
                    return true;
                }
            }

            return false;
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super T> accumulator) throws IllegalStateException {
        assertNotClosed();

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, RR, E extends Exception> RR collectThenApply(final Collector<? super T, ?, R> downstream,
            final Throwables.Function<? super R, ? extends RR, E> func) throws E {
        assertNotClosed();
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(func, cs.func);

        return func.apply(collect(downstream));
    }

    @Override
    public <R, E extends Exception> void collectThenAccept(final Collector<? super T, ?, R> downstream, final Throwables.Consumer<? super R, E> consumer)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(collect(downstream));
    }

    @Override
    public <R, E extends Exception> R toListThenApply(final Throwables.Function<? super List<T>, ? extends R, E> func) throws IllegalStateException, E {
        assertNotClosed();

        return func.apply(toList());
    }

    @Override
    public <E extends Exception> void toListThenAccept(final Throwables.Consumer<? super List<T>, E> consumer) throws IllegalStateException, E {
        assertNotClosed();

        consumer.accept(toList());
    }

    @Override
    public <R, E extends Exception> R toSetThenApply(final Throwables.Function<? super Set<T>, ? extends R, E> func) throws IllegalStateException, E {
        assertNotClosed();

        return func.apply(toSet());
    }

    @Override
    public <E extends Exception> void toSetThenAccept(final Throwables.Consumer<? super Set<T>, E> consumer) throws IllegalStateException, E {
        assertNotClosed();

        consumer.accept(toSet());
    }

    @Override
    public <R, C extends Collection<T>, E extends Exception> R toCollectionThenApply(final Supplier<? extends C> supplier,
            final Throwables.Function<? super C, ? extends R, E> func) throws IllegalStateException, E {
        assertNotClosed();

        return func.apply(toCollection(supplier));
    }

    @Override
    public <C extends Collection<T>, E extends Exception> void toCollectionThenAccept(final Supplier<? extends C> supplier,
            final Throwables.Consumer<? super C, E> consumer) throws IllegalStateException, E {
        assertNotClosed();

        consumer.accept(toCollection(supplier));
    }

    @Override
    public Stream<Indexed<T>> indexed() throws IllegalStateException {
        assertNotClosed();

        final MutableLong idx = MutableLong.of(0);

        //noinspection resource
        return newStream(sequential().map(t -> Indexed.of(t, idx.getAndIncrement())).iteratorEx(), true, INDEXED_COMPARATOR);
    }

    @Override
    public Stream<T> cycled() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<T> iter = null;
            private List<T> list = null;
            private Object[] a = null;
            private int len = 0;
            private int cursor = -1;
            private T e = null;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                if (a != null) {
                    return len > 0;
                } else if (iter.hasNext()) {
                    return true;
                } else {
                    a = list.toArray();
                    len = a.length;
                    cursor = 0;

                    return len > 0;
                }
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return (T) a[cursor++];
                } else {
                    e = iter.next();
                    list.add(e);

                    return e;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractStream.this.iteratorEx();
                    list = new ArrayList<>();
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> cycled(final long rounds) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        if (rounds == 0) {
            return limit(0);
        } else if (rounds == 1) {
            return skip(0);
        }

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<T> iter = null;
            private List<T> list = null;
            private Object[] a = null;
            private int len = 0;
            private int cursor = -1;
            private T e = null;
            private long m = 0;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                if (m >= rounds) {
                    return false;
                }

                if (a != null) {
                    // len == 0 means the source produced zero elements; cycling an empty
                    // stream any number of times yields nothing — must short-circuit here,
                    // otherwise rounds >= 3 would have hasNext() return true while next()
                    // throws NoSuchElementException on the exhausted iter.
                    return len > 0 && (cursor < len || rounds - m > 1);
                } else if (iter.hasNext()) {
                    return true;
                } else {
                    a = list.toArray();
                    len = a.length;
                    cursor = 0;
                    m++;

                    return m < rounds && len > 0;
                }
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                        m++;
                    }

                    return (T) a[cursor++];
                } else {
                    e = iter.next();
                    list.add(e);

                    return e;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = AbstractStream.this.iteratorEx();
                    list = new ArrayList<>();
                }
            }
        }, rounds <= 1 && isSorted(), rounds <= 1 ? comparator() : null);
    }

    @Override
    public Stream<List<T>> rollup() throws IllegalStateException {
        assertNotClosed();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean initialized = false;
            private List<T> elements;
            private int toIndex = -1;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    init();
                }

                return cursor < toIndex;
            }

            @Override
            public List<T> next() {
                if (!initialized) {
                    init();
                }

                if (cursor >= toIndex) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return elements.subList(0, cursor++);
            }

            @Override
            public long count() {
                if (!initialized) {
                    init();
                }

                final long ret = toIndex - cursor;
                cursor = toIndex;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                cursor = n < toIndex - cursor ? cursor + (int) n : toIndex;
            }

            private void init() {
                if (!initialized) {
                    initialized = true;

                    final Tuple3<Object[], Integer, Integer> tp = AbstractStream.this.arrayForIntermediateOp();

                    elements = Arrays.asList((T[]) tp._1).subList(tp._2, tp._3);
                    toIndex = elements.size() + 1;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> buffered() throws IllegalStateException {
        assertNotClosed();

        return buffered(DEFAULT_BUFFERED_SIZE_PER_ITERATOR);
    }

    @Override
    public Stream<T> buffered(final int bufferSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(bufferSize, cs.bufferSize);

        return buffered(new ArrayBlockingQueue<>(bufferSize));
    }

    @Override
    public Stream<T> buffered(final BlockingQueue<T> queueToBuffer) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(queueToBuffer, cs.queueToBuffer);
        checkArgument(queueToBuffer.isEmpty(), "'queueToBuffer' must be empty");

        final Supplier<BufferedIterator<T>> supplier = () -> buffered(iteratorEx(), queueToBuffer);

        //noinspection resource
        return just(supplier).map(Supplier::get)
                .flatMap(iter -> newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)));
    }

    @Override
    public Stream<T> append(final Stream<T> stream) throws IllegalStateException {
        assertNotClosed();

        return Stream.concat(this, stream);
    }

    @Override
    public Stream<T> append(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        return append(Stream.of(c));
    }

    @Override
    public Stream<T> append(final Optional<T> op) throws IllegalStateException {
        assertNotClosed();

        // return append(op.stream());
        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    @Override
    public Stream<T> prepend(final Stream<T> stream) throws IllegalStateException {
        assertNotClosed();

        return Stream.concat(stream, this);
    }

    @Override
    public Stream<T> prepend(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        return prepend(Stream.of(c));
    }

    @Override
    public Stream<T> prepend(final Optional<T> op) { //NOSONAR
        assertNotClosed();

        // return prepend(op.stream());
        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    @Override
    public Stream<T> mergeWith(final Collection<? extends T> b, final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        return Stream.merge(iteratorEx(), N.iterate(b), nextSelector).onClose(newCloseHandler(this));
    }

    @Override
    public Stream<T> mergeWith(final Stream<? extends T> b, final BiFunction<? super T, ? super T, MergeResult> nextSelector) throws IllegalStateException {
        assertNotClosed();

        return Stream.merge(this, b, nextSelector);
    }

    @Override
    public <T2, R> Stream<R> zipWith(final Collection<T2> b, final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return Stream.<T, T2, R> zip(iteratorEx(), N.iterate(b), zipFunction).onClose(newCloseHandler(this));
    }

    @Override
    public <T2, R> Stream<R> zipWith(final Collection<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return Stream.<T, T2, R> zip(iteratorEx(), N.iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(this));
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(final Collection<T2> b, final Collection<T3> c,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return Stream.<T, T2, T3, R> zip(iteratorEx(), N.iterate(b), N.iterate(c), zipFunction).onClose(newCloseHandler(this));
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(final Collection<T2> b, final Collection<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return Stream.<T, T2, T3, R> zip(iteratorEx(), N.iterate(b), N.iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(this));
    }

    @Override
    public <T2, R> Stream<R> zipWith(final Stream<T2> b, final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return Stream.zip(this, b, zipFunction);
    }

    @Override
    public <T2, R> Stream<R> zipWith(final Stream<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return Stream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return Stream.zip(this, b, c, zipFunction);
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return Stream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @SuppressWarnings("rawtypes")
    private static final Function TO_LINE_OF_STRING = N::stringOf;

    @Override
    public Stream<T> saveEach(final File output) {
        return saveEach(TO_LINE_OF_STRING, output);
    }

    @Override
    public Stream<T> saveEach(final Function<? super T, String> toLine, final File output) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private Writer writer = null;
            private BufferedWriter bw = null;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                } catch (final IOException e) {
                    throw toRuntimeException(e);
                }

                return next;
            }

            @Override
            public void closeResource() {
                if (writer != null) {
                    try {
                        if (bw != null) {
                            try {
                                bw.flush();
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            } finally {
                                Objectory.recycle(bw);
                            }
                        }
                    } finally {
                        IOUtil.close(writer);
                    }
                }
            }

            private void init() {
                initialized = true;

                writer = IOUtil.newFileWriter(output);
                bw = Objectory.createBufferedWriter(writer);
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> saveEach(final Function<? super T, String> toLine, final OutputStream output) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private BufferedWriter bw = null;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                } catch (final IOException e) {
                    throw toRuntimeException(e);
                }

                return next;
            }

            @Override
            public void closeResource() {
                if (bw != null) {
                    try {
                        bw.flush();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        Objectory.recycle(bw);
                    }
                }
            }

            private void init() {
                initialized = true;

                bw = Objectory.createBufferedWriter(output);
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> saveEach(final Function<? super T, String> toLine, final Writer output) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private Writer bw = null;
            private boolean isBufferedWriter = false;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                } catch (final IOException e) {
                    throw toRuntimeException(e);
                }

                return next;
            }

            @Override
            public void closeResource() {
                if (bw != null) {
                    try {
                        bw.flush();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        if (!isBufferedWriter) {
                            Objectory.recycle((BufferedWriter) bw);
                        }
                    }
                }
            }

            private void init() {
                initialized = true;

                isBufferedWriter = output instanceof java.io.BufferedWriter;
                bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> saveEach(final Throwables.BiConsumer<? super T, Writer, IOException> write, final File output) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private Writer writer = null;
            private BufferedWriter bw = null;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    write.accept(next, bw);
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                } catch (final IOException e) {
                    throw toRuntimeException(e);
                }

                return next;
            }

            @Override
            public void closeResource() {
                if (writer != null) {
                    try {
                        if (bw != null) {
                            try {
                                bw.flush();
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            } finally {
                                Objectory.recycle(bw);
                            }
                        }
                    } finally {
                        IOUtil.close(writer);
                    }
                }
            }

            private void init() {
                initialized = true;

                writer = IOUtil.newFileWriter(output);
                bw = Objectory.createBufferedWriter(writer);
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> saveEach(final Throwables.BiConsumer<? super T, Writer, IOException> write, final Writer output) throws IllegalStateException {
        assertNotClosed();

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private Writer bw = null;
            private boolean isBufferedWriter = false;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    write.accept(next, bw);
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                } catch (final IOException e) {
                    throw toRuntimeException(e);
                }

                return next;
            }

            @Override
            public void closeResource() {
                if (bw != null) {
                    try {
                        bw.flush();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        if (!isBufferedWriter) {
                            Objectory.recycle((BufferedWriter) bw);
                        }
                    }
                }
            }

            private void init() {
                initialized = true;

                isBufferedWriter = output instanceof java.io.BufferedWriter;
                bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> saveEach(final PreparedStatement stmt, final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException {
        return saveEach(stmt, 1, 0, stmtSetter);
    }

    @Override
    public Stream<T> saveEach(final PreparedStatement stmt, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(stmt, cs.PreparedStatement);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private final boolean isBatchUsed = batchSize > 1;
            long cnt = 0;

            /**
             * Returns {@code true} if the underlying source iterator has more elements.
             *
             * @return {@code true} if another element is available
             */
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * Returns the next element and persists it through the configured statement.
             *
             * @return the next processed element
             * @throws java.util.NoSuchElementException if the iteration has no more elements
             * @throws RuntimeException wrapping any {@link java.sql.SQLException} thrown during statement execution
             */
            @Override
            public T next() {
                final T next = iter.next();

                try {
                    stmtSetter.accept(next, stmt);

                    if (isBatchUsed) {
                        cnt++;
                        stmt.addBatch();

                        if (cnt % batchSize == 0) {
                            DataSourceUtil.executeBatch(stmt);

                            if (batchIntervalInMillis > 0) {
                                N.sleepUninterruptibly(batchIntervalInMillis);
                            }
                        }
                    } else {
                        stmt.execute();
                    }
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            public void closeResource() {
                if (isBatchUsed && (cnt % batchSize) > 0) {
                    try {
                        DataSourceUtil.executeBatch(stmt);
                    } catch (final SQLException e) {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    }
                }
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> saveEach(final Connection conn, final String insertSQL,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException {
        return saveEach(conn, insertSQL, 1, 0, stmtSetter);
    }

    @Override
    public Stream<T> saveEach(final Connection conn, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(conn, cs.Connection);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private PreparedStatement stmt = null;
            private boolean initialized = false;
            private final boolean isBatchUsed = batchSize > 1;
            long cnt = 0;

            /**
             * Returns {@code true} if the underlying source iterator has more elements.
             *
             * @return {@code true} if another element is available
             */
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * Returns the next element and persists it via a lazily created {@link PreparedStatement}.
             *
             * @return the next processed element
             * @throws java.util.NoSuchElementException if the iteration has no more elements
             * @throws RuntimeException wrapping any {@link java.sql.SQLException} thrown during statement preparation or execution
             */
            @Override
            public T next() {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    stmtSetter.accept(next, stmt);

                    if (isBatchUsed) {
                        cnt++;
                        stmt.addBatch();

                        if (cnt % batchSize == 0) {
                            DataSourceUtil.executeBatch(stmt);

                            if (batchIntervalInMillis > 0) {
                                N.sleepUninterruptibly(batchIntervalInMillis);
                            }
                        }
                    } else {
                        stmt.execute();
                    }
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            public void closeResource() {
                if (stmt != null) {
                    try {
                        if (isBatchUsed && (cnt % batchSize) > 0) {
                            DataSourceUtil.executeBatch(stmt);
                        }
                    } catch (final SQLException e) {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    } finally {
                        DataSourceUtil.closeQuietly(stmt);
                    }
                }
            }

            private void init() {
                initialized = true;

                try {
                    stmt = conn.prepareStatement(insertSQL);
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> saveEach(final javax.sql.DataSource ds, final String insertSQL,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException {
        return saveEach(ds, insertSQL, 1, 0, stmtSetter);
    }

    @Override
    public Stream<T> saveEach(final javax.sql.DataSource ds, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(ds, cs.DataSource);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private Connection conn = null;
            private PreparedStatement stmt = null;
            private boolean initialized = false;
            private final boolean isBatchUsed = batchSize > 1;
            long cnt = 0;

            /**
             * Returns {@code true} if the underlying source iterator has more elements.
             *
             * @return {@code true} if another element is available
             */
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * Returns the next element and persists it via lazily obtained {@link Connection} and {@link PreparedStatement}.
             *
             * @return the next processed element
             * @throws java.util.NoSuchElementException if the iteration has no more elements
             * @throws RuntimeException wrapping any {@link java.sql.SQLException} thrown during connection acquisition, statement preparation, or execution
             */
            @Override
            public T next() {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    stmtSetter.accept(next, stmt);

                    if (isBatchUsed) {
                        cnt++;
                        stmt.addBatch();

                        if (cnt % batchSize == 0) {
                            DataSourceUtil.executeBatch(stmt);

                            if (batchIntervalInMillis > 0) {
                                N.sleepUninterruptibly(batchIntervalInMillis);
                            }
                        }
                    } else {
                        stmt.execute();
                    }
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            public void closeResource() {
                try {
                    if (stmt != null) {
                        try {
                            if (isBatchUsed && (cnt % batchSize) > 0) {
                                DataSourceUtil.executeBatch(stmt);
                            }
                        } catch (final SQLException e) {
                            throw ExceptionUtil.toRuntimeException(e, true);
                        } finally {
                            DataSourceUtil.closeQuietly(stmt);
                        }
                    }
                } finally {
                    DataSourceUtil.releaseConnection(conn, ds);
                }
            }

            private void init() {
                initialized = true;

                try {
                    conn = ds.getConnection();
                    stmt = conn.prepareStatement(insertSQL);
                } catch (final SQLException e) {
                    try {
                        if (stmt != null) {
                            DataSourceUtil.closeQuietly(stmt);
                        }
                    } finally {
                        DataSourceUtil.releaseConnection(conn, ds);
                    }

                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public long persist(final File output) throws IOException {
        return persist(TO_LINE_OF_STRING, output);
    }

    @Override
    public long persist(final String header, final String tail, final File output) throws IOException {
        return persist(header, tail, TO_LINE_OF_STRING, output);
    }

    @Override
    public long persist(final Function<? super T, String> toLine, final File output) throws IOException {
        return persist(null, null, toLine, output);
    }

    @Override
    public long persist(final String header, final String tail, final Function<? super T, String> toLine, final File output)
            throws IllegalStateException, IOException {
        assertNotClosed();

        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persist(header, tail, toLine, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public long persist(final Function<? super T, String> toLine, final OutputStream output) throws IllegalStateException, IOException {
        assertNotClosed();

        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persist(toLine, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Override
    public long persist(final String header, final String tail, final Function<? super T, String> toLine, final OutputStream output)
            throws IllegalStateException, IOException {
        assertNotClosed();

        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persist(header, tail, toLine, bw);
        } finally {
            Objectory.recycle(bw);
        }

    }

    @Override
    public long persist(final Function<? super T, String> toLine, final Writer output) throws IllegalStateException, IOException {
        assertNotClosed();

        return persist(null, null, toLine, output);
    }

    @Override
    public long persist(final String header, final String tail, final Function<? super T, String> toLine, final Writer output)
            throws IllegalStateException, IOException {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
            final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
            long cnt = 0;

            try {
                @SuppressWarnings("resource")
                final Iterator<T> iter = iteratorEx();

                if (header != null) {
                    bw.write(header);
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                }

                while (iter.hasNext()) {
                    bw.write(toLine.apply(iter.next()));
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                    if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                }

                if (tail != null) {
                    bw.write(tail);
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                }
            } finally {
                try {
                    bw.flush();
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    @Override
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> write, final File output) throws IOException {
        return persist(null, null, write, output);
    }

    @Override
    public long persist(final String header, final String tail, final Throwables.BiConsumer<? super T, Writer, IOException> write, final File output)
            throws IOException {
        assertNotClosed();

        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persist(header, tail, write, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> write, final Writer output) throws IllegalStateException, IOException {
        assertNotClosed();

        return persist(null, null, write, output);
    }

    @Override
    public long persist(final String header, final String tail, final Throwables.BiConsumer<? super T, Writer, IOException> write, final Writer output)
            throws IOException {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
            final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
            long cnt = 0;

            try {
                @SuppressWarnings("resource")
                final Iterator<T> iter = iteratorEx();

                if (header != null) {
                    bw.write(header);
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                }

                while (iter.hasNext()) {
                    write.accept(iter.next(), bw);
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                    if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                }

                if (tail != null) {
                    bw.write(tail);
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                }

            } finally {
                try {
                    bw.flush();
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    @Override
    public long persist(final PreparedStatement stmt, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException, SQLException {
        assertNotClosed();
        checkArgNotNull(stmt, cs.PreparedStatement);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        try {
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();
            final boolean isBatchUsed = batchSize > 1;
            long cnt = 0;

            while (iter.hasNext()) {
                stmtSetter.accept(iter.next(), stmt);
                cnt++;

                if (isBatchUsed) {
                    stmt.addBatch();

                    if (cnt % batchSize == 0) {
                        DataSourceUtil.executeBatch(stmt);

                        if (batchIntervalInMillis > 0) {
                            N.sleepUninterruptibly(batchIntervalInMillis);
                        }
                    }
                } else {
                    stmt.execute();
                }
            }

            if (isBatchUsed && cnt % batchSize > 0) {
                DataSourceUtil.executeBatch(stmt);
            }

            return cnt;
        } finally {
            close();
        }
    }

    @Override
    public long persist(final Connection conn, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, SQLException {
        assertNotClosed();

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchIntervalInMillis, stmtSetter);
        } finally {
            DataSourceUtil.closeQuietly(stmt);
        }
    }

    @Override
    public long persist(final javax.sql.DataSource ds, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, SQLException {
        assertNotClosed();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ds.getConnection();
            stmt = conn.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchIntervalInMillis, stmtSetter);
        } finally {
            try {
                DataSourceUtil.closeQuietly(stmt);
            } finally {
                DataSourceUtil.releaseConnection(conn, ds);
            }
        }
    }

    @Override
    public long persistToCsv(final File output) throws IOException {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToCsv(writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public long persistToCsv(final Collection<String> headers, final File output) throws IOException {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToCsv(headers, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public long persistToCsv(final OutputStream output) throws IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToCsv(bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Override
    public long persistToCsv(final Collection<String> headers, final OutputStream output) throws IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToCsv(headers, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Override
    public long persistToCsv(final Writer output) throws IllegalStateException, IOException {
        return persistToCsv(null, output, true);
    }

    @TerminalOp
    @Override
    public long persistToCsv(final Collection<String> csvHeaders, final Writer output) throws IllegalStateException, IllegalArgumentException, IOException {
        return persistToCsv(csvHeaders, output, false);
    }

    private long persistToCsv(final Collection<String> csvHeaders, final Writer output, final boolean canCsvHeadersBeEmpty)
            throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        if (!canCsvHeadersBeEmpty) {
            checkArgNotEmpty(csvHeaders, cs.csvHeaders);
        }

        try {
            final List<Object> headers = N.newArrayList(csvHeaders);
            final boolean isBufferedWriter = output instanceof BufferedCsvWriter;
            final BufferedCsvWriter bw = isBufferedWriter ? (BufferedCsvWriter) output : Objectory.createBufferedCsvWriter(output);

            final char separator = SK._COMMA;
            long cnt = 0;
            T next = null;
            Class<?> cls = null;

            try {
                @SuppressWarnings("resource")
                final Iterator<T> iter = iteratorEx();

                if (iter.hasNext()) {
                    cnt++;
                    next = iter.next();
                    cls = next.getClass();

                    if (Beans.isBeanClass(cls)) {
                        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

                        if (N.isEmpty(headers)) {
                            headers.addAll(beanInfo.propNameList);
                        }

                        final int headSize = headers.size();
                        final PropInfo[] propInfos = new PropInfo[headSize];
                        PropInfo propInfo = null;

                        for (int i = 0; i < headSize; i++) {
                            propInfos[i] = beanInfo.getPropInfo(headers.get(i).toString());

                            if (propInfos[i] == null) {
                                throw new IllegalArgumentException("Property '" + headers.get(i) + "' is not found in " + cls);
                            }
                        }

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, headers.get(i));
                        }

                        bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                        for (int i = 0; i < headSize; i++) {
                            propInfo = propInfos[i];

                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, propInfo.jsonXmlType, propInfo.getPropValue(next));
                        }

                        while (iter.hasNext()) {
                            next = iter.next();

                            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                            for (int i = 0; i < headSize; i++) {
                                propInfo = propInfos[i];

                                if (i > 0) {
                                    bw.write(separator);
                                }

                                CsvUtil.writeField(bw, propInfo.jsonXmlType, propInfo.getPropValue(next));
                            }

                            if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                                bw.flush();
                            }
                        }
                    } else if (next instanceof Map) {
                        Map<Object, Object> row = (Map<Object, Object>) next;

                        if (N.isEmpty(headers)) {
                            headers.addAll(row.keySet());
                        }

                        final int headSize = headers.size();

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, headers.get(i));
                        }

                        bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, row.get(headers.get(i)));
                        }

                        while (iter.hasNext()) {
                            row = (Map<Object, Object>) iter.next();

                            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                            for (int i = 0; i < headSize; i++) {
                                if (i > 0) {
                                    bw.write(separator);
                                }

                                CsvUtil.writeField(bw, null, row.get(headers.get(i)));
                            }

                            if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                                bw.flush();
                            }
                        }
                    } else if (N.notEmpty(headers) && next instanceof Collection) {
                        final int headSize = headers.size();
                        Collection<Object> row = (Collection<Object>) next;

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, headers.get(i));
                        }

                        bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                        Iterator<Object> rowIter = row.iterator();

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, rowIter.next());
                        }

                        while (iter.hasNext()) {
                            row = (Collection<Object>) iter.next();
                            rowIter = row.iterator();

                            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                            for (int i = 0; i < headSize; i++) {
                                if (i > 0) {
                                    bw.write(separator);
                                }

                                CsvUtil.writeField(bw, null, rowIter.next());
                            }

                            if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                                bw.flush();
                            }
                        }
                    } else if (N.notEmpty(headers) && next instanceof Object[] row) {
                        final int headSize = headers.size();
                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, headers.get(i));
                        }

                        bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, row[i]);
                        }

                        while (iter.hasNext()) {
                            row = (Object[]) iter.next();

                            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                            for (int i = 0; i < headSize; i++) {
                                if (i > 0) {
                                    bw.write(separator);
                                }

                                CsvUtil.writeField(bw, null, row[i]);
                            }

                            if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                                bw.flush();
                            }
                        }
                    } else {
                        throw new RuntimeException(cls + " is not supported for CSV format. Only bean/Map types are supported");
                    }
                } else if (N.notEmpty(headers)) {
                    final int headSize = headers.size();

                    for (int i = 0; i < headSize; i++) {
                        if (i > 0) {
                            bw.write(separator);
                        }

                        CsvUtil.writeField(bw, null, headers.get(i));
                    }
                }
            } finally {
                try {
                    bw.flush();
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle(bw);
                    }
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    @Override
    public long persistToJson(final File output) throws IOException {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToJson(writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public long persistToJson(final OutputStream output) throws IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToJson(bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Override
    public long persistToJson(final Writer output) throws IllegalStateException, IOException {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = output instanceof BufferedJsonWriter;
            final BufferedJsonWriter bw = isBufferedWriter ? (BufferedJsonWriter) output : Objectory.createBufferedJsonWriter(output); // NOSONAR

            long cnt = 0;

            try {
                @SuppressWarnings("resource")
                final Iterator<T> iter = iteratorEx();

                bw.write("[");

                while (iter.hasNext()) {
                    if (cnt > 0) {
                        bw.write(SK._COMMA);
                    }

                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    N.toJson(iter.next(), bw);

                    if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                }

                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write("]");
            } finally {
                try {
                    bw.flush();
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle(bw);
                    }
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    Function<List<T>, Stream<T>> listToStreamMapper() {
        return t -> new ArrayStream<>(StreamBase.toArray(t), 0, t.size(), isSorted(), comparator(), null);
    }

    @Override
    public <U> Stream<Pair<T, U>> crossJoin(final Collection<? extends U> b) {
        return crossJoin(b, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> crossJoin(final Collection<? extends U> b, final BiFunction<? super T, ? super U, ? extends R> func) {
        return flatMap(t -> Stream.of(b).map(u -> func.apply(t, u)));
    }

    @Override
    public <U, R> Stream<R> crossJoin(final Stream<? extends U> b, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(func, cs.func);

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile List<U> c = null;

            @Override
            public Stream<R> apply(final T t) {
                if (c == null) {
                    synchronized (b) {
                        if (c == null) {
                            c = (List<U>) b.toList();
                        }
                    }
                }

                return Stream.of(c).map(u -> func.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> innerJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) {
        return innerJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, T>> innerJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper) {
        return innerJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> innerJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (rightKeyExtractor) {
                        if (rightKeyMap == null) {
                            rightKeyMap = ListMultimap.fromCollection(b, rightKeyExtractor);
                        }
                    }
                }

                return Stream.of(rightKeyMap.get(leftKeyExtractor.apply(t))).map(u -> func.apply(t, u));
            }
        });
    }

    @Override
    public <K, R> Stream<R> innerJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> func) {
        return innerJoin(b, keyMapper, keyMapper, func);
    }

    @Override
    public <U, K, R> Stream<R> innerJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (rightKeyExtractor) {
                        if (rightKeyMap == null) {
                            rightKeyMap = ((Stream<U>) b).toMultimap(Fn.from(rightKeyExtractor));
                        }
                    }
                }

                return Stream.of(rightKeyMap.get(leftKeyExtractor.apply(t))).map(u -> func.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, U>> innerJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate) {
        return innerJoin(b, predicate, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> innerJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate,
            final BiFunction<? super T, ? super U, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(func, cs.func);

        return flatMap(t -> Stream.of(b).filter(u -> predicate.test(t, u)).map(u -> func.apply(t, u)));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> fullJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) {
        return fullJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, T>> fullJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper) {
        return fullJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> fullJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<Object, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (rightKeyExtractor) {
                        if (rightKeyMap == null) {
                            rightKeyMap = ListMultimap.fromCollection(b, rightKeyExtractor);
                        }
                    }
                }

                final List<U> values = rightKeyMap.get(leftKeyExtractor.apply(t));

                return N.isEmpty(values) ? Stream.of(func.apply(t, null)) : Stream.of(values).map(u -> {
                    if (isParallelStream) {
                        synchronized (joinedRights) {
                            joinedRights.put(u, u);
                        }
                    } else {
                        joinedRights.put(u, u);
                    }

                    return func.apply(t, u);
                });
            }
        }).append(Stream.of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply(null, u)));
    }

    @Override
    public <K, R> Stream<R> fullJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> func) {
        return fullJoin(b, keyMapper, keyMapper, func);
    }

    @Override
    public <U, K, R> Stream<R> fullJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final Holder<List<U>> holder = new Holder<>();

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<Object, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (rightKeyExtractor) {
                        if (rightKeyMap == null) {
                            final List<U> c = ((Stream<U>) b).toList();
                            rightKeyMap = ListMultimap.fromCollection(c, rightKeyExtractor);
                            holder.setValue(c);
                        }
                    }
                }

                final List<U> values = rightKeyMap.get(leftKeyExtractor.apply(t));

                return N.isEmpty(values) ? Stream.of(func.apply(t, null)) : Stream.of(values).map(u -> {
                    if (isParallelStream) {
                        synchronized (joinedRights) {
                            joinedRights.put(u, u);
                        }
                    } else {
                        joinedRights.put(u, u);
                    }

                    return func.apply(t, u);
                });
            }
        }).append(Stream.defer(() -> {
            // The right side is materialized lazily by the mapper; when this (left) stream was empty
            // the mapper never ran, so consume b here to keep the documented join semantics.
            final List<U> rights = holder.value() != null ? holder.value() : ((Stream<U>) b).toList();

            return Stream.of(rights).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply(null, u));
        })).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, U>> fullJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate) {
        return fullJoin(b, predicate, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> fullJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate,
            final BiFunction<? super T, ? super U, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(func, cs.func);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();

        //noinspection resource
        return flatMap(t -> Stream.of(b).filter(u -> predicate.test(t, u)).map(u -> {
            if (isParallelStream) {
                synchronized (joinedRights) {
                    joinedRights.put(u, u);
                }
            } else {
                joinedRights.put(u, u);
            }

            return (R) func.apply(t, u);
        }).appendIfEmpty(() -> Stream.of(t).map(tt -> func.apply(t, null))))
                .append(Stream.of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply(null, u)));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> leftJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) {
        return leftJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, T>> leftJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper) {
        return leftJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> leftJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (rightKeyExtractor) {
                        if (rightKeyMap == null) {
                            rightKeyMap = ListMultimap.fromCollection(b, rightKeyExtractor);
                        }
                    }
                }

                final List<U> values = rightKeyMap.get(leftKeyExtractor.apply(t));

                return N.isEmpty(values) ? Stream.of(func.apply(t, null)) : Stream.of(values).map(u -> func.apply(t, u));
            }
        });
    }

    @Override
    public <K, R> Stream<R> leftJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> func) {
        return leftJoin(b, keyMapper, keyMapper, func);
    }

    @Override
    public <U, K, R> Stream<R> leftJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (rightKeyExtractor) {
                        if (rightKeyMap == null) {
                            rightKeyMap = ((Stream<U>) b).toMultimap(Fn.from(rightKeyExtractor));
                        }
                    }
                }

                final List<U> values = rightKeyMap.get(leftKeyExtractor.apply(t));

                return N.isEmpty(values) ? Stream.of(func.apply(t, null)) : Stream.of(values).map(u -> func.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, U>> leftJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate) {
        return leftJoin(b, predicate, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> leftJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate,
            final BiFunction<? super T, ? super U, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(func, cs.func);

        return flatMap(t -> Stream.of(b)
                .filter(u -> predicate.test(t, u))
                .map(u -> (R) func.apply(t, u))
                .appendIfEmpty(() -> Stream.of(t).map(tt -> func.apply(t, null))));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> rightJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) {
        return rightJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, T>> rightJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper) {
        return rightJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> rightJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (rightKeyExtractor) {
                        if (rightKeyMap == null) {
                            rightKeyMap = ListMultimap.fromCollection(b, rightKeyExtractor);
                        }
                    }
                }

                return Stream.of(rightKeyMap.get(leftKeyExtractor.apply(t))).map(u -> {
                    if (isParallelStream) {
                        synchronized (joinedRights) {
                            joinedRights.put(u, u);
                        }
                    } else {
                        joinedRights.put(u, u);
                    }

                    return func.apply(t, u);
                });
            }
        }).append(Stream.of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply(null, u)));
    }

    @Override
    public <K, R> Stream<R> rightJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> func) {
        return rightJoin(b, keyMapper, keyMapper, func);
    }

    @Override
    public <U, K, R> Stream<R> rightJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final Holder<List<U>> holder = new Holder<>();

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (rightKeyExtractor) {
                        if (rightKeyMap == null) {
                            final List<U> c = ((Stream<U>) b).toList();
                            rightKeyMap = ListMultimap.fromCollection(c, rightKeyExtractor);
                            holder.setValue(c);
                        }
                    }
                }

                return Stream.of(rightKeyMap.get(leftKeyExtractor.apply(t))).map(u -> {
                    if (isParallelStream) {
                        synchronized (joinedRights) {
                            joinedRights.put(u, u);
                        }
                    } else {
                        joinedRights.put(u, u);
                    }

                    return func.apply(t, u);
                });
            }
        }).append(Stream.defer(() -> {
            // The right side is materialized lazily by the mapper; when this (left) stream was empty
            // the mapper never ran, so consume b here to keep the documented join semantics.
            final List<U> rights = holder.value() != null ? holder.value() : ((Stream<U>) b).toList();

            return Stream.of(rights).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply(null, u));
        })).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, U>> rightJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate) {
        return rightJoin(b, predicate, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> rightJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate,
            final BiFunction<? super T, ? super U, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(func, cs.func);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();

        //noinspection resource
        return flatMap(t -> Stream.of(b).filter(u -> predicate.test(t, u)).map(u -> {
            if (isParallelStream) {
                synchronized (joinedRights) {
                    joinedRights.put(u, u);
                }
            } else {
                joinedRights.put(u, u);
            }

            return (R) func.apply(t, u);
        })).append(Stream.of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply(null, u)));
    }

    @Override
    public <U, K> Stream<Pair<T, List<U>>> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) {
        return groupJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, List<T>>> groupJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper) {
        return groupJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super List<U>, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, List<U>> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                final List<U> val = map.get(leftKeyExtractor.apply(t));

                return func.apply(t, Objects.requireNonNullElseGet(val, Suppliers.ofList()));
            }

            private void init() {
                if (!initialized) {
                    synchronized (this) {
                        if (!initialized) {
                            // map = Stream.of(b).parallel(ps.maxThreadNum(), ps.splitor(), ps.asyncExecutor()).groupTo(rightKeyExtractor);   // TODO may not be necessary.
                            map = Stream.<U> of(b).groupTo(Fn.from(rightKeyExtractor));
                            initialized = true;
                        }
                    }
                }
            }
        };

        return map(mapper);
    }

    @Override
    public <K, R> Stream<R> groupJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super List<T>, ? extends R> func) {
        return groupJoin(b, keyMapper, keyMapper, func);
    }

    @Override
    public <U, K, R> Stream<R> groupJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super List<U>, ? extends R> func)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(func, cs.func);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, List<U>> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                final List<U> val = map.get(leftKeyExtractor.apply(t));

                return func.apply(t, Objects.requireNonNullElseGet(val, Suppliers.ofList()));
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                // map = Stream.of(b).parallel(ps.maxThreadNum(), ps.splitor(), ps.asyncExecutor()).groupTo(rightKeyExtractor);   // TODO may not be necessary.
                                map = ((Stream<U>) b).groupTo(Fn.from(rightKeyExtractor));
                                initialized = true;
                            }
                        }
                    } else {
                        map = ((Stream<U>) b).groupTo(Fn.from(rightKeyExtractor));
                        initialized = true;
                    }
                }
            }
        };

        //noinspection resource
        return map(mapper).onClose(newCloseHandler(b));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BinaryOperator<U> mergeFunction) {
        return groupJoin(b, leftKeyExtractor, rightKeyExtractor, mergeFunction, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BinaryOperator<U> mergeFunction,
            final BiFunction<? super T, ? super U, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(func, cs.func);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, U> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                final U val = map.get(leftKeyExtractor.apply(t));

                if (val == null) {
                    return func.apply(t, null);
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                //    map = Stream.of(b)
                                //            .parallel(ps.maxThreadNum(), ps.splitor(), ps.asyncExecutor())
                                //            .toMap(rightKeyExtractor, Fn.<U> identity(), mergeFunction);   // TODO may not be necessary.

                                map = Stream.of(b).toMap(Fn.from(rightKeyExtractor), Fn.<U> identity(), mergeFunction);

                                initialized = true;
                            }
                        }
                    } else {
                        map = Stream.of(b).toMap(Fn.from(rightKeyExtractor), Fn.<U> identity(), mergeFunction);

                        initialized = true;
                    }
                }
            }
        };

        return map(mapper);
    }

    @Override
    public <U, K, R> Stream<R> groupJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BinaryOperator<U> mergeFunction,
            final BiFunction<? super T, ? super U, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(func, cs.func);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, U> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                final U val = map.get(leftKeyExtractor.apply(t));

                if (val == null) {
                    return func.apply(t, null);
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                //    map = Stream.of(b)
                                //            .parallel(ps.maxThreadNum(), ps.splitor(), ps.asyncExecutor())
                                //            .toMap(rightKeyExtractor, Fn.<U> identity(), mergeFunction);   // TODO may not be necessary.

                                map = b.toMap(Fn.from(rightKeyExtractor), Fn.identity(), mergeFunction);

                                initialized = true;
                            }
                        }
                    } else {
                        map = b.toMap(Fn.from(rightKeyExtractor), Fn.identity(), mergeFunction);

                        initialized = true;
                    }
                }
            }
        };

        //noinspection resource
        return map(mapper).onClose(newCloseHandler(b));
    }

    @Override
    public <U, K, D> Stream<Pair<T, D>> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final Collector<? super U, ?, D> downstream) {
        return groupJoin(b, leftKeyExtractor, rightKeyExtractor, downstream, Fn.pair());
    }

    @Override
    public <U, K, D, R> Stream<R> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final Collector<? super U, ?, D> downstream,
            final BiFunction<? super T, ? super D, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(func, cs.func);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, D> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                final D val = map.get(leftKeyExtractor.apply(t));

                if (val == null) {
                    //noinspection resource
                    return func.apply(t, Stream.<U> empty().collect(downstream));
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                //    map = Stream.of(b)
                                //            .parallel(ps.maxThreadNum(), ps.splitor(), ps.asyncExecutor())
                                //            .toMap(rightKeyExtractor, Fn.<U> identity(), downstream);   // TODO may not be necessary.

                                map = Stream.of(b).groupTo(Fn.from(rightKeyExtractor), Fn.<U> identity(), downstream);

                                initialized = true;
                            }
                        }
                    } else {
                        map = Stream.of(b).groupTo(Fn.from(rightKeyExtractor), Fn.<U> identity(), downstream);

                        initialized = true;
                    }
                }
            }
        };

        return map(mapper);
    }

    @Override
    public <K, D> Stream<Pair<T, D>> groupJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, ?, D> downstream) {
        return groupJoin(b, keyMapper, keyMapper, downstream);
    }

    @Override
    public <K, D, R> Stream<R> groupJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, ?, D> downstream, final BiFunction<? super T, ? super D, ? extends R> func) {
        return groupJoin(b, keyMapper, keyMapper, downstream, func);
    }

    @Override
    public <U, K, D, R> Stream<R> groupJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final Collector<? super U, ?, D> downstream,
            final BiFunction<? super T, ? super D, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(func, cs.func);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, D> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                final D val = map.get(leftKeyExtractor.apply(t));

                if (val == null) {
                    //noinspection resource
                    return func.apply(t, Stream.<U> empty().collect(downstream));
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                //    map = Stream.of(b)
                                //            .parallel(ps.maxThreadNum(), ps.splitor(), ps.asyncExecutor())
                                //            .toMap(rightKeyExtractor, Fn.<U> identity(), downstream);   // TODO may not be necessary.

                                map = b.groupTo(Fn.from(rightKeyExtractor), Fn.<U> identity(), downstream);

                                initialized = true;
                            }
                        }
                    } else {
                        map = b.groupTo(Fn.from(rightKeyExtractor), Fn.<U> identity(), downstream);

                        initialized = true;
                    }
                }
            }
        };

        //noinspection resource
        return map(mapper).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, List<U>>> joinByRange(final Iterator<U> b, final BiPredicate<? super T, ? super U> predicate)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "Iterator 'b' cannot be null");
        checkArgNotNull(predicate, "'predicate' cannot be null");

        final Function<T, Pair<T, List<U>>> mapper = new Function<>() {
            private final Iterator<U> iter = b;
            private final U none = (U) NONE;
            private U next = none;

            @Override
            public Pair<T, List<U>> apply(final T t) {
                final List<U> list = new ArrayList<>();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return Pair.of(t, list);
                    }
                }

                while (predicate.test(t, next)) {
                    list.add(next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                return Pair.of(t, list);
            }
        };

        if (isParallel()) {
            //noinspection resource
            return sequential().map(mapper).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return map(mapper);
        }
    }

    @Override
    public <U, R> Stream<Pair<T, R>> joinByRange(final Iterator<U> b, final BiPredicate<? super T, ? super U> predicate,
            final Collector<? super U, ?, R> collector) {
        return joinByRange(b, predicate, collector, Fn.pair());
    }

    @Override
    public <U, D, R> Stream<R> joinByRange(final Iterator<U> b, final BiPredicate<? super T, ? super U> predicate, final Collector<? super U, ?, D> collector,
            final BiFunction<? super T, ? super D, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "Iterator 'b' cannot be null");
        checkArgNotNull(predicate, "'predicate' cannot be null");
        checkArgNotNull(collector, "'collector' cannot be null");
        checkArgNotNull(func, "'func' cannot be null");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super U> accumulator = (BiConsumer<Object, ? super U>) collector.accumulator();
        final Function<Object, D> finisher = (Function<Object, D>) collector.finisher();

        final Function<T, R> mapper = new Function<>() {
            private final Iterator<U> iter = b;
            private final U none = (U) NONE;
            private U next = none;

            @Override
            public R apply(final T t) {
                final Object container = supplier.get();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return func.apply(t, finisher.apply(container));
                    }
                }

                while (predicate.test(t, next)) {
                    accumulator.accept(container, next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                return func.apply(t, finisher.apply(container));
            }
        };

        if (isParallel()) {
            //noinspection resource
            return sequential().map(mapper).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return map(mapper);
        }
    }

    @Override
    public <U, D, R> Stream<R> joinByRange(final Iterator<U> b, final BiPredicate<? super T, ? super U> predicate, final Collector<? super U, ?, D> collector,
            final BiFunction<? super T, ? super D, ? extends R> func, final Function<Iterator<U>, Stream<R>> mapperForUnJoinedElements)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "Iterator 'b' cannot be null");
        checkArgNotNull(predicate, "'predicate' cannot be null");
        checkArgNotNull(collector, "'collector' cannot be null");
        checkArgNotNull(func, "'func' cannot be null");
        checkArgNotNull(mapperForUnJoinedElements, "'mapperForUnJoinedElements' cannot be null");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super U> accumulator = (BiConsumer<Object, ? super U>) collector.accumulator();
        final Function<Object, D> finisher = (Function<Object, D>) collector.finisher();
        final U none = (U) NONE;
        final Holder<U> nextValueHolder = Holder.of(none);

        final Function<T, R> mapper = new Function<>() {
            private final Iterator<U> iter = b;
            private U next = none;

            @Override
            public R apply(final T t) {
                final Object container = supplier.get();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return func.apply(t, finisher.apply(container));
                    }
                }

                while (predicate.test(t, next)) {
                    accumulator.accept(container, next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                nextValueHolder.setValue(next);

                return func.apply(t, finisher.apply(container));
            }
        };

        if (isParallel()) {
            //noinspection resource
            return sequential().map(mapper)
                    // value == none also occurs when this (left) stream was empty and the mapper never
                    // ran: the untouched iterator must still be routed to mapperForUnJoinedElements.
                    .append(Stream.defer(() -> nextValueHolder.value() == none ? (b.hasNext() ? mapperForUnJoinedElements.apply(b) : Stream.<R> empty())
                            : mapperForUnJoinedElements.apply(Iterators.concat(ObjIterator.of(nextValueHolder.value()), b))))
                    .parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            //noinspection resource
            // value == none also occurs when this (left) stream was empty and the mapper never
            // ran: the untouched iterator must still be routed to mapperForUnJoinedElements.
            return map(mapper)
                    .append(Stream.defer(() -> nextValueHolder.value() == none ? (b.hasNext() ? mapperForUnJoinedElements.apply(b) : Stream.<R> empty())
                            : mapperForUnJoinedElements.apply(Iterators.concat(ObjIterator.of(nextValueHolder.value()), b))));
        }
    }

    @Override
    public <U> Stream<Pair<T, List<U>>> joinByRange(final Stream<U> b, final BiPredicate<? super T, ? super U> predicate)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "Stream 'b' cannot be null");
        checkArgNotNull(predicate, "'predicate' cannot be null");

        if (N.isEmpty(b.closeHandlers())) {
            return joinByRange(b.iteratorEx(), predicate);
        } else {
            return joinByRange(b.iteratorEx(), predicate).onClose(newCloseHandler(b));
        }
    }

    @Override
    public <U, R> Stream<Pair<T, R>> joinByRange(final Stream<U> b, final BiPredicate<? super T, ? super U> predicate,
            final Collector<? super U, ?, R> collector) {
        return joinByRange(b, predicate, collector, Fn.pair());
    }

    @Override
    public <U, D, R> Stream<R> joinByRange(final Stream<U> b, final BiPredicate<? super T, ? super U> predicate, final Collector<? super U, ?, D> collector,
            final BiFunction<? super T, ? super D, ? extends R> func) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "Stream 'b' cannot be null");
        checkArgNotNull(predicate, "'predicate' cannot be null");
        checkArgNotNull(collector, "'collector' cannot be null");
        checkArgNotNull(func, "'func' cannot be null");

        if (N.isEmpty(b.closeHandlers())) {
            return joinByRange(b.iteratorEx(), predicate, collector, func);
        } else {
            return ((Stream<R>) joinByRange(b.iteratorEx(), predicate, collector, func)).onClose(newCloseHandler(b));
        }
    }

    @Override
    public <U, D, R> Stream<R> joinByRange(final Stream<U> b, final BiPredicate<? super T, ? super U> predicate, final Collector<? super U, ?, D> collector,
            final BiFunction<? super T, ? super D, ? extends R> func, final Function<Iterator<U>, Stream<R>> mapperForUnJoinedElements)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(b, "Stream 'b' cannot be null");
        checkArgNotNull(predicate, "'predicate' cannot be null");
        checkArgNotNull(collector, "'collector' cannot be null");
        checkArgNotNull(func, "'func' cannot be null");
        checkArgNotNull(mapperForUnJoinedElements, "'mapperForUnJoinedElements' cannot be null");

        if (N.isEmpty(b.closeHandlers())) {
            return joinByRange(b.iteratorEx(), predicate, collector, func, mapperForUnJoinedElements);
        } else {
            return joinByRange(b.iteratorEx(), predicate, collector, func, mapperForUnJoinedElements).onClose(newCloseHandler(b));
        }
    }

    @Override
    public ObjIterator<T> iterator() throws IllegalStateException {
        assertNotClosed();

        if (!isEmptyCloseHandlers(closeHandlers()) && logger.isWarnEnabled()) {
            logger.warn("Remember to close {} after iteration because it has close handlers", ClassUtil.getSimpleClassName(getClass()));
        }

        return iteratorEx();
    }

}
