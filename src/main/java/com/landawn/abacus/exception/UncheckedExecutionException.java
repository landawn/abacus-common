/*
 * Copyright (C) 2026 HaiYang Li
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

package com.landawn.abacus.exception;

import java.io.Serial;
import java.util.concurrent.ExecutionException;

/**
 * A runtime exception that wraps {@link ExecutionException}, allowing execution
 * exceptions to be thrown without being declared in method signatures.
 *
 * <p>{@link ExecutionException} is a checked exception thrown when attempting to retrieve
 * the result of a task that aborted by throwing an exception. It is commonly encountered
 * when working with {@link java.util.concurrent.Future} and other asynchronous computation APIs.</p>
 *
 * <p>This exception is useful in contexts where you need to handle {@code ExecutionException} but
 * cannot change the method signature to declare it, such as in:</p>
 * <ul>
 *   <li>Lambda expressions and functional interfaces that don't declare checked exceptions.</li>
 *   <li>Stream operations where checked exceptions are not allowed.</li>
 *   <li>Implementing interfaces that don't declare {@code ExecutionException}.</li>
 *   <li>Simplifying error handling in asynchronous task processing code.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Retrieving a Future result without declaring ExecutionException
 * public String getResult(Future<String> future) {
 *     try {
 *         return future.get();
 *     } catch (ExecutionException e) {
 *         throw new UncheckedExecutionException("Task execution failed", e);
 *     } catch (InterruptedException e) {
 *         Thread.currentThread().interrupt();
 *         throw new UncheckedInterruptedException(e);
 *     }
 * }
 *
 * // In a stream operation processing Future results
 * List<String> results = futures.stream()
 *     .map(future -> {
 *         try {
 *             return future.get();
 *         } catch (ExecutionException e) {
 *             throw new UncheckedExecutionException(e);
 *         } catch (InterruptedException e) {
 *             Thread.currentThread().interrupt();
 *             throw new UncheckedInterruptedException(e);
 *         }
 *     })
 *     .collect(Collectors.toList());
 * }</pre>
 *
 * @see UncheckedException
 * @see ExecutionException
 * @see java.util.concurrent.Future
 */
public class UncheckedExecutionException extends UncheckedException {

    @Serial
    private static final long serialVersionUID = -8705769192988987051L;

    /**
     * Constructs a new {@code UncheckedExecutionException} by wrapping the specified {@link ExecutionException}.
     *
     * <p>This constructor preserves all information from the original {@code ExecutionException} including
     * its message, stack trace, and any suppressed exceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     String result = future.get();
     * } catch (ExecutionException e) {
     *     throw new UncheckedExecutionException(e);
     * }
     * }</pre>
     *
     * @param cause the {@link ExecutionException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedExecutionException(final ExecutionException cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code UncheckedExecutionException} with the specified detail message
     * and {@link ExecutionException}.
     *
     * <p>This constructor allows you to provide additional context about where or why the
     * execution failed, while preserving all information from the original exception.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     Object result = future.get(5, TimeUnit.SECONDS);
     * } catch (ExecutionException e) {
     *     throw new UncheckedExecutionException("Task failed for job ID: " + jobId, e);
     * }
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     * @param cause the {@link ExecutionException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedExecutionException(final String message, final ExecutionException cause) {
        super(message, cause);
    }
}
