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
 * Represents a function that accepts a long-valued argument and produces a double-valued result.
 * This is the long-to-double primitive specialization for {@code Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsDouble(long)}.
 * 
 * <p>The interface extends both {@code Throwables.LongToDoubleFunction} with {@code RuntimeException} 
 * and {@code java.util.function.LongToDoubleFunction}, providing compatibility with the Java standard library
 * while adding a predefined identity function.
 * 
 * @see java.util.function.Function
 * @see java.util.function.LongToDoubleFunction
 */
@FunctionalInterface
public interface LongToDoubleFunction extends Throwables.LongToDoubleFunction<RuntimeException>, java.util.function.LongToDoubleFunction { //NOSONAR

    /**
     * A function that converts a long value to a double value using a simple cast.
     * 
     * <p>This is the default conversion function that preserves the numeric value
     * as closely as possible when converting from long to double. Note that for
     * very large long values, there may be some loss of precision due to the
     * limitations of double representation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongToDoubleFunction converter = LongToDoubleFunction.DEFAULT;
     * double result = converter.applyAsDouble(42L); // returns 42.0
     * }</pre>
     */
    LongToDoubleFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given argument.
     * 
     * <p>Converts a long value to a double value according to the function's implementation.
     * The specific conversion logic depends on the implementation, but common use cases include:
     * <ul>
     *   <li>Simple type conversion (casting)</li>
     *   <li>Mathematical transformations (e.g., logarithm, square root)</li>
     *   <li>Scaling operations (e.g., converting cents to dollars)</li>
     *   <li>Custom business logic transformations</li>
     * </ul>
     *
     * @param value the function argument as a long
     * @return the function result as a double if any error occurs during the function execution
     */
    @Override
    double applyAsDouble(long value);
}
