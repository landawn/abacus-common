/*
 * Copyright (c) 2019, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.util.function.Function;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

/**
 * This class encapsulates a result of an operation that can either be a value of type {@code T} or an exception of type {@code E}.
 * The class implements the Immutable interface, indicating that instances of this class are immutable.
 * The class is parameterized by two types: {@code T} which is the type of the result value, and {@code E} which is the type of the exception.
 * The exception type {@code E} is constrained to be a subtype of {@code Throwable}.
 *
 * @param <T> the type of the result value.
 * @param <E> the type of the exception. Must be a subtype of {@code Throwable}.
 * @see com.landawn.abacus.util.u.Optional
 * @see com.landawn.abacus.util.u.Nullable
 * @see com.landawn.abacus.util.Holder
 * @see com.landawn.abacus.util.Pair
 * @see com.landawn.abacus.util.Triple
 * @see com.landawn.abacus.util.Tuple
 */
@com.landawn.abacus.annotation.Immutable
public class Result<T, E extends Throwable> implements Immutable {

    private final T value;

    private final E exception;

    Result(final T value, final E exception) {
        this.value = value;
        this.exception = exception;
    }

    /**
     * Creates a new Result instance with the specified value and exception.
     * Either value or exception can be present, but typically only one should be {@code non-null}.
     * If both are {@code null}, it represents a successful operation with a {@code null} result.
     * If both are {@code non-null}, the Result is considered to be in failure state (exception takes precedence).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Result<String, IOException> result = Result.of("success", null);
     * Result<String, IOException> failure = Result.of(null, new IOException("error"));
     * }</pre>
     *
     * @param <T> the type of the result value
     * @param <E> the type of the exception, must extend Throwable
     * @param value the successful result value, can be null
     * @param exception the exception that occurred during the operation, {@code null} if operation was successful
     * @return a new Result instance containing either the value or the exception
     */
    public static <T, E extends Throwable> Result<T, E> of(final T value, final E exception) {
        return new Result<>(value, exception);
    }

    /**
     * Checks if this Result represents a failed operation.
     * A Result is considered a failure if it contains a {@code non-null} exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (result.isFailure()) {
     *     // handle failure case
     * }
     * }</pre>
     *
     * @return {@code true} if the Result contains an exception (operation failed), {@code false} otherwise
     */
    public boolean isFailure() {
        return exception != null;
    }

    /**
     * Checks if this Result represents a successful operation.
     * A Result is considered successful if it does not contain an exception (exception is null).
     * Note that a successful Result may still have a {@code null} value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (result.isSuccess()) {
     *     processValue(result.orElseThrow());
     * }
     * }</pre>
     *
     * @return {@code true} if the Result does not contain an exception (operation succeeded), {@code false} otherwise
     */
    public boolean isSuccess() {
        return exception == null;
    }

    /**
     * Executes the provided action if this Result represents a failure.
     * The action receives the exception contained in this Result as its parameter.
     * If this Result is successful (no exception), the action is not executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifFailure(ex -> log.error("Operation failed", ex));
     * }</pre>
     *
     * @param <E2> the type of exception that the action might throw
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be null
     * @throws E2 if the actionOnFailure throws an exception of type E2
     */
    public <E2 extends Throwable> void ifFailure(final Throwables.Consumer<? super E, E2> actionOnFailure) throws E2 {
        ifFailureOrElse(actionOnFailure, Fn.emptyConsumer());
    }

    /**
     * Executes one of two actions based on whether this Result represents a failure or success.
     * If the Result contains an exception, actionOnFailure is executed with the exception.
     * If the Result is successful, actionOnSuccess is executed with the value.
     * Exactly one action will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifFailureOrElse(
     *     ex -> log.error("Failed", ex),
     *     val -> log.info("Success: {}", val)
     * );
     * }</pre>
     *
     * @param <E2> the type of exception that actionOnFailure might throw
     * @param <E3> the type of exception that actionOnSuccess might throw
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be null
     * @param actionOnSuccess the action to execute if this Result is successful, must not be null
     * @throws IllegalArgumentException if either actionOnFailure or actionOnSuccess is null
     * @throws E2 if the actionOnFailure is executed and throws an exception
     * @throws E3 if the actionOnSuccess is executed and throws an exception
     */
    public <E2 extends Throwable, E3 extends Throwable> void ifFailureOrElse(final Throwables.Consumer<? super E, E2> actionOnFailure,
            final Throwables.Consumer<? super T, E3> actionOnSuccess) throws IllegalArgumentException, E2, E3 {
        N.checkArgNotNull(actionOnFailure, cs.actionOnFailure);
        N.checkArgNotNull(actionOnSuccess, cs.actionOnSuccess);

        if (exception != null) {
            actionOnFailure.accept(exception);
        } else {
            actionOnSuccess.accept(value);
        }
    }

