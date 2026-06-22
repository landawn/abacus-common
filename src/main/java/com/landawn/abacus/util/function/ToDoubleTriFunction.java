/*
 * Copyright (C) 2024 HaiYang Li
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
 * Represents a function that accepts three arguments and produces a double-valued result.
 * This is the double-producing primitive specialization for a three-argument function.
 *
 * <p>This interface extends the Throwables.ToDoubleTriFunction, providing compatibility
 * with the abacus-common framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsDouble(Object, Object, Object)}.
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * <p><b>Note:</b> {@code TriFunction} primitive result-specializations are provided only for
 * {@code int}/{@code long}/{@code double} ({@code ToInt}/{@code ToLong}/{@code ToDoubleTriFunction}) by
 * design — there are intentionally no {@code byte}/{@code char}/{@code short}/{@code float}/{@code boolean}
 * three-arg variants, unlike the all-8 {@code ToXxxFunction}/{@code ToXxxBiFunction} families.
 *
 * @param <A> the type of the first argument to the function
 * @param <B> the type of the second argument to the function
 * @param <C> the type of the third argument to the function
 */
@FunctionalInterface
public interface ToDoubleTriFunction<A, B, C> extends Throwables.ToDoubleTriFunction<A, B, C, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments and returns a double result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToDoubleTriFunction<Integer, Integer, Integer> volumeCalculator =
     *     (length, width, height) -> (double) length * width * height;
     * double volume = volumeCalculator.applyAsDouble(2, 3, 4);   // returns 24.0
     *
     * ToDoubleTriFunction<String, Integer, Double> weightedLength =
     *     (str, weight, factor) -> str.length() * weight * factor;
     * double result = weightedLength.applyAsDouble("Hello", 2, 1.5);   // returns 15.0
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @return the function result as a primitive double
     */
    @Override
    double applyAsDouble(A a, B b, C c);

    /**
     * Returns this object as a {@link Throwables.ToDoubleTriFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ToDoubleTriFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ToDoubleTriFunction}
     * @return a {@link Throwables.ToDoubleTriFunction} view of this object
     */
    default <E extends Throwable> Throwables.ToDoubleTriFunction<A, B, C, E> toThrowable() {
        return (Throwables.ToDoubleTriFunction<A, B, C, E>) this;
    }
}
