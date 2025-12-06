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
 * Represents a function that accepts an int-valued argument and produces a
 * char-valued result. This is the {@code int}-to-{@code char} primitive
 * specialization for {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsChar(int)}.
 *
 * @see java.util.function.Function
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntToCharFunction {
    /**
     * A default implementation that performs a narrowing primitive conversion
     * from {@code int} to {@code char}. This conversion treats the int value
     * as a Unicode code point and converts it to the corresponding char value.
     * Values outside the range of valid char values (0 to 65,535) will be
     * truncated to fit within the 16-bit char representation.
     */
    IntToCharFunction DEFAULT = value -> (char) value;

    /**
     * Applies this function to the given argument.
     *
     * <p>The function performs a conversion from an {@code int} value to a
     * {@code char} value. This is typically used to convert Unicode code points
     * (represented as int values) to their corresponding char representations.
     *
     * <p>Note that {@code char} values in Java are 16-bit unsigned values
     * representing Unicode characters in the range from 0 to 65,535 (0xFFFF).
     * If the input int value is outside this range, only the lower 16 bits
     * will be used for the conversion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntToCharFunction toChar = value -> (char) value;
     * char result1 = toChar.applyAsChar(65);       // Returns 'A'
     * char result2 = toChar.applyAsChar(0x263A);   // Returns '☺' (smiley face)
     *
     * // Convert digit to char
     * IntToCharFunction digitToChar = digit -> (char) ('0' + digit);
     * char result3 = digitToChar.applyAsChar(5);   // Returns '5'
     * }</pre>
     *
     * @param value the function argument, an int value to be converted to char.
     *              This is typically a Unicode code point value
     * @return the function result as a char value. If the input value is outside
     *         the valid char range (0 to 65,535), the result will be the lower
     *         16 bits of the input value (equivalent to {@code (char) value})
     */
    char applyAsChar(int value);
}
