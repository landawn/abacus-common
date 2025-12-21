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
 * Represents a function that accepts a variable number of short-valued arguments and produces a result.
 * This is the variable-arity specialization of {@link ShortFunction}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(short...)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see ShortFunction
 * @see java.util.function.Function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ShortNFunction<R> extends Throwables.ShortNFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortNFunction<Integer> sum = args -> {
     *     int total = 0;
     *     for (short value : args) total += value;
     *     return total;
     * };
     * Integer result1 = sum.apply((short) 1, (short) 2, (short) 3);   // returns 6
     *
     * ShortNFunction<Short> max = args -> {
     *     if (args.length == 0) return 0;
     *     short maxVal = args[0];
     *     for (short v : args) if (v > maxVal) maxVal = v;
     *     return maxVal;
     * };
     * Short result2 = max.apply((short) 5, (short) 2, (short) 8, (short) 1);   // returns 8
     * }</pre>
     *
     * @param args the function arguments as a variable-length array of short values
     * @return the function result
     */
    @Override
    R apply(short... args);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortNFunction<Integer> sum = args -> {
     *     int total = 0;
     *     for (short value : args) total += value;
     *     return total;
     * };
     * ShortNFunction<String> formatted = sum.andThen(String::valueOf);
     * String result = formatted.apply((short) 1, (short) 2, (short) 3);   // "6"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    @Override
    default <V> ShortNFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return args -> after.apply(apply(args));
    }
}
