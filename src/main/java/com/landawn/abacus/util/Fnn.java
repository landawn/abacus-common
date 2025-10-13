/*
 * Copyright (c) 2025, Haiyang Li.
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

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Fn.BinaryOperators;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Runnable;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.stream.Stream;

/**
 * Utility class for exceptional Predicate/Function/Consumer operations.
 * 
 * <p>This class provides static methods for creating and manipulating {@code Throwables} functional interfaces
 * that can throw checked exceptions. It serves as a comprehensive toolkit for working with exception-throwing
 * functional programming constructs, offering utilities for memorization, conversion, synchronization, and
 * common functional operations.</p>
 *
 * <p>The class extends the functionality of standard Java functional interfaces by providing versions that
 * can throw checked exceptions, along with utilities to convert between standard and throwable variants.
 * It also includes specialized functions for common operations like null checking, string conversion,
 * map entry manipulation, and rate limiting.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Memorization utilities for caching supplier results with optional expiration</li>
 *   <li>Standard functional interfaces (identity, always true/false predicates)</li>
 *   <li>Conversion utilities between different functional interface types</li>
 *   <li>Synchronization wrappers for thread-safe functional operations</li>
 *   <li>Specialized predicates for null/empty checking</li>
 *   <li>Map entry manipulation functions (key, value, inverse)</li>
 *   <li>Tuple and pair creation utilities</li>
 *   <li>Rate limiting and sleep utilities</li>
 *   <li>Exception throwing utilities</li>
 *   <li>Conversion between standard Java and {@code Throwables} functional interfaces</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Memoized supplier with expiration
 * Throwables.Supplier<String, IOException> supplier = Fnn.memoizeWithExpiration(
 *     () -> expensiveOperation(), 5, TimeUnit.MINUTES);
 *
 * // Synchronized function
 * Object mutex = new Object();
 * Throwables.Function<String, String, Exception> syncFunc = 
 *     Fnn.sf(mutex, str -> processString(str));
 *
 * // Convert consumer to function
 * Throwables.Function<String, Void, Exception> func = 
 *     Fnn.c2f(str -> System.out.println(str));
 *
 * // Null-safe predicate
 * Throwables.Predicate<String, Exception> notNull = Fnn.notNull();
 *
 * // Rate-limited consumer
 * Throwables.Consumer<String, Exception> rateLimited = 
 *     Fnn.rateLimiter(10.0); // 10 permits per second
 *
 * // Map entry utilities
 * Throwables.Function<Map.Entry<String, Integer>, String, Exception> keyExtractor = 
 *     Fnn.key();
 * }</pre>
 *
 * <p>The class uses the "Fn" abbreviation followed by "n" to indicate it works with functions that can throw
 * exceptions (nullable/throwable functions). Many methods are marked with {@code @Beta} to indicate they
 * are experimental or subject to change.</p>
 *
 * <p><b>Thread Safety:</b> All static methods in this class are thread-safe. Individual returned functional
 * interfaces may or may not be thread-safe depending on their implementation and whether they are wrapped
 * with synchronization utilities.</p>
 *
 * @since 1.0
 * @see Throwables
 * @see java.util.function
 * @see Fn
 */
public final class Fnn {
    private Fnn() {
        // Singleton for utility class
    }

    /**
     * Returns a Supplier which returns a single instance created by calling the specified supplier.get().
     * The instance is created lazily on the first call and cached for subsequent calls.
     *
     * @param <T> the type of results supplied by this supplier
     * @param <E> the type of exception that may be thrown
     * @param supplier the supplier to memorize
     * @return a memorized version of the supplier that caches the result
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
            private final R none = (R) Fn.NONE;
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
     * Returns a Throwables.Predicate that always returns {@code true} regardless of input.
     *
     * @param <T> the type of the input to the predicate
     * @param <E> the type of exception that may be thrown
     * @return a predicate that always returns true
     */
    public static <T, E extends Exception> Throwables.Predicate<T, E> alwaysTrue() {
        return Fn.ALWAYS_TRUE;
    }

    /**
     * Returns a Throwables.Predicate that always returns {@code false} regardless of input.
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
        return Fn.TO_STRING;
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
     * @return a Predicate that returns {@code true} if the input is null
     */
    @Beta
    public static <T, E extends Exception> Throwables.Predicate<T, E> isNull() {
        return Fn.IS_NULL;
    }

    /**
     * Returns a Throwables.Predicate that tests if a CharSequence is empty.
     * The predicate returns {@code true} if the CharSequence has length 0.
     *
     * @param <T> the type of CharSequence
     * @param <E> the type of exception that may be thrown
     * @return a Predicate that returns {@code true} if the CharSequence is empty
     */
    public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> isEmpty() {
        return (Throwables.Predicate<T, E>) Fn.IS_EMPTY;
    }

