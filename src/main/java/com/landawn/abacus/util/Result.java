/*
 * Copyright (c) 2019, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
     * Creates a new instance of the Result class with the provided value and exception.
     *
     * @param <T> The type of the result value.
     * @param <E> The type of the exception. Must be a subtype of {@code Throwable}.
     * @param value The value of the result.
     * @param exception The exception that occurred during the operation.
     * @return A new instance of the Result class.
     */
    public static <T, E extends Throwable> Result<T, E> of(final T value, final E exception) {
        return new Result<>(value, exception);
    }

    /**
     * Checks if the operation resulted in a failure.
     *
     * @return {@code true} if the operation resulted in a failure (an exception was thrown), {@code false} otherwise.
     */
    public boolean isFailure() {
        return exception != null;
    }

    /**
     * Checks if the operation resulted in a success.
     *
     * @return {@code true} if the operation resulted in a success (no exception was thrown), {@code false} otherwise.
     */
    public boolean isSuccess() {
        return exception == null;
    }

    /**
     * Executes the provided action if the operation resulted in a failure (an exception was thrown).
     *
     * @param <E2> The type of the exception that the action can throw. Must be a subtype of {@code Throwable}.
     * @param actionOnFailure The action to be executed if the operation resulted in a failure.
     * @throws E2 if the action throws an exception.
     */
    public <E2 extends Throwable> void ifFailure(final Throwables.Consumer<? super E, E2> actionOnFailure) throws E2 {
        ifFailureOrElse(actionOnFailure, Fn.doNothing());
    }

    /**
     * Executes the provided action if the operation resulted in a failure (an exception was thrown), otherwise executes the action on success.
     *
     * @param <E2> The type of the exception that the actionOnFailure can throw. Must be a subtype of {@code Throwable}.
     * @param <E3> The type of the exception that the actionOnSuccess can throw. Must be a subtype of {@code Throwable}.
     * @param actionOnFailure The action to be executed if the operation resulted in a failure.
     * @param actionOnSuccess The action to be executed if the operation resulted in a success.
     * @throws IllegalArgumentException if either actionOnFailure or actionOnSuccess is {@code null}.
     * @throws E2 if the actionOnFailure throws an exception.
     * @throws E3 if the actionOnSuccess throws an exception.
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
     * Executes the provided action if the operation resulted in a success (no exception was thrown).
     *
     * @param <E2> The type of the exception that the action can throw. Must be a subtype of {@code Throwable}.
     * @param actionOnSuccess The action to be executed if the operation resulted in a success.
     * @throws E2 if the action throws an exception.
     */
    public <E2 extends Throwable> void ifSuccess(final Throwables.Consumer<? super T, E2> actionOnSuccess) throws E2 {
        ifSuccessOrElse(actionOnSuccess, Fn.doNothing());
    }

    /**
     * Executes the provided action if the operation resulted in a success (no exception was thrown), otherwise executes the action on failure.
     *
     * @param <E2> The type of the exception that the actionOnSuccess can throw. Must be a subtype of {@code Throwable}.
     * @param <E3> The type of the exception that the actionOnFailure can throw. Must be a subtype of {@code Throwable}.
     * @param actionOnSuccess The action to be executed if the operation resulted in a success.
     * @param actionOnFailure The action to be executed if the operation resulted in a failure.
     * @throws IllegalArgumentException if either actionOnSuccess or actionOnFailure is {@code null}.
     * @throws E2 if the actionOnSuccess throws an exception.
     * @throws E3 if the actionOnFailure throws an exception.
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

    //    /**
    //     *
    //     * @param defaultValueIfErrorOccurred
    //     * @return
    //     * @deprecated replaced by {@link #orElseIfFailure(Object)}
    //     */
    //    @Deprecated
    //    public T orElse(final T defaultValueIfErrorOccurred) {
    //        return orElseIfFailure(defaultValueIfErrorOccurred);
    //    }
    //
    //    /**
    //     *
    //     * @param otherIfErrorOccurred
    //     * @return
    //     * @deprecated replaced by {@link #orElseGetIfFailure(Supplier)}
    //     */
    //    @Deprecated
    //    public T orElseGet(final Supplier<? extends T> otherIfErrorOccurred) {
    //        return orElseGetIfFailure(otherIfErrorOccurred);
    //    }

    /**
     *
     * @param defaultValueIfErrorOccurred
     * @return
     */
    public T orElseIfFailure(final T defaultValueIfErrorOccurred) {
        if (exception == null) {
            return value;
        } else {
            return defaultValueIfErrorOccurred;
        }
    }

    /**
     *
     * @param otherIfErrorOccurred
     * @return
     * @throws IllegalArgumentException
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
     * Or else throw.
     *
     * @return
     * @throws E the e
     */
    public T orElseThrow() throws E {
        if (exception == null) {
            return value;
        } else {
            throw exception;
        }
    }

    /**
     * Or else throw.
     *
     * @param <E2>
     * @param exceptionSupplierIfErrorOccurred
     * @return
     * @throws IllegalArgumentException
     * @throws E2 the e2
     */
    public <E2 extends Throwable> T orElseThrow(final Function<? super E, E2> exceptionSupplierIfErrorOccurred) throws IllegalArgumentException, E2 {
        N.checkArgNotNull(exceptionSupplierIfErrorOccurred, cs.exceptionSupplierIfErrorOccurred);

        if (exception == null) {
            return value;
        } else {
            throw exceptionSupplierIfErrorOccurred.apply(exception);
        }
    }

    //    /**
    //     *
    //     * @param <E2>
    //     * @param exception
    //     * @return
    //     * @throws E2
    //     * @deprecated replaced by {@link #orElseThrow(Supplier)}
    //     */
    //    @Deprecated
    //    public <E2 extends Throwable> T orElseThrow(final E2 exception) throws E2 {
    //        if (exception == null) {
    //            return value;
    //        } else {
    //            throw exception;
    //        }
    //    }

    /**
     *
     * @param <E2>
     * @param exceptionSupplier
     * @return
     * @throws E2
     */
    public <E2 extends Throwable> T orElseThrow(final Supplier<? extends E2> exceptionSupplier) throws E2 {
        if (exception == null) {
            return value;
        } else {
            throw exceptionSupplier.get();
        }
    }

    /**
     * Returns the {@code Exception} if occurred, otherwise {@code null} is returned.
     *
     * @return
     */
    @Beta
    public E getException() {
        return exception;
    }

    /**
     * Returns the {@code Exception} if occurred, otherwise an empty {@code Optional} is returned.
     *
     * @return
     * @deprecated replaced by {@code getException}
     */
    @Deprecated
    @Beta
    public Optional<E> getExceptionIfPresent() {
        return Optional.ofNullable(exception);
    }

    //    /**
    //     *
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    public Nullable<T> toNullable() {
    //        return exception == null ? Nullable.of(value) : Nullable.<T> empty();
    //    }

    public Pair<T, E> toPair() {
        return Pair.of(value, exception);
    }

    public Tuple2<T, E> toTuple() {
        return Tuple.of(value, exception);
    }

    @Override
    public int hashCode() {
        return (exception == null) ? N.hashCode(value) : exception.hashCode();
    }

    /**
     *
     * @param obj
     * @return
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final Result other) { // NOSONAR
            return N.equals(other.value, value) && N.equals(other.exception, exception);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{value=" + N.toString(value) + ", exception=" + N.toString(exception) + "}";
    }

    @Beta
    public static class R<T> extends Result<T, RuntimeException> {
        R(final T value, final RuntimeException exception) {
            super(value, exception);
        }

        /**
         *
         * @param <T>
         * @param value
         * @param exception
         * @return
         */
        public static <T> Result.R<T> of(final T value, final RuntimeException exception) {
            return new R<>(value, exception);
        }
    }

    //    @Beta
    //    public static class X<T> extends Result<T, Exception> {
    //        X(T value, Exception exception) {
    //            super(value, exception);
    //        }
    //
    //        public static <T> Result.X<T> of(final T value, final Exception exception) {
    //            return new X<>(value, exception);
    //        }
    //    }
}
