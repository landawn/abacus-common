/*
 * Copyright (C) 2024 HaiYang Li
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
 * Represents a function that accepts a byte-valued argument and produces a boolean-valued result.
 * This is the {@code byte}-to-{@code boolean} primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsBoolean(byte)}.
 *
 * @see java.util.function.Function
 * @see ByteToIntFunction
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteToBooleanFunction {
    /**
     * A default implementation that converts a byte value to a boolean.
     * Returns {@code true} if the byte value is greater than 0, otherwise {@code false}.
     * This follows the common convention where positive values are considered "true"
     * and zero or negative values are considered "false".
     */
    ByteToBooleanFunction DEFAULT = value -> value > 0;

    /**
     * Applies this function to the given byte argument and returns a boolean result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteToBooleanFunction isPositive = value -> value > 0;
     * boolean result = isPositive.applyAsBoolean((byte) 5); // Returns true
     *
     * // Using DEFAULT
     * boolean defaultResult = ByteToBooleanFunction.DEFAULT.applyAsBoolean((byte) 0); // Returns false
     * }</pre>
     *
     * @param value the byte function argument
     * @return the boolean function result
     */
    boolean applyAsBoolean(byte value);
}
