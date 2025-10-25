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
 * Represents a function that accepts three double-valued arguments and produces a result.
 * This is the three-arity specialization of {@link java.util.function.Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #apply(double, double, double)}.</p>
 * 
 * <p>This interface extends {@link Throwables.DoubleTriFunction} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 * 
 * @param <R> the type of the result of the function
 * 
 * @see java.util.function.Function
 * @see java.util.function.DoubleFunction
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface DoubleTriFunction<R> extends Throwables.DoubleTriFunction<R, RuntimeException> { //NOSONAR

    /**
     * Applies this function to the given three double arguments and produces a result.
     * 
     * <p>This method takes three double values as input and returns a result of type {@code R}.
     * The specific computation performed depends on the implementation.</p>
     * 
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Mathematical calculations with three variables (e.g., volume calculation: length × width × height)</li>
     *   <li>Color space conversions (e.g., RGB to HSL)</li>
     *   <li>3D coordinate transformations</li>
     *   <li>Statistical computations requiring three parameters</li>
     *   <li>Creating objects from three double values</li>
     * </ul>
     *
     * @param a the first double argument
     * @param b the second double argument
     * @param c the third double argument
     * @return the function result of type {@code R} if the function encounters an error during computation
     */
    @Override
    R apply(double a, double b, double c);

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
     * // Function that calculates volume
     * DoubleTriFunction<Double> volume = (l, w, h) -> l * w * h;
     * 
     * // Function that formats the result
     * Function<Double, String> formatter = v -> String.format("Volume: %.2f cubic units", v);
     * 
     * // Composed function that calculates volume and formats it
     * DoubleTriFunction<String> volumeWithFormat = volume.andThen(formatter);
     * 
     * String result = volumeWithFormat.apply(2.0, 3.0, 4.0); // "Volume: 24.00 cubic units"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> DoubleTriFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}