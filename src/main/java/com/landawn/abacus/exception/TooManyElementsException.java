/*
 * Copyright (C) 2022 HaiYang Li
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

/**
 * Exception thrown when an operation encounters more elements than expected or allowed.
 * This exception extends {@link IllegalStateException} to indicate that the application
 * is in an illegal state due to exceeding the expected number of elements.
 * 
 * <p>Common use cases include:</p>
 * <ul>
 *   <li>Stream operations expecting at most one element but finding multiple</li>
 *   <li>Collection operations with size constraints that are violated</li>
 *   <li>Buffer or queue operations that exceed capacity limits</li>
 *   <li>API responses that return more results than the maximum allowed</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // In a stream operation expecting at most one element
 * List<String> results = stream.collect(Collectors.toList());
 * if (results.size() > 1) {
 *     throw new TooManyElementsException("Expected at most 1 element, but found: " + results.size());
 * }
 * 
 * // In a bounded collection
 * if (queue.size() >= maxCapacity) {
 *     throw new TooManyElementsException("Queue capacity exceeded: " + maxCapacity);
 * }
 * }</pre>
 * 
 * @see IllegalStateException
 * @see DuplicatedResultException
 */
public class TooManyElementsException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = 4230938963102900489L;

    /**
     * Constructs a new {@code TooManyElementsException} with no detail message.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new TooManyElementsException();
     * }</pre>
     */
    public TooManyElementsException() {
    }

    /**
     * Constructs a new {@code TooManyElementsException} with the specified detail message.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new TooManyElementsException("Result set contains " + count + " elements, maximum allowed is " + max);
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     */
    public TooManyElementsException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code TooManyElementsException} with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
     * incorporated in this exception's detail message.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     validateCollectionSize(collection);
     * } catch (ValidationException e) {
     *     throw new TooManyElementsException("Collection validation failed", e);
     * }
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public TooManyElementsException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code TooManyElementsException} with the specified cause and a detail
     * message of {@code (cause==null ? null : cause.toString())} (which typically contains the
     * class and detail message of {@code cause}).
     *
     * <p>This constructor is useful for exceptions that are little more than wrappers for other
     * throwables.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * catch (SQLException e) {
     *     throw new TooManyElementsException(e);
     * }
     * }</pre>
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public TooManyElementsException(final Throwable cause) {
        super(cause);
    }
}
