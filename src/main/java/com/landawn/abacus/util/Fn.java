/*
 * Copyright (c) 2017, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoubleBinaryOperator;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoubleFunction;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatBiPredicate;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiObjConsumer;
import com.landawn.abacus.util.function.IntBiObjFunction;
import com.landawn.abacus.util.function.IntBiObjPredicate;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.function.IntObjFunction;
import com.landawn.abacus.util.function.IntObjPredicate;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongBiPredicate;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.Runnable;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBiPredicate;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToBooleanFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.function.UnaryOperator;

/**
 * Factory utility class for functional interfaces.
 *
 * <br>
 * Note: Usually you shouldn't cache or reuse any Function/Predicate/Consumer/... created by calling the methods in this class.
 * These methods should be called every time.
 * </br>
 *
 * <pre>
 * <code>
 *
 * {@code Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);}
 * // Instead of
 * {@code Stream.of(map).filter(e -> e.getKey().equals("a") || e.getKey().equals("b")).toMap(e -> e.getKey(), e -> e.getValue());}
 * // Using Fn
 * {@code Stream.of(map).filter(Fn.testByKey(k -> k.equals("a") || k.equals("b"))).collect(Collectors.toMap());}
 *
 * </code>
 * </pre>
 *
 *
 *
 *
 */
@SuppressWarnings({ "java:S6539", "java:S1192", "java:S1221", "java:S1452", "java:S2445" })
public final class Fn {

    private static final Object NONE = ClassUtil.createNullMask();

    private static final Timer timer = new Timer();

    //    @SuppressWarnings("rawtypes")
    //    public static final IntFunction<Map<String, Object>> FACTORY_OF_MAP = (IntFunction) Factory.MAP_FACTORY;
    //
    //    @SuppressWarnings("rawtypes")
    //    public static final IntFunction<LinkedHashMap<String, Object>> FACTORY_OF_LINKED_HASH_MAP = (IntFunction) Factory.LINKED_HASH_MAP_FACTORY;
    //
    //    @SuppressWarnings("rawtypes")
    //    public static final Supplier<Map<String, Object>> SUPPLIER_OF_MAP = (Supplier) Suppliers.MAP;
    //
    //    @SuppressWarnings("rawtypes")
    //    public static final Supplier<LinkedHashMap<String, Object>> SUPPLIER_OF_LINKED_HASH_MAP = (Supplier) Suppliers.LINKED_HASH_MAP;

    private static final Runnable EMPTY_ACTION = () -> {
    };

    @SuppressWarnings("rawtypes")
    private static final Consumer EMPTY_CONSUMER = value -> {
        // do nothing.
    };

    private static final Consumer<AutoCloseable> CLOSE = IOUtil::close;

    private static final Consumer<AutoCloseable> CLOSE_QUIETLY = IOUtil::closeQuietly;

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_EQUAL = (key, value) -> N.println(Strings.concat(N.toString(key), "=", N.toString(value)));

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_HYPHEN = (key, value) -> N.println(Strings.concat(N.toString(key), "-", N.toString(value)));

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_UNDERSCORE = (key, value) -> N.println(Strings.concat(N.toString(key), "_", N.toString(value)));

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COLON = (key, value) -> N.println(Strings.concat(N.toString(key), ":", N.toString(value)));

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COLON_SPACE = (key, value) -> N.println(Strings.concat(N.toString(key), ": ", N.toString(value)));

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COMMA = (key, value) -> N.println(Strings.concat(N.toString(key), ",", N.toString(value)));

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COMMA_SPACE = (key, value) -> N.println(Strings.concat(N.toString(key), ", ", N.toString(value)));

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_EMPTY = (key, value) -> N.println(Strings.concat(N.toString(key), N.toString(value)));

    @SuppressWarnings("rawtypes")
    private static final Consumer PRINTLN = N::println;

    @SuppressWarnings("rawtypes")
    private static final Function TO_STRING = N::toString;

    private static final UnaryOperator<String> TO_CAMEL_CASE = Strings::toCamelCase;

    private static final UnaryOperator<String> TO_LOWER_CASE = Strings::toLowerCase;

    private static final UnaryOperator<String> TO_LOWER_CASE_WITH_UNDERSCORE = Strings::toLowerCaseWithUnderscore;

    private static final UnaryOperator<String> TO_UPPER_CASE = Strings::toUpperCase;

    private static final UnaryOperator<String> TO_UPPER_CASE_WITH_UNDERSCORE = Strings::toUpperCaseWithUnderscore;

    private static final Function<Throwable, RuntimeException> TO_RUNTIME_EXCEPTION = e -> ExceptionUtil.toRuntimeException(e, true);

    @SuppressWarnings("rawtypes")
    private static final BiFunction<Comparable, Comparable, Integer> COMPARE = N::compare;

    @SuppressWarnings("rawtypes")
    private static final Function IDENTITY = t -> t;

    private static final UnaryOperator<String> TRIM = Strings::trim;

    private static final UnaryOperator<String> TRIM_TO_EMPTY = Strings::trimToEmpty;

    private static final UnaryOperator<String> TRIM_TO_NULL = Strings::trimToNull;

    private static final UnaryOperator<String> STRIP = Strings::strip;

    private static final UnaryOperator<String> STRIP_TO_EMPTY = Strings::stripToEmpty;

    private static final UnaryOperator<String> STRIP_TO_NULL = Strings::stripToNull;

    private static final UnaryOperator<String> NULL_TO_EMPTY = t -> t == null ? Strings.EMPTY : t;

    @SuppressWarnings("rawtypes")
    private static final UnaryOperator<List> NULL_TO_EMPTY_LIST = t -> t == null ? N.emptyList() : t;

    @SuppressWarnings("rawtypes")
    private static final UnaryOperator<Set> NULL_TO_EMPTY_SET = t -> t == null ? N.emptySet() : t;

    @SuppressWarnings("rawtypes")
    private static final UnaryOperator<Map> NULL_TO_EMPTY_MAP = t -> t == null ? N.emptyMap() : t;

    private static final Function<CharSequence, Integer> LENGTH = t -> t == null ? 0 : t.length();

    private static final Function<Object[], Integer> LEN = t -> t == null ? 0 : t.length;

    @SuppressWarnings("rawtypes")
    private static final Function<Collection, Integer> SIZE = t -> t == null ? 0 : t.size();

    @SuppressWarnings("rawtypes")
    private static final Function<Map, Integer> SIZE_MAP = t -> t == null ? 0 : t.size();

    private static final Function<Map.Entry<Object, Object>, Object> KEY = Entry::getKey;

    private static final Function<Map.Entry<Object, Object>, Object> VALUE = Entry::getValue;

    private static final Function<Pair<Object, Object>, Object> LEFT = Pair::getLeft;

    private static final Function<Pair<Object, Object>, Object> RIGHT = Pair::getRight;

    private static final Function<Map.Entry<Object, Object>, Map.Entry<Object, Object>> INVERSE = t -> new ImmutableEntry<>(t.getValue(), t.getKey());

    private static final BiFunction<Object, Object, Map.Entry<Object, Object>> ENTRY = ImmutableEntry::new;

    private static final BiFunction<Object, Object, Pair<Object, Object>> PAIR = Pair::of;

    private static final TriFunction<Object, Object, Object, Triple<Object, Object, Object>> TRIPLE = Triple::of;

    private static final Function<Object, Tuple1<Object>> TUPLE_1 = Tuple::of;

    private static final BiFunction<Object, Object, Tuple2<Object, Object>> TUPLE_2 = Tuple::of;

    private static final TriFunction<Object, Object, Object, Tuple3<Object, Object, Object>> TUPLE_3 = Tuple::of;

    private static final QuadFunction<Object, Object, Object, Object, Tuple4<Object, Object, Object, Object>> TUPLE_4 = Tuple::of;

    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_TRUE = value -> true;

    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_FALSE = value -> false;

    @SuppressWarnings("rawtypes")
    private static final Predicate IS_NULL = Objects::isNull;

    private static final Predicate<CharSequence> IS_EMPTY = Strings::isEmpty;

    private static final Predicate<CharSequence> IS_BLANK = Strings::isBlank;

    @SuppressWarnings("rawtypes")
    private static final Predicate NOT_NULL = Objects::nonNull;

    private static final Predicate<CharSequence> IS_NOT_EMPTY = Strings::isNotEmpty;

    private static final Predicate<CharSequence> IS_NOT_BLANK = Strings::isNotBlank;

    private static final Predicate<File> IS_FILE = file -> file != null && file.isFile();

    private static final Predicate<File> IS_DIRECTORY = file -> file != null && file.isDirectory();

    public static final ToBooleanFunction<OptionalBoolean> GET_AS_BOOLEAN = OptionalBoolean::get;

    public static final ToCharFunction<OptionalChar> GET_AS_CHAR = OptionalChar::get;

    public static final ToByteFunction<OptionalByte> GET_AS_BYTE = OptionalByte::get;

    public static final ToShortFunction<OptionalShort> GET_AS_SHORT = OptionalShort::get;

    public static final ToIntFunction<OptionalInt> GET_AS_INT = OptionalInt::get;

    public static final ToLongFunction<OptionalLong> GET_AS_LONG = OptionalLong::get;

    public static final ToFloatFunction<OptionalFloat> GET_AS_FLOAT = OptionalFloat::get;

    public static final ToDoubleFunction<OptionalDouble> GET_AS_DOUBLE = OptionalDouble::get;

    public static final ToIntFunction<java.util.OptionalInt> GET_AS_INT_JDK = java.util.OptionalInt::getAsInt;

    public static final ToLongFunction<java.util.OptionalLong> GET_AS_LONG_JDK = java.util.OptionalLong::getAsLong;

    public static final ToDoubleFunction<java.util.OptionalDouble> GET_AS_DOUBLE_JDK = java.util.OptionalDouble::getAsDouble;

    public static final Predicate<OptionalBoolean> IS_PRESENT_BOOLEAN = OptionalBoolean::isPresent;

    public static final Predicate<OptionalChar> IS_PRESENT_CHAR = OptionalChar::isPresent;

    public static final Predicate<OptionalByte> IS_PRESENT_BYTE = OptionalByte::isPresent;

    public static final Predicate<OptionalShort> IS_PRESENT_SHORT = OptionalShort::isPresent;

    public static final Predicate<OptionalInt> IS_PRESENT_INT = OptionalInt::isPresent;

    public static final Predicate<OptionalLong> IS_PRESENT_LONG = OptionalLong::isPresent;

    public static final Predicate<OptionalFloat> IS_PRESENT_FLOAT = OptionalFloat::isPresent;

    public static final Predicate<OptionalDouble> IS_PRESENT_DOUBLE = OptionalDouble::isPresent;

    public static final Predicate<java.util.OptionalInt> IS_PRESENT_INT_JDK = java.util.OptionalInt::isPresent;

    public static final Predicate<java.util.OptionalLong> IS_PRESENT_LONG_JDK = java.util.OptionalLong::isPresent;

    public static final Predicate<java.util.OptionalDouble> IS_PRESENT_DOUBLE_JDK = java.util.OptionalDouble::isPresent;

    Fn() {
        // for extension.
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param supplier
    //     * @return
    //     */
    //    public static <T> T get(final java.util.function.Supplier<T> supplier) {
    //        return supplier.get();
    //    }

    /**
     * Returns a {@code Supplier} which returns a single instance created by calling the specified {@code supplier.get()}.
     *
     * @param <T>
     * @param supplier
     * @return
     */
    public static <T> Supplier<T> memoize(final java.util.function.Supplier<T> supplier) {
        return LazyInitializer.of(supplier);
    }

    // Copied from Google Guava under Apache License v2.

    /**
     * Copied from Google Guava under Apache License v2.
     * <br />
     * <br />
     *
     * Returns a supplier that caches the instance supplied by the delegate and removes the cached
     * value after the specified time has passed. Subsequent calls to {@code get()} return the cached
     * value if the expiration time has not passed. After the expiration time, a new value is
     * retrieved, cached, and returned. See: <a
     * href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
     *
     * <p>The returned supplier is thread-safe. The supplier's serialized form does not contain the
     * cached value, which will be recalculated when {@code get()} is called on the reserialized
     * instance. The actual memoization does not happen when the underlying delegate throws an
     * exception.
     *
     * <p>When the underlying delegate throws an exception, then this memorizing supplier will keep
     * delegating calls until it returns valid data.
     *
     * @param <T>
     * @param supplier
     * @param duration the length of time after a value is created that it should stop being returned
     *     by subsequent {@code get()} calls
     * @param unit the unit that {@code duration} is expressed in
     * @return
     * @throws IllegalArgumentException if {@code duration} is not positive
     */
    public static <T> Supplier<T> memoizeWithExpiration(final java.util.function.Supplier<T> supplier, final long duration, final TimeUnit unit)
            throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.Supplier);
        N.checkArgument(duration > 0, "duration (%s %s) must be > 0", duration, unit);

        return new Supplier<>() {
            private final java.util.function.Supplier<T> delegate = supplier;
            private final long durationNanos = unit.toNanos(duration);
            private volatile T value;
            // The special value 0 means "not yet initialized".
            private volatile long expirationNanos = 0;

            @Override
            public T get() {
                // Another variant of Double-Checked Locking.
                //
                // We use two volatile reads. We could reduce this to one by
                // putting our fields into a holder class, but (at least on x86)
                // the extra memory consumption and indirection are more
                // expensive than the extra volatile reads.
                long nanos = expirationNanos;
                final long now = System.nanoTime();
                if (nanos == 0 || now - nanos >= 0) {
                    synchronized (this) {
                        if (nanos == expirationNanos) { // recheck for lost race
                            final T t = delegate.get();
                            value = t;
                            nanos = now + durationNanos;
                            // In the very unlikely event that nanos is 0, set it to 1;
                            // no one will notice 1 ns of tardiness.
                            expirationNanos = (nanos == 0) ? 1 : nanos;
                            return t;
                        }
                    }
                }

                // This is safe because we checked `expirationNanos`.
                return value;
            }
        };
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <R>
    //     * @param func
    //     * @return a stateful {@code IntFunction}. Don't save or cache for reuse, but it can be used in parallel stream.
    //     */
    //    @Beta
    //    @Stateful
    //    public static <R> IntFunction<R> memoize(final IntFunction<? extends R> func) {
    //        return new IntFunction<R>() {
    //            private volatile R resultForNull = (R) NONE;
    //
    //            @Override
    //            public R apply(int t) {
    //                R result = resultForNull;
    //
    //                if (result == NONE) {
    //                    synchronized (this) {
    //                        if (resultForNull == NONE) {
    //                            resultForNull = func.apply(t);
    //                        }
    //
    //                        result = resultForNull;
    //                    }
    //                }
    //
    //                return result;
    //            }
    //        };
    //    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param func
     * @return
     */
    public static <T, R> Function<T, R> memoize(final java.util.function.Function<? super T, ? extends R> func) {
        return new Function<>() {
            private final R none = (R) NONE;
            private final Map<T, R> resultMap = new ConcurrentHashMap<>();
            private volatile R resultForNull = none; //NOSONAR

            @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
            @Override
            public R apply(final T t) {
                R result = null;

                if (t == null) {
                    result = resultForNull;

                    if (result == none) {
                        synchronized (this) {
                            if (resultForNull == none) {
                                resultForNull = func.apply(null);
                            }

                            result = resultForNull;
                        }
                    }
                } else {
                    result = resultMap.get(t);

                    if (result == null) {
                        result = func.apply(t);
                        resultMap.put(t, result == null ? none : result);
                    }
                }

                return result == none ? null : result;
            }
        };
    }

    //    /**
    //     * Only for temporary use in sequential stream/single thread, not for parallel stream/multiple threads.
    //     * The returned Collection will clean up before it's returned every time when {@code get} is called.
    //     * Don't save the returned Collection object or use it to save objects.
    //     *
    //     * @param <T>
    //     * @param <C>
    //     * @param supplier
    //     * @return a stateful {@code Supplier}. Don't save or cache for reuse or use it in parallel stream.
    //     * @see {@code Stream.split/sliding};
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @Beta
    //    @SequentialOnly
    //    @Stateful
    //    public static <T, C extends Collection<T>> Supplier<? extends C> reuse(final java.util.function.Supplier<? extends C> supplier) {
    //        return new Supplier<>() {
    //            private C c;
    //
    //            @Override
    //            public C get() {
    //                if (c == null) {
    //                    c = supplier.get();
    //                } else if (c.size() > 0) {
    //                    c.clear();
    //                }
    //
    //                return c;
    //            }
    //        };
    //    }
    //
    //    /**
    //     * Only for temporary use in sequential stream/single thread, not for parallel stream/multiple threads.
    //     * The returned Collection will clean up before it's returned every time when {@code get} is called.
    //     * Don't save the returned Collection object or use it to save objects.
    //     *
    //     * @param <T>
    //     * @param <C>
    //     * @param supplier
    //     * @return a stateful {@code IntFunction}. Don't save or cache for reuse or use it in parallel stream.
    //     * @see {@code Stream.split/sliding};
    //     * @deprecated
    //     */
    //    @Deprecated
    //    @Beta
    //    @SequentialOnly
    //    @Stateful
    //    public static <T, C extends Collection<T>> IntFunction<? extends C> reuse(final java.util.function.IntFunction<? extends C> supplier) {
    //        return new IntFunction<>() {
    //            private C c;
    //
    //            @Override
    //            public C apply(int size) {
    //                if (c == null) {
    //                    c = supplier.apply(size);
    //                } else if (c.size() > 0) {
    //                    c.clear();
    //                }
    //
    //                return c;
    //            }
    //        };
    //    }

