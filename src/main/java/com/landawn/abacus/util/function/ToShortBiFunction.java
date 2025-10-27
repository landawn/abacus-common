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
 * Represents a function that accepts two arguments and produces a short-valued result.
 * This is the short-producing primitive specialization for {@link java.util.function.BiFunction}.
 *
 * <p>Unlike the standard Java functional interfaces, this interface does not have a corresponding
 * class in java.util.function package, as Java only provides int, long, and double specializations.
 * This interface fills that gap for short primitive type operations.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsShort(Object, Object)}.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 */
@FunctionalInterface
public interface ToShortBiFunction<T, U> {

    /**
     * Applies this function to the given arguments and returns a short result.
     * 
     * <p>Note: Care should be taken to ensure the result fits within the short range
     * (-32,768 to 32,767) to avoid overflow.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToShortBiFunction<Integer, Integer> adder = (a, b) -> (short)(a + b);
     * short sum = adder.applyAsShort(100, 200); // returns 300
     * 
     * ToShortBiFunction<String, Integer> charCodeAt = (str, index) -> (short) str.charAt(index);
     * short charCode = charCodeAt.applyAsShort("Hello", 0); // returns 72 (ASCII code for 'H')
     * }</pre>
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result as a primitive short
     */
    short applyAsShort(T t, U u);
}
