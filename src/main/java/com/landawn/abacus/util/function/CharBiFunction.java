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
 * Represents a function that accepts two {@code char}-valued arguments and produces a result.
 * This is the primitive type specialization of {@link java.util.function.BiFunction} for {@code char}.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(char, char)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.BiFunction
 * @see CharFunction
 * @see CharTriFunction
 */
@FunctionalInterface
public interface CharBiFunction<R> extends Throwables.CharBiFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharBiFunction<String> concat = (c1, c2) -> "" + c1 + c2;
     * String result = concat.apply('H', 'i');   // Returns "Hi"
     *
     * CharBiFunction<Integer> sumCodes = (c1, c2) -> (int) c1 + (int) c2;
     * int sum = sumCodes.apply('A', 'B');   // Returns sum of char codes
     * }</pre>
     *
     * @param a the first function argument
     * @param b the second function argument
     * @return the function result
     */
    @Override
    R apply(char a, char b);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharBiFunction<String> concat = (c1, c2) -> "" + c1 + c2;
     * Function<String, Integer> length = String::length;
     * CharBiFunction<Integer> concatAndGetLength = concat.andThen(length);
     * Integer result = concatAndGetLength.apply('a', 'b');   // Returns 2
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied.
     * @return a composed function that first applies this function and then applies the
     *         {@code after} function
     * @throws IllegalArgumentException if {@code after} is {@code null}.
     */
    default <V> CharBiFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) throws IllegalArgumentException {
        N.checkArgNotNull(after, cs.after);

        return (a, b) -> after.apply(apply(a, b));
    }

    /**
     * Returns this object as a {@link Throwables.CharBiFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.CharBiFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.CharBiFunction}
     * @return a {@link Throwables.CharBiFunction} view of this object
     */
    default <E extends Throwable> Throwables.CharBiFunction<R, E> toThrowable() {
        return (Throwables.CharBiFunction<R, E>) this;
    }
}
