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
 * Represents a function that accepts an {@code int}-valued argument and produces a
 * {@code short}-valued result. This is the {@code int}-to-{@code short} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsShort(int)}.
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
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
     * <p>The function performs a transformation from an {@code int} value to a
     * {@code short} value. The default implementation provided by {@link #DEFAULT}
     * performs a narrowing primitive cast, which retains only the lower 16 bits
     * for int values outside the {@code short} range (-32,768 to 32,767).
     * Custom implementations may apply arbitrary computations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntToShortFunction low16Bits = value -> (short) value;
     * short result = low16Bits.applyAsShort(32768);   // -32768
     * }</pre>
     *
     * @param value the function argument, an int value to be converted to short
     * @return the function result as a short value
     */
    short applyAsShort(int value);
}