    /**
     * Returns a Throwables.Predicate that tests if a CharSequence is blank.
     * The predicate returns {@code true} if the CharSequence is empty or contains only whitespace characters.
     *
     * @param <T> the type of CharSequence
     * @param <E> the type of exception that may be thrown
     * @return a Predicate that returns {@code true} if the CharSequence is blank
     */
    public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> isBlank() {
        return (Throwables.Predicate<T, E>) Fn.IS_BLANK;
    }

    /**
     * Returns a Throwables.Predicate that tests if an array is empty.
     * The predicate returns {@code true} if the array is null or has length 0.
     * This method is marked as Beta.
     *
     * @param <T> the component type of the array
     * @param <E> the type of exception that may be thrown
     * @return a Predicate that returns {@code true} if the array is empty
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T, E extends Exception> Throwables.Predicate<T[], E> isEmptyA() {
        return (Throwables.Predicate) Fn.IS_EMPTY_A;
    }

    /**
     * Returns a Throwables.Predicate that tests if a Collection is empty.
     * The predicate returns {@code true} if the Collection is null or has size 0.
     * This method is marked as Beta.
     *
     * @param <T> the type of Collection
     * @param <E> the type of exception that may be thrown
     * @return a Predicate that returns {@code true} if the Collection is empty
     */
    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Collection, E extends Exception> Throwables.Predicate<T, E> isEmptyC() {
        return (Throwables.Predicate<T, E>) Fn.IS_EMPTY_C;
    }

    /**
     * Returns a Throwables.Predicate that tests if a Map is empty.
     * The predicate returns {@code true} if the Map is null or has size 0.
     * This method is marked as Beta.
     *
     * @param <T> the type of Map
     * @param <E> the type of exception that may be thrown
     * @return a Predicate that returns {@code true} if the Map is empty
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
     * @return a Predicate that returns {@code true} if the input is not null, {@code false} otherwise
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
     * @return a Predicate that returns {@code true} if the input CharSequence is not empty, {@code false} otherwise
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
     * @return a Predicate that returns {@code true} if the input CharSequence is not blank, {@code false} otherwise
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
     * @return a Predicate that returns {@code true} if the input array is not empty, {@code false} otherwise
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
     * @return a Predicate that returns {@code true} if the input Collection is not empty, {@code false} otherwise
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
     * @return a Predicate that returns {@code true} if the input Map is not empty, {@code false} otherwise
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
     * @see java.util.stream.Collectors#toMap(java.util.function.Function, java.util.function.Function, java.util.function.BinaryOperator, Supplier)
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
     * @see java.util.stream.Collectors#toMap(java.util.function.Function, java.util.function.Function, java.util.function.BinaryOperator, Supplier)
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
     * @see java.util.stream.Collectors#toMap(java.util.function.Function, java.util.function.Function, java.util.function.BinaryOperator, Supplier)
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
    private static final Throwables.BinaryOperator<Map.Entry<Comparable, Object>, Throwable> MIN_BY_KEY = (t, u) -> N.compare(t.getKey(), u.getKey()) <= 0 ? t
            : u;

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
    private static final Throwables.BinaryOperator<Map.Entry<Comparable, Object>, Throwable> MAX_BY_KEY = (t, u) -> N.compare(t.getKey(), u.getKey()) >= 0 ? t
            : u;

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
     * When evaluated, the returned predicate returns {@code true} if the given predicate returns false, and vice versa.
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
     * When evaluated, the returned bi-predicate returns {@code true} if the given bi-predicate returns false, and vice versa.
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
     * When evaluated, the returned tri-predicate returns {@code true} if the given tri-predicate returns false, and vice versa.
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
     * Returns a stateful Predicate that returns {@code true} for at most the specified number of evaluations.
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
    public static <A, B, C, E extends Throwable> Throwables.TriConsumer<A, B, C, E> cc(final TriConsumer<A, B, C> triConsumer) throws IllegalArgumentException {
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
    public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> ff(final BiFunction<T, U, R> biFunction) throws IllegalArgumentException {
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
    public static <A, T, E extends Throwable> Throwables.Predicate<T, E> sp(final Object mutex, final A a, final Throwables.BiPredicate<A, T, E> biPredicate)
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
    public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> sf(final Object mutex, final Throwables.BiFunction<T, U, R, E> biFunction)
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
    public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> c2f(final Throwables.BiConsumer<T, U, E> biConsumer, final R valueToReturn)
            throws IllegalArgumentException {
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
    public static <T, R, E extends Throwable> Throwables.Consumer<T, E> f2c(final Throwables.Function<T, ? extends R, E> func) throws IllegalArgumentException {
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
