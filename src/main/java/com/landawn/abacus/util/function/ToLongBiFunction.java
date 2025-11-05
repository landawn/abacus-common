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
 * Represents a function that accepts two arguments and produces a long-valued result.
 * This is the long-producing primitive specialization for {@link java.util.function.BiFunction}.
 *
 * <p>This interface extends both the Throwables.ToLongBiFunction and the standard Java
 * ToLongBiFunction, providing compatibility with both the abacus-common framework's error handling
 * mechanisms and the standard Java functional interfaces.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsLong(Object, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 */
@FunctionalInterface
public interface ToLongBiFunction<T, U> extends Throwables.ToLongBiFunction<T, U, RuntimeException>, java.util.function.ToLongBiFunction<T, U> { //NOSONAR
    /**
     * Applies this function to the given arguments and returns a long result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToLongBiFunction<Integer, Integer> multiplier = (a, b) -> (long) a * b;
     * long product = multiplier.applyAsLong(1000000, 1000000); // returns 1000000000000L
     *
     * ToLongBiFunction<String, Long> hasher = (str, seed) -> str.hashCode() + seed;
     * long hash = hasher.applyAsLong("Hello", 12345L); // returns hash of "Hello" plus 12345
     * }</pre>
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result as a primitive long
     */
    @Override
    long applyAsLong(T t, U u);

    /**
     * Converts this {@code ToLongBiFunction} to a {@code Throwables.ToLongBiFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToLongBiFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.ToLongBiFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.ToLongBiFunction<T, U, E> toThrowable() {
        return (Throwables.ToLongBiFunction<T, U, E>) this;
    }

}
