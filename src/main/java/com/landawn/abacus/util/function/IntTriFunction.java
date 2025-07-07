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
 * Represents a function that accepts three {@code int}-valued arguments and produces
 * a result. This is the three-arity specialization of {@link java.util.function.Function}.
 *
 * <p>This interface extends {@link Throwables.IntTriFunction} with
 * {@link RuntimeException}, providing compatibility with the Abacus framework's
 * exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(int, int, int)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 * @since 1.8
 */
@FunctionalInterface
public interface IntTriFunction<R> extends Throwables.IntTriFunction<R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given arguments.
     *
     * <p>The function processes three int values and produces a result of type R.
     * Common use cases include:
     * <ul>
     *   <li>Computing values from 3D coordinates (x, y, z)</li>
     *   <li>Creating objects from RGB color components</li>
     *   <li>Performing calculations on three numeric inputs</li>
     *   <li>Aggregating or reducing three int values to a single result</li>
     *   <li>Looking up values based on three int indices</li>
     *   <li>Creating composite keys or identifiers from three int values</li>
     * </ul>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @return the function result of type R
     */
    @Override
    R apply(int a, int b, int c);

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
     * <p>Example usage:
     * <pre>{@code
     * IntTriFunction<Integer> sum = (a, b, c) -> a + b + c;
     * IntTriFunction<String> sumToString = sum.andThen(String::valueOf);
     * String result = sumToString.apply(1, 2, 3); // "6"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied.
     *              Must not be null
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> IntTriFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}