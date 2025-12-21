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
 * Represents a function that accepts an object-valued argument and two int-valued arguments,
 * and produces a result. This is a three-arity specialization of {@code Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, int, int)}.
 *
 * <p>The interface extends {@code Throwables.ObjBiIntFunction} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * ObjBiIntFunction<String, String> substring = (str, start, end) ->
 *     str.substring(start, end);
 * String result = substring.apply("Hello World", 0, 5);   // returns "Hello"
 *
 * ObjBiIntFunction<int[][], Integer> getMatrixElement = (matrix, row, col) ->
 *     (row >= 0 && row < matrix.length && col >= 0 && matrix.length > 0 && col < matrix[0].length) ? matrix[row][col] : null;
 * Integer value = getMatrixElement.apply(matrix, 2, 3);
 *
 * ObjBiIntFunction<List<String>, List<String>> subList = (list, from, to) ->
 *     list.subList(from, to);
 * }</pre>
 *
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ObjBiIntFunction<T, R> extends Throwables.ObjBiIntFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p>This method takes an object of type T and two int values as input,
     * then processes them to produce a result of type R.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Extracting subranges from collections or strings</li>
     *   <li>Accessing elements in two-dimensional data structures using coordinates</li>
     *   <li>Performing calculations that require an object and two numeric parameters</li>
     *   <li>Creating new objects based on an existing object and two indices</li>
     *   <li>Implementing lookup operations with two numeric keys</li>
     * </ul>
     *
     * @param t the object first argument
     * @param i the first int argument (often used as start index, row, or x-coordinate)
     * @param j the second int argument (often used as end index, column, or y-coordinate)
     * @return the function result of type R
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
     * <p>This method allows for function composition, enabling the chaining of
     * operations where the output of this function becomes the input of the next.
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
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> ObjBiIntFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, i, j) -> after.apply(apply(t, i, j));
    }
}
