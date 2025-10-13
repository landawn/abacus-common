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
 * <p>Example usage:</p>
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
 * @since 1.0
 * @see RuntimeException
 * @see UncheckedIOException
 * @see UncheckedSQLException
 */
public class UncheckedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1973552812345999717L;

    // private final Throwable checkedException;

    //    /**
    //     * Constructor for UncheckedIOException.
    //     */
    //    UncheckedException() {
    //    }
    //
    //    /**
    //     * Constructor for UncheckedIOException.
    //     *
    //     * @param message
    //     */
    //    UncheckedException(final String message) {
    //        super(message);
    //    }

    /**
     * Constructs a new {@code UncheckedException} by wrapping the specified checked exception.
     * This constructor preserves all information from the original exception including its
     * stack trace and any suppressed exceptions.
     * 
     * <p>The wrapped exception becomes the cause of this UncheckedException, allowing the
     * original exception information to be preserved and accessed via {@link #getCause()}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * try {
     *     Class.forName("com.example.SomeClass");
     * } catch (ClassNotFoundException e) {
     *     throw new UncheckedException(e);
     * }
     * }</pre>
     *
     * @param checkedException the checked exception to wrap. Must not be null.
     * @throws IllegalArgumentException if checkedException is null
     */
    public UncheckedException(final Throwable checkedException) {
        super(getCause(checkedException));
        // this.checkedException = checkedException;

        final Throwable[] suspend = checkedException.getSuppressed();
        if (N.notEmpty(suspend)) {
            for (Throwable e : suspend) {
                this.addSuppressed(e);
            }
        }
    }

    /**
     * Constructs a new {@code UncheckedException} with a custom message and the specified checked exception.
     * This constructor allows you to provide additional context while preserving all information
     * from the original exception.
     * 
     * <p>The custom message helps provide context about where or why the exception was wrapped,
     * while the original exception's message and stack trace remain accessible through the cause.</p>
     * 
     * <p>All suppressed exceptions from the original exception are also preserved and added to
     * this UncheckedException.</p>
     * 
     * <p>Example:</p>
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
     * @param checkedException the checked exception to wrap. Must not be null.
     * @throws IllegalArgumentException if checkedException is null
     */
    public UncheckedException(final String message, final Throwable checkedException) {
        super(message, getCause(checkedException));
        // this.checkedException = checkedException;

        final Throwable[] suspend = checkedException.getSuppressed();
        if (N.notEmpty(suspend)) {
            for (Throwable e : suspend) {
                this.addSuppressed(e);
            }
        }
    }

    /**
     * Validates and returns the cause for this exception.
     * This method ensures that the provided checked exception is not null before using it as the cause.
     * 
     * @param checkedException the exception to validate and use as the cause
     * @return the validated exception to be used as the cause
     * @throws IllegalArgumentException if checkedException is null
     */
    private static Throwable getCause(final Throwable checkedException) {
        N.checkArgNotNull(checkedException, cs.checkedException);

        // Refer to ExceptionUtil.tryToGetOriginalCheckedException(Throwable e). It should/must be the original checked exception.
        // return checkedException.getCause() == null ? checkedException : checkedException.getCause();

        return checkedException;
    }

}
