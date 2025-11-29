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

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that accepts one argument and produces a result.
 * This is a functional interface whose functional method is {@link #apply(Object)}.
 *
 * <p>This interface extends both {@link Throwables.Function} and {@link java.util.function.Function},
 * providing compatibility with Java's standard functional interfaces while adding support for
 * the Throwables framework.</p>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 */
@FunctionalInterface
public interface Function<T, R> extends Throwables.Function<T, R, RuntimeException>, java.util.function.Function<T, R> { //NOSONAR
    /**
     * Returns a function that always returns its input argument unchanged.
     *
     * <p>This method delegates to {@link Fn#identity()} to provide a consistent
     * implementation across the framework.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<String, String> id = Function.identity();
     * String result = id.apply("test");  // Returns "test"
     * }</pre>
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    static <T> Function<T, T> identity() {
        return Fn.identity();
    }

    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<String, Integer> length = String::length;
     * Function<Integer, String> toString = Object::toString;
     * Function<Integer, Integer> composed = length.compose(toString);
     * Integer result = composed.apply(123);  // Returns 3 (length of "123")
     * }</pre>
     *
     * @param <V> the type of input to the {@code before} function, and to the
     *           composed function
     * @param before the function to apply before this function is applied. Must not be {@code null}.
     * @return a composed {@code Function} that first applies the {@code before}
     *         function and then applies this function
     *
     * @see #andThen(java.util.function.Function)
     */
    @Override
    default <V> Function<V, R> compose(final java.util.function.Function<? super V, ? extends T> before) {
        return (final V v) -> apply(before.apply(v));
    }

    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<String, Integer> length = String::length;
     * Function<Integer, String> toString = Object::toString;
     * Function<String, String> combined = length.andThen(toString);
     * String result = combined.apply("hello");  // Returns "5"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed {@code Function} that first applies this function and then
     *         applies the {@code after} function
     *
     * @see #compose(java.util.function.Function)
     */
    @Override
    default <V> Function<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (final T t) -> after.apply(apply(t));
    }

    /**
     * Converts this function to a {@link Throwables.Function} that can throw checked exceptions.
     *
     * <p>This method provides a bridge to use this function in contexts where checked exceptions
     * need to be handled. The returned function will have the same behavior as this function
     * but with the ability to declare checked exceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<String, Integer> parser = Integer::parseInt;
     * var throwableParser = parser.toThrowable();
     * // Can now be used in contexts that handle NumberFormatException
     * }</pre>
     *
     * @param <E> the type of exception that the returned function may throw
     * @return a {@code Throwables.Function} view of this function
     */
    default <E extends Throwable> Throwables.Function<T, R, E> toThrowable() {
        return (Throwables.Function<T, R, E>) this;
    }
}