    /**
     *
     * @param closeable
     * @return
     */
    public static Runnable close(final AutoCloseable closeable) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.close(closeable);
            }
        };
    }

    /**
     *
     * @param a
     * @return
     */
    public static Runnable closeAll(final AutoCloseable... a) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeAll(a);
            }
        };
    }

    /**
     *
     * @param c
     * @return
     */
    public static Runnable closeAll(final Collection<? extends AutoCloseable> c) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeAll(c);
            }
        };
    }

    /**
     *
     * @param closeable
     * @return
     */
    public static Runnable closeQuietly(final AutoCloseable closeable) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeQuietly(closeable);
            }
        };
    }

    /**
     * Close all quietly.
     *
     * @param a
     * @return
     */
    public static Runnable closeAllQuietly(final AutoCloseable... a) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeAllQuietly(a);
            }
        };
    }

    /**
     * Close all quietly.
     *
     * @param c
     * @return
     */
    public static Runnable closeAllQuietly(final Collection<? extends AutoCloseable> c) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeAllQuietly(c);
            }
        };
    }

    /**
     * Returns an empty {@code Runnable} which does nothing.
     *
     * @return an empty {@code Runnable} which does nothing.
     */
    public static Runnable emptyAction() {
        return EMPTY_ACTION;
    }

    /**
     *
     * @param service
     * @return
     */
    public static Runnable shutDown(final ExecutorService service) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                service.shutdown();
            }
        };
    }

    /**
     *
     * @param service
     * @param terminationTimeout
     * @param timeUnit
     * @return
     */
    public static Runnable shutDown(final ExecutorService service, final long terminationTimeout, final TimeUnit timeUnit) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                try {
                    service.shutdown();
                    //noinspection ResultOfMethodCallIgnored
                    service.awaitTermination(terminationTimeout, timeUnit);
                } catch (final InterruptedException e) {
                    // ignore.
                }
            }
        };
    }

    /**
     * Returns an empty {@code Consumer} which does nothing.
     *
     * @param <T>
     * @return an empty {@code Consumer} which does nothing.
     * @deprecated Use {@link #emptyConsumer()} instead.
     * @see #emptyConsumer()
     */
    public static <T> Consumer<T> doNothing() {
        return EMPTY_CONSUMER;
    }

    /**
     * Returns an empty {@code Consumer} which does nothing.
     *
     * @param <T>
     * @return an empty {@code Consumer} which does nothing.
     */
    public static <T> Consumer<T> emptyConsumer() {
        return EMPTY_CONSUMER;
    }

    /**
     *
     * @param <T>
     * @param errorMessage
     * @return
     */
    public static <T> Consumer<T> throwRuntimeException(final String errorMessage) {
        return t -> {
            throw new RuntimeException(errorMessage);
        };
    }

    /**
     *
     * @param <T>
     * @param exceptionSupplier
     * @return
     */
    public static <T> Consumer<T> throwException(final java.util.function.Supplier<? extends RuntimeException> exceptionSupplier) {
        return t -> {
            throw exceptionSupplier.get();
        };
    }

    public static Function<Throwable, RuntimeException> toRuntimeException() {
        return TO_RUNTIME_EXCEPTION;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends AutoCloseable> Consumer<T> close() {
        return (Consumer<T>) CLOSE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends AutoCloseable> Consumer<T> closeQuietly() {
        return (Consumer<T>) CLOSE_QUIETLY;
    }

    /**
     *
     * @param <T>
     * @param millis
     * @return
     */
    public static <T> Consumer<T> sleep(final long millis) {
        return t -> N.sleep(millis);
    }

    /**
     *
     * @param <T>
     * @param millis
     * @return
     */
    public static <T> Consumer<T> sleepUninterruptibly(final long millis) {
        return t -> N.sleepUninterruptibly(millis);
    }

    /**
     * Returns a stateful {@code Consumer}. Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T>
     * @param permitsPerSecond
     * @return a stateful {@code Consumer}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @see RateLimiter#acquire()
     * @see RateLimiter#create(double)
     */
    @Stateful
    public static <T> Consumer<T> rateLimiter(final double permitsPerSecond) {
        return rateLimiter(RateLimiter.create(permitsPerSecond));
    }

    /**
     * Returns a stateful {@code Consumer}. Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T>
     * @param rateLimiter
     * @return a stateful {@code Consumer}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @see RateLimiter#acquire()
     */
    @Stateful
    public static <T> Consumer<T> rateLimiter(final RateLimiter rateLimiter) {
        return t -> rateLimiter.acquire();
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Consumer<T> println() {
        return PRINTLN;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param separator
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> BiConsumer<T, U> println(final String separator) throws IllegalArgumentException {
        N.checkArgNotNull(separator);

        switch (separator) { // NOSONAR
            case "=":
                return PRINTLN_EQUAL;

            case ":":
                return PRINTLN_COLON;

            case ": ":
                return PRINTLN_COLON_SPACE;

            case "-":
                return PRINTLN_HYPHEN;

            case "_":
                return PRINTLN_UNDERSCORE;

            case ",":
                return PRINTLN_COMMA;

            case ", ":
                return PRINTLN_COMMA_SPACE;

            case "":
                return PRINTLN_EMPTY;

            default:
                return (t, u) -> N.println(t + separator + u);
        }
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Function<T, String> toStr() {
        return TO_STRING;
    }

    /**
     * To camel case.
     *
     * @return
     */
    public static UnaryOperator<String> toCamelCase() {
        return TO_CAMEL_CASE;
    }

    /**
     * To lower case.
     *
     * @return
     */
    public static UnaryOperator<String> toLowerCase() {
        return TO_LOWER_CASE;
    }

    /**
     * To lower case with underscore.
     *
     * @return
     */
    public static UnaryOperator<String> toLowerCaseWithUnderscore() {
        return TO_LOWER_CASE_WITH_UNDERSCORE;
    }

    /**
     * To upper case.
     *
     * @return
     */
    public static UnaryOperator<String> toUpperCase() {
        return TO_UPPER_CASE;
    }

    /**
     * To upper case with underscore.
     *
     * @return
     */
    public static UnaryOperator<String> toUpperCaseWithUnderscore() {
        return TO_UPPER_CASE_WITH_UNDERSCORE;
    }

    @SuppressWarnings("rawtypes")
    private static final Function TO_JSON = N::toJson;

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Function<T, String> toJson() {
        return TO_JSON;
    }

    @SuppressWarnings("rawtypes")
    private static final Function TO_XML = N::toXml;

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Function<T, String> toXml() {
        return TO_XML;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Function<T, T> identity() {
        return IDENTITY;
    }

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, T> Function<T, Keyed<K, T>> keyed(final java.util.function.Function<? super T, K> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return t -> Keyed.of(keyExtractor.apply(t), t);
    }

    private static final Function<Keyed<?, Object>, Object> VAL = Keyed::val;

    private static final Function<Map.Entry<Keyed<Object, Object>, Object>, Object> KK_VAL = t -> t.getKey().val();

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, T> Function<Keyed<K, T>, T> val() {
        return (Function) VAL;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T, K, V> Function<Map.Entry<Keyed<K, T>, V>, T> kkv() {
        return (Function) KK_VAL;
    }

    private static final Function<Object, Wrapper<Object>> WRAP = Wrapper::of;

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Wrapper<T>> wrap() {
        return (Function) WRAP;
    }

    /**
     *
     * @param <T>
     * @param hashFunction
     * @param equalsFunction
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Function<T, Wrapper<T>> wrap(final java.util.function.ToIntFunction<? super T> hashFunction,
            final java.util.function.BiPredicate<? super T, ? super T> equalsFunction) throws IllegalArgumentException {
        N.checkArgNotNull(hashFunction);
        N.checkArgNotNull(equalsFunction);

        return t -> Wrapper.of(t, hashFunction, equalsFunction);
    }

    private static final Function<Wrapper<Object>, Object> UNWRAP = Wrapper::value;

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<Wrapper<T>, T> unwrap() {
        return (Function) UNWRAP;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, K> key() {
        return (Function) KEY;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, V> value() {
        return (Function) VALUE;
    }

    /**
     *
     * @param <L>
     * @param <R>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <L, R> Function<Pair<L, R>, L> left() {
        return (Function) LEFT;
    }

    /**
     *
     * @param <L>
     * @param <R>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <L, R> Function<Pair<L, R>, R> right() {
        return (Function) RIGHT;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, Entry<V, K>> inverse() {
        return (Function) INVERSE;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> BiFunction<K, V, Map.Entry<K, V>> entry() {
        return (BiFunction) ENTRY;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @return
     * @deprecated replaced by {@code Fn#entryWithKey(Object)}
     */
    @Deprecated
    public static <K, V> Function<V, Map.Entry<K, V>> entry(final K key) {
        return entryWithKey(key);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyExtractor
     * @return
     * @deprecated replaced by {@code Fn#entryByKeyMapper(Function)}
     */
    @Deprecated
    public static <K, V> Function<V, Map.Entry<K, V>> entry(final java.util.function.Function<? super V, K> keyExtractor) {
        return entryByKeyMapper(keyExtractor);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @return
     */
    public static <K, V> Function<V, Map.Entry<K, V>> entryWithKey(final K key) {
        return v -> new ImmutableEntry<>(key, v);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Function<V, Map.Entry<K, V>> entryByKeyMapper(final java.util.function.Function<? super V, K> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return v -> new ImmutableEntry<>(keyExtractor.apply(v), v);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param value
     * @return
     */
    public static <K, V> Function<K, Map.Entry<K, V>> entryWithValue(final V value) {
        return k -> new ImmutableEntry<>(k, value);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param valueExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Function<K, Map.Entry<K, V>> entryByValueMapper(final java.util.function.Function<? super K, V> valueExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(valueExtractor);

        return k -> new ImmutableEntry<>(k, valueExtractor.apply(k));
    }

    /**
     *
     * @param <L>
     * @param <R>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <L, R> BiFunction<L, R, Pair<L, R>> pair() {
        return (BiFunction) PAIR;
    }

    /**
     *
     * @param <L>
     * @param <M>
     * @param <R>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <L, M, R> TriFunction<L, M, R, Triple<L, M, R>> triple() {
        return (TriFunction) TRIPLE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Tuple1<T>> tuple1() {
        return (Function) TUPLE_1;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T, U> BiFunction<T, U, Tuple2<T, U>> tuple2() {
        return (BiFunction) TUPLE_2;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <A, B, C> TriFunction<A, B, C, Tuple3<A, B, C>> tuple3() {
        return (TriFunction) TUPLE_3;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @return
     */
    @SuppressWarnings({ "rawtypes" })
    public static <A, B, C, D> QuadFunction<A, B, C, D, Tuple4<A, B, C, D>> tuple4() {
        return (QuadFunction) TUPLE_4;
    }

    public static UnaryOperator<String> trim() {
        return TRIM;
    }

    public static UnaryOperator<String> trimToEmpty() {
        return TRIM_TO_EMPTY;
    }

    public static UnaryOperator<String> trimToNull() {
        return TRIM_TO_NULL;
    }

    public static UnaryOperator<String> strip() {
        return STRIP;
    }

    public static UnaryOperator<String> stripToEmpty() {
        return STRIP_TO_EMPTY;
    }

    public static UnaryOperator<String> stripToNull() {
        return STRIP_TO_NULL;
    }

    public static UnaryOperator<String> nullToEmpty() {
        return NULL_TO_EMPTY;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @return
    //     * @deprecated replaced by {@code nullToEmptyList}
    //     */
    //    @Deprecated
    //    @SuppressWarnings("rawtypes")
    //    public static <T> UnaryOperator<List<T>> nullToEmptyL() {
    //        return (UnaryOperator) NULL_TO_EMPTY_LIST;
    //    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> UnaryOperator<List<T>> nullToEmptyList() {
        return (UnaryOperator) NULL_TO_EMPTY_LIST;
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @return
    //     * @deprecated replaced by {@code nullToEmptySet}
    //     */
    //    @Deprecated
    //    @SuppressWarnings("rawtypes")
    //    public static <T> UnaryOperator<Set<T>> nullToEmptyS() {
    //        return (UnaryOperator) NULL_TO_EMPTY_SET;
    //    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> UnaryOperator<Set<T>> nullToEmptySet() {
        return (UnaryOperator) NULL_TO_EMPTY_SET;
    }

    //    /**
    //     *
    //     * @param <K> the key type
    //     * @param <V> the value type
    //     * @return
    //     * @deprecated replaced by {@code nullToEmptyMap}
    //     */
    //    @Deprecated
    //    @SuppressWarnings("rawtypes")
    //    public static <K, V> UnaryOperator<Map<K, V>> nullToEmptyM() {
    //        return (UnaryOperator) NULL_TO_EMPTY_MAP;
    //    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> UnaryOperator<Map<K, V>> nullToEmptyMap() {
        return (UnaryOperator) NULL_TO_EMPTY_MAP;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T[], Integer> len() {
        return (Function) LEN;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Function<T, Integer> length() {
        return (Function<T, Integer>) LENGTH;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Function<T, Integer> size() {
        return (Function<T, Integer>) SIZE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Function<T, Integer> sizeM() {
        return (Function<T, Integer>) SIZE_MAP;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param clazz
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> Function<T, U> cast(final Class<U> clazz) throws IllegalArgumentException {
        N.checkArgNotNull(clazz);

        return t -> (U) t;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Checks if is {@code null}.
     *
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> isNull() {
        return IS_NULL;
    }

    /**
     * Checks if is {@code null}.
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> isNull(final java.util.function.Function<T, ?> valueExtractor) {
        return t -> valueExtractor.apply(t) == null;
    }

    /**
     * Checks if is {@code null} or empty.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Predicate<T> isEmpty() {
        return (Predicate<T>) IS_EMPTY;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> isEmpty(final java.util.function.Function<T, ? extends CharSequence> valueExtractor) {
        return t -> Strings.isEmpty(valueExtractor.apply(t));
    }

    /**
     * Checks if is {@code null} or empty or blank.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Predicate<T> isBlank() {
        return (Predicate<T>) IS_BLANK;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> isBlank(final java.util.function.Function<T, ? extends CharSequence> valueExtractor) {
        return t -> Strings.isBlank(valueExtractor.apply(t));
    }

    private static final Predicate<Object[]> IS_EMPTY_A = value -> value == null || value.length == 0;

    /**
     *
     * @param <T>
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<T[]> isEmptyA() {
        return (Predicate) IS_EMPTY_A;
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Collection> IS_EMPTY_C = value -> value == null || value.size() == 0;

    /**
     *
     * @param <T>
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Predicate<T> isEmptyC() {
        return (Predicate<T>) IS_EMPTY_C;
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Map> IS_EMPTY_M = value -> value == null || value.isEmpty();

    /**
     *
     * @param <T>
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Predicate<T> isEmptyM() {
        return (Predicate<T>) IS_EMPTY_M;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> notNull() {
        return NOT_NULL;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> notNull(final java.util.function.Function<T, ?> valueExtractor) {
        return t -> valueExtractor.apply(t) != null;
    }

    /**
     * Not {@code null} or empty.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Predicate<T> notEmpty() {
        return (Predicate<T>) IS_NOT_EMPTY;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> notEmpty(final java.util.function.Function<T, ? extends CharSequence> valueExtractor) {
        return t -> Strings.isNotEmpty(valueExtractor.apply(t));
    }

    /**
     * Not {@code null} or empty or blank.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Predicate<T> notBlank() {
        return (Predicate<T>) IS_NOT_BLANK;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> notBlank(final java.util.function.Function<T, ? extends CharSequence> valueExtractor) {
        return t -> Strings.isNotBlank(valueExtractor.apply(t));
    }

    private static final Predicate<Object[]> NOT_EMPTY_A = value -> value != null && value.length > 0;

    /**
     *
     * @param <T>
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<T[]> notEmptyA() {
        return (Predicate) NOT_EMPTY_A;
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Collection> NOT_EMPTY_C = value -> value != null && value.size() > 0;

    /**
     *
     * @param <T>
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Predicate<T> notEmptyC() {
        return (Predicate<T>) NOT_EMPTY_C;
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Map> NOT_EMPTY_M = value -> value != null && !value.isEmpty();

    /**
     *
     * @param <T>
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Predicate<T> notEmptyM() {
        return (Predicate<T>) NOT_EMPTY_M;
    }

    /**
     * Checks if it is a file.
     *
     * @return
     */
    public static Predicate<File> isFile() {
        return IS_FILE;
    }

    /**
     * Checks if it is directory.
     *
     * @return
     */
    public static Predicate<File> isDirectory() {
        return IS_DIRECTORY;
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T> Predicate<T> equal(final Object target) {
        return value -> N.equals(value, target);
    }

    /**
     *
     * @param <T>
     * @param targetValue1
     * @param targetValue2
     * @return
     */
    public static <T> Predicate<T> eqOr(final Object targetValue1, final Object targetValue2) {
        return value -> N.equals(value, targetValue1) || N.equals(value, targetValue2);
    }

    /**
     *
     * @param <T>
     * @param targetValue1
     * @param targetValue2
     * @param targetValue3
     * @return
     */
    public static <T> Predicate<T> eqOr(final Object targetValue1, final Object targetValue2, final Object targetValue3) {
        return value -> N.equals(value, targetValue1) || N.equals(value, targetValue2) || N.equals(value, targetValue3);
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T> Predicate<T> notEqual(final Object target) {
        return value -> !N.equals(value, target);
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> greaterThan(final T target) {
        return value -> N.compare(value, target) > 0;
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> greaterEqual(final T target) {
        return value -> N.compare(value, target) >= 0;
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> lessThan(final T target) {
        return value -> N.compare(value, target) < 0;
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> lessEqual(final T target) {
        return value -> N.compare(value, target) <= 0;
    }

    /**
     * Checks if the value/element: {@code minValue < e < maxValue}.
     *
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> gtAndLt(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }

    /**
     * Checks if the value/element: {@code minValue <= e < maxValue}.
     *
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> geAndLt(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) >= 0 && N.compare(value, maxValue) < 0;
    }

    /**
     * Checks if the value/element: {@code minValue <= e <= maxValue}.
     *
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> geAndLe(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) >= 0 && N.compare(value, maxValue) <= 0;
    }

    /**
     * Checks if the value/element: {@code minValue < e <= maxValue}.
     *
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> gtAndLe(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) <= 0;
    }

    /**
     * Checks if the value/element: {@code minValue < e < maxValue}.
     *
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     * @deprecated replaced by {@code gtAndLt}.
     */
    @Deprecated
    public static <T extends Comparable<? super T>> Predicate<T> between(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Predicate<T> in(final Collection<?> c) throws IllegalArgumentException {
        N.checkArgNotNull(c);

        final boolean isNotEmpty = N.notEmpty(c);

        return value -> isNotEmpty && c.contains(value);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Predicate<T> notIn(final Collection<?> c) throws IllegalArgumentException {
        N.checkArgNotNull(c);

        final boolean isEmpty = N.isEmpty(c);

        return value -> isEmpty || !c.contains(value);
    }

    /**
     *
     * @param <T>
     * @param clazz
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Predicate<T> instanceOf(final Class<?> clazz) throws IllegalArgumentException {
        N.checkArgNotNull(clazz);

        return clazz::isInstance;
    }

    /**
     *
     * @param clazz
     * @return
     * @throws IllegalArgumentException
     */
    public static Predicate<Class<?>> subtypeOf(final Class<?> clazz) throws IllegalArgumentException {
        N.checkArgNotNull(clazz);

        return clazz::isAssignableFrom;
    }

    /**
     *
     * @param prefix
     * @return
     * @throws IllegalArgumentException
     */
    public static Predicate<String> startsWith(final String prefix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);

        return value -> value != null && value.startsWith(prefix);
    }

    /**
     *
     * @param suffix
     * @return
     * @throws IllegalArgumentException
     */
    public static Predicate<String> endsWith(final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(suffix);

        return value -> value != null && value.endsWith(suffix);
    }

    /**
     *
     * @param valueToFind
     * @return
     * @throws IllegalArgumentException
     */
    public static Predicate<String> contains(final String valueToFind) throws IllegalArgumentException {
        N.checkArgNotNull(valueToFind);

        return value -> value != null && value.contains(valueToFind);
    }

    /**
     * Not starts with.
     *
     * @param prefix
     * @return
     * @throws IllegalArgumentException
     */
    public static Predicate<String> notStartsWith(final String prefix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);

        return value -> value == null || !value.startsWith(prefix);
    }

    /**
     * Not ends with.
     *
     * @param suffix
     * @return
     * @throws IllegalArgumentException
     */
    public static Predicate<String> notEndsWith(final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(suffix);

        return value -> value == null || !value.endsWith(suffix);
    }

    /**
     *
     * @param str
     * @return
     * @throws IllegalArgumentException
     */
    public static Predicate<String> notContains(final String str) throws IllegalArgumentException {
        N.checkArgNotNull(str);

        return value -> value == null || !value.contains(str);
    }

    /**
     *
     * @param pattern
     * @return
     * @throws IllegalArgumentException
     */
    public static Predicate<CharSequence> matches(final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern);

        return value -> pattern.matcher(value).find();
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @return
     */
    public static <T, U> BiPredicate<T, U> equal() {
        return BiPredicates.EQUAL;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @return
     */
    public static <T, U> BiPredicate<T, U> notEqual() {
        return BiPredicates.NOT_EQUAL;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> greaterThan() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_THAN;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> greaterEqual() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_EQUAL;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> lessThan() {
        return (BiPredicate<T, T>) BiPredicates.LESS_THAN;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> lessEqual() {
        return (BiPredicate<T, T>) BiPredicates.LESS_EQUAL;
    }

    /**
     *
     * @param <T>
     * @param predicate
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Predicate<T> not(final java.util.function.Predicate<T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return t -> !predicate.test(t);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param biPredicate
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> BiPredicate<T, U> not(final java.util.function.BiPredicate<T, U> biPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(biPredicate);

        return (t, u) -> !biPredicate.test(t, u);
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param triPredicate
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C> TriPredicate<A, B, C> not(final TriPredicate<A, B, C> triPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(triPredicate);

        return (a, b, c) -> !triPredicate.test(a, b, c);
    }

    /**
     *
     * @param first
     * @param second
     * @return
     * @throws IllegalArgumentException
     */
    public static BooleanSupplier and(final java.util.function.BooleanSupplier first, final java.util.function.BooleanSupplier second)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return () -> first.getAsBoolean() && second.getAsBoolean();
    }

    /**
     *
     * @param first
     * @param second
     * @param third
     * @return
     * @throws IllegalArgumentException
     */
    public static BooleanSupplier and(final java.util.function.BooleanSupplier first, final java.util.function.BooleanSupplier second,
            final java.util.function.BooleanSupplier third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return () -> first.getAsBoolean() && second.getAsBoolean() && third.getAsBoolean();
    }

    /**
     *
     * @param <T>
     * @param first
     * @param second
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Predicate<T> and(final java.util.function.Predicate<? super T> first, final java.util.function.Predicate<? super T> second)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return t -> first.test(t) && second.test(t);
    }

    /**
     *
     * @param <T>
     * @param first
     * @param second
     * @param third
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Predicate<T> and(final java.util.function.Predicate<? super T> first, final java.util.function.Predicate<? super T> second,
            final java.util.function.Predicate<? super T> third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return t -> first.test(t) && second.test(t) && third.test(t);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code c} is {@code null} or empty.
     */
    public static <T> Predicate<T> and(final Collection<? extends java.util.function.Predicate<? super T>> c) throws IllegalArgumentException {
        N.checkArgNotEmpty(c, cs.c);

        return t -> {
            for (final java.util.function.Predicate<? super T> p : c) {
                if (!p.test(t)) {
                    return false;
                }
            }

            return true;
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param first
     * @param second
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> BiPredicate<T, U> and(final java.util.function.BiPredicate<? super T, ? super U> first,
            final java.util.function.BiPredicate<? super T, ? super U> second) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return (t, u) -> first.test(t, u) && second.test(t, u);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param first
     * @param second
     * @param third
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> BiPredicate<T, U> and(final java.util.function.BiPredicate<? super T, ? super U> first,
            final java.util.function.BiPredicate<? super T, ? super U> second, final BiPredicate<? super T, ? super U> third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return (t, u) -> first.test(t, u) && second.test(t, u) && third.test(t, u);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code c} is {@code null} or empty.
     */
    public static <T, U> BiPredicate<T, U> and(final List<? extends java.util.function.BiPredicate<? super T, ? super U>> c) throws IllegalArgumentException {
        N.checkArgNotEmpty(c, cs.c);

        return (t, u) -> {
            for (final java.util.function.BiPredicate<? super T, ? super U> p : c) {
                if (!p.test(t, u)) {
                    return false;
                }
            }

            return true;
        };
    }

    /**
     *
     * @param first
     * @param second
     * @return
     * @throws IllegalArgumentException
     */
    public static BooleanSupplier or(final java.util.function.BooleanSupplier first, final java.util.function.BooleanSupplier second)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return () -> first.getAsBoolean() || second.getAsBoolean();
    }

    /**
     *
     * @param first
     * @param second
     * @param third
     * @return
     * @throws IllegalArgumentException
     */
    public static BooleanSupplier or(final java.util.function.BooleanSupplier first, final java.util.function.BooleanSupplier second,
            final java.util.function.BooleanSupplier third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return () -> first.getAsBoolean() || second.getAsBoolean() || third.getAsBoolean();
    }

    /**
     *
     * @param <T>
     * @param first
     * @param second
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Predicate<T> or(final java.util.function.Predicate<? super T> first, final java.util.function.Predicate<? super T> second)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return t -> first.test(t) || second.test(t);
    }

    /**
     *
     * @param <T>
     * @param first
     * @param second
     * @param third
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Predicate<T> or(final java.util.function.Predicate<? super T> first, final java.util.function.Predicate<? super T> second,
            final java.util.function.Predicate<? super T> third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return t -> first.test(t) || second.test(t) || third.test(t);
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code c} is {@code null} or empty.
     */
    public static <T> Predicate<T> or(final Collection<? extends java.util.function.Predicate<? super T>> c) throws IllegalArgumentException {
        N.checkArgNotEmpty(c, cs.c);

        return t -> {
            for (final java.util.function.Predicate<? super T> p : c) {
                if (p.test(t)) {
                    return true;
                }
            }

            return false;
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param first
     * @param second
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> BiPredicate<T, U> or(final java.util.function.BiPredicate<? super T, ? super U> first,
            final java.util.function.BiPredicate<? super T, ? super U> second) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return (t, u) -> first.test(t, u) || second.test(t, u);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param first
     * @param second
     * @param third
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> BiPredicate<T, U> or(final java.util.function.BiPredicate<? super T, ? super U> first,
            final java.util.function.BiPredicate<? super T, ? super U> second, final java.util.function.BiPredicate<? super T, ? super U> third)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return (t, u) -> first.test(t, u) || second.test(t, u) || third.test(t, u);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code c} is {@code null} or empty.
     */
    public static <T, U> BiPredicate<T, U> or(final List<? extends java.util.function.BiPredicate<? super T, ? super U>> c) throws IllegalArgumentException {
        N.checkArgNotEmpty(c, cs.c);

        return (t, u) -> {
            for (final java.util.function.BiPredicate<? super T, ? super U> p : c) {
                if (p.test(t, u)) {
                    return true;
                }
            }

            return false;
        };
    }

    /**
     * Test by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testByKey(final java.util.function.Predicate<? super K> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return entry -> predicate.test(entry.getKey());
    }

    /**
     * Test by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testByValue(final java.util.function.Predicate<? super V> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return entry -> predicate.test(entry.getValue());
    }

    /**
     * Accept by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptByKey(final java.util.function.Consumer<? super K> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer);

        return entry -> consumer.accept(entry.getKey());
    }

    /**
     * Accept by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptByValue(final java.util.function.Consumer<? super V> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer);

        return entry -> consumer.accept(entry.getValue());
    }

    /**
     * Apply by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R>
     * @param func
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyByKey(final java.util.function.Function<? super K, ? extends R> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> func.apply(entry.getKey());
    }

    /**
     * Apply by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R>
     * @param func
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyByValue(final java.util.function.Function<? super V, ? extends R> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> func.apply(entry.getValue());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <KK>
     * @param func
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V, KK> Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapKey(final java.util.function.Function<? super K, ? extends KK> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> new ImmutableEntry<>(func.apply(entry.getKey()), entry.getValue());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <VV>
     * @param func
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V, VV> Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapValue(final java.util.function.Function<? super V, ? extends VV> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> new ImmutableEntry<>(entry.getKey(), func.apply(entry.getValue()));
    }

    /**
     * Test key val.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testKeyVal(final java.util.function.BiPredicate<? super K, ? super V> predicate)
            throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return entry -> predicate.test(entry.getKey(), entry.getValue());
    }

    /**
     * Accept key val.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptKeyVal(final java.util.function.BiConsumer<? super K, ? super V> consumer)
            throws IllegalArgumentException {
        N.checkArgNotNull(consumer);

        return entry -> consumer.accept(entry.getKey(), entry.getValue());
    }

    /**
     * Apply key val.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R>
     * @param func
     * @return
     * @throws IllegalArgumentException
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyKeyVal(final java.util.function.BiFunction<? super K, ? super V, ? extends R> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> func.apply(entry.getKey(), entry.getValue());
    }

    /**
     *
     * @param <T>
     * @param consumer
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> Consumer<T> acceptIfNotNull(final java.util.function.Consumer<? super T> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer);

        return t -> {
            if (t != null) {
                consumer.accept(t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param predicate
     * @param consumer
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> Consumer<T> acceptIf(final java.util.function.Predicate<? super T> predicate, final java.util.function.Consumer<? super T> consumer)
            throws IllegalArgumentException {
        N.checkArgNotNull(predicate);
        N.checkArgNotNull(consumer);

        return t -> {
            if (predicate.test(t)) {
                consumer.accept(t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param predicate
     * @param consumerForTrue
     * @param consumerForFalse
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> Consumer<T> acceptIfOrElse(final java.util.function.Predicate<? super T> predicate,
            final java.util.function.Consumer<? super T> consumerForTrue, final java.util.function.Consumer<? super T> consumerForFalse)
            throws IllegalArgumentException {
        N.checkArgNotNull(predicate);
        N.checkArgNotNull(consumerForTrue);
        N.checkArgNotNull(consumerForFalse);

        return t -> {
            if (predicate.test(t)) {
                consumerForTrue.accept(t);
            } else {
                consumerForFalse.accept(t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param mapper
     * @return
     */
    @Beta
    public static <T, R> Function<T, Collection<R>> applyIfNotNullOrEmpty(final java.util.function.Function<T, ? extends Collection<R>> mapper) {
        return t -> t == null ? N.emptyList() : mapper.apply(t);
    }

    /**
     * Apply if not {@code null} or default.
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param defaultValue
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, R> Function<A, R> applyIfNotNullOrDefault(final java.util.function.Function<A, B> mapperA,
            final java.util.function.Function<B, ? extends R> mapperB, final R defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);

        return a -> {
            if (a == null) {
                return defaultValue;
            }

            final B b = mapperA.apply(a);

            if (b == null) {
                return defaultValue;
            } else {
                return mapperB.apply(b);
            }
        };
    }

    /**
     * Apply if not {@code null} or default.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param mapperC
     * @param defaultValue
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C, R> Function<A, R> applyIfNotNullOrDefault(final java.util.function.Function<A, B> mapperA,
            final java.util.function.Function<B, C> mapperB, final java.util.function.Function<C, ? extends R> mapperC, final R defaultValue)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);
        N.checkArgNotNull(mapperC);

        return a -> {
            if (a == null) {
                return defaultValue;
            }

            final B b = mapperA.apply(a);

            if (b == null) {
                return defaultValue;
            }

            final C c = mapperB.apply(b);

            if (c == null) {
                return defaultValue;
            } else {
                return mapperC.apply(c);
            }
        };
    }

    /**
     * Apply if not {@code null} or default.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param mapperC
     * @param mapperD
     * @param defaultValue
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C, D, R> Function<A, R> applyIfNotNullOrDefault(final java.util.function.Function<A, B> mapperA,
            final java.util.function.Function<B, C> mapperB, final java.util.function.Function<C, D> mapperC,
            final java.util.function.Function<D, ? extends R> mapperD, final R defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);
        N.checkArgNotNull(mapperC);
        N.checkArgNotNull(mapperD);

        return a -> {
            if (a == null) {
                return defaultValue;
            }

            final B b = mapperA.apply(a);

            if (b == null) {
                return defaultValue;
            }

            final C c = mapperB.apply(b);

            if (c == null) {
                return defaultValue;
            }

            final D d = mapperC.apply(c);

            if (d == null) {
                return defaultValue;
            } else {
                return mapperD.apply(d);
            }
        };
    }

    /**
     * Apply if not {@code null} or get.
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, R> Function<A, R> applyIfNotNullOrElseGet(final java.util.function.Function<A, B> mapperA,
            final java.util.function.Function<B, ? extends R> mapperB, final java.util.function.Supplier<R> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);

        return a -> {
            if (a == null) {
                return supplier.get();
            }

            final B b = mapperA.apply(a);

            if (b == null) {
                return supplier.get();
            } else {
                return mapperB.apply(b);
            }
        };
    }

    /**
     * Apply if not {@code null} or get.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param mapperC
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C, R> Function<A, R> applyIfNotNullOrElseGet(final java.util.function.Function<A, B> mapperA,
            final java.util.function.Function<B, C> mapperB, final java.util.function.Function<C, ? extends R> mapperC,
            final java.util.function.Supplier<R> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);
        N.checkArgNotNull(mapperC);

        return a -> {
            if (a == null) {
                return supplier.get();
            }

            final B b = mapperA.apply(a);

            if (b == null) {
                return supplier.get();
            }

            final C c = mapperB.apply(b);

            if (c == null) {
                return supplier.get();
            } else {
                return mapperC.apply(c);
            }
        };
    }

    /**
     * Apply if not {@code null} or get.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param mapperC
     * @param mapperD
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C, D, R> Function<A, R> applyIfNotNullOrElseGet(final java.util.function.Function<A, B> mapperA,
            final java.util.function.Function<B, C> mapperB, final java.util.function.Function<C, D> mapperC,
            final java.util.function.Function<D, ? extends R> mapperD, final java.util.function.Supplier<R> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);
        N.checkArgNotNull(mapperC);
        N.checkArgNotNull(mapperD);

        return a -> {
            if (a == null) {
                return supplier.get();
            }

            final B b = mapperA.apply(a);

            if (b == null) {
                return supplier.get();
            }

            final C c = mapperB.apply(b);

            if (c == null) {
                return supplier.get();
            }

            final D d = mapperC.apply(c);

            if (d == null) {
                return supplier.get();
            } else {
                return mapperD.apply(d);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param predicate
     * @param func
     * @param defaultValue
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T, R> Function<T, R> applyIfOrElseDefault(final java.util.function.Predicate<? super T> predicate,
            final java.util.function.Function<? super T, ? extends R> func, final R defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);
        N.checkArgNotNull(func);

        return t -> {
            if (predicate.test(t)) {
                return func.apply(t);
            } else {
                return defaultValue;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param predicate
     * @param func
     * @param supplier
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T, R> Function<T, R> applyIfOrElseGet(final java.util.function.Predicate<? super T> predicate,
            final java.util.function.Function<? super T, ? extends R> func, final java.util.function.Supplier<? extends R> supplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(predicate);
        N.checkArgNotNull(func);
        N.checkArgNotNull(supplier);

        return t -> {
            if (predicate.test(t)) {
                return func.apply(t);
            } else {
                return supplier.get();
            }
        };
    }

    private static final Function<Map<Object, Collection<Object>>, List<Map<Object, Object>>> FLAT_MAP_VALUE_FUNC = Maps::flatToMap;

    /**
     * {a=[1, 2, 3], b=[4, 5, 6], c=[7, 8]} -> [{a=1, b=4, c=7}, {a=2, b=5, c=8}, {a=3, b=6}].
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @see Maps#flatToMap(Map)
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Map<K, ? extends Collection<V>>, List<Map<K, V>>> flatmapValue() {
        return (Function) FLAT_MAP_VALUE_FUNC;
    }

    private static final ToByteFunction<String> PARSE_BYTE_FUNC = Numbers::toByte;

    /**
     * Parses the byte.
     *
     * @return
     */
    public static ToByteFunction<String> parseByte() {
        return PARSE_BYTE_FUNC;
    }

    private static final ToShortFunction<String> PARSE_SHORT_FUNC = Numbers::toShort;

    /**
     * Parses the short.
     *
     * @return
     */
    public static ToShortFunction<String> parseShort() {
        return PARSE_SHORT_FUNC;
    }

    private static final ToIntFunction<String> PARSE_INT_FUNC = Numbers::toInt;

    /**
     * Parses the int.
     *
     * @return
     */
    public static ToIntFunction<String> parseInt() {
        return PARSE_INT_FUNC;
    }

    private static final ToLongFunction<String> PARSE_LONG_FUNC = Numbers::toLong;

    /**
     * Parses the long.
     *
     * @return
     */
    public static ToLongFunction<String> parseLong() {
        return PARSE_LONG_FUNC;
    }

    private static final ToFloatFunction<String> PARSE_FLOAT_FUNC = Numbers::toFloat;

    /**
     * Parses the float.
     *
     * @return
     */
    public static ToFloatFunction<String> parseFloat() {
        return PARSE_FLOAT_FUNC;
    }

    private static final ToDoubleFunction<String> PARSE_DOUBLE_FUNC = Numbers::toDouble;

    /**
     * Parses the double.
     *
     * @return
     */
    public static ToDoubleFunction<String> parseDouble() {
        return PARSE_DOUBLE_FUNC;
    }

    private static final Function<String, Number> CREATE_NUMBER_FUNC = t -> Strings.isEmpty(t) ? null : Numbers.createNumber(t);

    /**
     * Creates the number.
     *
     * @return
     * @see Numbers#createNumber(String)
     */
    public static Function<String, Number> createNumber() {
        return CREATE_NUMBER_FUNC;
    }

    /**
     * Num to int.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToIntFunction<T> numToInt() {
        return (ToIntFunction) ToIntFunction.FROM_NUM;
    }

    /**
     * Num to long.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToLongFunction<T> numToLong() {
        return (ToLongFunction) ToLongFunction.FROM_NUM;
    }

    /**
     * Num to double.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToDoubleFunction<T> numToDouble() {
        return (ToDoubleFunction) ToDoubleFunction.FROM_NUM;
    }

    /**
     *
     * @param <T>
     * @param count
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> atMost(final int count) throws IllegalArgumentException {
        // TODO cnt or atMost? skip(atMost(n)/limit(atMots(n)/dropWhile(atMost(n)/takeWhile(atMost(n)
        // TODO cnt or atMost? skip(cnt(n)/limit(cnt(n)/dropWhile(cnt(n)/takeWhile(cnt(n)
        // public static <T> Predicate<T> cnt(final int count) {

        N.checkArgNotNegative(count, cs.count);

        return new Predicate<>() {
            private final AtomicInteger counter = new AtomicInteger(count);

            @Override
            public boolean test(final T t) {
                return counter.getAndDecrement() > 0;
            }
        };
    }

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T>
     * @param limit
     * @param predicate
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> limitThenFilter(final int limit, final java.util.function.Predicate<T> predicate) throws IllegalArgumentException {
        N.checkArgNotNegative(limit, cs.limit);
        N.checkArgNotNull(predicate);

        return new Predicate<>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(final T t) {
                return counter.getAndDecrement() > 0 && predicate.test(t);
            }
        };
    }

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T>
     * @param <U>
     * @param limit
     * @param predicate
     * @return a stateful {@code BiPredicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException
     */
    @Beta
    @Stateful
    public static <T, U> BiPredicate<T, U> limitThenFilter(final int limit, final java.util.function.BiPredicate<T, U> predicate)
            throws IllegalArgumentException {
        N.checkArgNotNegative(limit, cs.limit);
        N.checkArgNotNull(predicate);

        return new BiPredicate<>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(final T t, final U u) {
                return counter.getAndDecrement() > 0 && predicate.test(t, u);
            }
        };
    }

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T>
     * @param predicate
     * @param limit
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> filterThenLimit(final java.util.function.Predicate<T> predicate, final int limit) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);
        N.checkArgNotNegative(limit, cs.limit);

        return new Predicate<>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(final T t) {
                return predicate.test(t) && counter.getAndDecrement() > 0;
            }
        };
    }

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T>
     * @param <U>
     * @param predicate
     * @param limit
     * @return a stateful {@code BiPredicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException
     */
    @Beta
    @Stateful
    public static <T, U> BiPredicate<T, U> filterThenLimit(final java.util.function.BiPredicate<T, U> predicate, final int limit)
            throws IllegalArgumentException {
        N.checkArgNotNull(predicate);
        N.checkArgNotNegative(limit, cs.limit);

        return new BiPredicate<>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(final T t, final U u) {
                return predicate.test(t, u) && counter.getAndDecrement() > 0;
            }
        };
    }

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T>
     * @param timeInMillis
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> timeLimit(final long timeInMillis) throws IllegalArgumentException {
        N.checkArgNotNegative(timeInMillis, cs.timeInMillis);

        if (timeInMillis == 0) {
            return Fn.alwaysFalse();
        }

        final MutableBoolean ongoing = MutableBoolean.of(true);

        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ongoing.setFalse();
            }
        };

        timer.schedule(task, timeInMillis);

        return t -> ongoing.value();
    }

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T>
     * @param duration
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> timeLimit(final Duration duration) throws IllegalArgumentException {
        N.checkArgNotNull(duration, cs.duration);

        return timeLimit(duration.toMillis());
    }

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @return a stateful {@code Function}. Don't save or cache for reuse or use it in parallel stream.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Function<T, Indexed<T>> indexed() {
        return new Function<>() {
            private final MutableLong idx = new MutableLong(0);

            @Override
            public Indexed<T> apply(final T t) {
                return Indexed.of(t, idx.getAndIncrement());
            }
        };
    }

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @param predicate
     * @return a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Predicate<T> indexed(final IntObjPredicate<T> predicate) {
        return Predicates.indexed(predicate);
    }

    /** The Constant RETURN_FIRST. */
    private static final BinaryOperator<Object> RETURN_FIRST = (a, b) -> a;

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> BinaryOperator<T> selectFirst() {
        return (BinaryOperator<T>) RETURN_FIRST;
    }

    /** The Constant RETURN_SECOND. */
    private static final BinaryOperator<Object> RETURN_SECOND = (a, b) -> b;

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> BinaryOperator<T> selectSecond() {
        return (BinaryOperator<T>) RETURN_SECOND;
    }

    /** The Constant MIN. */
    @SuppressWarnings({ "rawtypes" })
    private static final BinaryOperator<Comparable> MIN = (a, b) -> Comparators.NULL_LAST_COMPARATOR.compare(a, b) <= 0 ? a : b;

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Comparable<? super T>> BinaryOperator<T> min() {
        return (BinaryOperator) MIN;
    }

    /**
     *
     * @param <T>
     * @param comparator
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> BinaryOperator<T> min(final Comparator<? super T> comparator) throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        return (a, b) -> comparator.compare(a, b) <= 0 ? a : b;
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
    public static <T> BinaryOperator<T> minBy(final java.util.function.Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        final Comparator<? super T> comparator = Comparators.nullsLastBy(keyExtractor);

        return (a, b) -> comparator.compare(a, b) <= 0 ? a : b;
    }

    /** The Constant MIN_BY_KEY. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Map.Entry<Comparable, Object>> MIN_BY_KEY = new BinaryOperator<>() {
        private final Comparator<Comparable> cmp = Comparators.NULL_LAST_COMPARATOR;

        @Override
        public Entry<Comparable, Object> apply(final Entry<Comparable, Object> a, final Entry<Comparable, Object> b) {
            return cmp.compare(a.getKey(), b.getKey()) <= 0 ? a : b;
        }
    };

    /**
     *
     * @param <K>
     * @param <V>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K extends Comparable<? super K>, V> BinaryOperator<Map.Entry<K, V>> minByKey() {
        return (BinaryOperator) MIN_BY_KEY;
    }

    /** The Constant MIN_BY_VALUE. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Map.Entry<Object, Comparable>> MIN_BY_VALUE = new BinaryOperator<>() {
        private final Comparator<Comparable> cmp = Comparators.NULL_LAST_COMPARATOR;

        @Override
        public Entry<Object, Comparable> apply(final Entry<Object, Comparable> a, final Entry<Object, Comparable> b) {
            return cmp.compare(a.getValue(), b.getValue()) <= 0 ? a : b;
        }
    };

    /**
     *
     * @param <K>
     * @param <V>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V extends Comparable<? super V>> BinaryOperator<Map.Entry<K, V>> minByValue() {
        return (BinaryOperator) MIN_BY_VALUE;
    }

    /** The Constant MAX. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Comparable> MAX = (a, b) -> Comparators.NULL_FIRST_COMPARATOR.compare(a, b) >= 0 ? a : b;

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Comparable<? super T>> BinaryOperator<T> max() {
        return (BinaryOperator) MAX;
    }

    /**
     *
     * @param <T>
     * @param comparator
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> BinaryOperator<T> max(final Comparator<? super T> comparator) throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        return (a, b) -> comparator.compare(a, b) >= 0 ? a : b;
    }

    /**
     *
     * @param <T>
     * @param keyExtractor
     * @return
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
    public static <T> BinaryOperator<T> maxBy(final java.util.function.Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        final Comparator<? super T> comparator = Comparators.nullsFirstBy(keyExtractor);

        return (a, b) -> comparator.compare(a, b) >= 0 ? a : b;
    }

    /** The Constant MAX_BY_KEY. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Map.Entry<Comparable, Object>> MAX_BY_KEY = new BinaryOperator<>() {
        private final Comparator<Comparable> cmp = Comparators.NULL_FIRST_COMPARATOR;

        @Override
        public Entry<Comparable, Object> apply(final Entry<Comparable, Object> a, final Entry<Comparable, Object> b) {
            return cmp.compare(a.getKey(), b.getKey()) >= 0 ? a : b;
        }
    };

    /**
     *
     * @param <K>
     * @param <V>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K extends Comparable<? super K>, V> BinaryOperator<Map.Entry<K, V>> maxByKey() {
        return (BinaryOperator) MAX_BY_KEY;
    }

    /** The Constant MAX_BY_VALUE. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Map.Entry<Object, Comparable>> MAX_BY_VALUE = new BinaryOperator<>() {
        private final Comparator<Comparable> cmp = Comparators.NULL_FIRST_COMPARATOR;

        @Override
        public Entry<Object, Comparable> apply(final Entry<Object, Comparable> a, final Entry<Object, Comparable> b) {
            return cmp.compare(a.getValue(), b.getValue()) >= 0 ? a : b;
        }
    };

    /**
     *
     * @param <K>
     * @param <V>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V extends Comparable<? super V>> BinaryOperator<Map.Entry<K, V>> maxByValue() {
        return (BinaryOperator) MAX_BY_VALUE;
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Function<T, Integer> compareTo(final T target) {
        return t -> N.compare(t, target);
    }

    /**
     *
     * @param <T>
     * @param target
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Function<T, Integer> compareTo(final T target, final Comparator<? super T> cmp) throws IllegalArgumentException {
        // N.checkArgNotNull(cmp);

        final Comparator<? super T> cmpToUse = cmp == null ? (Comparator<? super T>) Comparators.naturalOrder() : cmp;

        return t -> cmpToUse.compare(t, target);
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable<? super T>> BiFunction<T, T, Integer> compare() {
        return (BiFunction) COMPARE;
    }

    /**
     *
     * @param <T>
     * @param cmp
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> BiFunction<T, T, Integer> compare(final Comparator<? super T> cmp) throws IllegalArgumentException {
        // N.checkArgNotNull(cmp);

        if (cmp == null || cmp == Comparators.naturalOrder()) { // NOSONAR
            return (BiFunction<T, T, Integer>) COMPARE;
        }

        return cmp::compare;
    }

    /**
     *
     * @param <T>
     * @param defaultValue
     * @return
     */
    @Beta
    public static <T> Function<Future<T>, T> futureGetOrDefaultOnError(final T defaultValue) {
        return f -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                // throw ExceptionUtil.toRuntimeException(e, true);
                return defaultValue;
            }
        };
    }

    private static final Function<Future<Object>, Object> FUTURE_GETTER = f -> {
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            return ExceptionUtil.toRuntimeException(e, true);
        }
    };

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <T> Function<Future<T>, T> futureGet() {
        return (Function) FUTURE_GETTER;
    }

    /**
     *
     * @param <T>
     * @param supplier
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<T> from(final java.util.function.Supplier<T> supplier) {
        return supplier instanceof Supplier ? ((Supplier) supplier) : supplier::get;
    }

    /**
     *
     * @param <T>
     * @param func
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<T> from(final java.util.function.IntFunction<? extends T> func) {
        return func instanceof IntFunction ? ((IntFunction) func) : func::apply;
    }

    /**
     *
     * @param <T>
     * @param predicate
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<T> from(final java.util.function.Predicate<T> predicate) {
        return predicate instanceof Predicate ? ((Predicate) predicate) : predicate::test;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param predicate
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, U> BiPredicate<T, U> from(final java.util.function.BiPredicate<T, U> predicate) {
        return predicate instanceof BiPredicate ? ((BiPredicate) predicate) : predicate::test;
    }

    /**
     *
     * @param <T>
     * @param consumer
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Consumer<T> from(final java.util.function.Consumer<T> consumer) {
        return consumer instanceof Consumer ? ((Consumer) consumer) : consumer::accept;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param consumer
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, U> BiConsumer<T, U> from(final java.util.function.BiConsumer<T, U> consumer) {
        return consumer instanceof BiConsumer ? ((BiConsumer) consumer) : consumer::accept;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param function
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, R> Function<T, R> from(final java.util.function.Function<T, ? extends R> function) {
        return function instanceof Function ? ((Function) function) : function::apply;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param function
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, U, R> BiFunction<T, U, R> from(final java.util.function.BiFunction<T, U, ? extends R> function) {
        return function instanceof BiFunction ? ((BiFunction) function) : function::apply;
    }

    /**
     *
     * @param <T>
     * @param op
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> UnaryOperator<T> from(final java.util.function.UnaryOperator<T> op) {
        return op instanceof UnaryOperator ? ((UnaryOperator) op) : op::apply;
    }

    /**
     *
     * @param <T>
     * @param op
     * @return
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> BinaryOperator<T> from(final java.util.function.BinaryOperator<T> op) {
        return op instanceof BinaryOperator ? ((BinaryOperator) op) : op::apply;
    }

    /**
     * <p>Returns the provided supplier as is - a shorthand identity method for suppliers.</p>
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code p()} for Predicate
     * and others.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * Supplier&lt;String&gt; supplier = () -&gt; "value";
     * // You can use:
     * var supplier = Fn.s(() -&gt; "value");
     * </pre>
     *
     * @param <T> the type of results supplied by the supplier
     * @param supplier the supplier to return
     * @return the supplier unchanged
     * @see #s(Object, Function)
     * @see #ss(com.landawn.abacus.util.Throwables.Supplier)
     * @see #ss(Object, com.landawn.abacus.util.Throwables.Function)
     * @see Suppliers#of(Supplier)
     * @see Suppliers#of(Object, Function)
     * @see IntFunctions#of(IntFunction)
     */
    @Beta
    public static <T> Supplier<T> s(final Supplier<T> supplier) {
        return supplier;
    }

    /**
     * <p>Returns a supplier that applies the given function to the provided argument.</p>
     *
     * <p>This method is a shorthand for creating a supplier from a function and an argument.
     * It can be useful when you want to create a supplier that computes a value based on
     * a specific input.</p>
     *
     * @param <A> the type of the input argument
     * @param <T> the type of the result
     * @param a the input argument
     * @param func the function to apply to the argument
     * @return a supplier that computes the result by applying the function to the argument
     * @see #s(Supplier)
     * @see #ss(com.landawn.abacus.util.Throwables.Supplier)
     * @see #ss(Object, com.landawn.abacus.util.Throwables.Function)
     * @see Suppliers#of(Supplier)
     * @see Suppliers#of(Object, Function)
     * @see IntFunctions#of(IntFunction)
     */
    @Beta
    public static <A, T> Supplier<T> s(final A a, final Function<? super A, ? extends T> func) {
        return () -> func.apply(a);
    }

    /**
     * <p>Returns the provided predicate as is - a shorthand identity method for predicates.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and others.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * Predicate&lt;String&gt; predicate = s -> s.length() > 5;
     * // You can use:
     * var predicate = Fn.p(s -> s.length() > 5);
     * </pre>
     *
     * @param <T> the type of the input to the predicate
     * @param predicate the predicate to return
     * @return the predicate unchanged
     * @see #p(Object, java.util.function.BiPredicate)
     */
    @Beta
    public static <T> Predicate<T> p(final Predicate<T> predicate) {
        return predicate;
    }

    /**
     * <p>Creates a predicate that tests the input against a fixed value using the provided bi-predicate.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the bi-predicate
     * to a fixed value, resulting in a predicate that only requires the second parameter. This is useful
     * for creating predicates that compare an input with a specific reference value.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a predicate that checks if a string contains a specific substring
     * String searchText = "error";
     * Predicate&lt;String&gt; containsError = Fn.p(searchText, String::contains);
     * 
     * boolean result = containsError.test("runtime error occurred"); // Returns true
     * boolean result2 = containsError.test("success"); // Returns false
     * </pre>
     *
     * @param <A> the type of the fixed first argument to the bi-predicate
     * @param <T> the type of the input to the resulting predicate
     * @param a the fixed value to use as the first argument to the bi-predicate
     * @param biPredicate the bi-predicate to apply with the fixed first argument
     * @return a predicate that applies the input as the second argument to the bi-predicate
     * @throws IllegalArgumentException if the biPredicate is null
     * @see #p(Predicate)
     */
    @Beta
    public static <A, T> Predicate<T> p(final A a, final java.util.function.BiPredicate<A, T> biPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(biPredicate);

        return t -> biPredicate.test(a, t);
    }

    /**
     * <p>Creates a predicate that tests inputs against two fixed values using the provided tri-predicate.</p>
     *
     * <p>This method implements partial application by binding the first two parameters of the tri-predicate
     * to fixed values, resulting in a predicate that only requires the third parameter. This is useful for
     * creating predicates that incorporate two reference values in their comparison logic.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a predicate that checks if a substring appears between two indices
     * String text = "error message";
     * Predicate&lt;Integer&gt; containsErrorBetween = 
     *     Fn.p(text, 0, (str, start, end) -> str.substring(start, end).contains("error"));
     *
     * boolean result = containsErrorBetween.test(5);  // Returns true
     * boolean result2 = containsErrorBetween.test(14); // Returns false
     * </pre>
     *
     * @param <A> the type of the first fixed argument to the tri-predicate
     * @param <B> the type of the second fixed argument to the tri-predicate
     * @param <T> the type of the input to the resulting predicate
     * @param a the first fixed value to use as an argument to the tri-predicate
     * @param b the second fixed value to use as an argument to the tri-predicate
     * @param triPredicate the tri-predicate to apply with the fixed arguments
     * @return a predicate that applies the input as the third argument to the tri-predicate
     * @throws IllegalArgumentException if the triPredicate is null
     */
    @Beta
    public static <A, B, T> Predicate<T> p(final A a, final B b, final TriPredicate<A, B, T> triPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(triPredicate);

        return t -> triPredicate.test(a, b, t);
    }

    /**
     * <p>Returns the provided bi-predicate as is - a shorthand identity method for bi-predicates.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * BiPredicate&lt;String, Integer&gt; biPredicate = (str, len) -> str.length() > len;
     * // You can use:
     * var biPredicate = Fn.p((String str, Integer len) -> str.length() > len);
     * </pre>
     *
     * @param <T> the type of the first input to the bi-predicate
     * @param <U> the type of the second input to the bi-predicate
     * @param biPredicate the bi-predicate to return
     * @return the bi-predicate unchanged
     * @see #p(Predicate)
     * @see #p(Object, java.util.function.BiPredicate)
     */
    @Beta
    public static <T, U> BiPredicate<T, U> p(final BiPredicate<T, U> biPredicate) {
        return biPredicate;
    }

    /**
     * <p>Creates a bi-predicate that tests inputs against a fixed value using the provided tri-predicate.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the tri-predicate
     * to a fixed value, resulting in a bi-predicate that only requires the second and third parameters.
     * This is useful for creating bi-predicates that incorporate a reference value in their comparison logic.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a bi-predicate that checks if a substring appears between two indices
     * String text = "error message";
     * BiPredicate&lt;Integer, Integer&gt; containsErrorBetween = 
     *     Fn.p(text, (str, start, end) -> str.substring(start, end).contains("error"));
     *
     * boolean result = containsErrorBetween.test(0, 5);  // Returns true
     * boolean result2 = containsErrorBetween.test(6, 14); // Returns false
     * </pre>
     *
     * @param <A> the type of the fixed first argument to the tri-predicate
     * @param <T> the type of the first input to the resulting bi-predicate
     * @param <U> the type of the second input to the resulting bi-predicate
     * @param a the fixed value to use as the first argument to the tri-predicate
     * @param triPredicate the tri-predicate to apply with the fixed first argument
     * @return a bi-predicate that applies the inputs as the second and third arguments to the tri-predicate
     * @throws IllegalArgumentException if the triPredicate is null
     * @see #p(BiPredicate)
     * @see #p(Object, java.util.function.BiPredicate)
     */
    @Beta
    public static <A, T, U> BiPredicate<T, U> p(final A a, final TriPredicate<A, T, U> triPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(triPredicate);

        return (t, u) -> triPredicate.test(a, t, u);
    }

    /**
     * <p>Returns the provided tri-predicate as is - a shorthand identity method for tri-predicates.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate and BiPredicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * TriPredicate&lt;String, Integer, Boolean&gt; triPredicate = 
     *     (str, len, flag) -> flag && str.length() > len;
     * // You can use:
     * var triPredicate = Fn.p((String str, Integer len, Boolean flag) -> 
     *     flag && str.length() > len);
     * </pre>
     *
     * @param <A> the type of the first input to the tri-predicate
     * @param <B> the type of the second input to the tri-predicate
     * @param <C> the type of the third input to the tri-predicate
     * @param triPredicate the tri-predicate to return
     * @return the tri-predicate unchanged
     * @see #p(Predicate)
     * @see #p(BiPredicate)
     * @see #p(Object, TriPredicate)
     */
    @Beta
    public static <A, B, C> TriPredicate<A, B, C> p(final TriPredicate<A, B, C> triPredicate) {
        return triPredicate;
    }

    /**
     * <p>Returns the provided consumer as is - a shorthand identity method for consumers.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * Consumer&lt;String&gt; logger = str -> System.out.println("Log: " + str);
     * // You can use:
     * var logger = Fn.c((String str) -> System.out.println("Log: " + str));
     * </pre>
     *
     * @param <T> the type of the input to the consumer
     * @param consumer the consumer to return
     * @return the consumer unchanged
     * @see #p(Predicate)
     * @see #s(Supplier)
     */
    @Beta
    public static <T> Consumer<T> c(final Consumer<T> consumer) {
        return consumer;
    }

    /**
     * <p>Creates a consumer that applies inputs along with a fixed value to the provided bi-consumer.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the bi-consumer
     * to a fixed value, resulting in a consumer that only requires the second parameter.
     * This is useful for creating consumers that incorporate a reference value in their logic.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a consumer that adds elements to a specific list
     * List&lt;String&gt; myList = new ArrayList&lt;&gt;();
     * Consumer&lt;String&gt; addToMyList = Fn.c(myList, (list, item) -> list.add(item));
     *
     * addToMyList.accept("first");  // Adds "first" to myList
     * addToMyList.accept("second"); // Adds "second" to myList
     * </pre>
     *
     * @param <A> the type of the fixed first argument to the bi-consumer
     * @param <T> the type of the input to the resulting consumer
     * @param a the fixed value to use as the first argument to the bi-consumer
     * @param biConsumer the bi-consumer to apply with the fixed first argument
     * @return a consumer that applies the input as the second argument to the bi-consumer
     * @throws IllegalArgumentException if the biConsumer is null
     * @see #c(Consumer)
     * @see #p(Object, java.util.function.BiPredicate)
     */
    @Beta
    public static <A, T> Consumer<T> c(final A a, final java.util.function.BiConsumer<A, T> biConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(biConsumer);

        return t -> biConsumer.accept(a, t);
    }

    /**
     * <p>Creates a consumer that applies inputs along with two fixed values to the provided tri-consumer.</p>
     *
     * <p>This method implements partial application by binding the first two parameters of the tri-consumer
     * to fixed values, resulting in a consumer that only requires the third parameter.
     * This is useful for creating consumers that incorporate two reference values in their logic.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a consumer that logs messages with a specific prefix and suffix
     * String prefix = "Log: ";
     * String suffix = " [end]";
     * Consumer&lt;String&gt; logWithPrefixAndSuffix = Fn.c(prefix, suffix, (p, s, msg) -> 
     *     System.out.println(p + msg + s));
     *
     * logWithPrefixAndSuffix.accept("Hello"); // Prints: Log: Hello [end]
     * </pre>
     *
     * @param <A> the type of the first fixed argument to the tri-consumer
     * @param <B> the type of the second fixed argument to the tri-consumer
     * @param <T> the type of the input to the resulting consumer
     * @param a the fixed value to use as the first argument to the tri-consumer
     * @param b the fixed value to use as the second argument to the tri-consumer
     * @param triConsumer the tri-consumer to apply with the fixed first and second arguments
     * @return a consumer that applies the input as the third argument to the tri-consumer
     * @throws IllegalArgumentException if the triConsumer is null
     */
    @Beta
    public static <A, B, T> Consumer<T> c(final A a, final B b, final TriConsumer<A, B, T> triConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(triConsumer);

        return t -> triConsumer.accept(a, b, t);
    }

    /**
     * <p>Returns the provided bi-consumer as is - a shorthand identity method for bi-consumers.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * BiConsumer&lt;String, Integer&gt; biConsumer = (str, len) -> System.out.println(str + " has length " + len);
     * // You can use:
     * var biConsumer = Fn.c((String str, Integer len) -> System.out.println(str + " has length " + len));
     * </pre>
     *
     * @param <T> the type of the first input to the bi-consumer
     * @param <U> the type of the second input to the bi-consumer
     * @param biConsumer the bi-consumer to return
     * @return the bi-consumer unchanged
     */
    @Beta
    public static <T, U> BiConsumer<T, U> c(final BiConsumer<T, U> biConsumer) {
        return biConsumer;
    }

    /**
     * <p>Creates a bi-consumer that applies inputs along with a fixed value to the provided tri-consumer.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the tri-consumer
     * to a fixed value, resulting in a bi-consumer that only requires the second and third parameters.
     * This is useful for creating bi-consumers that incorporate a reference value in their logic.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a bi-consumer that logs messages with a specific prefix
     * String prefix = "Log: ";
     * BiConsumer&lt;String, Integer&gt; logWithPrefix = Fn.c(prefix, (p, msg) -> 
     *     System.out.println(p + msg));
     *
     * logWithPrefix.accept("Hello", 1); // Prints: Log: Hello
     * </pre>
     *
     * @param <A> the type of the fixed first argument to the tri-consumer
     * @param <T> the type of the first input to the resulting bi-consumer
     * @param <U> the type of the second input to the resulting bi-consumer
     * @param a the fixed value to use as the first argument to the tri-consumer
     * @param triConsumer the tri-consumer to apply with the fixed first argument
     * @return a bi-consumer that applies the inputs as the second and third arguments to the tri-consumer
     * @throws IllegalArgumentException if the triConsumer is null
     */
    @Beta
    public static <A, T, U> BiConsumer<T, U> c(final A a, final TriConsumer<A, T, U> triConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(triConsumer);

        return (t, u) -> triConsumer.accept(a, t, u);
    }

    /**
     * <p>Returns the provided tri-consumer as is - a shorthand identity method for tri-consumers.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate and BiPredicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * TriConsumer&lt;String, Integer, Boolean&gt; triConsumer = 
     *     (str, len, flag) -> System.out.println(flag ? str + " is long" : str + " is short");
     * // You can use:
     * var triConsumer = Fn.c((String str, Integer len, Boolean flag) -> 
     *     System.out.println(flag ? str + " is long" : str + " is short"));
     * </pre>
     *
     * @param <A> the type of the first input to the tri-consumer
     * @param <B> the type of the second input to the tri-consumer
     * @param <C> the type of the third input to the tri-consumer
     * @param triConsumer the tri-consumer to return
     * @return the tri-consumer unchanged
     */
    @Beta
    public static <A, B, C> TriConsumer<A, B, C> c(final TriConsumer<A, B, C> triConsumer) {
        return triConsumer;
    }

    /**
     * <p>Returns the provided function as is - a shorthand identity method for functions.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * Function&lt;String, Integer&gt; lengthFunction = String::length;
     * // You can use:
     * var lengthFunction = Fn.f(String::length);
     * </pre>
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param function the function to return
     * @return the function unchanged
     */
    @Beta
    public static <T, R> Function<T, R> f(final Function<T, R> function) {
        return function;
    }

    /**
     * <p>Creates a function that applies the given argument to the provided function.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the function
     * to a fixed value, resulting in a function that only requires the second parameter. This is useful
     * for creating functions that incorporate a reference value in their computation logic.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a function that appends a fixed prefix to a string
     * String prefix = "Hello, ";
     * Function&lt;String, String&gt; appendPrefix = Fn.f(prefix, s -> prefix + s);
     *
     * String result = appendPrefix.apply("World"); // Returns "Hello, World"
     * </pre>
     *
     * @param <A> the type of the fixed first argument to the function
     * @param <T> the type of the input to the resulting function
     * @param <R> the type of the result of the resulting function
     * @param a the fixed value to use as the first argument to the function
     * @param biFunction the bi-function to apply with the fixed first argument
     * @return a function that applies the input as the second argument to the bi-function
     * @throws IllegalArgumentException if the biFunction is null
     */
    @Beta
    public static <A, T, R> Function<T, R> f(final A a, final java.util.function.BiFunction<A, T, R> biFunction) throws IllegalArgumentException {
        N.checkArgNotNull(biFunction);

        return t -> biFunction.apply(a, t);
    }

    /**
     * <p>Creates a function that applies the given arguments to the provided tri-function.</p>
     *
     * <p>This method implements partial application by binding the first two parameters of the tri-function
     * to fixed values, resulting in a function that only requires the third parameter. This is useful
     * for creating functions that incorporate two reference values in their computation logic.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a function that formats a message with a fixed prefix and suffix
     * String prefix = "Message: ";
     * String suffix = " [end]";
     * Function&lt;String, String&gt; formatMessage = Fn.f(prefix, suffix, (p, s, msg) -> p + msg + s);
     *
     * String result = formatMessage.apply("Hello"); // Returns "Message: Hello [end]"
     * </pre>
     *
     * @param <A> the type of the first fixed argument to the tri-function
     * @param <B> the type of the second fixed argument to the tri-function
     * @param <T> the type of the input to the resulting function
     * @param <R> the type of the result of the resulting function
     * @param a the fixed value to use as the first argument to the tri-function
     * @param b the fixed value to use as the second argument to the tri-function
     * @param triFunction the tri-function to apply with the fixed first and second arguments
     * @return a function that applies the input as the third argument to the tri-function
     * @throws IllegalArgumentException if the triFunction is null
     */
    @Beta
    public static <A, B, T, R> Function<T, R> f(final A a, final B b, final TriFunction<A, B, T, R> triFunction) throws IllegalArgumentException {
        N.checkArgNotNull(triFunction);

        return t -> triFunction.apply(a, b, t);
    }

    /**
     * <p>Returns the provided bi-function as is - a shorthand identity method for bi-functions.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * BiFunction&lt;String, Integer, String&gt; concatFunction = (str, len) -> str + len;
     * // You can use:
     * var concatFunction = Fn.f((String str, Integer len) -> str + len);
     * </pre>
     *
     * @param <T> the type of the first input to the bi-function
     * @param <U> the type of the second input to the bi-function
     * @param <R> the type of the result of the bi-function
     * @param biFunction the bi-function to return
     * @return the bi-function unchanged
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> f(final BiFunction<T, U, R> biFunction) {
        return biFunction;
    }

    /**
     * <p>Creates a bi-function that applies the given argument to the provided bi-function.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the bi-function
     * to a fixed value, resulting in a bi-function that only requires the second parameter. This is useful
     * for creating bi-functions that incorporate a reference value in their computation logic.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a bi-function that appends a fixed prefix to a string
     * String prefix = "Hello, ";
     * BiFunction&lt;String, Integer, String&gt; appendPrefix = Fn.f(prefix, (p, s) -> p + s);
     *
     * String result = appendPrefix.apply("World", 1); // Returns "Hello, World"
     * </pre>
     *
     * @param <A> the type of the fixed first argument to the bi-function
     * @param <T> the type of the first input to the resulting bi-function
     * @param <U> the type of the second input to the resulting bi-function
     * @param <R> the type of the result of the resulting bi-function
     * @param a the fixed value to use as the first argument to the bi-function
     * @param triFunction the tri-function to apply with the fixed first argument
     * @return a bi-function that applies the input as the second argument to the tri-function
     * @throws IllegalArgumentException if the triFunction is null
     */
    @Beta
    public static <A, T, U, R> BiFunction<T, U, R> f(final A a, final TriFunction<A, T, U, R> triFunction) throws IllegalArgumentException {
        N.checkArgNotNull(triFunction);

        return (t, u) -> triFunction.apply(a, t, u);
    }

    /**
     * <p>Returns the provided tri-function as is - a shorthand identity method for tri-functions.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate and BiPredicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Instead of explicitly typing:
     * TriFunction&lt;String, Integer, Boolean, String&gt; triFunction = 
     *     (str, len, flag) -> flag ? str + " is long" : str + " is short";
     * // You can use:
     * var triFunction = Fn.f((String str, Integer len, Boolean flag) -> 
     *     flag ? str + " is long" : str + " is short");
     * </pre>
     *
     * @param <A> the type of the first input to the tri-function
     * @param <B> the type of the second input to the tri-function
     * @param <C> the type of the third input to the tri-function
     * @param <R> the type of the result of the tri-function
     * @param triFunction the tri-function to return
     * @return the tri-function unchanged
     */
    @Beta
    public static <A, B, C, R> TriFunction<A, B, C, R> f(final TriFunction<A, B, C, R> triFunction) {
        return triFunction;
    }

    /**
     * <p>Returns the provided unary operator as is - a shorthand identity method for unary operators.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * @param <T> the type of the operand and result of the unary operator
     * @param unaryOperator the unary operator to return
     * @return the unary operator unchanged
     */
    @Beta
    public static <T> UnaryOperator<T> o(final UnaryOperator<T> unaryOperator) {
        N.checkArgNotNull(unaryOperator);

        return unaryOperator;
    }

    /**
     * <p>Returns the provided binary operator as is - a shorthand identity method for binary operators.</p>
     *
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * @param <T> the type of the operands and result of the binary operator
     * @param binaryOperator the binary operator to return
     * @return the binary operator unchanged
     */
    @Beta
    public static <T> BinaryOperator<T> o(final BinaryOperator<T> binaryOperator) {
        N.checkArgNotNull(binaryOperator);

        return binaryOperator;
    }

    /**
     * <p>Returns a supplier that wraps a throwable supplier, converting checked exceptions to runtime exceptions.</p>
     *
     * <p>This method is useful for converting suppliers that throw checked exceptions into standard suppliers
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of results supplied by the supplier
     * @param supplier the throwable supplier to wrap
     * @return a supplier that applies the supplier and converts exceptions
     * @throws IllegalArgumentException if the supplier is null
     */
    @Beta
    public static <T> Supplier<T> ss(final Throwables.Supplier<? extends T, ? extends Exception> supplier) {
        N.checkArgNotNull(supplier);

        return () -> {
            try {
                return supplier.get();
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a supplier that applies the given argument to the provided throwable function.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the function
     * to a fixed value, resulting in a supplier that only requires the second parameter.
     * Any checked exceptions thrown by the function will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the function
     * @param <T> the type of the result
     * @param a the fixed value to use as the first argument to the function
     * @param func the throwable function to apply with the fixed first argument
     * @return a supplier that computes the result by applying the function to the argument
     * @throws IllegalArgumentException if the function is null
     */
    @Beta
    public static <A, T> Supplier<T> ss(final A a, final Throwables.Function<? super A, ? extends T, ? extends Exception> func) {
        N.checkArgNotNull(func);

        return () -> {
            try {
                return func.apply(a);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Returns a predicate that wraps a throwable predicate, converting checked exceptions to runtime exceptions.</p>
     *
     * <p>This method is useful for converting predicates that throw checked exceptions into standard predicates
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the input to the predicate
     * @param predicate the throwable predicate to wrap
     * @return a predicate that applies the input to the throwable predicate and converts exceptions
     * @throws IllegalArgumentException if the predicate is null
     */
    @Beta
    public static <T> Predicate<T> pp(final Throwables.Predicate<T, ? extends Exception> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return value -> {
            try {
                return predicate.test(value);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a predicate that applies the given argument to the provided throwable bi-predicate.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the bi-predicate
     * to a fixed value, resulting in a predicate that only requires the second parameter. 
     * Any checked exceptions thrown by the bi-predicate will be converted to runtime exceptions.
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     * 
     * @param <A> the type of the fixed first argument to the bi-predicate
     * @param <T> the type of the input to the resulting predicate
     * @param a the fixed value to use as the first argument to the bi-predicate
     * @param biPredicate the throwable bi-predicate to apply with the fixed first argument
     * @return a predicate that applies the input as the second argument to the bi-predicate
     * @throws IllegalArgumentException if the biPredicate is null
     */
    @Beta
    public static <A, T> Predicate<T> pp(final A a, final Throwables.BiPredicate<A, T, ? extends Exception> biPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(biPredicate);

        return t -> {
            try {
                return biPredicate.test(a, t);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a predicate that applies inputs along with two fixed values to the provided throwable tri-predicate.</p>
     *
     * <p>This method implements partial application by binding the first two parameters of the tri-predicate
     * to fixed values, resulting in a predicate that only requires the third parameter.
     * Any checked exceptions thrown by the tri-predicate will be converted to runtime exceptions.
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a predicate that tests if a string matches a pattern with specific options
     * Pattern pattern = Pattern.compile("\\d+");
     * boolean ignoreCase = true;
     * Predicate&lt;String&gt; isNumber = Fn.pp(pattern, ignoreCase, (p, ic, str) -> {
     *     if (ic) {
     *         return p.matcher(str.toLowerCase()).matches();
     *     }
     *     return p.matcher(str).matches();
     * });
     *
     * isNumber.test("123");  // Returns true
     * isNumber.test("abc");  // Returns false
     * </pre>
     *
     * @param <A> the type of the fixed first argument to the tri-predicate
     * @param <B> the type of the fixed second argument to the tri-predicate
     * @param <T> the type of the input to the resulting predicate
     * @param a the fixed value to use as the first argument to the tri-predicate
     * @param b the fixed value to use as the second argument to the tri-predicate
     * @param triPredicate the throwable tri-predicate to apply with the fixed first and second arguments
     * @return a predicate that applies the input as the third argument to the tri-predicate
     * @throws IllegalArgumentException if the triPredicate is null
     * @see #pp(Object, Throwables.BiPredicate)
     * @see #pp(Throwables.Predicate)
     */
    @Beta
    public static <A, B, T> Predicate<T> pp(final A a, final B b, final Throwables.TriPredicate<A, B, T, ? extends Exception> triPredicate)
            throws IllegalArgumentException {
        N.checkArgNotNull(triPredicate);

        return t -> {
            try {
                return triPredicate.test(a, b, t);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a bi-predicate that safely wraps a throwable bi-predicate by converting any checked exceptions into runtime exceptions.</p>
     *
     * <p>This utility method simplifies functional programming by allowing the use of operations that might throw checked exceptions
     * without explicit try-catch blocks. Any checked exception thrown by the bi-predicate will be caught and
     * wrapped in a runtime exception.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a bi-predicate that reads and compares two files
     * BiPredicate&lt;File, File&gt; filesHaveSameContent = Fn.pp((file1, file2) -> {
     *     try (InputStream is1 = new FileInputStream(file1);
     *          InputStream is2 = new FileInputStream(file2)) {
     *         byte[] buffer1 = new byte[8192];
     *         byte[] buffer2 = new byte[8192];
     *         int n1, n2;
     *         
     *         do {
     *             n1 = is1.read(buffer1);
     *             n2 = is2.read(buffer2);
     *             
     *             if (n1 != n2) {
     *                 return false;
     *             }
     *             
     *             if (n1 > 0 && !Arrays.equals(buffer1, 0, n1, buffer2, 0, n2)) {
     *                 return false;
     *             }
     *         } while (n1 > 0);
     *         
     *         return true;
     *     }
     * });
     * 
     * boolean result = filesHaveSameContent.test(new File("file1.txt"), new File("file2.txt"));
     * </pre>
     *
     * @param <T> the type of the first input to the bi-predicate
     * @param <U> the type of the second input to the bi-predicate
     * @param biPredicate the throwable bi-predicate to be wrapped
     * @return a bi-predicate that delegates to the given throwable bi-predicate, converting any checked exceptions to runtime exceptions
     * @throws IllegalArgumentException if the biPredicate is null
     * @see #pp(Throwables.Predicate)
     * @see #pp(Object, Throwables.BiPredicate)
     * @see #pp(Object, Object, Throwables.TriPredicate)
     */
    @Beta
    public static <T, U> BiPredicate<T, U> pp(final Throwables.BiPredicate<T, U, ? extends Exception> biPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(biPredicate);

        return (t, u) -> {
            try {
                return biPredicate.test(t, u);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a bi-predicate that applies the given argument to the provided throwable tri-predicate.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the tri-predicate
     * to a fixed value, resulting in a bi-predicate that only requires the second and third parameters.
     * Any checked exceptions thrown by the tri-predicate will be converted to runtime exceptions.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a bi-predicate that checks if a substring appears between two indices
     * String text = "error message";
     * BiPredicate&lt;Integer, Integer&gt; containsErrorBetween = 
     *     Fn.pp(text, (str, start, end) -> str.substring(start, end).contains("error"));
     *
     * boolean result = containsErrorBetween.test(0, 5);  // Returns true
     * boolean result2 = containsErrorBetween.test(6, 14); // Returns false
     * </pre>
     *
     * @param <A> the type of the fixed first argument to the tri-predicate
     * @param <T> the type of the first input to the resulting bi-predicate
     * @param <U> the type of the second input to the resulting bi-predicate
     * @param a the fixed value to use as the first argument to the tri-predicate
     * @param triPredicate the throwable tri-predicate to apply with the fixed first argument
     * @return a bi-predicate that applies the inputs as the second and third arguments to the tri-predicate
     * @throws IllegalArgumentException if the triPredicate is null
     */
    @Beta
    public static <A, T, U> BiPredicate<T, U> pp(final A a, final Throwables.TriPredicate<A, T, U, ? extends Exception> triPredicate)
            throws IllegalArgumentException {
        N.checkArgNotNull(triPredicate);

        return (t, u) -> {
            try {
                return triPredicate.test(a, t, u);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a tri-predicate that safely wraps a throwable tri-predicate by converting any checked exceptions into runtime exceptions.</p>
     *
     * <p>This utility method simplifies functional programming by allowing the use of operations that might throw checked exceptions
     * without explicit try-catch blocks. Any checked exception thrown by the tri-predicate will be caught and
     * wrapped in a runtime exception.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a tri-predicate that checks if a string matches a pattern with specific options
     * Pattern pattern = Pattern.compile("\\d+");
     * boolean ignoreCase = true;
     * TriPredicate&lt;String, Integer, Boolean&gt; isNumber = Fn.pp(pattern, ignoreCase, (p, ic, str) -> {
     *     if (ic) {
     *         return p.matcher(str.toLowerCase()).matches();
     *     }
     *     return p.matcher(str).matches();
     * });
     *
     * boolean result = isNumber.test("123", 3, true);  // Returns true
     * boolean result2 = isNumber.test("abc", 3, false); // Returns false
     * </pre>
     *
     * @param <A> the type of the first input to the tri-predicate
     * @param <B> the type of the second input to the tri-predicate
     * @param <C> the type of the third input to the tri-predicate
     * @param triPredicate the throwable tri-predicate to be wrapped
     * @return a tri-predicate that delegates to the given throwable tri-predicate, converting any checked exceptions to runtime exceptions
     * @throws IllegalArgumentException if the triPredicate is null
     */
    @Beta
    public static <A, B, C> TriPredicate<A, B, C> pp(final Throwables.TriPredicate<A, B, C, ? extends Exception> triPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(triPredicate);

        return (a, b, c) -> {
            try {
                return triPredicate.test(a, b, c);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Returns a consumer that wraps a throwable consumer, converting checked exceptions to runtime exceptions.</p>
     *
     * <p>This method is useful for converting consumers that throw checked exceptions into standard consumers
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the input to the consumer
     * @param consumer the throwable consumer to wrap
     * @return a consumer that applies the input to the throwable consumer and converts exceptions
     * @throws IllegalArgumentException if the consumer is null
     */
    @Beta
    public static <T> Consumer<T> cc(final Throwables.Consumer<T, ? extends Exception> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer);

        return t -> {
            try {
                consumer.accept(t);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a consumer that applies the given argument to the provided throwable bi-consumer.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the bi-consumer
     * to a fixed value, resulting in a consumer that only requires the second parameter.
     * Any checked exceptions thrown by the bi-consumer will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the bi-consumer
     * @param <T> the type of the input to the resulting consumer
     * @param a the fixed value to use as the first argument to the bi-consumer
     * @param biConsumer the throwable bi-consumer to apply with the fixed first argument
     * @return a consumer that applies the input as the second argument to the bi-consumer
     * @throws IllegalArgumentException if the biConsumer is null
     */
    @Beta
    public static <A, T> Consumer<T> cc(final A a, final Throwables.BiConsumer<A, T, ? extends Exception> biConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(biConsumer);

        return t -> {
            try {
                biConsumer.accept(a, t);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a consumer that applies inputs along with two fixed values to the provided throwable tri-consumer.</p>
     *
     * <p>This method implements partial application by binding the first two parameters of the tri-consumer
     * to fixed values, resulting in a consumer that only requires the third parameter.
     * Any checked exceptions thrown by the tri-consumer will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the tri-consumer
     * @param <B> the type of the fixed second argument to the tri-consumer
     * @param <T> the type of the input to the resulting consumer
     * @param a the fixed value to use as the first argument to the tri-consumer
     * @param b the fixed value to use as the second argument to the tri-consumer
     * @param triConsumer the throwable tri-consumer to apply with the fixed first and second arguments
     * @return a consumer that applies the input as the third argument to the tri-consumer
     * @throws IllegalArgumentException if the triConsumer is null
     */
    @Beta
    public static <A, B, T> Consumer<T> cc(final A a, final B b, final Throwables.TriConsumer<A, B, T, ? extends Exception> triConsumer)
            throws IllegalArgumentException {
        N.checkArgNotNull(triConsumer);

        return t -> {
            try {
                triConsumer.accept(a, b, t);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Returns a bi-consumer that wraps a throwable bi-consumer, converting checked exceptions to runtime exceptions.</p>
     *
     * <p>This method is useful for converting bi-consumers that throw checked exceptions into standard bi-consumers
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the first input to the bi-consumer
     * @param <U> the type of the second input to the bi-consumer
     * @param biConsumer the throwable bi-consumer to wrap
     * @return a bi-consumer that applies the inputs to the throwable bi-consumer and converts exceptions
     * @throws IllegalArgumentException if the biConsumer is null
     */
    @Beta
    public static <T, U> BiConsumer<T, U> cc(final Throwables.BiConsumer<T, U, ? extends Exception> biConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(biConsumer);

        return (t, u) -> {
            try {
                biConsumer.accept(t, u);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a bi-consumer that applies the given argument to the provided throwable tri-consumer.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the tri-consumer
     * to a fixed value, resulting in a bi-consumer that only requires the second and third parameters.
     * Any checked exceptions thrown by the tri-consumer will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the tri-consumer
     * @param <T> the type of the first input to the resulting bi-consumer
     * @param <U> the type of the second input to the resulting bi-consumer
     * @param a the fixed value to use as the first argument to the tri-consumer
     * @param triConsumer the throwable tri-consumer to apply with the fixed first argument
     * @return a bi-consumer that applies the inputs as the second and third arguments to the tri-consumer
     * @throws IllegalArgumentException if the triConsumer is null
     */
    @Beta
    public static <A, T, U> BiConsumer<T, U> cc(final A a, final Throwables.TriConsumer<A, T, U, ? extends Exception> triConsumer)
            throws IllegalArgumentException {
        N.checkArgNotNull(triConsumer);

        return (t, u) -> {
            try {
                triConsumer.accept(a, t, u);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Returns a tri-consumer that wraps a throwable tri-consumer, converting checked exceptions to runtime exceptions.</p>
     *
     * <p>This method is useful for converting tri-consumers that throw checked exceptions into standard tri-consumers
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <A> the type of the first input to the tri-consumer
     * @param <B> the type of the second input to the tri-consumer
     * @param <C> the type of the third input to the tri-consumer
     * @param triConsumer the throwable tri-consumer to wrap
     * @return a tri-consumer that applies the inputs to the throwable tri-consumer and converts exceptions
     * @throws IllegalArgumentException if the triConsumer is null
     */
    @Beta
    public static <A, B, C> TriConsumer<A, B, C> cc(final Throwables.TriConsumer<A, B, C, ? extends Exception> triConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(triConsumer);

        return (a, b, c) -> {
            try {
                triConsumer.accept(a, b, c);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Returns a function that wraps a throwable function, converting checked exceptions to runtime exceptions.</p>
     *
     * <p>This method is useful for converting functions that throw checked exceptions into standard functions
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param function the throwable function to wrap
     * @return a function that applies the input to the throwable function and converts exceptions
     * @throws IllegalArgumentException if the function is null
     */
    @Beta
    public static <T, R> Function<T, R> ff(final Throwables.Function<T, ? extends R, ? extends Exception> function) throws IllegalArgumentException {
        N.checkArgNotNull(function);

        return t -> {
            try {
                return function.apply(t);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a function that safely wraps a throwable function by returning a default value if the function throws an exception.</p>
     *
     * <p>This utility method simplifies functional programming by allowing the use of operations that might throw checked exceptions
     * without explicit try-catch blocks. Any checked exception thrown by the function will be caught and the provided
     * default value will be returned instead.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a function that reads content from a file but returns an empty string on error
     * Function&lt;File, String&gt; readFileContent = Fn.ff(file -> {
     *     try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
     *         StringBuilder content = new StringBuilder();
     *         String line;
     *         while ((line = reader.readLine()) != null) {
     *             content.append(line).append("\n");
     *         }
     *         return content.toString();
     *     }
     * }, "");
     *
     * String content = readFileContent.apply(new File("config.txt")); 
     * // Returns file content, or empty string if file doesn't exist or can't be read
     * </pre>
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param function the throwable function to be wrapped
     * @param defaultOnError the default value to return if the function throws an exception
     * @return a function that delegates to the given throwable function, returning the default value if an exception occurs
     * @throws IllegalArgumentException if the function is null
     * @see #ff(Throwables.Function)
     * @see #pp(Throwables.Predicate)
     * @see #cc(Throwables.Consumer)
     */
    @Beta
    public static <T, R> Function<T, R> ff(final Throwables.Function<T, ? extends R, ? extends Exception> function, final R defaultOnError)
            throws IllegalArgumentException {
        N.checkArgNotNull(function);

        return t -> {
            try {
                return function.apply(t);
            } catch (final Exception e) {
                return defaultOnError;
            }
        };
    }

    /**
     * <p>Creates a function that applies the given argument to the provided throwable bi-function.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the bi-function
     * to a fixed value, resulting in a function that only requires the second parameter.
     * Any checked exceptions thrown by the bi-function will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the bi-function
     * @param <T> the type of the input to the resulting function
     * @param <R> the type of the result of the resulting function
     * @param a the fixed value to use as the first argument to the bi-function
     * @param biFunction the throwable bi-function to apply with the fixed first argument
     * @return a function that applies the input as the second argument to the bi-function
     * @throws IllegalArgumentException if the biFunction is null
     */
    @Beta
    public static <A, T, R> Function<T, R> ff(final A a, final Throwables.BiFunction<A, T, R, ? extends Exception> biFunction) throws IllegalArgumentException {
        N.checkArgNotNull(biFunction);

        return t -> {
            try {
                return biFunction.apply(a, t);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a function that applies inputs along with two fixed values to the provided throwable tri-function.</p>
     *
     * <p>This method implements partial application by binding the first two parameters of the tri-function
     * to fixed values, resulting in a function that only requires the third parameter.
     * Any checked exceptions thrown by the tri-function will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the tri-function
     * @param <B> the type of the fixed second argument to the tri-function
     * @param <T> the type of the input to the resulting function
     * @param <R> the type of the result of the resulting function
     * @param a the fixed value to use as the first argument to the tri-function
     * @param b the fixed value to use as the second argument to the tri-function
     * @param triFunction the throwable tri-function to apply with the fixed first and second arguments
     * @return a function that applies the input as the third argument to the tri-function
     * @throws IllegalArgumentException if the triFunction is null
     */
    @Beta
    public static <A, B, T, R> Function<T, R> ff(final A a, final B b, final Throwables.TriFunction<A, B, T, R, ? extends Exception> triFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(triFunction);

        return t -> {
            try {
                return triFunction.apply(a, b, t);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Returns a bi-function that wraps a throwable bi-function, converting checked exceptions to runtime exceptions.</p>
     *
     * <p>This method is useful for converting bi-functions that throw checked exceptions into standard bi-functions
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the first input to the bi-function
     * @param <U> the type of the second input to the bi-function
     * @param <R> the type of the result of the bi-function
     * @param biFunction the throwable bi-function to wrap
     * @return a bi-function that applies the inputs to the throwable bi-function and converts exceptions
     * @throws IllegalArgumentException if the biFunction is null
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> ff(final Throwables.BiFunction<T, U, R, ? extends Exception> biFunction) throws IllegalArgumentException {
        N.checkArgNotNull(biFunction);

        return (t, u) -> {
            try {
                return biFunction.apply(t, u);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a bi-function that safely wraps a throwable bi-function by returning a default value if the function throws an exception.</p>
     *
     * <p>This utility method simplifies functional programming by allowing the use of operations that might throw checked exceptions
     * without explicit try-catch blocks. Any checked exception thrown by the bi-function will be caught and the provided
     * default value will be returned instead.</p>
     *
     * @param <T> the type of the first input to the bi-function
     * @param <U> the type of the second input to the bi-function
     * @param <R> the type of the result of the bi-function
     * @param biFunction the throwable bi-function to be wrapped
     * @param defaultOnError the default value to return if the function throws an exception
     * @return a bi-function that delegates to the given throwable bi-function, returning the default value if an exception occurs
     * @throws IllegalArgumentException if the biFunction is null
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> ff(final Throwables.BiFunction<T, U, R, ? extends Exception> biFunction, final R defaultOnError)
            throws IllegalArgumentException {
        N.checkArgNotNull(biFunction);

        return (t, u) -> {
            try {
                return biFunction.apply(t, u);
            } catch (final Exception e) {
                return defaultOnError;
            }
        };
    }

    /**
     * <p>Creates a bi-function that applies the given argument to the provided throwable tri-function.</p>
     *
     * <p>This method implements partial application by binding the first parameter of the tri-function
     * to a fixed value, resulting in a bi-function that only requires the second and third parameters.
     * Any checked exceptions thrown by the tri-function will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the tri-function
     * @param <T> the type of the first input to the resulting bi-function
     * @param <U> the type of the second input to the resulting bi-function
     * @param <R> the type of the result of the resulting bi-function
     * @param a the fixed value to use as the first argument to the tri-function
     * @param triFunction the throwable tri-function to apply with the fixed first argument
     * @return a bi-function that applies the inputs as the second and third arguments to the tri-function
     * @throws IllegalArgumentException if the triFunction is null
     */
    @Beta
    public static <A, T, U, R> BiFunction<T, U, R> ff(final A a, final Throwables.TriFunction<A, T, U, R, ? extends Exception> triFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(triFunction);

        return (t, u) -> {
            try {
                return triFunction.apply(a, t, u);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Returns a tri-function that wraps a throwable tri-function, converting checked exceptions to runtime exceptions.</p>
     *
     * <p>This method is useful for converting tri-functions that throw checked exceptions into standard tri-functions
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <A> the type of the first input to the tri-function
     * @param <B> the type of the second input to the tri-function
     * @param <C> the type of the third input to the tri-function
     * @param <R> the type of the result of the tri-function
     * @param triFunction the throwable tri-function to wrap
     * @return a tri-function that applies the inputs to the throwable tri-function and converts exceptions
     * @throws IllegalArgumentException if the triFunction is null
     */
    @Beta
    public static <A, B, C, R> TriFunction<A, B, C, R> ff(final Throwables.TriFunction<A, B, C, R, ? extends Exception> triFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(triFunction);

        return (a, b, c) -> {
            try {
                return triFunction.apply(a, b, c);
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     * <p>Creates a tri-function that safely wraps a throwable tri-function by returning a default value if the function throws an exception.</p>
     *
     * <p>This utility method simplifies functional programming by allowing the use of operations that might throw checked exceptions
     * without explicit try-catch blocks. Any checked exception thrown by the tri-function will be caught and the provided
     * default value will be returned instead.</p>
     *
     * @param <A> the type of the first input to the tri-function
     * @param <B> the type of the second input to the tri-function
     * @param <C> the type of the third input to the tri-function
     * @param <R> the type of the result of the tri-function
     * @param triFunction the throwable tri-function to be wrapped
     * @param defaultOnError the default value to return if the function throws an exception
     * @return a tri-function that delegates to the given throwable tri-function, returning the default value if an exception occurs
     * @throws IllegalArgumentException if the triFunction is null
     */
    @Beta
    public static <A, B, C, R> TriFunction<A, B, C, R> ff(final Throwables.TriFunction<A, B, C, R, ? extends Exception> triFunction, final R defaultOnError)
            throws IllegalArgumentException {
        N.checkArgNotNull(triFunction);

        return (a, b, c) -> {
            try {
                return triFunction.apply(a, b, c);
            } catch (final Exception e) {
                return defaultOnError;
            }
        };
    }

    /**
     * <p>Creates a synchronized predicate that safely wraps a standard predicate by ensuring all tests are performed within a synchronized block.</p>
     *
     * <p>This utility method provides thread safety for predicates that might be accessed concurrently. Any test operation
     * will be performed while holding the lock on the provided mutex object, ensuring thread-safe evaluation of the predicate.</p>
     *
     * <p>Example usage:</p>
     * <pre>
     * // Create a thread-safe counter-based predicate
     * final AtomicInteger counter = new AtomicInteger(0);
     * final Object lock = new Object();
     * 
     * Predicate&lt;String&gt; limitedAcceptance = Fn.sp(lock, str -> {
     *     if (counter.get() &lt; 5) {
     *         counter.incrementAndGet();
     *         return true;
     *     }
     *     return false;
     * });
     * 
     * // Can be safely used from multiple threads
     * boolean result1 = limitedAcceptance.test("first");  // true
     * boolean result6 = limitedAcceptance.test("sixth");  // false (limit reached)
     * </pre>
     *
     * @param <T> the type of the input to the predicate
     * @param mutex the object to synchronize on when testing values
     * @param predicate the predicate to be wrapped with synchronization
     * @return a predicate that delegates to the given predicate within a synchronized block on the mutex
     * @throws IllegalArgumentException if the mutex or predicate is null
     * @see #pp(Throwables.Predicate)
     * @see #p(Predicate)
     */
    @Beta
    public static <T> Predicate<T> sp(final Object mutex, final java.util.function.Predicate<T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(predicate, cs.Predicate);

        return t -> {
            synchronized (mutex) {
                return predicate.test(t);
            }
        };
    }

    /**
     * Synchronized {@code Predicate}.
     *
     * @param <A>
     * @param <T>
     * @param mutex to synchronize on
     * @param a
     * @param biPredicate
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <A, T> Predicate<T> sp(final Object mutex, final A a, final java.util.function.BiPredicate<A, T> biPredicate)
            throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(biPredicate, cs.BiPredicate);

        return t -> {
            synchronized (mutex) {
                return biPredicate.test(a, t);
            }
        };
    }

    @Beta
    public static <T, U> BiPredicate<T, U> sp(final Object mutex, final java.util.function.BiPredicate<T, U> biPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(biPredicate, cs.BiPredicate);

        return (t, u) -> {
            synchronized (mutex) {
                return biPredicate.test(t, u);
            }
        };
    }

    @Beta
    public static <A, B, C> TriPredicate<A, B, C> sp(final Object mutex, final TriPredicate<A, B, C> triPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(triPredicate, cs.TriPredicate);

        return (a, b, c) -> {
            synchronized (mutex) {
                return triPredicate.test(a, b, c);
            }
        };
    }

    /**
     * Synchronized {@code Consumer}.
     *
     * @param <T>
     * @param mutex to synchronize on
     * @param consumer
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T> Consumer<T> sc(final Object mutex, final java.util.function.Consumer<T> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(consumer, cs.Consumer);

        return t -> {
            synchronized (mutex) {
                consumer.accept(t);
            }
        };
    }

    /**
     * Synchronized {@code Consumer}.
     *
     * @param <A>
     * @param <T>
     * @param mutex to synchronize on
     * @param a
     * @param biConsumer
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <A, T> Consumer<T> sc(final Object mutex, final A a, final java.util.function.BiConsumer<A, T> biConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(biConsumer, cs.BiConsumer);

        return t -> {
            synchronized (mutex) {
                biConsumer.accept(a, t);
            }
        };
    }

    /**
     * Synchronized {@code BiConsumer}.
     *
     * @param <T>
     * @param <U>
     * @param mutex to synchronize on
     * @param biConsumer
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T, U> BiConsumer<T, U> sc(final Object mutex, final java.util.function.BiConsumer<T, U> biConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(biConsumer, cs.BiConsumer);

        return (t, u) -> {
            synchronized (mutex) {
                biConsumer.accept(t, u);
            }
        };
    }

    @Beta
    public static <A, B, C> TriConsumer<A, B, C> sp(final Object mutex, final TriConsumer<A, B, C> triPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(triPredicate, cs.TriConsumer);

        return (a, b, c) -> {
            synchronized (mutex) {
                triPredicate.accept(a, b, c);
            }
        };
    }

    /**
     * Synchronized {@code Function}.
     *
     * @param <T>
     * @param <R>
     * @param mutex to synchronize on
     * @param function
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T, R> Function<T, R> sf(final Object mutex, final java.util.function.Function<T, ? extends R> function) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(function, cs.function);

        return t -> {
            synchronized (mutex) {
                return function.apply(t);
            }
        };
    }

    /**
     * Synchronized {@code Function}.
     *
     * @param <A>
     * @param <T>
     * @param <R>
     * @param mutex to synchronize on
     * @param a
     * @param biFunction
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <A, T, R> Function<T, R> sf(final Object mutex, final A a, final java.util.function.BiFunction<A, T, R> biFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(biFunction, cs.BiFunction);

        return t -> {
            synchronized (mutex) {
                return biFunction.apply(a, t);
            }
        };
    }

    /**
     * Synchronized {@code BiFunction}.
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param mutex to synchronize on
     * @param biFunction
     * @return
     * @throws IllegalArgumentException
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> sf(final Object mutex, final java.util.function.BiFunction<T, U, R> biFunction)
            throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(biFunction, cs.BiFunction);

        return (t, u) -> {
            synchronized (mutex) {
                return biFunction.apply(t, u);
            }
        };
    }

    @Beta
    public static <A, B, C, R> TriFunction<A, B, C, R> sf(final Object mutex, final TriFunction<A, B, C, R> triFunction) {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(triFunction, cs.TriFunction);

        return (a, b, c) -> {
            synchronized (mutex) {
                return triFunction.apply(a, b, c);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param action
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Function<T, Void> c2f(final java.util.function.Consumer<? super T> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return t -> {
            action.accept(t);
            return null;
        };
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param action
     * @param valueToReturn
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, R> Function<T, R> c2f(final java.util.function.Consumer<? super T> action, final R valueToReturn) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return t -> {
            action.accept(t);
            return valueToReturn;
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param action
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> BiFunction<T, U, Void> c2f(final java.util.function.BiConsumer<? super T, ? super U> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return (t, u) -> {
            action.accept(t, u);
            return null;
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param action
     * @param valueToReturn
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U, R> BiFunction<T, U, R> c2f(final java.util.function.BiConsumer<? super T, ? super U> action, final R valueToReturn)
            throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return (t, u) -> {
            action.accept(t, u);
            return valueToReturn;
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param action
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C> TriFunction<A, B, C, Void> c2f(final TriConsumer<? super A, ? super B, ? super C> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return (a, b, c) -> {
            action.accept(a, b, c);
            return null;
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param action
     * @param valueToReturn
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C, R> TriFunction<A, B, C, R> c2f(final TriConsumer<? super A, ? super B, ? super C> action, final R valueToReturn)
            throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return (a, b, c) -> {
            action.accept(a, b, c);
            return valueToReturn;
        };
    }

    /**
     * Returns a {@code Consumer} which calls the specified {@code func}.
     *
     * @param <T>
     * @param func
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Consumer<T> f2c(final java.util.function.Function<? super T, ?> func) throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return func::apply;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param func
     * @return
     * @throws IllegalArgumentException
     */
    public static <T, U> BiConsumer<T, U> f2c(final java.util.function.BiFunction<? super T, ? super U, ?> func) throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return func::apply;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param func
     * @return
     * @throws IllegalArgumentException
     */
    public static <A, B, C> TriConsumer<A, B, C> f2c(final TriFunction<? super A, ? super B, ? super C, ?> func) throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return func::apply;
    }

    /**
     *
     * @param runnable
     * @return
     */
    public static Runnable rr(final Throwables.Runnable<? extends Exception> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     *
     * @param <R>
     * @param callable
     * @return
     */
    public static <R> Callable<R> cc(final Throwables.Callable<R, ? extends Exception> callable) {
        return () -> {
            try {
                return callable.call();
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     *
     * @param runnable
     * @return
     * @throws IllegalArgumentException
     */
    public static Runnable r(final Runnable runnable) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        return runnable;
    }

    /**
     *
     * @param <R>
     * @param callable
     * @return
     * @throws IllegalArgumentException
     */
    public static <R> Callable<R> c(final Callable<R> callable) throws IllegalArgumentException {
        N.checkArgNotNull(callable);

        return callable;
    }

    /**
     *
     * @param runnable
     * @return
     * @throws IllegalArgumentException
     */
    public static java.lang.Runnable jr(final java.lang.Runnable runnable) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        return runnable;
    }

    /**
     *
     * @param <R>
     * @param callable
     * @return
     * @throws IllegalArgumentException
     */
    public static <R> java.util.concurrent.Callable<R> jc(final java.util.concurrent.Callable<R> callable) throws IllegalArgumentException {
        N.checkArgNotNull(callable);

        return callable;
    }

    /**
     *
     * @param runnable
     * @return
     * @throws IllegalArgumentException
     */
    public static Callable<Void> r2c(final java.lang.Runnable runnable) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        return () -> {
            runnable.run();
            return null;
        };
    }

    /**
     *
     * @param <R>
     * @param runnable
     * @param valueToReturn
     * @return
     * @throws IllegalArgumentException
     */
    public static <R> Callable<R> r2c(final java.lang.Runnable runnable, final R valueToReturn) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        return () -> {
            runnable.run();
            return valueToReturn;
        };
    }

    /**
     *
     * @param <R>
     * @param callable
     * @return
     * @throws IllegalArgumentException
     */
    public static <R> Runnable c2r(final Callable<R> callable) throws IllegalArgumentException {
        N.checkArgNotNull(callable);

        return callable::call;
    }

    /**
     *
     * @param runnable
     * @return
     * @throws IllegalArgumentException
     */
    public static Runnable jr2r(final java.lang.Runnable runnable) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        if (runnable instanceof Runnable) {
            return (Runnable) runnable;
        }

        return runnable::run;
    }

    /**
     *
     * @param <R>
     * @param callable
     * @return
     * @throws IllegalArgumentException
     */
    public static <R> Callable<R> jc2c(final java.util.concurrent.Callable<R> callable) throws IllegalArgumentException {
        N.checkArgNotNull(callable);

        if (callable instanceof Callable) {
            return (Callable<R>) callable;
        }

        return () -> {
            try {
                return callable.call();
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     *
     * @param callable
     * @return
     * @throws IllegalArgumentException
     */
    public static Runnable jc2r(final java.util.concurrent.Callable<?> callable) throws IllegalArgumentException {
        N.checkArgNotNull(callable);

        return () -> {
            try {
                callable.call();
            } catch (final Exception e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        };
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> BinaryOperator<T> throwingMerger() {
        return BinaryOperators.THROWING_MERGER;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> BinaryOperator<T> ignoringMerger() {
        return BinaryOperators.IGNORING_MERGER;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> BinaryOperator<T> replacingMerger() {
        return BinaryOperators.REPLACING_MERGER;
    }

    @SuppressWarnings("rawtypes")
    static final Function<Optional, Object> GET_AS_IT = it -> it.orElse(null);

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<Optional<T>, T> getIfPresentOrElseNull() {
        return (Function) GET_AS_IT;
    }

    @SuppressWarnings("rawtypes")
    static final Function<java.util.Optional, Object> GET_AS_IT_JDK = it -> it.orElse(null);

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<java.util.Optional<T>, T> getIfPresentOrElseNullJdk() {
        return (Function) GET_AS_IT_JDK;
    }

    @SuppressWarnings("rawtypes")
    static final Predicate<Optional> IS_PRESENT_IT = Optional::isPresent;

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<Optional<T>> isPresent() {
        return (Predicate) IS_PRESENT_IT;
    }

    @SuppressWarnings("rawtypes")
    static final Predicate<java.util.Optional> IS_PRESENT_IT_JDK = java.util.Optional::isPresent;

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<java.util.Optional<T>> isPresentJdk() {
        return (Predicate) IS_PRESENT_IT_JDK;
    }

    /**
     * Returns a stateful {@code BiFunction}. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @return a stateful {@code BiFunction}. Don't save or cache for reuse or use it in parallel stream.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> BiFunction<T, T, MergeResult> alternate() {
        return new BiFunction<>() {
            private final MutableBoolean flag = MutableBoolean.of(true);

            @Override
            public MergeResult apply(final T t, final T u) {
                return flag.getAndNegate() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
            }
        };
    }

    public static final class LongSuppliers {
        private LongSuppliers() {
            // utility class
        }

        private static final LongSupplier CURRENT_TIME = System::currentTimeMillis;

        public static LongSupplier ofCurrentTimeMillis() {
            return CURRENT_TIME;
        }
    }

    public static final class Suppliers {

        /** The Constant UUID. */
        private static final Supplier<String> UUID = Strings::uuid;

        /** The Constant GUID. */
        private static final Supplier<String> GUID = Strings::guid;

        /** The Constant EMPTY_BOOLEAN_ARRAY. */
        private static final Supplier<boolean[]> EMPTY_BOOLEAN_ARRAY = () -> N.EMPTY_BOOLEAN_ARRAY;

        /** The Constant EMPTY_CHAR_ARRAY. */
        private static final Supplier<char[]> EMPTY_CHAR_ARRAY = () -> N.EMPTY_CHAR_ARRAY;

        /** The Constant EMPTY_BYTE_ARRAY. */
        private static final Supplier<byte[]> EMPTY_BYTE_ARRAY = () -> N.EMPTY_BYTE_ARRAY;

        /** The Constant EMPTY_SHORT_ARRAY. */
        private static final Supplier<short[]> EMPTY_SHORT_ARRAY = () -> N.EMPTY_SHORT_ARRAY;

        /** The Constant EMPTY_INT_ARRAY. */
        private static final Supplier<int[]> EMPTY_INT_ARRAY = () -> N.EMPTY_INT_ARRAY;

        /** The Constant EMPTY_LONG_ARRAY. */
        private static final Supplier<long[]> EMPTY_LONG_ARRAY = () -> N.EMPTY_LONG_ARRAY;

        /** The Constant EMPTY_FLOAT_ARRAY. */
        private static final Supplier<float[]> EMPTY_FLOAT_ARRAY = () -> N.EMPTY_FLOAT_ARRAY;

        /** The Constant EMPTY_DOUBLE_ARRAY. */
        private static final Supplier<double[]> EMPTY_DOUBLE_ARRAY = () -> N.EMPTY_DOUBLE_ARRAY;

        /** The Constant EMPTY_STRING_ARRAY. */
        private static final Supplier<String[]> EMPTY_STRING_ARRAY = () -> N.EMPTY_STRING_ARRAY;

        /** The Constant EMPTY_OBJECT_ARRAY. */
        private static final Supplier<Object[]> EMPTY_OBJECT_ARRAY = () -> N.EMPTY_OBJECT_ARRAY;

        /** The Constant EMPTY_STRING. */
        private static final Supplier<String> EMPTY_STRING = () -> Strings.EMPTY;

        /** The Constant BOOLEAN_LIST. */
        private static final Supplier<BooleanList> BOOLEAN_LIST = BooleanList::new;

        /** The Constant CHAR_LIST. */
        private static final Supplier<CharList> CHAR_LIST = CharList::new;

        /** The Constant BYTE_LIST. */
        private static final Supplier<ByteList> BYTE_LIST = ByteList::new;

        /** The Constant SHORT_LIST. */
        private static final Supplier<ShortList> SHORT_LIST = ShortList::new;

        /** The Constant INT_LIST. */
        private static final Supplier<IntList> INT_LIST = IntList::new;

        /** The Constant LONG_LIST. */
        private static final Supplier<LongList> LONG_LIST = LongList::new;

        /** The Constant FLOAT_LIST. */
        private static final Supplier<FloatList> FLOAT_LIST = FloatList::new;

        /** The Constant DOUBLE_LIST. */
        private static final Supplier<DoubleList> DOUBLE_LIST = DoubleList::new;

        /** The Constant LIST. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super List> LIST = ArrayList::new;

        /** The Constant LINKED_LIST. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedList> LINKED_LIST = LinkedList::new;

        /** The Constant SET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Set> SET = N::newHashSet;

        /** The Constant LINKED_HASH_SET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Set> LINKED_HASH_SET = N::newLinkedHashSet;

        /** The Constant TREE_SET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super TreeSet> TREE_SET = TreeSet::new;

        /** The Constant QUEUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Queue> QUEUE = LinkedList::new;

        /** The Constant DEQUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Deque> DEQUE = LinkedList::new;

        /** The Constant ARRAY_DEQUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ArrayDeque> ARRAY_DEQUE = ArrayDeque::new;

        /** The Constant LINKED_BLOCKING_QUEUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedBlockingQueue> LINKED_BLOCKING_QUEUE = LinkedBlockingQueue::new;

        /** The Constant LINKED_BLOCKING_DEQUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedBlockingDeque> LINKED_BLOCKING_DEQUE = LinkedBlockingDeque::new;

        /** The Constant CONCURRENT_LINKED_QUEUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ConcurrentLinkedQueue> CONCURRENT_LINKED_QUEUE = ConcurrentLinkedQueue::new;

        /** The Constant PRIORITY_QUEUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super PriorityQueue> PRIORITY_QUEUE = PriorityQueue::new;

        /** The Constant MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Map> MAP = N::newHashMap;

        /** The Constant LINKED_HASH_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Map> LINKED_HASH_MAP = N::newLinkedHashMap;

        /** The Constant IDENTITY_HASH_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super IdentityHashMap> IDENTITY_HASH_MAP = IdentityHashMap::new;

        /** The Constant TREE_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super TreeMap> TREE_MAP = TreeMap::new;

        /** The Constant CONCURRENT_HASH_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ConcurrentHashMap> CONCURRENT_HASH_MAP = ConcurrentHashMap::new;

        /** The Constant CONCURRENT_HASH_SET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Set> CONCURRENT_HASH_SET = ConcurrentHashMap::newKeySet;

        /** The Constant BI_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super BiMap> BI_MAP = BiMap::new;

        /** The Constant MULTISET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Multiset> MULTISET = Multiset::new;

        /** The Constant LIST_MULTIMAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ListMultimap> LIST_MULTIMAP = N::newListMultimap;

        /** The Constant SET_MULTIMAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super SetMultimap> SET_MULTIMAP = N::newSetMultimap;

        /** The Constant STRING_BUILDER. */
        private static final Supplier<StringBuilder> STRING_BUILDER = StringBuilder::new;

        private Suppliers() {
            // utility class
        }

        /**
         * <p>Returns the provided supplier as is - a shorthand identity method for suppliers.</p>
         *
         * <p>This method serves as a shorthand convenience method that can help with type inference
         * in certain contexts. It's part of a family of shorthand methods like {@code p()} for Predicate
         * and others.</p>
         *
         * <p>Example usage:</p>
         * <pre>
         * // Instead of explicitly typing:
         * Supplier&lt;String&gt; supplier = () -&gt; "value";
         * // You can use:
         * var supplier = Suppliers.of(() -&gt; "value");
         * </pre>
         *
         * @param <T> the type of results supplied by the supplier
         * @param supplier the supplier to return
         * @return the supplier unchanged
         * @see #of(Object, Function)
         * @see Fn#s(Supplier)
         * @see Fn#s(Object, Function)
         * @see Fn#ss(com.landawn.abacus.util.Throwables.Supplier)
         * @see Fn#ss(Object, com.landawn.abacus.util.Throwables.Function)
         * @see IntFunctions#of(IntFunction)
         */
        @Beta
        public static <T> Supplier<T> of(final Supplier<T> supplier) {
            return supplier;
        }

        /**
         * <p>Creates a supplier that always returns the result of applying the provided function to the given value.</p>
         *
         * <p>This method creates a supplier that captures the provided value and function,
         * and when the supplier is called, it applies the function to the value and returns the result.</p>
         *
         * <p>Example usage:</p>
         * <pre>
         * // Create a supplier that always returns the length of a specific string
         * String text = "Hello, World";
         * Supplier&lt;Integer&gt; lengthSupplier = Suppliers.of(text, String::length);
         * Integer length = lengthSupplier.get(); // Returns 12
         * </pre>
         *
         * @param <A> the type of the input value
         * @param <T> the type of results supplied by the supplier
         * @param a the value to be processed by the function
         * @param func the function to apply to the value
         * @return a supplier that will return the result of applying the function to the value
         * @see #of(Supplier)
         * @see Fn#s(Supplier)
         * @see Fn#s(Object, Function)
         * @see Fn#ss(com.landawn.abacus.util.Throwables.Supplier)
         * @see Fn#ss(Object, com.landawn.abacus.util.Throwables.Function)
         * @see IntFunctions#of(IntFunction)
         */
        @Beta
        public static <A, T> Supplier<T> of(final A a, final Function<? super A, ? extends T> func) {
            return () -> func.apply(a);
        }

        /**
         * Returns a supplier that always supplies {@code instance}.
         *
         * @param <T>
         * @param instance
         * @return
         */
        public static <T> Supplier<T> ofInstance(final T instance) {
            return () -> instance;
        }

        public static Supplier<String> ofUUID() {
            return UUID;
        }

        public static Supplier<String> ofGUID() {
            return GUID;
        }

        /**
         * Of empty boolean array.
         *
         * @return
         */
        public static Supplier<boolean[]> ofEmptyBooleanArray() {
            return EMPTY_BOOLEAN_ARRAY;
        }

        /**
         * Of empty char array.
         *
         * @return
         */
        public static Supplier<char[]> ofEmptyCharArray() {
            return EMPTY_CHAR_ARRAY;
        }

        /**
         * Of empty byte array.
         *
         * @return
         */
        public static Supplier<byte[]> ofEmptyByteArray() {
            return EMPTY_BYTE_ARRAY;
        }

        /**
         * Of empty short array.
         *
         * @return
         */
        public static Supplier<short[]> ofEmptyShortArray() {
            return EMPTY_SHORT_ARRAY;
        }

        /**
         * Of empty int array.
         *
         * @return
         */
        public static Supplier<int[]> ofEmptyIntArray() {
            return EMPTY_INT_ARRAY;
        }

        /**
         * Of empty long array.
         *
         * @return
         */
        public static Supplier<long[]> ofEmptyLongArray() {
            return EMPTY_LONG_ARRAY;
        }

        /**
         * Of empty float array.
         *
         * @return
         */
        public static Supplier<float[]> ofEmptyFloatArray() {
            return EMPTY_FLOAT_ARRAY;
        }

        /**
         * Of empty double array.
         *
         * @return
         */
        public static Supplier<double[]> ofEmptyDoubleArray() {
            return EMPTY_DOUBLE_ARRAY;
        }

        /**
         * Of empty string array.
         *
         * @return
         */
        public static Supplier<String[]> ofEmptyStringArray() {
            return EMPTY_STRING_ARRAY;
        }

        /**
         * Of empty object array.
         *
         * @return
         */
        public static Supplier<Object[]> ofEmptyObjectArray() {
            return EMPTY_OBJECT_ARRAY;
        }

        /**
         * Of empty String.
         *
         * @return
         */
        public static Supplier<String> ofEmptyString() {
            return EMPTY_STRING;
        }

        /**
         * Of boolean list.
         *
         * @return
         */
        public static Supplier<BooleanList> ofBooleanList() {
            return BOOLEAN_LIST;
        }

        /**
         * Of char list.
         *
         * @return
         */
        public static Supplier<CharList> ofCharList() {
            return CHAR_LIST;
        }

        /**
         * Of byte list.
         *
         * @return
         */
        public static Supplier<ByteList> ofByteList() {
            return BYTE_LIST;
        }

        /**
         * Of short list.
         *
         * @return
         */
        public static Supplier<ShortList> ofShortList() {
            return SHORT_LIST;
        }

        /**
         * Of int list.
         *
         * @return
         */
        public static Supplier<IntList> ofIntList() {
            return INT_LIST;
        }

        /**
         * Of long list.
         *
         * @return
         */
        public static Supplier<LongList> ofLongList() {
            return LONG_LIST;
        }

        /**
         * Of float list.
         *
         * @return
         */
        public static Supplier<FloatList> ofFloatList() {
            return FLOAT_LIST;
        }

        /**
         * Of double list.
         *
         * @return
         */
        public static Supplier<DoubleList> ofDoubleList() {
            return DOUBLE_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<List<T>> ofList() {
            return (Supplier) LIST;
        }

        /**
         * Of linked list.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedList<T>> ofLinkedList() {
            return (Supplier) LINKED_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofSet() {
            return (Supplier) SET;
        }

        /**
         * Of linked hash set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofLinkedHashSet() {
            return (Supplier) LINKED_HASH_SET;
        }

        /**
         * Of sorted set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<SortedSet<T>> ofSortedSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Of navigable set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<NavigableSet<T>> ofNavigableSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Of tree set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<TreeSet<T>> ofTreeSet() {
            return (Supplier) TREE_SET;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Queue<T>> ofQueue() {
            return (Supplier) QUEUE;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Deque<T>> ofDeque() {
            return (Supplier) DEQUE;
        }

        /**
         * Of array deque.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ArrayDeque<T>> ofArrayDeque() {
            return (Supplier) ARRAY_DEQUE;
        }

        /**
         * Of linked blocking queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (Supplier) LINKED_BLOCKING_QUEUE;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedBlockingDeque<T>> ofLinkedBlockingDeque() {
            return (Supplier) LINKED_BLOCKING_DEQUE;
        }

        /**
         * Of concurrent linked queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (Supplier) CONCURRENT_LINKED_QUEUE;
        }

        /**
         * Of priority queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<PriorityQueue<T>> ofPriorityQueue() {
            return (Supplier) PRIORITY_QUEUE;
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<Map<K, V>> ofMap() {
            return (Supplier) MAP;
        }

        /**
         * Of linked hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<Map<K, V>> ofLinkedHashMap() {
            return (Supplier) LINKED_HASH_MAP;
        }

        /**
         * Of identity hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<IdentityHashMap<K, V>> ofIdentityHashMap() {
            return (Supplier) IDENTITY_HASH_MAP;
        }

        /**
         * Of sorted map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<SortedMap<K, V>> ofSortedMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Of navigable map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<NavigableMap<K, V>> ofNavigableMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Of tree map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<TreeMap<K, V>> ofTreeMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Of concurrent map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofConcurrentHashSet() {
            return (Supplier) CONCURRENT_HASH_SET;
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<BiMap<K, V>> ofBiMap() {
            return (Supplier) BI_MAP;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Multiset<T>> ofMultiset() {
            return (Supplier) MULTISET;
        }

        /**
         *
         * @param <T>
         * @param valueMapType
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Multiset<T>> ofMultiset(final Class<? extends Map> valueMapType) {
            return () -> N.newMultiset(valueMapType);
        }

        /**
         *
         * @param <T>
         * @param mapSupplier
         * @return
         */
        public static <T> Supplier<Multiset<T>> ofMultiset(final java.util.function.Supplier<? extends Map<T, ?>> mapSupplier) {
            return () -> N.newMultiset(mapSupplier);
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap() {
            return (Supplier) LIST_MULTIMAP;
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @param mapType
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Class<? extends Map> mapType) {
            return () -> N.newListMultimap(mapType);
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @param mapType
         * @param valueType
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
            return () -> N.newListMultimap(mapType, valueType);
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @param mapSupplier
         * @param valueSupplier
         * @return
         */
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final java.util.function.Supplier<? extends Map<K, List<E>>> mapSupplier,
                final java.util.function.Supplier<? extends List<E>> valueSupplier) {
            return () -> N.newListMultimap(mapSupplier, valueSupplier);
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap() {
            return (Supplier) SET_MULTIMAP;
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @param mapType
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Class<? extends Map> mapType) {
            return () -> N.newSetMultimap(mapType);
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @param mapType
         * @param valueType
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
            return () -> N.newSetMultimap(mapType, valueType);
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @param mapSupplier
         * @param valueSupplier
         * @return
         */
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final java.util.function.Supplier<? extends Map<K, Set<E>>> mapSupplier,
                final java.util.function.Supplier<? extends Set<E>> valueSupplier) {
            return () -> N.newSetMultimap(mapSupplier, valueSupplier);
        }

        /**
         *
         * @param <K>
         * @param <E>
         * @param <V>
         * @param mapSupplier
         * @param valueSupplier
         * @return
         */
        public static <K, E, V extends Collection<E>> Supplier<Multimap<K, E, V>> ofMultimap(final java.util.function.Supplier<? extends Map<K, V>> mapSupplier,
                final java.util.function.Supplier<? extends V> valueSupplier) {
            return () -> N.newMultimap(mapSupplier, valueSupplier);
        }

        /**
         * Of string builder.
         *
         * @return
         */
        public static Supplier<StringBuilder> ofStringBuilder() {
            return STRING_BUILDER;
        }

        @SuppressWarnings("rawtypes")
        private static final Map<Class<?>, Supplier> collectionSupplierPool = new ConcurrentHashMap<>();

        /**
         *
         * @param <T>
         * @param targetType
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <T> java.util.function.Supplier<? extends Collection<T>> ofCollection(final Class<? extends Collection> targetType)
                throws IllegalArgumentException {
            Supplier ret = collectionSupplierPool.get(targetType);

            if (ret == null) {
                N.checkArgument(Collection.class.isAssignableFrom(targetType), "'targetType': {} is not a Collection class", targetType);

                if (Collection.class.equals(targetType) || AbstractCollection.class.equals(targetType) || List.class.equals(targetType)
                        || AbstractList.class.equals(targetType) || ArrayList.class.equals(targetType)) {
                    ret = ofList();
                } else if (LinkedList.class.equals(targetType)) {
                    ret = ofLinkedList();
                } else if (Set.class.equals(targetType) || AbstractSet.class.equals(targetType) || HashSet.class.equals(targetType)) {
                    ret = ofSet();
                } else if (LinkedHashSet.class.equals(targetType)) {
                    ret = ofLinkedHashSet();
                } else if (SortedSet.class.isAssignableFrom(targetType)) {
                    ret = ofSortedSet();
                } else if (Queue.class.equals(targetType) || AbstractQueue.class.equals(targetType) || Deque.class.equals(targetType)) {
                    return ofDeque();
                } else if (BlockingQueue.class.equals(targetType) || LinkedBlockingQueue.class.equals(targetType)) {
                    return ofLinkedBlockingQueue();
                } else if (BlockingDeque.class.equals(targetType) || LinkedBlockingDeque.class.equals(targetType)) {
                    return ofLinkedBlockingDeque();
                } else if (ConcurrentLinkedQueue.class.equals(targetType)) {
                    return ofConcurrentLinkedQueue();
                } else if (PriorityQueue.class.equals(targetType)) {
                    return ofPriorityQueue();
                } else if (ImmutableList.class.isAssignableFrom(targetType)) {
                    ret = ofList();
                } else if (ImmutableSet.class.isAssignableFrom(targetType)) {
                    ret = ofSet();
                } else if (Modifier.isAbstract(targetType.getModifiers())) {
                    throw new IllegalArgumentException("Can't create instance for abstract class: " + targetType);
                } else {
                    try {
                        if (N.newInstance(targetType) != null) {
                            ret = () -> N.newInstance(targetType);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }

                    if (ret == null) {
                        if (targetType.isAssignableFrom(LinkedHashSet.class)) {
                            ret = ofLinkedHashSet();
                        } else if (targetType.isAssignableFrom(HashSet.class)) {
                            ret = ofSet();
                        } else if (targetType.isAssignableFrom(LinkedList.class)) {
                            ret = ofLinkedList();
                        } else if (targetType.isAssignableFrom(ArrayList.class)) {
                            ret = ofList();
                        } else {
                            throw new IllegalArgumentException("Not able to create instance for collection: " + ClassUtil.getCanonicalClassName(targetType));
                        }
                    }
                }

                collectionSupplierPool.put(targetType, ret);
            }

            return ret;
        }

        @SuppressWarnings("rawtypes")
        private static final Map<Class<?>, Supplier> mapSupplierPool = new ConcurrentHashMap<>();

        /**
         *
         * @param <K>
         * @param <V>
         * @param targetType
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<? extends Map<K, V>> ofMap(final Class<? extends Map> targetType) throws IllegalArgumentException {
            Supplier ret = mapSupplierPool.get(targetType);

            if (ret == null) {
                N.checkArgument(Map.class.isAssignableFrom(targetType), "'targetType': {} is not a Map class", targetType);

                if (Map.class.equals(targetType) || AbstractMap.class.equals(targetType) || HashMap.class.equals(targetType)
                        || EnumMap.class.equals(targetType)) {
                    ret = ofMap();
                } else if (LinkedHashMap.class.equals(targetType)) {
                    ret = ofLinkedHashMap();
                } else if (SortedMap.class.isAssignableFrom(targetType)) {
                    ret = ofSortedMap();
                } else if (IdentityHashMap.class.isAssignableFrom(targetType)) {
                    ret = ofIdentityHashMap();
                } else if (ConcurrentHashMap.class.isAssignableFrom(targetType)) {
                    ret = ofConcurrentHashMap();
                } else if (BiMap.class.isAssignableFrom(targetType)) {
                    ret = ofBiMap();
                } else if (ImmutableMap.class.isAssignableFrom(targetType)) {
                    ret = ofMap();
                } else if (Modifier.isAbstract(targetType.getModifiers())) {
                    throw new IllegalArgumentException("Not able to create instance for abstract Map: " + targetType);
                } else {
                    try {
                        if (N.newInstance(targetType) != null) {
                            ret = () -> N.newInstance(targetType);
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }

                    if (ret == null) {
                        if (targetType.isAssignableFrom(TreeMap.class)) {
                            ret = ofTreeMap();
                        } else if (targetType.isAssignableFrom(LinkedHashMap.class)) {
                            ret = ofLinkedHashMap();
                        } else if (targetType.isAssignableFrom(HashMap.class)) {
                            ret = ofMap();
                        } else {
                            throw new IllegalArgumentException("Not able to create instance for Map: " + targetType);
                        }
                    }
                }

                mapSupplierPool.put(targetType, ret);
            }

            return ret;
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @param supplier
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <T extends Collection> boolean registerForCollection(final Class<T> targetClass, final java.util.function.Supplier<T> supplier)
                throws IllegalArgumentException {
            N.checkArgNotNull(targetClass, cs.targetClass);
            N.checkArgNotNull(supplier, cs.Supplier);

            if (N.isBuiltinClass(targetClass)) {
                throw new IllegalArgumentException("Can't register Supplier with built-in class: " + ClassUtil.getCanonicalClassName(targetClass));
            }

            if (collectionSupplierPool.containsKey(targetClass)) {
                return false;
            }

            return collectionSupplierPool.put(targetClass, Fn.from(supplier)) == null;
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @param supplier
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <T extends Map> boolean registerForMap(final Class<T> targetClass, final java.util.function.Supplier<T> supplier)
                throws IllegalArgumentException {
            N.checkArgNotNull(targetClass, cs.targetClass);
            N.checkArgNotNull(supplier, cs.Supplier);

            if (N.isBuiltinClass(targetClass)) {
                throw new IllegalArgumentException("Can't register Supplier with built-in class: " + ClassUtil.getCanonicalClassName(targetClass));
            }

            if (mapSupplierPool.containsKey(targetClass)) {
                return false;
            }

            return mapSupplierPool.put(targetClass, Fn.from(supplier)) == null;
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         * @deprecated unsupported operation.
         */
        @Deprecated
        public static Supplier<ImmutableList<?>> ofImmutableList() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         * @deprecated unsupported operation.
         */
        @Deprecated
        public static Supplier<ImmutableSet<?>> ofImmutableSet() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         * @deprecated unsupported operation.
         */
        @Deprecated
        public static Supplier<ImmutableMap<?, ?>> ofImmutableMap() {
            throw new UnsupportedOperationException();
        }

        //    /**
        //     *
        //     * @param <T>
        //     * @param <C>
        //     * @param supplier
        //     * @return a stateful {@code Supplier}. Don't save or cache for reuse or use it in parallel stream.
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @SequentialOnly
        //    @Stateful
        //    public static <T, C extends Collection<T>> Supplier<? extends C> single(final java.util.function.Supplier<? extends C> supplier) {
        //        return new Supplier<>() {
        //            private C c = null;
        //
        //            @Override
        //            public C get() {
        //                if (c == null) {
        //                    c = supplier.get();
        //                } else {
        //                    c.clear();
        //                }
        //
        //                return c;
        //            }
        //        };
        //    }

        private static final Supplier<Exception> EXCEPTION = Exception::new;

        @Beta
        public static Supplier<Exception> newException() {
            return EXCEPTION;
        }

        private static final Supplier<RuntimeException> RUNTIME_EXCEPTION = RuntimeException::new;

        @Beta
        public static Supplier<RuntimeException> newRuntimeException() {
            return RUNTIME_EXCEPTION;
        }

        private static final Supplier<NoSuchElementException> NO_SUCH_ELEMENT_EXCEPTION = NoSuchElementException::new;

        @Beta
        public static Supplier<NoSuchElementException> newNoSuchElementException() {
            return NO_SUCH_ELEMENT_EXCEPTION;
        }
    }

    /**
     * The Class Factory.
     */
    @SuppressWarnings({ "java:S1694" })
    public abstract sealed static class IntFunctions permits Factory {

        /** The Constant BOOLEAN_ARRAY. */
        private static final IntFunction<boolean[]> BOOLEAN_ARRAY = boolean[]::new;

        /** The Constant CHAR_ARRAY. */
        private static final IntFunction<char[]> CHAR_ARRAY = char[]::new;

        /** The Constant BYTE_ARRAY. */
        private static final IntFunction<byte[]> BYTE_ARRAY = byte[]::new;

        /** The Constant SHORT_ARRAY. */
        private static final IntFunction<short[]> SHORT_ARRAY = short[]::new;

        /** The Constant INT_ARRAY. */
        private static final IntFunction<int[]> INT_ARRAY = int[]::new;

        /** The Constant LONG_ARRAY. */
        private static final IntFunction<long[]> LONG_ARRAY = long[]::new;

        /** The Constant FLOAT_ARRAY. */
        private static final IntFunction<float[]> FLOAT_ARRAY = float[]::new;

        /** The Constant DOUBLE_ARRAY. */
        private static final IntFunction<double[]> DOUBLE_ARRAY = double[]::new;

        /** The Constant STRING_ARRAY. */
        private static final IntFunction<String[]> STRING_ARRAY = String[]::new;

        /** The Constant OBJECT_ARRAY. */
        private static final IntFunction<Object[]> OBJECT_ARRAY = Object[]::new;

        /** The Constant BOOLEAN_LIST. */
        private static final IntFunction<BooleanList> BOOLEAN_LIST = BooleanList::new;

        /** The Constant CHAR_LIST. */
        private static final IntFunction<CharList> CHAR_LIST = CharList::new;

        /** The Constant BYTE_LIST. */
        private static final IntFunction<ByteList> BYTE_LIST = ByteList::new;

        /** The Constant SHORT_LIST. */
        private static final IntFunction<ShortList> SHORT_LIST = ShortList::new;

        /** The Constant INT_LIST. */
        private static final IntFunction<IntList> INT_LIST = IntList::new;

        /** The Constant LONG_LIST. */
        private static final IntFunction<LongList> LONG_LIST = LongList::new;

        /** The Constant FLOAT_LIST. */
        private static final IntFunction<FloatList> FLOAT_LIST = FloatList::new;

        /** The Constant DOUBLE_LIST. */
        private static final IntFunction<DoubleList> DOUBLE_LIST = DoubleList::new;

        /** The Constant LIST_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super List> LIST_FACTORY = ArrayList::new;

        /** The Constant LINKED_LIST_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedList> LINKED_LIST_FACTORY = len -> new LinkedList<>();

        /** The Constant SET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Set> SET_FACTORY = N::newHashSet;

        /** The Constant LINKED_HASH_SET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Set> LINKED_HASH_SET_FACTORY = N::newLinkedHashSet;

        /** The Constant TREE_SET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super TreeSet> TREE_SET_FACTORY = len -> new TreeSet<>();

        /** The Constant QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Queue> QUEUE_FACTORY = len -> new LinkedList();

        /** The Constant DEQUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Deque> DEQUE_FACTORY = len -> new LinkedList();

        /** The Constant ARRAY_DEQUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ArrayDeque> ARRAY_DEQUE_FACTORY = ArrayDeque::new;

        /** The Constant LINKED_BLOCKING_QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedBlockingQueue> LINKED_BLOCKING_QUEUE_FACTORY = LinkedBlockingQueue::new;

        /** The Constant ARRAY_BLOCKING_QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ArrayBlockingQueue> ARRAY_BLOCKING_QUEUE_FACTORY = ArrayBlockingQueue::new;

        /** The Constant LINKED_BLOCKING_DEQUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedBlockingDeque> LINKED_BLOCKING_DEQUE_FACTORY = LinkedBlockingDeque::new;

        /** The Constant CONCURRENT_LINKED_QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ConcurrentLinkedQueue> CONCURRENT_LINKED_QUEUE_FACTORY = capacity -> new ConcurrentLinkedQueue();

        /** The Constant PRIORITY_QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super PriorityQueue> PRIORITY_QUEUE_FACTORY = PriorityQueue::new;

        /** The Constant MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Map> MAP_FACTORY = N::newHashMap;

        /** The Constant LINKED_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Map> LINKED_HASH_MAP_FACTORY = N::newLinkedHashMap;

        /** The Constant IDENTITY_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super IdentityHashMap> IDENTITY_HASH_MAP_FACTORY = N::newIdentityHashMap;

        /** The Constant TREE_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super TreeMap> TREE_MAP_FACTORY = len -> N.newTreeMap();

        /** The Constant CONCURRENT_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ConcurrentHashMap> CONCURRENT_HASH_MAP_FACTORY = N::newConcurrentHashMap;

        /** The Constant BI_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super BiMap> BI_MAP_FACTORY = N::newBiMap;

        /** The Constant MULTISET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Multiset> MULTISET_FACTORY = N::newMultiset;

        /** The Constant LIST_MULTIMAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ListMultimap> LIST_MULTIMAP_FACTORY = N::newLinkedListMultimap;

        /** The Constant SET_MULTIMAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super SetMultimap> SET_MULTIMAP_FACTORY = N::newSetMultimap;

        protected IntFunctions() {
            // utility class
        }

        /**
         * <p>Returns the provided IntFunction as is - a shorthand identity method for IntFunction instances.</p>
         * 
         * <p>This method serves as a shorthand convenience method that can help with type inference
         * in certain contexts. It's part of a family of factory methods that handle various function types.</p>
         * 
         * <p>Example usage:</p>
         * <pre>
         * // Instead of explicitly typing:
         * IntFunction&lt;String[]&gt; arrayCreator = size -> new String[size];
         * // You can use:
         * var arrayCreator = Factory.of(size -> new String[size]);
         * </pre>
         *
         * @param <T> the type of the result of the function
         * @param func the IntFunction to return
         * @return the IntFunction unchanged
         */
        public static <T> IntFunction<T> of(IntFunction<T> func) {
            return func;
        }

        /**
         * Of boolean array.
         *
         * @return
         */
        public static IntFunction<boolean[]> ofBooleanArray() {
            return BOOLEAN_ARRAY;
        }

        /**
         * Of char array.
         *
         * @return
         */
        public static IntFunction<char[]> ofCharArray() {
            return CHAR_ARRAY;
        }

        /**
         * Of byte array.
         *
         * @return
         */
        public static IntFunction<byte[]> ofByteArray() {
            return BYTE_ARRAY;
        }

        /**
         * Of short array.
         *
         * @return
         */
        public static IntFunction<short[]> ofShortArray() {
            return SHORT_ARRAY;
        }

        /**
         * Of int array.
         *
         * @return
         */
        public static IntFunction<int[]> ofIntArray() {
            return INT_ARRAY;
        }

        /**
         * Of long array.
         *
         * @return
         */
        public static IntFunction<long[]> ofLongArray() {
            return LONG_ARRAY;
        }

        /**
         * Of float array.
         *
         * @return
         */
        public static IntFunction<float[]> ofFloatArray() {
            return FLOAT_ARRAY;
        }

        /**
         * Of double array.
         *
         * @return
         */
        public static IntFunction<double[]> ofDoubleArray() {
            return DOUBLE_ARRAY;
        }

        /**
         * Of string array.
         *
         * @return
         */
        public static IntFunction<String[]> ofStringArray() {
            return STRING_ARRAY;
        }

        /**
         * Of object array.
         *
         * @return
         */
        public static IntFunction<Object[]> ofObjectArray() {
            return OBJECT_ARRAY;
        }

        /**
         * Of boolean list.
         *
         * @return
         */
        public static IntFunction<BooleanList> ofBooleanList() {
            return BOOLEAN_LIST;
        }

        /**
         * Of char list.
         *
         * @return
         */
        public static IntFunction<CharList> ofCharList() {
            return CHAR_LIST;
        }

        /**
         * Of byte list.
         *
         * @return
         */
        public static IntFunction<ByteList> ofByteList() {
            return BYTE_LIST;
        }

        /**
         * Of short list.
         *
         * @return
         */
        public static IntFunction<ShortList> ofShortList() {
            return SHORT_LIST;
        }

        /**
         * Of int list.
         *
         * @return
         */
        public static IntFunction<IntList> ofIntList() {
            return INT_LIST;
        }

        /**
         * Of long list.
         *
         * @return
         */
        public static IntFunction<LongList> ofLongList() {
            return LONG_LIST;
        }

        /**
         * Of float list.
         *
         * @return
         */
        public static IntFunction<FloatList> ofFloatList() {
            return FLOAT_LIST;
        }

        /**
         * Of double list.
         *
         * @return
         */
        public static IntFunction<DoubleList> ofDoubleList() {
            return DOUBLE_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<List<T>> ofList() {
            return (IntFunction) LIST_FACTORY;
        }

        /**
         * Of linked list.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedList<T>> ofLinkedList() {
            return (IntFunction) LINKED_LIST_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Set<T>> ofSet() {
            return (IntFunction) SET_FACTORY;
        }

        /**
         * Of linked hash set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Set<T>> ofLinkedHashSet() {
            return (IntFunction) LINKED_HASH_SET_FACTORY;
        }

        /**
         * Of sorted set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<SortedSet<T>> ofSortedSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Of navigable set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<NavigableSet<T>> ofNavigableSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Of tree set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<TreeSet<T>> ofTreeSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Queue<T>> ofQueue() {
            return (IntFunction) QUEUE_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Deque<T>> ofDeque() {
            return (IntFunction) DEQUE_FACTORY;
        }

        /**
         * Of array deque.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ArrayDeque<T>> ofArrayDeque() {
            return (IntFunction) ARRAY_DEQUE_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (IntFunction) LINKED_BLOCKING_QUEUE_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ArrayBlockingQueue<T>> ofArrayBlockingQueue() {
            return (IntFunction) ARRAY_BLOCKING_QUEUE_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedBlockingDeque<T>> ofLinkedBlockingDeque() {
            return (IntFunction) LINKED_BLOCKING_DEQUE_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (IntFunction) CONCURRENT_LINKED_QUEUE_FACTORY;
        }

        /**
         * Of priority queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<PriorityQueue<T>> ofPriorityQueue() {
            return (IntFunction) PRIORITY_QUEUE_FACTORY;
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<Map<K, V>> ofMap() {
            return (IntFunction) MAP_FACTORY;
        }

        /**
         * Of linked hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<Map<K, V>> ofLinkedHashMap() {
            return (IntFunction) LINKED_HASH_MAP_FACTORY;
        }

        /**
         * Of identity hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<IdentityHashMap<K, V>> ofIdentityHashMap() {
            return (IntFunction) IDENTITY_HASH_MAP_FACTORY;
        }

        /**
         * Of sorted map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<SortedMap<K, V>> ofSortedMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Of navigable map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<NavigableMap<K, V>> ofNavigableMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Of tree map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<TreeMap<K, V>> ofTreeMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Of concurrent map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
        }

        /**
         * Of concurrent hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
        }

        /**
         * Of {@code BiMap}.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<BiMap<K, V>> ofBiMap() {
            return (IntFunction) BI_MAP_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Multiset<T>> ofMultiset() {
            return (IntFunction) MULTISET_FACTORY;
        }

        /**
         * Of list multimap.
         *
         * @param <K> the key type
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> IntFunction<ListMultimap<K, E>> ofListMultimap() {
            return (IntFunction) LIST_MULTIMAP_FACTORY;
        }

        /**
         * Of set multimap.
         *
         * @param <K> the key type
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> IntFunction<SetMultimap<K, E>> ofSetMultimap() {
            return (IntFunction) SET_MULTIMAP_FACTORY;
        }

        /**
         * Returns a new created {@code IntFunction} whose {@code apply} will return the same {@code DisposableObjArray} which is defined as a private field.
         *
         * @return a stateful {@code IntFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static IntFunction<DisposableObjArray> ofDisposableArray() {
            return new IntFunction<>() {
                private DisposableObjArray ret = null;

                @Override
                public DisposableObjArray apply(final int len) {
                    if (ret == null) {
                        ret = DisposableObjArray.wrap(new Object[len]);
                    }

                    return ret;
                }
            };
        }

        /**
         * Returns a new created {@code IntFunction} whose {@code apply} will return the same {@code DisposableArray} which is defined as a private field.
         *
         * @param <T>
         * @param componentType
         * @return a stateful {@code IntFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> IntFunction<DisposableArray<T>> ofDisposableArray(final Class<T> componentType) {
            return new IntFunction<>() {
                private DisposableArray<T> ret = null;

                @Override
                public DisposableArray<T> apply(final int len) {
                    if (ret == null) {
                        ret = DisposableArray.wrap(N.newArray(componentType, len));
                    }

                    return ret;
                }
            };
        }

        @SuppressWarnings("rawtypes")
        private static final Map<Class<?>, IntFunction> collectionCreatorPool = new ConcurrentHashMap<>();

        /**
         *
         * @param <T>
         * @param targetType
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<? extends Collection<T>> ofCollection(final Class<? extends Collection> targetType) throws IllegalArgumentException {
            IntFunction ret = collectionCreatorPool.get(targetType);

            if (ret == null) {
                N.checkArgument(Collection.class.isAssignableFrom(targetType), "'targetType': {} is not a Collection class", targetType);

                if (Collection.class.equals(targetType) || AbstractCollection.class.equals(targetType) || List.class.equals(targetType)
                        || AbstractList.class.equals(targetType) || ArrayList.class.equals(targetType)) {
                    ret = ofList();
                } else if (LinkedList.class.equals(targetType)) {
                    ret = ofLinkedList();
                } else if (Set.class.equals(targetType) || AbstractSet.class.equals(targetType) || HashSet.class.equals(targetType)) {
                    ret = ofSet();
                } else if (LinkedHashSet.class.equals(targetType)) {
                    ret = ofLinkedHashSet();
                } else if (SortedSet.class.isAssignableFrom(targetType)) {
                    ret = ofSortedSet();
                } else if (Queue.class.equals(targetType) || AbstractQueue.class.equals(targetType) || Deque.class.equals(targetType)) {
                    return ofDeque();
                } else if (BlockingQueue.class.equals(targetType) || LinkedBlockingQueue.class.equals(targetType)) {
                    return ofLinkedBlockingQueue();
                } else if (ArrayBlockingQueue.class.equals(targetType)) {
                    return ofArrayBlockingQueue();
                } else if (BlockingDeque.class.equals(targetType) || LinkedBlockingDeque.class.equals(targetType)) {
                    return ofLinkedBlockingDeque();
                } else if (ConcurrentLinkedQueue.class.equals(targetType)) {
                    return ofConcurrentLinkedQueue();
                } else if (PriorityQueue.class.equals(targetType)) {
                    return ofPriorityQueue();
                } else if (ImmutableList.class.isAssignableFrom(targetType)) {
                    ret = ofList();
                } else if (ImmutableSet.class.isAssignableFrom(targetType)) {
                    ret = ofSet();
                } else if (Modifier.isAbstract(targetType.getModifiers())) {
                    throw new IllegalArgumentException("Not able to create instance for collection: " + targetType);
                } else {
                    try {
                        final Constructor<?> constructor = ClassUtil.getDeclaredConstructor(targetType, int.class);

                        //noinspection ConstantValue
                        if (constructor != null && N.invoke(constructor, 9) != null) { // magic number?
                            ret = size -> {
                                try {
                                    return (Collection<T>) N.invoke(constructor, size);
                                } catch (final Throwable e) { // NOSONAR
                                    throw new IllegalArgumentException("Not able to create instance for collection: " + targetType, e);
                                }
                            };
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }

                    try {
                        if (ret == null && N.newInstance(targetType) != null) {
                            ret = size -> {
                                try {
                                    return (Collection<T>) N.newInstance(targetType);
                                } catch (final Exception e) {
                                    throw new IllegalArgumentException("Not able to create instance for collection: " + targetType, e);
                                }
                            };
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }

                    if (ret == null) {
                        if (targetType.isAssignableFrom(LinkedHashSet.class)) {
                            ret = ofLinkedHashSet();
                        } else if (targetType.isAssignableFrom(HashSet.class)) {
                            ret = ofSet();
                        } else if (targetType.isAssignableFrom(LinkedList.class)) {
                            ret = ofLinkedList();
                        } else if (targetType.isAssignableFrom(ArrayList.class)) {
                            ret = ofList();
                        } else {
                            throw new IllegalArgumentException("Not able to create instance for collection: " + targetType);
                        }
                    }
                }

                collectionCreatorPool.put(targetType, ret);
            }

            return ret;
        }

        @SuppressWarnings("rawtypes")
        private static final Map<Class<?>, IntFunction> mapCreatorPool = new ConcurrentHashMap<>();

        /**
         *
         * @param <K>
         * @param <V>
         * @param targetType
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<? extends Map<K, V>> ofMap(final Class<? extends Map> targetType) throws IllegalArgumentException {
            IntFunction ret = mapCreatorPool.get(targetType);

            if (ret == null) {
                N.checkArgument(Map.class.isAssignableFrom(targetType), "'targetType': {} is not a Map class", targetType);

                if (Map.class.equals(targetType) || AbstractMap.class.equals(targetType) || HashMap.class.equals(targetType)
                        || EnumMap.class.equals(targetType)) {
                    ret = ofMap();
                } else if (LinkedHashMap.class.equals(targetType)) {
                    ret = ofLinkedHashMap();
                } else if (SortedMap.class.isAssignableFrom(targetType)) {
                    ret = ofSortedMap();
                } else if (IdentityHashMap.class.isAssignableFrom(targetType)) {
                    ret = ofIdentityHashMap();
                } else if (ConcurrentHashMap.class.isAssignableFrom(targetType)) {
                    ret = ofConcurrentHashMap();
                } else if (BiMap.class.isAssignableFrom(targetType)) {
                    ret = ofBiMap();
                } else if (ImmutableMap.class.isAssignableFrom(targetType)) {
                    ret = ofMap();
                } else if (Modifier.isAbstract(targetType.getModifiers())) {
                    throw new IllegalArgumentException("Not able to create instance for abstract Map: " + targetType);
                } else {
                    try {
                        final Constructor<?> constructor = ClassUtil.getDeclaredConstructor(targetType, int.class);

                        //noinspection ConstantValue
                        if (constructor != null && N.invoke(constructor, 9) != null) { // magic number?
                            ret = size -> {
                                try {
                                    return (Map<K, V>) N.invoke(constructor, size);
                                } catch (final Throwable e) { // NOSONAR
                                    throw new IllegalArgumentException("Not able to create instance for Map: " + targetType, e);
                                }
                            };
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }

                    try {
                        if (ret == null && N.newInstance(targetType) != null) {
                            ret = size -> {
                                try {
                                    return (Map<K, V>) N.newInstance(targetType);
                                } catch (final Exception e) {
                                    throw new IllegalArgumentException("Not able to create instance for Map: " + targetType, e);
                                }
                            };
                        }
                    } catch (final Throwable e) { // NOSONAR
                        // ignore
                    }

                    if (ret == null) {
                        if (targetType.isAssignableFrom(TreeMap.class)) {
                            ret = ofTreeMap();
                        } else if (targetType.isAssignableFrom(LinkedHashMap.class)) {
                            ret = ofLinkedHashMap();
                        } else if (targetType.isAssignableFrom(HashMap.class)) {
                            ret = ofMap();
                        } else {
                            throw new IllegalArgumentException("Not able to create instance for Map: " + ClassUtil.getCanonicalClassName(targetType));
                        }
                    }
                }

                mapCreatorPool.put(targetType, ret);
            }

            return ret;
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @param creator
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <T extends Collection> boolean registerForCollection(final Class<T> targetClass, final java.util.function.IntFunction<T> creator)
                throws IllegalArgumentException {
            N.checkArgNotNull(targetClass, cs.targetClass);
            N.checkArgNotNull(creator, cs.creator);

            if (N.isBuiltinClass(targetClass)) {
                throw new IllegalArgumentException("Can't register IntFunction with built-in class: " + ClassUtil.getCanonicalClassName(targetClass));
            }

            if (collectionCreatorPool.containsKey(targetClass)) {
                return false;
            }

            return collectionCreatorPool.put(targetClass, Fn.from(creator)) == null;
        }

        /**
         *
         * @param <T>
         * @param targetClass
         * @param creator
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <T extends Map> boolean registerForMap(final Class<T> targetClass, final java.util.function.IntFunction<T> creator)
                throws IllegalArgumentException {
            N.checkArgNotNull(targetClass, cs.targetClass);
            N.checkArgNotNull(creator, cs.creator);

            if (N.isBuiltinClass(targetClass)) {
                throw new IllegalArgumentException("Can't register IntFunction with built-in class: " + ClassUtil.getCanonicalClassName(targetClass));
            }

            if (mapCreatorPool.containsKey(targetClass)) {
                return false;
            }

            return mapCreatorPool.put(targetClass, Fn.from(creator)) == null;

        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         * @deprecated unsupported operation.
         */
        @Deprecated
        public static IntFunction<ImmutableList<?>> ofImmutableList() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         * @deprecated unsupported operation.
         */
        @Deprecated
        public static IntFunction<ImmutableSet<?>> ofImmutableSet() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         * @deprecated unsupported operation.
         */
        @Deprecated
        public static IntFunction<ImmutableMap<?, ?>> ofImmutableMap() {
            throw new UnsupportedOperationException();
        }

        //    /**
        //     *
        //     * @param <T>
        //     * @param <C>
        //     * @param supplier
        //     * @return a stateful {@code IntFunction}. Don't save or cache for reuse or use it in parallel stream.
        //     * @deprecated
        //     */
        //    @Deprecated
        //    @SequentialOnly
        //    @Stateful
        //    public static <T, C extends Collection<T>> IntFunction<? extends C> single(final java.util.function.IntFunction<? extends C> supplier) {
        //        return new IntFunction<>() {
        //            private C c = null;
        //
        //            @Override
        //            public C apply(final int t) {
        //                if (c == null) {
        //                    c = supplier.apply(t);
        //                } else {
        //                    c.clear();
        //                }
        //
        //                return c;
        //            }
        //        };
        //    }
    }

    /**
     * The Class
     */
    public static final class Factory extends IntFunctions {
        private Factory() {
            // utility class
        }
    }

    /**
     * The Class Predicates.
     */
    public static final class Predicates {

        private Predicates() {
        }

        /**
         * Returns a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param predicate
         * @return a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
         * @throws IllegalArgumentException
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Predicate<T> indexed(final IntObjPredicate<T> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return new Predicate<>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public boolean test(final T t) {
                    return predicate.test(idx.getAndIncrement(), t);
                }
            };
        }

        /**
         * Returns a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @return a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Predicate<T> distinct() {
            return new Predicate<>() {
                private final Set<Object> set = N.newHashSet();

                @Override
                public boolean test(final T value) {
                    return set.add(value);
                }
            };
        }

        /**
         * Returns a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param mapper
         * @return a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Predicate<T> distinctBy(final java.util.function.Function<? super T, ?> mapper) {
            return new Predicate<>() {
                private final Set<Object> set = N.newHashSet();

                @Override
                public boolean test(final T value) {
                    return set.add(mapper.apply(value));
                }
            };
        }

        /**
         * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
         *
         * @param <T>
         * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
         */
        @Beta
        @Stateful
        public static <T> Predicate<T> concurrentDistinct() {
            return new Predicate<>() {
                private final Map<Object, Object> map = new ConcurrentHashMap<>();

                @Override
                public boolean test(final T value) {
                    return map.put(value, NONE) == null;
                }
            };
        }

        /**
         * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
         *
         * @param <T>
         * @param mapper
         * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
         */
        @Beta
        @Stateful
        public static <T> Predicate<T> concurrentDistinctBy(final java.util.function.Function<? super T, ?> mapper) {
            return new Predicate<>() {
                private final Map<Object, Object> map = new ConcurrentHashMap<>();

                @Override
                public boolean test(final T value) {
                    return map.put(mapper.apply(value), NONE) == null;
                }
            };
        }

        /**
         * Returns a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
         * Remove the continuous repeat elements.
         *
         * @param <T>
         * @return a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Predicate<T> skipRepeats() {
            return new Predicate<>() {
                private T pre = (T) NONE;

                @Override
                public boolean test(final T value) {
                    final boolean res = pre == NONE || !N.equals(value, pre);
                    pre = value;
                    return res;
                }
            };
        }
    }

    /**
     * The Class BiPredicates.
     */
    public static final class BiPredicates {

        /** The Constant ALWAYS_TRUE. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate ALWAYS_TRUE = (t, u) -> true;

        /** The Constant ALWAYS_FALSE. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate ALWAYS_FALSE = (t, u) -> false;

        /** The Constant EQUAL. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate EQUAL = N::equals;

        /** The Constant NOT_EQUAL. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate NOT_EQUAL = (t, u) -> !N.equals(t, u);

        /** The Constant GREATER_THAN. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> GREATER_THAN = (t, u) -> N.compare(t, u) > 0;

        /** The Constant GREATER_EQUAL. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> GREATER_EQUAL = (t, u) -> N.compare(t, u) >= 0;

        /** The Constant LESS_THAN. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> LESS_THAN = (t, u) -> N.compare(t, u) < 0;

        /** The Constant LESS_EQUAL. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> LESS_EQUAL = (t, u) -> N.compare(t, u) <= 0;

        private BiPredicates() {
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiPredicate<T, U> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiPredicate<T, U> alwaysFalse() {
            return ALWAYS_FALSE;
        }

        /**
         * Returns a stateful {@code BiPredicate}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param <U>
         * @param predicate
         * @return a stateful {@code BiPredicate}. Don't save or cache for reuse or use it in parallel stream.
         * @throws IllegalArgumentException
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T, U> BiPredicate<T, U> indexed(final IntBiObjPredicate<T, U> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return new BiPredicate<>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public boolean test(final T t, final U u) {
                    return predicate.test(idx.getAndIncrement(), t, u);
                }
            };
        }
    }

    /**
     * The Class TriPredicates.
     */
    public static final class TriPredicates {

        /** The Constant ALWAYS_TRUE. */
        @SuppressWarnings("rawtypes")
        private static final TriPredicate ALWAYS_TRUE = (a, b, c) -> true;

        /** The Constant ALWAYS_FALSE. */
        @SuppressWarnings({ "rawtypes" })
        private static final TriPredicate ALWAYS_FALSE = (a, b, c) -> false;

        private TriPredicates() {
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @return
         */
        public static <A, B, C> TriPredicate<A, B, C> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @return
         */
        public static <A, B, C> TriPredicate<A, B, C> alwaysFalse() {
            return ALWAYS_FALSE;
        }

    }

    /**
     * The Class Consumers.
     */
    public static final class Consumers {
        private Consumers() {
        }

        /**
         * Returns a stateful {@code BiPredicate}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param action
         * @return a stateful {@code Consumer}. Don't save or cache for reuse or use it in parallel stream.
         * @throws IllegalArgumentException
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Consumer<T> indexed(final IntObjConsumer<T> action) throws IllegalArgumentException {
            N.checkArgNotNull(action);

            return new Consumer<>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public void accept(final T t) {
                    action.accept(idx.getAndIncrement(), t);
                }
            };
        }
    }

    /**
     * The Class BiConsumers.
     */
    public static final class BiConsumers {

        /** The Constant DO_NOTHING. */
        @SuppressWarnings("rawtypes")
        private static final BiConsumer DO_NOTHING = (t, u) -> {
            // do nothing.
        };

        /** The Constant ADD. */
        private static final BiConsumer<Collection<Object>, Object> ADD = Collection::add;

        /** The Constant ADD_ALL. */
        private static final BiConsumer<Collection<Object>, Collection<Object>> ADD_ALL = Collection::addAll;

        /** The Constant ADD_ALL_2. */
        @SuppressWarnings("rawtypes")
        private static final BiConsumer<PrimitiveList, PrimitiveList> ADD_ALL_2 = PrimitiveList::addAll;

        /** The Constant REMOVE. */
        private static final BiConsumer<Collection<Object>, Object> REMOVE = Collection::remove;

        /** The Constant REMOVE_ALL. */
        private static final BiConsumer<Collection<Object>, Collection<Object>> REMOVE_ALL = Collection::removeAll;

        /** The Constant REMOVE_ALL_2. */
        @SuppressWarnings("rawtypes")
        private static final BiConsumer<PrimitiveList, PrimitiveList> REMOVE_ALL_2 = PrimitiveList::removeAll;

        /** The Constant PUT. */
        private static final BiConsumer<Map<Object, Object>, Map.Entry<Object, Object>> PUT = (t, u) -> t.put(u.getKey(), u.getValue());

        /** The Constant PUT_ALL. */
        private static final BiConsumer<Map<Object, Object>, Map<Object, Object>> PUT_ALL = Map::putAll;

        /** The Constant REMOVE_BY_KEY. */
        private static final BiConsumer<Map<Object, Object>, Object> REMOVE_BY_KEY = Map::remove;

        /** The Constant MERGE. */
        private static final BiConsumer<Joiner, Joiner> MERGE = Joiner::merge;

        /** The Constant APPEND. */
        private static final BiConsumer<StringBuilder, Object> APPEND = StringBuilder::append;

        private BiConsumers() {
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiConsumer<T, U> doNothing() {
            return DO_NOTHING;
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofAdd() {
            return (BiConsumer<C, T>) ADD;
        }

        /**
         * Of add all.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<T>> BiConsumer<C, C> ofAddAll() {
            return (BiConsumer<C, C>) ADD_ALL;
        }

        /**
         * Of add alll.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiConsumer<T, T> ofAddAlll() {
            return (BiConsumer<T, T>) ADD_ALL_2;
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofRemove() {
            return (BiConsumer<C, T>) REMOVE;
        }

        /**
         * Of remove all.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<T>> BiConsumer<C, C> ofRemoveAll() {
            return (BiConsumer<C, C>) REMOVE_ALL;
        }

        /**
         * Of remove alll.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiConsumer<T, T> ofRemoveAlll() {
            return (BiConsumer<T, T>) REMOVE_ALL_2;
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param <E>
         * @return
         */
        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiConsumer<M, E> ofPut() {
            return (BiConsumer<M, E>) PUT;
        }

        /**
         * Of put all.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        public static <K, V, M extends Map<K, V>> BiConsumer<M, M> ofPutAll() {
            return (BiConsumer<M, M>) PUT_ALL;
        }

        /**
         * Of remove by key.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        public static <K, V, M extends Map<K, V>> BiConsumer<M, K> ofRemoveByKey() {
            return (BiConsumer<M, K>) REMOVE_BY_KEY;
        }

        public static BiConsumer<Joiner, Joiner> ofMerge() {
            return MERGE;
        }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T> BiConsumer<StringBuilder, T> ofAppend() {
            return (BiConsumer<StringBuilder, T>) APPEND;
        }

        /**
         * Returns a stateful {@code BiPredicate}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param <U>
         * @param action
         * @return a stateful {@code BiPredicate}. Don't save or cache for reuse or use it in parallel stream.
         * @throws IllegalArgumentException
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T, U> BiConsumer<T, U> indexed(final IntBiObjConsumer<T, U> action) throws IllegalArgumentException {
            N.checkArgNotNull(action);

            return new BiConsumer<>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public void accept(final T t, final U u) {
                    action.accept(idx.getAndIncrement(), t, u);
                }
            };
        }
    }

    /**
     * The Class TriConsumers.
     */
    public static final class TriConsumers {
        private TriConsumers() {
        }
    }

    /**
     * The Class Functions.
     */
    public static final class Functions {

        private Functions() {
        }

        /**
         * Returns a stateful {@code Function}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param <R>
         * @param func
         * @return a stateful {@code Function}. Don't save or cache for reuse or use it in parallel stream.
         * @throws IllegalArgumentException
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T, R> Function<T, R> indexed(final IntObjFunction<T, ? extends R> func) throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return new Function<>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public R apply(final T t) {
                    return func.apply(idx.getAndIncrement(), t);
                }
            };
        }
    }

    /**
     * The Class BiFunctions.
     */
    public static final class BiFunctions {

        /** The Constant RETURN_FIRST. */
        private static final BiFunction<Object, Object, Object> RETURN_FIRST = (t, u) -> t;

        /** The Constant RETURN_SECOND. */
        private static final BiFunction<Object, Object, Object> RETURN_SECOND = (t, u) -> u;

        /** The Constant ADD. */
        private static final BiFunction<Collection<Object>, Object, Collection<Object>> ADD = (t, u) -> {
            t.add(u);
            return t;
        };

        /** The Constant ADD_ALL. */
        private static final BiFunction<Collection<Object>, Collection<Object>, Collection<Object>> ADD_ALL = (t, u) -> {
            t.addAll(u);
            return t;
        };

        /** The Constant ADD_ALL_2. */
        @SuppressWarnings("rawtypes")
        private static final BiFunction<PrimitiveList, PrimitiveList, PrimitiveList> ADD_ALL_2 = (t, u) -> {
            t.addAll(u);
            return t;
        };

        /** The Constant REMOVE. */
        private static final BiFunction<Collection<Object>, Object, Collection<Object>> REMOVE = (t, u) -> {
            t.remove(u);
            return t;
        };

        /** The Constant REMOVE_ALL. */
        private static final BiFunction<Collection<Object>, Collection<Object>, Collection<Object>> REMOVE_ALL = (t, u) -> {
            t.removeAll(u);
            return t;
        };

        /** The Constant REMOVE_ALL_2. */
        @SuppressWarnings("rawtypes")
        private static final BiFunction<PrimitiveList, PrimitiveList, PrimitiveList> REMOVE_ALL_2 = (t, u) -> {
            t.removeAll(u);
            return t;
        };

        /** The Constant PUT. */
        private static final BiFunction<Map<Object, Object>, Map.Entry<Object, Object>, Map<Object, Object>> PUT = (t, u) -> {
            t.put(u.getKey(), u.getValue());
            return t;
        };

        /** The Constant PUT_ALL. */
        private static final BiFunction<Map<Object, Object>, Map<Object, Object>, Map<Object, Object>> PUT_ALL = (t, u) -> {
            t.putAll(u);
            return t;
        };

        /** The Constant REMOVE_BY_KEY. */
        private static final BiFunction<Map<Object, Object>, Object, Map<Object, Object>> REMOVE_BY_KEY = (t, u) -> {
            t.remove(u);
            return t;
        };

        /** The Constant MERGE. */
        private static final BiFunction<Joiner, Joiner, Joiner> MERGE = Joiner::merge;

        /** The Constant APPEND. */
        private static final BiFunction<StringBuilder, Object, StringBuilder> APPEND = StringBuilder::append;

        private BiFunctions() {
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiFunction<T, U, T> selectFirst() {
            return (BiFunction<T, U, T>) RETURN_FIRST;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiFunction<T, U, U> selectSecond() {
            return (BiFunction<T, U, U>) RETURN_SECOND;
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofAdd() {
            return (BiFunction<C, T, C>) ADD;
        }

        /**
         * Of add all.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofAddAll() {
            return (BiFunction<C, C, C>) ADD_ALL;
        }

        /**
         * Of add alll.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiFunction<T, T, T> ofAddAlll() {
            return (BiFunction<T, T, T>) ADD_ALL_2;
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofRemove() {
            return (BiFunction<C, T, C>) REMOVE;
        }

        /**
         * Of remove all.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofRemoveAll() {
            return (BiFunction<C, C, C>) REMOVE_ALL;
        }

        /**
         * Of remove alll.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiFunction<T, T, T> ofRemoveAlll() {
            return (BiFunction<T, T, T>) REMOVE_ALL_2;
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param <E>
         * @return
         */
        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiFunction<M, E, M> ofPut() {
            return (BiFunction<M, E, M>) PUT;
        }

        /**
         * Of put all.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        public static <K, V, M extends Map<K, V>> BiFunction<M, M, M> ofPutAll() {
            return (BiFunction<M, M, M>) PUT_ALL;
        }

        /**
         * Of remove by key.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        public static <K, V, M extends Map<K, V>> BiFunction<M, K, M> ofRemoveByKey() {
            return (BiFunction<M, K, M>) REMOVE_BY_KEY;
        }

        public static BiFunction<Joiner, Joiner, Joiner> ofMerge() {
            return MERGE;
        }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T> BiFunction<StringBuilder, T, StringBuilder> ofAppend() {
            return (BiFunction<StringBuilder, T, StringBuilder>) APPEND;
        }

        /**
         * Returns a stateful {@code BiPredicate}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param func
         * @return a stateful {@code BiFunction}. Don't save or cache for reuse or use it in parallel stream.
         * @throws IllegalArgumentException
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T, U, R> BiFunction<T, U, R> indexed(final IntBiObjFunction<T, U, ? extends R> func) throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return new BiFunction<>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public R apply(final T t, final U u) {
                    return func.apply(idx.getAndIncrement(), t, u);
                }
            };
        }
    }

    /**
     * The Class TriFunctions.
     */
    public static final class TriFunctions {

        private TriFunctions() {
        }
    }

    /**
     * The Class BinaryOperators.
     */
    public static final class BinaryOperators {

        /** The Constant THROWING_MERGER. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator THROWING_MERGER = (t, u) -> {
            throw new IllegalStateException(String.format("Duplicate key (attempted merging values %s and %s)", t, u));
        };

        /** The Constant IGNORING_MERGER. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator IGNORING_MERGER = (t, u) -> t;

        /** The Constant REPLACING_MERGER. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator REPLACING_MERGER = (t, u) -> u;

        /** The Constant ADD_ALL_TO_FIRST. */
        private static final BinaryOperator<Collection<Object>> ADD_ALL_TO_FIRST = (t, u) -> {
            t.addAll(u);
            return t;
        };

        /** The Constant ADD_ALL_TO_BIGGER. */
        private static final BinaryOperator<Collection<Object>> ADD_ALL_TO_BIGGER = (t, u) -> {
            if (t.size() >= u.size()) {
                t.addAll(u);
                return t;
            } else {
                u.addAll(t);
                return u;
            }
        };

        /** The Constant REMOVE_ALL_FROM_FIRST. */
        private static final BinaryOperator<Collection<Object>> REMOVE_ALL_FROM_FIRST = (t, u) -> {
            t.removeAll(u);
            return t;
        };

        /** The Constant PUT_ALL_TO_FIRST. */
        private static final BinaryOperator<Map<Object, Object>> PUT_ALL_TO_FIRST = (t, u) -> {
            t.putAll(u);
            return t;
        };

        /** The Constant PUT_ALL_TO_BIGGER. */
        private static final BinaryOperator<Map<Object, Object>> PUT_ALL_TO_BIGGER = (t, u) -> {
            if (t.size() >= u.size()) {
                t.putAll(u);
                return t;
            } else {
                u.putAll(t);
                return u;
            }
        };

        /** The Constant MERGE_TO_FIRST. */
        private static final BinaryOperator<Joiner> MERGE_TO_FIRST = Joiner::merge;

        /** The Constant MERGE_TO_BIGGER. */
        private static final BinaryOperator<Joiner> MERGE_TO_BIGGER = (t, u) -> {
            if (t.length() >= u.length()) {
                return t.merge(u);
            } else {
                return u.merge(t);
            }
        };

        /** The Constant APPEND_TO_FIRST. */
        private static final BinaryOperator<StringBuilder> APPEND_TO_FIRST = StringBuilder::append;

        /** The Constant APPEND_TO_BIGGER. */
        private static final BinaryOperator<StringBuilder> APPEND_TO_BIGGER = (t, u) -> {
            if (t.length() >= u.length()) {
                return t.append(u);
            } else {
                return u.append(t);
            }
        };

        /** The Constant CONCAT. */
        private static final BinaryOperator<String> CONCAT = (t, u) -> t + u;

        /** The Constant ADD_INTEGER. */
        private static final BinaryOperator<Integer> ADD_INTEGER = Integer::sum;

        /** The Constant ADD_LONG. */
        private static final BinaryOperator<Long> ADD_LONG = Long::sum;

        /** The Constant ADD_DOUBLE. */
        private static final BinaryOperator<Double> ADD_DOUBLE = Double::sum;

        /** The Constant ADD_BIG_INTEGER. */
        private static final BinaryOperator<BigInteger> ADD_BIG_INTEGER = BigInteger::add;

        /** The Constant ADD_BIG_DECIMAL. */
        private static final BinaryOperator<BigDecimal> ADD_BIG_DECIMAL = BigDecimal::add;

        private BinaryOperators() {
        }

        /**
         * Of add all.
         *
         * @param <T>
         * @param <C>
         * @return
         * @deprecated replaced by {@code #ofAddAllToFirst()}
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAll() {
            return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
        }

        /**
         * Of add all to first.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToFirst() {
            return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
        }

        /**
         * Of add all to bigger.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToBigger() {
            return (BinaryOperator<C>) ADD_ALL_TO_BIGGER;
        }

        /**
         * Of remove all.
         *
         * @param <T>
         * @param <C>
         * @return
         * @deprecated replaced by {@code #ofRemoveAllFromFirst()}.
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAll() {
            return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
        }

        /**
         * Of remove all from first.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAllFromFirst() {
            return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
        }

        /**
         * Of put all.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         * @deprecated replaced by {@code #ofPutAllToFirst()}
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAll() {
            return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
        }

        /**
         * Of put all to first.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToFirst() {
            return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
        }

        /**
         * Of put all to bigger.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToBigger() {
            return (BinaryOperator<M>) PUT_ALL_TO_BIGGER;
        }

        /**
         *
         * @return
         * @deprecated replaced by {@code #ofMergeToFirst}.
         */
        @Deprecated
        public static BinaryOperator<Joiner> ofMerge() {
            return MERGE_TO_FIRST;
        }

        /**
         * Of merge to first.
         *
         * @return
         */
        public static BinaryOperator<Joiner> ofMergeToFirst() {
            return MERGE_TO_FIRST;
        }

        /**
         * Of merge to bigger.
         *
         * @return
         */
        public static BinaryOperator<Joiner> ofMergeToBigger() {
            return MERGE_TO_BIGGER;
        }

        /**
         *
         * @return
         * @deprecated replaced by {@code #ofAppendToFirst()}
         */
        @Deprecated
        public static BinaryOperator<StringBuilder> ofAppend() {
            return APPEND_TO_FIRST;
        }

        /**
         * Of append to first.
         *
         * @return
         */
        public static BinaryOperator<StringBuilder> ofAppendToFirst() {
            return APPEND_TO_FIRST;
        }

        /**
         * Of append to bigger.
         *
         * @return
         */
        public static BinaryOperator<StringBuilder> ofAppendToBigger() {
            return APPEND_TO_BIGGER;
        }

        public static BinaryOperator<String> ofConcat() {
            return CONCAT;
        }

        /**
         * Of add int.
         *
         * @return
         */
        public static BinaryOperator<Integer> ofAddInt() {
            return ADD_INTEGER;
        }

        /**
         * Of add long.
         *
         * @return
         */
        public static BinaryOperator<Long> ofAddLong() {
            return ADD_LONG;
        }

        /**
         * Of add double.
         *
         * @return
         */
        public static BinaryOperator<Double> ofAddDouble() {
            return ADD_DOUBLE;
        }

        /**
         * Of add big integer.
         *
         * @return
         */
        public static BinaryOperator<BigInteger> ofAddBigInteger() {
            return ADD_BIG_INTEGER;
        }

        /**
         * Of add big decimal.
         *
         * @return
         */
        public static BinaryOperator<BigDecimal> ofAddBigDecimal() {
            return ADD_BIG_DECIMAL;
        }
    }

    /**
     * The Class UnaryOperators.
     */
    public static final class UnaryOperators {

        /** The Constant IDENTITY. */
        @SuppressWarnings("rawtypes")
        private static final UnaryOperator IDENTITY = t -> t;

        private UnaryOperators() {
        }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T> UnaryOperator<T> identity() {
            return IDENTITY;
        }
    }

    /**
     * The Class Entries.
     */
    public static final class Entries {

        private Entries() {
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V, T> Function<Map.Entry<K, V>, T> f(final java.util.function.BiFunction<? super K, ? super V, ? extends T> f)
                throws IllegalArgumentException {
            N.checkArgNotNull(f, cs.BiFunction);

            return e -> f.apply(e.getKey(), e.getValue());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V> Predicate<Map.Entry<K, V>> p(final java.util.function.BiPredicate<? super K, ? super V> p) throws IllegalArgumentException {
            N.checkArgNotNull(p, cs.BiPredicate);

            return e -> p.test(e.getKey(), e.getValue());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V> Consumer<Map.Entry<K, V>> c(final java.util.function.BiConsumer<? super K, ? super V> c) throws IllegalArgumentException {
            N.checkArgNotNull(c, cs.BiConsumer);

            return e -> c.accept(e.getKey(), e.getValue());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T>
         * @param <E>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <K, V, T, E extends Exception> Throwables.Function<Map.Entry<K, V>, T, E> ef(
                final Throwables.BiFunction<? super K, ? super V, ? extends T, E> f) throws IllegalArgumentException {
            N.checkArgNotNull(f, cs.BiFunction);

            return e -> f.apply(e.getKey(), e.getValue());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E>
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <K, V, E extends Exception> Throwables.Predicate<Map.Entry<K, V>, E> ep(final Throwables.BiPredicate<? super K, ? super V, E> p)
                throws IllegalArgumentException {
            N.checkArgNotNull(p, cs.BiPredicate);

            return e -> p.test(e.getKey(), e.getValue());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E>
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <K, V, E extends Exception> Throwables.Consumer<Map.Entry<K, V>, E> ec(final Throwables.BiConsumer<? super K, ? super V, E> c)
                throws IllegalArgumentException {
            N.checkArgNotNull(c, cs.BiConsumer);

            return e -> c.accept(e.getKey(), e.getValue());
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V, T> Function<Map.Entry<K, V>, T> ff(final Throwables.BiFunction<? super K, ? super V, ? extends T, ? extends Exception> f)
                throws IllegalArgumentException {
            N.checkArgNotNull(f, cs.BiFunction);

            return e -> {
                try {
                    return f.apply(e.getKey(), e.getValue());
                } catch (final Exception ex) {
                    throw ExceptionUtil.toRuntimeException(ex, true);
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V> Predicate<Map.Entry<K, V>> pp(final Throwables.BiPredicate<? super K, ? super V, ? extends Exception> p)
                throws IllegalArgumentException {
            N.checkArgNotNull(p, cs.BiPredicate);

            return e -> {
                try {
                    return p.test(e.getKey(), e.getValue());
                } catch (final Exception ex) {
                    throw ExceptionUtil.toRuntimeException(ex, true);
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V> Consumer<Map.Entry<K, V>> cc(final Throwables.BiConsumer<? super K, ? super V, ? extends Exception> c)
                throws IllegalArgumentException {
            N.checkArgNotNull(c, cs.BiConsumer);

            return e -> {
                try {
                    c.accept(e.getKey(), e.getValue());
                } catch (final Exception ex) {
                    throw ExceptionUtil.toRuntimeException(ex, true);
                }
            };
        }
    }

    /**
     * The Class Pairs.
     */
    public static final class Pairs {

        /** The Constant PAIR_TO_LIST. */
        @SuppressWarnings("rawtypes")
        private static final Function<Pair, List> PAIR_TO_LIST = t -> N.asList(t.left, t.right);

        /** The Constant PAIR_TO_SET. */
        @SuppressWarnings("rawtypes")
        private static final Function<Pair, Set> PAIR_TO_SET = t -> N.asSet(t.left, t.right);

        private Pairs() {
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Pair<T, T>, List<T>> toList() {
            return (Function) PAIR_TO_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Pair<T, T>, Set<T>> toSet() {
            return (Function) PAIR_TO_SET;
        }

    }

    /**
     * The Class Triples.
     */
    public static final class Triples {

        /** The Constant TRIPLE_TO_LIST. */
        @SuppressWarnings("rawtypes")
        private static final Function<Triple, List> TRIPLE_TO_LIST = t -> N.asList(t.left, t.middle, t.right);

        /** The Constant TRIPLE_TO_SET. */
        @SuppressWarnings("rawtypes")
        private static final Function<Triple, Set> TRIPLE_TO_SET = t -> N.asSet(t.left, t.middle, t.right);

        private Triples() {
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Triple<T, T, T>, List<T>> toList() {
            return (Function) TRIPLE_TO_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Triple<T, T, T>, Set<T>> toSet() {
            return (Function) TRIPLE_TO_SET;
        }
    }

    /**
     * The Class Disposables.
     */
    public static final class Disposables {

        /** The Constant CLONE. */
        @SuppressWarnings("rawtypes")
        private static final Function<DisposableArray, Object[]> CLONE = DisposableArray::copy;

        /** The Constant TO_STRING. */
        @SuppressWarnings("rawtypes")
        private static final Function<DisposableArray, String> TO_STRING = DisposableArray::toString;

        private Disposables() {
        }

        /**
         *
         * @param <T>
         * @param <A>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, A extends DisposableArray<T>> Function<A, T[]> cloneArray() {
            return (Function) CLONE;
        }

        /**
         *
         * @param <A>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <A extends DisposableArray> Function<A, String> toStr() {
            return (Function) TO_STRING;
        }

        /**
         *
         * @param <A>
         * @param delimiter
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <A extends DisposableArray> Function<A, String> join(final String delimiter) {
            return t -> t.join(delimiter);
        }
    }

    /**
     * Utility class for {@code CharPredicate/Function/Consumer}.
     *
     *
     */
    public static final class FC {

        /** The Constant IS_ZERO. */
        private static final CharPredicate IS_ZERO = t -> t == 0;

        /** The Constant IS_WHITE_SPACE. */
        private static final CharPredicate IS_WHITESPACE = Character::isWhitespace;

        /** The Constant EQUAL. */
        private static final CharBiPredicate EQUAL = (t, u) -> t == u;

        /** The Constant NOT_EQUAL. */
        private static final CharBiPredicate NOT_EQUAL = (t, u) -> t != u;

        /** The Constant GREATER_THAN. */
        private static final CharBiPredicate GREATER_THAN = (t, u) -> t > u;

        /** The Constant GREATER_EQUAL. */
        private static final CharBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

        /** The Constant LESS_THAN. */
        private static final CharBiPredicate LESS_THAN = (t, u) -> t < u;

        /** The Constant LESS_EQUAL. */
        private static final CharBiPredicate LESS_EQUAL = (t, u) -> t <= u;

        /** The Constant LEN. */
        private static final Function<char[], Integer> LEN = t -> t == null ? 0 : t.length;

        private FC() {
        }

        public static CharPredicate isZero() {
            return IS_ZERO;
        }

        public static CharPredicate isWhitespace() {
            return IS_WHITESPACE;
        }

        public static CharBiPredicate equal() {
            return EQUAL;
        }

        public static CharBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        public static CharBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        public static CharBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        public static CharBiPredicate lessThan() {
            return LESS_THAN;
        }

        public static CharBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        @SuppressWarnings("SameReturnValue")
        public static ToCharFunction<Character> unbox() {
            return ToCharFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static CharPredicate p(final CharPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> CharFunction<R> f(final CharFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static CharConsumer c(final CharConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        public static Function<char[], Integer> len() {
            return LEN;
        }

        /**
         * Returns a stateful {@code CharBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return a stateful {@code CharBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static CharBiFunction<MergeResult> alternate() {
            return new CharBiFunction<>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(final char t, final char u) {
                    return flag.getAndNegate() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class CharBinaryOperators {
            private CharBinaryOperators() {
                // Singleton for utility class.
            }

            public static final CharBinaryOperator MIN = (left, right) -> left <= right ? left : right;

            public static final CharBinaryOperator MAX = (left, right) -> left >= right ? left : right;
        }
    }

    /**
     * Utility class for {@code BytePredicate/Function/Consumer}.
     *
     *
     */
    public static final class FB {

        /** The Constant POSITIVE. */
        private static final BytePredicate POSITIVE = t -> t > 0;

        /** The Constant NOT_NEGATIVE. */
        private static final BytePredicate NOT_NEGATIVE = t -> t >= 0;

        /** The Constant EQUAL. */
        private static final ByteBiPredicate EQUAL = (t, u) -> t == u;

        /** The Constant NOT_EQUAL. */
        private static final ByteBiPredicate NOT_EQUAL = (t, u) -> t != u;

        /** The Constant GREATER_THAN. */
        private static final ByteBiPredicate GREATER_THAN = (t, u) -> t > u;

        /** The Constant GREATER_EQUAL. */
        private static final ByteBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

        /** The Constant LESS_THAN. */
        private static final ByteBiPredicate LESS_THAN = (t, u) -> t < u;

        /** The Constant LESS_EQUAL. */
        private static final ByteBiPredicate LESS_EQUAL = (t, u) -> t <= u;

        /** The Constant LEN. */
        private static final Function<byte[], Integer> LEN = t -> t == null ? 0 : t.length;

        private FB() {
        }

        public static BytePredicate positive() {
            return POSITIVE;
        }

        public static BytePredicate notNegative() {
            return NOT_NEGATIVE;
        }

        public static ByteBiPredicate equal() {
            return EQUAL;
        }

        public static ByteBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        public static ByteBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        public static ByteBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        public static ByteBiPredicate lessThan() {
            return LESS_THAN;
        }

        public static ByteBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        @SuppressWarnings("SameReturnValue")
        public static ToByteFunction<Byte> unbox() {
            return ToByteFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static BytePredicate p(final BytePredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> ByteFunction<R> f(final ByteFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static ByteConsumer c(final ByteConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        public static Function<byte[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<byte[], Integer> SUM = N::sum;

        public static Function<byte[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<byte[], Double> AVERAGE = N::average;

        public static Function<byte[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful {@code ByteBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return a stateful {@code ByteBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static ByteBiFunction<MergeResult> alternate() {
            return new ByteBiFunction<>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(final byte t, final byte u) {
                    return flag.getAndNegate() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class ByteBinaryOperators {
            private ByteBinaryOperators() {
                // Singleton for utility class.
            }

            public static final ByteBinaryOperator MIN = (left, right) -> left <= right ? left : right;

            public static final ByteBinaryOperator MAX = (left, right) -> left >= right ? left : right;
        }
    }

    /**
     * Utility class for {@code ShortPredicate/Function/Consumer}.
     *
     *
     */
    public static final class FS {

        /** The Constant POSITIVE. */
        private static final ShortPredicate POSITIVE = t -> t > 0;

        /** The Constant NOT_NEGATIVE. */
        private static final ShortPredicate NOT_NEGATIVE = t -> t >= 0;

        /** The Constant EQUAL. */
        private static final ShortBiPredicate EQUAL = (t, u) -> t == u;

        /** The Constant NOT_EQUAL. */
        private static final ShortBiPredicate NOT_EQUAL = (t, u) -> t != u;

        /** The Constant GREATER_THAN. */
        private static final ShortBiPredicate GREATER_THAN = (t, u) -> t > u;

        /** The Constant GREATER_EQUAL. */
        private static final ShortBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

        /** The Constant LESS_THAN. */
        private static final ShortBiPredicate LESS_THAN = (t, u) -> t < u;

        /** The Constant LESS_EQUAL. */
        private static final ShortBiPredicate LESS_EQUAL = (t, u) -> t <= u;

        /** The Constant LEN. */
        private static final Function<short[], Integer> LEN = t -> t == null ? 0 : t.length;

        private FS() {
        }

        public static ShortPredicate positive() {
            return POSITIVE;
        }

        public static ShortPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        public static ShortBiPredicate equal() {
            return EQUAL;
        }

        public static ShortBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        public static ShortBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        public static ShortBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        public static ShortBiPredicate lessThan() {
            return LESS_THAN;
        }

        public static ShortBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        @SuppressWarnings("SameReturnValue")
        public static ToShortFunction<Short> unbox() {
            return ToShortFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static ShortPredicate p(final ShortPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> ShortFunction<R> f(final ShortFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static ShortConsumer c(final ShortConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        public static Function<short[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<short[], Integer> SUM = N::sum;

        public static Function<short[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<short[], Double> AVERAGE = N::average;

        public static Function<short[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful {@code ShortBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return a stateful {@code ShortBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static ShortBiFunction<MergeResult> alternate() {
            return new ShortBiFunction<>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(final short t, final short u) {
                    return flag.getAndNegate() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class ShortBinaryOperators {
            private ShortBinaryOperators() {
                // Singleton for utility class.
            }

            public static final ShortBinaryOperator MIN = (left, right) -> left <= right ? left : right;

            public static final ShortBinaryOperator MAX = (left, right) -> left >= right ? left : right;
        }
    }

    /**
     * Utility class for {@code IntPredicate/Function/Consumer}.
     *
     *
     */
    public static final class FI {

        /** The Constant POSITIVE. */
        private static final IntPredicate POSITIVE = t -> t > 0;

        /** The Constant NOT_NEGATIVE. */
        private static final IntPredicate NOT_NEGATIVE = t -> t >= 0;

        /** The Constant EQUAL. */
        private static final IntBiPredicate EQUAL = (t, u) -> t == u;

        /** The Constant NOT_EQUAL. */
        private static final IntBiPredicate NOT_EQUAL = (t, u) -> t != u;

        /** The Constant GREATER_THAN. */
        private static final IntBiPredicate GREATER_THAN = (t, u) -> t > u;

        /** The Constant GREATER_EQUAL. */
        private static final IntBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

        /** The Constant LESS_THAN. */
        private static final IntBiPredicate LESS_THAN = (t, u) -> t < u;

        /** The Constant LESS_EQUAL. */
        private static final IntBiPredicate LESS_EQUAL = (t, u) -> t <= u;

        /** The Constant LEN. */
        private static final Function<int[], Integer> LEN = t -> t == null ? 0 : t.length;

        private FI() {
        }

        public static IntPredicate positive() {
            return POSITIVE;
        }

        public static IntPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        public static IntBiPredicate equal() {
            return EQUAL;
        }

        public static IntBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        public static IntBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        public static IntBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        public static IntBiPredicate lessThan() {
            return LESS_THAN;
        }

        public static IntBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        @SuppressWarnings("SameReturnValue")
        public static ToIntFunction<Integer> unbox() {
            return ToIntFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static IntPredicate p(final IntPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> IntFunction<R> f(final IntFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static IntConsumer c(final IntConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        public static Function<int[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<int[], Integer> SUM = N::sum;

        public static Function<int[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<int[], Double> AVERAGE = N::average;

        public static Function<int[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful {@code IntBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return a stateful {@code IntBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static IntBiFunction<MergeResult> alternate() {
            return new IntBiFunction<>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(final int t, final int u) {
                    return flag.getAndNegate() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class IntBinaryOperators {
            private IntBinaryOperators() {
                // Singleton for utility class.
            }

            public static final IntBinaryOperator MIN = Math::min;

            public static final IntBinaryOperator MAX = Math::max;
        }
    }

    /**
     * Utility class for {@code LongPredicate/Function/Consumer}.
     *
     *
     */
    public static final class FL {

        /** The Constant POSITIVE. */
        private static final LongPredicate POSITIVE = t -> t > 0;

        /** The Constant NOT_NEGATIVE. */
        private static final LongPredicate NOT_NEGATIVE = t -> t >= 0;

        /** The Constant EQUAL. */
        private static final LongBiPredicate EQUAL = (t, u) -> t == u;

        /** The Constant NOT_EQUAL. */
        private static final LongBiPredicate NOT_EQUAL = (t, u) -> t != u;

        /** The Constant GREATER_THAN. */
        private static final LongBiPredicate GREATER_THAN = (t, u) -> t > u;

        /** The Constant GREATER_EQUAL. */
        private static final LongBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

        /** The Constant LESS_THAN. */
        private static final LongBiPredicate LESS_THAN = (t, u) -> t < u;

        /** The Constant LESS_EQUAL. */
        private static final LongBiPredicate LESS_EQUAL = (t, u) -> t <= u;

        /** The Constant LEN. */
        private static final Function<long[], Integer> LEN = t -> t == null ? 0 : t.length;

        private FL() {
        }

        public static LongPredicate positive() {
            return POSITIVE;
        }

        public static LongPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        public static LongBiPredicate equal() {
            return EQUAL;
        }

        public static LongBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        public static LongBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        public static LongBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        public static LongBiPredicate lessThan() {
            return LESS_THAN;
        }

        public static LongBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        @SuppressWarnings("SameReturnValue")
        public static ToLongFunction<Long> unbox() {
            return ToLongFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static LongPredicate p(final LongPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> LongFunction<R> f(final LongFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static LongConsumer c(final LongConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        public static Function<long[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<long[], Long> SUM = N::sum;

        public static Function<long[], Long> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<long[], Double> AVERAGE = N::average;

        public static Function<long[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful {@code LongBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return a stateful {@code LongBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static LongBiFunction<MergeResult> alternate() {
            return new LongBiFunction<>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(final long t, final long u) {
                    return flag.getAndNegate() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class LongBinaryOperators {
            private LongBinaryOperators() {
                // Singleton for utility class.
            }

            public static final LongBinaryOperator MIN = Math::min;

            public static final LongBinaryOperator MAX = Math::max;
        }
    }

    /**
     * Utility class for {@code FloatPredicate/Function/Consumer}.
     *
     *
     */
    public static final class FF {

        /** The Constant POSITIVE. */
        private static final FloatPredicate POSITIVE = t -> t > 0;

        /** The Constant NOT_NEGATIVE. */
        private static final FloatPredicate NOT_NEGATIVE = t -> t >= 0;

        /** The Constant EQUAL. */
        private static final FloatBiPredicate EQUAL = N::equals;

        /** The Constant NOT_EQUAL. */
        private static final FloatBiPredicate NOT_EQUAL = (t, u) -> N.compare(t, u) != 0;

        /** The Constant GREATER_THAN. */
        private static final FloatBiPredicate GREATER_THAN = (t, u) -> N.compare(t, u) > 0;

        /** The Constant GREATER_EQUAL. */
        private static final FloatBiPredicate GREATER_EQUAL = (t, u) -> N.compare(t, u) >= 0;

        /** The Constant LESS_THAN. */
        private static final FloatBiPredicate LESS_THAN = (t, u) -> N.compare(t, u) < 0;

        /** The Constant LESS_EQUAL. */
        private static final FloatBiPredicate LESS_EQUAL = (t, u) -> N.compare(t, u) <= 0;

        /** The Constant LEN. */
        private static final Function<float[], Integer> LEN = t -> t == null ? 0 : t.length;

        private FF() {
        }

        public static FloatPredicate positive() {
            return POSITIVE;
        }

        public static FloatPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        public static FloatBiPredicate equal() {
            return EQUAL;
        }

        public static FloatBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        public static FloatBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        public static FloatBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        public static FloatBiPredicate lessThan() {
            return LESS_THAN;
        }

        public static FloatBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        @SuppressWarnings("SameReturnValue")
        public static ToFloatFunction<Float> unbox() {
            return ToFloatFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static FloatPredicate p(final FloatPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> FloatFunction<R> f(final FloatFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static FloatConsumer c(final FloatConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        public static Function<float[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<float[], Float> SUM = N::sum;

        public static Function<float[], Float> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<float[], Double> AVERAGE = N::average;

        public static Function<float[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful {@code FloatBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return a stateful {@code FloatBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static FloatBiFunction<MergeResult> alternate() {
            return new FloatBiFunction<>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(final float t, final float u) {
                    return flag.getAndNegate() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class FloatBinaryOperators {
            private FloatBinaryOperators() {
                // Singleton for utility class.
            }

            public static final FloatBinaryOperator MIN = Math::min;

            public static final FloatBinaryOperator MAX = Math::max;
        }
    }

    /**
     * Utility class for {@code DoublePredicate/Function/Consumer}.
     *
     *
     */
    public static final class FD {

        /** The Constant POSITIVE. */
        private static final DoublePredicate POSITIVE = t -> t > 0;

        /** The Constant NOT_NEGATIVE. */
        private static final DoublePredicate NOT_NEGATIVE = t -> t >= 0;

        /** The Constant EQUAL. */
        private static final DoubleBiPredicate EQUAL = N::equals;

        /** The Constant NOT_EQUAL. */
        private static final DoubleBiPredicate NOT_EQUAL = (t, u) -> N.compare(t, u) != 0;

        /** The Constant GREATER_THAN. */
        private static final DoubleBiPredicate GREATER_THAN = (t, u) -> N.compare(t, u) > 0;

        /** The Constant GREATER_EQUAL. */
        private static final DoubleBiPredicate GREATER_EQUAL = (t, u) -> N.compare(t, u) >= 0;

        /** The Constant LESS_THAN. */
        private static final DoubleBiPredicate LESS_THAN = (t, u) -> N.compare(t, u) < 0;

        /** The Constant LESS_EQUAL. */
        private static final DoubleBiPredicate LESS_EQUAL = (t, u) -> N.compare(t, u) <= 0;

        /** The Constant LEN. */
        private static final Function<double[], Integer> LEN = t -> t == null ? 0 : t.length;

        private FD() {
        }

        public static DoublePredicate positive() {
            return POSITIVE;
        }

        public static DoublePredicate notNegative() {
            return NOT_NEGATIVE;
        }

        public static DoubleBiPredicate equal() {
            return EQUAL;
        }

        public static DoubleBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        public static DoubleBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        public static DoubleBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        public static DoubleBiPredicate lessThan() {
            return LESS_THAN;
        }

        public static DoubleBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        @SuppressWarnings("SameReturnValue")
        public static ToDoubleFunction<Double> unbox() {
            return ToDoubleFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         * @throws IllegalArgumentException
         */
        public static DoublePredicate p(final DoublePredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> DoubleFunction<R> f(final DoubleFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         * @throws IllegalArgumentException
         */
        public static DoubleConsumer c(final DoubleConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        public static Function<double[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<double[], Double> SUM = N::sum;

        public static Function<double[], Double> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<double[], Double> AVERAGE = N::average;

        public static Function<double[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful {@code DoubleBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return a stateful {@code DoubleBiFunction}. Don't save or cache for reuse or use it in parallel stream.
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static DoubleBiFunction<MergeResult> alternate() {
            return new DoubleBiFunction<>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(final double t, final double u) {
                    return flag.getAndNegate() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class DoubleBinaryOperators {
            private DoubleBinaryOperators() {
                // Singleton for utility class.
            }

            public static final DoubleBinaryOperator MIN = Math::min;

            public static final DoubleBinaryOperator MAX = Math::max;
        }
    }

    /**
     * Utility class for exceptional {@code Predicate/Function/Consumer}.
     *
     *
     */
    public static final class Fnn {
        private Fnn() {
            // Singleton for utility class
        }

        /**
         * Returns a {@code Supplier} which returns a single instance created by calling the specified {@code supplier.get()}.
         *
         * @param <T>
         * @param <E>
         * @param supplier
         * @return
         */
        public static <T, E extends Throwable> Throwables.Supplier<T, E> memoize(final Throwables.Supplier<T, E> supplier) {
            return Throwables.LazyInitializer.of(supplier);
        }

        // Copied from Google Guava under Apache License v2.

        /**
         * Copied from Google Guava under Apache License v2.
         * <br />
         * <br />
         *
         * Returns a supplier that caches the instance supplied by the delegate and removes the cached
         * value after the specified time has passed. Subsequent calls to {@code get()} return the cached
         * value if the expiration time has not passed. After the expiration time, a new value is
         * retrieved, cached, and returned. See: <a
         * href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
         *
         * <p>The returned supplier is thread-safe. The supplier's serialized form does not contain the
         * cached value, which will be recalculated when {@code get()} is called on the reserialized
         * instance. The actual memoization does not happen when the underlying delegate throws an
         * exception.
         *
         * <p>When the underlying delegate throws an exception, then this memorizing supplier will keep
         * delegating calls until it returns valid data.
         *
         * @param <T>
         * @param <E>
         * @param supplier
         * @param duration the length of time after a value is created that it should stop being returned
         *     by subsequent {@code get()} calls
         * @param unit the unit that {@code duration} is expressed in
         * @return
         * @throws IllegalArgumentException if {@code duration} is not positive
         */
        public static <T, E extends Throwable> Throwables.Supplier<T, E> memoizeWithExpiration(final Throwables.Supplier<T, E> supplier, final long duration,
                final TimeUnit unit) throws IllegalArgumentException {
            N.checkArgNotNull(supplier, cs.Supplier);
            N.checkArgument(duration > 0, "duration (%s %s) must be > 0", duration, unit);

            return new Throwables.Supplier<>() {
                private final Throwables.Supplier<T, E> delegate = supplier;
                private final long durationNanos = unit.toNanos(duration);
                private volatile T value;
                // The special value 0 means "not yet initialized".
                private volatile long expirationNanos = 0;

                @Override
                public T get() throws E {
                    // Another variant of Double-Checked Locking.
                    //
                    // We use two volatile reads. We could reduce this to one by
                    // putting our fields into a holder class, but (at least on x86)
                    // the extra memory consumption and indirection are more
                    // expensive than the extra volatile reads.
                    long nanos = expirationNanos;
                    final long now = System.nanoTime();
                    if (nanos == 0 || now - nanos >= 0) {
                        synchronized (this) {
                            if (nanos == expirationNanos) { // recheck for lost race
                                final T t = delegate.get();
                                value = t;
                                nanos = now + durationNanos;
                                // In the very unlikely event that nanos is 0, set it to 1;
                                // no one will notice 1 ns of tardiness.
                                expirationNanos = (nanos == 0) ? 1 : nanos;
                                return t;
                            }
                        }
                    }

                    // This is safe because we checked `expirationNanos`.
                    return value;
                }
            };
        }

        /**
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         */
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> memoize(final Throwables.Function<? super T, ? extends R, E> func) {
            return new Throwables.Function<>() {
                private final R none = (R) NONE;
                private final Map<T, R> resultMap = new ConcurrentHashMap<>();
                private volatile R resultForNull = none; //NOSONAR

                @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
                @Override
                public R apply(final T t) throws E {
                    R result = null;

                    if (t == null) {
                        result = resultForNull;

                        if (result == none) {
                            synchronized (this) {
                                if (resultForNull == none) {
                                    resultForNull = func.apply(null);
                                }

                                result = resultForNull;
                            }
                        }
                    } else {
                        result = resultMap.get(t);

                        if (result == null) {
                            result = func.apply(t);
                            resultMap.put(t, result == null ? none : result);
                        }
                    }

                    return result == none ? null : result;
                }
            };
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.Function<T, T, E> identity() {
            return Fn.IDENTITY;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.Predicate<T, E> alwaysTrue() {
            return Fn.ALWAYS_TRUE;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.Predicate<T, E> alwaysFalse() {
            return Fn.ALWAYS_FALSE;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.Function<T, String, E> toStr() {
            return TO_STRING;
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Map.Entry<K, V>, K, E> key() {
            return (Throwables.Function) Fn.KEY;
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Map.Entry<K, V>, V, E> value() {
            return (Throwables.Function) Fn.VALUE;
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Entry<K, V>, Entry<V, K>, E> inverse() {
            return (Throwables.Function) Fn.INVERSE;
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.BiFunction<K, V, Map.Entry<K, V>, E> entry() {
            return (Throwables.BiFunction) Fn.ENTRY;
        }

        /**
         *
         * @param <L>
         * @param <R>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <L, R, E extends Exception> Throwables.BiFunction<L, R, Pair<L, R>, E> pair() {
            return (Throwables.BiFunction) Fn.PAIR;
        }

        /**
         *
         * @param <L>
         * @param <M>
         * @param <R>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <L, M, R, E extends Exception> Throwables.TriFunction<L, M, R, Triple<L, M, R>, E> triple() {
            return (Throwables.TriFunction) Fn.TRIPLE;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Function<T, Tuple1<T>, E> tuple1() {
            return (Throwables.Function) Fn.TUPLE_1;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, U, E extends Exception> Throwables.BiFunction<T, U, Tuple2<T, U>, E> tuple2() {
            return (Throwables.BiFunction) Fn.TUPLE_2;
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <A, B, C, E extends Exception> Throwables.TriFunction<A, B, C, Tuple3<A, B, C>, E> tuple3() {
            return (Throwables.TriFunction) Fn.TUPLE_3;
        }

        /**
         *
         * @param <E>
         * @return
         */
        public static <E extends Exception> Throwables.Runnable<E> emptyAction() {
            return (Throwables.Runnable<E>) Fn.EMPTY_ACTION;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> doNothing() {
            return Fn.EMPTY_CONSUMER;
        }

        /**
         *
         * @param <T>
         * @param errorMessage
         * @return
         */
        public static <T> Throwables.Consumer<T, RuntimeException> throwRuntimeException(final String errorMessage) {
            return t -> {
                throw new RuntimeException(errorMessage);
            };
        }

        /**
         *
         * @param <T>
         * @param errorMessage
         * @return
         */
        public static <T> Throwables.Consumer<T, IOException> throwIOException(final String errorMessage) {
            return t -> {
                throw new IOException(errorMessage);
            };
        }

        /**
         *
         * @param <T>
         * @param errorMessage
         * @return
         */
        public static <T> Throwables.Consumer<T, Exception> throwException(final String errorMessage) {
            return t -> {
                throw new Exception(errorMessage);
            };
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param exceptionSupplier
         * @return
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> throwException(final java.util.function.Supplier<? extends E> exceptionSupplier) {
            return t -> {
                throw exceptionSupplier.get();
            };
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param millis
         * @return
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> sleep(final long millis) {
            return t -> N.sleep(millis);
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param millis
         * @return
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> sleepUninterruptibly(final long millis) {
            return t -> N.sleepUninterruptibly(millis);
        }

        /**
         * Returns a stateful {@code Consumer}. Don't save or cache for reuse, but it can be used in parallel stream.
         *
         * @param <T>
         * @param <E>
         * @param permitsPerSecond
         * @return a stateful {@code Throwables.Consumer}. Don't save or cache for reuse, but it can be used in parallel stream.
         * @see RateLimiter#acquire()
         * @see RateLimiter#create(double)
         */
        @Stateful
        public static <T, E extends Exception> Throwables.Consumer<T, E> rateLimiter(final double permitsPerSecond) {
            return rateLimiter(RateLimiter.create(permitsPerSecond));
        }

        /**
         * Returns a stateful {@code Consumer}. Don't save or cache for reuse, but it can be used in parallel stream.
         *
         * @param <T>
         * @param <E>
         * @param rateLimiter
         * @return a stateful {@code Throwables.Consumer}. Don't save or cache for reuse, but it can be used in parallel stream.
         * @see RateLimiter#acquire()
         */
        @Stateful
        public static <T, E extends Exception> Throwables.Consumer<T, E> rateLimiter(final RateLimiter rateLimiter) {
            return t -> rateLimiter.acquire();
        }

        private static final Throwables.Consumer<AutoCloseable, Exception> CLOSE = closeable -> {
            if (closeable != null) {
                closeable.close();
            }
        };

        //    public static <E extends Exception> Throwables.Runnable<E> close(final AutoCloseable closeable) {
        //        return new Throwables.Runnable<E>() {
        //            private volatile boolean isClosed = false;
        //
        //            @Override
        //            public void run() throws E {
        //                if (isClosed) {
        //                    return;
        //                }
        //
        //                isClosed = true;
        //                IOUtil.close(closeable);
        //            }
        //        };
        //    }
        //
        //    public static <E extends Exception> Throwables.Runnable<E> closeQuietly(final AutoCloseable closeable) {
        //        return new Throwables.Runnable<E>() {
        //            private volatile boolean isClosed = false;
        //
        //            @Override
        //            public void run() {
        //                if (isClosed) {
        //                    return;
        //                }
        //
        //                isClosed = true;
        //                IOUtil.closeQuietly(closeable);
        //            }
        //        };
        //    }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T extends AutoCloseable> Throwables.Consumer<T, Exception> close() {
            return (Throwables.Consumer<T, Exception>) CLOSE;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T extends AutoCloseable, E extends Exception> Throwables.Consumer<T, E> closeQuietly() {
            return (Throwables.Consumer<T, E>) Fn.CLOSE_QUIETLY;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> println() {
            return Fn.PRINTLN;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param separator
         * @return
         */
        public static <T, U, E extends Exception> Throwables.BiConsumer<T, U, E> println(final String separator) {
            return cc(Fn.println(separator));
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @Beta
        public static <T, E extends Exception> Throwables.Predicate<T, E> isNull() {
            return Fn.IS_NULL;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> isEmpty() {
            return (Throwables.Predicate<T, E>) Fn.IS_EMPTY;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> isBlank() {
            return (Throwables.Predicate<T, E>) Fn.IS_BLANK;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Predicate<T[], E> isEmptyA() {
            return (Throwables.Predicate) Fn.IS_EMPTY_A;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Collection, E extends Exception> Throwables.Predicate<T, E> isEmptyC() {
            return (Throwables.Predicate<T, E>) Fn.IS_EMPTY_C;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Map, E extends Exception> Throwables.Predicate<T, E> isEmptyM() {
            return (Throwables.Predicate<T, E>) Fn.IS_EMPTY_M;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @Beta
        public static <T, E extends Exception> Throwables.Predicate<T, E> notNull() {
            return Fn.NOT_NULL;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> notEmpty() {
            return (Throwables.Predicate<T, E>) Fn.IS_NOT_EMPTY;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> notBlank() {
            return (Throwables.Predicate<T, E>) Fn.IS_NOT_BLANK;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Predicate<T[], E> notEmptyA() {
            return (Throwables.Predicate) Fn.NOT_EMPTY_A;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Collection, E extends Exception> Throwables.Predicate<T, E> notEmptyC() {
            return (Throwables.Predicate<T, E>) Fn.NOT_EMPTY_C;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Map, E extends Exception> Throwables.Predicate<T, E> notEmptyM() {
            return (Throwables.Predicate<T, E>) Fn.NOT_EMPTY_M;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> throwingMerger() {
            return BinaryOperators.THROWING_MERGER;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> ignoringMerger() {
            return BinaryOperators.IGNORING_MERGER;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> replacingMerger() {
            return BinaryOperators.REPLACING_MERGER;
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V, E extends Throwable> Throwables.Predicate<Map.Entry<K, V>, E> testByKey(final Throwables.Predicate<? super K, E> predicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return entry -> predicate.test(entry.getKey());
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V, E extends Throwable> Throwables.Predicate<Map.Entry<K, V>, E> testByValue(final Throwables.Predicate<? super V, E> predicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return entry -> predicate.test(entry.getValue());
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @param consumer
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V, E extends Throwable> Throwables.Consumer<Map.Entry<K, V>, E> acceptByKey(final Throwables.Consumer<? super K, E> consumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return entry -> consumer.accept(entry.getKey());
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @param consumer
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V, E extends Throwable> Throwables.Consumer<Map.Entry<K, V>, E> acceptByValue(final Throwables.Consumer<? super V, E> consumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return entry -> consumer.accept(entry.getValue());
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V, R, E extends Throwable> Throwables.Function<Map.Entry<K, V>, R, E> applyByKey(
                final Throwables.Function<? super K, ? extends R, E> func) throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return entry -> func.apply(entry.getKey());
        }

        /**
         *
         * @param <K>
         * @param <V>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws IllegalArgumentException
         */
        public static <K, V, R, E extends Throwable> Throwables.Function<Map.Entry<K, V>, R, E> applyByValue(
                final Throwables.Function<? super V, ? extends R, E> func) throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return entry -> func.apply(entry.getValue());
        }

        /** The Constant RETURN_FIRST. */
        private static final Throwables.BinaryOperator<Object, Throwable> RETURN_FIRST = (t, u) -> t;

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> selectFirst() {
            return (Throwables.BinaryOperator) RETURN_FIRST;
        }

        /** The Constant RETURN_SECOND. */
        private static final Throwables.BinaryOperator<Object, Throwable> RETURN_SECOND = (t, u) -> u;

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> selectSecond() {
            return (Throwables.BinaryOperator) RETURN_SECOND;
        }

        /** The Constant MIN. */
        @SuppressWarnings({ "rawtypes" })
        private static final Throwables.BinaryOperator<Comparable, Throwable> MIN = (t, u) -> N.compare(t, u) <= 0 ? t : u;

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>, E extends Throwable> Throwables.BinaryOperator<T, E> min() {
            return (Throwables.BinaryOperator) MIN;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param comparator
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> min(final Comparator<? super T> comparator) throws IllegalArgumentException {
            N.checkArgNotNull(comparator);

            return (t, u) -> comparator.compare(t, u) <= 0 ? t : u;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param keyExtractor
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> minBy(
                final java.util.function.Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
            N.checkArgNotNull(keyExtractor);

            return (t, u) -> N.compare(keyExtractor.apply(t), keyExtractor.apply(u)) <= 0 ? t : u;
        }

        /** The Constant MIN_BY_KEY. */
        @SuppressWarnings("rawtypes")
        private static final Throwables.BinaryOperator<Map.Entry<Comparable, Object>, Throwable> MIN_BY_KEY = (t,
                u) -> N.compare(t.getKey(), u.getKey()) <= 0 ? t : u;

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K extends Comparable<? super K>, V, E extends Throwable> Throwables.BinaryOperator<Map.Entry<K, V>, E> minByKey() {
            return (Throwables.BinaryOperator) MIN_BY_KEY;
        }

        /** The Constant MIN_BY_VALUE. */
        @SuppressWarnings("rawtypes")
        private static final Throwables.BinaryOperator<Map.Entry<Object, Comparable>, Throwable> MIN_BY_VALUE = (t,
                u) -> N.compare(t.getValue(), u.getValue()) <= 0 ? t : u;

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K, V extends Comparable<? super V>, E extends Throwable> Throwables.BinaryOperator<Map.Entry<K, V>, E> minByValue() {
            return (Throwables.BinaryOperator) MIN_BY_VALUE;
        }

        /** The Constant MAX. */
        @SuppressWarnings("rawtypes")
        private static final Throwables.BinaryOperator<Comparable, Throwable> MAX = (t, u) -> N.compare(t, u) >= 0 ? t : u;

        /**
         *
         * @param <T>
         * @param <E>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>, E extends Throwable> Throwables.BinaryOperator<T, E> max() {
            return (Throwables.BinaryOperator) MAX;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param comparator
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> max(final Comparator<? super T> comparator) throws IllegalArgumentException {
            N.checkArgNotNull(comparator);

            return (t, u) -> comparator.compare(t, u) >= 0 ? t : u;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param keyExtractor
         * @return
         * @throws IllegalArgumentException
         */
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> maxBy(
                final java.util.function.Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
            N.checkArgNotNull(keyExtractor);

            return (t, u) -> N.compare(keyExtractor.apply(t), keyExtractor.apply(u)) >= 0 ? t : u;
        }

        /** The Constant MAX_BY_KEY. */
        @SuppressWarnings("rawtypes")
        private static final Throwables.BinaryOperator<Map.Entry<Comparable, Object>, Throwable> MAX_BY_KEY = (t,
                u) -> N.compare(t.getKey(), u.getKey()) >= 0 ? t : u;

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K extends Comparable<? super K>, V, E extends Throwable> Throwables.BinaryOperator<Map.Entry<K, V>, E> maxByKey() {
            return (Throwables.BinaryOperator) MAX_BY_KEY;
        }

        /** The Constant MAX_BY_VALUE. */
        @SuppressWarnings("rawtypes")
        private static final Throwables.BinaryOperator<Map.Entry<Object, Comparable>, Throwable> MAX_BY_VALUE = (t,
                u) -> N.compare(t.getValue(), u.getValue()) >= 0 ? t : u;

        /**
         *
         * @param <K>
         * @param <V>
         * @param <E>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K, V extends Comparable<? super V>, E extends Throwable> Throwables.BinaryOperator<Map.Entry<K, V>, E> maxByValue() {
            return (Throwables.BinaryOperator) MAX_BY_VALUE;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, E extends Throwable> Throwables.Predicate<T, E> not(final Throwables.Predicate<T, E> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return t -> !predicate.test(t);
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param biPredicate
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> not(final Throwables.BiPredicate<T, U, E> biPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(biPredicate);

            return (t, u) -> !biPredicate.test(t, u);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <E>
         * @param triPredicate
         * @return
         * @throws IllegalArgumentException
         */
        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> not(final Throwables.TriPredicate<A, B, C, E> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return (a, b, c) -> !triPredicate.test(a, b, c);
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param count
         * @return a stateful {@code Throwables.Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
         * @throws IllegalArgumentException
         */
        @Beta
        @Stateful
        public static <T, E extends Throwable> Throwables.Predicate<T, E> atMost(final int count) throws IllegalArgumentException {
            N.checkArgNotNegative(count, cs.count);

            return new Throwables.Predicate<>() {
                private final AtomicInteger counter = new AtomicInteger(count);

                @Override
                public boolean test(final T t) {
                    return counter.getAndDecrement() > 0;
                }
            };
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param supplier
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.Supplier<T, E> from(final java.util.function.Supplier<T> supplier) {
            return supplier instanceof Throwables.Supplier ? ((Throwables.Supplier) supplier) : supplier::get;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param func
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.IntFunction<T, E> from(final java.util.function.IntFunction<? extends T> func) {
            return func instanceof Throwables.IntFunction ? ((Throwables.IntFunction) func) : func::apply;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param predicate
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.Predicate<T, E> from(final java.util.function.Predicate<T> predicate) {
            return predicate instanceof Throwables.Predicate ? ((Throwables.Predicate) predicate) : predicate::test;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param predicate
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> from(final java.util.function.BiPredicate<T, U> predicate) {
            return predicate instanceof Throwables.BiPredicate ? ((Throwables.BiPredicate) predicate) : predicate::test;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param consumer
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.Consumer<T, E> from(final java.util.function.Consumer<T> consumer) {
            return consumer instanceof Throwables.Consumer ? ((Throwables.Consumer) consumer) : consumer::accept;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param consumer
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> from(final java.util.function.BiConsumer<T, U> consumer) {
            return consumer instanceof Throwables.BiConsumer ? ((Throwables.BiConsumer) consumer) : consumer::accept;
        }

        /**
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param function
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> from(final java.util.function.Function<T, ? extends R> function) {
            return function instanceof Throwables.Function ? ((Throwables.Function) function) : function::apply;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param function
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> from(final java.util.function.BiFunction<T, U, ? extends R> function) {
            return function instanceof Throwables.BiFunction ? ((Throwables.BiFunction) function) : function::apply;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param op
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.UnaryOperator<T, E> from(final java.util.function.UnaryOperator<T> op) {
            return op instanceof Throwables.UnaryOperator ? ((Throwables.UnaryOperator) op) : op::apply;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param op
         * @return
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> from(final java.util.function.BinaryOperator<T> op) {
            return op instanceof Throwables.BinaryOperator ? ((Throwables.BinaryOperator) op) : op::apply;
        }

        @Beta
        public static <T, E extends Throwable> Throwables.Supplier<T, E> s(final Throwables.Supplier<T, E> supplier) {
            return supplier;
        }

        @Beta
        public static <A, T, E extends Throwable> Throwables.Supplier<T, E> s(final A a, final Throwables.Function<? super A, ? extends T, E> func) {
            return () -> func.apply(a);
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param predicate
         * @return
         * @see #from(java.util.function.Predicate)
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> p(final Throwables.Predicate<T, E> predicate) {
            return predicate;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <E>
         * @param a
         * @param biPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Predicate<T, E> p(final A a, final Throwables.BiPredicate<A, T, E> biPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(biPredicate);

            return t -> biPredicate.test(a, t);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <T>
         * @param <E>
         * @param a
         * @param b
         * @param triPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Predicate<T, E> p(final A a, final B b, final Throwables.TriPredicate<A, B, T, E> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return t -> triPredicate.test(a, b, t);
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param biPredicate
         * @return
         * @see #from(java.util.function.BiPredicate)
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> p(final Throwables.BiPredicate<T, U, E> biPredicate) {
            return biPredicate;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <U>
         * @param <E>
         * @param a
         * @param triPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> p(final A a, final Throwables.TriPredicate<A, T, U, E> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return (t, u) -> triPredicate.test(a, t, u);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <E>
         * @param triPredicate
         * @return
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> p(final Throwables.TriPredicate<A, B, C, E> triPredicate) {
            return triPredicate;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param consumer
         * @return
         * @see #from(java.util.function.Consumer)
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> c(final Throwables.Consumer<T, E> consumer) {
            return consumer;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <E>
         * @param a
         * @param biConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Consumer<T, E> c(final A a, final Throwables.BiConsumer<A, T, E> biConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return t -> biConsumer.accept(a, t);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <T>
         * @param <E>
         * @param a
         * @param b
         * @param triConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Consumer<T, E> c(final A a, final B b, final Throwables.TriConsumer<A, B, T, E> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return t -> triConsumer.accept(a, b, t);
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param biConsumer
         * @return
         * @see #from(java.util.function.BiConsumer)
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> c(final Throwables.BiConsumer<T, U, E> biConsumer) {
            return biConsumer;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <U>
         * @param <E>
         * @param a
         * @param triConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> c(final A a, final Throwables.TriConsumer<A, T, U, E> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (t, u) -> triConsumer.accept(a, t, u);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <E>
         * @param triConsumer
         * @return
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriConsumer<A, B, C, E> c(final Throwables.TriConsumer<A, B, C, E> triConsumer) {
            return triConsumer;
        }

        /**
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param function
         * @return
         * @see #from(java.util.function.Function)
         */
        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> f(final Throwables.Function<T, R, E> function) {
            return function;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <R>
         * @param <E>
         * @param a
         * @param biFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, R, E extends Throwable> Throwables.Function<T, R, E> f(final A a, final Throwables.BiFunction<A, T, R, E> biFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(biFunction);

            return t -> biFunction.apply(a, t);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <T>
         * @param <R>
         * @param <E>
         * @param a
         * @param b
         * @param triFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, T, R, E extends Throwable> Throwables.Function<T, R, E> f(final A a, final B b,
                final Throwables.TriFunction<A, B, T, R, E> triFunction) throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return t -> triFunction.apply(a, b, t);
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param biFunction
         * @return
         * @see #from(java.util.function.BiFunction)
         */
        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> f(final Throwables.BiFunction<T, U, R, E> biFunction) {
            return biFunction;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param a
         * @param triFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> f(final A a, final Throwables.TriFunction<A, T, U, R, E> triFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return (t, u) -> triFunction.apply(a, t, u);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <R>
         * @param <E>
         * @param triFunction
         * @return
         */
        @Beta
        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> f(final Throwables.TriFunction<A, B, C, R, E> triFunction) {
            return triFunction;
        }

        /**
         * <p>Returns the provided unary operator as is - a shorthand identity method for unary operators.</p>
         *
         * <p>This method serves as a shorthand convenience method that can help with type inference
         * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
         * and {@code p()} for Predicate.</p>
         *
         * @param <T> the type of the operand and result of the unary operator
         * @param <E> the type of the exception that may be thrown by the operator
         * @param unaryOperator the unary operator to return
         * @return the unary operator unchanged
         */
        @Beta
        public static <T, E extends Throwable> Throwables.UnaryOperator<T, E> o(final Throwables.UnaryOperator<T, E> unaryOperator) {
            N.checkArgNotNull(unaryOperator);

            return unaryOperator;
        }

        /**
         * <p>Returns the provided binary operator as is - a shorthand identity method for binary operators.</p>
         *
         * <p>This method serves as a shorthand convenience method that can help with type inference
         * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
         * and {@code p()} for Predicate.</p>
         *
         * @param <T> the type of the operands and result of the binary operator
         * @param <E> the type of the exception that may be thrown by the operator
         * @param binaryOperator the binary operator to return
         * @return the binary operator unchanged
         */
        @Beta
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> o(final Throwables.BinaryOperator<T, E> binaryOperator) {
            N.checkArgNotNull(binaryOperator);

            return binaryOperator;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         * @see #from(java.util.function.Predicate)
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> pp(final Predicate<T> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return (Throwables.Predicate<T, E>) predicate;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <E>
         * @param a
         * @param biPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Predicate<T, E> pp(final A a, final java.util.function.BiPredicate<A, T> biPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(biPredicate);

            return t -> biPredicate.test(a, t);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <T>
         * @param <E>
         * @param a
         * @param b
         * @param triPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Predicate<T, E> pp(final A a, final B b, final TriPredicate<A, B, T> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return t -> triPredicate.test(a, b, t);
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param biPredicate
         * @return
         * @throws IllegalArgumentException
         * @see #from(java.util.function.BiPredicate)
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> pp(final BiPredicate<T, U> biPredicate) throws IllegalArgumentException {
            N.checkArgNotNull(biPredicate);

            return (Throwables.BiPredicate<T, U, E>) biPredicate;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <U>
         * @param <E>
         * @param a
         * @param triPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> pp(final A a, final TriPredicate<A, T, U> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return (t, u) -> triPredicate.test(a, t, u);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <E>
         * @param triPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> pp(final TriPredicate<A, B, C> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return (Throwables.TriPredicate<A, B, C, E>) triPredicate;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param consumer
         * @return
         * @throws IllegalArgumentException
         * @see #from(java.util.function.Consumer)
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> cc(final Consumer<T> consumer) throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return (Throwables.Consumer<T, E>) consumer;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <E>
         * @param a
         * @param biConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Consumer<T, E> cc(final A a, final java.util.function.BiConsumer<A, T> biConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return t -> biConsumer.accept(a, t);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <T>
         * @param <E>
         * @param a
         * @param b
         * @param triConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Consumer<T, E> cc(final A a, final B b, final TriConsumer<A, B, T> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return t -> triConsumer.accept(a, b, t);
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param biConsumer
         * @return
         * @throws IllegalArgumentException
         * @see #from(java.util.function.BiConsumer)
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> cc(final BiConsumer<T, U> biConsumer) throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return (Throwables.BiConsumer<T, U, E>) biConsumer;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <U>
         * @param <E>
         * @param a
         * @param triConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> cc(final A a, final TriConsumer<A, T, U> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (t, u) -> triConsumer.accept(a, t, u);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <E>
         * @param triConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriConsumer<A, B, C, E> cc(final TriConsumer<A, B, C> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (Throwables.TriConsumer<A, B, C, E>) triConsumer;
        }

        /**
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param function
         * @return
         * @throws IllegalArgumentException
         * @see #from(java.util.function.Function)
         */
        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final Function<T, ? extends R> function) throws IllegalArgumentException {
            N.checkArgNotNull(function);

            return (Throwables.Function<T, R, E>) function;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <R>
         * @param <E>
         * @param a
         * @param biFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final A a, final java.util.function.BiFunction<A, T, R> biFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(biFunction);

            return t -> biFunction.apply(a, t);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <T>
         * @param <R>
         * @param <E>
         * @param a
         * @param b
         * @param triFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final A a, final B b, final TriFunction<A, B, T, R> triFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return t -> triFunction.apply(a, b, t);
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param biFunction
         * @return
         * @throws IllegalArgumentException
         * @see #from(java.util.function.BiFunction)
         */
        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> ff(final BiFunction<T, U, R> biFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(biFunction);

            return (Throwables.BiFunction<T, U, R, E>) biFunction;
        }

        /**
         *
         * @param <A>
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param a
         * @param triFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> ff(final A a, final TriFunction<A, T, U, R> triFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return (t, u) -> triFunction.apply(a, t, u);
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <R>
         * @param <E>
         * @param triFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> ff(final TriFunction<A, B, C, R> triFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return (Throwables.TriFunction<A, B, C, R, E>) triFunction;
        }

        /**
         * Synchronized {@code Predicate}.
         *
         * @param <T>
         * @param <E>
         * @param mutex to synchronize on
         * @param predicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> sp(final Object mutex, final Throwables.Predicate<T, E> predicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(predicate, cs.Predicate);

            return t -> {
                synchronized (mutex) {
                    return predicate.test(t);
                }
            };
        }

        /**
         * Synchronized {@code Predicate}.
         *
         * @param <A>
         * @param <T>
         * @param <E>
         * @param mutex to synchronize on
         * @param a
         * @param biPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Predicate<T, E> sp(final Object mutex, final A a,
                final Throwables.BiPredicate<A, T, E> biPredicate) throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(biPredicate, cs.BiPredicate);

            return t -> {
                synchronized (mutex) {
                    return biPredicate.test(a, t);
                }
            };
        }

        /**
         * Synchronized {@code BiPredicate}.
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param mutex to synchronize on
         * @param biPredicate
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> sp(final Object mutex, final Throwables.BiPredicate<T, U, E> biPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(biPredicate, cs.BiPredicate);

            return (t, u) -> {
                synchronized (mutex) {
                    return biPredicate.test(t, u);
                }
            };
        }

        /**
         * Synchronized {@code Consumer}.
         *
         * @param <T>
         * @param <E>
         * @param mutex to synchronize on
         * @param consumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> sc(final Object mutex, final Throwables.Consumer<T, E> consumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(consumer, cs.Consumer);

            return t -> {
                synchronized (mutex) {
                    consumer.accept(t);
                }
            };
        }

        /**
         * Synchronized {@code Consumer}.
         *
         * @param <A>
         * @param <T>
         * @param <E>
         * @param mutex to synchronize on
         * @param a
         * @param biConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Consumer<T, E> sc(final Object mutex, final A a, final Throwables.BiConsumer<A, T, E> biConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(biConsumer, cs.BiConsumer);

            return t -> {
                synchronized (mutex) {
                    biConsumer.accept(a, t);
                }
            };
        }

        /**
         * Synchronized {@code BiConsumer}.
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param mutex to synchronize on
         * @param biConsumer
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> sc(final Object mutex, final Throwables.BiConsumer<T, U, E> biConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(biConsumer, cs.BiConsumer);

            return (t, u) -> {
                synchronized (mutex) {
                    biConsumer.accept(t, u);
                }
            };
        }

        /**
         * Synchronized {@code Function}.
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param mutex to synchronize on
         * @param function
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> sf(final Object mutex, final Throwables.Function<T, ? extends R, E> function)
                throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(function, cs.function);

            return t -> {
                synchronized (mutex) {
                    return function.apply(t);
                }
            };
        }

        /**
         * Synchronized {@code Function}.
         *
         * @param <A>
         * @param <T>
         * @param <R>
         * @param <E>
         * @param mutex to synchronize on
         * @param a
         * @param biFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <A, T, R, E extends Throwable> Throwables.Function<T, R, E> sf(final Object mutex, final A a,
                final Throwables.BiFunction<A, T, R, E> biFunction) throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(biFunction, cs.BiFunction);

            return t -> {
                synchronized (mutex) {
                    return biFunction.apply(a, t);
                }
            };
        }

        /**
         * Synchronized {@code BiFunction}.
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param mutex to synchronize on
         * @param biFunction
         * @return
         * @throws IllegalArgumentException
         */
        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> sf(final Object mutex,
                final Throwables.BiFunction<T, U, R, E> biFunction) throws IllegalArgumentException {
            N.checkArgNotNull(mutex, cs.mutex);
            N.checkArgNotNull(biFunction, cs.BiFunction);

            return (t, u) -> {
                synchronized (mutex) {
                    return biFunction.apply(t, u);
                }
            };
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param consumer
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, E extends Throwable> Throwables.Function<T, Void, E> c2f(final Throwables.Consumer<T, E> consumer) throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return t -> {
                consumer.accept(t);

                return null;
            };
        }

        /**
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param consumer
         * @param valueToReturn
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> c2f(final Throwables.Consumer<T, E> consumer, final R valueToReturn)
                throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return t -> {
                consumer.accept(t);

                return valueToReturn;
            };
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param biConsumer
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, U, E extends Throwable> Throwables.BiFunction<T, U, Void, E> c2f(final Throwables.BiConsumer<T, U, E> biConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return (t, u) -> {
                biConsumer.accept(t, u);

                return null;
            };
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param biConsumer
         * @param valueToReturn
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> c2f(final Throwables.BiConsumer<T, U, E> biConsumer,
                final R valueToReturn) throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return (t, u) -> {
                biConsumer.accept(t, u);

                return valueToReturn;
            };
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <E>
         * @param triConsumer
         * @return
         * @throws IllegalArgumentException
         */
        public static <A, B, C, E extends Throwable> Throwables.TriFunction<A, B, C, Void, E> c2f(final Throwables.TriConsumer<A, B, C, E> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (a, b, c) -> {
                triConsumer.accept(a, b, c);

                return null;
            };
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <R>
         * @param <E>
         * @param triConsumer
         * @param valueToReturn
         * @return
         * @throws IllegalArgumentException
         */
        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> c2f(final Throwables.TriConsumer<A, B, C, E> triConsumer,
                final R valueToReturn) throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (a, b, c) -> {
                triConsumer.accept(a, b, c);

                return valueToReturn;
            };
        }

        /**
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, R, E extends Throwable> Throwables.Consumer<T, E> f2c(final Throwables.Function<T, ? extends R, E> func)
                throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return func::apply;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws IllegalArgumentException
         */
        public static <T, U, R, E extends Throwable> Throwables.BiConsumer<T, U, E> f2c(final Throwables.BiFunction<T, U, ? extends R, E> func)
                throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return func::apply;
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws IllegalArgumentException
         */
        public static <A, B, C, R, E extends Throwable> Throwables.TriConsumer<A, B, C, E> f2c(final Throwables.TriFunction<A, B, C, ? extends R, E> func)
                throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return func::apply;
        }

        /**
         *
         * @param <E>
         * @param runnable
         * @return
         * @throws IllegalArgumentException
         */
        public static <E extends Throwable> Throwables.Runnable<E> r(final Throwables.Runnable<E> runnable) throws IllegalArgumentException {
            N.checkArgNotNull(runnable);

            return runnable;
        }

        /**
         *
         * @param <R>
         * @param <E>
         * @param callable
         * @return
         * @throws IllegalArgumentException
         */
        public static <R, E extends Throwable> Throwables.Callable<R, E> c(final Throwables.Callable<R, E> callable) throws IllegalArgumentException {
            N.checkArgNotNull(callable);

            return callable;
        }

        /**
         *
         * @param <E>
         * @param runnable
         * @return
         * @throws IllegalArgumentException
         */
        public static <E extends Throwable> Throwables.Callable<Void, E> r2c(final Throwables.Runnable<E> runnable) throws IllegalArgumentException {
            N.checkArgNotNull(runnable);

            return () -> {
                runnable.run();
                return null;
            };
        }

        /**
         *
         * @param <R>
         * @param <E>
         * @param runnable
         * @param valueToReturn
         * @return
         * @throws IllegalArgumentException
         */
        public static <R, E extends Throwable> Throwables.Callable<R, E> r2c(final Throwables.Runnable<E> runnable, final R valueToReturn)
                throws IllegalArgumentException {
            N.checkArgNotNull(runnable);

            return () -> {
                runnable.run();
                return valueToReturn;
            };
        }

        /**
         *
         * @param <R>
         * @param <E>
         * @param callable
         * @return
         * @throws IllegalArgumentException
         */
        public static <R, E extends Throwable> Throwables.Runnable<E> c2r(final Throwables.Callable<R, E> callable) throws IllegalArgumentException {
            N.checkArgNotNull(callable);

            return callable::call;
        }

        /**
         *
         * @param <E>
         * @param runnable
         * @return
         */
        public static <E extends Throwable> Throwables.Runnable<E> rr(final Runnable runnable) {
            return (Throwables.Runnable<E>) runnable;
        }

        /**
         *
         * @param <R>
         * @param <E>
         * @param callable
         * @return
         */
        public static <R, E extends Throwable> Throwables.Callable<R, E> cc(final Callable<R> callable) {
            return (Throwables.Callable<R, E>) callable;
        }

        /**
         *
         * @param <E>
         * @param runnable
         * @return
         * @throws IllegalArgumentException
         */
        public static <E extends Throwable> Throwables.Runnable<E> jr2r(final java.lang.Runnable runnable) throws IllegalArgumentException {
            N.checkArgNotNull(runnable);

            if (runnable instanceof Throwables.Runnable) {
                return (Throwables.Runnable<E>) runnable;
            }

            return runnable::run;
        }

        /**
         *
         * @param <E>
         * @param runnable
         * @return
         * @throws IllegalArgumentException
         */
        public static <E extends Throwable> java.lang.Runnable r2jr(final Throwables.Runnable<E> runnable) throws IllegalArgumentException {
            N.checkArgNotNull(runnable);

            if (runnable instanceof java.lang.Runnable) {
                return (java.lang.Runnable) runnable;
            }

            return () -> {
                try {
                    runnable.run();
                } catch (final Throwable e) { // NOSONAR
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }

        /**
         *
         * @param <R>
         * @param callable
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> Throwables.Callable<R, Exception> jc2c(final java.util.concurrent.Callable<R> callable) throws IllegalArgumentException {
            N.checkArgNotNull(callable);

            if (callable instanceof Throwables.Callable) {
                return (Throwables.Callable<R, Exception>) callable;
            }

            return callable::call;
        }

        /**
         *
         * @param <R>
         * @param callable
         * @return
         * @throws IllegalArgumentException
         */
        public static <R> java.util.concurrent.Callable<R> c2jc(final Throwables.Callable<R, ? extends Exception> callable) throws IllegalArgumentException {
            N.checkArgNotNull(callable);

            if (callable instanceof java.util.concurrent.Callable) {
                return (java.util.concurrent.Callable<R>) callable;
            }

            return callable::call;
        }
    }
}