    /**
     * Executes the provided action if this Result represents a success.
     * The action receives the value contained in this Result as its parameter.
     * If this Result is a failure (contains an exception), the action is not executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifSuccess(val -> System.out.println("Success: " + val));
     * }</pre>
     *
     * @param <E2> the type of exception that the action might throw
     * @param actionOnSuccess the action to execute if this Result is successful, must not be null
     * @throws E2 if the actionOnSuccess throws an exception of type E2
     */
    public <E2 extends Throwable> void ifSuccess(final Throwables.Consumer<? super T, E2> actionOnSuccess) throws E2 {
        ifSuccessOrElse(actionOnSuccess, Fn.emptyConsumer());
    }

    /**
     * Executes one of two actions based on whether this Result represents a success or failure.
     * If the Result is successful, actionOnSuccess is executed with the value.
     * If the Result contains an exception, actionOnFailure is executed with the exception.
     * Exactly one action will be executed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * result.ifSuccessOrElse(
     *     val -> processValue(val),
     *     ex -> handleError(ex)
     * );
     * }</pre>
     *
     * @param <E2> the type of exception that actionOnSuccess might throw
     * @param <E3> the type of exception that actionOnFailure might throw
     * @param actionOnSuccess the action to execute if this Result is successful, must not be null
     * @param actionOnFailure the action to execute if this Result contains an exception, must not be null
     * @throws IllegalArgumentException if either actionOnSuccess or actionOnFailure is null
     * @throws E2 if the actionOnSuccess is executed and throws an exception
     * @throws E3 if the actionOnFailure is executed and throws an exception
     */
    public <E2 extends Throwable, E3 extends Throwable> void ifSuccessOrElse(final Throwables.Consumer<? super T, E2> actionOnSuccess,
            final Throwables.Consumer<? super E, E3> actionOnFailure) throws IllegalArgumentException, E2, E3 {
        N.checkArgNotNull(actionOnSuccess, cs.actionOnSuccess);
        N.checkArgNotNull(actionOnFailure, cs.actionOnFailure);

        if (exception == null) {
            actionOnSuccess.accept(value);
        } else {
            actionOnFailure.accept(exception);
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise returns the specified default value.
     * This method provides a way to handle failure cases with a fallback value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseIfFailure("default");
     * }</pre>
     *
     * @param defaultValueIfErrorOccurred the value to return if this Result contains an exception
     * @return the value contained in this Result if successful, otherwise the defaultValueIfErrorOccurred
     */
    public T orElseIfFailure(final T defaultValueIfErrorOccurred) {
        if (exception == null) {
            return value;
        } else {
            return defaultValueIfErrorOccurred;
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise returns a value supplied by the given supplier.
     * This method provides a way to handle failure cases with a lazily computed fallback value.
     * The supplier is only invoked if this Result contains an exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseGetIfFailure(() -> computeDefault());
     * }</pre>
     *
     * @param otherIfErrorOccurred a supplier that provides the value to return if this Result contains an exception, must not be null
     * @return the value contained in this Result if successful, otherwise the value provided by the supplier
     * @throws IllegalArgumentException if otherIfErrorOccurred is null
     */
    public T orElseGetIfFailure(final Supplier<? extends T> otherIfErrorOccurred) throws IllegalArgumentException {
        N.checkArgNotNull(otherIfErrorOccurred, cs.otherIfErrorOccurred);

        if (exception == null) {
            return value;
        } else {
            return otherIfErrorOccurred.get();
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise throws the contained exception.
     * This method provides a way to unwrap the Result, propagating any exception that occurred.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseThrow(); // throws exception if failed
     * }</pre>
     *
     * @return the value contained in this Result if successful
     * @throws E the exception contained in this Result if it represents a failure
     */
    public T orElseThrow() throws E {
        if (exception == null) {
            return value;
        } else {
            throw exception;
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise throws an exception created by applying
     * the contained exception to the provided exception mapper function.
     * This method allows transforming the original exception into a different exception type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseThrow(ex -> new RuntimeException("Failed", ex));
     * }</pre>
     *
     * @param <E2> the type of exception to be thrown
     * @param exceptionSupplierIfErrorOccurred a function that creates a new exception based on the contained exception, must not be null
     * @return the value contained in this Result if successful
     * @throws IllegalArgumentException if exceptionSupplierIfErrorOccurred is null
     * @throws E2 the exception created by the exceptionSupplierIfErrorOccurred function if this Result contains an exception
     */
    public <E2 extends Throwable> T orElseThrow(final Function<? super E, E2> exceptionSupplierIfErrorOccurred) throws IllegalArgumentException, E2 {
        N.checkArgNotNull(exceptionSupplierIfErrorOccurred, cs.exceptionSupplierIfErrorOccurred);

        if (exception == null) {
            return value;
        } else {
            throw exceptionSupplierIfErrorOccurred.apply(exception);
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise throws an exception supplied by the given supplier.
     * This method provides a way to throw a custom exception when the Result represents a failure.
     * The supplier is only invoked if this Result contains an exception.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String value = result.orElseThrow(() -> new IllegalStateException("Failed"));
     * }</pre>
     *
     * @param <E2> the type of exception to be thrown
     * @param exceptionSupplier a supplier that provides the exception to throw if this Result contains an exception, must not be null
     * @return the value contained in this Result if successful
     * @throws IllegalArgumentException if exceptionSupplier is null
     * @throws E2 the exception provided by the exceptionSupplier if this Result contains an exception
     */
    public <E2 extends Throwable> T orElseThrow(final Supplier<? extends E2> exceptionSupplier) throws IllegalArgumentException, E2 {
        N.checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

        if (exception == null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns the value if this Result is successful, otherwise throws the specified exception.
     * This method provides a way to throw a pre-created exception when the Result represents a failure.
     *
     * <p><b>Note:</b> This method is deprecated because it requires creating the exception object
     * eagerly even if the Result is successful. Use {@link #orElseThrow(Supplier)} instead for
     * better performance.
     *
     * @param <E2> the type of exception to be thrown
     * @param exception the exception to throw if this Result contains an exception
     * @return the value contained in this Result if successful
     * @throws E2 the provided exception if this Result contains an exception
     * @deprecated Use {@link #orElseThrow(Supplier)} instead for better performance (avoids creating exception if not needed)
     */
    @Deprecated
    public <E2 extends Throwable> T orElseThrow(final E2 exception) throws E2 {
        if (this.exception == null) {
            return value;
        } else {
            throw exception;
        }
    }

    /**
     * Returns the exception contained in this Result, or {@code null} if the Result is successful.
     * This method provides direct access to the exception without wrapping it in an Optional.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (result.isFailure()) {
     *     Exception ex = result.getException();
     *     log.error("Error occurred", ex);
     * }
     * }</pre>
     *
     * @return the exception if this Result represents a failure, {@code null} if the Result is successful
     */
    @Beta
    public E getException() {
        return exception;
    }

    /**
     * Returns an Optional containing the exception if this Result represents a failure,
     * or an empty Optional if the Result is successful.
     * This method provides a safe way to access the exception with Optional semantics.
     *
     * <p><b>Note:</b> This method is deprecated in favor of {@link #getException()} which provides
     * direct access to the exception without the Optional wrapper overhead.
     *
     * @return an Optional containing the exception if present, otherwise an empty Optional
     * @deprecated Use {@link #getException()} instead for direct access to the exception
     */
    @Deprecated
    @Beta
    public Optional<E> getExceptionIfPresent() {
        return Optional.ofNullable(exception);
    }

    /**
     * Converts this Result to a Pair containing both the value and the exception.
     * The first element of the Pair is the value (which may be null), and the second element is the exception (which may be null).
     * Typically, only one of these will be {@code non-null}, but both could be {@code null} or {@code non-null} depending on how the Result was created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Pair<String, Exception> pair = result.toPair();
     * String value = pair._1;
     * Exception ex = pair._2;
     * }</pre>
     *
     * @return a Pair where the first element is the value and the second element is the exception
     */
    public Pair<T, E> toPair() {
        return Pair.of(value, exception);
    }

    /**
     * Converts this Result to a Tuple2 containing both the value and the exception.
     * The first element of the Tuple2 is the value (which may be null), and the second element is the exception (which may be null).
     * Typically, only one of these will be {@code non-null}, but both could be {@code null} or {@code non-null} depending on how the Result was created.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple2<String, Exception> tuple = result.toTuple();
     * String value = tuple._1;
     * Exception ex = tuple._2;
     * }</pre>
     *
     * @return a Tuple2 where the first element is the value and the second element is the exception
     */
    public Tuple2<T, E> toTuple() {
        return Tuple.of(value, exception);
    }

    /**
     * Returns a hash code value for this Result.
     * The hash code is computed based on the exception if present, otherwise based on the value.
     * This ensures that Results with the same exception or value will have the same hash code.
     * This method is consistent with the {@link #equals(Object)} implementation.
     *
     * @return the hash code of the exception if present, otherwise the hash code of the value
     */
    @Override
    public int hashCode() {
        return (exception == null) ? N.hashCode(value) : exception.hashCode();
    }

    /**
     * Compares this Result with another object for equality.
     * Two Results are considered equal if they are both successful with equal values,
     * or both failures with equal exceptions. For equality, both the value and exception
     * must match between the two Result instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Result<String, Exception> r1 = Result.of("value", null);
     * Result<String, Exception> r2 = Result.of("value", null);
     * boolean isEqual = r1.equals(r2); // true
     * }</pre>
     *
     * @param obj the object to compare with this Result
     * @return {@code true} if the specified object is a Result with equal value and exception, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        //noinspection rawtypes
        if (obj instanceof Result other) { // NOSONAR
            return N.equals(other.value, value) && N.equals(other.exception, exception);
        }

        return false;
    }

    /**
     * Returns a string representation of this Result.
     * The string representation includes both the value and the exception in the format:
     * {@code {value=<value>, exception=<exception>}}.
     * This is useful for debugging and logging purposes.
     * 
     * <p><b>Example output:</b></p>
     * <pre>{@code
     * {value=success, exception=null}
     * {value=null, exception=java.io.IOException: Error}
     * }</pre>
     *
     * @return a string representation of this Result showing both value and exception
     */
    @Override
    public String toString() {
        return "{value=" + N.toString(value) + ", exception=" + N.toString(exception) + "}";
    }

    /**
     * A specialized Result class that specifically handles RuntimeException as the exception type.
     * This class provides a more convenient way to work with Results that may contain RuntimeExceptions,
     * which don't need to be declared in method signatures.
     *
     * @param <T> the type of the result value
     */
    @Beta
    public static class RR<T> extends Result<T, RuntimeException> {
        RR(final T value, final RuntimeException exception) {
            super(value, exception);
        }

        /**
         * Creates a new RR (Result with RuntimeException) instance with the specified value and exception.
         * Either value or exception can be present, but typically only one should be {@code non-null}.
         * This factory method provides a convenient way to create Results for operations that may throw RuntimeExceptions,
         * which don't need to be declared in method signatures.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Result.RR<String> result = Result.RR.of("success", null);
         * Result.RR<String> failure = Result.RR.of(null, new RuntimeException("error"));
         * }</pre>
         *
         * @param <T> the type of the result value
         * @param value the successful result value, can be null
         * @param exception the RuntimeException that occurred during the operation, {@code null} if operation was successful
         * @return a new RR instance containing either the value or the RuntimeException
         */
        public static <T> Result.RR<T> of(final T value, final RuntimeException exception) {
            return new RR<>(value, exception);
        }
    }
}
