/*
 * Copyright (C) 2019 HaiYang Li
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
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A utility class that provides enhanced try-with-resources functionality and exception handling mechanisms.
 *
 * <p>This class offers two main capabilities:</p>
 * <ul>
 *   <li>Static methods for executing code that may throw checked exceptions, converting them to runtime exceptions</li>
 *   <li>Instance methods for managing AutoCloseable resources with automatic cleanup and optional final actions</li>
 * </ul>
 *
 * <p><b>Static usage examples:</b></p>
 * <pre>{@code
 * // Execute code that throws checked exceptions
 * Try.run(() -> {
 *     Thread.sleep(1000);   // throws InterruptedException
 * });
 *
 * // Call code with return value and default on exception
 * String result = Try.call(() -> Files.readString(Path.of("config.txt")), "default content");
 *
 * // Handle exceptions with custom logic
 * Try.run(() -> Files.readString(Path.of("missing.txt")),
 *     ex -> System.err.println("Operation failed: " + ex.getMessage()));
 * }</pre>
 *
 * <p><b>Resource management examples:</b></p>
 * <pre>{@code
 * // Basic try-with-resources
 * Try.with(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)))
 *    .run(stream -> System.out.println(new String(stream.readAllBytes(), StandardCharsets.UTF_8)));
 *
 * // With final action
 * AtomicBoolean finished = new AtomicBoolean();
 * Try.with(new StringReader("ready"), () -> finished.set(true))
 *    .call(Reader::read);
 *
 * // With lazy resource initialization
 * Throwables.Supplier<java.io.StringWriter, Exception> writerSupplier = java.io.StringWriter::new;
 * Try.with(writerSupplier)
 *    .run(writer -> writer.write("Hello, World!"));
 * }</pre>
 *
 * <p>Each instance operation closes the resource it uses. A {@code Try} created with an
 * already-open resource should therefore normally execute only one operation; invoking another
 * operation reuses the same, already-closed object. A supplier-backed instance may be reused when
 * its supplier returns a fresh, non-null resource for every invocation; a supplier that returns
 * {@code null} is a configuration error. The resulting {@link IllegalArgumentException} is an
 * acquisition failure like any other: it reaches the caller only from the overloads that offer no
 * error handling - {@link #run(Throwables.Consumer)} and {@link #call(Throwables.Function)} - and is
 * routed to the {@code actionOnError}, {@code supplier}, {@code defaultValue} or fallback function of
 * every other overload.</p>
 *
 * <p>A configured final action runs after every attempted instance operation, including when resource
 * acquisition fails. If a resource was acquired, try-with-resources closes it before the final action.
 * A failure of the final action itself is <b>not</b> routed to a fallback: when the operation has no other
 * failure left to propagate - which includes every overload whose {@code actionOnError},
 * {@code supplier} or {@code defaultValue} has already handled one - the final action's exception is
 * thrown to the caller and the result is discarded; otherwise it is added as a suppressed exception on
 * the primary failure.</p>
 *
 * <p><b>&#9888;&#65039; A failing {@code close()} fails the whole operation.</b> Every instance
 * {@code run}/{@code call} wraps acquisition, the body, <i>and</i> {@code close()} in one
 * try-with-resources, exactly as the language construct does: the operation has not completed
 * successfully until the resource is closed, because closing is where a buffer is flushed or a
 * transaction is committed. So if the body returns normally but {@code close()} throws, the body's
 * result is <b>discarded</b> and the close failure is routed to whichever error handling the overload
 * offers - the {@code actionOnError} consumer/function, the {@code supplier}, or the
 * {@code defaultValue}. Use a fallback value only where a close failure genuinely means the work
 * should be abandoned.</p>
 *
 * <p><b>Overload note:</b> Because {@code with} accepts either a resource or a resource supplier, an
 * inline supplier lambda or constructor reference is ambiguous between the two and must be targeted
 * as a {@link Throwables.Supplier} (by assignment or cast). The same applies to a fallback that is a
 * no-argument method or constructor reference such as {@code Properties::new}, which fits both the
 * {@link Supplier} and the {@link Function} overload. Explicitly-shaped lambdas ({@code () -> x},
 * {@code ex -> x}) are <i>not</i> ambiguous and need no cast.</p>
 *
 * <p><b>{@code Error} is never caught.</b> Every {@code run}/{@code call} here catches {@link Exception}, so an
 * {@link Error} - {@link OutOfMemoryError}, {@link StackOverflowError}, a failed class initialization - propagates:
 * it is not wrapped, not passed to {@code actionOnError}, and never replaced by a fallback value. This
 * is the one behavioural difference from the similarly named {@link Throwables#run(Throwables.Runnable)} /
 * {@link Throwables#call(Throwables.Callable)} family, which is bounded on {@link Throwable} and therefore does
 * convert an {@code Error} into a runtime exception. Choose this class when an {@code Error} must reach the
 * caller untouched, and {@code Throwables} when every failure should be uniformly unchecked. A configured final
 * action still runs in either case.</p>
 *
 * <p>If an {@link InterruptedException} is converted, handled, or replaced with a fallback value,
 * the current thread's interrupted status is restored before control is passed to user recovery code.
 * This also applies when the interruption is a cause or a suppressed close failure. Only
 * {@code InterruptedException} itself counts: types that merely report an I/O timeout are deliberately
 * ignored, in particular {@link java.net.SocketTimeoutException}, which extends
 * {@link java.io.InterruptedIOException} but does not mean the thread was interrupted. An
 * {@code InterruptedException} found <i>under</i> an {@link ExecutionException} or a
 * {@link CompletionException} is ignored for the same reason: those wrappers report what a task threw on
 * another thread, so a task's interruption surfacing through {@code Try.call(future::get, fallback)} leaves
 * this thread's interrupted status alone. The {@code InterruptedException} that {@code get()} itself throws
 * is <i>not</i> wrapped and does mean this thread was interrupted while waiting, so that one still restores
 * the status. Exceptions suppressed on such a wrapper are also still examined, because they are attached
 * where the wrapper is built - on this thread.</p>
 *
 * @param <T> the type of the resource that extends {@link AutoCloseable}
 * @see Throwables
 * @see ExceptionUtil
 */
