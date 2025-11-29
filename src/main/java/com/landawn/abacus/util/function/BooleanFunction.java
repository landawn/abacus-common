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
 * Represents a function that accepts a {@code boolean}-valued argument and produces a result.
 * This is the {@code boolean}-consuming primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(boolean)}.
 *
 * @param <R> the type of the result of the function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface BooleanFunction<R> extends Throwables.BooleanFunction<R, RuntimeException> { //NOSONAR
    /**
     * A function that boxes a primitive {@code boolean} value into a {@code Boolean} object.
     * This is equivalent to {@code Boolean.valueOf(boolean)}.
     */
    BooleanFunction<Boolean> BOX = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanFunction<String> formatter = b -> b ? "YES" : "NO";
     * String result = formatter.apply(true);  // Returns "YES"
     * }</pre>
     *
     * @param value the function argument
     * @return the function result
     */
    @Override
    R apply(boolean value);

    /**
     * Returns a composed function that first applies this function to its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanFunction<String> toString = b -> String.valueOf(b);
     * Function<String, Integer> length = String::length;
     * BooleanFunction<Integer> combined = toString.andThen(length);
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> BooleanFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument.
     * This is equivalent to the identity function for {@code boolean} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanFunction<Boolean> id = BooleanFunction.identity();
     * Boolean result = id.apply(true);  // Returns true
     * }</pre>
     *
     * @return a function that always returns its input argument
     */
    static BooleanFunction<Boolean> identity() {
        return t -> t;
    }
}
