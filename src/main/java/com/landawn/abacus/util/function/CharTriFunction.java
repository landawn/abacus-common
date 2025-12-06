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
 * Represents a function that accepts three char-valued arguments and produces a result.
 * This is the three-arity specialization of {@link CharFunction}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(char, char, char)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 * @see CharFunction
 * @see CharBiFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharTriFunction<R> extends Throwables.CharTriFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given char arguments.
     * This method takes three char values as input and produces a result of type R.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTriFunction<String> concat = (c1, c2, c3) -> "" + c1 + c2 + c3;
     * String result = concat.apply('a', 'b', 'c');   // Returns "abc"
     *
     * CharTriFunction<Integer> sumValues = (c1, c2, c3) -> (int)c1 + c2 + c3;
     * Integer total = sumValues.apply('a', 'b', 'c');   // Returns 294 (97+98+99)
     * }</pre>
     *
     * @param a the first char function argument
     * @param b the second char function argument
     * @param c the third char function argument
     * @return the function result of type R
     */
    @Override
    R apply(char a, char b, char c);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTriFunction<String> concat = (c1, c2, c3) -> "" + c1 + c2 + c3;
     * Function<String, Integer> length = String::length;
     * CharTriFunction<Integer> concatAndGetLength = concat.andThen(length);
     * Integer result = concatAndGetLength.apply('a', 'b', 'c');   // Returns 3
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the
     *         {@code after} function
     */
    default <V> CharTriFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}
