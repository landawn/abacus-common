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
 * Represents a function that accepts a double-valued argument and produces a result.
 * This is the double-consuming primitive specialization for {@link java.util.function.Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #apply(double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.DoubleFunction
 */
@FunctionalInterface
public interface DoubleFunction<R> extends Throwables.DoubleFunction<R, RuntimeException>, java.util.function.DoubleFunction<R> { //NOSONAR
    /**
     * A function that boxes a primitive double value into a {@link Double} object.
     */
    DoubleFunction<Double> BOX = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * @param value the double function argument
     * @return the function result
     */
    @Override
    R apply(double value);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> DoubleFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its double input argument.
     *
     * @return a function that always returns its double input argument as a {@link Double}
     */
    static DoubleFunction<Double> identity() {
        return t -> t;
    }
}