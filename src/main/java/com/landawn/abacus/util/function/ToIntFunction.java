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
 * Represents a function that produces an int-valued result. This is the
 * int-producing primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This interface extends both the Throwables.ToIntFunction and the standard Java
 * ToIntFunction, providing compatibility with both the Abacus framework's error handling
 * mechanisms and the standard Java functional interfaces.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the input to the function
 */
@FunctionalInterface
public interface ToIntFunction<T> extends Throwables.ToIntFunction<T, RuntimeException>, java.util.function.ToIntFunction<T> { //NOSONAR

    /**
     * A predefined ToIntFunction instance that unboxes an Integer object to a primitive int.
     * Returns 0 if the input is {@code null}, otherwise returns the int value of the Integer object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer boxed = 42;
     * int primitive = ToIntFunction.UNBOX.applyAsInt(boxed); // returns 42
     * int defaultValue = ToIntFunction.UNBOX.applyAsInt(null); // returns 0
     * }</pre>
     */
    ToIntFunction<Integer> UNBOX = value -> value == null ? 0 : value;

    /**
     * A predefined ToIntFunction instance that converts any Number object to a primitive int.
     * Returns 0 if the input is {@code null}, otherwise calls the intValue() method on the Number object.
     * This function can handle various Number subclasses including Long, Double, Float, BigDecimal, etc.
     * 
     * <p>Note: This conversion may result in loss of precision for floating-point numbers or
     * overflow for numbers outside the int range.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Long longValue = 100L;
     * int result1 = ToIntFunction.FROM_NUM.applyAsInt(longValue); // returns 100
     * 
     * Double doubleValue = 123.456;
     * int result2 = ToIntFunction.FROM_NUM.applyAsInt(doubleValue); // returns 123 (truncated)
     * 
     * BigDecimal bigDecimal = new BigDecimal("999");
     * int result3 = ToIntFunction.FROM_NUM.applyAsInt(bigDecimal); // returns 999
     * 
     * int defaultValue = ToIntFunction.FROM_NUM.applyAsInt(null); // returns 0
     * }</pre>
     */
    ToIntFunction<Number> FROM_NUM = value -> value == null ? 0 : value.intValue();

    /**
     * Applies this function to the given argument and returns an int result.
     *
     * @param value the function argument
     * @return the function result as a primitive int if any error occurs during function execution
     */
    @Override
    int applyAsInt(T value);
}
