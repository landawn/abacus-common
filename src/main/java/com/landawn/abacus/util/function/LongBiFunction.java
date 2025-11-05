/*
 * Copyright (C) 2016 HaiYang Li
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
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that accepts two {@code long}-valued arguments and produces
 * a result. This is the {@code long}-consuming primitive specialization for
 * {@link java.util.function.BiFunction}.
 *
 * <p>This interface extends {@link Throwables.LongBiFunction} with
 * {@link RuntimeException}, providing compatibility with the abacus-common framework's
 * exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(long, long)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.BiFunction
 * @see java.util.function.LongFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongBiFunction<R> extends Throwables.LongBiFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p>The function processes two long values and produces a result of type R.
     * Common use cases include:
     * <ul>
     *   <li>Computing values from two long inputs (e.g., calculating differences)</li>
     *   <li>Creating objects from two long parameters (e.g., timestamp ranges)</li>
     *   <li>Performing lookups based on two long indices</li>
     *   <li>Aggregating or combining two long values into a single result</li>
     *   <li>Converting pairs of long values to other representations</li>
     *   <li>Implementing binary operations on long values that produce non-long results</li>
     * </ul>
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result of type R
     */
    @Override
    R apply(long t, long u);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation
     * of either function throws an exception, it is relayed to the caller of
     * the composed function.
     *
     * <p>This method enables function composition, allowing you to chain operations
     * where the output of this function becomes the input of the next function.
     * This is useful for building complex transformations from simpler ones.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongBiFunction<Long> difference = (a, b) -> Math.abs(a - b);
     * LongBiFunction<String> differenceAsString = difference.andThen(String::valueOf);
     * String result = differenceAsString.apply(100L, 75L); // "25"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied.
     *              Must not be {@code null}.
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> LongBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }

    /**
     * Converts this {@code LongBiFunction} to a {@code Throwables.LongBiFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongBiFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.LongBiFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.LongBiFunction<R, E> toThrowable() {
        return (Throwables.LongBiFunction<R, E>) this;
    }

}
