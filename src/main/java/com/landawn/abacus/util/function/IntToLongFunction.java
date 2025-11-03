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
 * long-valued result. This is the {@code int}-to-{@code long} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This interface extends both {@link Throwables.IntToLongFunction} with
 * {@link RuntimeException} and {@link java.util.function.IntToLongFunction},
 * providing compatibility with the Java standard library while supporting the
 * abacus-common framework's exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsLong(int)}.
 *
 * <p>Refer to JDK API documentation at:
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.Function
 * @see java.util.function.IntToLongFunction
 */
@FunctionalInterface
public interface IntToLongFunction extends Throwables.IntToLongFunction<RuntimeException>, java.util.function.IntToLongFunction { //NOSONAR
    /**
     * A default implementation that performs a widening primitive conversion
     * from {@code int} to {@code long}. This conversion is lossless as every
     * int value can be exactly represented as a long value.
     */
    IntToLongFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p>The function performs a conversion from an {@code int} value to a
     * {@code long} value. This is a widening primitive conversion that
     * preserves the numeric value and sign. All int values can be exactly
     * represented as long values, so this conversion is lossless.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Preparing int values for operations that may produce results
     *       exceeding the int range</li>
     *   <li>Converting timestamps or IDs from int to long format</li>
     *   <li>Ensuring compatibility with APIs that require long parameters</li>
     *   <li>Preventing integer overflow in arithmetic operations by converting
     *       to long before calculation</li>
     * </ul>
     *
     * @param value the function argument, an int value to be converted to long
     * @return the function result as a long value, which exactly represents
     *         the input int value with the same sign and magnitude
     */
    @Override
    long applyAsLong(int value);
}
