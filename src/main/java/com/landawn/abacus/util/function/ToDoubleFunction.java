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
 * Represents a function that produces a double-valued result. This is the
 * double-producing primitive specialization for {@link java.util.function.Function}.
 *
 * <p>This interface extends both the Throwables.ToDoubleFunction and the standard Java
 * ToDoubleFunction, providing compatibility with both the abacus-common framework's error handling
 * mechanisms and the standard Java functional interfaces.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsDouble(Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the input to the function
 */
@FunctionalInterface
public interface ToDoubleFunction<T> extends Throwables.ToDoubleFunction<T, RuntimeException>, java.util.function.ToDoubleFunction<T> { //NOSONAR
    /**
     * A predefined ToDoubleFunction instance that unboxes a Double object to a primitive double.
     * Returns 0.0 if the input is {@code null}, otherwise returns the double value of the Double object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Double boxed = 3.14;
     * double primitive = ToDoubleFunction.UNBOX.applyAsDouble(boxed); // returns 3.14
     * double defaultValue = ToDoubleFunction.UNBOX.applyAsDouble(null); // returns 0.0
     * }</pre>
     */
    ToDoubleFunction<Double> UNBOX = value -> value == null ? 0 : value;
    /**
     * A predefined ToDoubleFunction instance that converts any Number object to a primitive double.
     * Returns 0.0 if the input is {@code null}, otherwise uses the Numbers.toDouble utility method to
     * perform the conversion. This function can handle various Number subclasses including
     * Integer, Long, Float, BigDecimal, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer intValue = 42;
     * double result1 = ToDoubleFunction.FROM_NUM.applyAsDouble(intValue); // returns 42.0
     *
     * BigDecimal bigDecimal = new BigDecimal("123.456");
     * double result2 = ToDoubleFunction.FROM_NUM.applyAsDouble(bigDecimal); // returns 123.456
     *
     * double defaultValue = ToDoubleFunction.FROM_NUM.applyAsDouble(null); // returns 0.0
     * }</pre>
     */
    ToDoubleFunction<Number> FROM_NUM = value -> value == null ? 0 : Numbers.toDouble(value);

    /**
     * Applies this function to the given argument and returns a double result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToDoubleFunction<String> parseFunction = Double::parseDouble;
     * double parsed = parseFunction.applyAsDouble("3.14159"); // Returns 3.14159
     *
     * ToDoubleFunction<Integer> convertFunction = Integer::doubleValue;
     * double converted = convertFunction.applyAsDouble(42); // Returns 42.0
     *
     * ToDoubleFunction<BigDecimal> bdFunction = BigDecimal::doubleValue;
     * double value = bdFunction.applyAsDouble(new BigDecimal("123.45")); // Returns 123.45
     * }</pre>
     *
     * @param value the function argument
     * @return the function result as a primitive double
     */
    @Override
    double applyAsDouble(T value);
}
