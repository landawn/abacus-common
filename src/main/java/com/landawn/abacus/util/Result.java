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

/**
 * 
 * @author Haiyang Li
 *
 * @param <T>
 * @param <E>
 */
public final class Result<T, E extends Throwable> {
    private final T value;
    private final E exception;

    Result(T value, E exception) {
        this.value = value;
        this.exception = exception;
    }

    public static <T, E extends Throwable> Result<T, E> of(final T value, final E exception) {
        return new Result<>(value, exception);
    }

    public boolean isFailure() {
        return exception != null;
    }

    public boolean isSuccess() {
        return exception == null;
    }

    public <E2 extends Exception> void ifFailure(final Try.Consumer<? super E, E2> actionOnFailure) throws E2 {
        ifFailureOrElse(actionOnFailure, Fn.doNothing());
    }

    public <E2 extends Exception, E3 extends Exception> void ifFailureOrElse(final Try.Consumer<? super E, E2> actionOnFailure,
            final Try.Consumer<? super T, E3> actionOnSuccess) throws E2, E3 {
        N.checkArgNotNull(actionOnFailure, "actionOnFailure");
        N.checkArgNotNull(actionOnSuccess, "actionOnSuccess");

        if (exception != null) {
            actionOnFailure.accept(exception);
        } else {
            actionOnSuccess.accept(value);
        }
    }

    public <E2 extends Exception> void ifSuccess(final Try.Consumer<? super T, E2> actionOnSuccess) throws E2 {
        ifSuccessOrElse(actionOnSuccess, Fn.doNothing());
    }

    public <E2 extends Exception, E3 extends Exception> void ifSuccessOrElse(final Try.Consumer<? super T, E2> actionOnSuccess,
            final Try.Consumer<? super E, E3> actionOnFailure) throws E2, E3 {
        N.checkArgNotNull(actionOnSuccess, "actionOnSuccess");
        N.checkArgNotNull(actionOnFailure, "actionOnFailure");

        if (exception == null) {
            actionOnSuccess.accept(value);
        } else {
            actionOnFailure.accept(exception);
        }
    }

    public T orElse(final T defaultValueIfErrorOccurred) {
        if (exception == null) {
            return value;
        } else {
            return defaultValueIfErrorOccurred;
        }
    }

    public T orElseGet(final Supplier<T> valueSupplierIfErrorOccurred) {
        N.checkArgNotNull(valueSupplierIfErrorOccurred, "valueSupplierIfErrorOccurred");

        if (exception == null) {
            return value;
        } else {
            return valueSupplierIfErrorOccurred.get();
        }
    }

    public T orElseThrow() throws E {
        if (exception == null) {
            return value;
        } else {
            throw exception;
        }
    }

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

    public Tuple2<T, E> toTuple() {
        return Tuple.of(value, exception);
    }

    @Override
    public int hashCode() {
        return (exception == null) ? N.hashCode(value) : exception.hashCode();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj instanceof Result && (N.equals(((Result) obj).value, value) && N.equals(((Result) obj).exception, exception)));
    }

    @Override
    public String toString() {
        return "{value=" + N.toString(value) + ", exception=" + N.toString(exception) + "}";
    }
}
