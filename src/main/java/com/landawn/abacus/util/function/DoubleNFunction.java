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
 * Represents a function that accepts a variable number of double-valued arguments and produces a result.
 * This is the N-arity specialization of {@link DoubleFunction}.
 * 
 * <p>This is a functional interface whose functional method is {@link #apply(double...)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see DoubleFunction
 * @see DoubleBiFunction
 */
@FunctionalInterface
public interface DoubleNFunction<R> extends Throwables.DoubleNFunction<R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given arguments.
     *
     * @param args the double input arguments as a varargs array
     * @return the function result
     */
    @Override
    R apply(double... args);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    @Override
    default <V> DoubleNFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return args -> after.apply(apply(args));
    }
}