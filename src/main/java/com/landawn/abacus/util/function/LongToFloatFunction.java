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
 * Represents a function that accepts a long-valued argument and produces a float-valued result.
 * This is the long-to-float primitive specialization for {@code Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsFloat(long)}.
 *
 * <p>Note: Unlike other primitive function interfaces in this package, this interface does not extend
 * from java.util.function as the JDK does not provide a LongToFloatFunction interface.
 *
 * @see java.util.function.Function
 * @see LongToDoubleFunction
 * @see LongToIntFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongToFloatFunction {
    /**
     * A function that converts a long value to a float value using a simple cast.
     *
     * <p>This is the default conversion function that preserves the numeric value
     * as closely as possible when converting from long to float. Note that for
     * long values outside the range of float precision (larger than 2^24), there
     * will be loss of precision due to the limitations of float representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongToFloatFunction converter = LongToFloatFunction.DEFAULT;
     * float result = converter.applyAsFloat(42L); // returns 42.0f
     * float largeResult = converter.applyAsFloat(9223372036854775807L); // precision loss occurs
     * }</pre>
     */
    LongToFloatFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p>Converts a long value to a float value according to the function's implementation.
     * The specific conversion logic depends on the implementation, but common use cases include:
     * <ul>
     *   <li>Simple type conversion (casting)</li>
     *   <li>Mathematical transformations with float precision</li>
     *   <li>Scaling operations where float precision is sufficient</li>
     *   <li>Custom business logic requiring float output</li>
     * </ul>
     *
     * <p>Important: When converting from long to float, be aware that float has only
     * 24 bits of precision for the significand, which means that long values requiring
     * more than 24 bits of precision will lose accuracy in the conversion.
     *
     * @param value the function argument as a long
     * @return the function result as a float
     */
    float applyAsFloat(long value);
}
