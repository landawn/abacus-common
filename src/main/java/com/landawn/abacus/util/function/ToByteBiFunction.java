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
 * Represents a function that accepts two arguments and produces a byte-valued result.
 * This is the {@code byte}-producing primitive specialization for {@link java.util.function.BiFunction}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsByte(Object, Object)}.
 * 
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * 
 * @see java.util.function.BiFunction
 * @see ToByteFunction
 */
@FunctionalInterface
public interface ToByteBiFunction<T, U> {

    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToByteBiFunction<Integer, Integer> adder = (a, b) -> (byte) (a + b);
     * byte result1 = adder.applyAsByte(10, 20); // returns 30
     *
     * ToByteBiFunction<String, Integer> charAt = (str, index) ->
     *     (byte) str.charAt(index);
     * byte result2 = charAt.applyAsByte("Hello", 0); // returns 72 (ASCII 'H')
     *
     * ToByteBiFunction<Byte, Byte> max = (a, b) -> (byte) Math.max(a, b);
     * byte result3 = max.applyAsByte((byte) 5, (byte) 10); // returns 10
     * }</pre>
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result as a byte value
     */
    byte applyAsByte(T t, U u);
}