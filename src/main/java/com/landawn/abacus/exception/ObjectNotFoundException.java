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

package com.landawn.abacus.exception;

import java.io.Serial;

/**
 * Exception thrown when an expected object cannot be found.
 * This exception extends {@link IllegalStateException} to indicate that the application
 * is in an illegal state due to the absence of a required object.
 * 
 * <p>Common use cases include:</p>
 * <ul>
 *   <li>Database queries expecting to find a record but returning no results</li>
 *   <li>Cache lookups that fail to find the requested entry</li>
 *   <li>Resource loading operations where the resource doesn't exist</li>
 *   <li>API calls expecting an entity that has been deleted or never existed</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // In a service method
 * User user = userRepository.findById(userId);
 * if (user == null) {
 *     throw new ObjectNotFoundException("User not found with ID: " + userId);
 * }
 * }</pre>
 * 
 * @since 2.0
 * @see IllegalStateException
 */
public class ObjectNotFoundException extends IllegalStateException {

    @Serial
    private static final long serialVersionUID = -1806452586200243492L;

    /**
     * Constructs a new {@code ObjectNotFoundException} with no detail message.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new ObjectNotFoundException();
     * }</pre>
     */
    public ObjectNotFoundException() {
    }

    /**
     * Constructs a new {@code ObjectNotFoundException} with the specified detail message.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new ObjectNotFoundException("Product with SKU '" + sku + "' not found in inventory");
     * }</pre>
     *
     * @param message the detail message which provides information about the missing object.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public ObjectNotFoundException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ObjectNotFoundException} with the specified detail message and cause.
     * 
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
     * incorporated in this exception's detail message.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     return cache.get(key);
     * } catch (CacheException e) {
     *     throw new ObjectNotFoundException("Failed to retrieve object from cache", e);
     * }
     * }</pre>
     *
     * @param message the detail message which provides information about the missing object.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public ObjectNotFoundException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code ObjectNotFoundException} with the specified cause and a detail
     * message of {@code (cause==null ? {@code null} : cause.toString())} (which typically contains the
     * class and detail message of {@code cause}).
     * 
     * <p>This constructor is useful for exceptions that are little more than wrappers for other
     * throwables.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * catch (DataAccessException e) {
     *     throw new ObjectNotFoundException(e);
     * }
     * }</pre>
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public ObjectNotFoundException(final Throwable cause) {
        super(cause);
    }
}
