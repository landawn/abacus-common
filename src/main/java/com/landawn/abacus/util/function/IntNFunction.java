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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that accepts a variable number of {@code int}-valued arguments and produces a result.
 * This is the n-arity specialization of {@link IntFunction}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(int...)}.
 *
 * @param <R> the type of the result of the function
 * @see IntFunction
 * @see IntBiFunction
 */
@FunctionalInterface
public interface IntNFunction<R> extends Throwables.IntNFunction<R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given arguments.
     *
     * <p>The function processes a variable number of int values and produces
     * a result of type R. The varargs parameter allows for flexible argument
     * counts, from zero to many values.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Aggregating multiple int values into a single result (sum, average, etc.)</li>
     *   <li>Creating objects from arrays of int values</li>
     *   <li>Computing statistics from groups of int values</li>
     *   <li>Transforming arrays of int values into other representations</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntNFunction<Integer> sum = args -> {
     *     int total = 0;
     *     for (int value : args) total += value;
     *     return total;
     * };
     * Integer result = sum.apply(1, 2, 3, 4, 5); // Returns 15
     * }</pre>
     *
     * @param args the function arguments as a variable-length array of {@code int} values.
     *             Can be empty, contain a single value, or multiple values
     * @return the function result
     */
    @Override
    @MayReturnNull
    R apply(int... args);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntNFunction<Integer> sum = args -> Arrays.stream(args).sum();
     * IntNFunction<String> sumAsString = sum.andThen(String::valueOf);
     * String result = sumAsString.apply(1, 2, 3); // "6"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be null
     * @return a composed function that first applies this function and then applies the {@code after}
     *         function
     */
    @Override
    default <V> IntNFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return args -> after.apply(apply(args));
    }
}
