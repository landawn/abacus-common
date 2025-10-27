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
 * Represents a function that accepts two {@code int}-valued arguments and produces a result.
 * This is the two-arity specialization of {@link java.util.function.Function} for {@code int} arguments.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(int, int)}.
 *
 * @param <R> the type of the result of the function
 * @see java.util.function.Function
 * @see IntFunction
 */
@FunctionalInterface
public interface IntBiFunction<R> extends Throwables.IntBiFunction<R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given arguments.
     *
     * @param t the first {@code int} argument
     * @param u the second {@code int} argument
     * @return the function result
     */
    @Override
    @MayReturnNull
    R apply(int t, int u);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after}
     *         function
     */
    default <V> IntBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }
}
