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
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharToBooleanFunction {
    /**
     * A default implementation that converts a char value to a boolean.
     * Returns {@code true} if the char value is 'Y', 'y', or '1', otherwise {@code false}.
     * This follows common conventions for representing boolean values as characters,
     * where 'Y' represents "yes", 'y' is the lowercase variant, and '1' is the numeric representation of {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharToBooleanFunction defaultConversion = CharToBooleanFunction.DEFAULT;
     * boolean result1 = defaultConversion.applyAsBoolean('Y'); // Returns true
     * boolean result2 = defaultConversion.applyAsBoolean('N'); // Returns false
     * }</pre>
     */
    CharToBooleanFunction DEFAULT = value -> (value == 'Y' || value == 'y' || value == '1');

    /**
     * Applies this function to the given char argument and returns a boolean result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharToBooleanFunction isDigit = c -> Character.isDigit(c);
     * boolean result = isDigit.applyAsBoolean('7'); // Returns true
     *
     * CharToBooleanFunction isUpperCase = c -> Character.isUpperCase(c);
     * boolean upper = isUpperCase.applyAsBoolean('A'); // Returns true
     * }</pre>
     *
     * @param value the char function argument
     * @return the boolean function result
     */
    boolean applyAsBoolean(char value);
}
