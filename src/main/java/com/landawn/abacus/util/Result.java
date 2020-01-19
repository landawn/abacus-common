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

import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Supplier;

// TODO: Auto-generated Javadoc
/**
 * The Class Result.
 *
 * @author Haiyang Li
 * @param <T>
 * @param <E>
 */
public final class Result<T, E extends Throwable> {

    /** The value. */
    private final T value;

    /** The exception. */
    private final E exception;

    /**
     * Instantiates a new result.
     *
     * @param value
     * @param exception
     */
    Result(T value, E exception) {
        this.value = value;
        this.exception = exception;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param value
     * @param exception
     * @return
     */
    public static <T, E extends Throwable> Result<T, E> of(final T value, final E exception) {
        return new Result<>(value, exception);
    }

    /**
     * Checks if is failure.
     *
     * @return true, if is failure
     */
    public boolean isFailure() {
        return exception != null;
    }

    /**
     * Checks if is success.
     *
     * @return true, if is success
     */
    public boolean isSuccess() {
        return exception == null;
    }

    /**
     *
     * @param <E2>
     * @param actionOnFailure
     * @throws E2 the e2
     */
    public <E2 extends Exception> void ifFailure(final Throwables.Consumer<? super E, E2> actionOnFailure) throws E2 {
        ifFailureOrElse(actionOnFailure, Fn.doNothing());
    }

    /**
     * If failure or else.
     *
     * @param <E2>
     * @param <E3>
     * @param actionOnFailure
     * @param actionOnSuccess
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public <E2 extends Exception, E3 extends Exception> void ifFailureOrElse(final Throwables.Consumer<? super E, E2> actionOnFailure,
            final Throwables.Consumer<? super T, E3> actionOnSuccess) throws E2, E3 {
        N.checkArgNotNull(actionOnFailure, "actionOnFailure");
        N.checkArgNotNull(actionOnSuccess, "actionOnSuccess");

        if (exception != null) {
            actionOnFailure.accept(exception);
        } else {
            actionOnSuccess.accept(value);
        }
    }

    /**
     *
     * @param <E2>
     * @param actionOnSuccess
     * @throws E2 the e2
     */
    public <E2 extends Exception> void ifSuccess(final Throwables.Consumer<? super T, E2> actionOnSuccess) throws E2 {
        ifSuccessOrElse(actionOnSuccess, Fn.doNothing());
    }

    /**
     * If success or else.
     *
     * @param <E2>
     * @param <E3>
     * @param actionOnSuccess
     * @param actionOnFailure
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public <E2 extends Exception, E3 extends Exception> void ifSuccessOrElse(final Throwables.Consumer<? super T, E2> actionOnSuccess,
            final Throwables.Consumer<? super E, E3> actionOnFailure) throws E2, E3 {
        N.checkArgNotNull(actionOnSuccess, "actionOnSuccess");
        N.checkArgNotNull(actionOnFailure, "actionOnFailure");

        if (exception == null) {
            actionOnSuccess.accept(value);
        } else {
            actionOnFailure.accept(exception);
        }
    }

    /**
     *
     * @param defaultValueIfErrorOccurred
     * @return
     */
    public T orElse(final T defaultValueIfErrorOccurred) {
        if (exception == null) {
            return value;
        } else {
            return defaultValueIfErrorOccurred;
        }
    }

    /**
     * Or else get.
     *
     * @param valueSupplierIfErrorOccurred
     * @return
     */
    public T orElseGet(final Supplier<T> valueSupplierIfErrorOccurred) {
        N.checkArgNotNull(valueSupplierIfErrorOccurred, "valueSupplierIfErrorOccurred");

        if (exception == null) {
            return value;
        } else {
            return valueSupplierIfErrorOccurred.get();
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
     * @throws E2 the e2
     */
    public <E2 extends Throwable> T orElseThrow(final Function<? super E, E2> exceptionSupplierIfErrorOccurred) throws E2 {
        N.checkArgNotNull(exceptionSupplierIfErrorOccurred, "exceptionSupplierIfErrorOccurred");

        if (exception == null) {
            return value;
        } else {
            throw exceptionSupplierIfErrorOccurred.apply(exception);
        }
    }

    /**
     * Returns the {@code Exception} if occurred, otherwise {@code null} is returned.
     *
     * @return
     */
    public E getExceptionIfPresent() {
        return exception;
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

    /**
     *
     * @return
     */
    public Pair<T, E> toPair() {
        return Pair.of(value, exception);
    }

    /**
     *
     * @return
     */
    public Tuple2<T, E> toTuple() {
        return Tuple.of(value, exception);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (exception == null) ? N.hashCode(value) : exception.hashCode();
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Result && (N.equals(((Result) obj).value, value) && N.equals(((Result) obj).exception, exception)));
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "{value=" + N.toString(value) + ", exception=" + N.toString(exception) + "}";
    }
}
