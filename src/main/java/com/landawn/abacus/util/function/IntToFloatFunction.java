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
 * {@code float}-valued result. This is the {@code int}-to-{@code float} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsFloat(int)}.
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
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
     * <p>The function performs a transformation from an {@code int} value to a
     * {@code float} value. The default implementation provided by {@link #DEFAULT}
     * performs a widening primitive conversion, which may lose precision for
     * int values whose magnitude exceeds 2^24 (16,777,216). Custom implementations
     * may apply arbitrary computations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntToFloatFunction percent = value -> value / 100.0f;
     * float result = percent.applyAsFloat(25);   // 0.25f
     * }</pre>
     *
     * @param value the function argument, an int value to be converted to float
     * @return the function result as a float value
     */
    float applyAsFloat(int value);
}
