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
 * Represents a function that produces a long-valued result. This is the
 * long-producing primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This interface extends both the Throwables.ToLongFunction and the standard Java
 * ToLongFunction, providing compatibility with both the Abacus framework's error handling
 * mechanisms and the standard Java functional interfaces.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsLong(Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the input to the function
 */
@FunctionalInterface
public interface ToLongFunction<T> extends Throwables.ToLongFunction<T, RuntimeException>, java.util.function.ToLongFunction<T> { //NOSONAR

    /**
     * A predefined ToLongFunction instance that unboxes a Long object to a primitive long.
     * Returns 0L if the input is null, otherwise returns the long value of the Long object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Long boxed = 42L;
     * long primitive = ToLongFunction.UNBOX.applyAsLong(boxed); // returns 42L
     * long defaultValue = ToLongFunction.UNBOX.applyAsLong(null); // returns 0L
     * }</pre>
     */
    ToLongFunction<Long> UNBOX = value -> value == null ? 0 : value;

    /**
     * A predefined ToLongFunction instance that converts any Number object to a primitive long.
     * Returns 0L if the input is null, otherwise calls the longValue() method on the Number object.
     * This function can handle various Number subclasses including Integer, Double, Float, BigDecimal, etc.
     * 
     * <p>Note: This conversion may result in loss of precision for floating-point numbers or
     * overflow for numbers outside the long range.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer intValue = 100;
     * long result1 = ToLongFunction.FROM_NUM.applyAsLong(intValue); // returns 100L
     * 
     * Double doubleValue = 123.456;
     * long result2 = ToLongFunction.FROM_NUM.applyAsLong(doubleValue); // returns 123L (truncated)
     * 
     * BigDecimal bigDecimal = new BigDecimal("9999999999");
     * long result3 = ToLongFunction.FROM_NUM.applyAsLong(bigDecimal); // returns 9999999999L
     * 
     * long defaultValue = ToLongFunction.FROM_NUM.applyAsLong(null); // returns 0L
     * }</pre>
     */
    ToLongFunction<Number> FROM_NUM = value -> value == null ? 0 : value.longValue();

    /**
     * Applies this function to the given argument and returns a long result.
     *
     * @param value the function argument
     * @return the function result as a primitive long if any error occurs during function execution
     */
    @Override
    long applyAsLong(T value);
}