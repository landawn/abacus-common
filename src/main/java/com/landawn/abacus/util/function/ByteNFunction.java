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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;

/**
 * Represents a function that accepts a variable number of byte-valued arguments and produces a result.
 * This is a functional interface designed to process byte arrays of any length and return a value.
 *
 * <p>This is a functional interface whose functional method is {@link #apply(byte...)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <R> the type of the result of the function
 *
 * @see java.util.function.Function
 */
@FunctionalInterface
public interface ByteNFunction<R> extends Throwables.ByteNFunction<R, RuntimeException> { //NOSONAR
    /**
     * Applies this function to the given byte array arguments.
     * The array can be of any length, including zero.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteNFunction<Integer> summer = values -> {
     *     int sum = 0;
     *     for (byte v : values) sum += v;
     *     return sum;
     * };
     * Integer result = summer.apply((byte) 1, (byte) 2, (byte) 3);   // Returns 6
     * }</pre>
     *
     * @param args the byte array input arguments. Can be empty but not {@code null}.
     * @return the function result of type R
     */
    @Override
    R apply(byte... args);

    /**
     * Returns a composed function that first applies this function to its input,
     * and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteNFunction<Integer> summer = values -> {
     *     int sum = 0;
     *     for (byte v : values) sum += v;
     *     return sum;
     * };
     * Function<Integer, String> formatter = i -> "Sum: " + i;
     * ByteNFunction<String> combined = summer.andThen(formatter);
     * String result = combined.apply((byte) 1, (byte) 2, (byte) 3);   // Returns "Sum: 6"
     * }</pre>
     *
     * @param <V> the type of output of the {@code after} function, and of the composed function
     * @param after the function to apply after this function is applied. Must not be {@code null}.
     * @return a composed function that first applies this function and then applies the
     *         {@code after} function
     * @throws IllegalArgumentException if {@code after} is null
     */
    @Override
    default <V> ByteNFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        N.checkArgNotNull(after, cs.after);
        return args -> after.apply(apply(args));
    }

    /**
     * Returns this object as a {@link Throwables.ByteNFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.ByteNFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.ByteNFunction}
     * @return a {@link Throwables.ByteNFunction} view of this object
     */
    default <E extends Throwable> Throwables.ByteNFunction<R, E> toThrowable() {
        return (Throwables.ByteNFunction<R, E>) this;
    }
}
