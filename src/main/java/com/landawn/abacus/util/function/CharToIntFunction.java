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
 * Represents a function that accepts a char-valued argument and produces an int-valued result.
 * This is the {@code char}-to-{@code int} primitive specialization for {@link java.util.function.Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(char)}.
 * 
 * @see java.util.function.Function
 * @see java.util.function.ToIntFunction
 */
@FunctionalInterface
public interface CharToIntFunction {

    /**
     * A default implementation that performs a widening primitive conversion from char to int.
     * This is equivalent to a simple cast operation that converts the char's Unicode code point
     * value (ranging from 0 to 65,535) to its corresponding int value.
     */
    CharToIntFunction DEFAULT = value -> value;

    /**
     * Applies this function to the given char argument and returns an int result.
     * This method performs a transformation from a char value to an int value.
     *
     * @param value the char function argument
     * @return the int function result, typically the Unicode code point value of the char
     */
    int applyAsInt(char value);
}