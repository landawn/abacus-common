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

/**
 * Represents a function that accepts a float-valued argument and produces a double-valued result.
 * This is a functional interface whose functional method is {@link #applyAsDouble(float)}.
 *
 * <p>This is a primitive type specialization of {@link java.util.function.Function} for the
 * case where the input is a {@code float} and the output is a {@code double}.</p>
 *
 * @see java.util.function.Function
 * @see java.util.function.DoubleToIntFunction
 * @see java.util.function.IntToDoubleFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface FloatToDoubleFunction {
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
     * @param value the float function argument
     * @return the double function result
     */
    double applyAsDouble(float value);
}
