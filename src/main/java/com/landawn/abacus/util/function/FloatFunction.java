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
 * Represents a function that accepts a float-valued argument and produces a result.
 * This is the {@code float}-consuming primitive specialization for {@link java.util.function.Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #apply(float)}.</p>
 * 
 * <p>This interface extends {@link Throwables.FloatFunction} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 * 
 * <p>The interface provides a {@code BOX} constant that converts primitive float values to their
 * {@link Float} wrapper object equivalent.</p>
 * 
 * @param <R> the type of the result of the function
 * 
 * @see java.util.function.Function
 * @see java.util.function.DoubleFunction
 * @see java.util.function.IntFunction
 * @see java.util.function.LongFunction
 */
@FunctionalInterface
public interface FloatFunction<R> extends Throwables.FloatFunction<R, RuntimeException> { //NOSONAR

    /**
     * A function that boxes a primitive float value into a {@link Float} object.
     * This is useful when you need to convert primitive float values to their object wrapper.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * FloatFunction<Float> boxer = FloatFunction.BOX;
     * Float boxed = boxer.apply(3.14f); // Returns Float.valueOf(3.14f)
     * }</pre>
     */
    FloatFunction<Float> BOX = value -> value;

    /**
     * Applies this function to the given float-valued argument and produces a result.
     * 
     * <p>This method converts a float value to a result of type {@code R} according to
     * the implementation's logic.</p>
     * 
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Converting float values to strings or other representations</li>
     *   <li>Creating objects from float values</li>
     *   <li>Categorizing float values (e.g., returning enum based on ranges)</li>
     *   <li>Performing calculations and returning wrapped results</li>
     *   <li>Looking up values in maps or tables based on float keys</li>
     * </ul>
     * 
     * <p>Example implementations:</p>
     * <pre>{@code
     * FloatFunction<String> formatter = value -> String.format("%.2f", value);
     * FloatFunction<Boolean> isPositive = value -> value > 0;
     * FloatFunction<Integer> round = value -> Math.round(value);
     * }</pre>
     *
     * @param value the float value to be processed
     * @return the function result of type {@code R}
     * @throws RuntimeException if the function encounters an error during execution
     */
    @Override
    R apply(float value);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     * 
     * <p>This method enables function composition, allowing you to chain operations together.
     * The result of this function becomes the input to the {@code after} function.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * FloatFunction<Double> toDouble = value -> (double) value;
     * Function<Double, String> format = d -> String.format("%.4f", d);
     * 
     * // Composed function that converts float to double, then formats it
     * FloatFunction<String> floatToFormattedString = toDouble.andThen(format);
     * 
     * String result = floatToFormattedString.apply(3.14159f); // "3.1416"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> FloatFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument as a {@link Float} object.
     * This is the identity function for float values, boxing them into Float objects.
     * 
     * <p>This method is useful when you need a {@code FloatFunction<Float>} that performs
     * no transformation other than boxing the primitive value.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * FloatFunction<Float> identity = FloatFunction.identity();
     * Float result = identity.apply(42.0f); // Returns Float.valueOf(42.0f)
     * 
     * // Useful in conditional operations
     * FloatFunction<Float> processor = shouldProcess 
     *     ? value -> value * 2.0f 
     *     : FloatFunction.identity();
     * }</pre>
     *
     * @return a function that always returns its input argument as a boxed Float
     */
    static FloatFunction<Float> identity() {
        return t -> t;
    }
}