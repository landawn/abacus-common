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
 * Represents a function that accepts three long-valued arguments and produces a result.
 * This is the three-arity primitive specialization of {@code Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(long, long, long)}.
 *
 * <p>The interface extends {@code Throwables.LongTriFunction} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * LongTriFunction<String> formatter = (a, b, c) ->
 *     String.format("(%d, %d, %d)", a, b, c);
 * String result = formatter.apply(10L, 20L, 30L); // returns "(10, 20, 30)"
 *
 * LongTriFunction<Long> median = (a, b, c) -> {
 *     if ((a >= b && a <= c) || (a <= b && a >= c)) return a;
 *     if ((b >= a && b <= c) || (b <= a && b >= c)) return b;
 *     return c;
 * };
 * }</pre>
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.LongFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongTriFunction<R> extends Throwables.LongTriFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p>This method takes three long values as input and produces a result of type R.
     * The implementation defines how the three long values are processed to produce the result.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Combining three values into a single object</li>
     *   <li>Performing calculations and returning the result</li>
     *   <li>Creating formatted strings from three numeric values</li>
     *   <li>Looking up values based on three coordinates or indices</li>
     *   <li>Business logic that requires three long parameters</li>
     * </ul>
     *
     * @param a the first argument
     * @param b the second argument
     * @param c the third argument
     * @return the function result of type R
     */
    @Override
    R apply(long a, long b, long c);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     *
     * <p>If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p>This method allows for function composition, enabling the chaining of
     * operations where the output of this function becomes the input of the next.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongTriFunction<Long> sum = (a, b, c) -> a + b + c;
     * Function<Long, String> format = n -> "Result: " + n;
     * LongTriFunction<String> composed = sum.andThen(format);
     * String result = composed.apply(10L, 20L, 30L); // returns "Result: 60"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    default <V> LongTriFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}
