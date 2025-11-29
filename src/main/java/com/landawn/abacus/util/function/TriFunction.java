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
 * Represents a function that accepts three arguments and produces a result.
 * This is the three-arity specialization of {@link java.util.function.Function}.
 *
 * <p>This interface extends the Throwables.TriFunction, providing compatibility
 * with the abacus-common framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the function
 * @param <B> the type of the second argument to the function
 * @param <C> the type of the third argument to the function
 * @param <R> the type of the result of the function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> extends Throwables.TriFunction<A, B, C, R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments and returns the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;
     * Integer result = sum.apply(1, 2, 3);  // returns 6
     *
     * TriFunction<String, String, String, String> concatenator =
     *     (s1, s2, s3) -> s1 + "-" + s2 + "-" + s3;
     * String joined = concatenator.apply("2024", "01", "15");  // returns "2024-01-15"
     *
     * TriFunction<Double, Double, Double, Double> volumeCalculator =
     *     (length, width, height) -> length * width * height;
     * Double volume = volumeCalculator.apply(2.0, 3.0, 4.0);  // returns 24.0
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @return the function result
     */
    @Override
    R apply(A a, B b, C c);

    /**
     * Returns a composed function that first applies this function to its input, and then
     * applies the after function to the result. If evaluation of either function throws an
     * exception, it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriFunction<Integer, Integer, Integer, Integer> sum = (a, b, c) -> a + b + c;
     * Function<Integer, String> formatter = n -> "Result: " + n;
     *
     * TriFunction<Integer, Integer, Integer, String> sumAndFormat = sum.andThen(formatter);
     * String result = sumAndFormat.apply(10, 20, 30);  // returns "Result: 60"
     *
     * TriFunction<String, String, String, String> joiner =
     *     (s1, s2, s3) -> s1 + s2 + s3;
     * Function<String, Integer> lengthGetter = String::length;
     *
     * TriFunction<String, String, String, Integer> joinAndGetLength = joiner.andThen(lengthGetter);
     * Integer length = joinAndGetLength.apply("Hello", "World", "!");  // returns 11
     * }</pre>
     *
     * @param <V> the type of output of the after function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the after function
     */
    default <V> TriFunction<A, B, C, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }

    /**
     * Converts this TriFunction to a Throwables.TriFunction that can throw checked exceptions.
     * This method is useful when you need to use this function in a context that expects
     * a Throwables.TriFunction with a specific exception type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriFunction<String, Integer, Boolean, String> function = (s, i, b) -> { ... };
     * var throwableFunction =
     *     function.toThrowable();
     * }</pre>
     *
     * @param <E> the type of exception that the returned function may throw
     * @return a Throwables.TriFunction that wraps this function
     */
    default <E extends Throwable> Throwables.TriFunction<A, B, C, R, E> toThrowable() {
        return (Throwables.TriFunction<A, B, C, R, E>) this;
    }
}
