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
 * Represents a function that accepts a float-valued argument and produces a double-valued result.
 * This is a functional interface whose functional method is {@link #applyAsDouble(float)}.
 *
 * <p>This is a primitive type specialization of {@link java.util.function.Function} for the
 * case where the input is a {@code float} and the output is a {@code double}.</p>
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see FloatToIntFunction
 * @see FloatToLongFunction
 */
@FunctionalInterface
public interface FloatToDoubleFunction extends Throwables.FloatToDoubleFunction<RuntimeException> {
    /**
     * A default function that converts a float value to double through widening primitive conversion.
     * This is equivalent to a simple cast from float to double.
     */
    FloatToDoubleFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given float argument.
     *
     * <p>The implementation should define how the float value is transformed into a double value.
     * Common implementations include widening conversion, mathematical transformations,
     * or value mapping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatToDoubleFunction toDouble = value -> value;
     * double result = toDouble.applyAsDouble(3.5f); // Returns 3.5 as double
     *
     * FloatToDoubleFunction squared = value -> value * value;
     * double square = squared.applyAsDouble(5.0f); // Returns 25.0
     * }</pre>
     *
     * @param value the float function argument
     * @return the double function result
     */
    @Override
    double applyAsDouble(float value);

    /**
     * Returns this object as a {@link Throwables.FloatToDoubleFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.FloatToDoubleFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.FloatToDoubleFunction}
     * @return a {@link Throwables.FloatToDoubleFunction} view of this object
     */
    default <E extends Throwable> Throwables.FloatToDoubleFunction<E> toThrowable() {
        return (Throwables.FloatToDoubleFunction<E>) this;
    }
}
