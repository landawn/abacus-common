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
 *     Thread.sleep(1000); // throws InterruptedException
 * });
 * 
 * // Call code with return value and default on exception
 * String result = Try.call(() -> readFile("config.txt"), "default content");
 * 
 * // Handle exceptions with custom logic
 * Try.run(() -> riskyOperation(), ex -> logger.error("Operation failed", ex));
 * }</pre>
 * 
 * <p><b>Resource management examples:</b></p>
 * <pre>{@code
 * // Basic try-with-resources
 * Try.with(new FileInputStream("file.txt"))
 *    .run(stream -> processStream(stream));
 * 
 * // With final action
 * Try.with(connection, () -> connectionPool.returnConnection(connection))
 *    .call(conn -> conn.executeQuery("SELECT * FROM users"));
 * 
 * // With lazy resource initialization
 * Try.with(() -> new FileWriter("output.txt"))
 *    .run(writer -> writer.write("Hello, World!"));
 * }</pre>
 *
 * @param <T> The type of the resource that extends AutoCloseable
 */
public final class Try<T extends AutoCloseable> {
    private final T targetResource;
    private final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier;
    private final Runnable finalAction;

    Try(final T targetResource, final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier, final Runnable finalAction) {
        this.targetResource = targetResource;
        this.targetResourceSupplier = targetResourceSupplier;
        this.finalAction = finalAction;
    }

    /**
     * Creates a new Try instance with the specified AutoCloseable resource.
     * 
     * <p>The resource will be automatically closed after the operation completes,
     * whether it succeeds or throws an exception.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Try.with(new FileInputStream("data.txt"))
     *    .run(stream -> {
     *        // Process the stream
     *        byte[] data = stream.readAllBytes();
     *        System.out.println(new String(data));
     *    });
     * }</pre>
     *
     * @param <T> the type of the resource that extends AutoCloseable
     * @param targetResource the resource to be managed by the Try instance
     * @return a new Try instance managing the specified target resource
     * @throws IllegalArgumentException if the targetResource is null
     */
    public static <T extends AutoCloseable> Try<T> with(final T targetResource) throws IllegalArgumentException {
        N.checkArgNotNull(targetResource, cs.targetResource);

        return new Try<>(targetResource, null, null);
    }

