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
 * Represents a function that accepts a float-valued argument and produces an int-valued result.
 * This is a functional interface whose functional method is {@link #applyAsInt(float)}.
 *
 * <p>This is a primitive type specialization of {@link java.util.function.Function} for the
 * case where the input is a {@code float} and the output is an {@code int}.</p>
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see java.util.function.DoubleToIntFunction
 * @see java.util.function.ToIntFunction
 */
@FunctionalInterface
public interface FloatToIntFunction extends Throwables.FloatToIntFunction<RuntimeException> {
    /**
     * A default function that converts a float value to int through narrowing primitive conversion (casting).
     * This truncates the decimal portion; float values outside the int range are clamped rather than overflowing (see note below).
     *
     * <p>Note: For float values outside the int range [-2^31, 2^31-1], the result is clamped to
     * {@code Integer.MAX_VALUE} or {@code Integer.MIN_VALUE}. Special float values are converted as
     * follows: NaN becomes 0, while positive and negative infinity become {@code Integer.MAX_VALUE}
     * and {@code Integer.MIN_VALUE} respectively.</p>
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
    @Override
    int applyAsInt(float value);

    /**
     * Returns this object as a {@link Throwables.FloatToIntFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.FloatToIntFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.FloatToIntFunction}
     * @return a {@link Throwables.FloatToIntFunction} view of this object
     */
    default <E extends Throwable> Throwables.FloatToIntFunction<E> toThrowable() {
        return (Throwables.FloatToIntFunction<E>) this;
    }
}
