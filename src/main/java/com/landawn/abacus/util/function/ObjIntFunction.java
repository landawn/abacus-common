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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Throwables;

/**
 * A functional interface that represents a function that accepts an object-valued argument
 * and an int-valued argument, and produces a result. This is a specialization of BiFunction
 * for the case where the second argument is a primitive int.
 *
 * <p>This interface is typically used for operations that need to compute a value based on
 * an object and an integer, such as array/list access operations, indexed transformations,
 * or calculations that involve integer parameters like positions, counts, or identifiers.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(Object, int)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 * @see java.util.function.BiFunction
 * @see ToIntFunction
 */
@FunctionalInterface
public interface ObjIntFunction<T, R> extends Throwables.ObjIntFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p>This method takes an object of type T and an int value as input and
     * produces a result of type R. The function should be deterministic, meaning
     * that for the same inputs, it should always produce the same output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIntFunction<List<String>, String> getAtIndex = (list, index) -> 
     *     list.get(index);
     * ObjIntFunction<String, String> repeat = (str, times) -> 
     *     str.repeat(times);
     * 
     * String element = getAtIndex.apply(myList, 2);
     * String repeated = repeat.apply("Hello", 3); // Returns "HelloHelloHello"
     * }</pre>
     *
     * @param t the first function argument of type T
     * @param u the second function argument, a primitive int value
     * @return the function result of type R
     */
    @Override
    @MayReturnNull
    R apply(T t, int u);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation of
     * either function throws an exception, it is relayed to the caller of the
     * composed function.
     *
     * <p>This method enables function composition, allowing you to chain multiple
     * transformations together. The output of this function becomes the input to
     * the {@code after} function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ObjIntFunction<String[], String> getElement = (array, index) -> 
     *     array[index];
     * Function<String, Integer> parseLength = str -> 
     *     str.length();
     * 
     * ObjIntFunction<String[], Integer> getElementLength = 
     *     getElement.andThen(parseLength);
     * 
     * String[] words = {"Hello", "World", "Java"};
     * Integer length = getElementLength.apply(words, 1); // Returns 5 (length of "World")
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> ObjIntFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }
}