@SuppressWarnings("try") // InterruptedException from close() is detected and restores the thread's status below.
public final class Try<T extends AutoCloseable> {
    private final T targetResource;
    private final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier;
    private final Runnable finalAction;

    /**
     * Package-private constructor. Use the static {@code with} factory methods to create instances.
     *
     * @param targetResource the pre-created resource, or {@code null} if a supplier is used
     * @param targetResourceSupplier the supplier used to lazily create the resource, or {@code null} if the resource is pre-created
     * @param finalAction the action to execute in the outer {@code finally} block after each attempted
     *                    operation, or {@code null} if none
     */
    Try(final T targetResource, final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier, final Runnable finalAction) {
        this.targetResource = targetResource;
        this.targetResourceSupplier = targetResourceSupplier;
        this.finalAction = finalAction;
    }

    /**
     * Performs acquireResource using the configured resource or operation.
     * @throws IllegalArgumentException if no resource or supplier is configured, or if the supplier returns {@code null}
     * @throws Exception if acquiring a resource through the configured supplier throws
     */
    private T acquireResource() throws IllegalArgumentException, Exception {
        if (targetResource != null) {
            return targetResource;
        }

        if (targetResourceSupplier == null) {
            // Not reachable through the public factories: every with(...) overload stores exactly one of the two.
            throw new IllegalArgumentException("No target resource and no target resource supplier was configured");
        }

        final T resource = targetResourceSupplier.get();

        if (resource == null) {
            // Name the supplier rather than 'targetResource': the caller passed a supplier, and a message
            // naming a parameter they never supplied sends them looking in the wrong place.
            throw new IllegalArgumentException("'targetResourceSupplier' returned null; a supplied resource must not be null");
        }

        return resource;
    }

    /**
     * Performs executeWithFinalAction using the configured resource or operation.
     * @throws RuntimeException if {@code operation} fails, or if the final action fails without an earlier failure; later failures are suppressed
     */
    private <R> R executeWithFinalAction(final Throwables.Callable<R, RuntimeException> operation) throws RuntimeException {
        Throwable primaryFailure = null;

        try {
            return operation.call();
        } catch (final RuntimeException | Error e) {
            // An Error bypasses the `catch (final Exception e)` block that every operation wraps its body in,
            // so its graph is examined here. Every other failure was already examined there, on the *original*
            // exception: examining it again after ExceptionUtil.toRuntimeException(..) has unwrapped it would
            // look at a graph that no longer holds the ExecutionException wrapper, and would mistake a task's
            // interruption for this thread's.
            if (e instanceof Error) {
                restoreInterruptedStatusIfNeeded(e);
            }

            primaryFailure = e;
            throw e;
        } finally {
            if (finalAction != null) {
                try {
                    finalAction.run();
                } catch (final RuntimeException | Error finalActionFailure) {
                    restoreInterruptedStatusIfNeeded(finalActionFailure);

                    if (primaryFailure == null) {
                        throw finalActionFailure;
                    }

                    if (primaryFailure != finalActionFailure) {
                        primaryFailure.addSuppressed(finalActionFailure);
                    }
                }
            }
        }
    }

