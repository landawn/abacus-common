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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that accepts two arguments and produces a result.
 * This is the two-arity specialization of {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
@FunctionalInterface
public interface BiFunction<T, U, R> extends Throwables.BiFunction<T, U, R, RuntimeException>, java.util.function.BiFunction<T, U, R> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunction<String, String, String> concatenator = (s1, s2) -> s1 + s2;
     * String result = concatenator.apply("Hello, ", "World!");  // Returns "Hello, World!"
     *
     * BiFunction<Integer, Integer, Integer> adder = (a, b) -> a + b;
     * Integer sum = adder.apply(10, 20);  // Returns 30
     * }</pre>
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result
     */
    @Override
    R apply(T t, U u);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunction<Integer, Integer, Integer> multiplier = (a, b) -> a * b;
     * Function<Integer, String> toString = Object::toString;
     * BiFunction<Integer, Integer, String> combined = multiplier.andThen(toString);
     * String result = combined.apply(3, 4);  // Returns "12"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed {@code BiFunction} that first applies this function and then applies the {@code after} function
     */
    @Override
    default <V> BiFunction<T, U, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }

    /**
     * Converts this {@code BiFunction} to a {@code Throwables.BiFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiFunction<String, String, String> joiner = (s1, s2) -> s1 + " " + s2;
     * var throwableFunction = joiner.toThrowable();
     * // Can now be used in contexts that handle exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.BiFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.BiFunction<T, U, R, E> toThrowable() {
        return (Throwables.BiFunction<T, U, R, E>) this;
    }
}
