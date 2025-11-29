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
 * Represents a function that accepts two {@code int}-valued arguments and produces a result.
 * This is the two-arity specialization of {@link java.util.function.Function} for {@code int} arguments.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(int, int)}.
 *
 * @param <R> the type of the result of the function
 * @see java.util.function.Function
 * @see IntFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntBiFunction<R> extends Throwables.IntBiFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given {@code int}-valued arguments and produces a result.
     *
     * <p>This method takes two int values as input and produces a result of type {@code R}.
     * Common use cases include:
     * <ul>
     *   <li>Creating objects from two int values (e.g., creating a Point from x and y coordinates)</li>
     *   <li>Performing calculations on two ints and wrapping the result in an object</li>
     *   <li>Looking up values in a two-dimensional structure using int indices</li>
     *   <li>Combining two int values into a formatted string or other representation</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntBiFunction<String> formatter = (a, b) -> a + " x " + b;
     * String result = formatter.apply(10, 20);  // Returns "10 x 20"
     *
     * IntBiFunction<Point> pointCreator = Point::new;
     * Point point = pointCreator.apply(5, 10);  // Creates Point(5, 10)
     * }</pre>
     *
     * @param t the first {@code int} argument
     * @param u the second {@code int} argument
     * @return the function result
     */
    @Override
    R apply(int t, int u);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntBiFunction<Integer> adder = (a, b) -> a + b;
     * Function<Integer, String> formatter = n -> "Result: " + n;
     * IntBiFunction<String> combined = adder.andThen(formatter);
     * String result = combined.apply(10, 20);  // Returns "Result: 30"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after}
     *         function
     */
    default <V> IntBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }
}
