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
 * Represents a function that accepts an {@code int}-valued argument and produces a result.
 * This is the {@code int}-consuming primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This interface extends both {@link java.util.function.IntFunction} and
 * {@link Throwables.IntFunction}, providing compatibility with the standard Java functional
 * interfaces while also supporting the Throwables framework.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(int)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <R> the type of the result of the function
 * @see java.util.function.Function
 */
@FunctionalInterface
public interface IntFunction<R> extends Throwables.IntFunction<R, RuntimeException>, java.util.function.IntFunction<R> { //NOSONAR
    /**
     * A function that boxes an {@code int} value into an {@link Integer}.
     */
    IntFunction<Integer> BOX = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntFunction<String> toString = value -> "Number: " + value;
     * String result = toString.apply(42); // Returns "Number: 42"
     *
     * IntFunction<Integer> square = value -> value * value;
     * Integer squared = square.apply(5); // Returns 25
     * }</pre>
     *
     * @param value the function argument
     * @return the function result
     */
    @Override
    R apply(int value);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntFunction<String> intToString = value -> String.valueOf(value);
     * Function<String, Integer> stringLength = String::length;
     * IntFunction<Integer> combined = intToString.andThen(stringLength);
     * Integer length = combined.apply(12345); // Returns 5
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> IntFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument as an {@link Integer}.
     * This is equivalent to the {@link #BOX} function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntFunction<Integer> identity = IntFunction.identity();
     * Integer result = identity.apply(42); // Returns 42 (boxed)
     * int[] array = {1, 2, 3};
     * List<Integer> list = IntStream.of(array).mapToObj(identity).collect(Collectors.toList());
     * }</pre>
     *
     * @return a function that always returns its input argument boxed as an {@link Integer}
     */
    static IntFunction<Integer> identity() {
        return t -> t;
    }
}
