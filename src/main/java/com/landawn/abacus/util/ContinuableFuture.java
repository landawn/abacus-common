/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Tuple.Tuple4;

// This class is heavily inspired by CompletableFuture but redesigned for better usability and flexibility.
// It provides some important features not available in CompletableFuture,
// such as recursive cancellation support and fluent chaining with enhanced control.

/**
 * A powerful and flexible asynchronous computation framework that extends the standard {@link Future} interface
 * with advanced functional composition capabilities, recursive cancellation support, and fluent chaining operations.
 * This class provides a more intuitive and feature-rich alternative to {@link CompletableFuture} for building
 * complex asynchronous workflows with enhanced control over execution, error handling, and cancellation.
 *
 * <p>{@code ContinuableFuture} represents an asynchronous computation that can be seamlessly chained with other
 * computations, allowing developers to construct sophisticated asynchronous pipelines using a fluent API that
 * emphasizes readability and maintainability. Unlike traditional futures that require complex callback management,
 * this class provides intuitive methods for sequential, parallel, and conditional execution patterns.</p>
 *
 * <p><b>IMPORTANT - Design Philosophy:</b>
 * <ul>
 *   <li><b>Simplicity Over Complexity:</b> Streamlined API focused on common asynchronous patterns</li>
 *   <li><b>Fluent Composition:</b> Method chaining enables readable asynchronous workflow construction</li>
 *   <li><b>Recursive Control:</b> Cancellation can be propagated throughout execution chains</li>
 *   <li><b>Error Propagation:</b> Consistent exception handling and error recovery mechanisms</li>
 *   <li><b>Executor Flexibility:</b> Fine-grained control over thread pool usage and execution contexts</li>
 * </ul>
 *
 * <p><b>Key Features and Advantages:</b>
 * <ul>
 *   <li><b>Fluent API Design:</b> Intuitive method chaining for building complex asynchronous workflows</li>
 *   <li><b>Recursive Cancellation:</b> {@code cancelAll()} propagates cancellation through entire execution chains</li>
 *   <li><b>Built-in Delay Support:</b> Native {@code thenDelay()} methods for time-based workflow control</li>
 *   <li><b>Result Wrapping:</b> {@code getAsResult()} methods return {@link Result} objects for enhanced error handling</li>
 *   <li><b>Executor Flexibility:</b> Chain-wide executor selection with {@code thenUse()} - every later stage inherits it</li>
 *   <li><b>Multiple Combination Patterns:</b> Support for both/either completion scenarios with various callback types</li>
 *   <li><b>Type Safety:</b> Strong generic typing throughout the composition chain</li>
 *   <li><b>Explicit Dependencies:</b> Chained futures retain upstream references to support recursive cancellation</li>
 * </ul>
 *
 * <p><b>Core Composition Methods:</b>
 * <ul>
 *   <li><b>{@code thenRunAsync()}:</b> Execute a Runnable/Consumer/BiConsumer action after completion</li>
 *   <li><b>{@code thenCallAsync()}:</b> Execute a Callable/Function/BiFunction returning a new result</li>
 *   <li><b>{@code map()}:</b> Transform the result synchronously using a Function</li>
 *   <li><b>{@code thenDelay()}:</b> Add time delays between operations</li>
 *   <li><b>{@code thenUse()}:</b> Change executor for subsequent operations</li>
 * </ul>
 *
 * <p><b>Combination and Coordination Patterns:</b>
 * <ul>
 *   <li><b>Both Completion:</b> {@code runAsyncAfterBoth()}, {@code callAsyncAfterBoth()} - Wait for both futures</li>
 *   <li><b>Either Completion:</b> {@code runAsyncAfterEither()}, {@code callAsyncAfterEither()} - React to first completion</li>
 *   <li><b>First Success:</b> {@code runAsyncAfterFirstSuccess()}, {@code callAsyncAfterFirstSuccess()} - Wait for first successful completion</li>
 * </ul>
 *
 * <p><b>Naming map:</b> overloads sharing a {@code ContinuableFuture} method name can have
 * materially different continuation behavior. The table below lists the continuation methods on
 * {@code ContinuableFuture}, maps each to its closest {@link CompletableFuture} counterpart, and
 * states in the last column whether the behavior matches that counterpart (&quot;Same.&quot;) or how it
 * differs. A dash ({@code —}) in the {@code CompletableFuture} column means there is no direct
 * counterpart. Every executor-backed method in this table submits a worker that blocks in
 * {@code get()} on its inputs (see below); that shared difference is not repeated per row.</p>
 * <table border="1">
 *   <caption>{@code ContinuableFuture} continuation naming map</caption>
 *   <tr>
 *     <th>{@code ContinuableFuture} method</th>
 *     <th>Closest {@code CompletableFuture} operation</th>
 *     <th>Behavior</th>
 *     <th>Difference from {@code CompletableFuture}</th>
 *   </tr>
 *   <tr>
 *     <td>{@code map(Function)} ({@code @Beta})</td>
 *     <td>{@code thenApply}</td>
 *     <td>Transforms the successful result.</td>
 *     <td>Applied <b>lazily and synchronously</b> on the thread that calls {@code get()}, not on an executor;
 *         {@code thenApply} runs when the stage completes. Use {@code thenCallAsync(Function)} for the async form.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code thenRunAsync(Runnable)}</td>
 *     <td>{@code thenRunAsync}</td>
 *     <td>Runs after successful completion; ignores the upstream result.</td>
 *     <td>Same: an upstream failure or cancellation is reported as {@code ExecutionException(cause)} /
 *         {@code ExecutionException(CancellationException)} with {@code isCancelled() == false}, exactly as a
 *         {@code CompletableFuture} dependent reports it.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code thenRunAsync(Consumer)}</td>
 *     <td>{@code thenAcceptAsync}</td>
 *     <td>Consumes the successful upstream result.</td>
 *     <td>Same.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code thenRunAsync(BiConsumer)}</td>
 *     <td>{@code whenCompleteAsync}</td>
 *     <td>Receives the result and exception.</td>
 *     <td>Produces a {@code Void} result instead of preserving the upstream value the way {@code whenComplete}
 *         does. A normally returning callback also recovers an upstream failure; {@code whenComplete} preserves it.
 *         The callback receives the unwrapped {@link Exception} (see "Failure shape" below), not a {@link Throwable}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code thenCallAsync(Callable)}</td>
 *     <td>{@code thenRunAsync} (returning a value)</td>
 *     <td>Runs after success, ignores the upstream result, and produces a new result.</td>
 *     <td>No exact counterpart: {@code thenRunAsync} returns {@code Void}; the nearest is
 *         {@code thenApplyAsync(r -> ...)} that discards {@code r}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code thenCallAsync(Function)}</td>
 *     <td>{@code thenApplyAsync}</td>
 *     <td>Transforms the successful upstream result.</td>
 *     <td>Same, except the callback may throw checked exceptions.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code thenCallAsync(BiFunction)}</td>
 *     <td>{@code handleAsync}</td>
 *     <td>Receives the result and exception and produces the next result, including recovery values.</td>
 *     <td>Same, except the callback receives the unwrapped {@link Exception} rather than a {@link Throwable}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code runAsyncAfterBoth(other, Runnable)}</td>
 *     <td>{@code runAfterBothAsync}</td>
 *     <td>Runs after both complete successfully; ignores both results.</td>
 *     <td>Same.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code runAsyncAfterBoth(other, BiConsumer)}</td>
 *     <td>{@code thenAcceptBothAsync}</td>
 *     <td>Consumes both successful results.</td>
 *     <td>Same.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code runAsyncAfterBoth(other, Consumer<Tuple4>)}<br>{@code runAsyncAfterBoth(other, QuadConsumer)}</td>
 *     <td>{@code —}</td>
 *     <td>Always runs after both complete; receives both results and both exceptions (bundled in a
 *         {@link Tuple4}, or as four separate arguments).</td>
 *     <td>No counterpart: {@code CompletableFuture}'s both-methods run only when both succeed.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code callAsyncAfterBoth(other, Callable)}</td>
 *     <td>{@code runAfterBothAsync} (returning a value)</td>
 *     <td>Runs after both succeed, ignores both results, and produces a new result.</td>
 *     <td>{@code runAfterBoth} returns {@code Void}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code callAsyncAfterBoth(other, BiFunction)}</td>
 *     <td>{@code thenCombineAsync}</td>
 *     <td>Combines both successful results into a new result.</td>
 *     <td>Same.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code callAsyncAfterBoth(other, Function<Tuple4>)}<br>{@code callAsyncAfterBoth(other, QuadFunction)}</td>
 *     <td>{@code —}</td>
 *     <td>Always runs after both complete; transforms both results and both exceptions into a new result.</td>
 *     <td>No counterpart: {@code CompletableFuture}'s both-methods run only when both succeed.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code runAsyncAfterEither(other, Runnable)}</td>
 *     <td>{@code runAfterEitherAsync}</td>
 *     <td>Runs after the first of the two to complete, including failure or cancellation.</td>
 *     <td>The action still runs after an exceptional completion; {@code runAfterEitherAsync} requires normal completion.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code runAsyncAfterEither(other, Consumer)}</td>
 *     <td>{@code acceptEitherAsync}</td>
 *     <td>Consumes the result of the first future to complete.</td>
 *     <td>If that first future <b>failed</b>, the consumer receives {@code null} and still runs;
 *         {@code acceptEither} instead completes exceptionally.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code runAsyncAfterEither(other, BiConsumer)}</td>
 *     <td>{@code —}</td>
 *     <td>Reacts to the first completion, whether successful or exceptional; receives (result, exception).</td>
 *     <td>No counterpart; use {@code runAsyncAfterFirstSuccess} to wait for a successful result.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code callAsyncAfterEither(other, Callable)}</td>
 *     <td>{@code runAfterEitherAsync} (returning a value)</td>
 *     <td>Runs after the first to complete, ignores its result, and produces a new result.</td>
 *     <td>The action still runs after failure or cancellation; {@code runAfterEitherAsync} requires normal completion
 *         and returns {@code Void}.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code callAsyncAfterEither(other, Function)}</td>
 *     <td>{@code applyToEitherAsync}</td>
 *     <td>Transforms the result of the first future to complete.</td>
 *     <td>If that first future failed, the function receives {@code null} and still runs;
 *         {@code applyToEither} instead completes exceptionally.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code callAsyncAfterEither(other, BiFunction)}</td>
 *     <td>{@code —}</td>
 *     <td>Transforms (result, exception) of the first future to complete.</td>
 *     <td>No counterpart; reacts to the first to <i>complete</i>, not the first success.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code runAsyncAfterFirstSuccess(other, Runnable | Consumer)}</td>
 *     <td>{@code —}</td>
 *     <td>Runs after the first <b>successful</b> completion. If both inputs fail, propagates the failure without running the action.</td>
 *     <td>No counterpart.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code callAsyncAfterFirstSuccess(other, Callable | Function)}</td>
 *     <td>{@code —}</td>
 *     <td>Produces a result after the first <b>successful</b> completion. If both inputs fail, propagates the failure without running the action.</td>
 *     <td>No counterpart.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code runAsyncAfterFirstSuccess(other, BiConsumer)} ({@code @Beta})</td>
 *     <td>{@code —}</td>
 *     <td>Receives the first successful result, or an exception if both inputs fail; a normally returning action recovers that failure.</td>
 *     <td>No counterpart.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code callAsyncAfterFirstSuccess(other, BiFunction)} ({@code @Beta})</td>
 *     <td>{@code —}</td>
 *     <td>Transforms the first successful result, or an exception if both inputs fail, and may produce a recovery value.</td>
 *     <td>No counterpart.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code thenUse(Executor)}</td>
 *     <td>the {@code executor} argument of the {@code *Async(fn, executor)} overloads</td>
 *     <td>Selects the executor used by subsequent stages in the chain.</td>
 *     <td>{@code CompletableFuture} takes an {@link Executor} per call rather than as a chain-wide setting.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code thenDelay(delay, unit)}</td>
 *     <td>{@code —} (compare {@link CompletableFuture#delayedExecutor(long, TimeUnit)})</td>
 *     <td>Inserts a shared delay after upstream completion. Cancelling the delayed stage cancels the shared upstream
 *         task while it is pending, or ends an open delay window once the upstream has completed (see
 *         {@code thenDelay}); once the window has elapsed the stage is complete and {@code cancel()} is refused.</td>
 *     <td>No direct counterpart: use {@code delayedExecutor} to schedule a delayed continuation.</td>
 *   </tr>
 * </table>
 *
 * <p><b>Worker model.</b> Executor-backed methods submit a task to the configured executor <i>immediately</i>;
 * that task blocks in {@code get()} until its input future(s) complete and then runs the callback. It is not a
 * completion callback the way a {@code CompletableFuture} stage is, so it occupies one executor thread while it
 * waits. An executor may run a task directly on the calling thread, so an {@code Async} name does not itself
 * guarantee an immediate return. Cancellation or rejection can prevent a submitted continuation from executing,
 * including callbacks that handle both successful and exceptional upstream results. The {@code *AfterEither} and
 * {@code *AfterFirstSuccess} families additionally start one short-lived relay task on an internal daemon pool for
 * every input that is not a completed plain task (see {@link Futures}); a rejection by that pool surfaces as the
 * stage's failure.</p>
 *
 * <p><b>Failure shape.</b> Every executor-backed family ({@code then*}, {@code *AfterBoth}, {@code *AfterEither},
 * {@code *AfterFirstSuccess}) that propagates an upstream failure does so with a single wrapper: the
 * returned future's {@code get()} throws {@link ExecutionException} whose cause is the upstream's own failure
 * (the cause of the upstream's {@code ExecutionException}, or the {@link Error} itself when the upstream task threw
 * an {@code Error}); it is never a nested {@code ExecutionException}, however long the chain (the one exception is
 * a bare {@code Throwable} subclass that is neither an {@code Exception} nor an {@code Error}, which no task can
 * declare and which stays inside its carrier). A cancelled upstream
 * is reported as {@code ExecutionException(CancellationException)} with {@code isCancelled() == false}, exactly as a
 * {@code CompletableFuture} dependent reports it. The lazy wrappers ({@code map}, {@code thenDelay}, {@code thenUse})
 * instead mirror their source: a cancelled source makes them report {@code isCancelled() == true} and throw
 * {@code CancellationException} itself. The callbacks that receive an exception argument
 * ({@code BiConsumer}, {@code BiFunction}, {@code QuadConsumer}, {@code QuadFunction}, {@code Tuple4}) and the
 * {@link Result} returned by {@code getAsResult()} carry the same unwrapped exception: the cause of an
 * {@code ExecutionException}/{@code CompletionException}, a {@link CancellationException} for a cancelled input,
 * and - because the slot is typed {@link Exception} - the {@code ExecutionException} carrier when the upstream
 * failed with an {@code Error}. The synchronous {@code getAsResult}/{@code getThenApply}/{@code getThenAccept}
 * getters may also hand over the <i>calling thread's own</i> {@link InterruptedException} (interrupt status
 * restored) or {@link TimeoutException}.</p>
 *
 * <p>These are conceptual correspondences rather than signature-identical equivalents:
 * the bi-argument callbacks receive {@link Exception} (not {@link Throwable}), callbacks may throw
 * checked exceptions, and execution uses this future's configured executor.</p>
 *
 * <p>For coordinating across an arbitrary number of futures (all/any of), see {@link Futures}.
 *
 * <p><b>Advanced Cancellation and Control:</b>
 * <ul>
 *   <li><b>{@code cancel()}:</b> Standard Future cancellation for single operation</li>
 *   <li><b>{@code cancelAll()}:</b> Recursive cancellation propagating through entire execution chain</li>
 *   <li><b>{@code mayInterruptIfRunning}:</b> Control over thread interruption during cancellation</li>
 *   <li><b>Upstream Future Tracking:</b> Automatic management of dependent future relationships</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic asynchronous execution with chaining
 * ContinuableFuture<String> future = ContinuableFuture
 *     .call(() -> downloadData())
 *     .map(data -> processData(data))
 *     .thenDelay(1, TimeUnit.SECONDS)
 *     .thenCallAsync(data -> saveToDatabase(data));
 *
 * // Parallel execution with combination
 * ContinuableFuture<String> userFuture = ContinuableFuture.call(() -> fetchUser(userId));
 * ContinuableFuture<List<Order>> ordersFuture = ContinuableFuture.call(() -> fetchOrders(userId));
 *
 * ContinuableFuture<UserProfile> profileFuture = userFuture
 *     .callAsyncAfterBoth(ordersFuture, (user, orders) -> new UserProfile(user, orders));
 *
 * // Error handling and recovery using BiFunction-based thenCallAsync
 * ContinuableFuture<String> robustFuture = ContinuableFuture
 *     .call(() -> riskyOperation())
 *     .thenCallAsync((result, exception) -> {
 *         if (exception != null) {
 *             return "Default value";
 *         }
 *         return result;
 *     });
 *
 * // Custom executor usage
 * ExecutorService customExecutor = Executors.newCachedThreadPool();
 * try {
 *     String result = ContinuableFuture
 *         .call(() -> cpuIntensiveTask())
 *         .thenUse(customExecutor)
 *         .thenCallAsync(() -> ioIntensiveTask())
 *         .get();
 * } finally {
 *     customExecutor.shutdown();
 * }
 * }</pre>
 *
 * <p><b>Advanced Composition Examples:</b>
 * <pre>{@code
 * // Complex workflow with multiple decision points
 * ContinuableFuture<String> workflowFuture = ContinuableFuture
 *     .call(() -> authenticateUser(credentials))
 *     .thenCallAsync(user -> {
 *         if (user.hasPermission("READ")) {
 *             return loadUserData(user.getId());
 *         } else {
 *             throw new SecurityException("Insufficient permissions");
 *         }
 *     })
 *     .map(data -> transformData(data))
 *     .thenCallAsync((result, ex) -> ex != null ? "Access Denied" : result);
 *
 * // Race condition handling - first successful result wins
 * ContinuableFuture<String> primaryService = ContinuableFuture.call(() -> callPrimaryAPI());
 * ContinuableFuture<String> backupService = ContinuableFuture.call(() -> callBackupAPI());
 *
 * ContinuableFuture<String> fastestResponse = primaryService
 *     .callAsyncAfterFirstSuccess(backupService, (result) -> result);
 * }</pre>
 *
 * <p><b>Result Handling and Error Management:</b>
 * <ul>
 *   <li><b>{@code getAsResult()}:</b> Result retrieval as a {@link Result} wrapper instead of throwing</li>
 *   <li><b>{@code getAsResult(timeout, unit)}:</b> Timeout-aware Result retrieval</li>
 *   <li><b>{@code thenCallAsync(BiFunction)} / {@code thenRunAsync(BiConsumer)}:</b> Unified success/error handling
 *       (analogous to {@code CompletableFuture.handle}/{@code whenComplete})</li>
 *   <li><b>{@code getThenApply}/{@code getThenAccept}:</b> Synchronous result-or-exception consumption</li>
 * </ul>
 *
 * <p><b>Executor Management and Threading:</b>
 * <ul>
 *   <li><b>Default Executor:</b> Uses the shared {@code N.ASYNC_EXECUTOR} thread pool unless an executor is supplied explicitly</li>
 *   <li><b>Custom Executors:</b> {@code thenUse()} selects the executor for every subsequent stage of the chain</li>
 *   <li><b>Async Variants:</b> Methods ending with "Async" for explicit asynchronous execution</li>
 *   <li><b>Thread Safety:</b> Future state and built-in coordination are thread-safe; callbacks supplied by callers
 *       must themselves be safe if they can be invoked concurrently (for example, by concurrent {@code map().get()} calls)</li>
 *   <li><b>Executor Ownership:</b> Custom executors remain owned by the caller and must be shut down by the caller</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Chaining Overhead:</b> Each asynchronous stage submits a task that occupies an executor thread while waiting for its upstream stage</li>
 *   <li><b>Starvation:</b> the default {@code N.ASYNC_EXECUTOR} pool has a fixed size and an unbounded queue, so it never grows
 *       past its core size. If more stages are waiting for their inputs than the pool has workers, the work those inputs
 *       depend on can sit behind them in the queue; long fan-outs of pending stages should use a dedicated executor
 *       ({@code thenUse}) or {@link Futures}, whose aggregates never wait on the stage executor ({@code allOf}/
 *       {@code combine}/{@code compose} wait on the calling thread; {@code anyOf}/{@code iterate} observe pending
 *       inputs on an unbounded relay pool)</li>
 *   <li><b>Memory Usage:</b> Chained stages retain upstream references so that {@code cancelAll()} can traverse the dependency
 *       graph; a live downstream future therefore keeps every upstream stage and its result reachable</li>
 *   <li><b>Cancellation Cost:</b> O(n) where n is the length of the execution chain</li>
 *   <li><b>Combination Cost:</b> Coordination methods may use executor tasks that block while waiting for their input futures</li>
 *   <li><b>Delay Implementation:</b> The delay window starts when the delayed future first observes upstream completion
 *       (a call to {@code get()}, {@code isDone()} or {@code cancel()} on it, {@code thenDelay()} itself if the upstream was
 *       already done, or a further {@code thenDelay()}/{@code thenUse()} stage built on it while it is done) and is waited
 *       out with an interruptible, cancellable wait</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrency:</b>
 * <ul>
 *   <li><b>Immutable Chains:</b> Composition does not replace the original future; operations may return a new wrapper or, for a no-op delay, the same instance</li>
 *   <li><b>Safe Publication:</b> Results are safely published through happens-before relationships</li>
 *   <li><b>Concurrent Access:</b> Future state can be queried concurrently; a lazy {@code map()} function can run once per caller and must be thread-safe</li>
 *   <li><b>Executor Isolation:</b> Different stages can run on different thread pools safely</li>
 *   <li><b>Cancellation Coordination:</b> Thread-safe cancellation propagation throughout chains</li>
 * </ul>
 *
 * <p><b>Comparison with CompletableFuture:</b>
 * <ul>
 *   <li><b>Simpler API:</b> Focused on common use cases vs. comprehensive but complex API</li>
 *   <li><b>Better Cancellation:</b> Recursive {@code cancelAll()} vs. single-level cancellation</li>
 *   <li><b>Built-in Delays:</b> Native delay support vs. requiring external scheduling</li>
 *   <li><b>Result Wrapping:</b> {@link Result} objects for better error handling vs. exception throwing</li>
 *   <li><b>Upstream Tracking:</b> Automatic dependency management vs. manual reference management</li>
 *   <li><b>Fluent Design:</b> Optimized for method chaining readability</li>
 * </ul>
 *
 * <p><b>Static Factory Methods:</b>
 * <ul>
 *   <li><b>{@code call()}:</b> Create future from Callable with default or supplied executor</li>
 *   <li><b>{@code run()}:</b> Create future from Runnable with default or supplied executor</li>
 *   <li><b>{@code completed()}:</b> Create already-completed future with given value</li>
 *   <li><b>{@code wrap()}:</b> Wrap an existing {@link Future} as a {@code ContinuableFuture}</li>
 * </ul>
 *
 * <p><b>Best Practices and Recommendations:</b>
 * <ul>
 *   <li>Use method chaining to build readable asynchronous workflows</li>
 *   <li>Use {@code map()} when a lazy caller-thread transformation is intended; use {@code thenCallAsync()} for an executor-backed stage</li>
 *   <li>Use {@code cancelAll()} when cancellation should propagate to upstream futures</li>
 *   <li>Specify appropriate executors for CPU-bound vs I/O-bound operations</li>
 *   <li>Use {@code getAsResult()} methods for result retrieval that wraps exceptions in {@link Result}</li>
 *   <li>Use timed {@code get()} methods for wait budgets; {@code thenDelay()} adds a pause and is not a timeout mechanism</li>
 *   <li>Use {@code thenCallAsync(BiFunction)}/{@code thenRunAsync(BiConsumer)} for unified success/error processing</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Blocking on work queued to the same bounded executor from a callback, which can cause starvation or deadlock</li>
 *   <li>Creating deeply nested callback chains instead of using flat composition</li>
 *   <li>Ignoring cancellation propagation requirements in complex workflows</li>
 *   <li>Using default executor for both CPU-bound and I/O-bound operations</li>
 *   <li>Not handling exceptions appropriately in chained operations</li>
 *   <li>Holding long-lived references to the tail of a finished chain: it keeps every upstream stage and result reachable</li>
 * </ul>
 *
 * <p><b>Error Handling Strategies:</b>
 * <ul>
 *   <li><b>Propagation:</b> Exceptions automatically propagate through the chain unless handled</li>
 *   <li><b>Recovery:</b> Use {@code thenCallAsync(BiFunction)} (or the BiConsumer overload of {@code thenRunAsync})
 *       to handle and recover from upstream exceptions</li>
 *   <li><b>Timeout Handling:</b> Use timed {@code get()} methods or an external scheduler; {@code thenDelay()} only postpones the next stage</li>
 *   <li><b>Validation:</b> Use {@code getAsResult()} methods to safely retrieve results without exceptions</li>
 * </ul>
 *
 * <p><b>Usage Examples: Microservice Integration Pattern</b>
 * <pre>{@code
 * public class OrderProcessingService {
 *     private final UserService userService;
 *     private final InventoryService inventoryService;
 *     private final PaymentService paymentService;
 *
 *     public ContinuableFuture<OrderResult> processOrder(OrderRequest request) {
 *         // Parallel validation
 *         ContinuableFuture<User> userFuture = ContinuableFuture
 *             .call(() -> userService.validateUser(request.getUserId()));
 *
 *         ContinuableFuture<Boolean> inventoryFuture = ContinuableFuture
 *             .call(() -> inventoryService.checkAvailability(request.getItems()));
 *
 *         // Process after both validations complete
 *         return userFuture.callAsyncAfterBoth(inventoryFuture, (user, available) -> {
 *             if (!available) {
 *                 throw new OutOfStockException("Items not available");
 *             }
 *             return request;
 *         })
 *         .thenCallAsync(validatedRequest -> paymentService.processPayment(validatedRequest))
 *         .thenCallAsync(payment -> fulfillmentService.createShipment(payment))
 *         .map(shipment -> new OrderResult(shipment.getId(), "SUCCESS"))
 *         .thenCallAsync((result, ex) -> {
 *             if (ex != null) {
 *                 logger.error("Order processing failed", ex);
 *                 return new OrderResult(null, "FAILED: " + ex.getMessage());
 *             }
 *             return result;
 *         });
 *     }
 * }
 * }</pre>
 *
 * <p><b>Integration with Existing Futures:</b>
 * <ul>
 *   <li><b>CompletableFuture:</b> Can wrap and convert existing CompletableFuture instances</li>
 *   <li><b>Future:</b> Compatible with any Future implementation for result retrieval</li>
 *   <li><b>ExecutorService:</b> Works with any Executor implementation for custom threading</li>
 *   <li><b>Callable/Runnable:</b> Direct support for standard Java concurrency interfaces</li>
 * </ul>
 *
 * <p><b>Debugging and Monitoring:</b>
 * <ul>
 *   <li><b>Exception Stack Traces:</b> Preserves original exception information through chains</li>
 *   <li><b>Cancellation Propagation:</b> {@code cancelAll()} cancels the entire upstream chain</li>
 *   <li><b>State Inspection:</b> Standard Future methods for checking completion and cancellation state</li>
 * </ul>
 *
 * @param <T> the type of the value returned by this Future's {@code get} method.
 *
 * @see Future
 * @see CompletableFuture
 * @see Executor
 * @see Callable
 * @see Result
 * @see Futures
 * @see com.landawn.abacus.util.function.Function
 * @see com.landawn.abacus.util.Throwables
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html">CompletableFuture Documentation</a>
 * @see <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/Future.html">Future Documentation</a>
 */
