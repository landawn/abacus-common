/*
 * Copyright (C) 2024 HaiYang Li
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
 * Represents a function that accepts a char-valued argument and produces a boolean-valued result.
 * This is the {@code char}-to-{@code boolean} primitive specialization for {@link java.util.function.Function}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsBoolean(char)}.
 * 
 * @see java.util.function.Function
 * @see CharToIntFunction
 */
@FunctionalInterface
public interface CharToBooleanFunction {

    /**
     * A default implementation that converts a char value to a boolean.
     * Returns {@code true} if the char value is 'Y', 'y', or '1', otherwise {@code false}.
     * This follows common conventions for representing boolean values as characters,
     * where 'Y' represents "yes", 'y' is the lowercase variant, and '1' is the numeric representation of true.
     */
    CharToBooleanFunction DEFAULT = value -> (value == 'Y' || value == 'y' || value == '1');

    /**
     * Applies this function to the given char argument and returns a boolean result.
     *
     * @param value the char function argument
     * @return the boolean function result
     */
    boolean applyAsBoolean(char value);
}