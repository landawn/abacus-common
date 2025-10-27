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
 * Represents a function that accepts two arguments and produces a char-valued result.
 * This is the {@code char}-producing primitive specialization for {@link java.util.function.BiFunction}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsChar(Object, Object)}.
 * 
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * 
 * @see java.util.function.BiFunction
 * @see ToCharFunction
 */
@FunctionalInterface
public interface ToCharBiFunction<T, U> {

    /**
     * Applies this function to the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ToCharBiFunction<String, Integer> charAt = String::charAt;
     * char result1 = charAt.applyAsChar("Hello", 0); // returns 'H'
     * char result2 = charAt.applyAsChar("World", 4); // returns 'd'
     *
     * ToCharBiFunction<Integer, Integer> digitToChar = (digit, base) ->
     *     Character.forDigit(digit, base);
     * char result3 = digitToChar.applyAsChar(10, 16); // returns 'a' (hex)
     *
     * ToCharBiFunction<Character, Integer> shift = (ch, offset) ->
     *     (char) (ch + offset);
     * char result4 = shift.applyAsChar('A', 2); // returns 'C'
     * }</pre>
     *
     * @param t the first function argument
     * @param u the second function argument
     * @return the function result as a char value
     */
    char applyAsChar(T t, U u);
}
