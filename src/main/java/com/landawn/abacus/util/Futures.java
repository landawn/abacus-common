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
 * A comprehensive utility class providing powerful methods for composing, combining, and managing multiple
 * {@link Future} objects in concurrent programming scenarios. This class offers sophisticated functionality
 * for orchestrating asynchronous operations, including parallel execution coordination, result aggregation,
 * and sequential processing of completed futures in a thread-safe and efficient manner.
 *
 * <p>The {@code Futures} utility addresses common challenges in concurrent programming by providing intuitive
 * methods for handling multiple asynchronous operations simultaneously. It bridges the gap between individual
 * Future objects and complex multi-future workflows, offering both simple combination operations and advanced
 * composition patterns that maintain type safety and provide comprehensive error handling.</p>
 *
 * <p><b>Key Features and Capabilities:</b>
 * <ul>
 *   <li><b>Future Composition:</b> Combine multiple futures using custom zip functions for flexible result processing</li>
 *   <li><b>Tuple Integration:</b> Seamless conversion of multiple futures into strongly-typed Tuple objects</li>
 *   <li><b>Parallel Coordination:</b> {@code allOf()} methods for waiting until all futures complete successfully</li>
 *   <li><b>Race Conditions:</b> {@code anyOf()} methods for processing the first completed future result</li>
 *   <li><b>Completion Iteration:</b> Iterator-based access to futures as they complete (first-finished, first-out)</li>
 *   <li><b>Timeout Management:</b> Built-in timeout support for preventing indefinite blocking operations</li>
 *   <li><b>Error Aggregation:</b> Comprehensive exception handling and propagation across multiple operations</li>
 *   <li><b>Type Safety:</b> Strong generic typing maintained throughout all composition operations</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Simplicity Over Complexity:</b> Intuitive API that handles complex concurrency patterns transparently</li>
 *   <li><b>Type Safety First:</b> Strong generic typing prevents runtime errors and improves code clarity</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms minimizing overhead in multi-future operations</li>
 *   <li><b>Error Resilience:</b> Robust exception handling with proper propagation and aggregation</li>
 *   <li><b>Memory Efficient:</b> Optimized internal structures for large-scale concurrent operations</li>
 * </ul>
 *
 * <p><b>Primary Use Cases:</b>
 * <ul>
 *   <li><b>Microservice Integration:</b> Orchestrating multiple API calls in distributed systems</li>
 *   <li><b>Database Operations:</b> Coordinating parallel database queries and transactions</li>
 *   <li><b>File Processing:</b> Managing concurrent file I/O operations with result aggregation</li>
 *   <li><b>Web Service Composition:</b> Combining results from multiple web service endpoints</li>
 *   <li><b>Batch Processing:</b> Coordinating parallel processing of large datasets</li>
 *   <li><b>Real-time Analytics:</b> Processing streaming data from multiple concurrent sources</li>
 * </ul>
 *
 * <p><b>Method Categories:</b>
 * <ul>
 *   <li><b>Composition Methods:</b> {@code combine()} - Custom zip functions for flexible future combination</li>
 *   <li><b>Coordination Methods:</b> {@code allOf()}, {@code anyOf()} - Standard parallel execution patterns</li>
 *   <li><b>Iteration Methods:</b> {@code iterate()} - Process futures as they complete with optional timeouts</li>
 *   <li><b>Tuple Methods:</b> Direct combination into Tuple2 through Tuple7 for structured results</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic parallel execution - wait for all results
 * Future<User> userFuture = userService.fetchUser(userId);
 * Future<List<Order>> ordersFuture = orderService.fetchOrders(userId);
 * Future<Profile> profileFuture = profileService.fetchProfile(userId);
 *
 * ContinuableFuture<List<Object>> allResults = Futures.allOf(userFuture, ordersFuture, profileFuture);
 * List<Object> results = allResults.get();   // [User, List<Order>, Profile]
 *
 * // Structured result combination using Tuples
 * ContinuableFuture<Tuple3<User, List<Order>, Profile>> structuredResult =
 *     Futures.combine(userFuture, ordersFuture, profileFuture, Tuple::of);
 * Tuple3<User, List<Order>, Profile> data = structuredResult.get();
 *
 * // Race condition - process first completed result
 * Future<String> primaryAPI = callPrimaryService();
 * Future<String> backupAPI = callBackupService();
 * ContinuableFuture<String> firstResponse = Futures.anyOf(primaryAPI, backupAPI);
 *
 * // Process results as they complete
 * List<Future<ProcessingResult>> processingFutures = createProcessingTasks();
 * ObjIterator<ProcessingResult> completionIterator = Futures.iterate(processingFutures);
 * while (completionIterator.hasNext()) {
 *     ProcessingResult result = completionIterator.next();
 *     handleCompletedResult(result);
 * }
 * }</pre>
 *
 * <p><b>Advanced Composition Examples:</b>
 * <pre>{@code
 * // Complex data aggregation workflow
 * public class DataAggregationService {
 *     public ContinuableFuture<AnalyticsReport> generateReport(String reportId) {
 *         Future<MetricsData> metricsFuture = metricsService.fetchMetrics(reportId);
 *         Future<UserData> userDataFuture = userService.fetchUserData(reportId);
 *         Future<EventData> eventDataFuture = eventService.fetchEvents(reportId);
 *
 *         return Futures.combine(metricsFuture, userDataFuture, eventDataFuture,
 *             (metrics, userData, events) -> {
 *                 return analyticsEngine.createReport(metrics, userData, events);
 *             });
 *     }
 * }
 *
 * // Timeout-aware batch processing
 * public class BatchProcessor {
 *     public List<ProcessingResult> processBatch(List<Task> tasks, long timeoutSeconds) {
 *         List<Future<ProcessingResult>> futures = tasks.stream()
 *             .map(task -> executor.submit(() -> processTask(task)))
 *             .collect(Collectors.toList());
 *
 *         List<ProcessingResult> results = new ArrayList<>();
 *         ObjIterator<ProcessingResult> iterator = Futures.iterate(futures,
 *             timeoutSeconds, TimeUnit.SECONDS);
 *
 *         while (iterator.hasNext()) {
 *             try {
 *                 results.add(iterator.next());
 *             } catch (RuntimeException e) {
 *                 logger.warn("Task failed", e);
 *                 results.add(ProcessingResult.failed(e.getMessage()));
 *             }
 *         }
 *
 *         return results;
 *     }
 * }
 *
 * // Custom transformation with error handling
 * public ContinuableFuture<CustomerDashboard> buildDashboard(String customerId) {
 *     Future<Customer> customerFuture = fetchCustomer(customerId);
 *     Future<List<Transaction>> transactionsFuture = fetchTransactions(customerId);
 *     Future<AccountSummary> summaryFuture = fetchAccountSummary(customerId);
 *
 *     return Futures.combine(Arrays.asList(customerFuture, transactionsFuture, summaryFuture),
 *         results -> {
 *             Customer customer = (Customer) results.get(0);
 *             List<Transaction> transactions = (List<Transaction>) results.get(1);
 *             AccountSummary summary = (AccountSummary) results.get(2);
 *
 *             return new CustomerDashboard(customer, transactions, summary);
 *         });
 * }
 * }</pre>
 *
 * <p><b>Tuple Integration Patterns:</b>
 * <ul>
 *   <li><b>Tuple2:</b> Combine two futures into a pair for simple dual-result operations</li>
 *   <li><b>Tuple3:</b> Three-way combination for triple-result scenarios (common in database operations)</li>
 *   <li><b>Tuple4-7:</b> Higher-arity combinations for complex multi-service orchestration</li>
 *   <li><b>Type Safety:</b> Maintain compile-time type checking across all tuple operations</li>
 * </ul>
 *
 * <p><b>Completion Iteration Features:</b>
 * <ul>
 *   <li><b>First-Finished Processing:</b> Handle results as soon as individual futures complete</li>
 *   <li><b>Timeout Support:</b> Prevent indefinite blocking with configurable timeout values</li>
 *   <li><b>Exception Isolation:</b> Continue processing remaining futures even if some fail</li>
 *   <li><b>Memory Efficiency:</b> Stream-like processing without storing all results in memory</li>
 *   <li><b>Custom Transformation:</b> Apply functions to results during iteration</li>
 * </ul>
 *
 * <p><b>Error Handling and Exception Management:</b>
 * <ul>
 *   <li><b>Exception Propagation:</b> Automatic propagation of exceptions from constituent futures</li>
 *   <li><b>Aggregated Failures:</b> Collect and report multiple failure scenarios appropriately</li>
 *   <li><b>Timeout Exceptions:</b> Clear timeout handling with TimeoutException propagation</li>
 *   <li><b>Cancellation Support:</b> Proper handling of cancelled futures in combination operations</li>
 *   <li><b>Recovery Strategies:</b> Support for partial success scenarios and fallback values</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Combination Overhead:</b> O(1) for tuple combinations, O(n) for collection-based operations</li>
 *   <li><b>Memory Usage:</b> Efficient internal structures with minimal additional allocation</li>
 *   <li><b>Thread Safety:</b> All operations are thread-safe without synchronization overhead</li>
 *   <li><b>Completion Detection:</b> Optimized algorithms for detecting future completion states</li>
 *   <li><b>Iterator Efficiency:</b> Lazy evaluation in iteration methods for large future collections</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrency:</b>
 * <ul>
 *   <li><b>Static Methods:</b> All utility methods are static and inherently thread-safe</li>
 *   <li><b>Immutable Results:</b> Returned ContinuableFuture instances are safely publishable</li>
 *   <li><b>Concurrent Access:</b> Multiple threads can safely call methods simultaneously</li>
 *   <li><b>No Shared State:</b> No mutable static variables that could cause race conditions</li>
 *   <li><b>Executor Independence:</b> Works with any Executor implementation for flexible threading</li>
 * </ul>
 *
 * <p><b>Integration with Future Types:</b>
 * <ul>
 *   <li><b>CompletableFuture:</b> Full compatibility with Java 8+ CompletableFuture instances</li>
 *   <li><b>ContinuableFuture:</b> Native support for enhanced ContinuableFuture functionality</li>
 *   <li><b>ForkJoinTask:</b> Compatible with ForkJoinPool-based asynchronous operations</li>
 *   <li><b>ExecutorService Futures:</b> Works with any Future implementation from Executor submissions</li>
 *   <li><b>Custom Futures:</b> Accepts any object implementing the Future interface</li>
 * </ul>
 *
 * <p><b>Best Practices and Recommendations:</b>
 * <ul>
 *   <li>Use {@code allOf()} when you need all results before proceeding with computation</li>
 *   <li>Use {@code anyOf()} for race conditions where first completion is sufficient</li>
 *   <li>Use {@code iterate()} for processing results as they become available (stream-like processing)</li>
 *   <li>Prefer Tuple combinations for small, fixed numbers of futures (2-7 futures)</li>
 *   <li>Use collection-based methods for dynamic numbers of futures</li>
 *   <li>Always specify timeouts for iterate() methods to prevent indefinite blocking</li>
 *   <li>Handle exceptions appropriately - some futures may fail while others succeed</li>
 *   <li>Consider memory implications when dealing with large numbers of futures</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Calling {@code get()} on individual futures instead of using combination methods</li>
 *   <li>Creating deeply nested future chains instead of using parallel composition</li>
 *   <li>Ignoring timeout settings in long-running operations</li>
 *   <li>Not handling partial failure scenarios in multi-future operations</li>
 *   <li>Using inefficient sequential processing when parallel execution is possible</li>
 *   <li>Creating memory leaks by holding references to completed futures unnecessarily</li>
 * </ul>
 *
 * <p><b>Timeout and Cancellation Behavior:</b>
 * <ul>
 *   <li><b>Timeout Propagation:</b> Timeout exceptions are properly propagated through composition chains</li>
 *   <li><b>Partial Timeouts:</b> Iterator methods support timeouts with partial result processing</li>
 *   <li><b>Cancellation Handling:</b> Cancelled futures are handled gracefully in combination operations</li>
 *   <li><b>Resource Cleanup:</b> Proper cleanup of resources when operations timeout or are cancelled</li>
 * </ul>
 *
 * <p><b>Example: Microservice Orchestration</b>
 * <pre>{@code
 * public class OrderProcessingOrchestrator {
 *     private final UserService userService;
 *     private final InventoryService inventoryService;
 *     private final PaymentService paymentService;
 *     private final ShippingService shippingService;
 *
 *     public ContinuableFuture<OrderResult> processOrder(OrderRequest request) {
 *         // Step 1: Parallel validation
 *         Future<User> userValidation = userService.validateUser(request.getUserId());
 *         Future<Boolean> inventoryCheck = inventoryService.checkAvailability(request.getItems());
 *         Future<PaymentMethod> paymentValidation = paymentService.validatePayment(request.getPaymentInfo());
 *
 *         // Step 2: Wait for all validations to complete
 *         ContinuableFuture<Tuple3<User, Boolean, PaymentMethod>> validations =
 *             Futures.combine(userValidation, inventoryCheck, paymentValidation, Tuple::of);
 *
 *         // Step 3: Process order if all validations pass
 *         return validations.thenCompose(result -> {
 *             if (!result._2) { // inventory not available
 *                 throw new OrderProcessingException("Insufficient inventory");
 *             }
 *
 *             // Parallel processing
 *             Future<Payment> paymentProcessing = paymentService.processPayment(result._3, request.getAmount());
 *             Future<Shipment> shippingArrangement = shippingService.arrangeShipping(request.getShippingAddress());
 *
 *             return Futures.combine(paymentProcessing, shippingArrangement,
 *                 (payment, shipment) -> new OrderResult(request.getOrderId(), payment.getId(), shipment.getTrackingNumber()));
 *         });
 *     }
 *
 *     public List<OrderStatus> checkMultipleOrders(List<String> orderIds, int timeoutSeconds) {
 *         List<Future<OrderStatus>> statusFutures = orderIds.stream()
 *             .map(id -> executor.submit(() -> checkOrderStatus(id)))
 *             .collect(Collectors.toList());
 *
 *         List<OrderStatus> results = new ArrayList<>();
 *         ObjIterator<OrderStatus> iterator = Futures.iterate(statusFutures, timeoutSeconds, TimeUnit.SECONDS);
 *
 *         while (iterator.hasNext()) {
 *             try {
 *                 results.add(iterator.next());
 *             } catch (Exception e) {
 *                 results.add(OrderStatus.unknown("Status check failed: " + e.getMessage()));
 *             }
 *         }
 *
 *         return results;
 *     }
 * }
 * }</pre>
 *
 * <p><b>Comparison with Alternative Approaches:</b>
 * <ul>
 *   <li><b>vs. CompletableFuture.allOf():</b> Type-safe results vs. Object array returns</li>
 *   <li><b>vs. Manual Future.get() calls:</b> Parallel execution vs. sequential blocking</li>
 *   <li><b>vs. ExecutorCompletionService:</b> Simplified API vs. lower-level completion service management</li>
 *   <li><b>vs. Custom Thread Management:</b> Built-in error handling vs. manual exception aggregation</li>
 * </ul>
 *
 * <p><b>Integration with Concurrent Collections:</b>
 * <ul>
 *   <li><b>ConcurrentHashMap:</b> Thread-safe result caching and memoization</li>
 *   <li><b>BlockingQueue:</b> Producer-consumer patterns with future-based coordination</li>
 *   <li><b>CountDownLatch:</b> Coordination with traditional synchronization primitives</li>
 *   <li><b>Semaphore:</b> Resource management in conjunction with future-based operations</li>
 * </ul>
 *
 * @see ContinuableFuture
 * @see CompletableFuture
 * @see Future
 * @see ExecutorCompletionService
 * @see java.util.concurrent.Executor
 * @see com.landawn.abacus.util.Tuple
 * @see com.landawn.abacus.util.ObjIterator
 * @see com.landawn.abacus.util.function.Function
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Future.html">Future Documentation</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html">CompletableFuture Documentation</a>
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
     * The function is executed when get() or get(timeout, unit) is called on the returned future, allowing
     * lazy evaluation of the composition logic. This provides maximum flexibility for orchestrating the
     * completion of multiple futures with custom coordination strategies.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Lazy Execution:</b> The zip function is not executed until get() is called on the returned future</li>
     *   <li><b>Custom Coordination:</b> Full control over how and when to retrieve values from input futures</li>
     *   <li><b>Exception Handling:</b> InterruptedException and ExecutionException are propagated directly</li>
     *   <li><b>Unified Behavior:</b> Same logic applies to both timed and untimed get operations</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic arithmetic combination
     * Future<Integer> future1 = CompletableFuture.completedFuture(5);
     * Future<Integer> future2 = CompletableFuture.completedFuture(10);
     *
     * ContinuableFuture<Integer> sum = Futures.compose(future1, future2,
     *     (f1, f2) -> f1.get() + f2.get());
     *
     * System.out.println(sum.get());   // Prints: 15
     *
     * // Custom error handling
     * Future<String> mayFail1 = riskyOperation1();
     * Future<String> mayFail2 = riskyOperation2();
     *
     * ContinuableFuture<String> combined = Futures.compose(mayFail1, mayFail2,
     *     (f1, f2) -> {
     *         try {
     *             return f1.get() + " " + f2.get();
     *         } catch (Exception e) {
     *             return "Fallback value";
     *         }
     *     });
     *
     * // Conditional retrieval based on one future's result
     * Future<Boolean> condition = checkCondition();
     * Future<Data> expensiveData = loadExpensiveData();
     *
     * ContinuableFuture<Data> result = Futures.compose(condition, expensiveData,
     *     (condFuture, dataFuture) -> {
     *         if (condFuture.get()) {
     *             return dataFuture.get();
     *         } else {
     *             return Data.DEFAULT;  // Skip expensive retrieval
     *         }
     *     });
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <R> the result type of the composed future.
     * @param cf1 the first future to compose, must not be {@code null}.
     * @param cf2 the second future to compose, must not be {@code null}.
     * @param zipFunctionForGet the function that combines the futures' results. Receives both Future objects
     *                         as parameters and returns the composed result. Must not be {@code null}.
     * @return a ContinuableFuture that completes when both input futures complete, with a result
     *         computed by the provided zip function.
     * @throws RuntimeException if the zip function throws an exception other than InterruptedException or ExecutionException,
     *                         the exception is wrapped in a RuntimeException and thrown.
     * @see #compose(Future, Future, Throwables.BiFunction, Throwables.Function)
     * @see #combine(Future, Future, Throwables.BiFunction)
     * @see ContinuableFuture
     */
    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Throwables.BiFunction<? super Future<T1>, ? super Future<T2>, ? extends R, Exception> zipFunctionForGet) {
        return compose(cf1, cf2, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1, t._2));
    }

    /**
     * Composes two futures into a new ContinuableFuture with separate functions for regular and timeout-based operations.
     * This method provides maximum flexibility by allowing different logic for get() and get(timeout, unit) calls.
     * The timeout function receives a Tuple4 containing both futures, the timeout value, and the time unit, enabling
     * time-aware coordination strategies.
     *
     * <p>This is useful when you need different behavior for time-constrained operations, such as returning
     * a default value or using a different computation strategy when under time pressure. This enables
     * sophisticated patterns like graceful degradation, partial results, or fallback strategies when
     * operating under strict time constraints.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Dual Strategies:</b> Different execution paths for time-unlimited and time-limited scenarios</li>
     *   <li><b>Timeout Awareness:</b> Timeout function receives exact timeout parameters for precise control</li>
     *   <li><b>Graceful Degradation:</b> Supports returning partial or cached results when time is limited</li>
     *   <li><b>Performance Optimization:</b> Allows skipping expensive operations when under time pressure</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic timeout handling with fallback
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
     *
     * // Sophisticated timeout handling with partial results
     * Future<List<Data>> primaryData = fetchPrimaryData();
     * Future<List<Data>> cachedData = fetchCachedData();
     *
     * ContinuableFuture<Report> report = Futures.compose(primaryData, cachedData,
     *     // Full computation when time unlimited
     *     (primary, cached) -> {
     *         List<Data> all = new ArrayList<>(primary.get());
     *         all.addAll(cached.get());
     *         return generateFullReport(all);
     *     },
     *     // Quick computation when time limited
     *     tuple -> {
     *         try {
     *             // Try primary first with available time
     *             List<Data> data = tuple._1.get(tuple._3, tuple._4);
     *             return generateQuickReport(data);
     *         } catch (TimeoutException e) {
     *             // Fall back to cache if primary times out
     *             if (tuple._2.isDone()) {
     *                 return generateQuickReport(tuple._2.get());
     *             }
     *             return Report.EMPTY;
     *         }
     *     });
     *
     * // Conditional expensive operation
     * Future<Boolean> shouldProcess = checkProcessingFlag();
     * Future<ExpensiveData> expensiveOperation = performExpensiveOperation();
     *
     * ContinuableFuture<Result> result = Futures.compose(shouldProcess, expensiveOperation,
     *     (flag, data) -> flag.get() ? processData(data.get()) : Result.SKIPPED,
     *     tuple -> {
     *         // Under timeout, skip expensive operation if flag is false
     *         boolean process = tuple._1.get(tuple._3 / 2, tuple._4);
     *         if (!process) {
     *             return Result.SKIPPED;
     *         }
     *         return processData(tuple._2.get(tuple._3 / 2, tuple._4));
     *     });
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <R> the result type of the composed future.
     * @param cf1 the first future to compose, must not be {@code null}.
     * @param cf2 the second future to compose, must not be {@code null}.
     * @param zipFunctionForGet the function that combines the futures' results for regular get() operations.
     *                         Receives both Future objects. Must not be {@code null}.
     * @param zipFunctionTimeoutGet the function for get(timeout, unit) operations. Receives a Tuple4 with.
     *                              (_1: future1, _2: future2, _3: timeout value, _4: TimeUnit)
     *                              Must not be {@code null}.
     * @return a ContinuableFuture with custom logic for both regular and timeout operations.
     * @throws RuntimeException if either zip function throws an exception other than InterruptedException,.
     *                         ExecutionException, or TimeoutException.
     * @see #compose(Future, Future, Throwables.BiFunction)
     * @see Tuple4
     * @see TimeUnit
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
     * The function receives all three Future objects and can orchestrate their completion as needed, enabling
     * sophisticated coordination strategies for multiple asynchronous operations.
     *
     * <p>This overload uses the same function for both regular get() and timeout-based get() operations,
     * providing a unified composition strategy regardless of whether a timeout is specified.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Three-Way Composition:</b> Combines three independent asynchronous operations efficiently</li>
     *   <li><b>Flexible Coordination:</b> Full control over retrieval order and error handling strategy</li>
     *   <li><b>Lazy Evaluation:</b> Function executes only when result is requested</li>
     *   <li><b>Unified Behavior:</b> Same composition logic for timed and untimed operations</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic three-way string combination
     * Future<String> nameFuture = CompletableFuture.completedFuture("John");
     * Future<Integer> ageFuture = CompletableFuture.completedFuture(30);
     * Future<String> cityFuture = CompletableFuture.completedFuture("New York");
     *
     * ContinuableFuture<String> profile = Futures.compose(nameFuture, ageFuture, cityFuture,
     *     (f1, f2, f3) -> String.format("%s, %d years old, from %s",
     *         f1.get(), f2.get(), f3.get()));
     *
     * System.out.println(profile.get());   // "John, 30 years old, from New York"
     *
     * // Complex data aggregation from multiple sources
     * Future<UserData> userData = fetchUserData(userId);
     * Future<Preferences> preferences = fetchPreferences(userId);
     * Future<ActivityLog> activityLog = fetchActivityLog(userId);
     *
     * ContinuableFuture<Dashboard> dashboard = Futures.compose(
     *     userData, preferences, activityLog,
     *     (user, prefs, activity) -> {
     *         UserData u = user.get();
     *         Preferences p = prefs.get();
     *         ActivityLog a = activity.get();
     *         return new Dashboard(u, p, a);
     *     });
     *
     * // Conditional logic based on first future's result
     * Future<Boolean> featureEnabled = checkFeatureFlag("newFeature");
     * Future<NewData> newFeatureData = fetchNewFeatureData();
     * Future<LegacyData> legacyData = fetchLegacyData();
     *
     * ContinuableFuture<Response> response = Futures.compose(
     *     featureEnabled, newFeatureData, legacyData,
     *     (flag, newData, legacy) -> {
     *         if (flag.get()) {
     *             return Response.fromNew(newData.get());
     *         } else {
     *             return Response.fromLegacy(legacy.get());
     *         }
     *     });
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <R> the result type of the composed future.
     * @param cf1 the first future to compose, must not be {@code null}.
     * @param cf2 the second future to compose, must not be {@code null}.
     * @param cf3 the third future to compose, must not be {@code null}.
     * @param zipFunctionForGet the function that combines the futures' results. Receives all three Future objects
     *                         and returns the composed result. Must not be {@code null}.
     * @return a ContinuableFuture that completes when all three input futures complete, with a result
     *         computed by the provided zip function.
     * @throws RuntimeException if the zip function throws an exception other than InterruptedException or ExecutionException.
     * @see #compose(Future, Future, Future, Throwables.TriFunction, Throwables.Function)
     * @see #combine(Future, Future, Future, Throwables.TriFunction)
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Throwables.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, ? extends R, Exception> zipFunctionForGet) {
        return compose(cf1, cf2, cf3, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1, t._2, t._3));
    }

    /**
     * Composes three futures with separate functions for regular and timeout-based operations.
     * Similar to the two-future version, this provides different logic paths for time-constrained scenarios.
     * The timeout function receives a Tuple5 containing all three futures plus timeout information, enabling
     * sophisticated time-aware coordination strategies for three-way compositions.
     *
     * <p>This is particularly useful for complex operations where you might want to skip expensive
     * computations or use cached/default values when operating under time constraints. The method enables
     * patterns like partial data aggregation, priority-based retrieval, or graceful degradation when
     * coordinating three asynchronous operations under time pressure.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Dual Execution Paths:</b> Separate strategies for unlimited and time-limited scenarios</li>
     *   <li><b>Priority Handling:</b> Can prioritize which futures to retrieve first under time pressure</li>
     *   <li><b>Partial Results:</b> Supports returning results from subset of futures when time constrained</li>
     *   <li><b>Timeout Distribution:</b> Function receives timeout parameters for dynamic time allocation</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic timeout with cache fallback
     * Future<List<String>> dbQuery = queryDatabase();
     * Future<Map<String, Object>> cache = checkCache();
     * Future<String> config = loadConfig();
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
     *
     * // Priority-based retrieval with time allocation
     * Future<CriticalData> critical = fetchCriticalData();
     * Future<ImportantData> important = fetchImportantData();
     * Future<OptionalData> optional = fetchOptionalData();
     *
     * ContinuableFuture<AggregatedData> result = Futures.compose(
     *     critical, important, optional,
     *     // Full aggregation when time unlimited
     *     (c, i, o) -> new AggregatedData(c.get(), i.get(), o.get()),
     *     // Priority-based retrieval under timeout
     *     tuple -> {
     *         long timePerFuture = tuple._4 / 3;
     *         TimeUnit unit = tuple._5;
     *
     *         try {
     *             // Critical data first (40% of time)
     *             CriticalData c = tuple._1.get(timePerFuture * 2, unit);
     *
     *             // Important data second (40% of time)
     *             ImportantData i = tuple._2.get(timePerFuture * 2, unit);
     *
     *             // Optional data last (20% of time)
     *             OptionalData o = tuple._3.isDone() ?
     *                 tuple._3.get() : OptionalData.EMPTY;
     *
     *             return new AggregatedData(c, i, o);
     *         } catch (TimeoutException e) {
     *             // Return partial results
     *             return AggregatedData.partial(e.getMessage());
     *         }
     *     });
     *
     * // Conditional logic with multiple data sources
     * Future<Boolean> shouldUseNewAPI = checkAPIVersion();
     * Future<NewAPIData> newAPI = callNewAPI();
     * Future<OldAPIData> oldAPI = callOldAPI();
     *
     * ContinuableFuture<APIResponse> response = Futures.compose(
     *     shouldUseNewAPI, newAPI, oldAPI,
     *     (flag, newData, oldData) ->
     *         flag.get() ? APIResponse.from(newData.get()) :
     *                     APIResponse.from(oldData.get()),
     *     tuple -> {
     *         // Quick check with timeout
     *         boolean useNew = tuple._1.get(tuple._4 / 4, tuple._5);
     *         if (useNew) {
     *             return APIResponse.from(tuple._2.get(tuple._4 * 3 / 4, tuple._5));
     *         } else {
     *             return APIResponse.from(tuple._3.get(tuple._4 * 3 / 4, tuple._5));
     *         }
     *     });
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <R> the result type of the composed future.
     * @param cf1 the first future to compose, must not be {@code null}.
     * @param cf2 the second future to compose, must not be {@code null}.
     * @param cf3 the third future to compose, must not be {@code null}.
     * @param zipFunctionForGet the function that combines the futures' results for regular get() operations.
     *                         Receives all three Future objects. Must not be {@code null}.
     * @param zipFunctionTimeoutGet the function for get(timeout, unit) operations. Receives a Tuple5 with.
     *                              (_1: future1, _2: future2, _3: future3, _4: timeout value, _5: TimeUnit)
     *                              Must not be {@code null}.
     * @return a ContinuableFuture with custom logic for both regular and timeout operations.
     * @throws RuntimeException if either zip function throws an exception other than InterruptedException,.
     *                         ExecutionException, or TimeoutException.
     * @see #compose(Future, Future, Future, Throwables.TriFunction)
     * @see Tuple5
     * @see TimeUnit
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
     * receives the entire collection and can implement any logic for combining their results, making
     * it ideal for dynamic numbers of futures or custom aggregation logic.
     *
     * <p>The collection type is preserved (FC extends Collection), allowing type-safe operations
     * on specific collection implementations. This enables working with List, Set, or any other
     * Collection type while maintaining compile-time type safety.
     *
     * <p>This overload uses the same function for both regular get() and timeout-based get() operations.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Dynamic Size:</b> Works with any number of futures, determined at runtime</li>
     *   <li><b>Collection Type Preservation:</b> Maintains the specific collection type through generics</li>
     *   <li><b>Custom Aggregation:</b> Full control over how futures are combined and results processed</li>
     *   <li><b>Lazy Execution:</b> Zip function executes only when result is requested</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic sum aggregation
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
     * System.out.println(sum.get());   // Prints: 6
     *
     * // Complex data aggregation with error handling
     * List<Future<DataPoint>> dataFutures = sensors.stream()
     *     .map(sensor -> fetchDataAsync(sensor))
     *     .collect(Collectors.toList());
     *
     * ContinuableFuture<AggregatedReport> report = Futures.compose(dataFutures,
     *     futures -> {
     *         List<DataPoint> successfulData = new ArrayList<>();
     *         List<Exception> errors = new ArrayList<>();
     *
     *         for (Future<DataPoint> f : futures) {
     *             try {
     *                 successfulData.add(f.get());
     *             } catch (Exception e) {
     *                 errors.add(e);
     *             }
     *         }
     *
     *         return new AggregatedReport(successfulData, errors);
     *     });
     *
     * // Set-based uniqueness preservation
     * Set<Future<String>> uniqueQueries = new HashSet<>(queryFutures);
     *
     * ContinuableFuture<Set<String>> uniqueResults = Futures.compose(uniqueQueries,
     *     futureSet -> {
     *         Set<String> results = new HashSet<>();
     *         for (Future<String> f : futureSet) {
     *             results.add(f.get());
     *         }
     *         return results;
     *     });
     *
     * // Statistical analysis
     * List<Future<Double>> measurements = performMeasurements();
     *
     * ContinuableFuture<Statistics> stats = Futures.compose(measurements,
     *     futures -> {
     *         List<Double> values = new ArrayList<>();
     *         for (Future<Double> f : futures) {
     *             values.add(f.get());
     *         }
     *
     *         double sum = values.stream().mapToDouble(Double::doubleValue).sum();
     *         double avg = sum / values.size();
     *         double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
     *         double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
     *
     *         return new Statistics(avg, max, min, values.size());
     *     });
     * }</pre>
     *
     * @param <T> the result type of the input futures.
     * @param <FC> the specific collection type containing the futures (e.g., List, Set).
     * @param <R> the result type of the composed future.
     * @param cfs the collection of input futures, must not be {@code null} or empty.
     * @param zipFunctionForGet the function that combines the futures' results. Receives the collection
     *                         of Future objects and returns the composed result. Must not be {@code null}.
     * @return a ContinuableFuture that completes when all input futures complete, with a result
     *         computed by the provided zip function.
     * @throws IllegalArgumentException if the collection is {@code null} or empty.
     * @throws RuntimeException if the zip function throws an exception other than InterruptedException or ExecutionException.
     * @see #compose(Collection, Throwables.Function, Throwables.Function)
     * @see #combine(Collection, Throwables.Function)
     */
    public static <T, FC extends Collection<? extends Future<? extends T>>, R> ContinuableFuture<R> compose(final FC cfs,
            final Throwables.Function<? super FC, ? extends R, Exception> zipFunctionForGet) {
        return compose(cfs, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1));
    }

    /**
     * Composes a collection of futures with separate functions for regular and timeout operations.
     * This is the most flexible composition method, supporting any number of futures with custom
     * timeout handling. The timeout function receives a Tuple3 containing the collection, timeout value,
     * and time unit, enabling sophisticated time-aware aggregation strategies.
     *
     * <p>This method is ideal for scenarios where you need to aggregate results from many sources
     * but want different behavior when operating under time constraints. It supports patterns like
     * partial aggregation, best-effort collection, or graceful degradation when coordinating
     * large numbers of asynchronous operations.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Maximum Flexibility:</b> Supports any number of futures with custom coordination logic</li>
     *   <li><b>Dual Strategies:</b> Different aggregation approaches for unlimited and time-limited scenarios</li>
     *   <li><b>Partial Results:</b> Can return partial aggregations when some futures haven't completed</li>
     *   <li><b>Collection Type Preservation:</b> Maintains specific collection type through generics</li>
     *   <li><b>Best-Effort Processing:</b> Supports gathering available results within time constraints</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic partial aggregation under timeout
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
     *
     * // Time-budgeted distributed query aggregation
     * List<Future<QueryResult>> distributedQueries = servers.stream()
     *     .map(server -> queryServer(server, query))
     *     .collect(Collectors.toList());
     *
     * ContinuableFuture<AggregatedResult> result = Futures.compose(
     *     distributedQueries,
     *     // Full wait for all servers
     *     futures -> {
     *         List<QueryResult> results = new ArrayList<>();
     *         for (Future<QueryResult> f : futures) {
     *             results.add(f.get());
     *         }
     *         return AggregatedResult.complete(results);
     *     },
     *     // Time-budgeted collection
     *     tuple -> {
     *         Collection<Future<QueryResult>> futures = tuple._1;
     *         long timeout = tuple._2;
     *         TimeUnit unit = tuple._3;
     *
     *         long timePerQuery = timeout / futures.size();
     *         List<QueryResult> collected = new ArrayList<>();
     *
     *         for (Future<QueryResult> f : futures) {
     *             try {
     *                 collected.add(f.get(timePerQuery, unit));
     *             } catch (TimeoutException e) {
     *                 // Skip slow servers
     *             }
     *         }
     *
     *         return AggregatedResult.partial(collected);
     *     });
     *
     * // Priority-based processing with fallback
     * List<Future<CacheEntry>> cacheChecks = checkMultipleCaches(key);
     *
     * ContinuableFuture<CacheEntry> entry = Futures.compose(
     *     cacheChecks,
     *     futures -> {
     *         // Try all caches, return first successful
     *         for (Future<CacheEntry> f : futures) {
     *             CacheEntry e = f.get();
     *             if (e != null) return e;
     *         }
     *         return CacheEntry.MISS;
     *     },
     *     tuple -> {
     *         // Under timeout, check caches sequentially with time limit
     *         long remainingTime = unit.toMillis(tuple._2);
     *         long startTime = System.currentTimeMillis();
     *
     *         for (Future<CacheEntry> f : tuple._1) {
     *             long elapsed = System.currentTimeMillis() - startTime;
     *             long timeLeft = remainingTime - elapsed;
     *
     *             if (timeLeft <= 0) break;
     *
     *             try {
     *                 CacheEntry e = f.get(timeLeft, TimeUnit.MILLISECONDS);
     *                 if (e != null) return e;
     *             } catch (TimeoutException e) {
     *                 continue;
     *             }
     *         }
     *
     *         return CacheEntry.MISS;
     *     });
     * }</pre>
     *
     * @param <T> the result type of the input futures.
     * @param <FC> the specific collection type containing the futures (e.g., List, Set).
     * @param <R> the result type of the composed future.
     * @param cfs the collection of input futures, must not be {@code null} or empty.
     * @param zipFunctionForGet the function that combines the futures' results for regular get() operations.
     *                         Receives the collection of Future objects. Must not be {@code null}.
     * @param zipFunctionTimeoutGet the function for get(timeout, unit) operations. Receives a Tuple3 with.
     *                              (_1: futures collection, _2: timeout value, _3: TimeUnit). Must not be {@code null}.
     * @return a ContinuableFuture with custom logic for both regular and timeout operations.
     * @throws IllegalArgumentException if the collection is {@code null} or empty, or if either function is null.
     * @throws RuntimeException if either zip function throws an exception other than InterruptedException,.
     *                         ExecutionException, or TimeoutException.
     * @see #compose(Collection, Throwables.Function)
     * @see Tuple3
     * @see TimeUnit
     */
    public static <T, FC extends Collection<? extends Future<? extends T>>, R> ContinuableFuture<R> compose(final FC cfs,
            final Throwables.Function<? super FC, ? extends R, Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple3<FC, Long, TimeUnit>, ? extends R, Exception> zipFunctionTimeoutGet) throws IllegalArgumentException {
        N.checkArgument(N.notEmpty(cfs), "The specified collection cannot be null or empty");   //NOSONAR
        N.checkArgNotNull(zipFunctionForGet);
        N.checkArgNotNull(zipFunctionTimeoutGet);

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning);   //NOSONAR
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param cf1 the first future.
     * @param cf2 the second future.
     * @return a ContinuableFuture containing a Tuple2 with both results.
     * @see #combine(Future, Future, Future)
     * @see #combine(Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future, Future)
     */
    public static <T1, T2> ContinuableFuture<Tuple2<T1, T2>> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2) {
        return allOf(Arrays.asList(cf1, cf2)).map(t -> Tuple.of((T1) t.get(0), (T2) t.get(1)));
    }

    /**
     * Combines three futures into a Tuple3 containing all three results.
     * Similar to the two-argument version, but for three futures. Results are packaged in order
     * into a Tuple3 for convenient access to all values.
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param cf1 the first future.
     * @param cf2 the second future.
     * @param cf3 the third future.
     * @return a ContinuableFuture containing a Tuple3 with all three results.
     * @see #combine(Future, Future)
     * @see #combine(Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future, Future)
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <T4> the result type of the fourth future.
     * @param cf1 the first future.
     * @param cf2 the second future.
     * @param cf3 the third future.
     * @param cf4 the fourth future.
     * @return a ContinuableFuture containing a Tuple4 with all four results.
     * @see #combine(Future, Future)
     * @see #combine(Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future, Future)
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <T4> the result type of the fourth future.
     * @param <T5> the result type of the fifth future.
     * @param cf1 the first future.
     * @param cf2 the second future.
     * @param cf3 the third future.
     * @param cf4 the fourth future.
     * @param cf5 the fifth future.
     * @return a ContinuableFuture containing a Tuple5 with all five results.
     * @see #combine(Future, Future)
     * @see #combine(Future, Future, Future)
     * @see #combine(Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future, Future)
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <T4> the result type of the fourth future.
     * @param <T5> the result type of the fifth future.
     * @param <T6> the result type of the sixth future.
     * @param cf1 the first future.
     * @param cf2 the second future.
     * @param cf3 the third future.
     * @param cf4 the fourth future.
     * @param cf5 the fifth future.
     * @param cf6 the sixth future.
     * @return a ContinuableFuture containing a Tuple6 with all six results.
     * @see #combine(Future, Future)
     * @see #combine(Future, Future, Future)
     * @see #combine(Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future, Future)
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <T4> the result type of the fourth future.
     * @param <T5> the result type of the fifth future.
     * @param <T6> the result type of the sixth future.
     * @param <T7> the result type of the seventh future.
     * @param cf1 the first future.
     * @param cf2 the second future.
     * @param cf3 the third future.
     * @param cf4 the fourth future.
     * @param cf5 the fifth future.
     * @param cf6 the sixth future.
     * @param cf7 the seventh future.
     * @return a ContinuableFuture containing a Tuple7 with all seven results.
     * @see #combine(Future, Future)
     * @see #combine(Future, Future, Future)
     * @see #combine(Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future)
     * @see #combine(Future, Future, Future, Future, Future, Future)
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <R> the result type after applying the action.
     * @param cf1 the first future.
     * @param cf2 the second future.
     * @param action the function to apply to both results. Receives the actual values (not futures).
     * @return a ContinuableFuture containing the result of applying the action to both values.
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <R> the result type after applying the action.
     * @param cf1 the first future.
     * @param cf2 the second future.
     * @param cf3 the third future.
     * @param action the function to apply to all three results.
     * @return a ContinuableFuture containing the result of applying the action.
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the input futures.
     * @param <R> the result type after applying the action.
     * @param cfs the collection of futures to combine.
     * @param action the function to apply to the list of results.
     * @return a ContinuableFuture containing the result of the action.
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the futures.
     * @param cfs the array of futures to wait for.
     * @return a ContinuableFuture that completes with a list of all results when all input
     *         futures complete successfully.
     * @throws IllegalArgumentException if the array is {@code null} or empty.
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the futures.
     * @param cfs the collection of futures to wait for.
     * @return a ContinuableFuture that completes with a list of all results.
     * @throws IllegalArgumentException if the collection is {@code null} or empty.
     */
    public static <T> ContinuableFuture<List<T>> allOf(final Collection<? extends Future<? extends T>> cfs) {
        return allOf2(cfs);
    }

    private static <T> ContinuableFuture<List<T>> allOf2(final Collection<? extends Future<? extends T>> cfs) {
        N.checkArgument(N.notEmpty(cfs), "The specified collection cannot be null or empty");

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning);   //NOSONAR
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
     * Creates a ContinuableFuture that implements the "any of" semantics by returning the result of the first
     * future to complete successfully. If all futures complete exceptionally, the returned future completes
     * exceptionally with a composite exception containing all failures.
     *
     * <p>This method is useful for scenarios where you have multiple ways to get a result
     * and want to use whichever completes first, such as querying multiple replicas or
     * implementing timeouts with fallbacks.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Future<Data> primarySource = fetchFromPrimary();
     * Future<Data> secondarySource = fetchFromSecondary();
     * Future<Data> cacheSource = fetchFromCache();
     *
     * ContinuableFuture<Data> firstAvailable = Futures.anyOf(
     *     cacheSource, primarySource, secondarySource
     * );
     *
     * Data result = firstAvailable.get();   // Gets the fastest result
     * }</pre>
     *
     * @param <T> the result type of the futures.
     * @param cfs the array of futures to race.
     * @return a ContinuableFuture that completes with the first successful result.
     * @throws IllegalArgumentException if the array is {@code null} or empty.
     */
    @SafeVarargs
    public static <T> ContinuableFuture<T> anyOf(final Future<? extends T>... cfs) {
        return anyOf2(Arrays.asList(cfs));
    }

    /**
     * Creates a ContinuableFuture that implements the "any of" semantics by returning the result of the first
     * future to complete successfully. If all futures complete exceptionally, the returned future completes
     * exceptionally with a composite exception containing all failures.
     *
     * <p>This is particularly useful for implementing timeout patterns, redundancy, or
     * getting the fastest response from multiple sources.
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the futures.
     * @param cfs the collection of futures to race.
     * @return a ContinuableFuture that completes with the first successful result.
     * @throws IllegalArgumentException if the collection is {@code null} or empty.
     */
    public static <T> ContinuableFuture<T> anyOf(final Collection<? extends Future<? extends T>> cfs) {
        return anyOf2(cfs);
    }

    private static <T> ContinuableFuture<T> anyOf2(final Collection<? extends Future<? extends T>> cfs) {
        N.checkArgument(N.notEmpty(cfs), "The specified collection cannot be null or empty");

        return ContinuableFuture.wrap(new Future<>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : cfs) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning);   //NOSONAR
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
            public T get() {
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
            public T get(final long timeout, final TimeUnit unit) {
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the futures.
     * @param cfs the array of futures to iterate over.
     * @return an iterator that yields results in completion order (first-finished, first-out).
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the futures.
     * @param cfs the collection of futures to iterate over.
     * @return an iterator that yields results in completion order (first-finished, first-out).
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the futures.
     * @param cfs the collection of futures to iterate over.
     * @param totalTimeoutForAll the maximum time to wait for all results.
     * @param unit the time unit of the timeout.
     * @return an iterator that yields results in completion order (first-finished, first-out) with timeout enforcement.
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the input futures.
     * @param <R> the result type after transformation.
     * @param cfs the collection of futures to iterate over.
     * @param resultHandler the function to transform Result objects into desired output type.
     * @return an iterator that yields transformed results in completion order (first-finished, first-out).
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
     * <p><b>Usage Examples:</b></p>
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
     * @param <T> the result type of the input futures.
     * @param <R> the result type after transformation.
     * @param cfs the collection of futures to iterate over.
     * @param totalTimeoutForAll the maximum time to wait for all results.
     * @param unit the time unit of the timeout.
     * @param resultHandler the function to transform Result objects, including timeout handling.
     * @return an iterator that yields transformed results in completion order (first-finished, first-out) with timeout enforcement.
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

    /**
     * Converts an ExecutionException to its underlying cause exception if the cause is an Exception.
     * This utility method unwraps ExecutionExceptions to provide cleaner exception handling in
     * future composition operations. If the exception is not an ExecutionException or its cause
     * is not an Exception, the original exception is returned unchanged.
     *
     * <p>This method is used internally throughout the Futures utility to provide more meaningful
     * exception information when futures fail, avoiding nested ExecutionException wrappers that
     * can obscure the actual failure cause. It simplifies error handling by exposing the root
     * cause directly.
     *
     * <p><b>Conversion Rules:</b>
     * <ul>
     *   <li>If {@code e} is an {@link ExecutionException} and its cause is an Exception, returns the cause</li>
     *   <li>If {@code e} is an {@link ExecutionException} but its cause is not an Exception (e.g., Error), returns {@code e} unchanged</li>
     *   <li>If {@code e} is not an {@link ExecutionException}, returns {@code e} unchanged</li>
     *   <li>If {@code e} is {@code null}, behavior is undefined (should not occur in normal usage)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example 1: ExecutionException with Exception cause
     * ExecutionException ex1 = new ExecutionException(new IOException("Network error"));
     * Exception converted1 = Futures.convertException(ex1);
     * // converted1 is the IOException, not the ExecutionException
     *
     * // Example 2: ExecutionException with Error cause (not converted)
     * ExecutionException ex2 = new ExecutionException(new OutOfMemoryError());
     * Exception converted2 = Futures.convertException(ex2);
     * // converted2 is still the ExecutionException (cause is not an Exception)
     *
     * // Example 3: Non-ExecutionException (passed through)
     * RuntimeException ex3 = new IllegalArgumentException("Invalid argument");
     * Exception converted3 = Futures.convertException(ex3);
     * // converted3 is the same IllegalArgumentException
     *
     * // Internal usage in Futures methods
     * try {
     *     future.get();
     * } catch (Exception e) {
     *     Exception unwrapped = Futures.convertException(e);
     *     // unwrapped is now the actual cause, not the ExecutionException wrapper
     *     handleException(unwrapped);
     * }
     * }</pre>
     *
     * @param e the exception to convert, typically from a Future.get() call. Should not be {@code null}.
     * @return the unwrapped exception if {@code e} is an ExecutionException with an Exception cause,
     *         otherwise returns {@code e} unchanged. Never returns {@code null} if {@code e} is not {@code null}.
     * @see ExecutionException
     * @see Future#get()
     * @see Throwable#getCause()
     */
    static Exception convertException(final Exception e) {
        if (e instanceof ExecutionException && e.getCause() instanceof Exception ex) {
            return ex;
        }

        return e;
    }
}
