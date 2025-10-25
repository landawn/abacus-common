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
 * Represents a function that accepts three float-valued arguments and produces a result.
 * This is a functional interface whose functional method is {@link #apply(float, float, float)}.
 * 
 * <p>This is a primitive type specialization of function for three {@code float} arguments.
 * This interface is similar to {@link java.util.function.BiFunction} but accepts three arguments
 * instead of two.</p>
 * 
 * @param <R> the type of the result of the function
 * 
 * @since 1.0
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface FloatTriFunction<R> extends Throwables.FloatTriFunction<R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given float arguments.
     * 
     * <p>The function implementation should define how the three float arguments
     * are processed to produce the result.</p>
     *
     * @param a the first float function argument
     * @param b the second float function argument
     * @param c the third float function argument
     * @return the function result
     */
    @Override
    R apply(float a, float b, float c);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation of
     * either function throws an exception, it is relayed to the caller of the
     * composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied. Must not be null.
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> FloatTriFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}