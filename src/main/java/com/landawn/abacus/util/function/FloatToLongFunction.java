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
 * Represents a function that accepts a float-valued argument and produces a long-valued result.
 * This is a functional interface whose functional method is {@link #applyAsLong(float)}.
 *
 * <p>This is a primitive type specialization of {@link java.util.function.Function} for the
 * case where the input is a {@code float} and the output is a {@code long}.</p>
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see java.util.function.DoubleToLongFunction
 * @see java.util.function.ToLongFunction
 */
@FunctionalInterface
public interface FloatToLongFunction extends Throwables.FloatToLongFunction<RuntimeException> { //NOSONAR
    /**
     * A default function that converts a float value to long through narrowing primitive conversion (casting).
     * This truncates the decimal portion and may result in precision loss for large float values.
     *
     * <p>Note: For float values outside the long range [-2^63, 2^63-1], the result is clamped to
     * {@code Long.MAX_VALUE} or {@code Long.MIN_VALUE}. Special float values are converted as
     * follows: NaN becomes 0, while positive and negative infinity become {@code Long.MAX_VALUE}
     * and {@code Long.MIN_VALUE} respectively.</p>
     */
    FloatToLongFunction DEFAULT = value -> (long) value;

    /**
     * Applies this function to the given float argument.
     *
     * <p>The implementation should define how the float value is transformed into a long value.
     * Common implementations include truncation, rounding, or custom mapping logic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatToLongFunction truncate = value -> (long) value;
     * long result = truncate.applyAsLong(3.14f); // Returns 3L
     *
     * FloatToLongFunction round = value -> Math.round(value);
     * long rounded = round.applyAsLong(3.7f); // Returns 4L
     * }</pre>
     *
     * @param value the float function argument
     * @return the long function result
     */
    @Override
    long applyAsLong(float value);

    /**
     * Returns this object as a {@link Throwables.FloatToLongFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.FloatToLongFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.FloatToLongFunction}
     * @return a {@link Throwables.FloatToLongFunction} view of this object
     */
    default <E extends Throwable> Throwables.FloatToLongFunction<E> toThrowable() {
        return (Throwables.FloatToLongFunction<E>) this;
    }
}
