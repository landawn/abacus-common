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
 * Represents a function that accepts two {@code int}-valued arguments and a single object-valued
 * argument, and produces a result. This is a specialization of function for two primitive {@code int}
 * values and one reference type.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(int, int, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 */
@FunctionalInterface
public interface BiIntObjFunction<T, R> extends Throwables.BiIntObjFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjFunction<String, String> formatter = (i, j, prefix) -> prefix + (i + j);
     * String result = formatter.apply(10, 20, "Sum: "); // Returns "Sum: 30"
     * }</pre>
     *
     * @param i the first function argument (int value)
     * @param j the second function argument (int value)
     * @param t the third function argument (object value)
     * @return the function result
     */
    @Override
    R apply(int i, int j, T t);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjFunction<String, Integer> summer = (i, j, s) -> i + j + s.length();
     * Function<Integer, String> toString = Object::toString;
     * BiIntObjFunction<String, String> combined = summer.andThen(toString);
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> BiIntObjFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (i, j, t) -> after.apply(apply(i, j, t));
    }

    /**
     * Converts this {@code BiIntObjFunction} to a {@code Throwables.BiIntObjFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BiIntObjFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.BiIntObjFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.BiIntObjFunction<T, R, E> toThrowable() {
        return (Throwables.BiIntObjFunction<T, R, E>) this;
    }

}
