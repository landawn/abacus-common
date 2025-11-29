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
 * Represents a function that accepts a long-valued argument and produces an int-valued result.
 * This is the long-to-int primitive specialization for {@code Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(long)}.
 *
 * <p>The interface extends both {@code Throwables.LongToIntFunction} with {@code RuntimeException}
 * and {@code java.util.function.LongToIntFunction}, providing compatibility with the Java standard library
 * while adding a predefined default conversion function.
 *
 * @see java.util.function.Function
 * @see java.util.function.LongToIntFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
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
     * <p>Important: When converting from long to int, be aware that:
     * <ul>
     *   <li>Values outside the int range will be truncated</li>
     *   <li>The sign of the result may differ from the original</li>
     *   <li>Information about the magnitude may be lost</li>
     * </ul>
     *
     * @param value the function argument as a long
     * @return the function result as an int
     */
    @Override
    int applyAsInt(long value);
}
