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

import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that produces a float-valued result. This is the
 * float-producing primitive specialization for {@link java.util.function.Function}.
 *
 * <p>Unlike the standard Java functional interfaces, this interface does not have a corresponding
 * class in java.util.function package, as Java only provides int, long, and double specializations.
 * This interface fills that gap for float primitive type operations.
 *
 * <p>This interface extends the Throwables.ToFloatFunction, providing compatibility
 * with the Abacus framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsFloat(Object)}.
 *
 * @param <T> the type of the input to the function
 */
@FunctionalInterface
public interface ToFloatFunction<T> extends Throwables.ToFloatFunction<T, RuntimeException> { //NOSONAR

    /**
     * A predefined ToFloatFunction instance that unboxes a Float object to a primitive float.
     * Returns 0.0f if the input is null, otherwise returns the float value of the Float object.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Float boxed = 3.14f;
     * float primitive = ToFloatFunction.UNBOX.applyAsFloat(boxed); // returns 3.14f
     * float defaultValue = ToFloatFunction.UNBOX.applyAsFloat(null); // returns 0.0f
     * }</pre>
     */
    ToFloatFunction<Float> UNBOX = value -> value == null ? 0 : value;

    /**
     * A predefined ToFloatFunction instance that converts any Number object to a primitive float.
     * Returns 0.0f if the input is null, otherwise uses the Numbers.toFloat utility method to
     * perform the conversion. This function can handle various Number subclasses including
     * Integer, Long, Double, BigDecimal, etc.
     * 
     * <p>Example usage:
     * <pre>{@code
     * Integer intValue = 42;
     * float result1 = ToFloatFunction.FROM_NUM.applyAsFloat(intValue); // returns 42.0f
     * 
     * Double doubleValue = 123.456;
     * float result2 = ToFloatFunction.FROM_NUM.applyAsFloat(doubleValue); // returns 123.456f
     * 
     * BigDecimal bigDecimal = new BigDecimal("999.999");
     * float result3 = ToFloatFunction.FROM_NUM.applyAsFloat(bigDecimal); // returns 999.999f
     * 
     * float defaultValue = ToFloatFunction.FROM_NUM.applyAsFloat(null); // returns 0.0f
     * }</pre>
     */
    ToFloatFunction<Number> FROM_NUM = value -> value == null ? 0 : Numbers.toFloat(value);

    /**
     * Applies this function to the given argument and returns a float result.
     *
     * @param value the function argument
     * @return the function result as a primitive float
     * @throws RuntimeException if any error occurs during function execution
     */
    @Override
    float applyAsFloat(T value);
}