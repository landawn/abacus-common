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
 * Represents a function that accepts a short-valued argument and produces a result.
 * This is the {@code short}-consuming primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(short)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ShortFunction<R> extends Throwables.ShortFunction<R, RuntimeException> { //NOSONAR
    /**
     * A function that boxes a primitive short value into a Short object.
     * This is equivalent to {@code Short::valueOf}.
     */
    ShortFunction<Short> BOX = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p>This method takes a short value as input and produces a result of type R.
     * The function should be deterministic, meaning that for the same input, it
     * should always produce the same output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortFunction<String> formatter = value -> "Number: " + value;
     * String result1 = formatter.apply((short) 42); // returns "Number: 42"
     *
     * ShortFunction<Integer> widener = value -> (int) value;
     * Integer result2 = widener.apply((short) 100); // returns 100
     *
     * ShortFunction<Boolean> isPositive = value -> value > 0;
     * Boolean result3 = isPositive.apply((short) -5); // returns false
     * }</pre>
     *
     * @param value the function argument
     * @return the function result
     */
    @Override
    R apply(short value);

    /**
     * Returns a composed function that first applies this function to its input, and then applies
     * the {@code after} function to the result. If evaluation of either function throws an exception,
     * it is relayed to the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortFunction<Integer> widener = value -> (int) value;
     * Function<Integer, String> formatter = n -> "Value: " + n;
     *
     * ShortFunction<String> combined = widener.andThen(formatter);
     * String result = combined.apply((short) 42); // returns "Value: 42"
     *
     * ShortFunction<String> toString = value -> String.valueOf(value);
     * Function<String, Integer> length = String::length;
     *
     * ShortFunction<Integer> stringLength = toString.andThen(length);
     * Integer len = stringLength.apply((short) 12345); // returns 5
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then applies the {@code after} function
     */
    default <V> ShortFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return t -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument.
     * This is equivalent to the identity function for short values, boxing them into Short objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortFunction<Short> identity = ShortFunction.identity();
     * Short result1 = identity.apply((short) 42); // returns 42
     * Short result2 = identity.apply((short) -100); // returns -100
     *
     * // Useful in stream operations
     * short[] values = {1, 2, 3, 4, 5};
     * List<Short> boxed = Arrays.stream(values)
     *     .mapToObj(v -> ShortFunction.identity().apply((short) v))
     *     .collect(Collectors.toList());
     * }</pre>
     *
     * @return a function that always returns its input argument as a Short object
     */
    static ShortFunction<Short> identity() {
        return t -> t;
    }

    /**
     * Converts this {@code ShortFunction} to a {@code Throwables.ShortFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.ShortFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.ShortFunction<R, E> toThrowable() {
        return (Throwables.ShortFunction<R, E>) this;
    }

}