public class ContinuableFuture<T> implements Future<T> {

    final Future<? extends T> future;

    final List<ContinuableFuture<?>> upFutures;

    final Executor asyncExecutor;

    ContinuableFuture(final Future<? extends T> future) {
        this(future, null, null);
    }

    ContinuableFuture(final Future<? extends T> future, final List<ContinuableFuture<?>> upFutures, final Executor asyncExecutor) {
        this.future = future;
        this.upFutures = upFutures;
        this.asyncExecutor = asyncExecutor == null ? N.ASYNC_EXECUTOR.getExecutor() : asyncExecutor;
    }

    /**
     * Executes the provided action asynchronously and returns a {@code ContinuableFuture} representing
     * the pending completion of the action. The action is executed using the default async executor.
     *
     * <p>This method is useful for fire-and-forget operations that don't return a value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Void> future = ContinuableFuture.run(() -> {
     *     System.out.println("Running async task");
     *     Thread.sleep(1000);
     * });
     * }</pre>
     *
     * @param action the action to be executed asynchronously; must not be {@code null}.
     * @return a {@code ContinuableFuture<Void>} representing the pending completion of the action.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see N#asyncExecute(Throwables.Runnable)
     */
    public static ContinuableFuture<Void> run(final Throwables.Runnable<? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        return run(action, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     * Executes the provided action asynchronously using the specified executor and returns a
     * {@code ContinuableFuture} representing the pending completion of the action.
     *
     * <p>This method allows you to specify a custom executor for running the action, which is
     * useful when you need specific thread pool characteristics or execution policies.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService customExecutor = Executors.newFixedThreadPool(4);
     * try {
     *     ContinuableFuture.run(() -> {
     *         // Heavy computation
     *         performComplexCalculation();
     *     }, customExecutor).get();
     * } finally {
     *     customExecutor.shutdown();
     * }
     * }</pre>
     *
     * @param action the action to be executed asynchronously; must not be {@code null}.
     * @param executor the executor to use for running the action; must not be {@code null}.
     * @return a {@code ContinuableFuture<Void>} representing the pending completion of the action.
     * @throws IllegalArgumentException if any of {@code action}, {@code executor} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public static ContinuableFuture<Void> run(final Throwables.Runnable<? extends Exception> action, final Executor executor)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(action, cs.action);
        N.checkArgNotNull(executor, cs.executor);

        final FutureTask<Void> futureTask = new FutureTask<>(() -> {
            action.run();
            return null;
        });

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Executes the provided callable action asynchronously and returns a {@code ContinuableFuture}
     * representing the pending result of the action. The action is executed using the default async executor.
     *
     * <p>This method is the primary way to start an asynchronous computation that returns a value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> {
     *     // Simulate some computation
     *     Thread.sleep(1000);
     *     return 42;
     * });
     *
     * // Get the result (blocks until complete)
     * Integer result = future.get();
     * }</pre>
     *
     * @param <T> the type of the result returned by the callable.
     * @param action the callable action to be executed asynchronously; must not be {@code null}.
     * @return a {@code ContinuableFuture<T>} representing the pending result of the action.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see N#asyncExecute(Callable)
     */
    public static <T> ContinuableFuture<T> call(final Callable<? extends T> action) throws IllegalArgumentException, RejectedExecutionException {
        return call(action, N.ASYNC_EXECUTOR.getExecutor());
    }

    /**
     * Executes the provided callable action asynchronously using the specified executor and returns
     * a {@code ContinuableFuture} representing the pending result of the action.
     *
     * <p>This method allows you to specify a custom executor for running the callable, providing
     * control over thread pool characteristics and execution policies.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
     * try {
     *     String result = ContinuableFuture.call(() -> fetchDataFromService(), scheduler).get();
     * } finally {
     *     scheduler.shutdown();
     * }
     * }</pre>
     *
     * @param <T> the type of the result returned by the callable.
     * @param action the callable action to be executed asynchronously; must not be {@code null}.
     * @param executor the executor to use for running the action; must not be {@code null}.
     * @return a {@code ContinuableFuture<T>} representing the pending result of the action.
     * @throws IllegalArgumentException if any of {@code action}, {@code executor} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public static <T> ContinuableFuture<T> call(final Callable<? extends T> action, final Executor executor)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(action, cs.action);
        N.checkArgNotNull(executor, cs.executor);

        final FutureTask<? extends T> futureTask = new FutureTask<>(action);

        executor.execute(futureTask);

        return new ContinuableFuture<>(futureTask, null, executor);
    }

    /**
     * Returns a {@code ContinuableFuture} that is already completed with the provided result.
     * This is useful for creating a future that represents an immediately available value,
     * often used in testing or when converting synchronous code to asynchronous patterns.
     *
     * <p>The returned future:
     * <ul>
     *   <li>Cannot be cancelled (returns false)</li>
     *   <li>Is always done (returns true)</li>
     *   <li>Returns the provided result immediately from get() methods</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a pre-completed future
     * ContinuableFuture<String> future = ContinuableFuture.completed("Hello");
     *
     * // This doesn't block and returns immediately
     * String result = future.get();   // returns "Hello"
     *
     * // Useful for conditional async operations
     * ContinuableFuture<Data> loadData(boolean useCache) {
     *     if (useCache && cache.contains(key)) {
     *         return ContinuableFuture.completed(cache.get(key));
     *     }
     *     return ContinuableFuture.call(() -> fetchFromDatabase(key));
     * }
     * }</pre>
     *
     * @param <T> the type of the result.
     * @param result the result that the future should be completed with.
     * @return a {@code ContinuableFuture} that is already completed with the provided result.
     */
    public static <T> ContinuableFuture<T> completed(final T result) {
        return new ContinuableFuture<>(new CompletedFuture<>(result));
    }

    /**
     * The delegate of {@link #completed(Object)}: already done, never cancellable, and its {@code get()} only
     * hands back the stored value. A named class (rather than an anonymous one) lets {@link #hasInstantOutcome()}
     * recognise it as a delegate whose outcome can be read inline by {@link Futures}.
     *
     * @param <T> the type of the stored result
     */
    private static final class CompletedFuture<T> implements Future<T> {
        private final T result;

        CompletedFuture(final T result) {
            this.result = result;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public T get() {
            return result;
        }

        @Override
        public T get(final long timeout, final TimeUnit unit) {
            N.requireNonNull(unit, cs.unit);
            return result;
        }
    }

    /**
     * Tells whether this future is done and its {@code get()} does nothing but read a stored outcome: a plain
     * {@code ContinuableFuture} (not a {@code map}/{@code thenDelay}/{@code thenUse} wrapper, which run user code
     * or wait inside {@code get()}) over an exact {@link FutureTask}, an exact {@link CompletableFuture} or a
     * {@link #completed(Object)} delegate. {@link Futures} reads such inputs on the calling thread instead of
     * dedicating a relay thread to them.
     *
     * @return {@code true} if a call to {@code get()} on this future returns or throws immediately without running
     *         user code
     */
    boolean hasInstantOutcome() {
        // isDone() first, for every instance including the map/thenDelay/thenUse wrappers: a registration failure
        // raised by a custom delegate's isDone() must surface (and be retryable) whatever wraps it.
        return isDone() && getClass() == ContinuableFuture.class
                && (future.getClass() == FutureTask.class || future.getClass() == CompletableFuture.class || future instanceof CompletedFuture);
    }

    /**
     * Wraps an existing {@code Future} into a {@code ContinuableFuture}, enabling the use of
     * composition and chaining methods. This is useful when integrating with APIs that return
     * standard {@code Future} objects.
     *
     * <p>The wrapped future retains all the characteristics of the original future, including
     * its execution state, result, and cancellation behavior. A {@code ContinuableFuture} argument is
     * returned <i>unchanged</i> (as {@code CompletableFuture.toCompletableFuture()} returns {@code this}): its
     * executor and its upstream links stay intact, so {@code cancelAll()}/{@code isAllCancelled()} still reach the
     * whole chain and later stages keep running on the executor chosen with {@code thenUse}. Wrapping it in a new
     * node instead would silently sever both.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Working with ExecutorService that returns Future
     * ExecutorService executor = Executors.newFixedThreadPool(4);
     * try {
     *     Future<String> standardFuture = executor.submit(() -> "Hello");
     *
     *     // Wrap it to use ContinuableFuture features
     *     ContinuableFuture<String> continuable = ContinuableFuture.wrap(standardFuture);
     *
     *     // Wait for the terminal stage before shutting down its executor.
     *     continuable.thenRunAsync(result -> System.out.println("Got: " + result))
     *                .thenCallAsync(() -> processNextStep())
     *                .get();
     * } finally {
     *     executor.shutdown();
     * }
     * }</pre>
     *
     * @param <T> the type of the value returned by the future.
     * @param future the future to wrap; must not be {@code null}.
     * @return a {@code ContinuableFuture} that wraps the provided future, or {@code future} itself when it already
     *         is a {@code ContinuableFuture}.
     * @throws IllegalArgumentException if {@code future} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> ContinuableFuture<T> wrap(final Future<? extends T> future) throws IllegalArgumentException {
        N.checkArgNotNull(future, cs.future);

        if (future instanceof ContinuableFuture<?> continuableFuture) {
            // Identity, not a new node: a fresh wrapper has no upstream links and the default executor, so
            // cancelAll()/isAllCancelled() would stop here and the next stage would leave the thenUse executor.
            // The cast is sound: T only ever appears in producer positions of this class (get, map, callbacks
            // taking ? super T), so a ContinuableFuture<? extends T> can be used as a ContinuableFuture<T>.
            return (ContinuableFuture<T>) continuableFuture;
        }

        return new ContinuableFuture<>(future);
    }

    /**
     * Attempts to cancel execution of this task. This method follows the standard {@link Future#cancel(boolean)}
     * contract. If the task has already completed, has already been cancelled, or could not be
     * cancelled for some other reason, this attempt will fail.
     *
     * <p>If cancellation succeeds, subsequent calls to {@link #isDone()} will always return {@code true}.
     * Subsequent calls to {@link #isCancelled()} will always return {@code true} if this method returned {@code true}.
     *
     * <p><b>Note:</b> This method only cancels this future's own task, not the upstream stages that run as
     * separate tasks. To cancel the entire chain, use {@link #cancelAll(boolean)}. The lazy wrappers returned by
     * {@link #map(Throwables.Function)}, {@link #thenDelay(long, TimeUnit)} and {@link #thenUse(Executor)} share
     * their source's task, so cancelling either the wrapper or its source cancels both while that task is pending;
     * a {@code thenDelay} stage whose upstream has already completed can additionally be cancelled on its own
     * during its delay window, leaving the upstream result available.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> {
     *     Thread.sleep(5000);
     *     return "Result";
     * });
     *
     * // Cancel after 1 second
     * Thread.sleep(1000);
     * boolean cancelled = future.cancel(true);   // Interrupt if running
     * }</pre>
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this task should be interrupted;
     *                              otherwise, in-progress tasks are allowed to complete.
     * @return {@code false} if the task could not be cancelled, typically because it has already
     *         completed normally; {@code true} otherwise.
     * @see Future#cancel(boolean)
     */
    @Override
    public boolean cancel(final boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * Returns {@code true} if this task was cancelled before it completed normally.
     * A task that has been cancelled will never complete normally and will throw
     * a {@code CancellationException} when {@link #get()} is called.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> longRunningTask());
     *
     * // In another thread
     * future.cancel(true);
     *
     * if (future.isCancelled()) {
     *     System.out.println("Task was cancelled");
     * }
     * }</pre>
     *
     * @return {@code true} if this task was cancelled before it completed.
     * @see Future#isCancelled()
     */
    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    /**
     * Cancels this future and all distinct upstream futures in the dependency graph recursively. This method is useful
     * when you have a chain of dependent futures and want to cancel the entire computation pipeline.
     *
     * <p>The method attempts to cancel all futures in the chain and returns {@code true} only if
     * all cancellations were successful. If any future in the chain fails to cancel, the method
     * still attempts to cancel the remaining futures. A stage that had already completed cannot be cancelled, so
     * the result is {@code false} as soon as any stage of the chain is complete - even when every stage that was
     * still pending was cancelled; the return value does not distinguish "nothing left to cancel" from "could not
     * cancel".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future1 = ContinuableFuture.call(() -> fetchData());
     * ContinuableFuture<String> future2 = future1.thenCallAsync(data -> processData(data));
     * ContinuableFuture<Void> future3 = future2.thenRunAsync(result -> saveResult(result));
     *
     * // This cancels future3, future2, and future1
     * boolean allCancelled = future3.cancelAll(true);
     * }</pre>
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing the tasks should be interrupted;
     *                              otherwise, in-progress tasks are allowed to complete.
     * @return {@code true} if all futures in the chain were successfully cancelled; {@code false} if
     *         any future failed to cancel.
     * @see #cancel(boolean)
     * @see Future#cancel(boolean)
     */
    public boolean cancelAll(final boolean mayInterruptIfRunning) {
        IdentityHashMap<ContinuableFuture<?>, Boolean> results = CANCELLATION_RESULTS.get();
        final boolean outermost = results == null;
        if (outermost) {
            results = new IdentityHashMap<>();
            CANCELLATION_RESULTS.set(results);
        }
        try {
            if (results.containsKey(this)) {
                return results.get(this);
            }
            results.put(this, true);
            boolean cancelled = cancel(mayInterruptIfRunning);
            if (upFutures != null) {
                for (final ContinuableFuture<?> previous : upFutures) {
                    final boolean previousCancelled = results.containsKey(previous) ? results.get(previous) : previous.cancelAll(mayInterruptIfRunning);
                    results.put(previous, previousCancelled);
                    cancelled &= previousCancelled;
                }
            }
            results.put(this, cancelled);
            return cancelled;
        } finally {
            if (outermost) {
                CANCELLATION_RESULTS.remove();
            }
        }
    }

    // Invocation-scoped memoization follows virtual calls, including transparent map/delay/executor wrappers.
    // Keeping the context through those calls preserves subclass behavior and visits shared ancestors once.
    private static final ThreadLocal<IdentityHashMap<ContinuableFuture<?>, Boolean>> CANCELLATION_RESULTS = new ThreadLocal<>();
    private static final ThreadLocal<IdentityHashMap<ContinuableFuture<?>, Boolean>> CANCELLATION_STATUS_RESULTS = new ThreadLocal<>();

    /**
     * Checks if this task and all upstream futures in the chain have been cancelled. This method
     * recursively checks each distinct future's cancellation status once per invocation.
     *
     * <p>Returns {@code true} only if every future in the chain has been cancelled. If any future
     * in the chain is not cancelled, this method returns {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future1 = ContinuableFuture.call(() -> step1());
     * ContinuableFuture<String> future2 = future1.thenCallAsync(data -> step2(data));
     *
     * future2.cancelAll(true);
     *
     * if (future2.isAllCancelled()) {
     *     System.out.println("Entire chain was cancelled");
     * }
     * }</pre>
     *
     * @return {@code true} if all futures in the chain have been cancelled; {@code false} otherwise.
     * @see #isCancelled()
     * @see Future#isCancelled()
     */
    public boolean isAllCancelled() {
        IdentityHashMap<ContinuableFuture<?>, Boolean> results = CANCELLATION_STATUS_RESULTS.get();
        final boolean outermost = results == null;
        if (outermost) {
            results = new IdentityHashMap<>();
            CANCELLATION_STATUS_RESULTS.set(results);
        }
        try {
            if (results.containsKey(this)) {
                return results.get(this);
            }
            results.put(this, true);
            if (upFutures != null) {
                for (final ContinuableFuture<?> previous : upFutures) {
                    final boolean previousCancelled = results.containsKey(previous) ? results.get(previous) : previous.isAllCancelled();
                    results.put(previous, previousCancelled);
                    if (!previousCancelled) {
                        results.put(this, false);
                        return false;
                    }
                }
            }
            final boolean cancelled = isCancelled();
            results.put(this, cancelled);
            return cancelled;
        } finally {
            if (outermost) {
                CANCELLATION_STATUS_RESULTS.remove();
            }
        }
    }

    /**
     * Returns {@code true} if this task completed. Completion may be due to normal termination,
     * an exception, or cancellation -- in all of these cases, this method will return {@code true}.
     *
     * <p>A completed future will never transition to any other state.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> "Done");
     *
     * while (!future.isDone()) {
     *     System.out.println("Still processing...");
     *     Thread.sleep(100);
     * }
     * System.out.println("Task completed!");
     * }</pre>
     *
     * @return {@code true} if this task completed.
     * @see Future#isDone()
     */
    @Override
    public boolean isDone() {
        return future.isDone();
    }

    /**
     * Waits if necessary for the computation to complete, and then retrieves its result.
     * This method blocks the calling thread until the future completes.
     *
     * <p>If the computation was cancelled, this method throws a {@code CancellationException}.
     * If the computation threw an exception, this method throws an {@code ExecutionException}
     * with the original exception as its cause.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> {
     *     Thread.sleep(1000);
     *     return 42;
     * });
     *
     * try {
     *     Integer result = future.get();   // Blocks for ~1 second
     *     System.out.println("Result: " + result);
     * } catch (InterruptedException e) {
     *     Thread.currentThread().interrupt();
     * } catch (ExecutionException e) {
     *     System.err.println("Computation failed: " + e.getCause());
     * }
     * }</pre>
     *
     * @return the computed result.
     * @throws InterruptedException if the current thread was interrupted while waiting.
     * @throws CancellationException if the computation was cancelled.
     * @throws ExecutionException if the computation threw an exception.
     * @see Future#get()
     */
    @Override
    public T get() throws InterruptedException, CancellationException, ExecutionException {
        return future.get();
    }

    /**
     * Waits if necessary for at most the given time for the computation to complete,
     * and then retrieves its result, if available.
     *
     * <p>This method blocks the calling thread until:
     * <ul>
     *   <li>The future completes (normally or exceptionally)</li>
     *   <li>The timeout expires</li>
     *   <li>The thread is interrupted</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> fetchFromSlowService());
     *
     * try {
     *     // Wait maximum 5 seconds
     *     String result = future.get(5, TimeUnit.SECONDS);
     *     System.out.println("Got result: " + result);
     * } catch (TimeoutException e) {
     *     System.err.println("Operation timed out");
     *     future.cancel(true);   // Cancel the operation
     * }
     * }</pre>
     *
     * @param timeout the maximum time to wait; a non-positive value polls without waiting.
     * @param unit the time unit of the timeout argument; must not be {@code null}.
     * @return the computed result.
     * @throws NullPointerException if {@code unit} is {@code null} (the {@link Future} contract; the abacus-only
     *         timed getters of this class report it as {@link IllegalArgumentException} instead).
     * @throws InterruptedException if the current thread was interrupted while waiting.
     * @throws TimeoutException if the wait timed out.
     * @throws CancellationException if the computation was cancelled.
     * @throws ExecutionException if the computation threw an exception.
     * @see Future#get(long, TimeUnit)
     */
    @Override
    public T get(final long timeout, final TimeUnit unit)
            throws NullPointerException, InterruptedException, TimeoutException, CancellationException, ExecutionException {
        return future.get(timeout, unit);
    }

    /**
     * Retrieves the result of the computation when it completes, wrapping both the result
     * and any exception into a {@link Result} object. This method never throws checked exceptions,
     * making it convenient for use in lambda expressions and functional chains.
     *
     * <p>The returned {@code Result} object encapsulates either:
     * <ul>
     *   <li>The successful result of the computation</li>
     *   <li>The exception that occurred during computation or while waiting</li>
     * </ul>
     * If waiting is interrupted, the {@link InterruptedException} is returned as the failure and
     * the current thread's interrupt status is restored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> riskyOperation());
     *
     * Result<String, Exception> result = future.getAsResult();
     *
     * if (result.isSuccess()) {
     *     System.out.println("Success: " + result.orElseThrow());
     * } else {
     *     System.err.println("Failed: " + result.getException());
     * }
     *
     * // Or supply a fallback value to use if the computation failed
     * String value = result.orElseIfFailure("default value");
     * }</pre>
     *
     * @return a {@code Result} object containing either the computed result or the exception.
     */
    public Result<T, Exception> getAsResult() {
        try {
            return Result.of(get(), null);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.of(null, e);
        } catch (final Exception e) {
            return Result.of(null, Futures.convertException(e));
        }
    }

    /**
     * Retrieves the result of the computation when it completes within the specified timeout,
     * wrapping both the result and any exception into a {@link Result} object. This method
     * never throws checked exceptions.
     *
     * <p>This is the timeout version of {@link #getAsResult()}, useful when you want to limit
     * the waiting time but still handle results in a functional style without checked exceptions.
     * If waiting is interrupted, the {@link InterruptedException} is returned as the failure and
     * the current thread's interrupt status is restored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Data> future = ContinuableFuture.call(() -> fetchData());
     *
     * Result<Data, Exception> result = future.getAsResult(10, TimeUnit.SECONDS);
     *
     * Data data;
     * if (result.isFailure()) {
     *     data = (result.getException() instanceof TimeoutException)
     *         ? getCachedData()
     *         : getDefaultData();
     * } else {
     *     data = result.orElseThrow();
     * }
     * }</pre>
     *
     * @param timeout the maximum time to wait.
     * @param unit the time unit of the timeout argument; must not be {@code null}.
     * @return a {@code Result} object containing either the computed result or the exception.
     * @throws IllegalArgumentException if {@code unit} is {@code null}; an argument error is not a failed computation.
     */
    public Result<T, Exception> getAsResult(final long timeout, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgNotNull(unit, cs.unit);

        try {
            return Result.of(get(timeout, unit), null);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.of(null, e);
        } catch (final Exception e) {
            return Result.of(null, Futures.convertException(e));
        }
    }

    /**
     * Returns the result value if the computation is already complete, otherwise returns
     * the provided default value without calling {@code get()}. This method is useful for polling
     * or providing immediate fallback values. When already done, it calls {@code get()}, which can still
     * execute a lazy {@link #map(Throwables.Function)} transformation on the calling thread.
     *
     * <p>Note that this method still throws exceptions if the future is done but completed
     * exceptionally. Use {@link #getAsResult()} for exception-safe result retrieval that
     * wraps any failure in a {@link Result} instead of throwing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> slowComputation());
     *
     * // Check immediately without blocking
     * String result = future.getNow("Computing...");
     * System.out.println(result);   // Prints "Computing..." if not done
     *
     * // Polling pattern
     * while (true) {
     *     String current = future.getNow(null);
     *     if (current != null) {
     *         System.out.println("Got result: " + current);
     *         break;
     *     }
     *     Thread.sleep(100);
     * }
     * }</pre>
     *
     * @param defaultValue the value to return if the computation is not yet complete.
     * @return the computed result if the computation is already complete, otherwise {@code defaultValue}.
     * @throws InterruptedException if the current thread was interrupted (propagated from the underlying {@link Future#get()} call).
     * @throws CancellationException if the computation was cancelled.
     * @throws ExecutionException if the computation threw an exception.
     */
    public T getNow(final T defaultValue) throws InterruptedException, CancellationException, ExecutionException {
        if (isDone()) {
            return get();
        }

        return defaultValue;
    }

    /**
     * Waits for the computation to complete and then applies the provided function to the result.
     * This method blocks until the future completes, then synchronously applies the function.
     *
     * <p>This is a convenience method that combines {@link #get()} with function application,
     * useful for transforming results in a blocking manner.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> 42);
     *
     * // Transform the result synchronously after completion
     * String result = future.getThenApply(num -> "The answer is: " + num);
     * System.out.println(result);   // prints "The answer is: 42"
     *
     * // Can throw checked exceptions
     * Data processed = future.getThenApply(num -> {
     *     if (num < 0) throw new IllegalArgumentException("Negative!");
     *     return processNumber(num);
     * });
     * }</pre>
     *
     * @param <U> the type of the result of the function.
     * @param <E> the type of exception the function may throw.
     * @param action the function to apply to the result.
     * @return the result of applying the function to the computed result.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws InterruptedException if the current thread was interrupted while waiting.
     * @throws CancellationException if the computation was cancelled.
     * @throws ExecutionException if the computation threw an exception.
     * @throws E if the function throws an exception.
     */
    public <U, E extends Exception> U getThenApply(final Throwables.Function<? super T, ? extends U, E> action)
            throws IllegalArgumentException, InterruptedException, CancellationException, ExecutionException, E {
        N.checkArgNotNull(action, cs.action);

        return action.apply(get());
    }

    /**
     * Waits for the computation to complete within the specified timeout and then applies
     * the provided function to the result. This method blocks until the future completes
     * or the timeout expires. It applies the function synchronously only after successful retrieval.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> fetchData());
     *
     * try {
     *     // Wait max 5 seconds, then parse the result
     *     JsonObject json = future.getThenApply(5, TimeUnit.SECONDS,
     *         data -> parseJson(data));
     * } catch (TimeoutException e) {
     *     // Handle timeout
     * }
     * }</pre>
     *
     * @param <U> the type of the result of the function.
     * @param <E> the type of exception the function may throw.
     * @param timeout the maximum time to wait.
     * @param unit the time unit of the timeout argument; must not be {@code null}.
     * @param action the function to apply to the result.
     * @return the result of applying the function to the computed result.
     * @throws IllegalArgumentException if any of {@code unit}, {@code action} is {@code null}.
     * @throws InterruptedException if the current thread was interrupted while waiting.
     * @throws TimeoutException if the wait timed out.
     * @throws CancellationException if the computation was cancelled.
     * @throws ExecutionException if the computation threw an exception.
     * @throws E if the function throws an exception.
     */
    public <U, E extends Exception> U getThenApply(final long timeout, final TimeUnit unit, final Throwables.Function<? super T, ? extends U, E> action)
            throws IllegalArgumentException, InterruptedException, TimeoutException, CancellationException, ExecutionException, E {
        N.checkArgNotNull(unit, cs.unit);
        N.checkArgNotNull(action, cs.action);

        return action.apply(get(timeout, unit));
    }

    /**
     * Waits for the computation to complete and then applies the provided bi-function to both
     * the result (if successful) and any exception that occurred. This method never throws
     * the computation's exception, instead passing it to the bi-function.
     *
     * <p>This method is useful for handling both success and failure cases in a unified way,
     * without needing try-catch blocks.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> {
     *     if (Math.random() > 0.5) throw new RuntimeException("Bad luck!");
     *     return 42;
     * });
     *
     * String message = future.getThenApply((result, exception) -> {
     *     if (exception != null) {
     *         return "Failed: " + exception.getMessage();
     *     }
     *     return "Success: " + result;
     * });
     * }</pre>
     *
     * @param <U> the type of the result of the function.
     * @param <E> the type of exception the function may throw.
     * @param action the bi-function to apply to the result and exception.
     * @return the result of applying the function.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws E if the bi-function throws an exception.
     * @see #getAsResult()
     */
    public <U, E extends Exception> U getThenApply(final Throwables.BiFunction<? super T, ? super Exception, ? extends U, E> action)
            throws IllegalArgumentException, E {
        N.checkArgNotNull(action, cs.action);

        final Result<T, Exception> result = getAsResult();
        return action.apply(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Waits for the computation to complete within the specified timeout and then applies
     * the provided bi-function to both the result (if successful) and any exception that occurred.
     * This method never throws the computation's exception, instead passing it to the bi-function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Data> future = ContinuableFuture.call(() -> fetchFromSlowService());
     *
     * Response response = future.getThenApply(10, TimeUnit.SECONDS, (data, exception) -> {
     *     if (exception instanceof TimeoutException) {
     *         return Response.timeout();
     *     } else if (exception != null) {
     *         return Response.error(exception);
     *     }
     *     return Response.success(data);
     * });
     * }</pre>
     *
     * @param <U> the type of the result of the function.
     * @param <E> the type of exception the function may throw.
     * @param timeout the maximum time to wait.
     * @param unit the time unit of the timeout argument; must not be {@code null}.
     * @param action the bi-function to apply to the result and exception.
     * @return the result of applying the function.
     * @throws IllegalArgumentException if any of {@code unit}, {@code action} is {@code null}; an argument error
     *         is not handed to the bi-function as a failure.
     * @throws E if the bi-function throws an exception.
     * @see #getAsResult(long, TimeUnit)
     */
    public <U, E extends Exception> U getThenApply(final long timeout, final TimeUnit unit,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends U, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(unit, cs.unit);
        N.checkArgNotNull(action, cs.action);

        final Result<T, Exception> result = getAsResult(timeout, unit);
        return action.apply(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Waits for the computation to complete and then consumes the result with the provided consumer.
     * This method blocks until the future completes, then synchronously executes the consumer.
     *
     * <p>This is useful for side effects like logging, updating UI, or triggering other actions
     * based on the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> downloadFile());
     *
     * // Process the result when ready
     * future.getThenAccept(filePath -> {
     *     System.out.println("Downloaded to: " + filePath);
     *     processFile(filePath);
     * });
     * }</pre>
     *
     * @param <E> the type of exception the consumer may throw.
     * @param action the consumer to execute with the result.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws InterruptedException if the current thread was interrupted while waiting.
     * @throws CancellationException if the computation was cancelled.
     * @throws ExecutionException if the computation threw an exception.
     * @throws E if the consumer throws an exception.
     */
    public <E extends Exception> void getThenAccept(final Throwables.Consumer<? super T, E> action)
            throws IllegalArgumentException, InterruptedException, CancellationException, ExecutionException, E {
        N.checkArgNotNull(action, cs.action);

        action.accept(get());
    }

    /**
     * Waits for the computation to complete within the specified timeout and then consumes
     * the result with the provided consumer. This method blocks until the future completes
     * or the timeout expires. It executes the consumer synchronously only after successful retrieval.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<List<String>> future = ContinuableFuture.call(() -> fetchLogs());
     *
     * try {
     *     future.getThenAccept(30, TimeUnit.SECONDS, logs -> {
     *         logs.forEach(System.out::println);
     *         archiveLogs(logs);
     *     });
     * } catch (TimeoutException e) {
     *     System.err.println("Log fetch timed out");
     * }
     * }</pre>
     *
     * @param <E> the type of exception the consumer may throw.
     * @param timeout the maximum time to wait.
     * @param unit the time unit of the timeout argument; must not be {@code null}.
     * @param action the consumer to execute with the result.
     * @throws IllegalArgumentException if any of {@code unit}, {@code action} is {@code null}.
     * @throws InterruptedException if the current thread was interrupted while waiting.
     * @throws TimeoutException if the wait timed out.
     * @throws CancellationException if the computation was cancelled.
     * @throws ExecutionException if the computation threw an exception.
     * @throws E if the consumer throws an exception.
     */
    public <E extends Exception> void getThenAccept(final long timeout, final TimeUnit unit, final Throwables.Consumer<? super T, E> action)
            throws IllegalArgumentException, InterruptedException, TimeoutException, CancellationException, ExecutionException, E {
        N.checkArgNotNull(unit, cs.unit);
        N.checkArgNotNull(action, cs.action);

        action.accept(get(timeout, unit));
    }

    /**
     * Waits for the computation to complete and then consumes both the result (if successful)
     * and any exception that occurred with the provided bi-consumer. This method never throws
     * the computation's exception, instead passing it to the bi-consumer.
     *
     * <p>This method is useful for handling both success and failure cases with side effects,
     * such as logging or updating state based on the outcome.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> future = ContinuableFuture.call(() -> fetchUser(userId));
     *
     * future.getThenAccept((user, exception) -> {
     *     if (exception != null) {
     *         logger.error("Failed to fetch user " + userId, exception);
     *         notifyError(exception);
     *     } else {
     *         logger.info("Fetched user: " + user.getName());
     *         updateCache(user);
     *     }
     * });
     * }</pre>
     *
     * @param <E> the type of exception the bi-consumer may throw.
     * @param action the bi-consumer to execute with the result and exception.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws E if the bi-consumer throws an exception.
     * @see #getAsResult()
     */
    public <E extends Exception> void getThenAccept(final Throwables.BiConsumer<? super T, ? super Exception, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action, cs.action);

        final Result<T, Exception> result = getAsResult();
        action.accept(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Waits for the computation to complete within the specified timeout and then consumes
     * both the result (if successful) and any exception that occurred with the provided bi-consumer.
     * This method never throws the computation's exception, instead passing it to the bi-consumer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Config> future = ContinuableFuture.call(() -> loadConfig());
     *
     * future.getThenAccept(5, TimeUnit.SECONDS, (config, exception) -> {
     *     if (exception instanceof TimeoutException) {
     *         useDefaultConfig();
     *     } else if (exception != null) {
     *         handleConfigError(exception);
     *     } else {
     *         applyConfig(config);
     *     }
     * });
     * }</pre>
     *
     * @param <E> the type of exception the bi-consumer may throw.
     * @param timeout the maximum time to wait.
     * @param unit the time unit of the timeout argument; must not be {@code null}.
     * @param action the bi-consumer to execute with the result and exception.
     * @throws IllegalArgumentException if any of {@code unit}, {@code action} is {@code null}; an argument error
     *         is not handed to the bi-consumer as a failure.
     * @throws E if the bi-consumer throws an exception.
     * @see #getAsResult(long, TimeUnit)
     */
    public <E extends Exception> void getThenAccept(final long timeout, final TimeUnit unit,
            final Throwables.BiConsumer<? super T, ? super Exception, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(unit, cs.unit);
        N.checkArgNotNull(action, cs.action);

        final Result<T, Exception> result = getAsResult(timeout, unit);
        action.accept(result.orElseIfFailure(null), result.getException());
    }

    /**
     * Returns a new {@code ContinuableFuture} whose result is produced by applying the provided
     * function to the result of this future. The function is applied <b>lazily and synchronously</b>
     * inside each call to {@code get()}/{@code get(timeout, unit)} on the returned future, on the
     * thread that invokes {@code get()} (not on this future's executor).
     *
     * <p>Because the transformation is lazy, the returned future's {@link #isDone()},
     * {@link #isCancelled()} and cancellation behavior simply mirror this future; if you need
     * the function executed asynchronously on the configured executor, use
     * {@link #thenCallAsync(Throwables.Function)} instead.
     *
     * <p>If the function throws, {@code get()} reports the failure through {@link ExecutionException},
     * matching the {@link Future} contract.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Integer> future = ContinuableFuture.call(() -> 21);
     *
     * // Transformation is applied when get() is called
     * ContinuableFuture<String> stringFuture = future.map(num -> "The result is: " + (num * 2));
     *
     * System.out.println(stringFuture.get());   // prints "The result is: 42"
     * }</pre>
     *
     * @param <U> the type of the transformed result.
     * @param function the function to apply to the result.
     * @return a new {@code ContinuableFuture} that lazily applies the function on {@code get()}.
     * @throws IllegalArgumentException if {@code function} is {@code null}.
     * @see #thenCallAsync(Throwables.Function)
     */
    @Beta
    public <U> ContinuableFuture<U> map(final Throwables.Function<? super T, ? extends U, ? extends Exception> function) throws IllegalArgumentException {
        N.checkArgNotNull(function, cs.function);

        return new ContinuableFuture<>(new Future<U>() {
            @Override
            public boolean cancel(final boolean mayInterruptIfRunning) {
                return ContinuableFuture.this.cancel(mayInterruptIfRunning);
            }

            @Override
            public boolean isCancelled() {
                return ContinuableFuture.this.isCancelled();
            }

            @Override
            public boolean isDone() {
                return ContinuableFuture.this.isDone();
            }

            @Override
            public U get() throws InterruptedException, ExecutionException {
                final T ret = ContinuableFuture.this.get();

                try {
                    return function.apply(ret);
                } catch (final Throwable e) {
                    throw new ExecutionException(e);
                }
            }

            @Override
            public U get(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
                final T ret = ContinuableFuture.this.get(timeout, unit);

                try {
                    return function.apply(ret);
                } catch (final Throwable e) {
                    throw new ExecutionException(e);
                }
            }
        }, null, asyncExecutor) {
            @Override
            public boolean cancelAll(final boolean mayInterruptIfRunning) {
                return ContinuableFuture.this.cancelAll(mayInterruptIfRunning);
            }

            @Override
            public boolean isAllCancelled() {
                return ContinuableFuture.this.isAllCancelled();
            }
        };
    }

    /**
     * Executes the provided action asynchronously after this future completes. The action
     * is executed using the configured executor of this future.
     *
     * <p>This method returns a new {@code ContinuableFuture<Void>} that
     * completes when the action finishes executing. The action is only executed after
     * this future completes successfully. If this future fails or is cancelled, the action is not run and the
     * returned future's {@code get()} throws {@link ExecutionException} whose cause is this future's own failure
     * (see the class-level "Failure shape": the original cause, never a nested {@code ExecutionException}) or its
     * {@link CancellationException}; use the {@code BiConsumer} overload to handle the failure instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture.call(() -> downloadFile())
     *     .thenRunAsync(() -> {
     *         System.out.println("Download complete!");
     *         notifyUser();
     *     })
     *     .thenRunAsync(() -> cleanupTempFiles());
     * }</pre>
     *
     * @param action the action to execute after this future completes.
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public ContinuableFuture<Void> thenRunAsync(final Throwables.Runnable<? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            awaitValue(this);
            action.run();
            return null;
        });
    }

    /**
     * Executes the provided consumer asynchronously after this future completes, passing
     * the result to the consumer. The consumer is executed using the configured executor
     * of this future.
     *
     * <p>This method returns a new {@code ContinuableFuture<Void>} that
     * completes when the consumer finishes executing. The consumer receives the result
     * of this future if it completes successfully. If this future fails or is cancelled, the consumer is not run
     * and the returned future's {@code get()} throws {@link ExecutionException} whose cause is this future's own
     * failure (never a nested {@code ExecutionException}) or its {@link CancellationException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture.call(() -> fetchUserData(userId))
     *     .thenRunAsync(userData -> {
     *         updateUI(userData);
     *         saveToCache(userData);
     *     })
     *     .thenRunAsync(() -> logCompletion());
     * }</pre>
     *
     * @param action the consumer to execute with the result.
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public ContinuableFuture<Void> thenRunAsync(final Throwables.Consumer<? super T, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            action.accept(awaitValue(this));
            return null;
        });
    }

    /**
     * Executes the provided bi-consumer asynchronously after this future completes, passing
     * both the result (if successful) and any exception that occurred. The bi-consumer is
     * executed using the configured executor of this future.
     *
     * <p>This method returns a new {@code ContinuableFuture<Void>} that
     * completes when the bi-consumer finishes executing. The bi-consumer always executes,
     * regardless of whether this future completed normally or exceptionally. This is useful
     * for handling both success and failure cases in the asynchronous chain without breaking
     * the flow. If the worker waiting for this future is interrupted before this future completes
     * (for example by {@code cancel(true)} on the returned future), the bi-consumer is not executed:
     * the interruption ends the returned stage rather than being reported as a failure of this future.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture.call(() -> riskyOperation())
     *     .thenRunAsync((result, exception) -> {
     *         if (exception != null) {
     *             logger.error("Operation failed", exception);
     *             sendAlert(exception);
     *         } else {
     *             logger.info("Operation succeeded: " + result);
     *             processResult(result);
     *         }
     *     })
     *     .thenRunAsync(() -> cleanup());
     * }</pre>
     *
     * @param action the bi-consumer to execute with the result and exception.
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #getAsResult()
     */
    public ContinuableFuture<Void> thenRunAsync(final Throwables.BiConsumer<? super T, ? super Exception, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this); // never getAsResult here - see awaitResult
            action.accept(result.orElseIfFailure(null), result.getException());
            return null;
        });
    }

    /**
     * Executes the provided callable asynchronously after this future completes. The callable
     * is executed using the configured executor of this future, and its result becomes the
     * result of the returned future.
     *
     * <p>This method returns a new {@code ContinuableFuture} that completes
     * with the result of the callable. The callable is only executed after this future
     * completes successfully. If this future fails or is cancelled, the callable is not run and the returned
     * future's {@code get()} throws {@link ExecutionException} whose cause is this future's own failure (never a
     * nested {@code ExecutionException}) or its {@link CancellationException}; use the {@code BiFunction} overload
     * to recover instead.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<String> future = ContinuableFuture.call(() -> authenticate())
     *     .thenCallAsync(() -> {
     *         // This runs after authentication succeeds
     *         return fetchSecureData();
     *     })
     *     .thenCallAsync(() -> processData());
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable.
     * @param action the callable to execute after this future completes.
     * @return a new {@code ContinuableFuture<R>} with the result of the callable.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <R> ContinuableFuture<R> thenCallAsync(final Callable<? extends R> action) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            awaitValue(this);
            return action.call();
        });
    }

    /**
     * Executes the provided function asynchronously after this future completes, transforming
     * the result. The function is executed using the configured executor of this future and
     * receives the result of this future. The function's return value becomes the result of
     * the returned future.
     *
     * <p>This method returns a new {@code ContinuableFuture} that completes
     * with the transformed result. This method is similar to {@link #map(Throwables.Function)}
     * but executes asynchronously in the configured executor rather than synchronously when
     * get() is called. If this future fails or is cancelled, the function is not run and the returned future's
     * {@code get()} throws {@link ExecutionException} whose cause is this future's own failure (never a nested
     * {@code ExecutionException}, however long the chain) or its {@link CancellationException}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> userFuture = ContinuableFuture.call(() -> fetchUser(id))
     *     .thenCallAsync(user -> {
     *         // Transform User to UserProfile
     *         return buildProfile(user);
     *     })
     *     .thenCallAsync(profile -> enrichWithSocialData(profile));
     * }</pre>
     *
     * @param <R> the type of the result returned by the function.
     * @param action the function to apply to the result; must not be {@code null}.
     * @return a new {@code ContinuableFuture<R>} with the transformed result.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #map(Throwables.Function)
     */
    public <R> ContinuableFuture<R> thenCallAsync(final Throwables.Function<? super T, ? extends R, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(action, cs.action);

        return execute(() -> action.apply(awaitValue(this)));
    }

    /**
     * Executes the provided bi-function asynchronously after this future completes, transforming
     * the result based on both the value and any exception. The bi-function is executed using
     * the configured executor of this future and receives both the result (if successful) and
     * any exception that occurred.
     *
     * <p>This method returns a new {@code ContinuableFuture} that completes
     * with the transformed result. The bi-function always executes, regardless of whether this
     * future completed normally or exceptionally. This is useful for recovery scenarios where
     * you want to provide alternative values or transform exceptions into valid results.
     * If the worker waiting for this future is interrupted before this future completes
     * (for example by {@code cancel(true)} on the returned future), the bi-function is not executed:
     * the interruption ends the returned stage rather than being reported as a failure of this future.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Data> future = ContinuableFuture.call(() -> fetchFromPrimary())
     *     .thenCallAsync((data, exception) -> {
     *         if (exception != null) {
     *             logger.warn("Primary failed, using fallback", exception);
     *             return fetchFromSecondary();
     *         }
     *         return enhanceData(data);
     *     });
     * }</pre>
     *
     * @param <R> the type of the result returned by the bi-function.
     * @param action the bi-function to apply to the result and exception.
     * @return a new {@code ContinuableFuture<R>} with the transformed result.
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #getAsResult()
     */
    public <R> ContinuableFuture<R> thenCallAsync(final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this); // never getAsResult here - see awaitResult
            return action.apply(result.orElseIfFailure(null), result.getException());
        });
    }

    /**
     * Executes the provided action asynchronously after both this future and the other future complete.
     * The action is executed asynchronously using the configured executor.
     *
     * <p>The returned future completes when the action completes.
     * If either future fails, the action is not executed and the returned future completes exceptionally with
     * this future's exception when this future failed (the other future's exception, if any, is attached to it
     * as a suppressed exception); otherwise it completes exceptionally with the other future's exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<File> download1 = ContinuableFuture.call(() -> downloadFile1());
     * ContinuableFuture<File> download2 = ContinuableFuture.call(() -> downloadFile2());
     *
     * download1.runAsyncAfterBoth(download2, () -> {
     *     System.out.println("Both downloads complete!");
     *     mergeFiles();
     * });
     * }</pre>
     *
     * @param other the other future to wait for; must not be {@code null}.
     * @param action the action to execute after both futures complete.
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public ContinuableFuture<Void> runAsyncAfterBoth(final ContinuableFuture<?> other, final Throwables.Runnable<? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this);
            final Result<?, Exception> result2 = awaitResult(other);

            throwIfEitherFailed(result, result2);
            action.run();
            return null;
        }, other);
    }

    /**
     * Executes the provided bi-consumer asynchronously after both this future and the other future complete,
     * passing both results to the consumer. The consumer is executed asynchronously using
     * the configured executor.
     *
     * <p>The returned future completes when the consumer completes.
     * If either future fails, the consumer is not executed and the returned future completes exceptionally with
     * this future's exception when this future failed (the other future's exception, if any, is attached to it
     * as a suppressed exception); otherwise it completes exceptionally with the other future's exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> userFuture = ContinuableFuture.call(() -> fetchUser(id));
     * ContinuableFuture<Profile> profileFuture = ContinuableFuture.call(() -> fetchProfile(id));
     *
     * userFuture.runAsyncAfterBoth(profileFuture, (user, profile) -> {
     *     mergeUserAndProfile(user, profile);
     *     updateCache(user, profile);
     * });
     * }</pre>
     *
     * @param <U> the type of the other future's result.
     * @param other the other future to wait for; must not be {@code null}.
     * @param action the bi-consumer to execute with both results.
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <U> ContinuableFuture<Void> runAsyncAfterBoth(final ContinuableFuture<U> other,
            final Throwables.BiConsumer<? super T, ? super U, ? extends Exception> action) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this);
            final Result<U, Exception> result2 = awaitResult(other);

            throwIfEitherFailed(result, result2);
            action.accept(result.orElseIfFailure(null), result2.orElseIfFailure(null));
            return null;
        }, other);
    }

    /**
     * Executes the provided consumer asynchronously after both this future and the other future complete,
     * passing a {@link Tuple4} containing both results and any exceptions to the consumer.
     * The consumer is executed regardless of whether the futures completed successfully.
     *
     * <p>The tuple contains: (result1, exception1, result2, exception2) where:
     * <ul>
     *   <li>result1/exception1 are from this future</li>
     *   <li>result2/exception2 are from the other future</li>
     *   <li>If a future succeeds, its exception is {@code null} and its result is the (possibly null) computed value</li>
     *   <li>If a future fails, its result is {@code null} and exception is non-null</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Data> primary = ContinuableFuture.call(() -> fetchPrimary());
     * ContinuableFuture<Data> backup = ContinuableFuture.call(() -> fetchBackup());
     *
     * primary.runAsyncAfterBoth(backup, tuple -> {
     *     if (tuple._2 == null) {
     *         processData(tuple._1);
     *     } else if (tuple._4 == null) {
     *         processData(tuple._3);
     *     } else {
     *         handleBothFailed(tuple._2, tuple._4);
     *     }
     * });
     * }</pre>
     *
     * @param <U> the type of the other future's result.
     * @param other the other future to wait for; must not be {@code null}.
     * @param action the consumer to execute with the tuple of results and exceptions.
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #getAsResult()
     */
    public <U> ContinuableFuture<Void> runAsyncAfterBoth(final ContinuableFuture<U> other,
            final Throwables.Consumer<? super Tuple4<T, Exception, U, Exception>, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this); // never getAsResult here - see awaitResult
            final Result<U, Exception> result2 = awaitResult(other);

            action.accept(Tuple.of(result.orElseIfFailure(null), result.getException(), result2.orElseIfFailure(null), result2.getException()));
            return null;
        }, other);
    }

    /**
     * Executes the provided quad-consumer asynchronously after both this future and the other future complete,
     * passing all four values (both results and exceptions) as separate parameters. The consumer
     * is executed regardless of whether the futures completed successfully.
     *
     * <p>This method provides the same functionality as
     * {@link #runAsyncAfterBoth(ContinuableFuture, Throwables.Consumer)} but with individual parameters
     * instead of a tuple.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Order> orderFuture = ContinuableFuture.call(() -> createOrder());
     * ContinuableFuture<Payment> paymentFuture = ContinuableFuture.call(() -> processPayment());
     *
     * orderFuture.runAsyncAfterBoth(paymentFuture, (order, orderEx, payment, paymentEx) -> {
     *     if (orderEx != null || paymentEx != null) {
     *         rollbackTransaction(order, payment, orderEx, paymentEx);
     *     } else {
     *         confirmTransaction(order, payment);
     *     }
     * });
     * }</pre>
     *
     * @param <U> the type of the other future's result.
     * @param other the other future to wait for; must not be {@code null}.
     * @param action the quad-consumer to execute with both results and exceptions.
     * @return a new {@code ContinuableFuture<Void>} representing the completion of the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #getAsResult()
     */
    public <U> ContinuableFuture<Void> runAsyncAfterBoth(final ContinuableFuture<U> other,
            final Throwables.QuadConsumer<? super T, ? super Exception, ? super U, ? super Exception, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this); // never getAsResult here - see awaitResult
            final Result<U, Exception> result2 = awaitResult(other);

            action.accept(result.orElseIfFailure(null), result.getException(), result2.orElseIfFailure(null), result2.getException());
            return null;
        }, other);
    }

    /**
     * Executes the provided callable asynchronously after both this future and the other future complete successfully.
     * The callable is executed asynchronously using the configured executor of this future.
     *
     * <p>This method returns a new {@code ContinuableFuture} that completes
     * with the result of the callable. If either input future fails, the callable is not executed and the
     * returned future completes exceptionally with this future's exception when this future failed (the other
     * future's exception, if any, is attached to it as a suppressed exception); otherwise it completes
     * exceptionally with the other future's exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Config> configFuture = ContinuableFuture.call(() -> loadConfig());
     * ContinuableFuture<Database> dbFuture = ContinuableFuture.call(() -> connectDB());
     *
     * ContinuableFuture<Service> serviceFuture = configFuture.callAsyncAfterBoth(dbFuture, () -> {
     *     // Both config and database are ready
     *     return initializeService();
     * });
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable.
     * @param other the other future to wait for completion; must not be {@code null}.
     * @param action the callable to execute after both futures complete; must not be {@code null}.
     * @return a new {@code ContinuableFuture<R>} that completes with the result of the callable.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <R> ContinuableFuture<R> callAsyncAfterBoth(final ContinuableFuture<?> other, final Callable<? extends R> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this);
            final Result<?, Exception> result2 = awaitResult(other);

            throwIfEitherFailed(result, result2);
            return action.call();
        }, other);
    }

    /**
     * Executes the provided bi-function asynchronously after both this future and the other future complete successfully,
     * passing both results to the bi-function. The bi-function is executed asynchronously using the
     * configured executor of this future.
     *
     * <p>This method returns a new {@code ContinuableFuture} that completes
     * with the result of the bi-function. This enables combining the results of two independent
     * asynchronous computations. If either future fails, the bi-function is not executed and the returned
     * future completes exceptionally with this future's exception when this future failed (the other future's
     * exception, if any, is attached to it as a suppressed exception); otherwise it completes exceptionally
     * with the other future's exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> userFuture = ContinuableFuture.call(() -> fetchUser(userId));
     * ContinuableFuture<Settings> settingsFuture = ContinuableFuture.call(() -> fetchSettings());
     *
     * ContinuableFuture<Profile> profileFuture = userFuture.callAsyncAfterBoth(settingsFuture,
     *     (user, settings) -> createProfile(user, settings));
     * }</pre>
     *
     * @param <U> the result type of the other ContinuableFuture.
     * @param <R> the result type of the bi-function and the returned ContinuableFuture.
     * @param other the other ContinuableFuture that must complete before executing the action; must not be {@code null}.
     * @param action the bi-function to execute with both results; must not be {@code null}.
     * @return a new {@code ContinuableFuture<R>} that completes with the result of the bi-function.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <U, R> ContinuableFuture<R> callAsyncAfterBoth(final ContinuableFuture<U> other,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this);
            final Result<U, Exception> result2 = awaitResult(other);

            throwIfEitherFailed(result, result2);
            return action.apply(result.orElseIfFailure(null), result2.orElseIfFailure(null));
        }, other);
    }

    /**
     * Executes the provided function asynchronously after both this future and the other future complete,
     * regardless of whether they complete successfully or exceptionally. The function is executed
     * asynchronously using the configured executor of this future and receives a {@link Tuple4}
     * containing both results and their exceptions (if any).
     *
     * <p>This method returns a new {@code ContinuableFuture} that completes
     * with the result of the function. The function always executes, regardless of whether the
     * futures completed normally or exceptionally. This is useful when you need to handle the
     * results of both futures regardless of their success/failure status. The tuple contains:
     * (result1, exception1, result2, exception2) where results are {@code null} if the corresponding
     * future failed, and exceptions are {@code null} if the corresponding future succeeded.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Data> primaryFuture = ContinuableFuture.call(() -> fetchPrimaryData());
     * ContinuableFuture<Data> backupFuture = ContinuableFuture.call(() -> fetchBackupData());
     *
     * ContinuableFuture<Data> result = primaryFuture.callAsyncAfterBoth(backupFuture, tuple -> {
     *     Data primary = tuple._1;
     *     Exception primaryError = tuple._2;
     *     Data backup = tuple._3;
     *     Exception backupError = tuple._4;
     *
     *     if (primary != null) return primary;
     *     if (backup != null) return backup;
     *     throw new DataUnavailableException("Both sources failed");
     * });
     * }</pre>
     *
     * @param <U> the result type of the other ContinuableFuture.
     * @param <R> the result type of the function and the returned ContinuableFuture.
     * @param other the other ContinuableFuture to wait for; must not be {@code null}.
     * @param action the function that processes the tuple of results and exceptions; must not be {@code null}.
     * @return a new {@code ContinuableFuture<R>} that completes with the result of the function.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #getAsResult()
     */
    public <U, R> ContinuableFuture<R> callAsyncAfterBoth(final ContinuableFuture<U> other,
            final Throwables.Function<? super Tuple4<T, Exception, U, Exception>, ? extends R, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this); // never getAsResult here - see awaitResult
            final Result<U, Exception> result2 = awaitResult(other);

            return action.apply(Tuple.of(result.orElseIfFailure(null), result.getException(), result2.orElseIfFailure(null), result2.getException()));
        }, other);
    }

    /**
     * Executes the provided QuadFunction asynchronously after both this ContinuableFuture and the other ContinuableFuture complete,
     * regardless of whether they complete successfully or exceptionally.
     * The function receives four parameters: the results and exceptions from both futures.
     *
     * <p>This method provides maximum flexibility for handling the completion of two futures,
     * allowing custom logic based on any combination of success and failure states.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Response> apiFuture = ContinuableFuture.call(() -> callAPI());
     * ContinuableFuture<Cache> cacheFuture = ContinuableFuture.call(() -> loadCache());
     *
     * ContinuableFuture<PageModel> combined = apiFuture.callAsyncAfterBoth(cacheFuture,
     *     (apiResponse, apiError, cacheData, cacheError) -> {
     *         if (apiError == null && apiResponse.isValid()) {
     *             return PageModel.fromApi(apiResponse);
     *         } else if (cacheError == null) {
     *             return PageModel.fromCache(cacheData);
     *         } else {
     *             throw new ServiceUnavailableException("Both API and cache failed");
     *         }
     *     });
     * }</pre>
     *
     * @param <U> the result type of the other ContinuableFuture.
     * @param <R> the result type of the QuadFunction and the returned ContinuableFuture.
     * @param other the other ContinuableFuture to wait for; must not be {@code null}.
     * @param action the QuadFunction that processes both results and exceptions; must not be {@code null}.
     * @return a new ContinuableFuture that completes with the result of the QuadFunction.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <U, R> ContinuableFuture<R> callAsyncAfterBoth(final ContinuableFuture<U> other,
            final Throwables.QuadFunction<? super T, ? super Exception, ? super U, ? super Exception, ? extends R, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = awaitResult(this); // never getAsResult here - see awaitResult
            final Result<U, Exception> result2 = awaitResult(other);

            return action.apply(result.orElseIfFailure(null), result.getException(), result2.orElseIfFailure(null), result2.getException());
        }, other);
    }

    /**
     * Executes the provided Runnable action asynchronously after either this ContinuableFuture or the other ContinuableFuture completes
     * (successfully or exceptionally).
     *
     * <p>This method is useful for triggering an action as soon as any of the futures completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Data> primarySource = ContinuableFuture.call(() -> fetchFromPrimary());
     * ContinuableFuture<Data> secondarySource = ContinuableFuture.call(() -> fetchFromSecondary());
     *
     * primarySource.runAsyncAfterEither(secondarySource, () -> {
     *     System.out.println("At least one data source has responded");
     *     notifyDataAvailable();
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to race against; must not be {@code null}.
     * @param action the Runnable to execute after either future completes; must not be {@code null}.
     * @return a new ContinuableFuture&lt;Void&gt; that completes after executing the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public ContinuableFuture<Void> runAsyncAfterEither(final ContinuableFuture<?> other, final Throwables.Runnable<? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            firstCompletedOf(this, other);

            action.run();
            return null;
        }, other);
    }

    /**
     * Executes the provided Consumer action asynchronously after either this ContinuableFuture or the other ContinuableFuture completes
     * (successfully or exceptionally). The Consumer receives the result of whichever future completes first.
     * If the future that completes first fails, the consumer receives {@code null}.
     *
     * <p>This method is useful for triggering an action as soon as any of the futures completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Weather> localWeather = ContinuableFuture.call(() -> getLocalWeather());
     * ContinuableFuture<Weather> remoteWeather = ContinuableFuture.call(() -> getRemoteWeather());
     *
     * localWeather.runAsyncAfterEither(remoteWeather, weather -> {
     *     if (weather != null) {
     *         displayWeather(weather);
     *         logSource(weather.getSource());
     *     } else {
     *         showWeatherUnavailable(); // the first completion failed
     *     }
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to race against; must not be {@code null}.
     * @param action the Consumer to execute with the first available result; must not be {@code null}.
     * @return a new ContinuableFuture&lt;Void&gt; that completes after executing the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public ContinuableFuture<Void> runAsyncAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.Consumer<? super T, ? extends Exception> action) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> ret = firstCompletedOf(this, other);

            action.accept(ret.orElseIfFailure(null));
            return null;
        }, other);
    }

    /**
     * Executes the provided BiConsumer action asynchronously after either this ContinuableFuture or the other ContinuableFuture completes.
     * The BiConsumer receives {@code (result, null)} if the first completion succeeds, or
     * {@code (null, exception)} if it fails or is cancelled. The other future may still be pending.
     *
     * <p>Use {@link #runAsyncAfterFirstSuccess(ContinuableFuture, Throwables.BiConsumer)} to wait for
     * a successful result instead. This method does not cancel the other input future.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Config> localConfig = ContinuableFuture.call(() -> loadLocalConfig());
     * ContinuableFuture<Config> remoteConfig = ContinuableFuture.call(() -> loadRemoteConfig());
     *
     * localConfig.runAsyncAfterEither(remoteConfig, (config, error) -> {
     *     if (error != null) {
     *         logger.warn("Config loading failed: " + error.getMessage());
     *         useDefaultConfig();
     *     } else {
     *         applyConfig(config);
     *     }
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to race against; must not be {@code null}.
     * @param action the BiConsumer to execute with the result and exception; must not be {@code null}.
     * @return a new ContinuableFuture&lt;Void&gt; that completes after executing the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #getAsResult()
     */
    public ContinuableFuture<Void> runAsyncAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.BiConsumer<? super T, ? super Exception, ? extends Exception> action) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = firstCompletedOf(this, other);

            action.accept(result.orElseIfFailure(null), result.getException());
            return null;
        }, other);
    }

    /**
     * Executes the provided Callable action asynchronously after either this ContinuableFuture or the other ContinuableFuture
     * completes (successfully or exceptionally).
     *
     * <p>This method is useful when you need to compute a new value as soon as either future completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Connection> primary = ContinuableFuture.call(() -> connectToPrimary());
     * ContinuableFuture<Connection> backup = ContinuableFuture.call(() -> connectToBackup());
     *
     * ContinuableFuture<Session> session = primary.callAsyncAfterEither(backup, () -> {
     *     // Runs after either connection attempt completes, even if it fails
     *     return createNewSession();
     * });
     * }</pre>
     *
     * @param <R> the result type of the callable and the returned ContinuableFuture.
     * @param other the other ContinuableFuture to race against; must not be {@code null}.
     * @param action the Callable to execute after either future completes; must not be {@code null}.
     * @return a new ContinuableFuture that completes with the result of the callable.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <R> ContinuableFuture<R> callAsyncAfterEither(final ContinuableFuture<?> other, final Callable<? extends R> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            firstCompletedOf(this, other);

            return action.call();
        }, other);
    }

    /**
     * Executes the provided function asynchronously after either this ContinuableFuture or the other ContinuableFuture
     * completes (successfully or exceptionally). The function transforms the result of whichever future completes first.
     * If the future that completes first fails, the function receives {@code null}.
     *
     * <p>This method is useful when you need to compute a new value as soon as either future completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Price> vendorA = ContinuableFuture.call(() -> getPriceFromVendorA());
     * ContinuableFuture<Price> vendorB = ContinuableFuture.call(() -> getPriceFromVendorB());
     *
     * ContinuableFuture<Order> order = vendorA.callAsyncAfterEither(vendorB, price -> {
     *     // A failed first completion is represented by null in this overload.
     *     return price == null ? createFallbackOrder() : createOrder(price);
     * });
     * }</pre>
     *
     * @param <R> the result type of the function and the returned ContinuableFuture.
     * @param other the other ContinuableFuture to race against; must not be {@code null}.
     * @param action the function to transform the first available result; must not be {@code null}.
     * @return a new ContinuableFuture that completes with the transformed result.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <R> ContinuableFuture<R> callAsyncAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.Function<? super T, ? extends R, ? extends Exception> action) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> ret = firstCompletedOf(this, other);

            return action.apply(ret.orElseIfFailure(null));
        }, other);
    }

    /**
     * Executes the provided BiFunction asynchronously after either this ContinuableFuture or the other ContinuableFuture
     * completes (successfully or exceptionally). The BiFunction receives both the result (if successful)
     * and the exception (if failed) from whichever future completes first, and returns a transformed result.
     *
     * <p>This method is useful when you need to compute a new value as soon as either future completes,
     * regardless of which one finishes first or whether it succeeds or fails.
     *
     * <p>If the first completion failed,
     * the BiFunction receives {@code (null, exception)} even though the other future might still succeed. When you need
     * the first successful result instead, use {@link #callAsyncAfterFirstSuccess(ContinuableFuture, Throwables.BiFunction)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Data> fastSource = ContinuableFuture.call(() -> fetchFromFastSource());
     * ContinuableFuture<Data> slowSource = ContinuableFuture.call(() -> fetchFromSlowSource());
     *
     * ContinuableFuture<ProcessedData> result = fastSource.callAsyncAfterEither(slowSource,
     *     (data, error) -> {
     *         if (error != null) {
     *             return ProcessedData.empty();
     *         }
     *         return processData(data);
     *     });
     * }</pre>
     *
     * @param <R> the result type of the BiFunction and the returned ContinuableFuture.
     * @param other the other ContinuableFuture to race against; must not be {@code null}.
     * @param action the BiFunction to transform the result and exception; must not be {@code null}.
     * @return a new ContinuableFuture that completes with the transformed result.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #getAsResult()
     * @see #callAsyncAfterFirstSuccess(ContinuableFuture, Throwables.BiFunction)
     */
    public <R> ContinuableFuture<R> callAsyncAfterEither(final ContinuableFuture<? extends T> other,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> ret = firstCompletedOf(this, other);

            return action.apply(ret.orElseIfFailure(null), ret.getException());
        }, other);
    }

    /**
     * Executes the provided Runnable action asynchronously after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. If both futures fail, the action is not executed and the returned
     * future completes exceptionally with the exception of the first future to complete, with the other
     * failure attached to it as a suppressed exception.
     *
     * <p>This method waits for at least one successful completion before executing the action,
     * making it useful when you need a successful result from at least one source.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Void> saveToDatabase = ContinuableFuture.run(() -> saveToDb());
     * ContinuableFuture<Void> saveToCache = ContinuableFuture.run(() -> saveToCache());
     *
     * saveToDatabase.runAsyncAfterFirstSuccess(saveToCache, () -> {
     *     // Execute only after at least one save operation succeeds
     *     notifySaveComplete();
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to wait for; must not be {@code null}.
     * @param action the Runnable to execute after the first successful completion; must not be {@code null}.
     * @return a new ContinuableFuture&lt;Void&gt; that completes after executing the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public ContinuableFuture<Void> runAsyncAfterFirstSuccess(final ContinuableFuture<?> other, final Throwables.Runnable<? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            firstSuccessOf(this, other);
            action.run();
            return null;
        }, other);
    }

    /**
     * Executes the provided Consumer action asynchronously after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. The Consumer receives the result of the first future that completes successfully.
     *
     * <p>If both futures fail, the action is not executed and the returned future completes exceptionally
     * with the exception of the first future to complete, with the other failure attached to it as a
     * suppressed exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<User> dbUser = ContinuableFuture.call(() -> loadUserFromDatabase(id));
     * ContinuableFuture<User> cacheUser = ContinuableFuture.call(() -> loadUserFromCache(id));
     *
     * dbUser.runAsyncAfterFirstSuccess(cacheUser, user -> {
     *     // Process the first successfully loaded user
     *     updateLastAccessed(user);
     *     notifyUserLoaded(user);
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to wait for; must not be {@code null}.
     * @param action the Consumer to execute with the first successful result; must not be {@code null}.
     * @return a new ContinuableFuture&lt;Void&gt; that completes after executing the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public ContinuableFuture<Void> runAsyncAfterFirstSuccess(final ContinuableFuture<? extends T> other,
            final Throwables.Consumer<? super T, ? extends Exception> action) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            action.accept(firstSuccessOf(this, other));
            return null;
        }, other);
    }

    /**
     * Executes the provided BiConsumer action asynchronously after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. The BiConsumer receives the result and the exception.
     *
     * <p><b>BETA API:</b> this overload is marked {@code @Beta}; the way the two failures are reported when both
     * inputs fail (only the first, with the second attached as suppressed) may still be refined.</p>
     *
     * <p>If either future completes successfully, the BiConsumer receives {@code (firstSuccessfulResult, null)}.
     * Only if both futures fail does it receive {@code (null, exception)}, where the exception is the failure of
     * the first future to complete, with the other failure attached as suppressed to that failure - to the
     * {@code Error} itself when the failure is an {@code ExecutionException} carrying one - once, however often the
     * same failed pair is combined. Unlike the other {@code runAsyncAfterFirstSuccess} overloads, this
     * one never completes the returned future exceptionally because of an upstream failure.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Config> primaryConfig = ContinuableFuture.call(() -> loadPrimaryConfig());
     * ContinuableFuture<Config> fallbackConfig = ContinuableFuture.call(() -> loadFallbackConfig());
     *
     * primaryConfig.runAsyncAfterFirstSuccess(fallbackConfig, (config, error) -> {
     *     if (error == null) {
     *         applyConfig(config);
     *     } else {
     *         logger.error("All config sources failed", error);
     *         useHardcodedDefaults();
     *     }
     * });
     * }</pre>
     *
     * @param other the other ContinuableFuture to wait for; must not be {@code null}.
     * @param action the BiConsumer to execute with the result and exception; must not be {@code null}.
     * @return a new ContinuableFuture&lt;Void&gt; that completes after executing the action.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    @Beta
    public ContinuableFuture<Void> runAsyncAfterFirstSuccess(final ContinuableFuture<? extends T> other,
            final Throwables.BiConsumer<? super T, ? super Exception, ? extends Exception> action) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = firstSuccessOrFirstFailureOf(this, other);
            action.accept(result.orElseIfFailure(null), result.getException());
            return null;
        }, other);
    }

    /**
     * Executes the provided Callable action asynchronously after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. If both futures fail, the callable is not executed and the returned
     * future completes exceptionally with the exception of the first future to complete, with the other
     * failure attached to it as a suppressed exception.
     *
     * <p>This method ensures that the callable is only executed if at least one of the futures succeeds,
     * making it useful for dependent operations that require a successful prerequisite.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Auth> oauth = ContinuableFuture.call(() -> authenticateOAuth());
     * ContinuableFuture<Auth> apiKey = ContinuableFuture.call(() -> authenticateApiKey());
     *
     * ContinuableFuture<Session> session = oauth.callAsyncAfterFirstSuccess(apiKey, () -> {
     *     // Create session only after successful authentication
     *     return createUserSession();
     * });
     * }</pre>
     *
     * @param <R> the result type of the callable and the returned ContinuableFuture.
     * @param other the other ContinuableFuture to wait for; must not be {@code null}.
     * @param action the Callable to execute after the first successful completion; must not be {@code null}.
     * @return a new ContinuableFuture that completes with the result of the callable.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <R> ContinuableFuture<R> callAsyncAfterFirstSuccess(final ContinuableFuture<?> other, final Callable<? extends R> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            firstSuccessOf(this, other);
            return action.call();
        }, other);
    }

    /**
     * Executes the provided function asynchronously after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. The function transforms the result of the first future that completes successfully.
     *
     * <p>If both futures fail, the function is not executed and the returned future completes exceptionally
     * with the exception of the first future to complete, with the other failure attached to it as a
     * suppressed exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<RawData> primarySource = ContinuableFuture.call(() -> fetchFromPrimary());
     * ContinuableFuture<RawData> backupSource = ContinuableFuture.call(() -> fetchFromBackup());
     *
     * ContinuableFuture<ProcessedData> processed = primarySource.callAsyncAfterFirstSuccess(backupSource,
     *     rawData -> {
     *         // Transform the first successfully fetched data
     *         return processAndValidate(rawData);
     *     });
     * }</pre>
     *
     * @param <R> the result type of the function and the returned ContinuableFuture.
     * @param other the other ContinuableFuture to wait for; must not be {@code null}.
     * @param action the function to transform the first successful result; must not be {@code null}.
     * @return a new ContinuableFuture that completes with the transformed result.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    public <R> ContinuableFuture<R> callAsyncAfterFirstSuccess(final ContinuableFuture<? extends T> other,
            final Throwables.Function<? super T, ? extends R, ? extends Exception> action) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> action.apply(firstSuccessOf(this, other)), other);
    }

    /**
     * Executes the provided BiFunction asynchronously after the first successful completion between this ContinuableFuture
     * and the other ContinuableFuture. The BiFunction receives the result and the exception.
     *
     * <p>If either future completes successfully, the BiFunction receives {@code (firstSuccessfulResult, null)}.
     * Only if both futures fail does it receive {@code (null, exception)}, where the exception is the failure of
     * the first future to complete. This allows the BiFunction to handle both cases and produce an appropriate
     * result; unlike the other {@code callAsyncAfterFirstSuccess} overloads, this one never completes the
     * returned future exceptionally because of an upstream failure.
     *
     * <p><b>BETA API:</b> this overload is the exact mirror of
     * {@link #runAsyncAfterFirstSuccess(ContinuableFuture, Throwables.BiConsumer)} and, like it, is marked
     * {@code @Beta}: the way the two failures are reported when both inputs fail (only the first, with the second
     * attached as suppressed to that failure - to the {@code Error} itself when the failure is an
     * {@code ExecutionException} carrying one - once, however often the same failed pair is combined) may still
     * be refined.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Price> vendorPrice = ContinuableFuture.call(() -> getVendorPrice());
     * ContinuableFuture<Price> marketPrice = ContinuableFuture.call(() -> getMarketPrice());
     *
     * ContinuableFuture<Quote> quote = vendorPrice.callAsyncAfterFirstSuccess(marketPrice,
     *     (price, error) -> {
     *         if (error == null) {
     *             return Quote.withPrice(price);
     *         } else {
     *             // Both sources failed, return quote with error status
     *             return Quote.unavailable(error.getMessage());
     *         }
     *     });
     * }</pre>
     *
     * @param <R> the result type of the BiFunction and the returned ContinuableFuture.
     * @param other the other ContinuableFuture to wait for; must not be {@code null}.
     * @param action the BiFunction to transform based on result and exception; must not be {@code null}.
     * @return a new ContinuableFuture that completes with the transformed result.
     * @throws IllegalArgumentException if any of {@code other}, {@code action} is {@code null}.
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     * @see #getAsResult()
     */
    @Beta
    public <R> ContinuableFuture<R> callAsyncAfterFirstSuccess(final ContinuableFuture<? extends T> other,
            final Throwables.BiFunction<? super T, ? super Exception, ? extends R, ? extends Exception> action)
            throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(other, cs.other);
        N.checkArgNotNull(action, cs.action);

        return execute(() -> {
            final Result<T, Exception> result = firstSuccessOrFirstFailureOf(this, other);
            return action.apply(result.orElseIfFailure(null), result.getException());
        }, other);
    }

    /**
     * Waits for whichever of the two futures completes first (successfully or exceptionally) and returns
     * that outcome, then releases the background relay still blocked on the other one.
     *
     * <p>{@link Futures#iterate(java.util.Collection, java.util.function.Function)} starts one relay task
     * per input that is not a completed plain task; an "either" combinator consumes only the first outcome, so
     * without the explicit release the losing input would keep a relay thread parked until it completed on its
     * own.</p>
     *
     * @param <V> the common result type of the two futures
     * @param first the first future to race
     * @param second the second future to race
     * @return the outcome of whichever future completed first
     * @throws InterruptedException if this thread is interrupted while waiting for the first outcome
     */
    private static <V> Result<V, Exception> firstCompletedOf(final ContinuableFuture<? extends V> first, final ContinuableFuture<? extends V> second)
            throws InterruptedException {
        final ObjIterator<Result<V, Exception>> iter = Futures.iterate(Arrays.asList(first, second), Fn.identity());

        try {
            return nextOutcome(iter);
        } finally {
            Futures.cancelPendingRelays(iter);
        }
    }

    /**
     * Reads the next outcome from a {@link Futures#iterate(java.util.Collection, java.util.function.Function)}
     * iterator, treating this thread's own interruption as terminal for the stage rather than as an outcome of one
     * of the input futures.
     *
     * @param <V> the common result type of the futures being iterated
     * @param iterator the iterator to read the next outcome from
     * @return the next outcome
     * @throws InterruptedException if the outcome just read is the iterator's stand-in for this thread's own interruption
     */
    private static <V> Result<V, Exception> nextOutcome(final ObjIterator<Result<V, Exception>> iterator) throws InterruptedException {
        final Result<V, Exception> result = iterator.next();

        // Futures.iterate reports the CONSUMING thread's own InterruptedException as a failure Result (re-setting
        // the interrupt flag) instead of throwing it, so without this an interrupted combining worker would read
        // "an input failed" and still run the caller's action for two futures that had not completed at all. The
        // flag test is what separates that stand-in from a genuine input failure that happens to be an
        // InterruptedException: a failure relayed from an input arrives with this thread's flag clear - had it been
        // set, the queue's take() would have thrown instead of returning a value. This gives the *AfterEither and
        // *AfterFirstSuccess families the contract awaitResult already gives *AfterBoth.
        if (result.getException() instanceof final InterruptedException ie && Thread.currentThread().isInterrupted()) {
            throw ie;
        }

        return result;
    }

    /**
     * Waits for {@code continuableFuture} and reports its outcome as a {@link Result}: the value on success, or the
     * unwrapped failure (see {@link Futures#convertException(Throwable)}) - a cancelled input arrives as its
     * {@link CancellationException}, an input whose task threw an {@link Error} as the {@code ExecutionException}
     * carrier of that Error.
     *
     * <p>awaitResult, not getAsResult, is what every worker uses to read its inputs: getAsResult would turn the
     * WORKER's own interruption (for example {@code cancel(true)} on the returned stage) into a fabricated
     * "this input failed" outcome and restore the interrupt flag, which then makes the very next {@code get()} fail
     * instantly - so cancelling a combined stage used to invoke the callback with two fabricated
     * InterruptedExceptions for two futures that had not completed at all. Rethrowing the interruption instead ends
     * the stage, exactly as an interrupted plain task ends.</p>
     *
     * @param <V> the result type of the future
     * @param continuableFuture the input to wait for
     * @return the input's outcome
     * @throws InterruptedException if waiting for {@code continuableFuture} is interrupted; other Exceptions are returned in the Result
     */
    private static <V> Result<V, Exception> awaitResult(final ContinuableFuture<? extends V> continuableFuture) throws InterruptedException {
        try {
            return Result.of(continuableFuture.get(), null);
        } catch (final InterruptedException e) {
            throw e;
        } catch (final Exception e) {
            return Result.of(null, Futures.convertException(e));
        }
    }

    /**
     * Waits for {@code continuableFuture} and returns its value, or rethrows its failure with a single wrapper
     * removed so that the worker's own {@code ExecutionException} carries the input's original failure (the cause
     * of the input's {@code ExecutionException}, or the {@link Error} itself) rather than a nested
     * {@code ExecutionException}. This is what keeps the strict {@code then*} family on the same failure shape as
     * every other family and as {@code CompletableFuture}.
     *
     * @param <V> the result type of the future
     * @param continuableFuture the input to wait for
     * @return the input's value
     * @throws InterruptedException if waiting is interrupted
     * @throws Exception the input's unwrapped failure (a {@link CancellationException} for a cancelled input)
     */
    private static <V> V awaitValue(final ContinuableFuture<? extends V> continuableFuture) throws Exception {
        final Result<V, Exception> result = awaitResult(continuableFuture);

        if (result.isFailure()) {
            throw rethrowable(result.getException(), null);
        }

        return result.orElseIfFailure(null);
    }

    /**
     * Waits for whichever of the two inputs succeeds first. When the first outcome is a failure, the second
     * outcome decides: its success is returned, or its failure is attached (once) as suppressed to the first one,
     * which is then reported.
     *
     * @param <V> the common result type of the two futures
     * @param first the first input
     * @param second the second input
     * @return the first success, or the first failure with the second failure suppressed on it
     * @throws InterruptedException if this worker is interrupted while waiting
     */
    private static <V> Result<V, Exception> firstSuccessOrFirstFailureOf(final ContinuableFuture<? extends V> first,
            final ContinuableFuture<? extends V> second) throws InterruptedException {
        final ObjIterator<Result<V, Exception>> iter = Futures.iterate(Arrays.asList(first, second), Fn.identity());

        try {
            final Result<V, Exception> firstResult = nextOutcome(iter);

            if (firstResult.isSuccess()) {
                return firstResult;
            }

            // The iterator was built over exactly two inputs, so a second outcome always follows (this worker's own
            // interruption arrives as a stand-in that nextOutcome rethrows).
            final Result<V, Exception> secondResult = nextOutcome(iter);

            if (secondResult.isSuccess()) {
                return secondResult;
            }

            // Both inputs failed and only the first failure is reported. Keep the second one visible as a
            // suppressed exception - once, however often the same failed pair is combined.
            suppressOnce(firstResult.getException(), secondResult.getException());

            return firstResult;
        } finally {
            // Stops after the first outcome when that outcome is a success: release the relay still blocked on the
            // losing input instead of leaving a thread parked on it.
            Futures.cancelPendingRelays(iter);
        }
    }

    /**
     * Strict form of {@link #firstSuccessOrFirstFailureOf}: returns the first successful value, or rethrows the
     * first failure (unwrapped, with the second failure suppressed on it) when both inputs fail.
     *
     * @param <V> the common result type of the two futures
     * @param first the first input
     * @param second the second input
     * @return the first successful value
     * @throws InterruptedException if this worker is interrupted while waiting
     * @throws Exception the first input failure when both inputs fail
     */
    private static <V> V firstSuccessOf(final ContinuableFuture<? extends V> first, final ContinuableFuture<? extends V> second) throws Exception {
        final Result<V, Exception> result = firstSuccessOrFirstFailureOf(first, second);

        if (result.isFailure()) {
            throw rethrowable(result.getException(), null);
        }

        return result.orElseIfFailure(null);
    }

    /**
     * Prepares an input failure for rethrow from a worker so that the worker's {@code ExecutionException} wraps the
     * original failure exactly once: an {@code ExecutionException}/{@code CompletionException} that merely carries a
     * non-{@code Exception} throwable is unwrapped, and an {@link Error} is thrown right here (a
     * {@link Callable} cannot declare it). {@code secondary} is attached as suppressed to whatever is reported.
     *
     * @param primary the failure to report; must not be {@code null}
     * @param secondary the other input's failure to attach, may be {@code null}
     * @return the exception the caller should throw
     * @throws Error if {@code primary} carries an {@code Error}
     */
    private static Exception rethrowable(final Exception primary, final Exception secondary) {
        final Throwable reported = reportedFailure(primary);

        suppressOnce(reported, secondary);

        if (reported instanceof Error error) {
            throw error;
        }

        return (Exception) reported;
    }

    /**
     * The throwable that stands for an input failure once its per-{@code get()} carrier is removed: the
     * {@link Error} inside an {@code ExecutionException}/{@code CompletionException} carrier, otherwise the
     * exception itself (a bare {@code Throwable} subclass that is neither stays inside its carrier).
     *
     * @param failure the input failure; must not be {@code null}
     * @return the long-lived throwable to report or to attach suppressed exceptions to
     */
    private static Throwable reportedFailure(final Exception failure) {
        final Throwable unwrapped = Futures.unwrapErrorCarrier(failure);

        // unwrapErrorCarrier only ever exposes a non-Exception cause, so the choice is "the Error inside" or the
        // exception itself (a bare Throwable subclass that is neither stays inside its carrier).
        return unwrapped instanceof Error ? unwrapped : failure;
    }

    /**
     * Records {@code secondary} as suppressed on {@code primary}, unless it is already there.
     *
     * <p>The two exceptions belong to the <i>input</i> futures and outlive any one combination, so combining the
     * same failed pair twice must not append the same suppressed exception again and again - an unbounded,
     * caller-visible mutation of an object this class does not own. Hence: check-and-add is atomic under one
     * class-wide lock ({@code SUPPRESSION_LOCK}; the individual {@code Throwable} monitors are only ever taken inside
     * it, by {@code getSuppressed()}/{@code addSuppressed()}, so concurrent combinations of one pair in any order
     * cannot race); an {@code Error} failure is attached as the long-lived Error itself rather than the fresh
     * {@code ExecutionException} carrier that every {@code get()} creates (identity de-duplication never matched
     * those); a cancelled input, whose {@link CancellationException} is likewise fresh per {@code get()}, is attached
     * at most once by type; and a secondary from which the primary is already reachable through suppressed links is
     * skipped, so no combination - in any order, over any number of inputs (A -&gt; B -&gt; C -&gt; A included) - can
     * build a cycle that recursive suppressed-walkers choke on.</p>
     *
     * @param primaryFailure the exception that will be thrown or handed to the action; must not be {@code null}
     * @param secondary the other input's failure, may be {@code null}
     */
    private static void suppressOnce(final Throwable primaryFailure, final Exception secondary) {
        if (secondary == null) {
            return;
        }

        // Both sides shed their per-get() carriers: the entry is attached to the long-lived Error, never to a
        // carrier that the next get() replaces (which is what made "once" unbounded before).
        final Throwable primary = primaryFailure instanceof Exception ex ? reportedFailure(ex) : primaryFailure;
        final Throwable extra = reportedFailure(secondary);

        if (extra == primary) {
            return;
        }

        // One class-wide lock around the reachability check and the add: the check must be atomic with the add
        // (two threads combining (A,B) and (B,A) at once used to pass a lock-free pair check and both add), and a
        // static lock avoids ordering two Throwable monitors. getSuppressed()/addSuppressed() take the individual
        // Throwable monitor inside it; nothing in this class or in Futures nests the two the other way round.
        synchronized (SUPPRESSION_LOCK) {
            // The cheap direct-children check first: a repeat combination of the same pair never pays for the walk.
            for (final Throwable already : primary.getSuppressed()) {
                if (already == extra || (extra instanceof CancellationException && already instanceof CancellationException)) {
                    return;
                }
            }

            if (reachesThroughSuppressed(extra, primary)) {
                return; // attaching would close a cycle (A -> B -> C -> A included), not only the A <-> B pair
            }

            primary.addSuppressed(extra);
        }
    }

    /** Guards {@link #suppressOnce}: the cycle check and the add must be atomic across all combinations. */
    private static final Object SUPPRESSION_LOCK = new Object();

    /**
     * Tells whether {@code target} is reachable from {@code start} by following suppressed exceptions (identity
     * comparison, each node visited once).
     *
     * @param start the throwable to start from
     * @param target the throwable to look for
     * @return {@code true} if {@code target} is {@code start} or is (transitively) suppressed under it
     */
    private static boolean reachesThroughSuppressed(final Throwable start, final Throwable target) {
        if (start == target) {
            return true;
        }

        final Throwable[] direct = start.getSuppressed();

        if (direct.length == 0) {
            return false; // the common case - a leaf secondary - allocates nothing
        }

        final IdentityHashMap<Throwable, Boolean> visited = new IdentityHashMap<>();
        final ArrayDeque<Throwable> pending = new ArrayDeque<>();
        visited.put(start, Boolean.TRUE);

        for (final Throwable suppressed : direct) {
            pending.push(suppressed);
        }

        while (!pending.isEmpty()) {
            final Throwable current = pending.pop();

            if (current == target) {
                return true;
            }

            if (visited.put(current, Boolean.TRUE) == null) {
                for (final Throwable suppressed : current.getSuppressed()) {
                    pending.push(suppressed);
                }
            }
        }

        return false;
    }

    /**
     * @throws Exception if either result contains a failure; the first result takes precedence and a distinct secondary failure is suppressed
     */
    private static void throwIfEitherFailed(final Result<?, Exception> result, final Result<?, Exception> result2) throws Exception {
        final Exception exception = result.getException();
        final Exception exception2 = result2.getException();

        if (exception != null) {
            throw rethrowable(exception, exception2);
        } else if (exception2 != null) {
            throw rethrowable(exception2, null);
        }
    }

    /**
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    private <R> ContinuableFuture<R> execute(final Callable<? extends R> command) throws RejectedExecutionException {
        return execute(command, null);
    }

    /**
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    private <R> ContinuableFuture<R> execute(final Callable<? extends R> command, final ContinuableFuture<?> other) throws RejectedExecutionException {
        return execute(new FutureTask<>(command), other);
    }

    /**
     * @throws RejectedExecutionException if the executor cannot accept the submitted task
     */
    private <R> ContinuableFuture<R> execute(final FutureTask<? extends R> futureTask, final ContinuableFuture<?> other) throws RejectedExecutionException {
        asyncExecutor.execute(futureTask);

        final List<ContinuableFuture<?>> upFutureList = other == null ? List.<ContinuableFuture<?>> of(this) : List.<ContinuableFuture<?>> of(this, other);
        return new ContinuableFuture<>(futureTask, upFutureList, asyncExecutor);
    }

    /**
     * Configures this ContinuableFuture to delay the execution of the next chained action.
     * The delay is applied before the next action in the chain is executed.
     *
     * <p>This method is useful for retry backoff, rate limiting, or introducing deliberate
     * pauses in asynchronous workflows. It does not impose a timeout on the upstream operation.
     * The delay window (the delay converted to nanoseconds) is shared by every accessor of the returned future and
     * begins when that future first <i>observes</i> upstream completion - inside {@code get()}, {@code isDone()} or
     * {@code cancel()}, in this method itself if this future is already done, or when a further {@code thenDelay}/
     * {@code thenUse} stage is built on it while it is done. A delayed future that is created and then left
     * untouched does not count its window down in the background; its first {@code get()} waits the
     * full delay. Concurrent callers wait independently on that same window, so one caller cannot prevent another
     * caller from observing its own timeout. For {@link #get(long, TimeUnit)}, the timeout is a single budget
     * covering both the upstream wait and the remaining delay.
     *
     * <p><b>Cancellation.</b> The returned future is a stage of its own and follows the {@link Future} contract: after
     * {@code cancel()} returns, {@code isDone()} is {@code true}. Cancelling it before this future completes cancels
     * this future's task (the two share it) and bypasses the delay; cancelling it once this future has completed
     * but while the window is still open ends the window: {@code cancel()} returns {@code true}, the stage reports
     * {@code isCancelled()}, every getter parked in the window wakes up with {@link CancellationException}, and the
     * upstream result stays available from this future. Once the window has elapsed the stage is complete and
     * {@code cancel()} returns {@code false}. {@code cancelAll()} on the returned future first cancels the upstream
     * chain and then ends an open window in the same way; its result is still {@code false} when an upstream stage
     * had already completed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture.call(() -> sendRequest())
     *     .thenDelay(2, TimeUnit.SECONDS)  // Wait 2 seconds after request completes
     *     .thenCallAsync(() -> checkResponse())
     *     .thenDelay(1, TimeUnit.SECONDS)  // Wait 1 second before final action
     *     .thenRunAsync(() -> processResults());
     * }</pre>
     *
     * @param delay the delay duration before the next action is executed; values &lt;= 0 mean no delay.
     * @param unit the time unit of the delay parameter; must not be {@code null}.
     * @return a new ContinuableFuture configured with the specified delay if delay &gt; 0, or this future if delay &lt;= 0.
     * @throws IllegalArgumentException if {@code unit} is {@code null}.
     */
    public ContinuableFuture<T> thenDelay(final long delay, final TimeUnit unit) throws IllegalArgumentException {
        N.checkArgNotNull(unit, cs.unit);

        if (delay <= 0) {
            return this;
        }

        return with(asyncExecutor, delay, unit);
    }

    /**
     * Configures this ContinuableFuture to execute the next chained action using the specified executor.
     * This allows changing the execution context for subsequent operations in the chain.
     *
     * <p>This method is useful when different parts of an asynchronous workflow need to run
     * on different thread pools (e.g., I/O operations on an I/O pool, CPU-intensive work on
     * a computation pool).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService ioExecutor = Executors.newCachedThreadPool();
     * ExecutorService cpuExecutor = Executors.newFixedThreadPool(4);
     *
     * try {
     *     ContinuableFuture.call(() -> readFromFile())        // Runs on default executor
     *         .thenUse(cpuExecutor)
     *         .thenCallAsync(() -> processData())             // CPU-intensive processing
     *         .thenUse(ioExecutor)
     *         .thenRunAsync(result -> writeToFile(result))    // I/O operation
     *         .get();
     * } finally {
     *     ioExecutor.shutdown();
     *     cpuExecutor.shutdown();
     * }
     * }</pre>
     *
     * @param executor the executor to use for subsequent actions in the chain; must not be {@code null}.
     * @return a new ContinuableFuture configured with the specified executor.
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     */
    public ContinuableFuture<T> thenUse(final Executor executor) throws IllegalArgumentException {
        N.checkArgNotNull(executor, cs.executor);

        return with(executor, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates the stage behind {@link #thenDelay(long, TimeUnit)} and {@link #thenUse(Executor)}: a
     * {@link DelayedFuture} over this future's task, with no upstream list of its own (its {@code cancelAll} and
     * {@code isAllCancelled} delegate to this future, exactly like the {@link #map(Throwables.Function)} wrapper)
     * and with {@code executor} as the executor of every later stage.
     *
     * @param executor the executor to use for subsequent operations; already validated by the caller.
     * @param delay the delay before executing subsequent operations; {@code 0} for a pure executor switch.
     * @param unit the time unit for the delay; already validated by the caller.
     * @return a new ContinuableFuture with the specified configuration.
     */
    private ContinuableFuture<T> with(final Executor executor, final long delay, final TimeUnit unit) {
        // executor and unit were validated by the public caller (thenDelay / thenUse).
        final DelayedFuture<T> delayed = new DelayedFuture<>(future, delay, unit);

        return new ContinuableFuture<>(delayed, null, executor) {
            @Override
            public boolean cancelAll(final boolean mayInterruptIfRunning) {
                // Delegate to the enclosing future (like map()): this wrapper's upFutures is null, so a super-call
                // would silently sever cancellation from the upstream chain. Never route through this wrapper's
                // own cancel(): that would cancel the shared upstream task a second time and report the second
                // (failing) attempt as "could not cancel".
                final boolean upstreamCancelled = ContinuableFuture.this.cancelAll(mayInterruptIfRunning);

                if (!upstreamCancelled) {
                    // The upstream traversal cannot reach this stage's own delay window; end it when the upstream
                    // had already completed. The result stays false: a completed upstream stage was not cancelled.
                    delayed.cancelWindowIfUpstreamCompleted();
                }

                return upstreamCancelled;
            }

            @Override
            public boolean isAllCancelled() {
                return ContinuableFuture.this.isAllCancelled();
            }
        };
    }

    /**
     * The delegate of {@link #thenDelay(long, TimeUnit)} / {@link #thenUse(Executor)}: the upstream outcome, held
     * back for a delay window that opens when this future first observes upstream completion.
     *
     * <p>The window has one terminal transition, {@code PENDING -> ELAPSED | CANCELLED}, claimed by a CAS so that a
     * getter returning the value and a {@code cancel()} returning {@code true} are mutually exclusive
     * ({@code FutureTask} makes the same guarantee with its state CAS). Getters wait on a latch that {@code cancel()}
     * releases, and the window's start is claimed by a CAS as well: this object's monitor is never taken, because a
     * caller may hold it while another caller's timed {@code get()} must still observe its own timeout.</p>
     *
     * @param <T> the result type of the upstream future
     */
    private static final class DelayedFuture<T> implements Future<T> {
        /** Window state: the delay has not elapsed and the stage has not been cancelled. */
        private static final int WINDOW_PENDING = 0;
        /** Window state: the delay has elapsed; the stage is complete with the upstream outcome. */
        private static final int WINDOW_ELAPSED = 1;
        /** Window state: the stage was cancelled while the window was open. */
        private static final int WINDOW_CANCELLED = 2;
        /** {@link #delayStartTimeInNanos} value while the window has not been opened yet. */
        private static final long NOT_STARTED = Long.MIN_VALUE;

        private final Future<? extends T> future;
        private final long delayInNanos;
        private final AtomicLong delayStartTimeInNanos = new AtomicLong(NOT_STARTED);
        private final AtomicInteger window = new AtomicInteger(WINDOW_PENDING);
        private final CountDownLatch windowCancelled = new CountDownLatch(1);

        DelayedFuture(final Future<? extends T> future, final long delay, final TimeUnit unit) {
            this.future = future;
            delayInNanos = unit.toNanos(delay);

            if (future.isDone()) {
                startDelayIfNeeded();
            }
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            if (future.cancel(mayInterruptIfRunning)) {
                return true; // The upstream accepted: this stage is cancelled through it and the window is bypassed.
            }

            if (!future.isDone() || future.isCancelled()) {
                // Either the upstream is still running and refuses cancellation (this stage's getters are blocked
                // inside it, so there is nothing of our own to end), or it was already cancelled.
                return false;
            }

            // The upstream completed, so the only pending work is this stage's own delay window. Future contract:
            // once cancel() returns, isDone() must be true - so the window is ended rather than left to elapse.
            startDelayIfNeeded();

            return cancelWindow();
        }

        /**
         * Ends an open window on behalf of {@link ContinuableFuture#cancelAll(boolean)}, which has already dealt with
         * the upstream itself (so this must not call {@code future.cancel} again).
         */
        void cancelWindowIfUpstreamCompleted() {
            // Not guarded by future.isCancelled(): for a cancelled upstream the CAS below is harmless (the stage
            // already reports cancelled through the upstream), and skipping that query keeps cancelAll from
            // touching the upstream more often than the traversal itself does.
            if (future.isDone()) {
                startDelayIfNeeded();
                cancelWindow();
            }
        }

        private boolean cancelWindow() {
            settleWindowIfElapsed();

            if (window.compareAndSet(WINDOW_PENDING, WINDOW_CANCELLED)) {
                windowCancelled.countDown();
                return true;
            }

            return false;
        }

        /**
         * Claims the {@code ELAPSED} state once the window has run out.
         *
         * @return {@code true} if the window is over (elapsed), {@code false} while it is pending or once cancelled
         */
        private boolean settleWindowIfElapsed() {
            if (window.get() == WINDOW_PENDING && System.nanoTime() - delayStartTimeInNanos.get() >= delayInNanos) {
                window.compareAndSet(WINDOW_PENDING, WINDOW_ELAPSED);
            }

            return window.get() == WINDOW_ELAPSED;
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled() || window.get() == WINDOW_CANCELLED;
        }

        @Override
        public boolean isDone() {
            if (future.isCancelled() || window.get() == WINDOW_CANCELLED) {
                return true;
            }

            // The delay stage is not complete until the post-completion delay has elapsed.
            // Returning future.isDone() alone would make get()/getNow() block while isDone()
            // was already true, which violates the Future contract.
            if (!future.isDone()) {
                return false;
            }

            startDelayIfNeeded();
            settleWindowIfElapsed();

            // One read: a cancel() that claims the window between the checks above and here still counts as done.
            return window.get() != WINDOW_PENDING;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            T result = null;
            ExecutionException executionException = null;
            RuntimeException runtimeException = null;
            Error error = null;

            try {
                result = future.get();
            } catch (final CancellationException e) {
                // A getter already waiting upstream must also bypass the delay on cancellation.
                throw e;
            } catch (final ExecutionException e) {
                executionException = e;
            } catch (final RuntimeException e) {
                runtimeException = e;
            } catch (final Error e) {
                error = e;
            }

            startDelayIfNeeded();
            awaitWindow(Long.MAX_VALUE);
            throwIfWindowCancelled();

            if (executionException != null) {
                throw executionException;
            } else if (runtimeException != null) {
                throw runtimeException;
            } else if (error != null) {
                throw error;
            }

            return result;
        }

        @Override
        public T get(final long timeout, final TimeUnit unit) throws InterruptedException, TimeoutException, ExecutionException {
            N.requireNonNull(unit, cs.unit);

            final long timeoutNanos = unit.toNanos(timeout);
            final long startNanos = System.nanoTime();
            T result = null;
            ExecutionException executionException = null;
            RuntimeException runtimeException = null;
            Error error = null;

            try {
                // Future.get permits non-positive timeouts. Normalize them to zero so broken
                // implementations do not reject a negative value before checking completion.
                result = future.get(Math.max(0L, timeoutNanos), TimeUnit.NANOSECONDS);
            } catch (final CancellationException e) {
                // A getter already waiting upstream must also bypass the delay on cancellation.
                throw e;
            } catch (final ExecutionException e) {
                executionException = e;
            } catch (final RuntimeException e) {
                runtimeException = e;
            } catch (final Error e) {
                error = e;
            }

            startDelayIfNeeded();

            final long elapsedNanos = System.nanoTime() - startNanos;
            final long remainingNanos = timeoutNanos <= 0 ? 0 : timeoutNanos - elapsedNanos;
            awaitWindow(Math.max(0L, remainingNanos));
            throwIfWindowCancelled();

            if (window.get() != WINDOW_ELAPSED) {
                // The timeout budget was exhausted by the upstream wait and/or only part of
                // the post-completion delay. The value must not become visible early.
                throw new TimeoutException("Timeout after delay");
            }

            if (executionException != null) {
                throw executionException;
            } else if (runtimeException != null) {
                throw runtimeException;
            } else if (error != null) {
                throw error;
            }

            return result;
        }

        private void startDelayIfNeeded() {
            if (delayStartTimeInNanos.get() == NOT_STARTED) {
                delayStartTimeInNanos.compareAndSet(NOT_STARTED, System.nanoTime());
            }
        }

        private void throwIfWindowCancelled() {
            if (window.get() == WINDOW_CANCELLED) {
                throw new CancellationException("Cancelled while waiting for the delay to elapse");
            }
        }

        /**
         * Waits until the window is settled (elapsed or cancelled) or {@code maxWaitNanos} have passed, whichever
         * comes first.
         *
         * @param maxWaitNanos the caller's remaining budget
         * @throws InterruptedException if the calling thread is interrupted while waiting for the remaining delay
         */
        private void awaitWindow(final long maxWaitNanos) throws InterruptedException {
            final long waitStartTimeInNanos = System.nanoTime();

            while (window.get() == WINDOW_PENDING) {
                final long remainingDelayNanos = delayInNanos - (System.nanoTime() - delayStartTimeInNanos.get());

                if (remainingDelayNanos <= 0) {
                    window.compareAndSet(WINDOW_PENDING, WINDOW_ELAPSED);
                    return;
                }

                final long remainingWaitNanos = maxWaitNanos - (System.nanoTime() - waitStartTimeInNanos);

                if (remainingWaitNanos <= 0) {
                    return;
                }

                // Each caller waits independently and never on this object's monitor: holding it while waiting
                // would let an untimed get() prevent another caller's timed get() from observing its own timeout.
                // The latch, not a sleep, is what lets cancel() wake every parked getter at once.
                windowCancelled.await(Math.min(remainingDelayNanos, remainingWaitNanos), TimeUnit.NANOSECONDS);
            }
        }
    }

    // https://stackoverflow.com/questions/23301598/transform-java-future-into-a-completablefuture

    /**
     * Converts this ContinuableFuture into a standard {@link CompletableFuture} for interoperability
     * with APIs that require CompletableFuture instances. The conversion is performed asynchronously
     * using this future's configured executor.
     *
     * <p>This method creates a new CompletableFuture that completes with the same result as this
     * ContinuableFuture. The result retrieval is performed asynchronously, meaning this method
     * submits retrieval to the configured executor. Exceptions are reported through the returned
     * CompletableFuture; an ExecutionException from retrieval is unwrapped to its cause.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Executor-backed retrieval:</b> the submitted task blocks in {@code get()} until this future
     *       completes; a direct executor runs that task on the calling thread</li>
     *   <li><b>Executor Reuse:</b> Uses this future's asyncExecutor for the conversion</li>
     *   <li><b>Exception Wrapping:</b> All exceptions are wrapped in CompletionException</li>
     *   <li><b>Independent Lifecycle:</b> Returned CompletableFuture has independent cancellation</li>
     * </ul>
     *
     * <p><b>Common Use Cases:</b>
     * <ul>
     *   <li>Integrating ContinuableFuture with CompletableFuture-based frameworks</li>
     *   <li>Migrating from ContinuableFuture to CompletableFuture APIs</li>
     *   <li>Combining ContinuableFuture with third-party libraries expecting CompletableFuture</li>
     *   <li>Leveraging CompletableFuture-specific methods like thenCompose, allOf, anyOf</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * // Basic conversion for framework integration
     * ContinuableFuture<User> userFuture = ContinuableFuture.call(() -> loadUser());
     * CompletableFuture<User> completable = userFuture.toCompletableFuture();
     *
     * // Chain with CompletableFuture operations
     * ContinuableFuture<String> dataFuture = ContinuableFuture.call(() -> fetchData());
     * dataFuture.toCompletableFuture()
     *     .thenApply(data -> processData(data))
     *     .thenAccept(result -> saveResult(result));
     *
     * // Combine with other CompletableFutures
     * CompletableFuture<String> cf1 = continuableFuture1.toCompletableFuture();
     * CompletableFuture<Integer> cf2 = continuableFuture2.toCompletableFuture();
     * CompletableFuture<Object> combined = CompletableFuture.anyOf(cf1, cf2);
     * }</pre>
     *
     * <p><b>Exception Handling:</b>
     * <ul>
     *   <li><b>InterruptedException:</b> Wrapped in CompletionException</li>
     *   <li><b>ExecutionException:</b> Its original cause is propagated without retaining the intermediate wrapper</li>
     *   <li><b>CancellationException:</b> <i>Not</i> propagated as cancellation. Cancelling this
     *       {@code ContinuableFuture} makes the returned {@code CompletableFuture} complete exceptionally with the
     *       {@code CancellationException} as the <i>cause</i>: {@code isCancelled()} stays {@code false} while
     *       {@code isCompletedExceptionally()} becomes {@code true}, {@code get()} throws
     *       {@code ExecutionException(CancellationException)} and {@code join()} throws
     *       {@code CompletionException(CancellationException)}</li>
     *   <li><b>RuntimeException:</b> Wrapped in CompletionException</li>
     * </ul>
     *
     * <p><b>Important Considerations:</b>
     * <ul>
     *   <li>Cancelling the returned CompletableFuture does not cancel this ContinuableFuture, nor does it release the
     *       retrieval worker, which stays blocked in {@code get()} until this future completes</li>
     *   <li>Cancelling this ContinuableFuture will cause the CompletableFuture to complete exceptionally</li>
     *   <li>The retrieval task calls {@code get()} and may wait for upstream work</li>
     *   <li>Uses this future's asyncExecutor, which may impact thread pool usage</li>
     * </ul>
     *
     * <p><b>Performance Implications:</b>
     * <ul>
     *   <li>Creates additional task submission overhead</li>
     *   <li>Blocks a thread from asyncExecutor until this future completes</li>
     *   <li>For already-completed futures, consider using {@link CompletableFuture#completedFuture(Object)}</li>
     * </ul>
     *
     * <p><b>&#9888;&#65039; Starvation warning:</b> this overload uses <i>this future's own executor</i>, which is
     * usually the executor its upstream task is running or queued on. That pool blocks one worker per conversion,
     * and {@link AsyncExecutor}'s pool has an unbounded queue - so it never grows past its core size. Converting
     * pending futures can cause starvation if retrieval tasks occupy every worker while the work needed
     * to complete those futures is queued behind them. Pass an unrelated executor to {@link #toCompletableFuture(Executor)} when
     * the futures may still be pending.</p>
     *
     * @return a new {@code CompletableFuture} that completes with the same result as this {@code ContinuableFuture},
     *         executed asynchronously using this future's {@code asyncExecutor}.
     * @throws RejectedExecutionException if the executor rejects the asynchronous result-retrieval task
     * @see CompletableFuture#supplyAsync(java.util.function.Supplier, Executor)
     * @see #toCompletableFuture(Executor)
     * @see CompletionException
     */
    @Beta
    public CompletableFuture<T> toCompletableFuture() throws RejectedExecutionException {
        return CompletableFuture.supplyAsync(this::getForCompletableFuture, asyncExecutor);
    }

    /**
     * Converts this ContinuableFuture into a standard {@link CompletableFuture} using the specified executor
     * for the conversion operation. This method provides fine-grained control over which thread pool performs
     * the asynchronous result retrieval, making it ideal for scenarios requiring specific execution contexts.
     *
     * <p>This method creates a new CompletableFuture that completes with the same result as this
     * ContinuableFuture. The result retrieval is performed asynchronously using the provided executor,
     * which may run the retrieval task on the calling thread. Exceptions are reported through the
     * returned CompletableFuture; an ExecutionException from retrieval is unwrapped to its cause.
     *
     * <p><b>Key Characteristics:</b>
     * <ul>
     *   <li><b>Executor-backed retrieval:</b> the submitted task blocks in {@code get()} until this future
     *       completes; a direct executor runs that task on the calling thread</li>
     *   <li><b>Custom Executor:</b> Uses the provided executor instead of this future's asyncExecutor</li>
     *   <li><b>Exception Wrapping:</b> All exceptions are wrapped in CompletionException</li>
     *   <li><b>Independent Lifecycle:</b> Returned CompletableFuture has independent cancellation</li>
     * </ul>
     *
     * <p><b>Common Use Cases:</b>
     * <ul>
     *   <li>Converting to CompletableFuture with a specific thread pool (e.g., I/O vs CPU executor)</li>
     *   <li>Isolating CompletableFuture operations from ContinuableFuture's execution context</li>
     *   <li>Integrating with frameworks that provide their own executors</li>
     *   <li>Managing thread pool resources independently for different workflow stages</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b>
     * <pre>{@code
     * // Use a custom executor for the conversion
     * ExecutorService ioExecutor = Executors.newCachedThreadPool();
     * try {
     *     ContinuableFuture<Data> dataFuture = ContinuableFuture.call(() -> loadData());
     *     Data data = dataFuture.toCompletableFuture(ioExecutor).get();
     * } finally {
     *     ioExecutor.shutdown();
     * }
     *
     * // Integrate with framework-specific executors
     * Executor springTaskExecutor = applicationContext.getBean("taskExecutor", Executor.class);
     * ContinuableFuture<Result> result = ContinuableFuture.call(() -> computeResult());
     * CompletableFuture<Result> springManaged = result.toCompletableFuture(springTaskExecutor);
     *
     * // Use different executors for different conversion stages
     * ExecutorService cpuExecutor = Executors.newFixedThreadPool(4);
     * try {
     *     ContinuableFuture<ProcessedData> processed = ContinuableFuture.call(() -> process());
     *     processed.toCompletableFuture(cpuExecutor)
     *         .thenApplyAsync(data -> furtherProcessing(data), cpuExecutor)
     *         .get();
     * } finally {
     *     cpuExecutor.shutdown();
     * }
     * }</pre>
     *
     * <p><b>Exception Handling:</b>
     * <ul>
     *   <li><b>InterruptedException:</b> Wrapped in CompletionException</li>
     *   <li><b>ExecutionException:</b> Its original cause is propagated without retaining the intermediate wrapper</li>
     *   <li><b>CancellationException:</b> <i>Not</i> propagated as cancellation. Cancelling this
     *       {@code ContinuableFuture} makes the returned {@code CompletableFuture} complete exceptionally with the
     *       {@code CancellationException} as the <i>cause</i>: {@code isCancelled()} stays {@code false} while
     *       {@code isCompletedExceptionally()} becomes {@code true}, {@code get()} throws
     *       {@code ExecutionException(CancellationException)} and {@code join()} throws
     *       {@code CompletionException(CancellationException)}</li>
     *   <li><b>RuntimeException:</b> Wrapped in CompletionException</li>
     * </ul>
     *
     * <p><b>Important Considerations:</b>
     * <ul>
     *   <li>Cancelling the returned CompletableFuture does not cancel this ContinuableFuture, nor does it release the
     *       retrieval worker, which stays blocked in {@code get()} until this future completes</li>
     *   <li>Cancelling this ContinuableFuture will cause the CompletableFuture to complete exceptionally</li>
     *   <li>The retrieval task calls {@code get()} and may wait for upstream work</li>
     *   <li>The provided executor must be able to accept new tasks</li>
     *   <li>Executor shutdown should be managed externally; this method does not manage lifecycle</li>
     * </ul>
     *
     * <p><b>Performance Implications:</b>
     * <ul>
     *   <li>Creates additional task submission overhead to the specified executor</li>
     *   <li>Blocks a thread from the provided executor until this future completes</li>
     *   <li>Executor choice affects overall performance (e.g., ForkJoinPool vs ThreadPoolExecutor)</li>
     *   <li>For already-completed futures, still incurs executor submission cost</li>
     * </ul>
     *
     * <p><b>Comparison with {@link #toCompletableFuture()}:</b>
     * <ul>
     *   <li>This method allows custom executor selection vs using this future's asyncExecutor</li>
     *   <li>Useful when asyncExecutor is not suitable for CompletableFuture operations</li>
     *   <li>Provides better control over thread pool isolation and resource management</li>
     * </ul>
     *
     * @param executor the executor to use for asynchronous result retrieval; must not be {@code null}.
     * @return a new {@code CompletableFuture} that completes with the same result as this {@code ContinuableFuture},
     *         executed asynchronously using the provided executor.
     * @throws IllegalArgumentException if {@code executor} is {@code null}.
     * @throws RejectedExecutionException if the executor rejects the asynchronous result-retrieval task
     * @see CompletableFuture#supplyAsync(java.util.function.Supplier, Executor)
     * @see #toCompletableFuture()
     * @see CompletionException
     */
    @Beta
    public CompletableFuture<T> toCompletableFuture(final Executor executor) throws IllegalArgumentException, RejectedExecutionException {
        N.checkArgNotNull(executor, cs.executor);

        return CompletableFuture.supplyAsync(this::getForCompletableFuture, executor);
    }

    /**
     * @throws CancellationException if this future has been cancelled
     * @throws CompletionException if result retrieval is interrupted or fails with ExecutionException; interruption also restores the thread interrupt
     *         flag
     */
    private T getForCompletableFuture() throws CancellationException, CompletionException {
        try {
            return get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException(e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            throw new CompletionException(cause == null ? e : cause);
        }
    }
}
