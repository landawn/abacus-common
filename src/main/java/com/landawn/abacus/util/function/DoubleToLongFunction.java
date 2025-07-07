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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Throwables;

/**
 * Represents a function that accepts a double-valued argument and produces a long-valued result.
 * This is the {@code double}-to-{@code long} primitive specialization for {@link java.util.function.Function}.
 * 
 * <p>This functional interface extends both {@link Throwables.DoubleToLongFunction} with {@link RuntimeException}
 * and {@link java.util.function.DoubleToLongFunction}, providing a bridge between the Throwables-based
 * exception handling and the standard Java functional interface.</p>
 * 
 * <p>The interface provides a {@code DEFAULT} constant that performs a simple cast conversion
 * from double to long, truncating any decimal portion.</p>
 * 
 * @see java.util.function.Function
 * @see java.util.function.DoubleToLongFunction
 * @see Throwables.DoubleToLongFunction
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
@FunctionalInterface
public interface DoubleToLongFunction extends Throwables.DoubleToLongFunction<RuntimeException>, java.util.function.DoubleToLongFunction { //NOSONAR

    /**
     * A default implementation that converts a double value to long by casting.
     * The conversion truncates the decimal portion of the double value.
     * For example: 3.14 becomes 3, -2.99 becomes -2.
     */
    DoubleToLongFunction DEFAULT = value -> (long) value;

    /**
     * Applies this function to the given double-valued argument and returns a long result.
     * 
     * <p>This method converts a double value to a long value according to the implementation's logic.
     * The specific conversion behavior depends on the implementation, but typical uses include:</p>
     * <ul>
     *   <li>Simple casting (truncation)</li>
     *   <li>Rounding to nearest integer</li>
     *   <li>Custom conversion logic based on business rules</li>
     * </ul>
     * 
     * @param value the double value to be converted to long
     * @return the long result of applying this function to the input value
     * @throws RuntimeException if the function implementation encounters an error during conversion
     */
    @Override
    long applyAsLong(double value);
}