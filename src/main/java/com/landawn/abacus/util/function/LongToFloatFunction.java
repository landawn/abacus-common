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

/**
 * Represents a function that accepts a {@code long}-valued argument and produces a
 * {@code float}-valued result. This is the {@code long}-to-{@code float} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsFloat(long)}.
 *
 * <p>Note: Unlike other primitive function interfaces in this package, this interface does not extend
 * from java.util.function as the JDK does not provide a LongToFloatFunction interface.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see LongToDoubleFunction
 * @see LongToIntFunction
 */
@FunctionalInterface
public interface LongToFloatFunction {
    /**
     * A function that converts a long value to a float value using a simple cast.
     *
     * <p>This is the default conversion function that preserves the numeric value
     * as closely as possible when converting from long to float. Note that for
     * long values whose magnitude exceeds 2^24, there may be loss of precision
     * due to the limitations of float representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongToFloatFunction converter = LongToFloatFunction.DEFAULT;
     * float result = converter.applyAsFloat(42L);                         // returns 42.0f
     * float largeResult = converter.applyAsFloat(9223372036854775807L);   // precision loss occurs
     * }</pre>
     *
     */
    LongToFloatFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * @param value the {@code long} function argument
     * @return the {@code float} function result
     */
    float applyAsFloat(long value);
}
