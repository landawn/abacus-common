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
 * Represents a function that accepts three short-valued arguments and produces a result.
 * This is the three-arity specialization of {@link ShortFunction}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(short, short, short)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see ShortFunction
 * @see ShortBiFunction
 * @see java.util.function.Function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ShortTriFunction<R> extends Throwables.ShortTriFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortTriFunction<Integer> sum = (a, b, c) -> (int) (a + b + c);
     * Integer result1 = sum.apply((short) 10, (short) 20, (short) 30); // returns 60
     *
     * ShortTriFunction<String> formatter = (x, y, z) ->
     *     String.format("Point3D(%d, %d, %d)", x, y, z);
     * String result2 = formatter.apply((short) 1, (short) 2, (short) 3);
     * // returns "Point3D(1, 2, 3)"
     *
     * ShortTriFunction<Boolean> inRange = (value, min, max) ->
     *     value >= min && value <= max;
     * Boolean result3 = inRange.apply((short) 50, (short) 0, (short) 100); // returns true
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @param c the third function argument
     * @return the function result
     */
    @Override
    R apply(short a, short b, short c);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> ShortTriFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}
