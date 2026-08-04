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
 * its supplier returns a fresh, non-null resource for every invocation.</p>
 *
 * <p>A configured final action runs after every attempted instance operation, including when resource
 * acquisition fails. If a resource was acquired, try-with-resources closes it before the final action.</p>
 *
 * <p><b>Overload note:</b> Because {@code with} accepts either a resource or a resource supplier,
 * target an inline supplier lambda or method reference as a {@link Throwables.Supplier} (by assignment
 * or cast). Similarly, target fallback lambdas as {@link Supplier} or {@link Function} when an immediate
 * default-value overload would otherwise make the invocation ambiguous.</p>
 *
 * <p>If an {@link InterruptedException} is converted, handled, or replaced with a fallback value,
 * the current thread's interrupted status is restored before control is passed to user recovery code.
 * This also applies when the interruption is a cause or a suppressed close failure.</p>
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

    private T acquireResource() throws Exception {
        final T resource = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource;
        return N.checkArgNotNull(resource, cs.targetResource);
    }

    private <R> R executeWithFinalAction(final Throwables.Callable<R, RuntimeException> operation) {
        Throwable primaryFailure = null;

        try {
            return operation.call();
        } catch (final RuntimeException | Error e) {
            restoreInterruptedStatusIfNeeded(e);
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

            final Throwable cause = current.getCause();

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
     * @param <T> the type of the resource that extends AutoCloseable.
     * @param targetResourceSupplier the supplier that provides the closeable resource; must not be {@code null}
     * @return a new Try instance managing the specified target resource supplier.
     * @throws IllegalArgumentException if {@code targetResourceSupplier} is {@code null}.
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
     * @param <T> the type of the resource that extends AutoCloseable.
     * @param targetResourceSupplier the supplier that provides the closeable resource; must not be {@code null}
     * @param finalAction the action to be executed after the resource is closed.
     * @return a new Try instance managing the specified target resource supplier and final action.
     * @throws IllegalArgumentException if any of {@code targetResourceSupplier}, {@code finalAction} is {@code null}.
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
     * @throws RuntimeException if an exception occurs during the execution of the {@code cmd}.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @see Throwables#run(Throwables.Runnable)
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd) throws IllegalArgumentException {
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
     * @see Throwables#run(Throwables.Runnable, Consumer)
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd, final Consumer<? super Exception> actionOnError)
            throws IllegalArgumentException {
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
     * @throws RuntimeException if an exception occurs during the execution of the {@code cmd}.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @see Throwables#call(Throwables.Callable)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd) throws IllegalArgumentException {
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
     *     (java.util.function.Function<Exception, String>) ex -> null
     * );
     *
     * // Transform exception to error response
     * String response = Try.call(
     *     () -> { throw new IOException("request failed"); },
     *     (java.util.function.Function<Exception, String>) ex -> "error: " + ex.getMessage()
     * );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the callable task that might throw an exception and returns a result.
     * @param actionOnError the function to apply to the exception if one is thrown by the {@code cmd}.
     * @return the result of the {@code cmd} or the result of applying the {@code actionOnError} function to the exception if one is thrown.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code actionOnError} is {@code null}.
     * @see Throwables#call(Throwables.Callable, Function)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd, final Function<? super Exception, ? extends R> actionOnError)
            throws IllegalArgumentException {
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
     *     (java.util.function.Supplier<java.util.Properties>) java.util.Properties::new
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
     * @see Throwables#call(Throwables.Callable, Supplier)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd, final Supplier<R> supplier) throws IllegalArgumentException {
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
    public static <R extends Comparable<? super R>> R call(final java.util.concurrent.Callable<? extends R> cmd, final R defaultValue)
            throws IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);

        // <R extends Comparable<? super R>> avoids ambiguous overloads involving Comparable<R>.

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
     *     (java.util.function.Supplier<String>) () -> "default for IO errors"
     * );
     *
     * // Retry on timeout
     * String data = Try.call(
     *     () -> { throw new TimeoutException("timed out"); },
     *     ex -> ex instanceof TimeoutException,
     *     (java.util.function.Supplier<String>) () -> "retried value"
     * );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the callable task that might throw an exception.
     * @param predicate the predicate to test the exception.
     * @param supplier the supplier to provide a return value when an exception occurs and the {@code predicate} returns {@code true}.
     * @return the result of the {@code cmd} or the result of the {@code supplier} if an exception occurs and the {@code predicate} returns {@code true}.
     * @throws RuntimeException if an exception occurs and the {@code predicate} returns {@code false}.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code predicate}, {@code supplier} is {@code null}.
     * @see Throwables#call(Throwables.Callable, Predicate, Supplier)
     */
    public static <R> R call(final java.util.concurrent.Callable<? extends R> cmd, final Predicate<? super Exception> predicate, final Supplier<R> supplier)
            throws IllegalArgumentException {
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
     * @throws RuntimeException if an exception occurs and the {@code predicate} returns {@code false}.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code predicate} is {@code null}.
     * @see #call(java.util.concurrent.Callable, Predicate, Supplier)
     */
    public static <R extends Comparable<? super R>> R call(final java.util.concurrent.Callable<? extends R> cmd, final Predicate<? super Exception> predicate,
            final R defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.predicate);

        // <R extends Comparable<? super R>> avoids ambiguous overloads involving Comparable<R>.

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
     * @param cmd the consumer that operates on the managed resource;
     * @throws RuntimeException if an exception occurs while creating the resource, executing the
     *         {@code cmd}, or closing the resource. Checked exceptions are converted via
     *         {@link ExceptionUtil#toRuntimeException(Throwable, boolean)}. If the final action
     *         also fails while another failure is being propagated, its failure is suppressed on
     *         the primary failure.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd) throws IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);

        executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                cmd.accept(closeable);
            } catch (final Exception e) {
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
     * @param cmd the consumer that operates on the managed resource;
     * @param actionOnError the error handler invoked with any exception thrown while creating the
     *                      resource, executing the {@code cmd}, or closing the resource;
     * @throws IllegalArgumentException if any of {@code cmd}, {@code actionOnError} is {@code null}.
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd, final Consumer<? super Exception> actionOnError)
            throws IllegalArgumentException {
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
     * @param cmd the function that operates on the managed resource and returns a result;
     * @return the result produced by the function.
     * @throws RuntimeException if an exception occurs while creating the resource, executing the
     *         {@code cmd}, or closing the resource. Checked exceptions are converted via
     *         {@link ExceptionUtil#toRuntimeException(Throwable, boolean)}.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd) throws IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);

        return executeWithFinalAction(() -> {
            try (final T closeable = acquireResource()) {
                return cmd.apply(closeable);
            } catch (final Exception e) {
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
     *         (java.util.function.Function<Exception, java.util.Properties>) ex -> new java.util.Properties()
     *             // returns an empty configuration on error
     *     );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result;
     * @param actionOnError the function to transform exceptions into return values;
     * @return the result from the command or from the error handler if an exception occurs.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code actionOnError} is {@code null}.
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Function<? super Exception, ? extends R> actionOnError)
            throws IllegalArgumentException {
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
     *         (java.util.function.Supplier<java.util.Properties>) java.util.Properties::new
     *     );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result;
     * @param supplier the supplier to provide a fallback value if an exception occurs;
     * @return the result from the command or from the supplier if an exception occurs.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code supplier} is {@code null}.
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Supplier<R> supplier) throws IllegalArgumentException {
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
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result;
     * @param defaultValue the value to return if an exception occurs; may be {@code null}
     * @return the result from the command or the default value if an exception occurs.
     * @throws IllegalArgumentException if {@code cmd} is {@code null}.
     * @see #call(Throwables.Function, Supplier)
     */
    public <R extends Comparable<? super R>> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final R defaultValue)
            throws IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);

        // <R extends Comparable<? super R>> avoids ambiguous overloads involving Comparable<R>.

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
     *         (java.util.function.Supplier<String>) () -> "guest"
     *             // returns a guest user only for timeout errors
     *     );
     * }</pre>
     *
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result;
     * @param predicate the predicate to test exceptions;
     * @param supplier the supplier to provide a fallback value for matching exceptions;
     * @return the result from the command or from the supplier if a matching exception occurs.
     * @throws RuntimeException if an exception occurs that doesn't match the predicate.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code predicate}, {@code supplier} is {@code null}.
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Predicate<? super Exception> predicate,
            final Supplier<R> supplier) throws IllegalArgumentException {
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
     * @param <R> the type of the result.
     * @param cmd the function that operates on the managed resource and returns a result;
     * @param predicate the predicate to test exceptions;
     * @param defaultValue the value to return for matching exceptions; may be {@code null}
     * @return the result from the command or the default value if a matching exception occurs.
     * @throws RuntimeException if an exception occurs that doesn't match the predicate.
     * @throws IllegalArgumentException if any of {@code cmd}, {@code predicate} is {@code null}.
     * @see #call(Throwables.Function, Predicate, Supplier)
     */
    public <R extends Comparable<? super R>> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd,
            final Predicate<? super Exception> predicate, final R defaultValue) throws IllegalArgumentException {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.predicate);

        // <R extends Comparable<? super R>> avoids ambiguous overloads involving Comparable<R>.

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
