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
 * Represents a function that accepts a variable number of arguments and produces a result.
 * This is a variable-arity (varargs) generalization of {@code Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #apply(Object[])}.
 * 
 * <p>The 'N' in NFunction stands for 'N-ary', indicating that this function can accept
 * any number of arguments of the same type.
 * 
 * <p>The interface extends {@code Throwables.NFunction} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 * 
 * <p>Example usage:
 * <pre>{@code
 * NFunction<Integer, Integer> sum = args -> {
 *     int total = 0;
 *     for (Integer n : args) {
 *         total += n;
 *     }
 *     return total;
 * };
 * Integer result = sum.apply(1, 2, 3, 4, 5); // returns 15
 * 
 * NFunction<String, String> concatenate = args -> String.join(", ", args);
 * String joined = concatenate.apply("apple", "banana", "orange"); // returns "apple, banana, orange"
 * }</pre>
 * 
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * 
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface NFunction<T, R> extends Throwables.NFunction<T, R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given arguments.
     * 
     * <p>The varargs parameter allows this method to accept any number of arguments
     * of type T, including zero arguments (empty array), and produces a result of type R.
     * 
     * <p>Common use cases include:
     * <ul>
     *   <li>Aggregating multiple values into a single result</li>
     *   <li>Creating composite objects from multiple inputs</li>
     *   <li>Performing calculations on variable numbers of inputs</li>
     *   <li>Building formatted strings from multiple arguments</li>
     * </ul>
     * 
     * <p>Note: The {@code @SuppressWarnings("unchecked")} annotation is used because
     * varargs with generics can generate unchecked warnings at the call site.
     *
     * @param args the function arguments as a varargs array
     * @return the function result of type R
     * @throws RuntimeException if any error occurs during function execution
     */
    @SuppressWarnings("unchecked")
    @Override
    R apply(T... args);

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
     * <p>Example usage:
     * <pre>{@code
     * NFunction<Integer, Integer> sum = args -> {
     *     int total = 0;
     *     for (Integer n : args) {
     *         total += n;
     *     }
     *     return total;
     * };
     * Function<Integer, String> format = n -> "Total: " + n;
     * 
     * NFunction<Integer, String> sumAndFormat = sum.andThen(format);
     * String result = sumAndFormat.apply(10, 20, 30); // returns "Total: 60"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    @Override
    default <V> NFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return args -> after.apply(apply(args));
    }
}