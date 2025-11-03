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
 * A runtime exception that wraps checked exceptions, allowing them to be thrown without being declared.
 * This is the base class for all unchecked exception wrappers in the framework.
 * 
 * <p>This exception is designed to preserve the original checked exception's information including:</p>
 * <ul>
 *   <li>The complete stack trace of the original exception</li>
 *   <li>All suppressed exceptions from the original exception</li>
 *   <li>The cause chain of the original exception</li>
 * </ul>
 * 
 * <p>This class serves as the parent for more specific unchecked wrappers like:</p>
 * <ul>
 *   <li>{@link UncheckedIOException} for IOException</li>
 *   <li>{@link UncheckedSQLException} for SQLException</li>
 *   <li>{@link UncheckedInterruptedException} for InterruptedException</li>
 *   <li>And other checked exception wrappers</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Wrapping a generic checked exception
 * try {
 *     someMethodThatThrowsCheckedException();
 * } catch (SomeCheckedException e) {
 *     throw new UncheckedException(e);
 * }
 * 
 * // With custom message
 * catch (CheckedException e) {
 *     throw new UncheckedException("Operation failed during processing", e);
 * }
 * }</pre>
 * 
 * @see RuntimeException
 * @see UncheckedIOException
 * @see UncheckedSQLException
 */
public class UncheckedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1973552812345999717L;

    // private final Throwable checkedException;

    /**
     * Constructs a new {@code UncheckedException} by wrapping the specified checked exception.
     * This constructor preserves all information from the original exception including its
     * stack trace and any suppressed exceptions.
     *
     * <p>The wrapped exception becomes the cause of this UncheckedException, allowing the
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
     * @throws IllegalArgumentException if cause is null
     */
    public UncheckedException(final Throwable cause) {
        super(getCause(cause));
        // this.checkedException = cause;

        final Throwable[] suspend = cause.getSuppressed();
        if (N.notEmpty(suspend)) {
            for (Throwable e : suspend) {
                this.addSuppressed(e);
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
     * this UncheckedException.</p>
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
     * @param message the detail message providing context for why the exception was wrapped.
     *                The detail message is saved for later retrieval by the {@link #getMessage()} method.
     * @param cause the checked exception to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if cause is null
     */
    public UncheckedException(final String message, final Throwable cause) {
        super(message, getCause(cause));
        // this.checkedException = cause;

        final Throwable[] suspend = cause.getSuppressed();
        if (N.notEmpty(suspend)) {
            for (Throwable e : suspend) {
                this.addSuppressed(e);
            }
        }
    }

    /**
     * Validates and returns the cause for this exception.
     * This method ensures that the provided checked exception is not {@code null} before using it as the cause.
     *
     * @param cause the exception to validate and use as the cause
     * @return the validated exception to be used as the cause
     * @throws IllegalArgumentException if cause is null
     */
    private static Throwable getCause(final Throwable cause) {
        N.checkArgNotNull(cause, cs.cause);

        // Refer to ExceptionUtil.tryToGetOriginalCheckedException(Throwable e). It should/must be the original checked exception.
        // return cause.getCause() == null ? cause : cause.getCause();

        return cause;
    }

}
