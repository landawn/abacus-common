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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
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
 * for coordinating already-started asynchronous operations, including result aggregation and
 * completion-order processing. Iterators returned by this class are intended for a single consumer.
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
 *   <li><b>Result Coordination:</b> {@code allOf()} methods for collecting results in input order</li>
 *   <li><b>Successful Races:</b> {@code anyOf()} methods for processing the first successfully completed future</li>
 *   <li><b>Completion Iteration:</b> Iterator-based access to futures as they complete (first-finished, first-out)</li>
 *   <li><b>Timeout Management:</b> Built-in timeout support for preventing indefinite blocking operations</li>
 *   <li><b>Error Handling:</b> Exception propagation, with failure aggregation for {@code anyOf()}</li>
 *   <li><b>Type Safety:</b> Strong generic typing maintained throughout all composition operations</li>
 * </ul>
 *
 * <p><b>{@code compose} vs {@code combine} (do not confuse):</b>
 * <ul>
 *   <li><b>{@code compose(...)}:</b> the zip/action function receives the {@link Future} handles themselves
 *       (call {@code get()} inside when you need values). Use this for conditional retrieval, short-circuiting,
 *       or custom timeout strategies against the futures.</li>
 *   <li><b>{@code combine(...)}:</b> the action receives the <b>unwrapped result values</b> after the inputs
 *       complete (or packs them into a {@code Tuple}). Prefer this for ordinary map/aggregate of completed results.</li>
 * </ul>
 * Many lambda shapes compile for both; picking the wrong family silently changes whether you work with futures or values.
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Simplicity Over Complexity:</b> Intuitive API that handles complex concurrency patterns transparently</li>
 *   <li><b>Type Safety First:</b> Strong generic typing prevents runtime errors and improves code clarity</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms minimizing overhead in multi-future operations</li>
 *   <li><b>Error Resilience:</b> Robust exception handling with proper propagation and aggregation</li>
 *   <li><b>Input Snapshots:</b> Collection-based methods capture membership and iteration order when called</li>
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
 *   <li><b>Composition Methods:</b> {@code compose()} - Custom zip functions that operate on the {@code Future}
 *       objects themselves (calling {@code get()} within the zip function), with optional separate handling for
 *       {@code get(timeout, unit)}</li>
 *   <li><b>Combination Methods:</b> {@code combine()} - Combine the <i>completed results</i> of several futures,
 *       either into a {@code Tuple2}..{@code Tuple7} or via a supplied function</li>
 *   <li><b>Coordination Methods:</b> {@code allOf()}, {@code anyOf()} - Coordinate futures without starting their work</li>
 *   <li><b>Iteration Methods:</b> {@code iterate()} - Process futures as they complete with optional timeouts</li>
 *   <li><b>Tuple Methods:</b> Direct combination into Tuple2 through Tuple7 for structured results</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Coordinate independently started operations and retrieve all results
 * Future<User> userFuture = userService.fetchUser(userId);
 * Future<List<Order>> ordersFuture = orderService.fetchOrders(userId);
 * Future<Profile> profileFuture = profileService.fetchProfile(userId);
 *
 * ContinuableFuture<List<Object>> allResults = Futures.allOf(userFuture, ordersFuture, profileFuture);
 * List<Object> results = allResults.get();   // returns [User, List<Order>, Profile]
 *
 * // Structured result combination using Tuples
 * ContinuableFuture<Tuple3<User, List<Order>, Profile>> structuredResult =
 *     Futures.combine(userFuture, ordersFuture, profileFuture, Tuple::of);
 * Tuple3<User, List<Order>, Profile> data = structuredResult.get();
 *
 * // Race condition - process the first successful result
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
 *   <li><b>Combination Construction:</b> O(1) for fixed-arity tuple combinations, O(n) for collection-based operations</li>
 *   <li><b>Memory Usage:</b> O(n) for captured input lists and completion-relay structures</li>
 *   <li><b>Thread Safety:</b> Static factory calls share no mutable state; input futures retain their own concurrency contracts</li>
 *   <li><b>Completion Detection:</b> Optimized algorithms for detecting future completion states</li>
 *   <li><b>Iterator Behavior:</b> Results are consumed lazily, while all input futures are registered when the iterator is created</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrency:</b>
 * <ul>
 *   <li><b>Static Methods:</b> Factory calls do not mutate shared class state</li>
 *   <li><b>Input Contracts:</b> Operations delegate to input futures, whose own thread-safety guarantees still apply</li>
 *   <li><b>Iterators:</b> Returned iterators have mutable cursor state and should be consumed by one thread</li>
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
 * <p><b>Composite cancellation:</b> {@code cancel} attempts every input and returns {@code true} only
 * if every input accepts cancellation; {@code false} does not mean that no inputs were cancelled.
 * If cancellation throws a runtime exception, later inputs are still attempted and distinct later exceptions are suppressed
 * on the first. {@code allOf} (and {@code combine}) reports cancellation once all inputs are done and
 * at least one was cancelled; {@code anyOf} requires every input to be cancelled. {@code compose}
 * reports cancellation only after its own successful {@code cancel} call, since its function may ignore
 * cancelled inputs. Cancelling a composite also affects inputs shared with other consumers.
 *
 * <p><b>Iterator interruption:</b> interruption while waiting for the next result restores the
 * consumer thread's interrupt flag and ends iteration with one final failure. A result handler receives
 * that {@link InterruptedException}; an iterator without a handler wraps it in a runtime exception.
 * Pending relay tasks are released without cancelling the input futures.
 *
 * <p><b>Usage Examples: Microservice Orchestration</b></p>
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
 *         // Step 3: Process order if all validations pass.
 *         // ContinuableFuture exposes thenCallAsync(Function) for chaining a follow-up async stage.
 *         return validations.thenCallAsync(result -> {
 *             if (!result._2) {
 *                 throw new OrderProcessingException("Insufficient inventory");
 *             }
 *
 *             // Parallel processing
 *             Future<Payment> paymentProcessing = paymentService.processPayment(result._3, request.getAmount());
 *             Future<Shipment> shippingArrangement = shippingService.arrangeShipping(request.getShippingAddress());
 *
 *             return Futures.combine(paymentProcessing, shippingArrangement,
 *                 (payment, shipment) -> new OrderResult(request.getOrderId(), payment.getId(), shipment.getTrackingNumber()))
 *                 .get(); // unwrap nested ContinuableFuture
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
 *   <li><b>vs. CompletableFuture.allOf():</b> typed {@code List<T>} results vs. {@code CompletableFuture<Void>} requiring separate per-future result retrieval</li>
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
 * @see java.util.function.Function
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Future.html">Future Documentation</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html">CompletableFuture Documentation</a>
 */
public final class Futures {

    private static final AtomicLong RELAY_THREAD_INDEX = new AtomicLong();

    /**
     * Dedicated pool for the relay tasks created by {@code iterate02(...)} and {@code anyOf2(...)}.
     * Those tasks block in {@code Future.get()} and publish to an iterator queue or a shared aggregate
     * outcome, so they must never share a pool with the tasks that consume their results.
     *
     * <p>{@code N.ASYNC_EXECUTOR} is backed by an unbounded queue, and a {@link ThreadPoolExecutor}
     * only creates threads beyond its core size once its queue is full - so that pool never grows past
     * its core size. Running the relays there deadlocked permanently as soon as core-many consumers
     * (for example {@code ContinuableFuture#runAsyncAfterEither} continuations, which run on that same
     * pool and then block waiting for these relays) were in flight: every thread was parked waiting for
     * a relay that could never be scheduled. This pool uses direct hand-off, so submitting a relay
     * always starts or reuses a thread immediately and can never queue behind its own consumer.
     * Threads are daemon (they must not keep the JVM alive) and idle out after 60 seconds.</p>
     */
    private static final Executor RELAY_EXECUTOR = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
        final Thread t = new Thread(r, "abacus-futures-relay-" + RELAY_THREAD_INDEX.incrementAndGet());
        t.setDaemon(true);
        return t;
    });

    private Futures() {
        // Utility class - prevent instantiation
    }

    /**
     * Releases the background relay tasks still owned by an iterator returned by {@code iterate(...)},
     * for a consumer that deliberately stops early (for example an "either"/"first success" combinator
     * that only needs the first one or two outcomes). Without this, each abandoned input keeps a relay
     * thread blocked in {@code Future.get()} until that input completes on its own.
     *
     * @param iter an iterator previously returned by one of the {@code iterate(...)} methods; anything
     *             else (including {@code null}) is ignored
     */
    static void cancelPendingRelays(final Iterator<?> iter) {
        if (iter instanceof ResultIterator) {
            ((ResultIterator<?>) iter).cancelPending();
        }
    }

    /**
     * An {@link ObjIterator} over future outcomes that owns background relay tasks.
     *
     * @param <R> the element type produced by the result handler
     */
    abstract static class ResultIterator<R> extends ObjIterator<R> {

        /** Best-effort cancellation of the relay tasks whose results have not been consumed yet. */
        abstract void cancelPending();
    }

    /**
     * Composes two futures into a new ContinuableFuture by applying a zip function to the Future objects themselves.
     * This method allows you to create custom logic that operates on the Future objects directly, enabling
     * advanced composition patterns. The zip function receives the Future objects and can call get() on them
     * to retrieve their values.
     *
     * <p><b>Vs {@link #combine(Future, Future, Throwables.BiFunction)}:</b> {@code compose} passes
     * <b>{@code Future} handles</b> into the function; {@code combine} waits for completion and passes
     * <b>unwrapped values</b>. Prefer {@code combine} for simple value aggregation; use {@code compose}
     * when you need conditional {@code get()}, short-circuiting, or custom timeout handling on the futures.</p>
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
     * System.out.println(sum.get());   // prints 15
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
     *             return Data.DEFAULT;
     *         }
     *     });
     * }</pre>
     *
     * <p>The returned future's get methods propagate InterruptedException, ExecutionException and
     * CancellationException directly. Other exceptions from the zip function are wrapped in
     * ExecutionException. The timed get method also propagates TimeoutException directly.</p>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <R> the result type of the composed future.
     * @param cf1 the first future to compose, must not be {@code null}.
     * @param cf2 the second future to compose, must not be {@code null}.
     * @param zipFunctionForGet the function that combines the futures' results. Receives both Future objects
     *                         as parameters and returns the composed result.
     * @return a lazy {@code ContinuableFuture} whose {@code get()} invokes the supplied zip function;
     *         the function decides whether and how to wait for the inputs, while {@code isDone()} is
     *         {@code true} only when both input futures are done.
     * @throws IllegalArgumentException if {@code zipFunctionForGet} is {@code null}.
     * @see #compose(Future, Future, Throwables.BiFunction, Throwables.Function)
     * @see #combine(Future, Future, Throwables.BiFunction)
     * @see ContinuableFuture
     */
    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Throwables.BiFunction<? super Future<T1>, ? super Future<T2>, ? extends R, ? extends Exception> zipFunctionForGet)
            throws IllegalArgumentException {
        N.checkArgNotNull(zipFunctionForGet, cs.zipFunctionForGet);

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
     * <p>The returned future's get methods propagate InterruptedException, ExecutionException and
     * CancellationException directly. Other exceptions from the zip function are wrapped in
     * ExecutionException. The timed get method also propagates TimeoutException directly.</p>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <R> the result type of the composed future.
     * @param cf1 the first future to compose, must not be {@code null}.
     * @param cf2 the second future to compose, must not be {@code null}.
     * @param zipFunctionForGet the function that combines the futures' results for regular get() operations.
     *                         Receives both Future objects.
     * @param zipFunctionTimeoutGet the function for get(timeout, unit) operations. Receives a Tuple4 containing:
     *                              (_1: future1, _2: future2, _3: timeout value, _4: TimeUnit)
     *
     * @return a ContinuableFuture with custom logic for both regular and timeout operations.
     * @throws IllegalArgumentException if any of {@code zipFunctionForGet}, {@code zipFunctionTimeoutGet} is
     *         {@code null}.
     * @see #compose(Future, Future, Throwables.BiFunction)
     * @see Tuple4
     * @see TimeUnit
     */
    public static <T1, T2, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2,
            final Throwables.BiFunction<? super Future<T1>, ? super Future<T2>, ? extends R, ? extends Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple4<Future<T1>, Future<T2>, Long, TimeUnit>, R, ? extends Exception> zipFunctionTimeoutGet)
            throws IllegalArgumentException {
        N.checkArgNotNull(zipFunctionForGet, cs.zipFunctionForGet);
        N.checkArgNotNull(zipFunctionTimeoutGet, cs.zipFunctionTimeoutGet);

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
     * System.out.println(profile.get());   // prints "John, 30 years old, from New York"
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
     * <p>The returned future's get methods propagate InterruptedException, ExecutionException and
     * CancellationException directly. Other exceptions from the zip function are wrapped in
     * ExecutionException. The timed get method also propagates TimeoutException directly.</p>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <R> the result type of the composed future.
     * @param cf1 the first future to compose, must not be {@code null}.
     * @param cf2 the second future to compose, must not be {@code null}.
     * @param cf3 the third future to compose, must not be {@code null}.
     * @param zipFunctionForGet the function that combines the futures' results. Receives all three Future objects
     *                         and returns the composed result.
     * @return a lazy {@code ContinuableFuture} whose {@code get()} invokes the supplied zip function;
     *         the function decides whether and how to wait for the inputs, while {@code isDone()} is
     *         {@code true} only when all three input futures are done.
     * @throws IllegalArgumentException if {@code zipFunctionForGet} is {@code null}.
     * @see #compose(Future, Future, Future, Throwables.TriFunction, Throwables.Function)
     * @see #combine(Future, Future, Future, Throwables.TriFunction)
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Throwables.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, ? extends R, ? extends Exception> zipFunctionForGet)
            throws IllegalArgumentException {
        N.checkArgNotNull(zipFunctionForGet, cs.zipFunctionForGet);

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
     * <p>The returned future's get methods propagate InterruptedException, ExecutionException and
     * CancellationException directly. Other exceptions from the zip function are wrapped in
     * ExecutionException. The timed get method also propagates TimeoutException directly.</p>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <R> the result type of the composed future.
     * @param cf1 the first future to compose, must not be {@code null}.
     * @param cf2 the second future to compose, must not be {@code null}.
     * @param cf3 the third future to compose, must not be {@code null}.
     * @param zipFunctionForGet the function that combines the futures' results for regular get() operations.
     *                         Receives all three Future objects.
     * @param zipFunctionTimeoutGet the function for get(timeout, unit) operations. Receives a Tuple5 containing:
     *                              (_1: future1, _2: future2, _3: future3, _4: timeout value, _5: TimeUnit)
     *
     * @return a ContinuableFuture with custom logic for both regular and timeout operations.
     * @throws IllegalArgumentException if any of {@code zipFunctionForGet}, {@code zipFunctionTimeoutGet} is
     *         {@code null}.
     * @see #compose(Future, Future, Future, Throwables.TriFunction)
     * @see Tuple5
     * @see TimeUnit
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> compose(final Future<T1> cf1, final Future<T2> cf2, final Future<T3> cf3,
            final Throwables.TriFunction<? super Future<T1>, ? super Future<T2>, ? super Future<T3>, ? extends R, ? extends Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple5<Future<T1>, Future<T2>, Future<T3>, Long, TimeUnit>, R, ? extends Exception> zipFunctionTimeoutGet)
            throws IllegalArgumentException {
        N.checkArgNotNull(zipFunctionForGet, cs.zipFunctionForGet);
        N.checkArgNotNull(zipFunctionTimeoutGet, cs.zipFunctionTimeoutGet);

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
     * <p>Membership is captured in an immutable list in encounter order. Both callbacks and lifecycle
     * methods use that snapshot, including duplicate and null handles. Later changes to the source
     * collection have no effect. Callbacks needing mutable working storage must copy the list.</p>
     *
     * <p>This overload uses the same function for both regular get() and timeout-based get() operations.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Dynamic Size:</b> Works with any number of futures, determined at runtime</li>
     *   <li><b>Stable Membership:</b> Captures an immutable list in encounter order</li>
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
     *     for (Future<? extends Integer> f : list) {
     *         total += f.get();
     *     }
     *     return total;
     * });
     *
     * System.out.println(sum.get());   // prints 6
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
     *         for (Future<? extends DataPoint> f : futures) {
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
     *         for (Future<? extends String> f : futureSet) {
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
     *         for (Future<? extends Double> f : futures) {
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
     * <p>The returned future's get methods propagate InterruptedException, ExecutionException and
     * CancellationException directly. Other exceptions from the zip function are wrapped in
     * ExecutionException. The timed get method also propagates TimeoutException directly.</p>
     *
     * @param <T> the result type of the input futures.
     * @param <R> the result type of the composed future.
     * @param cfs the collection of input futures, must not be {@code null} or empty.
     * @param zipFunctionForGet the function that combines the futures' results. Receives an immutable list snapshot
     *                         of Future objects in encounter order and returns the composed result.
     * @return a lazy {@code ContinuableFuture} whose {@code get()} invokes the supplied zip function;
     *         the function decides whether and how to wait for the inputs, while {@code isDone()} is
     *         {@code true} only when all input futures are done.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty, or {@code zipFunctionForGet} is {@code null}.
     * @see #compose(Collection, Throwables.Function, Throwables.Function)
     * @see #combine(Collection, Throwables.Function)
     */
    public static <T, R> ContinuableFuture<R> compose(final Collection<? extends Future<? extends T>> cfs,
            final Throwables.Function<? super List<Future<? extends T>>, ? extends R, ? extends Exception> zipFunctionForGet) throws IllegalArgumentException {
        N.checkArgNotNull(zipFunctionForGet, cs.zipFunctionForGet);

        return compose(cfs, zipFunctionForGet, t -> zipFunctionForGet.apply(t._1));
    }

    /**
     * Composes a collection of futures with separate functions for regular and timeout operations.
     * This is the most flexible composition method, supporting any number of futures with custom
     * timeout handling. The timeout function receives a Tuple3 containing the immutable encounter-order
     * list snapshot, timeout value,
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
     *   <li><b>Stable Membership:</b> Captures an immutable list in encounter order</li>
     *   <li><b>Best-Effort Processing:</b> Supports gathering available results within time constraints</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Basic partial aggregation under timeout
     * Set<Future<DataPoint>> dataFutures = collectDataPointFutures();
     *
     * ContinuableFuture<Summary> summary = Futures.compose(dataFutures,
     *     futures -> {
     *         // Full aggregation when we have time
     *         List<DataPoint> allData = new ArrayList<>();
     *         for (Future<? extends DataPoint> f : futures) {
     *             allData.add(f.get());
     *         }
     *         return computeFullSummary(allData);
     *     },
     *     tuple -> {
     *         // Quick summary using only completed futures
     *         List<DataPoint> available = new ArrayList<>();
     *         for (Future<? extends DataPoint> f : tuple._1) {
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
     *         for (Future<? extends QueryResult> f : futures) {
     *             results.add(f.get());
     *         }
     *         return AggregatedResult.complete(results);
     *     },
     *     // Time-budgeted collection
     *     tuple -> {
     *         List<Future<? extends QueryResult>> futures = tuple._1;
     *         long timeout = tuple._2;
     *         TimeUnit unit = tuple._3;
     *
     *         long timePerQuery = timeout / futures.size();
     *         List<QueryResult> collected = new ArrayList<>();
     *
     *         for (Future<? extends QueryResult> f : futures) {
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
     *         for (Future<? extends CacheEntry> f : futures) {
     *             CacheEntry e = f.get();
     *             if (e != null) return e;
     *         }
     *         return CacheEntry.MISS;
     *     },
     *     tuple -> {
     *         // Under timeout, check caches sequentially with time limit
     *         long remainingTime = tuple._3.toMillis(tuple._2);
     *         long startTime = System.currentTimeMillis();
     *
     *         for (Future<? extends CacheEntry> f : tuple._1) {
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
     * <p>The returned future's get methods propagate InterruptedException, ExecutionException and
     * CancellationException directly. Other exceptions from the zip function are wrapped in
     * ExecutionException. The timed get method also propagates TimeoutException directly.</p>
     *
     * @param <T> the result type of the input futures.
     * @param <R> the result type of the composed future.
     * @param cfs the collection of input futures, must not be {@code null} or empty.
     * @param zipFunctionForGet the function that combines the futures' results for regular get() operations.
     *                         Receives an immutable list snapshot of Future objects.
     * @param zipFunctionTimeoutGet the function for get(timeout, unit) operations. Receives a Tuple3 containing:
     *                              (_1: futures collection, _2: timeout value, _3: TimeUnit).
     * @return a ContinuableFuture with custom logic for both regular and timeout operations.
     * @throws IllegalArgumentException if the collection is {@code null} or empty, or if any of
     *         {@code zipFunctionForGet}, {@code zipFunctionTimeoutGet} is {@code null}.
     * @see #compose(Collection, Throwables.Function)
     * @see Tuple3
     * @see TimeUnit
     */
    public static <T, R> ContinuableFuture<R> compose(final Collection<? extends Future<? extends T>> cfs,
            final Throwables.Function<? super List<Future<? extends T>>, ? extends R, ? extends Exception> zipFunctionForGet,
            final Throwables.Function<? super Tuple3<List<Future<? extends T>>, Long, TimeUnit>, ? extends R, ? extends Exception> zipFunctionTimeoutGet)
            throws IllegalArgumentException {
        N.checkArgument(N.notEmpty(cfs), "The specified collection cannot be null or empty");
        N.checkArgNotNull(zipFunctionForGet, cs.zipFunctionForGet);
        N.checkArgNotNull(zipFunctionTimeoutGet, cs.zipFunctionTimeoutGet); //NOSONAR

        final Throwables.Function<? super List<Future<? extends T>>, ? extends R, Exception> zipFunctionForGetToUse = (Throwables.Function<? super List<Future<? extends T>>, ? extends R, Exception>) zipFunctionForGet;

        final Throwables.Function<? super Tuple3<List<Future<? extends T>>, Long, TimeUnit>, ? extends R, Exception> zipFunctionTimeoutGetToUse = (Throwables.Function<? super Tuple3<List<Future<? extends T>>, Long, TimeUnit>, ? extends R, Exception>) zipFunctionTimeoutGet;
        // Keep encounter order and duplicate/null handles while preventing either caller or callback
        // mutations from changing the computation's membership. Null handles retain their legacy behavior.
        final List<Future<? extends T>> futures = java.util.Collections.unmodifiableList(new ArrayList<>(cfs));
        final AtomicBoolean cancelled = new AtomicBoolean();

        return ContinuableFuture.wrap(new Future<>() {
            /**
             * {@inheritDoc}
             * @throws RuntimeException if cancellation of an input future throws a runtime exception; subsequent cancellation failures are suppressed on the first.
             */
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) throws RuntimeException {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : futures) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            addSuppressedIfDistinct(exception, e);
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                if (res) {
                    cancelled.set(true);
                }

                return res;
            }

            @Override
            public boolean isCancelled() {
                // A compose function may ignore cancelled inputs and still return successfully, so
                // cancellation is a property of this composite's successful cancel(...) call.
                return cancelled.get();
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a {@code null} input future is reached while examining completion or cancellation.
             */
            @Override
            public boolean isDone() throws NullPointerException {
                if (cancelled.get()) {
                    return true;
                }

                for (final Future<?> future : futures) {
                    if (!future.isDone()) {
                        return false;
                    }
                }

                return true;
            }

            /**
             * {@inheritDoc}
             * @throws CancellationException if this composed future has been cancelled or the result-combining callback reports cancellation.
             * @throws InterruptedException if the wait or result-combining callback is interrupted.
             * @throws ExecutionException if an input computation or result-combining callback fails.
             */
            @Override
            public R get() throws CancellationException, InterruptedException, ExecutionException {
                if (cancelled.get()) {
                    throw new CancellationException();
                }

                try {
                    return zipFunctionForGetToUse.apply(futures);
                } catch (final InterruptedException | ExecutionException e) {
                    throw e;
                } catch (final CancellationException e) {
                    // Future contract: cancellation is signalled unchecked, never via ExecutionException.
                    throw e;
                } catch (final Exception e) {
                    // The zip function IS this future's computation, so a failure inside it has to reach
                    // the caller as ExecutionException. Wrapping it in a RuntimeException instead meant a
                    // standard `catch (ExecutionException)` around get() never saw it.
                    throw new ExecutionException(e);
                }
            }

            /**
             * {@inheritDoc}
             * @throws CancellationException if this composed future has been cancelled or the result-combining callback reports cancellation.
             * @throws InterruptedException if the wait or result-combining callback is interrupted.
             * @throws TimeoutException if the requested wait expires before a result can be obtained.
             * @throws ExecutionException if an input computation or result-combining callback fails.
             */
            @Override
            public R get(final long timeout, final TimeUnit unit) throws CancellationException, InterruptedException, TimeoutException, ExecutionException {
                if (cancelled.get()) {
                    throw new CancellationException();
                }

                final Tuple3<List<Future<? extends T>>, Long, TimeUnit> t = Tuple.of(futures, timeout, unit);

                try {
                    return zipFunctionTimeoutGetToUse.apply(t);
                } catch (final InterruptedException | ExecutionException | TimeoutException e) {
                    throw e;
                } catch (final CancellationException e) {
                    // Future contract: cancellation is signalled unchecked, never via ExecutionException.
                    throw e;
                } catch (final Exception e) {
                    // See get(): a zip-function failure is a computation failure of this future.
                    throw new ExecutionException(e);
                }
            }
        });
    }

    /**
     * Combines two futures into a Tuple2 containing both results.
     * On a successful {@code get()}, both results are packaged into a tuple in input order. If an
     * earlier input fails, that failure can be reported without waiting for a later input.
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
     * combined.thenRunAsync(tuple -> {
     *     User user = tuple._1;
     *     List<Order> orders = tuple._2;
     *     displayUserProfile(user, orders);
     * });
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param cf1 the first future, must not be {@code null}.
     * @param cf2 the second future, must not be {@code null}.
     * @return a {@code ContinuableFuture} whose successful result is a {@code Tuple2} holding both
     *         results in input order. {@code get()} may throw {@link InterruptedException} or
     *         {@link ExecutionException} before a later input completes if an earlier input fails.
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
     * @param cf1 the first future, must not be {@code null}.
     * @param cf2 the second future, must not be {@code null}.
     * @param cf3 the third future, must not be {@code null}.
     * @return a {@code ContinuableFuture} whose successful result is a {@code Tuple3} holding all
     *         three results in input order. {@code get()} may fail before a later input completes.
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
     * deps.thenRunAsync(tuple -> {
     *     initializeApplication(tuple._1, tuple._2, tuple._3, tuple._4);
     * });
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <T4> the result type of the fourth future.
     * @param cf1 the first future, must not be {@code null}.
     * @param cf2 the second future, must not be {@code null}.
     * @param cf3 the third future, must not be {@code null}.
     * @param cf4 the fourth future, must not be {@code null}.
     * @return a {@code ContinuableFuture} whose successful result is a {@code Tuple4} holding all
     *         four results in input order. {@code get()} may fail before a later input completes.
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
     * allData.thenRunAsync(data -> renderDashboard(data));
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <T4> the result type of the fourth future.
     * @param <T5> the result type of the fifth future.
     * @param cf1 the first future, must not be {@code null}.
     * @param cf2 the second future, must not be {@code null}.
     * @param cf3 the third future, must not be {@code null}.
     * @param cf4 the fourth future, must not be {@code null}.
     * @param cf5 the fifth future, must not be {@code null}.
     * @return a {@code ContinuableFuture} whose successful result is a {@code Tuple5} holding all
     *         five results in input order. {@code get()} may fail before a later input completes.
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
     * system.thenRunAsync(components -> startApplication(components));
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <T4> the result type of the fourth future.
     * @param <T5> the result type of the fifth future.
     * @param <T6> the result type of the sixth future.
     * @param cf1 the first future, must not be {@code null}.
     * @param cf2 the second future, must not be {@code null}.
     * @param cf3 the third future, must not be {@code null}.
     * @param cf4 the fourth future, must not be {@code null}.
     * @param cf5 the fifth future, must not be {@code null}.
     * @param cf6 the sixth future, must not be {@code null}.
     * @return a {@code ContinuableFuture} whose successful result is a {@code Tuple6} holding all
     *         six results in input order. {@code get()} may fail before a later input completes.
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
     * fullProfile.thenRunAsync(data -> generateComprehensiveReport(data));
     * }</pre>
     *
     * @param <T1> the result type of the first future.
     * @param <T2> the result type of the second future.
     * @param <T3> the result type of the third future.
     * @param <T4> the result type of the fourth future.
     * @param <T5> the result type of the fifth future.
     * @param <T6> the result type of the sixth future.
     * @param <T7> the result type of the seventh future.
     * @param cf1 the first future, must not be {@code null}.
     * @param cf2 the second future, must not be {@code null}.
     * @param cf3 the third future, must not be {@code null}.
     * @param cf4 the fourth future, must not be {@code null}.
     * @param cf5 the fifth future, must not be {@code null}.
     * @param cf6 the sixth future, must not be {@code null}.
     * @param cf7 the seventh future, must not be {@code null}.
     * @return a {@code ContinuableFuture} whose successful result is a {@code Tuple7} holding all
     *         seven results in input order. {@code get()} may fail before a later input completes.
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
     * A successful {@code get()} retrieves both results in input order and applies the provided
     * function. If the first input fails, that failure can be reported without waiting for the second.
     *
     * <p><b>Vs {@link #compose(Future, Future, Throwables.BiFunction)}:</b> {@code combine} passes
     * <b>unwrapped values</b> into {@code action}; {@code compose} passes the <b>{@code Future}
     * handles</b> so the function controls when/whether to call {@code get()}. Prefer this method for
     * ordinary value aggregation.</p>
     *
     * <p><b>The action runs lazily, on every {@code get()}.</b> It is applied by
     * {@link ContinuableFuture#map(Throwables.Function)} inside each call to {@code get()} on the returned
     * future - on the calling thread, and again for each further call. {@code isDone()} therefore reports the
     * inputs' state, not the action's. Keep {@code action} pure, or call {@code get()} once and reuse the value.</p>
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
     * @param cf1 the first future, must not be {@code null}.
     * @param cf2 the second future, must not be {@code null}.
     * @param action the function to apply to both results. Receives the actual values (not the
     *               futures).
     * @return a {@code ContinuableFuture} whose successful result is produced by {@code action}
     *         after both results are retrieved. {@code get()} may fail before a later input completes;
     *         if {@code action} throws, {@code get()} reports it as an {@link ExecutionException} whose cause is
     *         that exception - checked or unchecked alike - per the {@link Future} contract.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @see #compose(Future, Future, Throwables.BiFunction)
     */
    public static <T1, T2, R> ContinuableFuture<R> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2,
            final Throwables.BiFunction<? super T1, ? super T2, ? extends R, ? extends Exception> action) throws IllegalArgumentException {
        N.checkArgNotNull(action, cs.action);

        return allOf(Arrays.asList(cf1, cf2)).map(t -> action.apply((T1) t.get(0), (T2) t.get(1)));
    }

    /**
     * Combines three futures and applies a tri-function to their results.
     * Similar to the two-argument version but for three futures. Waits for all three to complete
     * before applying the transformation function.
     *
     * <p><b>The action runs lazily, on every {@code get()}.</b> It is applied by
     * {@link ContinuableFuture#map(Throwables.Function)} inside each call to {@code get()} on the returned
     * future - on the calling thread, and again for each further call. {@code isDone()} therefore reports the
     * inputs' state, not the action's. Keep {@code action} pure, or call {@code get()} once and reuse the value.</p>
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
     * @param cf1 the first future, must not be {@code null}.
     * @param cf2 the second future, must not be {@code null}.
     * @param cf3 the third future, must not be {@code null}.
     * @param action the function to apply to all three results. Receives the actual values (not
     *               the futures).
     * @return a {@code ContinuableFuture} whose successful result is produced by {@code action}
     *         after all three results are retrieved. {@code get()} may fail before a later input
     *         completes; if {@code action} throws, {@code get()} reports it as an {@link ExecutionException}
     *         whose cause is that exception - checked or unchecked alike - per the {@link Future} contract.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    public static <T1, T2, T3, R> ContinuableFuture<R> combine(final Future<? extends T1> cf1, final Future<? extends T2> cf2, final Future<? extends T3> cf3,
            final Throwables.TriFunction<? super T1, ? super T2, ? super T3, ? extends R, ? extends Exception> action) throws IllegalArgumentException {
        N.checkArgNotNull(action, cs.action);

        return allOf(Arrays.asList(cf1, cf2, cf3)).map(t -> action.apply((T1) t.get(0), (T2) t.get(1), (T3) t.get(2)));
    }

    /**
     * Combines a collection of futures and applies a function to all their results.
     * On a successful {@code get()}, this method retrieves all results in input iteration order
     * and applies the provided function. A failure is reported when encountered, without necessarily
     * waiting for later inputs. This is useful for aggregating a dynamic number of futures.
     *
     * <p><b>The action runs lazily, on every {@code get()}.</b> It is applied by
     * {@link ContinuableFuture#map(Throwables.Function)} inside each call to {@code get()} on the returned
     * future - on the calling thread, and again for each further call. {@code isDone()} therefore reports the
     * inputs' state, not the action's. Keep {@code action} pure, or call {@code get()} once and reuse the value.</p>
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
     * @param cfs the collection of futures to combine, must not be {@code null} or empty.
     * @param action the function to apply to the list of results, in iteration order of
     *               {@code cfs}.
     * @return a {@code ContinuableFuture} whose result is the value produced by {@code action}
     *         applied to the list of all completed results. Calling {@code get()} on it waits
     *         for all input futures and may throw {@link InterruptedException} or
     *         {@link ExecutionException}; if {@code action} throws, that same {@link ExecutionException} carries
     *         it as the cause - checked or unchecked alike - per the {@link Future} contract.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty, or if {@code action} is {@code null}.
     * @see #combine(Future, Future, Throwables.BiFunction)
     * @see #combine(Future, Future, Future, Throwables.TriFunction)
     */
    public static <T, R> ContinuableFuture<R> combine(final Collection<? extends Future<? extends T>> cfs,
            final Throwables.Function<List<T>, ? extends R, ? extends Exception> action) throws IllegalArgumentException {
        N.checkArgNotNull(action, cs.action);

        final ContinuableFuture<List<T>> f = allOf(cfs);
        return f.map(action);
    }

    //    public static <T, R> Future<R> combine(final List<? extends Future<? extends T>> cfs, final Try.Function<List<T>, ? extends R, ? extends Exception> action) {
    //        final Future<List<T>> future = allOf(cfs);
    //        return future.thenApply(action);
    //    }

    /**
     * Creates a {@code ContinuableFuture} whose {@code isDone()} reports {@code true} when all of
     * the given futures are done. {@code get()} retrieves results in input order and stops at the
     * first failure encountered, so a failure can be reported before later inputs complete.
     * A cancelled input throws {@link CancellationException} directly from {@code get()}, even while
     * the aggregate's {@code isDone()} and {@code isCancelled()} remain {@code false}.
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
     * allData.thenRunAsync(results -> {
     *     System.out.println("All services returned: " + results);
     * });
     * }</pre>
     *
     * @param <T> the result type of the futures.
     * @param cfs the array of futures to wait for, must not be {@code null} or empty.
     * @return a {@code ContinuableFuture} whose successful result lists all results in the same
     *         order as {@code cfs}. {@code get()} processes inputs in order and stops at the first
     *         failure; {@code get(timeout, unit)} may additionally throw {@link TimeoutException}.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     */
    @SafeVarargs
    public static <T> ContinuableFuture<List<T>> allOf(final Future<? extends T>... cfs) throws IllegalArgumentException {
        N.checkArgNotNull(cfs, cs.cfs);

        return allOf2(Arrays.asList(cfs));
    }

    /**
     * Creates a {@code ContinuableFuture} whose {@code isDone()} reports {@code true} when all
     * futures in the collection are done. {@code get()} retrieves results in input iteration order
     * and stops at the first failure encountered.
     * A cancelled input throws {@link CancellationException} directly from {@code get()}, even while
     * the aggregate's {@code isDone()} and {@code isCancelled()} remain {@code false}.
     *
     * <p>The returned future's list will have the same size as the input collection, with
     * results in corresponding positions. If any future fails, the returned future fails
     * with the first exception encountered.
     * The collection's membership and iteration order are captured when this method is called;
     * later structural changes to {@code cfs} do not affect the returned future.
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
     * allValidations.thenRunAsync(results -> {
     *     boolean allValid = results.stream()
     *         .allMatch(ValidationResult::isValid);
     *     if (allValid) {
     *         proceedWithRegistration();
     *     }
     * });
     * }</pre>
     *
     * @param <T> the result type of the futures.
     * @param cfs the collection of futures to wait for, must not be {@code null} or empty.
     * @return a {@code ContinuableFuture} whose successful result lists all results in iteration
     *         order of {@code cfs}. {@code get()} processes inputs in order and stops at the first
     *         failure; {@code get(timeout, unit)} may additionally throw {@link TimeoutException}.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     */
    public static <T> ContinuableFuture<List<T>> allOf(final Collection<? extends Future<? extends T>> cfs) throws IllegalArgumentException {
        return allOf2(cfs);
    }

    /**
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     */
    private static <T> ContinuableFuture<List<T>> allOf2(final Collection<? extends Future<? extends T>> cfs) throws IllegalArgumentException {
        N.checkArgument(N.notEmpty(cfs), "The specified collection cannot be null or empty");

        final List<Future<? extends T>> futures = new ArrayList<>(cfs);

        return ContinuableFuture.wrap(new Future<>() {
            /**
             * {@inheritDoc}
             * @throws RuntimeException if cancellation of an input future throws a runtime exception; subsequent cancellation failures are suppressed on the first.
             */
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) throws RuntimeException {
                boolean res = true;
                RuntimeException exception = null;

                for (final Future<? extends T> future : futures) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            addSuppressedIfDistinct(exception, e);
                        }
                    }
                }

                if (exception != null) {
                    throw exception;
                }

                return res;
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a {@code null} input future is reached while examining completion or cancellation.
             */
            @Override
            public boolean isCancelled() throws NullPointerException {
                // Future contract: isCancelled() implies isDone(). Report cancelled only when every
                // constituent is done and at least one was cancelled (composite cannot succeed).
                boolean sawCancelled = false;

                for (final Future<?> future : futures) {
                    if (!future.isDone()) {
                        return false;
                    }
                    if (future.isCancelled()) {
                        sawCancelled = true;
                    }
                }

                return sawCancelled;
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a {@code null} input future is reached while examining completion or cancellation.
             */
            @Override
            public boolean isDone() throws NullPointerException {
                for (final Future<?> future : futures) {
                    if (!future.isDone()) {
                        return false;
                    }
                }

                return true;
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a reached input future is {@code null}.
             * @throws CancellationException if an input future whose result is requested has been cancelled.
             * @throws InterruptedException if the caller is interrupted while waiting for an input result.
             * @throws ExecutionException if an input computation whose result is requested failed.
             */
            @Override
            public List<T> get() throws NullPointerException, CancellationException, InterruptedException, ExecutionException {
                final List<T> result = new ArrayList<>(futures.size());

                for (final Future<? extends T> future : futures) {
                    result.add(future.get());
                }

                return result;
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a reached input future is {@code null}, or {@code unit} is {@code null}.
             * @throws CancellationException if an input future whose result is requested has been cancelled.
             * @throws InterruptedException if the caller is interrupted while waiting for an input result.
             * @throws TimeoutException if the requested wait expires before a result can be obtained.
             * @throws ExecutionException if an input computation whose result is requested failed.
             */
            @Override
            public List<T> get(final long timeout, final TimeUnit unit)
                    throws NullPointerException, CancellationException, InterruptedException, TimeoutException, ExecutionException {
                final long timeoutInNanos = unit.toNanos(timeout);
                final long startTimeInNanos = System.nanoTime();

                final List<T> result = new ArrayList<>(futures.size());

                for (final Future<? extends T> future : futures) {
                    final long elapsedTimeInNanos = System.nanoTime() - startTimeInNanos;
                    final long remainingTimeInNanos = timeoutInNanos <= 0 ? 0 : N.max(0L, timeoutInNanos - elapsedTimeInNanos);
                    result.add(future.get(remainingTimeInNanos, TimeUnit.NANOSECONDS));
                }

                return result;
            }
        });
    }

    /**
     * Creates a ContinuableFuture that implements the "any of" semantics by returning the result of the first
     * successful result observed by the aggregate. The published result (including null) or terminal
     * failure is retained for every later get. When multiple inputs are already complete, observation
     * order decides the winner; completion history cannot be reconstructed from arbitrary Future handles.
     * Caller timeout and interruption do not publish a terminal outcome. If all futures complete exceptionally, {@code get()} throws an
     * {@link ExecutionException} whose cause is the first failure, with the remaining failures attached to
     * that cause as suppressed exceptions. If <i>every</i> input future was cancelled, {@code get()} instead
     * throws {@link CancellationException}, matching {@code isCancelled()}.
     * A timed {@code get(timeout, unit)} treats an input future's {@link TimeoutException} as an ordinary
     * computation failure and continues waiting for a successful sibling within the aggregate deadline.
     * Only expiration of that deadline is reported directly as {@code TimeoutException}.
     *
     * <p>Blocking getters share completion observations. CompletableFuture listeners are registered
     * once per input; other inputs may use relay tasks, released after publication or when the last waiting
     * caller leaves. Later calls can retry those relays without cancelling the input futures. Listener
     * registration or relay submission failures propagate to the caller and can also be retried.
     * {@code isDone()} returns immediately when all inputs report done; otherwise it polls for a
     * successful result. That poll, like a nonpositive-timeout {@code get}, may invoke a lazy mapper.
     * A mapper that ignores timeouts can block that poll; a failing mapper can run again on a later poll.
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
     * Data result = firstAvailable.get();   // gets the fastest result
     * }</pre>
     *
     * @param <T> the result type of the futures.
     * @param cfs the array of futures to race, must not be {@code null} or empty.
     * @return a {@code ContinuableFuture} whose {@code get()} returns the result of the first
     *         input success observed and published by the aggregate. If every input future fails, {@code get()}
     *         throws an {@link ExecutionException} whose cause is the first failure, with the
     *         remaining failures attached to that cause as suppressed exceptions; if every input
     *         future was cancelled, {@link CancellationException} is thrown instead. If the thread
     *         calling {@code get()} is interrupted while waiting, {@link InterruptedException} is thrown.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     */
    @SafeVarargs
    public static <T> ContinuableFuture<T> anyOf(final Future<? extends T>... cfs) throws IllegalArgumentException {
        N.checkArgNotNull(cfs, cs.cfs);

        return anyOf2(Arrays.asList(cfs));
    }

    /**
     * Creates a ContinuableFuture that implements the "any of" semantics by returning the result of the first
     * successful result observed by the aggregate. The published result (including null) or terminal
     * failure is retained for every later get. When multiple inputs are already complete, observation
     * order decides the winner; completion history cannot be reconstructed from arbitrary Future handles.
     * Caller timeout and interruption do not publish a terminal outcome. If all futures complete exceptionally, {@code get()} throws an
     * {@link ExecutionException} whose cause is the first failure, with the remaining failures attached to
     * that cause as suppressed exceptions. If <i>every</i> input future was cancelled, {@code get()} instead
     * throws {@link CancellationException}, matching {@code isCancelled()}.
     * A timed {@code get(timeout, unit)} treats an input future's {@link TimeoutException} as an ordinary
     * computation failure and continues waiting for a successful sibling within the aggregate deadline.
     * Only expiration of that deadline is reported directly as {@code TimeoutException}.
     *
     * <p>Blocking getters share completion observations. CompletableFuture listeners are registered
     * once per input; other inputs may use relay tasks, released after publication or when the last waiting
     * caller leaves. Later calls can retry those relays without cancelling the input futures. Listener
     * registration or relay submission failures propagate to the caller and can also be retried.
     * {@code isDone()} returns immediately when all inputs report done; otherwise it polls for a
     * successful result. That poll, like a nonpositive-timeout {@code get}, may invoke a lazy mapper.
     * A mapper that ignores timeouts can block that poll; a failing mapper can run again on a later poll.
     *
     * <p>This is particularly useful for implementing timeout patterns, redundancy, or
     * getting the fastest response from multiple sources.
     * The input collection's membership is captured when this method is called; later structural
     * changes to {@code cfs} do not affect the returned future.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Future<Price>> priceQueries = suppliers.stream()
     *     .map(supplier -> supplier.getQuote(item))
     *     .collect(Collectors.toList());
     *
     * ContinuableFuture<Price> firstQuote = Futures.anyOf(priceQueries);
     *
     * firstQuote.thenRunAsync(price -> {
     *     System.out.println("First price received: " + price);
     *     // Losing queries keep running. Cancel individual queries only if no other consumer needs them.
     *     // firstQuote.cancel(...) attempts to cancel every input query.
     * });
     * }</pre>
     *
     * @param <T> the result type of the futures.
     * @param cfs the collection of futures to race, must not be {@code null} or empty.
     * @return a {@code ContinuableFuture} whose {@code get()} returns the result of the first
     *         input success observed and published by the aggregate. If every input future fails, {@code get()}
     *         throws an {@link ExecutionException} whose cause is the first failure, with the
     *         remaining failures attached to that cause as suppressed exceptions; if every input
     *         future was cancelled, {@link CancellationException} is thrown instead. If the thread
     *         calling {@code get()} is interrupted while waiting, {@link InterruptedException} is thrown.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     */
    public static <T> ContinuableFuture<T> anyOf(final Collection<? extends Future<? extends T>> cfs) throws IllegalArgumentException {
        return anyOf2(cfs);
    }

    /**
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     */
    private static <T> ContinuableFuture<T> anyOf2(final Collection<? extends Future<? extends T>> cfs) throws IllegalArgumentException {
        N.checkArgument(N.notEmpty(cfs), "The specified collection cannot be null or empty");

        final List<Future<? extends T>> futures = new ArrayList<>(cfs);
        N.checkArgument(N.notEmpty(futures), "The specified collection cannot be null or empty");

        return ContinuableFuture.wrap(new Future<>() {
            // Complete normally with a Result so cancellation and failure retain their exact exception shape.
            private final CompletableFuture<Result<T, Exception>> terminal = new CompletableFuture<>();
            // Under this monitor, each slot is null (unregistered), its input/listener or relay (pending),
            // or this aggregate (settled). All getters share these observations and the terminal future.
            private final Future<?>[] observers = new Future<?>[futures.size()];
            private final List<Exception> failures = new ArrayList<>();
            private int remaining = futures.size();
            private int waiters;
            private boolean registered;

            /**
             * @throws CancellationException if all inputs were cancelled and the recorded failure is cancellation.
             * @throws ExecutionException if no input completed successfully and a failure was recorded.
             */
            private T terminalValue() throws CancellationException, ExecutionException {
                final Result<T, Exception> result = terminal.getNow(null);
                if (result.isSuccess()) {
                    return result.orElseIfFailure(null);
                }
                if (result.getException() instanceof CancellationException cancelled) {
                    throw cancelled;
                }
                throw (ExecutionException) result.getException();
            }

            private synchronized void finish(final Result<T, Exception> result) {
                if (!terminal.isDone()) {
                    terminal.complete(result);
                    releaseRelays();
                }
            }

            private synchronized void fail(final List<Exception> observedFailures) {
                if (!terminal.isDone()) {
                    finish(Result.failure(anyOfFailure(observedFailures, isCancelled())));
                }
            }

            private synchronized void record(final int index, final Future<?> observer, final T value, final Throwable error) {
                if (terminal.isDone() || observers[index] != observer) {
                    return; // A cancelled/superseded relay must not publish its own interruption.
                }
                observers[index] = this;
                remaining--;
                if (error == null) {
                    finish(Result.success(value));
                } else {
                    failures.add(convertException(error));
                    if (remaining == 0) {
                        fail(failures);
                    }
                }
            }

            /** Called under this monitor; only relay wrappers are cancelled, never the input futures. */
            private void releaseRelays() {
                for (int i = 0; i < observers.length; i++) {
                    if (observers[i] instanceof FutureTask<?> relay) {
                        observers[i] = null;
                        registered = false;
                        relay.cancel(true);
                    }
                }
            }

            /**
             * Called under this monitor; registration never runs a lazy input's get on a waiting caller.
             * @throws NullPointerException if an input future to be observed is {@code null}.
             * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an input.
             */
            private void register() throws NullPointerException, RejectedExecutionException {
                if (registered) {
                    return;
                }
                for (int i = 0; i < futures.size() && !terminal.isDone(); i++) {
                    if (observers[i] != null) {
                        continue;
                    }
                    final int index = i;
                    final Future<? extends T> input = futures.get(i);
                    try {
                        if (input instanceof CompletableFuture<? extends T> completable) {
                            observers[i] = input;
                            completable.whenComplete((value, error) -> record(index, input, value, error));
                        } else if (input.getClass() == FutureTask.class && input.isDone()) {
                            // An ordinary completed FutureTask just reads its stored outcome; subclasses
                            // and ContinuableFuture mappings may still execute user code and need a relay.
                            observers[i] = this;
                            try {
                                record(i, this, input.get(), null);
                            } catch (final Exception | Error e) {
                                record(i, this, null, e);
                            }
                        } else {
                            final FutureTask<T> relay = new FutureTask<>(input::get) {
                                @Override
                                protected void done() {
                                    try {
                                        record(index, this, get(), null);
                                    } catch (final ExecutionException e) {
                                        record(index, this, null, e.getCause()); // Remove this relay's own wrapper.
                                    } catch (final Exception | Error e) {
                                        record(index, this, null, e);
                                    }
                                }
                            };
                            observers[i] = relay; // Claim before execute: completion can be immediate.
                            RELAY_EXECUTOR.execute(relay);
                        }
                    } catch (final RuntimeException | Error e) {
                        // Failed listener registration/submission must be retryable. Keep an outcome if a
                        // callback already delivered it; otherwise nobody is watching this input yet.
                        if (observers[i] != this) {
                            observers[i] = null;
                        }
                        throw e;
                    }
                }
                registered = true;
            }

            /**
             * @throws NullPointerException if an input future to be observed is {@code null}.
             * @throws RejectedExecutionException if the relay executor rejects observation of an input.
             * @throws InterruptedException if interrupted while awaiting a terminal result; the interrupt status is restored.
             * @throws TimeoutException if {@code timed} is true and the wait expires before a terminal result.
             * @throws CancellationException if all inputs were cancelled and cancellation is the recorded failure.
             * @throws ExecutionException if no input completed successfully.
             */
            private T await(final boolean timed, final long timeoutNanos)
                    throws NullPointerException, RejectedExecutionException, InterruptedException, TimeoutException, CancellationException, ExecutionException {
                final long start = System.nanoTime();
                synchronized (this) {
                    waiters++;
                }
                try {
                    synchronized (this) {
                        if (!terminal.isDone()) {
                            register();
                        }
                    }
                    // CompletableFuture supplies precise timed waits and wakes every getter, including
                    // when a zero-timeout poll publishes. Never hold the observer monitor while waiting.
                    if (timed) {
                        terminal.get(timeoutNanos - (System.nanoTime() - start), TimeUnit.NANOSECONDS);
                    } else {
                        terminal.get();
                    }
                    return terminalValue();
                } catch (final InterruptedException | TimeoutException e) {
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                    }
                    synchronized (this) {
                        addSuppressedFailures(e, failures);
                    }
                    throw e;
                } finally {
                    synchronized (this) {
                        if (--waiters == 0) {
                            releaseRelays();
                        }
                    }
                }
            }

            /**
             * {@inheritDoc}
             * @throws RuntimeException if cancellation of an input future throws a runtime exception; subsequent cancellation failures are suppressed on the first.
             */
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) throws RuntimeException {
                boolean res = true;
                RuntimeException exception = null;
                for (final Future<? extends T> future : futures) {
                    try {
                        res = res & future.cancel(mayInterruptIfRunning); //NOSONAR
                    } catch (final RuntimeException e) {
                        if (exception == null) {
                            exception = e;
                        } else {
                            addSuppressedIfDistinct(exception, e);
                        }
                    }
                }
                if (exception != null) {
                    throw exception;
                }
                return res;
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a {@code null} input future is reached while examining completion or cancellation.
             */
            @Override
            public boolean isCancelled() throws NullPointerException {
                for (final Future<?> future : futures) {
                    if (!future.isCancelled()) {
                        return false;
                    }
                }
                return true;
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a {@code null} input future is reached while examining completion or cancellation.
             */
            @Override
            public boolean isDone() throws NullPointerException {
                if (terminal.isDone()) {
                    return true;
                }
                boolean allDone = true;
                for (final Future<?> future : futures) {
                    if (!future.isDone()) {
                        allDone = false;
                        break;
                    }
                }
                if (allDone) {
                    return true; // Do not run a lazy mapper when all inputs already report done.
                }
                try {
                    get(0, TimeUnit.NANOSECONDS);
                    return true;
                } catch (final TimeoutException pending) {
                    return false;
                } catch (final ExecutionException | CancellationException completed) {
                    return true;
                } catch (final InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a reached input future is {@code null}.
             * @throws CancellationException if all input futures were cancelled and the recorded terminal failure is a cancellation.
             * @throws RejectedExecutionException if the relay executor rejects observation of an unfinished input future.
             * @throws InterruptedException if the caller is interrupted while waiting for a result.
             * @throws ExecutionException if all inputs fail to produce a successful result and the terminal failure is not represented as a cancellation.
             */
            @Override
            public T get() throws NullPointerException, CancellationException, RejectedExecutionException, InterruptedException, ExecutionException {
                if (terminal.isDone()) {
                    return terminalValue();
                }
                try {
                    return await(false, 0);
                } catch (final TimeoutException unreachable) {
                    throw new AssertionError(unreachable); // An untimed wait cannot expire.
                }
            }

            /**
             * {@inheritDoc}
             * @throws NullPointerException if a reached input future is {@code null}, or {@code unit} is {@code null}.
             * @throws CancellationException if all input futures were cancelled and the recorded terminal failure is a cancellation.
             * @throws RejectedExecutionException if the relay executor rejects observation of an unfinished input future.
             * @throws InterruptedException if the caller is interrupted while waiting for a result.
             * @throws TimeoutException if the requested wait expires before a result can be obtained.
             * @throws ExecutionException if all inputs fail to produce a successful result and the terminal failure is not represented as a cancellation.
             */
            @Override
            public T get(final long timeout, final TimeUnit unit)
                    throws NullPointerException, CancellationException, RejectedExecutionException, InterruptedException, TimeoutException, ExecutionException {
                N.requireNonNull(unit, "unit");
                if (terminal.isDone()) {
                    return terminalValue();
                }
                if (timeout > 0) {
                    return await(true, unit.toNanos(timeout));
                }

                // Poll inputs directly: newly submitted relays need not have run yet. A timed get(0)
                // avoids an unbounded read, though a lazy mapper can itself ignore the input deadline.
                final List<Exception> observedFailures = new ArrayList<>();
                for (final Future<? extends T> future : futures) {
                    if (future.isDone()) {
                        final T value;
                        try {
                            value = future.get(0, TimeUnit.NANOSECONDS);
                        } catch (final TimeoutException pending) {
                            continue;
                        } catch (final InterruptedException e) {
                            throw e;
                        } catch (final Exception | Error e) {
                            observedFailures.add(convertException(e));
                            continue;
                        }
                        finish(Result.success(value));
                        return terminalValue();
                    }
                }
                if (observedFailures.size() == futures.size()) {
                    fail(observedFailures);
                }
                if (terminal.isDone()) {
                    return terminalValue();
                }
                throw new TimeoutException();
            }
        });
    }

    /**
     * Builds the one published failure, preserving cancellation and the original (possibly Error) cause.
     *
     * @param failures the observed input failures in encounter order; must not be {@code null}.
     * @param allCancelled whether every input future reports cancellation.
     * @return the first failure if it is a {@link CancellationException} and all inputs are cancelled;
     *         otherwise an {@link ExecutionException} carrying the aggregated failure.
     */
    private static Exception anyOfFailure(final List<Exception> failures, final boolean allCancelled) {
        if (failures.isEmpty()) {
            return new ExecutionException(new IllegalStateException("No result was produced by any of the given futures")); //NOSONAR
        }
        final Throwable first = unwrapErrorCarrier(failures.get(0));
        addSuppressedFailures(first, failures.subList(1, failures.size()));
        return allCancelled && first instanceof CancellationException cancelled ? cancelled : new ExecutionException(first);
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
     * @param cfs the array of futures to iterate over, must not be {@code null} or empty.
     * @return an {@code ObjIterator} that yields results in completion order (first-finished,
     *         first-out). Calling {@code next()} on a failed future rethrows a {@link RuntimeException}
     *         directly, or wraps another exception in a runtime exception.
     *         If the consumer is interrupted while waiting, its interrupt flag is restored, pending relays
     *         are released without cancelling inputs, and {@code next()} throws one final runtime exception
     *         wrapping an {@link InterruptedException} before iteration ends.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     */
    @SafeVarargs
    public static <T> ObjIterator<T> iterate(final Future<? extends T>... cfs)
            throws NullPointerException, IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(cfs, cs.cfs);

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
     * @param cfs the collection of futures to iterate over, must not be {@code null} or empty.
     * @return an {@code ObjIterator} that yields results in completion order (first-finished,
     *         first-out). Calling {@code next()} on a failed future rethrows a {@link RuntimeException}
     *         directly, or wraps another exception in a runtime exception.
     *         If the consumer is interrupted while waiting, its interrupt flag is restored, pending relays
     *         are released without cancelling inputs, and {@code next()} throws one final runtime exception
     *         wrapping an {@link InterruptedException} before iteration ends.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     * @see #iterate(Collection, long, TimeUnit)
     * @see #iterate(Collection, Function)
     */
    public static <T> ObjIterator<T> iterate(final Collection<? extends Future<? extends T>> cfs)
            throws IllegalArgumentException, NullPointerException, RejectedExecutionException {
        return iterate02(cfs);
    }

    /**
     * Creates an iterator with a total timeout for all futures.
     * Similar to the regular iterate method, but enforces a maximum total time for retrieving
     * all results. Results observed within the budget remain available after the deadline;
     * later results are excluded. After draining those results, if any inputs remain unobserved,
     * the iterator yields one final TimeoutException wrapped in a RuntimeException.
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
     * @param cfs the collection of futures to iterate over, must not be {@code null} or empty.
     * @param totalTimeoutForAll the maximum time to wait for all results; must be positive. The clock starts
     *        when this method is called, not at the first {@code hasNext()}, so an iterator that is built and
     *        then held before being consumed has already spent part of its budget.
     * @param unit the time unit of {@code totalTimeoutForAll}, must not be {@code null}.
     * @return an {@code ObjIterator} that yields results in completion order (first-finished,
     *         first-out) with timeout enforcement. Calling {@code next()} on a failed future
     *         rethrows a {@link RuntimeException} directly, or wraps another exception in a runtime
     *         exception. After draining results observed in time, an expired deadline with unobserved
     *         inputs produces one final runtime exception wrapping a {@link TimeoutException}.
     *         If the consumer is interrupted while waiting, its interrupt flag is restored, pending relays
     *         are released without cancelling inputs, and {@code next()} throws one final runtime exception
     *         wrapping an {@link InterruptedException} before iteration ends.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty, {@code totalTimeoutForAll} is not positive, or {@code unit} is {@code null}.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     * @see #iterate(Collection)
     * @see #iterate(Collection, long, TimeUnit, Function)
     */
    public static <T> ObjIterator<T> iterate(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit)
            throws IllegalArgumentException, NullPointerException, RejectedExecutionException {
        return iterate02(cfs, totalTimeoutForAll, unit);
    }

    /**
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     */
    private static <T> ObjIterator<T> iterate02(final Collection<? extends Future<? extends T>> cfs)
            throws IllegalArgumentException, NullPointerException, RejectedExecutionException {
        return iterate02(cfs, Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }

    /**
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty, {@code totalTimeoutForAll} is not positive, or {@code unit} is {@code null}.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     */
    private static <T> ObjIterator<T> iterate02(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit)
            throws IllegalArgumentException, NullPointerException, RejectedExecutionException {
        final Iterator<Result<T, Exception>> iter = iterate02(cfs, totalTimeoutForAll, unit, Fn.identity());

        // Stays a ResultIterator so Futures#cancelPendingRelays(Iterator) still reaches the relay tasks
        // through this unwrapping view.
        return new ResultIterator<>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no more results are available.
             * @throws RuntimeException if the next input computation failed, was cancelled, or could not be obtained because the wait timed out or was interrupted.
             */
            @Override
            public T next() throws NoSuchElementException, RuntimeException {
                final Result<T, Exception> result = iter.next();

                return result.orElseThrow(Fn.toRuntimeException());
            }

            @Override
            void cancelPending() {
                cancelPendingRelays(iter);
            }
        };
    }

    /**
     * Creates an iterator that yields transformed results from futures as they complete.
     * The resultHandler function receives Result objects that encapsulate either success values
     * or exceptions, allowing custom handling of both cases. This is useful for logging,
     * error recovery, or transforming results.
     *
     * <p>The Result object provides methods like {@code isSuccess()}, {@code isFailure()},
     * {@code orElseThrow()}, {@code orElseIfFailure(defaultValue)}, and {@code getException()}
     * for handling both success and failure cases elegantly.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<Future<Integer>> calculations = startCalculations();
     *
     * ObjIterator<String> results = Futures.iterate(calculations,
     *     result -> {
     *         if (result.isSuccess()) {
     *             return "Success: " + result.orElseThrow();
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
     * @param cfs the collection of futures to iterate over, must not be {@code null} or empty.
     * @param resultHandler the function to transform each {@code Result} (success value or
     *                      failure exception) into the desired output type. Must not be
     *                      {@code null}.
     * @return an {@code ObjIterator} that yields transformed results in completion order
     *         (first-finished, first-out).
     *         If the consumer is interrupted while waiting, its interrupt flag is restored, pending relays
     *         are released without cancelling inputs, and one final {@code Result} carrying an
     *         {@link InterruptedException} is passed to {@code resultHandler} before iteration ends.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty, or {@code resultHandler} is {@code null}.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     * @see #iterate(Collection)
     * @see #iterate(Collection, long, TimeUnit, Function)
     */
    public static <T, R> ObjIterator<R> iterate(final Collection<? extends Future<? extends T>> cfs,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler)
            throws IllegalArgumentException, NullPointerException, RejectedExecutionException {
        N.checkArgNotNull(resultHandler, cs.resultHandler);

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
     *             return processDataPoint(result.orElseThrow());
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
     * @param cfs the collection of futures to iterate over, must not be {@code null} or empty.
     * @param totalTimeoutForAll the maximum time to wait for all results; must be positive. The clock starts
     *        when this method is called, not at the first {@code hasNext()}, so an iterator that is built and
     *        then held before being consumed has already spent part of its budget.
     * @param unit the time unit of {@code totalTimeoutForAll}, must not be {@code null}.
     * @param resultHandler the function to transform each {@code Result}, including
     *                      timeout-failure handling.
     * @return an {@code ObjIterator} that yields transformed results in completion order
     *         (first-finished, first-out) with timeout enforcement. Results observed within the budget
     *         remain available after the deadline; later results are excluded. After draining those
     *         results, an expired deadline with unobserved inputs passes one final {@code Result}
     *         carrying a {@link TimeoutException} to {@code resultHandler}.
     *         If the consumer is interrupted while waiting, its interrupt flag is restored, pending relays
     *         are released without cancelling inputs, and one final {@code Result} carrying an
     *         {@link InterruptedException} is passed to {@code resultHandler} before iteration ends.
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty, {@code totalTimeoutForAll} is not positive, or {@code unit} is {@code null}, or {@code resultHandler} is {@code null}.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     * @see #iterate(Collection, Function)
     * @see #iterate(Collection, long, TimeUnit)
     */
    public static <T, R> ObjIterator<R> iterate(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler)
            throws IllegalArgumentException, NullPointerException, RejectedExecutionException {
        N.checkArgNotNull(resultHandler, cs.resultHandler);

        return iterate02(cfs, totalTimeoutForAll, unit, resultHandler);
    }

    /**
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     */
    private static <T, R> ObjIterator<R> iterate02(final Collection<? extends Future<? extends T>> cfs,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler)
            throws IllegalArgumentException, NullPointerException, RejectedExecutionException {
        return iterate02(cfs, Long.MAX_VALUE, TimeUnit.MILLISECONDS, resultHandler);
    }

    /**
     * @throws IllegalArgumentException if {@code cfs} is {@code null} or empty, {@code totalTimeoutForAll} is not positive, or {@code unit} is {@code null}.
     * @throws NullPointerException if the input contains a {@code null} future.
     * @throws RejectedExecutionException if the relay executor rejects a task submitted to observe an unfinished future.
     */
    private static <T, R> ObjIterator<R> iterate02(final Collection<? extends Future<? extends T>> cfs, final long totalTimeoutForAll, final TimeUnit unit,
            final Function<? super Result<T, Exception>, ? extends R> resultHandler)
            throws IllegalArgumentException, NullPointerException, RejectedExecutionException {
        N.checkArgument(N.notEmpty(cfs), "The specified collection cannot be null or empty");
        N.checkArgPositive(totalTimeoutForAll, cs.totalTimeoutForAll);
        N.checkArgNotNull(unit, cs.unit);

        final long startTimeInNanos = System.nanoTime();
        final long totalTimeoutForAllInNanos = totalTimeoutForAll == Long.MAX_VALUE ? Long.MAX_VALUE : unit.toNanos(totalTimeoutForAll);
        final BlockingQueue<Result<T, Exception>> completedResults = new LinkedBlockingQueue<>();
        // Count and register the same snapshot, even if the source collection changes during construction.
        final List<Future<? extends T>> futureList = new ArrayList<>(cfs);
        N.checkArgument(N.notEmpty(futureList), "The specified collection cannot be null or empty");
        final int futureCount = futureList.size();
        final Consumer<Result<T, Exception>> complete = outcome -> {
            // Retain unconsumed results that arrived in budget, but exclude all late outcomes.
            if (totalTimeoutForAllInNanos == Long.MAX_VALUE || System.nanoTime() - startTimeInNanos <= totalTimeoutForAllInNanos) {
                completedResults.offer(outcome);
            }
        };

        // Track every submitted wrapper task so we can cancel still-running ones when
        // the global timeout fires or the consumer is interrupted. Without this, blocked
        // future.get() calls keep occupying relay threads long after the caller has stopped
        // consuming results.
        final List<Future<?>> submitted = new ArrayList<>(futureCount);

        // Two passes on purpose. Pass 1 starts a relay for every input that is not finished yet, so
        // they all wait in parallel; pass 2 then drains the already-finished ones on this thread. Doing
        // the inline reads first would let one slow already-done input (for example a lazily-mapped
        // future, whose isDone() is the upstream's but whose get() still applies the mapper) delay
        // starting the relays for its siblings.
        List<Future<? extends T>> alreadyDone = null;

        for (final Future<? extends T> future : futureList) {
            if (future instanceof CompletableFuture<? extends T> completableFuture) {
                completableFuture.whenComplete((value, error) -> {
                    complete.accept(error == null ? Result.of(value, null) : Result.of(null, convertException(error)));
                });
            } else if (future.isDone()) {
                // No relay thread needed: the outcome is available now and get() will not block.
                if (alreadyDone == null) {
                    alreadyDone = new ArrayList<>(futureCount);
                }

                alreadyDone.add(future);
            } else {
                final FutureTask<Void> submittedTask = new FutureTask<>(() -> {
                    try {
                        complete.accept(Result.of(future.get(), null));
                    } catch (final Exception | Error e) { // A broken/custom Future may throw an Error directly; never leave the consumer waiting forever.
                        complete.accept(Result.of(null, convertException(e)));
                    }
                }, null);

                submitted.add(submittedTask);
                RELAY_EXECUTOR.execute(submittedTask);
            }
        }

        if (alreadyDone != null) {
            for (final Future<? extends T> future : alreadyDone) {
                try {
                    complete.accept(Result.of(future.get(), null));
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    complete.accept(Result.of(null, e));
                } catch (final Exception | Error e) {
                    complete.accept(Result.of(null, convertException(e)));
                }
            }
        }

        return new ResultIterator<>() {
            private int remainingCount = futureCount;
            private boolean resultReady = false;
            private boolean noMore = false;
            private Result<T, Exception> nextResult = null;

            @Override
            public boolean hasNext() {
                if (resultReady) {
                    return true;
                }

                if (noMore) {
                    return false;
                }

                if (remainingCount <= 0) {
                    // Deliberately no cancelPending() here. Every relay has already offered its result
                    // (that is what drove remainingCount to 0), so this could only catch one in the window
                    // between offer(..) and the FutureTask going done -- and cancel(true) would then
                    // interrupt a RELAY_EXECUTOR worker that is about to be reused, leaking the interrupt
                    // into the next task. The interrupt/timeout paths below cancel because they really do
                    // abandon in-flight relays.
                    noMore = true;
                    return false;
                }

                final long remainingTimeInNanos = totalTimeoutForAllInNanos == Long.MAX_VALUE ? Long.MAX_VALUE
                        : totalTimeoutForAllInNanos - (System.nanoTime() - startTimeInNanos);

                try {
                    if (remainingTimeInNanos == Long.MAX_VALUE) {
                        nextResult = completedResults.take();
                    } else if (remainingTimeInNanos > 0) {
                        nextResult = completedResults.poll(remainingTimeInNanos, TimeUnit.NANOSECONDS);
                    } else {
                        // In-budget results remain available after the deadline; an empty queue ends it.
                        nextResult = completedResults.poll();
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    cancelPending();
                    noMore = true;
                    resultReady = true;
                    nextResult = Result.of(null, e);
                    return true;
                }

                if (nextResult == null) {
                    cancelPending();
                    noMore = true;
                    resultReady = true;
                    nextResult = Result.of(null, new TimeoutException());
                    return true;
                }

                remainingCount--;
                resultReady = true;
                return true;
            }

            /**
             * {@inheritDoc}
             * @throws NoSuchElementException if no more results are available.
             * @throws RuntimeException if {@code resultHandler} throws while processing the next result.
             */
            @Override
            public R next() throws NoSuchElementException, RuntimeException {
                if (!hasNext()) {
                    throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                }

                resultReady = false;
                return resultHandler.apply(nextResult);
            }

            @Override
            void cancelPending() {
                for (final Future<?> f : submitted) {
                    if (!f.isDone()) {
                        // Best-effort interrupt-and-cancel of the wrapper task. The underlying
                        // user-supplied future is not cancelled (we don't own it).
                        f.cancel(true);
                    }
                }
            }
        };
    }

    /**
     * Unwraps an {@link ExecutionException} or {@link CompletionException} to its underlying cause
     * when the cause is itself an {@link Exception}. Used internally by {@code anyOf} and iteration to
     * avoid presenting callers with double-wrapped exceptions.
     *
     * <p>Conversion rules:
     * <ul>
     *   <li>If {@code e} is an {@link ExecutionException} or {@link CompletionException} and its
     *       cause is an {@code Exception}, returns the cause.</li>
     *   <li>Otherwise, if {@code e} is itself an {@code Exception} (including an
     *       {@link ExecutionException}/{@link CompletionException} whose cause is not an
     *       {@code Exception}), returns {@code e} unchanged.</li>
     *   <li>Otherwise (e.g., {@code e} is an {@link Error}), returns a new {@link ExecutionException}
     *       wrapping {@code e}.</li>
     * </ul>
     *
     * @param e the throwable to convert; must not be {@code null}.
     * @return the unwrapped cause when applicable, otherwise {@code e} itself or, for a non-{@code Exception}
     *         throwable, a new {@link ExecutionException} wrapping it; never {@code null}.
     * @see ExecutionException
     */
    static Exception convertException(final Throwable e) {
        if ((e instanceof ExecutionException || e instanceof CompletionException) && e.getCause() instanceof Exception ex) {
            return ex;
        }

        return e instanceof Exception ex ? ex : new ExecutionException(e);
    }

    /**
     * Adds a secondary failure once by identity, including across concurrent composites, without allowing
     * {@link Throwable#addSuppressed(Throwable)} to mask a reused primary through self-suppression.
     *
     * @param primary the failure that will be reported to the caller
     * @param secondary the additional failure to attach; ignored when it is the very same instance as
     *        {@code primary}, because {@code addSuppressed} rejects self-suppression
     */
    private static void addSuppressedIfDistinct(final Throwable primary, final Throwable secondary) {
        if (primary == secondary) {
            return;
        }
        synchronized (primary) {
            for (final Throwable existing : primary.getSuppressed()) {
                if (existing == secondary) {
                    return;
                }
            }
            primary.addSuppressed(secondary);
        }
    }

    /**
     * Deduplicates a whole batch in linear time, atomically across aggregates sharing a failure.
     *
     * @param primary the failure on which distinct secondary causes are suppressed; must not be {@code null}.
     * @param failures the secondary failures to unwrap and attach in order; must not be {@code null}.
     */
    private static void addSuppressedFailures(final Throwable primary, final List<? extends Throwable> failures) {
        if (failures.isEmpty()) {
            return;
        }
        final Throwable[] causes = failures.toArray(new Throwable[0]);
        for (int i = 0; i < causes.length; i++) {
            // Resolve causes before locking primary: getCause() may acquire another exception's monitor.
            causes[i] = unwrapErrorCarrier(causes[i]);
        }
        synchronized (primary) {
            final Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
            Collections.addAll(seen, primary.getSuppressed());
            seen.add(primary);
            for (final Throwable cause : causes) {
                if (seen.add(cause)) {
                    primary.addSuppressed(cause);
                }
            }
        }
    }

    /**
     * Exposes a non-Exception cause that {@code convertException} must carry in a wrapper.
     *
     * @param failure the observed failure to inspect; must not be {@code null}.
     * @return the non-null, non-Exception cause of an {@link ExecutionException} or {@link CompletionException},
     *         or {@code failure} itself otherwise.
     */
    private static Throwable unwrapErrorCarrier(final Throwable failure) {
        return (failure instanceof ExecutionException || failure instanceof CompletionException) && failure.getCause() != null
                && !(failure.getCause() instanceof Exception) ? failure.getCause() : failure;
    }
}
