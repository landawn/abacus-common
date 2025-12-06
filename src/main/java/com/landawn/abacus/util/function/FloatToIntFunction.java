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
 * Represents a function that accepts a float-valued argument and produces an int-valued result.
 * This is a functional interface whose functional method is {@link #applyAsInt(float)}.
 *
 * <p>This is a primitive type specialization of {@link java.util.function.Function} for the
 * case where the input is a {@code float} and the output is an {@code int}.</p>
 *
 * @see java.util.function.Function
 * @see java.util.function.DoubleToIntFunction
 * @see java.util.function.ToIntFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface FloatToIntFunction {
    /**
     * A default function that converts a float value to int through narrowing primitive conversion (casting).
     * This truncates the decimal portion and may result in overflow if the float value exceeds int range.
     *
     * <p>Note: For float values outside the int range [-2^31, 2^31-1], the result is undefined
     * due to overflow. Special float values (NaN, positive/negative infinity) are converted to 0 or
     * Integer.MAX_VALUE/Integer.MIN_VALUE respectively.</p>
     */
    FloatToIntFunction DEFAULT = value -> (int) value;

    /**
     * Applies this function to the given float argument.
     *
     * <p>The implementation should define how the float value is transformed into an int value.
     * Common implementations include truncation, rounding, or custom mapping logic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatToIntFunction truncate = value -> (int) value;
     * int result = truncate.applyAsInt(3.14f); // Returns 3
     *
     * FloatToIntFunction round = value -> Math.round(value);
     * int rounded = round.applyAsInt(3.7f); // Returns 4
     * }</pre>
     *
     * @param value the float function argument
     * @return the int function result
     */
    int applyAsInt(float value);
}
