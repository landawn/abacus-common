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
 * Represents a function that accepts a variable number of {@code long}-valued
 * arguments and produces a result. This is the N-arity specialization of
 * {@link java.util.function.Function} for {@code long} values.
 *
 * <p>This interface extends {@link Throwables.LongNFunction} with
 * {@link RuntimeException}, providing compatibility with the abacus-common framework's
 * exception handling capabilities.
 *
 * <p>This interface is particularly useful when you need to compute a result
 * from an arbitrary number of long values, without knowing the exact count
 * at compile time.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #apply(long...)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see LongFunction
 * @see LongBiFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongNFunction<R> extends Throwables.LongNFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p>The function processes a variable number of long values and produces
     * a result of type R. The varargs parameter allows for flexible argument
     * counts, from zero to many values.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Aggregating multiple long values into a single result (sum, average, etc.)</li>
     *   <li>Creating objects from arrays of long values</li>
     *   <li>Computing statistics from groups of long values</li>
     *   <li>Transforming arrays of long values into other representations</li>
     *   <li>Implementing variadic mathematical functions</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongNFunction<Double> average = args -> {
     *     if (args.length == 0) return 0.0;
     *     long sum = 0;
     *     for (long value : args) {
     *         sum += value;
     *     }
     *     return (double) sum / args.length;
     * };
     * Double avg = average.apply(10L, 20L, 30L, 40L); // Returns 25.0
     * }</pre>
     *
     * @param args the function arguments as a varargs array. Can be empty, contain
     *             a single value, or multiple values. The array should not be
     *             modified by the implementation
     * @return the function result of type R
     */
    @Override
    R apply(long... args);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation
     * of either function throws an exception, it is relayed to the caller of
     * the composed function.
     *
     * <p>This method enables function composition, allowing you to chain operations
     * where the output of this function becomes the input of the next function.
     * This is useful for building complex transformations from simpler ones.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongNFunction<Long> sum = args -> {
     *     long total = 0;
     *     for (long value : args) {
     *         total += value;
     *     }
     *     return total;
     * };
     * LongNFunction<String> sumAsString = sum.andThen(String::valueOf);
     * String result = sumAsString.apply(1L, 2L, 3L, 4L); // "10"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied.
     *              Must not be {@code null}.
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     */
    @Override
    default <V> LongNFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return args -> after.apply(apply(args));
    }

    /**
     * Converts this {@code LongNFunction} to a {@code Throwables.LongNFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongNFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.LongNFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.LongNFunction<R, E> toThrowable() {
        return (Throwables.LongNFunction<R, E>) this;
    }

}
