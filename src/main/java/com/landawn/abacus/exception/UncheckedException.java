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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;

/**
 * Base class for runtime exceptions that wrap checked exceptions, allowing them to be thrown
 * without being declared in method signatures.
 *
 * <p>This class preserves all information from the original checked exception, including:</p>
 * <ul>
 *   <li>The complete stack trace.</li>
 *   <li>All suppressed exceptions.</li>
 *   <li>The full cause chain.</li>
 * </ul>
 *
 * <p>Specialized subclasses are provided for commonly wrapped exception types:</p>
 * <ul>
 *   <li>{@link UncheckedIOException} for {@link java.io.IOException}.</li>
 *   <li>{@link UncheckedSQLException} for {@link java.sql.SQLException}.</li>
 *   <li>{@link UncheckedInterruptedException} for {@link InterruptedException}.</li>
 *   <li>{@link UncheckedExecutionException} for {@link java.util.concurrent.ExecutionException}.</li>
 *   <li>{@link UncheckedParseException} for {@link java.text.ParseException}.</li>
 *   <li>{@link UncheckedReflectiveOperationException} for {@link ReflectiveOperationException}.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * try {
 *     someMethodThatThrowsCheckedException();
 * } catch (SomeCheckedException e) {
 *     throw new UncheckedException(e);
 * }
 *
 * // With additional context message
 * try {
 *     someMethodThatThrowsCheckedException();
 * } catch (SomeCheckedException e) {
 *     throw new UncheckedException("Operation failed during processing", e);
 * }
 * }</pre>
 *
 * @see RuntimeException
 * @see UncheckedIOException
 * @see UncheckedSQLException
 * @see UncheckedExecutionException
 * @see UncheckedInterruptedException
 * @see UncheckedParseException
 * @see UncheckedReflectiveOperationException
 */
public class UncheckedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1973552812345999717L;

    /**
     * Constructs a new {@code UncheckedException} by wrapping the specified checked exception.
     * This constructor preserves all information from the original exception including its
     * stack trace and any suppressed exceptions.
     *
     * <p>The wrapped exception becomes the cause of this {@code UncheckedException}, allowing the
     * original exception information to be preserved and accessed via {@link #getCause()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     Class.forName("com.example.SomeClass");
     * } catch (ClassNotFoundException e) {
     *     throw new UncheckedException(e);
     * }
     * }</pre>
     *
     * @param cause the checked exception to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedException(final Throwable cause) {
        super(getCause(cause));

        final Throwable[] suppressedExceptions = cause.getSuppressed();
        if (N.notEmpty(suppressedExceptions)) {
            for (final Throwable suppressedException : suppressedExceptions) {
                this.addSuppressed(suppressedException);
            }
        }
    }

    /**
     * Constructs a new {@code UncheckedException} with the specified detail message and checked exception.
     * This constructor allows you to provide additional context while preserving all information
     * from the original exception.
     *
     * <p>The custom error message helps provide context about where or why the exception was wrapped,
     * while the original exception's message and stack trace remain accessible through the cause.</p>
     *
     * <p>All suppressed exceptions from the original exception are also preserved and added to
     * this {@code UncheckedException}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     performDatabaseOperation();
     * } catch (SQLException e) {
     *     throw new UncheckedException("Failed to update user record for ID: " + userId, e);
     * }
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     * @param cause the checked exception to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedException(final String message, final Throwable cause) {
        super(message, getCause(cause));

        final Throwable[] suppressedExceptions = cause.getSuppressed();
        if (N.notEmpty(suppressedExceptions)) {
            for (final Throwable suppressedException : suppressedExceptions) {
                this.addSuppressed(suppressedException);
            }
        }
    }

    /**
     * Validates that the provided throwable is not {@code null} and returns it to be used as the cause.
     *
     * @param cause the exception to validate; must not be {@code null}
     * @return {@code cause}, unchanged
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    private static Throwable getCause(final Throwable cause) {
        N.checkArgNotNull(cause, cs.cause);

        return cause;
    }

}