    private static void restoreInterruptedStatusIfNeeded(final Throwable failure) {
        // Fast paths. Every caught exception reaches this method, including on a hot
        // Try.call(cmd, defaultValue) loop, and the overwhelmingly common shape is a plain exception with
        // no cause and nothing suppressed - for which walking a graph (and allocating an identity set plus
        // a deque to do it) is pure overhead.
        if (failure instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            return;
        }

        if (failure.getCause() == null && failure.getSuppressed().length == 0) {
            return;
        }

        final Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        final ArrayDeque<Throwable> pending = new ArrayDeque<>();
        pending.add(failure);

        while (!pending.isEmpty()) {
            final Throwable current = pending.removeLast();

            if (!visited.add(current)) {
                continue;
            }

            if (current instanceof InterruptedException) {
                Thread.currentThread().interrupt();
                return;
            }

            // An ExecutionException or CompletionException carries what a task threw on *another* thread, so an
            // InterruptedException underneath one of them says nothing about this thread and must not interrupt
            // it. Exceptions suppressed on the wrapper itself are still examined: those are attached here.
            final Throwable cause = current instanceof ExecutionException || current instanceof CompletionException ? null : current.getCause();

            if (cause != null) {
                pending.add(cause);
            }

            for (final Throwable suppressed : current.getSuppressed()) {
                pending.add(suppressed);
            }
        }
    }

    /**
     * Creates a new Try instance with the specified AutoCloseable resource.
     *
     * <p>The resource will be automatically closed after the operation completes,
     * whether it succeeds or throws an exception.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Try.with(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)))
     *    .run(stream -> {
     *        // Process the stream
     *        byte[] data = stream.readAllBytes();
     *        System.out.println(new String(data, StandardCharsets.UTF_8));
     *    });
     * }</pre>
     *
     * @param <T> the type of the resource that extends AutoCloseable.
     * @param targetResource the resource to be managed by the Try instance.
     * @return a new Try instance managing the specified target resource.
     * @throws IllegalArgumentException if the targetResource is {@code null}.
     */
    public static <T extends AutoCloseable> Try<T> with(final T targetResource) throws IllegalArgumentException {
        N.checkArgNotNull(targetResource, cs.targetResource);

        return new Try<>(targetResource, null, null);
    }

    /**
     * Creates a new Try instance with the specified resource and a final action to execute after resource cleanup.
     *
     * <p>The final action is executed after the resource has been closed, regardless of whether
     * the main operation succeeded or failed. If resource acquisition itself fails, the final action
     * still runs. This is useful for additional cleanup or logging.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicBoolean finished = new AtomicBoolean();
     * Try.with(new BufferedReader(new StringReader("database row")), () -> finished.set(true))
     *    .run(reader -> System.out.println(reader.readLine()));
     * }</pre>
     *
     * @param <T> the type of the resource that extends AutoCloseable.
     * @param targetResource the resource to be managed by the Try instance.
     * @param finalAction the action to be executed after the resource is closed.
     * @return a new Try instance managing the specified target resource and final action.
     * @throws IllegalArgumentException if the targetResource is {@code null}, or if {@code finalAction} is
     *         {@code null}.
     */
    public static <T extends AutoCloseable> Try<T> with(final T targetResource, final Runnable finalAction) throws IllegalArgumentException {
        N.checkArgNotNull(targetResource, cs.targetResource);
        N.checkArgNotNull(finalAction, cs.finalAction);

        return new Try<>(targetResource, null, finalAction);
    }

