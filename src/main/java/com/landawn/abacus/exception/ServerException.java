/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.exception;

import java.io.Serial;

/**
 * Exception thrown when a server-side error occurs during application execution.
 * This is a runtime exception that indicates an error on the server side, typically used
 * in distributed systems, client-server architectures, or web applications to distinguish
 * server errors from client errors.
 *
 * <p>Common use cases include:</p>
 * <ul>
 *   <li>Remote service invocation failures</li>
 *   <li>Server-side validation or business logic errors</li>
 *   <li>Backend system unavailability or malfunction</li>
 *   <li>Server configuration or resource issues</li>
 *   <li>API gateway or proxy errors</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // In a remote service call
 * if (response.getStatusCode() >= 500) {
 *     throw new ServerException("Server returned error: " + response.getStatusCode());
 * }
 *
 * // In a backend service method
 * if (!backendService.isAvailable()) {
 *     throw new ServerException("Backend service is currently unavailable");
 * }
 *
 * // Wrapping a server-side error
 * try {
 *     processOnServer(data);
 * } catch (Exception e) {
 *     throw new ServerException("Server processing failed", e);
 * }
 * }</pre>
 *
 * @see RuntimeException
 * @since 0.8
 */
public class ServerException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1135227562167675407L;

    /**
     * Constructs a new {@code ServerException} with no detail message.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new ServerException();
     * }</pre>
     */
    public ServerException() {
    }

    /**
     * Constructs a new {@code ServerException} with the specified detail message.
     * The cause is not initialized and may subsequently be initialized by a call to {@link #initCause}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * throw new ServerException("Database server connection timeout after 30 seconds");
     * }</pre>
     *
     * @param message the detail message which provides information about the server error.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public ServerException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code ServerException} with the specified detail message and cause.
     *
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically
     * incorporated in this exception's detail message.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     remoteService.execute(request);
     * } catch (RemoteException e) {
     *     throw new ServerException("Remote service execution failed", e);
     * }
     * }</pre>
     *
     * @param message the detail message which provides information about the server error.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public ServerException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code ServerException} with the specified cause and a detail
     * message of {@code (cause==null ? {@code null} : cause.toString())} (which typically contains the
     * class and detail message of {@code cause}).
     *
     * <p>This constructor is useful for exceptions that are little more than wrappers for other
     * throwables.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * catch (ServiceUnavailableException e) {
     *     throw new ServerException(e);
     * }
     * }</pre>
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).
     *              A {@code null} value is permitted, and indicates that the cause is nonexistent or unknown.
     */
    public ServerException(final Throwable cause) {
        super(cause);
    }
}
