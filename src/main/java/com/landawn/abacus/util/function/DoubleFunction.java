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
 * Represents a function that accepts a double-valued argument and produces a result.
 * This is the double-consuming primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.DoubleFunction
 */
@FunctionalInterface
public interface DoubleFunction<R> extends Throwables.DoubleFunction<R, RuntimeException>, java.util.function.DoubleFunction<R> { //NOSONAR
    /**
     * A predefined function that boxes a primitive {@code double} value into a {@link Double} object.
     * This is equivalent to {@code Double::valueOf} and provides a convenient way to convert
     * primitive doubles to their wrapper type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleFunction<Double> boxer = DoubleFunction.BOX;
     * Double boxed = boxer.apply(3.14); // Returns Double.valueOf(3.14)
     * }</pre>
     */
    DoubleFunction<Double> BOX = value -> value;

    /**
     * Applies this function to the given double-valued argument and produces a result.
     *
     * <p>This method transforms a {@code double} value into a result of type {@code R}.
     * Common use cases include:
     * <ul>
     *   <li>Converting double values to strings or other representations</li>
     *   <li>Creating objects from double values (e.g., creating Date from timestamp)</li>
     *   <li>Categorizing double values based on ranges</li>
     *   <li>Looking up values in maps using double keys</li>
     *   <li>Performing calculations and wrapping results in objects</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleFunction<String> formatter = d -> String.format("%.2f", d);
     * String result = formatter.apply(3.14159); // Returns "3.14"
     *
     * DoubleFunction<Integer> rounder = d -> (int) Math.round(d);
     * Integer rounded = rounder.apply(42.7); // Returns 43
     *
     * DoubleFunction<Boolean> isPositive = d -> d > 0.0;
     * Boolean positive = isPositive.apply(-1.5); // Returns false
     * }</pre>
     *
     * @param value the double function argument
     * @return the function result
     */
    @Override
    R apply(double value);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleFunction<String> doubleToString = d -> String.valueOf(d);
     * Function<String, Integer> stringLength = String::length;
     * DoubleFunction<Integer> combined = doubleToString.andThen(stringLength);
     * Integer length = combined.apply(123.456); // Returns 7 (length of "123.456")
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> DoubleFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument unchanged as a {@link Double} object.
     *
     * <p>This is the identity function for double values, performing boxing from primitive {@code double}
     * to {@link Double}. It is useful when a {@code DoubleFunction<Double>} is required but no
     * transformation is needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleFunction<Double> identity = DoubleFunction.identity();
     * Double result = identity.apply(42.5); // Returns 42.5 (boxed)
     * }</pre>
     *
     * @return a function that always returns its double input argument as a {@link Double}
     */
    static DoubleFunction<Double> identity() {
        return t -> t;
    }

    /**
     * Converts this {@code DoubleFunction} to a {@code Throwables.DoubleFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.DoubleFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.DoubleFunction<R, E> toThrowable() {
        return (Throwables.DoubleFunction<R, E>) this;
    }

}
