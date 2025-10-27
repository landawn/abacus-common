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
 * short-valued result. This is the {@code int}-to-{@code short} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsShort(int)}.
 *
 * @see java.util.function.Function
 * @since 1.8
 */
@FunctionalInterface
public interface IntToShortFunction {

    /**
     * A default implementation that performs a narrowing primitive conversion
     * from {@code int} to {@code short}. This may result in loss of information
     * if the input value is outside the range of {@code short} (-32,768 to 32,767).
     */
    IntToShortFunction DEFAULT = value -> (short) value;

    /**
     * Applies this function to the given argument.
     *
     * <p>The function performs a conversion from an {@code int} value to a
     * {@code short} value. This is a narrowing primitive conversion that may
     * result in loss of information. The {@code short} data type is a 16-bit
     * signed two's complement integer with a range from -32,768 to 32,767,
     * while {@code int} is a 32-bit signed integer with a much larger range.
     *
     * <p>Conversion behavior:
     * <ul>
     *   <li>If the int value is within the short range (-32,768 to 32,767),
     *       the value is preserved exactly</li>
     *   <li>If the int value is outside the short range, only the lower 16 bits
     *       of the int value are retained, which may produce unexpected results</li>
     *   <li>For example, 32,768 converts to -32,768, and 65,536 converts to 0</li>
     * </ul>
     *
     * @param value the function argument, an int value to be converted to short
     * @return the function result as a short value. If the input value is outside
     *         the range of short (-32,768 to 32,767), the result will be the lower
     *         16 bits of the input value (equivalent to {@code (short) value})
     */
    short applyAsShort(int value);
}
