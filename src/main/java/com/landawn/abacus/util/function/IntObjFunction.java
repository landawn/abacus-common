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
 * Represents a function that accepts an {@code int}-valued and an object-valued argument and produces a result.
 * This is the {@code (int, reference)} specialization of {@link java.util.function.BiFunction}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(int, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 * @see java.util.function.BiFunction
 * @see IntFunction
 */
@FunctionalInterface
public interface IntObjFunction<T, R> extends Throwables.IntObjFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntObjFunction<String, String> concat = (index, str) ->
     *     str + " at index " + index;
     * String result = concat.apply(5, "Item");   // Returns "Item at index 5"
     *
     * IntObjFunction<List<String>, String> getElement = (index, list) ->
     *     list.get(index);
     * String element = getElement.apply(2, Arrays.asList("a", "b", "c"));   // Returns "c"
     * }</pre>
     *
     * @param t the {@code int} argument
     * @param u the object argument
     * @return the function result
     */
    @Override
    R apply(int t, T u);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntObjFunction<String, String> concat = (index, str) ->
     *     str + "[" + index + "]";
     * Function<String, Integer> getLength = String::length;
     *
     * IntObjFunction<String, Integer> concatAndGetLength = concat.andThen(getLength);
     * Integer result = concatAndGetLength.apply(5, "Item");   // Returns 10 (length of "Item[5]")
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after}
     *         function
     */
    default <V> IntObjFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (i, t) -> after.apply(apply(i, t));
    }
}
