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
 * Represents a function that accepts a {@code long}-valued argument and produces a
 * {@code double}-valued result. This is the {@code long}-to-{@code double} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This interface extends both {@link Throwables.LongToDoubleFunction} with
 * {@link RuntimeException} and {@link java.util.function.LongToDoubleFunction},
 * providing compatibility with the Java standard library while supporting the
 * abacus-common framework's exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsDouble(long)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see java.util.function.LongToDoubleFunction
 */
@FunctionalInterface
public interface LongToDoubleFunction extends Throwables.LongToDoubleFunction<RuntimeException>, java.util.function.LongToDoubleFunction { //NOSONAR
    /**
     * A function that converts a long value to a double value using a simple cast.
     *
     * <p>This is the default conversion function that preserves the numeric value
     * as closely as possible when converting from long to double. Note that for
     * values outside the exact-integer range -2<sup>53</sup> to 2<sup>53</sup>,
     * the result may be rounded to the nearest representable {@code double}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongToDoubleFunction converter = LongToDoubleFunction.DEFAULT;
     * double result = converter.applyAsDouble(42L);   // returns 42.0
     * }</pre>
     *
     */
    LongToDoubleFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p>Converts a long value to a double value according to the function's implementation.
     * The specific conversion logic depends on the implementation, but common use cases include:
     * <ul>
     *   <li>Simple type conversion (casting)</li>
     *   <li>Mathematical transformations (e.g., logarithm, square root)</li>
     *   <li>Scaling operations (e.g., converting cents to dollars)</li>
     *   <li>Custom business logic transformations</li>
     * </ul>
     *
     * @param value the {@code long} function argument
     * @return the {@code double} result of applying this function to the argument
     */
    @Override
    double applyAsDouble(long value);

    /**
     * Returns this object as a {@link Throwables.LongToDoubleFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.LongToDoubleFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.LongToDoubleFunction}
     * @return a {@link Throwables.LongToDoubleFunction} view of this object
     */
    default <E extends Throwable> Throwables.LongToDoubleFunction<E> toThrowable() {
        return (Throwables.LongToDoubleFunction<E>) this;
    }
}
