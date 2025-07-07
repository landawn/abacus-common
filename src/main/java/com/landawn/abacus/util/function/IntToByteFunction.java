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
 * byte-valued result. This is the {@code int}-to-{@code byte} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsByte(int)}.
 *
 * @see java.util.function.Function
 * @since 1.8
 */
@FunctionalInterface
public interface IntToByteFunction {

    /**
     * A default implementation that performs a narrowing primitive conversion
     * from {@code int} to {@code byte}. This may result in loss of information
     * if the input value is outside the range of {@code byte} (-128 to 127).
     */
    IntToByteFunction DEFAULT = value -> (byte) value;

    /**
     * Applies this function to the given argument.
     *
     * <p>The function performs a conversion from an {@code int} value to a
     * {@code byte} value. Implementations should be aware that converting
     * from {@code int} to {@code byte} may result in loss of precision,
     * as {@code byte} can only represent values from -128 to 127, while
     * {@code int} can represent values from -2,147,483,648 to 2,147,483,647.
     *
     * @param value the function argument, an int value to be converted to byte
     * @return the function result as a byte value. If the input value is outside
     *         the range of byte (-128 to 127), the result will be the lower 8 bits
     *         of the input value (equivalent to {@code (byte) value})
     */
    byte applyAsByte(int value);
}