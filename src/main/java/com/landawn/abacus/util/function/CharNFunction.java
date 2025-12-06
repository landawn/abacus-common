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
 * Represents a function that accepts a variable number of char-valued arguments and produces a result.
 * This is a functional interface designed to process char arrays of any length and return a value.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(char...)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see CharFunction
 * @see java.util.function.Function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharNFunction<R> extends Throwables.CharNFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given char array arguments.
     * The array can be of any length, including zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharNFunction<String> toString = chars -> new String(chars);
     * String result = toString.apply('h', 'e', 'l', 'l', 'o');   // Returns "hello"
     *
     * CharNFunction<Integer> sumValues = chars -> {
     *     int sum = 0;
     *     for (char c : chars) {
     *         sum += c;
     *     }
     *     return sum;
     * };
     * Integer total = sumValues.apply('a', 'b', 'c');   // Returns 294 (97+98+99)
     * }</pre>
     *
     * @param args the char array input arguments. Can be empty but not {@code null}.
     * @return the function result of type R
     */
    @Override
    R apply(char... args);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharNFunction<String> toString = chars -> new String(chars);
     * Function<String, Integer> length = String::length;
     * CharNFunction<Integer> toLength = toString.andThen(length);
     * Integer result = toLength.apply('a', 'b', 'c');   // Returns 3
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the
     *         {@code after} function
     */
    @Override
    default <V> CharNFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return args -> after.apply(apply(args));
    }
}
