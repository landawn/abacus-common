/*
 * Copyright (C) 2017 HaiYang Li
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

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.Tuple.Tuple5;
import com.landawn.abacus.util.Tuple.Tuple6;
import com.landawn.abacus.util.Tuple.Tuple7;

/**
 * The Futures utility class provides comprehensive methods for working with Future objects in concurrent programming.
 * It offers functionality for composing, combining, and managing multiple Future objects, including:
 * <ul>
 *   <li>Composing multiple futures with custom zip functions</li>
 *   <li>Combining futures into Tuples for easy access to multiple results</li>
 *   <li>Creating futures that complete when all input futures complete (allOf)</li>
 *   <li>Creating futures that complete when any input future completes (anyOf)</li>
 *   <li>Iterating over futures as they complete (first-finished, first-out)</li>
 * </ul>
 * 
 * <p>This class is particularly useful when dealing with concurrent operations where you need to:
 * <ul>
 *   <li>Wait for multiple asynchronous operations to complete</li>
 *   <li>Process results as soon as any operation completes</li>
 *   <li>Transform or combine results from multiple futures</li>
 *   <li>Handle timeouts and cancellations across multiple futures</li>
 * </ul>
 * 
 * <p>Example usage:
 * <pre>{@code
 * // Combine two futures into a tuple
 * Future<Integer> future1 = CompletableFuture.completedFuture(10);
 * Future<String> future2 = CompletableFuture.completedFuture("Hello");
 * ContinuableFuture<Tuple2<Integer, String>> combined = Futures.combine(future1, future2);
 * Tuple2<Integer, String> result = combined.get(); // (10, "Hello")
 * 
 * // Wait for all futures to complete
 * List<Future<Integer>> futures = Arrays.asList(future1, future2, future3);
 * ContinuableFuture<List<Integer>> allResults = Futures.allOf(futures);
 * 
 * // Get the first completed result
 * ContinuableFuture<Integer> firstCompleted = Futures.anyOf(future1, future2, future3);
 * }</pre>
 *
 * @see ContinuableFuture
 * @see CompletableFuture
 * @see ExecutorCompletionService
 */
public final class Futures {

    private Futures() {
        // singleton.
    }

