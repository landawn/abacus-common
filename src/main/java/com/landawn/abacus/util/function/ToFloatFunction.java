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
 * with the abacus-common framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsFloat(Object)}.
 *
 * @param <T> the type of the input to the function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ToFloatFunction<T> extends Throwables.ToFloatFunction<T, RuntimeException> { //NOSONAR
    /**
     * A predefined ToFloatFunction instance that unboxes a Float object to a primitive float.
     * Returns 0.0f if the input is {@code null}, otherwise returns the float value of the Float object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Float boxed = 3.14f;
     * float primitive = ToFloatFunction.UNBOX.applyAsFloat(boxed); // returns 3.14f
     * float defaultValue = ToFloatFunction.UNBOX.applyAsFloat(null); // returns 0.0f
     * }</pre>
     */
    ToFloatFunction<Float> UNBOX = value -> value == null ? 0 : value;
    /**
     * A predefined ToFloatFunction instance that converts any Number object to a primitive float.
     * Returns 0.0f if the input is {@code null}, otherwise uses the Numbers.toFloat utility method to
     * perform the conversion. This function can handle various Number subclasses including
     * Integer, Long, Double, BigDecimal, etc.
     *
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToFloatFunction<String> parseFunction = Float::parseFloat;
     * float parsed = parseFunction.applyAsFloat("3.14"); // Returns 3.14f
     *
     * ToFloatFunction<Integer> convertFunction = Integer::floatValue;
     * float converted = convertFunction.applyAsFloat(42); // Returns 42.0f
     *
     * ToFloatFunction<Double> castFunction = Double::floatValue;
     * float casted = castFunction.applyAsFloat(123.456); // Returns 123.456f
     * }</pre>
     *
     * @param value the function argument
     * @return the function result as a primitive float
     */
    @Override
    float applyAsFloat(T value);

    /**
     * Converts this {@code ToFloatFunction} to a {@code Throwables.ToFloatFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToFloatFunction function = (...) -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.ToFloatFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.ToFloatFunction<T, E> toThrowable() {
        return (Throwables.ToFloatFunction<T, E>) this;
    }

}