    /**
     * Creates a new Try instance with the specified resource and a final action to execute after resource cleanup.
     * 
     * <p>The final action is executed after the resource has been closed, regardless of whether
     * the main operation succeeded or failed. This is useful for additional cleanup or logging.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = dataSource.getConnection();
     * Try.with(conn, () -> connectionPool.releaseConnection(conn))
     *    .run(connection -> {
     *        // Use the connection
     *        connection.createStatement().execute("UPDATE users SET active = true");
     *    });
     * }</pre>
     *
     * @param <T> the type of the resource that extends AutoCloseable
     * @param targetResource the resource to be managed by the Try instance
     * @param finalAction the action to be executed after the resource is closed
     * @return a new Try instance managing the specified target resource and final action
     * @throws IllegalArgumentException if the targetResource or finalAction is null
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
     * Try.with(() -> new FileWriter("output.txt"))
     *    .run(writer -> {
     *        writer.write("Hello, World!");
     *        writer.flush();
     *    });
     * }</pre>
     *
     * @param <T> the type of the resource that extends AutoCloseable
     * @param targetResourceSupplier the supplier to provide the resource to be managed by the Try instance, must not be null
     * @return a new Try instance managing the specified target resource supplier
     * @throws IllegalArgumentException if the targetResourceSupplier is null
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
     * when needed, and the final action is executed after the resource is closed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Try.with(
     *     () -> openNetworkConnection(),
     *     () -> logger.info("Connection closed")
     * ).call(conn -> conn.sendRequest(data));
     * }</pre>
     *
     * @param <T> the type of the resource that extends AutoCloseable
     * @param targetResourceSupplier the supplier to provide the resource to be managed by the Try instance, must not be null
     * @param finalAction the action to be executed after the resource is closed, must not be null
     * @return a new Try instance managing the specified target resource supplier and final action
     * @throws IllegalArgumentException if the targetResourceSupplier or finalAction is null
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
     * Try.run(() -> {
     *     Files.write(path, data);
     *     Files.copy(source, target);
     * });
     * }</pre>
     *
     * @param cmd The runnable task that might throw an exception. Must not be {@code null}.
     * @throws RuntimeException if an exception occurs during the execution of the {@code cmd}.
     * @see Throwables#run(Throwables.Runnable)
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd) {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            cmd.run();
        } catch (final Exception e) {
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
     *     () -> sendEmail(recipient, message),
     *     ex -> logger.error("Failed to send email to " + recipient, ex)
     * );
     * 
     * // With recovery logic
     * Try.run(
     *     () -> primaryService.process(data),
     *     ex -> fallbackService.process(data)
     * );
     * }</pre>
     *
     * @param cmd The runnable task that might throw an exception, must not be {@code null}.
     * @param actionOnError The consumer to handle any exceptions thrown by the {@code cmd}, must not be {@code null}.
     * @see Throwables#run(Throwables.Runnable, Consumer)
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd, final Consumer<? super Exception> actionOnError) {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        try {
            cmd.run();
        } catch (final Exception e) {
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
     * // Parse JSON that might throw checked exception
     * Config config = Try.call(() -> objectMapper.readValue(json, Config.class));
     * }</pre>
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @return The result of the {@code cmd}.
     * @throws RuntimeException if an exception occurs during the execution of the {@code cmd}.
     * @see Throwables#call(Throwables.Callable)
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd) {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
     * User user = Try.call(
     *     () -> userService.findById(userId),
     *     ex -> null
     * );
     * 
     * // Transform exception to error response
     * Response response = Try.call(
     *     () -> processRequest(request),
     *     ex -> Response.error(ex.getMessage())
     * );
     * }</pre>
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @param actionOnError The function to apply to the exception if one is thrown by the {@code cmd}. Must not be {@code null}.
     * @return The result of the {@code cmd} or the result of applying the {@code actionOnError} function to the exception if one is thrown.
     * @see Throwables#call(Throwables.Callable, Function)
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final Function<? super Exception, ? extends R> actionOnError) {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
     * Config config = Try.call(
     *     () -> loadConfigFromFile(),
     *     () -> createDefaultConfig()
     * );
     * 
     * // With expensive fallback
     * Data data = Try.call(
     *     () -> fetchFromCache(key),
     *     () -> fetchFromDatabase(key) // Only called if cache fails
     * );
     * }</pre>
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @param supplier The supplier to provide a return value when an exception occurs. Must not be {@code null}.
     * @return The result of the {@code cmd} or the result of the {@code supplier} if an exception occurs.
     * @see Throwables#call(Throwables.Callable, Supplier)
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final Supplier<R> supplier) {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(supplier, cs.supplier);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
     * int value = Try.call(() -> Integer.parseInt(userInput), 0);
     * 
     * // Load optional configuration
     * String setting = Try.call(
     *     () -> properties.getProperty("advanced.setting"),
     *     "default-value"
     * );
     * }</pre>
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @param defaultValue The default value to return if an exception occurs during the execution of the {@code cmd}.
     * @return The result of the {@code cmd} or the default value if an exception occurs.
     * @see #call(java.util.concurrent.Callable, Supplier)
     */
    // <R extends Comparable<? super R>> to avoid ambiguous error with Comparable<R>. Comparable is most common super interface for all types.
    public static <R extends Comparable<? super R>> R call(final java.util.concurrent.Callable<R> cmd, final R defaultValue) {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
     *     () -> riskyOperation(),
     *     ex -> ex instanceof IOException,
     *     () -> "default for IO errors"
     * );
     * 
     * // Retry on timeout
     * Data data = Try.call(
     *     () -> fetchWithTimeout(),
     *     ex -> ex instanceof TimeoutException,
     *     () -> fetchWithLongerTimeout()
     * );
     * }</pre>
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception, must not be {@code null}.
     * @param predicate The predicate to test the exception, must not be {@code null}.
     * @param supplier The supplier to provide a return value when an exception occurs and the {@code predicate} returns {@code true}, must not be {@code null}.
     * @return The result of the {@code cmd} or the result of the {@code supplier} if an exception occurs and the {@code predicate} returns {@code true}.
     * @throws RuntimeException if an exception occurs and the {@code predicate} returns {@code false}.
     * @see Throwables#call(Throwables.Callable, Predicate, Supplier)
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final Predicate<? super Exception> predicate, final Supplier<R> supplier) {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.Predicate);
        N.checkArgNotNull(supplier, cs.supplier);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
     * int value = Try.call(
     *     () -> Integer.parseInt(input),
     *     ex -> ex instanceof NumberFormatException,
     *     -1
     * );
     * 
     * // Return null only for specific database errors
     * User user = Try.call(
     *     () -> userDao.findById(id),
     *     ex -> ex.getMessage().contains("Connection timeout"),
     *     null
     * );
     * }</pre>
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @param predicate The predicate to test the exception. If it returns {@code true}, the default value is returned. If it returns {@code false}, the exception is rethrown. Must not be {@code null}.
     * @param defaultValue The default value to return if an exception occurs during the execution of the {@code cmd} and the {@code predicate} returns {@code true}.
     * @return The result of the {@code cmd} or the default value if an exception occurs and the {@code predicate} returns {@code true}.
     * @throws RuntimeException if an exception occurs and the {@code predicate} returns {@code false}.
     * @see #call(java.util.concurrent.Callable, Predicate, Supplier)
     */
    // <R extends Comparable<? super R>> to avoid ambiguous error with Comparable<R>. Comparable is most common super interface for all types.
    public static <R extends Comparable<? super R>> R call(final java.util.concurrent.Callable<R> cmd, final Predicate<? super Exception> predicate,
            final R defaultValue) {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.Predicate);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
     * Try.with(new BufferedReader(new FileReader("data.txt")))
     *    .run(reader -> {
     *        String line;
     *        while ((line = reader.readLine()) != null) {
     *            System.out.println(line);
     *        }
     *    });
     * }</pre>
     *
     * @param cmd the consumer that operates on the managed resource
     * @throws RuntimeException if an exception occurs during execution or resource management
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            cmd.accept(closeable);
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     * Executes the provided consumer with the managed resource and custom exception handling.
     * 
     * <p>Instead of throwing exceptions, this method allows you to handle them with custom logic.
     * The resource is still automatically closed and any final action is executed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Try.with(new Socket("server.com", 8080))
     *    .run(
     *        socket -> socket.getOutputStream().write(data),
     *        ex -> logger.error("Failed to send data", ex)
     *    );
     * }</pre>
     *
     * @param cmd the consumer that operates on the managed resource
     * @param actionOnError the error handler for any exceptions that occur
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd, final Consumer<? super Exception> actionOnError) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            cmd.accept(closeable);
        } catch (final Exception e) {
            actionOnError.accept(e);
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     * Executes the provided function with the managed resource and returns the result.
     * 
     * <p>The resource is automatically closed after the function completes, and any final action
     * is executed. Checked exceptions are converted to RuntimeExceptions.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String content = Try.with(new FileInputStream("data.txt"))
     *     .call(stream -> new String(stream.readAllBytes()));
     * 
     * List<String> lines = Try.with(Files.newBufferedReader(path))
     *     .call(reader -> reader.lines().collect(Collectors.toList()));
     * }</pre>
     *
     * @param <R> the type of the result
     * @param cmd the function that operates on the managed resource and returns a result
     * @return the result produced by the function
     * @throws RuntimeException if an exception occurs during execution or resource management
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     * Executes the provided function with the managed resource and custom exception handling.
     * 
     * <p>If an exception occurs, the error function is applied to produce a return value instead
     * of throwing an exception. The resource is still automatically closed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonNode config = Try.with(() -> new FileInputStream("config.json"))
     *     .call(
     *         stream -> objectMapper.readTree(stream),
     *         ex -> objectMapper.createObjectNode() // Return empty config on error
     *     );
     * }</pre>
     *
     * @param <R> the type of the result
     * @param cmd the function that operates on the managed resource and returns a result
     * @param actionOnError the function to transform exceptions into return values
     * @return the result from the command or from the error handler if an exception occurs
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd,
            final Function<? super Exception, ? extends R> actionOnError) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            return actionOnError.apply(e);
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     * Executes the provided function with the managed resource, using a supplier for the fallback value.
     * 
     * <p>If an exception occurs, the supplier is invoked to provide a return value. This allows
     * for lazy evaluation of the fallback value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Properties props = Try.with(() -> new FileInputStream("app.properties"))
     *     .call(
     *         stream -> { Properties p = new Properties(); p.load(stream); return p; },
     *         () -> loadDefaultProperties() // Only called if loading fails
     *     );
     * }</pre>
     *
     * @param <R> the type of the result
     * @param cmd the function that operates on the managed resource and returns a result
     * @param supplier the supplier to provide a fallback value if an exception occurs
     * @return the result from the command or from the supplier if an exception occurs
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Supplier<R> supplier) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            return supplier.get();
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     * Executes the provided function with the managed resource, returning a default value on exception.
     * 
     * <p>This is the simplest form of error handling with a known fallback value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int lineCount = Try.with(Files.newBufferedReader(path))
     *     .call(
     *         reader -> (int) reader.lines().count(),
     *         0 // Default to 0 if file cannot be read
     *     );
     * }</pre>
     *
     * @param <R> the type of the result
     * @param cmd the function that operates on the managed resource and returns a result
     * @param defaultValue the value to return if an exception occurs
     * @return the result from the command or the default value if an exception occurs
     * @see #call(Throwables.Function, Supplier)
     */
    // <R extends Comparable<? super R>> to avoid ambiguous error with Comparable<R>. Comparable is most common super interface for all types.
    public <R extends Comparable<? super R>> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final R defaultValue) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            return defaultValue;
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     * Executes the function with conditional exception handling based on a predicate.
     * 
     * <p>If an exception occurs and the predicate returns {@code true}, the supplier provides the return value.
     * If the predicate returns {@code false}, the exception is rethrown as a RuntimeException.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = Try.with(databaseConnection)
     *     .call(
     *         conn -> userDao.findById(conn, userId),
     *         ex -> ex instanceof SQLException && ex.getMessage().contains("timeout"),
     *         () -> User.guest() // Return guest user only for timeout errors
     *     );
     * }</pre>
     *
     * @param <R> the type of the result
     * @param cmd the function that operates on the managed resource and returns a result
     * @param predicate the predicate to test exceptions
     * @param supplier the supplier to provide a fallback value for matching exceptions
     * @return the result from the command or from the supplier if a matching exception occurs
     * @throws RuntimeException if an exception occurs that doesn't match the predicate
     */
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Predicate<? super Exception> predicate,
            final Supplier<R> supplier) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     * Executes the function with conditional exception handling and a default value.
     * 
     * <p>Similar to {@link #call(Throwables.Function, Predicate, Supplier)} but with an immediate
     * default value instead of a supplier.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String content = Try.with(new FileInputStream(file))
     *     .call(
     *         stream -> new String(stream.readAllBytes()),
     *         ex -> ex instanceof FileNotFoundException,
     *         "" // Return empty string only if file not found
     *     );
     * }</pre>
     *
     * @param <R> the type of the result
     * @param cmd the function that operates on the managed resource and returns a result
     * @param predicate the predicate to test exceptions
     * @param defaultValue the value to return for matching exceptions
     * @return the result from the command or the default value if a matching exception occurs
     * @throws RuntimeException if an exception occurs that doesn't match the predicate
     * @see #call(Throwables.Function, Predicate, Supplier)
     */
    // <R extends Comparable<? super R>> to avoid ambiguous error with Comparable<R>. Comparable is most common super interface for all types.
    public <R extends Comparable<? super R>> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd,
            final Predicate<? super Exception> predicate, final R defaultValue) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }
}
