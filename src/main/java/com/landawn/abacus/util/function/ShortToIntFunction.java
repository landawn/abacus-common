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

/**
 * Represents a function that accepts a short-valued argument and produces an int-valued result.
 * This is the {@code short}-to-{@code int} primitive specialization for {@link java.util.function.Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(short)}.
 * 
 * @see java.util.function.Function
 * @see java.util.function.ToIntFunction
 */
@FunctionalInterface
public interface ShortToIntFunction {

    /**
     * A function that performs a widening primitive conversion from short to int.
     * This is equivalent to a simple cast operation and will always preserve the exact numeric value.
     */
    ShortToIntFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortToIntFunction widener = ShortToIntFunction.DEFAULT;
     * int result1 = widener.applyAsInt((short) 100); // returns 100
     *
     * ShortToIntFunction doubler = value -> value * 2;
     * int result2 = doubler.applyAsInt((short) 50); // returns 100
     *
     * ShortToIntFunction absolute = value -> Math.abs(value);
     * int result3 = absolute.applyAsInt((short) -42); // returns 42
     * }</pre>
     *
     * @param value the function argument as a short value
     * @return the function result as an int value
     */
    int applyAsInt(short value);
}