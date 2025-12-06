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
 * Represents a function that accepts two float-valued arguments and produces a result.
 * This is the two-arity specialization of {@link java.util.function.Function} for {@code float} values.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(float, float)}.</p>
 *
 * <p>This interface extends {@link Throwables.FloatBiFunction} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 * @see FloatFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface FloatBiFunction<R> extends Throwables.FloatBiFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given two float arguments and produces a result.
     *
     * <p>This method takes two float values as input and returns a result of type {@code R}.
     * The specific computation performed depends on the implementation.</p>
     *
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Mathematical calculations with two variables (e.g., addition, multiplication, power)</li>
     *   <li>Creating objects from two float values (e.g., Point2D from x and y coordinates)</li>
     *   <li>Comparison operations returning wrapped results</li>
     *   <li>Converting pairs of float values to other representations</li>
     *   <li>Aggregating two float values into a single result</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatBiFunction<Float> average = (a, b) -> (a + b) / 2.0f;
     * Float result = average.apply(10.0f, 20.0f);   // Returns 15.0f
     * }</pre>
     *
     * @param t the first float argument
     * @param u the second float argument
     * @return the function result of type {@code R}
     */
    @Override
    R apply(float t, float u);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * <p>This method enables function composition, allowing you to chain operations together.
     * The result of this function becomes the input to the {@code after} function.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Function that calculates the hypotenuse
     * FloatBiFunction<Float> hypotenuse = (a, b) -> (float) Math.sqrt(a * a + b * b);
     *
     * // Function that formats the result
     * Function<Float, String> formatter = h -> String.format("Hypotenuse: %.2f", h);
     *
     * // Composed function that calculates hypotenuse and formats it
     * FloatBiFunction<String> hypotenuseWithFormat = hypotenuse.andThen(formatter);
     *
     * String result = hypotenuseWithFormat.apply(3.0f, 4.0f);   // "Hypotenuse: 5.00"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> FloatBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }
}