    /**
     * Composes two futures into a new ContinuableFuture by applying a zip function to the Future objects themselves.
     * This method allows you to create custom logic that operates on the Future objects directly, enabling
     * advanced composition patterns. The zip function receives the Future objects and can call get() on them
     * to retrieve their values.
     * 
     * <p>This overload uses the same function for both regular get() and timeout-based get() operations.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<Integer> future1 = CompletableFuture.completedFuture(5);
     * Future<Integer> future2 = CompletableFuture.completedFuture(10);
     * 
     * ContinuableFuture<Integer> sum = Futures.compose(future1, future2, 
     *     (f1, f2) -> f1.get() + f2.get());
     * 
     * System.out.println(sum.get()); // Prints: 15
     * }</pre>
     *
     * @param <T1> The result type of the first input future
     * @param <T2> The result type of the second input future
     * @param <R> The result type of the composed future
     * @param cf1 The first input future
     * @param cf2 The second input future
     * @param zipFunctionForGet The function that combines the futures' results. It receives the Future objects
     *                          and should call get() on them to retrieve values
     * @return A new ContinuableFuture that completes when both input futures complete, with a result 
     *         computed by the provided zip function
     * @throws RuntimeException if the zip function throws an exception other than InterruptedException or ExecutionException
     */
    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Throwables.BiFunction<? super Future<T1>, ? super Future<T2>, ? extends R, Exception> zipFunctionForGet) {
        return compose(cf1, cf2, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1, t._2));
    }

    /**
     * Composes two futures into a new ContinuableFuture with separate functions for regular and timeout-based operations.
     * This method provides maximum flexibility by allowing different logic for get() and get(timeout, unit) calls.
     * The timeout function receives a Tuple4 containing both futures, the timeout value, and the time unit.
     * 
     * <p>This is useful when you need different behavior for time-constrained operations, such as returning
     * a default value or using a different computation strategy when under time pressure.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<String> slowFuture = CompletableFuture.supplyAsync(() -> {
     *     Thread.sleep(5000);
     *     return "Slow Result";
     * });
     * Future<String> fastFuture = CompletableFuture.completedFuture("Fast Result");
     * 
     * ContinuableFuture<String> composed = Futures.compose(slowFuture, fastFuture,
     *     (f1, f2) -> f1.get() + " + " + f2.get(),
     *     tuple -> {
     *         // For timeout, just use the fast future
     *         return "Timeout: " + tuple._2.get(tuple._3, tuple._4);
     *     });
     * 
     * // Will return quickly with timeout logic
     * String result = composed.get(100, TimeUnit.MILLISECONDS);
     * }</pre>
     *
     * @param <T1> The result type of the first input future
     * @param <T2> The result type of the second input future
     * @param <R> The result type of the composed future
     * @param cf1 The first input future
     * @param cf2 The second input future
     * @param zipFunctionForGet The function for regular get() operations
     * @param zipFunctionTimeoutGet The function for get(timeout, unit) operations. Receives a Tuple4 with
     *                              (future1, future2, timeout, timeUnit)
     * @return A new ContinuableFuture with custom logic for both regular and timeout operations
     */
    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Throwables.BiFunction<? super Future<T1>, ? super Future<T2>, ? extends R, Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple4<Future<T1>, Future<T2>, Long, TimeUnit>, R, Exception> zipFunctionTimeoutGet) {
        final List<Future<?>> cfs = Arrays.asList(cf1, cf2);

        return compose(cfs, c -> zipFunctionForGet.apply((Future<T1>) c.get(0), (Future<T2>) c.get(1)),
                t -> zipFunctionTimeoutGet.apply(Tuple.of((Future<T1>) t._1.get(0), (Future<T2>) t._1.get(1), t._2, t._3)));
    }

    /**
     * Composes three futures into a new ContinuableFuture by applying a tri-function to the Future objects.
     * This method extends the composition pattern to three futures, allowing complex three-way combinations.
     * The function receives all three Future objects and can orchestrate their completion as needed.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<String> nameFuture = CompletableFuture.completedFuture("John");
     * Future<Integer> ageFuture = CompletableFuture.completedFuture(30);
     * Future<String> cityFuture = CompletableFuture.completedFuture("New York");
     * 
     * ContinuableFuture<String> profile = Futures.compose(nameFuture, ageFuture, cityFuture,
     *     (f1, f2, f3) -> String.format("%s, %d years old, from %s", 
     *         f1.get(), f2.get(), f3.get()));
     * 
     * System.out.println(profile.get()); // "John, 30 years old, from New York"
     * }</pre>
     *
     * @param <T1> The result type of the first input future
     * @param <T2> The result type of the second input future
     * @param <T3> The result type of the third input future
     * @param <R> The result type of the composed future
     * @param cf1 The first input future
     * @param cf2 The second input future
     * @param cf3 The third input future
     * @param zipFunctionForGet The function that combines the three futures' results
     * @return A new ContinuableFuture that completes when all three input futures complete
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Throwables.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, ? extends R, Exception> zipFunctionForGet) {
        return compose(cf1, cf2, cf3, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1, t._2, t._3));
    }

    /**
     * Composes three futures with separate functions for regular and timeout-based operations.
     * Similar to the two-future version, this provides different logic paths for time-constrained scenarios.
     * The timeout function receives a Tuple5 containing all three futures plus timeout information.
     * 
     * <p>This is particularly useful for complex operations where you might want to skip expensive
     * computations or use cached/default values when operating under time constraints.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<List<String>> dbQuery = // ... database query future
     * Future<Map<String, Object>> cache = // ... cache lookup future  
     * Future<String> config = // ... configuration future
     * 
     * ContinuableFuture<Result> composed = Futures.compose(dbQuery, cache, config,
     *     (f1, f2, f3) -> processAllData(f1.get(), f2.get(), f3.get()),
     *     tuple -> {
     *         // Under time pressure, just use cache
     *         try {
     *             return processCacheOnly(tuple._2.get(50, TimeUnit.MILLISECONDS));
     *         } catch (TimeoutException e) {
     *             return Result.DEFAULT;
     *         }
     *     });
     * }</pre>
     *
     * @param <T1> The result type of the first input future
     * @param <T2> The result type of the second input future
     * @param <T3> The result type of the third input future
     * @param <R> The result type of the composed future
     * @param cf1 The first input future
     * @param cf2 The second input future
     * @param cf3 The third input future
     * @param zipFunctionForGet The function for regular get() operations
     * @param zipFunctionTimeoutGet The function for get(timeout, unit) operations. Receives a Tuple5 with
     *                              (future1, future2, future3, timeout, timeUnit)
     * @return A new ContinuableFuture with custom logic for both regular and timeout operations
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Throwables.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, ? extends R, Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple5<Future<T1>, Future<T2>, Future<T3>, Long, TimeUnit>, R, Exception> zipFunctionTimeoutGet) {
        final List<Future<?>> cfs = Arrays.asList(cf1, cf2, cf3);

        return compose(cfs, c -> zipFunctionForGet.apply((Future<T1>) c.get(0), (Future<T2>) c.get(1), (Future<T3>) c.get(2)),
                t -> zipFunctionTimeoutGet.apply(Tuple.of((Future<T1>) t._1.get(0), (Future<T2>) t._1.get(1), (Future<T3>) t._1.get(2), t._2, t._3)));
    }

    /**
     * Composes a collection of futures into a single ContinuableFuture using a custom function.
     * This method provides maximum flexibility for combining any number of futures. The function
     * receives the entire collection and can implement any logic for combining their results.
     * 
     * <p>The collection type is preserved (FC extends Collection), allowing type-safe operations
     * on specific collection implementations.
     * 
     * <p>Example usage:
     * <pre>{@code
     * List<Future<Integer>> futures = Arrays.asList(
     *     CompletableFuture.completedFuture(1),
     *     CompletableFuture.completedFuture(2),
     *     CompletableFuture.completedFuture(3)
     * );
     * 
     * ContinuableFuture<Integer> sum = Futures.compose(futures, list -> {
     *     int total = 0;
     *     for (Future<Integer> f : list) {
     *         total += f.get();
     *     }
     *     return total;
     * });
     * 
     * System.out.println(sum.get()); // Prints: 6
     * }</pre>
     *
     * @param <T> The result type of the input futures
     * @param <FC> The specific collection type containing the futures
     * @param <R> The result type of the composed future
     * @param cfs The collection of input futures. Must not be null or empty
     * @param zipFunctionForGet The function that processes the collection of futures
     * @return A new ContinuableFuture that completes when all input futures complete
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <T, FC extends Collection<? extends Future<? extends T>>, R> ContinuableFuture<R> compose(final FC cfs,
            final Throwables.Function<? super FC, ? extends R, Exception> zipFunctionForGet) {
        return compose(cfs, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1));
    }

    /**
     * Composes a collection of futures with separate functions for regular and timeout operations.
     * This is the most flexible composition method, supporting any number of futures with custom
     * timeout handling. The timeout function receives a Tuple3 containing the collection, timeout value,
     * and time unit.
     * 
     * <p>This method is ideal for scenarios where you need to aggregate results from many sources
     * but want different behavior when operating under time constraints.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Set<Future<DataPoint>> dataFutures = // ... multiple data source futures
     * 
     * ContinuableFuture<Summary> summary = Futures.compose(dataFutures,
     *     futures -> {
     *         // Full aggregation when we have time
     *         List<DataPoint> allData = new ArrayList<>();
     *         for (Future<DataPoint> f : futures) {
     *             allData.add(f.get());
     *         }
     *         return computeFullSummary(allData);
     *     },
     *     tuple -> {
     *         // Quick summary using only completed futures
     *         List<DataPoint> available = new ArrayList<>();
     *         for (Future<DataPoint> f : tuple._1) {
     *             if (f.isDone()) {
     *                 try {
     *                     available.add(f.get());
     *                 } catch (Exception e) {
     *                     // Skip failed futures
     *                 }
     *             }
     *         }
     *         return computeQuickSummary(available);
     *     });
     * }</pre>
     *
     * @param <T> The result type of the input futures
     * @param <FC> The specific collection type containing the futures
     * @param <R> The result type of the composed future
     * @param cfs The collection of input futures. Must not be null or empty
     * @param zipFunctionForGet The function for regular get() operations
     * @param zipFunctionTimeoutGet The function for get(timeout, unit) operations. Receives a Tuple3 with
     *                              (futures collection, timeout, timeUnit)
     * @return A new ContinuableFuture with custom logic for both regular and timeout operations
     * @throws IllegalArgumentException if the collection is null or empty, or if either function is null
     */
    public static <T, FC extends Collection<? extends Future<? extends T>>, R> ContinuableFuture<R> compose(final FC cfs,
            final Throwables.Function<? super FC, ? extends R, Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple3<FC, Long, TimeUnit>, ? extends R, Exception> zipFunctionTimeoutGet) throws IllegalArgumentException {
        N.checkArgument(N.notEmpty(cfs), "'cfs' can't be null or empty"); //NOSONAR
        N.checkArgNotNull(zipFunctionForGet);
        N.checkArgNotNull(zipFunctionTimeoutGet);

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            exception.addSuppressed(e);
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                return res;
            }

            @Override
            public boolean isCancelled() {
                for (final Future<?> future : cfs) {
                    if (future.isCancelled()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean isDone() {
                for (final Future<?> future : cfs) {
                    if (!future.isDone()) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public R get() throws InterruptedException, ExecutionException {
                try {
                    return zipFunctionForGet.apply(cfs);
                } catch (InterruptedException | ExecutionException e) {
                    throw e;
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }

            @Override
            public R get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final Tuple3<FC, Long, TimeUnit> t = Tuple.of(cfs, timeout, unit);

                try {
                    return zipFunctionTimeoutGet.apply(t);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    throw e;
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        });
    }

    /**
     * Combines two futures into a Tuple2 containing both results.
     * This is a convenience method that waits for both futures to complete and packages their results
     * into a tuple for easy access. The resulting tuple preserves the order of the input futures.
     * 
     * <p>This method is particularly useful when you need both results but don't want to transform
     * them immediately, or when passing multiple results to another function.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<User> userFuture = fetchUser(userId);
     * Future<List<Order>> ordersFuture = fetchOrders(userId);
     * 
     * ContinuableFuture<Tuple2<User, List<Order>>> combined = 
     *     Futures.combine(userFuture, ordersFuture);
     * 
     * combined.thenAccept(tuple -> {
     *     User user = tuple._1;
     *     List<Order> orders = tuple._2;
     *     displayUserProfile(user, orders);
     * });
     * }</pre>
     *
     * @param <T1> The result type of the first future
     * @param <T2> The result type of the second future
     * @param cf1 The first future
     * @param cf2 The second future
     * @return A ContinuableFuture containing a Tuple2 with both results
     */
    public static <T1, T2> ContinuableFuture<Tuple2<T1, T2>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2) {
        return allOf(Arrays.asList(cf1, cf2)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1)));
    }

    /**
     * Combines three futures into a Tuple3 containing all three results.
     * Similar to the two-argument version, but for three futures. Results are packaged in order
     * into a Tuple3 for convenient access to all values.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<String> nameFuture = fetchName();
     * Future<Integer> ageFuture = fetchAge();
     * Future<Address> addressFuture = fetchAddress();
     * 
     * ContinuableFuture<Tuple3<String, Integer, Address>> profile = 
     *     Futures.combine(nameFuture, ageFuture, addressFuture);
     * 
     * Tuple3<String, Integer, Address> result = profile.get();
     * System.out.printf("%s, age %d, lives at %s%n", 
     *     result._1, result._2, result._3);
     * }</pre>
     *
     * @param <T1> The result type of the first future
     * @param <T2> The result type of the second future
     * @param <T3> The result type of the third future
     * @param cf1 The first future
     * @param cf2 The second future
     * @param cf3 The third future
     * @return A ContinuableFuture containing a Tuple3 with all three results
     */
    public static <T1, T2, T3> ContinuableFuture<Tuple3<T1, T2, T3>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Future<? extends T3> cf3) {
        return allOf(Arrays.asList(cf1, cf2, cf3)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2)));
    }

    /**
     * Combines four futures into a Tuple4 containing all four results.
     * Extends the pattern to four futures, useful for operations that need to coordinate
     * four independent asynchronous operations.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<Config> configFuture = loadConfig();
     * Future<Database> dbFuture = connectDatabase();
     * Future<Cache> cacheFuture = initCache();
     * Future<Logger> loggerFuture = setupLogger();
     * 
     * ContinuableFuture<Tuple4<Config, Database, Cache, Logger>> deps = 
     *     Futures.combine(configFuture, dbFuture, cacheFuture, loggerFuture);
     * 
     * deps.thenAccept(tuple -> {
     *     initializeApplication(tuple._1, tuple._2, tuple._3, tuple._4);
     * });
     * }</pre>
     *
     * @param <T1> The result type of the first future
     * @param <T2> The result type of the second future
     * @param <T3> The result type of the third future
     * @param <T4> The result type of the fourth future
     * @param cf1 The first future
     * @param cf2 The second future
     * @param cf3 The third future
     * @param cf4 The fourth future
     * @return A ContinuableFuture containing a Tuple4 with all four results
     */
    public static <T1, T2, T3, T4> ContinuableFuture<Tuple4<T1, T2, T3, T4>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Future<? extends T3> cf3, final Future<? extends T4> cf4) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3)));
    }

    /**
     * Combines five futures into a Tuple5 containing all five results.
     * Useful for coordinating five independent operations where you need all results
     * before proceeding.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Gathering data from multiple microservices
     * Future<UserInfo> userService = callUserService(id);
     * Future<OrderHistory> orderService = callOrderService(id);
     * Future<Preferences> prefService = callPreferenceService(id);
     * Future<Recommendations> recService = callRecommendationService(id);
     * Future<ActivityLog> logService = callActivityService(id);
     * 
     * ContinuableFuture<Tuple5<UserInfo, OrderHistory, Preferences, 
     *                          Recommendations, ActivityLog>> allData = 
     *     Futures.combine(userService, orderService, prefService, 
     *                     recService, logService);
     * 
     * allData.thenAccept(data -> renderDashboard(data));
     * }</pre>
     *
     * @param <T1> The result type of the first future
     * @param <T2> The result type of the second future
     * @param <T3> The result type of the third future
     * @param <T4> The result type of the fourth future
     * @param <T5> The result type of the fifth future
     * @param cf1 The first future
     * @param cf2 The second future
     * @param cf3 The third future
     * @param cf4 The fourth future
     * @param cf5 The fifth future
     * @return A ContinuableFuture containing a Tuple5 with all five results
     */
    public static <T1, T2, T3, T4, T5> ContinuableFuture<Tuple5<T1, T2, T3, T4, T5>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Future<? extends T3> cf3, final Future<? extends T4> cf4, final Future<? extends T5> cf5) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4)));
    }

    /**
     * Combines six futures into a Tuple6 containing all six results.
     * Supports coordination of six independent asynchronous operations, useful for complex
     * initialization or data gathering scenarios.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Initializing a complex system with multiple components
     * Future<NetworkConfig> network = setupNetwork();
     * Future<StorageSystem> storage = initStorage();
     * Future<SecurityContext> security = loadSecurity();
     * Future<MetricsCollector> metrics = startMetrics();
     * Future<EventBus> events = createEventBus();
     * Future<SchedulerService> scheduler = initScheduler();
     * 
     * ContinuableFuture<Tuple6<NetworkConfig, StorageSystem, SecurityContext,
     *                          MetricsCollector, EventBus, SchedulerService>> system = 
     *     Futures.combine(network, storage, security, metrics, events, scheduler);
     * 
     * system.thenAccept(components -> startApplication(components));
     * }</pre>
     *
     * @param <T1> The result type of the first future
     * @param <T2> The result type of the second future
     * @param <T3> The result type of the third future
     * @param <T4> The result type of the fourth future
     * @param <T5> The result type of the fifth future
     * @param <T6> The result type of the sixth future
     * @param cf1 The first future
     * @param cf2 The second future
     * @param cf3 The third future
     * @param cf4 The fourth future
     * @param cf5 The fifth future
     * @param cf6 The sixth future
     * @return A ContinuableFuture containing a Tuple6 with all six results
     */
    public static <T1, T2, T3, T4, T5, T6> ContinuableFuture<Tuple6<T1, T2, T3, T4, T5, T6>> combine(final Future<? extends T1> cf1,
            final Future<? extends T2> cf2, final Future<? extends T3> cf3, final Future<? extends T4> cf4, final Future<? extends T5> cf5,
            final Future<? extends T6> cf6) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5, cf6))
                .map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4), (T6) t.get(5)));
    }

    /**
     * Combines seven futures into a Tuple7 containing all seven results.
     * The maximum tuple size supported, useful for very complex coordination scenarios
     * where seven independent operations must complete before proceeding.
     * 
     * <p>For more than seven futures, use the collection-based methods or create nested tuples.
     * 
     * <p>Example usage:
     * <pre>{@code
     * // Complex data aggregation from multiple sources
     * Future<CustomerData> customer = fetchCustomerData();
     * Future<AccountInfo> account = fetchAccountInfo();
     * Future<TransactionHistory> transactions = fetchTransactions();
     * Future<CreditScore> credit = fetchCreditScore();
     * Future<RiskProfile> risk = calculateRisk();
     * Future<ComplianceStatus> compliance = checkCompliance();
     * Future<MarketingPrefs> marketing = getMarketingPrefs();
     * 
     * ContinuableFuture<Tuple7<CustomerData, AccountInfo, TransactionHistory,
     *                          CreditScore, RiskProfile, ComplianceStatus,
     *                          MarketingPrefs>> fullProfile = 
     *     Futures.combine(customer, account, transactions, credit, 
     *                     risk, compliance, marketing);
     * 
     * fullProfile.thenAccept(data -> generateComprehensiveReport(data));
     * }</pre>
     *
     * @param <T1> The result type of the first future
     * @param <T2> The result type of the second future
     * @param <T3> The result type of the third future
     * @param <T4> The result type of the fourth future
     * @param <T5> The result type of the fifth future
     * @param <T6> The result type of the sixth future
     * @param <T7> The result type of the seventh future
     * @param cf1 The first future
     * @param cf2 The second future
     * @param cf3 The third future
     * @param cf4 The fourth future
     * @param cf5 The fifth future
     * @param cf6 The sixth future
     * @param cf7 The seventh future
     * @return A ContinuableFuture containing a Tuple7 with all seven results
     */
    public static <T1, T2, T3, T4, T5, T6, T7> ContinuableFuture<Tuple7<T1, T2, T3, T4, T5, T6, T7>> combine(final Future<? extends T1> cf1,
            final Future<? extends T2> cf2, final Future<? extends T3> cf3, final Future<? extends T4> cf4, final Future<? extends T5> cf5,
            final Future<? extends T6> cf6, final Future<? extends T7> cf7) {
        return allOf(Arrays.asList(cf1, cf2, cf3, cf4, cf5, cf6, cf7))
                .map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1), (T3) t.get(2), (T4) t.get(3), (T5) t.get(4), (T6) t.get(5), (T7) t.get(6)));
    }

    /**
     * Combines two futures and applies a bi-function to their results.
     * This method waits for both futures to complete and then applies the provided function
     * to transform their results into a single value. It's a convenience method that combines
     * waiting for completion with result transformation.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<Integer> priceFuture = fetchPrice();
     * Future<Double> taxRateFuture = fetchTaxRate();
     * 
     * ContinuableFuture<Double> totalPrice = Futures.combine(
     *     priceFuture, 
     *     taxRateFuture,
     *     (price, rate) -> price * (1 + rate)
     * );
     * 
     * System.out.println("Total with tax: " + totalPrice.get());
     * }</pre>
     *
     * @param <T1> The result type of the first future
     * @param <T2> The result type of the second future
     * @param <R> The result type after applying the action
     * @param cf1 The first future
     * @param cf2 The second future
     * @param action The function to apply to both results. Receives the actual values (not futures)
     * @return A ContinuableFuture containing the result of applying the action to both values
     */
    public static <T1, T2, R> ContinuableFuture<R> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Throwables.BiFunction<? super T1, ? super T2, ? extends R, ? extends Exception> action) {
        return allOf(Arrays.asList(cf1, cf2)).map(t -> action.apply((T1) t.get(0), (T2) t.get(1)));
    }

    /**
     * Combines three futures and applies a tri-function to their results.
     * Similar to the two-argument version but for three futures. Waits for all three to complete
     * before applying the transformation function.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<Double> length = measureLength();
     * Future<Double> width = measureWidth();
     * Future<Double> height = measureHeight();
     * 
     * ContinuableFuture<Double> volume = Futures.combine(
     *     length, width, height,
     *     (l, w, h) -> l * w * h
     * );
     * 
     * System.out.println("Volume: " + volume.get());
     * }</pre>
     *
     * @param <T1> The result type of the first future
     * @param <T2> The result type of the second future
     * @param <T3> The result type of the third future
     * @param <R> The result type after applying the action
     * @param cf1 The first future
     * @param cf2 The second future
     * @param cf3 The third future
     * @param action The function to apply to all three results
     * @return A ContinuableFuture containing the result of applying the action
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2, final Future<? extends T3> cf3,
            final Throwables.TriFunction<? super T1, ? super T2, ? super T3, ? extends R, ? extends Exception> action) {
        return allOf(Arrays.asList(cf1, cf2, cf3)).map(t -> action.apply((T1) t.get(0), (T2) t.get(1), (T3) t.get(2)));
    }

    /**
     * Combines a collection of futures and applies a function to all their results.
     * This method waits for all futures in the collection to complete, collects their results
     * into a list, and applies the provided function. Useful for aggregating results from
     * a dynamic number of futures.
     * 
     * <p>Example usage:
     * <pre>{@code
     * List<Future<Integer>> scoreFutures = players.stream()
     *     .map(player -> calculateScore(player))
     *     .collect(Collectors.toList());
     * 
     * ContinuableFuture<Integer> totalScore = Futures.combine(
     *     scoreFutures,
     *     scores -> scores.stream().mapToInt(Integer::intValue).sum()
     * );
     * 
     * System.out.println("Team total: " + totalScore.get());
     * }</pre>
     *
     * @param <T> The result type of the input futures
     * @param <R> The result type after applying the action
     * @param cfs The collection of futures to combine
     * @param action The function to apply to the list of results
     * @return A ContinuableFuture containing the result of the action
     */
    public static <T, R> ContinuableFuture<R> combine(final Collection<? extends Future<? extends T>> cfs,
            final Throwables.Function<List<T>, ? extends R, ? extends Exception> action) {
        final ContinuableFuture<List<T>> f = allOf(cfs);
        return f.map(action);
    }

    //    public static <T, R> Future<R> combine(final List<? extends Future<? extends T>> cfs, final Try.Function<List<T>, ? extends R, ? extends Exception> action) {
    //        final Future<List<T>> future = allOf(cfs);
    //        return future.thenApply(action);
    //    }

    /**
     * Creates a ContinuableFuture that completes when all of the given futures complete.
     * If any future completes exceptionally, the returned future also completes exceptionally
     * with the first exception encountered. The results are collected into a list in the same
     * order as the input array.
     * 
     * <p>This method is useful when you have multiple independent operations that all need
     * to complete before proceeding, and you need all their results.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<String> future1 = fetchDataFromService1();
     * Future<String> future2 = fetchDataFromService2();
     * Future<String> future3 = fetchDataFromService3();
     * 
     * ContinuableFuture<List<String>> allData = Futures.allOf(
     *     future1, future2, future3
     * );
     * 
     * allData.thenAccept(results -> {
     *     System.out.println("All services returned: " + results);
     * });
     * }</pre>
     *
     * @param <T> The result type of the futures
     * @param cfs The array of futures to wait for
     * @return A ContinuableFuture that completes with a list of all results when all input
     *         futures complete successfully
     * @throws IllegalArgumentException if the array is null or empty
     */
    @SafeVarargs
    public static <T> ContinuableFuture<List<T>> allOf(final Future<? extends T>... cfs) {
        return allOf2(Arrays.asList(cfs));
    }

    /**
     * Creates a ContinuableFuture that completes when all futures in the collection complete.
     * Similar to the array version but accepts any Collection implementation. Results are
     * collected into a list in iteration order of the input collection.
     * 
     * <p>The returned future's list will have the same size as the input collection, with
     * results in corresponding positions. If any future fails, the returned future fails
     * with the first exception encountered.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Set<Future<ValidationResult>> validations = new HashSet<>();
     * validations.add(validateEmail(email));
     * validations.add(validatePhone(phone));
     * validations.add(validateAddress(address));
     * 
     * ContinuableFuture<List<ValidationResult>> allValidations = 
     *     Futures.allOf(validations);
     * 
     * allValidations.thenAccept(results -> {
     *     boolean allValid = results.stream()
     *         .allMatch(ValidationResult::isValid);
     *     if (allValid) {
     *         proceedWithRegistration();
     *     }
     * });
     * }</pre>
     *
     * @param <T> The result type of the futures
     * @param cfs The collection of futures to wait for
     * @return A ContinuableFuture that completes with a list of all results
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <T> ContinuableFuture<List<T>> allOf(final Collection<? extends Future<? extends T>> cfs) {
        return allOf2(cfs);
    }

    private static <T> ContinuableFuture<List<T>> allOf2(final Collection<? extends Future<? extends T>> cfs) {
        N.checkArgument(N.notEmpty(cfs), "'cfs' can't be null or empty");

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            exception.addSuppressed(e);
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                return res;
            }

            @Override
            public boolean isCancelled() {
                for (final Future<?> future : cfs) {
                    if (future.isCancelled()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public boolean isDone() {
                for (final Future<?> future : cfs) {
                    if (!future.isDone()) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public List<T> get() throws InterruptedException, ExecutionException {
                final List<T> result = new ArrayList<>(cfs.size());

                for (final Future<? extends T> future : cfs) {
                    result.add(future.get());
                }

                return result;
            }

            @Override
            public List<T> get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final long timeoutInMillis = unit.toMillis(timeout);
                final long startTime = System.currentTimeMillis();
                final long endTime = timeoutInMillis > Long.MAX_VALUE - startTime ? Long.MAX_VALUE : startTime + timeoutInMillis;

                final List<T> result = new ArrayList<>(cfs.size());

                for (final Future<? extends T> future : cfs) {
                    result.add(future.get(N.max(0, endTime - System.currentTimeMillis()), TimeUnit.MILLISECONDS));
                }

                return result;
            }
        });
    }

    /**
     * Creates a new Future that implements the "any of" semantics by returning the result of the first
     * future to complete successfully. If all futures complete exceptionally, the returned future completes
     * exceptionally with a composite exception containing all failures.
     * 
     * <p>This method is useful for scenarios where you have multiple ways to get a result
     * and want to use whichever completes first, such as querying multiple replicas or
     * implementing timeouts with fallbacks.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<Data> primarySource = fetchFromPrimary();
     * Future<Data> secondarySource = fetchFromSecondary();
     * Future<Data> cacheSource = fetchFromCache();
     * 
     * ContinuableFuture<Data> firstAvailable = Futures.anyOf(
     *     cacheSource, primarySource, secondarySource
     * );
     * 
     * Data result = firstAvailable.get(); // Gets the fastest result
     * }</pre>
     *
     * @param <T> The result type of the futures
     * @param cfs The array of futures to race
     * @return A ContinuableFuture that completes with the first successful result
     * @throws IllegalArgumentException if the array is null or empty
     */
    @SafeVarargs
    public static <T> ContinuableFuture<T> anyOf(final Future<? extends T>... cfs) {
        return anyOf2(Arrays.asList(cfs));
    }

    /**
     * Creates a new Future that implements the "any of" semantics by returning the result of the first
     * future to complete successfully. If all futures complete exceptionally, the returned future completes
     * exceptionally with a composite exception containing all failures.
     * 
     * <p>This is particularly useful for implementing timeout patterns, redundancy, or
     * getting the fastest response from multiple sources.
     * 
     * <p>Example usage:
     * <pre>{@code
     * List<Future<Price>> priceQueries = suppliers.stream()
     *     .map(supplier -> supplier.getQuote(item))
     *     .collect(Collectors.toList());
     * 
     * ContinuableFuture<Price> firstQuote = Futures.anyOf(priceQueries);
     * 
     * firstQuote.thenAccept(price -> {
     *     System.out.println("First price received: " + price);
     *     // Optionally cancel remaining queries
     * });
     * }</pre>
     *
     * @param <T> The result type of the futures
     * @param cfs The collection of futures to race
     * @return A ContinuableFuture that completes with the first successful result
     * @throws IllegalArgumentException if the collection is null or empty
     */
    public static <T> ContinuableFuture<T> anyOf(final Collection<? extends Future<? extends T>> cfs) {
        return anyOf2(cfs);
    }

    /**
     * Internal implementation method that creates a ContinuableFuture completing when any of the input futures completes.
     * <p>
     * This method creates a new Future that implements the "any of" semantics by returning the result of the first
     * future to complete successfully. If all futures complete exceptionally, the returned future completes
     * exceptionally with a composite exception containing all failures.
     * </p>
     *
     * <p>The implementation uses an anonymous Future class that provides custom behavior for each Future method:</p>
     * <ul>
     *   <li><strong>cancel()</strong> - Attempts to cancel all input futures, aggregating any exceptions</li>
     *   <li><strong>isCancelled()</strong> - Returns true only if ALL input futures are cancelled</li>
     *   <li><strong>isDone()</strong> - Returns true as soon as ANY input future is done</li>
     *   <li><strong>get()</strong> - Returns the first successful result, or throws aggregated exceptions if all fail</li>
     *   <li><strong>get(timeout, unit)</strong> - Same as get() but with timeout enforcement</li>
     * </ul>
     *
     * <p>Exception handling strategy:</p>
     * <ul>
     *   <li>For cancellation: Collects RuntimeExceptions and adds them as suppressed exceptions</li>
     *   <li>For result retrieval: Uses {@link #iterate(Collection, Function)} to process futures as they complete</li>
     *   <li>Failed futures have their exceptions converted and accumulated using {@link Exception#addSuppressed(Throwable)}</li>
     *   <li>If no future succeeds, throws the accumulated exception with all failures</li>
     * </ul>
     *
     * <p>The method leverages the {@code iterate} utility methods to handle completion order and timeout scenarios.
     * This allows processing results as they become available rather than polling all futures repeatedly.</p>
     *
     * <p><strong>Cancellation semantics:</strong></p>
     * <ul>
     *   <li>cancel() returns true only if ALL underlying futures were successfully cancelled</li>
     *   <li>isCancelled() returns true only if ALL underlying futures are cancelled</li>
     *   <li>Individual future cancellation failures are collected and re-thrown as a composite exception</li>
     * </ul>
     *
     * <p><strong>Completion semantics:</strong></p>
     * <ul>
     *   <li>isDone() returns true as soon as any underlying future completes (successfully or exceptionally)</li>
     *   <li>get() methods return the first successful result encountered</li>
     *   <li>If all futures fail, the last accumulated exception is thrown</li>
     * </ul>
     *
     * @param <T> the result type returned by the input futures and the resulting ContinuableFuture
     * @param cfs the collection of input futures to race. Must not be null or empty.
     * @return a new ContinuableFuture that completes with the first successful result,
     *         or fails if all input futures fail
     * @throws IllegalArgumentException if the collection is null or empty
     *
     * @see #anyOf(Future[])
     * @see #anyOf(Collection)
     * @see #iterate(Collection, Function)
     * @see ContinuableFuture#wrap(Future)
     * @see ExceptionUtil#toRuntimeException(Throwable, boolean)
     */
    private static <T> ContinuableFuture<T> anyOf2(final Collection<? extends Future<? extends T>> cfs) {
        N.checkArgument(N.notEmpty(cfs), "'cfs' can't be null or empty");

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            exception.addSuppressed(e);
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                return res;
            }

            @Override
            public boolean isCancelled() {
                for (final Future<?> future : cfs) {
                    if (!future.isCancelled()) {
                        return false;
                    }
                }

                return true;
            }

            @Override
            public boolean isDone() {
                for (final Future<?> future : cfs) {
                    if (future.isDone()) {
                        return true;
                    }
                }

                return false;
            }

            @Override
            public T get() throws InterruptedException, ExecutionException {
                final Iterator<Result<T, Exception>> iter = iterate(cfs, Fn.identity());
                Result<T, Exception> result = null;
                RuntimeException exception = null;

                while (iter.hasNext()) {
                    result = iter.next();

                    if (result.isSuccess()) {
                        return result.orElseIfFailure(null);
                    } else {
                        if (exception == null) {
                            exception = ExceptionUtil.toRuntimeException(result.getException(), false);
                        } else {
                            exception.addSuppressed(ExceptionUtil.toRuntimeException(result.getException(), false));
                        }
                    }
                }

                throw exception;
            }

            @Override
            public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
                final Iterator<Result<T, Exception>> iter = iterate(cfs, timeout, unit, Fn.identity());
                Result<T, Exception> result = null;
                RuntimeException exception = null;

                while (iter.hasNext()) {
                    result = iter.next();

                    if (result.isSuccess()) {
                        return result.orElseIfFailure(null);
                    } else {
                        if (exception == null) {
                            exception = ExceptionUtil.toRuntimeException(result.getException(), false);
                        } else {
                            exception.addSuppressed(ExceptionUtil.toRuntimeException(result.getException(), false));
                        }
                    }
                }

                throw exception;
            }
        });
    }

    /**
     * Creates an iterator that yields results from futures as they complete (first-finished, first-out).
     * This method allows processing results as soon as they become available, without waiting
     * for all futures to complete. Failed futures will throw their exceptions when their result
     * is requested via next().
     * 
     * <p>The iterator will continue until all futures have been processed. Each call to next()
     * blocks until at least one more future completes.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Future<Data> slowQuery = performSlowQuery();
     * Future<Data> mediumQuery = performMediumQuery();
     * Future<Data> fastQuery = performFastQuery();
     * 
     * ObjIterator<Data> results = Futures.iterate(
     *     slowQuery, mediumQuery, fastQuery
     * );
     * 
     * while (results.hasNext()) {
     *     Data data = results.next();
     *     processDataImmediately(data);
     *     // Process each result as soon as it's available
     * }
     * }</pre>
     *
     * @param <T> The result type of the futures
     * @param cfs The array of futures to iterate over
     * @return An iterator that yields results in completion order
     */
    @SafeVarargs
    public static <T> ObjIterator<T> iterate(final Future<? extends T>... cfs) {
        return iterate02(Arrays.asList(cfs));
    }

    /**
     * Creates an iterator that yields results from futures in the collection as they complete.
     * Similar to the array version but accepts any Collection. Results are returned in the
     * order of completion, not the order in the collection.
     * 
     * <p>This is useful for processing results incrementally, implementing progress updates,
     * or handling results with different processing times.
     * 
     * <p>Example usage:
     * <pre>{@code
     * List<Future<ProcessedFile>> fileFutures = files.stream()
     *     .map(file -> processFileAsync(file))
     *     .collect(Collectors.toList());
     * 
     * ObjIterator<ProcessedFile> processedFiles = Futures.iterate(fileFutures);
     * 
     * int completed = 0;
     * while (processedFiles.hasNext()) {
     *     ProcessedFile result = processedFiles.next();
     *     saveResult(result);
     *     completed++;
     *     updateProgress(completed, files.size());
     * }
     * }</pre>
     *
     * @param <T> The result type of the futures
     * @param cfs The collection of futures to iterate over
     * @return An iterator that yields results in completion order
     */
    public static <T> ObjIterator<T> iterate(final Collection<? extends Future<? extends T>> cfs) {
        return iterate02(cfs);
    }

    /**
     * Creates an iterator with a total timeout for all futures.
     * Similar to the regular iterate method, but enforces a maximum total time for retrieving
     * all results. If the timeout is exceeded, the iterator will throw a TimeoutException
     * wrapped in a RuntimeException on the next() call.
     * 
     * <p>This is useful when you need to process as many results as possible within a time
     * budget, or when implementing overall operation timeouts.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Collection<Future<SearchResult>> searches = performParallelSearches();
     * 
     * // Process results for up to 5 seconds total
     * ObjIterator<SearchResult> results = Futures.iterate(
     *     searches, 5, TimeUnit.SECONDS
     * );
     * 
     * List<SearchResult> collected = new ArrayList<>();
     * try {
     *     while (results.hasNext()) {
     *         collected.add(results.next());
     *     }
     * } catch (RuntimeException e) {
     *     if (e.getCause() instanceof TimeoutException) {
     *         System.out.println("Timeout reached, got " + 
     *                            collected.size() + " results");
     *     }
     * }
     * }</pre>
     *
     * @param <T> The result type of the futures
     * @param cfs The collection of futures to iterate over
     * @param totalTimeoutForAll The maximum time to wait for all results
     * @param unit The time unit of the timeout
     * @return An iterator that yields results in completion order with timeout enforcement
     */
    public static <T> ObjIterator<T> iterate(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit) {
        return iterate02(cfs, totalTimeoutForAll, unit);
    }

    private static <T> ObjIterator<T> iterate02(final Collection<? extends Future<? extends T>> cfs) {
        return iterate02(cfs, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    private static <T> ObjIterator<T> iterate02(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit) {
        final Iterator<Result<T, Exception>> iter = iterate02(cfs, totalTimeoutForAll, unit, Fn.identity());

        return new ObjIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final Result<T, Exception> result = iter.next();

                return result.orElseThrow(Fn.toRuntimeException());
            }
        };
    }

    /**
     * Creates an iterator that yields transformed results from futures as they complete.
     * The resultHandler function receives Result objects that encapsulate either success values
     * or exceptions, allowing custom handling of both cases. This is useful for logging,
     * error recovery, or transforming results.
     * 
     * <p>The Result object provides methods like isSuccess(), isFailure(), get(), and
     * getException() for handling both success and failure cases elegantly.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Collection<Future<Integer>> calculations = startCalculations();
     * 
     * ObjIterator<String> results = Futures.iterate(calculations, 
     *     result -> {
     *         if (result.isSuccess()) {
     *             return "Success: " + result.get();
     *         } else {
     *             return "Failed: " + result.getException().getMessage();
     *         }
     *     });
     * 
     * while (results.hasNext()) {
     *     System.out.println(results.next());
     * }
     * }</pre>
     *
     * @param <T> The result type of the input futures
     * @param <R> The result type after transformation
     * @param cfs The collection of futures to iterate over
     * @param resultHandler Function to transform Result objects into desired output type
     * @return An iterator that yields transformed results in completion order
     */
    public static <T, R> ObjIterator<R> iterate(final Collection<? extends Future<? extends T>> cfs,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler) {
        return iterate02(cfs, resultHandler);
    }

    /**
     * Creates an iterator with custom result handling and a total timeout.
     * Combines the features of timeout enforcement and custom result transformation.
     * The resultHandler can process both successful results and failures, including
     * timeout exceptions.
     * 
     * <p>This is the most flexible iteration method, suitable for complex scenarios
     * requiring both error handling and time constraints.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Collection<Future<DataPoint>> dataFutures = collectDataAsync();
     * 
     * ObjIterator<ProcessedData> processed = Futures.iterate(
     *     dataFutures, 
     *     10, TimeUnit.SECONDS,
     *     result -> {
     *         if (result.isSuccess()) {
     *             return processDataPoint(result.get());
     *         } else if (result.getException() instanceof TimeoutException) {
     *             return ProcessedData.timeout();
     *         } else {
     *             logError(result.getException());
     *             return ProcessedData.error();
     *         }
     *     });
     * 
     * // Process available results within time budget
     * List<ProcessedData> results = new ArrayList<>();
     * while (processed.hasNext()) {
     *     results.add(processed.next());
     * }
     * }</pre>
     *
     * @param <T> The result type of the input futures
     * @param <R> The result type after transformation
     * @param cfs The collection of futures to iterate over
     * @param totalTimeoutForAll The maximum time to wait for all results
     * @param unit The time unit of the timeout
     * @param resultHandler Function to transform Result objects, including timeout handling
     * @return An iterator that yields transformed results with timeout enforcement
     */
    public static <T, R> ObjIterator<R> iterate(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler) {
        return iterate02(cfs, totalTimeoutForAll, unit, resultHandler);
    }

    private static <T, R> ObjIterator<R> iterate02(final Collection<? extends Future<? extends T>> cfs,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler) {
        return iterate02(cfs, Long.MAX_VALUE, TimeUnit.MILLISECONDS, resultHandler);
    }

    private static <T, R> ObjIterator<R> iterate02(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler) {
        N.checkArgPositive(totalTimeoutForAll, cs.totalTimeoutForAll);
        N.checkArgNotNull(unit, cs.unit);
        N.checkArgNotNull(resultHandler, cs.resultHandler);

        final long startTime = System.currentTimeMillis();
        final long totalTimeoutForAllInMillis = totalTimeoutForAll == Long.MAX_VALUE ? Long.MAX_VALUE : unit.toMillis(totalTimeoutForAll);

        return new ObjIterator<>() {
            private final Set<Future<? extends T>> activeFutures = N.newSetFromMap(new IdentityHashMap<>());

            { //NOSONAR
                activeFutures.addAll(cfs);
            }

            @Override
            public boolean hasNext() {
                return activeFutures.size() > 0;
            }

            @Override
            public R next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                while (true) {
                    final java.util.Iterator<Future<? extends T>> iterator = activeFutures.iterator();
                    while (iterator.hasNext()) {
                        final Future<? extends T> cf = iterator.next();
                        if (cf.isDone()) {
                            try {
                                return resultHandler.apply(Result.of(cf.get(), null));
                            } catch (final Exception e) {
                                return resultHandler.apply(Result.of(null, Futures.convertException(e)));
                            } finally {
                                iterator.remove();
                            }
                        }
                    }

                    if (System.currentTimeMillis() - startTime >= totalTimeoutForAllInMillis) {
                        return resultHandler.apply(Result.of(null, new TimeoutException()));
                    }

                    N.sleepUninterruptibly(1);
                }
            }
        };
    }

    static Exception convertException(final Exception e) {
        if (e instanceof ExecutionException && e.getCause() instanceof Exception ex) {
            return ex;
        }

        return e;
    }
}
