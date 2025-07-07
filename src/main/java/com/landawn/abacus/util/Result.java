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
 * @param <T> The type of the result value.
 * @param <E> The type of the exception. Must be a subtype of {@code Throwable}.
 *
 * @param <T>
 * @param <E>
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
     * Either value or exception can be present, but typically only one should be non-null.
     * If both are null, it represents a successful operation with a null result.
     * If both are non-null, the Result is considered to be in failure state (exception takes precedence).
     *
     * @param <T> The type of the result value
     * @param <E> The type of the exception, must extend Throwable
     * @param value The successful result value, can be null
     * @param exception The exception that occurred during the operation, null if operation was successful
     * @return A new Result instance containing either the value or the exception
     */
    public static <T, E extends Throwable> Result<T, E> of(final T value, final E exception) {
        return new Result<>(value, exception);
    }

    /**
     * Checks if this Result represents a failed operation.
     * A Result is considered a failure if it contains a non-null exception.
     *
     * @return {@code true} if the Result contains an exception (operation failed), {@code false} otherwise
     */
    public boolean isFailure() {
        return exception != null;
    }

    /**
     * Checks if this Result represents a successful operation.
     * A Result is considered successful if it does not contain an exception (exception is null).
     * Note that a successful Result may still have a null value.
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
     * @param <E2> The type of exception that the action might throw
     * @param actionOnFailure The action to execute if this Result contains an exception, must not be null
     * @throws E2 If the actionOnFailure throws an exception of type E2
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
     * @param <E2> The type of exception that actionOnFailure might throw
     * @param <E3> The type of exception that actionOnSuccess might throw
     * @param actionOnFailure The action to execute if this Result contains an exception, must not be null
     * @param actionOnSuccess The action to execute if this Result is successful, must not be null
     * @throws IllegalArgumentException If either actionOnFailure or actionOnSuccess is null
     * @throws E2 If the actionOnFailure is executed and throws an exception
     * @throws E3 If the actionOnSuccess is executed and throws an exception
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
     * @param <E2> The type of exception that the action might throw
     * @param actionOnSuccess The action to execute if this Result is successful, must not be null
     * @throws E2 If the actionOnSuccess throws an exception of type E2
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
     * @param <E2> The type of exception that actionOnSuccess might throw
     * @param <E3> The type of exception that actionOnFailure might throw
     * @param actionOnSuccess The action to execute if this Result is successful, must not be null
     * @param actionOnFailure The action to execute if this Result contains an exception, must not be null
     * @throws IllegalArgumentException If either actionOnSuccess or actionOnFailure is null
     * @throws E2 If the actionOnSuccess is executed and throws an exception
     * @throws E3 If the actionOnFailure is executed and throws an exception
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
     * @param defaultValueIfErrorOccurred The value to return if this Result contains an exception
     * @return The value contained in this Result if successful, otherwise the defaultValueIfErrorOccurred
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
     * @param otherIfErrorOccurred A supplier that provides the value to return if this Result contains an exception, must not be null
     * @return The value contained in this Result if successful, otherwise the value provided by the supplier
     * @throws IllegalArgumentException If otherIfErrorOccurred is null
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
     * @return The value contained in this Result if successful
     * @throws E The exception contained in this Result if it represents a failure
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
     * @param <E2> The type of exception to be thrown
     * @param exceptionSupplierIfErrorOccurred A function that creates a new exception based on the contained exception, must not be null
     * @return The value contained in this Result if successful
     * @throws IllegalArgumentException If exceptionSupplierIfErrorOccurred is null
     * @throws E2 The exception created by the exceptionSupplierIfErrorOccurred function if this Result contains an exception
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
     * @param <E2> The type of exception to be thrown
     * @param exceptionSupplier A supplier that provides the exception to throw if this Result contains an exception
     * @return The value contained in this Result if successful
     * @throws E2 The exception provided by the exceptionSupplier if this Result contains an exception
     */
    public <E2 extends Throwable> T orElseThrow(final Supplier<? extends E2> exceptionSupplier) throws E2 {
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
     * @param <E2> The type of exception to be thrown
     * @param exception The exception to throw if this Result contains an exception
     * @return The value contained in this Result if successful
     * @throws E2 The provided exception if this Result contains an exception
     * @deprecated Use {@link #orElseThrow(Supplier)} instead for better performance (avoids creating exception if not needed)
     */
    @Deprecated
    public <E2 extends Throwable> T orElseThrow(final E2 exception) throws E2 {
        if (exception == null) {
            return value;
        } else {
            throw exception;
        }
    }

    /**
     * Returns the exception contained in this Result, or null if the Result is successful.
     * This method provides direct access to the exception without wrapping it in an Optional.
     *
     * @return The exception if this Result represents a failure, null if the Result is successful
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
     * @return An Optional containing the exception if present, otherwise an empty Optional
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
     * Typically, only one of these will be non-null, but both could be null or non-null depending on how the Result was created.
     *
     * @return A Pair where the first element is the value and the second element is the exception
     */
    public Pair<T, E> toPair() {
        return Pair.of(value, exception);
    }

    /**
     * Converts this Result to a Tuple2 containing both the value and the exception.
     * The first element of the Tuple2 is the value (which may be null), and the second element is the exception (which may be null).
     * Typically, only one of these will be non-null, but both could be null or non-null depending on how the Result was created.
     *
     * @return A Tuple2 where the first element is the value and the second element is the exception
     */
    public Tuple2<T, E> toTuple() {
        return Tuple.of(value, exception);
    }

    /**
     * Returns a hash code value for this Result.
     * The hash code is computed based on the exception if present, otherwise based on the value.
     * This ensures that Results with the same exception or value will have the same hash code.
     *
     * @return The hash code of the exception if present, otherwise the hash code of the value
     */
    @Override
    public int hashCode() {
        return (exception == null) ? N.hashCode(value) : exception.hashCode();
    }

    /**
     * Compares this Result with another object for equality.
     * Two Results are considered equal if they are both successful with equal values,
     * or both failures with equal exceptions.
     *
     * @param obj The object to compare with this Result
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
     * {@code {value=<value>, exception=<exception>}}
     *
     * @return A string representation of this Result showing both value and exception
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
     * @param <T> The type of the result value
     */
    @Beta
    public static class RR<T> extends Result<T, RuntimeException> {
        RR(final T value, final RuntimeException exception) {
            super(value, exception);
        }

        /**
         * Creates a new R (Result with RuntimeException) instance with the specified value and exception.
         * Either value or exception can be present, but typically only one should be non-null.
         * This factory method provides a convenient way to create Results for operations that may throw RuntimeExceptions.
         *
         * @param <T> The type of the result value
         * @param value The successful result value, can be null
         * @param exception The RuntimeException that occurred during the operation, null if operation was successful
         * @return A new R instance containing either the value or the RuntimeException
         */
        public static <T> Result.RR<T> of(final T value, final RuntimeException exception) {
            return new RR<>(value, exception);
        }
    }
}