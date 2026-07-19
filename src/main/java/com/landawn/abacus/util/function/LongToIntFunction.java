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
 * Represents a function that accepts a {@code long}-valued argument and produces an
 * {@code int}-valued result. This is the {@code long}-to-{@code int} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This interface extends both {@link Throwables.LongToIntFunction} with
 * {@link RuntimeException} and {@link java.util.function.LongToIntFunction},
 * providing compatibility with the Java standard library while supporting the
 * abacus-common framework's exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(long)}.
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see java.util.function.LongToIntFunction
 */
@FunctionalInterface
public interface LongToIntFunction extends Throwables.LongToIntFunction<RuntimeException>, java.util.function.LongToIntFunction { //NOSONAR
    /**
     * A function that converts a long value to an int value using a narrowing primitive conversion (cast).
     *
     * <p>This default conversion function truncates the long value to fit within the int range.
     * If the long value is outside the range of int (Integer.MIN_VALUE to Integer.MAX_VALUE),
     * the result will be the low-order 32 bits of the long value, which may produce unexpected results.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongToIntFunction converter = LongToIntFunction.DEFAULT;
     * int result1 = converter.applyAsInt(42L);           // returns 42
     * int result2 = converter.applyAsInt(2147483648L);   // returns -2147483648 (overflow)
     * }</pre>
     *
     * <p>Warning: This conversion can lose information about the overall magnitude
     * of the long value and may produce a result with opposite sign.
     */
    LongToIntFunction DEFAULT = value -> (int) value;

    /**
     * Applies this function to the given argument.
     *
     * <p>Converts a long value to an int value according to the function's implementation.
     * The specific conversion logic depends on the implementation, but common use cases include:
     * <ul>
     *   <li>Narrowing conversion with overflow handling</li>
     *   <li>Extracting specific bits or bit ranges</li>
     *   <li>Hash code generation from long values</li>
     *   <li>Index calculations where the result must be an int</li>
     *   <li>Custom business logic requiring int output</li>
     * </ul>
     *
     * <p>Implementations that use a narrowing primitive conversion, such as {@link #DEFAULT},
     * have the following properties:
     * <ul>
     *   <li>Values outside the int range will be truncated</li>
     *   <li>The sign of the result may differ from the original</li>
     *   <li>Information about the magnitude may be lost</li>
     * </ul>
     *
     * @param value the {@code long} function argument
     * @return the {@code int} result of applying this function to the argument
     */
    @Override
    int applyAsInt(long value);

    /**
     * Returns this object as a {@link Throwables.LongToIntFunction} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.LongToIntFunction}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.LongToIntFunction}
     * @return a {@link Throwables.LongToIntFunction} view of this object
     */
    default <E extends Throwable> Throwables.LongToIntFunction<E> toThrowable() {
        return (Throwables.LongToIntFunction<E>) this;
    }
}
