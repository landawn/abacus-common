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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents a function that accepts an object-valued argument and two int-valued arguments,
 * and produces a result. This is a three-arity specialization of {@code Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, int, int)}.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjBiIntFunction<String, String> substring = (str, start, end) ->
 *     str.substring(start, end);
 * String result = substring.apply("Hello World", 0, 5);   // returns "Hello"
 *
 * ObjBiIntFunction<int[][], Integer> getMatrixElement = (matrix, row, col) ->
 *     (row >= 0 && row < matrix.length && matrix[row] != null &&
 *         col >= 0 && col < matrix[row].length) ? matrix[row][col] : null;
 * Integer value = getMatrixElement.apply(matrix, 2, 3);
 *
 * ObjBiIntFunction<List<String>, List<String>> subList = (list, from, to) ->
 *     list.subList(from, to);
 * }</pre>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface ObjBiIntFunction<T, R> extends Throwables.ObjBiIntFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * @param t the object argument
     * @param i the first int argument
     * @param j the second int argument
     * @return the function result
     */
    @Override
    R apply(T t, int i, int j);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     *
     * <p>If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjBiIntFunction<String, String> substring = (str, start, end) ->
     *     str.substring(start, end);
     * Function<String, String> toUpperCase = String::toUpperCase;
     *
     * ObjBiIntFunction<String, String> substringAndUpper =
     *     substring.andThen(toUpperCase);
     * String result = substringAndUpper.apply("hello world", 0, 5);   // returns "HELLO"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied.
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     * @throws IllegalArgumentException if {@code after} is {@code null}.
     */
    default <V> ObjBiIntFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
        N.checkArgNotNull(after, cs.after);

        return (t, i, j) -> after.apply(apply(t, i, j));
    }

    /**
     * Returns this object as a {@link Throwables.ObjBiIntFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ObjBiIntFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ObjBiIntFunction}
     * @return a {@link Throwables.ObjBiIntFunction} view of this object
     */
    default <E extends Throwable> Throwables.ObjBiIntFunction<T, R, E> toThrowable() {
        return (Throwables.ObjBiIntFunction<T, R, E>) this;
    }
}
