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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.BiFunctions;
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
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableList;
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
     * @param comparator the comparator for ordering elements, or {@code null} if using natural order
     * @param closeHandlers collection of handlers to execute when the stream is closed
     */
    AbstractStream(final boolean sorted, final Comparator<? super T> comparator, final Collection<LocalRunnable> closeHandlers) {
        super(sorted, comparator, closeHandlers);
    }

    @Override
    public <U> Stream<U> select(final Class<? extends U> targetType) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(targetType, cs.targetType);

        if (isParallel()) {
            //noinspection resource
            return (Stream<U>) sequential().filter(Fn.instanceOf(targetType))
                    .parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return (Stream<U>) filter(Fn.instanceOf(targetType));
        }
    }

    @Override
    public <U> Stream<Pair<T, U>> pairWith(final Function<? super T, ? extends U> extractor) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(extractor, cs.extractor);

        return map(t -> Pair.of(t, extractor.apply(t)));
    }

    @Override
    public Stream<T> skipUntil(final Predicate<? super T> predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        return dropWhile(Fn.not(predicate));
    }

    @Override
    public Stream<T> filter(final Predicate<? super T> predicate, final Consumer<? super T> onDrop) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(onDrop, cs.onDrop);

        return filter(value -> {
            if (!predicate.test(value)) {
                onDrop.accept(value);
                return false;
            }

            return true;
        });
    }

    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate, final Consumer<? super T> onDrop) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(onDrop, cs.onDrop);

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
            // Same shape as Seq.step: the gap is skipped on the way *in* to the next element, not on the way out of
            // the previous one. Skipping eagerly inside next() made step(n).first() pull n source elements (visible
            // through an upstream peek/onEach, wasteful for resource-backed or generated sources, and able to throw
            // for a gap element that is never needed).
            private long remainingGap = 0;

            @Override
            public boolean hasNext() {
                skipGapIfNeeded();

                return iter.hasNext();
            }

            @Override
            public T next() throws NoSuchElementException {
                skipGapIfNeeded();

                final T next = iter.next();
                remainingGap = skip;
                return next;
            }

            private void skipGapIfNeeded() {
                if (remainingGap > 0) {
                    final long gap = remainingGap;

                    if (!iter.supportsFailureAtomicAdvance()) {
                        // A failing non-atomic advance leaves an unknown position: never re-skip the gap on a retry.
                        remainingGap = 0;
                    }

                    // Bulk advance, never element by element: upstream range/skip/array-backed/step iterators skip the
                    // gap without reading the skipped elements, exactly as the former eager advance(skip) did. (A map()
                    // iterator keeps the default advance(), a next() loop, so it still maps what it skips.)
                    iter.advance(gap);
                    remainingGap = 0;
                }
            }
        };

        return newStream(iterator, isSorted(), comparator());
    }

    @Override
    public <R> Stream<R> slidingMap(final BiFunction<? super T, ? super T, ? extends R> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return slidingMap(1, mapper);
    }

    @Override
    public <R> Stream<R> slidingMap(final int increment, final BiFunction<? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgPositive(increment, cs.increment);
        checkArgNotNull(mapper, cs.mapper);

        return slidingMap(increment, false, mapper);
    }

    @Override
    public <R> Stream<R> slidingMap(final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return slidingMap(1, mapper);
    }

    @Override
    public <R> Stream<R> slidingMap(final int increment, final TriFunction<? super T, ? super T, ? super T, ? extends R> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgPositive(increment, cs.increment);
        checkArgNotNull(mapper, cs.mapper);

        return slidingMap(increment, false, mapper);
    }

    @Override
    public <R> Stream<R> mapIfNotNull(final Function<? super T, ? extends R> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        //noinspection resource
        return skipNulls().map(mapper);
    }

    @Override
    public <K, V> EntryStream<K, V> mapToEntry(final Function<? super T, ? extends Map.Entry<? extends K, ? extends V>> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Function<T, T> secondMapper = Fn.identity();

        if (mapper == secondMapper) {
            return EntryStream.of((Stream<Map.Entry<K, V>>) this);
        }

        return EntryStream.of(map(mapper));
    }

    @Override
    public <K, V> EntryStream<K, V> mapToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);

        final Function<T, Map.Entry<K, V>> mapper = t -> new SimpleImmutableEntry<>(keyMapper.apply(t), valueMapper.apply(t));

        return mapToEntry(mapper);
    }

    @Override
    public <R> Stream<R> flattMap(final Function<? super T, ? extends java.util.stream.Stream<? extends R>> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMap(t -> Stream.from(mapper.apply(t)));
    }

    @Override
    public CharStream flatMapArrayToChar(final Function<? super T, char[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToChar(t -> CharStream.of(mapper.apply(t)));
    }

    @Override
    public CharStream flatmapToChar(final Function<? super T, ? extends Collection<Character>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToChar(t -> CharStream.of(mapper.apply(t)));
    }

    @Override
    public ByteStream flatMapArrayToByte(final Function<? super T, byte[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToByte(t -> ByteStream.of(mapper.apply(t)));
    }

    @Override
    public ByteStream flatmapToByte(final Function<? super T, ? extends Collection<Byte>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToByte(t -> ByteStream.of(mapper.apply(t)));
    }

    @Override
    public ShortStream flatMapArrayToShort(final Function<? super T, short[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToShort(t -> ShortStream.of(mapper.apply(t)));
    }

    @Override
    public ShortStream flatmapToShort(final Function<? super T, ? extends Collection<Short>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToShort(t -> ShortStream.of(mapper.apply(t)));
    }

    @Override
    public IntStream flatMapArrayToInt(final Function<? super T, int[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToInt(t -> IntStream.of(mapper.apply(t)));
    }

    @Override
    public IntStream flatmapToInt(final Function<? super T, ? extends Collection<Integer>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToInt(t -> IntStream.of(mapper.apply(t)));
    }

    @Override
    public LongStream flatMapArrayToLong(final Function<? super T, long[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToLong(t -> LongStream.of(mapper.apply(t)));
    }

    @Override
    public LongStream flatmapToLong(final Function<? super T, ? extends Collection<Long>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToLong(t -> LongStream.of(mapper.apply(t)));
    }

    @Override
    public FloatStream flatMapArrayToFloat(final Function<? super T, float[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToFloat(t -> FloatStream.of(mapper.apply(t)));
    }

    @Override
    public FloatStream flatmapToFloat(final Function<? super T, ? extends Collection<Float>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToFloat(t -> FloatStream.of(mapper.apply(t)));
    }

    @Override
    public DoubleStream flatMapArrayToDouble(final Function<? super T, double[]> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToDouble(t -> DoubleStream.of(mapper.apply(t)));
    }

    @Override
    public DoubleStream flatmapToDouble(final Function<? super T, ? extends Collection<Double>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return flatMapToDouble(t -> DoubleStream.of(mapper.apply(t)));
    }

    @Override
    public <R> Stream<R> flatmapIfNotNull(final Function<? super T, ? extends Collection<? extends R>> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        //noinspection resource
        return skipNulls().flatmap(mapper);
    }

    @Override
    public <U, R> Stream<R> flatmapIfNotNull(final Function<? super T, ? extends Collection<? extends U>> mapper,
            final Function<? super U, ? extends Collection<? extends R>> secondMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);
        checkArgNotNull(secondMapper, cs.secondMapper);

        //noinspection resource
        return skipNulls().flatmap(mapper).skipNulls().flatmap(secondMapper);
    }

    @Override
    public <K, V> EntryStream<K, V> flatMapToEntry(final Function<? super T, ? extends Stream<? extends Map.Entry<? extends K, ? extends V>>> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return EntryStream.of(flatMap(mapper));
    }

    @Override
    public <K, V> EntryStream<K, V> flatmapToEntry(final Function<? super T, ? extends Map<? extends K, ? extends V>> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Function<? super T, ? extends Collection<Entry<K, V>>> secondMapper = t -> N.nullToEmpty((Map<K, V>) mapper.apply(t)).entrySet();

        //noinspection resource
        return flatmap(secondMapper).mapToEntry(Fn.identity());
    }

    @Override
    public <K, V> EntryStream<K, V> flattMapToEntry(final Function<? super T, ? extends EntryStream<? extends K, ? extends V>> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Function<? super T, ? extends Stream<? extends Entry<? extends K, ? extends V>>> secondMapper = t -> {
            final EntryStream<? extends K, ? extends V> s = mapper.apply(t);

            return s == null ? Stream.empty() : s.entries();
        };

        return flatMapToEntry(secondMapper);
    }

    @Override
    public <R> Stream<R> mapMulti(final BiConsumer<? super T, ? super Consumer<R>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final boolean isParallel = isParallel();

        if (isParallel) {
            final Function<T, Collection<R>> secondMapper = t -> {
                final SpinedBuffer<R> buffer = new SpinedBuffer<>();

                mapper.accept(t, buffer);

                return buffer;
            };

            return flatmap(secondMapper);
        } else {
            // A FIFO buffer read through a cursor: an ArrayList (not ArrayDeque) so that a mapper emitting a null element
            // does not throw NPE, matching the parallel path above (SpinedBuffer permits null) and JDK Stream.mapMulti,
            // and not a LinkedList, which allocated a node for every emitted element. The pending elements are
            // buffer[cursor, size); a delivered slot is cleared at once, and the drained buffer is reset before the
            // next source element is mapped.
            final List<R> buffer = new ArrayList<>();

            final Consumer<R> consumer = buffer::add;

            @SuppressWarnings("resource")
            final ObjIteratorEx<T> iter = iteratorEx();

            return newStream(new ObjIteratorEx<>() { //NOSONAR
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    if (cursor >= buffer.size()) {
                        if (cursor > 0) {
                            buffer.clear();
                            cursor = 0;
                        }

                        while (iter.hasNext()) {
                            mapper.accept(iter.next(), consumer);

                            if (buffer.size() > 0) {
                                break;
                            }
                        }
                    }

                    return cursor < buffer.size();
                }

                @Override
                public R next() throws NoSuchElementException {
                    if (cursor >= buffer.size() && !hasNext()) {
                        throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return buffer.set(cursor++, null);
                }
            }, false, null);
        }
    }

    @Override
    public IntStream mapMultiToInt(final BiConsumer<? super T, ? super IntConsumer> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Function<T, IntStream> secondMapper = t -> {
            final SpinedBuffer.OfInt buffer = new SpinedBuffer.OfInt();

            mapper.accept(t, buffer);

            return IntStream.of(buffer.iterator());
        };

        return flatMapToInt(secondMapper);
    }

    @Override
    public LongStream mapMultiToLong(final BiConsumer<? super T, ? super LongConsumer> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Function<T, LongStream> secondMapper = t -> {
            final SpinedBuffer.OfLong buffer = new SpinedBuffer.OfLong();

            mapper.accept(t, buffer);

            return LongStream.of(buffer.iterator());
        };

        return flatMapToLong(secondMapper);
    }

    @Override
    public DoubleStream mapMultiToDouble(final BiConsumer<? super T, ? super DoubleConsumer> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Function<T, DoubleStream> secondMapper = t -> {
            final SpinedBuffer.OfDouble buffer = new SpinedBuffer.OfDouble();

            mapper.accept(t, buffer);

            return DoubleStream.of(buffer.iterator());
        };

        return flatMapToDouble(secondMapper);
    }

    /**
     * Rejects a {@code null} {@code Optional} returned by a {@code mapPartial*} mapper with a
     * {@link NullPointerException} that names the cause (the type stays NPE, as for any other null dereference of a
     * mapper result). Same as {@code requireNonNullOptional(opt, "Optional")}.
     *
     * @param <O> the optional type
     * @param opt the value returned by the mapper
     * @return {@code opt}
     * @throws NullPointerException if {@code opt} is {@code null}
     */
    static <O> O requireNonNullOptional(final O opt) throws NullPointerException {
        return requireNonNullOptional(opt, "Optional");
    }

    /**
     * Rejects a {@code null} optional returned by a {@code mapPartial*} mapper with a {@link NullPointerException}
     * whose message names the optional type the mapper should have returned, e.g. {@code "mapper returned a null
     * OptionalInt; return OptionalInt.empty() for no result"}.
     *
     * @param <O> the optional type
     * @param opt the value returned by the mapper
     * @param optionalTypeName the simple (or qualified, for {@code java.util} types) name of the mapper's optional
     *        return type, such as {@code "OptionalInt"} or {@code "java.util.OptionalInt"}
     * @return {@code opt}
     * @throws NullPointerException if {@code opt} is {@code null}
     */
    static <O> O requireNonNullOptional(final O opt, final String optionalTypeName) throws NullPointerException {
        if (opt == null) {
            throw new NullPointerException("mapper returned a null " + optionalTypeName + "; return " + optionalTypeName + ".empty() for no result");
        }

        return opt;
    }

    @Override
    public <R> Stream<R> mapPartial(final Function<? super T, Optional<R>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Predicate<Optional<R>> predicate = o -> requireNonNullOptional(o).isPresent();
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
    public IntStream mapPartialToInt(final Function<? super T, OptionalInt> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Predicate<OptionalInt> isPresent = o -> requireNonNullOptional(o, "OptionalInt").isPresent();

        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(isPresent).mapToInt(Fn.GET_AS_INT));
        } else {
            //noinspection resource
            return map(mapper).filter(isPresent).mapToInt(Fn.GET_AS_INT);
        }
    }

    @Override
    public LongStream mapPartialToLong(final Function<? super T, OptionalLong> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Predicate<OptionalLong> isPresent = o -> requireNonNullOptional(o, "OptionalLong").isPresent();

        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(isPresent).mapToLong(Fn.GET_AS_LONG));
        } else {
            //noinspection resource
            return map(mapper).filter(isPresent).mapToLong(Fn.GET_AS_LONG);
        }
    }

    @Override
    public DoubleStream mapPartialToDouble(final Function<? super T, OptionalDouble> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Predicate<OptionalDouble> isPresent = o -> requireNonNullOptional(o, "OptionalDouble").isPresent();

        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(isPresent).mapToDouble(Fn.GET_AS_DOUBLE));
        } else {
            //noinspection resource
            return map(mapper).filter(isPresent).mapToDouble(Fn.GET_AS_DOUBLE);
        }
    }

    @Override
    public <R> Stream<R> mapPartialJdk(final Function<? super T, java.util.Optional<R>> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Predicate<java.util.Optional<R>> isPresent = o -> requireNonNullOptional(o, "java.util.Optional").isPresent();

        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(isPresent).map(Fn.getIfPresentOrElseNullJdk()));
        } else {
            //noinspection resource
            return map(mapper).filter(isPresent).map(Fn.getIfPresentOrElseNullJdk());
        }
    }

    @Override
    public IntStream mapPartialToIntJdk(final Function<? super T, java.util.OptionalInt> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Predicate<java.util.OptionalInt> isPresent = o -> requireNonNullOptional(o, "java.util.OptionalInt").isPresent();

        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(isPresent).mapToInt(Fn.GET_AS_INT_JDK));
        } else {
            //noinspection resource
            return map(mapper).filter(isPresent).mapToInt(Fn.GET_AS_INT_JDK);
        }
    }

    @Override
    public LongStream mapPartialToLongJdk(final Function<? super T, java.util.OptionalLong> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Predicate<java.util.OptionalLong> isPresent = o -> requireNonNullOptional(o, "java.util.OptionalLong").isPresent();

        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(isPresent).mapToLong(Fn.GET_AS_LONG_JDK));
        } else {
            //noinspection resource
            return map(mapper).filter(isPresent).mapToLong(Fn.GET_AS_LONG_JDK);
        }
    }

    @Override
    public DoubleStream mapPartialToDoubleJdk(final Function<? super T, java.util.OptionalDouble> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        final Predicate<java.util.OptionalDouble> isPresent = o -> requireNonNullOptional(o, "java.util.OptionalDouble").isPresent();

        if (isParallel()) {
            //noinspection resource
            return map(mapper).psp(s -> s.filter(isPresent).mapToDouble(Fn.GET_AS_DOUBLE_JDK));
        } else {
            //noinspection resource
            return map(mapper).filter(isPresent).mapToDouble(Fn.GET_AS_DOUBLE_JDK);
        }
    }

    @Override
    public <U> Stream<U> rangeMap(final BiPredicate<? super T, ? super T> sameRange, final BiFunction<? super T, ? super T, ? extends U> mapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(sameRange, cs.sameRange);
        checkArgNotNull(mapper, cs.mapper);

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private T left = null, right = null, next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                left = hasNext ? next : iter.next();
                right = left;

                hasNext = false;

                while (iter.hasNext()) {
                    next = iter.next();
                    hasNext = true;

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return mapper.apply(left, right);
            }
        }, false, null);
    }

    @Override
    public Stream<List<T>> collapse(final BiPredicate<? super T, ? super T> collapsible) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);

        return collapse(collapsible, Suppliers.ofList());
    }

    @Override
    public <C extends Collection<T>> Stream<C> collapse(final BiPredicate<? super T, ? super T> collapsible, final Supplier<? extends C> supplier)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);
        checkArgNotNull(supplier, cs.supplier);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private final ObjIteratorEx<T> iter = iteratorEx();
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public C next() throws NoSuchElementException {
                final C c = N.requireNonNull(supplier.get(), "supplier returned null");
                c.add(hasNext ? next : (next = iter.next()));

                hasNext = false;

                while (iter.hasNext()) {
                    final T previous = next;
                    next = iter.next();
                    hasNext = true;

                    if (collapsible.test(previous, next)) {
                        c.add(next);
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return c;
            }
        }, false, null);
    }

    @Override
    public Stream<T> collapse(final BiPredicate<? super T, ? super T> collapsible, final BinaryOperator<T> mergeFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);
        checkArgNotNull(mergeFunction, cs.mergeFunction);

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() throws NoSuchElementException {
                T res = hasNext ? next : (next = iter.next());

                hasNext = false;

                while (iter.hasNext()) {
                    final T previous = next;
                    next = iter.next();
                    hasNext = true;

                    if (collapsible.test(previous, next)) {
                        res = mergeFunction.apply(res, next);
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> collapse(final BiPredicate<? super T, ? super T> collapsible, final U init, final BiFunction<? super U, ? super T, U> operator)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);
        checkArgNotNull(operator, cs.mergeFunction); // the public parameter is named mergeFunction

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() throws NoSuchElementException {
                U res = operator.apply(init, hasNext ? next : (next = iter.next()));

                hasNext = false;

                while (iter.hasNext()) {
                    final T previous = next;
                    next = iter.next();
                    hasNext = true;

                    if (collapsible.test(previous, next)) {
                        res = operator.apply(res, next);
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> collapse(final BiPredicate<? super T, ? super T> collapsible, final Collector<? super T, ?, R> collector)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);
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
            public R next() throws NoSuchElementException {
                final Object c = supplier.get();
                accumulator.accept(c, hasNext ? next : (next = iter.next()));

                hasNext = false;

                while (iter.hasNext()) {
                    final T previous = next;
                    next = iter.next();
                    hasNext = true;

                    if (collapsible.test(previous, next)) {
                        accumulator.accept(c, next);
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return finisher.apply(c);
            }
        }, false, null);
    }

    @Override
    public Stream<List<T>> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);

        return collapse(collapsible, Suppliers.ofList());
    }

    @Override
    public <C extends Collection<T>> Stream<C> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible, final Supplier<? extends C> supplier)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);
        checkArgNotNull(supplier, cs.supplier);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private final ObjIteratorEx<T> iter = iteratorEx();
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public C next() throws NoSuchElementException {
                final T first = hasNext ? next : (next = iter.next());
                final C c = N.requireNonNull(supplier.get(), "supplier returned null");
                c.add(first);

                hasNext = false;

                while (iter.hasNext()) {
                    final T previous = next;
                    next = iter.next();
                    hasNext = true;

                    if (collapsible.test(first, previous, next)) {
                        c.add(next);
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return c;
            }
        }, false, null);
    }

    @Override
    public Stream<T> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible, final BinaryOperator<T> mergeFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);
        checkArgNotNull(mergeFunction, cs.mergeFunction);

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() throws NoSuchElementException {
                final T first = hasNext ? next : (next = iter.next());
                T res = first;

                hasNext = false;

                while (iter.hasNext()) {
                    final T previous = next;
                    next = iter.next();
                    hasNext = true;

                    if (collapsible.test(first, previous, next)) {
                        res = mergeFunction.apply(res, next);
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible, final U init,
            final BiFunction<? super U, ? super T, U> operator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);
        checkArgNotNull(operator, cs.mergeFunction); // the public parameter is named mergeFunction

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() throws NoSuchElementException {
                final T first = hasNext ? next : (next = iter.next());
                U res = operator.apply(init, first);

                hasNext = false;

                while (iter.hasNext()) {
                    final T previous = next;
                    next = iter.next();
                    hasNext = true;

                    if (collapsible.test(first, previous, next)) {
                        res = operator.apply(res, next);
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> collapse(final TriPredicate<? super T, ? super T, ? super T> collapsible, final Collector<? super T, ?, R> collector)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collapsible, cs.collapsible);
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
            public R next() throws NoSuchElementException {
                final T first = hasNext ? next : (next = iter.next());
                final Object c = supplier.get();
                accumulator.accept(c, first);

                hasNext = false;

                while (iter.hasNext()) {
                    final T previous = next;
                    next = iter.next();
                    hasNext = true;

                    if (collapsible.test(first, previous, next)) {
                        accumulator.accept(c, next);
                    } else {
                        break;
                    }

                    hasNext = false;
                }

                return finisher.apply(c);
            }
        }, false, null);
    }

    @Override
    public Stream<T> scan(final BinaryOperator<T> accumulator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private T res = null;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() throws NoSuchElementException {
                if (isFirst) {
                    res = iter.next();
                    isFirst = false;
                    return res;
                } else {
                    return (res = accumulator.apply(res, iter.next()));
                }
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> scan(final U init, final BiFunction<? super U, ? super T, U> accumulator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private U res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public U next() throws NoSuchElementException {
                return (res = accumulator.apply(res, iter.next()));
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> scan(final U init, final boolean initIncluded, final BiFunction<? super U, ? super T, U> accumulator)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(accumulator, cs.accumulator);

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
            public U next() throws NoSuchElementException {
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
    public Stream<List<T>> split(final int chunkSize) throws IllegalStateException, IllegalArgumentException {
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
    public Stream<List<T>> split(final Predicate<? super T> predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

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
        final MutableBoolean sourceCloseHandlersTransferred = MutableBoolean.of(false);
        final Deque<LocalRunnable> sourceCloseHandlers = closeHandlersForNewStream();
        final Deque<LocalRunnable> outerCloseHandlers = new LinkedList<>();
        outerCloseHandlers.add(() -> {
            if (!sourceCloseHandlersTransferred.value()) {
                close(sourceCloseHandlers);
            }
        });

        return newStreamWithTransferredCloseHandlers(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public Stream<T> next() throws NoSuchElementException {
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

                    result = newStreamWithTransferredCloseHandlers(StreamBase.toArray(list), 0, list.size(), isSorted(), comparator(), null);
                } else {
                    result = newStream(iter, isSorted(), comparator(), sourceCloseHandlers);
                    sourceCloseHandlersTransferred.setTrue();
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                if (cursor >= 2) {
                    return 0;
                }

                iter.count();

                final long ret = 2 - cursor; //NOSONAR
                cursor = 2;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0 || cursor >= 2) {
                    return;
                } else if ((n == 1) && (cursor == 0)) {
                    iter.advance(position);
                } else {
                    iter.advance(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }
        }, false, null, outerCloseHandlers);
    }

    @Override
    public <R> Stream<R> splitAt(final int position, final Collector<? super T, ?, R> collector) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNegative(position, cs.position);
        checkArgNotNull(collector, cs.collector);

        return collectParts(splitAt(position), collector);
    }

    /**
     * Collects each of the two parts produced by {@code splitAt} with {@code collector}, keeping the positional
     * {@code [head, tail]} result. On a parallel stream the outer map and each part's collect run sequentially:
     * a parallel outer map could emit the tail before the head, and a parallel collect scrambles the order of the
     * elements inside each part - both contradict the "sequential only" contract of the Collector overloads. The
     * 2-element result is then switched back to this stream's parallel settings, as {@link #select(Class)} does.
     *
     * @param <R> the collector result type
     * @param parts the two part streams
     * @param collector collects each part
     * @return a stream of the two collected parts, in order
     */
    private <R> Stream<R> collectParts(final Stream<Stream<T>> parts, final Collector<? super T, ?, R> collector) {
        if (isParallel()) {
            //noinspection resource
            return parts.sequential()
                    .map(s -> s.sequential().collect(collector))
                    .parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return parts.map(s -> s.collect(collector));
        }
    }

    @Override
    public Stream<Stream<T>> splitAt(final Predicate<? super T> where) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(where, cs.where);

        final IteratorEx<T> iter = iteratorEx();
        final MutableBoolean sourceCloseHandlersTransferred = MutableBoolean.of(false);
        final Deque<LocalRunnable> sourceCloseHandlers = closeHandlersForNewStream();
        final Deque<LocalRunnable> outerCloseHandlers = new LinkedList<>();
        outerCloseHandlers.add(() -> {
            if (!sourceCloseHandlersTransferred.value()) {
                close(sourceCloseHandlers);
            }
        });

        return newStreamWithTransferredCloseHandlers(new ObjIteratorEx<>() { //NOSONAR
            private int cursor = 0;
            private T next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public Stream<T> next() throws NoSuchElementException {
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

                    result = newStreamWithTransferredCloseHandlers(StreamBase.toArray(list), 0, list.size(), isSorted(), comparator(), null);
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
                            public T next() throws NoSuchElementException {
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

                    result = newStreamWithTransferredCloseHandlers(iterEx, isSorted(), comparator(), sourceCloseHandlers);
                    sourceCloseHandlersTransferred.setTrue();
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                if (cursor >= 2) {
                    return 0;
                }

                iter.count();

                final long ret = 2 - cursor; //NOSONAR
                cursor = 2;
                return ret;
            }

            @Override
            public void advance(final long n) {
                if (n <= 0 || cursor >= 2) {
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

        }, false, null, outerCloseHandlers);
    }

    @Override
    public <R> Stream<R> splitAt(final Predicate<? super T> where, final Collector<? super T, ?, R> collector)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(where, cs.where);
        checkArgNotNull(collector, cs.collector);

        return collectParts(splitAt(where), collector);
    }

    //    @Override
    //    public Stream<Stream<T>> sliding(final int windowSize, final int increment) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return slidingToList(windowSize, increment).map(listToStreamMapper());
    //    }

    @Override
    public Stream<List<T>> sliding(final int windowSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return sliding(windowSize, IntFunctions.ofList());
    }

    @Override
    public Stream<List<T>> sliding(final int windowSize, final int increment) throws IllegalStateException, IllegalArgumentException {
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
    public <C extends Collection<T>> Stream<C> sliding(final int windowSize, final IntFunction<? extends C> collectionSupplier)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return sliding(windowSize, 1, collectionSupplier);
    }

    @Override
    public <R> Stream<R> sliding(final int windowSize, final Collector<? super T, ?, R> collector) throws IllegalStateException, IllegalArgumentException {
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
            public T next() throws NoSuchElementException {
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
    public Stream<T> onFirst(final Consumer<? super T> action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        final Function<? super T, ? extends T> mapperForFirst = t -> {
            action.accept(t);
            return t;
        };

        return mapFirst(mapperForFirst);
    }

    @Override
    public Stream<T> onLast(final Consumer<? super T> action) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        final Function<? super T, ? extends T> mapperForLast = t -> {
            action.accept(t);
            return t;
        };

        return mapLast(mapperForLast);
    }

    @Override
    public <E extends Exception> void forEach(final Throwables.Consumer<? super T, E> action) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        forEach(action, Fn.emptyAction());
    }

    @Override
    public <E extends Exception> void forEachIndexed(final Throwables.IntObjConsumer<? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        if (isParallel()) {
            final AtomicInteger idx = new AtomicInteger(0);

            forEach(t -> action.accept(idx.getAndIncrement(), t));
        } else {
            final MutableInt idx = MutableInt.of(0);

            forEach(t -> action.accept(idx.getAndIncrement(), t));
        }
    }

    @Override
    public <E extends Exception> void forEachUntil(final Throwables.BiConsumer<? super T, MutableBoolean, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        final MutableBoolean flagToBreak = MutableBoolean.of(false);

        final Throwables.Consumer<? super T, E> tmp = t -> action.accept(t, flagToBreak);

        if (isParallel()) {
            this.psp(s -> s.takeWhile(value -> flagToBreak.isFalse())).forEach(tmp);
        } else {
            forEachUntilSequentially(flagToBreak, tmp);
        }
    }

    @Override
    public <E extends Exception> void forEachUntil(final MutableBoolean flagToBreak, final Throwables.Consumer<? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(flagToBreak, cs.flagToBreak);
        checkArgNotNull(action, cs.action);

        if (isParallel()) {
            if (flagToBreak.isTrue()) {
                close();
            } else {
                this.psp(s -> s.takeWhile(value -> flagToBreak.isFalse())).forEach(action);
            }
        } else {
            forEachUntilSequentially(flagToBreak, action);
        }
    }

    /**
     * Runs {@code action} for each element while {@code flagToBreak} is {@code false}, consuming from the
     * source exactly the elements it delivers.
     *
     * <p>{@code takeWhile(v -> flagToBreak.isFalse())} cannot be used for this: {@code takeWhile.hasNext()}
     * has to pull an element before it can test the predicate, so it reads one element past the stop point,
     * and reads one element even when the flag is already {@code true} - which contradicts the documented
     * guarantee that a flag set at the start iterates nothing. {@code Seq.forEachUntil} uses this same plain
     * loop for exactly this reason.
     *
     * @throws E if {@code action} throws while processing an element
     */
    private <E extends Exception> void forEachUntilSequentially(final MutableBoolean flagToBreak, final Throwables.Consumer<? super T, E> action) throws E {
        try {
            final ObjIteratorEx<T> iter = iteratorEx();

            while (flagToBreak.isFalse() && iter.hasNext()) {
                action.accept(iter.next());
            }
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> void forEachPair(final Throwables.BiConsumer<? super T, ? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        forEachPair(1, action);
    }

    @Override
    public <E extends Exception> void forEachTriple(final Throwables.TriConsumer<? super T, ? super T, ? super T, E> action)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(action, cs.action);

        forEachTriple(1, action);
    }

    @Override
    public <K> Stream<Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        return groupBy(keyMapper, Suppliers.ofMap());
    }

    @Override
    public <K> Stream<Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> keyMapper, final Supplier<? extends Map<K, List<T>>> mapFactory)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return groupBy(keyMapper, Fn.identity(), mapFactory);
    }

    @Override
    public <K, V> Stream<Entry<K, List<V>>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);

        return groupBy(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V> Stream<Map.Entry<K, List<V>>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Supplier<? extends Map<K, List<V>>> mapFactory) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return groupBy(keyMapper, valueMapper, Collectors.toList(), mapFactory);
    }

    @Override
    public <K, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);

        return groupBy(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, ?, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return groupBy(keyMapper, Fn.identity(), downstream, mapFactory);
    }

    @Override
    public <K, V, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, ?, D> downstream) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);

        return groupBy(keyMapper, valueMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, V, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, ?, D> downstream, final Supplier<? extends Map<K, D>> mapFactory)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

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
            public Entry<K, D> next() throws NoSuchElementException {
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
            final BinaryOperator<V> mergeFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);

        return groupBy(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, V> Stream<Entry<K, V>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<Entry<K, V>> iter = null;

            @Override
            public boolean hasNext() {
                init();
                return iter.hasNext();
            }

            @Override
            public Entry<K, V> next() throws NoSuchElementException {
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
    public Stream<Entry<Boolean, List<T>>> partitionBy(final Predicate<? super T> predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        return partitionBy(predicate, Collectors.toList());
    }

    @Override
    public <D> Stream<Entry<Boolean, D>> partitionBy(final Predicate<? super T> predicate, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(downstream, cs.downstream);

        return newStream(new ObjIteratorEx<>() { //NOSONAR
            private Iterator<Entry<Boolean, D>> iter = null;

            @Override
            public boolean hasNext() {
                init();
                return iter.hasNext();
            }

            @Override
            public Entry<Boolean, D> next() throws NoSuchElementException {
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
    public EntryStream<Boolean, List<T>> partitionByToEntry(final Predicate<? super T> predicate) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        return partitionByToEntry(predicate, Collectors.toList());
    }

    @Override
    public <D> EntryStream<Boolean, D> partitionByToEntry(final Predicate<? super T> predicate, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        //noinspection resource
        return partitionBy(predicate, downstream).mapToEntry(Fn.identity());
    }

    @Override
    public <K> EntryStream<K, List<T>> groupByToEntry(final Function<? super T, ? extends K> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        return groupByToEntry(keyMapper, Suppliers.ofMap());
    }

    @Override
    public <K> EntryStream<K, List<T>> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Supplier<? extends Map<K, List<T>>> mapFactory)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return groupByToEntry(keyMapper, Fn.identity(), mapFactory);
    }

    @Override
    public <K, V> EntryStream<K, List<V>> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);

        return groupByToEntry(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V> EntryStream<K, List<V>> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Supplier<? extends Map<K, List<V>>> mapFactory) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        //noinspection resource
        return groupBy(keyMapper, valueMapper, mapFactory).mapToEntry(Fn.identity());
    }

    @Override
    public <K, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);

        return groupByToEntry(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, ?, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        //noinspection resource
        return groupBy(keyMapper, downstream, mapFactory).mapToEntry(Fn.identity());
    }

    @Override
    public <K, V, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, ?, D> downstream) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);

        return groupByToEntry(keyMapper, valueMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, V, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, ?, D> downstream, final Supplier<? extends Map<K, D>> mapFactory)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        //noinspection resource
        return groupBy(keyMapper, valueMapper, downstream, mapFactory).mapToEntry(Fn.identity());
    }

    @Override
    public <K, V> EntryStream<K, V> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);

        return groupByToEntry(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, V> EntryStream<K, V> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(mapFactory, cs.mapFactory);

        //noinspection resource
        return groupBy(keyMapper, valueMapper, mergeFunction, mapFactory).mapToEntry(Fn.identity());
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);

        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>, E extends Exception, E2 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return toMap(keyMapper, valueMapper, Fn.throwingMerger(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final BinaryOperator<V> mergeFunction)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);

        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    @Override
    public <K, E extends Exception> Map<K, List<T>> groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        return groupTo(keyMapper, Suppliers.ofMap());
    }

    @Override
    public <K, M extends Map<K, List<T>>, E extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return groupTo(keyMapper, Collectors.toList(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, List<V>> groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);

        return groupTo(keyMapper, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, List<V>>, E extends Exception, E2 extends Exception> M groupTo(
            final Throwables.Function<? super T, ? extends K, E> keyMapper, final Throwables.Function<? super T, ? extends V, E2> valueMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return groupTo(keyMapper, valueMapper, Collectors.toList(), mapFactory);
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Collector<? super T, ?, D> downstream) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);

        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Collector<? super T, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return groupTo(keyMapper, Fn.identity(), downstream, mapFactory);
    }

    @Override
    public <K, V, D, E extends Exception, E2 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);

        return groupTo(keyMapper, valueMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, V, D, M extends Map<K, D>, E extends Exception, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        try {
            @SuppressWarnings("resource")
            final ObjIteratorEx<T> iter = iteratorEx();
            final M result = N.requireNonNull(mapFactory.get(), "mapFactory returned null");

            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super V> downstreamAccumulator = (BiConsumer<Object, ? super V>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final Map<K, Object> intermediate = (Map<K, Object>) result;
            K key = null;
            Object v = null;
            T next = null;

            while (iter.hasNext()) {
                next = iter.next();
                key = N.requireNonNull(keyMapper.apply(next), "element cannot be mapped to a null key");

                if ((v = intermediate.get(key)) == null) {
                    v = downstreamSupplier.get();
                    intermediate.put(key, v);
                }

                downstreamAccumulator.accept(v, valueMapper.apply(next));
            }

            final BiFunction<? super K, Object, Object> function = (k, v1) -> downstreamFinisher.apply(v1);

            Collectors.replaceAll(intermediate, function);

            return result;
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public <K, E extends Exception> Map<K, List<T>> flatGroupTo(final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(flatKeyExtractor, cs.flatKeyExtractor);

        return flatGroupTo(flatKeyExtractor, Suppliers.ofMap());
    }

    @Override
    public <K, M extends Map<K, List<T>>, E extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(flatKeyExtractor, cs.flatKeyExtractor);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return flatGroupTo(flatKeyExtractor, BiFunctions.selectSecond(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> Map<K, List<V>> flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(flatKeyExtractor, cs.flatKeyExtractor);
        checkArgNotNull(valueMapper, cs.valueMapper);

        return flatGroupTo(flatKeyExtractor, valueMapper, Suppliers.ofMap());
    }

    @Override
    public <K, V, M extends Map<K, List<V>>, E extends Exception, E2 extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(flatKeyExtractor, cs.flatKeyExtractor);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return flatGroupTo(flatKeyExtractor, valueMapper, Collectors.toList(), mapFactory);
    }

    @Override
    public <K, D, E extends Exception> Map<K, D> flatGroupTo(final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Collector<? super T, ?, D> downstream) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(flatKeyExtractor, cs.flatKeyExtractor);
        checkArgNotNull(downstream, cs.downstream);

        return flatGroupTo(flatKeyExtractor, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, D, M extends Map<K, D>, E extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor, final Collector<? super T, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(flatKeyExtractor, cs.flatKeyExtractor);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return flatGroupTo(flatKeyExtractor, BiFunctions.selectSecond(), downstream, mapFactory);
    }

    @Override
    public <K, V, D, E extends Exception, E2 extends Exception> Map<K, D> flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(flatKeyExtractor, cs.flatKeyExtractor);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);

        return flatGroupTo(flatKeyExtractor, valueMapper, downstream, Suppliers.ofMap());
    }

    @Override
    public <K, V, D, M extends Map<K, D>, E extends Exception, E2 extends Exception> M flatGroupTo(
            final Throwables.Function<? super T, ? extends Collection<? extends K>, E> flatKeyExtractor,
            final Throwables.BiFunction<? super K, ? super T, ? extends V, E2> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(flatKeyExtractor, cs.flatKeyExtractor);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

        try {
            @SuppressWarnings("resource")
            final ObjIteratorEx<T> iter = iteratorEx();
            final M result = N.requireNonNull(mapFactory.get(), "mapFactory returned null");

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
                        N.requireNonNull(k, "element cannot be mapped to a null key");

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
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public <E extends Exception> Map<Boolean, List<T>> partitionTo(final Throwables.Predicate<? super T, E> predicate)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

        return partitionTo(predicate, Collectors.toList());
    }

    @Override
    public <D, E extends Exception> Map<Boolean, D> partitionTo(final Throwables.Predicate<? super T, E> predicate, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(downstream, cs.downstream);

        final Throwables.Function<T, Boolean, E> keyMapper = predicate::test;

        final Supplier<Map<Boolean, D>> mapFactory = () -> N.newHashMap(2);

        final Map<Boolean, D> map = groupTo(keyMapper, downstream, mapFactory);

        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

        // Same contract as Seq.partitionTo: a LinkedHashMap that always iterates false-then-true, whatever
        // the encounter order of the two partitions (groupTo inserts keys in first-seen order).
        final Map<Boolean, D> result = new LinkedHashMap<>(4);

        result.put(Boolean.FALSE, map.containsKey(Boolean.FALSE) ? map.get(Boolean.FALSE) : downstreamFinisher.apply(downstreamSupplier.get()));
        result.put(Boolean.TRUE, map.containsKey(Boolean.TRUE) ? map.get(Boolean.TRUE) : downstreamFinisher.apply(downstreamSupplier.get()));

        return result;
    }

    @Override
    public <K, E extends Exception> ListMultimap<K, T> toMultimap(final Throwables.Function<? super T, ? extends K, E> keyMapper)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        return toMultimap(keyMapper, Suppliers.ofListMultimap());
    }

    @Override
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>, E extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E> keyMapper, final Supplier<? extends M> mapFactory)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

        return toMultimap(keyMapper, Fn.identity(), mapFactory);
    }

    @Override
    public <K, V, E extends Exception, E2 extends Exception> ListMultimap<K, V> toMultimap(final Throwables.Function<? super T, ? extends K, E> keyMapper,
            final Throwables.Function<? super T, ? extends V, E2> valueMapper) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);

        return toMultimap(keyMapper, valueMapper, Suppliers.ofListMultimap());
    }

    @Override
    public long sumInt(final ToIntFunction<? super T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return collect(Collectors.summingIntToLong(mapper));
    }

    @Override
    public long sumLong(final ToLongFunction<? super T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return collect(Collectors.summingLong(mapper));
    }

    @Override
    public double sumDouble(final ToDoubleFunction<? super T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return collect(Collectors.summingDouble(mapper));
    }

    @Override
    public OptionalDouble averageInt(final ToIntFunction<? super T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return collect(Collectors.averagingIntOrEmpty(mapper));
    }

    @Override
    public OptionalDouble averageLong(final ToLongFunction<? super T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        // Not Collectors.averagingLongOrEmpty: that collector keeps a plain long sum (documented JDK-compatible wrap),
        // while LongStream.average() is exact and overflow-safe, like Seq/N.averageLong - also under parallel().
        //noinspection resource
        return mapToLong(mapper).average();
    }

    @Override
    public OptionalDouble averageDouble(final ToDoubleFunction<? super T> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

        return collect(Collectors.averagingDoubleOrEmpty(mapper));
    }

    @Override
    public List<T> minAll(final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(comparator, cs.comparator);

        if (isParallel()) {
            return collect(Collectors.minAll(comparator));
        } else {
            try {
                @SuppressWarnings("resource")
                final ObjIteratorEx<T> iter = iteratorEx();
                List<T> result = new ArrayList<>();

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
                    final Comparator<? super T> cmp = comparator;
                    T next = null;
                    int cp = 0;

                    while (iter.hasNext()) {
                        next = iter.next();
                        cp = cmp.compare(next, candidate);

                        if (cp == 0) {
                            result.add(next);
                        } else if (cp < 0) {
                            if (result.size() > 16) {
                                // A long run of earlier ties may have grown the list: start a new one rather than keep that capacity.
                                result = new ArrayList<>();
                            } else {
                                result.clear();
                            }
                            result.add(next);
                            candidate = next;
                        }
                    }
                }

                return result;
            } catch (final Throwable e) {
                closeAfterFailure(e);
                throw e;
            } finally {
                close();
            }
        }
    }

    @Override
    public List<T> maxAll(final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(comparator, cs.comparator);

        if (isParallel()) {
            return collect(Collectors.maxAll(comparator));
        } else {
            try {
                @SuppressWarnings("resource")
                final ObjIteratorEx<T> iter = iteratorEx();
                List<T> result = new ArrayList<>();

                if (!iter.hasNext()) {
                    return result;
                }

                T candidate = iter.next();
                result.add(candidate);

                final Comparator<? super T> cmp = comparator;

                T next = null;
                int cp = 0;

                while (iter.hasNext()) {
                    next = iter.next();
                    cp = cmp.compare(next, candidate);

                    if (cp == 0) {
                        result.add(next);
                    } else if (cp > 0) {
                        if (result.size() > 16) {
                            // A long run of earlier ties may have grown the list: start a new one rather than keep that capacity.
                            result = new ArrayList<>();
                        } else {
                            result.clear();
                        }
                        result.add(next);
                        candidate = next;
                    }
                }

                return result;
            } catch (final Throwable e) {
                closeAfterFailure(e);
                throw e;
            } finally {
                close();
            }
        }
    }

    @Override
    public <E extends Exception> Optional<T> findAny(final Throwables.Predicate<? super T, E> predicate)
            throws IllegalStateException, IllegalArgumentException, E, NullPointerException {
        assertNotClosed();

        checkArgNotNull(predicate, cs.predicate);

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
            } else {
                return containsAll(N.toSet(a));
            }
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
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
                final Set<T> set = N.newHashSet(c);

                // Match using Object.equals even when stream distinctness has special handling for arrays.
                return sequential().anyMatch(value -> set.remove(value) && set.isEmpty());
            }
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
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
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
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
                final Set<T> set = N.newHashSet(c);

                return anyMatch(set::contains);
            }
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
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
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
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
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> first() throws IllegalStateException, NullPointerException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();

            if (!iter.hasNext()) {
                return Optional.empty();
            }

            return Optional.of(iter.next());
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> last() throws IllegalStateException, NullPointerException {
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
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> elementAt(final long position) throws IllegalStateException, IllegalArgumentException, NullPointerException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        try {
            if (position == 0) {
                return first();
            } else {
                //noinspection resource
                return skip(position).first();
            }
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @SuppressWarnings("DuplicateThrows")
    @Override
    public Optional<T> onlyOne() throws IllegalStateException, TooManyElementsException, NullPointerException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();

            if (!iter.hasNext()) {
                return Optional.empty();
            }

            // Check cardinality before wrapping: [null, x] must throw TooManyElementsException,
            // while a sole null element throws NullPointerException when wrapped below.
            final T first = iter.next();

            if (iter.hasNext()) {
                throw new TooManyElementsException("There are at least two elements: " + Strings.concat(N.toString(first), ", ", N.toString(iter.next())));
            }

            return Optional.of(first);
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> rateLimited(final RateLimiter rateLimiter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final Consumer<T> action = it -> rateLimiter.acquire();

        if (isParallel()) {
            //noinspection resource
            return sequential().onEach(action).parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public Stream<T> delay(final Duration duration) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(duration, cs.duration);

        final long millis = duration.toMillis();

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
            return sequential().onEach(action).parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return onEach(action);
        }
    }

    @Override
    public Stream<T> debounce(Duration duration) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(duration, cs.duration);
        checkArgPositive(duration.toMillis(), cs.duration);

        if (isParallel()) {
            // Debounce is inherently sequential and time-ordered; run it on the sequential view.
            return psp(s -> s.debounce(duration));
        } else {
            return newStream(new ObjIteratorEx<>() { //NOSONAR
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
                public T next() throws NoSuchElementException {
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
    public Stream<T> skipNulls() throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            //noinspection resource
            return sequential().filter(Fn.notNull()).parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return filter(Fn.notNull());
        }
    }

    @Override
    public Stream<T> skipRange(final int startInclusive, final int endExclusive) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(startInclusive, cs.startInclusive);
        checkArgNotNegative(endExclusive, cs.endExclusive);
        checkArgument(startInclusive <= endExclusive, "'startInclusive' (%s) must be <= 'endExclusive' (%s)", startInclusive, endExclusive);

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
                    while (iter.hasNext() && idx.value() < endExclusive) {
                        iter.next();
                        idx.increment();
                    }

                    skipped = true;
                }

                return iter.hasNext();
            }

            @Override
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T next = iter.next();
                idx.increment();

                return next;
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

        if (isParallel()) {
            // Like rateLimited/delay: the skip stage runs sequentially and the parallel settings are restored for the
            // downstream stages. A parallel dropWhile stage serialised every element under its lock anyway, and emitted
            // the retained elements in completion order.
            //noinspection resource
            return sequential().skip(n, action).parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        }

        final Predicate<T> filter = new Predicate<>() {
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
    public <U> Stream<T> intersection(final Function<? super T, ? extends U> mapper, final Collection<U> c)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

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
    public <U> Stream<T> difference(final Function<? super T, ? extends U> mapper, final Collection<U> c)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(mapper, cs.mapper);

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
            public T next() throws NoSuchElementException {
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
            public T next() throws NoSuchElementException {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return elements[(int) (((long) start + cnt++) % len) + fromIndex];
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

                if (remaining > 0 && a != elements && a.getClass().getComponentType().isAssignableFrom(elements.getClass().getComponentType())) {
                    // No element can fail the array-store check here (the usual case: toArray() passes an Object[]) and
                    // the target is not the source array, so copy the at most two contiguous runs of the backing range,
                    // [head, len) then [0, ...), in bulk.
                    final long first = (long) start + cnt;
                    final int head = (int) (first < len ? first : first - len);
                    final int headLength = Math.min(len - head, remaining);

                    System.arraycopy(elements, fromIndex + head, a, 0, headLength);
                    System.arraycopy(elements, fromIndex, a, headLength, remaining - headLength);
                } else {
                    // Element by element, so that a mistyped array fails with the usual ArrayStoreException.
                    for (int i = cnt; i < len; i++) {
                        a[i - cnt] = (A) elements[(int) (((long) start + i) % len) + fromIndex];
                    }
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
    public Stream<T> shuffled(final Random random) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(random, cs.random);

        return lazyLoad(a -> {
            N.shuffle(a, random);
            return a;
        }, false, null);
    }

    @Override
    public Stream<T> sorted() throws IllegalStateException {
        assertNotClosed();

        return sorted(NATURAL_COMPARATOR);
    }

    @Override
    public Stream<T> sorted(final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(comparator, cs.comparator);

        final Comparator<? super T> cmpToUse = comparator;

        // isSameComparator, like every other sorted shortcut: a boxed primitive stream is flagged sorted with its
        // primitive comparator (e.g. IntStream.range(..).boxed()), which orders exactly like NATURAL_COMPARATOR.
        if (isSorted() && isSameComparator(cmpToUse, comparator())) {
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
    public Stream<T> sortedBy(final Function<? super T, ? extends Comparable> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> comparator = Comparators.comparingBy(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByInt(final ToIntFunction<? super T> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> comparator = Comparators.comparingInt(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByLong(final ToLongFunction<? super T> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> comparator = Comparators.comparingLong(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByDouble(final ToDoubleFunction<? super T> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> comparator = Comparators.comparingDouble(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> reverseSorted() throws IllegalStateException {
        assertNotClosed();

        return sorted(REVERSED_COMPARATOR);
    }

    @Override
    public Stream<T> reverseSorted(final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(comparator, cs.comparator);

        final Comparator<? super T> cmpToUse = Comparators.reverseOrder(comparator);

        return sorted(cmpToUse);
    }

    @Override
    public Stream<T> reverseSortedByInt(final ToIntFunction<? super T> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> cmpToUse = Comparators.reversedComparingInt(keyMapper);

        return sorted(cmpToUse);
    }

    @Override
    public Stream<T> reverseSortedByLong(final ToLongFunction<? super T> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> cmpToUse = Comparators.reversedComparingLong(keyMapper);

        return sorted(cmpToUse);
    }

    @Override
    public Stream<T> reverseSortedByDouble(final ToDoubleFunction<? super T> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> cmpToUse = Comparators.reversedComparingDouble(keyMapper);

        return sorted(cmpToUse);
    }

    @Override
    public Stream<T> reverseSortedBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> cmpToUse = Comparators.reversedComparingBy(keyMapper);

        return sorted(cmpToUse);
    }

    private Stream<T> lazyLoad(final UnaryOperator<Object[]> operator, final boolean sorted, final Comparator<? super T> comparator) {
        // Preserve sorted/comparator on the outer stream. Stream.defer(...) wraps the supplier as a
        // plain unsorted iterator stream, which drops isSorted() and breaks sorted-aware ops (min/max
        // short-circuit, top window, redundant sorted() elision, etc.).
        return newStream(ObjIteratorEx.defer(() -> { //NOSONAR
            @SuppressWarnings("unchecked")
            final T[] a = (T[]) operator.apply(toArrayForIntermediateOp());
            return a == null || a.length == 0 ? ObjIteratorEx.<T> empty() : ObjIteratorEx.of(a);
        }), sorted, comparator);
    }

    @Override
    public Stream<T> distinctBy(final Function<? super T, ?> keyMapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(keyMapper, cs.keyMapper);

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
    public Stream<T> top(final int n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return top(n, NATURAL_COMPARATOR);
    }

    @Override
    public Optional<Map<Percentage, T>> percentiles() throws IllegalStateException, ClassCastException {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
            final Object[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Optional.empty();
            }

            return Optional.of((Map<Percentage, T>) N.percentilesOfSorted(a));
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public Optional<Map<Percentage, T>> percentiles(final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(comparator, cs.comparator);

        try {
            @SuppressWarnings("resource")
            final Object[] a = sorted(comparator).toArray();

            if (N.isEmpty(a)) {
                return Optional.empty();
            }

            return Optional.of((Map<Percentage, T>) N.percentilesOfSorted(a));
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
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
            return newStream(toArrayWithoutClosing(), false, null).combinations(); //NOSONAR
        }
    }

    @Override
    public Stream<List<T>> combinations(final int length) throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        assertNotClosed();
        checkArgNotNegative(length, cs.length);

        if (this instanceof ArrayStream<T> s) {
            final int count = s.toIndex - s.fromIndex;
            checkFromIndexSize(0, length, count);

            if (length == 0) {
                return newStream(N.asArray(N.emptyList()), false, null);
            } else if (length == 1) {
                return map(N::toList);
            } else if (length == count) {
                // Copy the elements directly: the terminal toList() would close this stream (running its close
                // handlers) during this intermediate call, before the returned stream is even traversed.
                return newStream(N.asArray(N.toList(s.elements, s.fromIndex, s.toIndex)), false, null);
            } else {
                final T[] a = s.elements;
                final int fromIndex = s.fromIndex;
                final int toIndex = s.toIndex;

                return newStream(new ObjIteratorEx<>() { //NOSONAR
                    private final int[] indices = Array.range(fromIndex, fromIndex + length);

                    @Override
                    public boolean hasNext() {
                        return indices[0] <= toIndex - length;
                    }

                    @Override
                    public List<T> next() throws NoSuchElementException {
                        if (!hasNext()) {
                            throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final List<T> result = new ArrayList<>(length);

                        for (final int idx : indices) {
                            result.add(a[idx]);
                        }

                        if (++indices[length - 1] == toIndex) {
                            for (int i = length - 1; i > 0; i--) {
                                if (indices[i] > toIndex - (length - i)) {
                                    indices[i - 1]++;

                                    for (int j = i; j < length; j++) {
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
            return newStream(toArrayWithoutClosing(), false, null).combinations(length); //NOSONAR
        }
    }

    /**
     * Materializes the remaining elements of this (iterator-backed) stream for {@code combinations()} and
     * {@code combinations(int)} without running a terminal operation on it.
     *
     * <p>The terminal {@code toArray()} used before closed this stream - running its close
     * handlers - during the intermediate call, before the returned stream was even traversed. The derived stream links
     * {@code this::close} through {@code closeHandlersForNewStream()}, so the handlers now run when the RESULT is
     * closed, as in the array-backed branch. A failure while reading the source still closes this stream, and wins
     * over a failing close handler (LST/C-114).
     *
     * @return the remaining elements
     */
    @SuppressWarnings("unchecked")
    private T[] toArrayWithoutClosing() {
        try {
            return (T[]) toArray(false);
        } catch (final Throwable e) { // NOSONAR
            closeAfterFailure(e);
            throw e;
        }
    }

    @Override
    public Stream<List<T>> combinations(final int length, final boolean repeat)
            throws IllegalStateException, IllegalArgumentException, IndexOutOfBoundsException {
        assertNotClosed();

        checkArgNotNegative(length, cs.length);

        if (!repeat) {
            return combinations(length);
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
                public List<T> next() throws NoSuchElementException {
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
                        list = Iterables.cartesianProduct(N.repeat(AbstractStream.this.toList(), length));
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
    public Stream<List<T>> orderedPermutations(final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(comparator, cs.comparator);

        final Iterator<List<T>> iter = PermutationIterator.ordered(toList(), comparator);

        return newStream(iter, false, null);
    }

    @Override
    public Stream<List<T>> cartesianProduct(final Collection<? extends Collection<? extends T>> collections)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(collections, cs.cs);

        final List<Collection<? extends T>> cList = new ArrayList<>(collections.size() + 1);
        cList.add(toList());
        cList.addAll(collections);

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
            public List<T> next() throws NoSuchElementException {
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
    public <A> A[] toArray(final IntFunction<A[]> generator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(generator, cs.generator);

        try {
            // toArray(false), not the closing terminal toArray(): the latter closed this stream
            // (running its close handlers) before the generator was called, so a failing close handler replaced a
            // generator/copy failure that had not even happened yet. The stream is now closed once, below, after
            // the generator step, through the ordinary closeAfterFailure/finally path (LST/C-114 precedence).
            final Object[] src = toArray(false);
            A[] dest = N.requireNonNull(generator.apply(src.length), "generator returned null");

            if (dest.length < src.length) {
                // A generator that ignores its size argument (e.g. n -> new String[0]) used to blow up in arraycopy
                // here while the array-backed stream tolerated it; grow the array, keeping its runtime type.
                dest = Arrays.copyOf(dest, src.length);
            }

            //noinspection SuspiciousSystemArraycopy
            System.arraycopy(src, 0, dest, 0, src.length);
            return dest;
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public Dataset toDataset() throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return N.newDataset(toList());
    }

    @Override
    public Dataset toDataset(final List<String> columnNames) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return N.newDataset(columnNames, toList());
    }

    @Override
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        try (final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseBuffer()) {
            @SuppressWarnings("resource")
            final IteratorEx<T> iter = iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.next());
            }

            return joiner.toString();
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
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
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public boolean containsDuplicates() throws IllegalStateException {
        assertNotClosed();

        try {
            final Set<Object> set = N.newHashSet();
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();

            while (iter.hasNext()) {
                // hashKey(..) - the normalization distinct() uses - so array elements compare by content, as in
                // Seq.containsDuplicates and N.containsDuplicates.
                if (!set.add(hashKey(iter.next()))) {
                    return true;
                }
            }

            return false;
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super T> accumulator)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(accumulator, cs.accumulator);

        @SuppressWarnings("UnnecessaryLocalVariable")
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, RR, E extends Exception> RR collectThenApply(final Collector<? super T, ?, R> downstream,
            final Throwables.Function<? super R, ? extends RR, E> function) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(function, cs.function);

        return function.apply(collect(downstream));
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
    public <R, E extends Exception> R toListThenApply(final Throwables.Function<? super List<T>, ? extends R, E> function)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(function, cs.function);

        return function.apply(toList());
    }

    @Override
    public <E extends Exception> void toListThenAccept(final Throwables.Consumer<? super List<T>, E> consumer)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(toList());
    }

    @Override
    public <R, E extends Exception> R toSetThenApply(final Throwables.Function<? super Set<T>, ? extends R, E> function)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(function, cs.function);

        return function.apply(toSet());
    }

    @Override
    public <E extends Exception> void toSetThenAccept(final Throwables.Consumer<? super Set<T>, E> consumer)
            throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(toSet());
    }

    @Override
    public <R, C extends Collection<T>, E extends Exception> R toCollectionThenApply(final Supplier<? extends C> supplier,
            final Throwables.Function<? super C, ? extends R, E> function) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(function, cs.function);

        return function.apply(toCollection(supplier));
    }

    @Override
    public <C extends Collection<T>, E extends Exception> void toCollectionThenAccept(final Supplier<? extends C> supplier,
            final Throwables.Consumer<? super C, E> consumer) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();

        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(consumer, cs.consumer);

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
                    list = null; // The immutable cycle snapshot now owns the cached values.
                    len = a.length;
                    cursor = 0;

                    return len > 0;
                }
            }

            @Override
            public T next() throws NoSuchElementException {
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
                    list = null; // The immutable cycle snapshot now owns the cached values.
                    len = a.length;
                    cursor = 0;
                    m++;

                    return m < rounds && len > 0;
                }
            }

            @Override
            public T next() throws NoSuchElementException {
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
            public List<T> next() throws NoSuchElementException {
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

                    // Snapshot the range, then hand out immutable views of the snapshot.
                    // arrayForIntermediateOp() may return the stream's own backing array - ArrayStream overrides it to
                    // return the CALLER's array, and StreamBase documents it as read-only - so wrapping it directly let
                    // a caller write through an emitted list into their own array, and made every emitted list a mutable
                    // view of one shared buffer. Copying per emitted list instead would be O(n^2): rollup emits n+1
                    // lists, measured at 7.5 s vs 4.4 ms for a single snapshot at n=50,000.
                    elements = ImmutableList.wrap(Arrays.asList((T[]) N.copyOfRange(tp._1, tp._2, tp._3)));
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

        final BlockingQueue<T> queueToBuffer;

        try {
            // bufferSize is a bound, not a size: ArrayBlockingQueue allocates its whole capacity up front, so a large
            // bound (e.g. Integer.MAX_VALUE, "let the producer run far ahead") would cost gigabytes or fail with an
            // OutOfMemoryError before a single element is read. Above MAX_BUFFERED_SIZE (the largest size the default
            // sizing picks, the same threshold as the Stream factories' bounded queues) use a LinkedBlockingQueue:
            // equally bounded and blocking, but it allocates only for elements actually buffered.
            queueToBuffer = bufferSize <= MAX_BUFFERED_SIZE ? new ArrayBlockingQueue<>(bufferSize) : new LinkedBlockingQueue<>(bufferSize);
        } catch (final Throwable e) {
            // No stream owns this one yet: release it (and any file/JDBC handle behind it) before the failure propagates.
            try {
                close();
            } catch (final Throwable e2) {
                if (e2 != e) {
                    e.addSuppressed(e2);
                }
            }

            throw e;
        }

        return buffered(queueToBuffer);
    }

    @Override
    public Stream<T> buffered(final BlockingQueue<T> queueToBuffer) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(queueToBuffer, cs.queueToBuffer);
        checkArgument(queueToBuffer.isEmpty(), "'queueToBuffer' must be empty");

        final Supplier<BufferedIterator<T>> supplier = () -> buffered(iteratorEx(), queueToBuffer);

        //noinspection resource
        return just(supplier).map(Supplier::get)
                .<T> flatMap(iter -> newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)))
                .onClose(this::close);
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
    public Stream<T> append(final Optional<T> optional) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(optional, cs.optional);

        // return append(op.stream());
        return optional.isEmpty() ? this : append(optional.orElseThrow());
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
    public Stream<T> prepend(final Optional<T> optional) throws IllegalStateException, IllegalArgumentException { //NOSONAR
        assertNotClosed();

        checkArgNotNull(optional, cs.optional);

        // return prepend(op.stream());
        return optional.isEmpty() ? this : prepend(optional.orElseThrow());
    }

    @Override
    public Stream<T> mergeWith(final Collection<? extends T> b, final BiFunction<? super T, ? super T, MergeResult> nextSelector)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(nextSelector, cs.nextSelector);

        return Stream.merge(iteratorEx(), N.iterate(b), nextSelector).onClose(newCloseHandler(this));
    }

    @Override
    public Stream<T> mergeWith(final Stream<? extends T> b, final BiFunction<? super T, ? super T, MergeResult> nextSelector)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(nextSelector, cs.nextSelector);

        return Stream.merge(this, b, nextSelector);
    }

    @Override
    public <T2, R> Stream<R> zipWith(final Collection<T2> b, final BiFunction<? super T, ? super T2, ? extends R> zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        return Stream.<T, T2, R> zip(iteratorEx(), N.iterate(b), zipFunction).onClose(newCloseHandler(this));
    }

    @Override
    public <T2, R> Stream<R> zipWith(final Collection<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        return Stream.<T, T2, R> zip(iteratorEx(), N.iterate(b), valueForNoneA, valueForNoneB, zipFunction).onClose(newCloseHandler(this));
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(final Collection<T2> b, final Collection<T3> c,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        return Stream.<T, T2, T3, R> zip(iteratorEx(), N.iterate(b), N.iterate(c), zipFunction).onClose(newCloseHandler(this));
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(final Collection<T2> b, final Collection<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(zipFunction, cs.zipFunction);

        return Stream.<T, T2, T3, R> zip(iteratorEx(), N.iterate(b), N.iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction)
                .onClose(newCloseHandler(this));
    }

    @Override
    public <T2, R> Stream<R> zipWith(final Stream<T2> b, final BiFunction<? super T, ? super T2, ? extends R> zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(zipFunction, cs.zipFunction);

        return Stream.zip(this, b, zipFunction);
    }

    @Override
    public <T2, R> Stream<R> zipWith(final Stream<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final BiFunction<? super T, ? super T2, ? extends R> zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(zipFunction, cs.zipFunction);

        return Stream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(c, cs.c);
        checkArgNotNull(zipFunction, cs.zipFunction);

        return Stream.zip(this, b, c, zipFunction);
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(final Stream<T2> b, final Stream<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final TriFunction<? super T, ? super T2, ? super T3, ? extends R> zipFunction) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(c, cs.c);
        checkArgNotNull(zipFunction, cs.zipFunction);

        return Stream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @SuppressWarnings("rawtypes")
    private static final Function TO_LINE_OF_STRING = N::stringOf;

    @Override
    public Stream<T> onEachSave(final File output) throws IllegalStateException {
        assertNotClosed();

        return onEachSave(TO_LINE_OF_STRING, output);
    }

    @Override
    public Stream<T> onEachSave(final Function<? super T, String> toLine, final File output) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private Writer writer = null;
            private BufferedWriter bw = null;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    // Create/truncate the file as soon as traversal starts - even if the stream turns out to be
                    // empty - like persist(File) does, so an empty run does not leave a previous run's output behind.
                    init();
                }

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

            /**
             * @throws UncheckedIOException if flushing buffered output fails.
             */
            @Override
            public void closeResource() throws UncheckedIOException {
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
                // Mark initialized only once the file is open: after a failed open (e.g. the output is a directory) a
                // later hasNext()/next() retries - and fails again with the same descriptive exception - instead of
                // writing to a null writer.
                if (writer == null) {
                    writer = IOUtil.newFileWriter(output);
                }

                bw = Objectory.createBufferedWriter(writer);
                initialized = true;
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> onEachSave(final Function<? super T, String> toLine, final OutputStream output) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);

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

            /**
             * @throws UncheckedIOException if flushing buffered output fails.
             */
            @Override
            public void closeResource() throws UncheckedIOException {
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
    public Stream<T> onEachSave(final Function<? super T, String> toLine, final Writer output) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);

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

            /**
             * @throws UncheckedIOException if flushing buffered output fails.
             */
            @Override
            public void closeResource() throws UncheckedIOException {
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
    public Stream<T> onEachSave(final Throwables.BiConsumer<? super T, Writer, IOException> write, final File output)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(write, cs.write);

        final ObjIteratorEx<T> iter = new ObjIteratorEx<>() { //NOSONAR
            private final Iterator<T> iter = iteratorEx();
            private Writer writer = null;
            private BufferedWriter bw = null;
            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (!initialized) {
                    // Create/truncate the file as soon as traversal starts - even if the stream turns out to be
                    // empty - like persist(File) does, so an empty run does not leave a previous run's output behind.
                    init();
                }

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

            /**
             * @throws UncheckedIOException if flushing buffered output fails.
             */
            @Override
            public void closeResource() throws UncheckedIOException {
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
                // Mark initialized only once the file is open: after a failed open (e.g. the output is a directory) a
                // later hasNext()/next() retries - and fails again with the same descriptive exception - instead of
                // writing to a null writer.
                if (writer == null) {
                    writer = IOUtil.newFileWriter(output);
                }

                bw = Objectory.createBufferedWriter(writer);
                initialized = true;
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> onEachSave(final Throwables.BiConsumer<? super T, Writer, IOException> write, final Writer output)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(write, cs.write);

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

            /**
             * @throws UncheckedIOException if flushing buffered output fails.
             */
            @Override
            public void closeResource() throws UncheckedIOException {
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
    public Stream<T> onEachSave(final PreparedStatement statement, final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return onEachSave(statement, 1, 0, stmtSetter);
    }

    @Override
    public Stream<T> onEachSave(final PreparedStatement statement, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(statement, cs.statement);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);
        checkArgNotNull(stmtSetter, cs.stmtSetter);

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
             * @throws NoSuchElementException if the iteration has no more elements
             * @throws RuntimeException if {@code stmtSetter} or statement execution throws {@link SQLException}; the SQL exception is wrapped
             */
            @Override
            public T next() throws NoSuchElementException, RuntimeException {
                final T next = iter.next();

                try {
                    stmtSetter.accept(next, statement);

                    if (isBatchUsed) {
                        cnt++;
                        statement.addBatch();

                        if (cnt % batchSize == 0) {
                            DataSourceUtil.executeBatch(statement);

                            if (batchIntervalInMillis > 0) {
                                N.sleepUninterruptibly(batchIntervalInMillis);
                            }
                        }
                    } else {
                        statement.execute();
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
                        DataSourceUtil.executeBatch(statement);
                    } catch (final SQLException e) {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    }
                }
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> onEachSave(final Connection connection, final String insertSQL,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return onEachSave(connection, insertSQL, 1, 0, stmtSetter);
    }

    @Override
    public Stream<T> onEachSave(final Connection connection, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(connection, cs.connection);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);
        checkArgNotNull(stmtSetter, cs.stmtSetter);

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
             * @throws NoSuchElementException if the iteration has no more elements
             * @throws RuntimeException if statement preparation, {@code stmtSetter}, or statement execution throws {@link SQLException}; the SQL exception is wrapped
             */
            @Override
            public T next() throws NoSuchElementException, RuntimeException {
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
                // Mark initialized only once the statement is prepared: after a failed prepareStatement a later
                // next() retries - and fails again with the SQL failure - instead of passing a null statement on.
                try {
                    stmt = connection.prepareStatement(insertSQL);
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                initialized = true;
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public Stream<T> onEachSave(final javax.sql.DataSource dataSource, final String insertSQL,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return onEachSave(dataSource, insertSQL, 1, 0, stmtSetter);
    }

    @Override
    public Stream<T> onEachSave(final javax.sql.DataSource dataSource, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(dataSource, cs.dataSource);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);
        checkArgNotNull(stmtSetter, cs.stmtSetter);

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
             * @throws NoSuchElementException if the iteration has no more elements
             * @throws RuntimeException if connection acquisition, statement preparation, {@code stmtSetter}, or statement execution throws
             *         {@link SQLException}; the SQL exception is wrapped
             */
            @Override
            public T next() throws NoSuchElementException, RuntimeException {
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
                    DataSourceUtil.releaseConnection(conn, dataSource);
                }
            }

            private void init() {
                // Mark initialized only once the statement is prepared (see the Connection overload): a failed
                // getConnection/prepareStatement is retried by a later next() instead of using a null statement. A
                // connection kept after an unchecked prepareStatement failure is reused, not replaced and leaked.
                try {
                    if (conn == null) {
                        conn = dataSource.getConnection();
                    }

                    stmt = conn.prepareStatement(insertSQL);
                } catch (final SQLException e) {
                    final Connection failedConnection = conn;
                    conn = null;
                    try {
                        if (stmt != null) {
                            DataSourceUtil.closeQuietly(stmt);
                        }
                    } finally {
                        DataSourceUtil.releaseConnection(failedConnection, dataSource);
                    }

                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                initialized = true;
            }
        };

        return newStream(iter, isSorted(), comparator(), mergeCloseHandlers(iter::closeResource, closeHandlers(), true)); //NOSONAR
    }

    @Override
    public long persist(final File output) throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotNull(output, cs.output);

        return persist(TO_LINE_OF_STRING, output);
    }

    @Override
    public long persist(final String header, final String tail, final File output)
            throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotNull(output, cs.output);

        return persist(header, tail, TO_LINE_OF_STRING, output);
    }

    @Override
    public long persist(final Function<? super T, String> toLine, final File output)
            throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);
        checkArgNotNull(output, cs.output);

        return persist(null, null, toLine, output);
    }

    @Override
    public long persist(final String header, final String tail, final Function<? super T, String> toLine, final File output)
            throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);
        checkArgNotNull(output, cs.output);

        Writer writer = null;

        try {
            writer = IOUtil.newFileWriter(output);
            return persist(header, tail, toLine, writer);
        } catch (final Throwable e) {
            // Opening the file can fail before the Writer overload owns (and closes) this stream, and flushing/closing
            // the writer can fail as well: neither close failure may replace the primary failure.
            final Writer toClose = writer;
            writer = null; // closed here, not again in the finally below
            closeAfterFailure(toClose, e);
            throw e;
        } finally {
            try {
                IOUtil.close(writer);
            } finally {
                close();
            }
        }
    }

    @Override
    public long persist(final Function<? super T, String> toLine, final OutputStream output)
            throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);
        checkArgNotNull(output, cs.output);

        BufferedWriter bw = null;

        try {
            bw = Objectory.createBufferedWriter(output);
            return persist(toLine, bw);
        } catch (final Throwable e) {
            // The writer is still flushed and recycled, but that must not replace the primary failure either.
            final BufferedWriter toRecycle = bw;
            bw = null; // recycled here, not again in the finally below
            recycleAfterFailure(toRecycle, e);
            closeAfterFailure(e);
            throw e;
        } finally {
            try {
                if (bw != null) {
                    Objectory.recycle(bw);
                }
            } finally {
                close();
            }
        }
    }

    @Override
    public long persist(final String header, final String tail, final Function<? super T, String> toLine, final OutputStream output)
            throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);
        checkArgNotNull(output, cs.output);

        BufferedWriter bw = null;

        try {
            bw = Objectory.createBufferedWriter(output);
            return persist(header, tail, toLine, bw);
        } catch (final Throwable e) {
            // The writer is still flushed and recycled, but that must not replace the primary failure either.
            final BufferedWriter toRecycle = bw;
            bw = null; // recycled here, not again in the finally below
            recycleAfterFailure(toRecycle, e);
            closeAfterFailure(e);
            throw e;
        } finally {
            try {
                if (bw != null) {
                    Objectory.recycle(bw);
                }
            } finally {
                close();
            }
        }

    }

    @Override
    public long persist(final Function<? super T, String> toLine, final Writer output) throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);
        checkArgNotNull(output, cs.output);

        return persist(null, null, toLine, output);
    }

    @Override
    public long persist(final String header, final String tail, final Function<? super T, String> toLine, final Writer output)
            throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(toLine, cs.toLine);
        checkArgNotNull(output, cs.output);

        try {
            final boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
            final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
            long cnt = 0;
            Throwable primaryFailure = null;

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
            } catch (final Throwable e) {
                primaryFailure = e;
                throw e;
            } finally {
                flushAndRecycle(bw, !isBufferedWriter, primaryFailure);
            }

            return cnt;
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> write, final File output)
            throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotNull(write, cs.write);
        checkArgNotNull(output, cs.output);

        return persist(null, null, write, output);
    }

    @Override
    public long persist(final String header, final String tail, final Throwables.BiConsumer<? super T, Writer, IOException> write, final File output)
            throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotNull(write, cs.write);
        checkArgNotNull(output, cs.output);

        Writer writer = null;

        try {
            writer = IOUtil.newFileWriter(output);
            return persist(header, tail, write, writer);
        } catch (final Throwable e) {
            // Opening the file can fail before the Writer overload owns (and closes) this stream, and flushing/closing
            // the writer can fail as well: neither close failure may replace the primary failure.
            final Writer toClose = writer;
            writer = null; // closed here, not again in the finally below
            closeAfterFailure(toClose, e);
            throw e;
        } finally {
            try {
                IOUtil.close(writer);
            } finally {
                close();
            }
        }
    }

    @Override
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> write, final Writer output)
            throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(write, cs.write);
        checkArgNotNull(output, cs.output);

        return persist(null, null, write, output);
    }

    @Override
    public long persist(final String header, final String tail, final Throwables.BiConsumer<? super T, Writer, IOException> write, final Writer output)
            throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(write, cs.write);
        checkArgNotNull(output, cs.output);

        try {
            final boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
            final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
            long cnt = 0;
            Throwable primaryFailure = null;

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

            } catch (final Throwable e) {
                primaryFailure = e;
                throw e;
            } finally {
                flushAndRecycle(bw, !isBufferedWriter, primaryFailure);
            }

            return cnt;
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public long persist(final PreparedStatement statement, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException, SQLException {
        assertNotClosed();

        checkArgNotNull(statement, cs.statement);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);
        checkArgNotNull(stmtSetter, cs.stmtSetter);

        final boolean isBatchUsed = batchSize > 1;

        try {
            @SuppressWarnings("resource")
            final Iterator<T> iter = iteratorEx();
            long cnt = 0;

            while (iter.hasNext()) {
                stmtSetter.accept(iter.next(), statement);
                cnt++;

                if (isBatchUsed) {
                    statement.addBatch();

                    if (cnt % batchSize == 0) {
                        DataSourceUtil.executeBatch(statement);

                        if (batchIntervalInMillis > 0) {
                            N.sleepUninterruptibly(batchIntervalInMillis);
                        }
                    }
                } else {
                    statement.execute();
                }
            }

            if (isBatchUsed && cnt % batchSize > 0) {
                DataSourceUtil.executeBatch(statement);
            }

            return cnt;
        } catch (final Throwable e) {
            if (isBatchUsed) {
                // The statement belongs to the caller: rows added by addBatch() but not yet executed would otherwise be
                // inserted by the caller's next executeBatch() (e.g. on a retry). DataSourceUtil.executeBatch clears the
                // batch on its own failure path for the same reason.
                try {
                    statement.clearBatch();
                } catch (final Throwable e2) {
                    e.addSuppressed(e2);
                }
            }

            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    @Override
    public long persist(final Connection connection, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException, SQLException {
        assertNotClosed();

        checkArgNotNull(connection, cs.connection);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);
        checkArgNotNull(stmtSetter, cs.stmtSetter);

        PreparedStatement stmt = null;

        try {
            stmt = connection.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchIntervalInMillis, stmtSetter);
        } catch (final Throwable e) {
            // prepareStatement can fail before persist(PreparedStatement, ...) owns (and closes) this stream.
            closeAfterFailure(e);
            throw e;
        } finally {
            try {
                DataSourceUtil.closeQuietly(stmt);
            } finally {
                close();
            }
        }
    }

    @Override
    public long persist(final javax.sql.DataSource dataSource, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException, SQLException {
        assertNotClosed();

        checkArgNotNull(dataSource, cs.dataSource);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);
        checkArgNotNull(stmtSetter, cs.stmtSetter);

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = dataSource.getConnection();
            stmt = conn.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchIntervalInMillis, stmtSetter);
        } catch (final Throwable e) {
            // getConnection/prepareStatement can fail before persist(PreparedStatement, ...) owns (and closes) this stream.
            closeAfterFailure(e);
            throw e;
        } finally {
            try {
                DataSourceUtil.closeQuietly(stmt);
            } finally {
                try {
                    DataSourceUtil.releaseConnection(conn, dataSource);
                } finally {
                    close();
                }
            }
        }
    }

    @Override
    public long persistToCsv(final File output) throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotNull(output, cs.output);

        Writer writer = null;

        try {
            writer = IOUtil.newFileWriter(output);
            return persistToCsv(writer);
        } catch (final Throwable e) {
            // Opening the file can fail before the Writer overload owns (and closes) this stream, and flushing/closing
            // the writer can fail as well: neither close failure may replace the primary failure.
            final Writer toClose = writer;
            writer = null; // closed here, not again in the finally below
            closeAfterFailure(toClose, e);
            throw e;
        } finally {
            try {
                IOUtil.close(writer);
            } finally {
                close();
            }
        }
    }

    @Override
    public long persistToCsv(final Collection<String> headers, final File output)
            throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotEmpty(headers, cs.csvHeaders);
        checkArgNotNull(output, cs.output);

        Writer writer = null;

        try {
            writer = IOUtil.newFileWriter(output);
            return persistToCsv(headers, writer);
        } catch (final Throwable e) {
            // Opening the file can fail before the Writer overload owns (and closes) this stream, and flushing/closing
            // the writer can fail as well: neither close failure may replace the primary failure.
            final Writer toClose = writer;
            writer = null; // closed here, not again in the finally below
            closeAfterFailure(toClose, e);
            throw e;
        } finally {
            try {
                IOUtil.close(writer);
            } finally {
                close();
            }
        }
    }

    @Override
    public long persistToCsv(final OutputStream output) throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(output, cs.output);

        BufferedWriter bw = null;

        try {
            bw = Objectory.createBufferedWriter(output);
            return persistToCsv(bw);
        } catch (final Throwable e) {
            // The writer is still flushed and recycled, but that must not replace the primary failure either.
            final BufferedWriter toRecycle = bw;
            bw = null; // recycled here, not again in the finally below
            recycleAfterFailure(toRecycle, e);
            closeAfterFailure(e);
            throw e;
        } finally {
            try {
                if (bw != null) {
                    Objectory.recycle(bw);
                }
            } finally {
                close();
            }
        }
    }

    @Override
    public long persistToCsv(final Collection<String> headers, final OutputStream output) throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotEmpty(headers, cs.csvHeaders);
        checkArgNotNull(output, cs.output);

        BufferedWriter bw = null;

        try {
            bw = Objectory.createBufferedWriter(output);
            return persistToCsv(headers, bw);
        } catch (final Throwable e) {
            // The writer is still flushed and recycled, but that must not replace the primary failure either.
            final BufferedWriter toRecycle = bw;
            bw = null; // recycled here, not again in the finally below
            recycleAfterFailure(toRecycle, e);
            closeAfterFailure(e);
            throw e;
        } finally {
            try {
                if (bw != null) {
                    Objectory.recycle(bw);
                }
            } finally {
                close();
            }
        }
    }

    @Override
    public long persistToCsv(final Writer output) throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        return persistToCsv(null, output, true);
    }

    @TerminalOp
    @Override
    public long persistToCsv(final Collection<String> csvHeaders, final Writer output) throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        return persistToCsv(csvHeaders, output, false);
    }

    private long persistToCsv(final Collection<String> csvHeaders, final Writer output, final boolean canCsvHeadersBeEmpty)
            throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        if (!canCsvHeadersBeEmpty) {
            checkArgNotEmpty(csvHeaders, cs.csvHeaders);
        }

        checkArgNotNull(output, cs.output);

        try {
            final List<Object> headers = N.newArrayList(csvHeaders);
            final boolean isBufferedWriter = output instanceof BufferedCsvWriter;
            final BufferedCsvWriter bw = isBufferedWriter ? (BufferedCsvWriter) output : Objectory.createBufferedCsvWriter(output);

            final char separator = SK._COMMA;
            long cnt = 0;
            T next = null;
            Class<?> cls = null;
            Throwable primaryFailure = null;

            try {
                @SuppressWarnings("resource")
                final Iterator<T> iter = iteratorEx();

                if (iter.hasNext()) {
                    cnt++;
                    next = iter.next();

                    if (next == null) {
                        throw new IllegalArgumentException(
                                "null is not supported for CSV format. Use bean/Map rows, or Collection/Object[] rows with explicit headers");
                    }

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

                        // A later row may be of another bean class (e.g. sibling subclasses in a Stream<Animal>), so the
                        // header properties are resolved per row class - each row is read through its own class's
                        // accessors - and a row is rejected only if its class lacks a header property.
                        final Class<?> firstCls = cls;
                        Map<Class<?>, PropInfo[]> propInfosByClass = null;
                        Class<?> rowCls = cls;
                        PropInfo[] rowPropInfos = propInfos;

                        while (iter.hasNext()) {
                            next = checkCsvRowType(iter.next(), Object.class, cnt + 1);

                            if (next.getClass() != rowCls) {
                                rowCls = next.getClass();

                                if (propInfosByClass == null) {
                                    propInfosByClass = new HashMap<>();
                                    propInfosByClass.put(firstCls, propInfos);
                                }

                                rowPropInfos = propInfosByClass.get(rowCls);

                                if (rowPropInfos == null) {
                                    rowPropInfos = getCsvBeanRowPropInfos(rowCls, headers, firstCls, cnt + 1);
                                    propInfosByClass.put(rowCls, rowPropInfos);
                                }
                            }

                            bw.write(IOUtil.LINE_SEPARATOR_UNIX);

                            for (int i = 0; i < headSize; i++) {
                                propInfo = rowPropInfos[i];

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
                            row = (Map<Object, Object>) checkCsvRowType(iter.next(), Map.class, cnt + 1);

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

                        checkCsvRowSize(row.size(), headSize, cnt);

                        Iterator<Object> rowIter = row.iterator();

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, rowIter.next());
                        }

                        while (iter.hasNext()) {
                            row = (Collection<Object>) checkCsvRowType(iter.next(), Collection.class, cnt + 1);
                            checkCsvRowSize(row.size(), headSize, cnt + 1);
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

                        checkCsvRowSize(row.length, headSize, cnt);

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(separator);
                            }

                            CsvUtil.writeField(bw, null, row[i]);
                        }

                        while (iter.hasNext()) {
                            row = (Object[]) checkCsvRowType(iter.next(), Object[].class, cnt + 1);
                            checkCsvRowSize(row.length, headSize, cnt + 1);

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
                        throw new IllegalArgumentException(
                                cls + " is not supported for CSV format. Use bean/Map rows, or Collection/Object[] rows with explicit headers");
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
            } catch (final Throwable e) {
                primaryFailure = e;
                throw e;
            } finally {
                flushAndRecycle(bw, !isBufferedWriter, primaryFailure);
            }

            return cnt;
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    /**
     * Resolves the header properties for a bean CSV row whose class differs from the first row's.
     *
     * @param rowCls the class of the row
     * @param headers the CSV headers (property names)
     * @param firstCls the class of the first row, for the message
     * @param rowNum the 1-based position of the row among the data rows
     * @return the property of {@code rowCls} for each header, in header order
     * @throws IllegalArgumentException if {@code rowCls} is not a bean class or lacks a header property
     */
    private static PropInfo[] getCsvBeanRowPropInfos(final Class<?> rowCls, final List<Object> headers, final Class<?> firstCls, final long rowNum)
            throws IllegalArgumentException {
        if (!Beans.isBeanClass(rowCls)) {
            throw new IllegalArgumentException("CSV row " + rowNum + " is a " + ClassUtil.getCanonicalClassName(rowCls) + ", but the first row is a bean ("
                    + ClassUtil.getCanonicalClassName(firstCls) + "); every row must be a bean that has all header properties");
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(rowCls);
        final int headSize = headers.size();
        final PropInfo[] propInfos = new PropInfo[headSize];

        for (int i = 0; i < headSize; i++) {
            propInfos[i] = beanInfo.getPropInfo(headers.get(i).toString());

            if (propInfos[i] == null) {
                throw new IllegalArgumentException("CSV row " + rowNum + " is a " + ClassUtil.getCanonicalClassName(rowCls) + ", which has no property '"
                        + headers.get(i) + "' (a CSV header)");
            }
        }

        return propInfos;
    }

    /**
     * Validates a CSV data row after the first one: it must be non-null and of the same kind as the first row
     * (the header row and the column extraction are derived from the first row). Bean rows are only checked for
     * {@code null} here ({@code expectedType} {@code Object}); their properties are checked per row class.
     *
     * @param row the row
     * @param expectedType {@code Map}/{@code Collection}/{@code Object[]}, or {@code Object} for bean rows
     * @param rowNum the 1-based position of the row among the data rows
     * @return {@code row}
     * @throws IllegalArgumentException if {@code row} is {@code null} or not an instance of {@code expectedType}
     */
    private static <R> R checkCsvRowType(final R row, final Class<?> expectedType, final long rowNum) throws IllegalArgumentException {
        if (row == null) {
            throw new IllegalArgumentException("CSV row " + rowNum + " is null; null rows are not supported for CSV format");
        } else if (!expectedType.isInstance(row)) {
            throw new IllegalArgumentException("CSV row " + rowNum + " is a " + ClassUtil.getCanonicalClassName(row.getClass()) + ", but the first row is a "
                    + ClassUtil.getCanonicalClassName(expectedType) + "; all rows must be of the same kind");
        }

        return row;
    }

    /**
     * Validates that a {@code Collection}/{@code Object[]} CSV row has exactly one field per header, so that a
     * longer row is not silently truncated and a shorter one does not fail with a raw iterator/index exception.
     *
     * @param rowSize the number of fields in the row
     * @param headSize the number of headers
     * @param rowNum the 1-based position of the row among the data rows
     * @throws IllegalArgumentException if {@code rowSize != headSize}
     */
    private static void checkCsvRowSize(final int rowSize, final int headSize, final long rowNum) throws IllegalArgumentException {
        if (rowSize != headSize) {
            throw new IllegalArgumentException("CSV row " + rowNum + " has " + rowSize + " field(s), but there are " + headSize + " header(s)");
        }
    }

    @Override
    public long persistToJson(final File output) throws IllegalStateException, IllegalArgumentException, UncheckedIOException, IOException {
        assertNotClosed();

        checkArgNotNull(output, cs.output);

        Writer writer = null;

        try {
            writer = IOUtil.newFileWriter(output);
            return persistToJson(writer);
        } catch (final Throwable e) {
            // Opening the file can fail before the Writer overload owns (and closes) this stream, and flushing/closing
            // the writer can fail as well: neither close failure may replace the primary failure.
            final Writer toClose = writer;
            writer = null; // closed here, not again in the finally below
            closeAfterFailure(toClose, e);
            throw e;
        } finally {
            try {
                IOUtil.close(writer);
            } finally {
                close();
            }
        }
    }

    @Override
    public long persistToJson(final OutputStream output) throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(output, cs.output);

        BufferedWriter bw = null;

        try {
            bw = Objectory.createBufferedWriter(output);
            return persistToJson(bw);
        } catch (final Throwable e) {
            // The writer is still flushed and recycled, but that must not replace the primary failure either.
            final BufferedWriter toRecycle = bw;
            bw = null; // recycled here, not again in the finally below
            recycleAfterFailure(toRecycle, e);
            closeAfterFailure(e);
            throw e;
        } finally {
            try {
                if (bw != null) {
                    Objectory.recycle(bw);
                }
            } finally {
                close();
            }
        }
    }

    @Override
    public long persistToJson(final Writer output) throws IllegalStateException, IllegalArgumentException, IOException {
        assertNotClosed();

        checkArgNotNull(output, cs.output);

        try {
            final boolean isBufferedWriter = output instanceof BufferedJsonWriter;
            final BufferedJsonWriter bw = isBufferedWriter ? (BufferedJsonWriter) output : Objectory.createBufferedJsonWriter(output); // NOSONAR

            long cnt = 0;
            Throwable primaryFailure = null;

            try {
                @SuppressWarnings("resource")
                final Iterator<T> iter = iteratorEx();

                bw.write("[");

                while (iter.hasNext()) {
                    if (cnt > 0) {
                        bw.write(SK._COMMA);
                    }

                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                    writeJsonArrayElement(bw, iter.next());

                    if (++cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                }

                bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                bw.write("]");
            } catch (final Throwable e) {
                primaryFailure = e;
                throw e;
            } finally {
                flushAndRecycle(bw, !isBufferedWriter, primaryFailure);
            }

            return cnt;
        } catch (final Throwable e) {
            closeAfterFailure(e);
            throw e;
        } finally {
            close();
        }
    }

    /**
     * Final flush (and, for a writer created by the terminal itself, recycle) of the {@code persist*(..., Writer)}
     * terminals. It also runs after a failure of the write loop, so that the lines written before the failure still
     * reach the target, but then a flush or recycle failure must not replace the primary failure (for example the
     * {@code toLine} mapper's exception): it is added to {@code primaryFailure} as a suppressed exception instead, as
     * {@code closeAfterFailure} does for a failing close handler.
     *
     * @param writer the writer to flush
     * @param recycle whether {@code writer} was created by the terminal and must be returned to the {@code Objectory} pool
     * @param primaryFailure the failure of the write loop, or {@code null} if it completed normally
     * @throws IOException if {@code primaryFailure} is {@code null} and flushing fails
     */
    private static void flushAndRecycle(final Writer writer, final boolean recycle, final Throwable primaryFailure) throws IOException {
        if (primaryFailure == null) {
            try {
                writer.flush();
            } finally {
                if (recycle) {
                    Objectory.recycle((BufferedWriter) writer);
                }
            }

            return;
        }

        try {
            writer.flush();
        } catch (final Throwable flushFailure) { // NOSONAR
            addSuppressedTo(primaryFailure, flushFailure);
        }

        if (recycle) {
            try {
                Objectory.recycle((BufferedWriter) writer);
            } catch (final Throwable recycleFailure) { // NOSONAR
                addSuppressedTo(primaryFailure, recycleFailure);
            }
        }
    }

    private static void recycleAfterFailure(final BufferedWriter writer, final Throwable primaryFailure) {
        if (writer != null) {
            try {
                Objectory.recycle(writer);
            } catch (final Throwable recycleFailure) { // NOSONAR
                addSuppressedTo(primaryFailure, recycleFailure);
            }
        }
    }

    private static void addSuppressedTo(final Throwable primaryFailure, final Throwable secondaryFailure) {
        if (secondaryFailure != primaryFailure) {
            primaryFailure.addSuppressed(secondaryFailure);
        }
    }

    /** Default configuration for writing one element in JSON value position. */
    private static final JsonSerConfig JSON_ELEMENT_SER_CONFIG = JsonSerConfig.create();

    /**
     * Writes one element in JSON <i>value</i> position, i.e. as it would appear inside a JSON array.
     *
     * <p>This cannot use {@code N.toJson(Object, Writer)}: that is the <i>root document</i> entry point,
     * and its root-scalar shortcut deliberately writes a {@code String}, {@code Character} or date
     * unquoted and unescaped, and a {@code null} as nothing at all. That contract is correct for a root
     * value but produces unparseable text inside the {@code [...]} array written here - a quote or a
     * comma in an element would corrupt the document and a {@code null} element would disappear.
     * Routing scalars through {@link Type#serializeTo} applies the same quoting and escaping the JSON
     * serializer uses for a nested value, so the result matches
     * {@code N.toJson(aCollectionOfTheSameElements)} exactly.
     *
     * @param bw the writer to write to
     * @param element the element to write; may be {@code null}
     * @throws IOException if writing the JSON representation of {@code element} to {@code bw} fails
     */
    private static void writeJsonArrayElement(final BufferedJsonWriter bw, final Object element) throws IOException {
        if (element == null) {
            bw.write("null");
            return;
        }

        @SuppressWarnings("unchecked")
        final Type<Object> type = (Type<Object>) N.typeOf(element.getClass());

        if (type.isSerializable()) {
            type.serializeTo(bw, element, JSON_ELEMENT_SER_CONFIG);
        } else {
            // Beans, Maps, Collections and arrays are already written correctly by the parser.
            N.toJson(element, bw);
        }
    }

    /**
     * Returns a mapper that turns each {@code List} produced by a chunking operation (such as {@code split}
     * or {@code sliding}) into a {@code Stream} over a snapshot copy of that list. The returned streams
     * inherit this stream's sorted flag and comparator and carry no close handlers.
     *
     * @return a function mapping a list of elements to a stream over a copy of that list
     */
    Function<List<T>, Stream<T>> listToStreamMapper() {
        return t -> new ArrayStream<>(StreamBase.toArray(t), 0, t.size(), isSorted(), comparator(), null);
    }

    @Override
    public <U> Stream<Pair<T, U>> crossJoin(final Collection<? extends U> b) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return crossJoin(b, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> crossJoin(final Collection<? extends U> b, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(function, cs.function);

        return flatMap(t -> Stream.of(b).map(u -> function.apply(t, u)));
    }

    @Override
    public <U, R> Stream<R> crossJoin(final Stream<? extends U> b, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(function, cs.function);

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile List<U> c = null;

            @Override
            public Stream<R> apply(final T t) {
                if (c == null) {
                    synchronized (this) { // lock the private function instance, never the caller's stream
                        if (c == null) {
                            c = (List<U>) b.toList();
                        }
                    }
                }

                return Stream.of(c).map(u -> function.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> innerJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);

        return innerJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, T>> innerJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);

        return innerJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> innerJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (this) { // lock the private function instance, never a caller-supplied object
                        if (rightKeyMap == null) {
                            rightKeyMap = ListMultimap.fromCollection(b, rightKeyExtractor);
                        }
                    }
                }

                return Stream.of(rightKeyMap.get(leftKeyExtractor.apply(t))).map(u -> function.apply(t, u));
            }
        });
    }

    @Override
    public <K, R> Stream<R> innerJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(function, cs.function);

        return innerJoin(b, keyMapper, keyMapper, function);
    }

    @Override
    public <U, K, R> Stream<R> innerJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (this) { // lock the private function instance, never a caller-supplied object
                        if (rightKeyMap == null) {
                            rightKeyMap = ((Stream<U>) b).toMultimap(Fn.from(rightKeyExtractor));
                        }
                    }
                }

                return Stream.of(rightKeyMap.get(leftKeyExtractor.apply(t))).map(u -> function.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, U>> innerJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);

        return innerJoin(b, predicate, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> innerJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate,
            final BiFunction<? super T, ? super U, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(function, cs.function);

        return flatMap(t -> Stream.of(b).filter(u -> predicate.test(t, u)).map(u -> function.apply(t, u)));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> fullJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);

        return fullJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, T>> fullJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);

        return fullJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> fullJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<Object, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (this) { // lock the private function instance, never a caller-supplied object
                        if (rightKeyMap == null) {
                            rightKeyMap = ListMultimap.fromCollection(b, rightKeyExtractor);
                        }
                    }
                }

                final List<U> values = rightKeyMap.get(leftKeyExtractor.apply(t));

                return N.isEmpty(values) ? Stream.of(function.apply(t, null)) : Stream.of(values).map(u -> {
                    if (isParallelStream) {
                        synchronized (joinedRights) {
                            joinedRights.put(u, u);
                        }
                    } else {
                        joinedRights.put(u, u);
                    }

                    return function.apply(t, u);
                });
            }
        }).append(Stream.of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> function.apply(null, u)));
    }

    @Override
    public <K, R> Stream<R> fullJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(function, cs.function);

        return fullJoin(b, keyMapper, keyMapper, function);
    }

    @Override
    public <U, K, R> Stream<R> fullJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final Holder<List<U>> holder = new Holder<>();

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<Object, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (this) { // lock the private function instance, never a caller-supplied object
                        if (rightKeyMap == null) {
                            final List<U> c = ((Stream<U>) b).toList();
                            rightKeyMap = ListMultimap.fromCollection(c, rightKeyExtractor);
                            holder.setValue(c);
                        }
                    }
                }

                final List<U> values = rightKeyMap.get(leftKeyExtractor.apply(t));

                return N.isEmpty(values) ? Stream.of(function.apply(t, null)) : Stream.of(values).map(u -> {
                    if (isParallelStream) {
                        synchronized (joinedRights) {
                            joinedRights.put(u, u);
                        }
                    } else {
                        joinedRights.put(u, u);
                    }

                    return function.apply(t, u);
                });
            }
        }).append(deferTail(() -> {
            // The right side is materialized lazily by the mapper; when this (left) stream was empty
            // the mapper never ran, so consume b here to keep the documented join semantics. Built only when
            // traversal reaches the tail (see deferTail): closing the result early does not consume b.
            final List<U> rights = holder.value() != null ? holder.value() : ((Stream<U>) b).toList();

            return Stream.of(rights).filter(u -> !joinedRights.containsKey(u)).map(u -> function.apply(null, u));
        })).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, U>> fullJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);

        return fullJoin(b, predicate, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> fullJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate,
            final BiFunction<? super T, ? super U, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(function, cs.function);

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

            return (R) function.apply(t, u);
        }).appendIfEmpty(() -> Stream.of(t).map(tt -> function.apply(t, null))))
                .append(Stream.of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> function.apply(null, u)));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> leftJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);

        return leftJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, T>> leftJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);

        return leftJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> leftJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (this) { // lock the private function instance, never a caller-supplied object
                        if (rightKeyMap == null) {
                            rightKeyMap = ListMultimap.fromCollection(b, rightKeyExtractor);
                        }
                    }
                }

                final List<U> values = rightKeyMap.get(leftKeyExtractor.apply(t));

                return N.isEmpty(values) ? Stream.of(function.apply(t, null)) : Stream.of(values).map(u -> function.apply(t, u));
            }
        });
    }

    @Override
    public <K, R> Stream<R> leftJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(function, cs.function);

        return leftJoin(b, keyMapper, keyMapper, function);
    }

    @Override
    public <U, K, R> Stream<R> leftJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (this) { // lock the private function instance, never a caller-supplied object
                        if (rightKeyMap == null) {
                            rightKeyMap = ((Stream<U>) b).toMultimap(Fn.from(rightKeyExtractor));
                        }
                    }
                }

                final List<U> values = rightKeyMap.get(leftKeyExtractor.apply(t));

                return N.isEmpty(values) ? Stream.of(function.apply(t, null)) : Stream.of(values).map(u -> function.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, U>> leftJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);

        return leftJoin(b, predicate, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> leftJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate,
            final BiFunction<? super T, ? super U, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(function, cs.function);

        return flatMap(t -> Stream.of(b)
                .filter(u -> predicate.test(t, u))
                .map(u -> (R) function.apply(t, u))
                .appendIfEmpty(() -> Stream.of(t).map(tt -> function.apply(t, null))));
    }

    @Override
    public <U, K> Stream<Pair<T, U>> rightJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);

        return rightJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, T>> rightJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);

        return rightJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> rightJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (this) { // lock the private function instance, never a caller-supplied object
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

                    return function.apply(t, u);
                });
            }
        }).append(Stream.of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> function.apply(null, u)));
    }

    @Override
    public <K, R> Stream<R> rightJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final BiFunction<? super T, ? super T, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(function, cs.function);

        return rightJoin(b, keyMapper, keyMapper, function);
    }

    @Override
    public <U, K, R> Stream<R> rightJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super U, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

        final boolean isParallelStream = isParallel();
        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final Holder<List<U>> holder = new Holder<>();

        //noinspection resource
        return flatMap(new Function<T, Stream<R>>() {
            private volatile ListMultimap<K, U> rightKeyMap = null;

            @Override
            public Stream<R> apply(final T t) {
                if (rightKeyMap == null) {
                    synchronized (this) { // lock the private function instance, never a caller-supplied object
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

                    return function.apply(t, u);
                });
            }
        }).append(deferTail(() -> {
            // The right side is materialized lazily by the mapper; when this (left) stream was empty
            // the mapper never ran, so consume b here to keep the documented join semantics. Built only when
            // traversal reaches the tail (see deferTail): closing the result early does not consume b.
            final List<U> rights = holder.value() != null ? holder.value() : ((Stream<U>) b).toList();

            return Stream.of(rights).filter(u -> !joinedRights.containsKey(u)).map(u -> function.apply(null, u));
        })).onClose(newCloseHandler(b));
    }

    @Override
    public <U> Stream<Pair<T, U>> rightJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);

        return rightJoin(b, predicate, Fn.pair());
    }

    @Override
    public <U, R> Stream<R> rightJoin(final Collection<? extends U> b, final BiPredicate<? super T, ? super U> predicate,
            final BiFunction<? super T, ? super U, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(function, cs.function);

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

            return (R) function.apply(t, u);
        })).append(Stream.of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> function.apply(null, u)));
    }

    @Override
    public <U, K> Stream<Pair<T, List<U>>> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);

        return groupJoin(b, leftKeyExtractor, rightKeyExtractor, Fn.pair());
    }

    @Override
    public <K> Stream<Pair<T, List<T>>> groupJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);

        return groupJoin(b, keyMapper, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super List<U>, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

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

                return function.apply(t, Objects.requireNonNullElseGet(val, Suppliers.ofList()));
            }

            private void init() {
                if (!initialized) {
                    synchronized (this) {
                        if (!initialized) {
                            // map = Stream.of(b).parallel(ps.maxThreadNum(), ps.splitStrategy(), ps.asyncExecutor()).groupTo(rightKeyExtractor);   // TODO may not be necessary.
                            map = groupRightSide(b.iterator(), rightKeyExtractor, Collectors.<U> toList());
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
            final BiFunction<? super T, ? super List<T>, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(function, cs.function);

        return groupJoin(b, keyMapper, keyMapper, function);
    }

    @Override
    public <U, K, R> Stream<R> groupJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BiFunction<? super T, ? super List<U>, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(function, cs.function);

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

                return function.apply(t, Objects.requireNonNullElseGet(val, Suppliers.ofList()));
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                // map = Stream.of(b).parallel(ps.maxThreadNum(), ps.splitStrategy(), ps.asyncExecutor()).groupTo(rightKeyExtractor);   // TODO may not be necessary.
                                map = groupRightSide((Stream<U>) b, rightKeyExtractor, Collectors.<U> toList());
                                initialized = true;
                            }
                        }
                    } else {
                        map = groupRightSide((Stream<U>) b, rightKeyExtractor, Collectors.<U> toList());
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
            final Function<? super U, ? extends K> rightKeyExtractor, final BinaryOperator<U> mergeFunction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(mergeFunction, cs.mergeFunction);

        return groupJoin(b, leftKeyExtractor, rightKeyExtractor, mergeFunction, Fn.pair());
    }

    @Override
    public <U, K, R> Stream<R> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final BinaryOperator<U> mergeFunction,
            final BiFunction<? super T, ? super U, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(function, cs.function);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, U> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                // An absent key and a key mapped to null are indistinguishable to 'func' here (both hand it
                // null), so a containsKey() probe would only cost a second lookup.
                final U val = map.get(leftKeyExtractor.apply(t));

                return function.apply(t, val);
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                //    map = Stream.of(b)
                                //            .parallel(ps.maxThreadNum(), ps.splitStrategy(), ps.asyncExecutor())
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
            final BiFunction<? super T, ? super U, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(function, cs.function);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, U> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                // An absent key and a key mapped to null are indistinguishable to 'func' here (both hand it
                // null), so a containsKey() probe would only cost a second lookup.
                final U val = map.get(leftKeyExtractor.apply(t));

                return function.apply(t, val);
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                //    map = Stream.of(b)
                                //            .parallel(ps.maxThreadNum(), ps.splitStrategy(), ps.asyncExecutor())
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
            final Function<? super U, ? extends K> rightKeyExtractor, final Collector<? super U, ?, D> downstream)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);

        return groupJoin(b, leftKeyExtractor, rightKeyExtractor, downstream, Fn.pair());
    }

    @Override
    public <U, K, D, R> Stream<R> groupJoin(final Collection<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final Collector<? super U, ?, D> downstream,
            final BiFunction<? super T, ? super D, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(function, cs.function);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, D> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                final K key = leftKeyExtractor.apply(t);

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                final D val = map.get(key);

                if (val == null && !map.containsKey(key)) {
                    //noinspection resource
                    return function.apply(t, Stream.<U> empty().collect(downstream));
                } else {
                    return function.apply(t, val);
                }
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                //    map = Stream.of(b)
                                //            .parallel(ps.maxThreadNum(), ps.splitStrategy(), ps.asyncExecutor())
                                //            .toMap(rightKeyExtractor, Fn.<U> identity(), downstream);   // TODO may not be necessary.

                                map = groupRightSide(b.iterator(), rightKeyExtractor, downstream);

                                initialized = true;
                            }
                        }
                    } else {
                        map = groupRightSide(b.iterator(), rightKeyExtractor, downstream);

                        initialized = true;
                    }
                }
            }
        };

        return map(mapper);
    }

    @Override
    public <K, D> Stream<Pair<T, D>> groupJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, ?, D> downstream) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);

        return groupJoin(b, keyMapper, keyMapper, downstream);
    }

    @Override
    public <K, D, R> Stream<R> groupJoin(final Collection<? extends T> b, final Function<? super T, ? extends K> keyMapper,
            final Collector<? super T, ?, D> downstream, final BiFunction<? super T, ? super D, ? extends R> function)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, cs.b);
        checkArgNotNull(keyMapper, cs.keyMapper);

        return groupJoin(b, keyMapper, keyMapper, downstream, function);
    }

    @Override
    public <U, K, D, R> Stream<R> groupJoin(final Stream<? extends U> b, final Function<? super T, ? extends K> leftKeyExtractor,
            final Function<? super U, ? extends K> rightKeyExtractor, final Collector<? super U, ?, D> downstream,
            final BiFunction<? super T, ? super D, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' cannot be null");
        checkArgNotNull(leftKeyExtractor, cs.leftKeyExtractor);
        checkArgNotNull(rightKeyExtractor, cs.rightKeyExtractor);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(function, cs.function);

        final Function<T, R> mapper = new Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, D> map = null;

            @Override
            public R apply(final T t) {
                if (!initialized) {
                    init();
                }

                final K key = leftKeyExtractor.apply(t);

                // Local, not a field: this mapper instance is shared across parallel worker threads.
                final D val = map.get(key);

                if (val == null && !map.containsKey(key)) {
                    //noinspection resource
                    return function.apply(t, Stream.<U> empty().collect(downstream));
                } else {
                    return function.apply(t, val);
                }
            }

            private void init() {
                if (!initialized) {
                    final Stream<T> currentStream = AbstractStream.this;

                    if (currentStream.isParallel()) {
                        synchronized (this) {
                            if (!initialized) {
                                //    map = Stream.of(b)
                                //            .parallel(ps.maxThreadNum(), ps.splitStrategy(), ps.asyncExecutor())
                                //            .toMap(rightKeyExtractor, Fn.<U> identity(), downstream);   // TODO may not be necessary.

                                map = groupRightSide((Stream<U>) b, rightKeyExtractor, downstream);

                                initialized = true;
                            }
                        }
                    } else {
                        map = groupRightSide((Stream<U>) b, rightKeyExtractor, downstream);

                        initialized = true;
                    }
                }
            }
        };

        //noinspection resource
        return map(mapper).onClose(newCloseHandler(b));
    }

    /**
     * Returns a stream whose elements are those of the stream built by {@code supplier}, built on first traversal.
     * Used for the tails that {@code fullJoin}/{@code rightJoin(Stream, ..)} and {@code joinByRange(..,
     * mapperForUnJoinedElements)} append after the joined elements.
     *
     * <p>Unlike {@link Stream#defer(Supplier)}, whose close handler invokes the supplier so that a never-traversed
     * supplied stream can still be closed, closing this stream never invokes {@code supplier}: the tail suppliers
     * have side effects (they drain the right-hand source, or call the user's unjoined-elements mapper), so closing
     * the join result early, {@code limit(0)}, or a failure of the left stream must not run them - that drained
     * {@code b} for nothing and let a secondary failure replace the real one. The stream built by a traversal is
     * closed with this stream (a mapper-supplied stream may carry close handlers), provided it was built before the
     * close started: a close from another thread that races the build may miss it. Sources such as {@code b} keep
     * their own close handlers on the join result, so they are closed exactly once either way.
     *
     * @param <R> the element type
     * @param supplier builds the tail stream; may return {@code null} for an empty tail
     * @return a lazily built stream that closes the built stream, if any, when closed
     */
    static <R> Stream<R> deferTail(final Supplier<? extends Stream<? extends R>> supplier) {
        // The tail may be built on a parallel worker thread and closed on the caller's thread. Known window:
        // a close() that runs while supplier.get() is still executing - only a cross-thread close of the join result
        // during a running terminal - finds nothing built yet, and the tail built afterwards is not closed by this
        // handler. The join terminals close the result after the traversal, so every single-threaded path closes the
        // tail; only a mapper-supplied tail with its own close handlers could be affected.
        final AtomicReference<Stream<? extends R>> built = new AtomicReference<>();

        return Stream.<R> of(ObjIteratorEx.<R> defer(() -> {
            final Stream<? extends R> s = supplier.get();
            built.set(s);
            return s == null ? ObjIteratorEx.<R> empty() : s.iteratorEx();
        })).onClose(() -> {
            @SuppressWarnings("resource")
            final Stream<? extends R> s = built.get();

            if (s != null) {
                s.close();
            }
        });
    }

    /**
     * Builds the right-side index of a {@code groupJoin} over a stream, closing {@code b} afterwards like the
     * terminal {@code groupTo} this replaces did. A failure of the grouping (the right key extractor, the downstream
     * collector or {@code b}'s own upstream) wins over a failing close handler of {@code b}, which is added to it as
     * a suppressed exception.
     *
     * @see #groupRightSide(Iterator, Function, Collector)
     */
    static <U, K, D> Map<K, D> groupRightSide(final Stream<U> b, final Function<? super U, ? extends K> keyExtractor,
            final Collector<? super U, ?, D> downstream) {
        try {
            return groupRightSide(b.iteratorEx(), keyExtractor, downstream);
        } catch (final Throwable e) { // NOSONAR
            // let the primary failure win over a close failure of b (LST/C-114 at a site the
            // C-011 rewrite created); the innerJoin/leftJoin(Stream) paths get this from the terminal b.toList().
            b.closeAfterFailure(e);
            throw e;
        } finally {
            b.close();
        }
    }

    /**
     * Groups the right side of a {@code groupJoin} by key. Unlike {@code groupTo}, which rejects a {@code null} key,
     * this accepts it: every {@code groupJoin} overload documents that keys use ordinary map equality, so a
     * {@code null} right key must match a {@code null} left key (as it does in {@code innerJoin}/{@code leftJoin}).
     *
     * @param <U> the right element type
     * @param <K> the key type
     * @param <D> the downstream result type
     * @param iterator the right-side elements
     * @param keyExtractor extracts the join key of a right element; may return {@code null}
     * @param downstream reduces the right elements sharing a key
     * @return a {@code HashMap} from key to the reduced right elements
     */
    @SuppressWarnings("unchecked")
    static <U, K, D> Map<K, D> groupRightSide(final Iterator<? extends U> iterator, final Function<? super U, ? extends K> keyExtractor,
            final Collector<? super U, ?, D> downstream) {
        final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
        final BiConsumer<Object, ? super U> downstreamAccumulator = (BiConsumer<Object, ? super U>) downstream.accumulator();
        final Map<K, Object> result = new HashMap<>();
        K key = null;
        Object container = null;
        U next = null;

        while (iterator.hasNext()) {
            next = iterator.next();
            key = keyExtractor.apply(next);

            if ((container = result.get(key)) == null) {
                container = downstreamSupplier.get();
                result.put(key, container);
            }

            downstreamAccumulator.accept(container, next);
        }

        if (!downstream.characteristics().contains(Collector.Characteristics.IDENTITY_FINISH)) {
            final Function<Object, Object> downstreamFinisher = (Function<Object, Object>) downstream.finisher();

            result.replaceAll((k, v) -> downstreamFinisher.apply(v));
        }

        return (Map<K, D>) result;
    }

    @Override
    public <U> Stream<Pair<T, List<U>>> joinByRange(final Iterator<U> b, final BiPredicate<? super T, ? super U> predicate)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "Iterator 'b' cannot be null");
        checkArgNotNull(predicate, cs.predicate);

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
            return sequential().map(mapper).parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return map(mapper);
        }
    }

    @Override
    public <U, R> Stream<Pair<T, R>> joinByRange(final Iterator<U> b, final BiPredicate<? super T, ? super U> predicate,
            final Collector<? super U, ?, R> collector) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return joinByRange(b, predicate, collector, Fn.pair());
    }

    @Override
    public <U, D, R> Stream<R> joinByRange(final Iterator<U> b, final BiPredicate<? super T, ? super U> predicate, final Collector<? super U, ?, D> collector,
            final BiFunction<? super T, ? super D, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "Iterator 'b' cannot be null");
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(collector, cs.collector);
        checkArgNotNull(function, cs.function);

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
                        return function.apply(t, finisher.apply(container));
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

                return function.apply(t, finisher.apply(container));
            }
        };

        if (isParallel()) {
            //noinspection resource
            return sequential().map(mapper).parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return map(mapper);
        }
    }

    @Override
    public <U, D, R> Stream<R> joinByRange(final Iterator<U> b, final BiPredicate<? super T, ? super U> predicate, final Collector<? super U, ?, D> collector,
            final BiFunction<? super T, ? super D, ? extends R> function, final Function<Iterator<U>, Stream<R>> mapperForUnJoinedElements)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "Iterator 'b' cannot be null");
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(collector, cs.collector);
        checkArgNotNull(function, cs.function);
        checkArgNotNull(mapperForUnJoinedElements, cs.mapperForUnJoinedElements);

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
                        return function.apply(t, finisher.apply(container));
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

                return function.apply(t, finisher.apply(container));
            }
        };

        if (isParallel()) {
            //noinspection resource
            return sequential().map(mapper)
                    // value == none also occurs when this (left) stream was empty and the mapper never
                    // ran: the untouched iterator must still be routed to mapperForUnJoinedElements.
                    .append(deferTail(() -> nextValueHolder.value() == none ? (b.hasNext() ? mapperForUnJoinedElements.apply(b) : Stream.<R> empty())
                            : mapperForUnJoinedElements.apply(Iterators.concat(ObjIterator.of(nextValueHolder.value()), b))))
                    .parallel(maxThreadNum(), splitStrategy(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            //noinspection resource
            // value == none also occurs when this (left) stream was empty and the mapper never
            // ran: the untouched iterator must still be routed to mapperForUnJoinedElements.
            return map(mapper).append(deferTail(() -> nextValueHolder.value() == none ? (b.hasNext() ? mapperForUnJoinedElements.apply(b) : Stream.<R> empty())
                    : mapperForUnJoinedElements.apply(Iterators.concat(ObjIterator.of(nextValueHolder.value()), b))));
        }
    }

    @Override
    public <U> Stream<Pair<T, List<U>>> joinByRange(final Stream<U> b, final BiPredicate<? super T, ? super U> predicate)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "Stream 'b' cannot be null");
        checkArgNotNull(predicate, cs.predicate);

        return joinByRange(b.iteratorEx(), predicate).onClose(newCloseHandler(b));
    }

    @Override
    public <U, R> Stream<Pair<T, R>> joinByRange(final Stream<U> b, final BiPredicate<? super T, ? super U> predicate,
            final Collector<? super U, ?, R> collector) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        return joinByRange(b, predicate, collector, Fn.pair());
    }

    @Override
    public <U, D, R> Stream<R> joinByRange(final Stream<U> b, final BiPredicate<? super T, ? super U> predicate, final Collector<? super U, ?, D> collector,
            final BiFunction<? super T, ? super D, ? extends R> function) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "Stream 'b' cannot be null");
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(collector, cs.collector);
        checkArgNotNull(function, cs.function);

        return ((Stream<R>) joinByRange(b.iteratorEx(), predicate, collector, function)).onClose(newCloseHandler(b));
    }

    @Override
    public <U, D, R> Stream<R> joinByRange(final Stream<U> b, final BiPredicate<? super T, ? super U> predicate, final Collector<? super U, ?, D> collector,
            final BiFunction<? super T, ? super D, ? extends R> function, final Function<Iterator<U>, Stream<R>> mapperForUnJoinedElements)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();

        checkArgNotNull(b, "Stream 'b' cannot be null");
        checkArgNotNull(predicate, cs.predicate);
        checkArgNotNull(collector, cs.collector);
        checkArgNotNull(function, cs.function);
        checkArgNotNull(mapperForUnJoinedElements, cs.mapperForUnJoinedElements);

        return joinByRange(b.iteratorEx(), predicate, collector, function, mapperForUnJoinedElements).onClose(newCloseHandler(b));
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
