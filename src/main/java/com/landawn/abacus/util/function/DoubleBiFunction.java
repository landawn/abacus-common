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
 * Represents a function that accepts two double-valued arguments and produces a result.
 * This is the two-arity specialization of {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(double, double)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 * @see DoubleFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface DoubleBiFunction<R> extends Throwables.DoubleBiFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given {@code double}-valued arguments and produces a result.
     *
     * <p>This method takes two double values as input and produces a result of type {@code R}.
     * Common use cases include:
     * <ul>
     *   <li>Creating objects from two double values (e.g., creating a Point2D from coordinates)</li>
     *   <li>Performing calculations and wrapping the result in an object</li>
     *   <li>Converting pairs of double values to other representations</li>
     *   <li>Aggregating or combining two double values into a single result</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleBiFunction<String> formatter = (x, y) -> String.format("(%.2f, %.2f)", x, y);
     * String result = formatter.apply(3.14159, 2.71828);   // Returns "(3.14, 2.72)"
     *
     * DoubleBiFunction<Double> average = (a, b) -> (a + b) / 2.0;
     * Double avg = average.apply(10.0, 20.0);   // Returns 15.0
     * }</pre>
     *
     * @param t the first double input argument
     * @param u the second double input argument
     * @return the function result
     */
    @Override
    R apply(double t, double u);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleBiFunction<Double> multiplier = (a, b) -> a * b;
     * Function<Double, String> formatter = d -> "Result: " + d;
     * DoubleBiFunction<String> combined = multiplier.andThen(formatter);
     * String result = combined.apply(2.5, 4.0);   // Returns "Result: 10.0"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> DoubleBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }
}
