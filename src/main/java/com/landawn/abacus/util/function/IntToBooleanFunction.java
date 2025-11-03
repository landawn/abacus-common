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
 * Represents a function that accepts an {@code int}-valued argument and produces a {@code boolean}-valued result.
 * This is the {@code int}-to-{@code boolean} primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsBoolean(int)}.
 *
 * @see java.util.function.Function
 * @see IntFunction
 * @see IntPredicate
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntToBooleanFunction {
    /**
     * Default function that converts an {@code int} value to a {@code boolean}.
     * Returns {@code true} if the value is greater than 0, otherwise {@code false}.
     */
    IntToBooleanFunction DEFAULT = value -> value > 0;

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntToBooleanFunction isEven = value -> value % 2 == 0;
     * boolean result = isEven.applyAsBoolean(4); // Returns true
     *
     * // Using DEFAULT
     * boolean defaultResult = IntToBooleanFunction.DEFAULT.applyAsBoolean(1); // Returns true
     * }</pre>
     *
     * @param value the function argument
     * @return the {@code boolean} result
     */
    boolean applyAsBoolean(int value);
}
