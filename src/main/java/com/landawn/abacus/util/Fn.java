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
import com.landawn.abacus.util.stream.Stream;

//Claude Opus 4 and generate the Javadoc with blow prompts 
// Please generate comprehensive javadoc for all public methods starting from line 6 in Fn.java, including public static methods. Please use javadoc of method "toStr()" as a template to generate javadoc for other methods. Please don't take shortcut. The generated javadoc should be specific and details enough to describe the behavior of the method. And merge the generated javadoc into source file Fn.java to replace existing javadoc in Fn.java. Don't generate javadoc for method which is not found in Fn.java. Remember don't use any cache file because I have modified Fn.java. Again, don't generate javadoc for the method which is not in the attached file. Please read and double check if the method is in the attached file before starting to generate javadoc. If the method is not the attached file, don't generate javadoc for it.

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

    private static final Function<Pair<Object, Object>, Object> LEFT = Pair::left;

    private static final Function<Pair<Object, Object>, Object> RIGHT = Pair::right;

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

    /**
     * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
     * 
     * Creates a memoizing supplier that caches the result of the delegate supplier and automatically
     * expires the cached value after a specified duration. This implementation is thread-safe and
     * provides automatic cache invalidation based on time.
     * 
     * <p>This method is particularly useful for expensive computations or I/O operations that:
     * <ul>
     *   <li>Have results that remain valid for a known period of time</li>
     *   <li>Are called frequently enough to benefit from caching</li>
     *   <li>Need automatic expiration without manual cache management</li>
     * </ul>
     * 
     * <p><b>Thread Safety:</b> The returned supplier is fully thread-safe. Multiple threads can
     * safely call {@code get()} concurrently. The implementation uses double-checked locking to
     * ensure that the delegate supplier is called at most once per expiration period, even under
     * concurrent access.
     * 
     * 
     * <p><b>Example Usage:</b>
     * <pre>{@code
     * // Cache database query results for 5 minutes
     * Supplier<List<User>> cachedUsers = Fn.memoizeWithExpiration(
     *     () -> database.getAllUsers(),
     *     5, TimeUnit.MINUTES
     * );
     * 
     * // First call queries the database
     * List<User> users1 = cachedUsers.get();
     * 
     * // Subsequent calls within 5 minutes return cached value
     * List<User> users2 = cachedUsers.get(); // No database query
     * 
     * // After 5 minutes, the next call will query database again
     * // and cache the new result
     * }</pre>
     * 
     * <p><b>Typical Use Cases:</b>
     * <pre>{@code
     * // Configuration values that might change periodically
     * Supplier<Config> config = Fn.memoizeWithExpiration(
     *     () -> loadConfigFromFile(),
     *     30, TimeUnit.SECONDS
     * );
     * 
     * // API responses with known freshness requirements
     * Supplier<WeatherData> weather = Fn.memoizeWithExpiration(
     *     () -> weatherApi.getCurrentWeather(),
     *     10, TimeUnit.MINUTES
     * );
     * }</pre>
     *
     * @param <T> the type of object returned by the supplier
     * @param supplier the delegate supplier whose results should be cached. Must not be null.
     *                 This supplier will be called to provide values when the cache is empty
     *                 or expired
     * @param duration the length of time after a value is created that it should remain in
     *                 the cache before expiring. Must be positive. After this duration passes,
     *                 the next call to {@code get()} will invoke the delegate supplier again
     * @param unit the time unit for the duration parameter. Must not be null. Common units
     *             include {@code TimeUnit.SECONDS}, {@code TimeUnit.MINUTES}, etc.
     * @return a new supplier that caches the result of the delegate supplier for the specified
     *         duration. The returned supplier's {@code get()} method will return cached values
     *         within the expiration window and fetch fresh values when the cache expires
     * @throws IllegalArgumentException if {@code duration} is not positive (i.e., duration ≤ 0)
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

    @SuppressWarnings("rawtypes")
    private static final Function TO_JSON = N::toJson;

    @SuppressWarnings("rawtypes")
    private static final Function TO_XML = N::toXml;

    private static final Function<Keyed<?, Object>, Object> VAL = Keyed::val;

    private static final Function<Map.Entry<Keyed<Object, Object>, Object>, Object> KK_VAL = t -> t.getKey().val();

    private static final Function<Object, Wrapper<Object>> WRAP = Wrapper::of;

    private static final Function<Wrapper<Object>, Object> UNWRAP = Wrapper::value;

    private static final Predicate<Object[]> IS_EMPTY_A = value -> value == null || value.length == 0;

    @SuppressWarnings("rawtypes")
    private static final Predicate<Collection> IS_EMPTY_C = value -> value == null || value.size() == 0;

    @SuppressWarnings("rawtypes")
    private static final Predicate<Map> IS_EMPTY_M = value -> value == null || value.isEmpty();

    private static final Predicate<Object[]> NOT_EMPTY_A = value -> value != null && value.length > 0;

    @SuppressWarnings("rawtypes")
    private static final Predicate<Collection> NOT_EMPTY_C = value -> value != null && value.size() > 0;

    @SuppressWarnings("rawtypes")
    private static final Predicate<Map> NOT_EMPTY_M = value -> value != null && !value.isEmpty();

    private static final Function<Map<Object, Collection<Object>>, List<Map<Object, Object>>> FLAT_MAP_VALUE_FUNC = Maps::flatToMap;

    private static final ToByteFunction<String> PARSE_BYTE_FUNC = Numbers::toByte;

    private static final ToShortFunction<String> PARSE_SHORT_FUNC = Numbers::toShort;

    private static final ToIntFunction<String> PARSE_INT_FUNC = Numbers::toInt;

    private static final ToLongFunction<String> PARSE_LONG_FUNC = Numbers::toLong;

    private static final ToFloatFunction<String> PARSE_FLOAT_FUNC = Numbers::toFloat;

    private static final ToDoubleFunction<String> PARSE_DOUBLE_FUNC = Numbers::toDouble;

    private static final Function<String, Number> CREATE_NUMBER_FUNC = t -> Strings.isEmpty(t) ? null : Numbers.createNumber(t);

    /**
     * Returns a Runnable that closes the specified AutoCloseable resource.
     * The returned Runnable ensures the resource is closed only once, even if called multiple times.
     *
     * @param closeable the AutoCloseable resource to close
     * @return a Runnable that closes the resource when executed
     * @see IOUtil#close(AutoCloseable)
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
     * Returns a Runnable that closes all specified AutoCloseable resources.
     * The returned Runnable ensures all resources are closed only once, even if called multiple times.
     *
     * @param a the array of AutoCloseable resources to close
     * @return a Runnable that closes all resources when executed
     * @see IOUtil#closeAll(AutoCloseable...)
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
     * Returns a Runnable that closes all AutoCloseable resources in the specified collection.
     * The returned Runnable ensures all resources are closed only once, even if called multiple times.
     *
     * @param c the collection of AutoCloseable resources to close
     * @return a Runnable that closes all resources when executed
     * @see IOUtil#closeAll(Collection)
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
     * Returns a Runnable that quietly closes the specified AutoCloseable resource.
     * Any exceptions thrown during closing are suppressed. The returned Runnable 
     * ensures the resource is closed only once, even if called multiple times.
     *
     * @param closeable the AutoCloseable resource to close quietly
     * @return a Runnable that closes the resource quietly when executed
     * @see IOUtil#closeQuietly(AutoCloseable)
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
     * Returns a Runnable that quietly closes all specified AutoCloseable resources.
     * Any exceptions thrown during closing are suppressed. The returned Runnable 
     * ensures all resources are closed only once, even if called multiple times.
     *
     * @param a the array of AutoCloseable resources to close quietly
     * @return a Runnable that closes all resources quietly when executed
     * @see IOUtil#closeAllQuietly(AutoCloseable...)
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
     * Returns a Runnable that quietly closes all AutoCloseable resources in the specified collection.
     * Any exceptions thrown during closing are suppressed. The returned Runnable 
     * ensures all resources are closed only once, even if called multiple times.
     *
     * @param c the collection of AutoCloseable resources to close quietly
     * @return a Runnable that closes all resources quietly when executed
     * @see IOUtil#closeAllQuietly(Collection)
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
     * Returns an empty Runnable that performs no operation when executed.
     *
     * @return an empty Runnable which does nothing
     */
    public static Runnable emptyAction() {
        return EMPTY_ACTION;
    }

    /**
     * Returns a Runnable that shuts down the specified ExecutorService.
     * The returned Runnable ensures the service is shut down only once, even if called multiple times.
     *
     * @param service the ExecutorService to shut down
     * @return a Runnable that shuts down the service when executed
     * @see ExecutorService#shutdown()
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
     * Returns a Runnable that shuts down the specified ExecutorService and waits for termination.
     * The returned Runnable ensures the service is shut down only once, even if called multiple times.
     *
     * @param service the ExecutorService to shut down
     * @param terminationTimeout the maximum time to wait for termination
     * @param timeUnit the time unit of the timeout argument
     * @return a Runnable that shuts down the service and waits for termination
     * @see ExecutorService#shutdown()
     * @see ExecutorService#awaitTermination(long, TimeUnit)
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
     * Returns an empty Consumer that performs no operation on its input.
     *
     * @param <T> the type of the input to the consumer
     * @return an empty Consumer which does nothing
     * @deprecated Use {@link #emptyConsumer()} instead.
     * @see #emptyConsumer()
     */
    public static <T> Consumer<T> doNothing() {
        return EMPTY_CONSUMER;
    }

    /**
     * Returns an empty Consumer that performs no operation on its input.
     *
     * @param <T> the type of the input to the consumer
     * @return an empty Consumer which does nothing
     */
    public static <T> Consumer<T> emptyConsumer() {
        return EMPTY_CONSUMER;
    }

    /**
     * Returns a Consumer that throws a RuntimeException with the specified error message.
     *
     * @param <T> the type of the input to the consumer
     * @param errorMessage the error message for the RuntimeException
     * @return a Consumer that throws RuntimeException when invoked
     */
    public static <T> Consumer<T> throwRuntimeException(final String errorMessage) {
        return t -> {
            throw new RuntimeException(errorMessage);
        };
    }

    /**
     * Returns a Consumer that throws a RuntimeException created by the specified supplier.
     *
     * @param <T> the type of the input to the consumer
     * @param exceptionSupplier the supplier that creates the exception to throw
     * @return a Consumer that throws the supplied exception when invoked
     */
    public static <T> Consumer<T> throwException(final java.util.function.Supplier<? extends RuntimeException> exceptionSupplier) {
        return t -> {
            throw exceptionSupplier.get();
        };
    }

    /**
     * Returns a Function that converts Throwable to RuntimeException.
     * If the input is already a RuntimeException, it is returned as-is.
     * Otherwise, the Throwable is wrapped in a RuntimeException.
     *
     * @return a Function that converts Throwable to RuntimeException
     */
    public static Function<Throwable, RuntimeException> toRuntimeException() {
        return TO_RUNTIME_EXCEPTION;
    }

    /**
     * Returns a Consumer that closes AutoCloseable resources.
     *
     * @param <T> the type of AutoCloseable
     * @return a Consumer that closes AutoCloseable resources
     * @see AutoCloseable#close()
     */
    public static <T extends AutoCloseable> Consumer<T> close() {
        return (Consumer<T>) CLOSE;
    }

    /**
     * Returns a Consumer that quietly closes AutoCloseable resources.
     * Any exceptions thrown during closing are suppressed.
     *
     * @param <T> the type of AutoCloseable
     * @return a Consumer that quietly closes AutoCloseable resources
     * @see IOUtil#closeQuietly(AutoCloseable)
     */
    public static <T extends AutoCloseable> Consumer<T> closeQuietly() {
        return (Consumer<T>) CLOSE_QUIETLY;
    }

    /**
     * Returns a Consumer that sleeps for the specified number of milliseconds.
     * The input value is ignored.
     *
     * @param <T> the type of the input (ignored)
     * @param millis the sleep duration in milliseconds
     * @return a Consumer that sleeps for the specified duration
     * @see N#sleep(long)
     */
    public static <T> Consumer<T> sleep(final long millis) {
        return t -> N.sleep(millis);
    }

    /**
     * Returns a Consumer that sleeps uninterruptibly for the specified number of milliseconds.
     * The input value is ignored. If interrupted, the interrupt status is preserved.
     *
     * @param <T> the type of the input (ignored)
     * @param millis the sleep duration in milliseconds
     * @return a Consumer that sleeps uninterruptibly for the specified duration
     * @see N#sleepUninterruptibly(long)
     */
    public static <T> Consumer<T> sleepUninterruptibly(final long millis) {
        return t -> N.sleepUninterruptibly(millis);
    }

    /**
     * Returns a stateful Consumer that rate-limits execution based on permits per second.
     * Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T> the type of the input
     * @param permitsPerSecond the rate limit in permits per second
     * @return a stateful Consumer that rate-limits execution
     * @see RateLimiter#acquire()
     * @see RateLimiter#create(double)
     */
    @Stateful
    public static <T> Consumer<T> rateLimiter(final double permitsPerSecond) {
        return rateLimiter(RateLimiter.create(permitsPerSecond));
    }

    /**
     * Returns a stateful Consumer that rate-limits execution using the provided RateLimiter.
     * Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T> the type of the input
     * @param rateLimiter the RateLimiter to use for rate limiting
     * @return a stateful Consumer that rate-limits execution
     * @see RateLimiter#acquire()
     */
    @Stateful
    public static <T> Consumer<T> rateLimiter(final RateLimiter rateLimiter) {
        return t -> rateLimiter.acquire();
    }

    /**
     * Returns a Consumer that prints its input to standard output using N.println().
     *
     * @param <T> the type of the input
     * @return a Consumer that prints its input
     * @see N#println(Object)
     */
    public static <T> Consumer<T> println() {
        return PRINTLN;
    }

    /**
     * Returns a BiConsumer that prints its two inputs separated by the specified separator.
     *
     * @param <T> the type of the first input
     * @param <U> the type of the second input
     * @param separator the separator to use between the two values
     * @return a BiConsumer that prints both inputs with separator
     * @throws IllegalArgumentException if separator is null
     * @see N#println(Object)
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
     * Returns a Function that converts its input to a String using String.valueOf().
     * 
     * @param <T> the type of the input
     * @return a Function that converts its input to String
     * @see String#valueOf(Object)
     */
    public static <T> Function<T, String> toStr() {
        return TO_STRING;
    }

    /**
     * Returns a UnaryOperator that converts strings to camel case.
     *
     * @return a UnaryOperator that converts strings to camel case
     * @see Strings#toCamelCase(String)
     */
    public static UnaryOperator<String> toCamelCase() {
        return TO_CAMEL_CASE;
    }

    /**
     * Returns a UnaryOperator that converts strings to lower case.
     *
     * @return a UnaryOperator that converts strings to lower case
     * @see String#toLowerCase()
     */
    public static UnaryOperator<String> toLowerCase() {
        return TO_LOWER_CASE;
    }

    /**
     * Returns a UnaryOperator that converts strings to lower case with underscores.
     *
     * @return a UnaryOperator that converts strings to lower case with underscores
     * @see Strings#toLowerCaseWithUnderscore(String)
     */
    public static UnaryOperator<String> toLowerCaseWithUnderscore() {
        return TO_LOWER_CASE_WITH_UNDERSCORE;
    }

    /**
     * Returns a UnaryOperator that converts strings to upper case.
     *
     * @return a UnaryOperator that converts strings to upper case
     * @see String#toUpperCase()
     */
    public static UnaryOperator<String> toUpperCase() {
        return TO_UPPER_CASE;
    }

    /**
     * Returns a UnaryOperator that converts strings to upper case with underscores.
     *
     * @return a UnaryOperator that converts strings to upper case with underscores
     * @see Strings#toUpperCaseWithUnderscore(String)
     */
    public static UnaryOperator<String> toUpperCaseWithUnderscore() {
        return TO_UPPER_CASE_WITH_UNDERSCORE;
    }

    /**
     * Returns a Function that converts objects to JSON string representation.
     *
     * @param <T> the type of the input object
     * @return a Function that converts objects to JSON
     * @see N#toJson(Object)
     */
    public static <T> Function<T, String> toJson() {
        return TO_JSON;
    }

    /**
     * Returns a Function that converts objects to XML string representation.
     *
     * @param <T> the type of the input object
     * @return a Function that converts objects to XML
     * @see N#toXml(Object)
     */
    public static <T> Function<T, String> toXml() {
        return TO_XML;
    }

    /**
     * Returns an identity Function that returns its input unchanged.
     *
     * @param <T> the type of the input and output
     * @return an identity Function
     * @see Function#identity()
     */
    public static <T> Function<T, T> identity() {
        return IDENTITY;
    }

    /**
     * Returns a Function that wraps an object with its key extracted by the keyExtractor.
     *
     * @param <K> the key type
     * @param <T> the value type
     * @param keyExtractor the function to extract the key from the value
     * @return a Function that creates Keyed objects
     * @throws IllegalArgumentException if keyExtractor is null
     * @see Keyed#of(Object, Object)
     */
    public static <K, T> Function<T, Keyed<K, T>> keyed(final java.util.function.Function<? super T, K> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return t -> Keyed.of(keyExtractor.apply(t), t);
    }

    /**
     * Returns a Function that extracts the value from a Keyed object.
     *
     * @param <K> the key type
     * @param <T> the value type
     * @return a Function that extracts values from Keyed objects
     * @see Keyed#val()
     */
    @SuppressWarnings("rawtypes")
    public static <K, T> Function<Keyed<K, T>, T> val() {
        return (Function) VAL;
    }

    /**
     * Returns a Function that extracts the value from a Map.Entry with a Keyed key.
     * This is useful when working with entries where the key itself is a Keyed object.
     *
     * @param <K> the key type in the Keyed key
     * @param <T> the value type in the Keyed key
     * @param <V> the value type of the Map.Entry
     * @return a Function that extracts the value from the Keyed key
     */
    @SuppressWarnings("rawtypes")
    public static <K, T, V> Function<Map.Entry<Keyed<K, T>, V>, T> kkv() {
        return (Function) KK_VAL;
    }

    /**
     * Returns a Function that wraps objects in a Wrapper.
     *
     * @param <T> the type to wrap
     * @return a Function that creates Wrapper objects
     * @see Wrapper#of(Object)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Wrapper<T>> wrap() {
        return (Function) WRAP;
    }

    /**
     * Returns a Function that wraps objects with custom hash and equals functions.
     *
     * @param <T> the type to wrap
     * @param hashFunction the function to compute hash codes
     * @param equalsFunction the function to test equality
     * @return a Function that creates Wrapper objects with custom behavior
     * @throws IllegalArgumentException if hashFunction or equalsFunction is null
     * @see Wrapper#of(Object, ToIntFunction, BiPredicate)
     */
    public static <T> Function<T, Wrapper<T>> wrap(final java.util.function.ToIntFunction<? super T> hashFunction,
            final java.util.function.BiPredicate<? super T, ? super T> equalsFunction) throws IllegalArgumentException {
        N.checkArgNotNull(hashFunction);
        N.checkArgNotNull(equalsFunction);

        return t -> Wrapper.of(t, hashFunction, equalsFunction);
    }

    /**
     * Returns a Function that unwraps Wrapper objects to get their values.
     *
     * @param <T> the wrapped type
     * @return a Function that extracts values from Wrapper objects
     * @see Wrapper#value()
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<Wrapper<T>, T> unwrap() {
        return (Function) UNWRAP;
    }

    /**
     * Returns a Function that extracts the key from a Map.Entry.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a Function that extracts keys from Map.Entry objects
     * @see Map.Entry#getKey()
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, K> key() {
        return (Function) KEY;
    }

    /**
     * Returns a Function that extracts the value from a Map.Entry.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a Function that extracts values from Map.Entry objects
     * @see Map.Entry#getValue()
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, V> value() {
        return (Function) VALUE;
    }

    /**
     * Returns a Function that extracts the left element from a Pair.
     *
     * @param <L> the left element type
     * @param <R> the right element type
     * @return a Function that extracts the left element from Pair objects
     * @see Pair#left
     */
    @SuppressWarnings("rawtypes")
    public static <L, R> Function<Pair<L, R>, L> left() {
        return (Function) LEFT;
    }

    /**
     * Returns a Function that extracts the right element from a Pair.
     *
     * @param <L> the left element type
     * @param <R> the right element type
     * @return a Function that extracts the right element from Pair objects
     * @see Pair#right
     */
    @SuppressWarnings("rawtypes")
    public static <L, R> Function<Pair<L, R>, R> right() {
        return (Function) RIGHT;
    }

    /**
     * Returns a Function that inverts a Map.Entry by swapping its key and value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a Function that inverts Map.Entry objects
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, Entry<V, K>> inverse() {
        return (Function) INVERSE;
    }

    /**
     * Returns a BiFunction that creates a Map.Entry from a key and value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a BiFunction that creates Map.Entry objects
     * @see ImmutableEntry
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> BiFunction<K, V, Map.Entry<K, V>> entry() {
        return (BiFunction) ENTRY;
    }

    /**
     * Returns a Function that creates Map.Entry objects with a fixed key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key the fixed key for all created entries
     * @return a Function that creates Map.Entry objects with the fixed key
     * @deprecated replaced by {@code Fn#entryWithKey(Object)}
     * @see #entryWithKey(Object)
     */
    @Deprecated
    public static <K, V> Function<V, Map.Entry<K, V>> entry(final K key) {
        return entryWithKey(key);
    }

    /**
     * Returns a Function that creates Map.Entry objects by extracting keys from values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyExtractor the function to extract keys from values
     * @return a Function that creates Map.Entry objects
     * @deprecated replaced by {@code Fn#entryByKeyMapper(Function)}
     * @see #entryByKeyMapper(Function)
     */
    @Deprecated
    public static <K, V> Function<V, Map.Entry<K, V>> entry(final java.util.function.Function<? super V, K> keyExtractor) {
        return entryByKeyMapper(keyExtractor);
    }

    /**
     * Returns a Function that creates Map.Entry objects with a fixed key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key the fixed key for all created entries
     * @return a Function that creates Map.Entry objects with the fixed key
     */
    public static <K, V> Function<V, Map.Entry<K, V>> entryWithKey(final K key) {
        return v -> new ImmutableEntry<>(key, v);
    }

    /**
     * Returns a Function that creates Map.Entry objects by extracting keys from values.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyExtractor the function to extract keys from values
     * @return a Function that creates Map.Entry objects
     * @throws IllegalArgumentException if keyExtractor is null
     */
    public static <K, V> Function<V, Map.Entry<K, V>> entryByKeyMapper(final java.util.function.Function<? super V, K> keyExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);

        return v -> new ImmutableEntry<>(keyExtractor.apply(v), v);
    }

    /**
     * Returns a Function that creates Map.Entry objects with a fixed value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param value the fixed value for all created entries
     * @return a Function that creates Map.Entry objects with the fixed value
     */
    public static <K, V> Function<K, Map.Entry<K, V>> entryWithValue(final V value) {
        return k -> new ImmutableEntry<>(k, value);
    }

    /**
     * Returns a Function that creates Map.Entry objects by extracting values from keys.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param valueExtractor the function to extract values from keys
     * @return a Function that creates Map.Entry objects
     * @throws IllegalArgumentException if valueExtractor is null
     */
    public static <K, V> Function<K, Map.Entry<K, V>> entryByValueMapper(final java.util.function.Function<? super K, V> valueExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(valueExtractor);

        return k -> new ImmutableEntry<>(k, valueExtractor.apply(k));
    }

    /**
     * Returns a BiFunction that creates Pair objects from two elements.
     *
     * @param <L> the left element type
     * @param <R> the right element type
     * @return a BiFunction that creates Pair objects
     * @see Pair#of(Object, Object)
     */
    @SuppressWarnings("rawtypes")
    public static <L, R> BiFunction<L, R, Pair<L, R>> pair() {
        return (BiFunction) PAIR;
    }

    /**
     * Returns a TriFunction that creates Triple objects from three elements.
     *
     * @param <L> the left element type
     * @param <M> the middle element type
     * @param <R> the right element type
     * @return a TriFunction that creates Triple objects
     * @see Triple#of(Object, Object, Object)
     */
    @SuppressWarnings("rawtypes")
    public static <L, M, R> TriFunction<L, M, R, Triple<L, M, R>> triple() {
        return (TriFunction) TRIPLE;
    }

    /**
     * Returns a Function that creates Tuple1 objects from a single element.
     *
     * @param <T> the element type
     * @return a Function that creates Tuple1 objects
     * @see Tuple1#of(Object)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Tuple1<T>> tuple1() {
        return (Function) TUPLE_1;
    }

    /**
     * Returns a BiFunction that creates Tuple2 objects from two elements.
     *
     * @param <T> the first element type
     * @param <U> the second element type
     * @return a BiFunction that creates Tuple2 objects
     * @see Tuple2#of(Object, Object)
     */
    @SuppressWarnings("rawtypes")
    public static <T, U> BiFunction<T, U, Tuple2<T, U>> tuple2() {
        return (BiFunction) TUPLE_2;
    }

    /**
     * Returns a TriFunction that creates Tuple3 objects from three elements.
     *
     * @param <A> the first element type
     * @param <B> the second element type
     * @param <C> the third element type
     * @return a TriFunction that creates Tuple3 objects
     * @see Tuple3#of(Object, Object, Object)
     */
    @SuppressWarnings("rawtypes")
    public static <A, B, C> TriFunction<A, B, C, Tuple3<A, B, C>> tuple3() {
        return (TriFunction) TUPLE_3;
    }

    /**
     * Returns a QuadFunction that creates Tuple4 objects from four elements.
     *
     * @param <A> the first element type
     * @param <B> the second element type
     * @param <C> the third element type
     * @param <D> the fourth element type
     * @return a QuadFunction that creates Tuple4 objects
     * @see Tuple4#of(Object, Object, Object, Object)
     */
    @SuppressWarnings({ "rawtypes" })
    public static <A, B, C, D> QuadFunction<A, B, C, D, Tuple4<A, B, C, D>> tuple4() {
        return (QuadFunction) TUPLE_4;
    }

    /**
     * Returns a UnaryOperator that trims strings by removing leading and trailing whitespace.
     *
     * @return a UnaryOperator that trims strings
     * @see String#trim()
     */
    public static UnaryOperator<String> trim() {
        return TRIM;
    }

    /**
     * Returns a UnaryOperator that trims strings and converts null to empty string.
     *
     * @return a UnaryOperator that trims strings to empty
     * @see Strings#trimToEmpty(String)
     */
    public static UnaryOperator<String> trimToEmpty() {
        return TRIM_TO_EMPTY;
    }

    /**
     * Returns a UnaryOperator that trims strings and converts empty results to null.
     *
     * @return a UnaryOperator that trims strings to null
     * @see Strings#trimToNull(String)
     */
    public static UnaryOperator<String> trimToNull() {
        return TRIM_TO_NULL;
    }

    /**
     * Returns a UnaryOperator that strips strings by removing leading and trailing whitespace.
     * Unlike trim(), this method uses Character.isWhitespace() for determining whitespace.
     *
     * @return a UnaryOperator that strips strings
     * @see Strings#strip(String)
     */
    public static UnaryOperator<String> strip() {
        return STRIP;
    }

    /**
     * Returns a UnaryOperator that strips strings and converts null to empty string.
     *
     * @return a UnaryOperator that strips strings to empty
     * @see Strings#stripToEmpty(String)
     */
    public static UnaryOperator<String> stripToEmpty() {
        return STRIP_TO_EMPTY;
    }

    /**
     * Returns a UnaryOperator that strips strings and converts empty results to null.
     *
     * @return a UnaryOperator that strips strings to null
     * @see Strings#stripToNull(String)
     */
    public static UnaryOperator<String> stripToNull() {
        return STRIP_TO_NULL;
    }

    /**
     * Returns a UnaryOperator that converts null strings to empty strings.
     *
     * @return a UnaryOperator that converts null to empty string
     * @see Strings#nullToEmpty(String)
     */
    public static UnaryOperator<String> nullToEmpty() {
        return NULL_TO_EMPTY;
    }

    /**
     * Returns a UnaryOperator that converts null Lists to empty Lists.
     *
     * @param <T> the element type
     * @return a UnaryOperator that converts null to empty List
     * @see N#emptyList()
     */
    @SuppressWarnings("rawtypes")
    public static <T> UnaryOperator<List<T>> nullToEmptyList() {
        return (UnaryOperator) NULL_TO_EMPTY_LIST;
    }

    /**
     * Returns a UnaryOperator that converts null Sets to empty Sets.
     *
     * @param <T> the element type
     * @return a UnaryOperator that converts null to empty Set
     * @see N#emptySet()
     */
    @SuppressWarnings("rawtypes")
    public static <T> UnaryOperator<Set<T>> nullToEmptySet() {
        return (UnaryOperator) NULL_TO_EMPTY_SET;
    }

    /**
     * Returns a UnaryOperator that converts null Maps to empty Maps.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a UnaryOperator that converts null to empty Map
     * @see N#emptyMap()
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> UnaryOperator<Map<K, V>> nullToEmptyMap() {
        return (UnaryOperator) NULL_TO_EMPTY_MAP;
    }

    /**
     * Returns a Function that calculates the length of an array.
     * Returns 0 for null arrays.
     *
     * @param <T> the array element type
     * @return a Function that returns array length
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T[], Integer> len() {
        return (Function) LEN;
    }

    /**
     * Returns a Function that calculates the length of a CharSequence.
     *
     * @param <T> the CharSequence type
     * @return a Function that returns CharSequence length
     * @see CharSequence#length()
     */
    public static <T extends CharSequence> Function<T, Integer> length() {
        return (Function<T, Integer>) LENGTH;
    }

    /**
     * Returns a Function that calculates the size of a Collection.
     *
     * @param <T> the Collection type
     * @return a Function that returns Collection size
     * @see Collection#size()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Function<T, Integer> size() {
        return (Function<T, Integer>) SIZE;
    }

    /**
     * Returns a Function that calculates the size of a Map.
     *
     * @param <T> the Map type
     * @return a Function that returns Map size
     * @see Map#size()
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Function<T, Integer> sizeM() {
        return (Function<T, Integer>) SIZE_MAP;
    }

    /**
     * Returns a Function that casts objects to the specified class.
     * This performs an unchecked cast and should be used with caution.
     *
     * @param <T> the source type
     * @param <U> the target type
     * @param clazz the class to cast to
     * @return a Function that performs type casting
     * @throws IllegalArgumentException if clazz is null
     * @see Class#cast(Object)
     */
    public static <T, U> Function<T, U> cast(final Class<U> clazz) throws IllegalArgumentException {
        N.checkArgNotNull(clazz);

        return t -> clazz.cast(t);
    }

    /**
     * Returns a Predicate that always evaluates to true.
     *
     * @param <T> the type of the input to the predicate
     * @return a Predicate that always returns true
     */
    public static <T> Predicate<T> alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     * Returns a Predicate that always evaluates to false.
     *
     * @param <T> the type of the input to the predicate
     * @return a Predicate that always returns false
     */
    public static <T> Predicate<T> alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Returns a Predicate that tests if the input is null.
     *
     * @param <T> the type of the input to the predicate
     * @return a Predicate that tests for null
     */
    public static <T> Predicate<T> isNull() {
        return IS_NULL;
    }

    /**
     * Returns a Predicate that tests if a value extracted by the valueExtractor is null.
     *
     * @param <T> the type of the input to the predicate
     * @param valueExtractor the function to extract the value to test
     * @return a Predicate that tests if the extracted value is null
     */
    public static <T> Predicate<T> isNull(final java.util.function.Function<T, ?> valueExtractor) {
        return t -> valueExtractor.apply(t) == null;
    }

    /**
     * Returns a Predicate that tests if a CharSequence is null or empty.
     *
     * @param <T> the CharSequence type
     * @return a Predicate that tests for null or empty
     * @see Strings#isEmpty(CharSequence)
     */
    public static <T extends CharSequence> Predicate<T> isEmpty() {
        return (Predicate<T>) IS_EMPTY;
    }

    /**
     * Returns a Predicate that tests if a CharSequence extracted by valueExtractor is empty.
     *
     * @param <T> the type of the input to the predicate
     * @param valueExtractor the function to extract the CharSequence to test
     * @return a Predicate that tests if the extracted value is empty
     */
    public static <T> Predicate<T> isEmpty(final java.util.function.Function<T, ? extends CharSequence> valueExtractor) {
        return t -> Strings.isEmpty(valueExtractor.apply(t));
    }

    /**
     * Returns a Predicate that tests if a CharSequence is null, empty, or contains only whitespace.
     *
     * @param <T> the CharSequence type
     * @return a Predicate that tests for null, empty, or blank
     * @see Strings#isBlank(CharSequence)
     */
    public static <T extends CharSequence> Predicate<T> isBlank() {
        return (Predicate<T>) IS_BLANK;
    }

    /**
     * Returns a Predicate that tests if a CharSequence extracted by valueExtractor is blank.
     *
     * @param <T> the type of the input to the predicate
     * @param valueExtractor the function to extract the CharSequence to test
     * @return a Predicate that tests if the extracted value is blank
     */
    public static <T> Predicate<T> isBlank(final java.util.function.Function<T, ? extends CharSequence> valueExtractor) {
        return t -> Strings.isBlank(valueExtractor.apply(t));
    }

    /**
     * Returns a Predicate that tests if an array is null or empty.
     *
     * @param <T> the array element type
     * @return a Predicate that tests if arrays are empty
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<T[]> isEmptyA() {
        return (Predicate) IS_EMPTY_A;
    }

    /**
     * Returns a Predicate that tests if a Collection is null or empty.
     *
     * @param <T> the Collection type
     * @return a Predicate that tests if Collections are empty
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Predicate<T> isEmptyC() {
        return (Predicate<T>) IS_EMPTY_C;
    }

    /**
     * Returns a Predicate that tests if a Map is null or empty.
     *
     * @param <T> the Map type
     * @return a Predicate that tests if Maps are empty
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Predicate<T> isEmptyM() {
        return (Predicate<T>) IS_EMPTY_M;
    }

    /**
     * Returns a Predicate that tests if the input is not null.
     *
     * @param <T> the type of the input to the predicate
     * @return a Predicate that tests for non-null
     */
    public static <T> Predicate<T> notNull() {
        return NOT_NULL;
    }

    /**
     * Returns a Predicate that tests if a value extracted by the valueExtractor is not null.
     *
     * @param <T> the type of the input to the predicate
     * @param valueExtractor the function to extract the value to test
     * @return a Predicate that tests if the extracted value is not null
     */
    public static <T> Predicate<T> notNull(final java.util.function.Function<T, ?> valueExtractor) {
        return t -> valueExtractor.apply(t) != null;
    }

    /**
     * Returns a Predicate that tests if a CharSequence is not null and not empty.
     *
     * @param <T> the CharSequence type
     * @return a Predicate that tests for non-empty
     * @see Strings#isNotEmpty(CharSequence)
     */
    public static <T extends CharSequence> Predicate<T> notEmpty() {
        return (Predicate<T>) IS_NOT_EMPTY;
    }

    /**
     * Returns a Predicate that tests if a CharSequence extracted by valueExtractor is not empty.
     *
     * @param <T> the type of the input to the predicate
     * @param valueExtractor the function to extract the CharSequence to test
     * @return a Predicate that tests if the extracted value is not empty
     */
    public static <T> Predicate<T> notEmpty(final java.util.function.Function<T, ? extends CharSequence> valueExtractor) {
        return t -> Strings.isNotEmpty(valueExtractor.apply(t));
    }

    /**
     * Returns a Predicate that tests if a CharSequence is not null, not empty, and not blank.
     *
     * @param <T> the CharSequence type
     * @return a Predicate that tests for non-blank
     * @see Strings#isNotBlank(CharSequence)
     */
    public static <T extends CharSequence> Predicate<T> notBlank() {
        return (Predicate<T>) IS_NOT_BLANK;
    }

    /**
     * Returns a Predicate that tests if a CharSequence extracted by valueExtractor is not blank.
     *
     * @param <T> the type of the input to the predicate
     * @param valueExtractor the function to extract the CharSequence to test
     * @return a Predicate that tests if the extracted value is not blank
     */
    public static <T> Predicate<T> notBlank(final java.util.function.Function<T, ? extends CharSequence> valueExtractor) {
        return t -> Strings.isNotBlank(valueExtractor.apply(t));
    }

    /**
     * Returns a Predicate that tests if an array is not null and not empty.
     *
     * @param <T> the array element type
     * @return a Predicate that tests if arrays are not empty
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<T[]> notEmptyA() {
        return (Predicate) NOT_EMPTY_A;
    }

    /**
     * Returns a Predicate that tests if a Collection is not null and not empty.
     *
     * @param <T> the Collection type
     * @return a Predicate that tests if Collections are not empty
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Predicate<T> notEmptyC() {
        return (Predicate<T>) NOT_EMPTY_C;
    }

    /**
     * Returns a Predicate that tests if a Map is not null and not empty.
     *
     * @param <T> the Map type
     * @return a Predicate that tests if Maps are not empty
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Predicate<T> notEmptyM() {
        return (Predicate<T>) NOT_EMPTY_M;
    }

    /**
     * Returns a Predicate that tests if a File is a regular file.
     *
     * @return a Predicate that tests if Files are regular files
     * @see File#isFile()
     */
    public static Predicate<File> isFile() {
        return IS_FILE;
    }

    /**
     * Returns a Predicate that tests if a File is a directory.
     *
     * @return a Predicate that tests if Files are directories
     * @see File#isDirectory()
     */
    public static Predicate<File> isDirectory() {
        return IS_DIRECTORY;
    }

    /**
     * Returns a Predicate that tests if the input equals the target value.
     * Uses N.equals() for null-safe comparison.
     *
     * @param <T> the type of the input to the predicate
     * @param target the value to compare against
     * @return a Predicate that tests for equality with target
     * @see N#equals(Object, Object)
     */
    public static <T> Predicate<T> equal(final Object target) {
        return value -> N.equals(value, target);
    }

    /**
     * Returns a Predicate that tests if the input equals either of two target values.
     *
     * @param <T> the type of the input to the predicate
     * @param targetValue1 the first value to compare against
     * @param targetValue2 the second value to compare against
     * @return a Predicate that tests for equality with either target
     */
    public static <T> Predicate<T> eqOr(final Object targetValue1, final Object targetValue2) {
        return value -> N.equals(value, targetValue1) || N.equals(value, targetValue2);
    }

    /**
     * Returns a Predicate that tests if the input equals any of three target values.
     *
     * @param <T> the type of the input to the predicate
     * @param targetValue1 the first value to compare against
     * @param targetValue2 the second value to compare against
     * @param targetValue3 the third value to compare against
     * @return a Predicate that tests for equality with any target
     */
    public static <T> Predicate<T> eqOr(final Object targetValue1, final Object targetValue2, final Object targetValue3) {
        return value -> N.equals(value, targetValue1) || N.equals(value, targetValue2) || N.equals(value, targetValue3);
    }

    /**
     * Returns a Predicate that tests if the input does not equal the target value.
     * Uses N.equals() for null-safe comparison.
     *
     * @param <T> the type of the input to the predicate
     * @param target the value to compare against
     * @return a Predicate that tests for inequality with target
     * @see N#equals(Object, Object)
     */
    public static <T> Predicate<T> notEqual(final Object target) {
        return value -> !N.equals(value, target);
    }

    /**
     * Returns a Predicate that tests if a Comparable is greater than the target value.
     *
     * @param <T> the type of objects that may be compared
     * @param target the value to compare against
     * @return a Predicate that tests if input > target
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Predicate<T> greaterThan(final T target) {
        return value -> N.compare(value, target) > 0;
    }

    /**
     * Returns a Predicate that tests if a Comparable is greater than or equal to the target value.
     *
     * @param <T> the type of objects that may be compared
     * @param target the value to compare against
     * @return a Predicate that tests if input >= target
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Predicate<T> greaterEqual(final T target) {
        return value -> N.compare(value, target) >= 0;
    }

    /**
     * Returns a Predicate that tests if a Comparable is less than the target value.
     *
     * @param <T> the type of objects that may be compared
     * @param target the value to compare against
     * @return a Predicate that tests if input < target
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Predicate<T> lessThan(final T target) {
        return value -> N.compare(value, target) < 0;
    }

    /**
     * Returns a Predicate that tests if a Comparable is less than or equal to the target value.
     *
     * @param <T> the type of objects that may be compared
     * @param target the value to compare against
     * @return a Predicate that tests if input <= target
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Predicate<T> lessEqual(final T target) {
        return value -> N.compare(value, target) <= 0;
    }

    /**
     * Returns a Predicate that tests if a value is strictly between two bounds.
     * Tests if: minValue < value < maxValue
     *
     * @param <T> the type of objects that may be compared
     * @param minValue the lower bound (exclusive)
     * @param maxValue the upper bound (exclusive)
     * @return a Predicate that tests if minValue < input < maxValue
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Predicate<T> gtAndLt(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }

    /**
     * Returns a Predicate that tests if a value is between two bounds (inclusive lower).
     * Tests if: minValue <= value < maxValue
     *
     * @param <T> the type of objects that may be compared
     * @param minValue the lower bound (inclusive)
     * @param maxValue the upper bound (exclusive)
     * @return a Predicate that tests if minValue <= input < maxValue
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Predicate<T> geAndLt(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) >= 0 && N.compare(value, maxValue) < 0;
    }

    /**
     * Returns a Predicate that tests if a value is between two bounds (both inclusive).
     * Tests if: minValue <= value <= maxValue
     *
     * @param <T> the type of objects that may be compared
     * @param minValue the lower bound (inclusive)
     * @param maxValue the upper bound (inclusive)
     * @return a Predicate that tests if minValue <= input <= maxValue
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Predicate<T> geAndLe(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) >= 0 && N.compare(value, maxValue) <= 0;
    }

    /**
     * Returns a Predicate that tests if a value is between two bounds (inclusive upper).
     * Tests if: minValue < value <= maxValue
     *
     * @param <T> the type of objects that may be compared
     * @param minValue the lower bound (exclusive)
     * @param maxValue the upper bound (inclusive)
     * @return a Predicate that tests if minValue < input <= maxValue
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> Predicate<T> gtAndLe(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) <= 0;
    }

    /**
     * Returns a Predicate that tests if a value is strictly between two bounds.
     * Tests if: minValue < value < maxValue
     *
     * @param <T> the type of objects that may be compared
     * @param minValue the lower bound (exclusive)
     * @param maxValue the upper bound (exclusive)
     * @return a Predicate that tests if minValue < input < maxValue
     * @deprecated replaced by {@code gtAndLt}.
     * @see #gtAndLt(Comparable, Comparable)
     */
    @Deprecated
    public static <T extends Comparable<? super T>> Predicate<T> between(final T minValue, final T maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }

    /**
     * Returns a Predicate that tests if a value is contained in the specified collection.
     * Returns false if the collection is empty.
     *
     * @param <T> the type of the input to the predicate
     * @param c the collection to check membership in
     * @return a Predicate that tests for collection membership
     * @throws IllegalArgumentException if c is null
     * @see Collection#contains(Object)
     */
    public static <T> Predicate<T> in(final Collection<?> c) throws IllegalArgumentException {
        N.checkArgNotNull(c);

        final boolean isNotEmpty = N.notEmpty(c);

        return value -> isNotEmpty && c.contains(value);
    }

    /**
     * Returns a Predicate that tests if a value is not contained in the specified collection.
     * Returns true if the collection is empty.
     *
     * @param <T> the type of the input to the predicate
     * @param c the collection to check membership in
     * @return a Predicate that tests for non-membership in collection
     * @throws IllegalArgumentException if c is null
     * @see Collection#contains(Object)
     */
    public static <T> Predicate<T> notIn(final Collection<?> c) throws IllegalArgumentException {
        N.checkArgNotNull(c);

        final boolean isEmpty = N.isEmpty(c);

        return value -> isEmpty || !c.contains(value);
    }

    /**
     * Returns a Predicate that tests if an object is an instance of the specified class.
     *
     * @param <T> the type of the input to the predicate
     * @param clazz the class to test instance membership
     * @return a Predicate that tests if objects are instances of clazz
     * @throws IllegalArgumentException if clazz is null
     * @see Class#isInstance(Object)
     */
    public static <T> Predicate<T> instanceOf(final Class<?> clazz) throws IllegalArgumentException {
        N.checkArgNotNull(clazz);

        return clazz::isInstance;
    }

    /**
     * Returns a Predicate that tests if a Class is a subtype of the specified class.
     *
     * @param clazz the superclass to test against
     * @return a Predicate that tests if classes are subtypes of clazz
     * @throws IllegalArgumentException if clazz is null
     * @see Class#isAssignableFrom(Class)
     */
    public static Predicate<Class<?>> subtypeOf(final Class<?> clazz) throws IllegalArgumentException {
        N.checkArgNotNull(clazz);

        return clazz::isAssignableFrom;
    }

    /**
     * Returns a Predicate that tests if a String starts with the specified prefix.
     *
     * @param prefix the prefix to test for
     * @return a Predicate that tests if strings start with prefix
     * @throws IllegalArgumentException if prefix is null
     * @see String#startsWith(String)
     */
    public static Predicate<String> startsWith(final String prefix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);

        return value -> value != null && value.startsWith(prefix);
    }

    /**
     * Returns a Predicate that tests if a String ends with the specified suffix.
     *
     * @param suffix the suffix to test for
     * @return a Predicate that tests if strings end with suffix
     * @throws IllegalArgumentException if suffix is null
     * @see String#endsWith(String)
     */
    public static Predicate<String> endsWith(final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(suffix);

        return value -> value != null && value.endsWith(suffix);
    }

    /**
     * Returns a Predicate that tests if a String contains the specified substring.
     *
     * @param valueToFind the substring to search for
     * @return a Predicate that tests if strings contain the substring
     * @throws IllegalArgumentException if valueToFind is null
     * @see String#contains(CharSequence)
     */
    public static Predicate<String> contains(final String valueToFind) throws IllegalArgumentException {
        N.checkArgNotNull(valueToFind);

        return value -> value != null && value.contains(valueToFind);
    }

    /**
     * Returns a Predicate that tests if a String does not start with the specified prefix.
     *
     * @param prefix the prefix to test against
     * @return a Predicate that tests if strings don't start with prefix
     * @throws IllegalArgumentException if prefix is null
     * @see String#startsWith(String)
     */
    public static Predicate<String> notStartsWith(final String prefix) throws IllegalArgumentException {
        N.checkArgNotNull(prefix);

        return value -> value == null || !value.startsWith(prefix);
    }

    /**
     * Returns a Predicate that tests if a String does not end with the specified suffix.
     *
     * @param suffix the suffix to test against
     * @return a Predicate that tests if strings don't end with suffix
     * @throws IllegalArgumentException if suffix is null
     * @see String#endsWith(String)
     */
    public static Predicate<String> notEndsWith(final String suffix) throws IllegalArgumentException {
        N.checkArgNotNull(suffix);

        return value -> value == null || !value.endsWith(suffix);
    }

    /**
     * Returns a Predicate that tests if a String does not contain the specified substring.
     *
     * @param str the substring to test against
     * @return a Predicate that tests if strings don't contain the substring
     * @throws IllegalArgumentException if str is null
     * @see String#contains(CharSequence)
     */
    public static Predicate<String> notContains(final String str) throws IllegalArgumentException {
        N.checkArgNotNull(str);

        return value -> value == null || !value.contains(str);
    }

    /**
     * Returns a Predicate that tests if a CharSequence matches the specified Pattern.
     *
     * @param pattern the Pattern to match against
     * @return a Predicate that tests if CharSequences match the pattern
     * @throws IllegalArgumentException if pattern is null
     * @see Pattern#matcher(CharSequence)
     * @see Matcher#find()
     */
    public static Predicate<CharSequence> matches(final Pattern pattern) throws IllegalArgumentException {
        N.checkArgNotNull(pattern);

        return value -> pattern.matcher(value).find();
    }

    /**
     * Returns a BiPredicate that tests if two objects are equal using N.equals().
     *
     * @param <T> the type of the first object
     * @param <U> the type of the second object
     * @return a BiPredicate that tests for equality
     * @see N#equals(Object, Object)
     */
    public static <T, U> BiPredicate<T, U> equal() {
        return BiPredicates.EQUAL;
    }

    /**
     * Returns a BiPredicate that tests if two objects are not equal using N.equals().
     *
     * @param <T> the type of the first object
     * @param <U> the type of the second object
     * @return a BiPredicate that tests for inequality
     * @see N#equals(Object, Object)
     */
    public static <T, U> BiPredicate<T, U> notEqual() {
        return BiPredicates.NOT_EQUAL;
    }

    /**
     * Returns a BiPredicate that tests if the first Comparable is greater than the second.
     *
     * @param <T> the type of objects that may be compared
     * @return a BiPredicate that tests if first > second
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> greaterThan() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_THAN;
    }

    /**
     * Returns a BiPredicate that tests if the first Comparable is greater than or equal to the second.
     *
     * @param <T> the type of objects that may be compared
     * @return a BiPredicate that tests if first >= second
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> greaterEqual() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_EQUAL;
    }

    /**
     * Returns a BiPredicate that tests if the first Comparable is less than the second.
     *
     * @param <T> the type of objects that may be compared
     * @return a BiPredicate that tests if first < second
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> lessThan() {
        return (BiPredicate<T, T>) BiPredicates.LESS_THAN;
    }

    /**
     * Returns a BiPredicate that tests if the first Comparable is less than or equal to the second.
     *
     * @param <T> the type of objects that may be compared
     * @return a BiPredicate that tests if first <= second
     * @see N#compare(Comparable, Comparable)
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> lessEqual() {
        return (BiPredicate<T, T>) BiPredicates.LESS_EQUAL;
    }

    /**
     * Returns a Predicate that negates the result of the specified predicate.
     *
     * @param <T> the type of the input to the predicate
     * @param predicate the predicate to negate
     * @return a Predicate that returns the opposite of the input predicate
     * @throws IllegalArgumentException if predicate is null
     */
    public static <T> Predicate<T> not(final java.util.function.Predicate<T> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return t -> !predicate.test(t);
    }

    /**
     * Returns a BiPredicate that negates the result of the specified bi-predicate.
     *
     * @param <T> the type of the first input to the predicate
     * @param <U> the type of the second input to the predicate
     * @param biPredicate the bi-predicate to negate
     * @return a BiPredicate that returns the opposite of the input bi-predicate
     * @throws IllegalArgumentException if biPredicate is null
     */
    public static <T, U> BiPredicate<T, U> not(final java.util.function.BiPredicate<T, U> biPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(biPredicate);

        return (t, u) -> !biPredicate.test(t, u);
    }

    /**
     * Returns a TriPredicate that negates the result of the specified tri-predicate.
     *
     * @param <A> the type of the first input to the predicate
     * @param <B> the type of the second input to the predicate
     * @param <C> the type of the third input to the predicate
     * @param triPredicate the tri-predicate to negate
     * @return a TriPredicate that returns the opposite of the input tri-predicate
     * @throws IllegalArgumentException if triPredicate is null
     */
    public static <A, B, C> TriPredicate<A, B, C> not(final TriPredicate<A, B, C> triPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(triPredicate);

        return (a, b, c) -> !triPredicate.test(a, b, c);
    }

    /**
     * Returns a BooleanSupplier that performs logical AND on two boolean suppliers.
     *
     * @param first the first boolean supplier
     * @param second the second boolean supplier
     * @return a BooleanSupplier that returns first AND second
     * @throws IllegalArgumentException if first or second is null
     */
    public static BooleanSupplier and(final java.util.function.BooleanSupplier first, final java.util.function.BooleanSupplier second)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return () -> first.getAsBoolean() && second.getAsBoolean();
    }

    /**
     * Returns a BooleanSupplier that performs logical AND on three boolean suppliers.
     *
     * @param first the first boolean supplier
     * @param second the second boolean supplier
     * @param third the third boolean supplier
     * @return a BooleanSupplier that returns first AND second AND third
     * @throws IllegalArgumentException if any supplier is null
     */
    public static BooleanSupplier and(final java.util.function.BooleanSupplier first, final java.util.function.BooleanSupplier second,
            final java.util.function.BooleanSupplier third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return () -> first.getAsBoolean() && second.getAsBoolean() && third.getAsBoolean();
    }

    /**
     * Returns a Predicate that performs logical AND on two predicates.
     *
     * @param <T> the type of the input to the predicate
     * @param first the first predicate
     * @param second the second predicate
     * @return a Predicate that returns first AND second
     * @throws IllegalArgumentException if first or second is null
     */
    public static <T> Predicate<T> and(final java.util.function.Predicate<? super T> first, final java.util.function.Predicate<? super T> second)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return t -> first.test(t) && second.test(t);
    }

    /**
     * Returns a Predicate that performs logical AND on three predicates.
     *
     * @param <T> the type of the input to the predicate
     * @param first the first predicate
     * @param second the second predicate
     * @param third the third predicate
     * @return a Predicate that returns first AND second AND third
     * @throws IllegalArgumentException if any predicate is null
     */
    public static <T> Predicate<T> and(final java.util.function.Predicate<? super T> first, final java.util.function.Predicate<? super T> second,
            final java.util.function.Predicate<? super T> third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return t -> first.test(t) && second.test(t) && third.test(t);
    }

    /**
     * Returns a Predicate that performs logical AND on all predicates in the collection.
     *
     * @param <T> the type of the input to the predicate
     * @param c the collection of predicates
     * @return a Predicate that returns true only if all predicates return true
     * @throws IllegalArgumentException if the collection is null or empty
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
     * Returns a BiPredicate that performs logical AND on two bi-predicates.
     *
     * @param <T> the type of the first input to the predicate
     * @param <U> the type of the second input to the predicate
     * @param first the first bi-predicate
     * @param second the second bi-predicate
     * @return a BiPredicate that returns first AND second
     * @throws IllegalArgumentException if first or second is null
     */
    public static <T, U> BiPredicate<T, U> and(final java.util.function.BiPredicate<? super T, ? super U> first,
            final java.util.function.BiPredicate<? super T, ? super U> second) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return (t, u) -> first.test(t, u) && second.test(t, u);
    }

    /**
     * Returns a BiPredicate that performs logical AND on three bi-predicates.
     *
     * @param <T> the type of the first input to the predicate
     * @param <U> the type of the second input to the predicate
     * @param first the first bi-predicate
     * @param second the second bi-predicate
     * @param third the third bi-predicate
     * @return a BiPredicate that returns first AND second AND third
     * @throws IllegalArgumentException if any bi-predicate is null
     */
    public static <T, U> BiPredicate<T, U> and(final java.util.function.BiPredicate<? super T, ? super U> first,
            final java.util.function.BiPredicate<? super T, ? super U> second, final java.util.function.BiPredicate<? super T, ? super U> third)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return (t, u) -> first.test(t, u) && second.test(t, u) && third.test(t, u);
    }

    /**
     * Returns a BiPredicate that performs logical AND on all bi-predicates in the list.
     *
     * @param <T> the type of the first input to the predicate
     * @param <U> the type of the second input to the predicate
     * @param c the list of bi-predicates
     * @return a BiPredicate that returns true only if all bi-predicates return true
     * @throws IllegalArgumentException if the list is null or empty
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
     * Returns a BooleanSupplier that performs logical OR on two boolean suppliers.
     *
     * @param first the first boolean supplier
     * @param second the second boolean supplier
     * @return a BooleanSupplier that returns first OR second
     * @throws IllegalArgumentException if first or second is null
     */
    public static BooleanSupplier or(final java.util.function.BooleanSupplier first, final java.util.function.BooleanSupplier second)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return () -> first.getAsBoolean() || second.getAsBoolean();
    }

    /**
     * Returns a BooleanSupplier that performs logical OR on three boolean suppliers.
     *
     * @param first the first boolean supplier
     * @param second the second boolean supplier
     * @param third the third boolean supplier
     * @return a BooleanSupplier that returns first OR second OR third
     * @throws IllegalArgumentException if any supplier is null
     */
    public static BooleanSupplier or(final java.util.function.BooleanSupplier first, final java.util.function.BooleanSupplier second,
            final java.util.function.BooleanSupplier third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return () -> first.getAsBoolean() || second.getAsBoolean() || third.getAsBoolean();
    }

    /**
     * Returns a Predicate that performs logical OR on two predicates.
     *
     * @param <T> the type of the input to the predicate
     * @param first the first predicate
     * @param second the second predicate
     * @return a Predicate that returns first OR second
     * @throws IllegalArgumentException if first or second is null
     */
    public static <T> Predicate<T> or(final java.util.function.Predicate<? super T> first, final java.util.function.Predicate<? super T> second)
            throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return t -> first.test(t) || second.test(t);
    }

    /**
     * Returns a Predicate that performs logical OR on three predicates.
     *
     * @param <T> the type of the input to the predicate
     * @param first the first predicate
     * @param second the second predicate
     * @param third the third predicate
     * @return a Predicate that returns first OR second OR third
     * @throws IllegalArgumentException if any predicate is null
     */
    public static <T> Predicate<T> or(final java.util.function.Predicate<? super T> first, final java.util.function.Predicate<? super T> second,
            final java.util.function.Predicate<? super T> third) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return t -> first.test(t) || second.test(t) || third.test(t);
    }

    /**
     * Returns a Predicate that performs logical OR on all predicates in the collection.
     *
     * @param <T> the type of the input to the predicate
     * @param c the collection of predicates
     * @return a Predicate that returns true if any predicate returns true
     * @throws IllegalArgumentException if the collection is null or empty
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
     * Returns a BiPredicate that performs logical OR on two bi-predicates.
     *
     * @param <T> the type of the first input to the predicate
     * @param <U> the type of the second input to the predicate
     * @param first the first bi-predicate
     * @param second the second bi-predicate
     * @return a BiPredicate that returns first OR second
     * @throws IllegalArgumentException if first or second is null
     */
    public static <T, U> BiPredicate<T, U> or(final java.util.function.BiPredicate<? super T, ? super U> first,
            final java.util.function.BiPredicate<? super T, ? super U> second) throws IllegalArgumentException {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return (t, u) -> first.test(t, u) || second.test(t, u);
    }

    /**
     * Returns a BiPredicate that performs logical OR on three bi-predicates.
     *
     * @param <T> the type of the first input to the predicate
     * @param <U> the type of the second input to the predicate
     * @param first the first bi-predicate
     * @param second the second bi-predicate
     * @param third the third bi-predicate
     * @return a BiPredicate that returns first OR second OR third
     * @throws IllegalArgumentException if any bi-predicate is null
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
     * Returns a BiPredicate that performs logical OR on all bi-predicates in the list.
     *
     * @param <T> the type of the first input to the predicate
     * @param <U> the type of the second input to the predicate
     * @param c the list of bi-predicates
     * @return a BiPredicate that returns true if any bi-predicate returns true
     * @throws IllegalArgumentException if the list is null or empty
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
     * Returns a Predicate for Map.Entry that tests the key using the specified predicate.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate the predicate to apply to the key
     * @return a Predicate that tests Map.Entry keys
     * @throws IllegalArgumentException if predicate is null
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testByKey(final java.util.function.Predicate<? super K> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return entry -> predicate.test(entry.getKey());
    }

    /**
     * Returns a Predicate for Map.Entry that tests the value using the specified predicate.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate the predicate to apply to the value
     * @return a Predicate that tests Map.Entry values
     * @throws IllegalArgumentException if predicate is null
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testByValue(final java.util.function.Predicate<? super V> predicate) throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return entry -> predicate.test(entry.getValue());
    }

    /**
     * Returns a Consumer for Map.Entry that applies the specified consumer to the key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer the consumer to apply to the key
     * @return a Consumer that operates on Map.Entry keys
     * @throws IllegalArgumentException if consumer is null
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptByKey(final java.util.function.Consumer<? super K> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer);

        return entry -> consumer.accept(entry.getKey());
    }

    /**
     * Returns a Consumer for Map.Entry that applies the specified consumer to the value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer the consumer to apply to the value
     * @return a Consumer that operates on Map.Entry values
     * @throws IllegalArgumentException if consumer is null
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptByValue(final java.util.function.Consumer<? super V> consumer) throws IllegalArgumentException {
        N.checkArgNotNull(consumer);

        return entry -> consumer.accept(entry.getValue());
    }

    /**
     * Returns a Function for Map.Entry that applies the specified function to the key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     * @param func the function to apply to the key
     * @return a Function that transforms Map.Entry keys
     * @throws IllegalArgumentException if func is null
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyByKey(final java.util.function.Function<? super K, ? extends R> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> func.apply(entry.getKey());
    }

    /**
     * Returns a Function for Map.Entry that applies the specified function to the value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     * @param func the function to apply to the value
     * @return a Function that transforms Map.Entry values
     * @throws IllegalArgumentException if func is null
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyByValue(final java.util.function.Function<? super V, ? extends R> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> func.apply(entry.getValue());
    }

    /**
     * Returns a Function that transforms a Map.Entry by applying a function to its key.
     * The value remains unchanged.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <KK> the new key type
     * @param func the function to transform the key
     * @return a Function that creates new Map.Entry with transformed key
     * @throws IllegalArgumentException if func is null
     */
    public static <K, V, KK> Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapKey(final java.util.function.Function<? super K, ? extends KK> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> new ImmutableEntry<>(func.apply(entry.getKey()), entry.getValue());
    }

    /**
     * Returns a Function that transforms a Map.Entry by applying a function to its value.
     * The key remains unchanged.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <VV> the new value type
     * @param func the function to transform the value
     * @return a Function that creates new Map.Entry with transformed value
     * @throws IllegalArgumentException if func is null
     */
    public static <K, V, VV> Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapValue(final java.util.function.Function<? super V, ? extends VV> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> new ImmutableEntry<>(entry.getKey(), func.apply(entry.getValue()));
    }

    /**
     * Returns a Predicate for Map.Entry that tests both key and value using a BiPredicate.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate the bi-predicate to test key and value
     * @return a Predicate that tests Map.Entry using key and value
     * @throws IllegalArgumentException if predicate is null
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testKeyVal(final java.util.function.BiPredicate<? super K, ? super V> predicate)
            throws IllegalArgumentException {
        N.checkArgNotNull(predicate);

        return entry -> predicate.test(entry.getKey(), entry.getValue());
    }

    /**
     * Returns a Consumer for Map.Entry that accepts both key and value using a BiConsumer.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer the bi-consumer to accept key and value
     * @return a Consumer that operates on Map.Entry key and value
     * @throws IllegalArgumentException if consumer is null
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptKeyVal(final java.util.function.BiConsumer<? super K, ? super V> consumer)
            throws IllegalArgumentException {
        N.checkArgNotNull(consumer);

        return entry -> consumer.accept(entry.getKey(), entry.getValue());
    }

    /**
     * Returns a Function for Map.Entry that applies a BiFunction to both key and value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R> the result type
     * @param func the bi-function to apply to key and value
     * @return a Function that transforms Map.Entry using key and value
     * @throws IllegalArgumentException if func is null
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyKeyVal(final java.util.function.BiFunction<? super K, ? super V, ? extends R> func)
            throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return entry -> func.apply(entry.getKey(), entry.getValue());
    }

    /**
     * Returns a Consumer that only accepts non-null values.
     * If the value is null, the consumer is not invoked.
     *
     * @param <T> the type of the input to the consumer
     * @param consumer the consumer to invoke for non-null values
     * @return a Consumer that only processes non-null values
     * @throws IllegalArgumentException if consumer is null
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
     * Returns a Consumer that only accepts values that satisfy the predicate.
     *
     * @param <T> the type of the input to the consumer
     * @param predicate the condition to test
     * @param consumer the consumer to invoke when predicate is true
     * @return a Consumer that conditionally processes values
     * @throws IllegalArgumentException if predicate or consumer is null
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
     * Returns a Consumer that accepts values based on a predicate, with different consumers for true/false.
     *
     * @param <T> the type of the input to the consumer
     * @param predicate the condition to test
     * @param consumerForTrue the consumer to invoke when predicate is true
     * @param consumerForFalse the consumer to invoke when predicate is false
     * @return a Consumer that conditionally processes values
     * @throws IllegalArgumentException if any parameter is null
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
     * Returns a Function that applies a mapper and returns an empty list if the input is null.
     *
     * @param <T> the type of the input
     * @param <R> the element type of the result collection
     * @param mapper the function to apply to non-null inputs
     * @return a Function that safely handles null inputs
     */
    @Beta
    public static <T, R> Function<T, Collection<R>> applyIfNotNullOrEmpty(final java.util.function.Function<T, ? extends Collection<R>> mapper) {
        return t -> t == null ? N.emptyList() : mapper.apply(t);
    }

    /**
     * Returns a Function that applies two mappers in sequence, returning a default value if any step produces null.
     *
     * @param <A> the type of the input
     * @param <B> the intermediate type
     * @param <R> the result type
     * @param mapperA the first mapper
     * @param mapperB the second mapper
     * @param defaultValue the default value to return if any step is null
     * @return a Function with null-safe chaining
     * @throws IllegalArgumentException if mapperA or mapperB is null
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
     * Returns a Function that applies three mappers in sequence, returning a default value if any step produces null.
     *
     * @param <A> the type of the input
     * @param <B> the first intermediate type
     * @param <C> the second intermediate type
     * @param <R> the result type
     * @param mapperA the first mapper
     * @param mapperB the second mapper
     * @param mapperC the third mapper
     * @param defaultValue the default value to return if any step is null
     * @return a Function with null-safe chaining
     * @throws IllegalArgumentException if any mapper is null
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
     * Returns a Function that applies four mappers in sequence, returning a default value if any step produces null.
     *
     * @param <A> the type of the input
     * @param <B> the first intermediate type
     * @param <C> the second intermediate type
     * @param <D> the third intermediate type
     * @param <R> the result type
     * @param mapperA the first mapper
     * @param mapperB the second mapper
     * @param mapperC the third mapper
     * @param mapperD the fourth mapper
     * @param defaultValue the default value to return if any step is null
     * @return a Function with null-safe chaining
     * @throws IllegalArgumentException if any mapper is null
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
     * Returns a Function that applies two mappers in sequence, using a supplier for the default value if any step produces null.
     *
     * @param <A> the type of the input
     * @param <B> the intermediate type
     * @param <R> the result type
     * @param mapperA the first mapper
     * @param mapperB the second mapper
     * @param supplier the supplier for the default value
     * @return a Function with null-safe chaining
     * @throws IllegalArgumentException if mapperA or mapperB is null
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
     * Returns a Function that applies three mappers in sequence, using a supplier for the default value if any step produces null.
     *
     * @param <A> the type of the input
     * @param <B> the first intermediate type
     * @param <C> the second intermediate type
     * @param <R> the result type
     * @param mapperA the first mapper
     * @param mapperB the second mapper
     * @param mapperC the third mapper
     * @param supplier the supplier for the default value
     * @return a Function with null-safe chaining
     * @throws IllegalArgumentException if any mapper is null
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
     * Returns a Function that applies four mappers in sequence, using a supplier for the default value if any step produces null.
     *
     * @param <A> the type of the input
     * @param <B> the first intermediate type
     * @param <C> the second intermediate type
     * @param <D> the third intermediate type
     * @param <R> the result type
     * @param mapperA the first mapper
     * @param mapperB the second mapper
     * @param mapperC the third mapper
     * @param mapperD the fourth mapper
     * @param supplier the supplier for the default value
     * @return a Function with null-safe chaining
     * @throws IllegalArgumentException if any mapper is null
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
     * Returns a Function that conditionally applies a function based on a predicate, with a default value.
     *
     * @param <T> the type of the input
     * @param <R> the result type
     * @param predicate the condition to test
     * @param func the function to apply when predicate is true
     * @param defaultValue the value to return when predicate is false
     * @return a Function that conditionally applies transformation
     * @throws IllegalArgumentException if predicate or func is null
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
     * Returns a Function that conditionally applies a function based on a predicate, with a supplier for the else value.
     *
     * @param <T> the type of the input
     * @param <R> the result type
     * @param predicate the condition to test
     * @param func the function to apply when predicate is true
     * @param supplier the supplier for the value when predicate is false
     * @return a Function that conditionally applies transformation
     * @throws IllegalArgumentException if any parameter is null
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

    /**
     * Returns a Function that flattens a Map with Collection values into a List of Maps.
     * Each output Map contains one key-value pair from the original Map.
     * 
     * @implSpec 
     * {a=[1, 2, 3], b=[4, 5, 6], c=[7, 8]} -> [{a=1, b=4, c=7}, {a=2, b=5, c=8}, {a=3, b=6}].
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a Function that flattens Maps with Collection values
     * @see Maps#flatToMap(Map)
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Map<K, ? extends Collection<V>>, List<Map<K, V>>> flatmapValue() {
        return (Function) FLAT_MAP_VALUE_FUNC;
    }

    /**
     * Returns a ToByteFunction that parses Strings to byte values.
     *
     * @return a ToByteFunction that parses strings
     * @see Numbers#toByte(String)
     */
    public static ToByteFunction<String> parseByte() {
        return PARSE_BYTE_FUNC;
    }

    /**
     * Returns a ToShortFunction that parses Strings to short values.
     *
     * @return a ToShortFunction that parses strings
     * @see Numbers#toShort(String)
     */
    public static ToShortFunction<String> parseShort() {
        return PARSE_SHORT_FUNC;
    }

    /**
     * Returns a ToIntFunction that parses Strings to int values.
     *
     * @return a ToIntFunction that parses strings
     * @see Numbers#toInt(String)
     */
    public static ToIntFunction<String> parseInt() {
        return PARSE_INT_FUNC;
    }

    /**
     * Returns a ToLongFunction that parses Strings to long values.
     *
     * @return a ToLongFunction that parses strings
     * @see Numbers#toLong(String)
     */
    public static ToLongFunction<String> parseLong() {
        return PARSE_LONG_FUNC;
    }

    /**
     * Returns a ToFloatFunction that parses Strings to float values.
     *
     * @return a ToFloatFunction that parses strings
     * @see Numbers#toFloat(String)
     */
    public static ToFloatFunction<String> parseFloat() {
        return PARSE_FLOAT_FUNC;
    }

    /**
     * Returns a ToDoubleFunction that parses Strings to double values.
     *
     * @return a ToDoubleFunction that parses strings
     * @see Numbers#toDouble(String)
     */
    public static ToDoubleFunction<String> parseDouble() {
        return PARSE_DOUBLE_FUNC;
    }

    /**
     * Returns a Function that creates Number objects from Strings.
     * Returns null for empty strings. The type of Number returned depends on the string format.
     *
     * @return a Function that creates Number objects
     * @see Numbers#createNumber(String)
     */
    public static Function<String, Number> createNumber() {
        return CREATE_NUMBER_FUNC;
    }

    /**
     * Returns a ToIntFunction that converts Number objects to int values.
     *
     * @param <T> the Number type
     * @return a ToIntFunction that converts Numbers to int
     * @see Number#intValue()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToIntFunction<T> numToInt() {
        return (ToIntFunction) ToIntFunction.FROM_NUM;
    }

    /**
     * Returns a ToLongFunction that converts Number objects to long values.
     *
     * @param <T> the Number type
     * @return a ToLongFunction that converts Numbers to long
     * @see Number#longValue()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToLongFunction<T> numToLong() {
        return (ToLongFunction) ToLongFunction.FROM_NUM;
    }

    /**
     * Returns a ToDoubleFunction that converts Number objects to double values.
     *
     * @param <T> the Number type
     * @return a ToDoubleFunction that converts Numbers to double
     * @see Number#doubleValue()
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToDoubleFunction<T> numToDouble() {
        return (ToDoubleFunction) ToDoubleFunction.FROM_NUM;
    }

    /**
     * Returns a stateful Predicate that limits the number of elements that pass through.
     * The first 'count' elements return true, all subsequent elements return false.
     * Don't save or cache for reuse, but it can be used in parallel stream.
     *
     * @param <T> the type of the input to the predicate
     * @param count the maximum number of elements to accept
     * @return a stateful Predicate that limits elements
     * @throws IllegalArgumentException if count is negative
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

    /** The Constant RETURN_FIRST. */
    private static final BinaryOperator<Object> RETURN_FIRST = (a, b) -> a;

    /** The Constant RETURN_SECOND. */
    private static final BinaryOperator<Object> RETURN_SECOND = (a, b) -> b;

    /** The Constant MIN. */
    @SuppressWarnings({ "rawtypes" })
    private static final BinaryOperator<Comparable> MIN = (a, b) -> Comparators.NULL_LAST_COMPARATOR.compare(a, b) <= 0 ? a : b;

    /** The Constant MIN_BY_KEY. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Map.Entry<Comparable, Object>> MIN_BY_KEY = new BinaryOperator<>() {
        private final Comparator<Comparable> cmp = Comparators.NULL_LAST_COMPARATOR;

        @Override
        public Entry<Comparable, Object> apply(final Entry<Comparable, Object> a, final Entry<Comparable, Object> b) {
            return cmp.compare(a.getKey(), b.getKey()) <= 0 ? a : b;
        }
    };

    /** The Constant MIN_BY_VALUE. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Map.Entry<Object, Comparable>> MIN_BY_VALUE = new BinaryOperator<>() {
        private final Comparator<Comparable> cmp = Comparators.NULL_LAST_COMPARATOR;

        @Override
        public Entry<Object, Comparable> apply(final Entry<Object, Comparable> a, final Entry<Object, Comparable> b) {
            return cmp.compare(a.getValue(), b.getValue()) <= 0 ? a : b;
        }
    };

    /** The Constant MAX. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Comparable> MAX = (a, b) -> Comparators.NULL_FIRST_COMPARATOR.compare(a, b) >= 0 ? a : b;

    /** The Constant MAX_BY_KEY. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Map.Entry<Comparable, Object>> MAX_BY_KEY = new BinaryOperator<>() {
        private final Comparator<Comparable> cmp = Comparators.NULL_FIRST_COMPARATOR;

        @Override
        public Entry<Comparable, Object> apply(final Entry<Comparable, Object> a, final Entry<Comparable, Object> b) {
            return cmp.compare(a.getKey(), b.getKey()) >= 0 ? a : b;
        }
    };

    /** The Constant MAX_BY_VALUE. */
    @SuppressWarnings("rawtypes")
    private static final BinaryOperator<Map.Entry<Object, Comparable>> MAX_BY_VALUE = new BinaryOperator<>() {
        private final Comparator<Comparable> cmp = Comparators.NULL_FIRST_COMPARATOR;

        @Override
        public Entry<Object, Comparable> apply(final Entry<Object, Comparable> a, final Entry<Object, Comparable> b) {
            return cmp.compare(a.getValue(), b.getValue()) >= 0 ? a : b;
        }
    };

    private static final Function<Future<Object>, Object> FUTURE_GETTER = f -> {
        try {
            return f.get();
        } catch (InterruptedException | ExecutionException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    };

    /**
     * Returns a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * 
     * <p>The predicate limits the number of elements that can pass through. Once the limit is reached,
     * all subsequent elements will fail the test.
     *
     * @param <T> the type of the input to the predicate
     * @param limit the maximum number of elements that can pass the predicate
     * @param predicate the predicate to test elements after checking the limit
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException if limit is negative or predicate is null
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
     * Returns a stateful {@code BiPredicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * 
     * <p>The bi-predicate limits the number of element pairs that can pass through. Once the limit is reached,
     * all subsequent pairs will fail the test.
     *
     * @param <T> the type of the first input to the bi-predicate
     * @param <U> the type of the second input to the bi-predicate
     * @param limit the maximum number of element pairs that can pass the bi-predicate
     * @param predicate the bi-predicate to test element pairs after checking the limit
     * @return a stateful {@code BiPredicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException if limit is negative or predicate is null
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
     * <p>The predicate first tests elements with the given predicate, and only allows a limited number
     * of elements that pass the predicate to return true.
     *
     * @param <T> the type of the input to the predicate
     * @param predicate the predicate to test elements before applying the limit
     * @param limit the maximum number of elements that pass the predicate to allow through
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException if predicate is null or limit is negative
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
     * Returns a stateful {@code BiPredicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * 
     * <p>The bi-predicate first tests element pairs with the given bi-predicate, and only allows a limited number
     * of pairs that pass the bi-predicate to return true.
     *
     * @param <T> the type of the first input to the bi-predicate
     * @param <U> the type of the second input to the bi-predicate
     * @param predicate the bi-predicate to test element pairs before applying the limit
     * @param limit the maximum number of element pairs that pass the bi-predicate to allow through
     * @return a stateful {@code BiPredicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException if predicate is null or limit is negative
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
     * <p>The predicate allows elements to pass for a specified duration in milliseconds.
     * After the time limit expires, all subsequent elements will fail the test.
     *
     * @param <T> the type of the input to the predicate
     * @param timeInMillis the time limit in milliseconds
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException if timeInMillis is negative
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
     * <p>The predicate allows elements to pass for a specified duration.
     * After the time limit expires, all subsequent elements will fail the test.
     *
     * @param <T> the type of the input to the predicate
     * @param duration the time limit as a Duration
     * @return a stateful {@code Predicate}. Don't save or cache for reuse, but it can be used in parallel stream.
     * @throws IllegalArgumentException if duration is null
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> timeLimit(final Duration duration) throws IllegalArgumentException {
        N.checkArgNotNull(duration, cs.duration);

        return timeLimit(duration.toMillis());
    }

    /**
     * Returns a stateful {@code Function}. Don't save or cache for reuse or use it in parallel stream.
     * 
     * <p>The function wraps each element with its index, starting from 0.
     * This is useful for tracking the position of elements during stream operations.
     *
     * @param <T> the type of the input elements
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
     * <p>The predicate tests elements along with their index position using the provided IntObjPredicate.
     * The index starts from 0 and increments for each element tested.
     *
     * @param <T> the type of the input to the predicate
     * @param predicate the predicate that tests elements along with their indices
     * @return a stateful {@code Predicate}. Don't save or cache for reuse or use it in parallel stream.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Predicate<T> indexed(final IntObjPredicate<T> predicate) {
        return Predicates.indexed(predicate);
    }

    /**
     * Returns a BinaryOperator that always returns the first argument.
     * 
     * <p>This is useful in reduction operations where you want to keep the first occurrence
     * of duplicate elements.
     *
     * @param <T> the type of the operands and result
     * @return a BinaryOperator that returns the first argument
     */
    public static <T> BinaryOperator<T> selectFirst() {
        return (BinaryOperator<T>) RETURN_FIRST;
    }

    /**
     * Returns a BinaryOperator that always returns the second argument.
     * 
     * <p>This is useful in reduction operations where you want to keep the last occurrence
     * of duplicate elements.
     *
     * @param <T> the type of the operands and result
     * @return a BinaryOperator that returns the second argument
     */
    public static <T> BinaryOperator<T> selectSecond() {
        return (BinaryOperator<T>) RETURN_SECOND;
    }

    /**
     * Returns a BinaryOperator that finds the minimum of two Comparable values.
     * 
     * <p>Null values are considered greater than non-null values.
     *
     * @param <T> the type of the Comparable operands and result
     * @return a BinaryOperator that returns the minimum value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Comparable<? super T>> BinaryOperator<T> min() {
        return (BinaryOperator) MIN;
    }

    /**
     * Returns a BinaryOperator that finds the minimum of two values using the given Comparator.
     *
     * @param <T> the type of the operands and result
     * @param comparator the Comparator to determine the minimum
     * @return a BinaryOperator that returns the minimum value according to the comparator
     * @throws IllegalArgumentException if comparator is null
     */
    public static <T> BinaryOperator<T> min(final Comparator<? super T> comparator) throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        return (a, b) -> comparator.compare(a, b) <= 0 ? a : b;
    }

    /**
     * Returns a BinaryOperator that finds the minimum of two values by comparing a key extracted from each.
     * 
     * <p>The key must be Comparable. Null keys are considered greater than non-null keys.
     *
     * @param <T> the type of the operands and result
     * @param keyExtractor the function to extract the Comparable key
     * @return a BinaryOperator that returns the element with the minimum key
     * @throws IllegalArgumentException if keyExtractor is null
     */
    @SuppressWarnings("rawtypes")
    public static <T> BinaryOperator<T> minBy(final java.util.function.Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        final Comparator<? super T> comparator = Comparators.nullsLastBy(keyExtractor);

        return (a, b) -> comparator.compare(a, b) <= 0 ? a : b;
    }

    /**
     * Returns a BinaryOperator for Map.Entry that finds the entry with the minimum key.
     * 
     * <p>Keys must be Comparable. Null keys are considered greater than non-null keys.
     *
     * @param <K> the type of the Comparable keys
     * @param <V> the type of the values
     * @return a BinaryOperator that returns the entry with the minimum key
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K extends Comparable<? super K>, V> BinaryOperator<Map.Entry<K, V>> minByKey() {
        return (BinaryOperator) MIN_BY_KEY;
    }

    /**
     * Returns a BinaryOperator for Map.Entry that finds the entry with the minimum value.
     * 
     * <p>Values must be Comparable. Null values are considered greater than non-null values.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the Comparable values
     * @return a BinaryOperator that returns the entry with the minimum value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V extends Comparable<? super V>> BinaryOperator<Map.Entry<K, V>> minByValue() {
        return (BinaryOperator) MIN_BY_VALUE;
    }

    /**
     * Returns a BinaryOperator that finds the maximum of two Comparable values.
     * 
     * <p>Null values are considered less than non-null values.
     *
     * @param <T> the type of the Comparable operands and result
     * @return a BinaryOperator that returns the maximum value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Comparable<? super T>> BinaryOperator<T> max() {
        return (BinaryOperator) MAX;
    }

    /**
     * Returns a BinaryOperator that finds the maximum of two values using the given Comparator.
     *
     * @param <T> the type of the operands and result
     * @param comparator the Comparator to determine the maximum
     * @return a BinaryOperator that returns the maximum value according to the comparator
     * @throws IllegalArgumentException if comparator is null
     */
    public static <T> BinaryOperator<T> max(final Comparator<? super T> comparator) throws IllegalArgumentException {
        N.checkArgNotNull(comparator);

        return (a, b) -> comparator.compare(a, b) >= 0 ? a : b;
    }

    /**
     * Returns a BinaryOperator that finds the maximum of two values by comparing a key extracted from each.
     * 
     * <p>The key must be Comparable. Null keys are considered less than non-null keys.
     *
     * @param <T> the type of the operands and result
     * @param keyExtractor the function to extract the Comparable key
     * @return a BinaryOperator that returns the element with the maximum key
     * @throws IllegalArgumentException if keyExtractor is null
     */
    @SuppressWarnings("rawtypes")
    public static <T> BinaryOperator<T> maxBy(final java.util.function.Function<? super T, ? extends Comparable> keyExtractor) throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor);
        final Comparator<? super T> comparator = Comparators.nullsFirstBy(keyExtractor);

        return (a, b) -> comparator.compare(a, b) >= 0 ? a : b;
    }

    /**
     * Returns a BinaryOperator for Map.Entry that finds the entry with the maximum key.
     * 
     * <p>Keys must be Comparable. Null keys are considered less than non-null keys.
     *
     * @param <K> the type of the Comparable keys
     * @param <V> the type of the values
     * @return a BinaryOperator that returns the entry with the maximum key
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K extends Comparable<? super K>, V> BinaryOperator<Map.Entry<K, V>> maxByKey() {
        return (BinaryOperator) MAX_BY_KEY;
    }

    /**
     * Returns a BinaryOperator for Map.Entry that finds the entry with the maximum value.
     * 
     * <p>Values must be Comparable. Null values are considered less than non-null values.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the Comparable values
     * @return a BinaryOperator that returns the entry with the maximum value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <K, V extends Comparable<? super V>> BinaryOperator<Map.Entry<K, V>> maxByValue() {
        return (BinaryOperator) MAX_BY_VALUE;
    }

    /**
     * Returns a Function that compares its input to the target value.
     * 
     * <p>The function returns a negative integer, zero, or a positive integer as the input
     * is less than, equal to, or greater than the target.
     *
     * @param <T> the type of the Comparable values
     * @param target the value to compare against
     * @return a Function that compares its input to the target
     */
    public static <T extends Comparable<? super T>> Function<T, Integer> compareTo(final T target) {
        return t -> N.compare(t, target);
    }

    /**
     * Returns a Function that compares its input to the target value using the specified Comparator.
     * 
     * <p>The function returns a negative integer, zero, or a positive integer as the input
     * is less than, equal to, or greater than the target according to the comparator.
     *
     * @param <T> the type of the values
     * @param target the value to compare against
     * @param cmp the Comparator to use (uses natural order if null)
     * @return a Function that compares its input to the target using the comparator
     */
    public static <T> Function<T, Integer> compareTo(final T target, final Comparator<? super T> cmp) throws IllegalArgumentException {
        // N.checkArgNotNull(cmp);

        final Comparator<? super T> cmpToUse = cmp == null ? (Comparator<? super T>) Comparators.naturalOrder() : cmp;

        return t -> cmpToUse.compare(t, target);
    }

    /**
     * Returns a BiFunction that compares two Comparable values.
     * 
     * <p>The function returns a negative integer, zero, or a positive integer as the first
     * argument is less than, equal to, or greater than the second.
     *
     * @param <T> the type of the Comparable values
     * @return a BiFunction that compares two values
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable<? super T>> BiFunction<T, T, Integer> compare() {
        return (BiFunction) COMPARE;
    }

    /**
     * Returns a BiFunction that compares two values using the specified Comparator.
     * 
     * <p>The function returns a negative integer, zero, or a positive integer as the first
     * argument is less than, equal to, or greater than the second according to the comparator.
     *
     * @param <T> the type of the values
     * @param cmp the Comparator to use (uses natural order if null)
     * @return a BiFunction that compares two values using the comparator
     */
    public static <T> BiFunction<T, T, Integer> compare(final Comparator<? super T> cmp) throws IllegalArgumentException {
        // N.checkArgNotNull(cmp);

        if (cmp == null || cmp == Comparators.naturalOrder()) { // NOSONAR
            return (BiFunction<T, T, Integer>) COMPARE;
        }

        return cmp::compare;
    }

    /**
     * Returns a Function that gets the result from a Future, returning the default value on error.
     * 
     * <p>If the Future throws an InterruptedException or ExecutionException, the function
     * will return the provided default value instead of propagating the exception.
     *
     * @param <T> the type of the Future's result
     * @param defaultValue the value to return if the Future throws an exception
     * @return a Function that gets the Future's result or returns the default value on error
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

    /**
     * Returns a Function that gets the result from a Future.
     * 
     * <p>If the Future throws an InterruptedException or ExecutionException, the function
     * will wrap it in a RuntimeException and throw it.
     *
     * @param <T> the type of the Future's result
     * @return a Function that gets the Future's result
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <T> Function<Future<T>, T> futureGet() {
        return (Function) FUTURE_GETTER;
    }

    /**
     * Converts a java.util.function.Supplier to a Supplier, preserving the instance if already a Supplier.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of results supplied by the supplier
     * @param supplier the supplier to convert
     * @return a Supplier instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Supplier<T> from(final java.util.function.Supplier<T> supplier) {
        return supplier instanceof Supplier ? ((Supplier) supplier) : supplier::get;
    }

    /**
     * Converts a java.util.function.IntFunction to an IntFunction, preserving the instance if already an IntFunction.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the result of the function
     * @param func the function to convert
     * @return an IntFunction instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> IntFunction<T> from(final java.util.function.IntFunction<? extends T> func) {
        return func instanceof IntFunction ? ((IntFunction) func) : func::apply;
    }

    /**
     * Converts a java.util.function.Predicate to a Predicate, preserving the instance if already a Predicate.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the input to the predicate
     * @param predicate the predicate to convert
     * @return a Predicate instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<T> from(final java.util.function.Predicate<T> predicate) {
        return predicate instanceof Predicate ? ((Predicate) predicate) : predicate::test;
    }

    /**
     * Converts a java.util.function.BiPredicate to a BiPredicate, preserving the instance if already a BiPredicate.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the first input to the predicate
     * @param <U> the type of the second input to the predicate
     * @param predicate the bi-predicate to convert
     * @return a BiPredicate instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, U> BiPredicate<T, U> from(final java.util.function.BiPredicate<T, U> predicate) {
        return predicate instanceof BiPredicate ? ((BiPredicate) predicate) : predicate::test;
    }

    /**
     * Converts a java.util.function.Consumer to a Consumer, preserving the instance if already a Consumer.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the input to the consumer
     * @param consumer the consumer to convert
     * @return a Consumer instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Consumer<T> from(final java.util.function.Consumer<T> consumer) {
        return consumer instanceof Consumer ? ((Consumer) consumer) : consumer::accept;
    }

    /**
     * Converts a java.util.function.BiConsumer to a BiConsumer, preserving the instance if already a BiConsumer.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the first input to the consumer
     * @param <U> the type of the second input to the consumer
     * @param consumer the bi-consumer to convert
     * @return a BiConsumer instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, U> BiConsumer<T, U> from(final java.util.function.BiConsumer<T, U> consumer) {
        return consumer instanceof BiConsumer ? ((BiConsumer) consumer) : consumer::accept;
    }

    /**
     * Converts a java.util.function.Function to a Function, preserving the instance if already a Function.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param function the function to convert
     * @return a Function instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, R> Function<T, R> from(final java.util.function.Function<T, ? extends R> function) {
        return function instanceof Function ? ((Function) function) : function::apply;
    }

    /**
     * Converts a java.util.function.BiFunction to a BiFunction, preserving the instance if already a BiFunction.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the first input to the function
     * @param <U> the type of the second input to the function
     * @param <R> the type of the result of the function
     * @param function the bi-function to convert
     * @return a BiFunction instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, U, R> BiFunction<T, U, R> from(final java.util.function.BiFunction<T, U, ? extends R> function) {
        return function instanceof BiFunction ? ((BiFunction) function) : function::apply;
    }

    /**
     * Converts a java.util.function.UnaryOperator to a UnaryOperator, preserving the instance if already a UnaryOperator.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the operand and result of the operator
     * @param op the unary operator to convert
     * @return a UnaryOperator instance
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> UnaryOperator<T> from(final java.util.function.UnaryOperator<T> op) {
        return op instanceof UnaryOperator ? ((UnaryOperator) op) : op::apply;
    }

    /**
     * Converts a java.util.function.BinaryOperator to a BinaryOperator, preserving the instance if already a BinaryOperator.
     * 
     * <p>This method is useful for ensuring type compatibility while avoiding unnecessary wrapping.
     *
     * @param <T> the type of the operands and result of the operator
     * @param op the binary operator to convert
     * @return a BinaryOperator instance
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

    @SuppressWarnings("rawtypes")
    static final Function<Optional, Object> GET_AS_IT = it -> it.orElse(null);

    /**
     * Returns the provided bi-consumer as is - a shorthand identity method for bi-consumers.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * @param <T> the type of the first input to the bi-consumer
     * @param <U> the type of the second input to the bi-consumer
     * @param biConsumer the bi-consumer to return
     * @return the bi-consumer unchanged
     * @see #c(Object, TriConsumer)
     * @see #c(TriConsumer)
     */
    @Beta
    public static <T, U> BiConsumer<T, U> c(final BiConsumer<T, U> biConsumer) {
        return biConsumer;
    }

    /**
     * Creates a bi-consumer that applies inputs along with a fixed value to the provided tri-consumer.
     * 
     * <p>This method implements partial application by binding the first parameter of the tri-consumer
     * to a fixed value, resulting in a bi-consumer that only requires the second and third parameters.
     * This is useful for creating bi-consumers that incorporate a reference value in their logic.</p>
     *
     * @param <A> the type of the fixed first argument to the tri-consumer
     * @param <T> the type of the first input to the resulting bi-consumer
     * @param <U> the type of the second input to the resulting bi-consumer
     * @param a the fixed value to use as the first argument to the tri-consumer
     * @param triConsumer the tri-consumer to apply with the fixed first argument
     * @return a bi-consumer that applies the inputs as the second and third arguments to the tri-consumer
     * @throws IllegalArgumentException if the triConsumer is null
     * @see #c(BiConsumer)
     * @see #c(TriConsumer)
     */
    @Beta
    public static <A, T, U> BiConsumer<T, U> c(final A a, final TriConsumer<A, T, U> triConsumer) throws IllegalArgumentException {
        N.checkArgNotNull(triConsumer);

        return (t, u) -> triConsumer.accept(a, t, u);
    }

    /**
     * Returns the provided tri-consumer as is - a shorthand identity method for tri-consumers.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate and BiPredicate.</p>
     *
     * @param <A> the type of the first input to the tri-consumer
     * @param <B> the type of the second input to the tri-consumer
     * @param <C> the type of the third input to the tri-consumer
     * @param triConsumer the tri-consumer to return
     * @return the tri-consumer unchanged
     * @see #c(BiConsumer)
     * @see #c(Object, TriConsumer)
     */
    @Beta
    public static <A, B, C> TriConsumer<A, B, C> c(final TriConsumer<A, B, C> triConsumer) {
        return triConsumer;
    }

    /**
     * Returns the provided function as is - a shorthand identity method for functions.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param function the function to return
     * @return the function unchanged
     * @see #f(Object, BiFunction)
     * @see #f(Object, Object, TriFunction)
     */
    @Beta
    public static <T, R> Function<T, R> f(final Function<T, R> function) {
        return function;
    }

    /**
     * Creates a function that applies the given argument to the provided bi-function.
     * 
     * <p>This method implements partial application by binding the first parameter of the bi-function
     * to a fixed value, resulting in a function that only requires the second parameter. This is useful
     * for creating functions that incorporate a reference value in their computation logic.</p>
     *
     * @param <A> the type of the fixed first argument to the bi-function
     * @param <T> the type of the input to the resulting function
     * @param <R> the type of the result of the resulting function
     * @param a the fixed value to use as the first argument to the bi-function
     * @param biFunction the bi-function to apply with the fixed first argument
     * @return a function that applies the input as the second argument to the bi-function
     * @throws IllegalArgumentException if the biFunction is null
     * @see #f(Function)
     * @see #f(Object, Object, TriFunction)
     */
    @Beta
    public static <A, T, R> Function<T, R> f(final A a, final java.util.function.BiFunction<A, T, R> biFunction) throws IllegalArgumentException {
        N.checkArgNotNull(biFunction);

        return t -> biFunction.apply(a, t);
    }

    /**
     * Creates a function that applies the given arguments to the provided tri-function.
     * 
     * <p>This method implements partial application by binding the first two parameters of the tri-function
     * to fixed values, resulting in a function that only requires the third parameter. This is useful
     * for creating functions that incorporate two reference values in their computation logic.</p>
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
     * @see #f(Function)
     * @see #f(Object, BiFunction)
     */
    @Beta
    public static <A, B, T, R> Function<T, R> f(final A a, final B b, final TriFunction<A, B, T, R> triFunction) throws IllegalArgumentException {
        N.checkArgNotNull(triFunction);

        return t -> triFunction.apply(a, b, t);
    }

    /**
     * Returns the provided bi-function as is - a shorthand identity method for bi-functions.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * @param <T> the type of the first input to the bi-function
     * @param <U> the type of the second input to the bi-function
     * @param <R> the type of the result of the bi-function
     * @param biFunction the bi-function to return
     * @return the bi-function unchanged
     * @see #f(Function)
     * @see #f(Object, TriFunction)
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> f(final BiFunction<T, U, R> biFunction) {
        return biFunction;
    }

    /**
     * Creates a bi-function that applies the given argument to the provided tri-function.
     * 
     * <p>This method implements partial application by binding the first parameter of the tri-function
     * to a fixed value, resulting in a bi-function that only requires the second and third parameters. 
     * This is useful for creating bi-functions that incorporate a reference value in their computation logic.</p>
     *
     * @param <A> the type of the fixed first argument to the tri-function
     * @param <T> the type of the first input to the resulting bi-function
     * @param <U> the type of the second input to the resulting bi-function
     * @param <R> the type of the result of the resulting bi-function
     * @param a the fixed value to use as the first argument to the tri-function
     * @param triFunction the tri-function to apply with the fixed first argument
     * @return a bi-function that applies the inputs as the second and third arguments to the tri-function
     * @throws IllegalArgumentException if the triFunction is null
     * @see #f(BiFunction)
     * @see #f(TriFunction)
     */
    @Beta
    public static <A, T, U, R> BiFunction<T, U, R> f(final A a, final TriFunction<A, T, U, R> triFunction) throws IllegalArgumentException {
        N.checkArgNotNull(triFunction);

        return (t, u) -> triFunction.apply(a, t, u);
    }

    /**
     * Returns the provided tri-function as is - a shorthand identity method for tri-functions.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate and BiPredicate.</p>
     *
     * @param <A> the type of the first input to the tri-function
     * @param <B> the type of the second input to the tri-function
     * @param <C> the type of the third input to the tri-function
     * @param <R> the type of the result of the tri-function
     * @param triFunction the tri-function to return
     * @return the tri-function unchanged
     * @see #f(Function)
     * @see #f(BiFunction)
     */
    @Beta
    public static <A, B, C, R> TriFunction<A, B, C, R> f(final TriFunction<A, B, C, R> triFunction) {
        return triFunction;
    }

    /**
     * Returns the provided unary operator as is - a shorthand identity method for unary operators.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * @param <T> the type of the operand and result of the unary operator
     * @param unaryOperator the unary operator to return
     * @return the unary operator unchanged
     * @throws IllegalArgumentException if the unaryOperator is null
     * @see #o(BinaryOperator)
     */
    @Beta
    public static <T> UnaryOperator<T> o(final UnaryOperator<T> unaryOperator) {
        N.checkArgNotNull(unaryOperator);

        return unaryOperator;
    }

    /**
     * Returns the provided binary operator as is - a shorthand identity method for binary operators.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts. It's part of a family of shorthand methods like {@code s()} for Supplier
     * and {@code p()} for Predicate.</p>
     *
     * @param <T> the type of the operands and result of the binary operator
     * @param binaryOperator the binary operator to return
     * @return the binary operator unchanged
     * @throws IllegalArgumentException if the binaryOperator is null
     * @see #o(UnaryOperator)
     */
    @Beta
    public static <T> BinaryOperator<T> o(final BinaryOperator<T> binaryOperator) {
        N.checkArgNotNull(binaryOperator);

        return binaryOperator;
    }

    /**
     * Returns the provided {@code java.util.function.BiConsumer} as-is.
     * This is a shorthand identity method for a mapper that can help with type inference in certain contexts,
     * particularly when used with stream operations like {@code Stream.mapMulti(mapper)}.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Using mc() to help with type inference in a stream operation
     * Stream<List<String>> listStream = ...;
     * Stream<String> flatStream = listStream.mapMulti(
     *     Fn.mc((List<String> list, Consumer<String> consumer) -> {
     *         for (String item : list) {
     *             if (item != null && !item.isEmpty()) {
     *                 consumer.accept(item);
     *             }
     *         }
     *     }));
     * }</pre>
     *
     * @param <T> the type of the first argument to the consumer
     * @param <U> the type of elements to be accepted by the result consumer
     * @param mapper the mapping bi-consumer to return
     * @return the bi-consumer unchanged
     * @see Stream#mapMulti(java.util.function.BiConsumer)
     * @see Seq#mapMulti(Throwables.BiConsumer)
     * @see Fnn#mc(Throwables.BiConsumer)
     */
    @Beta
    public static <T, U> java.util.function.BiConsumer<T, Consumer<U>> mc(
            final java.util.function.BiConsumer<? super T, ? extends java.util.function.Consumer<U>> mapper) {
        N.checkArgNotNull(mapper);

        return (BiConsumer<T, Consumer<U>>) mapper;
    }

    /**
     * Returns a supplier that wraps a throwable supplier, converting checked exceptions to runtime exceptions.
     * 
     * <p>This method is useful for converting suppliers that throw checked exceptions into standard suppliers
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of results supplied by the supplier
     * @param supplier the throwable supplier to wrap
     * @return a supplier that applies the supplier and converts exceptions
     * @throws IllegalArgumentException if the supplier is null
     * @see #ss(Object, Throwables.Function)
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
     * Creates a supplier that applies the given argument to the provided throwable function.
     * 
     * <p>This method implements partial application by binding the parameter of the function
     * to a fixed value, resulting in a supplier that requires no parameters.
     * Any checked exceptions thrown by the function will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed argument to the function
     * @param <T> the type of the result
     * @param a the fixed value to use as the argument to the function
     * @param func the throwable function to apply with the fixed argument
     * @return a supplier that computes the result by applying the function to the argument
     * @throws IllegalArgumentException if the function is null
     * @see #ss(Throwables.Supplier)
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
     * Returns a predicate that wraps a throwable predicate, converting checked exceptions to runtime exceptions.
     * 
     * <p>This method is useful for converting predicates that throw checked exceptions into standard predicates
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the input to the predicate
     * @param predicate the throwable predicate to wrap
     * @return a predicate that applies the input to the throwable predicate and converts exceptions
     * @throws IllegalArgumentException if the predicate is null
     * @see #pp(Object, Throwables.BiPredicate)
     * @see #pp(Object, Object, Throwables.TriPredicate)
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
     * Creates a predicate that applies the given argument to the provided throwable bi-predicate.
     * 
     * <p>This method implements partial application by binding the first parameter of the bi-predicate
     * to a fixed value, resulting in a predicate that only requires the second parameter. 
     * Any checked exceptions thrown by the bi-predicate will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the bi-predicate
     * @param <T> the type of the input to the resulting predicate
     * @param a the fixed value to use as the first argument to the bi-predicate
     * @param biPredicate the throwable bi-predicate to apply with the fixed first argument
     * @return a predicate that applies the input as the second argument to the bi-predicate
     * @throws IllegalArgumentException if the biPredicate is null
     * @see #pp(Throwables.Predicate)
     * @see #pp(Object, Object, Throwables.TriPredicate)
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
     * Creates a predicate that applies inputs along with two fixed values to the provided throwable tri-predicate.
     * 
     * <p>This method implements partial application by binding the first two parameters of the tri-predicate
     * to fixed values, resulting in a predicate that only requires the third parameter.
     * Any checked exceptions thrown by the tri-predicate will be converted to runtime exceptions.</p>
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
     * Creates a bi-predicate that safely wraps a throwable bi-predicate by converting any checked exceptions into runtime exceptions.
     * 
     * <p>This utility method simplifies functional programming by allowing the use of operations that might throw checked exceptions
     * without explicit try-catch blocks. Any checked exception thrown by the bi-predicate will be caught and
     * wrapped in a runtime exception.</p>
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
     * Creates a bi-predicate that applies the given argument to the provided throwable tri-predicate.
     * 
     * <p>This method implements partial application by binding the first parameter of the tri-predicate
     * to a fixed value, resulting in a bi-predicate that only requires the second and third parameters.
     * Any checked exceptions thrown by the tri-predicate will be converted to runtime exceptions.</p>
     *
     * @param <A> the type of the fixed first argument to the tri-predicate
     * @param <T> the type of the first input to the resulting bi-predicate
     * @param <U> the type of the second input to the resulting bi-predicate
     * @param a the fixed value to use as the first argument to the tri-predicate
     * @param triPredicate the throwable tri-predicate to apply with the fixed first argument
     * @return a bi-predicate that applies the inputs as the second and third arguments to the tri-predicate
     * @throws IllegalArgumentException if the triPredicate is null
     * @see #pp(Throwables.BiPredicate)
     * @see #pp(Throwables.TriPredicate)
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
     * Creates a tri-predicate that safely wraps a throwable tri-predicate by converting any checked exceptions into runtime exceptions.
     * 
     * <p>This utility method simplifies functional programming by allowing the use of operations that might throw checked exceptions
     * without explicit try-catch blocks. Any checked exception thrown by the tri-predicate will be caught and
     * wrapped in a runtime exception.</p>
     *
     * @param <A> the type of the first input to the tri-predicate
     * @param <B> the type of the second input to the tri-predicate
     * @param <C> the type of the third input to the tri-predicate
     * @param triPredicate the throwable tri-predicate to be wrapped
     * @return a tri-predicate that delegates to the given throwable tri-predicate, converting any checked exceptions to runtime exceptions
     * @throws IllegalArgumentException if the triPredicate is null
     * @see #pp(Throwables.Predicate)
     * @see #pp(Throwables.BiPredicate)
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
     * Returns a consumer that wraps a throwable consumer, converting checked exceptions to runtime exceptions.
     * 
     * <p>This method is useful for converting consumers that throw checked exceptions into standard consumers
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the input to the consumer
     * @param consumer the throwable consumer to wrap
     * @return a consumer that applies the input to the throwable consumer and converts exceptions
     * @throws IllegalArgumentException if the consumer is null
     * @see #cc(Object, Throwables.BiConsumer)
     * @see #cc(Object, Object, Throwables.TriConsumer)
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
     * Creates a consumer that applies the given argument to the provided throwable bi-consumer.
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
     * @see #cc(Throwables.Consumer)
     * @see #cc(Object, Object, Throwables.TriConsumer)
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
     * Creates a consumer that applies inputs along with two fixed values to the provided throwable tri-consumer.
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
     * @see #cc(Throwables.Consumer)
     * @see #cc(Object, Throwables.BiConsumer)
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
     * Returns a bi-consumer that wraps a throwable bi-consumer, converting checked exceptions to runtime exceptions.
     * 
     * <p>This method is useful for converting bi-consumers that throw checked exceptions into standard bi-consumers
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the first input to the bi-consumer
     * @param <U> the type of the second input to the bi-consumer
     * @param biConsumer the throwable bi-consumer to wrap
     * @return a bi-consumer that applies the inputs to the throwable bi-consumer and converts exceptions
     * @throws IllegalArgumentException if the biConsumer is null
     * @see #cc(Throwables.Consumer)
     * @see #cc(Object, Throwables.TriConsumer)
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
     * Creates a bi-consumer that applies the given argument to the provided throwable tri-consumer.
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
     * @see #cc(Throwables.BiConsumer)
     * @see #cc(Throwables.TriConsumer)
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
     * Returns a tri-consumer that wraps a throwable tri-consumer, converting checked exceptions to runtime exceptions.
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
     * @see #cc(Throwables.Consumer)
     * @see #cc(Throwables.BiConsumer)
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
     * Returns a function that wraps a throwable function, converting checked exceptions to runtime exceptions.
     * 
     * <p>This method is useful for converting functions that throw checked exceptions into standard functions
     * that can be used in functional programming contexts without the need for explicit exception handling.</p>
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param function the throwable function to wrap
     * @return a function that applies the input to the throwable function and converts exceptions
     * @throws IllegalArgumentException if the function is null
     * @see #ff(Throwables.Function, Object)
     * @see #ff(Object, Throwables.BiFunction)
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
     * Creates a function that safely wraps a throwable function by returning a default value if the function throws an exception.
     * 
     * <p>This utility method simplifies functional programming by allowing the use of operations that might throw checked exceptions
     * without explicit try-catch blocks. Any checked exception thrown by the function will be caught and the provided
     * default value will be returned instead.</p>
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
     * Creates a function that applies the given argument to the provided throwable bi-function.
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
     * @see #ff(Throwables.Function)
     * @see #ff(Object, Object, Throwables.TriFunction)
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
     * Creates a function that applies inputs along with two fixed values to the provided throwable tri-function.
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
     * @see #ff(Throwables.Function)
     * @see #ff(Object, Throwables.BiFunction)
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
     * Returns a bi-function that wraps a throwable bi-function, converting checked exceptions to runtime exceptions.
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
     * @see #ff(Throwables.Function)
     * @see #ff(Throwables.BiFunction, Object)
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
     * Creates a bi-function that safely wraps a throwable bi-function by returning a default value if the function throws an exception.
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
     * @see #ff(Throwables.BiFunction)
     * @see #ff(Throwables.Function, Object)
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
     * Creates a bi-function that applies the given argument to the provided throwable tri-function.
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
     * @see #ff(Throwables.BiFunction)
     * @see #ff(Throwables.TriFunction)
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
     * Returns a tri-function that wraps a throwable tri-function, converting checked exceptions to runtime exceptions.
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
     * @see #ff(Throwables.Function)
     * @see #ff(Throwables.BiFunction)
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
     * Creates a tri-function that safely wraps a throwable tri-function by returning a default value if the function throws an exception.
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
     * @see #ff(Throwables.TriFunction)
     * @see #ff(Throwables.BiFunction, Object)
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
     * Creates a synchronized predicate that safely wraps a standard predicate by ensuring all tests are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for predicates that might be accessed concurrently. Any test operation
     * will be performed while holding the lock on the provided mutex object, ensuring thread-safe evaluation of the predicate.</p>
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
     * Creates a synchronized predicate that applies the given argument to the provided bi-predicate within a synchronized block.
     * 
     * <p>This method combines partial application with synchronization. It binds the first parameter of the bi-predicate
     * to a fixed value and ensures thread-safe execution by synchronizing on the provided mutex object.</p>
     *
     * @param <A> the type of the fixed first argument to the bi-predicate
     * @param <T> the type of the input to the resulting predicate
     * @param mutex the object to synchronize on when testing values
     * @param a the fixed value to use as the first argument to the bi-predicate
     * @param biPredicate the bi-predicate to apply with the fixed first argument
     * @return a synchronized predicate that applies the input as the second argument to the bi-predicate
     * @throws IllegalArgumentException if the mutex or biPredicate is null
     * @see #sp(Object, Predicate)
     * @see #sp(Object, BiPredicate)
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

    /**
     * Creates a synchronized bi-predicate that safely wraps a standard bi-predicate by ensuring all tests are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for bi-predicates that might be accessed concurrently. Any test operation
     * will be performed while holding the lock on the provided mutex object.</p>
     *
     * @param <T> the type of the first input to the bi-predicate
     * @param <U> the type of the second input to the bi-predicate
     * @param mutex the object to synchronize on when testing values
     * @param biPredicate the bi-predicate to be wrapped with synchronization
     * @return a bi-predicate that delegates to the given bi-predicate within a synchronized block on the mutex
     * @throws IllegalArgumentException if the mutex or biPredicate is null
     * @see #sp(Object, Predicate)
     * @see #sp(Object, TriPredicate)
     */
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

    /**
     * Creates a synchronized tri-predicate that safely wraps a tri-predicate by ensuring all tests are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for tri-predicates that might be accessed concurrently. Any test operation
     * will be performed while holding the lock on the provided mutex object.</p>
     *
     * @param <A> the type of the first input to the tri-predicate
     * @param <B> the type of the second input to the tri-predicate
     * @param <C> the type of the third input to the tri-predicate
     * @param mutex the object to synchronize on when testing values
     * @param triPredicate the tri-predicate to be wrapped with synchronization
     * @return a tri-predicate that delegates to the given tri-predicate within a synchronized block on the mutex
     * @throws IllegalArgumentException if the mutex or triPredicate is null
     * @see #sp(Object, Predicate)
     * @see #sp(Object, BiPredicate)
     */
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
     * Creates a synchronized consumer that safely wraps a standard consumer by ensuring all accept operations are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for consumers that might be accessed concurrently. Any accept operation
     * will be performed while holding the lock on the provided mutex object, ensuring thread-safe execution.</p>
     *
     * @param <T> the type of the input to the consumer
     * @param mutex the object to synchronize on when accepting values
     * @param consumer the consumer to be wrapped with synchronization
     * @return a consumer that delegates to the given consumer within a synchronized block on the mutex
     * @throws IllegalArgumentException if the mutex or consumer is null
     * @see #cc(Throwables.Consumer)
     * @see #c(Consumer)
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
     * Creates a synchronized consumer that applies the given argument to the provided bi-consumer within a synchronized block.
     * 
     * <p>This method combines partial application with synchronization. It binds the first parameter of the bi-consumer
     * to a fixed value and ensures thread-safe execution by synchronizing on the provided mutex object.</p>
     *
     * @param <A> the type of the fixed first argument to the bi-consumer
     * @param <T> the type of the input to the resulting consumer
     * @param mutex the object to synchronize on when accepting values
     * @param a the fixed value to use as the first argument to the bi-consumer
     * @param biConsumer the bi-consumer to apply with the fixed first argument
     * @return a synchronized consumer that applies the input as the second argument to the bi-consumer
     * @throws IllegalArgumentException if the mutex or biConsumer is null
     * @see #sc(Object, Consumer)
     * @see #sc(Object, BiConsumer)
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
     * Creates a synchronized bi-consumer that safely wraps a standard bi-consumer by ensuring all accept operations are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for bi-consumers that might be accessed concurrently. Any accept operation
     * will be performed while holding the lock on the provided mutex object.</p>
     *
     * @param <T> the type of the first input to the bi-consumer
     * @param <U> the type of the second input to the bi-consumer
     * @param mutex the object to synchronize on when accepting values
     * @param biConsumer the bi-consumer to be wrapped with synchronization
     * @return a bi-consumer that delegates to the given bi-consumer within a synchronized block on the mutex
     * @throws IllegalArgumentException if the mutex or biConsumer is null
     * @see #sc(Object, Consumer)
     * @see #sp(Object, TriConsumer)
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

    /**
     * Creates a synchronized tri-consumer that safely wraps a tri-consumer by ensuring all accept operations are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for tri-consumers that might be accessed concurrently. Any accept operation
     * will be performed while holding the lock on the provided mutex object.</p>
     *
     * @param <A> the type of the first input to the tri-consumer
     * @param <B> the type of the second input to the tri-consumer
     * @param <C> the type of the third input to the tri-consumer
     * @param mutex the object to synchronize on when accepting values
     * @param triConsumer the tri-consumer to be wrapped with synchronization  
     * @return a tri-consumer that delegates to the given tri-consumer within a synchronized block on the mutex
     * @throws IllegalArgumentException if the mutex or triConsumer is null
     * @see #sc(Object, Consumer)
     * @see #sc(Object, BiConsumer)
     */
    @Beta
    public static <A, B, C> TriConsumer<A, B, C> sc(final Object mutex, final TriConsumer<A, B, C> triPredicate) throws IllegalArgumentException {
        N.checkArgNotNull(mutex, cs.mutex);
        N.checkArgNotNull(triPredicate, cs.TriConsumer);

        return (a, b, c) -> {
            synchronized (mutex) {
                triPredicate.accept(a, b, c);
            }
        };
    }

    /**
     * Creates a synchronized function that safely wraps a standard function by ensuring all apply operations are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for functions that might be accessed concurrently. Any apply operation
     * will be performed while holding the lock on the provided mutex object, ensuring thread-safe execution.</p>
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param mutex the object to synchronize on when applying the function
     * @param function the function to be wrapped with synchronization
     * @return a function that delegates to the given function within a synchronized block on the mutex
     * @throws IllegalArgumentException if the mutex or function is null
     * @see #ff(Throwables.Function)
     * @see #f(Function)
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
     * Creates a synchronized function that applies the given argument to the provided bi-function within a synchronized block.
     * 
     * <p>This method combines partial application with synchronization. It binds the first parameter of the bi-function
     * to a fixed value and ensures thread-safe execution by synchronizing on the provided mutex object.</p>
     *
     * @param <A> the type of the fixed first argument to the bi-function
     * @param <T> the type of the input to the resulting function
     * @param <R> the type of the result of the resulting function
     * @param mutex the object to synchronize on when applying the function
     * @param a the fixed value to use as the first argument to the bi-function
     * @param biFunction the bi-function to apply with the fixed first argument
     * @return a synchronized function that applies the input as the second argument to the bi-function
     * @throws IllegalArgumentException if the mutex or biFunction is null
     * @see #sf(Object, Function)
     * @see #sf(Object, BiFunction)
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
     * Creates a synchronized bi-function that safely wraps a standard bi-function by ensuring all apply operations are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for bi-functions that might be accessed concurrently. Any apply operation
     * will be performed while holding the lock on the provided mutex object.</p>
     *
     * @param <T> the type of the first input to the bi-function
     * @param <U> the type of the second input to the bi-function
     * @param <R> the type of the result of the bi-function
     * @param mutex the object to synchronize on when applying the function
     * @param biFunction the bi-function to be wrapped with synchronization
     * @return a bi-function that delegates to the given bi-function within a synchronized block on the mutex
     * @throws IllegalArgumentException if the mutex or biFunction is null
     * @see #sf(Object, Function)
     * @see #sf(Object, TriFunction)
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

    /**
     * Creates a synchronized tri-function that safely wraps a tri-function by ensuring all apply operations are performed within a synchronized block.
     * 
     * <p>This utility method provides thread safety for tri-functions that might be accessed concurrently. Any apply operation
     * will be performed while holding the lock on the provided mutex object.</p>
     *
     * @param <A> the type of the first input to the tri-function
     * @param <B> the type of the second input to the tri-function
     * @param <C> the type of the third input to the tri-function
     * @param <R> the type of the result of the tri-function
     * @param mutex the object to synchronize on when applying the function
     * @param triFunction the tri-function to be wrapped with synchronization
     * @return a tri-function that delegates to the given tri-function within a synchronized block on the mutex
     * @see #sf(Object, Function)
     * @see #sf(Object, BiFunction)
     */
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
     * Converts a consumer to a function that returns void (null) after executing the consumer.
     * 
     * <p>This method is useful when you need to use a consumer in a context that requires a function,
     * such as in stream map operations where you want side effects but also need to continue the stream.</p>
     *
     * @param <T> the type of the input to the consumer
     * @param action the consumer to convert to a function
     * @return a function that executes the consumer and returns null
     * @throws IllegalArgumentException if the action is null
     * @see #c2f(Consumer, Object)
     * @see #f2c(Function)
     */
    @Beta
    public static <T> Function<T, Void> c2f(final java.util.function.Consumer<? super T> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return t -> {
            action.accept(t);
            return null;
        };
    }

    /**
     * Converts a consumer to a function that returns a specified value after executing the consumer.
     * 
     * <p>This method is useful when you need to use a consumer in a context that requires a function
     * and want to return a specific value after the consumer executes, such as for chaining operations.</p>
     *
     * @param <T> the type of the input to the consumer
     * @param <R> the type of the value to return
     * @param action the consumer to convert to a function
     * @param valueToReturn the value to return after the consumer executes
     * @return a function that executes the consumer and returns the specified value
     * @throws IllegalArgumentException if the action is null
     * @see #c2f(Consumer)
     * @see #f2c(Function)
     */
    @Beta
    public static <T, R> Function<T, R> c2f(final java.util.function.Consumer<? super T> action, final R valueToReturn) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return t -> {
            action.accept(t);
            return valueToReturn;
        };
    }

    /**
     * Converts a bi-consumer to a bi-function that returns void (null) after executing the bi-consumer.
     * 
     * <p>This method is useful when you need to use a bi-consumer in a context that requires a bi-function,
     * allowing you to perform side effects while maintaining functional composition.</p>
     *
     * @param <T> the type of the first input to the bi-consumer
     * @param <U> the type of the second input to the bi-consumer
     * @param action the bi-consumer to convert to a bi-function
     * @return a bi-function that executes the bi-consumer and returns null
     * @throws IllegalArgumentException if the action is null
     * @see #c2f(BiConsumer, Object)
     * @see #f2c(BiFunction)
     */
    @Beta
    public static <T, U> BiFunction<T, U, Void> c2f(final java.util.function.BiConsumer<? super T, ? super U> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return (t, u) -> {
            action.accept(t, u);
            return null;
        };
    }

    /**
     * Converts a bi-consumer to a bi-function that returns a specified value after executing the bi-consumer.
     * 
     * <p>This method is useful when you need to use a bi-consumer in a context that requires a bi-function
     * and want to return a specific value after the bi-consumer executes.</p>
     *
     * @param <T> the type of the first input to the bi-consumer
     * @param <U> the type of the second input to the bi-consumer
     * @param <R> the type of the value to return
     * @param action the bi-consumer to convert to a bi-function
     * @param valueToReturn the value to return after the bi-consumer executes
     * @return a bi-function that executes the bi-consumer and returns the specified value
     * @throws IllegalArgumentException if the action is null
     * @see #c2f(BiConsumer)
     * @see #f2c(BiFunction)
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> c2f(final java.util.function.BiConsumer<? super T, ? super U> action, final R valueToReturn)
            throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return (t, u) -> {
            action.accept(t, u);
            return valueToReturn;
        };
    }

    /**
     * Converts a tri-consumer to a tri-function that returns void (null) after executing the tri-consumer.
     * 
     * <p>This method is useful when you need to use a tri-consumer in a context that requires a tri-function,
     * allowing you to perform side effects while maintaining functional composition.</p>
     *
     * @param <A> the type of the first input to the tri-consumer
     * @param <B> the type of the second input to the tri-consumer
     * @param <C> the type of the third input to the tri-consumer
     * @param action the tri-consumer to convert to a tri-function
     * @return a tri-function that executes the tri-consumer and returns null
     * @throws IllegalArgumentException if the action is null
     * @see #c2f(TriConsumer, Object)
     * @see #f2c(TriFunction)
     */
    @Beta
    public static <A, B, C> TriFunction<A, B, C, Void> c2f(final TriConsumer<? super A, ? super B, ? super C> action) throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return (a, b, c) -> {
            action.accept(a, b, c);
            return null;
        };
    }

    /**
     * Converts a tri-consumer to a tri-function that returns a specified value after executing the tri-consumer.
     * 
     * <p>This method is useful when you need to use a tri-consumer in a context that requires a tri-function
     * and want to return a specific value after the tri-consumer executes.</p>
     *
     * @param <A> the type of the first input to the tri-consumer
     * @param <B> the type of the second input to the tri-consumer
     * @param <C> the type of the third input to the tri-consumer
     * @param <R> the type of the value to return
     * @param action the tri-consumer to convert to a tri-function
     * @param valueToReturn the value to return after the tri-consumer executes
     * @return a tri-function that executes the tri-consumer and returns the specified value
     * @throws IllegalArgumentException if the action is null
     * @see #c2f(TriConsumer)
     * @see #f2c(TriFunction)
     */
    @Beta
    public static <A, B, C, R> TriFunction<A, B, C, R> c2f(final TriConsumer<? super A, ? super B, ? super C> action, final R valueToReturn)
            throws IllegalArgumentException {
        N.checkArgNotNull(action);

        return (a, b, c) -> {
            action.accept(a, b, c);
            return valueToReturn;
        };
    }

    /**
     * Converts a function to a consumer by discarding the function's return value.
     * 
     * <p>This method is useful when you have a function but need a consumer, and you don't care about
     * the return value. The function will still be executed for its side effects.</p>
     *
     * @param <T> the type of the input to the function
     * @param func the function to convert to a consumer
     * @return a consumer that executes the function and discards its return value
     * @throws IllegalArgumentException if the func is null
     * @see #c2f(Consumer)
     */
    @Beta
    public static <T> Consumer<T> f2c(final java.util.function.Function<? super T, ?> func) throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return func::apply;
    }

    /**
     * Converts a bi-function to a bi-consumer by discarding the bi-function's return value.
     * 
     * <p>This method is useful when you have a bi-function but need a bi-consumer, and you don't care about
     * the return value. The bi-function will still be executed for its side effects.</p>
     *
     * @param <T> the type of the first input to the bi-function
     * @param <U> the type of the second input to the bi-function
     * @param func the bi-function to convert to a bi-consumer
     * @return a bi-consumer that executes the bi-function and discards its return value
     * @throws IllegalArgumentException if the func is null
     * @see #c2f(BiConsumer)
     */
    @Beta
    public static <T, U> BiConsumer<T, U> f2c(final java.util.function.BiFunction<? super T, ? super U, ?> func) throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return func::apply;
    }

    /**
     * Converts a tri-function to a tri-consumer by discarding the tri-function's return value.
     * 
     * <p>This method is useful when you have a tri-function but need a tri-consumer, and you don't care about
     * the return value. The tri-function will still be executed for its side effects.</p>
     *
     * @param <A> the type of the first input to the tri-function
     * @param <B> the type of the second input to the tri-function
     * @param <C> the type of the third input to the tri-function
     * @param func the tri-function to convert to a tri-consumer
     * @return a tri-consumer that executes the tri-function and discards its return value
     * @throws IllegalArgumentException if the func is null
     * @see #c2f(TriConsumer)
     */
    @Beta
    public static <A, B, C> TriConsumer<A, B, C> f2c(final TriFunction<? super A, ? super B, ? super C, ?> func) throws IllegalArgumentException {
        N.checkArgNotNull(func);

        return func::apply;
    }

    /**
     * Wraps a throwable runnable to convert checked exceptions to runtime exceptions.
     * 
     * <p>This method allows you to use runnables that throw checked exceptions in contexts
     * that expect standard Runnable interfaces, such as thread creation or executor services.</p>
     *
     * @param runnable the throwable runnable to wrap
     * @return a runnable that executes the throwable runnable and converts checked exceptions to runtime exceptions
     * @see #cc(Throwables.Callable)
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
     * Wraps a throwable callable to convert checked exceptions to runtime exceptions.
     * 
     * <p>This method allows you to use callables that throw checked exceptions beyond the standard
     * Exception in contexts that expect standard Callable interfaces.</p>
     *
     * @param <R> the type of the result
     * @param callable the throwable callable to wrap
     * @return a callable that executes the throwable callable and converts checked exceptions to runtime exceptions
     * @see #rr(Throwables.Runnable)
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
     * Returns the provided runnable as is - a shorthand identity method for runnables.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts.</p>
     *
     * @param runnable the runnable to return
     * @return the runnable unchanged
     * @throws IllegalArgumentException if the runnable is null
     * @see #c(Callable)
     */
    public static Runnable r(final Runnable runnable) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        return runnable;
    }

    /**
     * Returns the provided callable as is - a shorthand identity method for callables.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts.</p>
     *
     * @param <R> the type of the result
     * @param callable the callable to return
     * @return the callable unchanged
     * @throws IllegalArgumentException if the callable is null
     * @see #r(Runnable)
     */
    public static <R> Callable<R> c(final Callable<R> callable) throws IllegalArgumentException {
        N.checkArgNotNull(callable);

        return callable;
    }

    /**
     * Returns the provided Java runnable as is - a shorthand identity method for Java runnables.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts when working with java.lang.Runnable.</p>
     *
     * @param runnable the Java runnable to return
     * @return the Java runnable unchanged
     * @throws IllegalArgumentException if the runnable is null
     * @see #jc(java.util.concurrent.Callable)
     */
    public static java.lang.Runnable jr(final java.lang.Runnable runnable) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        return runnable;
    }

    /**
     * Returns the provided Java callable as is - a shorthand identity method for Java callables.
     * 
     * <p>This method serves as a shorthand convenience method that can help with type inference
     * in certain contexts when working with java.util.concurrent.Callable.</p>
     *
     * @param <R> the type of the result
     * @param callable the Java callable to return
     * @return the Java callable unchanged
     * @throws IllegalArgumentException if the callable is null
     * @see #jr(java.lang.Runnable)
     */
    public static <R> java.util.concurrent.Callable<R> jc(final java.util.concurrent.Callable<R> callable) throws IllegalArgumentException {
        N.checkArgNotNull(callable);

        return callable;
    }

    /**
     * Converts a runnable to a callable that returns void (null).
     * 
     * <p>This method is useful when you need to use a runnable in a context that requires a callable,
     * such as with executor services when you want to track completion but don't need a return value.</p>
     *
     * @param runnable the runnable to convert to a callable
     * @return a callable that executes the runnable and returns null
     * @throws IllegalArgumentException if the runnable is null
     * @see #r2c(Runnable, Object)
     * @see #c2r(Callable)
     */
    public static Callable<Void> r2c(final java.lang.Runnable runnable) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        return () -> {
            runnable.run();
            return null;
        };
    }

    /**
     * Converts a runnable to a callable that returns a specified value.
     * 
     * <p>This method is useful when you need to use a runnable in a context that requires a callable
     * and want to return a specific value after the runnable executes.</p>
     *
     * @param <R> the type of the value to return
     * @param runnable the runnable to convert to a callable
     * @param valueToReturn the value to return after the runnable executes
     * @return a callable that executes the runnable and returns the specified value
     * @throws IllegalArgumentException if the runnable is null
     * @see #r2c(Runnable)
     * @see #c2r(Callable)
     */
    public static <R> Callable<R> r2c(final java.lang.Runnable runnable, final R valueToReturn) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        return () -> {
            runnable.run();
            return valueToReturn;
        };
    }

    /**
     * Converts a callable to a runnable by discarding the callable's return value.
     * 
     * <p>This method is useful when you have a callable but need a runnable, and you don't care about
     * the return value. The callable will still be executed for its side effects.</p>
     *
     * @param <R> the type of the callable's result
     * @param callable the callable to convert to a runnable
     * @return a runnable that executes the callable and discards its return value
     * @throws IllegalArgumentException if the callable is null
     * @see #r2c(Runnable)
     */
    public static <R> Runnable c2r(final Callable<R> callable) throws IllegalArgumentException {
        N.checkArgNotNull(callable);

        return callable::call;
    }

    /**
     * Converts a Java runnable to an abacus Runnable.
     * 
     * <p>This method provides compatibility between Java's standard Runnable interface and
     * the abacus framework's Runnable interface.</p>
     *
     * @param runnable the Java runnable to convert
     * @return an abacus Runnable that delegates to the Java runnable
     * @throws IllegalArgumentException if the runnable is null
     * @see #jc2c(java.util.concurrent.Callable)
     */
    public static Runnable jr2r(final java.lang.Runnable runnable) throws IllegalArgumentException {
        N.checkArgNotNull(runnable);

        if (runnable instanceof Runnable) {
            return (Runnable) runnable;
        }

        return runnable::run;
    }

    /**
     * Converts a Java callable to an abacus Callable.
     * 
     * <p>This method provides compatibility between Java's standard Callable interface and
     * the abacus framework's Callable interface, handling exception conversion.</p>
     *
     * @param <R> the type of the result
     * @param callable the Java callable to convert
     * @return an abacus Callable that delegates to the Java callable
     * @throws IllegalArgumentException if the callable is null
     * @see #jr2r(java.lang.Runnable)
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
     * Converts a callable to a Java runnable by discarding the callable's return value.
     * 
     * <p>This method is useful when you have a callable but need a Java runnable for use with
     * thread creation or other Java APIs that expect Runnable.</p>
     *
     * @param callable the Java callable to convert to a runnable
     * @return a Java runnable that executes the callable and discards its return value
     * @throws IllegalArgumentException if the callable is null
     * @see #r2c(Runnable)
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
     * Returns a BinaryOperator that always throws an exception when attempting to merge duplicate keys.
     * 
     * <p>This operator is useful in collectors and map operations where duplicate keys should be
     * treated as an error condition rather than being silently merged.</p>
     *
     * @param <T> the type of the operands and result
     * @return a BinaryOperator that throws IllegalStateException on merge attempts
     * @see #ignoringMerger()
     * @see #replacingMerger()
     */
    public static <T> BinaryOperator<T> throwingMerger() {
        return BinaryOperators.THROWING_MERGER;
    }

    /**
     * Returns a BinaryOperator that ignores the second value when merging duplicates, keeping the first value.
     * 
     * <p>This operator is useful in collectors and map operations where you want to keep the first
     * occurrence of duplicate keys and ignore subsequent ones.</p>
     *
     * @param <T> the type of the operands and result
     * @return a BinaryOperator that returns the first operand
     * @see #throwingMerger()
     * @see #replacingMerger()
     */
    public static <T> BinaryOperator<T> ignoringMerger() {
        return BinaryOperators.IGNORING_MERGER;
    }

    /**
     * Returns a BinaryOperator that replaces the first value with the second value when merging duplicates.
     * 
     * <p>This operator is useful in collectors and map operations where you want to keep the last
     * occurrence of duplicate keys, replacing earlier ones.</p>
     *
     * @param <T> the type of the operands and result
     * @return a BinaryOperator that returns the second operand
     * @see #throwingMerger()
     * @see #ignoringMerger()
     */
    public static <T> BinaryOperator<T> replacingMerger() {
        return BinaryOperators.REPLACING_MERGER;
    }

    /**
     * Returns a Function that extracts the value from an Optional, returning null if the Optional is empty.
     * 
     * <p>This function is useful for converting streams of Optionals to their contained values,
     * with empty Optionals becoming null values.</p>
     *
     * @param <T> the type of the value in the Optional
     * @return a Function that extracts the Optional's value or returns null
     * @see #getIfPresentOrElseNullJdk()
     * @see #isPresent()
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<Optional<T>, T> getIfPresentOrElseNull() {
        return (Function) GET_AS_IT;
    }

    @SuppressWarnings("rawtypes")
    static final Function<java.util.Optional, Object> GET_AS_IT_JDK = it -> it.orElse(null);

    /**
     * Returns a Function that extracts the value from a Java Optional, returning null if the Optional is empty.
     * 
     * <p>This function is useful for converting streams of Java Optionals to their contained values,
     * with empty Optionals becoming null values. This is the JDK Optional version of getIfPresentOrElseNull.</p>
     *
     * @param <T> the type of the value in the Optional
     * @return a Function that extracts the Java Optional's value or returns null
     * @see #getIfPresentOrElseNull()
     * @see #isPresentJdk()
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<java.util.Optional<T>, T> getIfPresentOrElseNullJdk() {
        return (Function) GET_AS_IT_JDK;
    }

    @SuppressWarnings("rawtypes")
    static final Predicate<Optional> IS_PRESENT_IT = Optional::isPresent;

    /**
     * Returns a Predicate that tests whether an Optional contains a value.
     * 
     * <p>This predicate is useful for filtering streams of Optionals to keep only those that contain values.</p>
     *
     * @param <T> the type of the value in the Optional
     * @return a Predicate that returns true if the Optional contains a value
     * @see #isPresentJdk()
     * @see #getIfPresentOrElseNull()
     */
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<Optional<T>> isPresent() {
        return (Predicate) IS_PRESENT_IT;
    }

    @SuppressWarnings("rawtypes")
    static final Predicate<java.util.Optional> IS_PRESENT_IT_JDK = java.util.Optional::isPresent;

    /**
     * Returns a Predicate that tests whether a Java Optional contains a value.
     * 
     * <p>This predicate is useful for filtering streams of Java Optionals to keep only those that contain values.
     * This is the JDK Optional version of isPresent.</p>
     *
     * @param <T> the type of the value in the Optional
     * @return a Predicate that returns true if the Java Optional contains a value
     * @see #isPresent()
     * @see #getIfPresentOrElseNullJdk()
     */
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<java.util.Optional<T>> isPresentJdk() {
        return (Predicate) IS_PRESENT_IT_JDK;
    }

    /**
     * Returns a stateful BiFunction that alternates between returning MergeResult.TAKE_FIRST and MergeResult.TAKE_SECOND.
     * 
     * <p>This function maintains internal state and alternates its result with each call. It starts by returning
     * TAKE_FIRST, then TAKE_SECOND, then TAKE_FIRST again, and so on. This is useful for implementing
     * alternating merge strategies in stream operations.</p>
     * 
     * <p><b>Warning:</b> This is a stateful function. Don't save or cache it for reuse, and don't use it in parallel streams.</p>
     *
     * @param <T> the type of the input elements
     * @return a stateful BiFunction that alternates between TAKE_FIRST and TAKE_SECOND
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

        /**
         * Returns a LongSupplier that supplies the current time in milliseconds.
         * 
         * <p>This supplier returns the current time in milliseconds since the Unix epoch
         * (January 1, 1970, 00:00:00 GMT) each time it is called.</p>
         *
         * @return a LongSupplier that returns System.currentTimeMillis()
         */
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
         * Returns the provided supplier as is - a shorthand identity method for suppliers.
         * 
         * <p>This method serves as a shorthand convenience method that can help with type inference
         * in certain contexts. It's part of a family of shorthand methods like {@code p()} for Predicate
         * and others.</p>
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
         * Creates a supplier that always returns the result of applying the provided function to the given value.
         * 
         * <p>This method creates a supplier that captures the provided value and function,
         * and when the supplier is called, it applies the function to the value and returns the result.</p>
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
         * Returns a supplier that always supplies the same instance.
         * 
         * <p>This method creates a supplier that always returns the provided instance,
         * useful for creating constant suppliers.</p>
         *
         * @param <T> the type of the instance
         * @param instance the instance to be supplied
         * @return a supplier that always returns the same instance
         */
        public static <T> Supplier<T> ofInstance(final T instance) {
            return () -> instance;
        }

        /**
         * Returns a supplier that generates UUID strings.
         * 
         * <p>Each call to the supplier's get() method will generate a new UUID string.</p>
         *
         * @return a supplier that generates UUID strings
         * @see #ofGUID()
         */
        public static Supplier<String> ofUUID() {
            return UUID;
        }

        /**
         * Returns a supplier that generates GUID strings.
         * 
         * <p>Each call to the supplier's get() method will generate a new GUID string.</p>
         *
         * @return a supplier that generates GUID strings
         * @see #ofUUID()
         */
        public static Supplier<String> ofGUID() {
            return GUID;
        }

        /**
         * Returns a supplier that supplies empty boolean arrays.
         * 
         * <p>This supplier always returns the same empty boolean array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty boolean array
         */
        public static Supplier<boolean[]> ofEmptyBooleanArray() {
            return EMPTY_BOOLEAN_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty char arrays.
         * 
         * <p>This supplier always returns the same empty char array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty char array
         */
        public static Supplier<char[]> ofEmptyCharArray() {
            return EMPTY_CHAR_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty byte arrays.
         * 
         * <p>This supplier always returns the same empty byte array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty byte array
         */
        public static Supplier<byte[]> ofEmptyByteArray() {
            return EMPTY_BYTE_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty short arrays.
         * 
         * <p>This supplier always returns the same empty short array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty short array
         */
        public static Supplier<short[]> ofEmptyShortArray() {
            return EMPTY_SHORT_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty int arrays.
         * 
         * <p>This supplier always returns the same empty int array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty int array
         */
        public static Supplier<int[]> ofEmptyIntArray() {
            return EMPTY_INT_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty long arrays.
         * 
         * <p>This supplier always returns the same empty long array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty long array
         */
        public static Supplier<long[]> ofEmptyLongArray() {
            return EMPTY_LONG_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty float arrays.
         * 
         * <p>This supplier always returns the same empty float array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty float array
         */
        public static Supplier<float[]> ofEmptyFloatArray() {
            return EMPTY_FLOAT_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty double arrays.
         * 
         * <p>This supplier always returns the same empty double array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty double array
         */
        public static Supplier<double[]> ofEmptyDoubleArray() {
            return EMPTY_DOUBLE_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty String arrays.
         * 
         * <p>This supplier always returns the same empty String array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty String array
         */
        public static Supplier<String[]> ofEmptyStringArray() {
            return EMPTY_STRING_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty Object arrays.
         * 
         * <p>This supplier always returns the same empty Object array instance for efficiency.</p>
         *
         * @return a supplier that returns an empty Object array
         */
        public static Supplier<Object[]> ofEmptyObjectArray() {
            return EMPTY_OBJECT_ARRAY;
        }

        /**
         * Returns a supplier that supplies empty strings.
         * 
         * <p>This supplier always returns the same empty string instance.</p>
         *
         * @return a supplier that returns an empty string
         */
        public static Supplier<String> ofEmptyString() {
            return EMPTY_STRING;
        }

        /**
         * Returns a supplier that creates new BooleanList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty BooleanList.</p>
         *
         * @return a supplier that creates new BooleanList instances
         */
        public static Supplier<BooleanList> ofBooleanList() {
            return BOOLEAN_LIST;
        }

        /**
         * Returns a supplier that creates new CharList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty CharList.</p>
         *
         * @return a supplier that creates new CharList instances
         */
        public static Supplier<CharList> ofCharList() {
            return CHAR_LIST;
        }

        /**
         * Returns a supplier that creates new ByteList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty ByteList.</p>
         *
         * @return a supplier that creates new ByteList instances
         */
        public static Supplier<ByteList> ofByteList() {
            return BYTE_LIST;
        }

        /**
         * Returns a supplier that creates new ShortList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty ShortList.</p>
         *
         * @return a supplier that creates new ShortList instances
         */
        public static Supplier<ShortList> ofShortList() {
            return SHORT_LIST;
        }

        /**
         * Returns a supplier that creates new IntList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty IntList.</p>
         *
         * @return a supplier that creates new IntList instances
         */
        public static Supplier<IntList> ofIntList() {
            return INT_LIST;
        }

        /**
         * Returns a supplier that creates new LongList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty LongList.</p>
         *
         * @return a supplier that creates new LongList instances
         */
        public static Supplier<LongList> ofLongList() {
            return LONG_LIST;
        }

        /**
         * Returns a supplier that creates new FloatList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty FloatList.</p>
         *
         * @return a supplier that creates new FloatList instances
         */
        public static Supplier<FloatList> ofFloatList() {
            return FLOAT_LIST;
        }

        /**
         * Returns a supplier that creates new DoubleList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty DoubleList.</p>
         *
         * @return a supplier that creates new DoubleList instances
         */
        public static Supplier<DoubleList> ofDoubleList() {
            return DOUBLE_LIST;
        }

        /**
         * Returns a supplier that creates new List instances (ArrayList).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty ArrayList.</p>
         *
         * @param <T> the type of elements in the list
         * @return a supplier that creates new ArrayList instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<List<T>> ofList() {
            return (Supplier) LIST;
        }

        /**
         * Returns a supplier that creates new LinkedList instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty LinkedList.</p>
         *
         * @param <T> the type of elements in the list
         * @return a supplier that creates new LinkedList instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedList<T>> ofLinkedList() {
            return (Supplier) LINKED_LIST;
        }

        /**
         * Returns a supplier that creates new Set instances (HashSet).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty HashSet.</p>
         *
         * @param <T> the type of elements in the set
         * @return a supplier that creates new HashSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofSet() {
            return (Supplier) SET;
        }

        /**
         * Returns a supplier that creates new LinkedHashSet instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty LinkedHashSet.</p>
         *
         * @param <T> the type of elements in the set
         * @return a supplier that creates new LinkedHashSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofLinkedHashSet() {
            return (Supplier) LINKED_HASH_SET;
        }

        /**
         * Returns a supplier that creates new SortedSet instances (TreeSet).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty TreeSet.</p>
         *
         * @param <T> the type of elements in the set
         * @return a supplier that creates new TreeSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<SortedSet<T>> ofSortedSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Returns a supplier that creates new NavigableSet instances (TreeSet).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty TreeSet.</p>
         *
         * @param <T> the type of elements in the set
         * @return a supplier that creates new TreeSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<NavigableSet<T>> ofNavigableSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Returns a supplier that creates new TreeSet instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty TreeSet.</p>
         *
         * @param <T> the type of elements in the set
         * @return a supplier that creates new TreeSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<TreeSet<T>> ofTreeSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Returns a supplier that creates new Queue instances (LinkedList).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty LinkedList as a Queue.</p>
         *
         * @param <T> the type of elements in the queue
         * @return a supplier that creates new LinkedList instances as Queue
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Queue<T>> ofQueue() {
            return (Supplier) QUEUE;
        }

        /**
         * Returns a supplier that creates new Deque instances (LinkedList).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty LinkedList as a Deque.</p>
         *
         * @param <T> the type of elements in the deque
         * @return a supplier that creates new LinkedList instances as Deque
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Deque<T>> ofDeque() {
            return (Supplier) DEQUE;
        }

        /**
         * Returns a supplier that creates new ArrayDeque instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty ArrayDeque.</p>
         *
         * @param <T> the type of elements in the deque
         * @return a supplier that creates new ArrayDeque instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ArrayDeque<T>> ofArrayDeque() {
            return (Supplier) ARRAY_DEQUE;
        }

        /**
         * Returns a supplier that creates new LinkedBlockingQueue instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty LinkedBlockingQueue
         * with unbounded capacity.</p>
         *
         * @param <T> the type of elements in the queue
         * @return a supplier that creates new LinkedBlockingQueue instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (Supplier) LINKED_BLOCKING_QUEUE;
        }

        /**
         * Returns a supplier that creates new LinkedBlockingDeque instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty LinkedBlockingDeque
         * with unbounded capacity.</p>
         *
         * @param <T> the type of elements in the deque
         * @return a supplier that creates new LinkedBlockingDeque instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedBlockingDeque<T>> ofLinkedBlockingDeque() {
            return (Supplier) LINKED_BLOCKING_DEQUE;
        }

        /**
         * Returns a supplier that creates new ConcurrentLinkedQueue instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty ConcurrentLinkedQueue.</p>
         *
         * @param <T> the type of elements in the queue
         * @return a supplier that creates new ConcurrentLinkedQueue instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (Supplier) CONCURRENT_LINKED_QUEUE;
        }

        /**
         * Returns a supplier that creates new PriorityQueue instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty PriorityQueue
         * with natural ordering.</p>
         *
         * @param <T> the type of elements in the queue
         * @return a supplier that creates new PriorityQueue instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<PriorityQueue<T>> ofPriorityQueue() {
            return (Supplier) PRIORITY_QUEUE;
        }

        /**
         * Returns a supplier that creates new Map instances (HashMap).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty HashMap.</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new HashMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<Map<K, V>> ofMap() {
            return (Supplier) MAP;
        }

        /**
         * Returns a supplier that creates new LinkedHashMap instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty LinkedHashMap.</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new LinkedHashMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<Map<K, V>> ofLinkedHashMap() {
            return (Supplier) LINKED_HASH_MAP;
        }

        /**
         * Returns a supplier that creates new IdentityHashMap instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty IdentityHashMap.
         * IdentityHashMap uses reference equality (==) instead of object equality (equals).</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new IdentityHashMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<IdentityHashMap<K, V>> ofIdentityHashMap() {
            return (Supplier) IDENTITY_HASH_MAP;
        }

        /**
         * Returns a supplier that creates new SortedMap instances (TreeMap).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty TreeMap.</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new TreeMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<SortedMap<K, V>> ofSortedMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Returns a supplier that creates new NavigableMap instances (TreeMap).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty TreeMap.</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new TreeMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<NavigableMap<K, V>> ofNavigableMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Returns a supplier that creates new TreeMap instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty TreeMap.</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new TreeMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<TreeMap<K, V>> ofTreeMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Returns a supplier that creates new ConcurrentMap instances (ConcurrentHashMap).
         * 
         * <p>Each call to the supplier's get() method will create a new, empty ConcurrentHashMap.</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new ConcurrentHashMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        /**
         * Returns a supplier that creates new ConcurrentHashMap instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty ConcurrentHashMap.</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new ConcurrentHashMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        /**
         * Returns a supplier that creates new concurrent Set instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty Set backed by ConcurrentHashMap.</p>
         *
         * @param <T> the type of elements in the set
         * @return a supplier that creates new concurrent Set instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofConcurrentHashSet() {
            return (Supplier) CONCURRENT_HASH_SET;
        }

        /**
         * Returns a supplier that creates new BiMap instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty BiMap.
         * A BiMap maintains a bidirectional mapping between keys and values.</p>
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return a supplier that creates new BiMap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<BiMap<K, V>> ofBiMap() {
            return (Supplier) BI_MAP;
        }

        /**
         * Returns a supplier that creates new Multiset instances.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty Multiset.
         * A Multiset is a collection that allows duplicate elements and counts their occurrences.</p>
         *
         * @param <T> the type of elements in the multiset
         * @return a supplier that creates new Multiset instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Multiset<T>> ofMultiset() {
            return (Supplier) MULTISET;
        }

        /**
         * Returns a supplier that creates new Multiset instances with a specific map type.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty Multiset
         * backed by the specified type of Map.</p>
         *
         * @param <T> the type of elements in the multiset
         * @param valueMapType the class of Map to use for storing element counts
         * @return a supplier that creates new Multiset instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Multiset<T>> ofMultiset(final Class<? extends Map> valueMapType) {
            return () -> N.newMultiset(valueMapType);
        }

        /**
         * Returns a supplier that creates new Multiset instances with a custom map supplier.
         * 
         * <p>Each call to the supplier's get() method will create a new, empty Multiset
         * backed by a Map created by the provided supplier.</p>
         *
         * @param <T> the type of elements in the multiset
         * @param mapSupplier supplier to create the backing Map
         * @return a supplier that creates new Multiset instances
         */
        public static <T> Supplier<Multiset<T>> ofMultiset(final java.util.function.Supplier<? extends Map<T, ?>> mapSupplier) {
            return () -> N.newMultiset(mapSupplier);
        }

        /**
         * Returns a Supplier that creates a new ListMultimap with default backing Map and List implementations.
         * 
         * <p>The returned supplier creates ListMultimaps backed by HashMap and ArrayList.
         * Each invocation of the supplier creates a new empty ListMultimap instance.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @return a Supplier that creates new ListMultimap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap() {
            return (Supplier) LIST_MULTIMAP;
        }

        /**
         * Returns a Supplier that creates a new ListMultimap with the specified Map type and default List implementation.
         * 
         * <p>The returned supplier creates ListMultimaps backed by the specified Map type and ArrayList.
         * Each invocation of the supplier creates a new empty ListMultimap instance.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @param mapType the Class object representing the Map implementation to use
         * @return a Supplier that creates new ListMultimap instances with the specified Map type
         * @throws IllegalArgumentException if mapType is null
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Class<? extends Map> mapType) {
            return () -> N.newListMultimap(mapType);
        }

        /**
         * Returns a Supplier that creates a new ListMultimap with the specified Map and List types.
         * 
         * <p>The returned supplier creates ListMultimaps backed by the specified Map and List implementations.
         * Each invocation of the supplier creates a new empty ListMultimap instance.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @param mapType the Class object representing the Map implementation to use
         * @param valueType the Class object representing the List implementation to use for values
         * @return a Supplier that creates new ListMultimap instances with the specified types
         * @throws IllegalArgumentException if mapType or valueType is null
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
            return () -> N.newListMultimap(mapType, valueType);
        }

        /**
         * Returns a Supplier that creates a new ListMultimap using the provided map and value suppliers.
         * 
         * <p>The returned supplier creates ListMultimaps using custom suppliers for both the backing Map
         * and the List instances used for values. This allows for complete control over the multimap's
         * internal structure.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @param mapSupplier supplier that creates the backing Map instances
         * @param valueSupplier supplier that creates the List instances for values
         * @return a Supplier that creates new ListMultimap instances using the provided suppliers
         * @throws IllegalArgumentException if mapSupplier or valueSupplier is null
         */
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final java.util.function.Supplier<? extends Map<K, List<E>>> mapSupplier,
                final java.util.function.Supplier<? extends List<E>> valueSupplier) {
            return () -> N.newListMultimap(mapSupplier, valueSupplier);
        }

        /**
         * Returns a Supplier that creates a new SetMultimap with default backing Map and Set implementations.
         * 
         * <p>The returned supplier creates SetMultimaps backed by HashMap and HashSet.
         * Each invocation of the supplier creates a new empty SetMultimap instance.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @return a Supplier that creates new SetMultimap instances
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap() {
            return (Supplier) SET_MULTIMAP;
        }

        /**
         * Returns a Supplier that creates a new SetMultimap with the specified Map type and default Set implementation.
         * 
         * <p>The returned supplier creates SetMultimaps backed by the specified Map type and HashSet.
         * Each invocation of the supplier creates a new empty SetMultimap instance.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @param mapType the Class object representing the Map implementation to use
         * @return a Supplier that creates new SetMultimap instances with the specified Map type
         * @throws IllegalArgumentException if mapType is null
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Class<? extends Map> mapType) {
            return () -> N.newSetMultimap(mapType);
        }

        /**
         * Returns a Supplier that creates a new SetMultimap with the specified Map and Set types.
         * 
         * <p>The returned supplier creates SetMultimaps backed by the specified Map and Set implementations.
         * Each invocation of the supplier creates a new empty SetMultimap instance.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @param mapType the Class object representing the Map implementation to use
         * @param valueType the Class object representing the Set implementation to use for values
         * @return a Supplier that creates new SetMultimap instances with the specified types
         * @throws IllegalArgumentException if mapType or valueType is null
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
            return () -> N.newSetMultimap(mapType, valueType);
        }

        /**
         * Returns a Supplier that creates a new SetMultimap using the provided map and value suppliers.
         * 
         * <p>The returned supplier creates SetMultimaps using custom suppliers for both the backing Map
         * and the Set instances used for values. This allows for complete control over the multimap's
         * internal structure.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @param mapSupplier supplier that creates the backing Map instances
         * @param valueSupplier supplier that creates the Set instances for values
         * @return a Supplier that creates new SetMultimap instances using the provided suppliers
         * @throws IllegalArgumentException if mapSupplier or valueSupplier is null
         */
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final java.util.function.Supplier<? extends Map<K, Set<E>>> mapSupplier,
                final java.util.function.Supplier<? extends Set<E>> valueSupplier) {
            return () -> N.newSetMultimap(mapSupplier, valueSupplier);
        }

        /**
         * Returns a Supplier that creates a new Multimap using the provided map and value collection suppliers.
         * 
         * <p>This is the most general multimap supplier, allowing any Collection type for values.
         * The returned supplier creates Multimaps using custom suppliers for both the backing Map
         * and the Collection instances used for values.</p>
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @param <V> the type of Collection used to store values
         * @param mapSupplier supplier that creates the backing Map instances
         * @param valueSupplier supplier that creates the Collection instances for values
         * @return a Supplier that creates new Multimap instances using the provided suppliers
         * @throws IllegalArgumentException if mapSupplier or valueSupplier is null
         */
        public static <K, E, V extends Collection<E>> Supplier<Multimap<K, E, V>> ofMultimap(final java.util.function.Supplier<? extends Map<K, V>> mapSupplier,
                final java.util.function.Supplier<? extends V> valueSupplier) {
            return () -> N.newMultimap(mapSupplier, valueSupplier);
        }

        /**
         * Returns a Supplier that creates new StringBuilder instances.
         * 
         * <p>Each invocation of the supplier creates a new empty StringBuilder with default initial capacity.</p>
         *
         * @return a Supplier that creates new StringBuilder instances
         */
        public static Supplier<StringBuilder> ofStringBuilder() {
            return STRING_BUILDER;
        }

        @SuppressWarnings("rawtypes")
        private static final Map<Class<?>, Supplier> collectionSupplierPool = new ConcurrentHashMap<>();

        /**
         * Returns a Supplier that creates Collection instances of the specified type.
         * 
         * <p>This method provides suppliers for various Collection implementations including List, Set, Queue,
         * and their subtypes. The method uses a cache to avoid creating duplicate suppliers for the same type.</p>
         * 
         * <p>Supported types include:
         * <ul>
         *   <li>Collection, List, ArrayList - returns ArrayList supplier</li>
         *   <li>LinkedList - returns LinkedList supplier</li>
         *   <li>Set, HashSet - returns HashSet supplier</li>
         *   <li>LinkedHashSet - returns LinkedHashSet supplier</li>
         *   <li>SortedSet, TreeSet - returns TreeSet supplier</li>
         *   <li>Queue, Deque - returns ArrayDeque supplier</li>
         *   <li>Various concurrent collections</li>
         * </ul>
         * </p>
         *
         * @param <T> the element type of the collection
         * @param targetType the Class object representing the desired Collection implementation
         * @return a Supplier that creates instances of the specified Collection type
         * @throws IllegalArgumentException if targetType is not a Collection class, is abstract and cannot be instantiated,
         *         or if no suitable implementation can be found
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<? extends Collection<T>> ofCollection(final Class<? extends Collection> targetType) throws IllegalArgumentException {
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
         * Returns a Supplier that creates Map instances of the specified type.
         * 
         * <p>This method provides suppliers for various Map implementations including HashMap, LinkedHashMap,
         * TreeMap, and concurrent maps. The method uses a cache to avoid creating duplicate suppliers for the same type.</p>
         * 
         * <p>Supported types include:
         * <ul>
         *   <li>Map, HashMap - returns HashMap supplier</li>
         *   <li>LinkedHashMap - returns LinkedHashMap supplier</li>
         *   <li>SortedMap, TreeMap - returns TreeMap supplier</li>
         *   <li>IdentityHashMap - returns IdentityHashMap supplier</li>
         *   <li>ConcurrentHashMap - returns ConcurrentHashMap supplier</li>
         *   <li>BiMap - returns BiMap supplier</li>
         * </ul>
         * </p>
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param targetType the Class object representing the desired Map implementation
         * @return a Supplier that creates instances of the specified Map type
         * @throws IllegalArgumentException if targetType is not a Map class, is abstract and cannot be instantiated,
         *         or if no suitable implementation can be found
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
         * Registers a custom Supplier for creating instances of the specified Collection class.
         * 
         * <p>This method allows registering custom suppliers for Collection implementations that are not
         * built-in or require special initialization. Once registered, the supplier will be used by
         * {@link #ofCollection(Class)} when creating instances of the target class.</p>
         * 
         * <p>Note: Built-in classes (like ArrayList, HashSet, etc.) cannot be registered with custom suppliers.</p>
         *
         * @param <T> the Collection type
         * @param targetClass the Class object of the Collection implementation to register
         * @param supplier the Supplier that creates instances of the target class
         * @return true if the registration was successful, false if a supplier was already registered for this class
         * @throws IllegalArgumentException if targetClass or supplier is null, or if targetClass is a built-in class
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
         * Registers a custom Supplier for creating instances of the specified Map class.
         * 
         * <p>This method allows registering custom suppliers for Map implementations that are not
         * built-in or require special initialization. Once registered, the supplier will be used by
         * {@link #ofMap(Class)} when creating instances of the target class.</p>
         * 
         * <p>Note: Built-in classes (like HashMap, TreeMap, etc.) cannot be registered with custom suppliers.</p>
         *
         * @param <T> the Map type
         * @param targetClass the Class object of the Map implementation to register
         * @param supplier the Supplier that creates instances of the target class
         * @return true if the registration was successful, false if a supplier was already registered for this class
         * @throws IllegalArgumentException if targetClass or supplier is null, or if targetClass is a built-in class
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
         * Throws UnsupportedOperationException. ImmutableList creation is not supported.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         * @deprecated unsupported operation.
         */
        @Deprecated
        public static Supplier<ImmutableList<?>> ofImmutableList() {
            throw new UnsupportedOperationException();
        }

        /**
         * Throws UnsupportedOperationException. ImmutableSet creation is not supported.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
         * @deprecated unsupported operation.
         */
        @Deprecated
        public static Supplier<ImmutableSet<?>> ofImmutableSet() {
            throw new UnsupportedOperationException();
        }

        /**
         * Throws UnsupportedOperationException. ImmutableMap creation is not supported.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always
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

        /**
         * Returns a Supplier that creates new Exception instances.
         * 
         * <p>Each invocation of the supplier creates a new Exception with no message or cause.</p>
         *
         * @return a Supplier that creates new Exception instances
         */
        @Beta
        public static Supplier<Exception> newException() {
            return EXCEPTION;
        }

        private static final Supplier<RuntimeException> RUNTIME_EXCEPTION = RuntimeException::new;

        /**
         * Returns a Supplier that creates new RuntimeException instances.
         * 
         * <p>Each invocation of the supplier creates a new RuntimeException with no message or cause.</p>
         *
         * @return a Supplier that creates new RuntimeException instances
         */
        @Beta
        public static Supplier<RuntimeException> newRuntimeException() {
            return RUNTIME_EXCEPTION;
        }

        private static final Supplier<NoSuchElementException> NO_SUCH_ELEMENT_EXCEPTION = NoSuchElementException::new;

        /**
         * Returns a Supplier that creates new NoSuchElementException instances.
         * 
         * <p>Each invocation of the supplier creates a new NoSuchElementException with no message.</p>
         *
         * @return a Supplier that creates new NoSuchElementException instances
         */
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
         * Returns the provided IntFunction as is - a shorthand identity method for IntFunction instances.
         * 
         * <p>This method serves as a shorthand convenience method that can help with type inference
         * in certain contexts. It's part of a family of factory methods that handle various function types.</p>
         * 
         * <p>Example usage:
         * <pre>{@code
         * // Instead of explicitly typing:
         * IntFunction<String[]> arrayCreator = size -> new String[size];
         * // You can use:
         * var arrayCreator = IntFunctions.of(size -> new String[size]);
         * }</pre>
         *
         * @param <T> the type of the result of the function
         * @param func the IntFunction to return
         * @return the IntFunction unchanged
         */
        public static <T> IntFunction<T> of(IntFunction<T> func) {
            return func;
        }

        /**
         * Returns an IntFunction that creates boolean arrays of the specified size.
         * 
         * <p>The returned function creates new boolean arrays with all elements initialized to false.</p>
         *
         * @return an IntFunction that creates boolean arrays
         */
        public static IntFunction<boolean[]> ofBooleanArray() {
            return BOOLEAN_ARRAY;
        }

        /**
         * Returns an IntFunction that creates char arrays of the specified size.
         * 
         * <p>The returned function creates new char arrays with all elements initialized to '\u0000'.</p>
         *
         * @return an IntFunction that creates char arrays
         */
        public static IntFunction<char[]> ofCharArray() {
            return CHAR_ARRAY;
        }

        /**
         * Returns an IntFunction that creates byte arrays of the specified size.
         * 
         * <p>The returned function creates new byte arrays with all elements initialized to 0.</p>
         *
         * @return an IntFunction that creates byte arrays
         */
        public static IntFunction<byte[]> ofByteArray() {
            return BYTE_ARRAY;
        }

        /**
         * Returns an IntFunction that creates short arrays of the specified size.
         * 
         * <p>The returned function creates new short arrays with all elements initialized to 0.</p>
         *
         * @return an IntFunction that creates short arrays
         */
        public static IntFunction<short[]> ofShortArray() {
            return SHORT_ARRAY;
        }

        /**
         * Returns an IntFunction that creates int arrays of the specified size.
         * 
         * <p>The returned function creates new int arrays with all elements initialized to 0.</p>
         *
         * @return an IntFunction that creates int arrays
         */
        public static IntFunction<int[]> ofIntArray() {
            return INT_ARRAY;
        }

        /**
         * Returns an IntFunction that creates long arrays of the specified size.
         * 
         * <p>The returned function creates new long arrays with all elements initialized to 0L.</p>
         *
         * @return an IntFunction that creates long arrays
         */
        public static IntFunction<long[]> ofLongArray() {
            return LONG_ARRAY;
        }

        /**
         * Returns an IntFunction that creates float arrays of the specified size.
         * 
         * <p>The returned function creates new float arrays with all elements initialized to 0.0f.</p>
         *
         * @return an IntFunction that creates float arrays
         */
        public static IntFunction<float[]> ofFloatArray() {
            return FLOAT_ARRAY;
        }

        /**
         * Returns an IntFunction that creates double arrays of the specified size.
         * 
         * <p>The returned function creates new double arrays with all elements initialized to 0.0.</p>
         *
         * @return an IntFunction that creates double arrays
         */
        public static IntFunction<double[]> ofDoubleArray() {
            return DOUBLE_ARRAY;
        }

        /**
         * Returns an IntFunction that creates String arrays of the specified size.
         * 
         * <p>The returned function creates new String arrays with all elements initialized to null.</p>
         *
         * @return an IntFunction that creates String arrays
         */
        public static IntFunction<String[]> ofStringArray() {
            return STRING_ARRAY;
        }

        /**
         * Returns an IntFunction that creates Object arrays of the specified size.
         * 
         * <p>The returned function creates new Object arrays with all elements initialized to null.</p>
         *
         * @return an IntFunction that creates Object arrays
         */
        public static IntFunction<Object[]> ofObjectArray() {
            return OBJECT_ARRAY;
        }

        /**
         * Returns an IntFunction that creates BooleanList instances with the specified initial capacity.
         * 
         * <p>BooleanList is a specialized list implementation for primitive boolean values,
         * avoiding boxing/unboxing overhead.</p>
         *
         * @return an IntFunction that creates BooleanList instances
         */
        public static IntFunction<BooleanList> ofBooleanList() {
            return BOOLEAN_LIST;
        }

        /**
         * Returns an IntFunction that creates CharList instances with the specified initial capacity.
         * 
         * <p>CharList is a specialized list implementation for primitive char values,
         * avoiding boxing/unboxing overhead.</p>
         *
         * @return an IntFunction that creates CharList instances
         */
        public static IntFunction<CharList> ofCharList() {
            return CHAR_LIST;
        }

        /**
         * Returns an IntFunction that creates ByteList instances with the specified initial capacity.
         * 
         * <p>ByteList is a specialized list implementation for primitive byte values,
         * avoiding boxing/unboxing overhead.</p>
         *
         * @return an IntFunction that creates ByteList instances
         */
        public static IntFunction<ByteList> ofByteList() {
            return BYTE_LIST;
        }

        /**
         * Returns an IntFunction that creates ShortList instances with the specified initial capacity.
         * 
         * <p>ShortList is a specialized list implementation for primitive short values,
         * avoiding boxing/unboxing overhead.</p>
         *
         * @return an IntFunction that creates ShortList instances
         */
        public static IntFunction<ShortList> ofShortList() {
            return SHORT_LIST;
        }

        /**
         * Returns an IntFunction that creates IntList instances with the specified initial capacity.
         * 
         * <p>IntList is a specialized list implementation for primitive int values,
         * avoiding boxing/unboxing overhead.</p>
         *
         * @return an IntFunction that creates IntList instances
         */
        public static IntFunction<IntList> ofIntList() {
            return INT_LIST;
        }

        /**
         * Returns an IntFunction that creates LongList instances with the specified initial capacity.
         * 
         * <p>LongList is a specialized list implementation for primitive long values,
         * avoiding boxing/unboxing overhead.</p>
         *
         * @return an IntFunction that creates LongList instances
         */
        public static IntFunction<LongList> ofLongList() {
            return LONG_LIST;
        }

        /**
         * Returns an IntFunction that creates FloatList instances with the specified initial capacity.
         * 
         * <p>FloatList is a specialized list implementation for primitive float values,
         * avoiding boxing/unboxing overhead.</p>
         *
         * @return an IntFunction that creates FloatList instances
         */
        public static IntFunction<FloatList> ofFloatList() {
            return FLOAT_LIST;
        }

        /**
         * Returns an IntFunction that creates DoubleList instances with the specified initial capacity.
         * 
         * <p>DoubleList is a specialized list implementation for primitive double values,
         * avoiding boxing/unboxing overhead.</p>
         *
         * @return an IntFunction that creates DoubleList instances
         */
        public static IntFunction<DoubleList> ofDoubleList() {
            return DOUBLE_LIST;
        }

        /**
         * Returns an IntFunction that creates ArrayList instances with the specified initial capacity.
         * 
         * <p>The returned function creates new ArrayList instances optimized with the given initial capacity
         * to avoid resizing during element addition.</p>
         *
         * @param <T> the type of elements in the list
         * @return an IntFunction that creates ArrayList instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<List<T>> ofList() {
            return (IntFunction) LIST_FACTORY;
        }

        /**
         * Returns an IntFunction that creates LinkedList instances.
         * 
         * <p>The returned function creates new LinkedList instances. Note that the capacity parameter
         * is ignored as LinkedList does not support initial capacity.</p>
         *
         * @param <T> the type of elements in the list
         * @return an IntFunction that creates LinkedList instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedList<T>> ofLinkedList() {
            return (IntFunction) LINKED_LIST_FACTORY;
        }

        /**
         * Returns an IntFunction that creates HashSet instances with the specified initial capacity.
         * 
         * <p>The returned function creates new HashSet instances optimized with the given initial capacity
         * to avoid rehashing during element addition.</p>
         *
         * @param <T> the type of elements in the set
         * @return an IntFunction that creates HashSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Set<T>> ofSet() {
            return (IntFunction) SET_FACTORY;
        }

        /**
         * Returns an IntFunction that creates LinkedHashSet instances with the specified initial capacity.
         * 
         * <p>The returned function creates new LinkedHashSet instances that maintain insertion order
         * and are optimized with the given initial capacity.</p>
         *
         * @param <T> the type of elements in the set
         * @return an IntFunction that creates LinkedHashSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Set<T>> ofLinkedHashSet() {
            return (IntFunction) LINKED_HASH_SET_FACTORY;
        }

        /**
         * Returns an IntFunction that creates TreeSet instances.
         * 
         * <p>The returned function creates new TreeSet instances that maintain elements in sorted order.
         * Note that the capacity parameter is ignored as TreeSet does not support initial capacity.</p>
         *
         * @param <T> the type of elements in the set
         * @return an IntFunction that creates TreeSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<SortedSet<T>> ofSortedSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Returns an IntFunction that creates TreeSet instances as NavigableSet.
         * 
         * <p>The returned function creates new TreeSet instances that provide navigation methods
         * for accessing elements relative to other elements. Note that the capacity parameter
         * is ignored as TreeSet does not support initial capacity.</p>
         *
         * @param <T> the type of elements in the set
         * @return an IntFunction that creates TreeSet instances as NavigableSet
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<NavigableSet<T>> ofNavigableSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Returns an IntFunction that creates TreeSet instances.
         * 
         * <p>The returned function creates new TreeSet instances that maintain elements in sorted order.
         * Note that the capacity parameter is ignored as TreeSet does not support initial capacity.</p>
         *
         * @param <T> the type of elements in the set
         * @return an IntFunction that creates TreeSet instances
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<TreeSet<T>> ofTreeSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Returns an IntFunction that creates LinkedList instances as Queue.
         * 
         * <p>The returned function creates new LinkedList instances that implement the Queue interface.
         * Note that the capacity parameter is ignored as LinkedList does not support initial capacity.</p>
         *
         * @param <T> the type of elements in the queue
         * @return an IntFunction that creates Queue instances (backed by LinkedList)
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Queue<T>> ofQueue() {
            return (IntFunction) QUEUE_FACTORY;
        }

        /**
         * Returns an IntFunction that creates LinkedList instances as Deque.
         * 
         * <p>The returned function creates new LinkedList instances that implement the Deque interface,
         * supporting element insertion and removal at both ends. Note that the capacity parameter
         * is ignored as LinkedList does not support initial capacity.</p>
         *
         * @param <T> the type of elements in the deque
         * @return an IntFunction that creates Deque instances (backed by LinkedList)
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Deque<T>> ofDeque() {
            return (IntFunction) DEQUE_FACTORY;
        }

        /**
         * Returns an IntFunction that creates ArrayDeque instances with the specified initial capacity.
         * The returned function can be used to create pre-sized ArrayDeque collections for performance optimization.
         *
         * @param <T> the type of elements to be stored in the ArrayDeque
         * @return an IntFunction that accepts an initial capacity and returns a new ArrayDeque instance
         * @see ArrayDeque#ArrayDeque(int)
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ArrayDeque<T>> ofArrayDeque() {
            return (IntFunction) ARRAY_DEQUE_FACTORY;
        }

        /**
         * Returns an IntFunction that creates LinkedBlockingQueue instances with the specified initial capacity.
         * The returned function can be used to create thread-safe blocking queues with bounded capacity.
         *
         * @param <T> the type of elements to be stored in the LinkedBlockingQueue
         * @return an IntFunction that accepts an initial capacity and returns a new LinkedBlockingQueue instance
         * @see LinkedBlockingQueue#LinkedBlockingQueue(int)
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (IntFunction) LINKED_BLOCKING_QUEUE_FACTORY;
        }

        /**
         * Returns an IntFunction that creates ArrayBlockingQueue instances with the specified capacity.
         * The returned function creates fixed-size, thread-safe blocking queues backed by an array.
         *
         * @param <T> the type of elements to be stored in the ArrayBlockingQueue
         * @return an IntFunction that accepts a capacity and returns a new ArrayBlockingQueue instance
         * @see ArrayBlockingQueue#ArrayBlockingQueue(int)
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ArrayBlockingQueue<T>> ofArrayBlockingQueue() {
            return (IntFunction) ARRAY_BLOCKING_QUEUE_FACTORY;
        }

        /**
         * Returns an IntFunction that creates LinkedBlockingDeque instances with the specified initial capacity.
         * The returned function creates thread-safe, optionally-bounded blocking deques based on linked nodes.
         *
         * @param <T> the type of elements to be stored in the LinkedBlockingDeque
         * @return an IntFunction that accepts an initial capacity and returns a new LinkedBlockingDeque instance
         * @see LinkedBlockingDeque#LinkedBlockingDeque(int)
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedBlockingDeque<T>> ofLinkedBlockingDeque() {
            return (IntFunction) LINKED_BLOCKING_DEQUE_FACTORY;
        }

        /**
         * Returns an IntFunction that creates ConcurrentLinkedQueue instances.
         * The returned function creates unbounded thread-safe queues based on linked nodes.
         * Note: The capacity parameter is ignored as ConcurrentLinkedQueue is always unbounded.
         *
         * @param <T> the type of elements to be stored in the ConcurrentLinkedQueue
         * @return an IntFunction that accepts a capacity (ignored) and returns a new ConcurrentLinkedQueue instance
         * @see ConcurrentLinkedQueue#ConcurrentLinkedQueue()
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (IntFunction) CONCURRENT_LINKED_QUEUE_FACTORY;
        }

        /**
         * Returns an IntFunction that creates PriorityQueue instances with the specified initial capacity.
         * The returned function creates unbounded priority queues based on a priority heap.
         *
         * @param <T> the type of elements to be stored in the PriorityQueue
         * @return an IntFunction that accepts an initial capacity and returns a new PriorityQueue instance
         * @see PriorityQueue#PriorityQueue(int)
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<PriorityQueue<T>> ofPriorityQueue() {
            return (IntFunction) PRIORITY_QUEUE_FACTORY;
        }

        /**
         * Returns an IntFunction that creates HashMap instances with the specified initial capacity.
         * The returned function creates hash table based Map implementations.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts an initial capacity and returns a new HashMap instance
         * @see HashMap#HashMap(int)
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<Map<K, V>> ofMap() {
            return (IntFunction) MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates LinkedHashMap instances with the specified initial capacity.
         * The returned function creates hash table and linked list implementations of the Map interface,
         * with predictable iteration order.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts an initial capacity and returns a new LinkedHashMap instance
         * @see LinkedHashMap#LinkedHashMap(int)
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<Map<K, V>> ofLinkedHashMap() {
            return (IntFunction) LINKED_HASH_MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates IdentityHashMap instances with the specified expected maximum size.
         * The returned function creates maps that use reference-equality instead of object-equality when comparing keys.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts an expected maximum size and returns a new IdentityHashMap instance
         * @see IdentityHashMap#IdentityHashMap(int)
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<IdentityHashMap<K, V>> ofIdentityHashMap() {
            return (IntFunction) IDENTITY_HASH_MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates TreeMap instances.
         * The returned function creates Red-Black tree based NavigableMap implementations.
         * Note: The capacity parameter is ignored as TreeMap doesn't have a capacity constructor.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts a capacity (ignored) and returns a new TreeMap instance as SortedMap
         * @see TreeMap#TreeMap()
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<SortedMap<K, V>> ofSortedMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates TreeMap instances.
         * The returned function creates Red-Black tree based NavigableMap implementations.
         * Note: The capacity parameter is ignored as TreeMap doesn't have a capacity constructor.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts a capacity (ignored) and returns a new TreeMap instance as NavigableMap
         * @see TreeMap#TreeMap()
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<NavigableMap<K, V>> ofNavigableMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates TreeMap instances.
         * The returned function creates Red-Black tree based NavigableMap implementations.
         * Note: The capacity parameter is ignored as TreeMap doesn't have a capacity constructor.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts a capacity (ignored) and returns a new TreeMap instance
         * @see TreeMap#TreeMap()
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<TreeMap<K, V>> ofTreeMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates ConcurrentHashMap instances with the specified initial capacity.
         * The returned function creates thread-safe hash table based Map implementations.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts an initial capacity and returns a new ConcurrentHashMap instance as ConcurrentMap
         * @see ConcurrentHashMap#ConcurrentHashMap(int)
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates ConcurrentHashMap instances with the specified initial capacity.
         * The returned function creates thread-safe hash table based Map implementations.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts an initial capacity and returns a new ConcurrentHashMap instance
         * @see ConcurrentHashMap#ConcurrentHashMap(int)
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates BiMap instances with the specified initial capacity.
         * BiMap is a bidirectional map that preserves the uniqueness of its values as well as that of its keys.
         *
         * @param <K> the type of keys maintained by the BiMap
         * @param <V> the type of mapped values
         * @return an IntFunction that accepts an initial capacity and returns a new BiMap instance
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<BiMap<K, V>> ofBiMap() {
            return (IntFunction) BI_MAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates Multiset instances with the specified initial capacity.
         * A Multiset is a collection that supports order-independent equality and may contain duplicate elements.
         *
         * @param <T> the type of elements in the Multiset
         * @return an IntFunction that accepts an initial capacity and returns a new Multiset instance
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Multiset<T>> ofMultiset() {
            return (IntFunction) MULTISET_FACTORY;
        }

        /**
         * Returns an IntFunction that creates ListMultimap instances with the specified initial capacity.
         * A ListMultimap is a Multimap that can hold duplicate key-value pairs and maintains insertion ordering of values for a given key.
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @return an IntFunction that accepts an initial capacity and returns a new ListMultimap instance
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> IntFunction<ListMultimap<K, E>> ofListMultimap() {
            return (IntFunction) LIST_MULTIMAP_FACTORY;
        }

        /**
         * Returns an IntFunction that creates SetMultimap instances with the specified initial capacity.
         * A SetMultimap is a Multimap that cannot hold duplicate key-value pairs.
         *
         * @param <K> the type of keys maintained by the multimap
         * @param <E> the type of mapped values
         * @return an IntFunction that accepts an initial capacity and returns a new SetMultimap instance
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> IntFunction<SetMultimap<K, E>> ofSetMultimap() {
            return (IntFunction) SET_MULTIMAP_FACTORY;
        }

        /**
         * Returns a new created stateful IntFunction whose apply method will return the same DisposableObjArray instance.
         * The DisposableObjArray is created lazily on first call and reused for subsequent calls.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @return a stateful IntFunction that returns a reusable DisposableObjArray instance
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
         * Returns a new created stateful IntFunction whose apply method will return the same DisposableArray instance.
         * The DisposableArray is created lazily on first call with the specified component type and reused for subsequent calls.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the component type of the array
         * @param componentType the Class object representing the component type of the array
         * @return a stateful IntFunction that returns a reusable DisposableArray instance
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
         * Returns an IntFunction that creates Collection instances of the specified target type with the given initial capacity.
         * This method supports various Collection implementations and attempts to find an appropriate constructor or factory method.
         * The returned IntFunction is cached for performance optimization.
         *
         * @param <T> the type of elements in the collection
         * @param targetType the Class object representing the desired Collection implementation
         * @return an IntFunction that accepts an initial capacity and returns a new Collection instance of the specified type
         * @throws IllegalArgumentException if targetType is not a Collection class or if no suitable constructor/factory can be found
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
         * Returns an IntFunction that creates Map instances of the specified target type with the given initial capacity.
         * This method supports various Map implementations and attempts to find an appropriate constructor or factory method.
         * The returned IntFunction is cached for performance optimization.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param targetType the Class object representing the desired Map implementation
         * @return an IntFunction that accepts an initial capacity and returns a new Map instance of the specified type
         * @throws IllegalArgumentException if targetType is not a Map class or if no suitable constructor/factory can be found
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
         * Registers a custom IntFunction creator for the specified Collection target class.
         * The registered creator will be used by ofCollection method to create instances of the target class.
         * Built-in collection classes cannot be registered.
         *
         * @param <T> the type of Collection to register
         * @param targetClass the Class object representing the Collection type to register
         * @param creator the IntFunction that creates instances of the target class with specified capacity
         * @return true if the registration was successful, false if a creator was already registered for this class
         * @throws IllegalArgumentException if targetClass or creator is null, or if targetClass is a built-in class
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
         * Registers a custom IntFunction creator for the specified Map target class.
         * The registered creator will be used by ofMap method to create instances of the target class.
         * Built-in map classes cannot be registered.
         *
         * @param <T> the type of Map to register
         * @param targetClass the Class object representing the Map type to register
         * @param creator the IntFunction that creates instances of the target class with specified capacity
         * @return true if the registration was successful, false if a creator was already registered for this class
         * @throws IllegalArgumentException if targetClass or creator is null, or if targetClass is a built-in class
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
         * Returns an IntFunction for creating ImmutableList instances.
         * This operation is not supported.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always thrown as this operation is not supported
         * @deprecated unsupported operation
         */
        @Deprecated
        public static IntFunction<ImmutableList<?>> ofImmutableList() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns an IntFunction for creating ImmutableSet instances.
         * This operation is not supported.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always thrown as this operation is not supported
         * @deprecated unsupported operation
         */
        @Deprecated
        public static IntFunction<ImmutableSet<?>> ofImmutableSet() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns an IntFunction for creating ImmutableMap instances.
         * This operation is not supported.
         *
         * @return never returns normally
         * @throws UnsupportedOperationException always thrown as this operation is not supported
         * @deprecated unsupported operation
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
     * Factory class that extends IntFunctions to provide factory methods for creating collection and map instances.
     * This class serves as a utility class and cannot be instantiated.
     */
    public static final class Factory extends IntFunctions {
        private Factory() {
            // utility class
        }
    }

    /**
     * Utility class providing various Predicate implementations and factory methods.
     * This class contains methods for creating stateful, indexed, and specialized predicates.
     */
    public static final class Predicates {

        private Predicates() {
        }

        /**
         * Returns a stateful Predicate that tests elements based on their index position.
         * The predicate maintains an internal counter that increments with each test.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the input to the predicate
         * @param predicate the IntObjPredicate that accepts an index and element for testing
         * @return a stateful Predicate that applies the given IntObjPredicate with an incrementing index
         * @throws IllegalArgumentException if predicate is null
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
         * Returns a stateful Predicate that maintains a set of seen elements and returns true only for distinct elements.
         * The predicate uses a HashSet internally to track previously seen elements.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the input to the predicate
         * @return a stateful Predicate that returns true for first occurrence of each distinct element
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
         * Returns a stateful Predicate that maintains distinct elements based on a key extracted by the mapper function.
         * The predicate returns true only for elements whose mapped keys haven't been seen before.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the input to the predicate
         * @param mapper the function to extract the key for distinctness comparison
         * @return a stateful Predicate that returns true for elements with distinct mapped keys
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
         * Returns a stateful Predicate that maintains a concurrent set of seen elements and returns true only for distinct elements.
         * This predicate is thread-safe and can be used in parallel streams.
         * This method is marked as Beta and Stateful, indicating it should not be saved or cached for reuse.
         *
         * @param <T> the type of the input to the predicate
         * @return a stateful thread-safe Predicate that returns true for first occurrence of each distinct element
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
         * Returns a stateful Predicate that maintains distinct elements based on a key extracted by the mapper function.
         * This predicate is thread-safe and can be used in parallel streams.
         * This method is marked as Beta and Stateful, indicating it should not be saved or cached for reuse.
         *
         * @param <T> the type of the input to the predicate
         * @param mapper the function to extract the key for distinctness comparison
         * @return a stateful thread-safe Predicate that returns true for elements with distinct mapped keys
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
         * Returns a stateful Predicate that removes continuous repeat elements.
         * The predicate returns false for elements that are equal to the immediately preceding element.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the input to the predicate
         * @return a stateful Predicate that returns true for elements different from their immediate predecessor
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
     * Utility class providing various BiPredicate implementations and factory methods.
     * This class contains predefined BiPredicates and methods for creating indexed BiPredicates.
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
         * Returns a BiPredicate that always returns true regardless of input.
         *
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @return a BiPredicate that always returns true
         */
        public static <T, U> BiPredicate<T, U> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        /**
         * Returns a BiPredicate that always returns false regardless of input.
         *
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @return a BiPredicate that always returns false
         */
        public static <T, U> BiPredicate<T, U> alwaysFalse() {
            return ALWAYS_FALSE;
        }

        /**
         * Returns a stateful BiPredicate that tests elements based on their index position.
         * The predicate maintains an internal counter that increments with each test.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param predicate the IntBiObjPredicate that accepts an index and two elements for testing
         * @return a stateful BiPredicate that applies the given IntBiObjPredicate with an incrementing index
         * @throws IllegalArgumentException if predicate is null
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
     * Utility class providing various TriPredicate implementations and factory methods.
     * This class contains predefined TriPredicates for common operations.
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
         * Returns a TriPredicate that always returns true regardless of input.
         *
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @return a TriPredicate that always returns true
         */
        public static <A, B, C> TriPredicate<A, B, C> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        /**
         * Returns a TriPredicate that always returns false regardless of input.
         *
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @return a TriPredicate that always returns false
         */
        public static <A, B, C> TriPredicate<A, B, C> alwaysFalse() {
            return ALWAYS_FALSE;
        }

    }

    /**
     * Utility class providing various Consumer implementations and factory methods.
     * This class contains methods for creating indexed consumers.
     */
    public static final class Consumers {
        private Consumers() {
        }

        /**
         * Returns a stateful Consumer that accepts elements based on their index position.
         * The consumer maintains an internal counter that increments with each accept call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the input to the consumer
         * @param action the IntObjConsumer that accepts an index and element
         * @return a stateful Consumer that applies the given IntObjConsumer with an incrementing index
         * @throws IllegalArgumentException if action is null
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
     * Utility class providing various BiConsumer implementations and factory methods.
     * This class contains predefined BiConsumers for common collection and map operations.
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
         * Returns a BiConsumer that does nothing.
         *
         * @param <T> the type of the first argument to the consumer
         * @param <U> the type of the second argument to the consumer
         * @return a BiConsumer that performs no operation
         */
        public static <T, U> BiConsumer<T, U> doNothing() {
            return DO_NOTHING;
        }

        /**
         * Returns a BiConsumer that adds an element to a collection.
         * The BiConsumer calls Collection.add(element) on the first argument with the second argument.
         *
         * @param <T> the type of element to add
         * @param <C> the type of collection
         * @return a BiConsumer that adds the second argument to the first argument collection
         */
        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofAdd() {
            return (BiConsumer<C, T>) ADD;
        }

        /**
         * Returns a BiConsumer that adds all elements from one collection to another.
         * The BiConsumer calls Collection.addAll(collection) on the first argument with the second argument.
         *
         * @param <T> the type of elements in the collections
         * @param <C> the type of collection
         * @return a BiConsumer that adds all elements from the second collection to the first collection
         */
        public static <T, C extends Collection<T>> BiConsumer<C, C> ofAddAll() {
            return (BiConsumer<C, C>) ADD_ALL;
        }

        /**
         * Returns a BiConsumer that adds all elements from one PrimitiveList to another.
         * The BiConsumer calls PrimitiveList.addAll(list) on the first argument with the second argument.
         *
         * @param <T> the type of PrimitiveList
         * @return a BiConsumer that adds all elements from the second PrimitiveList to the first PrimitiveList
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiConsumer<T, T> ofAddAlll() {
            return (BiConsumer<T, T>) ADD_ALL_2;
        }

        /**
         * Returns a BiConsumer that removes an element from a collection.
         * The BiConsumer calls Collection.remove(element) on the first argument with the second argument.
         *
         * @param <T> the type of element to remove
         * @param <C> the type of collection
         * @return a BiConsumer that removes the second argument from the first argument collection
         */
        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofRemove() {
            return (BiConsumer<C, T>) REMOVE;
        }

        /**
         * Returns a BiConsumer that removes all elements of one collection from another.
         * The BiConsumer calls Collection.removeAll(collection) on the first argument with the second argument.
         *
         * @param <T> the type of elements in the collections
         * @param <C> the type of collection
         * @return a BiConsumer that removes all elements in the second collection from the first collection
         */
        public static <T, C extends Collection<T>> BiConsumer<C, C> ofRemoveAll() {
            return (BiConsumer<C, C>) REMOVE_ALL;
        }

        /**
         * Returns a BiConsumer that removes all elements of one PrimitiveList from another.
         * The BiConsumer calls PrimitiveList.removeAll(list) on the first argument with the second argument.
         *
         * @param <T> the type of PrimitiveList
         * @return a BiConsumer that removes all elements in the second PrimitiveList from the first PrimitiveList
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiConsumer<T, T> ofRemoveAlll() {
            return (BiConsumer<T, T>) REMOVE_ALL_2;
        }

        /**
         * Returns a BiConsumer that puts a Map.Entry into a Map.
         * The BiConsumer extracts the key and value from the entry and puts them into the map.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @param <E> the type of map entry
         * @return a BiConsumer that puts the entry into the map
         */
        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiConsumer<M, E> ofPut() {
            return (BiConsumer<M, E>) PUT;
        }

        /**
         * Returns a BiConsumer that puts all entries from one map into another.
         * The BiConsumer calls Map.putAll(map) on the first argument with the second argument.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @return a BiConsumer that puts all entries from the second map into the first map
         */
        public static <K, V, M extends Map<K, V>> BiConsumer<M, M> ofPutAll() {
            return (BiConsumer<M, M>) PUT_ALL;
        }

        /**
         * Returns a BiConsumer that removes an entry from a map by key.
         * The BiConsumer calls Map.remove(key) on the first argument with the second argument as the key.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @return a BiConsumer that removes the entry with the given key from the map
         */
        public static <K, V, M extends Map<K, V>> BiConsumer<M, K> ofRemoveByKey() {
            return (BiConsumer<M, K>) REMOVE_BY_KEY;
        }

        /**
         * Returns a BiConsumer that merges two Joiner instances.
         * The BiConsumer calls Joiner.merge(joiner) on the first argument with the second argument.
         *
         * @return a BiConsumer that merges the second Joiner into the first Joiner
         */
        public static BiConsumer<Joiner, Joiner> ofMerge() {
            return MERGE;
        }

        /**
         * Returns a BiConsumer that appends an object to a StringBuilder.
         * The BiConsumer calls StringBuilder.append(object) on the first argument with the second argument.
         *
         * @param <T> the type of object to append
         * @return a BiConsumer that appends the second argument to the first argument StringBuilder
         */
        public static <T> BiConsumer<StringBuilder, T> ofAppend() {
            return (BiConsumer<StringBuilder, T>) APPEND;
        }

        /**
         * Returns a stateful BiConsumer that accepts elements based on their index position.
         * The consumer maintains an internal counter that increments with each accept call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the first argument to the consumer
         * @param <U> the type of the second argument to the consumer
         * @param action the IntBiObjConsumer that accepts an index and two elements
         * @return a stateful BiConsumer that applies the given IntBiObjConsumer with an incrementing index
         * @throws IllegalArgumentException if action is null
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
     * Utility class providing various TriConsumer implementations and factory methods.
     * This class is reserved for future TriConsumer utilities.
     */
    public static final class TriConsumers {
        private TriConsumers() {
        }
    }

    /**
     * Utility class providing various Function implementations and factory methods.
     * This class contains methods for creating indexed functions.
     */
    public static final class Functions {

        private Functions() {
        }

        /**
         * Returns a stateful Function that applies a function based on element index position.
         * The function maintains an internal counter that increments with each apply call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param func the IntObjFunction that accepts an index and element and produces a result
         * @return a stateful Function that applies the given IntObjFunction with an incrementing index
         * @throws IllegalArgumentException if func is null
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
     * Utility class providing various BiFunction implementations and factory methods.
     * This class contains predefined BiFunctions for common collection and map operations.
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
         * Returns a BiFunction that always returns the first argument.
         *
         * @param <T> the type of the first argument and result
         * @param <U> the type of the second argument
         * @return a BiFunction that returns the first argument
         */
        public static <T, U> BiFunction<T, U, T> selectFirst() {
            return (BiFunction<T, U, T>) RETURN_FIRST;
        }

        /**
         * Returns a BiFunction that always returns the second argument.
         *
         * @param <T> the type of the first argument
         * @param <U> the type of the second argument and result
         * @return a BiFunction that returns the second argument
         */
        public static <T, U> BiFunction<T, U, U> selectSecond() {
            return (BiFunction<T, U, U>) RETURN_SECOND;
        }

        /**
         * Returns a BiFunction that adds an element to a collection and returns the collection.
         * The BiFunction calls Collection.add(element) and returns the modified collection.
         *
         * @param <T> the type of element to add
         * @param <C> the type of collection
         * @return a BiFunction that adds the second argument to the first argument collection and returns the collection
         */
        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofAdd() {
            return (BiFunction<C, T, C>) ADD;
        }

        /**
         * Returns a BiFunction that adds all elements from one collection to another and returns the target collection.
         * The BiFunction calls Collection.addAll(collection) and returns the modified collection.
         *
         * @param <T> the type of elements in the collections
         * @param <C> the type of collection
         * @return a BiFunction that adds all elements from the second collection to the first and returns the first collection
         */
        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofAddAll() {
            return (BiFunction<C, C, C>) ADD_ALL;
        }

        /**
         * Returns a BiFunction that adds all elements from one PrimitiveList to another and returns the target list.
         * The BiFunction calls PrimitiveList.addAll(list) and returns the modified list.
         *
         * @param <T> the type of PrimitiveList
         * @return a BiFunction that adds all elements from the second PrimitiveList to the first and returns the first list
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiFunction<T, T, T> ofAddAlll() {
            return (BiFunction<T, T, T>) ADD_ALL_2;
        }

        /**
         * Returns a BiFunction that removes an element from a collection and returns the collection.
         * The BiFunction calls Collection.remove(element) and returns the modified collection.
         *
         * @param <T> the type of element to remove
         * @param <C> the type of collection
         * @return a BiFunction that removes the second argument from the first argument collection and returns the collection
         */
        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofRemove() {
            return (BiFunction<C, T, C>) REMOVE;
        }

        /**
         * Returns a BiFunction that removes all elements of one collection from another and returns the target collection.
         * The BiFunction calls Collection.removeAll(collection) and returns the modified collection.
         *
         * @param <T> the type of elements in the collections
         * @param <C> the type of collection
         * @return a BiFunction that removes all elements in the second collection from the first and returns the first collection
         */
        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofRemoveAll() {
            return (BiFunction<C, C, C>) REMOVE_ALL;
        }

        /**
         * Returns a BiFunction that removes all elements of one PrimitiveList from another and returns the target list.
         * The BiFunction calls PrimitiveList.removeAll(list) and returns the modified list.
         *
         * @param <T> the type of PrimitiveList
         * @return a BiFunction that removes all elements in the second PrimitiveList from the first and returns the first list
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiFunction<T, T, T> ofRemoveAlll() {
            return (BiFunction<T, T, T>) REMOVE_ALL_2;
        }

        /**
         * Returns a BiFunction that puts a Map.Entry into a Map and returns the map.
         * The BiFunction extracts the key and value from the entry, puts them into the map, and returns the map.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @param <E> the type of map entry
         * @return a BiFunction that puts the entry into the map and returns the map
         */
        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiFunction<M, E, M> ofPut() {
            return (BiFunction<M, E, M>) PUT;
        }

        /**
         * Returns a BiFunction that puts all entries from one map into another and returns the target map.
         * The BiFunction calls Map.putAll(map) and returns the modified map.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @return a BiFunction that puts all entries from the second map into the first and returns the first map
         */
        public static <K, V, M extends Map<K, V>> BiFunction<M, M, M> ofPutAll() {
            return (BiFunction<M, M, M>) PUT_ALL;
        }

        /**
         * Returns a BiFunction that removes an entry from a map by key and returns the map.
         * The BiFunction calls Map.remove(key) and returns the modified map.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @return a BiFunction that removes the entry with the given key from the map and returns the map
         */
        public static <K, V, M extends Map<K, V>> BiFunction<M, K, M> ofRemoveByKey() {
            return (BiFunction<M, K, M>) REMOVE_BY_KEY;
        }

        /**
         * Returns a BiFunction that merges two Joiner instances and returns the result.
         * The BiFunction calls Joiner.merge(joiner) and returns the merged Joiner.
         *
         * @return a BiFunction that merges the second Joiner into the first and returns the result
         */
        public static BiFunction<Joiner, Joiner, Joiner> ofMerge() {
            return MERGE;
        }

        /**
         * Returns a BiFunction that appends an object to a StringBuilder and returns the StringBuilder.
         * The BiFunction calls StringBuilder.append(object) and returns the modified StringBuilder.
         *
         * @param <T> the type of object to append
         * @return a BiFunction that appends the second argument to the first argument StringBuilder and returns the StringBuilder
         */
        public static <T> BiFunction<StringBuilder, T, StringBuilder> ofAppend() {
            return (BiFunction<StringBuilder, T, StringBuilder>) APPEND;
        }

        /**
         * Returns a stateful BiFunction that applies a function based on element index position.
         * The function maintains an internal counter that increments with each apply call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param func the IntBiObjFunction that accepts an index and two elements and produces a result
         * @return a stateful BiFunction that applies the given IntBiObjFunction with an incrementing index
         * @throws IllegalArgumentException if func is null
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
     * Utility class providing various TriFunction implementations and factory methods.
     * This class is reserved for future TriFunction utilities.
     */
    public static final class TriFunctions {

        private TriFunctions() {
        }
    }

    /**
     * Utility class providing various BinaryOperator implementations and factory methods.
     * This class contains predefined BinaryOperators for common merge and combination operations.
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
         * Returns a BinaryOperator that adds all elements from the second collection to the first.
         * This method is deprecated, use ofAddAllToFirst() instead.
         *
         * @param <T> the type of elements in the collection
         * @param <C> the type of collection
         * @return a BinaryOperator that adds all elements from the second collection to the first and returns the first
         * @deprecated replaced by {@code #ofAddAllToFirst()}
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAll() {
            return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
        }

        /**
         * Returns a BinaryOperator that adds all elements from the second collection to the first.
         * The operator modifies and returns the first collection.
         *
         * @param <T> the type of elements in the collection
         * @param <C> the type of collection
         * @return a BinaryOperator that adds all elements from the second collection to the first and returns the first
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToFirst() {
            return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
        }

        /**
         * Returns a BinaryOperator that adds all elements to the bigger collection.
         * The operator compares sizes and adds the smaller collection to the larger one, returning the larger.
         *
         * @param <T> the type of elements in the collection
         * @param <C> the type of collection
         * @return a BinaryOperator that adds all elements to the bigger collection and returns it
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToBigger() {
            return (BinaryOperator<C>) ADD_ALL_TO_BIGGER;
        }

        /**
         * Returns a BinaryOperator that removes all elements of the second collection from the first.
         * This method is deprecated, use ofRemoveAllFromFirst() instead.
         *
         * @param <T> the type of elements in the collection
         * @param <C> the type of collection
         * @return a BinaryOperator that removes all elements of the second collection from the first and returns the first
         * @deprecated replaced by {@code #ofRemoveAllFromFirst()}
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAll() {
            return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
        }

        /**
         * Returns a BinaryOperator that removes all elements of the second collection from the first.
         * The operator modifies and returns the first collection.
         *
         * @param <T> the type of elements in the collection
         * @param <C> the type of collection
         * @return a BinaryOperator that removes all elements of the second collection from the first and returns the first
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAllFromFirst() {
            return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
        }

        /**
         * Returns a BinaryOperator that puts all entries from the second map into the first.
         * This method is deprecated, use ofPutAllToFirst() instead.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @return a BinaryOperator that puts all entries from the second map into the first and returns the first
         * @deprecated replaced by {@code #ofPutAllToFirst()}
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAll() {
            return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
        }

        /**
         * Returns a BinaryOperator that puts all entries from the second map into the first.
         * The operator modifies and returns the first map.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @return a BinaryOperator that puts all entries from the second map into the first and returns the first
         */
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToFirst() {
            return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
        }

        /**
         * Returns a BinaryOperator that puts all entries into the bigger map.
         * The operator compares sizes and puts the smaller map into the larger one, returning the larger.
         *
         * @param <K> the type of keys maintained by the map
         * @param <V> the type of mapped values
         * @param <M> the type of map
         * @return a BinaryOperator that puts all entries into the bigger map and returns it
         */
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToBigger() {
            return (BinaryOperator<M>) PUT_ALL_TO_BIGGER;
        }

        /**
         * Returns a BinaryOperator that merges two Joiners.
         * This method is deprecated, use ofMergeToFirst() instead.
         *
         * @return a BinaryOperator that merges the second Joiner into the first and returns the first
         * @deprecated replaced by {@code #ofMergeToFirst}
         */
        @Deprecated
        public static BinaryOperator<Joiner> ofMerge() {
            return MERGE_TO_FIRST;
        }

        /**
         * Returns a BinaryOperator that merges the second Joiner into the first.
         * The operator modifies and returns the first Joiner.
         *
         * @return a BinaryOperator that merges the second Joiner into the first and returns the first
         */
        public static BinaryOperator<Joiner> ofMergeToFirst() {
            return MERGE_TO_FIRST;
        }

        /**
         * Returns a BinaryOperator that merges to the bigger Joiner.
         * The operator compares lengths and merges the smaller Joiner into the larger one, returning the larger.
         *
         * @return a BinaryOperator that merges to the bigger Joiner and returns it
         */
        public static BinaryOperator<Joiner> ofMergeToBigger() {
            return MERGE_TO_BIGGER;
        }

        /**
         * Returns a BinaryOperator that appends the second StringBuilder to the first.
         * This method is deprecated, use ofAppendToFirst() instead.
         *
         * @return a BinaryOperator that appends the second StringBuilder to the first and returns the first
         * @deprecated replaced by {@code #ofAppendToFirst()}
         */
        @Deprecated
        public static BinaryOperator<StringBuilder> ofAppend() {
            return APPEND_TO_FIRST;
        }

        /**
         * Returns a BinaryOperator that appends the second StringBuilder to the first.
         * The operator modifies and returns the first StringBuilder.
         *
         * @return a BinaryOperator that appends the second StringBuilder to the first and returns the first
         */
        public static BinaryOperator<StringBuilder> ofAppendToFirst() {
            return APPEND_TO_FIRST;
        }

        /**
         * Returns a BinaryOperator that appends to the bigger StringBuilder.
         * The operator compares lengths and appends the smaller StringBuilder to the larger one, returning the larger.
         *
         * @return a BinaryOperator that appends to the bigger StringBuilder and returns it
         */
        public static BinaryOperator<StringBuilder> ofAppendToBigger() {
            return APPEND_TO_BIGGER;
        }

        /**
         * Returns a BinaryOperator that concatenates two strings.
         * The operator performs string concatenation using the + operator.
         *
         * @return a BinaryOperator that concatenates two strings
         */
        public static BinaryOperator<String> ofConcat() {
            return CONCAT;
        }

        /**
         * Returns a BinaryOperator that adds two Integer values.
         * The operator uses Integer.sum for addition.
         *
         * @return a BinaryOperator that adds two Integer values
         */
        public static BinaryOperator<Integer> ofAddInt() {
            return ADD_INTEGER;
        }

        /**
         * Returns a BinaryOperator that adds two Long values.
         * The operator uses Long.sum for addition.
         *
         * @return a BinaryOperator that adds two Long values
         */
        public static BinaryOperator<Long> ofAddLong() {
            return ADD_LONG;
        }

        /**
         * Returns a BinaryOperator that adds two Double values.
         * The operator uses Double.sum for addition.
         *
         * @return a BinaryOperator that adds two Double values
         */
        public static BinaryOperator<Double> ofAddDouble() {
            return ADD_DOUBLE;
        }

        /**
         * Returns a BinaryOperator that adds two BigInteger values.
         * The operator uses BigInteger.add for addition.
         *
         * @return a BinaryOperator that adds two BigInteger values
         */
        public static BinaryOperator<BigInteger> ofAddBigInteger() {
            return ADD_BIG_INTEGER;
        }

        /**
         * Returns a BinaryOperator that adds two BigDecimal values.
         * The operator uses BigDecimal.add for addition.
         *
         * @return a BinaryOperator that adds two BigDecimal values
         */
        public static BinaryOperator<BigDecimal> ofAddBigDecimal() {
            return ADD_BIG_DECIMAL;
        }
    }

    /**
     * Utility class providing various UnaryOperator implementations and factory methods.
     * This class contains the identity operator.
     */
    public static final class UnaryOperators {

        /** The Constant IDENTITY. */
        @SuppressWarnings("rawtypes")
        private static final UnaryOperator IDENTITY = t -> t;

        private UnaryOperators() {
        }

        /**
         * Returns a UnaryOperator that always returns its input argument unchanged.
         * This is the identity function for UnaryOperator.
         *
         * @param <T> the type of the operand and result of the operator
         * @return a UnaryOperator that returns its input argument
         */
        public static <T> UnaryOperator<T> identity() {
            return IDENTITY;
        }
    }

    /**
     * Utility class providing functions for working with Map.Entry objects.
     * This class contains adapter methods to convert BiFunction/BiPredicate/BiConsumer to work with Map.Entry.
     */
    public static final class Entries {

        private Entries() {
        }

        /**
         * Adapts a BiFunction to work with Map.Entry by extracting key and value.
         * The returned function applies the BiFunction to the entry's key and value.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <T> the type of the result of the function
         * @param f the BiFunction to adapt
         * @return a Function that extracts key and value from an entry and applies the BiFunction
         * @throws IllegalArgumentException if f is null
         */
        public static <K, V, T> Function<Map.Entry<K, V>, T> f(final java.util.function.BiFunction<? super K, ? super V, ? extends T> f)
                throws IllegalArgumentException {
            N.checkArgNotNull(f, cs.BiFunction);

            return e -> f.apply(e.getKey(), e.getValue());
        }

        /**
         * Adapts a BiPredicate to work with Map.Entry by extracting key and value.
         * The returned predicate tests the entry by applying the BiPredicate to its key and value.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param p the BiPredicate to adapt
         * @return a Predicate that extracts key and value from an entry and applies the BiPredicate
         * @throws IllegalArgumentException if p is null
         */
        public static <K, V> Predicate<Map.Entry<K, V>> p(final java.util.function.BiPredicate<? super K, ? super V> p) throws IllegalArgumentException {
            N.checkArgNotNull(p, cs.BiPredicate);

            return e -> p.test(e.getKey(), e.getValue());
        }

        /**
         * Adapts a BiConsumer to work with Map.Entry by extracting key and value.
         * The returned consumer accepts the entry by applying the BiConsumer to its key and value.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param c the BiConsumer to adapt
         * @return a Consumer that extracts key and value from an entry and applies the BiConsumer
         * @throws IllegalArgumentException if c is null
         */
        public static <K, V> Consumer<Map.Entry<K, V>> c(final java.util.function.BiConsumer<? super K, ? super V> c) throws IllegalArgumentException {
            N.checkArgNotNull(c, cs.BiConsumer);

            return e -> c.accept(e.getKey(), e.getValue());
        }

        /**
         * Adapts a Throwables.BiFunction to work with Map.Entry by extracting key and value.
         * The returned function applies the BiFunction to the entry's key and value.
         * This method is marked as Beta.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <T> the type of the result of the function
         * @param <E> the type of exception that may be thrown
         * @param f the Throwables.BiFunction to adapt
         * @return a Throwables.Function that extracts key and value from an entry and applies the BiFunction
         * @throws IllegalArgumentException if f is null
         */
        @Beta
        public static <K, V, T, E extends Exception> Throwables.Function<Map.Entry<K, V>, T, E> ef(
                final Throwables.BiFunction<? super K, ? super V, ? extends T, E> f) throws IllegalArgumentException {
            N.checkArgNotNull(f, cs.BiFunction);

            return e -> f.apply(e.getKey(), e.getValue());
        }

        /**
         * Adapts a Throwables.BiPredicate to work with Map.Entry by extracting key and value.
         * The returned predicate tests the entry by applying the BiPredicate to its key and value.
         * This method is marked as Beta.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <E> the type of exception that may be thrown
         * @param p the Throwables.BiPredicate to adapt
         * @return a Throwables.Predicate that extracts key and value from an entry and applies the BiPredicate
         * @throws IllegalArgumentException if p is null
         */
        @Beta
        public static <K, V, E extends Exception> Throwables.Predicate<Map.Entry<K, V>, E> ep(final Throwables.BiPredicate<? super K, ? super V, E> p)
                throws IllegalArgumentException {
            N.checkArgNotNull(p, cs.BiPredicate);

            return e -> p.test(e.getKey(), e.getValue());
        }

        /**
         * Adapts a Throwables.BiConsumer to work with Map.Entry by extracting key and value.
         * The returned consumer accepts the entry by applying the BiConsumer to its key and value.
         * This method is marked as Beta.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <E> the type of exception that may be thrown
         * @param c the Throwables.BiConsumer to adapt
         * @return a Throwables.Consumer that extracts key and value from an entry and applies the BiConsumer
         * @throws IllegalArgumentException if c is null
         */
        @Beta
        public static <K, V, E extends Exception> Throwables.Consumer<Map.Entry<K, V>, E> ec(final Throwables.BiConsumer<? super K, ? super V, E> c)
                throws IllegalArgumentException {
            N.checkArgNotNull(c, cs.BiConsumer);

            return e -> c.accept(e.getKey(), e.getValue());
        }

        /**
         * Adapts a Throwables.BiFunction to work with Map.Entry by extracting key and value, wrapping exceptions.
         * The returned function applies the BiFunction to the entry's key and value, converting checked exceptions to runtime exceptions.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <T> the type of the result of the function
         * @param f the Throwables.BiFunction to adapt
         * @return a Function that extracts key and value from an entry and applies the BiFunction
         * @throws IllegalArgumentException if f is null
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
         * Adapts a Throwables.BiPredicate to work with Map.Entry by extracting key and value, wrapping exceptions.
         * The returned predicate tests the entry by applying the BiPredicate to its key and value, converting checked exceptions to runtime exceptions.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param p the Throwables.BiPredicate to adapt
         * @return a Predicate that extracts key and value from an entry and applies the BiPredicate
         * @throws IllegalArgumentException if p is null
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
         * Adapts a Throwables.BiConsumer to work with Map.Entry by extracting key and value, wrapping exceptions.
         * The returned consumer accepts the entry by applying the BiConsumer to its key and value, converting checked exceptions to runtime exceptions.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param c the Throwables.BiConsumer to adapt
         * @return a Consumer that extracts key and value from an entry and applies the BiConsumer
         * @throws IllegalArgumentException if c is null
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
     * Utility class providing functions for working with Pair objects.
     * This class contains conversion methods to transform Pairs into collections.
     */
    public static final class Pairs {

        /** The Constant PAIR_TO_LIST. */
        @SuppressWarnings("rawtypes")
        private static final Function<Pair, List> PAIR_TO_LIST = t -> N.asList(t.left(), t.right());

        /** The Constant PAIR_TO_SET. */
        @SuppressWarnings("rawtypes")
        private static final Function<Pair, Set> PAIR_TO_SET = t -> N.asSet(t.left(), t.right());

        private Pairs() {
        }

        /**
         * Returns a Function that converts a Pair into a List containing its two elements.
         * The list will contain the left element followed by the right element.
         *
         * @param <T> the type of elements in the Pair
         * @return a Function that converts a Pair<T,T> to a List<T>
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Pair<T, T>, List<T>> toList() {
            return (Function) PAIR_TO_LIST;
        }

        /**
         * Returns a Function that converts a Pair into a Set containing its two elements.
         * If both elements are equal, the set will contain only one element.
         *
         * @param <T> the type of elements in the Pair
         * @return a Function that converts a Pair<T,T> to a Set<T>
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Pair<T, T>, Set<T>> toSet() {
            return (Function) PAIR_TO_SET;
        }

    }

    /**
     * Utility class providing functions for working with Triple objects.
     * This class contains conversion methods to transform Triples into collections.
     */
    public static final class Triples {

        /** The Constant TRIPLE_TO_LIST. */
        @SuppressWarnings("rawtypes")
        private static final Function<Triple, List> TRIPLE_TO_LIST = t -> N.asList(t.left(), t.middle(), t.right());

        /** The Constant TRIPLE_TO_SET. */
        @SuppressWarnings("rawtypes")
        private static final Function<Triple, Set> TRIPLE_TO_SET = t -> N.asSet(t.left(), t.middle(), t.right());

        private Triples() {
        }

        /**
         * Returns a Function that converts a Triple into a List containing its three elements.
         * The list will contain the left element, middle element, and right element in that order.
         *
         * @param <T> the type of elements in the Triple
         * @return a Function that converts a Triple<T,T,T> to a List<T>
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Triple<T, T, T>, List<T>> toList() {
            return (Function) TRIPLE_TO_LIST;
        }

        /**
         * Returns a Function that converts a Triple into a Set containing its three elements.
         * Duplicate elements will appear only once in the resulting set.
         *
         * @param <T> the type of elements in the Triple
         * @return a Function that converts a Triple<T,T,T> to a Set<T>
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Triple<T, T, T>, Set<T>> toSet() {
            return (Function) TRIPLE_TO_SET;
        }
    }

    /**
     * Utility class providing functions for working with DisposableArray objects.
     * This class contains methods for cloning, converting to string, and joining array elements.
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
         * Returns a Function that creates a copy of a DisposableArray's underlying array.
         * The returned function calls DisposableArray.copy() to create a defensive copy.
         *
         * @param <T> the component type of the array
         * @param <A> the type of DisposableArray
         * @return a Function that copies the DisposableArray's content to a new array
         */
        @SuppressWarnings("rawtypes")
        public static <T, A extends DisposableArray<T>> Function<A, T[]> cloneArray() {
            return (Function) CLONE;
        }

        /**
         * Returns a Function that converts a DisposableArray to its string representation.
         * The returned function calls DisposableArray.toString() for the conversion.
         *
         * @param <A> the type of DisposableArray
         * @return a Function that converts a DisposableArray to String
         */
        @SuppressWarnings("rawtypes")
        public static <A extends DisposableArray> Function<A, String> toStr() {
            return (Function) TO_STRING;
        }

        /**
         * Returns a Function that joins the elements of a DisposableArray with the specified delimiter.
         * The returned function calls DisposableArray.join(delimiter) for the concatenation.
         *
         * @param <A> the type of DisposableArray
         * @param delimiter the delimiter to be used between each element
         * @return a Function that joins the DisposableArray elements with the delimiter
         */
        @SuppressWarnings("rawtypes")
        public static <A extends DisposableArray> Function<A, String> join(final String delimiter) {
            return t -> t.join(delimiter);
        }
    }

    /**
     * Utility class for CharPredicate/Function/Consumer operations.
     * This class provides common char predicates, functions, and binary operators.
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

        /**
         * Returns a CharPredicate that tests if a character is zero.
         *
         * @return a CharPredicate that returns true if the character is '\0'
         */
        public static CharPredicate isZero() {
            return IS_ZERO;
        }

        /**
         * Returns a CharPredicate that tests if a character is whitespace.
         * Uses Character.isWhitespace() for the test.
         *
         * @return a CharPredicate that returns true if the character is whitespace
         */
        public static CharPredicate isWhitespace() {
            return IS_WHITESPACE;
        }

        /**
         * Returns a CharBiPredicate that tests if two characters are equal.
         *
         * @return a CharBiPredicate that returns true if the two characters are equal
         */
        public static CharBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Returns a CharBiPredicate that tests if two characters are not equal.
         *
         * @return a CharBiPredicate that returns true if the two characters are not equal
         */
        public static CharBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Returns a CharBiPredicate that tests if the first character is greater than the second.
         *
         * @return a CharBiPredicate that returns true if the first character is greater than the second
         */
        public static CharBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Returns a CharBiPredicate that tests if the first character is greater than or equal to the second.
         *
         * @return a CharBiPredicate that returns true if the first character is greater than or equal to the second
         */
        public static CharBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Returns a CharBiPredicate that tests if the first character is less than the second.
         *
         * @return a CharBiPredicate that returns true if the first character is less than the second
         */
        public static CharBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Returns a CharBiPredicate that tests if the first character is less than or equal to the second.
         *
         * @return a CharBiPredicate that returns true if the first character is less than or equal to the second
         */
        public static CharBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Returns a ToCharFunction that converts a Character object to a primitive char.
         * This function unboxes the Character wrapper to its primitive value.
         *
         * @return a ToCharFunction that unboxes Character to char
         */
        @SuppressWarnings("SameReturnValue")
        public static ToCharFunction<Character> unbox() {
            return ToCharFunction.UNBOX;
        }

        /**
         * Returns the provided CharPredicate as-is.
         * This is an identity method for CharPredicate that can be useful for type inference.
         *
         * @param p the CharPredicate to return
         * @return the same CharPredicate
         * @throws IllegalArgumentException if p is null
         */
        public static CharPredicate p(final CharPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Returns the provided CharFunction as-is.
         * This is an identity method for CharFunction that can be useful for type inference.
         *
         * @param <R> the type of the result of the function
         * @param f the CharFunction to return
         * @return the same CharFunction
         * @throws IllegalArgumentException if f is null
         */
        public static <R> CharFunction<R> f(final CharFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Returns the provided CharConsumer as-is.
         * This is an identity method for CharConsumer that can be useful for type inference.
         *
         * @param c the CharConsumer to return
         * @return the same CharConsumer
         * @throws IllegalArgumentException if c is null
         */
        public static CharConsumer c(final CharConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Returns a Function that calculates the length of a char array.
         * Returns 0 for null arrays.
         *
         * @return a Function that returns the length of a char array or 0 if null
         */
        public static Function<char[], Integer> len() {
            return LEN;
        }

        /**
         * Returns a stateful CharBiFunction that alternates between returning TAKE_FIRST and TAKE_SECOND.
         * The function maintains internal state and switches its return value on each call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @return a stateful CharBiFunction that alternates between merge results
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

        /**
         * Utility class providing CharBinaryOperator implementations for common operations.
         */
        public static final class CharBinaryOperators {
            private CharBinaryOperators() {
                // Singleton for utility class.
            }

            /**
             * A CharBinaryOperator that returns the smaller of two char values.
             */
            public static final CharBinaryOperator MIN = (left, right) -> left <= right ? left : right;

            /**
             * A CharBinaryOperator that returns the larger of two char values.
             */
            public static final CharBinaryOperator MAX = (left, right) -> left >= right ? left : right;
        }
    }

    /**
     * Utility class for BytePredicate/Function/Consumer operations.
     * This class provides common byte predicates, functions, and binary operators.
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

        /**
         * Returns a BytePredicate that tests if a byte value is positive (greater than zero).
         *
         * @return a BytePredicate that returns true if the byte is greater than 0
         */
        public static BytePredicate positive() {
            return POSITIVE;
        }

        /**
         * Returns a BytePredicate that tests if a byte value is not negative (greater than or equal to zero).
         *
         * @return a BytePredicate that returns true if the byte is greater than or equal to 0
         */
        public static BytePredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Returns a ByteBiPredicate that tests if two byte values are equal.
         *
         * @return a ByteBiPredicate that returns true if the two bytes are equal
         */
        public static ByteBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Returns a ByteBiPredicate that tests if two byte values are not equal.
         *
         * @return a ByteBiPredicate that returns true if the two bytes are not equal
         */
        public static ByteBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Returns a ByteBiPredicate that tests if the first byte is greater than the second.
         *
         * @return a ByteBiPredicate that returns true if the first byte is greater than the second
         */
        public static ByteBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Returns a ByteBiPredicate that tests if the first byte is greater than or equal to the second.
         *
         * @return a ByteBiPredicate that returns true if the first byte is greater than or equal to the second
         */
        public static ByteBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Returns a ByteBiPredicate that tests if the first byte is less than the second.
         *
         * @return a ByteBiPredicate that returns true if the first byte is less than the second
         */
        public static ByteBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Returns a ByteBiPredicate that tests if the first byte is less than or equal to the second.
         *
         * @return a ByteBiPredicate that returns true if the first byte is less than or equal to the second
         */
        public static ByteBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Returns a ToByteFunction that converts a Byte object to a primitive byte.
         * This function unboxes the Byte wrapper to its primitive value.
         *
         * @return a ToByteFunction that unboxes Byte to byte
         */
        @SuppressWarnings("SameReturnValue")
        public static ToByteFunction<Byte> unbox() {
            return ToByteFunction.UNBOX;
        }

        /**
         * Returns the provided BytePredicate as-is.
         * This is an identity method for BytePredicate that can be useful for type inference.
         *
         * @param p the BytePredicate to return
         * @return the same BytePredicate
         * @throws IllegalArgumentException if p is null
         */
        public static BytePredicate p(final BytePredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Returns the provided ByteFunction as-is.
         * This is an identity method for ByteFunction that can be useful for type inference.
         *
         * @param <R> the type of the result of the function
         * @param f the ByteFunction to return
         * @return the same ByteFunction
         * @throws IllegalArgumentException if f is null
         */
        public static <R> ByteFunction<R> f(final ByteFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Returns the provided ByteConsumer as-is.
         * This is an identity method for ByteConsumer that can be useful for type inference.
         *
         * @param c the ByteConsumer to return
         * @return the same ByteConsumer
         * @throws IllegalArgumentException if c is null
         */
        public static ByteConsumer c(final ByteConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Returns a Function that calculates the length of a byte array.
         * Returns 0 for null arrays.
         *
         * @return a Function that returns the length of a byte array or 0 if null
         */
        public static Function<byte[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<byte[], Integer> SUM = N::sum;

        /**
         * Returns a Function that calculates the sum of all elements in a byte array.
         * The sum is returned as an Integer to avoid overflow.
         *
         * @return a Function that returns the sum of byte array elements
         */
        public static Function<byte[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<byte[], Double> AVERAGE = N::average;

        /**
         * Returns a Function that calculates the average of all elements in a byte array.
         * Returns Double.NaN for empty or null arrays.
         *
         * @return a Function that returns the average of byte array elements
         */
        public static Function<byte[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful ByteBiFunction that alternates between returning TAKE_FIRST and TAKE_SECOND.
         * The function maintains internal state and switches its return value on each call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @return a stateful ByteBiFunction that alternates between merge results
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

        /**
         * Utility class providing ByteBinaryOperator implementations for common operations.
         */
        public static final class ByteBinaryOperators {
            private ByteBinaryOperators() {
                // Singleton for utility class.
            }

            /**
             * A ByteBinaryOperator that returns the smaller of two byte values.
             */
            public static final ByteBinaryOperator MIN = (left, right) -> left <= right ? left : right;

            /**
             * A ByteBinaryOperator that returns the larger of two byte values.
             */
            public static final ByteBinaryOperator MAX = (left, right) -> left >= right ? left : right;
        }
    }

    /**
     * Utility class for ShortPredicate/Function/Consumer operations.
     * This class provides common short predicates, functions, and binary operators.
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

        /**
         * Returns a ShortPredicate that tests if a short value is positive (greater than zero).
         *
         * @return a ShortPredicate that returns true if the short is greater than 0
         */
        public static ShortPredicate positive() {
            return POSITIVE;
        }

        /**
         * Returns a ShortPredicate that tests if a short value is not negative (greater than or equal to zero).
         *
         * @return a ShortPredicate that returns true if the short is greater than or equal to 0
         */
        public static ShortPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Returns a ShortBiPredicate that tests if two short values are equal.
         *
         * @return a ShortBiPredicate that returns true if the two shorts are equal
         */
        public static ShortBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Returns a ShortBiPredicate that tests if two short values are not equal.
         *
         * @return a ShortBiPredicate that returns true if the two shorts are not equal
         */
        public static ShortBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Returns a ShortBiPredicate that tests if the first short is greater than the second.
         *
         * @return a ShortBiPredicate that returns true if the first short is greater than the second
         */
        public static ShortBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Returns a ShortBiPredicate that tests if the first short is greater than or equal to the second.
         *
         * @return a ShortBiPredicate that returns true if the first short is greater than or equal to the second
         */
        public static ShortBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Returns a ShortBiPredicate that tests if the first short is less than the second.
         *
         * @return a ShortBiPredicate that returns true if the first short is less than the second
         */
        public static ShortBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Returns a ShortBiPredicate that tests if the first short is less than or equal to the second.
         *
         * @return a ShortBiPredicate that returns true if the first short is less than or equal to the second
         */
        public static ShortBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Returns a ToShortFunction that converts a Short object to a primitive short.
         * This function unboxes the Short wrapper to its primitive value.
         *
         * @return a ToShortFunction that unboxes Short to short
         */
        @SuppressWarnings("SameReturnValue")
        public static ToShortFunction<Short> unbox() {
            return ToShortFunction.UNBOX;
        }

        /**
         * Returns the provided ShortPredicate as-is.
         * This is an identity method for ShortPredicate that can be useful for type inference.
         *
         * @param p the ShortPredicate to return
         * @return the same ShortPredicate
         * @throws IllegalArgumentException if p is null
         */
        public static ShortPredicate p(final ShortPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Returns the provided ShortFunction as-is.
         * This is an identity method for ShortFunction that can be useful for type inference.
         *
         * @param <R> the type of the result of the function
         * @param f the ShortFunction to return
         * @return the same ShortFunction
         * @throws IllegalArgumentException if f is null
         */
        public static <R> ShortFunction<R> f(final ShortFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Returns the provided ShortConsumer as-is.
         * This is an identity method for ShortConsumer that can be useful for type inference.
         *
         * @param c the ShortConsumer to return
         * @return the same ShortConsumer
         * @throws IllegalArgumentException if c is null
         */
        public static ShortConsumer c(final ShortConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Returns a Function that calculates the length of a short array.
         * Returns 0 for null arrays.
         *
         * @return a Function that returns the length of a short array or 0 if null
         */
        public static Function<short[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<short[], Integer> SUM = N::sum;

        /**
         * Returns a Function that calculates the sum of all elements in a short array.
         * The sum is returned as an Integer to avoid overflow.
         *
         * @return a Function that returns the sum of short array elements
         */
        public static Function<short[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<short[], Double> AVERAGE = N::average;

        /**
         * Returns a Function that calculates the average of all elements in a short array.
         * Returns Double.NaN for empty or null arrays.
         *
         * @return a Function that returns the average of short array elements
         */
        public static Function<short[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful ShortBiFunction that alternates between returning TAKE_FIRST and TAKE_SECOND.
         * The function maintains internal state and switches its return value on each call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @return a stateful ShortBiFunction that alternates between merge results
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

        /**
         * Utility class providing ShortBinaryOperator implementations for common operations.
         */
        public static final class ShortBinaryOperators {
            private ShortBinaryOperators() {
                // Singleton for utility class.
            }

            /**
             * A ShortBinaryOperator that returns the smaller of two short values.
             */
            public static final ShortBinaryOperator MIN = (left, right) -> left <= right ? left : right;

            /**
             * A ShortBinaryOperator that returns the larger of two short values.
             */
            public static final ShortBinaryOperator MAX = (left, right) -> left >= right ? left : right;
        }
    }

    /**
     * Utility class for IntPredicate/Function/Consumer operations.
     * This class provides common int predicates, functions, and binary operators.
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

        /**
         * Returns an IntPredicate that tests if an int value is positive (greater than zero).
         *
         * @return an IntPredicate that returns true if the int is greater than 0
         */
        public static IntPredicate positive() {
            return POSITIVE;
        }

        /**
         * Returns an IntPredicate that tests if an int value is not negative (greater than or equal to zero).
         *
         * @return an IntPredicate that returns true if the int is greater than or equal to 0
         */
        public static IntPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Returns an IntBiPredicate that tests if two int values are equal.
         *
         * @return an IntBiPredicate that returns true if the two ints are equal
         */
        public static IntBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Returns an IntBiPredicate that tests if two int values are not equal.
         *
         * @return an IntBiPredicate that returns true if the two ints are not equal
         */
        public static IntBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Returns an IntBiPredicate that tests if the first int is greater than the second.
         *
         * @return an IntBiPredicate that returns true if the first int is greater than the second
         */
        public static IntBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Returns an IntBiPredicate that tests if the first int is greater than or equal to the second.
         *
         * @return an IntBiPredicate that returns true if the first int is greater than or equal to the second
         */
        public static IntBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Returns an IntBiPredicate that tests if the first int is less than the second.
         *
         * @return an IntBiPredicate that returns true if the first int is less than the second
         */
        public static IntBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Returns an IntBiPredicate that tests if the first int is less than or equal to the second.
         *
         * @return an IntBiPredicate that returns true if the first int is less than or equal to the second
         */
        public static IntBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Returns a ToIntFunction that converts an Integer object to a primitive int.
         * This function unboxes the Integer wrapper to its primitive value.
         *
         * @return a ToIntFunction that unboxes Integer to int
         */
        @SuppressWarnings("SameReturnValue")
        public static ToIntFunction<Integer> unbox() {
            return ToIntFunction.UNBOX;
        }

        /**
         * Returns the provided IntPredicate as-is.
         * This is an identity method for IntPredicate that can be useful for type inference.
         *
         * @param p the IntPredicate to return
         * @return the same IntPredicate
         * @throws IllegalArgumentException if p is null
         */
        public static IntPredicate p(final IntPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Returns the provided IntFunction as-is.
         * This is an identity method for IntFunction that can be useful for type inference.
         *
         * @param <R> the type of the result of the function
         * @param f the IntFunction to return
         * @return the same IntFunction
         * @throws IllegalArgumentException if f is null
         */
        public static <R> IntFunction<R> f(final IntFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Returns the provided IntConsumer as-is.
         * This is an identity method for IntConsumer that can be useful for type inference.
         *
         * @param c the IntConsumer to return
         * @return the same IntConsumer
         * @throws IllegalArgumentException if c is null
         */
        public static IntConsumer c(final IntConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Returns a Function that calculates the length of an int array.
         * Returns 0 for null arrays.
         *
         * @return a Function that returns the length of an int array or 0 if null
         */
        public static Function<int[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<int[], Integer> SUM = N::sum;

        /**
         * Returns a Function that calculates the sum of all elements in an int array.
         *
         * @return a Function that returns the sum of int array elements
         */
        public static Function<int[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<int[], Double> AVERAGE = N::average;

        /**
         * Returns a Function that calculates the average of all elements in an int array.
         * Returns Double.NaN for empty or null arrays.
         *
         * @return a Function that returns the average of int array elements
         */
        public static Function<int[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful IntBiFunction that alternates between returning TAKE_FIRST and TAKE_SECOND.
         * The function maintains internal state and switches its return value on each call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @return a stateful IntBiFunction that alternates between merge results
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

        /**
         * Utility class providing IntBinaryOperator implementations for common operations.
         */
        public static final class IntBinaryOperators {
            private IntBinaryOperators() {
                // Singleton for utility class.
            }

            /**
             * An IntBinaryOperator that returns the smaller of two int values using Math.min.
             */
            public static final IntBinaryOperator MIN = Math::min;

            /**
             * An IntBinaryOperator that returns the larger of two int values using Math.max.
             */
            public static final IntBinaryOperator MAX = Math::max;
        }
    }

    /**
     * Utility class for LongPredicate/Function/Consumer operations.
     * This class provides common long predicates, functions, and binary operators.
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

        /**
         * Returns a LongPredicate that tests if a long value is positive (greater than zero).
         *
         * @return a LongPredicate that returns true if the long is greater than 0
         */
        public static LongPredicate positive() {
            return POSITIVE;
        }

        /**
         * Returns a LongPredicate that tests if a long value is not negative (greater than or equal to zero).
         *
         * @return a LongPredicate that returns true if the long is greater than or equal to 0
         */
        public static LongPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Returns a LongBiPredicate that tests if two long values are equal.
         *
         * @return a LongBiPredicate that returns true if the two longs are equal
         */
        public static LongBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Returns a LongBiPredicate that tests if two long values are not equal.
         *
         * @return a LongBiPredicate that returns true if the two longs are not equal
         */
        public static LongBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Returns a LongBiPredicate that tests if the first long is greater than the second.
         *
         * @return a LongBiPredicate that returns true if the first long is greater than the second
         */
        public static LongBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Returns a LongBiPredicate that tests if the first long is greater than or equal to the second.
         *
         * @return a LongBiPredicate that returns true if the first long is greater than or equal to the second
         */
        public static LongBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Returns a LongBiPredicate that tests if the first long is less than the second.
         *
         * @return a LongBiPredicate that returns true if the first long is less than the second
         */
        public static LongBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Returns a LongBiPredicate that tests if the first long is less than or equal to the second.
         *
         * @return a LongBiPredicate that returns true if the first long is less than or equal to the second
         */
        public static LongBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Returns a ToLongFunction that converts a Long object to a primitive long.
         * This function unboxes the Long wrapper to its primitive value.
         *
         * @return a ToLongFunction that unboxes Long to long
         */
        @SuppressWarnings("SameReturnValue")
        public static ToLongFunction<Long> unbox() {
            return ToLongFunction.UNBOX;
        }

        /**
         * Returns the provided LongPredicate as-is.
         * This is an identity method for LongPredicate that can be useful for type inference.
         *
         * @param p the LongPredicate to return
         * @return the same LongPredicate
         * @throws IllegalArgumentException if p is null
         */
        public static LongPredicate p(final LongPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Returns the provided LongFunction as-is.
         * This is an identity method for LongFunction that can be useful for type inference.
         *
         * @param <R> the type of the result of the function
         * @param f the LongFunction to return
         * @return the same LongFunction
         * @throws IllegalArgumentException if f is null
         */
        public static <R> LongFunction<R> f(final LongFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Returns the provided LongConsumer as-is.
         * This is an identity method for LongConsumer that can be useful for type inference.
         *
         * @param c the LongConsumer to return
         * @return the same LongConsumer
         * @throws IllegalArgumentException if c is null
         */
        public static LongConsumer c(final LongConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Returns a Function that calculates the length of a long array.
         * Returns 0 for null arrays.
         *
         * @return a Function that returns the length of a long array or 0 if null
         */
        public static Function<long[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<long[], Long> SUM = N::sum;

        /**
         * Returns a Function that calculates the sum of all elements in a long array.
         *
         * @return a Function that returns the sum of long array elements
         */
        public static Function<long[], Long> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<long[], Double> AVERAGE = N::average;

        /**
         * Returns a Function that calculates the average of all elements in a long array.
         * Returns Double.NaN for empty or null arrays.
         *
         * @return a Function that returns the average of long array elements
         */
        public static Function<long[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful LongBiFunction that alternates between returning TAKE_FIRST and TAKE_SECOND.
         * The function maintains internal state and switches its return value on each call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @return a stateful LongBiFunction that alternates between merge results
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

        /**
         * Utility class providing LongBinaryOperator implementations for common operations.
         */
        public static final class LongBinaryOperators {
            private LongBinaryOperators() {
                // Singleton for utility class.
            }

            /**
             * A LongBinaryOperator that returns the smaller of two long values using Math.min.
             */
            public static final LongBinaryOperator MIN = Math::min;

            /**
             * A LongBinaryOperator that returns the larger of two long values using Math.max.
             */
            public static final LongBinaryOperator MAX = Math::max;
        }
    }

    /**
     * Utility class for FloatPredicate/Function/Consumer operations.
     * This class provides common float predicates, functions, and binary operators.
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

        /**
         * Returns a FloatPredicate that tests if a float value is positive (greater than zero).
         *
         * @return a FloatPredicate that returns true if the float is greater than 0
         */
        public static FloatPredicate positive() {
            return POSITIVE;
        }

        /**
         * Returns a FloatPredicate that tests if a float value is not negative (greater than or equal to zero).
         *
         * @return a FloatPredicate that returns true if the float is greater than or equal to 0
         */
        public static FloatPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Returns a FloatBiPredicate that tests if two float values are equal.
         * Uses N.equals for proper float comparison including NaN handling.
         *
         * @return a FloatBiPredicate that returns true if the two floats are equal
         */
        public static FloatBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Returns a FloatBiPredicate that tests if two float values are not equal.
         * Uses N.compare for proper float comparison including NaN handling.
         *
         * @return a FloatBiPredicate that returns true if the two floats are not equal
         */
        public static FloatBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Returns a FloatBiPredicate that tests if the first float is greater than the second.
         * Uses N.compare for proper float comparison including NaN handling.
         *
         * @return a FloatBiPredicate that returns true if the first float is greater than the second
         */
        public static FloatBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Returns a FloatBiPredicate that tests if the first float is greater than or equal to the second.
         * Uses N.compare for proper float comparison including NaN handling.
         *
         * @return a FloatBiPredicate that returns true if the first float is greater than or equal to the second
         */
        public static FloatBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Returns a FloatBiPredicate that tests if the first float is less than the second.
         * Uses N.compare for proper float comparison including NaN handling.
         *
         * @return a FloatBiPredicate that returns true if the first float is less than the second
         */
        public static FloatBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Returns a FloatBiPredicate that tests if the first float is less than or equal to the second.
         * Uses N.compare for proper float comparison including NaN handling.
         *
         * @return a FloatBiPredicate that returns true if the first float is less than or equal to the second
         */
        public static FloatBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Returns a ToFloatFunction that converts a Float object to a primitive float.
         * This function unboxes the Float wrapper to its primitive value.
         *
         * @return a ToFloatFunction that unboxes Float to float
         */
        @SuppressWarnings("SameReturnValue")
        public static ToFloatFunction<Float> unbox() {
            return ToFloatFunction.UNBOX;
        }

        /**
         * Returns the provided FloatPredicate as-is.
         * This is an identity method for FloatPredicate that can be useful for type inference.
         *
         * @param p the FloatPredicate to return
         * @return the same FloatPredicate
         * @throws IllegalArgumentException if p is null
         */
        public static FloatPredicate p(final FloatPredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Returns the provided FloatFunction as-is.
         * This is an identity method for FloatFunction that can be useful for type inference.
         *
         * @param <R> the type of the result of the function
         * @param f the FloatFunction to return
         * @return the same FloatFunction
         * @throws IllegalArgumentException if f is null
         */
        public static <R> FloatFunction<R> f(final FloatFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Returns the provided FloatConsumer as-is.
         * This is an identity method for FloatConsumer that can be useful for type inference.
         *
         * @param c the FloatConsumer to return
         * @return the same FloatConsumer
         * @throws IllegalArgumentException if c is null
         */
        public static FloatConsumer c(final FloatConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Returns a Function that calculates the length of a float array.
         * Returns 0 for null arrays.
         *
         * @return a Function that returns the length of a float array or 0 if null
         */
        public static Function<float[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<float[], Float> SUM = N::sum;

        /**
         * Returns a Function that calculates the sum of all elements in a float array.
         *
         * @return a Function that returns the sum of float array elements
         */
        public static Function<float[], Float> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<float[], Double> AVERAGE = N::average;

        /**
         * Returns a Function that calculates the average of all elements in a float array.
         * Returns Double.NaN for empty or null arrays.
         *
         * @return a Function that returns the average of float array elements as a Double
         */
        public static Function<float[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful FloatBiFunction that alternates between returning TAKE_FIRST and TAKE_SECOND.
         * The function maintains internal state and switches its return value on each call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @return a stateful FloatBiFunction that alternates between merge results
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

        /**
         * Utility class providing FloatBinaryOperator implementations for common operations.
         */
        public static final class FloatBinaryOperators {
            private FloatBinaryOperators() {
                // Singleton for utility class.
            }

            /**
             * A FloatBinaryOperator that returns the smaller of two float values using Math.min.
             */
            public static final FloatBinaryOperator MIN = Math::min;

            /**
             * A FloatBinaryOperator that returns the larger of two float values using Math.max.
             */
            public static final FloatBinaryOperator MAX = Math::max;
        }
    }

    /**
     * Utility class for DoublePredicate/Function/Consumer operations.
     * This class provides common double predicates, functions, and binary operators.
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

        /**
         * Returns a DoublePredicate that tests if a double value is positive (greater than zero).
         *
         * @return a DoublePredicate that returns true if the double is greater than 0
         */
        public static DoublePredicate positive() {
            return POSITIVE;
        }

        /**
         * Returns a DoublePredicate that tests if a double value is not negative (greater than or equal to zero).
         *
         * @return a DoublePredicate that returns true if the double is greater than or equal to 0
         */
        public static DoublePredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Returns a DoubleBiPredicate that tests if two double values are equal.
         * Uses N.equals for proper double comparison including NaN handling.
         *
         * @return a DoubleBiPredicate that returns true if the two doubles are equal
         */
        public static DoubleBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Returns a DoubleBiPredicate that tests if two double values are not equal.
         * Uses N.compare for proper double comparison including NaN handling.
         *
         * @return a DoubleBiPredicate that returns true if the two doubles are not equal
         */
        public static DoubleBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Returns a DoubleBiPredicate that tests if the first double is greater than the second.
         * Uses N.compare for proper double comparison including NaN handling.
         *
         * @return a DoubleBiPredicate that returns true if the first double is greater than the second
         */
        public static DoubleBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Returns a DoubleBiPredicate that tests if the first double is greater than or equal to the second.
         * Uses N.compare for proper double comparison including NaN handling.
         *
         * @return a DoubleBiPredicate that returns true if the first double is greater than or equal to the second
         */
        public static DoubleBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Returns a DoubleBiPredicate that tests if the first double is less than the second.
         * Uses N.compare for proper double comparison including NaN handling.
         *
         * @return a DoubleBiPredicate that returns true if the first double is less than the second
         */
        public static DoubleBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Returns a DoubleBiPredicate that tests if the first double is less than or equal to the second.
         * Uses N.compare for proper double comparison including NaN handling.
         *
         * @return a DoubleBiPredicate that returns true if the first double is less than or equal to the second
         */
        public static DoubleBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Returns a ToDoubleFunction that converts a Double object to a primitive double.
         * This function unboxes the Double wrapper to its primitive value.
         *
         * @return a ToDoubleFunction that unboxes Double to double
         */
        @SuppressWarnings("SameReturnValue")
        public static ToDoubleFunction<Double> unbox() {
            return ToDoubleFunction.UNBOX;
        }

        /**
         * Returns the provided DoublePredicate as-is.
         * This is an identity method for DoublePredicate that can be useful for type inference.
         *
         * @param p the DoublePredicate to return
         * @return the same DoublePredicate
         * @throws IllegalArgumentException if p is null
         */
        public static DoublePredicate p(final DoublePredicate p) throws IllegalArgumentException {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Returns the provided DoubleFunction as-is.
         * This is an identity method for DoubleFunction that can be useful for type inference.
         *
         * @param <R> the type of the result of the function
         * @param f the DoubleFunction to return
         * @return the same DoubleFunction
         * @throws IllegalArgumentException if f is null
         */
        public static <R> DoubleFunction<R> f(final DoubleFunction<R> f) throws IllegalArgumentException {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Returns the provided DoubleConsumer as-is.
         * This is an identity method for DoubleConsumer that can be useful for type inference.
         *
         * @param c the DoubleConsumer to return
         * @return the same DoubleConsumer
         * @throws IllegalArgumentException if c is null
         */
        public static DoubleConsumer c(final DoubleConsumer c) throws IllegalArgumentException {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Returns a Function that calculates the length of a double array.
         * Returns 0 for null arrays.
         *
         * @return a Function that returns the length of a double array or 0 if null
         */
        public static Function<double[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<double[], Double> SUM = N::sum;

        /**
         * Returns a Function that calculates the sum of all elements in a double array.
         *
         * @return a Function that returns the sum of double array elements
         */
        public static Function<double[], Double> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<double[], Double> AVERAGE = N::average;

        /**
         * Returns a Function that calculates the average of all elements in a double array.
         * Returns Double.NaN for empty or null arrays.
         *
         * @return a Function that returns the average of double array elements
         */
        public static Function<double[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful DoubleBiFunction that alternates between returning TAKE_FIRST and TAKE_SECOND.
         * The function maintains internal state and switches its return value on each call.
         * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
         *
         * @return a stateful DoubleBiFunction that alternates between merge results
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

        /**
         * Utility class providing DoubleBinaryOperator implementations for common operations.
         */
        public static final class DoubleBinaryOperators {
            private DoubleBinaryOperators() {
                // Singleton for utility class.
            }

            /**
             * A DoubleBinaryOperator that returns the smaller of two double values using Math.min.
             */
            public static final DoubleBinaryOperator MIN = Math::min;

            /**
             * A DoubleBinaryOperator that returns the larger of two double values using Math.max.
             */
            public static final DoubleBinaryOperator MAX = Math::max;
        }
    }

    /**
     * Utility class for exceptional Predicate/Function/Consumer operations.
     * This class provides methods for creating and manipulating Throwables functional interfaces.
     */
    public static final class Fnn {
        private Fnn() {
            // Singleton for utility class
        }

        /**
         * Returns a Supplier which returns a single instance created by calling the specified supplier.get().
         * The instance is created lazily on the first call and cached for subsequent calls.
         *
         * @param <T> the type of results supplied by this supplier
         * @param <E> the type of exception that may be thrown
         * @param supplier the supplier to memoize
         * @return a memoized version of the supplier that caches the result
         */
        public static <T, E extends Throwable> Throwables.Supplier<T, E> memoize(final Throwables.Supplier<T, E> supplier) {
            return Throwables.LazyInitializer.of(supplier);
        }

        /**
         * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
         *
         * Returns a supplier that caches the instance supplied by the delegate and removes the cached
         * value after the specified time has passed. Subsequent calls to get() return the cached
         * value if the expiration time has not passed. After the expiration time, a new value is
         * retrieved, cached, and returned. See: <a
         * href="http://en.wikipedia.org/wiki/Memoization">memoization</a>
         *
         * <p>The returned supplier is thread-safe. The supplier's serialized form does not contain the
         * cached value, which will be recalculated when get() is called on the reserialized
         * instance. The actual memoization does not happen when the underlying delegate throws an
         * exception.
         *
         * <p>When the underlying delegate throws an exception, then this memorizing supplier will keep
         * delegating calls until it returns valid data.
         *
         *
         * @param <T> the type of results supplied by this supplier
         * @param <E> the type of exception that may be thrown
         * @param supplier the delegate supplier
         * @param duration the length of time after a value is created that it should stop being returned
         *     by subsequent get() calls
         * @param unit the unit that duration is expressed in
         * @return a supplier that caches with expiration
         * @throws IllegalArgumentException if duration is not positive
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
         * Returns a memoized version of the input function that caches the result of each distinct input.
         * The function uses a ConcurrentHashMap internally to cache results, making it thread-safe.
         *
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of exception that may be thrown
         * @param func the function to memoize
         * @return a memoized version of the function that caches results by input
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
         * Returns a Throwables.Function that always returns its input argument unchanged.
         * This is the identity function for Throwables.Function.
         *
         * @param <T> the type of the input and output of the function
         * @param <E> the type of exception that may be thrown
         * @return a function that always returns its input argument
         */
        public static <T, E extends Exception> Throwables.Function<T, T, E> identity() {
            return Fn.IDENTITY;
        }

        /**
         * Returns a Throwables.Predicate that always returns true regardless of input.
         *
         * @param <T> the type of the input to the predicate
         * @param <E> the type of exception that may be thrown
         * @return a predicate that always returns true
         */
        public static <T, E extends Exception> Throwables.Predicate<T, E> alwaysTrue() {
            return Fn.ALWAYS_TRUE;
        }

        /**
         * Returns a Throwables.Predicate that always returns false regardless of input.
         *
         * @param <T> the type of the input to the predicate
         * @param <E> the type of exception that may be thrown
         * @return a predicate that always returns false
         */
        public static <T, E extends Exception> Throwables.Predicate<T, E> alwaysFalse() {
            return Fn.ALWAYS_FALSE;
        }

        /**
         * Returns a Throwables.Function that converts its input to a String using String.valueOf().
         *
         * @param <T> the type of the input to the function
         * @param <E> the type of exception that may be thrown
         * @return a function that converts its input to String
         */
        public static <T, E extends Exception> Throwables.Function<T, String, E> toStr() {
            return TO_STRING;
        }

        /**
         * Returns a Throwables.Function that extracts the key from a Map.Entry.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <E> the type of exception that may be thrown
         * @return a function that returns the key of a Map.Entry
         */
        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Map.Entry<K, V>, K, E> key() {
            return (Throwables.Function) Fn.KEY;
        }

        /**
         * Returns a Throwables.Function that extracts the value from a Map.Entry.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <E> the type of exception that may be thrown
         * @return a function that returns the value of a Map.Entry
         */
        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Map.Entry<K, V>, V, E> value() {
            return (Throwables.Function) Fn.VALUE;
        }

        /**
         * Returns a Throwables.Function that inverts a Map.Entry by swapping its key and value.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <E> the type of exception that may be thrown
         * @return a function that returns an inverted Entry with key and value swapped
         */
        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Entry<K, V>, Entry<V, K>, E> inverse() {
            return (Throwables.Function) Fn.INVERSE;
        }

        /**
         * Returns a Throwables.BiFunction that creates a Map.Entry from a key and value.
         *
         * @param <K> the type of keys in the entry
         * @param <V> the type of values in the entry
         * @param <E> the type of exception that may be thrown
         * @return a BiFunction that creates a Map.Entry from key and value
         */
        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.BiFunction<K, V, Map.Entry<K, V>, E> entry() {
            return (Throwables.BiFunction) Fn.ENTRY;
        }

        /**
         * Returns a Throwables.BiFunction that creates a Pair from two values.
         *
         * @param <L> the type of the left element
         * @param <R> the type of the right element
         * @param <E> the type of exception that may be thrown
         * @return a BiFunction that creates a Pair from left and right values
         */
        @SuppressWarnings("rawtypes")
        public static <L, R, E extends Exception> Throwables.BiFunction<L, R, Pair<L, R>, E> pair() {
            return (Throwables.BiFunction) Fn.PAIR;
        }

        /**
         * Returns a Throwables.TriFunction that creates a Triple from three values.
         *
         * @param <L> the type of the left element
         * @param <M> the type of the middle element
         * @param <R> the type of the right element
         * @param <E> the type of exception that may be thrown
         * @return a TriFunction that creates a Triple from left, middle, and right values
         */
        @SuppressWarnings("rawtypes")
        public static <L, M, R, E extends Exception> Throwables.TriFunction<L, M, R, Triple<L, M, R>, E> triple() {
            return (Throwables.TriFunction) Fn.TRIPLE;
        }

        /**
         * Returns a Throwables.Function that wraps a single value in a Tuple1.
         *
         * @param <T> the type of the element
         * @param <E> the type of exception that may be thrown
         * @return a Function that creates a Tuple1 from a single value
         */
        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Function<T, Tuple1<T>, E> tuple1() {
            return (Throwables.Function) Fn.TUPLE_1;
        }

        /**
         * Returns a Throwables.BiFunction that wraps two values in a Tuple2.
         *
         * @param <T> the type of the first element
         * @param <U> the type of the second element
         * @param <E> the type of exception that may be thrown
         * @return a BiFunction that creates a Tuple2 from two values
         */
        @SuppressWarnings("rawtypes")
        public static <T, U, E extends Exception> Throwables.BiFunction<T, U, Tuple2<T, U>, E> tuple2() {
            return (Throwables.BiFunction) Fn.TUPLE_2;
        }

        /**
         * Returns a Throwables.TriFunction that wraps three values in a Tuple3.
         *
         * @param <A> the type of the first element
         * @param <B> the type of the second element
         * @param <C> the type of the third element
         * @param <E> the type of exception that may be thrown
         * @return a TriFunction that creates a Tuple3 from three values
         */
        @SuppressWarnings("rawtypes")
        public static <A, B, C, E extends Exception> Throwables.TriFunction<A, B, C, Tuple3<A, B, C>, E> tuple3() {
            return (Throwables.TriFunction) Fn.TUPLE_3;
        }

        /**
         * Returns a Throwables.Runnable that performs no operation when executed.
         *
         * @param <E> the type of exception that may be thrown
         * @return a Runnable that does nothing
         */
        public static <E extends Exception> Throwables.Runnable<E> emptyAction() {
            return (Throwables.Runnable<E>) Fn.EMPTY_ACTION;
        }

        /**
         * Returns a Throwables.Consumer that performs no operation on its input.
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that may be thrown
         * @return a Consumer that does nothing
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> doNothing() {
            return Fn.EMPTY_CONSUMER;
        }

        /**
         * Returns a Throwables.Consumer that throws a RuntimeException with the specified error message.
         *
         * @param <T> the type of the input to the consumer
         * @param errorMessage the error message for the exception
         * @return a Consumer that throws a RuntimeException
         */
        public static <T> Throwables.Consumer<T, RuntimeException> throwRuntimeException(final String errorMessage) {
            return t -> {
                throw new RuntimeException(errorMessage);
            };
        }

        /**
         * Returns a Throwables.Consumer that throws an IOException with the specified error message.
         *
         * @param <T> the type of the input to the consumer
         * @param errorMessage the error message for the exception
         * @return a Consumer that throws an IOException
         */
        public static <T> Throwables.Consumer<T, IOException> throwIOException(final String errorMessage) {
            return t -> {
                throw new IOException(errorMessage);
            };
        }

        /**
         * Returns a Throwables.Consumer that throws an Exception with the specified error message.
         *
         * @param <T> the type of the input to the consumer
         * @param errorMessage the error message for the exception
         * @return a Consumer that throws an Exception
         */
        public static <T> Throwables.Consumer<T, Exception> throwException(final String errorMessage) {
            return t -> {
                throw new Exception(errorMessage);
            };
        }

        /**
         * Returns a Throwables.Consumer that throws an exception provided by the supplier.
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that may be thrown
         * @param exceptionSupplier the supplier that provides the exception to throw
         * @return a Consumer that throws the supplied exception
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> throwException(final java.util.function.Supplier<? extends E> exceptionSupplier) {
            return t -> {
                throw exceptionSupplier.get();
            };
        }

        /**
         * Returns a Throwables.Consumer that sleeps for the specified number of milliseconds.
         * The consumer ignores its input and calls N.sleep(millis).
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that may be thrown
         * @param millis the number of milliseconds to sleep
         * @return a Consumer that sleeps for the specified duration
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> sleep(final long millis) {
            return t -> N.sleep(millis);
        }

        /**
         * Returns a Throwables.Consumer that sleeps uninterruptibly for the specified number of milliseconds.
         * The consumer ignores its input and calls N.sleepUninterruptibly(millis).
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that may be thrown
         * @param millis the number of milliseconds to sleep
         * @return a Consumer that sleeps uninterruptibly for the specified duration
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> sleepUninterruptibly(final long millis) {
            return t -> N.sleepUninterruptibly(millis);
        }

        /**
         * Returns a stateful Throwables.Consumer that rate limits execution to the specified permits per second.
         * The consumer uses a RateLimiter internally to control the rate of execution.
         * This consumer is stateful and should not be saved or cached for reuse, but it can be used in parallel streams.
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that may be thrown
         * @param permitsPerSecond the number of permits per second
         * @return a stateful Consumer that rate limits execution
         * @see RateLimiter#acquire()
         * @see RateLimiter#create(double)
         */
        @Stateful
        public static <T, E extends Exception> Throwables.Consumer<T, E> rateLimiter(final double permitsPerSecond) {
            return rateLimiter(RateLimiter.create(permitsPerSecond));
        }

        /**
         * Returns a stateful Throwables.Consumer that rate limits execution using the provided RateLimiter.
         * The consumer calls rateLimiter.acquire() before allowing execution to proceed.
         * This consumer is stateful and should not be saved or cached for reuse, but it can be used in parallel streams.
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that may be thrown
         * @param rateLimiter the RateLimiter to use
         * @return a stateful Consumer that rate limits execution
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
         * Returns a Throwables.Consumer that closes an AutoCloseable resource.
         * The consumer calls close() on the resource if it is not null.
         *
         * @param <T> the type of AutoCloseable
         * @return a Consumer that closes the AutoCloseable resource
         */
        public static <T extends AutoCloseable> Throwables.Consumer<T, Exception> close() {
            return (Throwables.Consumer<T, Exception>) CLOSE;
        }

        /**
         * Returns a Throwables.Consumer that closes an AutoCloseable resource quietly.
         * The consumer calls closeQuietly() on the resource, suppressing any exceptions.
         *
         * @param <T> the type of AutoCloseable
         * @param <E> the type of exception that may be thrown
         * @return a Consumer that closes the AutoCloseable resource quietly
         */
        public static <T extends AutoCloseable, E extends Exception> Throwables.Consumer<T, E> closeQuietly() {
            return (Throwables.Consumer<T, E>) Fn.CLOSE_QUIETLY;
        }

        /**
         * Returns a Throwables.Consumer that prints its input to standard output.
         * The consumer calls System.out.println() with the input value.
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that may be thrown
         * @return a Consumer that prints to standard output
         */
        public static <T, E extends Exception> Throwables.Consumer<T, E> println() {
            return Fn.PRINTLN;
        }

        /**
         * Returns a Throwables.BiConsumer that prints its two inputs to standard output with a separator.
         * The consumer formats the output as "first separator second".
         *
         * @param <T> the type of the first input to the consumer
         * @param <U> the type of the second input to the consumer
         * @param <E> the type of exception that may be thrown
         * @param separator the separator string to use between the two values
         * @return a BiConsumer that prints two values with a separator
         */
        public static <T, U, E extends Exception> Throwables.BiConsumer<T, U, E> println(final String separator) {
            return cc(Fn.println(separator));
        }

        /**
         * Returns a Throwables.Predicate that tests if its input is null.
         * This method is marked as Beta.
         *
         * @param <T> the type of the input to the predicate
         * @param <E> the type of exception that may be thrown
         * @return a Predicate that returns true if the input is null
         */
        @Beta
        public static <T, E extends Exception> Throwables.Predicate<T, E> isNull() {
            return Fn.IS_NULL;
        }

        /**
         * Returns a Throwables.Predicate that tests if a CharSequence is empty.
         * The predicate returns true if the CharSequence has length 0.
         *
         * @param <T> the type of CharSequence
         * @param <E> the type of exception that may be thrown
         * @return a Predicate that returns true if the CharSequence is empty
         */
        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> isEmpty() {
            return (Throwables.Predicate<T, E>) Fn.IS_EMPTY;
        }

        /**
         * Returns a Throwables.Predicate that tests if a CharSequence is blank.
         * The predicate returns true if the CharSequence is empty or contains only whitespace characters.
         *
         * @param <T> the type of CharSequence
         * @param <E> the type of exception that may be thrown
         * @return a Predicate that returns true if the CharSequence is blank
         */
        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> isBlank() {
            return (Throwables.Predicate<T, E>) Fn.IS_BLANK;
        }

        /**
         * Returns a Throwables.Predicate that tests if an array is empty.
         * The predicate returns true if the array is null or has length 0.
         * This method is marked as Beta.
         *
         * @param <T> the component type of the array
         * @param <E> the type of exception that may be thrown
         * @return a Predicate that returns true if the array is empty
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Predicate<T[], E> isEmptyA() {
            return (Throwables.Predicate) Fn.IS_EMPTY_A;
        }

        /**
         * Returns a Throwables.Predicate that tests if a Collection is empty.
         * The predicate returns true if the Collection is null or has size 0.
         * This method is marked as Beta.
         *
         * @param <T> the type of Collection
         * @param <E> the type of exception that may be thrown
         * @return a Predicate that returns true if the Collection is empty
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Collection, E extends Exception> Throwables.Predicate<T, E> isEmptyC() {
            return (Throwables.Predicate<T, E>) Fn.IS_EMPTY_C;
        }

        /**
         * Returns a Throwables.Predicate that tests if a Map is empty.
         * The predicate returns true if the Map is null or has size 0.
         * This method is marked as Beta.
         *
         * @param <T> the type of Map
         * @param <E> the type of exception that may be thrown
         * @return a Predicate that returns true if the Map is empty
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Map, E extends Exception> Throwables.Predicate<T, E> isEmptyM() {
            return (Throwables.Predicate<T, E>) Fn.IS_EMPTY_M;
        }

        /**
         * Returns a Predicate that tests whether its input is not null.
         * 
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the exception that may be thrown
         * @return a Predicate that returns true if the input is not null, false otherwise
         * @see java.util.Objects#nonNull(Object)
         */
        @Beta
        public static <T, E extends Exception> Throwables.Predicate<T, E> notNull() {
            return Fn.NOT_NULL;
        }

        /**
         * Returns a Predicate that tests whether a CharSequence is not empty.
         * A CharSequence is considered not empty if it is not null and has a length greater than 0.
         * 
         * @param <T> the type of the CharSequence to test
         * @param <E> the type of the exception that may be thrown
         * @return a Predicate that returns true if the input CharSequence is not empty, false otherwise
         * @see CharSequence#length()
         */
        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> notEmpty() {
            return (Throwables.Predicate<T, E>) Fn.IS_NOT_EMPTY;
        }

        /**
         * Returns a Predicate that tests whether a CharSequence is not blank.
         * A CharSequence is considered not blank if it is not null, not empty, 
         * and contains at least one non-whitespace character.
         * 
         * @param <T> the type of the CharSequence to test
         * @param <E> the type of the exception that may be thrown
         * @return a Predicate that returns true if the input CharSequence is not blank, false otherwise
         * @see Character#isWhitespace(char)
         */
        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> notBlank() {
            return (Throwables.Predicate<T, E>) Fn.IS_NOT_BLANK;
        }

        /**
         * Returns a Predicate that tests whether an array is not empty.
         * An array is considered not empty if it is not null and has a length greater than 0.
         * 
         * @param <T> the component type of the array
         * @param <E> the type of the exception that may be thrown
         * @return a Predicate that returns true if the input array is not empty, false otherwise
         * @see java.lang.reflect.Array#getLength(Object)
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Predicate<T[], E> notEmptyA() {
            return (Throwables.Predicate) Fn.NOT_EMPTY_A;
        }

        /**
         * Returns a Predicate that tests whether a Collection is not empty.
         * A Collection is considered not empty if it is not null and has a size greater than 0.
         * 
         * @param <T> the type of the Collection to test
         * @param <E> the type of the exception that may be thrown
         * @return a Predicate that returns true if the input Collection is not empty, false otherwise
         * @see Collection#isEmpty()
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Collection, E extends Exception> Throwables.Predicate<T, E> notEmptyC() {
            return (Throwables.Predicate<T, E>) Fn.NOT_EMPTY_C;
        }

        /**
         * Returns a Predicate that tests whether a Map is not empty.
         * A Map is considered not empty if it is not null and has a size greater than 0.
         * 
         * @param <T> the type of the Map to test
         * @param <E> the type of the exception that may be thrown
         * @return a Predicate that returns true if the input Map is not empty, false otherwise
         * @see Map#isEmpty()
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Map, E extends Exception> Throwables.Predicate<T, E> notEmptyM() {
            return (Throwables.Predicate<T, E>) Fn.NOT_EMPTY_M;
        }

        /**
         * Returns a BinaryOperator that throws an exception when attempting to merge duplicate keys.
         * This merger is typically used in Collectors.toMap() when duplicate keys should not be allowed.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that throws an exception for any merge operation
         * @see java.util.stream.Collectors#toMap(Function, Function, BinaryOperator)
         */
        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> throwingMerger() {
            return BinaryOperators.THROWING_MERGER;
        }

        /**
         * Returns a BinaryOperator that ignores the second value and returns the first value.
         * This merger is typically used in Collectors.toMap() when keeping the first occurrence of duplicate keys.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the first operand
         * @see java.util.stream.Collectors#toMap(Function, Function, BinaryOperator)
         */
        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> ignoringMerger() {
            return BinaryOperators.IGNORING_MERGER;
        }

        /**
         * Returns a BinaryOperator that replaces the first value with the second value.
         * This merger is typically used in Collectors.toMap() when keeping the last occurrence of duplicate keys.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the second operand
         * @see java.util.stream.Collectors#toMap(Function, Function, BinaryOperator)
         */
        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> replacingMerger() {
            return BinaryOperators.REPLACING_MERGER;
        }

        /**
         * Returns a Predicate that tests Map.Entry objects by applying the given predicate to the entry's key.
         * The returned predicate extracts the key from the Map.Entry and applies the provided key predicate to it.
         * 
         * @param <K> the type of the key
         * @param <V> the type of the value
         * @param <E> the type of the exception that may be thrown
         * @param predicate the predicate to apply to the entry's key
         * @return a Predicate that tests Map.Entry objects by their keys
         * @throws IllegalArgumentException if predicate is null
         * @see Map.Entry#getKey()
         */
        public static <K, V, E extends Throwable> Throwables.Predicate<Map.Entry<K, V>, E> testByKey(final Throwables.Predicate<? super K, E> predicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return entry -> predicate.test(entry.getKey());
        }

        /**
         * Returns a Predicate that tests Map.Entry objects by applying the given predicate to the entry's value.
         * The returned predicate extracts the value from the Map.Entry and applies the provided value predicate to it.
         * 
         * @param <K> the type of the key
         * @param <V> the type of the value
         * @param <E> the type of the exception that may be thrown
         * @param predicate the predicate to apply to the entry's value
         * @return a Predicate that tests Map.Entry objects by their values
         * @throws IllegalArgumentException if predicate is null
         * @see Map.Entry#getValue()
         */
        public static <K, V, E extends Throwable> Throwables.Predicate<Map.Entry<K, V>, E> testByValue(final Throwables.Predicate<? super V, E> predicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return entry -> predicate.test(entry.getValue());
        }

        /**
         * Returns a Consumer that accepts Map.Entry objects and applies the given consumer to the entry's key.
         * The returned consumer extracts the key from the Map.Entry and passes it to the provided key consumer.
         * 
         * @param <K> the type of the key
         * @param <V> the type of the value
         * @param <E> the type of the exception that may be thrown
         * @param consumer the consumer to apply to the entry's key
         * @return a Consumer that processes Map.Entry objects by their keys
         * @throws IllegalArgumentException if consumer is null
         * @see Map.Entry#getKey()
         */
        public static <K, V, E extends Throwable> Throwables.Consumer<Map.Entry<K, V>, E> acceptByKey(final Throwables.Consumer<? super K, E> consumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return entry -> consumer.accept(entry.getKey());
        }

        /**
         * Returns a Consumer that accepts Map.Entry objects and applies the given consumer to the entry's value.
         * The returned consumer extracts the value from the Map.Entry and passes it to the provided value consumer.
         * 
         * @param <K> the type of the key
         * @param <V> the type of the value
         * @param <E> the type of the exception that may be thrown
         * @param consumer the consumer to apply to the entry's value
         * @return a Consumer that processes Map.Entry objects by their values
         * @throws IllegalArgumentException if consumer is null
         * @see Map.Entry#getValue()
         */
        public static <K, V, E extends Throwable> Throwables.Consumer<Map.Entry<K, V>, E> acceptByValue(final Throwables.Consumer<? super V, E> consumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return entry -> consumer.accept(entry.getValue());
        }

        /**
         * Returns a Function that applies the given function to a Map.Entry's key.
         * The returned function extracts the key from the Map.Entry and applies the provided key function to it.
         * 
         * @param <K> the type of the key
         * @param <V> the type of the value
         * @param <R> the type of the result
         * @param <E> the type of the exception that may be thrown
         * @param func the function to apply to the entry's key
         * @return a Function that transforms Map.Entry objects by applying a function to their keys
         * @throws IllegalArgumentException if func is null
         * @see Map.Entry#getKey()
         */
        public static <K, V, R, E extends Throwable> Throwables.Function<Map.Entry<K, V>, R, E> applyByKey(
                final Throwables.Function<? super K, ? extends R, E> func) throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return entry -> func.apply(entry.getKey());
        }

        /**
         * Returns a Function that applies the given function to a Map.Entry's value.
         * The returned function extracts the value from the Map.Entry and applies the provided value function to it.
         * 
         * @param <K> the type of the key
         * @param <V> the type of the value
         * @param <R> the type of the result
         * @param <E> the type of the exception that may be thrown
         * @param func the function to apply to the entry's value
         * @return a Function that transforms Map.Entry objects by applying a function to their values
         * @throws IllegalArgumentException if func is null
         * @see Map.Entry#getValue()
         */
        public static <K, V, R, E extends Throwable> Throwables.Function<Map.Entry<K, V>, R, E> applyByValue(
                final Throwables.Function<? super V, ? extends R, E> func) throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return entry -> func.apply(entry.getValue());
        }

        /** The Constant RETURN_FIRST. */
        private static final Throwables.BinaryOperator<Object, Throwable> RETURN_FIRST = (t, u) -> t;

        /**
         * Returns a BinaryOperator that always returns the first of its two operands.
         * This operator ignores the second operand and returns the first operand unchanged.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the first operand
         * @see BinaryOperator
         */
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> selectFirst() {
            return (Throwables.BinaryOperator) RETURN_FIRST;
        }

        /** The Constant RETURN_SECOND. */
        private static final Throwables.BinaryOperator<Object, Throwable> RETURN_SECOND = (t, u) -> u;

        /**
         * Returns a BinaryOperator that always returns the second of its two operands.
         * This operator ignores the first operand and returns the second operand unchanged.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the second operand
         * @see BinaryOperator
         */
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> selectSecond() {
            return (Throwables.BinaryOperator) RETURN_SECOND;
        }

        /** The Constant MIN. */
        @SuppressWarnings({ "rawtypes" })
        private static final Throwables.BinaryOperator<Comparable, Throwable> MIN = (t, u) -> N.compare(t, u) <= 0 ? t : u;

        /**
         * Returns a BinaryOperator that returns the minimum of two Comparable values.
         * The comparison is performed using the natural ordering of the Comparable type.
         * 
         * @param <T> the type of the Comparable operands and result
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the smaller of two Comparable values
         * @see Comparable#compareTo(Object)
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>, E extends Throwable> Throwables.BinaryOperator<T, E> min() {
            return (Throwables.BinaryOperator) MIN;
        }

        /**
         * Returns a BinaryOperator that returns the minimum of two values according to the specified Comparator.
         * If the comparator indicates the values are equal, the first value is returned.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @param comparator the Comparator to use for comparing values
         * @return a BinaryOperator that returns the smaller of two values according to the comparator
         * @throws IllegalArgumentException if comparator is null
         * @see Comparator#compare(Object, Object)
         */
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> min(final Comparator<? super T> comparator) throws IllegalArgumentException {
            N.checkArgNotNull(comparator);

            return (t, u) -> comparator.compare(t, u) <= 0 ? t : u;
        }

        /**
         * Returns a BinaryOperator that returns the minimum of two values by comparing the results of applying a key extractor function.
         * The key extractor function is applied to both operands and the resulting Comparable values are compared.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @param keyExtractor the function to extract a Comparable key from each operand
         * @return a BinaryOperator that returns the operand with the smaller extracted key
         * @throws IllegalArgumentException if keyExtractor is null
         * @see Comparable#compareTo(Object)
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
         * Returns a BinaryOperator that returns the minimum of two Map.Entry objects by comparing their keys.
         * The keys must be Comparable and are compared using their natural ordering.
         * 
         * @param <K> the type of the Comparable key
         * @param <V> the type of the value
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the Map.Entry with the smaller key
         * @see Map.Entry#getKey()
         * @see Comparable#compareTo(Object)
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
         * Returns a BinaryOperator that returns the minimum of two Map.Entry objects by comparing their values.
         * The values must be Comparable and are compared using their natural ordering.
         * 
         * @param <K> the type of the key
         * @param <V> the type of the Comparable value
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the Map.Entry with the smaller value
         * @see Map.Entry#getValue()
         * @see Comparable#compareTo(Object)
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K, V extends Comparable<? super V>, E extends Throwable> Throwables.BinaryOperator<Map.Entry<K, V>, E> minByValue() {
            return (Throwables.BinaryOperator) MIN_BY_VALUE;
        }

        /** The Constant MAX. */
        @SuppressWarnings("rawtypes")
        private static final Throwables.BinaryOperator<Comparable, Throwable> MAX = (t, u) -> N.compare(t, u) >= 0 ? t : u;

        /**
         * Returns a BinaryOperator that returns the maximum of two Comparable values.
         * The comparison is performed using the natural ordering of the Comparable type.
         * 
         * @param <T> the type of the Comparable operands and result
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the larger of two Comparable values
         * @see Comparable#compareTo(Object)
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>, E extends Throwable> Throwables.BinaryOperator<T, E> max() {
            return (Throwables.BinaryOperator) MAX;
        }

        /**
         * Returns a BinaryOperator that returns the maximum of two values according to the specified Comparator.
         * If the comparator indicates the values are equal, the first value is returned.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @param comparator the Comparator to use for comparing values
         * @return a BinaryOperator that returns the larger of two values according to the comparator
         * @throws IllegalArgumentException if comparator is null
         * @see Comparator#compare(Object, Object)
         */
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> max(final Comparator<? super T> comparator) throws IllegalArgumentException {
            N.checkArgNotNull(comparator);

            return (t, u) -> comparator.compare(t, u) >= 0 ? t : u;
        }

        /**
         * Returns a BinaryOperator that returns the maximum of two values by comparing the results of applying a key extractor function.
         * The key extractor function is applied to both operands and the resulting Comparable values are compared.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @param keyExtractor the function to extract a Comparable key from each operand
         * @return a BinaryOperator that returns the operand with the larger extracted key
         * @throws IllegalArgumentException if keyExtractor is null
         * @see Comparable#compareTo(Object)
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
         * Returns a BinaryOperator that returns the maximum of two Map.Entry objects by comparing their keys.
         * The keys must be Comparable and are compared using their natural ordering.
         * 
         * @param <K> the type of the Comparable key
         * @param <V> the type of the value
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the Map.Entry with the larger key
         * @see Map.Entry#getKey()
         * @see Comparable#compareTo(Object)
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
         * Returns a BinaryOperator that returns the maximum of two Map.Entry objects by comparing their values.
         * The values must be Comparable and are compared using their natural ordering.
         * 
         * @param <K> the type of the key
         * @param <V> the type of the Comparable value
         * @param <E> the type of the exception that may be thrown
         * @return a BinaryOperator that returns the Map.Entry with the larger value
         * @see Map.Entry#getValue()
         * @see Comparable#compareTo(Object)
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K, V extends Comparable<? super V>, E extends Throwable> Throwables.BinaryOperator<Map.Entry<K, V>, E> maxByValue() {
            return (Throwables.BinaryOperator) MAX_BY_VALUE;
        }

        /**
         * Returns a Predicate that represents the logical negation of the given predicate.
         * When evaluated, the returned predicate returns true if the given predicate returns false, and vice versa.
         * 
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param predicate the predicate to negate
         * @return a Predicate that represents the logical negation of the given predicate
         * @throws IllegalArgumentException if predicate is null
         * @see Predicate#negate()
         */
        public static <T, E extends Throwable> Throwables.Predicate<T, E> not(final Throwables.Predicate<T, E> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return t -> !predicate.test(t);
        }

        /**
         * Returns a BiPredicate that represents the logical negation of the given bi-predicate.
         * When evaluated, the returned bi-predicate returns true if the given bi-predicate returns false, and vice versa.
         * 
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param biPredicate the bi-predicate to negate
         * @return a BiPredicate that represents the logical negation of the given bi-predicate
         * @throws IllegalArgumentException if biPredicate is null
         * @see BiPredicate#negate()
         */
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> not(final Throwables.BiPredicate<T, U, E> biPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(biPredicate);

            return (t, u) -> !biPredicate.test(t, u);
        }

        /**
         * Returns a TriPredicate that represents the logical negation of the given tri-predicate.
         * When evaluated, the returned tri-predicate returns true if the given tri-predicate returns false, and vice versa.
         * 
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param triPredicate the tri-predicate to negate
         * @return a TriPredicate that represents the logical negation of the given tri-predicate
         * @throws IllegalArgumentException if triPredicate is null
         */
        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> not(final Throwables.TriPredicate<A, B, C, E> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return (a, b, c) -> !triPredicate.test(a, b, c);
        }

        /**
         * Returns a stateful Predicate that returns true for at most the specified number of evaluations.
         * The predicate maintains an internal counter that decrements with each test, returning true 
         * while the counter is positive and false once it reaches zero. This predicate is thread-safe
         * and can be used in parallel streams.
         * 
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param count the maximum number of times the predicate should return true
         * @return a stateful Predicate that limits the number of true results. Don't save or cache for reuse.
         * @throws IllegalArgumentException if count is negative
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
         * Converts a standard Java Supplier to a Throwables.Supplier.
         * If the input is already a Throwables.Supplier, it is returned as-is.
         * 
         * @param <T> the type of results supplied by the supplier
         * @param <E> the type of the exception that may be thrown
         * @param supplier the Java Supplier to convert
         * @return a Throwables.Supplier that delegates to the given supplier
         * @see java.util.function.Supplier
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.Supplier<T, E> from(final java.util.function.Supplier<T> supplier) {
            return supplier instanceof Throwables.Supplier ? ((Throwables.Supplier) supplier) : supplier::get;
        }

        /**
         * Converts a standard Java IntFunction to a Throwables.IntFunction.
         * If the input is already a Throwables.IntFunction, it is returned as-is.
         * 
         * @param <T> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param func the Java IntFunction to convert
         * @return a Throwables.IntFunction that delegates to the given function
         * @see java.util.function.IntFunction
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.IntFunction<T, E> from(final java.util.function.IntFunction<? extends T> func) {
            return func instanceof Throwables.IntFunction ? ((Throwables.IntFunction) func) : func::apply;
        }

        /**
         * Converts a standard Java Predicate to a Throwables.Predicate.
         * If the input is already a Throwables.Predicate, it is returned as-is.
         * 
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param predicate the Java Predicate to convert
         * @return a Throwables.Predicate that delegates to the given predicate
         * @see java.util.function.Predicate
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.Predicate<T, E> from(final java.util.function.Predicate<T> predicate) {
            return predicate instanceof Throwables.Predicate ? ((Throwables.Predicate) predicate) : predicate::test;
        }

        /**
         * Converts a standard Java BiPredicate to a Throwables.BiPredicate.
         * If the input is already a Throwables.BiPredicate, it is returned as-is.
         * 
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param predicate the Java BiPredicate to convert
         * @return a Throwables.BiPredicate that delegates to the given predicate
         * @see java.util.function.BiPredicate
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> from(final java.util.function.BiPredicate<T, U> predicate) {
            return predicate instanceof Throwables.BiPredicate ? ((Throwables.BiPredicate) predicate) : predicate::test;
        }

        /**
         * Converts a standard Java Consumer to a Throwables.Consumer.
         * If the input is already a Throwables.Consumer, it is returned as-is.
         * 
         * @param <T> the type of the input to the consumer
         * @param <E> the type of the exception that may be thrown
         * @param consumer the Java Consumer to convert
         * @return a Throwables.Consumer that delegates to the given consumer
         * @see java.util.function.Consumer
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.Consumer<T, E> from(final java.util.function.Consumer<T> consumer) {
            return consumer instanceof Throwables.Consumer ? ((Throwables.Consumer) consumer) : consumer::accept;
        }

        /**
         * Converts a standard Java BiConsumer to a Throwables.BiConsumer.
         * If the input is already a Throwables.BiConsumer, it is returned as-is.
         * 
         * @param <T> the type of the first argument to the consumer
         * @param <U> the type of the second argument to the consumer
         * @param <E> the type of the exception that may be thrown
         * @param consumer the Java BiConsumer to convert
         * @return a Throwables.BiConsumer that delegates to the given consumer
         * @see java.util.function.BiConsumer
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> from(final java.util.function.BiConsumer<T, U> consumer) {
            return consumer instanceof Throwables.BiConsumer ? ((Throwables.BiConsumer) consumer) : consumer::accept;
        }

        /**
         * Converts a standard Java Function to a Throwables.Function.
         * If the input is already a Throwables.Function, it is returned as-is.
         * 
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param function the Java Function to convert
         * @return a Throwables.Function that delegates to the given function
         * @see java.util.function.Function
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> from(final java.util.function.Function<T, ? extends R> function) {
            return function instanceof Throwables.Function ? ((Throwables.Function) function) : function::apply;
        }

        /**
         * Converts a standard Java BiFunction to a Throwables.BiFunction.
         * If the input is already a Throwables.BiFunction, it is returned as-is.
         * 
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param function the Java BiFunction to convert
         * @return a Throwables.BiFunction that delegates to the given function
         * @see java.util.function.BiFunction
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> from(final java.util.function.BiFunction<T, U, ? extends R> function) {
            return function instanceof Throwables.BiFunction ? ((Throwables.BiFunction) function) : function::apply;
        }

        /**
         * Converts a standard Java UnaryOperator to a Throwables.UnaryOperator.
         * If the input is already a Throwables.UnaryOperator, it is returned as-is.
         * 
         * @param <T> the type of the operand and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @param op the Java UnaryOperator to convert
         * @return a Throwables.UnaryOperator that delegates to the given operator
         * @see java.util.function.UnaryOperator
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.UnaryOperator<T, E> from(final java.util.function.UnaryOperator<T> op) {
            return op instanceof Throwables.UnaryOperator ? ((Throwables.UnaryOperator) op) : op::apply;
        }

        /**
         * Converts a standard Java BinaryOperator to a Throwables.BinaryOperator.
         * If the input is already a Throwables.BinaryOperator, it is returned as-is.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @param op the Java BinaryOperator to convert
         * @return a Throwables.BinaryOperator that delegates to the given operator
         * @see java.util.function.BinaryOperator
         */
        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> from(final java.util.function.BinaryOperator<T> op) {
            return op instanceof Throwables.BinaryOperator ? ((Throwables.BinaryOperator) op) : op::apply;
        }

        /**
         * Returns the provided Throwables.Supplier as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of results supplied by the supplier
         * @param <E> the type of the exception that may be thrown
         * @param supplier the supplier to return
         * @return the supplier unchanged
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Supplier<T, E> s(final Throwables.Supplier<T, E> supplier) {
            return supplier;
        }

        /**
         * Creates a Throwables.Supplier by partially applying a function to a fixed argument.
         * The returned supplier will invoke the function with the provided argument when called.
         * 
         * @param <A> the type of the fixed argument
         * @param <T> the type of the result
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed argument to apply to the function
         * @param func the function to partially apply
         * @return a Supplier that applies the function to the fixed argument
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Supplier<T, E> s(final A a, final Throwables.Function<? super A, ? extends T, E> func) {
            return () -> func.apply(a);
        }

        /**
         * Returns the provided Throwables.Predicate as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param predicate the predicate to return
         * @return the predicate unchanged
         * @see #from(java.util.function.Predicate)
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> p(final Throwables.Predicate<T, E> predicate) {
            return predicate;
        }

        /**
         * Creates a Throwables.Predicate by partially applying a BiPredicate to a fixed first argument.
         * The returned predicate will invoke the bi-predicate with the fixed argument and the test input.
         * 
         * @param <A> the type of the fixed first argument
         * @param <T> the type of the input to the resulting predicate
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the bi-predicate
         * @param biPredicate the bi-predicate to partially apply
         * @return a Predicate that applies the bi-predicate with the fixed first argument
         * @throws IllegalArgumentException if biPredicate is null
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Predicate<T, E> p(final A a, final Throwables.BiPredicate<A, T, E> biPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(biPredicate);

            return t -> biPredicate.test(a, t);
        }

        /**
         * Creates a Throwables.Predicate by partially applying a TriPredicate to fixed first and second arguments.
         * The returned predicate will invoke the tri-predicate with the fixed arguments and the test input.
         * 
         * @param <A> the type of the fixed first argument
         * @param <B> the type of the fixed second argument
         * @param <T> the type of the input to the resulting predicate
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the tri-predicate
         * @param b the fixed second argument to apply to the tri-predicate
         * @param triPredicate the tri-predicate to partially apply
         * @return a Predicate that applies the tri-predicate with the fixed arguments
         * @throws IllegalArgumentException if triPredicate is null
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Predicate<T, E> p(final A a, final B b, final Throwables.TriPredicate<A, B, T, E> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return t -> triPredicate.test(a, b, t);
        }

        /**
         * Returns the provided Throwables.BiPredicate as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param biPredicate the bi-predicate to return
         * @return the bi-predicate unchanged
         * @see #from(java.util.function.BiPredicate)
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> p(final Throwables.BiPredicate<T, U, E> biPredicate) {
            return biPredicate;
        }

        /**
         * Creates a Throwables.BiPredicate by partially applying a TriPredicate to a fixed first argument.
         * The returned bi-predicate will invoke the tri-predicate with the fixed argument and the two test inputs.
         * 
         * @param <A> the type of the fixed first argument
         * @param <T> the type of the first argument to the resulting bi-predicate
         * @param <U> the type of the second argument to the resulting bi-predicate
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the tri-predicate
         * @param triPredicate the tri-predicate to partially apply
         * @return a BiPredicate that applies the tri-predicate with the fixed first argument
         * @throws IllegalArgumentException if triPredicate is null
         */
        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> p(final A a, final Throwables.TriPredicate<A, T, U, E> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return (t, u) -> triPredicate.test(a, t, u);
        }

        /**
         * Returns the provided Throwables.TriPredicate as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param triPredicate the tri-predicate to return
         * @return the tri-predicate unchanged
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> p(final Throwables.TriPredicate<A, B, C, E> triPredicate) {
            return triPredicate;
        }

        /**
         * Returns the provided Throwables.Consumer as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of the input to the consumer
         * @param <E> the type of the exception that may be thrown
         * @param consumer the consumer to return
         * @return the consumer unchanged
         * @see #from(java.util.function.Consumer)
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> c(final Throwables.Consumer<T, E> consumer) {
            return consumer;
        }

        /**
         * Creates a Throwables.Consumer by partially applying a BiConsumer to a fixed first argument.
         * The returned consumer will invoke the bi-consumer with the fixed argument and the consumed input.
         * 
         * @param <A> the type of the fixed first argument
         * @param <T> the type of the input to the resulting consumer
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the bi-consumer
         * @param biConsumer the bi-consumer to partially apply
         * @return a Consumer that applies the bi-consumer with the fixed first argument
         * @throws IllegalArgumentException if biConsumer is null
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Consumer<T, E> c(final A a, final Throwables.BiConsumer<A, T, E> biConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return t -> biConsumer.accept(a, t);
        }

        /**
         * Creates a Throwables.Consumer by partially applying a TriConsumer to fixed first and second arguments.
         * The returned consumer will invoke the tri-consumer with the fixed arguments and the consumed input.
         * 
         * @param <A> the type of the fixed first argument
         * @param <B> the type of the fixed second argument
         * @param <T> the type of the input to the resulting consumer
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the tri-consumer
         * @param b the fixed second argument to apply to the tri-consumer
         * @param triConsumer the tri-consumer to partially apply
         * @return a Consumer that applies the tri-consumer with the fixed arguments
         * @throws IllegalArgumentException if triConsumer is null
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Consumer<T, E> c(final A a, final B b, final Throwables.TriConsumer<A, B, T, E> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return t -> triConsumer.accept(a, b, t);
        }

        /**
         * Returns the provided Throwables.BiConsumer as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of the first argument to the consumer
         * @param <U> the type of the second argument to the consumer
         * @param <E> the type of the exception that may be thrown
         * @param biConsumer the bi-consumer to return
         * @return the bi-consumer unchanged
         * @see #from(java.util.function.BiConsumer)
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> c(final Throwables.BiConsumer<T, U, E> biConsumer) {
            return biConsumer;
        }

        /**
         * Creates a Throwables.BiConsumer by partially applying a TriConsumer to a fixed first argument.
         * The returned bi-consumer will invoke the tri-consumer with the fixed argument and the two consumed inputs.
         * 
         * @param <A> the type of the fixed first argument
         * @param <T> the type of the first argument to the resulting bi-consumer
         * @param <U> the type of the second argument to the resulting bi-consumer
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the tri-consumer
         * @param triConsumer the tri-consumer to partially apply
         * @return a BiConsumer that applies the tri-consumer with the fixed first argument
         * @throws IllegalArgumentException if triConsumer is null
         */
        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> c(final A a, final Throwables.TriConsumer<A, T, U, E> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (t, u) -> triConsumer.accept(a, t, u);
        }

        /**
         * Returns the provided Throwables.TriConsumer as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <A> the type of the first argument to the consumer
         * @param <B> the type of the second argument to the consumer
         * @param <C> the type of the third argument to the consumer
         * @param <E> the type of the exception that may be thrown
         * @param triConsumer the tri-consumer to return
         * @return the tri-consumer unchanged
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriConsumer<A, B, C, E> c(final Throwables.TriConsumer<A, B, C, E> triConsumer) {
            return triConsumer;
        }

        /**
         * Returns the provided Throwables.Function as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param function the function to return
         * @return the function unchanged
         * @see #from(java.util.function.Function)
         */
        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> f(final Throwables.Function<T, R, E> function) {
            return function;
        }

        /**
         * Creates a Throwables.Function by partially applying a BiFunction to a fixed first argument.
         * The returned function will invoke the bi-function with the fixed argument and the function input.
         * 
         * @param <A> the type of the fixed first argument
         * @param <T> the type of the input to the resulting function
         * @param <R> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the bi-function
         * @param biFunction the bi-function to partially apply
         * @return a Function that applies the bi-function with the fixed first argument
         * @throws IllegalArgumentException if biFunction is null
         */
        @Beta
        public static <A, T, R, E extends Throwable> Throwables.Function<T, R, E> f(final A a, final Throwables.BiFunction<A, T, R, E> biFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(biFunction);

            return t -> biFunction.apply(a, t);
        }

        /**
         * Creates a Throwables.Function by partially applying a TriFunction to fixed first and second arguments.
         * The returned function will invoke the tri-function with the fixed arguments and the function input.
         * 
         * @param <A> the type of the fixed first argument
         * @param <B> the type of the fixed second argument
         * @param <T> the type of the input to the resulting function
         * @param <R> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the tri-function
         * @param b the fixed second argument to apply to the tri-function
         * @param triFunction the tri-function to partially apply
         * @return a Function that applies the tri-function with the fixed arguments
         * @throws IllegalArgumentException if triFunction is null
         */
        @Beta
        public static <A, B, T, R, E extends Throwable> Throwables.Function<T, R, E> f(final A a, final B b,
                final Throwables.TriFunction<A, B, T, R, E> triFunction) throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return t -> triFunction.apply(a, b, t);
        }

        /**
         * Returns the provided Throwables.BiFunction as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param biFunction the bi-function to return
         * @return the bi-function unchanged
         * @see #from(java.util.function.BiFunction)
         */
        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> f(final Throwables.BiFunction<T, U, R, E> biFunction) {
            return biFunction;
        }

        /**
         * Creates a Throwables.BiFunction by partially applying a TriFunction to a fixed first argument.
         * The returned bi-function will invoke the tri-function with the fixed argument and the two function inputs.
         * 
         * @param <A> the type of the fixed first argument
         * @param <T> the type of the first argument to the resulting bi-function
         * @param <U> the type of the second argument to the resulting bi-function
         * @param <R> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the tri-function
         * @param triFunction the tri-function to partially apply
         * @return a BiFunction that applies the tri-function with the fixed first argument
         * @throws IllegalArgumentException if triFunction is null
         */
        @Beta
        public static <A, T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> f(final A a, final Throwables.TriFunction<A, T, U, R, E> triFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return (t, u) -> triFunction.apply(a, t, u);
        }

        /**
         * Returns the provided Throwables.TriFunction as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <A> the type of the first argument to the function
         * @param <B> the type of the second argument to the function
         * @param <C> the type of the third argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the exception that may be thrown
         * @param triFunction the tri-function to return
         * @return the tri-function unchanged
         */
        @Beta
        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> f(final Throwables.TriFunction<A, B, C, R, E> triFunction) {
            return triFunction;
        }

        /**
         * Returns the provided Throwables.UnaryOperator as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of the operand and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @param unaryOperator the unary operator to return
         * @return the unary operator unchanged
         */
        @Beta
        public static <T, E extends Throwable> Throwables.UnaryOperator<T, E> o(final Throwables.UnaryOperator<T, E> unaryOperator) {
            N.checkArgNotNull(unaryOperator);

            return unaryOperator;
        }

        /**
         * Returns the provided Throwables.BinaryOperator as-is.
         * This is a shorthand identity method that can help with type inference in certain contexts.
         * 
         * @param <T> the type of the operands and result of the operator
         * @param <E> the type of the exception that may be thrown
         * @param binaryOperator the binary operator to return
         * @return the binary operator unchanged
         */
        @Beta
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> o(final Throwables.BinaryOperator<T, E> binaryOperator) {
            N.checkArgNotNull(binaryOperator);

            return binaryOperator;
        }

        /**
         * Returns the provided Throwables.BiConsumer as-is.
         * This is a shorthand identity method for a mapper that can help with type inference in certain contexts,
         * particularly when used with stream operations like mapMulti.
         *
         * <p>Example usage:</p>
         * <pre>{@code
         * // Using mc() to help with type inference in a stream operation
         * Seq<List<String>, Exception> seq = ...;
         * seq<String, Exception> flatStream = seq.mapMulti(
         *     Fnn.mc((List<String> list, Consumer<String> consumer) -> {
         *         for (String item : list) {
         *             if (item != null && !item.isEmpty()) {
         *                 consumer.accept(item);
         *             }
         *         }
         *     }));
         * }</pre>
         *
         * @param <T> the type of the first argument to the consumer
         * @param <U> the type of elements to be accepted by the result consumer
         * @param <E> the type of the exception that may be thrown
         * @param mapper the mapping bi-consumer to return
         * @return the bi-consumer unchanged
         * @see Seq#mapMulti(Throwables.BiConsumer)
         * @see Stream#mapMulti(java.util.function.BiConsumer)
         * @see Fn#mc(java.util.function.BiConsumer)
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, java.util.function.Consumer<U>, E> mc(
                final Throwables.BiConsumer<? super T, ? extends java.util.function.Consumer<U>, E> mapper) {
            N.checkArgNotNull(mapper);

            return (Throwables.BiConsumer<T, java.util.function.Consumer<U>, E>) mapper;
        }

        /**
         * Casts a standard Java Predicate to a Throwables.Predicate.
         * This method performs an unchecked cast and should be used with caution.
         * 
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the exception that may be thrown
         * @param predicate the Java Predicate to cast
         * @return the predicate cast to Throwables.Predicate
         * @throws IllegalArgumentException if predicate is null
         * @see #from(java.util.function.Predicate)
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> pp(final Predicate<T> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate);

            return (Throwables.Predicate<T, E>) predicate;
        }

        /**
         * Creates a Throwables.Predicate by partially applying a standard Java BiPredicate to a fixed first argument.
         * The returned predicate will invoke the bi-predicate with the fixed argument and the test input.
         * 
         * @param <A> the type of the fixed first argument
         * @param <T> the type of the input to the resulting predicate
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the bi-predicate
         * @param biPredicate the Java BiPredicate to partially apply
         * @return a Throwables.Predicate that applies the bi-predicate with the fixed first argument
         * @throws IllegalArgumentException if biPredicate is null
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Predicate<T, E> pp(final A a, final java.util.function.BiPredicate<A, T> biPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(biPredicate);

            return t -> biPredicate.test(a, t);
        }

        /**
         * Creates a Throwables.Predicate by partially applying a TriPredicate to fixed first and second arguments.
         * The returned predicate will invoke the tri-predicate with the fixed arguments and the test input.
         * 
         * @param <A> the type of the fixed first argument
         * @param <B> the type of the fixed second argument
         * @param <T> the type of the input to the resulting predicate
         * @param <E> the type of the exception that may be thrown
         * @param a the fixed first argument to apply to the tri-predicate
         * @param b the fixed second argument to apply to the tri-predicate
         * @param triPredicate the TriPredicate to partially apply
         * @return a Throwables.Predicate that applies the tri-predicate with the fixed arguments
         * @throws IllegalArgumentException if triPredicate is null
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Predicate<T, E> pp(final A a, final B b, final TriPredicate<A, B, T> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return t -> triPredicate.test(a, b, t);
        }

        /**
        * Returns a BiPredicate that can throw checked exceptions by wrapping the provided BiPredicate.
        * This method is used to convert a standard BiPredicate to a Throwables.BiPredicate, allowing
        * it to be used in contexts where checked exceptions are expected.
        *
        * @param <T> the type of the first argument to the predicate
        * @param <U> the type of the second argument to the predicate
        * @param <E> the type of the checked exception that the returned predicate may throw
        * @param biPredicate the BiPredicate to wrap
        * @return a Throwables.BiPredicate that wraps the provided BiPredicate
        * @throws IllegalArgumentException if biPredicate is null
        * @see #from(java.util.function.BiPredicate)
        */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> pp(final BiPredicate<T, U> biPredicate) throws IllegalArgumentException {
            N.checkArgNotNull(biPredicate);

            return (Throwables.BiPredicate<T, U, E>) biPredicate;
        }

        /**
         * Returns a BiPredicate that partially applies the first argument to a TriPredicate.
         * The returned BiPredicate will test its two arguments along with the pre-supplied
         * first argument against the original TriPredicate.
         *
         * @param <A> the type of the first argument to the TriPredicate
         * @param <T> the type of the second argument to the TriPredicate (first argument to the returned BiPredicate)
         * @param <U> the type of the third argument to the TriPredicate (second argument to the returned BiPredicate)
         * @param <E> the type of the checked exception that the returned predicate may throw
         * @param a the first argument to be partially applied to the TriPredicate
         * @param triPredicate the TriPredicate to be partially applied
         * @return a Throwables.BiPredicate that partially applies the first argument to the TriPredicate
         * @throws IllegalArgumentException if triPredicate is null
         */
        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> pp(final A a, final TriPredicate<A, T, U> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return (t, u) -> triPredicate.test(a, t, u);
        }

        /**
         * Returns a TriPredicate that can throw checked exceptions by wrapping the provided TriPredicate.
         * This method is used to convert a standard TriPredicate to a Throwables.TriPredicate, allowing
         * it to be used in contexts where checked exceptions are expected.
         *
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @param <E> the type of the checked exception that the returned predicate may throw
         * @param triPredicate the TriPredicate to wrap
         * @return a Throwables.TriPredicate that wraps the provided TriPredicate
         * @throws IllegalArgumentException if triPredicate is null
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> pp(final TriPredicate<A, B, C> triPredicate)
                throws IllegalArgumentException {
            N.checkArgNotNull(triPredicate);

            return (Throwables.TriPredicate<A, B, C, E>) triPredicate;
        }

        /**
         * Returns a Consumer that can throw checked exceptions by wrapping the provided Consumer.
         * This method is used to convert a standard Consumer to a Throwables.Consumer, allowing
         * it to be used in contexts where checked exceptions are expected.
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of the checked exception that the returned consumer may throw
         * @param consumer the Consumer to wrap
         * @return a Throwables.Consumer that wraps the provided Consumer
         * @throws IllegalArgumentException if consumer is null
         * @see #from(java.util.function.Consumer)
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> cc(final Consumer<T> consumer) throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return (Throwables.Consumer<T, E>) consumer;
        }

        /**
         * Returns a Consumer that partially applies the first argument to a BiConsumer.
         * The returned Consumer will accept its argument along with the pre-supplied
         * first argument to the original BiConsumer.
         *
         * @param <A> the type of the first argument to the BiConsumer
         * @param <T> the type of the second argument to the BiConsumer (argument to the returned Consumer)
         * @param <E> the type of the checked exception that the returned consumer may throw
         * @param a the first argument to be partially applied to the BiConsumer
         * @param biConsumer the BiConsumer to be partially applied
         * @return a Throwables.Consumer that partially applies the first argument to the BiConsumer
         * @throws IllegalArgumentException if biConsumer is null
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Consumer<T, E> cc(final A a, final java.util.function.BiConsumer<A, T> biConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return t -> biConsumer.accept(a, t);
        }

        /**
         * Returns a Consumer that partially applies the first two arguments to a TriConsumer.
         * The returned Consumer will accept its argument along with the pre-supplied
         * first two arguments to the original TriConsumer.
         *
         * @param <A> the type of the first argument to the TriConsumer
         * @param <B> the type of the second argument to the TriConsumer
         * @param <T> the type of the third argument to the TriConsumer (argument to the returned Consumer)
         * @param <E> the type of the checked exception that the returned consumer may throw
         * @param a the first argument to be partially applied to the TriConsumer
         * @param b the second argument to be partially applied to the TriConsumer
         * @param triConsumer the TriConsumer to be partially applied
         * @return a Throwables.Consumer that partially applies the first two arguments to the TriConsumer
         * @throws IllegalArgumentException if triConsumer is null
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Consumer<T, E> cc(final A a, final B b, final TriConsumer<A, B, T> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return t -> triConsumer.accept(a, b, t);
        }

        /**
         * Returns a BiConsumer that can throw checked exceptions by wrapping the provided BiConsumer.
         * This method is used to convert a standard BiConsumer to a Throwables.BiConsumer, allowing
         * it to be used in contexts where checked exceptions are expected.
         *
         * @param <T> the type of the first argument to the consumer
         * @param <U> the type of the second argument to the consumer
         * @param <E> the type of the checked exception that the returned consumer may throw
         * @param biConsumer the BiConsumer to wrap
         * @return a Throwables.BiConsumer that wraps the provided BiConsumer
         * @throws IllegalArgumentException if biConsumer is null
         * @see #from(java.util.function.BiConsumer)
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> cc(final BiConsumer<T, U> biConsumer) throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return (Throwables.BiConsumer<T, U, E>) biConsumer;
        }

        /**
         * Returns a BiConsumer that partially applies the first argument to a TriConsumer.
         * The returned BiConsumer will accept its two arguments along with the pre-supplied
         * first argument to the original TriConsumer.
         *
         * @param <A> the type of the first argument to the TriConsumer
         * @param <T> the type of the second argument to the TriConsumer (first argument to the returned BiConsumer)
         * @param <U> the type of the third argument to the TriConsumer (second argument to the returned BiConsumer)
         * @param <E> the type of the checked exception that the returned consumer may throw
         * @param a the first argument to be partially applied to the TriConsumer
         * @param triConsumer the TriConsumer to be partially applied
         * @return a Throwables.BiConsumer that partially applies the first argument to the TriConsumer
         * @throws IllegalArgumentException if triConsumer is null
         */
        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> cc(final A a, final TriConsumer<A, T, U> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (t, u) -> triConsumer.accept(a, t, u);
        }

        /**
         * Returns a TriConsumer that can throw checked exceptions by wrapping the provided TriConsumer.
         * This method is used to convert a standard TriConsumer to a Throwables.TriConsumer, allowing
         * it to be used in contexts where checked exceptions are expected.
         *
         * @param <A> the type of the first argument to the consumer
         * @param <B> the type of the second argument to the consumer
         * @param <C> the type of the third argument to the consumer
         * @param <E> the type of the checked exception that the returned consumer may throw
         * @param triConsumer the TriConsumer to wrap
         * @return a Throwables.TriConsumer that wraps the provided TriConsumer
         * @throws IllegalArgumentException if triConsumer is null
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriConsumer<A, B, C, E> cc(final TriConsumer<A, B, C> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (Throwables.TriConsumer<A, B, C, E>) triConsumer;
        }

        /**
         * Returns a Function that can throw checked exceptions by wrapping the provided Function.
         * This method is used to convert a standard Function to a Throwables.Function, allowing
         * it to be used in contexts where checked exceptions are expected.
         *
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the returned function may throw
         * @param function the Function to wrap
         * @return a Throwables.Function that wraps the provided Function
         * @throws IllegalArgumentException if function is null
         * @see #from(java.util.function.Function)
         */
        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final Function<T, ? extends R> function) throws IllegalArgumentException {
            N.checkArgNotNull(function);

            return (Throwables.Function<T, R, E>) function;
        }

        /**
         * Returns a Function that partially applies the first argument to a BiFunction.
         * The returned Function will apply its argument along with the pre-supplied
         * first argument to the original BiFunction.
         *
         * @param <A> the type of the first argument to the BiFunction
         * @param <T> the type of the second argument to the BiFunction (argument to the returned Function)
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the returned function may throw
         * @param a the first argument to be partially applied to the BiFunction
         * @param biFunction the BiFunction to be partially applied
         * @return a Throwables.Function that partially applies the first argument to the BiFunction
         * @throws IllegalArgumentException if biFunction is null
         */
        @Beta
        public static <A, T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final A a, final java.util.function.BiFunction<A, T, R> biFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(biFunction);

            return t -> biFunction.apply(a, t);
        }

        /**
         * Returns a Function that partially applies the first two arguments to a TriFunction.
         * The returned Function will apply its argument along with the pre-supplied
         * first two arguments to the original TriFunction.
         *
         * @param <A> the type of the first argument to the TriFunction
         * @param <B> the type of the second argument to the TriFunction
         * @param <T> the type of the third argument to the TriFunction (argument to the returned Function)
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the returned function may throw
         * @param a the first argument to be partially applied to the TriFunction
         * @param b the second argument to be partially applied to the TriFunction
         * @param triFunction the TriFunction to be partially applied
         * @return a Throwables.Function that partially applies the first two arguments to the TriFunction
         * @throws IllegalArgumentException if triFunction is null
         */
        @Beta
        public static <A, B, T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final A a, final B b, final TriFunction<A, B, T, R> triFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return t -> triFunction.apply(a, b, t);
        }

        /**
         * Returns a BiFunction that can throw checked exceptions by wrapping the provided BiFunction.
         * This method is used to convert a standard BiFunction to a Throwables.BiFunction, allowing
         * it to be used in contexts where checked exceptions are expected.
         *
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the returned function may throw
         * @param biFunction the BiFunction to wrap
         * @return a Throwables.BiFunction that wraps the provided BiFunction
         * @throws IllegalArgumentException if biFunction is null
         * @see #from(java.util.function.BiFunction)
         */
        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> ff(final BiFunction<T, U, R> biFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(biFunction);

            return (Throwables.BiFunction<T, U, R, E>) biFunction;
        }

        /**
         * Returns a BiFunction that partially applies the first argument to a TriFunction.
         * The returned BiFunction will apply its two arguments along with the pre-supplied
         * first argument to the original TriFunction.
         *
         * @param <A> the type of the first argument to the TriFunction
         * @param <T> the type of the second argument to the TriFunction (first argument to the returned BiFunction)
         * @param <U> the type of the third argument to the TriFunction (second argument to the returned BiFunction)
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the returned function may throw
         * @param a the first argument to be partially applied to the TriFunction
         * @param triFunction the TriFunction to be partially applied
         * @return a Throwables.BiFunction that partially applies the first argument to the TriFunction
         * @throws IllegalArgumentException if triFunction is null
         */
        @Beta
        public static <A, T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> ff(final A a, final TriFunction<A, T, U, R> triFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return (t, u) -> triFunction.apply(a, t, u);
        }

        /**
         * Returns a TriFunction that can throw checked exceptions by wrapping the provided TriFunction.
         * This method is used to convert a standard TriFunction to a Throwables.TriFunction, allowing
         * it to be used in contexts where checked exceptions are expected.
         *
         * @param <A> the type of the first argument to the function
         * @param <B> the type of the second argument to the function
         * @param <C> the type of the third argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the returned function may throw
         * @param triFunction the TriFunction to wrap
         * @return a Throwables.TriFunction that wraps the provided TriFunction
         * @throws IllegalArgumentException if triFunction is null
         */
        @Beta
        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> ff(final TriFunction<A, B, C, R> triFunction)
                throws IllegalArgumentException {
            N.checkArgNotNull(triFunction);

            return (Throwables.TriFunction<A, B, C, R, E>) triFunction;
        }

        /**
         * Returns a synchronized Predicate that executes the provided predicate within a synchronized block.
         * All calls to the returned predicate's test method will be synchronized on the specified mutex object.
         *
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the checked exception that the predicate may throw
         * @param mutex the object to synchronize on
         * @param predicate the predicate to be executed within the synchronized block
         * @return a Throwables.Predicate that synchronizes on the mutex before executing the predicate
         * @throws IllegalArgumentException if mutex or predicate is null
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
         * Returns a synchronized Predicate that partially applies the first argument to a BiPredicate
         * and executes it within a synchronized block. All calls to the returned predicate's test method
         * will be synchronized on the specified mutex object.
         *
         * @param <A> the type of the first argument to the BiPredicate
         * @param <T> the type of the second argument to the BiPredicate (argument to the returned Predicate)
         * @param <E> the type of the checked exception that the predicate may throw
         * @param mutex the object to synchronize on
         * @param a the first argument to be partially applied to the BiPredicate
         * @param biPredicate the BiPredicate to be partially applied and executed within the synchronized block
         * @return a Throwables.Predicate that synchronizes on the mutex before executing the partially applied BiPredicate
         * @throws IllegalArgumentException if mutex or biPredicate is null
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
         * Returns a synchronized BiPredicate that executes the provided BiPredicate within a synchronized block.
         * All calls to the returned BiPredicate's test method will be synchronized on the specified mutex object.
         *
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param <E> the type of the checked exception that the predicate may throw
         * @param mutex the object to synchronize on
         * @param biPredicate the BiPredicate to be executed within the synchronized block
         * @return a Throwables.BiPredicate that synchronizes on the mutex before executing the BiPredicate
         * @throws IllegalArgumentException if mutex or biPredicate is null
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
         * Returns a synchronized Consumer that executes the provided consumer within a synchronized block.
         * All calls to the returned consumer's accept method will be synchronized on the specified mutex object.
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of the checked exception that the consumer may throw
         * @param mutex the object to synchronize on
         * @param consumer the consumer to be executed within the synchronized block
         * @return a Throwables.Consumer that synchronizes on the mutex before executing the consumer
         * @throws IllegalArgumentException if mutex or consumer is null
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
         * Returns a synchronized Consumer that partially applies the first argument to a BiConsumer
         * and executes it within a synchronized block. All calls to the returned consumer's accept method
         * will be synchronized on the specified mutex object.
         *
         * @param <A> the type of the first argument to the BiConsumer
         * @param <T> the type of the second argument to the BiConsumer (argument to the returned Consumer)
         * @param <E> the type of the checked exception that the consumer may throw
         * @param mutex the object to synchronize on
         * @param a the first argument to be partially applied to the BiConsumer
         * @param biConsumer the BiConsumer to be partially applied and executed within the synchronized block
         * @return a Throwables.Consumer that synchronizes on the mutex before executing the partially applied BiConsumer
         * @throws IllegalArgumentException if mutex or biConsumer is null
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
         * Returns a synchronized BiConsumer that executes the provided BiConsumer within a synchronized block.
         * All calls to the returned BiConsumer's accept method will be synchronized on the specified mutex object.
         *
         * @param <T> the type of the first argument to the consumer
         * @param <U> the type of the second argument to the consumer
         * @param <E> the type of the checked exception that the consumer may throw
         * @param mutex the object to synchronize on
         * @param biConsumer the BiConsumer to be executed within the synchronized block
         * @return a Throwables.BiConsumer that synchronizes on the mutex before executing the BiConsumer
         * @throws IllegalArgumentException if mutex or biConsumer is null
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
         * Returns a synchronized Function that executes the provided function within a synchronized block.
         * All calls to the returned function's apply method will be synchronized on the specified mutex object.
         *
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the function may throw
         * @param mutex the object to synchronize on
         * @param function the function to be executed within the synchronized block
         * @return a Throwables.Function that synchronizes on the mutex before executing the function
         * @throws IllegalArgumentException if mutex or function is null
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
         * Returns a synchronized Function that partially applies the first argument to a BiFunction
         * and executes it within a synchronized block. All calls to the returned function's apply method
         * will be synchronized on the specified mutex object.
         *
         * @param <A> the type of the first argument to the BiFunction
         * @param <T> the type of the second argument to the BiFunction (argument to the returned Function)
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the function may throw
         * @param mutex the object to synchronize on
         * @param a the first argument to be partially applied to the BiFunction
         * @param biFunction the BiFunction to be partially applied and executed within the synchronized block
         * @return a Throwables.Function that synchronizes on the mutex before executing the partially applied BiFunction
         * @throws IllegalArgumentException if mutex or biFunction is null
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
         * Returns a synchronized BiFunction that executes the provided BiFunction within a synchronized block.
         * All calls to the returned BiFunction's apply method will be synchronized on the specified mutex object.
         *
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that the function may throw
         * @param mutex the object to synchronize on
         * @param biFunction the BiFunction to be executed within the synchronized block
         * @return a Throwables.BiFunction that synchronizes on the mutex before executing the BiFunction
         * @throws IllegalArgumentException if mutex or biFunction is null
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
         * Converts a Consumer to a Function that returns null after executing the consumer.
         * The returned function will execute the consumer's accept method on its input
         * and then return null.
         *
         * @param <T> the type of the input to the consumer/function
         * @param <E> the type of the checked exception that may be thrown
         * @param consumer the consumer to convert to a function
         * @return a Throwables.Function that executes the consumer and returns null
         * @throws IllegalArgumentException if consumer is null
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Function<T, Void, E> c2f(final Throwables.Consumer<T, E> consumer) throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return t -> {
                consumer.accept(t);

                return null;
            };
        }

        /**
         * Converts a Consumer to a Function that returns a specified value after executing the consumer.
         * The returned function will execute the consumer's accept method on its input
         * and then return the specified value.
         *
         * @param <T> the type of the input to the consumer/function
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that may be thrown
         * @param consumer the consumer to convert to a function
         * @param valueToReturn the value to return after executing the consumer
         * @return a Throwables.Function that executes the consumer and returns the specified value
         * @throws IllegalArgumentException if consumer is null
         */
        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> c2f(final Throwables.Consumer<T, E> consumer, final R valueToReturn)
                throws IllegalArgumentException {
            N.checkArgNotNull(consumer);

            return t -> {
                consumer.accept(t);

                return valueToReturn;
            };
        }

        /**
         * Converts a BiConsumer to a BiFunction that returns null after executing the consumer.
         * The returned function will execute the consumer's accept method on its inputs
         * and then return null.
         *
         * @param <T> the type of the first argument to the consumer/function
         * @param <U> the type of the second argument to the consumer/function
         * @param <E> the type of the checked exception that may be thrown
         * @param biConsumer the BiConsumer to convert to a BiFunction
         * @return a Throwables.BiFunction that executes the BiConsumer and returns null
         * @throws IllegalArgumentException if biConsumer is null
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiFunction<T, U, Void, E> c2f(final Throwables.BiConsumer<T, U, E> biConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return (t, u) -> {
                biConsumer.accept(t, u);

                return null;
            };
        }

        /**
         * Converts a BiConsumer to a BiFunction that returns a specified value after executing the consumer.
         * The returned function will execute the consumer's accept method on its inputs
         * and then return the specified value.
         *
         * @param <T> the type of the first argument to the consumer/function
         * @param <U> the type of the second argument to the consumer/function
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that may be thrown
         * @param biConsumer the BiConsumer to convert to a BiFunction
         * @param valueToReturn the value to return after executing the BiConsumer
         * @return a Throwables.BiFunction that executes the BiConsumer and returns the specified value
         * @throws IllegalArgumentException if biConsumer is null
         */
        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> c2f(final Throwables.BiConsumer<T, U, E> biConsumer,
                final R valueToReturn) throws IllegalArgumentException {
            N.checkArgNotNull(biConsumer);

            return (t, u) -> {
                biConsumer.accept(t, u);

                return valueToReturn;
            };
        }

        /**
         * Converts a TriConsumer to a TriFunction that returns null after executing the consumer.
         * The returned function will execute the consumer's accept method on its inputs
         * and then return null.
         *
         * @param <A> the type of the first argument to the consumer/function
         * @param <B> the type of the second argument to the consumer/function
         * @param <C> the type of the third argument to the consumer/function
         * @param <E> the type of the checked exception that may be thrown
         * @param triConsumer the TriConsumer to convert to a TriFunction
         * @return a Throwables.TriFunction that executes the TriConsumer and returns null
         * @throws IllegalArgumentException if triConsumer is null
         */
        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriFunction<A, B, C, Void, E> c2f(final Throwables.TriConsumer<A, B, C, E> triConsumer)
                throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (a, b, c) -> {
                triConsumer.accept(a, b, c);

                return null;
            };
        }

        /**
         * Converts a TriConsumer to a TriFunction that returns a specified value after executing the consumer.
         * The returned function will execute the consumer's accept method on its inputs
         * and then return the specified value.
         *
         * @param <A> the type of the first argument to the consumer/function
         * @param <B> the type of the second argument to the consumer/function
         * @param <C> the type of the third argument to the consumer/function
         * @param <R> the type of the result of the function
         * @param <E> the type of the checked exception that may be thrown
         * @param triConsumer the TriConsumer to convert to a TriFunction
         * @param valueToReturn the value to return after executing the TriConsumer
         * @return a Throwables.TriFunction that executes the TriConsumer and returns the specified value
         * @throws IllegalArgumentException if triConsumer is null
         */
        @Beta
        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> c2f(final Throwables.TriConsumer<A, B, C, E> triConsumer,
                final R valueToReturn) throws IllegalArgumentException {
            N.checkArgNotNull(triConsumer);

            return (a, b, c) -> {
                triConsumer.accept(a, b, c);

                return valueToReturn;
            };
        }

        /**
         * Converts a Function to a Consumer that ignores the function's return value.
         * The returned consumer will execute the function's apply method on its input
         * and discard the result.
         *
         * @param <T> the type of the input to the function/consumer
         * @param <R> the type of the result of the function (ignored)
         * @param <E> the type of the checked exception that may be thrown
         * @param func the function to convert to a consumer
         * @return a Throwables.Consumer that executes the function and ignores its result
         * @throws IllegalArgumentException if func is null
         */
        @Beta
        public static <T, R, E extends Throwable> Throwables.Consumer<T, E> f2c(final Throwables.Function<T, ? extends R, E> func)
                throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return func::apply;
        }

        /**
         * Converts a BiFunction to a BiConsumer that ignores the function's return value.
         * The returned consumer will execute the function's apply method on its inputs
         * and discard the result.
         *
         * @param <T> the type of the first argument to the function/consumer
         * @param <U> the type of the second argument to the function/consumer
         * @param <R> the type of the result of the function (ignored)
         * @param <E> the type of the checked exception that may be thrown
         * @param func the BiFunction to convert to a BiConsumer
         * @return a Throwables.BiConsumer that executes the BiFunction and ignores its result
         * @throws IllegalArgumentException if func is null
         */
        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiConsumer<T, U, E> f2c(final Throwables.BiFunction<T, U, ? extends R, E> func)
                throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return func::apply;
        }

        /**
         * Converts a TriFunction to a TriConsumer that ignores the function's return value.
         * The returned consumer will execute the function's apply method on its inputs
         * and discard the result.
         *
         * @param <A> the type of the first argument to the function/consumer
         * @param <B> the type of the second argument to the function/consumer
         * @param <C> the type of the third argument to the function/consumer
         * @param <R> the type of the result of the function (ignored)
         * @param <E> the type of the checked exception that may be thrown
         * @param func the TriFunction to convert to a TriConsumer
         * @return a Throwables.TriConsumer that executes the TriFunction and ignores its result
         * @throws IllegalArgumentException if func is null
         */
        @Beta
        public static <A, B, C, R, E extends Throwable> Throwables.TriConsumer<A, B, C, E> f2c(final Throwables.TriFunction<A, B, C, ? extends R, E> func)
                throws IllegalArgumentException {
            N.checkArgNotNull(func);

            return func::apply;
        }

        /**
         * Returns the provided Runnable as a Throwables.Runnable.
         * This is an identity function that simply returns the input runnable.
         *
         * @param <E> the type of the checked exception that the runnable may throw
         * @param runnable the runnable to return
         * @return the same Throwables.Runnable that was provided
         * @throws IllegalArgumentException if runnable is null
         */
        public static <E extends Throwable> Throwables.Runnable<E> r(final Throwables.Runnable<E> runnable) throws IllegalArgumentException {
            N.checkArgNotNull(runnable);

            return runnable;
        }

        /**
         * Returns the provided Callable as a Throwables.Callable.
         * This is an identity function that simply returns the input callable.
         *
         * @param <R> the type of the result of the callable
         * @param <E> the type of the checked exception that the callable may throw
         * @param callable the callable to return
         * @return the same Throwables.Callable that was provided
         * @throws IllegalArgumentException if callable is null
         */
        public static <R, E extends Throwable> Throwables.Callable<R, E> c(final Throwables.Callable<R, E> callable) throws IllegalArgumentException {
            N.checkArgNotNull(callable);

            return callable;
        }

        /**
         * Converts a Runnable to a Callable that returns null after executing the runnable.
         * The returned callable will execute the runnable's run method and then return null.
         *
         * @param <E> the type of the checked exception that may be thrown
         * @param runnable the runnable to convert to a callable
         * @return a Throwables.Callable that executes the runnable and returns null
         * @throws IllegalArgumentException if runnable is null
         */
        public static <E extends Throwable> Throwables.Callable<Void, E> r2c(final Throwables.Runnable<E> runnable) throws IllegalArgumentException {
            N.checkArgNotNull(runnable);

            return () -> {
                runnable.run();
                return null;
            };
        }

        /**
         * Converts a Runnable to a Callable that returns a specified value after executing the runnable.
         * The returned callable will execute the runnable's run method and then return the specified value.
         *
         * @param <R> the type of the result of the callable
         * @param <E> the type of the checked exception that may be thrown
         * @param runnable the runnable to convert to a callable
         * @param valueToReturn the value to return after executing the runnable
         * @return a Throwables.Callable that executes the runnable and returns the specified value
         * @throws IllegalArgumentException if runnable is null
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
         * Converts a Callable to a Runnable that ignores the callable's return value.
         * The returned runnable will execute the callable's call method and discard the result.
         *
         * @param <R> the type of the result of the callable (ignored)
         * @param <E> the type of the checked exception that may be thrown
         * @param callable the callable to convert to a runnable
         * @return a Throwables.Runnable that executes the callable and ignores its result
         * @throws IllegalArgumentException if callable is null
         */
        public static <R, E extends Throwable> Throwables.Runnable<E> c2r(final Throwables.Callable<R, E> callable) throws IllegalArgumentException {
            N.checkArgNotNull(callable);

            return callable::call;
        }

        /**
         * Casts a standard java.lang.Runnable to a Throwables.Runnable that can throw checked exceptions.
         * This method performs an unchecked cast and should be used with caution.
         *
         * @param <E> the type of the checked exception that the returned runnable may throw
         * @param runnable the standard Runnable to cast
         * @return a Throwables.Runnable that wraps the provided Runnable
         */
        public static <E extends Throwable> Throwables.Runnable<E> rr(final Runnable runnable) {
            return (Throwables.Runnable<E>) runnable;
        }

        /**
         * Casts a standard java.util.concurrent.Callable to a Throwables.Callable that can throw checked exceptions.
         * This method performs an unchecked cast and should be used with caution.
         *
         * @param <R> the type of the result of the callable
         * @param <E> the type of the checked exception that the returned callable may throw
         * @param callable the standard Callable to cast
         * @return a Throwables.Callable that wraps the provided Callable
         */
        public static <R, E extends Throwable> Throwables.Callable<R, E> cc(final Callable<R> callable) {
            return (Throwables.Callable<R, E>) callable;
        }

        /**
         * Converts a standard java.lang.Runnable to a Throwables.Runnable that can throw checked exceptions.
         * If the input is already a Throwables.Runnable, it is returned as-is.
         * Otherwise, a new Throwables.Runnable is created that wraps the standard Runnable.
         *
         * @param <E> the type of the checked exception that the returned runnable may throw
         * @param runnable the standard Runnable to convert
         * @return a Throwables.Runnable that wraps the provided Runnable
         * @throws IllegalArgumentException if runnable is null
         */
        public static <E extends Throwable> Throwables.Runnable<E> jr2r(final java.lang.Runnable runnable) throws IllegalArgumentException {
            N.checkArgNotNull(runnable);

            if (runnable instanceof Throwables.Runnable) {
                return (Throwables.Runnable<E>) runnable;
            }

            return runnable::run;
        }

        /**
         * Converts a Throwables.Runnable to a standard java.lang.Runnable.
         * If the input is already a java.lang.Runnable, it is returned as-is.
         * Otherwise, a new java.lang.Runnable is created that wraps the Throwables.Runnable
         * and converts any checked exceptions to runtime exceptions.
         *
         * @param <E> the type of the checked exception that the input runnable may throw
         * @param runnable the Throwables.Runnable to convert
         * @return a standard java.lang.Runnable that wraps the provided Throwables.Runnable
         * @throws IllegalArgumentException if runnable is null
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
         * Converts a standard java.util.concurrent.Callable to a Throwables.Callable that can throw checked exceptions.
         * If the input is already a Throwables.Callable, it is returned as-is.
         * Otherwise, a new Throwables.Callable is created that wraps the standard Callable.
         *
         * @param <R> the type of the result of the callable
         * @param callable the standard Callable to convert
         * @return a Throwables.Callable that wraps the provided Callable
         * @throws IllegalArgumentException if callable is null
         */
        public static <R> Throwables.Callable<R, Exception> jc2c(final java.util.concurrent.Callable<R> callable) throws IllegalArgumentException {
            N.checkArgNotNull(callable);

            if (callable instanceof Throwables.Callable) {
                return (Throwables.Callable<R, Exception>) callable;
            }

            return callable::call;
        }

        /**
         * Converts a Throwables.Callable to a standard java.util.concurrent.Callable.
         * If the input is already a java.util.concurrent.Callable, it is returned as-is.
         * Otherwise, a new java.util.concurrent.Callable is created that wraps the Throwables.Callable.
         *
         * @param <R> the type of the result of the callable
         * @param callable the Throwables.Callable to convert
         * @return a standard java.util.concurrent.Callable that wraps the provided Throwables.Callable
         * @throws IllegalArgumentException if callable is null
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
