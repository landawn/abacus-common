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
 * Represents a function that accepts two char-valued arguments and produces a result.
 * This is the two-arity specialization of {@link CharFunction}.
 * 
 * <p>This is a functional interface whose functional method is {@link #apply(char, char)}.
 * 
 * @param <R> the type of the result of the function
 * 
 * @see java.util.function.BiFunction
 * @see CharFunction
 */
@FunctionalInterface
public interface CharBiFunction<R> extends Throwables.CharBiFunction<R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given char arguments.
     * This method takes two char values as input and produces a result of type R.
     *
     * @param t the first char function argument
     * @param u the second char function argument
     * @return the function result of type R
     */
    @Override
    R apply(char t, char u);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be null.
     * @return a composed function that first applies this function and then applies the
     *         {@code after} function
     */
    default <V> CharBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }
}