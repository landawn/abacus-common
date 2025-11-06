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
 * Represents a function that accepts an int-valued argument and produces a
 * double-valued result. This is the {@code int}-to-{@code double} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This interface extends both {@link Throwables.IntToDoubleFunction} with
 * {@link RuntimeException} and {@link java.util.function.IntToDoubleFunction},
 * providing compatibility with the Java standard library while supporting the
 * abacus-common framework's exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsDouble(int)}.
 *
 * <p>Refer to JDK API documentation at:
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see java.util.function.IntToDoubleFunction
 */
@FunctionalInterface
public interface IntToDoubleFunction extends Throwables.IntToDoubleFunction<RuntimeException>, java.util.function.IntToDoubleFunction { //NOSONAR
    /**
     * A default implementation that performs a widening primitive conversion
     * from {@code int} to {@code double}. This conversion is lossless as every
     * int value can be exactly represented as a double value.
     */
    IntToDoubleFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p>The function performs a conversion from an {@code int} value to a
     * {@code double} value. This is a widening primitive conversion that
     * preserves the numeric value. All int values can be exactly represented
     * as double values, so this conversion is lossless.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Converting integer counts or indices to double values for mathematical calculations</li>
     *   <li>Preparing integer data for floating-point arithmetic operations</li>
     *   <li>Converting integer measurements to double for higher precision calculations</li>
     * </ul>
     *
     * @param value the function argument, an int value to be converted to double
     * @return the function result as a double value, which exactly represents
     *         the input int value
     */
    @Override
    double applyAsDouble(int value);

    /**
     * Converts this {@code IntToDoubleFunction} to a {@code Throwables.IntToDoubleFunction} that can throw a checked exception.
     * This method provides a way to use this function in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntToDoubleFunction function = value -> { ... };
     * var throwableFunction = function.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned function can throw
     * @return a {@code Throwables.IntToDoubleFunction} view of this function that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.IntToDoubleFunction<E> toThrowable() {
        return (Throwables.IntToDoubleFunction<E>) this;
    }

}
