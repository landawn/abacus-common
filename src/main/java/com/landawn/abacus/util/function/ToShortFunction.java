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
 * Represents a function that produces a short-valued result. This is the
 * short-producing primitive specialization for {@link java.util.function.Function}.
 *
 * <p>Unlike the standard Java functional interfaces, this interface does not have a corresponding
 * class in java.util.function package, as Java only provides int, long, and double specializations.
 * This interface fills that gap for short primitive type operations.
 *
 * <p>This interface extends the Throwables.ToShortFunction, providing compatibility
 * with the Abacus framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsShort(Object)}.
 *
 * @param <T> the type of the input to the function
 */
@FunctionalInterface
public interface ToShortFunction<T> extends Throwables.ToShortFunction<T, RuntimeException> { //NOSONAR

    /**
     * A predefined ToShortFunction instance that unboxes a Short object to a primitive short.
     * Returns 0 if the input is {@code null}, otherwise returns the short value of the Short object.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Short boxed = 42;
     * short primitive = ToShortFunction.UNBOX.applyAsShort(boxed); // returns 42
     * short defaultValue = ToShortFunction.UNBOX.applyAsShort(null); // returns 0
     * }</pre>
     */
    ToShortFunction<Short> UNBOX = value -> value == null ? 0 : value;

    /**
     * A predefined ToShortFunction instance that converts any Number object to a primitive short.
     * Returns 0 if the input is {@code null}, otherwise calls the shortValue() method on the Number object.
     * This function can handle various Number subclasses including Integer, Long, Double, Float, BigDecimal, etc.
     * 
     * <p>Note: This conversion may result in loss of precision for floating-point numbers or
     * overflow for numbers outside the short range (-32,768 to 32,767).
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer intValue = 100;
     * short result1 = ToShortFunction.FROM_NUM.applyAsShort(intValue); // returns 100
     * 
     * Long longValue = 30000L;
     * short result2 = ToShortFunction.FROM_NUM.applyAsShort(longValue); // returns 30000
     * 
     * Double doubleValue = 123.456;
     * short result3 = ToShortFunction.FROM_NUM.applyAsShort(doubleValue); // returns 123 (truncated)
     * 
     * short defaultValue = ToShortFunction.FROM_NUM.applyAsShort(null); // returns 0
     * }</pre>
     */
    ToShortFunction<Number> FROM_NUM = value -> value == null ? 0 : value.shortValue();

    /**
     * Applies this function to the given argument and returns a short result.
     *
     * @param value the function argument
     * @return the function result as a primitive short if any error occurs during function execution
     */
    @Override
    short applyAsShort(T value);
}
