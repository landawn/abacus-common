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
 * Represents a function that accepts three byte-valued arguments and produces a result.
 * This is the three-arity specialization of {@link java.util.function.Function} for byte values.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(byte, byte, byte)}.
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 * @see java.util.function.BiFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteTriFunction<R> extends Throwables.ByteTriFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given byte arguments.
     * This method takes three byte values as input and produces a result of type R.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriFunction<String> formatter = (r, g, b) -> String.format("RGB(%d,%d,%d)", r, g, b);
     * String color = formatter.apply((byte) 255, (byte) 128, (byte) 0);   // Returns "RGB(255,128,0)"
     *
     * ByteTriFunction<Integer> sum = (a, b, c) -> (int) (a + b + c);
     * Integer total = sum.apply((byte) 10, (byte) 20, (byte) 30);   // Returns 60
     * }</pre>
     *
     * @param a the first byte function argument
     * @param b the second byte function argument
     * @param c the third byte function argument
     * @return the function result of type R
     */
    @Override
    R apply(byte a, byte b, byte c);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteTriFunction<Integer> sum = (a, b, c) -> (int) (a + b + c);
     * Function<Integer, String> toString = Object::toString;
     * ByteTriFunction<String> combined = sum.andThen(toString);
     * String result = combined.apply((byte) 1, (byte) 2, (byte) 3);   // Returns "6"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the
     *         {@code after} function
     */
    default <V> ByteTriFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}