    /**
     * Creates a new Try instance with a supplier that provides the AutoCloseable resource.
     *
     * <p>The resource is created lazily when the operation is executed. This is useful
     * when resource creation itself might throw an exception or when you want to delay
     * resource creation until it's actually needed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Throwables.Supplier<java.io.StringWriter, Exception> writerSupplier = java.io.StringWriter::new;
     * Try.with(writerSupplier)
     *    .run(writer -> {
     *        writer.write("Hello, World!");
     *        writer.flush();
     *    });
     * }</pre>
     *
     * <p>When a later operation acquires the resource, a {@code null} supplier result causes
     * {@link IllegalArgumentException}; overloads with a fallback route that acquisition failure to the fallback.</p>
     *
     * @param <T> the type of the resource that extends AutoCloseable.
     * @param targetResourceSupplier the supplier that provides the closeable resource; must not be {@code null},
     *                               and must not return {@code null} when it is invoked
     * @return a new Try instance managing the specified target resource supplier.
     * @throws IllegalArgumentException if {@code targetResourceSupplier} is {@code null}
     */
    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetResourceSupplier, cs.targetResourceSupplier);

        return new Try<>(null, targetResourceSupplier, null);
    }

    /**
     * Creates a new Try instance with a resource supplier and a final action.
     *
     * <p>Combines lazy resource creation with a final cleanup action. The resource is created
     * when needed, and the final action is executed after the resource is closed. If creation
     * fails, the final action still runs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AtomicBoolean finished = new AtomicBoolean();
     * Throwables.Supplier<ByteArrayInputStream, Exception> inputSupplier =
     *     () -> new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8));
     * String response = Try.with(
     *     inputSupplier,
     *     () -> finished.set(true)
     * ).call(stream -> new String(stream.readAllBytes(), StandardCharsets.UTF_8));
     * }</pre>
     *
     * <p>When a later operation acquires the resource, a {@code null} supplier result causes
     * {@link IllegalArgumentException}; overloads with a fallback route that acquisition failure to the fallback.</p>
     *
     * @param <T> the type of the resource that extends AutoCloseable.
     * @param targetResourceSupplier the supplier that provides the closeable resource; must not be {@code null},
     *                               and must not return {@code null} when it is invoked
     * @param finalAction the action to be executed after the resource is closed.
     * @return a new Try instance managing the specified target resource supplier and final action.
     * @throws IllegalArgumentException if {@code targetResourceSupplier} or {@code finalAction} is {@code null}
     */
    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier, final Runnable finalAction)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetResourceSupplier, cs.targetResourceSupplier);
        N.checkArgNotNull(finalAction, cs.finalAction);

        return new Try<>(null, targetResourceSupplier, finalAction);
    }

    /**
     * Executes the provided runnable, converting any checked exception to a RuntimeException.
     *
     * <p>This method is useful for working with lambda expressions or method references that
     * throw checked exceptions in contexts where only unchecked exceptions are allowed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Instead of handling InterruptedException
     * Try.run(() -> Thread.sleep(1000));
     *
     * // Working with I/O operations
     * Path path = Path.of("output.txt");
     * byte[] data = "content".getBytes(StandardCharsets.UTF_8);
     * Try.run(() -> Files.write(path, data));
     * }</pre>
     *
     * @param cmd the runnable task that might throw an exception.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @throws RuntimeException if the operation throws; checked exceptions are converted to unchecked exceptions
     * @see Throwables#run(Throwables.Runnable)
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            cmd.run();
        } catch (final Exception e) {
            restoreInterruptedStatusIfNeeded(e);
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Executes the provided runnable and handles any exception with the specified error handler.
     *
     * <p>Instead of propagating exceptions, this method allows you to handle them with custom logic,
     * such as logging, recovery, or graceful degradation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Try.run(
     *     () -> { throw new IOException("mail server unavailable"); },
     *     ex -> System.err.println("Failed to send email: " + ex.getMessage())
     * );
     *
     * // With recovery logic
     * AtomicBoolean fallbackUsed = new AtomicBoolean();
     * Try.run(
     *     () -> { throw new IOException("primary service unavailable"); },
     *     ex -> fallbackUsed.set(true)
     * );
     * }</pre>
     *
     * @param cmd the runnable task that might throw an exception.
     * @param actionOnError the consumer to handle any exceptions thrown by the {@code cmd}.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code actionOnError} is {@code null}.
     * @throws RuntimeException if {@code actionOnError} throws a runtime exception while handling an exception from {@code cmd}
     * @see Throwables#run(Throwables.Runnable, Consumer)
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd, final Consumer<? super Exception> actionOnError)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        try {
            cmd.run();
        } catch (final Exception e) {
            restoreInterruptedStatusIfNeeded(e);
            actionOnError.accept(e);
        }
    }

    /**
     * Executes the provided callable and returns its result, converting any checked exception to a RuntimeException.
     *
     * <p>This method enables the use of lambda expressions that throw checked exceptions in
     * contexts that expect unchecked behavior, while still returning the computed value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read file content without explicit exception handling
     * String content = Try.call(() -> Files.readString(Path.of("config.txt")));
     *
     * // Parse a value without a checked-exception declaration at the call site
     * Integer number = Try.call(() -> Integer.valueOf("42"));
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the callable task that might throw an exception and returns a result.
     * @return the result of the {@code cmd}.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @throws RuntimeException if the operation throws; checked exceptions are converted to unchecked exceptions
     * @see Throwables#call(Throwables.Callable)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            return cmd.call();
        } catch (final Exception e) {
            restoreInterruptedStatusIfNeeded(e);
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Executes the provided callable and returns its result, or applies the error function if an exception occurs.
     *
     * <p>This method provides a way to transform exceptions into valid return values, enabling
     * graceful error recovery and functional error handling patterns.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Return null on error
     * String user = Try.call(
     *     () -> { throw new IOException("user service unavailable"); },
     *     ex -> null
     * );
     *
     * // Transform exception to error response
     * String response = Try.call(
     *     () -> { throw new IOException("request failed"); },
     *     ex -> "error: " + ex.getMessage()
     * );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the callable task that might throw an exception and returns a result.
     * @param actionOnError the function to apply to the exception if one is thrown by the {@code cmd}.
     * @return the result of the {@code cmd} or the result of applying the {@code actionOnError} function to the exception if one is thrown.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code actionOnError} is {@code null}.
     * @throws RuntimeException if {@code actionOnError} throws a runtime exception while handling an exception from {@code cmd}
     * @see Throwables#call(Throwables.Callable, Function)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd, final Function<? super Exception, ? extends R> actionOnError)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        try {
            return cmd.call();
        } catch (final Exception e) {
            restoreInterruptedStatusIfNeeded(e);
            return actionOnError.apply(e);
        }
    }

    /**
     * Executes the provided callable and returns its result, or returns the value from the supplier if an exception occurs.
     *
     * <p>This method allows for lazy evaluation of the fallback value, which is only computed
     * if an exception actually occurs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Lazy default value computation
     * java.util.Properties config = Try.call(
     *     () -> { java.util.Properties p = new java.util.Properties(); p.load(new StringReader("mode=safe")); return p; },
     *     (java.util.function.Supplier<java.util.Properties>) java.util.Properties::new   // cast required: a
     *         // no-arg constructor reference fits both the Supplier and the Function overload
     * );
     *
     * // With expensive fallback
     * byte[] cachedData = Try.call(
     *     () -> Files.readAllBytes(Path.of("cache.bin")),
     *     () -> new byte[0]
     * );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the callable task that might throw an exception and returns a result.
     * @param supplier the supplier to provide a return value when an exception occurs.
     * @return the result of the {@code cmd} or the result of the {@code supplier} if an exception occurs.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code supplier} is {@code null}.
     * @throws RuntimeException if the fallback supplier throws after a failure
     * @see Throwables#call(Throwables.Callable, Supplier)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd, final Supplier<R> supplier)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(supplier, cs.supplier);

        try {
            return cmd.call();
        } catch (final Exception e) {
            restoreInterruptedStatusIfNeeded(e);
            return supplier.get();
        }
    }

    /**
     * Executes the provided callable and returns its result, or returns the default value if an exception occurs.
     *
     * <p>This is the simplest form of exception handling with a fallback value, useful when
     * you have a known default that should be used in case of any error.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Parse with default
     * String userInput = "not a number";
     * int value = Try.call(() -> Integer.parseInt(userInput), 0);
     *
     * // Load optional configuration
     * java.util.Properties properties = new java.util.Properties();
     * String setting = Try.call(
     *     () -> properties.getProperty("advanced.setting"),
     *     "default-value"
     * );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the callable task that might throw an exception and returns a result.
     * @param defaultValue the default value to return if an exception occurs during the execution of the {@code cmd}, may be {@code null}.
     * @return the result of the {@code cmd} or the default value if an exception occurs.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @see #call(java.util.concurrent.Callable, Supplier)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd, final R defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            return cmd.call();
        } catch (final Exception e) {
            restoreInterruptedStatusIfNeeded(e);
            return defaultValue;
        }
    }

    /**
     * Executes the callable with conditional exception handling based on a predicate.
     *
     * <p>If an exception occurs and the predicate returns {@code true}, the supplier provides the return value.
     * If the predicate returns {@code false}, the exception is rethrown as a RuntimeException.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Only handle specific exceptions
     * String result = Try.call(
     *     () -> { throw new IOException("read failed"); },
     *     ex -> ex instanceof IOException,
     *     () -> "default for IO errors"
     * );
     *
     * // Retry on timeout
     * String data = Try.call(
     *     () -> { throw new TimeoutException("timed out"); },
     *     ex -> ex instanceof TimeoutException,
     *     () -> "retried value"
     * );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the callable task that might throw an exception.
     * @param predicate the predicate to test the exception.
     * @param supplier the supplier to provide a return value when an exception occurs and the {@code predicate} returns {@code true}.
     * @return the result of the {@code cmd} or the result of the {@code supplier} if an exception occurs and the {@code predicate} returns {@code true}.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code predicate}, {@code supplier} is {@code null}.
     * @throws RuntimeException if the fallback supplier throws after a failure, or if the predicate throws or rejects the caught exception
     * @see Throwables#call(Throwables.Callable, Predicate, Supplier)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd, final Predicate<? super Exception> predicate, final Supplier<R> supplier)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.predicate);
        N.checkArgNotNull(supplier, cs.supplier);

        try {
            return cmd.call();
        } catch (final Exception e) {
            restoreInterruptedStatusIfNeeded(e);

            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Executes the callable with conditional exception handling and a default value.
     *
     * <p>Similar to {@link #call(java.util.concurrent.Callable, Predicate, Supplier)} but with an immediate default value
     * instead of a supplier.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Return -1 only for NumberFormatException
     * String input = "not a number";
     * int value = Try.call(
     *     () -> Integer.parseInt(input),
     *     ex -> ex instanceof NumberFormatException,
     *     -1
     * );
     *
     * // Return null only for specific database errors
     * String user = Try.call(
     *     () -> { throw new SQLException("Connection timeout"); },
     *     ex -> ex.getMessage().contains("Connection timeout"),
     *     (String) null
     * );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the callable task that might throw an exception and returns a result.
     * @param predicate the predicate to test the exception. If it returns {@code true}, the default value is returned. If it returns {@code false}, the exception is rethrown.
     * @param defaultValue the default value to return if an exception occurs during the execution of the {@code cmd} and the {@code predicate} returns {@code true}.
     * @return the result of the {@code cmd} or the default value if an exception occurs and the {@code predicate} returns {@code true}.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code predicate} is {@code null}.
     * @throws RuntimeException if the predicate throws or rejects the caught exception
     * @see #call(java.util.concurrent.Callable, Predicate, Supplier)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd, final Predicate<? super Exception> predicate, final R defaultValue)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.predicate);

        try {
            return cmd.call();
        } catch (final Exception e) {
            restoreInterruptedStatusIfNeeded(e);

            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Executes the provided consumer with the managed resource.
     *
     * <p>The resource is automatically closed after the consumer completes, and any final action
     * is executed. Checked exceptions are converted to RuntimeExceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Try.with(new BufferedReader(new StringReader("first line\nsecond line")))
     *    .run(reader -> {
     *        String line;
     *        while ((line = reader.readLine()) != null) {
     *            System.out.println(line);
     *        }
     *    });
     * }</pre>
     *
     * @param cmd the consumer that operates on the managed resource; must not be {@code null}.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @throws RuntimeException if resource acquisition, the operation, or resource closing throws; checked exceptions are converted to unchecked exceptions, or if the configured final action throws after otherwise successful or recovered execution
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);

        executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                cmd.accept(closeable);
            } catch (final Exception e) {
                restoreInterruptedStatusIfNeeded(e);

                throw ExceptionUtil.toRuntimeException(e, true);
            }

            return null;
        });
    }

    /**
     * Executes the provided consumer with the managed resource and custom exception handling.
     *
     * <p>Instead of throwing exceptions, this method allows you to handle them with custom logic.
     * The resource is still automatically closed and any final action is executed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = "request".getBytes(StandardCharsets.UTF_8);
     * Try.with(new java.io.ByteArrayOutputStream())
     *    .run(
     *        output -> output.write(data),
     *        ex -> System.err.println("Failed to write data: " + ex.getMessage())
     *    );
     * }</pre>
     *
     * @param cmd the consumer that operates on the managed resource; must not be {@code null}.
     * @param actionOnError the error handler invoked with any exception thrown while creating the
     *                      resource, executing the {@code cmd}, or closing the resource; must not be {@code null}.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code actionOnError} is {@code null}.
     * @throws RuntimeException if the error handler throws while handling an acquisition, operation, or close failure, or if the configured final action throws after otherwise successful or recovered execution
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd, final Consumer<? super Exception> actionOnError)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                cmd.accept(closeable);
            } catch (final Exception e) {
                restoreInterruptedStatusIfNeeded(e);
                actionOnError.accept(e);
            }

            return null;
        });
    }

    /**
     * Executes the provided function with the managed resource and returns the result.
     *
     * <p>The resource is automatically closed after the function completes, and any final action
     * is executed. Checked exceptions are converted to RuntimeExceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String content = Try.with(new ByteArrayInputStream("data".getBytes(StandardCharsets.UTF_8)))
     *     .call(stream -> new String(stream.readAllBytes(), StandardCharsets.UTF_8));
     *
     * List<String> lines = Try.with(new BufferedReader(new StringReader("one\ntwo")))
     *     .call(reader -> reader.lines().collect(Collectors.toList()));
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result; must not be {@code null}.
     * @return the result produced by the function.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @throws RuntimeException if resource acquisition, the operation, or resource closing throws; checked exceptions are converted to unchecked exceptions, or if the configured final action throws after otherwise successful or recovered execution
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);

        return executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                return cmd.apply(closeable);
            } catch (final Exception e) {
                restoreInterruptedStatusIfNeeded(e);

                throw ExceptionUtil.toRuntimeException(e, true);
            }
        });
    }

    /**
     * Executes the provided function with the managed resource and custom exception handling.
     *
     * <p>If an exception occurs, the error function is applied to produce a return value instead
     * of throwing an exception. The resource is still automatically closed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Throwables.Supplier<ByteArrayInputStream, Exception> inputSupplier =
     *     () -> new ByteArrayInputStream("mode=safe".getBytes(StandardCharsets.UTF_8));
     * java.util.Properties config = Try.with(inputSupplier)
     *     .call(
     *         stream -> { java.util.Properties p = new java.util.Properties(); p.load(stream); return p; },
     *         ex -> new java.util.Properties()
     *             // returns an empty configuration on error
     *     );
     * }</pre>
     *
     * <p><b>Note:</b> a failure from {@code close()} is handled here too, and it takes precedence over a
     * successful body: if {@code cmd} returns normally but closing the resource throws, the body's result
     * is discarded and the fallback is used instead.</p>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result; must not be {@code null}.
     * @param actionOnError the function to transform exceptions into return values; must not be {@code null}.
     * @return the result from the command or from the error handler if an exception occurs.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code actionOnError} is {@code null}.
     * @throws RuntimeException if the error handler throws while handling an acquisition, operation, or close failure, or if the configured final action throws after otherwise successful or recovered execution
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Function<? super Exception, ? extends R> actionOnError)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        return executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                return cmd.apply(closeable);
            } catch (final Exception e) {
                restoreInterruptedStatusIfNeeded(e);
                return actionOnError.apply(e);
            }
        });
    }

    /**
     * Executes the provided function with the managed resource, using a supplier for the fallback value.
     *
     * <p>If an exception occurs, the supplier is invoked to provide a return value. This allows
     * for lazy evaluation of the fallback value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Throwables.Supplier<FileInputStream, Exception> inputSupplier =
     *     () -> new FileInputStream("app.properties");
     * java.util.Properties props = Try.with(inputSupplier)
     *     .call(
     *         stream -> { java.util.Properties p = new java.util.Properties(); p.load(stream); return p; },
     *         (java.util.function.Supplier<java.util.Properties>) java.util.Properties::new   // cast required
     *     );
     * }</pre>
     *
     * <p><b>Note:</b> a failure from {@code close()} is handled here too, and it takes precedence over a
     * successful body: if {@code cmd} returns normally but closing the resource throws, the body's result
     * is discarded and the fallback is used instead.</p>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result; must not be {@code null}.
     * @param supplier the supplier to provide a fallback value if an exception occurs; must not be {@code null}.
     * @return the result from the command or from the supplier if an exception occurs.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code supplier} is {@code null}.
     * @throws RuntimeException if the fallback supplier throws after a failure, or if the configured final action throws after otherwise successful or recovered execution
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Supplier<R> supplier)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(supplier, cs.supplier);

        return executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                return cmd.apply(closeable);
            } catch (final Exception e) {
                restoreInterruptedStatusIfNeeded(e);
                return supplier.get();
            }
        });
    }

    /**
     * Executes the provided function with the managed resource, returning a default value on exception.
     *
     * <p>This is the simplest form of error handling with a known fallback value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int lineCount = Try.with(new BufferedReader(new StringReader("one\ntwo")))
     *     .call(
     *         reader -> (int) reader.lines().count(),
     *         0
     *     );
     * }</pre>
     *
     * <p><b>Note:</b> a failure from {@code close()} is handled here too, and it takes precedence over a
     * successful body: if {@code cmd} returns normally but closing the resource throws, the body's result
     * is discarded and the fallback is used instead.</p>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result; must not be {@code null}.
     * @param defaultValue the value to return if an exception occurs; may be {@code null}
     * @return the result from the command or the default value if an exception occurs.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @throws RuntimeException if the configured final action throws after otherwise successful or recovered execution
     * @see #call(Throwables.Function, Supplier)
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final R defaultValue)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);

        return executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                return cmd.apply(closeable);
            } catch (final Exception e) {
                restoreInterruptedStatusIfNeeded(e);
                return defaultValue;
            }
        });
    }

    /**
     * Executes the function with conditional exception handling based on a predicate.
     *
     * <p>If an exception occurs and the predicate returns {@code true}, the supplier provides the return value.
     * If the predicate returns {@code false}, the exception is rethrown as a RuntimeException.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Throwables.Supplier<ByteArrayInputStream, Exception> inputSupplier =
     *     () -> new ByteArrayInputStream(new byte[0]);
     * String user = Try.with(inputSupplier)
     *     .call(
     *         stream -> { throw new SQLTimeoutException("query timeout"); },
     *         ex -> ex instanceof SQLTimeoutException,
     *         () -> "guest"
     *             // returns a guest user only for timeout errors
     *     );
     * }</pre>
     *
     * <p><b>Note:</b> a failure from {@code close()} is handled here too, and it takes precedence over a
     * successful body: if {@code cmd} returns normally but closing the resource throws, the body's result
     * is discarded and the fallback is used instead.</p>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result; must not be {@code null}.
     * @param predicate the predicate to test exceptions; must not be {@code null}.
     * @param supplier the supplier to provide a fallback value for matching exceptions; must not be {@code null}.
     * @return the result from the command or from the supplier if a matching exception occurs.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code predicate}, {@code supplier} is {@code null}.
     * @throws RuntimeException if the fallback supplier throws after a failure, or if the predicate throws or rejects the caught exception, or if the configured final action throws after otherwise successful or recovered execution
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Predicate<? super Exception> predicate,
            final Supplier<R> supplier) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.predicate);
        N.checkArgNotNull(supplier, cs.supplier);

        return executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                return cmd.apply(closeable);
            } catch (final Exception e) {
                restoreInterruptedStatusIfNeeded(e);

                if (predicate.test(e)) {
                    return supplier.get();
                } else {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        });
    }

    /**
     * Executes the function with conditional exception handling and a default value.
     *
     * <p>Similar to {@link #call(Throwables.Function, Predicate, Supplier)} but with an immediate
     * default value instead of a supplier.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Path file = Path.of("missing.txt");
     * Throwables.Supplier<FileInputStream, Exception> inputSupplier =
     *     () -> new FileInputStream(file.toFile());
     * String content = Try.with(inputSupplier)
     *     .call(
     *         stream -> new String(stream.readAllBytes(), StandardCharsets.UTF_8),
     *         ex -> ex instanceof FileNotFoundException,
     *         "" // returns empty string only if file not found
     *     );
     * }</pre>
     *
     * <p><b>Note:</b> a failure from {@code close()} is handled here too, and it takes precedence over a
     * successful body: if {@code cmd} returns normally but closing the resource throws, the body's result
     * is discarded and the fallback is used instead.</p>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result; must not be {@code null}.
     * @param predicate the predicate to test exceptions; must not be {@code null}.
     * @param defaultValue the value to return for matching exceptions; may be {@code null}
     * @return the result from the command or the default value if a matching exception occurs.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code predicate} is {@code null}.
     * @throws RuntimeException if the predicate throws or rejects the caught exception, or if the configured final action throws after otherwise successful or recovered execution
     * @see #call(Throwables.Function, Predicate, Supplier)
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Predicate<? super Exception> predicate,
            final R defaultValue) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.predicate);

        return executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                return cmd.apply(closeable);
            } catch (final Exception e) {
                restoreInterruptedStatusIfNeeded(e);

                if (predicate.test(e)) {
                    return defaultValue;
                } else {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        });
    }
}
