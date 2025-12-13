/*
 * Copyright (C) 2015 HaiYang Li
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
 * Exception thrown when an operation that expects a unique result encounters duplicate results.
 * This exception extends {@link IllegalStateException} to indicate that the application is in
 * an illegal state due to the presence of duplicate results where uniqueness was expected.
 * 
 * <p>Common use cases include:</p>
 * <ul>
 *   <li>Database queries expected to return a single row but returning multiple rows</li>
 *   <li>Collection operations where uniqueness constraint is violated</li>
 *   <li>Stream operations expecting a single element but finding duplicates</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // In a DAO method expecting unique result
 * List<User> users = query.getResultList();
 * if (users.size() > 1) {
 *     throw new DuplicatedResultException("Expected unique user but found " + users.size());
 * }
 * }</pre>
 * 
 * @see IllegalStateException
 */
public class DuplicatedResultException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = -8407459420058648924L;

    /**
     * Constructs a new {@code DuplicatedResultException} with no detail message.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new DuplicatedResultException();
     * }</pre>
     */
    public DuplicatedResultException() {
    }

    /**
     * Constructs a new {@code DuplicatedResultException} with the specified detail message.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new DuplicatedResultException("Found 3 records for unique ID: " + id);
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     */
    public DuplicatedResultException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DuplicatedResultException} with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
     * incorporated in this exception's detail message.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     return repository.findUniqueById(id);
     * } catch (DataAccessException e) {
     *     throw new DuplicatedResultException("Multiple results for unique constraint", e);
     * }
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public DuplicatedResultException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code DuplicatedResultException} with the specified cause and a detail
     * message of {@code (cause==null ? null : cause.toString())} (which typically contains the
     * class and detail message of {@code cause}).
     *
     * <p>This constructor is useful for exceptions that are little more than wrappers for other
     * throwables.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * catch (SQLException e) {
     *     throw new DuplicatedResultException(e);
     * }
     * }</pre>
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public DuplicatedResultException(final Throwable cause) {
        super(cause);
    }
}
