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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents a function that accepts a variable number of float-valued arguments and produces a result.
 * This is the N-arity specialization of {@link FloatFunction}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(float...)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <R> the type of the result of the function
 *
 * @see FloatFunction
 * @see FloatBiFunction
 */
@FunctionalInterface
public interface FloatNFunction<R> extends Throwables.FloatNFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given float arguments.
     *
     * <p>The function implementation should define how the variable number of float
     * arguments are processed to produce the result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatNFunction<Float> average = args -> {
     *     if (args.length == 0) return 0.0f;
     *     float sum = 0;
     *     for (float f : args) sum += f;
     *     return sum / args.length;
     * };
     * Float avg = average.apply(1.0f, 2.0f, 3.0f);   // Returns 2.0f
     * }</pre>
     *
     * @param args the float values to be processed. May be empty, in which case
     *             the function should handle the empty array appropriately.
     * @return the function result
     */
    @Override
    R apply(float... args);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result. If evaluation of
     * either function throws an exception, it is relayed to the caller of the
     * composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatNFunction<Float> sum = args -> {
     *     float total = 0;
     *     for (float f : args) total += f;
     *     return total;
     * };
     * FloatNFunction<String> formatted = sum.andThen(f -> String.format("%.2f", f));
     * String result = formatted.apply(1.1f, 2.2f, 3.3f);   // "6.60"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     *         applies the {@code after} function
     * @throws IllegalArgumentException if {@code after} is null
     */
    @Override
    default <V> FloatNFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        N.checkArgNotNull(after, cs.after);
        return args -> after.apply(apply(args));
    }

    /**
     * Returns this object as a {@link Throwables.FloatNFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.FloatNFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.FloatNFunction}
     * @return a {@link Throwables.FloatNFunction} view of this object
     */
    default <E extends Throwable> Throwables.FloatNFunction<R, E> toThrowable() {
        return (Throwables.FloatNFunction<R, E>) this;
    }
}
