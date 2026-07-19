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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents a function that accepts a double-valued argument and an object-valued argument,
 * and produces a result. This is the (double, reference) specialization of {@link java.util.function.BiFunction}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(double, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the function
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.BiFunction
 */
@FunctionalInterface
public interface DoubleObjFunction<T, R> extends Throwables.DoubleObjFunction<T, R, RuntimeException> { // NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleObjFunction<String, String> formatter = (val, unit) -> val + " " + unit;
     * String result = formatter.apply(3.14, "meters");   // Returns "3.14 meters"
     *
     * DoubleObjFunction<List<Double>, List<Double>> adder = (val, list) -> {
     *     list.add(val);
     *     return list;
     * };
     * }</pre>
     *
     * @param t the double input argument
     * @param u the object input argument
     * @return the function result
     */
    @Override
    R apply(double t, T u);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleObjFunction<String, String> formatter = (val, unit) -> val + " " + unit;
     * Function<String, Integer> length = String::length;
     * DoubleObjFunction<String, Integer> composed = formatter.andThen(length);
     * Integer result = composed.apply(3.14, "meters");   // Returns 11
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the {@code after} function
     * @throws IllegalArgumentException if {@code after} is null
     */
    default <V> DoubleObjFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        N.checkArgNotNull(after, cs.after);
        return (i, t) -> after.apply(apply(i, t));
    }

    /**
     * Returns this object as a {@link Throwables.DoubleObjFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.DoubleObjFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.DoubleObjFunction}
     * @return a {@link Throwables.DoubleObjFunction} view of this object
     */
    default <E extends Throwable> Throwables.DoubleObjFunction<T, R, E> toThrowable() {
        return (Throwables.DoubleObjFunction<T, R, E>) this;
    }
}
