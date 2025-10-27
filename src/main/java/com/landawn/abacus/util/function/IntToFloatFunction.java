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
 * Represents a function that accepts an int-valued argument and produces a
 * float-valued result. This is the {@code int}-to-{@code float} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsFloat(int)}.
 *
 * @see java.util.function.Function
 * @since 1.8
 */
@FunctionalInterface
public interface IntToFloatFunction {

    /**
     * A default implementation that performs a widening primitive conversion
     * from {@code int} to {@code float}. Note that this conversion may result
     * in loss of precision for large int values, as float has only 24 bits
     * of mantissa compared to int's 32 bits.
     */
    IntToFloatFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p>The function performs a conversion from an {@code int} value to a
     * {@code float} value. This is a widening primitive conversion, but unlike
     * the int-to-double conversion, it may result in loss of precision for
     * large integer values.
     *
     * <p>Important considerations:
     * <ul>
     *   <li>Float values have approximately 7 decimal digits of precision</li>
     *   <li>Int values larger than 2^24 (16,777,216) may lose precision when
     *       converted to float</li>
     *   <li>The sign of the int value is always preserved</li>
     *   <li>Special int values like Integer.MAX_VALUE and Integer.MIN_VALUE
     *       will be approximated as float values</li>
     * </ul>
     *
     * @param value the function argument, an int value to be converted to float
     * @return the function result as a float value. For int values with magnitude
     *         greater than 2^24, the result may be an approximation of the input value
     */
    float applyAsFloat(int value);
}
